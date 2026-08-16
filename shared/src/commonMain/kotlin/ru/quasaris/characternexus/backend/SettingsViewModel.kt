package ru.quasaris.characternexus.backend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.*
import kotlin.math.roundToInt

class SettingsViewModel(
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _rollHistorySize = MutableStateFlow(settingsManager.settings.rollHistorySize)
    val rollHistorySize = _rollHistorySize.asStateFlow()

    private val _customRollHistorySize = MutableStateFlow(settingsManager.settings.customRollHistorySize)
    val customRollHistorySize = _customRollHistorySize.asStateFlow()

    private val _blurRolls = MutableStateFlow(settingsManager.settings.blurRolls)
    val blurRolls = _blurRolls.asStateFlow()

    private val _blurFullscreen = MutableStateFlow(settingsManager.settings.blurFullscreen)
    val blurFullscreen = _blurFullscreen.asStateFlow()

    private val _blurPopups = MutableStateFlow(settingsManager.settings.blurPopups)
    val blurPopups = _blurPopups.asStateFlow()

    private val _masterBlurEnabled = MutableStateFlow(settingsManager.settings.masterBlurEnabled)
    val masterBlurEnabled = _masterBlurEnabled.asStateFlow()

    private val _rollInterfaceAlpha = MutableStateFlow(settingsManager.settings.rollInterfaceAlpha)
    val rollInterfaceAlpha = _rollInterfaceAlpha.asStateFlow()

    private val _rollPassThrough = MutableStateFlow(settingsManager.settings.rollPassThrough)
    val rollPassThrough = _rollPassThrough.asStateFlow()

    private val _rollPosition = MutableStateFlow(DiceRollPosition.valueOf(settingsManager.settings.rollPosition))
    val rollPosition = _rollPosition.asStateFlow()

    private val _rollCloseButtonPosition = MutableStateFlow(DiceRollPosition.valueOf(settingsManager.settings.rollCloseButtonPosition))
    val rollCloseButtonPosition = _rollCloseButtonPosition.asStateFlow()

    private val _debugInfoEnabled = MutableStateFlow(settingsManager.settings.debugInfoEnabled)
    val debugInfoEnabled = _debugInfoEnabled.asStateFlow()

    private val _deletionWarningEnabled = MutableStateFlow(settingsManager.settings.deletionWarningEnabled)
    val deletionWarningEnabled = _deletionWarningEnabled.asStateFlow()

    private val _advantageLogic = MutableStateFlow(settingsManager.settings.advantageLogic)
    val advantageLogic = _advantageLogic.asStateFlow()

    private val _exportFormat = MutableStateFlow(settingsManager.settings.exportFormat)
    val exportFormat = _exportFormat.asStateFlow()

    private val _useNewACInterface = MutableStateFlow(settingsManager.settings.useNewACInterface)
    val useNewACInterface = _useNewACInterface.asStateFlow()

    private val _useNewInitInterface = MutableStateFlow(settingsManager.settings.useNewInitInterface)
    val useNewInitInterface = _useNewInitInterface.asStateFlow()

    private val _useNewCondInterface = MutableStateFlow(settingsManager.settings.useNewCondInterface)
    val useNewCondInterface = _useNewCondInterface.asStateFlow()

    private val _useNewSpeedInterface = MutableStateFlow(settingsManager.settings.useNewSpeedInterface)
    val useNewSpeedInterface = _useNewSpeedInterface.asStateFlow()

    private val _diceFabOffsetX = MutableStateFlow(settingsManager.settings.diceFabOffsetX)
    val diceFabOffsetX = _diceFabOffsetX.asStateFlow()

    private val _diceFabOffsetY = MutableStateFlow(settingsManager.settings.diceFabOffsetY)
    val diceFabOffsetY = _diceFabOffsetY.asStateFlow()

    private val _diceFabAlpha = MutableStateFlow(settingsManager.settings.diceFabAlpha)
    val diceFabAlpha = _diceFabAlpha.asStateFlow()

    private val _diceFabBlurEnabled = MutableStateFlow(settingsManager.settings.diceFabBlurEnabled)
    val diceFabBlurEnabled = _diceFabBlurEnabled.asStateFlow()

    private val _diceFabEnabled = MutableStateFlow(settingsManager.settings.diceFabEnabled)
    val diceFabEnabled = _diceFabEnabled.asStateFlow()

    private val _scaleFactor = MutableStateFlow(settingsManager.settings.scaleFactor)
    val scaleFactor: StateFlow<Float> = _scaleFactor.asStateFlow()

    private val _lastModuleExportName = MutableStateFlow(settingsManager.settings.lastModuleExportName)
    val lastModuleExportName = _lastModuleExportName.asStateFlow()

    private val _lastModuleExportDescription = MutableStateFlow(settingsManager.settings.lastModuleExportDescription)
    val lastModuleExportDescription = _lastModuleExportDescription.asStateFlow()

    private val _lastModuleExportVersion = MutableStateFlow(settingsManager.settings.lastModuleExportVersion)
    val lastModuleExportVersion = _lastModuleExportVersion.asStateFlow()

    private val _lastModuleExportId = MutableStateFlow(settingsManager.settings.lastModuleExportId)
    val lastModuleExportId = _lastModuleExportId.asStateFlow()

    private val _blurCards = MutableStateFlow(settingsManager.settings.blurCards)
    val blurCards = _blurCards.asStateFlow()

    private val _blurDynamicFields = MutableStateFlow(settingsManager.settings.blurDynamicFields)
    val blurDynamicFields = _blurDynamicFields.asStateFlow()

    private val _fullscreenEditingOnly = MutableStateFlow(settingsManager.settings.fullscreenEditingOnly)
    val fullscreenEditingOnly = _fullscreenEditingOnly.asStateFlow()

    private val _topMarginStep = MutableStateFlow(settingsManager.settings.topMarginStep)
    val topMarginStep = _topMarginStep.asStateFlow()

    private val _customTopMargin = MutableStateFlow(settingsManager.settings.customTopMargin)
    val customTopMargin = _customTopMargin.asStateFlow()

    private val _autoDownloadLssAvatar = MutableStateFlow(settingsManager.settings.autoDownloadLssAvatar)
    val autoDownloadLssAvatar = _autoDownloadLssAvatar.asStateFlow()

    private val _useOldAvatarStyle = MutableStateFlow(settingsManager.settings.useOldAvatarStyle)
    val useOldAvatarStyle = _useOldAvatarStyle.asStateFlow()

    private val _renderDiceInOrder = MutableStateFlow(settingsManager.settings.renderDiceInOrder)
    val renderDiceInOrder = _renderDiceInOrder.asStateFlow()

    private val _collapseActionsOnEdit = MutableStateFlow(settingsManager.settings.collapseActionsOnEdit)
    val collapseActionsOnEdit = _collapseActionsOnEdit.asStateFlow()

    private val _collapseSpellsOnEdit = MutableStateFlow(settingsManager.settings.collapseSpellsOnEdit)
    val collapseSpellsOnEdit = _collapseSpellsOnEdit.asStateFlow()

    private val _collapseDynamicFieldsOnEdit = MutableStateFlow(settingsManager.settings.collapseDynamicFieldsOnEdit)
    val collapseDynamicFieldsOnEdit = _collapseDynamicFieldsOnEdit.asStateFlow()

    private val _longRestAlignment = MutableStateFlow(settingsManager.settings.longRestAlignment)
    val longRestAlignment = _longRestAlignment.asStateFlow()

    private val _longRestFillDirection = MutableStateFlow(settingsManager.settings.longRestFillDirection)
    val longRestFillDirection = _longRestFillDirection.asStateFlow()

    private val _shortRestAlignment = MutableStateFlow(settingsManager.settings.shortRestAlignment)
    val shortRestAlignment = _shortRestAlignment.asStateFlow()

    private val _shortRestFillDirection = MutableStateFlow(settingsManager.settings.shortRestFillDirection)
    val shortRestFillDirection = _shortRestFillDirection.asStateFlow()

    private val _dawnRestAlignment = MutableStateFlow(settingsManager.settings.dawnRestAlignment)
    val dawnRestAlignment = _dawnRestAlignment.asStateFlow()

    private val _dawnRestFillDirection = MutableStateFlow(settingsManager.settings.dawnRestFillDirection)
    val dawnRestFillDirection = _dawnRestFillDirection.asStateFlow()

    private val _exportDirectoryUri = MutableStateFlow(settingsManager.settings.exportDirectoryUri)
    val exportDirectoryUri = _exportDirectoryUri.asStateFlow()

    val performanceClass get() = ru.quasaris.characternexus.performanceClass

    fun updateRollHistorySize(size: Int) {
        _rollHistorySize.value = size
        settingsManager.settings.rollHistorySize = size
        settingsManager.save()
    }

    fun updateCustomRollHistorySize(size: Int) {
        _customRollHistorySize.value = size
        settingsManager.settings.customRollHistorySize = size
        settingsManager.save()
    }

    fun updateBlurRolls(enabled: Boolean) {
        _blurRolls.value = enabled
        settingsManager.settings.blurRolls = enabled
        settingsManager.save()
    }

    fun updateBlurFullscreen(enabled: Boolean) {
        _blurFullscreen.value = enabled
        settingsManager.settings.blurFullscreen = enabled
        settingsManager.save()
    }

    fun updateBlurPopups(enabled: Boolean) {
        _blurPopups.value = enabled
        settingsManager.settings.blurPopups = enabled
        settingsManager.save()
    }

    fun updateMasterBlurEnabled(enabled: Boolean) {
        _masterBlurEnabled.value = enabled
        settingsManager.settings.masterBlurEnabled = enabled
        settingsManager.save()
    }

    fun updateRollInterfaceAlpha(alpha: Float) {
        _rollInterfaceAlpha.value = alpha
        settingsManager.settings.rollInterfaceAlpha = alpha
        settingsManager.save()
    }

    fun updateRollPassThrough(enabled: Boolean) {
        _rollPassThrough.value = enabled
        settingsManager.settings.rollPassThrough = enabled
        settingsManager.save()
    }

    fun updateRollPosition(position: DiceRollPosition) {
        _rollPosition.value = position
        settingsManager.settings.rollPosition = position.name
        settingsManager.save()
    }

    fun updateRollCloseButtonPosition(position: DiceRollPosition) {
        _rollCloseButtonPosition.value = position
        settingsManager.settings.rollCloseButtonPosition = position.name
        settingsManager.save()
    }

    fun updateDebugInfoEnabled(enabled: Boolean) {
        _debugInfoEnabled.value = enabled
        settingsManager.settings.debugInfoEnabled = enabled
        settingsManager.save()
    }

    fun updateDeletionWarningEnabled(enabled: Boolean) {
        _deletionWarningEnabled.value = enabled
        settingsManager.settings.deletionWarningEnabled = enabled
        settingsManager.save()
    }

    fun updateAdvantageLogic(logic: AdvantageLogic) {
        _advantageLogic.value = logic
        settingsManager.settings.advantageLogic = logic
        settingsManager.save()
    }

    fun updateExportDirectoryUri(uri: String?) {
        _exportDirectoryUri.value = uri
        settingsManager.settings.exportDirectoryUri = uri
        settingsManager.save()
    }

    fun updateExportFormat(format: ExportFormat) {
        _exportFormat.value = format
        settingsManager.settings.exportFormat = format
        settingsManager.save()
    }

    fun updateUseNewACInterface(enabled: Boolean) {
        _useNewACInterface.value = enabled
        settingsManager.settings.useNewACInterface = enabled
        settingsManager.save()
    }

    fun updateUseNewInitInterface(enabled: Boolean) {
        _useNewInitInterface.value = enabled
        settingsManager.settings.useNewInitInterface = enabled
        settingsManager.save()
    }

    fun updateUseNewCondInterface(enabled: Boolean) {
        _useNewCondInterface.value = enabled
        settingsManager.settings.useNewCondInterface = enabled
        settingsManager.save()
    }

    fun updateUseNewSpeedInterface(enabled: Boolean) {
        _useNewSpeedInterface.value = enabled
        settingsManager.settings.useNewSpeedInterface = enabled
        settingsManager.save()
    }

    fun updateDiceFabPosition(x: Float, y: Float) {
        _diceFabOffsetX.value = x
        _diceFabOffsetY.value = y
        settingsManager.settings.diceFabOffsetX = x
        settingsManager.settings.diceFabOffsetY = y
        settingsManager.save()
    }

    fun updateDiceFabAlpha(alpha: Float) {
        _diceFabAlpha.value = alpha
        settingsManager.settings.diceFabAlpha = alpha
        settingsManager.save()
    }

    fun updateDiceFabBlurEnabled(enabled: Boolean) {
        _diceFabBlurEnabled.value = enabled
        settingsManager.settings.diceFabBlurEnabled = enabled
        settingsManager.save()
    }

    fun updateDiceFabEnabled(enabled: Boolean) {
        _diceFabEnabled.value = enabled
        settingsManager.settings.diceFabEnabled = enabled
        settingsManager.save()
    }

    fun updateScaleFactor(newScale: Float) {
        val roundedScale = (newScale * 10f).roundToInt() / 10f
        _scaleFactor.value = roundedScale
        settingsManager.settings.scaleFactor = roundedScale
        settingsManager.save()
    }

    fun updateLastModuleExport(name: String, desc: String, version: String, id: String) {
        _lastModuleExportName.value = name
        _lastModuleExportDescription.value = desc
        _lastModuleExportVersion.value = version
        _lastModuleExportId.value = id
        settingsManager.settings.lastModuleExportName = name
        settingsManager.settings.lastModuleExportDescription = desc
        settingsManager.settings.lastModuleExportVersion = version
        settingsManager.settings.lastModuleExportId = id
        settingsManager.save()
    }

    fun updateBlurCards(enabled: Boolean) {
        _blurCards.value = enabled
        settingsManager.settings.blurCards = enabled
        settingsManager.save()
    }

    fun updateBlurDynamicFields(enabled: Boolean) {
        _blurDynamicFields.value = enabled
        settingsManager.settings.blurDynamicFields = enabled
        settingsManager.save()
    }

    fun updateFullscreenEditingOnly(enabled: Boolean) {
        _fullscreenEditingOnly.value = enabled
        settingsManager.settings.fullscreenEditingOnly = enabled
        settingsManager.save()
    }

    fun updateTopMarginStep(step: Int) {
        _topMarginStep.value = step
        settingsManager.settings.topMarginStep = step
        settingsManager.save()
    }

    fun updateCustomTopMargin(margin: Int) {
        _customTopMargin.value = margin
        settingsManager.settings.customTopMargin = margin
        settingsManager.save()
    }

    fun updateAutoDownloadLssAvatar(enabled: Boolean) {
        _autoDownloadLssAvatar.value = enabled
        settingsManager.settings.autoDownloadLssAvatar = enabled
        settingsManager.save()
    }

    fun updateUseOldAvatarStyle(enabled: Boolean) {
        _useOldAvatarStyle.value = enabled
        settingsManager.settings.useOldAvatarStyle = enabled
        settingsManager.save()
    }

    fun updateRenderDiceInOrder(enabled: Boolean) {
        _renderDiceInOrder.value = enabled
        settingsManager.settings.renderDiceInOrder = enabled
        settingsManager.save()
    }

    fun updateCollapseActionsOnEdit(enabled: Boolean) {
        _collapseActionsOnEdit.value = enabled
        settingsManager.settings.collapseActionsOnEdit = enabled
        settingsManager.save()
    }

    fun updateCollapseSpellsOnEdit(enabled: Boolean) {
        _collapseSpellsOnEdit.value = enabled
        settingsManager.settings.collapseSpellsOnEdit = enabled
        settingsManager.save()
    }

    fun updateCollapseDynamicFieldsOnEdit(enabled: Boolean) {
        _collapseDynamicFieldsOnEdit.value = enabled
        settingsManager.settings.collapseDynamicFieldsOnEdit = enabled
        settingsManager.save()
    }

    fun updateLongRestAlignment(alignment: SlotAlignment) {
        _longRestAlignment.value = alignment
        settingsManager.settings.longRestAlignment = alignment
        settingsManager.save()
    }

    fun updateLongRestFillDirection(direction: SlotFillDirection) {
        _longRestFillDirection.value = direction
        settingsManager.settings.longRestFillDirection = direction
        settingsManager.save()
    }

    fun updateShortRestAlignment(alignment: SlotAlignment) {
        _shortRestAlignment.value = alignment
        settingsManager.settings.shortRestAlignment = alignment
        settingsManager.save()
    }

    fun updateShortRestFillDirection(direction: SlotFillDirection) {
        _shortRestFillDirection.value = direction
        settingsManager.settings.shortRestFillDirection = direction
        settingsManager.save()
    }

    fun updateDawnRestAlignment(alignment: SlotAlignment) {
        _dawnRestAlignment.value = alignment
        settingsManager.settings.dawnRestAlignment = alignment
        settingsManager.save()
    }

    fun updateDawnRestFillDirection(direction: SlotFillDirection) {
        _dawnRestFillDirection.value = direction
        settingsManager.settings.dawnRestFillDirection = direction
        settingsManager.save()
    }

    fun resetToDefaults() {
        settingsManager.resetToDefaults()
        
        val s = settingsManager.settings
        _rollHistorySize.value = s.rollHistorySize
        _customRollHistorySize.value = s.customRollHistorySize
        _blurRolls.value = s.blurRolls
        _blurFullscreen.value = s.blurFullscreen
        _blurPopups.value = s.blurPopups
        _masterBlurEnabled.value = s.masterBlurEnabled
        _rollInterfaceAlpha.value = s.rollInterfaceAlpha
        _rollPassThrough.value = s.rollPassThrough
        _rollPosition.value = DiceRollPosition.valueOf(s.rollPosition)
        _rollCloseButtonPosition.value = DiceRollPosition.valueOf(s.rollCloseButtonPosition)
        _debugInfoEnabled.value = s.debugInfoEnabled
        _deletionWarningEnabled.value = s.deletionWarningEnabled
        _advantageLogic.value = s.advantageLogic
        _exportFormat.value = s.exportFormat
        _useNewACInterface.value = s.useNewACInterface
        _useNewInitInterface.value = s.useNewInitInterface
        _useNewCondInterface.value = s.useNewCondInterface
        _useNewSpeedInterface.value = s.useNewSpeedInterface
        _diceFabOffsetX.value = s.diceFabOffsetX
        _diceFabOffsetY.value = s.diceFabOffsetY
        _diceFabAlpha.value = s.diceFabAlpha
        _diceFabBlurEnabled.value = s.diceFabBlurEnabled
        _diceFabEnabled.value = s.diceFabEnabled
        _scaleFactor.value = s.scaleFactor
        
        _lastModuleExportName.value = s.lastModuleExportName
        _lastModuleExportDescription.value = s.lastModuleExportDescription
        _lastModuleExportVersion.value = s.lastModuleExportVersion
        _lastModuleExportId.value = s.lastModuleExportId
        _blurCards.value = s.blurCards
        _blurDynamicFields.value = s.blurDynamicFields
        _fullscreenEditingOnly.value = s.fullscreenEditingOnly
        _topMarginStep.value = s.topMarginStep
        _customTopMargin.value = s.customTopMargin
        _autoDownloadLssAvatar.value = s.autoDownloadLssAvatar
        _useOldAvatarStyle.value = s.useOldAvatarStyle
        _renderDiceInOrder.value = s.renderDiceInOrder
        _collapseActionsOnEdit.value = s.collapseActionsOnEdit
        _collapseSpellsOnEdit.value = s.collapseSpellsOnEdit
        _collapseDynamicFieldsOnEdit.value = s.collapseDynamicFieldsOnEdit
        _longRestAlignment.value = s.longRestAlignment
        _longRestFillDirection.value = s.longRestFillDirection
        _shortRestAlignment.value = s.shortRestAlignment
        _shortRestFillDirection.value = s.shortRestFillDirection
        _dawnRestAlignment.value = s.dawnRestAlignment
        _dawnRestFillDirection.value = s.dawnRestFillDirection
        _exportDirectoryUri.value = s.exportDirectoryUri
    }
}
