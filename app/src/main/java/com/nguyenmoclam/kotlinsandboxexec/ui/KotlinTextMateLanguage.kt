package com.nguyenmoclam.kotlinsandboxexec.ui

import android.content.Context
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import org.eclipse.tm4e.core.registry.IThemeSource

class KotlinTextMateLanguage(private val context: Context) {
//    private val themeRegistry = ThemeRegistry.getInstance()
//
//    init {
//        loadDefaultThemes()
//    }
//
//    fun createLanguage(): TextMateLanguage {
//        return TextMateLanguage.create(
//            "source.kotlin",
//            true,
//            context.assets.open("textmate/kotlin/syntaxes/Kotlin.tmLanguage"),
//            context.assets.open("textmate/kotlin/language-configuration.json")
//        )
//    }
//
//    private fun loadDefaultThemes() {
//        val themes = arrayOf("darcula", "abyss", "quietlight", "solarized_dark")
//
//        themes.forEach { name ->
//            try {
//                val themeAssetsPath = "textmate/themes/$name.json"
//                val themeSource = IThemeSource.fromInputStream(
//                    context.assets.open(themeAssetsPath),
//                    themeAssetsPath,
//                    null
//                )
//                val theme = ThemeModel(themeSource, name).apply {
//                    isDark = name != "quietlight"
//                }
//                themeRegistry.loadTheme(theme)
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
//
//        // Set default theme
//        themeRegistry.setTheme("quietlight")
//    }
//
//    fun setThemeBasedOnDarkMode(isDarkMode: Boolean) {
//        val defaultTheme = if (isDarkMode) "darcula" else "quietlight"
//        themeRegistry.setTheme(defaultTheme)
//    }
}