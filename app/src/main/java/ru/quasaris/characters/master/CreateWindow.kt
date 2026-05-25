package ru.quasaris.characters.master

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import coil.compose.AsyncImage
import ru.quasaris.characters.master.ui.theme.quasarisTheme
import com.google.gson.Gson
import java.util.Stack
import kotlin.math.floor

// --- Вспомогательные функции ---

fun calculateModifier(scoreStr: String): Int {
    val score = scoreStr.toIntOrNull() ?: 10
    return floor((score - 10) / 2.0).toInt()
}

fun getProficiencyBonus(levelStr: String): Int {
    val level = levelStr.toIntOrNull() ?: 1
    return when {
        level >= 17 -> 6
        level >= 13 -> 5
        level >= 9 -> 4
        level >= 5 -> 3
        else -> 2
    }
}

fun getNextLevelThreshold(levelStr: String): String {
    val level = levelStr.toIntOrNull() ?: 1
    return when (level) {
        0, 1 -> "300"
        2 -> "900"
        3 -> "2700"
        4 -> "6500"
        5 -> "14000"
        6 -> "23000"
        7 -> "34000"
        8 -> "48000"
        9 -> "64000"
        10 -> "85000"
        11 -> "100000"
        12 -> "120000"
        13 -> "140000"
        14 -> "165000"
        15 -> "195000"
        16 -> "225000"
        17 -> "265000"
        18 -> "305000"
        19 -> "355000"
        else -> "—"
    }
}

fun calculateLevelFromExperience(expStr: String): Int {
    val exp = expStr.toLongOrNull() ?: 0L
    return when {
        exp >= 355000 -> 20
        exp >= 305000 -> 19
        exp >= 265000 -> 18
        exp >= 225000 -> 17
        exp >= 195000 -> 16
        exp >= 165000 -> 15
        exp >= 140000 -> 14
        exp >= 120000 -> 13
        exp >= 100000 -> 12
        exp >= 85000 -> 11
        exp >= 64000 -> 10
        exp >= 48000 -> 9
        exp >= 34000 -> 8
        exp >= 23000 -> 7
        exp >= 14000 -> 6
        exp >= 6500 -> 5
        exp >= 2700 -> 4
        exp >= 900 -> 3
        exp >= 300 -> 2
        else -> 1
    }
}

val SquirclePath = GenericShape { size, _ ->
    val r = size.width * 0.25f
    moveTo(r, 0f)
    lineTo(size.width - r, 0f)
    quadraticTo(size.width, 0f, size.width, r)
    lineTo(size.width, size.height - r)
    quadraticTo(size.width, size.height, size.width - r, size.height)
    lineTo(r, size.height)
    quadraticTo(0f, size.height, 0f, size.height - r)
    lineTo(0f, r)
    quadraticTo(0f, 0f, r, 0f)
    close()
}

