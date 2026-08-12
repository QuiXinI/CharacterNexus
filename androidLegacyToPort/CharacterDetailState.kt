package ru.quasaris.characters.master

import android.content.Context
import androidx.compose.runtime.*
import ru.quasaris.characters.master.backend.*
import ru.quasaris.characters.master.tabs.*
import ru.quasaris.characters.master.tabs.attacks.*
import ru.quasaris.characters.master.HeaderCode.*
import java.util.UUID
import kotlin.reflect.KProperty

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
    var bitmapToCrop by mutableStateOf<android.graphics.Bitmap?>(null)

    // Panels visibility
    var isLevelPanelVisible by mutableStateOf(false)
    var isArmorClassPanelVisible by mutableStateOf(false)
    var isInitiativePanelVisible by mutableStateOf(false)
    var isSpeedPanelVisible by mutableStateOf(false)
    var isConditionsPanelVisible by mutableStateOf(false)
    var isHealthPanelVisible by mutableStateOf(false)
    var isRestPanelVisible by mutableStateOf(false)

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

    // HP Dialog
    var hpDialogType by mutableStateOf("")
    var hpDialogValue by mutableStateOf("")
    var showHpDialog by mutableStateOf(false)

    // Modes
    var isEditMode by mutableStateOf(false)
    var isAdvancedMode by mutableStateOf(false)

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
