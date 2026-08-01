package ru.quasaris.characters.master.backend

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.quasaris.characters.master.SlotAlignment
import ru.quasaris.characters.master.SlotFillDirection
import kotlin.math.roundToInt

class SettingsViewModel(
    private val appScaleManager: AppScaleManager,
    private val settingsManager: SettingsManager? = null
) : ViewModel() {

    private val _rollHistorySize = MutableStateFlow(settingsManager?.rollHistorySize ?: 5)
    val rollHistorySize = _rollHistorySize.asStateFlow()

    private val _customRollHistorySize = MutableStateFlow(settingsManager?.customRollHistorySize ?: 10)
    val customRollHistorySize = _customRollHistorySize.asStateFlow()

    private val _blurRolls = MutableStateFlow(settingsManager?.blurRolls ?: true)
    val blurRolls = _blurRolls.asStateFlow()

    private val _blurFullscreen = MutableStateFlow(settingsManager?.blurFullscreen ?: false)
    val blurFullscreen = _blurFullscreen.asStateFlow()

    private val _blurPopups = MutableStateFlow(settingsManager?.blurPopups ?: true)
    val blurPopups = _blurPopups.asStateFlow()

    private val _masterBlurEnabled = MutableStateFlow(settingsManager?.masterBlurEnabled ?: true)
    val masterBlurEnabled = _masterBlurEnabled.asStateFlow()

    private val _rollInterfaceAlpha = MutableStateFlow(settingsManager?.rollInterfaceAlpha ?: 1.0f)
    val rollInterfaceAlpha = _rollInterfaceAlpha.asStateFlow()

    private val _rollPassThrough = MutableStateFlow(settingsManager?.rollPassThrough ?: true)
    val rollPassThrough = _rollPassThrough.asStateFlow()

    private val _rollPosition = MutableStateFlow(DiceRollPosition.valueOf(settingsManager?.rollPosition ?: "BOTTOM_LEFT"))
    val rollPosition = _rollPosition.asStateFlow()

    private val _rollCloseButtonPosition = MutableStateFlow(DiceRollPosition.valueOf(settingsManager?.rollCloseButtonPosition ?: "TOP_RIGHT"))
    val rollCloseButtonPosition = _rollCloseButtonPosition.asStateFlow()

    private val _debugInfoEnabled = MutableStateFlow(settingsManager?.debugInfoEnabled ?: false)
    val debugInfoEnabled = _debugInfoEnabled.asStateFlow()

    private val _deletionWarningEnabled = MutableStateFlow(settingsManager?.deletionWarningEnabled ?: true)
    val deletionWarningEnabled = _deletionWarningEnabled.asStateFlow()

    private val _fullscreenEditingOnly = MutableStateFlow(settingsManager?.fullscreenEditingOnly ?: false)
    val fullscreenEditingOnly = _fullscreenEditingOnly.asStateFlow()

    private val _topMarginStep = MutableStateFlow(settingsManager?.topMarginStep ?: 2)
    val topMarginStep = _topMarginStep.asStateFlow()

    private val _customTopMargin = MutableStateFlow(settingsManager?.customTopMargin ?: 96)
    val customTopMargin = _customTopMargin.asStateFlow()

    private val _autoDownloadLssAvatar = MutableStateFlow(settingsManager?.autoDownloadLssAvatar ?: false)
    val autoDownloadLssAvatar = _autoDownloadLssAvatar.asStateFlow()

    private val _advantageLogic = MutableStateFlow(settingsManager?.advantageLogic ?: AdvantageLogic.TOTAL)
    val advantageLogic = _advantageLogic.asStateFlow()

    private val _exportFormat = MutableStateFlow(settingsManager?.exportFormat ?: ExportFormat.WEBP)
    val exportFormat = _exportFormat.asStateFlow()

    private val _exportDirectoryUri = MutableStateFlow(settingsManager?.exportDirectoryUri)
    val exportDirectoryUri = _exportDirectoryUri.asStateFlow()

    private val _longRestAlignment = MutableStateFlow(SlotAlignment.valueOf(settingsManager?.longRestAlignment ?: "RIGHT"))
    val longRestAlignment = _longRestAlignment.asStateFlow()

    private val _longRestFillDirection = MutableStateFlow(SlotFillDirection.valueOf(settingsManager?.longRestFillDirection ?: "LTR"))
    val longRestFillDirection = _longRestFillDirection.asStateFlow()

    private val _shortRestAlignment = MutableStateFlow(SlotAlignment.valueOf(settingsManager?.shortRestAlignment ?: "RIGHT"))
    val shortRestAlignment = _shortRestAlignment.asStateFlow()

    private val _shortRestFillDirection = MutableStateFlow(SlotFillDirection.valueOf(settingsManager?.shortRestFillDirection ?: "LTR"))
    val shortRestFillDirection = _shortRestFillDirection.asStateFlow()

    private val _useNewACInterface = MutableStateFlow(settingsManager?.useNewACInterface ?: true)
    val useNewACInterface = _useNewACInterface.asStateFlow()

    private val _useNewInitInterface = MutableStateFlow(settingsManager?.useNewInitInterface ?: true)
    val useNewInitInterface = _useNewInitInterface.asStateFlow()

    private val _useNewCondInterface = MutableStateFlow(settingsManager?.useNewCondInterface ?: true)
    val useNewCondInterface = _useNewCondInterface.asStateFlow()

    private val _useNewSpeedInterface = MutableStateFlow(settingsManager?.useNewSpeedInterface ?: true)
    val useNewSpeedInterface = _useNewSpeedInterface.asStateFlow()

    fun updateRollHistorySize(size: Int) {
        _rollHistorySize.value = size
        settingsManager?.rollHistorySize = size
    }

    fun updateCustomRollHistorySize(size: Int) {
        _customRollHistorySize.value = size
        settingsManager?.customRollHistorySize = size
    }

    fun updateBlurRolls(enabled: Boolean) {
        _blurRolls.value = enabled
        settingsManager?.blurRolls = enabled
    }

    fun updateBlurFullscreen(enabled: Boolean) {
        _blurFullscreen.value = enabled
        settingsManager?.blurFullscreen = enabled
    }

    fun updateBlurPopups(enabled: Boolean) {
        _blurPopups.value = enabled
        settingsManager?.blurPopups = enabled
    }

    fun updateMasterBlurEnabled(enabled: Boolean) {
        _masterBlurEnabled.value = enabled
        settingsManager?.masterBlurEnabled = enabled
    }

    fun updateRollInterfaceAlpha(alpha: Float) {
        _rollInterfaceAlpha.value = alpha
        settingsManager?.rollInterfaceAlpha = alpha
    }

    fun updateRollPassThrough(enabled: Boolean) {
        _rollPassThrough.value = enabled
        settingsManager?.rollPassThrough = enabled
    }

    fun updateRollPosition(position: DiceRollPosition) {
        _rollPosition.value = position
        settingsManager?.rollPosition = position.name
    }

    fun updateRollCloseButtonPosition(position: DiceRollPosition) {
        _rollCloseButtonPosition.value = position
        settingsManager?.rollCloseButtonPosition = position.name
    }

    @Deprecated("Use specific blur settings")
    fun updateForceBlurEnabled(enabled: Boolean) {
        updateBlurRolls(enabled)
        updateBlurFullscreen(enabled)
        updateBlurPopups(enabled)
        settingsManager?.forceBlurEnabled = enabled
    }

    fun updateDebugInfoEnabled(enabled: Boolean) {
        _debugInfoEnabled.value = enabled
        settingsManager?.debugInfoEnabled = enabled
    }

    fun updateDeletionWarningEnabled(enabled: Boolean) {
        _deletionWarningEnabled.value = enabled
        settingsManager?.deletionWarningEnabled = enabled
    }

    fun updateFullscreenEditingOnly(enabled: Boolean) {
        _fullscreenEditingOnly.value = enabled
        settingsManager?.fullscreenEditingOnly = enabled
    }

    fun updateTopMarginStep(step: Int) {
        _topMarginStep.value = step
        settingsManager?.topMarginStep = step
    }

    fun updateCustomTopMargin(margin: Int) {
        _customTopMargin.value = margin
        settingsManager?.customTopMargin = margin
    }

    fun updateAutoDownloadLssAvatar(enabled: Boolean) {
        _autoDownloadLssAvatar.value = enabled
        settingsManager?.autoDownloadLssAvatar = enabled
    }

    fun updateAdvantageLogic(logic: AdvantageLogic) {
        _advantageLogic.value = logic
        settingsManager?.advantageLogic = logic
    }

    fun updateExportFormat(format: ExportFormat) {
        _exportFormat.value = format
        settingsManager?.exportFormat = format
    }

    fun updateExportDirectoryUri(uri: String?) {
        _exportDirectoryUri.value = uri
        settingsManager?.exportDirectoryUri = uri
    }

    fun updateLongRestAlignment(alignment: SlotAlignment) {
        _longRestAlignment.value = alignment
        settingsManager?.longRestAlignment = alignment.name
    }

    fun updateLongRestFillDirection(direction: SlotFillDirection) {
        _longRestFillDirection.value = direction
        settingsManager?.longRestFillDirection = direction.name
    }

    fun updateShortRestAlignment(alignment: SlotAlignment) {
        _shortRestAlignment.value = alignment
        settingsManager?.shortRestAlignment = alignment.name
    }

    fun updateShortRestFillDirection(direction: SlotFillDirection) {
        _shortRestFillDirection.value = direction
        settingsManager?.shortRestFillDirection = direction.name
    }

    fun updateUseNewACInterface(enabled: Boolean) {
        _useNewACInterface.value = enabled
        settingsManager?.useNewACInterface = enabled
    }

    fun updateUseNewInitInterface(enabled: Boolean) {
        _useNewInitInterface.value = enabled
        settingsManager?.useNewInitInterface = enabled
    }

    fun updateUseNewCondInterface(enabled: Boolean) {
        _useNewCondInterface.value = enabled
        settingsManager?.useNewCondInterface = enabled
    }

    fun updateUseNewSpeedInterface(enabled: Boolean) {
        _useNewSpeedInterface.value = enabled
        settingsManager?.useNewSpeedInterface = enabled
    }

    val performanceClass: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.VERSION.MEDIA_PERFORMANCE_CLASS
        } else {
            0
        }

    val scaleFactor: StateFlow<Float> = appScaleManager.scaleFactor
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 1.0f
        )

    fun updateScaleFactor(newScale: Float) {
        viewModelScope.launch {
            // Round to 1 decimal place (e.g., 1.1) to ensure strict 10% steps
            val roundedScale = (newScale * 10f).roundToInt() / 10f
            appScaleManager.setScaleFactor(roundedScale)
        }
    }

    fun resetToDefaults() {
        settingsManager?.resetToDefaults()
        viewModelScope.launch {
            appScaleManager.resetToDefaults()
        }
        
        // Update all flows to reflect the reset state
        _rollHistorySize.value = settingsManager?.rollHistorySize ?: 5
        _customRollHistorySize.value = settingsManager?.customRollHistorySize ?: 10
        _blurRolls.value = settingsManager?.blurRolls ?: true
        _blurFullscreen.value = settingsManager?.blurFullscreen ?: false
        _blurPopups.value = settingsManager?.blurPopups ?: true
        _masterBlurEnabled.value = settingsManager?.masterBlurEnabled ?: true
        _rollInterfaceAlpha.value = settingsManager?.rollInterfaceAlpha ?: 1.0f
        _rollPassThrough.value = settingsManager?.rollPassThrough ?: true
        _rollPosition.value = DiceRollPosition.valueOf(settingsManager?.rollPosition ?: "BOTTOM_LEFT")
        _rollCloseButtonPosition.value = DiceRollPosition.valueOf(settingsManager?.rollCloseButtonPosition ?: "TOP_RIGHT")
        _debugInfoEnabled.value = settingsManager?.debugInfoEnabled ?: false
        _deletionWarningEnabled.value = settingsManager?.deletionWarningEnabled ?: true
        _fullscreenEditingOnly.value = settingsManager?.fullscreenEditingOnly ?: false
        _topMarginStep.value = settingsManager?.topMarginStep ?: 2
        _customTopMargin.value = settingsManager?.customTopMargin ?: 96
        _autoDownloadLssAvatar.value = settingsManager?.autoDownloadLssAvatar ?: false
        _advantageLogic.value = settingsManager?.advantageLogic ?: AdvantageLogic.TOTAL
        _exportFormat.value = settingsManager?.exportFormat ?: ExportFormat.WEBP
        _exportDirectoryUri.value = settingsManager?.exportDirectoryUri
        _longRestAlignment.value = SlotAlignment.valueOf(settingsManager?.longRestAlignment ?: "RIGHT")
        _longRestFillDirection.value = SlotFillDirection.valueOf(settingsManager?.longRestFillDirection ?: "LTR")
        _shortRestAlignment.value = SlotAlignment.valueOf(settingsManager?.shortRestAlignment ?: "RIGHT")
        _shortRestFillDirection.value = SlotFillDirection.valueOf(settingsManager?.shortRestFillDirection ?: "LTR")
        _useNewACInterface.value = settingsManager?.useNewACInterface ?: true
        _useNewInitInterface.value = settingsManager?.useNewInitInterface ?: true
        _useNewCondInterface.value = settingsManager?.useNewCondInterface ?: true
        _useNewSpeedInterface.value = settingsManager?.useNewSpeedInterface ?: true
    }
}