fun evaluateFormula(formula: String, stats: Map<String, String>): Int {
    var processed = formula.uppercase()
    val statKeys = mapOf(
        "СИЛ" to "strength", "STR" to "strength",
        "ЛОВ" to "dexterity", "DEX" to "dexterity",
        "ТЕЛ" to "constitution", "CON" to "constitution",
        "ИНТ" to "intelligence", "INT" to "intelligence",
        "МУД" to "wisdom", "WIS" to "wisdom",
        "ХАР" to "charisma", "CHA" to "charisma"
    )
    statKeys.forEach { (key, statKey) ->
        val score = stats[statKey] ?: "10"
        val mod = calculateModifier(score).toString()
        processed = processed.replace("[$key ЗНАЧ]", score).replace("[$key SCR]", score)
            .replace("[$key]", mod).replace("[$key ", "$mod ")
    }
    val pb = stats["proficiencyBonus"] ?: "2"
    val level = stats["level"] ?: "1"
    val realPb = getProficiencyBonus(level).toString()
    processed = processed.replace("[БМ]", pb).replace("[PB]", pb)
        .replace("[НАСТ БМ]", realPb).replace("[REAL PB]", realPb)
    if (processed.contains("[БМ]") || processed.contains("[PB]")) {
        processed = processed.replace("[БМ]", realPb).replace("[PB]", realPb)
    }
    
    fun processFunctions(input: String): String {
        var current = input
        val functions = listOf(
            listOf("МАКС", "MAX", "НИЗ", "FLOOR") to true,
            listOf("МИН", "MIN", "ВЕРХ", "CEIL") to false
        )
        functions.forEach { (names, isMax) ->
            names.forEach { func ->
                val patternStandard = Regex("(?:\\[$func\\s*\\(([^()]+)\\)\\]|$func\\s*\\(([^()]+)\\))")
                while (current.contains(func)) {
                    val match = patternStandard.find(current) ?: break
                    val content = match.groupValues[1].ifEmpty { match.groupValues[2] }
                    val values = content.split(Regex("[;,]")).map { evaluateFormula(it.trim(), stats) }
                    val result = if (isMax) values.maxOrNull() ?: 0 else values.minOrNull() ?: 0
                    current = current.replace(match.value, result.toString())
                }
                val patternTrailing = Regex("(-?\\d+)[^\\d\\[]*\\[$func\\]\\s*\\((-?\\d+)\\)")
                while (current.contains("[$func]")) {
                    val match = patternTrailing.find(current) ?: break
                    val val1 = match.groupValues[1].toInt()
                    val val2 = match.groupValues[2].toInt()
                    val result = if (isMax) maxOf(val1, val2) else minOf(val1, val2)
                    current = current.replace(match.value, result.toString())
                }
            }
        }
        return current
    }
    processed = processFunctions(processed).replace(Regex("[^\\d+\\-*/]"), " ")
    return try {
        val clean = processed.replace(" ", "")
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < clean.length) {
            val c = clean[i]
            if (c.isDigit()) {
                val start = i
                while (i < clean.length && clean[i].isDigit()) i++
                tokens.add(clean.substring(start, i))
            } else if ("+*/".contains(c)) { tokens.add(c.toString()); i++ }
            else if (c == '-') {
                if (i > 0 && clean[i-1].isDigit()) { tokens.add("-"); i++ }
                else {
                    val s = i; i++; while (i < clean.length && clean[i].isDigit()) i++
                    if (i > s + 1) tokens.add(clean.substring(s, i)) else tokens.add("-")
                }
            } else i++
        }
        val vS = Stack<Int>(); val oS = Stack<String>()
        fun prc(o1: String, o2: String): Boolean = !((o1 == "*" || o1 == "/") && (o2 == "+" || o2 == "-"))
        fun app(op: String, b: Int, a: Int): Int = when (op) {
            "+" -> a + b; "-" -> a - b; "*" -> a * b; "/" -> if (b != 0) a / b else 0; else -> 0
        }
        for (token in tokens) {
            if (token.isEmpty()) continue
            if (token[0].isDigit() || (token.length > 1 && token[0] == '-' && token[1].isDigit())) vS.push(token.toInt())
            else if ("+-*/".contains(token)) {
                while (!oS.empty() && prc(token, oS.peek())) { if (vS.size < 2) break; vS.push(app(oS.pop(), vS.pop(), vS.pop())) }
                oS.push(token)
            }
        }
        while (!oS.empty() && vS.size >= 2) vS.push(app(oS.pop(), vS.pop(), vS.pop()))
        if (vS.isEmpty()) 0 else vS.pop()
    } catch (e: Exception) { 0 }
}

data class Condition(val name: String, val description: String)

fun parseConditions(content: String): List<Condition> {
    return content.split("##").filter { it.isNotBlank() }.map { s ->
        val lines = s.trim().lines()
        Condition(lines.firstOrNull()?.trim() ?: "", lines.drop(1).joinToString("\n").trim())
    }
}

fun formatDescription(text: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.lines()
        lines.forEachIndexed { i, line ->
            var l = line.trim()
            if (l.startsWith("- ")) l = l.substring(2)
            val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
            var last = 0
            boldRegex.findAll(l).forEach { m ->
                append(l.substring(last, m.range.first))
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(m.groupValues[1]) }
                last = m.range.last + 1
            }
            append(l.substring(last))
            if (i < lines.size - 1) append("\n")
        }
    }
}

