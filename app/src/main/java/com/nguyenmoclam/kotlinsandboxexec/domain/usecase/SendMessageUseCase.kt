package com.nguyenmoclam.kotlinsandboxexec.domain.usecase

import com.nguyenmoclam.kotlinsandboxexec.domain.repository.IChatRepository
import com.nguyenmoclam.kotlinsandboxexec.domain.model.Message
import com.nguyenmoclam.kotlinsandboxexec.domain.model.MessageResult

class SendMessageUseCase(private val chatRepository: IChatRepository) {
    suspend operator fun invoke(content: String, codeSnippet: String? = null): MessageResult {
        if (content.isBlank() && codeSnippet == null) {
            return MessageResult.Error(Exception("Message content cannot be empty"))
        }

        val userMessage = Message(
            content = content,
            isFromUser = true,
            codeSnippet = codeSnippet,
            status = Message.Status.SENDING
        )
        
        return try {
            chatRepository.addMessage(userMessage)
            val botResponse = chatRepository.getBotResponse(content)
            userMessage.status = Message.Status.SENT
            chatRepository.updateMessage(userMessage)
            MessageResult.Success(botResponse)
        } catch (e: Exception) {
            userMessage.status = Message.Status.ERROR
            chatRepository.updateMessage(userMessage)
            when (e) {
                is IllegalStateException -> MessageResult.Error(Exception("AI model not initialized properly"))
                is OutOfMemoryError -> MessageResult.Error(Exception("Message processing exceeded memory limit"))
                else -> MessageResult.Error(Exception("Failed to process message: ${e.message}"))
            }
        }
    }
}