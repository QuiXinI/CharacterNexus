package ru.quasaris.characternexus.ui

import androidx.compose.runtime.*
import ru.quasaris.characternexus.backend.*
import ru.quasaris.characternexus.tabs.*
import ru.quasaris.characternexus.tabs.attacks.*
import ru.quasaris.characternexus.HeaderCode.*
import ru.quasaris.characternexus.*
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.model.Character
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import ru.quasaris.characternexus.util.generateUuid
import characternexus.shared.generated.resources.Res
import characternexus.shared.generated.resources.*

@Composable
fun rememberCharacterDetailState(
    character: Character?,
    settingsViewModel: SettingsViewModel?
): CharacterDetailState {
    return remember(character?.uuid) {
        CharacterDetailState(character, settingsViewModel)
    }
}

class CharacterDetailState(
    val initialCharacter: Character?,
    val settingsViewModel: SettingsViewModel?
) {
    val characterUuid = initialCharacter?.uuid ?: ""
    var name by mutableStateOf(initialCharacter?.name ?: "")
    var characterClass by mutableStateOf(initialCharacter?.characterClass ?: "")
    var race by mutableStateOf(initialCharacter?.race ?: "")
    var classes by mutableStateOf(initialCharacter?.classes ?: emptyList<ClassEntry>())
    var order by mutableStateOf(initialCharacter?.order ?: "")
    var level by mutableStateOf(initialCharacter?.level ?: "1")
    var experience by mutableStateOf(initialCharacter?.experience ?: "50")
    var proficiencyBonus by mutableStateOf(initialCharacter?.proficiencyBonus ?: "[НАСТ БМ]")
    var nextLevelExp by mutableStateOf(getNextLevelThreshold(initialCharacter?.level ?: "1"))

    var selectedConditions by mutableStateOf(initialCharacter?.selectedConditions ?: emptyList())
    var exhaustion by mutableIntStateOf(initialCharacter?.exhaustion ?: 0)
    var hasInspiration by mutableStateOf(initialCharacter?.hasInspiration ?: false)

    var attacks by mutableStateOf(initialCharacter?.attacks ?: emptyList())

    var statsState by mutableStateOf(
        StatsState(
            strength = initialCharacter?.strength ?: "10",
            dexterity = initialCharacter?.dexterity ?: "10",
            constitution = initialCharacter?.constitution ?: "10",
            intelligence = initialCharacter?.intelligence ?: "10",
            wisdom = initialCharacter?.wisdom ?: "10",
            charisma = initialCharacter?.charisma ?: "10",
            strProf = initialCharacter?.strengthProficient ?: false,
            dexProf = initialCharacter?.dexterityProficient ?: false,
            conProf = initialCharacter?.constitutionProficient ?: false,
            intProf = initialCharacter?.intelligenceProficient ?: false,
            wisProf = initialCharacter?.wisdomProficient ?: false,
            chaProf = initialCharacter?.charismaProficient ?: false,
            skilledProficiencies = initialCharacter?.skilledProficiencies ?: emptyList(),
            skilledExpertise = initialCharacter?.skilledExpertise ?: emptyList(),
            statBonuses = initialCharacter?.statBonuses ?: emptyList(),
            skillBonuses = initialCharacter?.skillBonuses ?: emptyList()
        )
    )

    var maxHp by mutableStateOf(initialCharacter?.maxHp ?: "10")
    var currentHp by mutableStateOf(initialCharacter?.currentHp ?: "10")
    var tempHp by mutableStateOf(initialCharacter?.tempHp ?: "0")

    var armorClassEntries by mutableStateOf(initialCharacter?.armorClassEntries ?: listOf(ArmorClassEntry(name = "Базовый КД", formula = "10 + [ЛОВ]")))
    var activeArmorClassId by mutableStateOf(initialCharacter?.activeArmorClassId ?: armorClassEntries.firstOrNull()?.id)
    var acDeleteConfirmId by mutableStateOf<String?>(null)

    var initiativeEntries by mutableStateOf(initialCharacter?.initiativeEntries ?: listOf(InitiativeEntry(name = "Базовая Инициатива", formula = "[ЛОВ]")))
    var activeInitiativeId by mutableStateOf(initialCharacter?.activeInitiativeId ?: initiativeEntries.firstOrNull()?.id)
    var initDeleteConfirmId by mutableStateOf<String?>(null)

    var speedEntries by mutableStateOf(initialCharacter?.speedEntries ?: listOf(SpeedEntry(name = "Базовая Скорость", formula = "30")))
    var activeSpeedId by mutableStateOf(initialCharacter?.activeSpeedId ?: speedEntries.firstOrNull()?.id)
    var speedDeleteConfirmId by mutableStateOf<String?>(null)

    var hitDiceEntries by mutableStateOf(initialCharacter?.hitDiceEntries ?: emptyList())
    var hitDiceMap by mutableStateOf(initialCharacter?.hitDiceMap ?: emptyMap<Int, Int>())
    var defaultHitDie by mutableIntStateOf(initialCharacter?.defaultHitDie ?: 8)
    var hpLevelData by mutableStateOf(initialCharacter?.hpLevelData ?: emptyList<HPLevelEntry>())
    var manualHPLevelData by mutableStateOf(initialCharacter?.manualHPLevelData ?: emptyList<HPLevelEntry>())
    var isMulticlassHP by mutableStateOf(initialCharacter?.isMulticlassHP ?: false)
    var isManualHP by mutableStateOf(initialCharacter?.isManualHP ?: false)
    var manualMaxHp by mutableIntStateOf(initialCharacter?.manualMaxHp ?: 0)
    var manualMaxHitDice by mutableIntStateOf(initialCharacter?.manualMaxHitDice ?: 0)
    var hpBonusesAtLevel by mutableStateOf(initialCharacter?.hpBonusesAtLevel ?: emptyList<AttackBonus>())
    var hpBonusesTotal by mutableStateOf(initialCharacter?.hpBonusesTotal ?: emptyList<AttackBonus>())

    var isShieldActive by mutableStateOf(initialCharacter?.isShieldActive ?: false)
    var shieldEntries by mutableStateOf(initialCharacter?.shieldEntries ?: listOf(ShieldEntry(name = "Базовый Щит", formula = "2")))
    var activeShieldId by mutableStateOf(initialCharacter?.activeShieldId ?: shieldEntries.firstOrNull()?.id)
    var shieldDeleteConfirmId by mutableStateOf<String?>(null)

    var bioShortFields by mutableStateOf(
        if (initialCharacter?.bioShortFields.isNullOrEmpty()) {
            listOf(
                BioShortField(title = "Предыстория", widthRatio = 0.5f),
                BioShortField(title = "Мировоззрение", widthRatio = 0.5f),
                BioShortField(title = "Рост", widthRatio = 0.33f),
                BioShortField(title = "Вес", widthRatio = 0.33f),
                BioShortField(title = "Возраст", widthRatio = 0.33f),
                BioShortField(title = "Кожа", widthRatio = 0.33f),
                BioShortField(title = "Глаза", widthRatio = 0.33f),
                BioShortField(title = "Волосы", widthRatio = 0.33f)
            )
        } else initialCharacter?.bioShortFields!!
    )
    var bioLongSections by mutableStateOf(
        if (initialCharacter?.bioLongSections.isNullOrEmpty()) {
            listOf(
                DynamicNoteState(title = "Предыстория персонажа"),
                DynamicNoteState(title = "Союзники и организации"),
                DynamicNoteState(title = "Враги и организации"),
                DynamicNoteState(title = "Черты характера"),
                DynamicNoteState(title = "Идеалы"),
                DynamicNoteState(title = "Привязанности"),
                DynamicNoteState(title = "Слабости")
            )
        } else initialCharacter?.bioLongSections!!
    )
    var skillsAndTraits by mutableStateOf(
        if (initialCharacter?.skillsAndTraits.isNullOrEmpty()) {
            listOf(
                DynamicNoteState(title = "Умения"),
                DynamicNoteState(title = "Черты", content = "**_Черты происхождения:_**\n\n\n**_Общие черты_**\n")
            )
        } else initialCharacter?.skillsAndTraits!!
    )
    var inventory by mutableStateOf(
        if (initialCharacter?.inventory.isNullOrEmpty()) {
            listOf(
                DynamicNoteState(title = "Снаряжение"),
                DynamicNoteState(title = "Сокровища"),
                DynamicNoteState(title = "Экипировано", content = "\n\n**_Настройки_**\n1. \n2. \n3. ")
            )
        } else initialCharacter?.inventory!!
    )
    var spells by mutableStateOf(
        if (initialCharacter?.spells.isNullOrEmpty()) {
            listOf(DynamicNoteState(title = "Заговоры")) + (1..9).map {
                DynamicNoteState(title = "$it уровень")
            }
        } else initialCharacter?.spells!!
    )
    var spellSettings by mutableStateOf(initialCharacter?.spellSettings ?: SpellSettings())
    var wallet by mutableStateOf(initialCharacter?.wallet ?: Wallet())
    var notes by mutableStateOf(initialCharacter?.notes ?: listOf(DynamicNoteState()))

    var characterImageData by mutableStateOf(initialCharacter?.imageData)
    var themeSeedColorArgb by mutableStateOf(initialCharacter?.themeSeedColorArgb)
    var bytesToCrop by mutableStateOf<ByteArray?>(null)
    var imageToCrop by mutableStateOf<ImageBitmap?>(null)

    // Panels visibility
    var isLevelPanelVisible by mutableStateOf(false)
    var isArmorClassPanelVisible by mutableStateOf(false)
    var isInitiativePanelVisible by mutableStateOf(false)
    var isSpeedPanelVisible by mutableStateOf(false)
    var isConditionsPanelVisible by mutableStateOf(false)
    var isHealthPanelVisible by mutableStateOf(false)
    var isRestPanelVisible by mutableStateOf(false)
    var showRestPopup by mutableStateOf(false)

    // Fullscreen Dialogs visibility
    var showEnhancedAC by mutableStateOf(false)
    var showEnhancedInit by mutableStateOf(false)
    var showEnhancedSpeed by mutableStateOf(false)
    var showEnhancedCond by mutableStateOf(false)
    var showCharacterSettings by mutableStateOf(false)
    var showHealthSettings by mutableStateOf(false)
    var showSpellSettings by mutableStateOf(false)
    var showAvatarMenu by mutableStateOf(false)

    var isBonusConfigOpen by mutableStateOf(false)
    var isAttackConfigOpen by mutableStateOf(false)
    var isSpellEditorOpen by mutableStateOf(false)
    var isMagicBonusSettingsOpen by mutableStateOf(false)
    var isFullscreenDynamicFieldOpen by mutableStateOf(false)
    var isWalletDialogOpen by mutableStateOf(false)
    var isSpellbookSelectionOpen by mutableStateOf(false)

    var isArmorClassSubDialogOpen by mutableStateOf(false)
    var isInitiativeSubDialogOpen by mutableStateOf(false)
    var isSpeedSubDialogOpen by mutableStateOf(false)

    // Settings (moved from Composable to State for simplification)
    @Composable
    fun CollectSettings() {
        if (settingsViewModel == null) return

        val useNewACVal by settingsViewModel.useNewACInterface.collectAsState()
        val useNewInitVal by settingsViewModel.useNewInitInterface.collectAsState()
        val useNewCondVal by settingsViewModel.useNewCondInterface.collectAsState()
        val useNewSpeedVal by settingsViewModel.useNewSpeedInterface.collectAsState()

        val diceFabOffsetXVal by settingsViewModel.diceFabOffsetX.collectAsState()
        val diceFabOffsetYVal by settingsViewModel.diceFabOffsetY.collectAsState()
        val diceFabAlphaSettingVal by settingsViewModel.diceFabAlpha.collectAsState()
        val diceFabBlurEnabledVal by settingsViewModel.diceFabBlurEnabled.collectAsState()
        val masterBlurEnabledVal by settingsViewModel.masterBlurEnabled.collectAsState()
        val diceFabEnabledVal by settingsViewModel.diceFabEnabled.collectAsState()
        val advantageLogicVal by settingsViewModel.advantageLogic.collectAsState()

        LaunchedEffect(
            useNewACVal, useNewInitVal, useNewCondVal, useNewSpeedVal,
            diceFabOffsetXVal, diceFabOffsetYVal, diceFabAlphaSettingVal,
            diceFabBlurEnabledVal, masterBlurEnabledVal, diceFabEnabledVal, advantageLogicVal
        ) {
            if (useNewAC != useNewACVal) useNewAC = useNewACVal
            if (useNewInit != useNewInitVal) useNewInit = useNewInitVal
            if (useNewCond != useNewCondVal) useNewCond = useNewCondVal
            if (useNewSpeed != useNewSpeedVal) useNewSpeed = useNewSpeedVal
            if (diceFabOffsetX != diceFabOffsetXVal) diceFabOffsetX = diceFabOffsetXVal
            if (diceFabOffsetY != diceFabOffsetYVal) diceFabOffsetY = diceFabOffsetYVal
            if (diceFabAlphaSetting != diceFabAlphaSettingVal) diceFabAlphaSetting = diceFabAlphaSettingVal
            if (diceFabBlurEnabled != diceFabBlurEnabledVal) diceFabBlurEnabled = diceFabBlurEnabledVal
            if (masterBlurEnabled != masterBlurEnabledVal) masterBlurEnabled = masterBlurEnabledVal
            if (diceFabEnabled != diceFabEnabledVal) diceFabEnabled = diceFabEnabledVal
            if (advantageLogic != advantageLogicVal) advantageLogic = advantageLogicVal
        }
    }

    var useNewAC by mutableStateOf(true)
    var useNewInit by mutableStateOf(true)
    var useNewCond by mutableStateOf(true)
    var useNewSpeed by mutableStateOf(true)

    var diceFabOffsetX by mutableStateOf(-40f)
    var diceFabOffsetY by mutableStateOf(-40f)
    var diceFabAlphaSetting by mutableStateOf(1.0f)
    var diceFabBlurEnabled by mutableStateOf(true)
    var masterBlurEnabled by mutableStateOf(true)
    var diceFabEnabled by mutableStateOf(true)
    var advantageLogic by mutableStateOf(AdvantageLogic.TOTAL)

    val effectiveDiceFabBlur by derivedStateOf { masterBlurEnabled && diceFabBlurEnabled }
    val effectiveDiceFabAlpha by derivedStateOf { diceFabAlphaSetting }

    // Derived State
    val baseStatsMapForHP by derivedStateOf {
        val totalMaxHD = hitDiceMap.values.sum()
        val totalCurrentHD = totalMaxHD - hitDiceEntries.sumOf { it.spent }
        statsState.toStatsMap(level, proficiencyBonus) + mapOf(
            "manualMaxHitDice" to manualMaxHitDice.toString(),
            "totalMaxHD" to totalMaxHD.toString(),
            "totalCurrentHD" to totalCurrentHD.toString()
        )
    }

    val calculatedMaxHp by derivedStateOf {
        val conMod = evaluateFormula("[CON]", baseStatsMapForHP)
        val levelInt = level.toIntOrNull() ?: 1
        val totalFixedBonus = hpBonusesTotal.filter { it.isActive }.sumOf { evaluateFormula(it.formula, baseStatsMapForHP) }

        if (isManualHP) {
            manualMaxHp + totalFixedBonus
        } else {
            val totalPerLevelBonus = hpBonusesAtLevel.filter { it.isActive }.sumOf { evaluateFormula(it.formula, baseStatsMapForHP) }
            val dataToUse = hpLevelData.take(levelInt)
            val totalRolls = dataToUse.sumOf { it.rollResult ?: 0 }
            totalRolls + (conMod * levelInt) + (totalPerLevelBonus * levelInt) + totalFixedBonus
        }
    }

    val statsMap by derivedStateOf {
        val pbVal = (proficiencyBonus.replace("+", "").toIntOrNull() ?: getProficiencyBonus(level))

        val baseStats = statsState.toStatsMap(level, pbVal.toString()) + ("manualMaxHitDice" to manualMaxHitDice.toString())
        val mutableStats = baseStats.toMutableMap()

        Attribute.entries.forEach { attr ->
            if (attr == Attribute.NONE) return@forEach
            val key = attr.name.lowercase()
            val baseScore = baseStats[key] ?: ""
            
            val characteristicBonuses = statsState.statBonuses.filter { bonus -> 
                bonus.attribute == attr && bonus.type == StatBonusType.CHARACTERISTIC_VALUE && bonus.isActive 
            }
            
            val effScore = if (characteristicBonuses.isEmpty()) {
                baseScore
            } else {
                ru.quasaris.characternexus.tabs.attacks.calculateTotalBonus(
                    bonuses = characteristicBonuses,
                    stats = baseStats,
                    initialValue = baseScore.toIntOrNull() ?: 10
                ).toString()
            }

            mutableStats[key] = effScore
            mutableStats["base_$key"] = baseScore
        }

        mutableStats.apply {
            put("[MAG ATC BON]", spellSettings.spellAttackBonus.ifBlank { "0" })
            put("[МАГ АТК БОН]", spellSettings.spellAttackBonus.ifBlank { "0" })
            put("[MAG SAVE BON]", spellSettings.spellSaveDcBonus.ifBlank { "0" })
            put("[МАГ СПАС БОН]", spellSettings.spellSaveDcBonus.ifBlank { "0" })

            if (spellSettings.spellcastingAbility != Attribute.NONE) {
                val score = get(spellSettings.spellcastingAbility.name.lowercase()) ?: "10"
                val mod = calculateModifier(score)
                put("[mdmg]", mod.toString())
            } else {
                put("[mdmg]", "0")
            }

            put("hp", currentHp)
            put("max_hp", maxHp)
            put("temp_hp", tempHp)
            put("xp", experience)
            put("exhaustion", exhaustion.toString())
            put("conditions", selectedConditions.size.toString())

            if (spellSettings.spellcastingAbility != Attribute.NONE) {
                val score = get(spellSettings.spellcastingAbility.name.lowercase()) ?: "10"
                val mod = calculateModifier(score)
                put("[MAG MOD]", mod.toString())
                put("[МАГ МОД]", mod.toString())
            } else {
                put("[MAG MOD]", "0")
                put("[МАГ МОД]", "0")
            }

            val ac = ru.quasaris.characternexus.backend.CombatCalculations.calculateAC(
                activeArmorClassId, armorClassEntries, this, isShieldActive, activeShieldId, shieldEntries
            )
            put("ac", ac)
        }
    }

    val attributeModifiers by derivedStateOf {
        Attribute.entries.filter { it != Attribute.NONE }.associateWith { attr ->
            calculateModifier(statsMap[attr.name.lowercase()] ?: "10")
        }
    }

    val acValue by derivedStateOf {
        ru.quasaris.characternexus.backend.CombatCalculations.calculateAC(activeArmorClassId, armorClassEntries, statsMap, isShieldActive, activeShieldId, shieldEntries)
    }

    val initValue by derivedStateOf {
        ru.quasaris.characternexus.backend.CombatCalculations.calculateInitiative(activeInitiativeId, initiativeEntries, statsMap, exhaustion)
    }

    val speedValue by derivedStateOf {
        ru.quasaris.characternexus.backend.CombatCalculations.calculateSpeed(activeSpeedId, speedEntries, statsMap, exhaustion)
    }

    val healthStatus by derivedStateOf {
        val c = currentHp.toIntOrNull() ?: 0
        val m = maxHp.toIntOrNull() ?: 0
        when {
            c <= 0 -> "dead"
            m > 0 && (c <= m / 2) -> "bloodied"
            else -> "healthy"
        }
    }

    val healthColor by derivedStateOf {
        when(healthStatus) {
            "dead" -> Color(0xFF454545)
            "bloodied" -> Color(0xFFE57373)
            else -> Color(0xFF00C46F)
        }
    }

    val healthIcon by derivedStateOf {
        when(healthStatus) {
            "dead" -> Res.drawable.ic_health_death
            "bloodied" -> Res.drawable.ic_health_bloodied
            else -> Res.drawable.ic_health
        }
    }

    // HP Dialog
    var hpDialogType by mutableStateOf("")
    var hpDialogValue by mutableStateOf("")
    var showHpDialog by mutableStateOf(false)

    // Modes
    var isEditMode by mutableStateOf(false)
    var isAdvancedMode by mutableStateOf(false)

    fun syncHPAndHitDice() {
        // Sync Max HP
        val calcMax = calculatedMaxHp.toString()
        if (maxHp != calcMax) {
            maxHp = calcMax
        }

        val targetLevel = level.toIntOrNull() ?: 1
        
        // Sync hit dice counters for short rest
        val dataToSync = if (isManualHP) manualHPLevelData else hpLevelData.take(targetLevel)
        val groups = dataToSync.groupBy { it.hitDie }
        
        // Update hitDiceMap
        val newHitDiceMap = groups.mapValues { it.value.size }
        if (newHitDiceMap != hitDiceMap) {
            hitDiceMap = newHitDiceMap
        }

        val newHitDiceEntries = groups.map { (die, list) ->
            val existing = hitDiceEntries.find { it.formula.endsWith("d$die") }
            HitDiceEntry(
                id = existing?.id ?: generateUuid(),
                name = existing?.name ?: "Кости Хитов d$die",
                formula = "${list.size}d$die",
                spent = existing?.spent?.coerceAtMost(list.size) ?: 0
            )
        }.sortedByDescending { 
            it.formula.split('d').lastOrNull()?.toIntOrNull() ?: 0 
        }

        if (newHitDiceEntries != hitDiceEntries) {
            hitDiceEntries = newHitDiceEntries
        }

        if (hpLevelData.size < targetLevel) {
            val newList = hpLevelData.toMutableList()
            for (i in newList.size + 1..targetLevel) {
                val die = if (isMulticlassHP) defaultHitDie else (newList.lastOrNull()?.hitDie ?: defaultHitDie)
                newList.add(HPLevelEntry(level = i, hitDie = die))
            }
            hpLevelData = newList
        }
    }

    fun toCharacter(character: Character): Character {
        return character.copy(
            name = name, characterClass = characterClass, race = race, classes = classes, order = order, level = level, experience = experience,
            imageData = characterImageData, strength = statsState.strength, dexterity = statsState.dexterity,
            constitution = statsState.constitution, intelligence = statsState.intelligence, wisdom = statsState.wisdom,
            charisma = statsState.charisma, strengthProficient = statsState.strProf, dexterityProficient = statsState.dexProf,
            constitutionProficient = statsState.conProf, intelligenceProficient = statsState.intProf,
            wisdomProficient = statsState.wisProf, charismaProficient = statsState.chaProf,
            maxHp = maxHp, currentHp = currentHp, tempHp = tempHp, proficiencyBonus = proficiencyBonus,
            selectedConditions = selectedConditions, exhaustion = exhaustion, attacks = attacks,
            armorClassEntries = armorClassEntries, activeArmorClassId = activeArmorClassId,
            initiativeEntries = initiativeEntries, activeInitiativeId = activeInitiativeId,
            speedEntries = speedEntries, activeSpeedId = activeSpeedId, isShieldActive = isShieldActive,
            shieldEntries = shieldEntries, activeShieldId = activeShieldId,
            skilledProficiencies = statsState.skilledProficiencies, skilledExpertise = statsState.skilledExpertise,
            statBonuses = statsState.statBonuses, skillBonuses = statsState.skillBonuses,
            themeSeedColorArgb = themeSeedColorArgb, notes = notes, skillsAndTraits = skillsAndTraits,
            inventory = inventory, spells = spells, spellSettings = spellSettings, wallet = wallet,
            bioShortFields = bioShortFields, bioLongSections = bioLongSections, hitDiceEntries = hitDiceEntries,
            hitDiceMap = hitDiceMap,
            defaultHitDie = defaultHitDie, hpLevelData = hpLevelData, manualHPLevelData = manualHPLevelData,
            isMulticlassHP = isMulticlassHP,
            isManualHP = isManualHP, manualMaxHp = manualMaxHp,
            manualMaxHitDice = manualMaxHitDice,
            hpBonusesAtLevel = hpBonusesAtLevel, hpBonusesTotal = hpBonusesTotal,
            hasInspiration = hasInspiration
        )
    }

    fun handleRestoration(restType: String, statsMap: Map<String, String>) {
        val updateNote = { note: DynamicNoteState ->
            val blocks = DynamicContentParser.parse(note.content)
            val updatedBlocks = blocks.map { block ->
                if (block is DynamicContentBlock.Resource) {
                    val recovery = when (restType) {
                        "short" -> block.shortRest
                        "long" -> block.longRest
                        "dawn" -> block.dawnRest
                        else -> "0"
                    }
                    val actualRecovery = if (restType == "long" && recovery == "0") block.shortRest else recovery
                    if (actualRecovery == "0") return@map block
                    val maxVal = evaluateFormula(block.max, statsMap)
                    val curVal = block.current.toIntOrNull() ?: 0
                    val amount = if (actualRecovery.lowercase() == "all" || actualRecovery.lowercase() == "все") {
                        maxVal
                    } else {
                        val (flat, dice) = parseFormulaParts(actualRecovery, statsMap)
                        var rolled = flat
                        dice.forEach { part ->
                            val sides = part.sides
                            val count = kotlin.math.abs(part.count)
                            val sign = if (part.count >= 0) 1 else -1
                            repeat(count) {
                                rolled += (1..sides).random() * sign
                            }
                        }
                        rolled
                    }
                    val newCur = if (actualRecovery.lowercase() == "all" || actualRecovery.lowercase() == "все") {
                        maxVal
                    } else {
                        minOf(maxVal, curVal + amount)
                    }
                    block.copy(current = newCur.toString())
                } else block
            }
            note.copy(content = DynamicContentParser.render(updatedBlocks))
        }

        notes = notes.map { updateNote(it) }
        skillsAndTraits = skillsAndTraits.map { updateNote(it) }
        inventory = inventory.map { updateNote(it) }
        spells = spells.map { updateNote(it) }

        when (restType) {
            "long" -> {
                spellSettings = spellSettings.copy(
                    usedSlots = emptyMap(),
                    usedSlotsShortRest = emptyMap(),
                    specialSlots = spellSettings.specialSlots.map { it.copy() }
                )
            }
            "short" -> {
                spellSettings = spellSettings.copy(
                    usedSlotsShortRest = emptyMap(),
                    specialSlots = spellSettings.specialSlots.map {
                        if (it.restoreOnShortRest) it.copy() else it
                    }
                )
            }
            "dawn" -> {
                spellSettings = spellSettings.copy(
                    usedSlotsDawn = emptyMap(),
                    specialSlots = spellSettings.specialSlots.map {
                        if (it.restoreOnDawn) it.copy() else it
                    }
                )
            }
        }

        if (restType == "long") {
            currentHp = maxHp
            tempHp = "0"
            hitDiceEntries = hitDiceEntries.map { entry ->
                val maxHD = evaluateFormula(entry.formula, statsMap)
                val recover = maxOf(1, maxHD / 2)
                entry.copy(spent = maxOf(0, entry.spent - recover))
            }
            if (exhaustion > 0) exhaustion--
        }
    }

    fun syncIdentity() {
        if (classes.isEmpty()) return

        val totalLevel = classes.sumOf { it.level }
        level = totalLevel.toString()

        characterClass = classes.joinToString(" / ") {
            if (it.subclass.isNotBlank()) "${it.className.displayName} (${it.subclass}) ${it.level}"
            else "${it.className.displayName} ${it.level}"
        }

        // Sync HP Level Data
        val newHpLevelData = mutableListOf<HPLevelEntry>()
        var currentLvl = 1
        classes.forEach { entry ->
            val die = when (entry.className) {
                CharacterClass.BARBARIAN -> 12
                CharacterClass.FIGHTER -> 10
                CharacterClass.ARTIFICER -> 8
                CharacterClass.WIZARD -> 6
                else -> 8
            }
            for (i in 1..entry.level) {
                // Try to preserve existing roll results if the level and hit die match
                val existing = hpLevelData.getOrNull(currentLvl - 1)
                newHpLevelData.add(
                    HPLevelEntry(
                        level = currentLvl,
                        hitDie = die,
                        rollResult = if (existing?.hitDie == die) existing.rollResult else null,
                        manualValue = if (existing?.hitDie == die) existing.manualValue else null
                    )
                )
                currentLvl++
            }
        }
        hpLevelData = newHpLevelData

        // Sync Spell Settings
        var fullLevels = 0
        var halfLevels = 0
        var thirdLevels = 0
        
        classes.forEach { entry ->
            when (entry.className) {
                CharacterClass.WIZARD -> fullLevels += entry.level
                CharacterClass.ARTIFICER -> halfLevels += entry.level
                else -> {} // Fighter/Barbarian no slots by default
            }
        }

        spellSettings = spellSettings.copy(
            fullCasterLevel = fullLevels,
            halfCasterLevel = halfLevels,
            thirdCasterLevel = thirdLevels,
            isMulticlass = classes.size > 1,
            casterType = if (classes.size == 1) {
                when (classes[0].className) {
                    CharacterClass.WIZARD -> CasterType.FULL
                    CharacterClass.ARTIFICER -> CasterType.HALF
                    else -> CasterType.NONE
                }
            } else spellSettings.casterType
        )
    }
}
