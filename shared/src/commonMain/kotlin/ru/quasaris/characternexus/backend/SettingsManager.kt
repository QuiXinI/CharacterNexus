package ru.quasaris.characternexus.backend

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.quasaris.characternexus.getAppDataDir
import ru.quasaris.characternexus.platformFileSystem
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.util.log

@Serializable
data class AppSettings(
    var themeMode: AppThemeMode = AppThemeMode.M3,
    var themeBehavior: AppThemeBehavior = AppThemeBehavior.SYSTEM,
    var m3SeedColor: String = "#6750A4",
    var exportFormat: ExportFormat = ExportFormat.WEBP,
    var exportDirectoryUri: String? = null,
    var lastCharacterId: Int = -1,
    var lastCharacterSeedColor: Int? = null,
    var rollHistorySize: Int = 5,
    var customRollHistorySize: Int = 10,
    var rollInterfaceAlpha: Float = 1.0f,
    var masterBlurEnabled: Boolean = true,
    var blurRolls: Boolean = true,
    var blurFullscreen: Boolean = true,
    var blurPopups: Boolean = true,
    var blurCards: Boolean = true,
    var blurDynamicFields: Boolean = true,
    var rollPassThrough: Boolean = true,
    var rollPosition: String = DiceRollPosition.BOTTOM_LEFT.name,
    var rollCloseButtonPosition: String = DiceRollPosition.TOP_RIGHT.name,
    var debugInfoEnabled: Boolean = false,
    var deletionWarningEnabled: Boolean = true,
    var fullscreenEditingOnly: Boolean = false,
    var topMarginStep: Int = 2,
    var customTopMargin: Int = 96,
    var autoDownloadLssAvatar: Boolean = false,
    var scaleFactor: Float = 1.0f,
    var advantageLogic: AdvantageLogic = AdvantageLogic.TOTAL,
    var longRestAlignment: SlotAlignment = SlotAlignment.RIGHT,
    var longRestFillDirection: SlotFillDirection = SlotFillDirection.LTR,
    var shortRestAlignment: SlotAlignment = SlotAlignment.RIGHT,
    var shortRestFillDirection: SlotFillDirection = SlotFillDirection.LTR,
    var dawnRestAlignment: SlotAlignment = SlotAlignment.RIGHT,
    var dawnRestFillDirection: SlotFillDirection = SlotFillDirection.LTR,
    var lastModuleExportName: String = "",
    var lastModuleExportDescription: String = "",
    var lastModuleExportVersion: String = "1.0.0",
    var lastModuleExportId: String = "",
    var useNewACInterface: Boolean = true,
    var useNewInitInterface: Boolean = true,
    var useNewCondInterface: Boolean = true,
    var useNewSpeedInterface: Boolean = true,
    var blurRadius: Int = 12,
    var customBlurRadius: Int = 12,
    var useOldAvatarStyle: Boolean = false,
    var diceFabOffsetX: Float = -40f,
    var diceFabOffsetY: Float = -40f,
    var diceFabAlpha: Float = 1.0f,
    var diceFabBlurEnabled: Boolean = true,
    var diceFabEnabled: Boolean = true,
    var renderDiceInOrder: Boolean = true,
    var collapseActionsOnEdit: Boolean = true,
    var collapseSpellsOnEdit: Boolean = true,
    var collapseDynamicFieldsOnEdit: Boolean = true,
    var veryResponsiveHaptics: Boolean = true,
    var isPremium: Boolean = false
)

class SettingsManager {
    private val settingsFile = getAppDataDir().div("settings.json")
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    var settings: AppSettings = loadSettings()
        private set

    private fun loadSettings(): AppSettings {
        return try {
            if (platformFileSystem.exists(settingsFile)) {
                platformFileSystem.read(settingsFile) {
                    json.decodeFromString<AppSettings>(readUtf8())
                }
            } else {
                AppSettings()
            }
        } catch (e: Exception) {
            e.log()
            AppSettings()
        }
    }

    fun save() {
        try {
            platformFileSystem.write(settingsFile) {
                writeUtf8(json.encodeToString(settings))
            }
        } catch (e: Exception) {
            e.log()
        }
    }

