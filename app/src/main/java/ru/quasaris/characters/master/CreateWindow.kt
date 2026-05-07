package ru.quasaris.characters.master

import android.net.Uri
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.quasaris.characters.master.ui.theme.quasarisTheme
import java.util.UUID
import java.util.Stack
import kotlin.math.floor

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
        0 -> "300"
        1 -> "300"
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
        processed = processed.replace("[$key ЗНАЧ]", score)
        processed = processed.replace("[$key SCR]", score)
        processed = processed.replace("[$key]", mod)
        processed = processed.replace("[$key ", "$mod ")
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

    processed = processFunctions(processed)
    processed = processed.replace(Regex("[^\\d+\\-*/]"), " ")

    return try {
        val tokens = processed.replace(" ", "").split(Regex("(?=[+*/])|(?<=[+*/])|(?<=\\d)(?=-)"))
        val values = Stack<Int>()
        val ops = Stack<String>()

        fun hasPrecedence(op1: String, op2: String): Boolean {
            if ((op1 == "*" || op1 == "/") && (op2 == "+" || op2 == "-")) return false
            return true
        }

        fun applyOp(op: String, b: Int, a: Int): Int {
            return when (op) {
                "+" -> a + b
                "-" -> a - b
                "*" -> a * b
                "/" -> if (b != 0) a / b else 0
                else -> 0
            }
        }

        for (token in tokens) {
            if (token.isEmpty()) continue
            if (token[0].isDigit() || (token.length > 1 && token[0] == '-' && token[1].isDigit())) {
                values.push(token.toInt())
            } else if ("+-*/".contains(token)) {
                while (!ops.empty() && hasPrecedence(token, ops.peek())) {
                    values.push(applyOp(ops.pop(), values.pop(), values.pop()))
                }
                ops.push(token)
            }
        }

        while (!ops.empty()) {
            values.push(applyOp(ops.pop(), values.pop(), values.pop()))
        }

        if (values.isEmpty()) 0 else values.pop()
    } catch (e: Exception) {
        0
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWindow(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    onCharacterCreate: (Character) -> Unit
) {
    var name by remember { mutableStateOf("Мирослав") }
    var level by remember { mutableStateOf("1") }
    var experience by remember { mutableStateOf("50") }
    var nextLevelExp by remember { mutableStateOf("300") }

    var strength by remember { mutableStateOf("10") }
    var dexterity by remember { mutableStateOf("10") }
    var constitution by remember { mutableStateOf("10") }
    var intelligence by remember { mutableStateOf("10") }
    var wisdom by remember { mutableStateOf("10") }
    var charisma by remember { mutableStateOf("10") }

    var strProf by remember { mutableStateOf(false) }
    var dexProf by remember { mutableStateOf(false) }
    var conProf by remember { mutableStateOf(false) }
    var intProf by remember { mutableStateOf(false) }
    var wisProf by remember { mutableStateOf(false) }
    var chaProf by remember { mutableStateOf(false) }

    val statsMap = mapOf(
        "strength" to strength, "dexterity" to dexterity, "constitution" to constitution,
        "intelligence" to intelligence, "wisdom" to wisdom, "charisma" to charisma
    )

    // Level & Exp State
    var isLevelPanelVisible by remember { mutableStateOf(false) }
    val proficiencyBonusValue = remember(level) { getProficiencyBonus(level).toString() }

    LaunchedEffect(level) {
        nextLevelExp = getNextLevelThreshold(level)
    }

    // AC State
    var armorClassEntries by remember { mutableStateOf(listOf(ArmorClassEntry(name = "Базовый КД", formula = "10 + [ЛОВ]"))) }
    var activeArmorClassId by remember { mutableStateOf<String?>(armorClassEntries.firstOrNull()?.id) }
    var isArmorClassPanelVisible by remember { mutableStateOf(false) }
    var acDeleteConfirmId by remember { mutableStateOf<String?>(null) }

    val activeACValue = remember(activeArmorClassId, armorClassEntries, statsMap) {
        val active = armorClassEntries.find { it.id == activeArmorClassId }
        if (active != null) evaluateFormula(active.formula, statsMap).toString() else "10"
    }

    // Initiative State
    var initiativeEntries by remember { mutableStateOf(listOf(InitiativeEntry(name = "Базовая Инициатива", formula = "[ЛОВ]"))) }
    var activeInitiativeId by remember { mutableStateOf<String?>(initiativeEntries.firstOrNull()?.id) }
    var isInitiativePanelVisible by remember { mutableStateOf(false) }
    var initDeleteConfirmId by remember { mutableStateOf<String?>(null) }

    val activeInitValue = remember(activeInitiativeId, initiativeEntries, statsMap) {
        val active = initiativeEntries.find { it.id == activeInitiativeId }
        val value = if (active != null) evaluateFormula(active.formula, statsMap) else 0
        if (value >= 0) "+$value" else value.toString()
    }

    // Speed State
    var speedEntries by remember { mutableStateOf(listOf(SpeedEntry(name = "Базовая Скорость", formula = "30"))) }
    var activeSpeedId by remember { mutableStateOf<String?>(speedEntries.firstOrNull()?.id) }
    var isSpeedPanelVisible by remember { mutableStateOf(false) }
    var speedDeleteConfirmId by remember { mutableStateOf<String?>(null) }

    val activeSpeedValue = remember(activeSpeedId, speedEntries, statsMap) {
        val active = speedEntries.find { it.id == activeSpeedId }
        if (active != null) evaluateFormula(active.formula, statsMap).toString() else "30"
    }

    val focusManager = LocalFocusManager.current
    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .background(colorScheme.surface)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(Icons.Default.Menu, contentDescription = null, modifier = Modifier.size(32.dp), tint = colorScheme.onSurface)
                    Text(
                        text = name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = colorScheme.onPrimaryContainer)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .height(24.dp)
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(20.dp))
                        .background(colorScheme.surface, RoundedCornerShape(20.dp))
                        .padding(2.dp)
                        .clickable {
                            isLevelPanelVisible = !isLevelPanelVisible
                            isArmorClassPanelVisible = false
                            isInitiativePanelVisible = false
                            isSpeedPanelVisible = false
                        }
                ) {
                    Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 0.dp, bottomEnd = 0.dp))
                                .background(colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$level уровень", fontSize = 11.sp, color = colorScheme.onPrimaryContainer)
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 20.dp, bottomEnd = 20.dp))
                                .background(colorScheme.surface)
                        ) {
                            val progress = remember(experience, nextLevelExp) {
                                val current = experience.toFloatOrNull() ?: 0f
                                val next = nextLevelExp.toFloatOrNull() ?: 0f
                                if (next <= 0f) 1f else (current / next).coerceIn(0f, 1f)
                            }
                            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progress).background(colorScheme.primaryContainer))
                            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Spacer(Modifier.weight(0.4f))
                                Text("$experience | $nextLevelExp", fontSize = 11.sp, color = colorScheme.onSurface)
                                Spacer(Modifier.weight(0.6f))
                                val nextLvlNum = (level.toIntOrNull() ?: 0) + 1
                                Text(if (nextLvlNum <= 20) "$nextLvlNum" else "", fontSize = 11.sp, color = colorScheme.onSurface)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatIconBox(activeACValue, R.drawable.ic_shield, onClick = {
                            isArmorClassPanelVisible = !isArmorClassPanelVisible
                            isInitiativePanelVisible = false
                            isSpeedPanelVisible = false
                            isLevelPanelVisible = false
                        })
                        StatIconBox(activeInitValue, R.drawable.ic_sword, onClick = {
                            isInitiativePanelVisible = !isInitiativePanelVisible
                            isArmorClassPanelVisible = false
                            isSpeedPanelVisible = false
                            isLevelPanelVisible = false
                        })
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .height(55.dp)
                            .border(1.5.dp, Color(0xFF00C46F), RoundedCornerShape(8.dp))
                            .background(colorScheme.surface, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_health),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                colorFilter = ColorFilter.tint(Color(0xFF00C46F))
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("10 / 10", color = Color(0xFF00C46F), fontSize = 16.sp, fontWeight = FontWeight.Normal)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatIconBox("1", R.drawable.ic_conditions)
                        StatIconBox(activeSpeedValue, R.drawable.ic_speed, onClick = {
                            isSpeedPanelVisible = !isSpeedPanelVisible
                            isArmorClassPanelVisible = false
                            isInitiativePanelVisible = false
                            isLevelPanelVisible = false
                        })
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(colorScheme.background)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                    acDeleteConfirmId = null
                    initDeleteConfirmId = null
                    speedDeleteConfirmId = null
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(colorScheme.surface)
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(colorScheme.onSurface.copy(alpha = 0.15f), Color.Transparent)
                            )
                        )
                )

                AnimatedVisibility(
                    visible = isLevelPanelVisible,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    LevelPanel(
                        level = level,
                        onLevelChange = { level = it },
                        experience = experience,
                        onExperienceChange = { experience = it },
                        nextLevelExp = nextLevelExp,
                        proficiencyBonus = proficiencyBonusValue
                    )
                }

                AnimatedVisibility(
                    visible = isArmorClassPanelVisible,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    FormulaPanel(
                        title = "Класс Доспеха",
                        entries = armorClassEntries,
                        activeId = activeArmorClassId,
                        deleteConfirmId = acDeleteConfirmId,
                        onEntriesChanged = { armorClassEntries = it.filterIsInstance<ArmorClassEntry>() },
                        onActiveIdChanged = { activeArmorClassId = it },
                        onDeleteConfirmIdChanged = { acDeleteConfirmId = it },
                        onAdd = { armorClassEntries = armorClassEntries + ArmorClassEntry() }
                    )
                }

                AnimatedVisibility(
                    visible = isInitiativePanelVisible,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    FormulaPanel(
                        title = "Инициатива",
                        entries = initiativeEntries,
                        activeId = activeInitiativeId,
                        deleteConfirmId = initDeleteConfirmId,
                        onEntriesChanged = { initiativeEntries = it.filterIsInstance<InitiativeEntry>() },
                        onActiveIdChanged = { activeInitiativeId = it },
                        onDeleteConfirmIdChanged = { initDeleteConfirmId = it },
                        onAdd = { initiativeEntries = initiativeEntries + InitiativeEntry() }
                    )
                }

                AnimatedVisibility(
                    visible = isSpeedPanelVisible,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    FormulaPanel(
                        title = "Скорость",
                        entries = speedEntries,
                        activeId = activeSpeedId,
                        deleteConfirmId = speedDeleteConfirmId,
                        onEntriesChanged = { speedEntries = it.filterIsInstance<SpeedEntry>() },
                        onActiveIdChanged = { activeSpeedId = it },
                        onDeleteConfirmIdChanged = { speedDeleteConfirmId = it },
                        onAdd = { speedEntries = speedEntries + SpeedEntry() }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Характеристики", fontSize = 12.sp, color = colorScheme.onPrimaryContainer.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text("Характеристики", fontSize = 18.sp, fontWeight = FontWeight.Normal, color = colorScheme.onPrimaryContainer, textAlign = TextAlign.Center, modifier = Modifier.weight(1.5f))
                        Text("Характеристики", fontSize = 12.sp, color = colorScheme.onPrimaryContainer.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                    }
                }

                Button(
                    onClick = { /* TODO */ },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 12.dp, bottom = 20.dp)
                        .width(220.dp)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.secondaryContainer,
                        contentColor = colorScheme.onSecondaryContainer
                    ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text("Расширенный режим", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("Сила", strength, level, strProf, Modifier.weight(1f), { strength = it }, { strProf = it })
                        StatCard("Интеллект", intelligence, level, intProf, Modifier.weight(1f), { intelligence = it }, { intProf = it })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("Ловкость", dexterity, level, dexProf, Modifier.weight(1f), { dexterity = it }, { dexProf = it })
                        StatCard("Мудрость", wisdom, level, wisProf, Modifier.weight(1f), { wisdom = it }, { wisProf = it })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("Телосложение", constitution, level, conProf, Modifier.weight(1f), { constitution = it }, { conProf = it })
                        StatCard("Харизма", charisma, level, chaProf, Modifier.weight(1f), { charisma = it }, { chaProf = it })
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "Пассивные проверки",
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface)

                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorScheme.primary.copy(alpha = 0.1f))
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PassiveCheckRow("Анализ (Интеллект)", (10 + calculateModifier(intelligence)).toString())
                    PassiveCheckRow("Внимательность (Мудрость)", (10 + calculateModifier(wisdom)).toString())
                    PassiveCheckRow("Проницательность (Мудрость)", (10 + calculateModifier(wisdom)).toString())
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun LevelPanel(
    level: String,
    onLevelChange: (String) -> Unit,
    experience: String,
    onExperienceChange: (String) -> Unit,
    nextLevelExp: String,
    proficiencyBonus: String
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .background(colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .border(1.dp, colorScheme.outline.copy(0.3f), RoundedCornerShape(12.dp))
            .animateContentSize()
    ) {
        Text(
            text = "Уровень и Опыт",
            modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.titleMedium,
            color = colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Уровень персонажа",
                modifier = Modifier.padding(start = 16.dp).weight(1f),
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariant
            )
            BasicTextField(
                value = level,
                onValueChange = { newValue ->
                    val filtered = newValue.filter { it.isDigit() }
                    if (filtered.isEmpty()) { onLevelChange("0") }
                    else {
                        val num = filtered.toIntOrNull()
                        if (num != null && num in 0..20) onLevelChange(num.toString())
                    }
                },
                textStyle = TextStyle(textAlign = TextAlign.End, fontSize = 16.sp, color = colorScheme.onSurface, fontWeight = FontWeight.Bold),
                modifier = Modifier.width(100.dp).padding(end = 16.dp),
                cursorBrush = SolidColor(colorScheme.primary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        HorizontalDivider(color = colorScheme.outline.copy(0.15f), thickness = 1.dp)

        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Текущий опыт",
                modifier = Modifier.padding(start = 16.dp).weight(1f),
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariant
            )
            BasicTextField(
                value = experience,
                onValueChange = { onExperienceChange(it.filter { it.isDigit() }) },
                textStyle = TextStyle(textAlign = TextAlign.End, fontSize = 16.sp, color = colorScheme.onSurface, fontWeight = FontWeight.Bold),
                modifier = Modifier.width(100.dp).padding(end = 4.dp),
                cursorBrush = SolidColor(colorScheme.primary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Text(
                "/ $nextLevelExp",
                modifier = Modifier.padding(end = 16.dp),
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        HorizontalDivider(color = colorScheme.outline.copy(0.15f), thickness = 1.dp)

        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Бонус мастерства",
                modifier = Modifier.padding(start = 16.dp).weight(1f),
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariant
            )
            Text(
                text = "+$proficiencyBonus",
                modifier = Modifier.padding(end = 16.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
        }
    }
}

@Composable
fun FormulaPanel(
    title: String,
    entries: List<FormulaEntry>,
    activeId: String?,
    deleteConfirmId: String?,
    onEntriesChanged: (List<FormulaEntry>) -> Unit,
    onActiveIdChanged: (String?) -> Unit,
    onDeleteConfirmIdChanged: (String?) -> Unit,
    onAdd: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .background(colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .border(1.dp, colorScheme.outline.copy(0.3f), RoundedCornerShape(12.dp))
            .animateContentSize()
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.titleMedium,
            color = colorScheme.onSurfaceVariant
        )

        entries.forEachIndexed { index, entry ->
            val isActive = entry.id == activeId
            FormulaEntryItem(
                entry = entry,
                isActive = isActive,
                isDeleteConfirm = entry.id == deleteConfirmId,
                onUpdate = { updated ->
                    val newList = entries.toMutableList()
                    newList[index] = updated
                    onEntriesChanged(newList)
                },
                onDelete = {
                    val newList = entries.toMutableList()
                    newList.removeAt(index)
                    if (entry.id == activeId) onActiveIdChanged(null)
                    onEntriesChanged(newList)
                    onDeleteConfirmIdChanged(null)
                },
                onDeleteConfirmRequest = {
                    onDeleteConfirmIdChanged(entry.id)
                },
                onToggleActive = {
                    onActiveIdChanged(if (isActive) null else entry.id)
                    onDeleteConfirmIdChanged(null)
                }
            )
            HorizontalDivider(color = colorScheme.outline.copy(0.15f), thickness = 1.dp)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable {
                    onAdd()
                    onDeleteConfirmIdChanged(null)
                }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = colorScheme.onSurface, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Добавить Новое", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colorScheme.onSurface)
        }
    }
}

@Composable
fun FormulaEntryItem(
    entry: FormulaEntry,
    isActive: Boolean,
    isDeleteConfirm: Boolean,
    onUpdate: (FormulaEntry) -> Unit,
    onDelete: () -> Unit,
    onDeleteConfirmRequest: () -> Unit,
    onToggleActive: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val backgroundColor = if (isActive) colorScheme.primaryContainer else Color.Transparent
    val separatorColor = colorScheme.outline.copy(0.2f)
    val separatorThickness = 1.2.dp

    Column(modifier = Modifier.fillMaxWidth().background(backgroundColor).animateContentSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .fillMaxHeight()
                    .clickable {
                        if (isDeleteConfirm) onDelete() else onDeleteConfirmRequest()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isDeleteConfirm) colorScheme.error else colorScheme.onSurface.copy(0.7f)
                )
            }

            Box(modifier = Modifier.width(separatorThickness).fillMaxHeight().background(separatorColor))

            Box(modifier = Modifier.weight(1f).padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                if (entry.name.isEmpty()) {
                    Text("Название", color = colorScheme.onSurface.copy(0.4f), fontSize = 16.sp)
                }
                BasicTextField(
                    value = entry.name,
                    onValueChange = {
                        val updated = when(entry) {
                            is ArmorClassEntry -> entry.copy(name = it)
                            is InitiativeEntry -> entry.copy(name = it)
                            is SpeedEntry -> entry.copy(name = it)
                            else -> entry
                        }
                        onUpdate(updated)
                    },
                    textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 16.sp, color = colorScheme.onSurface),
                    cursorBrush = SolidColor(colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )
            }

            Box(modifier = Modifier.width(separatorThickness).fillMaxHeight().background(separatorColor))

            Box(
                modifier = Modifier
                    .width(44.dp)
                    .fillMaxHeight()
                    .clickable { onToggleActive() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isActive) Icons.Default.Close else Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colorScheme.onSurface
                )
            }
        }
        HorizontalDivider(color = separatorColor, thickness = separatorThickness)
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.CenterStart) {
            if (entry.formula.isEmpty()) {
                Text("Формула", color = colorScheme.onSurface.copy(0.4f), fontSize = 14.sp)
            }
            BasicTextField(
                value = entry.formula,
                onValueChange = {
                    val updated = when(entry) {
                        is ArmorClassEntry -> entry.copy(formula = it)
                        is InitiativeEntry -> entry.copy(formula = it)
                        is SpeedEntry -> entry.copy(formula = it)
                        else -> entry
                    }
                    onUpdate(updated)
                },
                textStyle = TextStyle(fontSize = 14.sp, color = colorScheme.onSurface),
                cursorBrush = SolidColor(colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun StatIconBox(value: String, iconRes: Int, onClick: () -> Unit = {}) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.size(42.dp).clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        ),
        contentAlignment = Alignment.Center
    ) {
        val tint = colorScheme.primary.copy(alpha = 0.50f)
        if (iconRes == R.drawable.ic_sword) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(painter = painterResource(id = R.drawable.ic_sword), contentDescription = null, modifier = Modifier.fillMaxSize(), colorFilter = ColorFilter.tint(tint))
                Image(painter = painterResource(id = R.drawable.ic_sword), contentDescription = null, modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = -1f), colorFilter = ColorFilter.tint(tint))
            }
        } else {
            Image(painter = painterResource(id = iconRes), contentDescription = null, modifier = Modifier.fillMaxSize(), colorFilter = ColorFilter.tint(tint))
        }
        Text(
            text = value,
            fontSize = 15.sp,
            color = colorScheme.onSurface,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            style = TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = colorScheme.surface,
                    offset = Offset(0f, 0f),
                    blurRadius = 14f
                )
            )
        )
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    level: String,
    isProficient: Boolean,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
    onProficiencyToggle: (Boolean) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val baseMod = calculateModifier(value)
    val profBonus = if (isProficient) getProficiencyBonus(level) else 0
    val totalMod = baseMod + profBonus

    val modStr = if (baseMod >= 0) "+$baseMod" else baseMod.toString()
    val totalModStr = if (totalMod >= 0) "+$totalMod" else totalMod.toString()

    Box(
        modifier = modifier
            .height(104.dp)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(8.dp))
            .background(colorScheme.surface, RoundedCornerShape(8.dp))
            .border(1.dp, colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colorScheme.onSurface)

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 22.dp, bottom = 2.dp)
                .size(38.dp)
                .rotate(if (isProficient) -45f else 0f)
                .clip(if (isProficient) SquirclePath else RoundedCornerShape(8.dp))
                .background(if (isProficient) colorScheme.primaryContainer else colorScheme.surfaceVariant)
                .border(
                    width = 1.dp,
                    color = if (isProficient) colorScheme.primary else colorScheme.outline.copy(alpha = 0.2f),
                    shape = if (isProficient) SquirclePath else RoundedCornerShape(8.dp)
                )
                .clickable { onProficiencyToggle(!isProficient) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = totalModStr,
                modifier = Modifier.rotate(if (isProficient) 45f else 0f),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isProficient) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
            )
        }

        Column(modifier = Modifier.align(Alignment.TopEnd), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colorScheme.surfaceVariant)
                    .border(1.dp, colorScheme.outline.copy(0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = { newValue ->
                        val filtered = newValue.filter { it.isDigit() }
                        if (filtered.isEmpty()) { onValueChange("") }
                        else { val num = filtered.toIntOrNull(); if (num != null && num in 1..30) onValueChange(filtered) }
                    },
                    textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface),
                    cursorBrush = SolidColor(colorScheme.primary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(32.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colorScheme.surface)
                    .border(1.dp, colorScheme.outline.copy(0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(modStr, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun PassiveCheckRow(label: String, value: String) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colorScheme.primary.copy(alpha = 0.2f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, modifier = Modifier.padding(start = 12.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colorScheme.onSurface)
        Text(value, modifier = Modifier.padding(end = 12.dp), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
fun CreateWindowPreview() {
    quasarisTheme { CreateWindow(onNavigateBack = {}, onCharacterCreate = { _ -> }) }
}