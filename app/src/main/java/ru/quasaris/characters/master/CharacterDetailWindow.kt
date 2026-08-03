package ru.quasaris.characters.master

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.quasaris.characters.master.tabs.*
import ru.quasaris.characters.master.MainWindow.*
import ru.quasaris.characters.master.HeaderCode.*
import ru.quasaris.characters.master.tabs.attacks.AttacksTab
import ru.quasaris.characters.master.backend.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.LocalHazeStyle
import ru.quasaris.characters.master.tabs.spells.SpellSettingsDialog
import ru.quasaris.characters.master.tabs.spells.SpellsTab
import ru.quasaris.characters.master.ui.DiceRollingFab
import ru.quasaris.characters.master.ui.cropper.AvatarCropperWindow
import ru.quasaris.characters.master.ui.RestPopup
import ru.quasaris.characters.master.ui.RestPanel
import androidx.compose.ui.draw.scale
import java.util.UUID

/**
 * Стиль размытия для нижней панели выбора вкладок.
 */
val TabSheetHazeStyle = HazeStyle(
    blurRadius = 24.dp,
    tints = listOf(HazeTint(Color.Black.copy(alpha = 0.25f)))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterDetailWindow(
    character: Character?,
    onNavigateBack: () -> Unit,
    onDeleteCharacter: (Character) -> Unit,
    onSaveChanges: (Character) -> Unit,
    onOpenDrawer: () -> Unit = {},
    onRoll: (RollResult) -> Unit = {},
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    blurPopups: Boolean = false,
    settingsViewModel: SettingsViewModel? = null
) {
    val useNewAC by settingsViewModel?.useNewACInterface?.collectAsState() ?: remember { mutableStateOf(true) }
    val useNewInit by settingsViewModel?.useNewInitInterface?.collectAsState() ?: remember { mutableStateOf(true) }
    val useNewCond by settingsViewModel?.useNewCondInterface?.collectAsState() ?: remember { mutableStateOf(true) }
    val useNewSpeed by settingsViewModel?.useNewSpeedInterface?.collectAsState() ?: remember { mutableStateOf(true) }

    var name by remember { mutableStateOf(character?.name ?: "") }
    var characterClass by remember { mutableStateOf(character?.characterClass ?: "") }
    var order by remember { mutableStateOf(character?.order ?: "") }
    var level by remember { mutableStateOf(character?.level ?: "1") }
    var experience by remember { mutableStateOf(character?.experience ?: "50") }
    var proficiencyBonus by remember { mutableStateOf(character?.proficiencyBonus ?: "[НАСТ БМ]") }
    var nextLevelExp by remember { mutableStateOf(getNextLevelThreshold(character?.level ?: "1")) }

    LaunchedEffect(level) {
        nextLevelExp = getNextLevelThreshold(level)
    }

    var selectedConditions by remember { mutableStateOf(character?.selectedConditions ?: emptyList()) }
    var exhaustion by remember { mutableIntStateOf(character?.exhaustion ?: 0) }
    var hasInspiration by remember { mutableStateOf(character?.hasInspiration ?: false) }

    val diceFabOffsetX by settingsViewModel?.diceFabOffsetX?.collectAsState() ?: remember { mutableStateOf(-40f) }
    val diceFabOffsetY by settingsViewModel?.diceFabOffsetY?.collectAsState() ?: remember { mutableStateOf(-40f) }
    val diceFabAlphaSetting by settingsViewModel?.diceFabAlpha?.collectAsState() ?: remember { mutableStateOf(1.0f) }
    val diceFabBlurEnabled by settingsViewModel?.diceFabBlurEnabled?.collectAsState() ?: remember { mutableStateOf(true) }
    val masterBlurEnabled by settingsViewModel?.masterBlurEnabled?.collectAsState() ?: remember { mutableStateOf(true) }

    val effectiveDiceFabBlur = masterBlurEnabled && diceFabBlurEnabled
    val effectiveDiceFabAlpha = diceFabAlphaSetting

    val advantageLogic by settingsViewModel?.advantageLogic?.collectAsState() ?: remember { mutableStateOf(AdvantageLogic.TOTAL) }

    var attacks by remember { mutableStateOf(character?.attacks ?: emptyList()) }

    var statsState by remember {
        mutableStateOf(
            StatsState(
                strength = character?.strength ?: "10",
                dexterity = character?.dexterity ?: "10",
                constitution = character?.constitution ?: "10",
                intelligence = character?.intelligence ?: "10",
                wisdom = character?.wisdom ?: "10",
                charisma = character?.charisma ?: "10",
                strProf = character?.strengthProficient ?: false,
                dexProf = character?.dexterityProficient ?: false,
                conProf = character?.constitutionProficient ?: false,
                intProf = character?.intelligenceProficient ?: false,
                wisProf = character?.wisdomProficient ?: false,
                chaProf = character?.charismaProficient ?: false,
                skilledProficiencies = character?.skilledProficiencies ?: emptyList(),
                skilledExpertise = character?.skilledExpertise ?: emptyList(),
                statBonuses = character?.statBonuses ?: emptyList(),
                skillBonuses = character?.skillBonuses ?: emptyList()
            )
        )
    }

    var maxHp by remember { mutableStateOf(character?.maxHp ?: "10") }
    var currentHp by remember { mutableStateOf(character?.currentHp ?: "10") }
    var tempHp by remember { mutableStateOf(character?.tempHp ?: "0") }

    var isLevelPanelVisible by remember { mutableStateOf(false) }
    var isArmorClassPanelVisible by remember { mutableStateOf(false) }
    var isInitiativePanelVisible by remember { mutableStateOf(false) }
    var isSpeedPanelVisible by remember { mutableStateOf(false) }
    var isConditionsPanelVisible by remember { mutableStateOf(false) }
    var isHealthPanelVisible by remember { mutableStateOf(false) }
    var isRestPanelVisible by remember { mutableStateOf(false) }

    var showEnhancedAC by remember { mutableStateOf(false) }
    var showEnhancedInit by remember { mutableStateOf(false) }
    var showEnhancedSpeed by remember { mutableStateOf(false) }
    var showEnhancedCond by remember { mutableStateOf(false) }
    var showHealthSettings by remember { mutableStateOf(false) }

    var hpDialogType by remember { mutableStateOf("") }
    var hpDialogValue by remember { mutableStateOf("") }
    var showHpDialog by remember { mutableStateOf(false) }

    var armorClassEntries by remember { mutableStateOf(character?.armorClassEntries ?: listOf(ArmorClassEntry(name = "Базовый КД", formula = "10 + [ЛОВ]"))) }
    var activeArmorClassId by remember { mutableStateOf(character?.activeArmorClassId ?: armorClassEntries.firstOrNull()?.id) }
    var acDeleteConfirmId by remember { mutableStateOf<String?>(null) }

    var initiativeEntries by remember { mutableStateOf(character?.initiativeEntries ?: listOf(InitiativeEntry(name = "Базовая Инициатива", formula = "[ЛОВ]"))) }
    var activeInitiativeId by remember { mutableStateOf(character?.activeInitiativeId ?: initiativeEntries.firstOrNull()?.id) }
    var initDeleteConfirmId by remember { mutableStateOf<String?>(null) }

    var speedEntries by remember { mutableStateOf(character?.speedEntries ?: listOf(SpeedEntry(name = "Базовая Скорость", formula = "30"))) }
    var activeSpeedId by remember { mutableStateOf(character?.activeSpeedId ?: speedEntries.firstOrNull()?.id) }
    var speedDeleteConfirmId by remember { mutableStateOf<String?>(null) }

    var hitDiceEntries by remember { mutableStateOf(character?.hitDiceEntries ?: emptyList()) }

    var isShieldActive by remember { mutableStateOf(character?.isShieldActive ?: false) }
    var shieldEntries by remember { mutableStateOf(character?.shieldEntries ?: listOf(ShieldEntry(name = "Базовый Щит", formula = "2"))) }
    var activeShieldId by remember { mutableStateOf(character?.activeShieldId ?: shieldEntries.firstOrNull()?.id) }
    var shieldDeleteConfirmId by remember { mutableStateOf<String?>(null) }

    var showSpellSettings by remember { mutableStateOf(false) }

    var bioShortFields by remember { mutableStateOf(
        if (character?.bioShortFields.isNullOrEmpty()) {
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
        } else character?.bioShortFields!!
    ) }
    var bioLongSections by remember { mutableStateOf(
        if (character?.bioLongSections.isNullOrEmpty()) {
            listOf(
                DynamicNoteState(title = "Предыстория персонажа"),
                DynamicNoteState(title = "Союзники и организации"),
                DynamicNoteState(title = "Враги и организации"),
                DynamicNoteState(title = "Черты характера"),
                DynamicNoteState(title = "Идеалы"),
                DynamicNoteState(title = "Привязанности"),
                DynamicNoteState(title = "Слабости")
            )
        } else character?.bioLongSections!!
    ) }
    var skillsAndTraits by remember { mutableStateOf(
        if (character?.skillsAndTraits.isNullOrEmpty()) {
            listOf(
                DynamicNoteState(title = "Умения"),
                DynamicNoteState(title = "Черты", content = "**_Черты происхождения:_**\n\n\n**_Общие черты_**\n")
            )
        } else character?.skillsAndTraits!!
    ) }
    var inventory by remember { mutableStateOf(
        if (character?.inventory.isNullOrEmpty()) {
            listOf(
                DynamicNoteState(title = "Снаряжение"),
                DynamicNoteState(title = "Сокровища"),
                DynamicNoteState(title = "Экипировано", content = "\n\n**_Настройки_**\n1. \n2. \n3. ")
            )
        } else character?.inventory!!
    ) }
    var spells by remember { mutableStateOf(
        if (character?.spells.isNullOrEmpty()) {
            listOf(DynamicNoteState(title = "Заговоры")) + (1..9).map {
                DynamicNoteState(title = "$it уровень")
            }
        } else character?.spells!!
    ) }
    var spellSettings by remember { mutableStateOf(character?.spellSettings ?: SpellSettings()) }
    var wallet by remember { mutableStateOf(character?.wallet ?: Wallet()) }
    var notes by remember { mutableStateOf(character?.notes ?: listOf(DynamicNoteState())) }

    var characterImageData by remember { mutableStateOf(character?.imageData) }
    var themeSeedColorArgb by remember { mutableStateOf(character?.themeSeedColorArgb) }
    var showAvatarMenu by remember { mutableStateOf(false) }
    var bitmapToCrop by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    val fileCreatorLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/${ArchiveManager.EXPORT_EXTENSION}")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val currentCharacterState = character!!.copy(
            name = name, characterClass = characterClass, order = order, level = level, experience = experience,
            strength = statsState.strength, dexterity = statsState.dexterity, constitution = statsState.constitution,
            intelligence = statsState.intelligence, wisdom = statsState.wisdom, charisma = statsState.charisma,
            attacks = attacks,
            strengthProficient = statsState.strProf, dexterityProficient = statsState.dexProf, constitutionProficient = statsState.conProf,
            intelligenceProficient = statsState.intProf, wisdomProficient = statsState.wisProf, charismaProficient = statsState.chaProf,
            maxHp = maxHp, currentHp = currentHp, tempHp = tempHp,
            proficiencyBonus = proficiencyBonus,
            selectedConditions = selectedConditions, exhaustion = exhaustion,
            armorClassEntries = armorClassEntries, activeArmorClassId = activeArmorClassId,
            initiativeEntries = initiativeEntries, activeInitiativeId = activeInitiativeId,
            speedEntries = speedEntries, activeSpeedId = activeSpeedId,
            isShieldActive = isShieldActive, shieldEntries = shieldEntries, activeShieldId = activeShieldId,
            skilledProficiencies = statsState.skilledProficiencies, skilledExpertise = statsState.skilledExpertise,
            statBonuses = statsState.statBonuses, skillBonuses = statsState.skillBonuses,
            imageData = characterImageData, themeSeedColorArgb = themeSeedColorArgb,
            notes = notes,
            skillsAndTraits = skillsAndTraits,
            inventory = inventory,
            spells = spells,
            spellSettings = spellSettings,
            wallet = wallet
        )
        scope.launch {
            ArchiveManager.exportCharacter(context, currentCharacterState, uri)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    bitmapToCrop = bitmap
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    if (character == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Персонаж не найден!", color = colorScheme.onBackground)
        }
        return
    }

    val statsMap = remember(statsState, level, proficiencyBonus, spellSettings, currentHp, maxHp, tempHp, exhaustion, selectedConditions, activeArmorClassId, armorClassEntries, isShieldActive, activeShieldId, shieldEntries) {
        val pbVal = (proficiencyBonus.replace("+", "").toIntOrNull() ?: getProficiencyBonus(level))
        
        val baseStats = statsState.toStatsMap(level, pbVal.toString())
        val mutableStats = baseStats.toMutableMap()
        
        Attribute.entries.forEach { attr ->
            if (attr == Attribute.NONE) return@forEach
            val key = attr.name.lowercase()
            val baseScore = baseStats[key] ?: "10"
            val effScore = ru.quasaris.characters.master.tabs.attacks.calculateTotalBonus(
                bonuses = statsState.statBonuses.filter { it.attribute == attr && it.type == StatBonusType.CHARACTERISTIC_VALUE },
                stats = baseStats,
                initialValue = baseScore.toIntOrNull() ?: 10
            ).toString()
            
            mutableStats[key] = effScore
            mutableStats["base_$key"] = baseScore
        }

        mutableStats.apply {
            put("[MAG ATC BON]", spellSettings.spellAttackBonus.ifBlank { "0" })
            put("[МАГ АТК БОН]", spellSettings.spellAttackBonus.ifBlank { "0" })
            put("[MAG SAVE BON]", spellSettings.spellSaveDcBonus.ifBlank { "0" })
            put("[МАГ СПАС БОН]", spellSettings.spellSaveDcBonus.ifBlank { "0" })

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

            val ac = CombatCalculations.calculateAC(
                activeArmorClassId, armorClassEntries, this, isShieldActive, activeShieldId, shieldEntries
            )
            put("ac", ac)
        }
    }

    val attributeModifiers = remember(statsMap) {
        Attribute.entries.filter { it != Attribute.NONE }.associateWith { attr ->
            calculateModifier(statsMap[attr.name.lowercase()] ?: "10")
        }
    }
    val pb = getProficiencyBonus(level)

    val acValue = CombatCalculations.calculateAC(activeArmorClassId, armorClassEntries, statsMap, isShieldActive, activeShieldId, shieldEntries)
    val initValue = CombatCalculations.calculateInitiative(activeInitiativeId, initiativeEntries, statsMap, exhaustion)
    val speedValue = CombatCalculations.calculateSpeed(activeSpeedId, speedEntries, statsMap, exhaustion)

    val healthState = remember(currentHp, maxHp) {
        val c = currentHp.toIntOrNull() ?: 0; val m = maxHp.toIntOrNull() ?: 0
        when { c <= 0 -> "dead"; m > 0 && (c <= m / 2) -> "bloodied"; else -> "healthy" }
    }
    val healthColor = when(healthState) { "dead" -> Color(0xFF454545); "bloodied" -> Color(0xFFE57373); else -> Color(0xFF00C46F) }
    val healthIcon = when(healthState) { "dead" -> R.drawable.ic_health_death; "bloodied" -> R.drawable.ic_health_bloodied; else -> R.drawable.ic_health }
    val allConditions = rememberAllConditions(context)

    val tabs = CharacterTab.entries
    val totalPages = 10000
    val initialPage = totalPages / 2 - (totalPages / 2 % tabs.size)
    val pagerState = rememberPagerState(initialPage = initialPage) { totalPages }
    var showTabSheet by remember { mutableStateOf(false) }
    val currentTab = tabs[pagerState.currentPage % tabs.size]
    val sheetState = rememberModalBottomSheetState()
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    var isEditMode by remember { mutableStateOf(false) }
    var isAdvancedMode by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        isEditMode = false
        focusManager.clearFocus()
    }

    val saveCurrentCharacter = {
        onSaveChanges(character.copy(
            name = name, characterClass = characterClass, order = order, level = level, experience = experience,
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
            defaultHitDie = character.defaultHitDie, hasInspiration = hasInspiration
        ))
    }

    LaunchedEffect(
        name, characterClass, order, level, experience, characterImageData,
        statsState, maxHp, currentHp, tempHp, proficiencyBonus, selectedConditions, exhaustion,
        attacks, armorClassEntries, activeArmorClassId, initiativeEntries,
        activeInitiativeId, speedEntries, activeSpeedId, isShieldActive,
        shieldEntries, activeShieldId, themeSeedColorArgb, notes,
        skillsAndTraits, inventory, spells, spellSettings, wallet,
        bioShortFields, bioLongSections, hitDiceEntries, hasInspiration
    ) {
        saveCurrentCharacter()
    }

    val handleRestoration = { restType: String ->
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
                        val (flat, dice) = ru.quasaris.characters.master.backend.parseFormulaParts(actualRecovery, statsMap)
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

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            Column(modifier = Modifier.background(colorScheme.surface)) {
                CharacterHeader(
                    name = name, onNameChange = { name = it },
                    level = level, experience = experience, nextLevelExp = nextLevelExp,
                    characterImageData = characterImageData,
                    onAvatarClick = { showAvatarMenu = true },
                    onLevelClick = {
                        isLevelPanelVisible = !isLevelPanelVisible
                    },
                    onOpenDrawer = onOpenDrawer,
                    activeACValue = acValue,
                    onACClick = { isShieldActive = !isShieldActive },
                    onACLongClick = {
                        if (useNewAC) {
                            showEnhancedAC = true
                        } else {
                            isArmorClassPanelVisible = !isArmorClassPanelVisible
                        }
                    },
                    isShieldActive = isShieldActive,
                    activeInitValue = initValue,
                    onInitClick = {
                        val baseInit = (initValue.replace("+", "").toIntOrNull() ?: 0) + (exhaustion * 2)
                        val activeEntry = initiativeEntries.find { it.id == activeInitiativeId }
                        val advantage = if (activeEntry?.hasAdvantage == true) AdvantageType.ADVANTAGE else AdvantageType.NONE
                        onRoll(DiceRoller.roll("Инициатива", baseInit, bonuses = activeEntry?.bonuses ?: emptyList(), stats = statsMap, exhaustion = exhaustion, sourceType = RollSourceType.ABILITY, advantageType = advantage, advantageLogic = advantageLogic))
                    },
                    onInitLongClick = {
                        if (useNewInit) {
                            showEnhancedInit = true
                        } else {
                            isInitiativePanelVisible = !isInitiativePanelVisible
                        }
                    },
                    currentHp = currentHp, maxHp = maxHp, tempHp = tempHp,
                    healthColor = healthColor, healthIcon = healthIcon,
                    onHealthClick = {
                        isHealthPanelVisible = !isHealthPanelVisible
                    },
                    conditionsCount = exhaustion.toString(),
                    selectedConditions = selectedConditions,
                    onConditionsClick = {
                        if (useNewCond) {
                            showEnhancedCond = true
                        } else {
                            isConditionsPanelVisible = !isConditionsPanelVisible
                        }
                    },
                    activeSpeedValue = speedValue,
                    onSpeedClick = {
                        if (useNewSpeed) {
                            showEnhancedSpeed = true
                        } else {
                            isSpeedPanelVisible = !isSpeedPanelVisible
                        }
                    },
                    showAvatarMenu = showAvatarMenu,
                    onDismissAvatarMenu = { showAvatarMenu = false },
                    onImagePickerClick = { imagePickerLauncher.launch("image/*"); showAvatarMenu = false },
                    onDownloadClick = { fileCreatorLauncher.launch("MP_${name}.${ArchiveManager.EXPORT_EXTENSION}"); showAvatarMenu = false },
                    selectedImageUri = null,
                    onNavigateBack = onNavigateBack,
                    exhaustion = exhaustion,
                    hasInspiration = hasInspiration,
                    onInspirationChange = { hasInspiration = it },
                    onShortRest = { 
                        isRestPanelVisible = !isRestPanelVisible
                    },
                    onLongRest = { handleRestoration("long") },
                    onDawn = { handleRestoration("dawn") },
                    hazeState = hazeState,
                    blurPopups = blurPopups
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val hasContentToEdit = when(currentTab) {
                        CharacterTab.ATTACKS -> attacks.isNotEmpty()
                        CharacterTab.NOTES -> notes.isNotEmpty()
                        CharacterTab.SKILLS_FEATS -> skillsAndTraits.isNotEmpty()
                        CharacterTab.INVENTORY -> inventory.isNotEmpty()
                        CharacterTab.SPELLS -> spells.isNotEmpty()
                        CharacterTab.BIO -> true
                        else -> false
                    }

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (currentTab == CharacterTab.SPELLS) {
                            IconButton(
                                onClick = { showSpellSettings = true },
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.AutoFixHigh,
                                    contentDescription = "Spell Settings",
                                    tint = colorScheme.primary
                                )
                            }
                        } else if (currentTab == CharacterTab.STATS) {
                            IconButton(
                                onClick = { isAdvancedMode = !isAdvancedMode },
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isAdvancedMode) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                                    contentDescription = "Toggle Advanced Mode",
                                    tint = colorScheme.primary
                                )
                            }
                        }
                    }

                    Surface(
                        color = colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showTabSheet = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentTab.title.uppercase(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val collapsibleTabs = listOf(
                                CharacterTab.SKILLS_FEATS,
                                CharacterTab.INVENTORY,
                                CharacterTab.SPELLS,
                                CharacterTab.NOTES
                            )
                            if (currentTab in collapsibleTabs) {
                                val currentList = when (currentTab) {
                                    CharacterTab.SKILLS_FEATS -> skillsAndTraits
                                    CharacterTab.INVENTORY -> inventory
                                    CharacterTab.SPELLS -> spells
                                    CharacterTab.NOTES -> notes
                                    else -> emptyList()
                                }
                                val anyCollapsed = currentList.any { !it.isExpanded }

                                IconButton(
                                    onClick = {
                                        val newState = if (anyCollapsed) {
                                            currentList.map { it.copy(isExpanded = true) }
                                        } else {
                                            currentList.map { it.copy(isExpanded = false) }
                                        }
                                        when (currentTab) {
                                            CharacterTab.SKILLS_FEATS -> skillsAndTraits = newState
                                            CharacterTab.INVENTORY -> inventory = newState
                                            CharacterTab.SPELLS -> spells = newState
                                            CharacterTab.NOTES -> notes = newState
                                            else -> {}
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (anyCollapsed) Icons.Default.UnfoldMore else Icons.Default.UnfoldLess,
                                        contentDescription = "Toggle All Expansion",
                                        tint = colorScheme.primary
                                    )
                                }
                            }

                            if (hasContentToEdit) {
                                IconButton(
                                    onClick = { isEditMode = !isEditMode },
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Icon(
                                        if (isEditMode) Icons.Default.EditOff else Icons.Default.Edit,
                                        contentDescription = "Toggle Edit Mode",
                                        tint = if (isEditMode) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(colorScheme.background)) {

            Column {
                val totalMaxHitDice = hitDiceEntries.sumOf { evaluateFormula(it.formula.split('d').firstOrNull() ?: "0", statsMap) }.coerceAtLeast(level.toIntOrNull() ?: 1)
                val totalSpentHitDice = hitDiceEntries.sumOf { it.spent }

                ExpandingPanelsSection(
                    isLevelPanelVisible = isLevelPanelVisible, level = level, onLevelChange = { level = it },
                    experience = experience, onExpChange = { experience = it },
                    proficiencyBonus = proficiencyBonus, onProfChange = { proficiencyBonus = it },
                    nextLevelExp = nextLevelExp, statsMap = statsMap,
                    isHealthPanelVisible = isHealthPanelVisible, maxHp = maxHp, onMaxHpChange = { maxHp = it },
                    tempHp = tempHp, onTempHpChange = { tempHp = it },
                    currentHp = currentHp, onCurrentHpChange = { currentHp = it },
                    onHealClick = { hpDialogType = "heal"; hpDialogValue = ""; showHpDialog = true },
                    onDamageClick = { hpDialogType = "damage"; hpDialogValue = ""; showHpDialog = true },
                    onTempClick = { hpDialogType = "temp"; hpDialogValue = ""; showHpDialog = true },
                    healthColor = healthColor, clampHp = { },
                    spentHitDice = totalSpentHitDice,
                    maxHitDice = totalMaxHitDice,
                    onSpentHitDiceChange = { newSpent ->
                        if (hitDiceEntries.isEmpty()) {
                            hitDiceEntries = listOf(HitDiceEntry(name = "Кости Хитов", formula = "[LVL]d${character?.defaultHitDie ?: 8}", spent = newSpent))
                        } else {
                            val diff = newSpent - totalSpentHitDice
                            if (diff != 0) {
                                val newList = hitDiceEntries.toMutableList()
                                val first = newList[0]
                                val maxFirst = evaluateFormula(first.formula.split('d').firstOrNull() ?: "0", statsMap)
                                newList[0] = first.copy(spent = (first.spent + diff).coerceIn(0, maxFirst))
                                hitDiceEntries = newList
                            }
                        }
                    },
                    onOpenHealthSettings = { showHealthSettings = true },
                    isRestPanelVisible = isRestPanelVisible,
                    onRestPanelDismiss = { isRestPanelVisible = false },
                    hitDiceEntries = hitDiceEntries,
                    onHitDiceEntriesChange = { hitDiceEntries = it },
                    onHealAmount = { amount ->
                        currentHp = minOf(maxHp.toIntOrNull() ?: 0, (currentHp.toIntOrNull() ?: 0) + amount).toString()
                    },
                    onShortRestConfirmed = {
                        handleRestoration("short")
                        isRestPanelVisible = false
                    },
                    defaultHitDie = character?.defaultHitDie ?: 8,
                    isArmorClassPanelVisible = isArmorClassPanelVisible, armorClassEntries = armorClassEntries,
                    activeArmorClassId = activeArmorClassId, acDeleteConfirmId = acDeleteConfirmId,
                    onArmorClassEntries = { armorClassEntries = it }, onActiveArmorClass = { activeArmorClassId = it },
                    onAcDeleteReq = { acDeleteConfirmId = it }, onAddArmorClass = { armorClassEntries = armorClassEntries + ArmorClassEntry() },
                    isInitiativePanelVisible = isInitiativePanelVisible, initiativeEntries = initiativeEntries,
                    activeInitiativeId = activeInitiativeId, initDeleteConfirmId = initDeleteConfirmId,
                    onInitiativeEntries = { initiativeEntries = it }, onActiveInitiative = { activeInitiativeId = it },
                    onInitDeleteReq = { initDeleteConfirmId = it }, onAddInitiative = { initiativeEntries = initiativeEntries + InitiativeEntry() },
                    isConditionsPanelVisible = isConditionsPanelVisible, allConditions = allConditions,
                    selectedConditions = selectedConditions,
                    onToggleCondition = { cond -> selectedConditions = if (selectedConditions.contains(cond)) selectedConditions - cond else selectedConditions + cond },
                    exhaustion = exhaustion,
                    onExhaustionChange = { exhaustion = it },
                    isShieldActive = isShieldActive,
                    onShieldActiveChange = { isShieldActive = it },
                    shieldEntries = shieldEntries,
                    activeShieldId = activeShieldId,
                    shieldDeleteConfirmId = shieldDeleteConfirmId,
                    onShieldEntries = { shieldEntries = it },
                    onActiveShield = { activeShieldId = it },
                    onShieldDeleteReq = { shieldDeleteConfirmId = it },
                    onAddShield = { shieldEntries = shieldEntries + ShieldEntry() },
                    isSpeedPanelVisible = isSpeedPanelVisible, speedEntries = speedEntries,
                    activeSpeedId = activeSpeedId, speedDeleteConfirmId = speedDeleteConfirmId,
                    onSpeedEntries = { speedEntries = it }, onActiveSpeed = { activeSpeedId = it },
                    onSpeedDeleteReq = { speedDeleteConfirmId = it }, onAddSpeed = { speedEntries = speedEntries + SpeedEntry() }
                )

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    beyondViewportPageCount = 1
                ) { page ->
                    val tab = tabs[page % tabs.size]

                    when (tab) {
                        CharacterTab.STATS -> {
                            StatsTab(
                                character = character,
                                level = level,
                                statsState = statsState,
                                onStatsStateChange = { statsState = it },
                                onRoll = onRoll,
                                hazeState = hazeState,
                                forceBlurEnabled = forceBlurEnabled,
                                blurPopups = blurPopups,
                                isAdvancedMode = isAdvancedMode,
                                advantageLogic = advantageLogic,
                                attributeModifiers = attributeModifiers,
                                statsMap = statsMap
                            )
                        }
                        CharacterTab.ATTACKS -> {
                            AttacksTab(
                                attacks = attacks,
                                proficiencyBonus = pb,
                                attributeModifiers = attributeModifiers,
                                onUpdateAttacks = { attacks = it },
                                onRoll = onRoll,
                                stats = statsMap,
                                exhaustion = exhaustion,
                                hazeState = hazeState,
                                forceBlurEnabled = forceBlurEnabled,
                                blurPopups = blurPopups,
                                isEditMode = isEditMode,
                                settingsViewModel = settingsViewModel,
                                spellSettings = spellSettings,
                                advantageLogic = advantageLogic
                            )
                        }
                        CharacterTab.BIO -> {
                            BioTab(
                                character = character.copy(
                                    bioShortFields = bioShortFields,
                                    bioLongSections = bioLongSections,
                                    imageData = characterImageData
                                ),
                                onCharacterChange = { updated ->
                                    bioShortFields = updated.bioShortFields
                                    bioLongSections = updated.bioLongSections
                                    characterImageData = updated.imageData
                                },
                                onAvatarEditRequest = {
                                    imagePickerLauncher.launch("image/*")
                                },
                                hazeState = hazeState,
                                forceBlurEnabled = forceBlurEnabled,
                                blurPopups = blurPopups,
                                isEditMode = isEditMode,
                                settingsViewModel = settingsViewModel,
                                statsMap = statsMap
                            )
                        }
                        CharacterTab.SKILLS_FEATS -> {
                            SkillsFeatsTab(
                                skillsAndTraits = skillsAndTraits,
                                onSkillsAndTraitsChange = { skillsAndTraits = it },
                                hazeState = hazeState,
                                forceBlurEnabled = forceBlurEnabled,
                                blurPopups = blurPopups,
                                isEditMode = isEditMode,
                                settingsViewModel = settingsViewModel,
                                statsMap = statsMap
                            )
                        }
                        CharacterTab.INVENTORY -> {
                            InventoryTab(
                                inventory = inventory,
                                onInventoryChange = { inventory = it },
                                wallet = wallet,
                                onWalletChange = { wallet = it },
                                hazeState = hazeState,
                                forceBlurEnabled = forceBlurEnabled,
                                blurPopups = blurPopups,
                                isEditMode = isEditMode,
                                settingsViewModel = settingsViewModel,
                                statsMap = statsMap
                            )
                        }
                        CharacterTab.SPELLS -> {
                            SpellsTab(
                                spells = spells,
                                onSpellsChange = { spells = it },
                                characterLevel = level.toIntOrNull() ?: 1,
                                spellSettings = spellSettings,
                                onSpellSettingsChange = { spellSettings = it },
                                hazeState = hazeState,
                                forceBlurEnabled = forceBlurEnabled,
                                blurPopups = blurPopups,
                                isEditMode = isEditMode,
                                settingsViewModel = settingsViewModel,
                                onRoll = onRoll,
                                statsMap = statsMap,
                                exhaustion = exhaustion,
                                advantageLogic = advantageLogic
                            )
                        }
                        CharacterTab.NOTES -> {
                            NotesTab(
                                notes = notes,
                                onNotesChange = { notes = it },
                                hazeState = hazeState,
                                forceBlurEnabled = forceBlurEnabled,
                                blurPopups = blurPopups,
                                isEditMode = isEditMode,
                                settingsViewModel = settingsViewModel,
                                statsMap = statsMap
                            )
                        }
                    }
                }
            }

            DiceRollingFab(
                onRoll = { pool ->
                    val res = DiceRoller.rollPool(pool)
                    onRoll(res)
                },
                offsetX = diceFabOffsetX,
                offsetY = diceFabOffsetY,
                hazeState = hazeState,
                isOled = colorScheme.background == Color.Black,
                alpha = effectiveDiceFabAlpha,
                forceBlurEnabled = effectiveDiceFabBlur,
                positionKey = diceFabOffsetX to diceFabOffsetY,
                onDrag = { dx, dy ->
                    val density = context.resources.displayMetrics.density
                    settingsViewModel?.updateDiceFabPosition(
                        diceFabOffsetX + dx / density,
                        diceFabOffsetY + dy / density
                    )
                }
            )

            HealthDialog(
                showDialog = showHpDialog,
                hpDialogType = hpDialogType,
                hpDialogValue = hpDialogValue,
                onValueChange = { newVal -> hpDialogValue = newVal },
                onDismiss = { showHpDialog = false },
                onConfirm = { value: Int ->
                    when(hpDialogType) {
                        "heal" -> currentHp = minOf(maxHp.toIntOrNull() ?: 0, (currentHp.toIntOrNull() ?: 0) + value).toString()
                        "damage" -> {
                            var d = value; var t = tempHp.toIntOrNull() ?: 0; val c = currentHp.toIntOrNull() ?: 0
                            if (t > 0) { val a = minOf(t, d); t -= a; d -= a; tempHp = t.toString() }
                            if (d > 0) currentHp = maxOf(0, c - d).toString()
                        }
                        "temp" -> tempHp = minOf(9999, value).toString()
                    }
                    showHpDialog = false
                }
            )
        }
    }

    if (showSpellSettings) {
        SpellSettingsDialog(
            settings = spellSettings,
            characterLevel = level.toIntOrNull() ?: 1,
            onSettingsChange = { spellSettings = it },
            onDismiss = { showSpellSettings = false },
            hazeState = hazeState,
            forceBlurEnabled = forceBlurEnabled,
            statsMap = statsMap
        )
    }

    val effectiveBlurFullscreen = forceBlurEnabled

    if (showEnhancedAC) {
        val activeArmor = armorClassEntries.find { it.id == activeArmorClassId }
        val activeShieldObj = shieldEntries.find { it.id == activeShieldId }
        
        EnhancedStatDialog(
            title = "Класс Доспеха",
            statType = "AC",
            activeEntry = activeArmor,
            allEntries = armorClassEntries,
            onAllEntriesChange = { armorClassEntries = it.filterIsInstance<ArmorClassEntry>() },
            onActiveIdChange = { activeArmorClassId = it },
            statsMap = statsMap,
            hazeState = hazeState,
            forceBlurEnabled = effectiveBlurFullscreen,
            onDismiss = { showEnhancedAC = false },
            isShieldActive = isShieldActive,
            onShieldActiveChange = { isShieldActive = it },
            activeShield = activeShieldObj,
            allShields = shieldEntries,
            onShieldChange = { updated ->
                val newList = shieldEntries.toMutableList()
                val idx = newList.indexOfFirst { it.id == updated.id }
                if (idx != -1) {
                    newList[idx] = updated
                    shieldEntries = newList
                }
            },
            onAllShieldsChange = { shieldEntries = it },
            onActiveShieldIdChange = { activeShieldId = it }
        )
    }

    if (showEnhancedInit) {
        val activeInit = initiativeEntries.find { it.id == activeInitiativeId }
        EnhancedStatDialog(
            title = "Инициатива",
            statType = "INIT",
            activeEntry = activeInit,
            allEntries = initiativeEntries,
            onAllEntriesChange = { initiativeEntries = it.filterIsInstance<InitiativeEntry>() },
            onActiveIdChange = { activeInitiativeId = it },
            statsMap = statsMap,
            hazeState = hazeState,
            forceBlurEnabled = effectiveBlurFullscreen,
            onDismiss = { showEnhancedInit = false }
        )
    }

    if (showEnhancedSpeed) {
        val activeSpeed = speedEntries.find { it.id == activeSpeedId }
        EnhancedStatDialog(
            title = "Скорость",
            statType = "SPEED",
            activeEntry = activeSpeed,
            allEntries = speedEntries,
            onAllEntriesChange = { speedEntries = it.filterIsInstance<SpeedEntry>() },
            onActiveIdChange = { activeSpeedId = it },
            statsMap = statsMap,
            hazeState = hazeState,
            forceBlurEnabled = effectiveBlurFullscreen,
            onDismiss = { showEnhancedSpeed = false }
        )
    }

    if (showHealthSettings) {
        EnhancedHealthSettingsDialog(
            currentHitDie = character?.defaultHitDie ?: 8,
            onHitDieChange = { newDie ->
                onSaveChanges(character!!.copy(defaultHitDie = newDie))
            },
            hazeState = hazeState,
            forceBlurEnabled = effectiveBlurFullscreen,
            onDismiss = { showHealthSettings = false }
        )
    }

    if (showEnhancedCond) {
        EnhancedConditionsDialog(
            allConditions = allConditions,
            selectedConditions = selectedConditions,
            onToggleCondition = { cond -> selectedConditions = if (selectedConditions.contains(cond)) selectedConditions - cond else selectedConditions + cond },
            exhaustion = exhaustion,
            onExhaustionChange = { exhaustion = it },
            hazeState = hazeState,
            forceBlurEnabled = effectiveBlurFullscreen,
            onDismiss = { showEnhancedCond = false }
        )
    }

    if (showTabSheet) {
        val isOled = colorScheme.background == Color.Black

        CompositionLocalProvider(LocalHazeStyle provides TabSheetHazeStyle) {
            ModalBottomSheet(
                onDismissRequest = { showTabSheet = false },
                sheetState = sheetState,
                containerColor = if (isOled) Color.Black 
                                 else if (blurPopups) colorScheme.surface.copy(alpha = 0.1f) 
                                 else colorScheme.surface,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .run {
                            if (blurPopups && hazeState != null && !isOled) {
                                this.clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                                    .hazeEffect(state = hazeState) {
                                        style = HazeStyle(blurRadius = 24.dp, tints = listOf(HazeTint(colorScheme.surface.copy(alpha = 0.1f))))
                                        inputScale = HazeInputScale.Fixed(0.6f)
                                    }
                            } else this
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 32.dp)
                    ) {
                        Text(
                            "Перейти к вкладке",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        tabs.forEachIndexed { index, tab ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        tab.title,
                                        fontWeight = if (tab == currentTab) FontWeight.Bold else FontWeight.Normal,
                                        color = if (tab == currentTab) colorScheme.primary else colorScheme.onSurface
                                    )
                                },
                                leadingContent = {
                                    val icon = when(tab) {
                                        CharacterTab.STATS -> Icons.Default.Person
                                        CharacterTab.ATTACKS -> Icons.Default.Gavel
                                        CharacterTab.BIO -> Icons.Default.Book
                                        CharacterTab.INVENTORY -> Icons.Default.Inventory
                                        CharacterTab.SPELLS -> Icons.Default.AutoFixHigh
                                        CharacterTab.NOTES -> Icons.AutoMirrored.Filled.Note
                                        CharacterTab.SKILLS_FEATS -> Icons.Default.Star
                                    }
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = if (tab == currentTab) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable {
                                    val currentP = pagerState.currentPage
                                    val currentIdx = currentP % tabs.size
                                    val diff = index - currentIdx

                                    scope.launch {
                                        launch { pagerState.animateScrollToPage(currentP + diff) }
                                        sheetState.hide()
                                        showTabSheet = false
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (bitmapToCrop != null) {
        AvatarCropperWindow(
            imageToCrop = bitmapToCrop!!,
            hazeState = hazeState,
            forceBlurEnabled = blurPopups,
            onCropSuccess = { cropped ->
                scope.launch {
                    val id = ImageManager.saveBitmapAsOriginal(context, bitmapToCrop!!)
                    ImageManager.saveCropped(context, id, cropped)

                    val portraitFile = ImageManager.getPortraitFile(context, id)
                    var seedColor: Int? = null
                    if (portraitFile.exists()) {
                        val bitmap = BitmapFactory.decodeFile(portraitFile.absolutePath)
                        if (bitmap != null) {
                            seedColor = PaletteHelper.extractSeedColor(bitmap)
                        }
                    }
                    characterImageData = id
                    themeSeedColorArgb = seedColor
                    bitmapToCrop = null
                }
            },
            onCancel = { bitmapToCrop = null }
        )
    }
}
