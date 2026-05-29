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
import ru.quasaris.characters.master.ArmorClassEntry
import ru.quasaris.characters.master.Character
import ru.quasaris.characters.master.HeaderCode.ExpandingPanelsSection
import ru.quasaris.characters.master.HeaderCode.getNextLevelThreshold
import ru.quasaris.characters.master.HeaderCode.evaluateFormula
import ru.quasaris.characters.master.InitiativeEntry
import ru.quasaris.characters.master.R
import ru.quasaris.characters.master.SpeedEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWindow(
    modifier: Modifier = Modifier,
    character: Character? = null,
    onNavigateBack: () -> Unit,
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
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { selectedImageUri = it }

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
    val allConditions = rememberAllConditions(context)

    var showAvatarMenu by remember { mutableStateOf(false) }
    val charId = remember { character?.id ?: (0..Int.MAX_VALUE).random() }

    // Derived values
    val statsMap = mapOf(
        "strength" to strength, "dexterity" to dexterity, "constitution" to constitution,
        "intelligence" to intelligence, "wisdom" to wisdom, "charisma" to charisma,
        "proficiencyBonus" to proficiencyBonus, "level" to level
    )

    val activeACValue = remember(activeArmorClassId, armorClassEntries, statsMap) {
        CombatCalculations.calculateAC(activeArmorClassId, armorClassEntries, statsMap)
    }
    val activeInitValue = remember(activeInitiativeId, initiativeEntries, statsMap) {
        CombatCalculations.calculateInitiative(activeInitiativeId, initiativeEntries, statsMap)
    }
    val activeSpeedValue = remember(activeSpeedId, speedEntries, statsMap) {
        CombatCalculations.calculateSpeed(activeSpeedId, speedEntries, statsMap)
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
        initiativeEntries, activeInitiativeId, speedEntries, activeSpeedId, selectedImageUri,
        maxHp, currentHp, tempHp, selectedConditions
    ) {
        val updated = CharacterDataHandler.createCharacter(
            charId, name, level, experience, strength, dexterity, constitution, intelligence, wisdom, charisma,
            strProf, dexProf, conProf, intProf, wisProf, chaProf, maxHp, currentHp, tempHp,
            armorClassEntries, activeArmorClassId, initiativeEntries, activeInitiativeId,
            speedEntries, activeSpeedId, selectedConditions, character?.imageData, context, selectedImageUri
        )
        onCharacterChange(updated)
    }

    val fileCreator = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val currentChar = CharacterDataHandler.createCharacter(
            charId, name, level, experience, strength, dexterity, constitution, intelligence, wisdom, charisma,
            strProf, dexProf, conProf, intProf, wisProf, chaProf, maxHp, currentHp, tempHp,
            armorClassEntries, activeArmorClassId, initiativeEntries, activeInitiativeId,
            speedEntries, activeSpeedId, selectedConditions, character?.imageData, context, selectedImageUri
        )
        CharacterDataHandler.exportToJson(context, uri, currentChar)
    }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            CharacterIdentitySection(
                name = name, onNameChange = { name = it },
                level = level, experience = experience, nextLevelExp = nextLevelExp,
                selectedImageUri = selectedImageUri, characterImageData = character?.imageData,
                showAvatarMenu = showAvatarMenu, onAvatarClick = { showAvatarMenu = true },
                onDismissAvatarMenu = { showAvatarMenu = false },
                onLevelClick = {
                    isLevelPanelVisible = !isLevelPanelVisible; isArmorClassPanelVisible = false; isInitiativePanelVisible = false
                    isSpeedPanelVisible = false; isHealthPanelVisible = false; isConditionsPanelVisible = false
                },
                onNavigateBack = onNavigateBack, activeACValue = activeACValue,
                onACClick = {
                    isArmorClassPanelVisible = !isArmorClassPanelVisible; isInitiativePanelVisible = false
                    isSpeedPanelVisible = false; isLevelPanelVisible = false; isHealthPanelVisible = false; isConditionsPanelVisible = false
                },
                activeInitValue = activeInitValue,
                onInitClick = {
                    isInitiativePanelVisible = !isInitiativePanelVisible; isArmorClassPanelVisible = false
                    isSpeedPanelVisible = false; isLevelPanelVisible = false; isHealthPanelVisible = false; isConditionsPanelVisible = false
                },
                currentHp = currentHp, maxHp = maxHp, tempHp = tempHp,
                healthColor = healthColor, healthIcon = healthIcon,
                onHealthClick = {
                    isHealthPanelVisible = !isHealthPanelVisible; isArmorClassPanelVisible = false; isInitiativePanelVisible = false
                    isSpeedPanelVisible = false; isLevelPanelVisible = false; isConditionsPanelVisible = false
                },
                conditionsCount = selectedConditions.size.toString(),
                onConditionsClick = {
                    isConditionsPanelVisible = !isConditionsPanelVisible; isArmorClassPanelVisible = false; isInitiativePanelVisible = false
                    isSpeedPanelVisible = false; isLevelPanelVisible = false; isHealthPanelVisible = false
                },
                activeSpeedValue = activeSpeedValue,
                onSpeedClick = {
                    isSpeedPanelVisible = !isSpeedPanelVisible; isArmorClassPanelVisible = false; isInitiativePanelVisible = false
                    isLevelPanelVisible = false; isHealthPanelVisible = false; isConditionsPanelVisible = false
                },
                imagePicker = imagePicker,
                onDownloadClick = { filename -> fileCreator.launch(filename) }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(colorScheme.background).clickable(remember { MutableInteractionSource() }, null) { 
            focusManager.clearFocus(); acDeleteConfirmId = null; initDeleteConfirmId = null; speedDeleteConfirmId = null 
        }) {
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
                    onToggleCondition = { n -> selectedConditions = toggleCondition(selectedConditions, n) },
                    isSpeedPanelVisible = isSpeedPanelVisible, speedEntries = speedEntries,
                    activeSpeedId = activeSpeedId, speedDeleteConfirmId = speedDeleteConfirmId,
                    onSpeedEntries = { speedEntries = it }, onActiveSpeed = { activeSpeedId = it },
                    onSpeedDeleteReq = { speedDeleteConfirmId = it }, onAddSpeed = { speedEntries = speedEntries + SpeedEntry() }
                )

                Button(onClick = {}, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp, bottom = 20.dp).width(220.dp).height(44.dp), colors = ButtonDefaults.buttonColors(containerColor = colorScheme.secondaryContainer, contentColor = colorScheme.onSecondaryContainer), shape = RoundedCornerShape(8.dp), elevation = ButtonDefaults.buttonElevation(4.dp)) { Text("Расширенный режим", fontSize = 14.sp) }
                
                AttributesSection(
                    strength = strength, onStrengthChange = { strength = it }, strProf = strProf, onStrProfChange = { strProf = it },
                    intelligence = intelligence, onIntelligenceChange = { intelligence = it }, intProf = intProf, onIntProfChange = { intProf = it },
                    dexterity = dexterity, onDexterityChange = { dexterity = it }, dexProf = dexProf, onDexProfChange = { dexProf = it },
                    wisdom = wisdom, onWisdomChange = { wisdom = it }, wisProf = wisProf, onWisProfChange = { wisProf = it },
                    constitution = constitution, onConstitutionChange = { constitution = it }, conProf = conProf, onConProfChange = { conProf = it },
                    charisma = charisma, onCharismaChange = { charisma = it }, chaProf = chaProf, onChaProfChange = { chaProf = it },
                    evalPB = evalPB
                )
                
                Spacer(Modifier.height(24.dp))
            }
        }
        
        HealthDialog(
            showDialog = showHpDialog,
            hpDialogType = hpDialogType,
            hpDialogValue = hpDialogValue,
            onValueChange = { hpDialogValue = it },
            onDismiss = { showHpDialog = false },
            onConfirm = { v ->
                when(hpDialogType) {
                    "heal" -> currentHp = minOf(maxHp.toIntOrNull() ?: 0, (currentHp.toIntOrNull() ?: 0) + v).toString()
                    "damage" -> {
                        var d = v; var t = tempHp.toIntOrNull() ?: 0; var c = currentHp.toIntOrNull() ?: 0
                        if (t > 0) { val a = minOf(t, d); t -= a; d -= a; tempHp = t.toString() }
                        if (d > 0) currentHp = maxOf(0, c - d).toString()
                    }
                    "temp" -> tempHp = minOf(9999, v).toString()
                }
                showHpDialog = false
            }
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
fun CreateWindowPreview() { quasarisTheme { CreateWindow(onNavigateBack = {}, onCharacterChange = {}) } }
