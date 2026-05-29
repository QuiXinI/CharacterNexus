package ru.quasaris.characters.master

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import coil.compose.AsyncImage
import java.util.Stack
import kotlin.math.floor

// --- Вспомогательные функции и модели ---

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

// --- Компоненты Шапки ---

@Composable
fun CharacterHeader(
    name: String,
    onNameChange: (String) -> Unit,
    level: String,
    experience: String,
    nextLevelExp: String,
    selectedImageUri: Uri?,
    characterImageData: String?,
    onAvatarClick: () -> Unit,
    onLevelClick: () -> Unit,
    onNavigateBack: () -> Unit,
    activeACValue: String,
    onACClick: () -> Unit,
    activeInitValue: String,
    onInitClick: () -> Unit,
    currentHp: String,
    maxHp: String,
    tempHp: String,
    healthColor: Color,
    healthIcon: Int,
    onHealthClick: () -> Unit,
    conditionsCount: String,
    onConditionsClick: () -> Unit,
    activeSpeedValue: String,
    onSpeedClick: () -> Unit,
    showAvatarMenu: Boolean,
    onDismissAvatarMenu: () -> Unit,
    onImagePickerClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.background(colorScheme.surface).statusBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) { Icon(Icons.Default.Menu, null, modifier = Modifier.size(32.dp), tint = colorScheme.onSurface) }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                BasicTextField(
                    value = name, onValueChange = onNameChange,
                    textStyle = TextStyle(fontSize = 22.sp, textAlign = TextAlign.Center, color = colorScheme.onSurface, fontWeight = FontWeight.Normal),
                    cursorBrush = SolidColor(colorScheme.primary),
                    decorationBox = { innerTextField -> if (name.isEmpty()) Text("Имя персонажа", fontSize = 22.sp, textAlign = TextAlign.Center, color = colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()); innerTextField() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Box(contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(colorScheme.primaryContainer).clickable { onAvatarClick() }, contentAlignment = Alignment.Center) {
                    if (selectedImageUri != null) AsyncImage(model = selectedImageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else {
                        val bm = remember(characterImageData) {
                            if (characterImageData != null) {
                                try { val d = Base64.decode(characterImageData, Base64.DEFAULT); BitmapFactory.decodeByteArray(d, 0, d.size)?.asImageBitmap() } catch (e: Exception) { null }
                            } else null
                        }
                        if (bm != null) Image(bitmap = bm, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else Icon(Icons.Default.Person, null, tint = colorScheme.onPrimaryContainer)
                    }
                }
                DropdownMenu(expanded = showAvatarMenu, onDismissRequest = onDismissAvatarMenu) {
                    DropdownMenuItem(text = { Text("Выбор изображения") }, leadingIcon = { Icon(Icons.Default.Image, null) }, onClick = onImagePickerClick)
                    DropdownMenuItem(text = { Text("Скачать персонажа") }, leadingIcon = { Icon(Icons.Default.Download, null) }, onClick = onDownloadClick)
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).height(24.dp).shadow(2.dp, RoundedCornerShape(20.dp)).background(colorScheme.surface, RoundedCornerShape(20.dp)).padding(2.dp).clickable { onLevelClick() }) {
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
                StatIconBox(activeACValue, R.drawable.ic_shield, onClick = onACClick)
                StatIconBox(activeInitValue, R.drawable.ic_sword, onClick = onInitClick)
            }
            Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp).height(55.dp).border(1.5.dp, healthColor, RoundedCornerShape(8.dp)).background(colorScheme.surface, RoundedCornerShape(8.dp)).clickable { onHealthClick() }, contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(healthIcon), null, modifier = Modifier.size(32.dp), colorFilter = ColorFilter.tint(healthColor))
                    Spacer(Modifier.width(6.dp)); Text("$currentHp / ${maxHp.toIntOrNull() ?: 0}", color = healthColor, fontSize = 16.sp)
                    if ((tempHp.toIntOrNull() ?: 0) > 0) Text(" (+$tempHp)", color = healthColor.copy(alpha = 0.7f), fontSize = 14.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatIconBox(conditionsCount, R.drawable.ic_conditions, onClick = onConditionsClick)
                StatIconBox(activeSpeedValue, R.drawable.ic_speed, onClick = onSpeedClick)
            }
        }
    }
}