// --- Основное окно ---

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
        try { context.assets.open("Conditions.md").bufferedReader().use { allConditions = parseConditions(it.readText()) } }
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
            Column(modifier = Modifier.background(colorScheme.surface).statusBarsPadding()) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.Menu, null, modifier = Modifier.size(32.dp), tint = colorScheme.onSurface) }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        BasicTextField(
                            value = name, onValueChange = { name = it },
                            textStyle = TextStyle(fontSize = 22.sp, textAlign = TextAlign.Center, color = colorScheme.onSurface, fontWeight = FontWeight.Normal),
                            cursorBrush = SolidColor(colorScheme.primary),
                            decorationBox = { if (name.isEmpty()) Text("Имя персонажа", fontSize = 22.sp, textAlign = TextAlign.Center, color = colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()); it() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Box(contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(colorScheme.primaryContainer).clickable { showAvatarMenu = true }, contentAlignment = Alignment.Center) {
                            if (selectedImageUri != null) AsyncImage(model = selectedImageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            else {
                                val bm = remember(character?.imageData) {
                                    if (character?.imageData != null) {
                                        try { val d = Base64.decode(character.imageData, Base64.DEFAULT); BitmapFactory.decodeByteArray(d, 0, d.size)?.asImageBitmap() } catch (e: Exception) { null }
                                    } else null
                                }
                                if (bm != null) Image(bitmap = bm, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                else Icon(Icons.Default.Person, null, tint = colorScheme.onPrimaryContainer)
                            }
                        }
                        DropdownMenu(expanded = showAvatarMenu, onDismissRequest = { showAvatarMenu = false }) {
                            DropdownMenuItem(text = { Text("Выбор изображения") }, leadingIcon = { Icon(Icons.Default.Image, null) }, onClick = { showAvatarMenu = false; imagePicker.launch("image/*") })
                            DropdownMenuItem(text = { Text("Скачать персонажа") }, leadingIcon = { Icon(Icons.Default.Download, null) }, onClick = { showAvatarMenu = false; fileCreator.launch("${if (name.isEmpty()) "character" else name}.json") })
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).height(24.dp).shadow(2.dp, RoundedCornerShape(20.dp)).background(colorScheme.surface, RoundedCornerShape(20.dp)).padding(2.dp).clickable { isLevelPanelVisible = !isLevelPanelVisible; isArmorClassPanelVisible = false; isInitiativePanelVisible = false; isSpeedPanelVisible = false; isHealthPanelVisible = false; isConditionsPanelVisible = false }) {
                    Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.width(90.dp).fillMaxHeight().clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)).background(colorScheme.primaryContainer), contentAlignment = Alignment.Center) { Text("$level уровень", fontSize = 11.sp, color = colorScheme.onPrimaryContainer) }
                        val pr = remember(experience, nextLevelExp) { val c = experience.toFloatOrNull() ?: 0f; val n = nextLevelExp.toFloatOrNull() ?: 0f; if (n <= 0f) 1f else (c / n).coerceIn(0f, 1f) }
                        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)).background(colorScheme.surface)) {
                            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(pr).background(colorScheme.primaryContainer))
                            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Spacer(Modifier.weight(0.4f)); Text("$experience | $nextLevelExp", fontSize = 11.sp, color = colorScheme.onSurface); Spacer(Modifier.weight(0.6f))
                                val nxt = (level.toIntOrNull() ?: 0) + 1; Text(if (nxt <= 20) "$nxt" else "", fontSize = 11.sp, color = colorScheme.onSurface)
                            }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatIconBox(activeACValue, R.drawable.ic_shield, onClick = { isArmorClassPanelVisible = !isArmorClassPanelVisible; isInitiativePanelVisible = false; isSpeedPanelVisible = false; isLevelPanelVisible = false; isHealthPanelVisible = false; isConditionsPanelVisible = false })
                        StatIconBox(activeInitValue, R.drawable.ic_sword, onClick = { isInitiativePanelVisible = !isInitiativePanelVisible; isArmorClassPanelVisible = false; isSpeedPanelVisible = false; isLevelPanelVisible = false; isHealthPanelVisible = false; isConditionsPanelVisible = false })
                    }
                    Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp).height(55.dp).border(1.5.dp, healthColor, RoundedCornerShape(8.dp)).background(colorScheme.surface, RoundedCornerShape(8.dp)).clickable { isHealthPanelVisible = !isHealthPanelVisible; isArmorClassPanelVisible = false; isInitiativePanelVisible = false; isSpeedPanelVisible = false; isLevelPanelVisible = false; isConditionsPanelVisible = false }, contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(painterResource(healthIcon), null, modifier = Modifier.size(32.dp), colorFilter = ColorFilter.tint(healthColor))
                            Spacer(Modifier.width(6.dp)); Text("$currentHp / ${maxHp.toIntOrNull() ?: 0}", color = healthColor, fontSize = 16.sp)
                            if ((tempHp.toIntOrNull() ?: 0) > 0) Text(" (+$tempHp)", color = healthColor.copy(alpha = 0.7f), fontSize = 14.sp)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatIconBox(selectedConditions.size.toString(), R.drawable.ic_conditions, onClick = { isConditionsPanelVisible = !isConditionsPanelVisible; isArmorClassPanelVisible = false; isInitiativePanelVisible = false; isSpeedPanelVisible = false; isLevelPanelVisible = false; isHealthPanelVisible = false })
                        StatIconBox(activeSpeedValue, R.drawable.ic_speed, onClick = { isSpeedPanelVisible = !isSpeedPanelVisible; isArmorClassPanelVisible = false; isInitiativePanelVisible = false; isLevelPanelVisible = false; isHealthPanelVisible = false; isConditionsPanelVisible = false })
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(colorScheme.background).clickable(remember { MutableInteractionSource() }, null) { focusManager.clearFocus(); acDeleteConfirmId = null; initDeleteConfirmId = null; speedDeleteConfirmId = null }) {
            Column(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(colorScheme.surface).verticalScroll(rememberScrollState())) {
                Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Brush.verticalGradient(listOf(colorScheme.onSurface.copy(alpha = 0.15f), Color.Transparent))))
                AnimatedVisibility(isLevelPanelVisible, enter = expandVertically(), exit = shrinkVertically()) { LevelPanel(level, { level = it }, experience, { experience = it }, proficiencyBonus, { proficiencyBonus = it }, nextLevelExp, statsMap) }
                AnimatedVisibility(isHealthPanelVisible, enter = expandVertically(), exit = shrinkVertically()) { HealthPanel(maxHp, { maxHp = it }, tempHp, { tempHp = it }, currentHp, { currentHp = it }, { hpDialogType = "heal"; hpDialogValue = ""; showHpDialog = true }, { hpDialogType = "damage"; hpDialogValue = ""; showHpDialog = true }, { hpDialogType = "temp"; hpDialogValue = ""; showHpDialog = true }, healthColor, clampHp) }
                AnimatedVisibility(isArmorClassPanelVisible, enter = expandVertically(), exit = shrinkVertically()) { FormulaPanel("Класс Доспеха", armorClassEntries, activeArmorClassId, acDeleteConfirmId, { armorClassEntries = it.filterIsInstance<ArmorClassEntry>() }, { activeArmorClassId = it }, { acDeleteConfirmId = it }, { armorClassEntries = armorClassEntries + ArmorClassEntry() }) }
                AnimatedVisibility(isInitiativePanelVisible, enter = expandVertically(), exit = shrinkVertically()) { FormulaPanel("Инициатива", initiativeEntries, activeInitiativeId, initDeleteConfirmId, { initiativeEntries = it.filterIsInstance<InitiativeEntry>() }, { activeInitiativeId = it }, { initDeleteConfirmId = it }, { initiativeEntries = initiativeEntries + InitiativeEntry() }) }
                AnimatedVisibility(isConditionsPanelVisible, enter = expandVertically(), exit = shrinkVertically()) { ConditionsPanel(allConditions, selectedConditions) { n -> selectedConditions = if (selectedConditions.contains(n)) selectedConditions - n else selectedConditions + n } }
                AnimatedVisibility(isSpeedPanelVisible, enter = expandVertically(), exit = shrinkVertically()) { FormulaPanel("Скорость", speedEntries, activeSpeedId, speedDeleteConfirmId, { speedEntries = it.filterIsInstance<SpeedEntry>() }, { activeSpeedId = it }, { speedDeleteConfirmId = it }, { speedEntries = speedEntries + SpeedEntry() }) }
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).height(40.dp).clip(RoundedCornerShape(8.dp)).background(colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Характеристики", fontSize = 12.sp, color = colorScheme.onPrimaryContainer.copy(alpha = 0.7f), modifier = Modifier.weight(1f))
                        Text("Характеристики", fontSize = 18.sp, color = colorScheme.onPrimaryContainer, modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center)
                        Text("Характеристики", fontSize = 12.sp, color = colorScheme.onPrimaryContainer.copy(alpha = 0.7f), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    }
                }
                Button(onClick = {}, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp, bottom = 20.dp).width(220.dp).height(44.dp), colors = ButtonDefaults.buttonColors(containerColor = colorScheme.secondaryContainer, contentColor = colorScheme.onSecondaryContainer), shape = RoundedCornerShape(8.dp), elevation = ButtonDefaults.buttonElevation(4.dp)) { Text("Расширенный режим", fontSize = 14.sp) }
                val evalPB = remember(proficiencyBonus, statsMap) { evaluateFormula(proficiencyBonus, statsMap).toString() }
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
fun ConditionsPanel(allConditions: List<Condition>, selectedConditions: List<String>, onToggleCondition: (String) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).shadow(4.dp, RoundedCornerShape(12.dp)).background(colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).border(1.dp, colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).animateContentSize()) {
        Text("Состояния", modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally), style = MaterialTheme.typography.titleMedium, color = colorScheme.onSurfaceVariant)
        allConditions.forEach { condition ->
            ConditionItem(condition, selectedConditions.contains(condition.name)) { onToggleCondition(condition.name) }
            HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.15f), thickness = 1.dp)
        }
    }
}

