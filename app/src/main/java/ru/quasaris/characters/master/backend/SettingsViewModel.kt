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
import kotlin.math.roundToInt

class SettingsViewModel(
    private val appScaleManager: AppScaleManager,
    private val settingsManager: SettingsManager? = null
) : ViewModel() {

    private val _rollHistorySize = MutableStateFlow(settingsManager?.rollHistorySize ?: 5)
    val rollHistorySize = _rollHistorySize.asStateFlow()

    private val _customRollHistorySize = MutableStateFlow(settingsManager?.customRollHistorySize ?: 10)
    val customRollHistorySize = _customRollHistorySize.asStateFlow()

    private val _forceBlurEnabled = MutableStateFlow(settingsManager?.forceBlurEnabled ?: false)
    val forceBlurEnabled = _forceBlurEnabled.asStateFlow()

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

    fun updateRollHistorySize(size: Int) {
        _rollHistorySize.value = size
        settingsManager?.rollHistorySize = size
    }

    fun updateCustomRollHistorySize(size: Int) {
        _customRollHistorySize.value = size
        settingsManager?.customRollHistorySize = size
    }

    fun updateForceBlurEnabled(enabled: Boolean) {
        _forceBlurEnabled.value = enabled
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
}
