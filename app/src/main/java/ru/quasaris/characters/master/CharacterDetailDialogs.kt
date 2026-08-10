package ru.quasaris.characters.master

import androidx.compose.runtime.*
import dev.chrisbanes.haze.HazeState
import ru.quasaris.characters.master.HeaderCode.Fullscreen.*
import ru.quasaris.characters.master.HeaderCode.HealthDialog
import ru.quasaris.characters.master.backend.*
import ru.quasaris.characters.master.tabs.spells.SpellSettingsDialog
import ru.quasaris.characters.master.ui.RestPanel

@Composable
fun CharacterDetailDialogs(
    state: CharacterDetailState,
    statsMap: Map<String, String>,
    hazeState: HazeState?,
    forceBlurEnabled: Boolean,
    blurPopups: Boolean,
    allConditions: List<ru.quasaris.characters.master.backend.Condition>
) {
    val effectiveBlurFullscreen = forceBlurEnabled

    if (state.showSpellSettings) {
        SpellSettingsDialog(
            settings = state.spellSettings,
            characterLevel = state.level.toIntOrNull() ?: 1,
            onSettingsChange = { state.spellSettings = it },
            onDismiss = { state.showSpellSettings = false },
            onSubDialogOpenChange = { state.isMagicBonusSettingsOpen = it },
            hazeState = hazeState,
            forceBlurEnabled = forceBlurEnabled,
            statsMap = statsMap
        )
    }

    if (state.showEnhancedAC) {
        val activeArmor = state.armorClassEntries.find { it.id == state.activeArmorClassId }
        val activeShieldObj = state.shieldEntries.find { it.id == state.activeShieldId }
        
        ArmorClassDialog(
            activeEntry = activeArmor,
            allEntries = state.armorClassEntries,
            onAllEntriesChange = { state.armorClassEntries = it.filterIsInstance<ArmorClassEntry>() },
            onActiveIdChange = { state.activeArmorClassId = it },
            statsMap = statsMap,
            hazeState = hazeState,
            forceBlurEnabled = effectiveBlurFullscreen,
            onDismiss = { state.showEnhancedAC = false },
            onSubDialogOpenChange = { state.isArmorClassSubDialogOpen = it },
            isShieldActive = state.isShieldActive,
            onShieldActiveChange = { state.isShieldActive = it },
            activeShield = activeShieldObj,
            allShields = state.shieldEntries,
            onShieldChange = { updated ->
                val newList = state.shieldEntries.toMutableList()
                val idx = newList.indexOfFirst { it.id == updated.id }
                if (idx != -1) {
                    newList[idx] = updated
                    state.shieldEntries = newList
                }
            },
            onAllShieldsChange = { state.shieldEntries = it },
            onActiveShieldIdChange = { state.activeShieldId = it }
        )
    }

    if (state.showEnhancedInit) {
        val activeInit = state.initiativeEntries.find { it.id == state.activeInitiativeId }
        InitiativeDialog(
            activeEntry = activeInit,
            allEntries = state.initiativeEntries,
            onAllEntriesChange = { state.initiativeEntries = it.filterIsInstance<InitiativeEntry>() },
            onActiveIdChange = { state.activeInitiativeId = it },
            statsMap = statsMap,
            hazeState = hazeState,
            forceBlurEnabled = effectiveBlurFullscreen,
            onDismiss = { state.showEnhancedInit = false },
            onSubDialogOpenChange = { state.isInitiativeSubDialogOpen = it }
        )
    }

    if (state.showEnhancedSpeed) {
        val activeSpeed = state.speedEntries.find { it.id == state.activeSpeedId }
        SpeedDialog(
            activeEntry = activeSpeed,
            allEntries = state.speedEntries,
            onAllEntriesChange = { state.speedEntries = it.filterIsInstance<SpeedEntry>() },
            onActiveIdChange = { state.activeSpeedId = it },
            statsMap = statsMap,
            hazeState = hazeState,
            forceBlurEnabled = effectiveBlurFullscreen,
            onDismiss = { state.showEnhancedSpeed = false },
            onSubDialogOpenChange = { state.isSpeedSubDialogOpen = it }
        )
    }

    if (state.showHealthSettings) {
        HealthSettingsDialog(
            isManual = state.isManualHP,
            onManualChange = { state.isManualHP = it },
            manualMaxHp = state.manualMaxHp,
            onManualMaxHpChange = { state.manualMaxHp = it },
            isMulticlass = state.isMulticlassHP,
            onMulticlassChange = { state.isMulticlassHP = it },
            currentHitDie = state.defaultHitDie,
            onHitDieChange = { state.defaultHitDie = it },
            hpLevelData = state.hpLevelData,
            onHPLevelDataChange = { state.hpLevelData = it },
            manualHPLevelData = state.manualHPLevelData,
            onManualHPLevelDataChange = { state.manualHPLevelData = it },
            manualMaxHitDice = state.manualMaxHitDice,
            onManualMaxHitDiceChange = { state.manualMaxHitDice = it },
            hpBonusesAtLevel = state.hpBonusesAtLevel,
            onHpBonusesAtLevelChange = { state.hpBonusesAtLevel = it },
            hpBonusesTotal = state.hpBonusesTotal,
            onHpBonusesTotalChange = { state.hpBonusesTotal = it },
            statsMap = statsMap,
            level = state.level.toIntOrNull() ?: 1,
            hazeState = hazeState,
            forceBlurEnabled = effectiveBlurFullscreen,
            onDismiss = { state.showHealthSettings = false }
        )
    }

    if (state.showEnhancedCond) {
        ConditionsDialog(
            allConditions = allConditions,
            selectedConditions = state.selectedConditions,
            onToggleCondition = { cond -> 
                state.selectedConditions = if (state.selectedConditions.contains(cond)) state.selectedConditions - cond else state.selectedConditions + cond 
            },
            exhaustion = state.exhaustion,
            onExhaustionChange = { state.exhaustion = it },
            hazeState = hazeState,
            forceBlurEnabled = effectiveBlurFullscreen,
            onDismiss = { state.showEnhancedCond = false }
        )
    }

    HealthDialog(
        showDialog = state.showHpDialog,
        hpDialogType = state.hpDialogType,
        hpDialogValue = state.hpDialogValue,
        onValueChange = { newVal: String -> state.hpDialogValue = newVal },
        onDismiss = { state.showHpDialog = false },
        onConfirm = { value: Int ->
            val maxHpInt = state.maxHp.toIntOrNull() ?: 0
            val currentHpInt = state.currentHp.toIntOrNull() ?: 0
            val tempHpInt = state.tempHp.toIntOrNull() ?: 0

            when(state.hpDialogType) {
                "heal" -> state.currentHp = minOf(maxHpInt, currentHpInt + value).toString()
                "damage" -> {
                    var d = value
                    var t = tempHpInt
                    val c = currentHpInt
                    if (t > 0) {
                        val a = minOf(t, d)
                        t -= a
                        d -= a
                        state.tempHp = t.toString()
                    }
                    if (d > 0) state.currentHp = maxOf(0, c - d).toString()
                }
                "temp" -> state.tempHp = minOf(9999, value).toString()
            }
            state.showHpDialog = false
        }
    )
}
