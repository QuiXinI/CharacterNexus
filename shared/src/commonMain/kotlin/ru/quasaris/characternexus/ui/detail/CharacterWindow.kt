package ru.quasaris.characternexus.ui.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import ru.quasaris.characternexus.backend.*
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.ui.components.*
import ru.quasaris.characternexus.ui.theme.QuasarisTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterWindow(
    character: Character? = null,
    onNavigateBack: () -> Unit,
    onOpenDrawer: () -> Unit,
    onCharacterChange: (Character) -> Unit,
    // Resources passed as parameters for KMP compatibility
    headerIcons: CharacterHeaderIcons,
    diceIcons: Map<Int, DrawableResource>
) {
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

    var characterImageData by remember { mutableStateOf(character?.imageData) }
    var themeSeedColorArgb by remember { mutableStateOf(character?.themeSeedColorArgb) }

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
    
    var defaultHitDie by remember { mutableStateOf(character?.defaultHitDie ?: 8) }
    var hitDiceEntries by remember { mutableStateOf(character?.hitDiceEntries ?: emptyList<HitDiceEntry>()) }
    var hitDiceMap by remember { mutableStateOf(character?.hitDiceMap ?: emptyMap<Int, Int>()) }
    var hpLevelData by remember { mutableStateOf(character?.hpLevelData ?: emptyList<HPLevelEntry>()) }
    var manualHPLevelData by remember { mutableStateOf(character?.manualHPLevelData ?: emptyList<HPLevelEntry>()) }
    var isMulticlassHP by remember { mutableStateOf(character?.isMulticlassHP ?: false) }
    var isManualHP by remember { mutableStateOf(character?.isManualHP ?: false) }
    var manualMaxHp by remember { mutableStateOf(character?.manualMaxHp ?: 0) }
    var manualMaxHitDice by remember { mutableStateOf(character?.manualMaxHitDice ?: 0) }
    var hpBonusesAtLevel by remember { mutableStateOf(character?.hpBonusesAtLevel ?: emptyList<AttackBonus>()) }
    var hpBonusesTotal by remember { mutableStateOf(character?.hpBonusesTotal ?: emptyList<AttackBonus>()) }

    var isShieldActive by remember { mutableStateOf(character?.isShieldActive ?: false) }
    var shieldEntries by remember { mutableStateOf(character?.shieldEntries ?: listOf(
        ShieldEntry(name = "Базовый Щит", formula = "2")
    )) }
    var activeShieldId by remember { mutableStateOf<String?>(character?.activeShieldId ?: shieldEntries.firstOrNull()?.id) }
    var shieldDeleteConfirmId by remember { mutableStateOf<String?>(null) }
    
    val allConditions = remember { emptyList<Condition>() } // Should be populated from resources

    var showAvatarMenu by remember { mutableStateOf(false) }
    val charId = remember { character?.id ?: (0..Int.MAX_VALUE).random() }
    var isAdvancedMode by remember { mutableStateOf(false) }
    var skilledProficiencies by remember { mutableStateOf(character?.skilledProficiencies ?: emptyList()) }
    var skilledExpertise by remember { mutableStateOf(character?.skilledExpertise ?: emptyList()) }

    // Derived values
    val statsMap = mapOf(
        "strength" to strength, "dexterity" to dexterity, "constitution" to constitution,
        "intelligence" to intelligence, "wisdom" to wisdom, "charisma" to charisma,
        "proficiencyBonus" to proficiencyBonus, "level" to level,
        "manualMaxHitDice" to manualMaxHitDice.toString(),
        "totalMaxHitDice" to hitDiceMap.values.sum().toString(),
        "totalCurrentHitDice" to (hitDiceMap.values.sum() - hitDiceEntries.sumOf { it.spent }).toString()
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
    val healthColor = when(healthState) { 
        "dead" -> Color(0xFF454545) 
        "bloodied" -> Color(0xFFE57373) 
        else -> Color(0xFF00C46F) 
    }
    val healthIcon = when(healthState) { 
        "dead" -> headerIcons.healthDeath 
        "bloodied" -> headerIcons.healthBloodied 
        else -> headerIcons.health 
    }

    val evalPB = remember(proficiencyBonus, statsMap) { evaluateFormula(proficiencyBonus, statsMap).toString() }

    val conMod = remember(statsMap) { evaluateFormula("[CON]", statsMap) }
    val levelInt = remember(level) { level.toIntOrNull() ?: 1 }
    val calculatedMaxHp = remember(isManualHP, manualMaxHp, hpLevelData, conMod, levelInt, hpBonusesAtLevel, hpBonusesTotal, statsMap) {
        val totalPerLevelBonus = hpBonusesAtLevel.filter { it.isActive }.sumOf { evaluateFormula(it.formula, statsMap) }
        val totalFixedBonus = hpBonusesTotal.filter { it.isActive }.sumOf { evaluateFormula(it.formula, statsMap) }
        
        if (isManualHP) {
            manualMaxHp + totalFixedBonus
        } else {
            val dataToUse = hpLevelData.take(levelInt)
            val totalRolls = dataToUse.sumOf { it.rollResult ?: 0 }
            totalRolls + (conMod * levelInt) + (totalPerLevelBonus * levelInt) + totalFixedBonus
        }
    }

    LaunchedEffect(calculatedMaxHp) {
        maxHp = calculatedMaxHp.toString()
    }

    LaunchedEffect(level) { nextLevelExp = getNextLevelThreshold(level) }

    LaunchedEffect(
        name, level, experience, strength, dexterity, constitution, intelligence, wisdom, charisma,
        strProf, dexProf, conProf, intProf, wisProf, chaProf, armorClassEntries, activeArmorClassId,
        initiativeEntries, activeInitiativeId, speedEntries, activeSpeedId,
        maxHp, currentHp, tempHp, selectedConditions, exhaustion, isShieldActive, shieldEntries, activeShieldId,
        skilledProficiencies, skilledExpertise, characterImageData, themeSeedColorArgb,
        hitDiceEntries, defaultHitDie, hpLevelData, manualHPLevelData, isMulticlassHP, isManualHP, manualMaxHp, manualMaxHitDice, hpBonusesAtLevel, hpBonusesTotal
    ) {
        val updated = CharacterDataHandler.createCharacter(
            charId, name, level, experience, strength, dexterity, constitution, intelligence, wisdom, charisma,
            strProf, dexProf, conProf, intProf, wisProf, chaProf, maxHp, currentHp, tempHp,
            armorClassEntries, activeArmorClassId, initiativeEntries, activeInitiativeId,
            speedEntries, activeSpeedId, selectedConditions, exhaustion, isShieldActive, shieldEntries, activeShieldId,
            characterImageData, skilledProficiencies, skilledExpertise, themeSeedColorArgb, hitDiceEntries, hitDiceMap, defaultHitDie,
            hpLevelData, manualHPLevelData, isMulticlassHP, isManualHP, manualMaxHp, manualMaxHitDice, hpBonusesAtLevel, hpBonusesTotal
        )
        onCharacterChange(updated)
    }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            Column(modifier = Modifier.background(colorScheme.background)) {
                Surface(
                    tonalElevation = 1.dp,
                    shadowElevation = 6.dp,
                    color = colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        CharacterHeader(
                            name = name, onNameChange = { name = it },
                            level = level, experience = experience, nextLevelExp = nextLevelExp,
                            characterImageData = remember(characterImageData, character?.uuid) {
                                if (characterImageData != null) {
                                    ImageManager.getPortraitFile(characterImageData!!, character?.uuid)
                                } else null
                            },
                            onAvatarClick = { showAvatarMenu = true },
                            onLevelClick = {
                                isLevelPanelVisible = !isLevelPanelVisible; isArmorClassPanelVisible = false; isInitiativePanelVisible = false
                                isSpeedPanelVisible = false; isHealthPanelVisible = false; isConditionsPanelVisible = false
                            },
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
                            showAvatarMenu = showAvatarMenu,
                            onDismissAvatarMenu = { showAvatarMenu = false },
                            onImagePickerClick = { },
                            onDownloadClick = { },
                            inspirationIcon = headerIcons.inspiration,
                            shieldIcon = headerIcons.shield,
                            swordIcon = headerIcons.sword,
                            conditionsIcon = headerIcons.conditions,
                            speedIcon = headerIcons.speed
                        )

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

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    color = colorScheme.surface
                ) {
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
                        hpPanelHitDice = hitDiceEntries,
                        onSpentHitDiceChange = { idx, newValue ->
                            val newList = hitDiceEntries.toMutableList()
                            if (idx in newList.indices) {
                                newList[idx] = newList[idx].copy(spent = newValue)
                                hitDiceEntries = newList
                            }
                        },
                        onOpenHealthSettings = { },
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
                        onToggleCondition = { n -> selectedConditions = if (selectedConditions.contains(n)) selectedConditions - n else selectedConditions + n },
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
                        onSpeedDeleteReq = { speedDeleteConfirmId = it }, onAddSpeed = { speedEntries = speedEntries + SpeedEntry() },
                        diceIcons = diceIcons
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(colorScheme.background)
            .clickable(remember { MutableInteractionSource() }, null) { 
                focusManager.clearFocus(); acDeleteConfirmId = null; initDeleteConfirmId = null; speedDeleteConfirmId = null 
            }
        ) {
            Column(modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(colorScheme.surface)
                .verticalScroll(rememberScrollState())
            ) {
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
                        var d = v
                        var t = tempHp.toIntOrNull() ?: 0
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
    }
}

data class CharacterHeaderIcons(
    val health: DrawableResource,
    val healthBloodied: DrawableResource,
    val healthDeath: DrawableResource,
    val inspiration: DrawableResource,
    val shield: DrawableResource,
    val sword: DrawableResource,
    val conditions: DrawableResource,
    val speed: DrawableResource
)