    // Facade properties for backward compatibility and simpler access
    var themeMode: AppThemeMode get() = settings.themeMode; set(value) { settings.themeMode = value; save() }
    var themeBehavior: AppThemeBehavior get() = settings.themeBehavior; set(value) { settings.themeBehavior = value; save() }
    var m3SeedColor: String get() = settings.m3SeedColor; set(value) { settings.m3SeedColor = value; save() }
    var exportFormat: ExportFormat get() = settings.exportFormat; set(value) { settings.exportFormat = value; save() }
    var lastCharacterId: Int get() = settings.lastCharacterId; set(value) { settings.lastCharacterId = value; save() }
    var lastCharacterSeedColor: Int? get() = settings.lastCharacterSeedColor; set(value) { settings.lastCharacterSeedColor = value; save() }
    var rollHistorySize: Int get() = settings.rollHistorySize; set(value) { settings.rollHistorySize = value; save() }
    var customRollHistorySize: Int get() = settings.customRollHistorySize; set(value) { settings.customRollHistorySize = value; save() }
    var rollInterfaceAlpha: Float get() = settings.rollInterfaceAlpha; set(value) { settings.rollInterfaceAlpha = value; save() }
    var masterBlurEnabled: Boolean get() = settings.masterBlurEnabled; set(value) { settings.masterBlurEnabled = value; save() }
    var blurRolls: Boolean get() = settings.blurRolls; set(value) { settings.blurRolls = value; save() }
    var blurFullscreen: Boolean get() = settings.blurFullscreen; set(value) { settings.blurFullscreen = value; save() }
    var blurPopups: Boolean get() = settings.blurPopups; set(value) { settings.blurPopups = value; save() }
    var rollPassThrough: Boolean get() = settings.rollPassThrough; set(value) { settings.rollPassThrough = value; save() }
    var rollPosition: String get() = settings.rollPosition; set(value) { settings.rollPosition = value; save() }
    var rollCloseButtonPosition: String get() = settings.rollCloseButtonPosition; set(value) { settings.rollCloseButtonPosition = value; save() }
    var debugInfoEnabled: Boolean get() = settings.debugInfoEnabled; set(value) { settings.debugInfoEnabled = value; save() }
    var deletionWarningEnabled: Boolean get() = settings.deletionWarningEnabled; set(value) { settings.deletionWarningEnabled = value; save() }
    var scaleFactor: Float get() = settings.scaleFactor; set(value) { settings.scaleFactor = value; save() }
    var advantageLogic: AdvantageLogic get() = settings.advantageLogic; set(value) { settings.advantageLogic = value; save() }
    var useNewACInterface: Boolean get() = settings.useNewACInterface; set(value) { settings.useNewACInterface = value; save() }
    var useNewInitInterface: Boolean get() = settings.useNewInitInterface; set(value) { settings.useNewInitInterface = value; save() }
    var useNewCondInterface: Boolean get() = settings.useNewCondInterface; set(value) { settings.useNewCondInterface = value; save() }
    var useNewSpeedInterface: Boolean get() = settings.useNewSpeedInterface; set(value) { settings.useNewSpeedInterface = value; save() }
    var diceFabOffsetX: Float get() = settings.diceFabOffsetX; set(value) { settings.diceFabOffsetX = value; save() }
    var diceFabOffsetY: Float get() = settings.diceFabOffsetY; set(value) { settings.diceFabOffsetY = value; save() }
    var diceFabAlpha: Float get() = settings.diceFabAlpha; set(value) { settings.diceFabAlpha = value; save() }
    var diceFabBlurEnabled: Boolean get() = settings.diceFabBlurEnabled; set(value) { settings.diceFabBlurEnabled = value; save() }
    var diceFabEnabled: Boolean get() = settings.diceFabEnabled; set(value) { settings.diceFabEnabled = value; save() }
    var blurRadius: Int get() = settings.blurRadius; set(value) { settings.blurRadius = value; save() }
    var customBlurRadius: Int get() = settings.customBlurRadius; set(value) { settings.customBlurRadius = value; save() }
    var veryResponsiveHaptics: Boolean get() = settings.veryResponsiveHaptics; set(value) { settings.veryResponsiveHaptics = value; save() }
    var isPremium: Boolean get() = settings.isPremium; set(value) { settings.isPremium = value; save() }

    fun resetToDefaults() {
        settings = AppSettings()
        save()
    }
}
