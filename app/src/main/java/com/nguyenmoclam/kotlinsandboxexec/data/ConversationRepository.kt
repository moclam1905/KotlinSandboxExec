package com.nguyenmoclam.kotlinsandboxexec.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ConversationRepository(context: Context) {
    private val gson = Gson()
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .setKeyGenParameterSpec(
            KeyGenParameterSpec.Builder(
                MasterKey.DEFAULT_MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        )
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "conversations",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveConversation(conversation: Conversation) {
        val conversations = getConversations().toMutableList()
        val index = conversations.indexOfFirst { it.id == conversation.id }
        if (index != -1) {
            conversations[index] = conversation
        } else {
            conversations.add(conversation)
        }
        val json = gson.toJson(conversations)
        sharedPreferences.edit().putString("conversations", json).apply()
    }

    fun getConversations(): List<Conversation> {
        val json = sharedPreferences.getString("conversations", null)
        return if (json != null) {
            val type = object : TypeToken<List<Conversation>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    fun deleteConversation(conversationId: String) {
        val conversations = getConversations().toMutableList()
        conversations.removeAll { it.id == conversationId }
        val json = gson.toJson(conversations)
        sharedPreferences.edit().putString("conversations", json).apply()
    }

    fun getConversation(conversationId: String): Conversation? {
        return getConversations().find { it.id == conversationId }
    }

    fun clearConversations() {
        sharedPreferences.edit().remove("conversations").apply()
    }
}