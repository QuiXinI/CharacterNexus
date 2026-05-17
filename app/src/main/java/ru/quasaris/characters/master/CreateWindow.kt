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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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

    // 1. Stat Replacements
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

    // 2. Proficiency Bonus Replacements
    val pbFormula = stats["proficiencyBonus"] ?: "2"
    val level = stats["level"] ?: "1"
    val realPb = getProficiencyBonus(level).toString()
    
    // Replace [БМ] first. If pbFormula is [НАСТ БМ], it will become [НАСТ БМ]
    processed = processed.replace("[БМ]", pbFormula)
    processed = processed.replace("[PB]", pbFormula)
    processed = processed.replace("[НАСТ БМ]", realPb)
    processed = processed.replace("[REAL PB]", realPb)
    
    // If we still have [БМ] after replacement (e.g. infinite recursion prevention), fallback to level bonus
    if (processed.contains("[БМ]") || processed.contains("[PB]")) {
        processed = processed.replace("[БМ]", realPb).replace("[PB]", realPb)
    }

    // 3. Function Processing (MAX, MIN, CEIL/ВЕРХ, FLOOR/НИЗ)
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
    
    // 4. Final Cleanup for Math Evaluator
    processed = processed.replace(Regex("[^\\d+\\-*/]"), " ")

    // 5. Math Evaluation - Manual Tokenization for Negative Numbers and Subtraction
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
            } else if ("+*/".contains(c)) {
                tokens.add(c.toString())
                i++
            } else if (c == '-') {
                // Check if it's subtraction or negative sign
                if (i > 0 && clean[i-1].isDigit()) {
                    tokens.add("-")
                    i++
                } else {
                    // It's a negative sign, combine with digits
                    val start = i
                    i++ // skip '-'
                    while (i < clean.length && clean[i].isDigit()) i++
                    if (i > start + 1) {
                        tokens.add(clean.substring(start, i))
                    } else {
                        tokens.add("-")
                    }
                }
            } else {
                i++
            }
        }

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
                    if (values.size < 2) break
                    values.push(applyOp(ops.pop(), values.pop(), values.pop()))
                }
                ops.push(token)
            }
        }

        while (!ops.empty() && values.size >= 2) {
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
    var proficiencyBonus by remember { mutableStateOf("[НАСТ БМ]") }

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

    // Health State
    var maxHp by remember { mutableStateOf("10") }
    var tempHp by remember { mutableStateOf("0") }
    var currentHp by remember { mutableStateOf("10") }
    var isHealthPanelVisible by remember { mutableStateOf(false) }
    var hpDialogType by remember { mutableStateOf("") } // "heal", "damage", "temp"
    var hpDialogValue by remember { mutableStateOf("") }
    var showHpDialog by remember { mutableStateOf(false) }

    val healthState = remember(currentHp, maxHp) {
        val current = currentHp.toIntOrNull() ?: 0
        val max = maxHp.toIntOrNull() ?: 0
        when {
            current <= 0 -> "dead"
            max > 0 && current <= max / 2 -> "bloodied"
            else -> "healthy"
        }
    }

    val healthColor = when(healthState) {
        "dead" -> Color(0xFF454545)
        "bloodied" -> Color(0xFFE57373)
        else -> Color(0xFF00C46F)
    }

    val healthIcon = when(healthState) {
        "dead" -> R.drawable.ic_health_death
        "bloodied" -> R.drawable.ic_health_bloodied
        else -> R.drawable.ic_health
    }

    val clampHp = {
        val totalMax = maxHp.toIntOrNull() ?: 0
        val current = currentHp.toIntOrNull() ?: 0
        if (current > totalMax && maxHp.isNotEmpty()) {
            currentHp = totalMax.coerceAtLeast(0).toString()
        }
    }

    val statsMap = mapOf(
        "strength" to strength, "dexterity" to dexterity, "constitution" to constitution,
        "intelligence" to intelligence, "wisdom" to wisdom, "charisma" to charisma,
        "proficiencyBonus" to proficiencyBonus, "level" to level
    )

    // Level & Exp State
    var isLevelPanelVisible by remember { mutableStateOf(false) }

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
                            isHealthPanelVisible = false
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
                            isHealthPanelVisible = false
                        })
                        StatIconBox(activeInitValue, R.drawable.ic_sword, onClick = {
                            isInitiativePanelVisible = !isInitiativePanelVisible
                            isArmorClassPanelVisible = false
                            isSpeedPanelVisible = false
                            isLevelPanelVisible = false
                            isHealthPanelVisible = false
                        })
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .height(55.dp)
                            .border(1.5.dp, healthColor, RoundedCornerShape(8.dp))
                            .background(colorScheme.surface, RoundedCornerShape(8.dp))
                            .clickable {
                                isHealthPanelVisible = !isHealthPanelVisible
                                isArmorClassPanelVisible = false
                                isInitiativePanelVisible = false
                                isSpeedPanelVisible = false
                                isLevelPanelVisible = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = healthIcon),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                colorFilter = ColorFilter.tint(healthColor)
                            )
                            Spacer(Modifier.width(6.dp))
                            val totalMax = maxHp.toIntOrNull() ?: 0
                            Text("$currentHp / $totalMax", color = healthColor, fontSize = 16.sp, fontWeight = FontWeight.Normal)
                            if ((tempHp.toIntOrNull() ?: 0) > 0) {
                                Text(" (+$tempHp)", color = healthColor.copy(alpha = 0.7f), fontSize = 14.sp)
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatIconBox("1", R.drawable.ic_conditions)
                        StatIconBox(activeSpeedValue, R.drawable.ic_speed, onClick = {
                            isSpeedPanelVisible = !isSpeedPanelVisible
                            isArmorClassPanelVisible = false
                            isInitiativePanelVisible = false
                            isLevelPanelVisible = false
                            isHealthPanelVisible = false
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
                        proficiencyBonus = proficiencyBonus,
                        onProficiencyBonusChange = { proficiencyBonus = it },
                        nextLevelExp = nextLevelExp,
                        statsMap = statsMap
                    )
                }

                AnimatedVisibility(
                    visible = isHealthPanelVisible,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    HealthPanel(
                        maxHp = maxHp, onMaxHpChange = { maxHp = it },
                        tempHp = tempHp, onTempHpChange = { tempHp = it },
                        currentHp = currentHp, onCurrentHpChange = { currentHp = it },
                        onHealClick = { hpDialogType = "heal"; hpDialogValue = ""; showHpDialog = true },
                        onDamageClick = { hpDialogType = "damage"; hpDialogValue = ""; showHpDialog = true },
                        onTempClick = { hpDialogType = "temp"; hpDialogValue = ""; showHpDialog = true },
                        healthColor = healthColor,
                        onFocusLost = clampHp
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

                val evaluatedPB = remember(proficiencyBonus, statsMap) { evaluateFormula(proficiencyBonus, statsMap).toString() }

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("Сила", strength, evaluatedPB, strProf, Modifier.weight(1f), { strength = it }, { strProf = it })
                        StatCard("Интеллект", intelligence, evaluatedPB, intProf, Modifier.weight(1f), { intelligence = it }, { intProf = it })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("Ловкость", dexterity, evaluatedPB, dexProf, Modifier.weight(1f), { dexterity = it }, { dexProf = it })
                        StatCard("Мудрость", wisdom, evaluatedPB, wisProf, Modifier.weight(1f), { wisdom = it }, { wisProf = it })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("Телосложение", constitution, evaluatedPB, conProf, Modifier.weight(1f), { constitution = it }, { conProf = it })
                        StatCard("Харизма", charisma, evaluatedPB, chaProf, Modifier.weight(1f), { charisma = it }, { chaProf = it })
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

        if (showHpDialog) {
            AlertDialog(
                onDismissRequest = { showHpDialog = false },
                title = { Text(when(hpDialogType) {
                    "heal" -> "Лечение"
                    "damage" -> "Получение урона"
                    else -> "Временные Хиты"
                }) },
                text = {
                    OutlinedTextField(
                        value = hpDialogValue,
                        onValueChange = { hpDialogValue = it.filter { c -> c.isDigit() } },
                        label = { Text("Значение") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val value = hpDialogValue.toIntOrNull() ?: 0
                        when(hpDialogType) {
                            "heal" -> {
                                val max = maxHp.toIntOrNull() ?: 0
                                val current = currentHp.toIntOrNull() ?: 0
                                currentHp = minOf(max, current + value).toString()
                            }
                            "damage" -> {
                                var damage = value
                                var temp = tempHp.toIntOrNull() ?: 0
                                var current = currentHp.toIntOrNull() ?: 0
                                
                                if (temp > 0) {
                                    val absorbed = minOf(temp, damage)
                                    temp -= absorbed
                                    damage -= absorbed
                                    tempHp = temp.toString()
                                }
                                
                                if (damage > 0) {
                                    current = maxOf(0, current - damage)
                                    currentHp = current.toString()
                                }
                            }
                            "temp" -> {
                                tempHp = minOf(9999, value).toString()
                            }
                        }
                        showHpDialog = false
                    }) { Text("ОК") }
                },
                dismissButton = {
                    TextButton(onClick = { showHpDialog = false }) { Text("Отмена") }
                }
            )
        }
    }
}

@Composable
fun HealthPanel(
    maxHp: String, onMaxHpChange: (String) -> Unit,
    tempHp: String, onTempHpChange: (String) -> Unit,
    currentHp: String, onCurrentHpChange: (String) -> Unit,
    onHealClick: () -> Unit,
    onDamageClick: () -> Unit,
    onTempClick: () -> Unit,
    healthColor: Color,
    onFocusLost: () -> Unit = {}
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
            text = "Хиты",
            modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.titleMedium,
            color = healthColor
        )

        HealthRow("Максимум Хитов", maxHp, { newValue ->
            val num = newValue.toIntOrNull() ?: 0
            onMaxHpChange(minOf(999, num).toString())
        }, onFocusLost)
        HorizontalDivider(color = colorScheme.outline.copy(0.15f), thickness = 1.dp)
        HealthRow("Текущие Хиты", currentHp, { newValue ->
            val num = newValue.toIntOrNull() ?: 0
            onCurrentHpChange(minOf(999, num).toString())
        }, onFocusLost)
        HorizontalDivider(color = colorScheme.outline.copy(0.15f), thickness = 1.dp)
        HealthRow("Временные Хиты", tempHp, { newValue ->
            val num = newValue.toIntOrNull() ?: 0
            onTempHpChange(minOf(9999, num).toString())
        })

        Spacer(modifier = Modifier.height(12.dp))

        HealthActionRow("Лечение", Color(0xFF00C46F), onHealClick)
        HorizontalDivider(color = colorScheme.outline.copy(0.15f), thickness = 1.dp)
        HealthActionRow("Получение урона", Color(0xFFE57373), onDamageClick)
        HorizontalDivider(color = colorScheme.outline.copy(0.15f), thickness = 1.dp)
        HealthActionRow("Укрепление", Color(0xFF64B5F6), onTempClick)

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun HealthRow(label: String, value: String, onValueChange: (String) -> Unit, onFocusLost: () -> Unit = {}) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.padding(start = 16.dp).weight(1f), fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(it.filter { c -> c.isDigit() || c == '-' }) },
            textStyle = TextStyle(textAlign = TextAlign.End, fontSize = 16.sp, color = colorScheme.onSurface, fontWeight = FontWeight.Bold),
            modifier = Modifier
                .width(100.dp)
                .padding(end = 16.dp)
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused) {
                        onFocusLost()
                    }
                },
            cursorBrush = SolidColor(colorScheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}

