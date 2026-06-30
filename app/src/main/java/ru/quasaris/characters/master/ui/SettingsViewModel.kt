package ru.quasaris.characters.master.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.quasaris.characters.master.AppScaleManager
import kotlin.math.roundToInt

class SettingsViewModel(private val appScaleManager: AppScaleManager) : ViewModel() {

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
