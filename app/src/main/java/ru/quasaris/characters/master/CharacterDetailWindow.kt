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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import ru.quasaris.characters.master.tabs.attacks.AttacksTab
import ru.quasaris.characters.master.backend.ArchiveManager
import ru.quasaris.characters.master.backend.ImageManager
import ru.quasaris.characters.master.backend.getNextLevelThreshold
import ru.quasaris.characters.master.backend.getProficiencyBonus
import ru.quasaris.characters.master.backend.RollResult
import ru.quasaris.characters.master.backend.RollSourceType
import ru.quasaris.characters.master.backend.DiceRoller
import ru.quasaris.characters.master.backend.SettingsViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.HazeInputScale

import dev.chrisbanes.haze.LocalHazeStyle

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
    settingsViewModel: SettingsViewModel? = null
) {
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
    var exhaustion by remember { mutableStateOf(character?.exhaustion ?: 0) }

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

    var isShieldActive by remember { mutableStateOf(character?.isShieldActive ?: false) }
    var shieldEntries by remember { mutableStateOf(character?.shieldEntries ?: listOf(ShieldEntry(name = "Базовый Щит", formula = "2"))) }
    var activeShieldId by remember { mutableStateOf(character?.activeShieldId ?: shieldEntries.firstOrNull()?.id) }
    var shieldDeleteConfirmId by remember { mutableStateOf<String?>(null) }

    var notes by remember { mutableStateOf(character?.notes ?: listOf(DynamicNoteState())) }
    var skillsAndTraits by remember { mutableStateOf(character?.skillsAndTraits ?: emptyList()) }
    var inventory by remember { mutableStateOf(character?.inventory ?: emptyList()) }
    var spells by remember { mutableStateOf(character?.spells ?: emptyList()) }

    var characterImageData by remember { mutableStateOf(character?.imageData) }
    var themeSeedColorArgb by remember { mutableStateOf(character?.themeSeedColorArgb) }
    var showAvatarMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    val fileCreatorLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/${ArchiveManager.EXPORT_EXTENSION}")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val currentCharacterState = character!!.copy(
            name = name, characterClass = characterClass, order = order, level = level,
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
            imageData = characterImageData, themeSeedColorArgb = themeSeedColorArgb,
            notes = notes
        )
        scope.launch {
            ArchiveManager.exportCharacter(context, currentCharacterState, uri)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> 
        uri?.let {
            CoroutineScope(Dispatchers.IO).launch {
                val newId = ImageManager.processAndSaveImage(context, it)
                val portraitFile = ImageManager.getPortraitFile(context, newId)
                var seedColor: Int? = null
                if (portraitFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(portraitFile.absolutePath)
                    if (bitmap != null) {
                        seedColor = PaletteHelper.extractSeedColor(bitmap)
                    }
                }
                characterImageData = newId
                themeSeedColorArgb = seedColor
            }
        }
    }

    if (character == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Персонаж не найден!", color = colorScheme.onBackground)
        }
        return
    }

    val statsMap = remember(statsState, level, proficiencyBonus) { statsState.toStatsMap(level, proficiencyBonus) }

    val attributeModifiers = remember(statsState) { statsState.toAttributeModifiers() }
    val pb = getProficiencyBonus(level)

    val acValue = CombatCalculations.calculateAC(activeArmorClassId, armorClassEntries, statsMap, isShieldActive, activeShieldId, shieldEntries)
    val initValue = CombatCalculations.calculateInitiative(activeInitiativeId, initiativeEntries, statsMap, exhaustion)
    val speedValue = CombatCalculations.calculateSpeed(activeSpeedId, speedEntries, statsMap, exhaustion)

    val healthState = remember(currentHp, maxHp) {
        val c = currentHp.toIntOrNull() ?: 0; val m = maxHp.toIntOrNull() ?: 0
        when { c <= 0 -> "dead"; m > 0 && c <= m / 2 -> "bloodied"; else -> "healthy" }
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
    
    var isEditMode by remember { mutableStateOf(false) }
    
    // Reset edit mode when changing tabs
    LaunchedEffect(pagerState.currentPage) {
        isEditMode = false
    }

    val saveCurrentCharacter = {
        onSaveChanges(character.copy(
            name = name, 
            characterClass = characterClass, 
            order = order, 
            level = level, 
            imageData = characterImageData, 
            strength = statsState.strength, 
            dexterity = statsState.dexterity, 
            constitution = statsState.constitution, 
            intelligence = statsState.intelligence, 
            wisdom = statsState.wisdom, 
            charisma = statsState.charisma, 
            strengthProficient = statsState.strProf,
            dexterityProficient = statsState.dexProf,
            constitutionProficient = statsState.conProf,
            intelligenceProficient = statsState.intProf,
            wisdomProficient = statsState.wisProf,
            charismaProficient = statsState.chaProf,
            maxHp = maxHp,
            currentHp = currentHp,
            tempHp = tempHp,
            proficiencyBonus = proficiencyBonus,
            selectedConditions = selectedConditions,
            exhaustion = exhaustion, 
            attacks = attacks,
            armorClassEntries = armorClassEntries,
            activeArmorClassId = activeArmorClassId,
            initiativeEntries = initiativeEntries,
            activeInitiativeId = activeInitiativeId,
            speedEntries = speedEntries,
            activeSpeedId = activeSpeedId,
            isShieldActive = isShieldActive,
            shieldEntries = shieldEntries,
            activeShieldId = activeShieldId,
            skilledProficiencies = statsState.skilledProficiencies,
            skilledExpertise = statsState.skilledExpertise,
            statBonuses = statsState.statBonuses,
            skillBonuses = statsState.skillBonuses,
            themeSeedColorArgb = themeSeedColorArgb,
            notes = notes,
            skillsAndTraits = skillsAndTraits,
            inventory = inventory,
            spells = spells
        ))
    }

    LaunchedEffect(
        name, characterClass, order, level, characterImageData,
        statsState, maxHp, currentHp, tempHp, proficiencyBonus, selectedConditions, exhaustion,
        attacks, armorClassEntries, activeArmorClassId, initiativeEntries,
        activeInitiativeId, speedEntries, activeSpeedId, isShieldActive,
        shieldEntries, activeShieldId, themeSeedColorArgb, notes,
        skillsAndTraits, inventory, spells
    ) {
        saveCurrentCharacter()
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
                        isLevelPanelVisible = !isLevelPanelVisible; isArmorClassPanelVisible = false; isInitiativePanelVisible = false
                        isSpeedPanelVisible = false; isHealthPanelVisible = false; isConditionsPanelVisible = false
                    },
                    onOpenDrawer = onOpenDrawer,
                    activeACValue = acValue,
                    onACClick = { isShieldActive = !isShieldActive },
                    onACLongClick = {
                        isArmorClassPanelVisible = !isArmorClassPanelVisible; isInitiativePanelVisible = false
                        isSpeedPanelVisible = false; isLevelPanelVisible = false; isHealthPanelVisible = false; isConditionsPanelVisible = false
                    },
                    isShieldActive = isShieldActive,
                    activeInitValue = initValue,
                    onInitClick = {
                        val baseInit = (initValue.replace("+", "").toIntOrNull() ?: 0) + (exhaustion * 2)
                        onRoll(DiceRoller.roll("Инициатива", baseInit, stats = statsMap, exhaustion = exhaustion, sourceType = RollSourceType.ABILITY))
                    },
                    onInitLongClick = {
                        isInitiativePanelVisible = !isInitiativePanelVisible; isArmorClassPanelVisible = false
                        isSpeedPanelVisible = false; isLevelPanelVisible = false; isHealthPanelVisible = false; isConditionsPanelVisible = false
                    },
                    currentHp = currentHp, maxHp = maxHp, tempHp = tempHp,
                    healthColor = healthColor, healthIcon = healthIcon,
                    onHealthClick = {
                        isHealthPanelVisible = !isHealthPanelVisible; isArmorClassPanelVisible = false; isInitiativePanelVisible = false
                        isSpeedPanelVisible = false; isLevelPanelVisible = false; isConditionsPanelVisible = false
                    },
                    conditionsCount = exhaustion.toString(),
                    selectedConditions = selectedConditions,
                    onConditionsClick = {
                        isConditionsPanelVisible = !isConditionsPanelVisible; isArmorClassPanelVisible = false; isInitiativePanelVisible = false
                        isSpeedPanelVisible = false; isLevelPanelVisible = false; isHealthPanelVisible = false
                    },
                    activeSpeedValue = speedValue,
                    onSpeedClick = {
                        isSpeedPanelVisible = !isSpeedPanelVisible; isArmorClassPanelVisible = false; isInitiativePanelVisible = false
                        isLevelPanelVisible = false; isHealthPanelVisible = false; isConditionsPanelVisible = false
                    },
                    showAvatarMenu = showAvatarMenu,
                    onDismissAvatarMenu = { showAvatarMenu = false },
                    onImagePickerClick = { imagePickerLauncher.launch("image/*"); showAvatarMenu = false },
                    onDownloadClick = { fileCreatorLauncher.launch("MP_${name}.${ArchiveManager.EXPORT_EXTENSION}"); showAvatarMenu = false },
                    selectedImageUri = null,
                    onNavigateBack = onNavigateBack,
                    exhaustion = exhaustion
                )

                // Tab Selector
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
                        else -> false
                    }
                    
                    // Left spacer to balance the icon on the right
                    Spacer(modifier = Modifier.weight(1f))

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
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(colorScheme.background)) {
            
            Column {
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
                            forceBlurEnabled = forceBlurEnabled
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
                            isEditMode = isEditMode,
                            settingsViewModel = settingsViewModel
                        )
                    }
                    CharacterTab.BIO -> {
                        BioTab()
                    }
                    CharacterTab.SKILLS_FEATS -> {
                        SkillsFeatsTab(
                            skillsAndTraits = skillsAndTraits,
                            onSkillsAndTraitsChange = { skillsAndTraits = it },
                            hazeState = hazeState,
                            forceBlurEnabled = forceBlurEnabled,
                            isEditMode = isEditMode,
                            settingsViewModel = settingsViewModel
                        )
                    }
                    CharacterTab.INVENTORY -> {
                        InventoryTab(
                            inventory = inventory,
                            onInventoryChange = { inventory = it },
                            hazeState = hazeState,
                            forceBlurEnabled = forceBlurEnabled,
                            isEditMode = isEditMode,
                            settingsViewModel = settingsViewModel
                        )
                    }
                    CharacterTab.SPELLS -> {
                        SpellsTab(
                            spells = spells,
                            onSpellsChange = { spells = it },
                            hazeState = hazeState,
                            forceBlurEnabled = forceBlurEnabled,
                            isEditMode = isEditMode,
                            settingsViewModel = settingsViewModel
                        )
                    }
                    CharacterTab.NOTES -> {
                        NotesTab(
                            notes = notes,
                            onNotesChange = { notes = it },
                            hazeState = hazeState,
                            forceBlurEnabled = forceBlurEnabled,
                            isEditMode = isEditMode,
                            settingsViewModel = settingsViewModel
                        )
                    }
                    else -> {
                        PlaceholderTab(title = tab.title)
                    }
                }
            } // Pager end
        } // Column end

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
    } // Box end
}

    if (showTabSheet) {
        val isOled = colorScheme.background == Color.Black

        CompositionLocalProvider(LocalHazeStyle provides TabSheetHazeStyle) {
            ModalBottomSheet(
                onDismissRequest = { showTabSheet = false },
                sheetState = sheetState,
                containerColor = if (forceBlurEnabled && !isOled) Color.Transparent else BottomSheetDefaults.ContainerColor,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .run {
                            if (forceBlurEnabled && hazeState != null && !isOled) {
                                // ПРАВИЛЬНЫЙ порядок: clip -> hazeEffect
                                this.clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                                    .hazeEffect(state = hazeState) {
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
}
