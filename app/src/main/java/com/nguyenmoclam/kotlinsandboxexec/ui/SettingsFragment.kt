package com.nguyenmoclam.kotlinsandboxexec.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.nguyenmoclam.kotlinsandboxexec.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        // Apply saved dark mode setting on startup
        val prefs = requireContext().getSharedPreferences("kotlin_sandbox_settings", android.content.Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSettings()
    }

    private fun setupSettings() {
        // Load current settings
        val prefs = requireContext().getSharedPreferences("kotlin_sandbox_settings", android.content.Context.MODE_PRIVATE)
        
        // Initialize UI components with current values
        binding.apply {
            timeoutSeekBar.progress = prefs.getInt("execution_timeout", 10)
            memorySeekBar.progress = prefs.getInt("memory_limit", 50)
            hapticSwitch.isChecked = prefs.getBoolean("haptic_feedback", true)
            soundSwitch.isChecked = prefs.getBoolean("sound_feedback", false)
            customKeyboardSwitch.isChecked = prefs.getBoolean("custom_keyboard", true)
            autoIndentSwitch.isChecked = prefs.getBoolean("auto_indent", true)
            historySeekBar.progress = prefs.getInt("history_size", 10)
            fontSizeSeekBar.progress = prefs.getInt("font_size", 14)
            bracketCompletionSwitch.isChecked = prefs.getBoolean("bracket_completion", true)
            syntaxHighlightSwitch.isChecked = prefs.getBoolean("syntax_highlight", true)
            darkModeSwitch.isChecked = prefs.getBoolean("dark_mode", false)

            // Setup font family spinner
            val fonts = arrayOf("Roboto Mono", "JetBrains Mono", "Fira Code", "Source Code Pro")
            val fontAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, fonts)
            fontAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            fontFamilySpinner.adapter = fontAdapter
            fontFamilySpinner.setSelection(fonts.indexOf(prefs.getString("font_family", "Roboto Mono")))

            // Setup seekbar labels
            timeoutValue.text = "${timeoutSeekBar.progress} seconds"
            memoryValue.text = "${memorySeekBar.progress}%"
            historyValue.text = "${historySeekBar.progress} items"
            fontSizeValue.text = "${fontSizeSeekBar.progress}sp"

            // Setup listeners
            fontSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    fontSizeValue.text = "${progress}sp"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            // Setup dark mode switch listener
            darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
                AppCompatDelegate.setDefaultNightMode(
                    if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                    else AppCompatDelegate.MODE_NIGHT_NO
                )
            }

            timeoutSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    timeoutValue.text = "$progress seconds"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            memorySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    memoryValue.text = "$progress%"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            historySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    historyValue.text = "$progress items"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            // Save button
            saveButton.setOnClickListener {
                prefs.edit().apply {
                    putInt("execution_timeout", timeoutSeekBar.progress)
                    putInt("memory_limit", memorySeekBar.progress)
                    putBoolean("haptic_feedback", hapticSwitch.isChecked)
                    putBoolean("sound_feedback", soundSwitch.isChecked)
                    putBoolean("custom_keyboard", customKeyboardSwitch.isChecked)
                    putBoolean("auto_indent", autoIndentSwitch.isChecked)
                    putInt("history_size", historySeekBar.progress)
                    putInt("font_size", fontSizeSeekBar.progress)
                    putString("font_family", fontFamilySpinner.selectedItem.toString())
                    putBoolean("bracket_completion", bracketCompletionSwitch.isChecked)
                    putBoolean("syntax_highlight", syntaxHighlightSwitch.isChecked)
                    putBoolean("dark_mode", darkModeSwitch.isChecked)
                }.apply()
                requireActivity().supportFragmentManager.popBackStack()
            }

            // Cancel button
            cancelButton.setOnClickListener {
                requireActivity().supportFragmentManager.popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = SettingsFragment()
    }
}