package ru.quasaris.characters.master

import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.quasaris.characters.master.ui.theme.quasarisTheme
import com.google.gson.Gson
import ru.quasaris.characters.master.HeaderCode.CharacterHeader
import ru.quasaris.characters.master.HeaderCode.Condition
import ru.quasaris.characters.master.HeaderCode.ExpandingPanelsSection
import ru.quasaris.characters.master.HeaderCode.SquirclePath
import ru.quasaris.characters.master.HeaderCode.calculateModifier
import ru.quasaris.characters.master.HeaderCode.evaluateFormula
import ru.quasaris.characters.master.HeaderCode.getNextLevelThreshold
import ru.quasaris.characters.master.HeaderCode.parseConditions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWindow(
    modifier: Modifier = Modifier,
    character: Character? = null,
    onNavigateBack: () -> Unit,
    onCharacterChange: (Character) -> Unit
) {
    val context = LocalContext.current
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

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { selectedImageUri = it }

    val healthState = remember(currentHp, maxHp) {
        val c = currentHp.toIntOrNull() ?: 0; val m = maxHp.toIntOrNull() ?: 0
        when { c <= 0 -> "dead"; m > 0 && c <= m / 2 -> "bloodied"; else -> "healthy" }
    }
    val healthColor = when(healthState) { "dead" -> Color(0xFF454545); "bloodied" -> Color(0xFFE57373); else -> Color(0xFF00C46F) }
    val healthIcon = when(healthState) { "dead" -> R.drawable.ic_health_death; "bloodied" -> R.drawable.ic_health_bloodied; else -> R.drawable.ic_health }
    val clampHp = { val m = maxHp.toIntOrNull() ?: 0; val c = currentHp.toIntOrNull() ?: 0; if (c > m && maxHp.isNotEmpty()) currentHp = m.coerceAtLeast(0).toString() }

    val statsMap = mapOf(
        "strength" to strength, "dexterity" to dexterity, "constitution" to constitution,
        "intelligence" to intelligence, "wisdom" to wisdom, "charisma" to charisma,
        "proficiencyBonus" to proficiencyBonus, "level" to level
    )

    var isLevelPanelVisible by remember { mutableStateOf(false) }
    LaunchedEffect(level) { nextLevelExp = getNextLevelThreshold(level) }

    var armorClassEntries by remember { mutableStateOf(character?.armorClassEntries ?: listOf(ArmorClassEntry(name = "Базовый КД", formula = "10 + [ЛОВ]"))) }
    var activeArmorClassId by remember { mutableStateOf<String?>(character?.activeArmorClassId ?: armorClassEntries.firstOrNull()?.id) }
    var isArmorClassPanelVisible by remember { mutableStateOf(false) }
    var acDeleteConfirmId by remember { mutableStateOf<String?>(null) }
    val activeACValue = remember(activeArmorClassId, armorClassEntries, statsMap) {
        val active = armorClassEntries.find { it.id == activeArmorClassId }
        if (active != null) evaluateFormula(active.formula, statsMap).toString() else "10"
    }

    var initiativeEntries by remember { mutableStateOf(character?.initiativeEntries ?: listOf(InitiativeEntry(name = "Базовая Инициатива", formula = "[ЛОВ]"))) }
    var activeInitiativeId by remember { mutableStateOf<String?>(character?.activeInitiativeId ?: initiativeEntries.firstOrNull()?.id) }
    var isInitiativePanelVisible by remember { mutableStateOf(false) }
    var initDeleteConfirmId by remember { mutableStateOf<String?>(null) }
    val activeInitValue = remember(activeInitiativeId, initiativeEntries, statsMap) {
        val active = initiativeEntries.find { it.id == activeInitiativeId }
        val v = if (active != null) evaluateFormula(active.formula, statsMap) else 0
        if (v >= 0) "+$v" else v.toString()
    }

    var speedEntries by remember { mutableStateOf(character?.speedEntries ?: listOf(SpeedEntry(name = "Базовая Скорость", formula = "30"))) }
    var activeSpeedId by remember { mutableStateOf<String?>(character?.activeSpeedId ?: speedEntries.firstOrNull()?.id) }
    var isSpeedPanelVisible by remember { mutableStateOf(false) }
    var speedDeleteConfirmId by remember { mutableStateOf<String?>(null) }
    val activeSpeedValue = remember(activeSpeedId, speedEntries, statsMap) {
        val active = speedEntries.find { it.id == activeSpeedId }
        if (active != null) evaluateFormula(active.formula, statsMap).toString() else "30"
    }

    var selectedConditions by remember { mutableStateOf(character?.selectedConditions ?: emptyList<String>()) }
    var isConditionsPanelVisible by remember { mutableStateOf(false) }
    var allConditions by remember { mutableStateOf(emptyList<Condition>()) }

    LaunchedEffect(Unit) {
        try { context.assets.open("Conditions.md").bufferedReader().use { allConditions =
            parseConditions(it.readText())
        } }
        catch (e: Exception) { e.printStackTrace() }
    }

    val charId = remember { character?.id ?: (0..Int.MAX_VALUE).random() }
    var showAvatarMenu by remember { mutableStateOf(false) }
    val gson = remember { Gson() }

    val fileCreator = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val img = selectedImageUri?.let { u ->
            try { context.contentResolver.openInputStream(u)?.use { Base64.encodeToString(it.readBytes(), Base64.DEFAULT) } } catch (e: Exception) { null }
        } ?: character?.imageData
        val charToExport = Character(
            id = charId, name = name, characterClass = character?.characterClass ?: "", order = character?.order ?: "Человек",
            imageData = img, level = level, experience = experience, strength = strength, dexterity = dexterity,
            constitution = constitution, intelligence = intelligence, wisdom = wisdom, charisma = charisma,
            strengthProficient = strProf, dexterityProficient = dexProf, constitutionProficient = conProf,
            intelligenceProficient = intProf, wisdomProficient = wisProf, charismaProficient = chaProf,
            armorClassEntries = armorClassEntries, activeArmorClassId = activeArmorClassId,
            initiativeEntries = initiativeEntries, activeInitiativeId = activeInitiativeId,
            speedEntries = speedEntries, activeSpeedId = activeSpeedId,
            maxHp = maxHp, currentHp = currentHp, tempHp = tempHp, selectedConditions = selectedConditions
        )
        try { context.contentResolver.openOutputStream(uri)?.use { it.write(gson.toJson(charToExport).toByteArray()) } } catch (e: Exception) { e.printStackTrace() }
    }

    LaunchedEffect(
        name, level, experience, strength, dexterity, constitution, intelligence, wisdom, charisma,
        strProf, dexProf, conProf, intProf, wisProf, chaProf, armorClassEntries, activeArmorClassId,
        initiativeEntries, activeInitiativeId, speedEntries, activeSpeedId, selectedImageUri,
        maxHp, currentHp, tempHp, selectedConditions
    ) {
        val img = selectedImageUri?.let { u ->
            try { context.contentResolver.openInputStream(u)?.use { Base64.encodeToString(it.readBytes(), Base64.DEFAULT) } } catch (e: Exception) { null }
        } ?: character?.imageData
        val updated = Character(
            id = charId, name = name, characterClass = character?.characterClass ?: "", order = character?.order ?: "Человек",
            imageData = img, level = level, experience = experience, strength = strength, dexterity = dexterity,
            constitution = constitution, intelligence = intelligence, wisdom = wisdom, charisma = charisma,
            strengthProficient = strProf, dexterityProficient = dexProf, constitutionProficient = conProf,
            intelligenceProficient = intProf, wisdomProficient = wisProf, charismaProficient = chaProf,
            armorClassEntries = armorClassEntries, activeArmorClassId = activeArmorClassId,
            initiativeEntries = initiativeEntries, activeInitiativeId = activeInitiativeId,
            speedEntries = speedEntries, activeSpeedId = activeSpeedId,
            maxHp = maxHp, currentHp = currentHp, tempHp = tempHp, selectedConditions = selectedConditions
        )
        onCharacterChange(updated)
    }

    val focusManager = LocalFocusManager.current
    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            CharacterHeader(
                name = name,
                onNameChange = { name = it },
                level = level,
                experience = experience,
                nextLevelExp = nextLevelExp,
                selectedImageUri = selectedImageUri,
                characterImageData = character?.imageData,
                onAvatarClick = { showAvatarMenu = true },
                onLevelClick = {
                    isLevelPanelVisible = !isLevelPanelVisible; isArmorClassPanelVisible =
                    false; isInitiativePanelVisible = false; isSpeedPanelVisible =
                    false; isHealthPanelVisible = false; isConditionsPanelVisible = false
                },
                onNavigateBack = onNavigateBack,
                activeACValue = activeACValue,
                onACClick = {
                    isArmorClassPanelVisible = !isArmorClassPanelVisible; isInitiativePanelVisible =
                    false; isSpeedPanelVisible = false; isLevelPanelVisible =
                    false; isHealthPanelVisible = false; isConditionsPanelVisible = false
                },
                activeInitValue = activeInitValue,
                onInitClick = {
                    isInitiativePanelVisible = !isInitiativePanelVisible; isArmorClassPanelVisible =
                    false; isSpeedPanelVisible = false; isLevelPanelVisible =
                    false; isHealthPanelVisible = false; isConditionsPanelVisible = false
                },
                currentHp = currentHp,
                maxHp = maxHp,
                tempHp = tempHp,
                healthColor = healthColor,
                healthIcon = healthIcon,
                onHealthClick = {
                    isHealthPanelVisible = !isHealthPanelVisible; isArmorClassPanelVisible =
                    false; isInitiativePanelVisible = false; isSpeedPanelVisible =
                    false; isLevelPanelVisible = false; isConditionsPanelVisible = false
                },
                conditionsCount = selectedConditions.size.toString(),
                onConditionsClick = {
                    isConditionsPanelVisible = !isConditionsPanelVisible; isArmorClassPanelVisible =
                    false; isInitiativePanelVisible = false; isSpeedPanelVisible =
                    false; isLevelPanelVisible = false; isHealthPanelVisible = false
                },
                activeSpeedValue = activeSpeedValue,
                onSpeedClick = {
                    isSpeedPanelVisible = !isSpeedPanelVisible; isArmorClassPanelVisible =
                    false; isInitiativePanelVisible = false; isLevelPanelVisible =
                    false; isHealthPanelVisible = false; isConditionsPanelVisible = false
                },
                showAvatarMenu = showAvatarMenu,
                onDismissAvatarMenu = { showAvatarMenu = false },
                onImagePickerClick = { showAvatarMenu = false; imagePicker.launch("image/*") },
                onDownloadClick = {
                    showAvatarMenu =
                        false; fileCreator.launch("${if (name.isEmpty()) "character" else name}.json")
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(colorScheme.background).clickable(remember { MutableInteractionSource() }, null) { focusManager.clearFocus(); acDeleteConfirmId = null; initDeleteConfirmId = null; speedDeleteConfirmId = null }) {
            Column(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(colorScheme.surface).verticalScroll(rememberScrollState())) {
                ExpandingPanelsSection(
                    isLevelPanelVisible = isLevelPanelVisible,
                    level = level,
                    onLevelChange = { level = it },
                    experience = experience,
                    onExpChange = { experience = it },
                    proficiencyBonus = proficiencyBonus,
                    onProfChange = { proficiencyBonus = it },
                    nextLevelExp = nextLevelExp,
                    statsMap = statsMap,
                    isHealthPanelVisible = isHealthPanelVisible,
                    maxHp = maxHp,
                    onMaxHpChange = { maxHp = it },
                    tempHp = tempHp,
                    onTempHpChange = { tempHp = it },
                    currentHp = currentHp,
                    onCurrentHpChange = { currentHp = it },
                    onHealClick = {
                        hpDialogType = "heal"; hpDialogValue = ""; showHpDialog = true
                    },
                    onDamageClick = {
                        hpDialogType = "damage"; hpDialogValue = ""; showHpDialog = true
                    },
                    onTempClick = {
                        hpDialogType = "temp"; hpDialogValue = ""; showHpDialog = true
                    },
                    healthColor = healthColor,
                    clampHp = clampHp,
                    isArmorClassPanelVisible = isArmorClassPanelVisible,
                    armorClassEntries = armorClassEntries,
                    activeArmorClassId = activeArmorClassId,
                    acDeleteConfirmId = acDeleteConfirmId,
                    onArmorClassEntries = { armorClassEntries = it },
                    onActiveArmorClass = { activeArmorClassId = it },
                    onAcDeleteReq = { acDeleteConfirmId = it },
                    onAddArmorClass = { armorClassEntries = armorClassEntries + ArmorClassEntry() },
                    isInitiativePanelVisible = isInitiativePanelVisible,
                    initiativeEntries = initiativeEntries,
                    activeInitiativeId = activeInitiativeId,
                    initDeleteConfirmId = initDeleteConfirmId,
                    onInitiativeEntries = { initiativeEntries = it },
                    onActiveInitiative = { activeInitiativeId = it },
                    onInitDeleteReq = { initDeleteConfirmId = it },
                    onAddInitiative = { initiativeEntries = initiativeEntries + InitiativeEntry() },
                    isConditionsPanelVisible = isConditionsPanelVisible,
                    allConditions = allConditions,
                    selectedConditions = selectedConditions,
                    onToggleCondition = { n ->
                        selectedConditions =
                            if (selectedConditions.contains(n)) selectedConditions - n else selectedConditions + n
                    },
                    isSpeedPanelVisible = isSpeedPanelVisible,
                    speedEntries = speedEntries,
                    activeSpeedId = activeSpeedId,
                    speedDeleteConfirmId = speedDeleteConfirmId,
                    onSpeedEntries = { speedEntries = it },
                    onActiveSpeed = { activeSpeedId = it },
                    onSpeedDeleteReq = { speedDeleteConfirmId = it },
                    onAddSpeed = { speedEntries = speedEntries + SpeedEntry() }
                )

                Button(onClick = {}, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp, bottom = 20.dp).width(220.dp).height(44.dp), colors = ButtonDefaults.buttonColors(containerColor = colorScheme.secondaryContainer, contentColor = colorScheme.onSecondaryContainer), shape = RoundedCornerShape(8.dp), elevation = ButtonDefaults.buttonElevation(4.dp)) { Text("Расширенный режим", fontSize = 14.sp) }
                val evalPB = remember(proficiencyBonus, statsMap) { evaluateFormula(
                    proficiencyBonus,
                    statsMap
                ).toString() }
                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatCard("Сила", strength, evalPB, strProf, Modifier.weight(1f), { strength = it }, { strProf = it }); StatCard("Интеллект", intelligence, evalPB, intProf, Modifier.weight(1f), { intelligence = it }, { intProf = it }) }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatCard("Ловкость", dexterity, evalPB, dexProf, Modifier.weight(1f), { dexterity = it }, { dexProf = it }); StatCard("Мудрость", wisdom, evalPB, wisProf, Modifier.weight(1f), { wisdom = it }, { wisProf = it }) }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatCard("Телосложение", constitution, evalPB, conProf, Modifier.weight(1f), { constitution = it }, { conProf = it }); StatCard("Харизма", charisma, evalPB, chaProf, Modifier.weight(1f), { charisma = it }, { chaProf = it }) }
                }
                Spacer(Modifier.height(16.dp)); Text("Пассивные проверки", modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), textAlign = TextAlign.Center, fontSize = 15.sp, color = colorScheme.onSurface)
                Column(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(colorScheme.primary.copy(alpha = 0.1f)).padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PassiveCheckRow("Анализ (Интеллект)", (10 + calculateModifier(intelligence)).toString())
                    PassiveCheckRow("Внимательность (Мудрость)", (10 + calculateModifier(wisdom)).toString())
                    PassiveCheckRow("Проницательность (Мудрость)", (10 + calculateModifier(wisdom)).toString())
                }
                Spacer(Modifier.height(24.dp))
            }
        }
        if (showHpDialog) {
            AlertDialog(onDismissRequest = { showHpDialog = false },
                title = { Text(when(hpDialogType) { "heal" -> "Лечение"; "damage" -> "Получение урона"; else -> "Временные Хиты" }) },
                text = { OutlinedTextField(value = hpDialogValue, onValueChange = { hpDialogValue = it.filter { it.isDigit() } }, label = { Text("Значение") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true) },
                confirmButton = { TextButton(onClick = {
                    val v = hpDialogValue.toIntOrNull() ?: 0
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
                }) { Text("ОК") } }, dismissButton = { TextButton(onClick = { showHpDialog = false }) { Text("Отмена") } })
        }
    }
}

