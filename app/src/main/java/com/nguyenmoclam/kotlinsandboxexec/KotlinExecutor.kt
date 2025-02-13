package com.nguyenmoclam.kotlinsandboxexec

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.util.concurrent.TimeUnit
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import java.net.URLClassLoader

class KotlinExecutor {
    private var currentFuture: java.util.concurrent.Future<*>? = null
    private val executorService = java.util.concurrent.Executors.newSingleThreadExecutor()
    private val compiler = K2JVMCompiler()
    private val tempDir = File(System.getProperty("java.io.tmpdir"), "kotlin_sandbox")

    init {
        setIdeaIoUseFallback()
        tempDir.mkdirs()
    }

    fun execute(code: String, timeoutMs: Long, memoryLimitPercent: Int): String {
        require(timeoutMs > 0) { "Timeout must be positive" }
        require(memoryLimitPercent in 1..90) { "Memory limit must be between 1 and 90 percent" }
        val outputStream = ByteArrayOutputStream()
        val printStream = PrintStream(outputStream)
        val originalOut = System.out
        val originalErr = System.err
        val memoryMonitor = java.util.Timer("MemoryMonitor", true)

        try {
            System.setOut(printStream)
            System.setErr(printStream)

            // Set memory limit and monitor usage
            val runtime = Runtime.getRuntime()
            val maxMemory = runtime.maxMemory() * memoryLimitPercent / 100
            
            var executionCancelled = false
            val memoryTask = object : java.util.TimerTask() {
                override fun run() {
                    val usedMemory = runtime.totalMemory() - runtime.freeMemory()
                    if (usedMemory > maxMemory) {
                        currentFuture?.cancel(true)
                        executionCancelled = true
                        cancel()
                        System.gc()
                        printStream.println("Memory limit exceeded")
                    }
                }
            }
            memoryMonitor.schedule(memoryTask, 0, 50)

            try {
                currentFuture = executorService.submit<String> {
                    try {
                        // Create a temporary file for the code
                        val tempFile = File.createTempFile("script", ".kt", tempDir)
                        tempFile.writeText(code)

                        // Prepare compiler arguments
                        val args = K2JVMCompilerArguments().apply {
                            freeArgs = listOf(tempFile.absolutePath)
                            noStdlib = true
                            noReflect = true
                            jvmTarget = "1.8"
                            classpath = System.getProperty("java.class.path")
                            destination = tempDir.absolutePath
                        }

                        // Compile the code
                        val messageCollector = object : org.jetbrains.kotlin.cli.common.messages.MessageCollector {
                            override fun clear() {}
                            override fun hasErrors(): Boolean = false
                            override fun report(severity: org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity, message: String, location: org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation?) {
                                printStream.println(message)
                            }
                        }
                        val exitCode = compiler.exec(messageCollector, Services.EMPTY, args)
                        if (exitCode != org.jetbrains.kotlin.cli.common.ExitCode.OK) {
                            return@submit "Compilation failed: $outputStream"
                        }

                        // Load and execute the compiled class
                        val className = "Script" // Default name for Kotlin script
                        val classLoader = URLClassLoader(arrayOf(tempDir.toURI().toURL()))
                        val mainClass = classLoader.loadClass(className)
                        val mainMethod = mainClass.getDeclaredMethod("main", Array<String>::class.java)
                        mainMethod.invoke(null, arrayOf<String>())

                        val output = outputStream.toString()
                        output.ifEmpty { "Code executed successfully with no output" }
                    } catch (e: Exception) {
                        throw e
                    } finally {
                        System.gc()
                    }
                }

                val result = currentFuture?.get(timeoutMs, TimeUnit.MILLISECONDS)
                return if (executionCancelled) {
                    "Execution exceeded memory limit of ${maxMemory / (1024 * 1024)}MB"
                } else {
                    (result as? String) ?: "No output"
                }
            } finally {
                memoryTask.cancel()
                memoryMonitor.cancel()
                currentFuture?.cancel(true)
                currentFuture = null
                System.gc()
            }
        } catch (e: java.util.concurrent.TimeoutException) {
            "Execution timed out after ${timeoutMs/1000} seconds"
        } catch (e: OutOfMemoryError) {
            System.gc()
            "Execution exceeded memory limit"
        } catch (e: Exception) {
            when {
                e.message?.contains("timed out", ignoreCase = true) == true -> 
                    "Execution timed out after ${timeoutMs/1000} seconds"
                e.message?.contains("memory", ignoreCase = true) == true -> 
                    "Execution exceeded memory limit"
                else -> "Error: ${e.message}"
            }
        } finally {
            System.setOut(originalOut)
            System.setErr(originalErr)
            tempDir.listFiles()?.forEach { it.delete() }
        }
        return "Execution completed with no output"
    }

    fun shutdown() {
        executorService.shutdown()
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executorService.shutdownNow()
        }
        tempDir.deleteRecursively()
    }
}