package com.nguyenmoclam.kotlinsandboxexec.domain.repository

import com.nguyenmoclam.kotlinsandboxexec.domain.model.Message

interface IChatRepository {
    suspend fun addMessage(message: Message)
    suspend fun updateMessage(message: Message)
    suspend fun getBotResponse(content: String): Message
    suspend fun getMessages(): List<Message>
    suspend fun getMessagesForConversation(conversationId: String): List<Message>
    suspend fun clearMessages()
    suspend fun saveMessages(messages: List<Message>)
}