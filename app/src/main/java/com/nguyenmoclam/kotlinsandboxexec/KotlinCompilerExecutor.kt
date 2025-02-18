package com.nguyenmoclam.kotlinsandboxexec

import android.content.Context
import dalvik.system.DexClassLoader
import java.io.*
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * KotlinCompilerExecutor kiểu CodeAssist:
 *
 * - Sử dụng file pre‑dexed của Kotlin compiler (ví dụ "libs/kotlin_compiler_dex.jar") được đóng gói trong assets
 *   để biên dịch code.
 * - Đồng thời, extract file JAR gốc của Kotlin compiler (ví dụ "libs/kotlin-compiler-embeddable-1.7.20-RC.jar")
 *   để cung cấp các resource cần thiết, sau đó set system property "kotlin.home".
 * - Sau khi biên dịch thành công (tạo file .class), dùng DexClassLoader khác để nạp các lớp đã biên dịch và chạy hàm main.
 */
class KotlinCompilerExecutor(private val context: Context) {

    private val executorService = Executors.newSingleThreadExecutor()
    private var currentFuture: Future<*>? = null

    // Thư mục tạm dùng để lưu source code, output, resource… (sử dụng cacheDir của Android)
    private val tempDir: File = File(context.cacheDir, "kotlin_compiler_temp").apply { mkdirs() }