@Composable
fun HealthActionRow(text: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = color)
    }
}

@Composable
fun LevelPanel(
    level: String,
    onLevelChange: (String) -> Unit,
    experience: String,
    onExperienceChange: (String) -> Unit,
    proficiencyBonus: String,
    onProficiencyBonusChange: (String) -> Unit,
    nextLevelExp: String,
    statsMap: Map<String, String>
) {
    val colorScheme = MaterialTheme.colorScheme

    // Используем TextFieldValue для контроля курсора
    var levelTextFieldValue by remember {
        mutableStateOf(TextFieldValue(level))
    }
    var experienceTextFieldValue by remember {
        mutableStateOf(TextFieldValue(experience))
    }
    var proficiencyTextFieldValue by remember {
        mutableStateOf(TextFieldValue(proficiencyBonus))
    }

    var isProfFocused by remember { mutableStateOf(false) }

    LaunchedEffect(level) {
        if (levelTextFieldValue.text != level) {
            levelTextFieldValue = levelTextFieldValue.copy(text = level, selection = TextRange(level.length))
        }
    }
    LaunchedEffect(experience) {
        if (experienceTextFieldValue.text != experience) {
            experienceTextFieldValue = experienceTextFieldValue.copy(text = experience, selection = TextRange(experience.length))
        }
    }
    
    // Синхронизация поля Бонуса Мастерства с учетом фокуса и формулы
    LaunchedEffect(proficiencyBonus, isProfFocused, statsMap) {
        val displayStr = if (isProfFocused) proficiencyBonus else evaluateFormula(proficiencyBonus, statsMap).toString()
        if (proficiencyTextFieldValue.text != displayStr) {
            proficiencyTextFieldValue = TextFieldValue(
                text = displayStr,
                selection = if (isProfFocused) TextRange(displayStr.length) else TextRange.Zero
            )
        }
    }

    val focusRequesterLevel = remember { FocusRequester() }
    val focusRequesterExp = remember { FocusRequester() }
    val focusRequesterProf = remember { FocusRequester() }

    val currentLevelInt = level.toIntOrNull() ?: 1
    val targetLevel = calculateLevelFromExperience(experience)

    val canUpdate = targetLevel != currentLevelInt
    val buttonText = when {
        targetLevel > currentLevelInt -> "Повысить уровень"
        targetLevel < currentLevelInt -> "Понизить уровень"
        else -> "Повысить уровень"
    }

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
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable {
                    levelTextFieldValue = levelTextFieldValue.copy(selection = TextRange(level.length))
                    focusRequesterLevel.requestFocus()
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Уровень персонажа",
                modifier = Modifier.padding(start = 16.dp).weight(1f),
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariant
            )
            BasicTextField(
                value = levelTextFieldValue,
                onValueChange = {
                    levelTextFieldValue = it
                    val filtered = it.text.filter { c -> c.isDigit() }
                    if (filtered.isEmpty()) { onLevelChange("") }
                    else {
                        val num = filtered.toIntOrNull()
                        if (num != null && num in 0..20) onLevelChange(num.toString())
                    }
                },
                textStyle = TextStyle(textAlign = TextAlign.End, fontSize = 16.sp, color = colorScheme.onSurface, fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .width(100.dp)
                    .padding(end = 16.dp)
                    .focusRequester(focusRequesterLevel)
                    .onFocusChanged { if (!it.isFocused && level.isEmpty()) onLevelChange("0") },
                cursorBrush = SolidColor(colorScheme.primary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        HorizontalDivider(color = colorScheme.outline.copy(0.15f), thickness = 1.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable {
                    experienceTextFieldValue = experienceTextFieldValue.copy(selection = TextRange(experience.length))
                    focusRequesterExp.requestFocus()
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Текущий опыт",
                modifier = Modifier.padding(start = 16.dp).weight(1f),
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariant
            )
            BasicTextField(
                value = experienceTextFieldValue,
                onValueChange = {
                    experienceTextFieldValue = it
                    onExperienceChange(it.text.filter { c -> c.isDigit() })
                },
                textStyle = TextStyle(textAlign = TextAlign.End, fontSize = 16.sp, color = colorScheme.onSurface, fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .width(100.dp)
                    .padding(end = 4.dp)
                    .focusRequester(focusRequesterExp)
                    .onFocusChanged { if (!it.isFocused && experience.isEmpty()) onExperienceChange("0") },
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
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable {
                    focusRequesterProf.requestFocus()
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Бонус Мастерства",
                modifier = Modifier.padding(start = 16.dp).weight(1f),
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 16.dp)
            ) {
                val evaluated = remember(proficiencyBonus, statsMap) { evaluateFormula(proficiencyBonus, statsMap) }
                if (!isProfFocused && evaluated >= 0) {
                    Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                }
                BasicTextField(
                    value = proficiencyTextFieldValue,
                    onValueChange = {
                        if (isProfFocused) {
                            proficiencyTextFieldValue = it
                            onProficiencyBonusChange(it.text)
                        }
                    },
                    textStyle = TextStyle(
                        textAlign = if (isProfFocused) TextAlign.Start else TextAlign.End,
                        fontSize = 16.sp,
                        color = colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .width(IntrinsicSize.Min)
                        .defaultMinSize(minWidth = 10.dp)
                        .focusRequester(focusRequesterProf)
                        .onFocusChanged { 
                            if (isProfFocused != it.isFocused) {
                                isProfFocused = it.isFocused
                                if (!it.isFocused && proficiencyBonus.isEmpty()) {
                                    onProficiencyBonusChange("[НАСТ БМ]")
                                }
                            }
                        },
                    cursorBrush = SolidColor(colorScheme.primary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { if (canUpdate) onLevelChange(targetLevel.toString()) },
            enabled = canUpdate,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(40.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (canUpdate) colorScheme.primary else colorScheme.outline.copy(alpha = 0.12f),
                contentColor = if (canUpdate) colorScheme.onPrimary else colorScheme.onSurface.copy(alpha = 0.38f)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = if (canUpdate) 2.dp else 0.dp)
        ) {
            Text(buttonText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
    proficiencyBonus: String,
    isProficient: Boolean,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
    onProficiencyToggle: (Boolean) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val baseMod = calculateModifier(value)
    val profBonus = if (isProficient) (proficiencyBonus.toIntOrNull() ?: 0) else 0
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
