package ru.quasaris.characters.master

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ru.quasaris.characters.master.ui.theme.quasarisTheme
import ru.quasaris.characters.master.utils.GsonFactory
import kotlinx.coroutines.launch
import ru.quasaris.characters.master.tabs.*
import ru.quasaris.characters.master.MainWindow.*
import ru.quasaris.characters.master.HeaderCode.*
import kotlin.math.floor
import ru.quasaris.characters.master.ImageManager
import ru.quasaris.characters.master.PaletteHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterDetailWindow(
    character: Character?,
    onNavigateBack: () -> Unit,
    onDeleteCharacter: (Character) -> Unit,
    onSaveChanges: (Character) -> Unit,
    onOpenDrawer: () -> Unit = {}
) {
    var name by remember { mutableStateOf(character?.name ?: "") }
    var characterClass by remember { mutableStateOf(character?.characterClass ?: "") }
    var order by remember { mutableStateOf(character?.order ?: "") }
    var level by remember { mutableStateOf(character?.level ?: "1") }
    var experience by remember { mutableStateOf(character?.experience ?: "50") }
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
                skilledExpertise = character?.skilledExpertise ?: emptyList()
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

    var characterImageData by remember { mutableStateOf(character?.imageData) }
    var themeSeedColorArgb by remember { mutableStateOf(character?.themeSeedColorArgb) }
    var showAvatarMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val gson = remember { GsonFactory.create() }
    val colorScheme = MaterialTheme.colorScheme

    val fileCreatorLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
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
            selectedConditions = selectedConditions, exhaustion = exhaustion,
            armorClassEntries = armorClassEntries, activeArmorClassId = activeArmorClassId,
            initiativeEntries = initiativeEntries, activeInitiativeId = activeInitiativeId,
            speedEntries = speedEntries, activeSpeedId = activeSpeedId,
            isShieldActive = isShieldActive, shieldEntries = shieldEntries, activeShieldId = activeShieldId,
            skilledProficiencies = statsState.skilledProficiencies, skilledExpertise = statsState.skilledExpertise,
            imageData = characterImageData, themeSeedColorArgb = themeSeedColorArgb
        )
        val jsonString = gson.toJson(currentCharacterState)
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(jsonString.toByteArray())
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

    val statsMap = remember(statsState, level) { statsState.toStatsMap(level) }

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
    val scope = rememberCoroutineScope()
    var showTabSheet by remember { mutableStateOf(false) }
    val currentTab = tabs[pagerState.currentPage % tabs.size]
    val sheetState = rememberModalBottomSheetState()

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
            themeSeedColorArgb = themeSeedColorArgb
        ))
    }

    LaunchedEffect(
        name, characterClass, order, level, characterImageData,
        statsState, maxHp, currentHp, tempHp, selectedConditions, exhaustion,
        attacks, armorClassEntries, activeArmorClassId, initiativeEntries,
        activeInitiativeId, speedEntries, activeSpeedId, isShieldActive,
        shieldEntries, activeShieldId, themeSeedColorArgb
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
                    onDownloadClick = { fileCreatorLauncher.launch("MP_${name}.json"); showAvatarMenu = false },
                    selectedImageUri = null,
                    onNavigateBack = onNavigateBack,
                    exhaustion = exhaustion
                )

                // Tab Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showTabSheet = true }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = currentTab.title.uppercase(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = colorScheme.onSurface, modifier = Modifier.size(24.dp))
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
                    proficiencyBonus = "[НАСТ БМ]", onProfChange = { }, // Handled by passed lambda
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
                            onStatsStateChange = { statsState = it }
                        )
                    }
                    CharacterTab.ATTACKS -> {
                        AttacksTab(
                            attacks = attacks,
                            proficiencyBonus = pb,
                            attributeModifiers = attributeModifiers,
                            onUpdateAttacks = { attacks = it }
                        )
                    }
                    CharacterTab.BIO -> {
                        BioTab()
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
        ModalBottomSheet(
            onDismissRequest = { showTabSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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
                        modifier = Modifier.clickable {
                            scope.launch {
                                val currentP = pagerState.currentPage
                                val currentIdx = currentP % tabs.size
                                val diff = index - currentIdx
                                pagerState.animateScrollToPage(currentP + diff)
                                sheetState.hide()
                            }.invokeOnCompletion {
                                showTabSheet = false
                            }
                        }
                    )
                }
            }
        }
    }
}

