package ru.quasaris.characters.master.backend

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

    fun updateRollHistorySize(size: Int) {
        _rollHistorySize.value = size
        settingsManager?.rollHistorySize = size
    }

    fun updateCustomRollHistorySize(size: Int) {
        _customRollHistorySize.value = size
        settingsManager?.customRollHistorySize = size
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
