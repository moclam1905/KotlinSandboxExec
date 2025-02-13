package com.nguyenmoclam.kotlinsandboxexec.data.repository

import com.nguyenmoclam.kotlinsandboxexec.domain.repository.IChatRepository
import com.nguyenmoclam.kotlinsandboxexec.domain.model.Message
import com.nguyenmoclam.kotlinsandboxexec.data.MessageRepository
import com.nguyenmoclam.kotlinsandboxexec.ai.AIModelAdapter
import com.nguyenmoclam.kotlinsandboxexec.data.ChatMessage
import com.nguyenmoclam.kotlinsandboxexec.data.MessageStatus

class ChatRepositoryImpl(
    private val messageRepository: MessageRepository,
    private val aiModel: AIModelAdapter
) : IChatRepository {

    override suspend fun addMessage(message: Message) {
        val chatMessage = message.toChatMessage()
        messageRepository.addMessage(chatMessage)
    }

    override suspend fun updateMessage(message: Message) {
        val chatMessage = message.toChatMessage()
        val messages = messageRepository.getMessages().toMutableList()
        val index = messages.indexOfFirst { it.id == chatMessage.id }
        if (index != -1) {
            messages[index] = chatMessage
            messageRepository.saveMessages(messages)
        }
    }

    override suspend fun getBotResponse(content: String): Message {
        val result = aiModel.sendMessage(content, emptyList())
        return result.fold(
            onSuccess = { response ->
                Message(
                    content = response,
                    isFromUser = false,
                    status = Message.Status.SENT
                )
            },
            onFailure = { error ->
                Message(
                    content = "Error: ${error.message}",
                    isFromUser = false,
                    status = Message.Status.ERROR
                )
            }
        )
    }

    override suspend fun getMessages(): List<Message> {
        return messageRepository.getMessages().map { it.toMessage() }
    }

    override suspend fun getMessagesForConversation(conversationId: String): List<Message> {
        return messageRepository.getMessages()
            .filter { it.conversationId == conversationId }
            .map { it.toMessage() }
    }

    override suspend fun clearMessages() {
        messageRepository.clearMessages()
    }

    override suspend fun saveMessages(messages: List<Message>) {
        messageRepository.saveMessages(messages.map { it.toChatMessage() })
    }

    private fun Message.toChatMessage(): ChatMessage {
        return ChatMessage(
            id = id.toLong(),
            content = content,
            isFromUser = isFromUser,
            codeSnippet = codeSnippet,
            status = when (status) {
                Message.Status.SENT -> MessageStatus.SENT
                Message.Status.SENDING -> MessageStatus.SENDING
                Message.Status.ERROR -> MessageStatus.ERROR
            },
            conversationId = conversationId,
            timestamp = timestamp
        )
    }

    private fun ChatMessage.toMessage(): Message {
        return Message(
            id = id,
            content = content,
            isFromUser = isFromUser,
            codeSnippet = codeSnippet,
            status = when (status) {
                MessageStatus.SENT -> Message.Status.SENT
                MessageStatus.SENDING -> Message.Status.SENDING
                MessageStatus.ERROR -> Message.Status.ERROR
            },
            conversationId = conversationId,
            timestamp = timestamp
        )
    }
}