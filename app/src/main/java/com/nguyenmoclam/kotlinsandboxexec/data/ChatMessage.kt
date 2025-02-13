package com.nguyenmoclam.kotlinsandboxexec.data

import java.util.*

data class ChatMessage(
    val id: Long = 0,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFromUser: Boolean,
    val codeSnippet: String? = null,
    val status: MessageStatus = MessageStatus.SENT,
    val conversationId: String? = null
)

enum class MessageStatus {
    SENDING,
    SENT,
    ERROR
}