package com.nguyenmoclam.kotlinsandboxexec.domain.model

data class Message(
    val id: Long = 0,
    val content: String,
    val isFromUser: Boolean,
    val codeSnippet: String? = null,
    var status: Status = Status.SENT,
    val conversationId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class Status {
        SENT,
        SENDING,
        ERROR
    }
}

sealed class MessageResult {
    data class Success(val message: Message) : MessageResult()
    data class Error(val exception: Exception) : MessageResult()
}