@Composable
fun StatCard(label: String, value: String, profB: String, isP: Boolean, modifier: Modifier = Modifier, onValue: (String) -> Unit, onPToggle: (Boolean) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme; val base = calculateModifier(value); val total = base + (if (isP) profB.toIntOrNull() ?: 0 else 0)
    Box(modifier = modifier.height(104.dp).shadow(2.dp, RoundedCornerShape(8.dp)).background(colorScheme.surface, RoundedCornerShape(8.dp)).border(1.dp, colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(8.dp)) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Box(modifier = Modifier.align(Alignment.BottomStart).padding(start = 22.dp, bottom = 2.dp).size(38.dp).rotate(if (isP) -45f else 0f).clip(if (isP) SquirclePath else RoundedCornerShape(8.dp)).background(if (isP) colorScheme.primaryContainer else colorScheme.surfaceVariant).border(1.dp, if (isP) colorScheme.primary else colorScheme.outline.copy(alpha = 0.2f), if (isP) SquirclePath else RoundedCornerShape(8.dp)).clickable { onPToggle(!isP) }, contentAlignment = Alignment.Center) {
            Text(if (total >= 0) "+$total" else "$total", modifier = Modifier.rotate(if (isP) 45f else 0f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(modifier = Modifier.align(Alignment.TopEnd), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)).background(colorScheme.surfaceVariant).border(1.dp, colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                BasicTextField(value = value, onValueChange = { val f = it.filter { it.isDigit() }; if (f.isEmpty()) onValue("") else { val n = f.toIntOrNull(); if (n != null && n in 1..30) onValue(f) } }, textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface), modifier = Modifier.width(32.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)).background(colorScheme.surface).border(1.dp, colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Text(if (base >= 0) "+$base" else "$base", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun PassiveCheckRow(label: String, value: String) {
    val colorScheme = MaterialTheme.colorScheme
    Row(modifier = Modifier.fillMaxWidth().height(30.dp).clip(RoundedCornerShape(8.dp)).background(colorScheme.primary.copy(alpha = 0.2f)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.padding(start = 12.dp), fontSize = 13.sp, color = colorScheme.onSurface)
        Text(value, modifier = Modifier.padding(end = 12.dp), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
fun CreateWindowPreview() { quasarisTheme { CreateWindow(onNavigateBack = {}, onCharacterChange = {}) } }
