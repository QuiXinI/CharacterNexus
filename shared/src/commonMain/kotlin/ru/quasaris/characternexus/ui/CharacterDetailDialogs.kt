package ru.quasaris.characternexus.ui

import androidx.compose.runtime.*
import dev.chrisbanes.haze.HazeState
import ru.quasaris.characternexus.backend.*
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.tabs.BonusConfigDialog
import ru.quasaris.characternexus.tabs.DynamicFieldFullscreenDialog
import ru.quasaris.characternexus.tabs.ResourceConfigDialog
import ru.quasaris.characternexus.tabs.DynamicContentBlock
import ru.quasaris.characternexus.tabs.attacks.AttackConfigDialog
import ru.quasaris.characternexus.tabs.attacks.calculateAttackFormulaParts
import ru.quasaris.characternexus.tabs.spells.SpellSettingsDialog
import ru.quasaris.characternexus.tabs.spells.SpellbookSelectionDialog
import ru.quasaris.characternexus.ui.editors.SpellEditorWindow
import ru.quasaris.characternexus.HeaderCode.Fullscreen.*
import ru.quasaris.characternexus.HeaderCode.HealthDialog
import ru.quasaris.characternexus.model.HitDiceEntry

@Composable
fun CharacterDetailDialogs(
    state: CharacterDetailState,
    statsMap: Map<String, String>,
    forceBlurEnabled: Boolean,
    blurPopups: Boolean,
    allConditions: List<Condition>,
    isDesktop: Boolean = false,
    targetSection: String = "",
    spellbookManager: SpellbookManager? = null,
    onSpellSettingsChange: (SpellSettings) -> Unit = {},
    hazeState: HazeState? = null,
    popupHazeState: HazeState? = null,
    onRoll: (RollResult) -> Unit = {}
) {
    val effectiveBlurFullscreen = forceBlurEnabled

    val spellSettings = state.spellSettings
    val pb = getProficiencyBonus(state.level)
    val abilityModifier = if (spellSettings.spellcastingAbility != Attribute.NONE) {
        val statKey = spellSettings.spellcastingAbility.name.lowercase()
        calculateModifier(statsMap[statKey] ?: "10")
    } else 0

    val renderDiceInOrder by state.settingsViewModel?.renderDiceInOrder?.collectAsState() ?: remember { mutableStateOf(true) }

    val magicAtkCalculation = remember(spellSettings.spellAttackBonuses, pb, abilityModifier, statsMap, state.exhaustion, renderDiceInOrder) {
        calculateAttackFormulaParts(
            baseFlat = pb + abilityModifier,
            bonuses = spellSettings.spellAttackBonuses,
            stats = statsMap,
            renderInOrder = renderDiceInOrder
        )
    }

    val magicSaveCalculation = remember(spellSettings.spellSaveDcBonuses, pb, abilityModifier, statsMap, renderDiceInOrder) {
        calculateAttackFormulaParts(
            baseFlat = 8 + pb + abilityModifier,
            bonuses = spellSettings.spellSaveDcBonuses,
            stats = statsMap,
            renderInOrder = renderDiceInOrder
        )
    }

    // --- ALL DIALOGS on RIGHT SIDE for Desktop ---
    if (!isDesktop || targetSection == "right") {
        if (state.showSpellSettings) {
            SpellSettingsDialog(
                settings = state.spellSettings,
                characterLevel = state.level.toIntOrNull() ?: 1,
                onSettingsChange = { state.spellSettings = it },
                onDismiss = { state.showSpellSettings = false },
                onSubDialogOpenChange = { state.isMagicBonusSettingsOpen = it },
                forceBlurEnabled = forceBlurEnabled,
                statsMap = statsMap,
                isDesktop = isDesktop,
                hazeState = hazeState,
                popupHazeState = popupHazeState
            )
        }

        if (state.isAttackConfigOpen && state.editingAttack != null) {
            AttackConfigDialog(
                attack = state.editingAttack!!,
                proficiencyBonus = getProficiencyBonus(state.level),
                attributeModifiers = state.attributeModifiers,
                onDismiss = { 
                    state.isAttackConfigOpen = false
                    state.activeAttackConfigId = null
                    state.editingAttack = null
                },
                onSave = { updated: AttackEntry ->
                    val newAttacks = if (state.attacks.any { it.id == updated.id }) {
                        state.attacks.map { if (it.id == updated.id) updated else it }
                    } else {
                        state.attacks + updated
                    }
                    state.attacks = newAttacks
                    state.isAttackConfigOpen = false
                    state.activeAttackConfigId = null
                    state.editingAttack = null
                },
                onDelete = { deleted: AttackEntry ->
                    state.attacks = state.attacks.filter { it.id != deleted.id }
                    state.isAttackConfigOpen = false
                    state.activeAttackConfigId = null
                    state.editingAttack = null
                },
                forceBlurEnabled = forceBlurEnabled,
                exhaustion = state.exhaustion,
                settingsViewModel = state.settingsViewModel,
                stats = statsMap,
                spellSettings = state.spellSettings,
                isDesktop = isDesktop,
                hazeState = popupHazeState ?: hazeState
            )
        }

        if (state.isSpellEditorOpen && state.editingSpell != null) {
            SpellEditorWindow(
                spell = state.editingSpell!!,
                onDismiss = { 
                    state.isSpellEditorOpen = false
                    state.editingSpell = null
                },
                onSave = { updated: SpellCard ->
                    spellbookManager?.addOrUpdateSpell(updated)
                    if (updated.id !in state.spellSettings.selectedSpellIds) {
                        onSpellSettingsChange(state.spellSettings.copy(selectedSpellIds = state.spellSettings.selectedSpellIds + updated.id))
                    }
                    state.isSpellEditorOpen = false
                    state.editingSpell = null
                },
                onDelete = { deleted: SpellCard ->
                    spellbookManager?.deleteSpell(deleted.id)
                    onSpellSettingsChange(state.spellSettings.copy(selectedSpellIds = state.spellSettings.selectedSpellIds - deleted.id))
                    state.isSpellEditorOpen = false
                    state.editingSpell = null
                },
                forceBlurEnabled = forceBlurEnabled,
                settingsViewModel = state.settingsViewModel,
                isDesktop = isDesktop,
                hazeState = hazeState,
                popupHazeState = popupHazeState
            )
        }

        if (state.isWalletDialogOpen) {
            CurrencyEditDialog(
                wallet = state.wallet,
                initialCurrency = state.selectedCurrency ?: Currency.GOLD, 
                onWalletChange = { state.wallet = it },
                onDismiss = { 
                    state.isWalletDialogOpen = false
                    state.selectedCurrency = null
                },
                hazeState = popupHazeState ?: hazeState,
                forceBlurEnabled = forceBlurEnabled,
                isDesktop = isDesktop,
                settingsViewModel = state.settingsViewModel
            )
        }

        if (state.isFullscreenDynamicFieldOpen && state.activeDynamicField != null) {
            DynamicFieldFullscreenDialog(
                field = state.activeDynamicField!!,
                onFieldChange = { updated: DynamicNoteState ->
                    state.updateDynamicField(updated)
                },
                onDelete = {
                    state.deleteDynamicField(state.activeDynamicField!!)
                    state.isFullscreenDynamicFieldOpen = false
                    state.activeDynamicField = null
                },
                onDismiss = { 
                    state.isFullscreenDynamicFieldOpen = false
                    state.activeDynamicField = null
                },
                hazeState = popupHazeState ?: hazeState,
                forceBlurEnabled = forceBlurEnabled,
                settingsViewModel = state.settingsViewModel,
                statsMap = statsMap,
                isDesktop = isDesktop
            )
        }

        if (state.isSpellbookSelectionOpen && spellbookManager != null) {
            val blurCards by state.settingsViewModel?.blurCards?.collectAsState() ?: remember { mutableStateOf(true) }
            SpellbookSelectionDialog(
                spellbookManager = spellbookManager,
                selectedIds = spellSettings.selectedSpellIds,
                preparedIds = spellSettings.preparedSpellIds,
                isSpellbookEnabled = spellSettings.isSpellbookEnabled,
                onDismiss = { state.isSpellbookSelectionOpen = false },
                onSave = { newIds, newPrepared ->
                    onSpellSettingsChange(spellSettings.copy(
                        selectedSpellIds = newIds,
                        preparedSpellIds = newPrepared
                    ))
                },
                hazeState = popupHazeState ?: hazeState,
                forceBlurEnabled = forceBlurEnabled,
                blurCards = blurCards,
                statsMap = statsMap,
                characterLevel = state.level.toIntOrNull() ?: 1,
                spellAttackBonus = magicAtkCalculation.first,
                spellAttackDice = magicAtkCalculation.second,
                spellSaveDc = magicSaveCalculation.first,
                spellSaveDice = magicSaveCalculation.second,
                onRollDamage = { formula, title, advantage ->
                    onRoll(DiceRoller.roll(
                        title = title,
                        baseModifier = 0,
                        bonuses = listOf(SimpleBonus(formula = formula, name = "Урон")),
                        isDamage = true,
                        stats = statsMap,
                        exhaustion = 0,
                        sourceType = RollSourceType.OTHER,
                        advantageType = advantage,
                        advantageLogic = state.advantageLogic
                    ))
                },
                onRollAttack = { advantage ->
                    onRoll(DiceRoller.roll(
                        title = "Атака заклинанием",
                        baseModifier = pb + abilityModifier,
                        bonuses = spellSettings.spellAttackBonuses,
                        stats = statsMap,
                        exhaustion = state.exhaustion,
                        sourceType = RollSourceType.ATTACK,
                        advantageType = advantage,
                        advantageLogic = state.advantageLogic
                    ))
                },
                settingsViewModel = state.settingsViewModel,
                isDesktop = isDesktop
            )
        }
        
        if (state.isResourceConfigOpen && state.activeResourceConfig != null && !state.isFullscreenDynamicFieldOpen) {
            ResourceConfigDialog(
                resource = state.activeResourceConfig!!,
                onDismiss = { 
                    state.isResourceConfigOpen = false
                    state.activeResourceConfig = null
                },
                onSave = { updated: DynamicContentBlock.Resource ->
                    state.updateResource(updated)
                    state.isResourceConfigOpen = false
                    state.activeResourceConfig = null
                },
                onDelete = { deleted: DynamicContentBlock.Resource ->
                    state.deleteResource(deleted)
                    state.isResourceConfigOpen = false
                    state.activeResourceConfig = null
                },
                forceBlurEnabled = forceBlurEnabled,
                settingsViewModel = state.settingsViewModel,
                isDesktop = isDesktop,
                hazeState = popupHazeState ?: hazeState,
                isNested = state.isFullscreenDynamicFieldOpen
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
                forceBlurEnabled = effectiveBlurFullscreen,
                onDismiss = { 
                    state.showEnhancedAC = false
                },
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
                onActiveShieldIdChange = { state.activeShieldId = it },
                isDesktop = isDesktop,
                hazeState = hazeState,
                popupHazeState = popupHazeState,
                settingsViewModel = state.settingsViewModel
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
                forceBlurEnabled = effectiveBlurFullscreen,
                onDismiss = { 
                    state.showEnhancedInit = false
                },
                onSubDialogOpenChange = { state.isInitiativeSubDialogOpen = it },
                isDesktop = isDesktop,
                hazeState = hazeState,
                popupHazeState = popupHazeState,
                settingsViewModel = state.settingsViewModel
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
                forceBlurEnabled = effectiveBlurFullscreen,
                onDismiss = { 
                    state.showEnhancedSpeed = false
                },
                onSubDialogOpenChange = { state.isSpeedSubDialogOpen = it },
                isDesktop = isDesktop,
                hazeState = hazeState,
                popupHazeState = popupHazeState,
                settingsViewModel = state.settingsViewModel
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
                forceBlurEnabled = effectiveBlurFullscreen,
                onDismiss = { state.showHealthSettings = false },
                isDesktop = isDesktop,
                hazeState = hazeState,
                popupHazeState = popupHazeState
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
                forceBlurEnabled = effectiveBlurFullscreen,
                onDismiss = { 
                    state.showEnhancedCond = false
                },
                isDesktop = isDesktop,
                hazeState = hazeState,
                popupHazeState = popupHazeState,
                settingsViewModel = state.settingsViewModel
            )
        }
        
        if (state.showCharacterSettings) {
            CharacterSettingsWindow(
                state = state,
                statsMap = statsMap,
                onDismiss = { state.showCharacterSettings = false },
                forceBlurEnabled = blurPopups,
                isDesktop = isDesktop,
                hazeState = hazeState,
                popupHazeState = popupHazeState
            )
        }

        if (state.showHpDialog) {
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
                },
                isDesktop = isDesktop
            )
        }
        
        // Bonus Config Dialog routing
        if (state.isBonusConfigOpen && state.activeBonusConfigAttribute != null) {
             val attr = state.activeBonusConfigAttribute!!
             val skills = when (attr) {
                 Attribute.STRENGTH -> listOf("Атлетика")
                 Attribute.DEXTERITY -> listOf("Акробатика", "Ловкость рук", "Скрытность")
                 Attribute.CONSTITUTION -> emptyList()
                 Attribute.INTELLIGENCE -> listOf("Анализ", "История", "Магия", "Природа", "Религия")
                 Attribute.WISDOM -> listOf("Внимательность", "Выживание", "Медицина", "Проницательность", "Уход за животными")
                 Attribute.CHARISMA -> listOf("Выступление", "Запугивание", "Обман", "Убеждение")
                 else -> emptyList()
             }
             BonusConfigDialog(
                 title = "Бонусы: ${attr.fullName}",
                 attribute = attr,
                 proficiencyBonus = getProficiencyBonus(state.level),
                 attributeModifiers = state.attributeModifiers,
                 initialBaseScore = when(attr) {
                     Attribute.STRENGTH -> state.statsState.strength
                     Attribute.DEXTERITY -> state.statsState.dexterity
                     Attribute.CONSTITUTION -> state.statsState.constitution
                     Attribute.INTELLIGENCE -> state.statsState.intelligence
                     Attribute.WISDOM -> state.statsState.wisdom
                     Attribute.CHARISMA -> state.statsState.charisma
                     else -> "10"
                 },
                 initialStatBonuses = state.statsState.statBonuses,
                 initialIsStatProficient = when(attr) {
                     Attribute.STRENGTH -> state.statsState.strProf
                     Attribute.DEXTERITY -> state.statsState.dexProf
                     Attribute.CONSTITUTION -> state.statsState.conProf
                     Attribute.INTELLIGENCE -> state.statsState.intProf
                     Attribute.WISDOM -> state.statsState.wisProf
                     Attribute.CHARISMA -> state.statsState.chaProf
                     else -> false
                 },
                 initialSkillBonuses = state.statsState.skillBonuses,
                 initialSkillProficiencies = state.statsState.skilledProficiencies,
                 initialSkillExpertise = state.statsState.skilledExpertise,
                 skillsToDisplay = skills,
                 onDismiss = { 
                     state.isBonusConfigOpen = false
                     state.activeBonusConfigAttribute = null
                     state.activeBonusConfigSkill = null
                 },
                 onSave = { baseScore: String, statBonuses: List<StatBonus>, isStatProficient: Boolean, skillBonuses: List<SkillBonus>, skillProficiencies: List<String>, skillExpertise: List<String> ->
                     var ns = state.statsState.copy(
                         statBonuses = statBonuses,
                         skillBonuses = skillBonuses,
                         skilledProficiencies = skillProficiencies,
                         skilledExpertise = skillExpertise
                     )
                     ns = when(attr) {
                         Attribute.STRENGTH -> ns.copy(strProf = isStatProficient, strength = baseScore)
                         Attribute.DEXTERITY -> ns.copy(dexProf = isStatProficient, dexterity = baseScore)
                         Attribute.CONSTITUTION -> ns.copy(conProf = isStatProficient, constitution = baseScore)
                         Attribute.INTELLIGENCE -> ns.copy(intProf = isStatProficient, intelligence = baseScore)
                         Attribute.WISDOM -> ns.copy(wisProf = isStatProficient, wisdom = baseScore)
                         Attribute.CHARISMA -> ns.copy(chaProf = isStatProficient, charisma = baseScore)
                         else -> ns
                     }
                     state.statsState = ns
                     state.isBonusConfigOpen = false
                     state.activeBonusConfigAttribute = null
                     state.activeBonusConfigSkill = null
                 },
                 forceBlurEnabled = forceBlurEnabled,
                 isDesktop = isDesktop,
                 hazeState = popupHazeState ?: hazeState,
                 settingsViewModel = state.settingsViewModel
             )
        }

        if (state.isBonusConfigOpen && state.activeBonusConfigSkill != null) {
            val skillName = state.activeBonusConfigSkill!!
            val attr = when (skillName) {
                "Атлетика" -> Attribute.STRENGTH
                "Акробатика", "Ловкость рук", "Скрытность" -> Attribute.DEXTERITY
                "Анализ", "История", "Магия", "Природа", "Религия" -> Attribute.INTELLIGENCE
                "Внимательность", "Выживание", "Медицина", "Проницательность", "Уход за животными" -> Attribute.WISDOM
                "Выступление", "Запугивание", "Обман", "Убеждение" -> Attribute.CHARISMA
                else -> Attribute.NONE
            }

            BonusConfigDialog(
                title = "Бонусы: $skillName",
                attribute = attr,
                proficiencyBonus = getProficiencyBonus(state.level),
                attributeModifiers = state.attributeModifiers,
                initialBaseScore = "10",
                initialStatBonuses = emptyList<StatBonus>(), // Not needed for single skill
                initialIsStatProficient = false, // Not needed
                initialSkillBonuses = state.statsState.skillBonuses,
                initialSkillProficiencies = state.statsState.skilledProficiencies,
                initialSkillExpertise = state.statsState.skilledExpertise,
                skillsToDisplay = listOf(skillName),
                showStatBonuses = false,
                onDismiss = { 
                    state.isBonusConfigOpen = false
                    state.activeBonusConfigAttribute = null
                    state.activeBonusConfigSkill = null
                },
                onSave = { _: String, _: List<StatBonus>, _: Boolean, skillBonuses: List<SkillBonus>, skillProficiencies: List<String>, skillExpertise: List<String> ->
                    state.statsState = state.statsState.copy(
                        skillBonuses = skillBonuses,
                        skilledProficiencies = skillProficiencies,
                        skilledExpertise = skillExpertise
                    )
                    state.isBonusConfigOpen = false
                    state.activeBonusConfigAttribute = null
                    state.activeBonusConfigSkill = null
                },
                forceBlurEnabled = forceBlurEnabled,
                isDesktop = isDesktop,
                hazeState = popupHazeState ?: hazeState,
                settingsViewModel = state.settingsViewModel
            )
        }
    }
}
