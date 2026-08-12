package ru.quasaris.characternexus.backend

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.quasaris.characternexus.model.DiceRollPosition
import ru.quasaris.characternexus.model.AdvantageLogic
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

    private val _blurFullscreen = MutableStateFlow(settingsManager?.blurFullscreen ?: true)
    val blurFullscreen = _blurFullscreen.asStateFlow()

    private val _blurPopups = MutableStateFlow(settingsManager?.blurPopups ?: true)
    val blurPopups = _blurPopups.asStateFlow()

    private val _blurCards = MutableStateFlow(settingsManager?.blurCards ?: true)
    val blurCards = _blurCards.asStateFlow()

    private val _blurDynamicFields = MutableStateFlow(settingsManager?.blurDynamicFields ?: true)
    val blurDynamicFields = _blurDynamicFields.asStateFlow()

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

    private val _useNewACInterface = MutableStateFlow(settingsManager?.useNewACInterface ?: true)
    val useNewACInterface = _useNewACInterface.asStateFlow()

    private val _useNewInitInterface = MutableStateFlow(settingsManager?.useNewInitInterface ?: true)
    val useNewInitInterface = _useNewInitInterface.asStateFlow()

    private val _useNewCondInterface = MutableStateFlow(settingsManager?.useNewCondInterface ?: true)
    val useNewCondInterface = _useNewCondInterface.asStateFlow()

    private val _useNewSpeedInterface = MutableStateFlow(settingsManager?.useNewSpeedInterface ?: true)
    val useNewSpeedInterface = _useNewSpeedInterface.asStateFlow()

    private val _useOldAvatarStyle = MutableStateFlow(settingsManager?.useOldAvatarStyle ?: false)
    val useOldAvatarStyle = _useOldAvatarStyle.asStateFlow()

    private val _diceFabOffsetX = MutableStateFlow(settingsManager?.diceFabOffsetX ?: -40f)
    val diceFabOffsetX = _diceFabOffsetX.asStateFlow()

    private val _diceFabOffsetY = MutableStateFlow(settingsManager?.diceFabOffsetY ?: -40f)
    val diceFabOffsetY = _diceFabOffsetY.asStateFlow()

    private val _diceFabAlpha = MutableStateFlow(settingsManager?.diceFabAlpha ?: 1.0f)
    val diceFabAlpha = _diceFabAlpha.asStateFlow()

    private val _diceFabBlurEnabled = MutableStateFlow(settingsManager?.diceFabBlurEnabled ?: true)
    val diceFabBlurEnabled = _diceFabBlurEnabled.asStateFlow()

    private val _diceFabEnabled = MutableStateFlow(settingsManager?.diceFabEnabled ?: true)
    val diceFabEnabled = _diceFabEnabled.asStateFlow()

    private val _renderDiceInOrder = MutableStateFlow(settingsManager?.renderDiceInOrder ?: true)
    val renderDiceInOrder = _renderDiceInOrder.asStateFlow()

    private val _collapseActionsOnEdit = MutableStateFlow(settingsManager?.collapseActionsOnEdit ?: true)
    val collapseActionsOnEdit = _collapseActionsOnEdit.asStateFlow()

    private val _collapseSpellsOnEdit = MutableStateFlow(settingsManager?.collapseSpellsOnEdit ?: true)
    val collapseSpellsOnEdit = _collapseSpellsOnEdit.asStateFlow()

    private val _collapseDynamicFieldsOnEdit = MutableStateFlow(settingsManager?.collapseDynamicFieldsOnEdit ?: true)
    val collapseDynamicFieldsOnEdit = _collapseDynamicFieldsOnEdit.asStateFlow()

    private val _lastModuleExportName = MutableStateFlow(settingsManager?.lastModuleExportName ?: "")
    val lastModuleExportName = _lastModuleExportName.asStateFlow()

    private val _lastModuleExportDescription = MutableStateFlow(settingsManager?.lastModuleExportDescription ?: "")
    val lastModuleExportDescription = _lastModuleExportDescription.asStateFlow()

    private val _lastModuleExportVersion = MutableStateFlow(settingsManager?.lastModuleExportVersion ?: "1.0.0")
    val lastModuleExportVersion = _lastModuleExportVersion.asStateFlow()

    private val _lastModuleExportId = MutableStateFlow(settingsManager?.lastModuleExportId ?: "")
    val lastModuleExportId = _lastModuleExportId.asStateFlow()

    fun updateLastModuleExport(name: String, description: String, version: String, id: String) {
        _lastModuleExportName.value = name
        _lastModuleExportDescription.value = description
        _lastModuleExportVersion.value = version
        _lastModuleExportId.value = id
        settingsManager?.lastModuleExportName = name
        settingsManager?.lastModuleExportDescription = description
        settingsManager?.lastModuleExportVersion = version
        settingsManager?.lastModuleExportId = id
    }

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

    fun updateBlurCards(enabled: Boolean) {
        _blurCards.value = enabled
        settingsManager?.blurCards = enabled
    }

    fun updateBlurDynamicFields(enabled: Boolean) {
        _blurDynamicFields.value = enabled
        settingsManager?.blurDynamicFields = enabled
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

    fun updateUseOldAvatarStyle(enabled: Boolean) {
        _useOldAvatarStyle.value = enabled
        settingsManager?.useOldAvatarStyle = enabled
    }

    fun updateDiceFabPosition(x: Float, y: Float) {
        _diceFabOffsetX.value = x
        _diceFabOffsetY.value = y
        settingsManager?.diceFabOffsetX = x
        settingsManager?.diceFabOffsetY = y
    }

    fun updateDiceFabAlpha(alpha: Float) {
        _diceFabAlpha.value = alpha
        settingsManager?.diceFabAlpha = alpha
    }

    fun updateDiceFabBlurEnabled(enabled: Boolean) {
        _diceFabBlurEnabled.value = enabled
        settingsManager?.diceFabBlurEnabled = enabled
    }

    fun updateDiceFabEnabled(enabled: Boolean) {
        _diceFabEnabled.value = enabled
        settingsManager?.diceFabEnabled = enabled
    }

    fun updateRenderDiceInOrder(enabled: Boolean) {
        _renderDiceInOrder.value = enabled
        settingsManager?.renderDiceInOrder = enabled
    }

    fun updateCollapseActionsOnEdit(enabled: Boolean) {
        _collapseActionsOnEdit.value = enabled
        settingsManager?.collapseActionsOnEdit = enabled
    }

    fun updateCollapseSpellsOnEdit(enabled: Boolean) {
        _collapseSpellsOnEdit.value = enabled
        settingsManager?.collapseSpellsOnEdit = enabled
    }

    fun updateCollapseDynamicFieldsOnEdit(enabled: Boolean) {
        _collapseDynamicFieldsOnEdit.value = enabled
        settingsManager?.collapseDynamicFieldsOnEdit = enabled
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
            val roundedScale = (newScale * 10f).roundToInt() / 10f
            appScaleManager.setScaleFactor(roundedScale)
        }
    }

    fun resetToDefaults() {
        settingsManager?.resetToDefaults()
        viewModelScope.launch {
            appScaleManager.resetToDefaults()
        }
        
        _rollHistorySize.value = settingsManager?.rollHistorySize ?: 5
        _customRollHistorySize.value = settingsManager?.customRollHistorySize ?: 10
        _blurRolls.value = settingsManager?.blurRolls ?: true
        _blurFullscreen.value = settingsManager?.blurFullscreen ?: true
        _blurPopups.value = settingsManager?.blurPopups ?: true
        _blurCards.value = settingsManager?.blurCards ?: true
        _blurDynamicFields.value = settingsManager?.blurDynamicFields ?: true
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
        _useNewACInterface.value = settingsManager?.useNewACInterface ?: true
        _useNewInitInterface.value = settingsManager?.useNewInitInterface ?: true
        _useNewCondInterface.value = settingsManager?.useNewCondInterface ?: true
        _useNewSpeedInterface.value = settingsManager?.useNewSpeedInterface ?: true
        _useOldAvatarStyle.value = settingsManager?.useOldAvatarStyle ?: false
        
        _diceFabOffsetX.value = settingsManager?.diceFabOffsetX ?: -40f
        _diceFabOffsetY.value = settingsManager?.diceFabOffsetY ?: -40f
        _diceFabAlpha.value = settingsManager?.diceFabAlpha ?: 1.0f
        _diceFabBlurEnabled.value = settingsManager?.diceFabBlurEnabled ?: true
        _diceFabEnabled.value = settingsManager?.diceFabEnabled ?: true
        _renderDiceInOrder.value = settingsManager?.renderDiceInOrder ?: true
        _collapseActionsOnEdit.value = settingsManager?.collapseActionsOnEdit ?: true
        _collapseSpellsOnEdit.value = settingsManager?.collapseSpellsOnEdit ?: true
        _collapseDynamicFieldsOnEdit.value = settingsManager?.collapseDynamicFieldsOnEdit ?: true
    }
}
