package com.nguyenmoclam.kotlinsandboxexec

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import io.github.rosemoe.sora.widget.CodeEditor

class CodeKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var editor: CodeEditor? = null
    private val commonSymbols = listOf(
        "{", "}", "(", ")", "[", "]",
        "=", ".", ",", ";", ":", "->",
        "\"\"" // Double quotes
    )

    init {
        orientation = VERTICAL
        setupSymbolRow()
    }

    private fun setupSymbolRow() {
        val scrollView = android.widget.HorizontalScrollView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        val symbolRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }

        commonSymbols.forEach { symbol ->
            android.widget.Button(context).apply {
                text = symbol
                setOnClickListener { insertText(symbol) }
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                setPadding(16, 8, 16, 8)
            }.also { symbolRow.addView(it) }
        }

        scrollView.addView(symbolRow)
        addView(scrollView)
    }

    fun attachToEditor(codeEditor: CodeEditor) {
        editor = codeEditor
    }

    private fun insertText(text: String) {
        editor?.let { editor ->
            try {
                val cursor = editor.cursor ?: return
                val content = editor.text
                content.insert(cursor.leftLine, cursor.leftColumn, text)
                if (text == "\"\"") {
                    cursor.setLeft(cursor.leftLine, cursor.leftColumn + 1)
                } else {
                    cursor.setLeft(cursor.leftLine, cursor.leftColumn + text.length)
                }
                editor.notifyIMEExternalCursorChange()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}