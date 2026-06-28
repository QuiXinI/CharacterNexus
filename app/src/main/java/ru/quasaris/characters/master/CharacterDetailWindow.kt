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
import ru.quasaris.characters.master.attacks.AttacksTab
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
    var experience by remember { mutableStateOf("50") }
    var nextLevelExp by remember { mutableStateOf("300") }
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }

    var selectedConditions by remember { mutableStateOf(character?.selectedConditions ?: emptyList()) }
    var exhaustion by remember { mutableStateOf(character?.exhaustion ?: 0) }

    var attacks by remember { mutableStateOf(character?.attacks ?: emptyList()) }

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

    var skilledProficiencies by remember { mutableStateOf(character?.skilledProficiencies ?: emptyList()) }
    var skilledExpertise by remember { mutableStateOf(character?.skilledExpertise ?: emptyList()) }
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
            strength = strength, dexterity = dexterity, constitution = constitution,
            intelligence = intelligence, wisdom = wisdom, charisma = charisma,
            attacks = attacks
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

    val statsMap = mapOf(
        "strength" to strength, "dexterity" to dexterity, "constitution" to constitution,
        "intelligence" to intelligence, "wisdom" to wisdom, "charisma" to charisma,
        "level" to level
    )

    val attributeModifiers = remember(strength, dexterity, constitution, intelligence, wisdom, charisma) {
        mapOf(
            Attribute.STRENGTH to calculateModifier(strength),
            Attribute.DEXTERITY to calculateModifier(dexterity),
            Attribute.CONSTITUTION to calculateModifier(constitution),
            Attribute.INTELLIGENCE to calculateModifier(intelligence),
            Attribute.WISDOM to calculateModifier(wisdom),
            Attribute.CHARISMA to calculateModifier(charisma)
        )
    }
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
                    proficiencyBonus = "[НАСТ БМ]", onProfChange = { }, // CharacterDetailWindow uses getProficiencyBonus(level)
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
                    onToggleCondition = { name -> selectedConditions = if (selectedConditions.contains(name)) selectedConditions - name else selectedConditions + name },
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
                    CharacterTab.ATTACKS -> {
                        AttacksTab(
                            attacks = attacks,
                            proficiencyBonus = pb,
                            attributeModifiers = attributeModifiers,
                            onUpdateAttacks = { attacks = it }
                        )
                    }
                    CharacterTab.BIO -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                                .background(colorScheme.surface)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Box(modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            colorScheme.onSurface.copy(
                                                alpha = 0.15f
                                            ), Color.Transparent
                                        )
                                    )
                                ))

                            // Characteristics Header
                            Box(modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .height(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                                Row(modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Характеристики", fontSize = 12.sp, color = colorScheme.onPrimaryContainer.copy(0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    Text("Характеристики", fontSize = 18.sp, fontWeight = FontWeight.Normal, color = colorScheme.onPrimaryContainer, textAlign = TextAlign.Center, modifier = Modifier.weight(1.5f))
                                    Text("Характеристики", fontSize = 12.sp, color = colorScheme.onPrimaryContainer.copy(0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                                }
                            }

                            // Profile Section
                            Row(modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(colorScheme.surfaceVariant)
                                    .clickable { imagePickerLauncher.launch("image/*") }, contentAlignment = Alignment.Center) {
                                    if (characterImageData != null) {
                                        val context = LocalContext.current
                                        val portraitFile = remember(characterImageData) {
                                            if (characterImageData!!.length < 100) {
                                                ImageManager.getPortraitFile(context, characterImageData!!)
                                            } else null
                                        }
                                        val base64Bitmap = remember(characterImageData) {
                                            if (characterImageData!!.length >= 100) {
                                                try {
                                                    val decoded = Base64.decode(characterImageData, Base64.DEFAULT)
                                                    BitmapFactory.decodeByteArray(decoded, 0, decoded.size)?.asImageBitmap()
                                                } catch (_: Exception) { null }
                                            } else null
                                        }

                                        if (portraitFile != null && portraitFile.exists()) {
                                            AsyncImage(model = portraitFile, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        } else if (base64Bitmap != null) {
                                            Image(bitmap = base64Bitmap, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        } else {
                                            Icon(Icons.Default.Person, null, modifier = Modifier.size(40.dp), tint = colorScheme.onSurfaceVariant)
                                        }
                                    } else {
                                        Icon(Icons.Default.Person, null, modifier = Modifier.size(40.dp), tint = colorScheme.onSurfaceVariant)
                                    }
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Имя") }, modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp))
                                    OutlinedTextField(value = characterClass, onValueChange = { characterClass = it }, label = { Text("Класс") }, modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp))
                                }
                            }

                            // Compact Stats Grid
                            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                AttributesSection(
                                    strength = strength, onStrengthChange = { newStr -> strength = newStr }, strProf = strProf, onStrProfChange = { newStrProf -> strProf = newStrProf },
                                    intelligence = intelligence, onIntelligenceChange = { newInt -> intelligence = newInt }, intProf = intProf, onIntProfChange = { newIntProf -> intProf = newIntProf },
                                    dexterity = dexterity, onDexterityChange = { newDex -> dexterity = newDex }, dexProf = dexProf, onDexProfChange = { newDexProf -> dexProf = newDexProf },
                                    wisdom = wisdom, onWisdomChange = { newWis -> wisdom = newWis }, wisProf = wisProf, onWisProfChange = { newWisProf -> wisProf = newWisProf },
                                    constitution = constitution, onConstitutionChange = { newCon -> constitution = newCon }, conProf = conProf, onConProfChange = { newConProf -> conProf = newConProf },
                                    charisma = charisma, onCharismaChange = { newCha -> charisma = newCha }, chaProf = chaProf, onChaProfChange = { newChaProf -> chaProf = newChaProf },
                                    evalPB = remember(level) { getProficiencyBonus(level).let { if (it >= 0) "+$it" else it.toString() } },
                                    isAdvancedMode = true,
                                    skilledProficiencies = skilledProficiencies,
                                    skilledExpertise = skilledExpertise,
                                    onSkillClick = { skill: String ->
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

                            Spacer(Modifier.height(16.dp))
                            
                            Row(modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(onClick = { onDeleteCharacter(character) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp), shape = RoundedCornerShape(8.dp)) { Text("Удалить") }
                                Button(onClick = {
                                    onSaveChanges(character.copy(
                                        name = name, 
                                        characterClass = characterClass, 
                                        order = order, 
                                        level = level, 
                                        imageData = characterImageData, 
                                        strength = strength, 
                                        dexterity = dexterity, 
                                        constitution = constitution, 
                                        intelligence = intelligence, 
                                        wisdom = wisdom, 
                                        charisma = charisma, 
                                        strengthProficient = strProf,
                                        dexterityProficient = dexProf,
                                        constitutionProficient = conProf,
                                        intelligenceProficient = intProf,
                                        wisdomProficient = wisProf,
                                        charismaProficient = chaProf,
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
                                        skilledProficiencies = skilledProficiencies,
                                        skilledExpertise = skilledExpertise,
                                        themeSeedColorArgb = themeSeedColorArgb
                                    ))
                                }, modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp), shape = RoundedCornerShape(8.dp)) { Text("Сохранить") }
                            }
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                    else -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Вкладка ${tab.title} в разработке", color = colorScheme.onBackground.copy(alpha = 0.5f))
                        }
                    }
                }
            } // Column end
        } // Box end

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

