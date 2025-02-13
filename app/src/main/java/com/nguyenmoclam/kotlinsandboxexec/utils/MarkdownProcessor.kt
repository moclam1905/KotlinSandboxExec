package com.nguyenmoclam.kotlinsandboxexec.utils

data class ProcessedResponse(
    val text: String,
    val codeSnippets: List<CodeSnippet> = emptyList()
)

data class CodeSnippet(
    val code: String,
    val language: String = "kotlin"
)

object MarkdownProcessor {
    private val CODE_BLOCK_REGEX = Regex("```(\\w*)\\r?\\n([\\s\\S]*?)```", RegexOption.MULTILINE)
    private val INLINE_CODE_REGEX = Regex("`([^`\\n]+?)`")
    private val BOLD_REGEX = Regex("\\*\\*([^*\\n]+?)\\*\\*")
    private val ITALIC_REGEX = Regex("\\*([^*\\n]+?)\\*")
    private val ESCAPE_REGEX = Regex("\\\\([`*\\[\\]\\\\])") 

    fun process(markdown: String): ProcessedResponse {
        if (markdown.isBlank()) {
            return ProcessedResponse("", emptyList())
        }

        val codeSnippets = mutableListOf<CodeSnippet>()
        
        // Handle escape sequences first
        var processedText = markdown.replace(ESCAPE_REGEX, "$1")
        
        // Extract code blocks
        processedText = processedText.replace(CODE_BLOCK_REGEX) { matchResult ->
            val language = matchResult.groupValues[1].takeIf { it.isNotBlank() } ?: "kotlin"
            val code = matchResult.groupValues[2].trim()
            if (code.isNotBlank()) {
                codeSnippets.add(CodeSnippet(code, language))
                "\n[CODE_BLOCK_${codeSnippets.size - 1}]\n"
            } else {
                ""
            }
        }

        // Process other markdown elements
        processedText = processedText
            .replace(INLINE_CODE_REGEX) { matchResult ->
                val code = matchResult.groupValues[1].trim()
                if (code.isNotBlank()) "「$code」" else ""
            }
            .replace(BOLD_REGEX) { matchResult ->
                val text = matchResult.groupValues[1].trim()
                if (text.isNotBlank()) "【$text】" else ""
            }
            .replace(ITALIC_REGEX) { matchResult ->
                val text = matchResult.groupValues[1].trim()
                if (text.isNotBlank()) "「$text」" else ""
            }
            .trim()

        return ProcessedResponse(processedText, codeSnippets)
    }
}