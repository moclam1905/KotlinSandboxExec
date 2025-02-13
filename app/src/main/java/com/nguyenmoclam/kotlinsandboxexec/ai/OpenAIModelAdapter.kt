package com.nguyenmoclam.kotlinsandboxexec.ai

import com.nguyenmoclam.kotlinsandboxexec.data.ChatMessage
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage as OpenAIChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.api.exception.OpenAIAPIException
import kotlinx.coroutines.withTimeout
import com.nguyenmoclam.kotlinsandboxexec.utils.MarkdownProcessor

class OpenAIModelAdapter(private val apiKey: String) : AIModelAdapter {
    private val client by lazy { OpenAI(apiKey) }
    private val modelId = ModelId("gpt-3.5-turbo")
    private val costPerToken = 0.000002 // $0.002 per 1K tokens
    private val maxTokens = 4096
    private val maxContextMessages = 10
    private val timeout = 30000L // 30 seconds timeout

    @OptIn(BetaOpenAI::class)
    override suspend fun sendMessage(message: String, context: List<ChatMessage>): Result<String> {
        return try {
            if (!isAvailable()) {
                return Result.failure(Exception("API key is not set or invalid"))
            }
    
            if (message.isBlank()) {
                return Result.failure(Exception("Message cannot be empty"))
            }
    
            val messages = mutableListOf(
                OpenAIChatMessage(
                    role = ChatRole.System,
                    content = "You are a helpful AI assistant with expertise in Kotlin programming. Provide clear, concise answers with code examples when appropriate."
                )
            )
    
            // Add recent conversation history, limited to prevent token overflow
            context.takeLast(maxContextMessages).forEach { chatMessage ->
                if (chatMessage.content.isNotBlank()) {
                    messages.add(
                        OpenAIChatMessage(
                            role = if (chatMessage.isFromUser) ChatRole.User else ChatRole.Assistant,
                            content = chatMessage.content
                        )
                    )
                }
            }
    
            messages.add(
                OpenAIChatMessage(
                    role = ChatRole.User,
                    content = message
                )
            )
    
            val request = ChatCompletionRequest(
                model = modelId,
                messages = messages,
                maxTokens = maxTokens,
                temperature = 0.7,
                presencePenalty = 0.7,  // Increased to reduce repetition
                frequencyPenalty = 0.3   // Reduced to allow more natural responses
            )
    
            withTimeout(timeout) {
                val response = client.chatCompletion(request)
                val rawReply = response.choices.firstOrNull()?.message?.content
                    ?: throw Exception("No response generated")
    
                // Process the markdown response using MarkdownProcessor
                val processedResponse = MarkdownProcessor.process(rawReply)
                Result.success(processedResponse.text)
            }
        } catch (e: OpenAIAPIException) {
            when {
                e.message?.contains("rate limit", ignoreCase = true) == true -> 
                    Result.failure(Exception("Rate limit exceeded. Please try again in a few seconds."))
                e.message?.contains("invalid_api_key", ignoreCase = true) == true -> 
                    Result.failure(Exception("Invalid API key. Please check your settings."))
                else -> Result.failure(Exception("OpenAI API error: ${e.message}"))
            }
        } catch (e: java.util.concurrent.TimeoutException) {
            Result.failure(Exception("Request timed out after ${timeout/1000} seconds. Please try again."))
        } catch (e: OutOfMemoryError) {
            Result.failure(Exception("Memory limit exceeded. Please try with a shorter message."))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get response: ${e.message}"))
        }
    }

    override fun isAvailable(): Boolean = apiKey.isNotBlank()

    override fun getModelName(): String = "GPT-3.5 Turbo"

    override fun getModelType(): ModelType = ModelType.CLOUD

    override fun getEstimatedCostPerMessage(): Double = costPerToken * 1000 // Estimate for average message
}