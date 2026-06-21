package ru.quasaris.characters.master

import android.content.Context
import androidx.core.content.edit

class SettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    var isModernLayout: Boolean
        get() = prefs.getBoolean("is_modern_layout", true)
        set(value) = prefs.edit { putBoolean("is_modern_layout", value) }
}
