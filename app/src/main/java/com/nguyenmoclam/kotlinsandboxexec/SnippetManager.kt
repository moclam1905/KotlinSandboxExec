package com.nguyenmoclam.kotlinsandboxexec

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SnippetManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("kotlin_sandbox_snippets", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val maxHistorySize = context.getSharedPreferences("kotlin_sandbox_settings", Context.MODE_PRIVATE)
        .getInt("history_size", 10)

    fun saveSnippet(name: String, code: String) {
        val snippets = getSavedSnippets().toMutableMap()
        snippets[name] = code
        prefs.edit().putString("saved_snippets", gson.toJson(snippets)).apply()
    }

    fun getSavedSnippets(): Map<String, String> {
        val json = prefs.getString("saved_snippets", "{}")
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(json, type) ?: emptyMap()
    }

    fun deleteSnippet(name: String) {
        val snippets = getSavedSnippets().toMutableMap()
        snippets.remove(name)
        prefs.edit().putString("saved_snippets", gson.toJson(snippets)).apply()
    }

    fun addToHistory(code: String) {
        val history = getHistory().toMutableList()
        history.remove(code) // Remove if already exists
        history.add(0, code) // Add to beginning
        
        // Trim history to max size
        while (history.size > maxHistorySize) {
            history.removeAt(history.size - 1)
        }
        
        prefs.edit().putString("code_history", gson.toJson(history)).apply()
    }

    fun getHistory(): List<String> {
        val json = prefs.getString("code_history", "[]")
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun copyToClipboard(code: String) {
        val clip = ClipData.newPlainText("code_snippet", code)
        clipboardManager.setPrimaryClip(clip)
    }

    fun getFromClipboard(): String? {
        val clip = clipboardManager.primaryClip
        return clip?.getItemAt(0)?.text?.toString()
    }

}
