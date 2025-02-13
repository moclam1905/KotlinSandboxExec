package com.nguyenmoclam.kotlinsandboxexec.ai

import com.nguyenmoclam.kotlinsandboxexec.data.ChatMessage

interface AIModelAdapter {
    suspend fun sendMessage(message: String, context: List<ChatMessage> = emptyList()): Result<String>
    
    fun isAvailable(): Boolean
    
    fun getModelName(): String
    
    fun getModelType(): ModelType
    
    fun getEstimatedCostPerMessage(): Double = 0.0 // Default 0 for free models
}

enum class ModelType {
    CLOUD,
    ON_DEVICE
}