@Composable
fun ExpandingPanelsSection(
    isLevelPanelVisible: Boolean,
    level: String,
    onLevelChange: (String) -> Unit,
    experience: String,
    onExpChange: (String) -> Unit,
    proficiencyBonus: String,
    onProfChange: (String) -> Unit,
    nextLevelExp: String,
    statsMap: Map<String, String>,
    
    isHealthPanelVisible: Boolean,
    maxHp: String,
    onMaxHpChange: (String) -> Unit,
    tempHp: String,
    onTempHpChange: (String) -> Unit,
    currentHp: String,
    onCurrentHpChange: (String) -> Unit,
    onHealClick: () -> Unit,
    onDamageClick: () -> Unit,
    onTempClick: () -> Unit,
    healthColor: Color,
    clampHp: () -> Unit,
    
    isArmorClassPanelVisible: Boolean,
    armorClassEntries: List<ArmorClassEntry>,
    activeArmorClassId: String?,
    acDeleteConfirmId: String?,
    onArmorClassEntries: (List<ArmorClassEntry>) -> Unit,
    onActiveArmorClass: (String?) -> Unit,
    onAcDeleteReq: (String?) -> Unit,
    onAddArmorClass: () -> Unit,
    
    isInitiativePanelVisible: Boolean,
    initiativeEntries: List<InitiativeEntry>,
    activeInitiativeId: String?,
    initDeleteConfirmId: String?,
    onInitiativeEntries: (List<InitiativeEntry>) -> Unit,
    onActiveInitiative: (String?) -> Unit,
    onInitDeleteReq: (String?) -> Unit,
    onAddInitiative: () -> Unit,
    
    isConditionsPanelVisible: Boolean,
    allConditions: List<Condition>,
    selectedConditions: List<String>,
    onToggleCondition: (String) -> Unit,
    
    isSpeedPanelVisible: Boolean,
    speedEntries: List<SpeedEntry>,
    activeSpeedId: String?,
    speedDeleteConfirmId: String?,
    onSpeedEntries: (List<SpeedEntry>) -> Unit,
    onActiveSpeed: (String?) -> Unit,
    onSpeedDeleteReq: (String?) -> Unit,
    onAddSpeed: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column {
        Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Brush.verticalGradient(listOf(colorScheme.onSurface.copy(alpha = 0.15f), Color.Transparent))))
        AnimatedVisibility(isLevelPanelVisible, enter = expandVertically(), exit = shrinkVertically()) { LevelPanel(level, onLevelChange, experience, onExpChange, proficiencyBonus, onProfChange, nextLevelExp, statsMap) }
        AnimatedVisibility(isHealthPanelVisible, enter = expandVertically(), exit = shrinkVertically()) { HealthPanel(maxHp, onMaxHpChange, tempHp, onTempHpChange, currentHp, onCurrentHpChange, onHealClick, onDamageClick, onTempClick, healthColor, clampHp) }
        AnimatedVisibility(isArmorClassPanelVisible, enter = expandVertically(), exit = shrinkVertically()) { FormulaPanel("Класс Доспеха", armorClassEntries, activeArmorClassId, acDeleteConfirmId, { updated -> onArmorClassEntries(updated.filterIsInstance<ArmorClassEntry>()) }, onActiveArmorClass, onAcDeleteReq, onAddArmorClass) }
        AnimatedVisibility(isInitiativePanelVisible, enter = expandVertically(), exit = shrinkVertically()) { FormulaPanel("Инициатива", initiativeEntries, activeInitiativeId, initDeleteConfirmId, { updated -> onInitiativeEntries(updated.filterIsInstance<InitiativeEntry>()) }, onActiveInitiative, onInitDeleteReq, onAddInitiative) }
        AnimatedVisibility(isConditionsPanelVisible, enter = expandVertically(), exit = shrinkVertically()) { ConditionsPanel(allConditions, selectedConditions, onToggleCondition) }
        AnimatedVisibility(isSpeedPanelVisible, enter = expandVertically(), exit = shrinkVertically()) { FormulaPanel("Скорость", speedEntries, activeSpeedId, speedDeleteConfirmId, { updated -> onSpeedEntries(updated.filterIsInstance<SpeedEntry>()) }, onActiveSpeed, onSpeedDeleteReq, onAddSpeed) }
        
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).height(40.dp).clip(RoundedCornerShape(8.dp)).background(colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Характеристики", fontSize = 12.sp, color = colorScheme.onPrimaryContainer.copy(alpha = 0.7f), modifier = Modifier.weight(1f))
                Text("Характеристики", fontSize = 18.sp, color = colorScheme.onPrimaryContainer, modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center)
                Text("Характеристики", fontSize = 12.sp, color = colorScheme.onPrimaryContainer.copy(alpha = 0.7f), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }
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
        HealthRow("Максимум Хитов", maxHp, { s -> onMaxHpChange(minOf(999, s.toIntOrNull() ?: 0).toString()) }, onFocusLost)
        HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.15f)); HealthRow("Текущие Хиты", currentHp, { s -> onCurrentHpChange(minOf(999, s.toIntOrNull() ?: 0).toString()) }, onFocusLost)
        HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.15f)); HealthRow("Временные Хиты", tempHp, { s -> onTempHpChange(minOf(9999, s.toIntOrNull() ?: 0).toString()) })
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
        BasicTextField(value = tv, onValueChange = { textFieldValue -> tv = textFieldValue; onValueChange(textFieldValue.text.filter { c -> c.isDigit() || c == '-' }) }, textStyle = TextStyle(textAlign = TextAlign.End, fontSize = 16.sp, color = colorScheme.onSurface, fontWeight = FontWeight.Bold), modifier = Modifier.width(100.dp).padding(end = 16.dp).focusRequester(fr).onFocusChanged { state -> if (!state.isFocused) onFocusLost() }, cursorBrush = SolidColor(colorScheme.primary), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
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
            BasicTextField(value = ltv, onValueChange = { textFieldValue -> ltv = textFieldValue; val f = textFieldValue.text.filter { it.isDigit() }; if (f.isEmpty()) onLevelChange("") else { val n = f.toIntOrNull(); if (n != null && n in 0..20) onLevelChange(n.toString()) } }, textStyle = TextStyle(textAlign = TextAlign.End, fontSize = 16.sp, color = colorScheme.onSurface, fontWeight = FontWeight.Bold), modifier = Modifier.width(100.dp).padding(end = 16.dp).focusRequester(fl).onFocusChanged { state -> if (!state.isFocused) { if (level.isEmpty()) onLevelChange("0") } }, cursorBrush = SolidColor(colorScheme.primary), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
        HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.15f)); Row(modifier = Modifier.fillMaxWidth().height(48.dp).clickable { etv = etv.copy(selection = TextRange(exp.length)); fe.requestFocus() }, verticalAlignment = Alignment.CenterVertically) {
            Text("Текущий опыт", modifier = Modifier.padding(start = 16.dp).weight(1f), fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
            BasicTextField(value = etv, onValueChange = { textFieldValue -> etv = textFieldValue; onExpChange(textFieldValue.text.filter { it.isDigit() }) }, textStyle = TextStyle(textAlign = TextAlign.End, fontSize = 16.sp, color = colorScheme.onSurface, fontWeight = FontWeight.Bold), modifier = Modifier.width(100.dp).padding(end = 4.dp).focusRequester(fe).onFocusChanged { state -> if (!state.isFocused) { if (exp.isEmpty()) onExpChange("0") } }, cursorBrush = SolidColor(colorScheme.primary), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Text("/ $nextExp", modifier = Modifier.padding(end = 16.dp), fontSize = 14.sp, color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
        HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.15f)); Row(modifier = Modifier.fillMaxWidth().height(48.dp).clickable { fp.requestFocus() }, verticalAlignment = Alignment.CenterVertically) {
            Text("Бонус Мастерства", modifier = Modifier.padding(start = 16.dp).weight(1f), fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!isPFocused && evaluateFormula(prof, stats) >= 0) Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                BasicTextField(value = ptv, onValueChange = { textFieldValue -> if (isPFocused) { ptv = textFieldValue; onProfChange(textFieldValue.text) } }, textStyle = TextStyle(textAlign = if (isPFocused) TextAlign.Start else TextAlign.End, fontSize = 16.sp, color = colorScheme.onSurface, fontWeight = FontWeight.Bold), modifier = Modifier.width(IntrinsicSize.Min).defaultMinSize(10.dp).focusRequester(fp).onFocusChanged { state -> if (isPFocused != state.isFocused) { isPFocused = state.isFocused; if (!state.isFocused && prof.isEmpty()) onProfChange("[НАСТ БМ]") } }, cursorBrush = SolidColor(colorScheme.primary))
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
            FormulaEntryItem(entry, entry.id == activeId, entry.id == deleteId, { updated -> val nl = entries.toMutableList(); nl[i] = updated; onEntries(nl) }, { val nl = entries.toMutableList(); nl.removeAt(i); if (entry.id == activeId) onActive(null); onEntries(nl); onDeleteReq(null) }, { onDeleteReq(entry.id) }, { onActive(if (entry.id == activeId) null else entry.id); onDeleteReq(null) })
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
                BasicTextField(value = entry.name, onValueChange = { s -> 
                    val u: FormulaEntry = when(entry) { is ArmorClassEntry -> entry.copy(name = s); is InitiativeEntry -> entry.copy(name = s); is SpeedEntry -> entry.copy(name = s); else -> entry }
                    onUpdate(u) 
                }, textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 16.sp, color = colorScheme.onSurface), modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp))
            }
            Box(modifier = Modifier.width(1.2.dp).fillMaxHeight().background(sep))
            Box(modifier = Modifier.width(44.dp).fillMaxHeight().clickable { onToggle() }, contentAlignment = Alignment.Center) { Icon(if (isActive) Icons.Default.Close else Icons.Default.Check, null, modifier = Modifier.size(20.dp)) }
        }
        HorizontalDivider(color = sep, thickness = 1.2.dp); Box(modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(horizontal = 16.dp, vertical = 8.dp)) {
            if (entry.formula.isEmpty()) Text("Формула", color = colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 14.sp)
            BasicTextField(value = entry.formula, onValueChange = { s -> 
                val u: FormulaEntry = when(entry) { is ArmorClassEntry -> entry.copy(formula = s); is InitiativeEntry -> entry.copy(formula = s); is SpeedEntry -> entry.copy(formula = s); else -> entry }
                onUpdate(u) 
            }, textStyle = TextStyle(fontSize = 14.sp, color = colorScheme.onSurface), modifier = Modifier.fillMaxWidth())
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