@Composable
fun ConditionItem(condition: Condition, isSelected: Boolean, onToggle: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme; var expanded by remember { mutableStateOf(false) }
    val sep = colorScheme.outline.copy(alpha = 0.2f)
    Column(modifier = Modifier.fillMaxWidth().background(if (isSelected) colorScheme.primaryContainer else Color.Transparent).animateContentSize()) {
        Row(modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable { expanded = !expanded }, verticalAlignment = Alignment.CenterVertically) {
            Text(condition.name, modifier = Modifier.weight(1f).padding(horizontal = 12.dp), fontSize = 16.sp, color = colorScheme.onSurface, textAlign = TextAlign.Center)
            Box(modifier = Modifier.width(1.2.dp).fillMaxHeight().background(sep))
            Box(modifier = Modifier.width(44.dp).fillMaxHeight().clickable { onToggle() }, contentAlignment = Alignment.Center) {
                Icon(if (isSelected) Icons.Default.Close else Icons.Default.Check, null, modifier = Modifier.size(20.dp), tint = if (isSelected) colorScheme.error else colorScheme.onSurface)
            }
        }
        if (expanded) {
            HorizontalDivider(color = sep, thickness = 1.2.dp)
            Text(formatDescription(condition.description), modifier = Modifier.padding(16.dp), fontSize = 14.sp, color = colorScheme.onSurface.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun HealthPanel(maxHp: String, onMaxHpChange: (String) -> Unit, tempHp: String, onTempHpChange: (String) -> Unit, currentHp: String, onCurrentHpChange: (String) -> Unit, onHealClick: () -> Unit, onDamageClick: () -> Unit, onTempClick: () -> Unit, healthColor: Color, onFocusLost: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).shadow(4.dp, RoundedCornerShape(12.dp)).background(colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).border(1.dp, colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).animateContentSize()) {
        Text("Хиты", modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally), style = MaterialTheme.typography.titleMedium, color = healthColor)
        HealthRow("Максимум Хитов", maxHp, { onMaxHpChange(minOf(999, it.toIntOrNull() ?: 0).toString()) }, onFocusLost)
        HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.15f)); HealthRow("Текущие Хиты", currentHp, { onCurrentHpChange(minOf(999, it.toIntOrNull() ?: 0).toString()) }, onFocusLost)
        HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.15f)); HealthRow("Временные Хиты", tempHp, { onTempHpChange(minOf(9999, it.toIntOrNull() ?: 0).toString()) })
        Spacer(Modifier.height(12.dp)); HealthActionRow("Лечение", Color(0xFF00C46F), onHealClick); HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.15f))
        HealthActionRow("Получение урона", Color(0xFFE57373), onDamageClick); HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.15f)); HealthActionRow("Укрепление", Color(0xFF64B5F6), onTempClick); Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun HealthRow(label: String, value: String, onValueChange: (String) -> Unit, onFocusLost: () -> Unit = {}) {
    val colorScheme = MaterialTheme.colorScheme; var tv by remember { mutableStateOf(TextFieldValue(value)) }
    val fr = remember { FocusRequester() }; LaunchedEffect(value) { if (tv.text != value) tv = tv.copy(text = value, selection = TextRange(value.length)) }
    Row(modifier = Modifier.fillMaxWidth().height(48.dp).clickable { tv = tv.copy(selection = TextRange(value.length)); fr.requestFocus() }, verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.padding(start = 16.dp).weight(1f), fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
        BasicTextField(value = tv, onValueChange = { tv = it; onValueChange(it.text.filter { c -> c.isDigit() || c == '-' }) }, textStyle = TextStyle(textAlign = TextAlign.End, fontSize = 16.sp, color = colorScheme.onSurface, fontWeight = FontWeight.Bold), modifier = Modifier.width(100.dp).padding(end = 16.dp).focusRequester(fr).onFocusChanged { if (!it.isFocused) onFocusLost() }, cursorBrush = SolidColor(colorScheme.primary), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
    }
}

@Composable
fun HealthActionRow(text: String, color: Color, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(48.dp).clickable { onClick() }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) { Text(text, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = color) }
}