@Composable
fun StatIconBoxDetail(value: String, iconRes: Int, isActive: Boolean = true) {
    val colorScheme = MaterialTheme.colorScheme
    Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
        val tint = if (isActive) colorScheme.primary.copy(alpha = 0.38f) else colorScheme.onSurface.copy(alpha = 0.12f)
        if (iconRes == R.drawable.ic_sword) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(painter = painterResource(id = R.drawable.ic_sword), contentDescription = null, modifier = Modifier.fillMaxSize(), colorFilter = ColorFilter.tint(tint))
                Image(painter = painterResource(id = R.drawable.ic_sword), contentDescription = null, modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = -1f), colorFilter = ColorFilter.tint(tint))
            }
        } else {
            Image(painter = painterResource(id = iconRes), contentDescription = null, modifier = Modifier.fillMaxSize(), colorFilter = ColorFilter.tint(tint))
        }
        Text(
            text = value,
            fontSize = 15.sp,
            color = colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            style = TextStyle(
                shadow = Shadow(
                    color = colorScheme.surface,
                    offset = Offset(0f, 0f),
                    blurRadius = 14f
                )
            )
        )
    }
}

@Composable
fun StatCardDetail(label: String, value: String, modifier: Modifier = Modifier, onValueChange: (String) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val score = value.toIntOrNull() ?: 10
    val mod = floor((score - 10) / 2.0).toInt()
    val modStr = if (mod >= 0) "+$mod" else mod.toString()
    Box(modifier = modifier
        .height(104.dp)
        .shadow(2.dp, RoundedCornerShape(8.dp))
        .background(colorScheme.surface, RoundedCornerShape(8.dp))
        .border(1.dp, colorScheme.outline.copy(0.5f), RoundedCornerShape(8.dp))
        .padding(8.dp)) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colorScheme.onSurface)
        Box(modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = 25.dp, bottom = 5.dp)
            .size(40.dp)
            .rotate(-45f)
            .clip(RoundedCornerShape(12.dp))
            .background(colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Text(modStr, modifier = Modifier.rotate(45f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.onPrimaryContainer)
        }
        Column(modifier = Modifier.align(Alignment.TopEnd), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colorScheme.surfaceVariant)
                .border(1.dp, colorScheme.outline.copy(0.05f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                BasicTextField(value = value, onValueChange = { val num = it.filter { it.isDigit() }.toIntOrNull(); if (it.isEmpty()) onValueChange(""); else if (num != null && num in 1..30) onValueChange(it.filter { it.isDigit() }) }, textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.width(36.dp))
            }
            Box(modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colorScheme.surface)
                .border(1.dp, colorScheme.outline.copy(0.05f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Text(modStr, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
            }
        }
    }
}
