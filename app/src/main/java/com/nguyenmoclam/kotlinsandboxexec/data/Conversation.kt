package com.nguyenmoclam.kotlinsandboxexec.data

import com.nguyenmoclam.kotlinsandboxexec.data.ChatMessage
import java.util.UUID

data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val messages: List<ChatMessage> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)