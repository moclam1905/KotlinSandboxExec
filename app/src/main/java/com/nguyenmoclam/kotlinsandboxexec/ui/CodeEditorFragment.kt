package com.nguyenmoclam.kotlinsandboxexec.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.nguyenmoclam.kotlinsandboxexec.CodeKeyboardView
import com.nguyenmoclam.kotlinsandboxexec.LogActivity
import com.nguyenmoclam.kotlinsandboxexec.MainActivity
import com.nguyenmoclam.kotlinsandboxexec.R
import com.nguyenmoclam.kotlinsandboxexec.SnippetManager
import com.nguyenmoclam.kotlinsandboxexec.databinding.FragmentCodeEditorBinding
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.dsl.languages
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.tm4e.core.registry.IThemeSource

class CodeEditorFragment : Fragment() {
    private var _binding: FragmentCodeEditorBinding? = null
    private val binding get() = _binding!!
    private lateinit var snippetManager: SnippetManager

    // Settings parameters
    private var maxExecutionTimeMs = 10000L
    private var maxMemoryPercentage = 50
    private var enableHapticFeedback = true
    private var enableSoundFeedback = false
    private var useCustomKeyboard = true
    private var autoIndentEnabled = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCodeEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        snippetManager = SnippetManager(requireContext())

        // Restore editor content if available
        savedInstanceState?.getString("editor_content")?.let { content ->
            binding.codeEditor.setText(content)
        } ?: run {
            binding.codeEditor.setText(DEFAULT_CODE)
        }

