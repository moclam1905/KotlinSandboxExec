package com.nguyenmoclam.kotlinsandboxexec.ai

import android.content.Context
import com.nguyenmoclam.kotlinsandboxexec.data.ChatMessage
import org.tensorflow.lite.Interpreter
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalModelAdapter(private val context: Context) : AIModelAdapter {
    private var interpreter: Interpreter? = null
    private val modelFile = "kotlin_assistant_model.tflite"

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            val modelPath = File(context.getExternalFilesDir(null), modelFile)
            if (!modelPath.exists()) {
                throw Exception("Model file not found at ${modelPath.absolutePath}")
            }
            if (!modelPath.canRead()) {
                throw Exception("Cannot read model file at ${modelPath.absolutePath}")
            }
            interpreter = Interpreter(modelPath)
        } catch (e: Exception) {
            e.printStackTrace()
            // Reset interpreter to ensure isAvailable() returns false
            interpreter = null
        }
    }

    override suspend fun sendMessage(message: String, context: List<ChatMessage>): Result<String> {
        return try {
            if (!isAvailable()) {
                return Result.failure(Exception("Local model not available. Please check if the model file exists."))
            }
    
            if (message.isBlank()) {
                return Result.failure(Exception("Message cannot be empty"))
            }
    
            val response = withContext(Dispatchers.Default) {
                try {
                    val contextHistory = context.takeLast(5).joinToString("\n") { it.content }
                    when {
                        message.contains("help", ignoreCase = true) -> generateHelpResponse(message)
                        message.contains("example", ignoreCase = true) -> generateExampleResponse(message)
                        message.contains("error", ignoreCase = true) -> generateErrorResponse(message)
                        else -> generateGeneralResponse("$contextHistory\n$message")
                    }
                } catch (e: Exception) {
                    throw Exception("Failed to generate response: ${e.message}")
                }
            }
            
            if (response.isBlank()) {
                Result.failure(Exception("Failed to generate response"))
            } else {
                Result.success(response)
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to process message: ${e.message}"))
        }
    }

    override fun isAvailable(): Boolean = interpreter != null

    override fun getModelName(): String = "Local Kotlin Assistant"

    override fun getModelType(): ModelType = ModelType.ON_DEVICE

    override fun getEstimatedCostPerMessage(): Double = 0.0 // Free as it runs locally

    private fun generateHelpResponse(message: String): String {
        val topic = when {
            message.contains("function", ignoreCase = true) -> "functions"
            message.contains("class", ignoreCase = true) -> "classes"
            message.contains("syntax", ignoreCase = true) -> "syntax"
            else -> "general"
        }

        return "I can help you with Kotlin programming. Here are some things I can do:\n" +
               "1. Provide code examples\n" +
               "2. Explain Kotlin concepts\n" +
               "3. Help with error messages\n" +
               "4. Suggest best practices\n\n" +
               "You asked about $topic. What would you like to know more about?"
    }

    private fun generateExampleResponse(message: String): String {
        return when {
            message.contains("function", ignoreCase = true) -> 
                "Here's an example of a Kotlin function:\n" +
                "```kotlin\n" +
                "fun greet(name: String): String {\n" +
                "    return \"Hello, \$name!\"\n" +
                "}\n" +
                "\n// Usage example:\n" +
                "val greeting = greet(\"Alice\")  // Returns: Hello, Alice!\n" +
                "```"
            message.contains("class", ignoreCase = true) ->
                "Here's an example of a Kotlin class:\n" +
                "```kotlin\n" +
                "data class User(\n" +
                "    val name: String,\n" +
                "    val age: Int\n" +
                ")\n\n" +
                "// Usage example:\n" +
                "val user = User(\"Alice\", 25)\n" +
                "println(\"\${user.name} is \${user.age} years old\")\n" +
                "```"
            else -> "Here's a basic Kotlin example:\n" +
                   "```kotlin\n" +
                   "fun main() {\n" +
                   "    val message = \"Hello, World!\"\n" +
                   "    println(message)\n" +
                   "}\n" +
                   "```"
        }
    }

    private fun generateErrorResponse(message: String): String {
        val errorType = when {
            message.contains("compile", ignoreCase = true) -> "compilation"
            message.contains("runtime", ignoreCase = true) -> "runtime"
            message.contains("null", ignoreCase = true) -> "null pointer"
            else -> "the"
        }

        return "To help you with $errorType error, please provide:\n" +
               "1. The complete error message\n" +
               "2. The relevant code snippet\n" +
               "3. What you were trying to achieve\n\n" +
               "This will help me provide a more accurate solution."
    }

    private fun generateGeneralResponse(message: String): String {
        val topic = when {
            message.contains("learn", ignoreCase = true) -> "want to learn about"
            message.contains("help", ignoreCase = true) -> "need help with"
            message.contains("how", ignoreCase = true) -> "want to know how to work with"
            else -> "are asking about"
        }

        return "I understand you $topic Kotlin. To help you better, could you:\n" +
               "1. Be more specific about what you want to learn\n" +
               "2. Share any code you're working with\n" +
               "3. Mention if you're facing any particular challenges"
    }

    fun release() {
        try {
            interpreter?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            interpreter = null
        }
    }
}