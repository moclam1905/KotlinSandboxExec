package com.nguyenmoclam.kotlinsandboxexec.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MessageRepository(context: Context) {
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
        "chat_messages",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveMessages(messages: List<ChatMessage>) {
        val json = gson.toJson(messages)
        sharedPreferences.edit().putString("messages", json).apply()
    }

    fun getMessages(): List<ChatMessage> {
        val json = sharedPreferences.getString("messages", null)
        return if (json != null) {
            val type = object : TypeToken<List<ChatMessage>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    fun clearMessages() {
        sharedPreferences.edit().remove("messages").apply()
    }

    fun addMessage(message: ChatMessage) {
        val messages = getMessages().toMutableList()
        messages.add(message)
        saveMessages(messages)
    }
}