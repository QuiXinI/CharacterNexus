package ru.quasaris.characters.master.MainWindow

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.quasaris.characters.master.ui.theme.quasarisTheme
import ru.quasaris.characters.master.backend.ArchiveManager
import ru.quasaris.characters.master.ArmorClassEntry
import ru.quasaris.characters.master.Character
import ru.quasaris.characters.master.HeaderCode.ExpandingPanelsSection
import ru.quasaris.characters.master.backend.getNextLevelThreshold
import ru.quasaris.characters.master.backend.evaluateFormula
import ru.quasaris.characters.master.InitiativeEntry
import ru.quasaris.characters.master.R
import ru.quasaris.characters.master.SpeedEntry
import ru.quasaris.characters.master.ShieldEntry
import ru.quasaris.characters.master.backend.ImageManager
import ru.quasaris.characters.master.PaletteHelper
import android.graphics.BitmapFactory
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.quasaris.characters.master.HeaderCode.HealthDialog
import ru.quasaris.characters.master.HeaderCode.rememberAllConditions
import ru.quasaris.characters.master.HeaderCode.toggleCondition
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWindow(
    character: Character? = null,
    onNavigateBack: () -> Unit,
    onOpenDrawer: () -> Unit,
    onCharacterChange: (Character) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val colorScheme = MaterialTheme.colorScheme

    // States
    var name by remember { mutableStateOf(character?.name ?: "") }
    var level by remember { mutableStateOf(character?.level ?: "1") }
    var experience by remember { mutableStateOf(character?.experience ?: "0") }
    var nextLevelExp by remember { mutableStateOf("300") }
    var proficiencyBonus by remember { mutableStateOf("[НАСТ БМ]") }

    var strength by remember { mutableStateOf(character?.strength ?: "10") }
    var dexterity by remember { mutableStateOf(character?.dexterity ?: "10") }
    var constitution by remember { mutableStateOf(character?.constitution ?: "10") }
    var intelligence by remember { mutableStateOf(character?.intelligence ?: "10") }
    var wisdom by remember { mutableStateOf(character?.wisdom ?: "10") }
    var charisma by remember { mutableStateOf(character?.charisma ?: "10") }

    var strProf by remember { mutableStateOf(character?.strengthProficient ?: false) }
    var dexProf by remember { mutableStateOf(character?.dexterityProficient ?: false) }
    var conProf by remember { mutableStateOf(character?.constitutionProficient ?: false) }
    var intProf by remember { mutableStateOf(character?.intelligenceProficient ?: false) }
    var wisProf by remember { mutableStateOf(character?.wisdomProficient ?: false) }
    var chaProf by remember { mutableStateOf(character?.charismaProficient ?: false) }

    var maxHp by remember { mutableStateOf(character?.maxHp ?: "10") }
    var tempHp by remember { mutableStateOf(character?.tempHp ?: "0") }
    var currentHp by remember { mutableStateOf(character?.currentHp ?: "10") }
    
    var isHealthPanelVisible by remember { mutableStateOf(false) }
    var hpDialogType by remember { mutableStateOf("") }
    var hpDialogValue by remember { mutableStateOf("") }
    var showHpDialog by remember { mutableStateOf(false) }

    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var characterImageData by remember { mutableStateOf(character?.imageData) }
    var themeSeedColorArgb by remember { mutableStateOf(character?.themeSeedColorArgb) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
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

    var isLevelPanelVisible by remember { mutableStateOf(false) }
    var isArmorClassPanelVisible by remember { mutableStateOf(false) }
    var isInitiativePanelVisible by remember { mutableStateOf(false) }
    var isSpeedPanelVisible by remember { mutableStateOf(false) }
    var isConditionsPanelVisible by remember { mutableStateOf(false) }

    var armorClassEntries by remember { mutableStateOf(character?.armorClassEntries ?: listOf(
        ArmorClassEntry(name = "Базовый КД", formula = "10 + [ЛОВ]")
    )) }
    var activeArmorClassId by remember { mutableStateOf<String?>(character?.activeArmorClassId ?: armorClassEntries.firstOrNull()?.id) }
    var acDeleteConfirmId by remember { mutableStateOf<String?>(null) }

    var initiativeEntries by remember { mutableStateOf(character?.initiativeEntries ?: listOf(
        InitiativeEntry(name = "Базовая Инициатива", formula = "[ЛОВ]")
    )) }
    var activeInitiativeId by remember { mutableStateOf<String?>(character?.activeInitiativeId ?: initiativeEntries.firstOrNull()?.id) }
    var initDeleteConfirmId by remember { mutableStateOf<String?>(null) }

    var speedEntries by remember { mutableStateOf(character?.speedEntries ?: listOf(
        SpeedEntry(name = "Базовая Скорость", formula = "30")
    )) }
    var activeSpeedId by remember { mutableStateOf<String?>(character?.activeSpeedId ?: speedEntries.firstOrNull()?.id) }
    var speedDeleteConfirmId by remember { mutableStateOf<String?>(null) }

    var selectedConditions by remember { mutableStateOf(character?.selectedConditions ?: emptyList<String>()) }
    var exhaustion by remember { mutableStateOf(character?.exhaustion ?: 0) }
    var isRestPanelVisible by remember { mutableStateOf(false) }
    var showHealthSettings by remember { mutableStateOf(false) }
    var defaultHitDie by remember { mutableIntStateOf(character?.defaultHitDie ?: 8) }
    var hitDiceEntries by remember { mutableStateOf(character?.hitDiceEntries ?: emptyList<ru.quasaris.characters.master.HitDiceEntry>()) }

    var isShieldActive by remember { mutableStateOf(character?.isShieldActive ?: false) }
    var shieldEntries by remember { mutableStateOf(character?.shieldEntries ?: listOf(
        ShieldEntry(name = "Базовый Щит", formula = "2")
    )) }
    var activeShieldId by remember { mutableStateOf<String?>(character?.activeShieldId ?: shieldEntries.firstOrNull()?.id) }
    var shieldDeleteConfirmId by remember { mutableStateOf<String?>(null) }
    val allConditions = rememberAllConditions(context)

    var showAvatarMenu by remember { mutableStateOf(false) }
    val charId = remember { character?.id ?: (0..Int.MAX_VALUE).random() }
    var isAdvancedMode by remember { mutableStateOf(false) }
    var skilledProficiencies by remember { mutableStateOf(character?.skilledProficiencies ?: emptyList()) }
    var skilledExpertise by remember { mutableStateOf(character?.skilledExpertise ?: emptyList()) }

    // Derived values
    val statsMap = mapOf(
        "strength" to strength, "dexterity" to dexterity, "constitution" to constitution,
        "intelligence" to intelligence, "wisdom" to wisdom, "charisma" to charisma,
        "proficiencyBonus" to proficiencyBonus, "level" to level
    )

    val activeACValue = remember(activeArmorClassId, armorClassEntries, statsMap, isShieldActive, activeShieldId, shieldEntries) {
        CombatCalculations.calculateAC(activeArmorClassId, armorClassEntries, statsMap, isShieldActive, activeShieldId, shieldEntries)
    }
    val activeInitValue = remember(activeInitiativeId, initiativeEntries, statsMap, exhaustion) {
        CombatCalculations.calculateInitiative(activeInitiativeId, initiativeEntries, statsMap, exhaustion)
    }
    val activeSpeedValue = remember(activeSpeedId, speedEntries, statsMap, exhaustion) {
        CombatCalculations.calculateSpeed(activeSpeedId, speedEntries, statsMap, exhaustion)
    }

    val healthState = remember(currentHp, maxHp) {
        val c = currentHp.toIntOrNull() ?: 0; val m = maxHp.toIntOrNull() ?: 0
        when { c <= 0 -> "dead"; m > 0 && c <= m / 2 -> "bloodied"; else -> "healthy" }
    }
    val healthColor = when(healthState) { "dead" -> androidx.compose.ui.graphics.Color(0xFF454545); "bloodied" -> androidx.compose.ui.graphics.Color(0xFFE57373); else -> androidx.compose.ui.graphics.Color(0xFF00C46F) }
    val healthIcon = when(healthState) { "dead" -> R.drawable.ic_health_death; "bloodied" -> R.drawable.ic_health_bloodied; else -> R.drawable.ic_health }
    val clampHp = { val m = maxHp.toIntOrNull() ?: 0; val c = currentHp.toIntOrNull() ?: 0; if (c > m && maxHp.isNotEmpty()) currentHp = m.coerceAtLeast(0).toString() }

    val evalPB = remember(proficiencyBonus, statsMap) { evaluateFormula(proficiencyBonus, statsMap).toString() }

    // Side Effects
    LaunchedEffect(level) { nextLevelExp = getNextLevelThreshold(level) }

    LaunchedEffect(
        name, level, experience, strength, dexterity, constitution, intelligence, wisdom, charisma,
        strProf, dexProf, conProf, intProf, wisProf, chaProf, armorClassEntries, activeArmorClassId,
        initiativeEntries, activeInitiativeId, speedEntries, activeSpeedId,
        maxHp, currentHp, tempHp, selectedConditions, exhaustion, isShieldActive, shieldEntries, activeShieldId,
        skilledProficiencies, skilledExpertise, characterImageData, themeSeedColorArgb
    ) {
        val updated = CharacterDataHandler.createCharacter(
            charId, name, level, experience, strength, dexterity, constitution, intelligence, wisdom, charisma,
            strProf, dexProf, conProf, intProf, wisProf, chaProf, maxHp, currentHp, tempHp,
            armorClassEntries, activeArmorClassId, initiativeEntries, activeInitiativeId,
            speedEntries, activeSpeedId, selectedConditions, exhaustion, isShieldActive, shieldEntries, activeShieldId,
            characterImageData, skilledProficiencies, skilledExpertise, themeSeedColorArgb, hitDiceEntries, defaultHitDie
        )
        onCharacterChange(updated)
    }

    val fileCreator = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/${ArchiveManager.EXPORT_EXTENSION}")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val currentChar = CharacterDataHandler.createCharacter(
            charId, name, level, experience, strength, dexterity, constitution, intelligence, wisdom, charisma,
            strProf, dexProf, conProf, intProf, wisProf, chaProf, maxHp, currentHp, tempHp,
            armorClassEntries, activeArmorClassId, initiativeEntries, activeInitiativeId,
            speedEntries, activeSpeedId, selectedConditions, exhaustion, isShieldActive, shieldEntries, activeShieldId,
            characterImageData, skilledProficiencies, skilledExpertise, themeSeedColorArgb, hitDiceEntries, defaultHitDie
        )
        val scope = CoroutineScope(Dispatchers.IO)
        CharacterDataHandler.exportToLssKiller(context, uri, currentChar, scope)
    }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            Column(modifier = Modifier.background(colorScheme.surface)) {
                CharacterIdentitySection(
                    name = name, onNameChange = { name = it },
                    level = level, experience = experience, nextLevelExp = nextLevelExp,
                    selectedImageUri = selectedImageUri, characterImageData = characterImageData,
                    showAvatarMenu = showAvatarMenu, onAvatarClick = { showAvatarMenu = true },
                    onDismissAvatarMenu = { showAvatarMenu = false },
                    onLevelClick = {
                        isLevelPanelVisible = !isLevelPanelVisible; isArmorClassPanelVisible = false; isInitiativePanelVisible = false
                        isSpeedPanelVisible = false; isHealthPanelVisible = false; isConditionsPanelVisible = false
                    },
                    onNavigateBack = onNavigateBack,
                    onOpenDrawer = onOpenDrawer,
                    activeACValue = activeACValue,
                    onACClick = { isShieldActive = !isShieldActive },
                    onACLongClick = {
                        isArmorClassPanelVisible = !isArmorClassPanelVisible; isInitiativePanelVisible = false
                        isSpeedPanelVisible = false; isLevelPanelVisible = false; isHealthPanelVisible = false; isConditionsPanelVisible = false
                    },
                    isShieldActive = isShieldActive,
                    activeInitValue = activeInitValue,
                    onInitClick = {
                        isInitiativePanelVisible = !isInitiativePanelVisible; isArmorClassPanelVisible = false
                        isSpeedPanelVisible = false; isLevelPanelVisible = false; isHealthPanelVisible = false; isConditionsPanelVisible = false
                    },
                    currentHp = currentHp, maxHp = maxHp, tempHp = tempHp,
                    healthColor = healthColor, healthIcon = healthIcon,
                    onHealthClick = {
                        isHealthPanelVisible = !isHealthPanelVisible; isArmorClassPanelVisible = false; isInitiativePanelVisible = false
                        isSpeedPanelVisible = false; isLevelPanelVisible = false; isHealthPanelVisible = false; isConditionsPanelVisible = false
                    },
                    conditionsCount = exhaustion.toString(),
                    selectedConditions = selectedConditions,
                    onConditionsClick = {
                        isConditionsPanelVisible = !isConditionsPanelVisible; isArmorClassPanelVisible = false; isInitiativePanelVisible = false
                        isSpeedPanelVisible = false; isLevelPanelVisible = false; isHealthPanelVisible = false
                    },
                    exhaustion = exhaustion,
                    activeSpeedValue = activeSpeedValue,
                    onSpeedClick = {
                        isSpeedPanelVisible = !isSpeedPanelVisible; isArmorClassPanelVisible = false; isInitiativePanelVisible = false
                        isLevelPanelVisible = false; isHealthPanelVisible = false; isConditionsPanelVisible = false
                    },
                    imagePicker = imagePicker,
                    onDownloadClick = { filename -> fileCreator.launch(filename) },
                    onShortRest = { 
                        isRestPanelVisible = !isRestPanelVisible
                        isArmorClassPanelVisible = false; isInitiativePanelVisible = false
                        isLevelPanelVisible = false; isHealthPanelVisible = false; isConditionsPanelVisible = false
                        isSpeedPanelVisible = false
                    }
                )

                // Tab Selector Row with Advanced Mode Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
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

                    Surface(
                        color = colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ПЕРСОНАЖ",
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

                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(colorScheme.background).clickable(remember { MutableInteractionSource() }, null) { 
            focusManager.clearFocus(); acDeleteConfirmId = null; initDeleteConfirmId = null; speedDeleteConfirmId = null 
        }) {
            // Multi-Phase Synchronized Animation
            Column(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(colorScheme.surface).verticalScroll(rememberScrollState())) {
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
                    healthColor = healthColor, clampHp = clampHp,
                    spentHitDice = hitDiceEntries.sumOf { it.spent },
                    maxHitDice = hitDiceEntries.sumOf { evaluateFormula(it.formula.split('d').firstOrNull() ?: "0", statsMap) }.coerceAtLeast(level.toIntOrNull() ?: 1),
                    onSpentHitDiceChange = { newSpent ->
                        if (hitDiceEntries.isNotEmpty()) {
                            val diff = newSpent - hitDiceEntries.sumOf { it.spent }
                            val newList = hitDiceEntries.toMutableList()
                            val first = newList[0]
                            val maxFirst = evaluateFormula(first.formula.split('d').firstOrNull() ?: "0", statsMap)
                            newList[0] = first.copy(spent = (first.spent + diff).coerceIn(0, maxFirst))
                            hitDiceEntries = newList
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
                    onShortRestConfirmed = { isRestPanelVisible = false },
                    defaultHitDie = defaultHitDie,
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
                    onToggleCondition = { n -> selectedConditions =
                        toggleCondition(selectedConditions, n)
                    },
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
                
                AttributesSection(
                    strength = strength, onStrengthChange = { strength = it }, strProf = strProf, onStrProfChange = { strProf = it },
                    intelligence = intelligence, onIntelligenceChange = { intelligence = it }, intProf = intProf, onIntProfChange = { intProf = it },
                    dexterity = dexterity, onDexterityChange = { dexterity = it }, dexProf = dexProf, onDexProfChange = { dexProf = it },
                    wisdom = wisdom, onWisdomChange = { wisdom = it }, wisProf = wisProf, onWisProfChange = { wisProf = it },
                    constitution = constitution, onConstitutionChange = { constitution = it }, conProf = conProf, onConProfChange = { conProf = it },
                    charisma = charisma, onCharismaChange = { charisma = it }, chaProf = chaProf, onChaProfChange = { chaProf = it },
                    evalPB = evalPB,
                    isAdvancedMode = isAdvancedMode,
                    skilledProficiencies = skilledProficiencies,
                    skilledExpertise = skilledExpertise,
                    statsMap = statsMap,
                    exhaustion = exhaustion,
                    onSkillClick = { skill ->
                        if (skilledExpertise.contains(skill)) {
                            skilledExpertise = skilledExpertise - skill
                        } else if (skilledProficiencies.contains(skill)) {
                            skilledExpertise = skilledExpertise + skill
                            skilledProficiencies = skilledProficiencies - skill
                        } else {
                            skilledProficiencies = skilledProficiencies + skill
                        }
                    }
                )
            }
        }

        HealthDialog(
            showDialog = showHpDialog,
            hpDialogType = hpDialogType,
            hpDialogValue = hpDialogValue,
            onValueChange = { hpDialogValue = it },
            onDismiss = { showHpDialog = false },
            onConfirm = { v ->
                when (hpDialogType) {
                    "heal" -> currentHp = minOf(
                        maxHp.toIntOrNull() ?: 0,
                        (currentHp.toIntOrNull() ?: 0) + v
                    ).toString()

                    "damage" -> {
                        var d = v;
                        var t = tempHp.toIntOrNull() ?: 0;
                        val c = currentHp.toIntOrNull() ?: 0
                        if (t > 0) {
                            val a = minOf(t, d); t -= a; d -= a; tempHp = t.toString()
                        }
                        if (d > 0) currentHp = maxOf(0, c - d).toString()
                    }

                    "temp" -> tempHp = minOf(9999, v).toString()
                }
                showHpDialog = false
            }
        )

        if (showHealthSettings) {
            ru.quasaris.characters.master.HeaderCode.EnhancedHealthSettingsDialog(
                currentHitDie = defaultHitDie,
                onHitDieChange = { newDie ->
                    defaultHitDie = newDie
                },
                hazeState = null,
                forceBlurEnabled = false,
                onDismiss = { showHealthSettings = false }
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
fun CreateWindowPreview() { quasarisTheme { CreateWindow(onNavigateBack = {}, onOpenDrawer = {}, onCharacterChange = {}) } }
