package ru.quasaris.characters.master.backend

import android.content.Context
import android.os.Build
import androidx.core.content.edit

enum class AppThemeMode {
    M3, OFF, CHARACTER
}

class SettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    var themeMode: AppThemeMode
        get() = AppThemeMode.valueOf(prefs.getString("theme_mode", AppThemeMode.M3.name) ?: AppThemeMode.M3.name)
        set(value) = prefs.edit { putString("theme_mode", value.name) }

    var lastCharacterId: Int
        get() = prefs.getInt("last_character_id", -1)
        set(value) = prefs.edit { putInt("last_character_id", value) }

    var lastCharacterSeedColor: Int?
        get() = if (prefs.contains("last_character_seed_color")) prefs.getInt("last_character_seed_color", 0) else null
        set(value) = prefs.edit {
            if (value != null) putInt("last_character_seed_color", value)
            else remove("last_character_seed_color")
        }

    var rollHistorySize: Int
        get() = prefs.getInt("roll_history_size", 5)
        set(value) = prefs.edit { putInt("roll_history_size", value) }

    var customRollHistorySize: Int
        get() = prefs.getInt("custom_roll_history_size", 10)
        set(value) = prefs.edit { putInt("custom_roll_history_size", value) }

    var forceBlurEnabled: Boolean
        get() {
            val default = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Build.VERSION.MEDIA_PERFORMANCE_CLASS >= 33
            } else false
            return prefs.getBoolean("force_blur_enabled", default)
        }
        set(value) = prefs.edit { putBoolean("force_blur_enabled", value) }

    var debugInfoEnabled: Boolean
        get() = prefs.getBoolean("debug_info_enabled", false)
        set(value) = prefs.edit { putBoolean("debug_info_enabled", value) }

    var deletionWarningEnabled: Boolean
        get() = prefs.getBoolean("deletion_warning_enabled", true)
        set(value) = prefs.edit { putBoolean("deletion_warning_enabled", value) }
}
