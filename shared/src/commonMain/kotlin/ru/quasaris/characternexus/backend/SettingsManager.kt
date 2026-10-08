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
    var interfaceMode: AppInterfaceMode = AppInterfaceMode.AUTO,
    var m3SeedColor: String = "#6750A4",
    var customThemeSeed: String = "#6750A4",
    var customUseFlexibleColors: Boolean = false,
    var customPrimary: String = "#6750A4",
    var customOnPrimary: String = "#FFFFFF",
    var customPrimaryContainer: String = "#EADDFF",
    var customOnPrimaryContainer: String = "#21005D",
    var customSecondary: String = "#625B71",
    var customOnSecondary: String = "#FFFFFF",
    var customSecondaryContainer: String = "#E8DEF8",
    var customOnSecondaryContainer: String = "#1D192B",
    var customTertiary: String = "#7D5260",
    var customOnTertiary: String = "#FFFFFF",
    var customTertiaryContainer: String = "#FFD8E4",
    var customOnTertiaryContainer: String = "#31111D",
    var customBackground: String = "#FFFBFE",
    var customOnBackground: String = "#1C1B1F",
    var customSurface: String = "#FFFBFE",
    var customOnSurface: String = "#1C1B1F",
    var customSurfaceVariant: String = "#E7E0EC",
    var customOnSurfaceVariant: String = "#49454F",
    var customOutline: String = "#79747E",
    var exportFormat: ExportFormat = ExportFormat.WEBP,
    var exportDirectoryUri: String? = null,
    var lastCharacterUuid: String? = null,
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
    var isPremium: Boolean = false,
    var desktopLeftColumnWidth: Float = 450f,
    var lastCrashLog: String? = null
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
    var interfaceMode: AppInterfaceMode get() = settings.interfaceMode; set(value) { settings.interfaceMode = value; save() }
    var desktopLeftColumnWidth: Float get() = settings.desktopLeftColumnWidth; set(value) { settings.desktopLeftColumnWidth = value; save() }
    var m3SeedColor: String get() = settings.m3SeedColor; set(value) { settings.m3SeedColor = value; save() }
    var customThemeSeed: String get() = settings.customThemeSeed; set(value) { settings.customThemeSeed = value; save() }
    var customUseFlexibleColors: Boolean get() = settings.customUseFlexibleColors; set(value) { settings.customUseFlexibleColors = value; save() }
    var customPrimary: String get() = settings.customPrimary; set(value) { settings.customPrimary = value; save() }
    var customOnPrimary: String get() = settings.customOnPrimary; set(value) { settings.customOnPrimary = value; save() }
    var customPrimaryContainer: String get() = settings.customPrimaryContainer; set(value) { settings.customPrimaryContainer = value; save() }
    var customOnPrimaryContainer: String get() = settings.customOnPrimaryContainer; set(value) { settings.customOnPrimaryContainer = value; save() }
    var customSecondary: String get() = settings.customSecondary; set(value) { settings.customSecondary = value; save() }
    var customOnSecondary: String get() = settings.customOnSecondary; set(value) { settings.customOnSecondary = value; save() }
    var customSecondaryContainer: String get() = settings.customSecondaryContainer; set(value) { settings.customSecondaryContainer = value; save() }
    var customOnSecondaryContainer: String get() = settings.customOnSecondaryContainer; set(value) { settings.customOnSecondaryContainer = value; save() }
    var customTertiary: String get() = settings.customTertiary; set(value) { settings.customTertiary = value; save() }
    var customOnTertiary: String get() = settings.customOnTertiary; set(value) { settings.customOnTertiary = value; save() }
    var customTertiaryContainer: String get() = settings.customTertiaryContainer; set(value) { settings.customTertiaryContainer = value; save() }
    var customOnTertiaryContainer: String get() = settings.customOnTertiaryContainer; set(value) { settings.customOnTertiaryContainer = value; save() }
    var customBackground: String get() = settings.customBackground; set(value) { settings.customBackground = value; save() }
    var customOnBackground: String get() = settings.customOnBackground; set(value) { settings.customOnBackground = value; save() }
    var customSurface: String get() = settings.customSurface; set(value) { settings.customSurface = value; save() }
    var customOnSurface: String get() = settings.customOnSurface; set(value) { settings.customOnSurface = value; save() }
    var customSurfaceVariant: String get() = settings.customSurfaceVariant; set(value) { settings.customSurfaceVariant = value; save() }
    var customOnSurfaceVariant: String get() = settings.customOnSurfaceVariant; set(value) { settings.customOnSurfaceVariant = value; save() }
    var customOutline: String get() = settings.customOutline; set(value) { settings.customOutline = value; save() }
    var exportFormat: ExportFormat get() = settings.exportFormat; set(value) { settings.exportFormat = value; save() }
    var lastCharacterUuid: String? get() = settings.lastCharacterUuid; set(value) { settings.lastCharacterUuid = value; save() }
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
    var lastCrashLog: String? get() = settings.lastCrashLog; set(value) { settings.lastCrashLog = value; save() }

    fun resetToDefaults() {
        settings = AppSettings()
        save()
    }
}
