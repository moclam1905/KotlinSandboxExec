package com.nguyenmoclam.kotlinsandboxexec.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nguyenmoclam.kotlinsandboxexec.ai.AIModelAdapter
import com.nguyenmoclam.kotlinsandboxexec.ai.LocalModelAdapter
import com.nguyenmoclam.kotlinsandboxexec.data.Conversation
import com.nguyenmoclam.kotlinsandboxexec.data.repository.ChatRepositoryImpl
import com.nguyenmoclam.kotlinsandboxexec.data.MessageRepository
import com.nguyenmoclam.kotlinsandboxexec.domain.model.Message
import com.nguyenmoclam.kotlinsandboxexec.domain.model.MessageResult
import com.nguyenmoclam.kotlinsandboxexec.domain.usecase.SendMessageUseCase
import com.nguyenmoclam.kotlinsandboxexec.ui.ChatFragment
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private var isCleared = false
    private val messageRepository = MessageRepository(application)
    private val _messages = MutableLiveData<List<Message>>(emptyList())
    val messages: LiveData<List<Message>> = _messages

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _currentConversationId = MutableLiveData<String?>()
    val currentConversationId: LiveData<String?> = _currentConversationId

    private var currentModel: AIModelAdapter? = null
    private var chatRepository: ChatRepositoryImpl? = null
    private var sendMessageUseCase: SendMessageUseCase? = null
    private val chatHistory = mutableListOf<Message>()

    fun loadMessages() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                chatRepository?.let { repo ->
                    val messages = repo.getMessages()
                    chatHistory.clear() // Clear before adding to prevent duplicates
                    chatHistory.addAll(messages)
                    _messages.value = chatHistory.toList()
                } ?: run {
                    _error.value = "Chat repository not initialized"
                }
            } catch (e: Exception) {
                _error.value = "Failed to load messages: ${e.message}"
                chatHistory.clear()
                _messages.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setAIModel(model: AIModelAdapter) {
        if (!isCleared) {
            try {
                // Clean up existing model if it's a LocalModelAdapter
                if (currentModel is LocalModelAdapter) {
                    (currentModel as LocalModelAdapter).release()
                }
                
                currentModel = model
                chatRepository = ChatRepositoryImpl(messageRepository, model)
                sendMessageUseCase = SendMessageUseCase(chatRepository!!)
                
                // Clear existing messages when switching models
                chatHistory.clear()
                _messages.value = emptyList()
                _error.value = null
                loadMessages()
            } catch (e: Exception) {
                _error.value = "Failed to initialize AI model: ${e.message}"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        isCleared = true
        viewModelScope.launch {
            try {
                chatRepository?.clearMessages()
            } catch (e: Exception) {
                // Log error but don't crash
                e.printStackTrace()
            }
        }
        try {
            if (currentModel is LocalModelAdapter) {
                (currentModel as LocalModelAdapter).release()
            }
        } catch (e: Exception) {
            // Log error but don't crash during cleanup
            e.printStackTrace()
        } finally {
            currentModel = null
            chatRepository = null
            sendMessageUseCase = null
            chatHistory.clear()
        }
    }

    fun sendMessage(content: String, codeSnippet: String? = null) {
        if (content.isBlank() && codeSnippet == null) return
        if (sendMessageUseCase == null) {
            _error.value = "AI Model not initialized"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
    
                // Create user message first
                val userMessage = Message(
                    content = content,
                    isFromUser = true,
                    codeSnippet = codeSnippet,
                    status = Message.Status.SENDING,
                    conversationId = _currentConversationId.value
                )
                chatHistory.add(userMessage)
                _messages.value = chatHistory.toList()
    
                val result = sendMessageUseCase!!(content, codeSnippet)
                // Update user message status to sent
                val updatedUserMessage = userMessage.copy(status = Message.Status.SENT)
                chatHistory[chatHistory.lastIndex] = updatedUserMessage
                
                when (result) {
                    is MessageResult.Success -> {
                        val botMessage = result.message.copy(
                            conversationId = _currentConversationId.value,
                            status = Message.Status.SENT
                        )
                        chatHistory.add(botMessage)
                        _messages.value = chatHistory.toList()
                        _error.value = null
                    }
                    is MessageResult.Error -> {
                        _error.value = result.exception.message ?: "Unknown error occurred"
                        // Update user message status to error
                        val errorMessage = updatedUserMessage.copy(status = Message.Status.ERROR)
                        chatHistory[chatHistory.lastIndex] = errorMessage
                        _messages.value = chatHistory.toList()
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An unexpected error occurred"
                // Update user message status to error if it exists
                val lastMessage = chatHistory.lastOrNull()
                if (lastMessage?.isFromUser == true) {
                    val errorMessage = lastMessage.copy(status = Message.Status.ERROR)
                    chatHistory[chatHistory.lastIndex] = errorMessage
                    _messages.value = chatHistory.toList()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            try {
                chatHistory.clear()
                _messages.value = emptyList()
                _currentConversationId.value = null
                _error.value = null
                chatRepository?.clearMessages()
            } catch (e: Exception) {
                _error.value = "Failed to clear chat: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadConversation(conversation: Conversation) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                chatRepository?.let { repo ->
                    chatHistory.clear()
                    _currentConversationId.value = conversation.id
                    val messages = repo.getMessagesForConversation(conversation.id)
                    chatHistory.addAll(messages)
                    _messages.value = chatHistory.toList()
                } ?: run {
                    _error.value = "Chat repository not initialized"
                }
            } catch (e: Exception) {
                _error.value = "Failed to load conversation: ${e.message}"
                chatHistory.clear()
                _messages.value = emptyList()
                _currentConversationId.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    companion object {
        fun newInstance() = ChatFragment()
    }
}