        setupCodeEditor()
        setupRunButton()
        setupSnippetManagement()
    }

    private fun setupCodeEditor() {
        // Load settings
        val prefs = requireContext().getSharedPreferences(
            "kotlin_sandbox_settings",
            android.content.Context.MODE_PRIVATE
        )
        useCustomKeyboard = prefs.getBoolean("custom_keyboard", true)
        autoIndentEnabled = prefs.getBoolean("auto_indent", true)
        maxExecutionTimeMs = prefs.getInt("execution_timeout", 10) * 1000L
        maxMemoryPercentage = prefs.getInt("memory_limit", 50)
        enableHapticFeedback = prefs.getBoolean("haptic_feedback", true)
        enableSoundFeedback = prefs.getBoolean("sound_feedback", false)

        binding.codeEditor.apply {
            // Other configurations
            isWordwrap = true
            props.stickyScroll = true
            setLineSpacing(2f, 1.1f)
            nonPrintablePaintingFlags =
                CodeEditor.FLAG_DRAW_WHITESPACE_LEADING or CodeEditor.FLAG_DRAW_LINE_SEPARATOR or CodeEditor.FLAG_DRAW_WHITESPACE_IN_SELECTION

            props.symbolPairAutoCompletion = prefs.getBoolean("bracket_completion", true)
            props.autoCompletionOnComposing = true
            setTextSize(prefs.getInt("font_size", 14).toFloat())

            val fontFamily = prefs.getString("font_family", "Roboto Mono")
            setTypefaceText(
                android.graphics.Typeface.create(
                    fontFamily,
                    android.graphics.Typeface.NORMAL
                )
            )

            if (prefs.getBoolean("syntax_highlight", true)) {
                colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
            }
        }
        // Load textmate themes and grammars
        setupTextmate()
        // Before using Textmate Language, TextmateColorScheme should be applied
        ensureTextmateTheme()
        // Set editor language to textmate Java
        val editor = binding.codeEditor
        val language = TextMateLanguage.create(
            "source.kotlin", true
        )
        editor.setEditorLanguage(language)

        if (useCustomKeyboard) {
            val keyboardView = CodeKeyboardView(requireContext())
            keyboardView.attachToEditor(binding.codeEditor)

            // Delay adding the window until the activity is ready
            view?.post {
                try {
                    val activity = activity
                    if (activity != null && !activity.isFinishing) {
                        val windowManager = activity.windowManager
                        val layoutParams = android.view.WindowManager.LayoutParams().apply {
                            width = android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                            type = android.view.WindowManager.LayoutParams.TYPE_APPLICATION_PANEL
                            flags = android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            format = android.graphics.PixelFormat.TRANSLUCENT
                            gravity = android.view.Gravity.BOTTOM
                            token = activity.window.decorView.windowToken
                            softInputMode = android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                        }
                        windowManager.addView(keyboardView, layoutParams)

                        // Set up keyboard visibility listener
                        binding.codeEditor.setOnClickListener {
                            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                            imm.showSoftInput(binding.codeEditor, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                            keyboardView.visibility = View.VISIBLE
                        }

                        // Monitor keyboard visibility changes
                        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
                            val rect = android.graphics.Rect()
                            binding.root.getWindowVisibleDisplayFrame(rect)
                            val screenHeight = binding.root.rootView.height
                            val keypadHeight = screenHeight - rect.bottom

                            if (keypadHeight > screenHeight * 0.15) {
                                keyboardView.visibility = View.VISIBLE
                                layoutParams.y = keypadHeight
                                try {
                                    windowManager.updateViewLayout(keyboardView, layoutParams)
                                } catch (e: IllegalArgumentException) {
                                    // View might have been removed
                                }
                            } else {
                                keyboardView.visibility = View.GONE
                            }
                        }

                        // Clean up the keyboard view when fragment is destroyed
                        view?.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                            override fun onViewAttachedToWindow(v: View) {}
                            override fun onViewDetachedFromWindow(v: View) {
                                try {
                                    windowManager.removeView(keyboardView)
                                } catch (e: IllegalArgumentException) {
                                    // View might already be removed
                                }
                            }
                        })
                    }
                } catch (e: Exception) {
                    // Handle any window token related exceptions
                    e.printStackTrace()
                }
            }
        }
    }

    private fun setupTextmate() {
        // Add assets file provider so that files in assets can be loaded
        FileProviderRegistry.getInstance().addFileProvider(
            AssetsFileResolver(
                requireContext().assets
            )
        )
        loadDefaultTextMateThemes()
        //loadDefaultTextMateLanguages()
        loadDefaultLanguagesWithDSL()
    }

    private fun loadDefaultTextMateThemes() {
        val themes = arrayOf("darcula", "abyss", "quietlight", "solarized_dark")
        val themeRegistry = ThemeRegistry.getInstance()
        themes.forEach { name ->
            val path = "textmate/themes/$name.json"
            themeRegistry.loadTheme(
                ThemeModel(
                    IThemeSource.fromInputStream(
                        FileProviderRegistry.getInstance().tryGetInputStream(path), path, null
                    ), name
                ).apply {
                    if (name != "quietlight") {
                        isDark = true
                    }
                }
            )
        }

        themeRegistry.setTheme("quietlight")
    }

    private fun loadDefaultTextMateLanguages() {
        GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
    }

    private fun loadDefaultLanguagesWithDSL() {
        GrammarRegistry.getInstance().loadGrammars(
            languages {
                language("kotlin") {
                    grammar = "textmate/kotlin/syntaxes/Kotlin.tmLanguage"
                    defaultScopeName()
                    languageConfiguration = "textmate/kotlin/language-configuration.json"
                }
            }
        )
    }

    private fun ensureTextmateTheme() {
        val editor = binding.codeEditor
        var editorColorScheme = editor.colorScheme
        if (editorColorScheme !is TextMateColorScheme) {
            editorColorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
            editor.colorScheme = editorColorScheme
        }
    }

    private fun setupRunButton() {
        binding.runButton.apply {
            setOnClickListener {
                if (enableHapticFeedback) {
                    performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                }
                if (enableSoundFeedback) {
                    playSoundEffect(android.view.SoundEffectConstants.CLICK)
                }
                executeCode()
            }
        }
    }

    private fun executeCode() {
        val code = binding.codeEditor.text.toString()
        binding.runButton.isEnabled = false

        snippetManager.addToHistory(code)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val result = (activity as? MainActivity)?.executeKotlinCode(code)
                    ?: "Error: Unable to execute code"
                withContext(Dispatchers.Main) {
                    val intent =
                        android.content.Intent(requireContext(), LogActivity::class.java).apply {
                            putExtra(LogActivity.EXTRA_CODE, code)
                            putExtra(LogActivity.EXTRA_LOG, result)
                        }
                    startActivity(intent)
                    binding.runButton.isEnabled = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val intent =
                        android.content.Intent(requireContext(), LogActivity::class.java).apply {
                            putExtra(LogActivity.EXTRA_CODE, code)
                            putExtra(LogActivity.EXTRA_LOG, "Error: ${e.message}")
                        }
                    startActivity(intent)
                    binding.runButton.isEnabled = true
                }
            }
        }
    }

    private fun setupSnippetManagement() {
        binding.copyButton.setOnClickListener {
            val code = binding.codeEditor.text.toString()
            snippetManager.copyToClipboard(code)
            showToast("Code copied to clipboard")
        }

        binding.pasteButton.setOnClickListener {
            snippetManager.getFromClipboard()?.let { code ->
                binding.codeEditor.setText(code)
            }
        }

        binding.saveSnippetButton.setOnClickListener {
            showSaveSnippetDialog()
        }

        binding.historyButton.setOnClickListener {
            showSnippetHistoryDialog()
        }
    }

    private fun showSaveSnippetDialog() {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Save Snippet")
            .setView(layoutInflater.inflate(R.layout.dialog_save_snippet, null).apply {
                findViewById<android.widget.EditText>(R.id.snippetNameInput).setText("")
            })
            .setPositiveButton("Save") { dialog, _ ->
                val view =
                    (dialog as android.app.AlertDialog).findViewById<android.view.View>(R.id.dialogRoot)
                val name =
                    view.findViewById<android.widget.EditText>(R.id.snippetNameInput).text.toString()
                val code = binding.codeEditor.text.toString()
                snippetManager.saveSnippet(name, code)
                showToast("Snippet saved")
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }

    private fun showSnippetHistoryDialog() {
        val history = snippetManager.getHistory()
        val savedSnippets = snippetManager.getSavedSnippets()

        val items = mutableListOf<String>()
        items.addAll(savedSnippets.keys)
        items.addAll(history)

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Snippets & History")
            .setItems(items.toTypedArray()) { _, which ->
                val selectedItem = items[which]
                val code = savedSnippets[selectedItem] ?: history[which - savedSnippets.size]
                binding.codeEditor.setText(code)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getNavigationBarHeight(): Int {
        val resources = requireContext().resources
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT)
            .show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("editor_content", binding.codeEditor.text.toString())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.codeEditor.release()
        _binding = null
    }

    companion object {
        private const val DEFAULT_CODE = """// Write your Kotlin code here
println("Hello, Kotlin Sandbox!")"""

        fun newInstance() = CodeEditorFragment()
    }

}