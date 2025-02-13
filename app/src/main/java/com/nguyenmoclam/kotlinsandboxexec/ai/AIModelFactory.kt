package com.nguyenmoclam.kotlinsandboxexec.ai

import android.content.Context

class AIModelFactory {
    companion object {
        fun createModel(context: Context, apiKey: String? = null): AIModelAdapter {
            return if (!apiKey.isNullOrBlank()) {
                OpenAIModelAdapter(apiKey)
            } else {
                LocalModelAdapter(context)
            }
        }
    }
}