@Composable
fun LevelPanel(level: String, onLevelChange: (String) -> Unit, exp: String, onExpChange: (String) -> Unit, prof: String, onProfChange: (String) -> Unit, nextExp: String, stats: Map<String, String>) {
    val colorScheme = MaterialTheme.colorScheme; var ltv by remember { mutableStateOf(TextFieldValue(level)) }; var etv by remember { mutableStateOf(TextFieldValue(exp)) }; var ptv by remember { mutableStateOf(TextFieldValue(prof)) }
    var isPFocused by remember { mutableStateOf(false) }; LaunchedEffect(level) { if (ltv.text != level) ltv = ltv.copy(text = level, selection = TextRange(level.length)) }
    LaunchedEffect(exp) { if (etv.text != exp) etv = etv.copy(text = exp, selection = TextRange(exp.length)) }
    LaunchedEffect(prof, isPFocused, stats) { val d = if (isPFocused) prof else evaluateFormula(prof, stats).toString(); if (ptv.text != d) ptv = TextFieldValue(text = d, selection = if (isPFocused) TextRange(d.length) else TextRange.Zero) }
    val fl = remember { FocusRequester() }; val fe = remember { FocusRequester() }; val fp = remember { FocusRequester() }
    val targetLvl = calculateLevelFromExperience(exp); val canUp = targetLvl != (level.toIntOrNull() ?: 1)
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).shadow(4.dp, RoundedCornerShape(12.dp)).background(colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).border(1.dp, colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).animateContentSize()) {
        Text("Уровень и Опыт", modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally), style = MaterialTheme.typography.titleMedium, color = colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.fillMaxWidth().height(48.dp).clickable { ltv = ltv.copy(selection = TextRange(level.length)); fl.requestFocus() }, verticalAlignment = Alignment.CenterVertically) {
            Text("Уровень персонажа", modifier = Modifier.padding(start = 16.dp).weight(1f), fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
            BasicTextField(value = ltv, onValueChange = { ltv = it; val f = it.text.filter { it.isDigit() }; if (f.isEmpty()) onLevelChange("") else { val n = f.toIntOrNull(); if (n != null && n in 0..20) onLevelChange(n.toString()) } }, textStyle = TextStyle(textAlign = TextAlign.End, fontSize = 16.sp, color = colorScheme.onSurface, fontWeight = FontWeight.Bold), modifier = Modifier.width(100.dp).padding(end = 16.dp).focusRequester(fl).onFocusChanged { if (!it.isFocused) { if (level.isEmpty()) onLevelChange("0") } }, cursorBrush = SolidColor(colorScheme.primary), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
        HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.15f)); Row(modifier = Modifier.fillMaxWidth().height(48.dp).clickable { etv = etv.copy(selection = TextRange(exp.length)); fe.requestFocus() }, verticalAlignment = Alignment.CenterVertically) {
            Text("Текущий опыт", modifier = Modifier.padding(start = 16.dp).weight(1f), fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
            BasicTextField(value = etv, onValueChange = { etv = it; onExpChange(it.text.filter { it.isDigit() }) }, textStyle = TextStyle(textAlign = TextAlign.End, fontSize = 16.sp, color = colorScheme.onSurface, fontWeight = FontWeight.Bold), modifier = Modifier.width(100.dp).padding(end = 4.dp).focusRequester(fe).onFocusChanged { if (!it.isFocused) { if (exp.isEmpty()) onExpChange("0") } }, cursorBrush = SolidColor(colorScheme.primary), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Text("/ $nextExp", modifier = Modifier.padding(end = 16.dp), fontSize = 14.sp, color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
        HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.15f)); Row(modifier = Modifier.fillMaxWidth().height(48.dp).clickable { fp.requestFocus() }, verticalAlignment = Alignment.CenterVertically) {
            Text("Бонус Мастерства", modifier = Modifier.padding(start = 16.dp).weight(1f), fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!isPFocused && evaluateFormula(prof, stats) >= 0) Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                BasicTextField(value = ptv, onValueChange = { if (isPFocused) { ptv = it; onProfChange(it.text) } }, textStyle = TextStyle(textAlign = if (isPFocused) TextAlign.Start else TextAlign.End, fontSize = 16.sp, color = colorScheme.onSurface, fontWeight = FontWeight.Bold), modifier = Modifier.width(IntrinsicSize.Min).defaultMinSize(10.dp).focusRequester(fp).onFocusChanged { fs -> if (isPFocused != fs.isFocused) { isPFocused = fs.isFocused; if (!fs.isFocused && prof.isEmpty()) onProfChange("[НАСТ БМ]") } }, cursorBrush = SolidColor(colorScheme.primary))
            }
        }
        Spacer(Modifier.height(8.dp)); Button(onClick = { if (canUp) onLevelChange(targetLvl.toString()) }, enabled = canUp, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(40.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = if (canUp) colorScheme.primary else colorScheme.outline.copy(alpha = 0.12f))) { Text(if (targetLvl > (level.toIntOrNull() ?: 1)) "Повысить уровень" else "Понизить уровень", fontSize = 14.sp) }
    }
}

