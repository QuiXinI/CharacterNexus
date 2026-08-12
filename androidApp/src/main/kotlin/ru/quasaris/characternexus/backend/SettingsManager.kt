package ru.quasaris.characternexus.backend

import android.content.Context
import android.os.Build
import androidx.core.content.edit
import ru.quasaris.characternexus.model.AdvantageLogic
import ru.quasaris.characternexus.model.DiceRollPosition

enum class AppThemeMode {
    M3, OFF, CHARACTER
}

enum class ExportFormat {
    WEBP, PNG, JPG
}

class SettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    var themeMode: String
        get() = prefs.getString("theme_mode", AppThemeMode.M3.name) ?: AppThemeMode.M3.name
        set(value) = prefs.edit { putString("theme_mode", value) }

    var exportFormat: ExportFormat
        get() = ExportFormat.valueOf(prefs.getString("export_format", ExportFormat.WEBP.name) ?: ExportFormat.WEBP.name)
        set(value) = prefs.edit { putString("export_format", value.name) }

    var exportDirectoryUri: String?
        get() = prefs.getString("export_directory_uri", null)
        set(value) = prefs.edit { putString("export_directory_uri", value) }

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

    var rollInterfaceAlpha: Float
        get() = prefs.getFloat("roll_interface_alpha", 1.0f)
        set(value) = prefs.edit { putFloat("roll_interface_alpha", value) }

    var masterBlurEnabled: Boolean
        get() = prefs.getBoolean("master_blur_enabled", true)
        set(value) = prefs.edit { putBoolean("master_blur_enabled", value) }

    var blurRolls: Boolean
        get() = prefs.getBoolean("blur_rolls", true)
        set(value) = prefs.edit { putBoolean("blur_rolls", value) }

    var blurFullscreen: Boolean
        get() = prefs.getBoolean("blur_fullscreen", true)
        set(value) = prefs.edit { putBoolean("blur_fullscreen", value) }

    var blurPopups: Boolean
        get() = prefs.getBoolean("blur_popups", true)
        set(value) = prefs.edit { putBoolean("blur_popups", value) }

    var blurCards: Boolean
        get() = prefs.getBoolean("blur_cards", true)
        set(value) = prefs.edit { putBoolean("blur_cards", value) }

    var blurDynamicFields: Boolean
        get() = prefs.getBoolean("blur_dynamic_fields", true)
        set(value) = prefs.edit { putBoolean("blur_dynamic_fields", value) }

    var rollPassThrough: Boolean
        get() = prefs.getBoolean("roll_pass_through", true)
        set(value) = prefs.edit { putBoolean("roll_pass_through", value) }

    var rollPosition: String
        get() = prefs.getString("roll_position", DiceRollPosition.BOTTOM_LEFT.name) ?: DiceRollPosition.BOTTOM_LEFT.name
        set(value) = prefs.edit { putString("roll_position", value) }

    var rollCloseButtonPosition: String
        get() = prefs.getString("roll_close_button_position", DiceRollPosition.TOP_RIGHT.name) ?: DiceRollPosition.TOP_RIGHT.name
        set(value) = prefs.edit { putString("roll_close_button_position", value) }

    private val forceBlurEnabledDefault: Boolean
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Build.VERSION.MEDIA_PERFORMANCE_CLASS >= 33
            } else false
        }

    var debugInfoEnabled: Boolean
        get() = prefs.getBoolean("debug_info_enabled", false)
        set(value) = prefs.edit { putBoolean("debug_info_enabled", value) }

    var deletionWarningEnabled: Boolean
        get() = prefs.getBoolean("deletion_warning_enabled", true)
        set(value) = prefs.edit { putBoolean("deletion_warning_enabled", value) }

    var fullscreenEditingOnly: Boolean
        get() = prefs.getBoolean("fullscreen_editing_only", false)
        set(value) = prefs.edit { putBoolean("fullscreen_editing_only", value) }

    var topMarginStep: Int
        get() = prefs.getInt("top_margin_step", 2)
        set(value) = prefs.edit { putInt("top_margin_step", value) }

    var customTopMargin: Int
        get() = prefs.getInt("custom_top_margin", 96)
        set(value) = prefs.edit { putInt("custom_top_margin", value) }

    var autoDownloadLssAvatar: Boolean
        get() = prefs.getBoolean("auto_download_lss_avatar", false)
        set(value) = prefs.edit { putBoolean("auto_download_lss_avatar", value) }

    var advantageLogic: AdvantageLogic
        get() = AdvantageLogic.valueOf(prefs.getString("advantage_logic", AdvantageLogic.TOTAL.name) ?: AdvantageLogic.TOTAL.name)
        set(value) = prefs.edit { putString("advantage_logic", value.name) }

    var lastModuleExportName: String
        get() = prefs.getString("last_module_export_name", "") ?: ""
        set(value) = prefs.edit { putString("last_module_export_name", value) }

    var lastModuleExportDescription: String
        get() = prefs.getString("last_module_export_description", "") ?: ""
        set(value) = prefs.edit { putString("last_module_export_description", value) }

    var lastModuleExportVersion: String
        get() = prefs.getString("last_module_export_version", "1.0.0") ?: "1.0.0"
        set(value) = prefs.edit { putString("last_module_export_version", value) }

    var lastModuleExportId: String
        get() = prefs.getString("last_module_export_id", "") ?: ""
        set(value) = prefs.edit { putString("last_module_export_id", value) }

    var useNewACInterface: Boolean
        get() = prefs.getBoolean("use_new_ac_interface", true)
        set(value) = prefs.edit { putBoolean("use_new_ac_interface", value) }

    var useNewInitInterface: Boolean
        get() = prefs.getBoolean("use_new_init_interface", true)
        set(value) = prefs.edit { putBoolean("use_new_init_interface", value) }

    var useNewCondInterface: Boolean
        get() = prefs.getBoolean("use_new_cond_interface", true)
        set(value) = prefs.edit { putBoolean("use_new_cond_interface", value) }

    var useNewSpeedInterface: Boolean
        get() = prefs.getBoolean("use_new_speed_interface", true)
        set(value) = prefs.edit { putBoolean("use_new_speed_interface", value) }

    var useOldAvatarStyle: Boolean
        get() = prefs.getBoolean("use_old_avatar_style", false)
        set(value) = prefs.edit { putBoolean("use_old_avatar_style", value) }

    var diceFabOffsetX: Float
        get() = prefs.getFloat("dice_fab_offset_x", -40f)
        set(value) = prefs.edit { putFloat("dice_fab_offset_x", value) }

    var diceFabOffsetY: Float
        get() = prefs.getFloat("dice_fab_offset_y", -40f)
        set(value) = prefs.edit { putFloat("dice_fab_offset_y", value) }

    var diceFabAlpha: Float
        get() = prefs.getFloat("dice_fab_alpha", 1.0f)
        set(value) = prefs.edit { putFloat("dice_fab_alpha", value) }

    var diceFabBlurEnabled: Boolean
        get() = prefs.getBoolean("dice_fab_blur_enabled", true)
        set(value) = prefs.edit { putBoolean("dice_fab_blur_enabled", value) }

    var diceFabEnabled: Boolean
        get() = prefs.getBoolean("dice_fab_enabled", true)
        set(value) = prefs.edit { putBoolean("dice_fab_enabled", value) }

    var renderDiceInOrder: Boolean
        get() = prefs.getBoolean("render_dice_in_order", true)
        set(value) = prefs.edit { putBoolean("render_dice_in_order", value) }

    var collapseActionsOnEdit: Boolean
        get() = prefs.getBoolean("collapse_actions_on_edit", true)
        set(value) = prefs.edit { putBoolean("collapse_actions_on_edit", value) }

    var collapseSpellsOnEdit: Boolean
        get() = prefs.getBoolean("collapse_spells_on_edit", true)
        set(value) = prefs.edit { putBoolean("collapse_spells_on_edit", value) }

    var collapseDynamicFieldsOnEdit: Boolean
        get() = prefs.getBoolean("collapse_dynamic_fields_on_edit", true)
        set(value) = prefs.edit { putBoolean("collapse_dynamic_fields_on_edit", value) }

    fun resetToDefaults() {
        prefs.edit { clear() }
    }
}