    /**
     * Thực thi code Kotlin động với giới hạn thời gian [timeoutMs] và giới hạn bộ nhớ [memoryLimitPercent].
     */
    fun execute(code: String, timeoutMs: Long, memoryLimitPercent: Int): String {
        require(timeoutMs > 0) { "Timeout must be positive" }
        require(memoryLimitPercent in 1..90) { "Memory limit must be between 1 and 90 percent" }

        // --- Bước 0: Extract resource JAR và set kotlin.home ---
        // File gốc chứa resource của Kotlin compiler.
        val compilerResourcesDir = extractCompilerResources("libs/kotlin-compiler-embeddable-1.7.20-RC.jar")
        System.setProperty("kotlin.home", compilerResourcesDir.absolutePath)

        val outputStream = ByteArrayOutputStream()
        val printStream = PrintStream(outputStream)

        val originalOut = System.out
        val originalErr = System.err

        val runtime = Runtime.getRuntime()
        val maxMemoryAllowed = (runtime.maxMemory() * memoryLimitPercent) / 100
        var executionCancelledByMemory = false

        val memoryMonitor = Timer("MemoryMonitor", true)
        try {
            System.setOut(printStream)
            System.setErr(printStream)

            val memoryTask = object : TimerTask() {
                override fun run() {
                    val usedMem = runtime.totalMemory() - runtime.freeMemory()
                    if (usedMem > maxMemoryAllowed) {
                        currentFuture?.cancel(true)
                        executionCancelledByMemory = true
                        printStream.println("Memory limit exceeded!")
                        cancel()
                    }
                }
            }
            memoryMonitor.schedule(memoryTask, 0L, 100L)

            currentFuture = executorService.submit<String> {
                // --- Bước 1: Biên dịch code bằng Kotlin compiler ---
                // Tạo file nguồn (nếu code chưa có fun main, tự bọc nó vào)
                val sourceFile = File(tempDir, "KotlinScript.kt").apply {
                    writeText(wrapCodeInMain(code))
                }
                // Tạo thư mục output chứa các file .class được biên dịch
                val outputDir = File(tempDir, "output").apply { mkdirs() }

                // Xây dựng mảng đối số cho Kotlin compiler:
                // -d <outputDir> : thư mục output
                // -jvm-target 1.8: (điều chỉnh nếu cần)
                // -no-reflect    : vô hiệu hoá reflect
                // -no-stdlib     : không tự thêm kotlin-stdlib và kotlin-script-runtime từ kotlin.home
                // Và cuối cùng là đường dẫn đến file nguồn
                val argsArray = arrayOf(
                    "-d", outputDir.absolutePath,
                    "-jvm-target", "1.8",
                    "-no-reflect",
                    "-no-stdlib",  // <-- Thêm flag này
                    sourceFile.absolutePath
                )

                // Nạp file pre‑dexed compiler từ assets.
                val compilerJar = extractCompilerJar("libs/kotlin_compiler_dex.jar")
                val compilerClassLoader = DexClassLoader(
                    compilerJar.absolutePath,
                    context.codeCacheDir.absolutePath,
                    null,
                    context.classLoader
                )
                // Nạp lớp K2JVMCompiler từ file pre‑dexed.
                val compilerClass = compilerClassLoader.loadClass("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
                val compilerInstance = compilerClass.getDeclaredConstructor().newInstance()
                // Gọi phương thức exec(PrintStream, String[]) qua reflection.
                val execMethod = compilerClass.getMethod("exec", PrintStream::class.java, Array<String>::class.java)
                val exitCode = execMethod.invoke(compilerInstance, printStream, argsArray).toString()
                if (!exitCode.contains("OK")) {
                    return@submit "Compilation error:\n${outputStream.toString()}"
                }

                // --- Bước 2: Chạy code đã biên dịch ---
                return@submit runCompiledCode(outputDir, printStream)
            }

            val result = try {
                currentFuture?.get(timeoutMs, TimeUnit.MILLISECONDS) as? String
            } catch (toe: TimeoutException) {
                currentFuture?.cancel(true)
                "Execution timed out after ${timeoutMs} ms"
            }

            return if (executionCancelledByMemory) {
                "Execution exceeded memory limit (~${maxMemoryAllowed / (1024 * 1024)} MB)"
            } else {
                result ?: "No output"
            }
        } catch (oom: OutOfMemoryError) {
            return "Execution exceeded memory limit (OutOfMemoryError)"
        } catch (e: Exception) {
            return "Execution error: ${e.message}"
        } finally {
            System.setOut(originalOut)
            System.setErr(originalErr)
            memoryMonitor.cancel()
            memoryMonitor.purge()
            currentFuture?.cancel(true)
            currentFuture = null
            tempDir.deleteRecursively()
        }
    }

    /**
     * Chạy code đã biên dịch: dùng DexClassLoader để nạp các file .class từ [outputDir],
     * sau đó gọi "KotlinScriptKt.main()".
     */
    private fun runCompiledCode(outputDir: File, printStream: PrintStream): String {
        val codeClassLoader = DexClassLoader(
            outputDir.absolutePath,
            context.codeCacheDir.absolutePath,
            null,
            this::class.java.classLoader
        )
        return try {
            val mainClass = codeClassLoader.loadClass("KotlinScriptKt")
            val mainMethod = mainClass.getDeclaredMethod("main", Array<String>::class.java)
            mainMethod.invoke(null, arrayOf<String>())
            val logs = printStream.toString()
            if (logs.isBlank()) "Code executed successfully with no output" else logs
        } catch (ex: Throwable) {
            "Run error: ${ex.message}"
        }
    }

    /**
     * Nếu code chưa có fun main(...), tự động bọc nó vào hàm main.
     */
    private fun wrapCodeInMain(code: String): String {
        return if (!code.contains("fun main")) {
            """
            fun main(args: Array<String>) {
                $code
            }
            """.trimIndent()
        } else {
            code
        }
    }

    /**
     * Trích xuất file (như pre‑dexed compiler) từ assets về thư mục filesDir.
     * Tên file asset cần trùng khớp (ví dụ "libs/kotlin_compiler_dex.jar").
     */
    private fun extractCompilerJar(assetName: String): File {
        val dest = File(context.filesDir, assetName.substringAfterLast("/"))
        if (!dest.exists()) {
            context.assets.open(assetName).use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return dest
    }

    /**
     * Extract toàn bộ resource từ file JAR gốc của Kotlin compiler (chứa resource) vào một thư mục.
     * Ví dụ, asset "libs/kotlin-compiler-embeddable-1.7.20-RC.jar" sẽ được giải nén vào folder "compilerResources".
     */
    private fun extractCompilerResources(assetName: String): File {
        val destDir = File(context.filesDir, "compilerResources")
        if (!destDir.exists()) {
            destDir.mkdirs()
            context.assets.open(assetName).use { input ->
                unzip(input, destDir)
            }
        }
        return destDir
    }

    /**
     * Giúp giải nén file ZIP (JAR) từ inputStream vào destDir.
     */
    private fun unzip(inputStream: InputStream, destDir: File) {
        ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val file = File(destDir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile.mkdirs()
                    FileOutputStream(file).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    /**
     * Tắt executorService và xoá thư mục tạm.
     */
    fun shutdown() {
        executorService.shutdown()
        try {
            if (!executorService.awaitTermination(3, TimeUnit.SECONDS)) {
                executorService.shutdownNow()
            }
        } catch (ie: InterruptedException) {
            executorService.shutdownNow()
        }
        tempDir.deleteRecursively()
    }
}
