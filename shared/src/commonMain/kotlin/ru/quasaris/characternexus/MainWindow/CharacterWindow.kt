package ru.quasaris.characternexus.MainWindow

import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.model.Character
import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.quasaris.characternexus.ui.quasarisTheme
import ru.quasaris.characternexus.backend.ArchiveManager
import ru.quasaris.characternexus.*
import ru.quasaris.characternexus.HeaderCode.ExpandingPanelsSection
import ru.quasaris.characternexus.backend.getNextLevelThreshold
import ru.quasaris.characternexus.backend.evaluateFormula
import org.jetbrains.compose.resources.painterResource
import characternexus.shared.generated.resources.*
import ru.quasaris.characternexus.backend.ImageManager
import ru.quasaris.characternexus.PaletteHelper
import ru.quasaris.characternexus.util.generateUuid
import ru.quasaris.characternexus.util.ImageProcessor
import ru.quasaris.characternexus.backend.cropper.AvatarCropperWindow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.quasaris.characternexus.HeaderCode.HealthDialog
import ru.quasaris.characternexus.HeaderCode.rememberAllConditions
import ru.quasaris.characternexus.HeaderCode.toggleCondition
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.text.style.TextAlign
import ru.quasaris.characternexus.ui.CommonFilePicker
import ru.quasaris.characternexus.ioDispatcher
import ru.quasaris.characternexus.util.decodeImageBitmap
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWindow(
    character: Character? = null,
    onNavigateBack: () -> Unit,
    onOpenDrawer: () -> Unit,
    onCharacterChange: (Character) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val colorScheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    val charId = remember { character?.id ?: (0..Int.MAX_VALUE).random() }
    val characterUuid = remember { character?.uuid ?: generateUuid() }

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
    var bytesToCrop by remember { mutableStateOf<ByteArray?>(null) }
    var imageToCrop by remember { mutableStateOf<ImageBitmap?>(null) }
    var showImagePicker by remember { mutableStateOf(false) }

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
    var hitDiceEntries by remember { mutableStateOf(character?.hitDiceEntries ?: emptyList<HitDiceEntry>()) }
    var hitDiceMap by remember { mutableStateOf(character?.hitDiceMap ?: emptyMap<Int, Int>()) }
    var hpLevelData by remember { mutableStateOf(character?.hpLevelData ?: emptyList<HPLevelEntry>()) }
    var manualHPLevelData by remember { mutableStateOf(character?.manualHPLevelData ?: emptyList<HPLevelEntry>()) }
    var isMulticlassHP by remember { mutableStateOf(character?.isMulticlassHP ?: false) }
    var isManualHP by remember { mutableStateOf(character?.isManualHP ?: false) }
    var manualMaxHp by remember { mutableIntStateOf(character?.manualMaxHp ?: 0) }
    var manualMaxHitDice by remember { mutableIntStateOf(character?.manualMaxHitDice ?: 0) }
    var hpBonusesAtLevel by remember { mutableStateOf(character?.hpBonusesAtLevel ?: emptyList<AttackBonus>()) }
    var hpBonusesTotal by remember { mutableStateOf(character?.hpBonusesTotal ?: emptyList<AttackBonus>()) }

    var isShieldActive by remember { mutableStateOf(character?.isShieldActive ?: false) }
    var shieldEntries by remember { mutableStateOf(character?.shieldEntries ?: listOf(
        ShieldEntry(name = "Базовый Щит", formula = "2")
    )) }
    var activeShieldId by remember { mutableStateOf<String?>(character?.activeShieldId ?: shieldEntries.firstOrNull()?.id) }
    var shieldDeleteConfirmId by remember { mutableStateOf<String?>(null) }
    val allConditions = rememberAllConditions()

    var showAvatarMenu by remember { mutableStateOf(false) }
    var isAdvancedMode by remember { mutableStateOf(false) }
    var skilledProficiencies by remember { mutableStateOf(character?.skilledProficiencies ?: emptyList()) }
    var skilledExpertise by remember { mutableStateOf(character?.skilledExpertise ?: emptyList()) }

    CommonFilePicker(show = showImagePicker, fileExtensions = listOf("jpg", "png", "webp")) { file ->
        showImagePicker = false
        file?.let {
            scope.launch {
                val bytes = it.readBytes()
                bytesToCrop = bytes
                imageToCrop = decodeImageBitmap(bytes)
            }
        }
    }

    var showExportSheetSaver by remember { mutableStateOf(false) }
    ru.quasaris.characternexus.ui.CommonFileSaver(
        show = showExportSheetSaver,
        fileName = "${name}_Sheet",
        fileExtension = ArchiveManager.EXPORT_EXTENSION
    ) { saver ->
        showExportSheetSaver = false
        saver?.let {
            val currentChar = CharacterDataHandler.createCharacter(
                charId, name, level, experience, strength, dexterity, constitution, intelligence, wisdom, charisma,
                strProf, dexProf, conProf, intProf, wisProf, chaProf, maxHp, currentHp, tempHp,
                armorClassEntries, activeArmorClassId, initiativeEntries, activeInitiativeId,
                speedEntries, activeSpeedId, selectedConditions, exhaustion, isShieldActive, shieldEntries, activeShieldId,
                characterImageData, skilledProficiencies, skilledExpertise, themeSeedColorArgb, hitDiceEntries, hitDiceMap, defaultHitDie,
                hpLevelData, manualHPLevelData, isMulticlassHP, isManualHP, manualMaxHp, manualMaxHitDice, hpBonusesAtLevel, hpBonusesTotal
            )
            scope.launch {
                val bytes = ArchiveManager.getExportBundleBytes(listOf(currentChar.copy(uuid = characterUuid)))
                it.save(bytes)
            }
        }
    }

    var showExportPortraitSaver by remember { mutableStateOf(false) }
    ru.quasaris.characternexus.ui.CommonFileSaver(
        show = showExportPortraitSaver,
        fileName = "${name}_Portrait",
        fileExtension = "webp"
    ) { saver ->
        showExportPortraitSaver = false
        saver?.let {
            scope.launch {
                characterImageData?.let { imageId ->
                    val portraitFile = ImageManager.getPortraitFile(imageId, characterUuid)
                    if (platformFileSystem.exists(portraitFile)) {
                        val bytes = platformFileSystem.read(portraitFile) { readByteArray() }
                        it.save(bytes)
                    }
                }
            }
        }
    }

    var showExportToLssSaver by remember { mutableStateOf(false) }
    ru.quasaris.characternexus.ui.CommonFileSaver(
        show = showExportToLssSaver,
        fileName = "${name}_LSS",
        fileExtension = "json"
    ) { saver ->
        showExportToLssSaver = false
        saver?.let {
            val currentChar = CharacterDataHandler.createCharacter(
                charId, name, level, experience, strength, dexterity, constitution, intelligence, wisdom, charisma,
                strProf, dexProf, conProf, intProf, wisProf, chaProf, maxHp, currentHp, tempHp,
                armorClassEntries, activeArmorClassId, initiativeEntries, activeInitiativeId,
                speedEntries, activeSpeedId, selectedConditions, exhaustion, isShieldActive, shieldEntries, activeShieldId,
                characterImageData, skilledProficiencies, skilledExpertise, themeSeedColorArgb, hitDiceEntries, hitDiceMap, defaultHitDie,
                hpLevelData, manualHPLevelData, isMulticlassHP, isManualHP, manualMaxHp, manualMaxHitDice, hpBonusesAtLevel, hpBonusesTotal
            )
            scope.launch {
                val jsonString = CharacterDataHandler.getLssKillerJson(currentChar)
                it.save(jsonString.encodeToByteArray())
            }
        }
    }

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
    val healthColor = when(healthState) { "dead" -> androidx.compose.ui.graphics.Color(0xFF454545); "bloodied" -> androidx.compose.ui.graphics.Color(0xFFE57373); else -> androidx.compose.ui.graphics.Color(0xFF00C46F) }
    val healthIcon = when(healthState) { "dead" -> Res.drawable.ic_health_death; "bloodied" -> Res.drawable.ic_health_bloodied; else -> Res.drawable.ic_health }
    val clampHp = { val m = maxHp.toIntOrNull() ?: 0; val c = currentHp.toIntOrNull() ?: 0; if (c > m && maxHp.isNotEmpty()) currentHp = m.coerceAtLeast(0).toString() }

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

    LaunchedEffect(level, isManualHP, hpLevelData, manualHPLevelData) {
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
                id = existing?.id ?: ru.quasaris.characternexus.util.generateUuid(),
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

        // Ensure level data size matches target level for automatic mode
        if (hpLevelData.size < targetLevel) {
            val newList = hpLevelData.toMutableList()
            for (i in newList.size + 1..targetLevel) {
                val die = if (isMulticlassHP) defaultHitDie else (newList.lastOrNull()?.hitDie ?: defaultHitDie)
                newList.add(HPLevelEntry(level = i, hitDie = die))
            }
            hpLevelData = newList
        }
    }

    // Side Effects
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
        onCharacterChange(updated.copy(uuid = characterUuid))
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
                        CharacterIdentitySection(
                            name = name, onNameChange = { name = it },
                            level = level, experience = experience, nextLevelExp = nextLevelExp,
                            characterImageData = characterImageData,
                            characterUuid = character?.uuid ?: "",
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
                            onImagePickerClick = { showImagePicker = true },
                            onExportSheetClick = { showExportSheetSaver = true },
                            onExportPortraitClick = { showExportPortraitSaver = true },
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

                // Expanding Panels Section (Scrollable internally)
                val panelScrollState = rememberScrollState()
                val screenHeight = 800.dp
                val surfaceColor = colorScheme.surface
                
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .heightIn(max = screenHeight / 2),
                    color = surfaceColor
                ) {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(panelScrollState)
                        .drawWithContent {
                            drawContent()
                            // Top Fade
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0f to surfaceColor,
                                    1f to Color.Transparent,
                                    startY = 0f,
                                    endY = 4.dp.toPx()
                                ),
                                size = size.copy(height = 4.dp.toPx())
                            )
                            // Bottom Fade
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0f to Color.Transparent,
                                    1f to surfaceColor,
                                    startY = size.height - 4.dp.toPx(),
                                    endY = size.height
                                ),
                                topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - 4.dp.toPx()),
                                size = size.copy(height = 4.dp.toPx())
                            )
                        }
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
                            healthColor = healthColor, clampHp = clampHp,
                            hpPanelHitDice = hitDiceEntries,
                            onSpentHitDiceChange = { idx, newValue ->
                                val newList = hitDiceEntries.toMutableList()
                                if (idx in newList.indices) {
                                    newList[idx] = newList[idx].copy(spent = newValue)
                                    hitDiceEntries = newList
                                }
                            },
                            onOpenHealthSettings = { showHealthSettings = true },
                            isRestPanelVisible = isRestPanelVisible,
                            onRestPanelDismiss = { isRestPanelVisible = false },
                            onRestPanelHitDiceChange = { hitDiceEntries = it },
                            onHealAmount = { amount ->
                                currentHp = minOf(maxHp.toIntOrNull() ?: 0, (currentHp.toIntOrNull() ?: 0) + amount).toString()
                            },
                            onShortRestConfirmed = { isRestPanelVisible = false },
                            onLongRest = { /* No-op for now */ },
                            onDawn = { /* No-op for now */ },
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
                    }
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
            // Main Content Area
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
            ru.quasaris.characternexus.HeaderCode.Fullscreen.HealthSettingsDialog(
                isManual = isManualHP,
                onManualChange = { isManualHP = it },
                manualMaxHp = manualMaxHp,
                onManualMaxHpChange = { manualMaxHp = it },
                isMulticlass = isMulticlassHP,
                onMulticlassChange = { isMulticlassHP = it },
                currentHitDie = defaultHitDie,
                onHitDieChange = { defaultHitDie = it },
                hpLevelData = hpLevelData,
                onHPLevelDataChange = { hpLevelData = it },
                manualHPLevelData = manualHPLevelData,
                onManualHPLevelDataChange = { manualHPLevelData = it },
                manualMaxHitDice = manualMaxHitDice,
                onManualMaxHitDiceChange = { manualMaxHitDice = it },
                hpBonusesAtLevel = hpBonusesAtLevel,
                onHpBonusesAtLevelChange = { hpBonusesAtLevel = it },
                hpBonusesTotal = hpBonusesTotal,
                onHpBonusesTotalChange = { hpBonusesTotal = it },
                statsMap = statsMap,
                level = level.toIntOrNull() ?: 1,
                forceBlurEnabled = false,
                onDismiss = { showHealthSettings = false }
            )
        }
    }

    if (imageToCrop != null && bytesToCrop != null) {
        AvatarCropperWindow(
            imageBitmap = imageToCrop!!,
            onCrop = { cropped ->
                scope.launch {
                    val croppedBytes = ImageProcessor.encodeToByteArray(cropped)
                    val originalBytes = bytesToCrop!!
                    
                    ImageManager.saveCharacterImages(
                        characterUuid = characterUuid,
                        originalBytes = originalBytes,
                        portraitBytes = originalBytes,
                        croppedBytes = croppedBytes
                    )

                    val seedColor = PaletteHelper.extractSeedColor(croppedBytes)

                    characterImageData = generateUuid() // version
                    themeSeedColorArgb = seedColor
                    imageToCrop = null
                    bytesToCrop = null
                }
            },
            onDismiss = { imageToCrop = null; bytesToCrop = null }
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
fun CreateWindowPreview() { quasarisTheme { CreateWindow(onNavigateBack = {}, onOpenDrawer = {}, onCharacterChange = {}) } }