@Composable
fun FormulaPanel(title: String, entries: List<FormulaEntry>, activeId: String?, deleteId: String?, onEntries: (List<FormulaEntry>) -> Unit, onActive: (String?) -> Unit, onDeleteReq: (String?) -> Unit, onAdd: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).shadow(4.dp, RoundedCornerShape(12.dp)).background(colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).border(1.dp, colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).animateContentSize()) {
        Text(title, modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally), style = MaterialTheme.typography.titleMedium, color = colorScheme.onSurfaceVariant)
        entries.forEachIndexed { i, entry ->
            FormulaEntryItem(entry, entry.id == activeId, entry.id == deleteId, { u -> val nl = entries.toMutableList(); nl[i] = u; onEntries(nl) }, { val nl = entries.toMutableList(); nl.removeAt(i); if (entry.id == activeId) onActive(null); onEntries(nl); onDeleteReq(null) }, { onDeleteReq(entry.id) }, { onActive(if (entry.id == activeId) null else entry.id); onDeleteReq(null) })
            HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.15f))
        }
        Row(modifier = Modifier.fillMaxWidth().height(48.dp).clickable { onAdd(); onDeleteReq(null) }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) { Icon(Icons.Default.AddCircleOutline, null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Добавить Новое", fontSize = 16.sp) }
    }
}

@Composable
fun FormulaEntryItem(entry: FormulaEntry, isActive: Boolean, isDelete: Boolean, onUpdate: (FormulaEntry) -> Unit, onDelete: () -> Unit, onDeleteReq: () -> Unit, onToggle: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme; val sep = colorScheme.outline.copy(alpha = 0.2f)
    Column(modifier = Modifier.fillMaxWidth().background(if (isActive) colorScheme.primaryContainer else Color.Transparent).animateContentSize()) {
        Row(modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(44.dp).fillMaxHeight().clickable { if (isDelete) onDelete() else onDeleteReq() }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Delete, null, modifier = Modifier.size(20.dp), tint = if (isDelete) colorScheme.error else colorScheme.onSurface.copy(alpha = 0.7f)) }
            Box(modifier = Modifier.width(1.2.dp).fillMaxHeight().background(sep))
            Box(modifier = Modifier.weight(1f).padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                if (entry.name.isEmpty()) Text("Название", color = colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 16.sp)
                BasicTextField(value = entry.name, onValueChange = { val u = when(entry) { is ArmorClassEntry -> entry.copy(name = it); is InitiativeEntry -> entry.copy(name = it); is SpeedEntry -> entry.copy(name = it); else -> entry }; onUpdate(u) }, textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 16.sp, color = colorScheme.onSurface), modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp))
            }
            Box(modifier = Modifier.width(1.2.dp).fillMaxHeight().background(sep))
            Box(modifier = Modifier.width(44.dp).fillMaxHeight().clickable { onToggle() }, contentAlignment = Alignment.Center) { Icon(if (isActive) Icons.Default.Close else Icons.Default.Check, null, modifier = Modifier.size(20.dp)) }
        }
        HorizontalDivider(color = sep, thickness = 1.2.dp); Box(modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(horizontal = 16.dp, vertical = 8.dp)) {
            if (entry.formula.isEmpty()) Text("Формула", color = colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 14.sp)
            BasicTextField(value = entry.formula, onValueChange = { val u = when(entry) { is ArmorClassEntry -> entry.copy(formula = it); is InitiativeEntry -> entry.copy(formula = it); is SpeedEntry -> entry.copy(formula = it); else -> entry }; onUpdate(u) }, textStyle = TextStyle(fontSize = 14.sp, color = colorScheme.onSurface), modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun StatIconBox(value: String, iconRes: Int, onClick: () -> Unit = {}) {
    val colorScheme = MaterialTheme.colorScheme
    Box(modifier = Modifier.size(42.dp).clickable(remember { MutableInteractionSource() }, null, onClick = onClick), contentAlignment = Alignment.Center) {
        val tint = colorScheme.primary.copy(alpha = 0.5f)
        if (iconRes == R.drawable.ic_sword) {
            Box(Modifier.fillMaxSize()) {
                Image(painterResource(R.drawable.ic_sword), null, modifier = Modifier.size(42.dp), colorFilter = ColorFilter.tint(tint))
                Image(painterResource(R.drawable.ic_sword), null, modifier = Modifier.size(42.dp).graphicsLayer(scaleX = -1f), colorFilter = ColorFilter.tint(tint))
            }
        } else Image(painterResource(iconRes), null, modifier = Modifier.fillMaxSize(), colorFilter = ColorFilter.tint(tint))
        Text(value, fontSize = 15.sp, color = colorScheme.onSurface, fontWeight = FontWeight.Bold, style = TextStyle(shadow = Shadow(colorScheme.surface, Offset.Zero, 14f)))
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
