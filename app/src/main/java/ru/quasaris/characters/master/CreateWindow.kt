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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
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

fun evaluateFormula(formula: String, stats: Map<String, String>): Int {
    var processed = formula.uppercase()
    val replacements = mapOf(
        "[СИЛ]" to calculateModifier(stats["strength"] ?: "10"),
        "[STR]" to calculateModifier(stats["strength"] ?: "10"),
        "[ЛОВ]" to calculateModifier(stats["dexterity"] ?: "10"),
        "[DEX]" to calculateModifier(stats["dexterity"] ?: "10"),
        "[ТЕЛ]" to calculateModifier(stats["constitution"] ?: "10"),
        "[CON]" to calculateModifier(stats["constitution"] ?: "10"),
        "[ИНТ]" to calculateModifier(stats["intelligence"] ?: "10"),
        "[INT]" to calculateModifier(stats["intelligence"] ?: "10"),
        "[МУД]" to calculateModifier(stats["wisdom"] ?: "10"),
        "[WIS]" to calculateModifier(stats["wisdom"] ?: "10"),
        "[ХАР]" to calculateModifier(stats["charisma"] ?: "10"),
        "[CHA]" to calculateModifier(stats["charisma"] ?: "10")
    )

    replacements.forEach { (key, value) ->
        processed = processed.replace(key, value.toString())
    }

    // Handle [MAX(...)] and [MIN(...)]
    fun processFunctions(input: String): String {
        var current = input
        val functions = listOf("МАКС", "MAX", "МИН", "MIN")
        
        functions.forEach { func ->
            // Регулярное выражение теперь ищет функцию с квадратными скобками (приоритетно) или без них
            val patternWrapped = Regex("\\[$func\\s*\\(([^()]+)\\)\\]")
            val patternUnwrapped = Regex("$func\\s*\\(([^()]+)\\)")
            
            while (current.contains(func)) {
                val match = patternWrapped.find(current) ?: patternUnwrapped.find(current) ?: break
                val content = match.groupValues[1]
                val values = content.split(";").map { evaluateFormula(it.trim(), stats) }
                val result = if (func.startsWith("МА") || func.startsWith("MA")) values.maxOrNull() ?: 0 else values.minOrNull() ?: 0
                current = current.replace(match.value, result.toString())
            }
        }
        return current
    }

    processed = processFunctions(processed)
    
    // Удаляем любые оставшиеся квадратные скобки, чтобы они не ломали математический парсер
    processed = processed.replace("[", "").replace("]", "")

    return try {
        val tokens = processed.replace(" ", "").split(Regex("(?=[+\\-*/])|(?<=[+\\-*/])"))
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
            } else {
                while (!ops.empty() && hasPrecedence(token, ops.peek())) {
                    values.push(applyOp(ops.pop(), values.pop(), values.pop()))
                }
                ops.push(token)
            }
        }

        while (!ops.empty()) {
            values.push(applyOp(ops.pop(), values.pop(), values.pop()))
        }

        values.pop()
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

    val statsMap = mapOf(
        "strength" to strength, "dexterity" to dexterity, "constitution" to constitution,
        "intelligence" to intelligence, "wisdom" to wisdom, "charisma" to charisma
    )

    var armorClassEntries by remember { mutableStateOf(listOf(ArmorClassEntry(name = "Базовый КД", formula = "10 + [ЛОВ]"))) }
    var activeArmorClassId by remember { mutableStateOf<String?>(armorClassEntries.firstOrNull()?.id) }
    var isArmorClassPanelVisible by remember { mutableStateOf(false) }
    var deleteConfirmId by remember { mutableStateOf<String?>(null) }

    val activeACValue = remember(activeArmorClassId, armorClassEntries, statsMap) {
        val active = armorClassEntries.find { it.id == activeArmorClassId }
        if (active != null) evaluateFormula(active.formula, statsMap).toString() else "10"
    }

    val focusManager = LocalFocusManager.current

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Column(
                modifier = Modifier
                    .background(Color.White)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(Icons.Default.Menu, contentDescription = null, modifier = Modifier.size(32.dp))
                    Text(
                        text = name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEADDFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF4F378A))
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .height(24.dp)
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(20.dp))
                        .background(Color.White, RoundedCornerShape(20.dp))
                        .padding(2.dp)
                ) {
                    Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 0.dp, bottomEnd = 0.dp))
                                .background(Color(0xFFEADDFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$level уровень", fontSize = 11.sp, color = Color.Black)
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 20.dp, bottomEnd = 20.dp))
                                .background(Color.White)
                        ) {
                            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.35f).background(Color(0xFFEADDFF)))
                            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Spacer(Modifier.weight(0.4f))
                                Text("$experience | $nextLevelExp", fontSize = 11.sp, color = Color.Black)
                                Spacer(Modifier.weight(0.6f))
                                Text("${level.toInt() + 1}", fontSize = 11.sp, color = Color.Black)
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
                        StatIconBox(activeACValue, R.drawable.ic_shield, onClick = { isArmorClassPanelVisible = !isArmorClassPanelVisible })
                        StatIconBox("+2", R.drawable.ic_sword)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .height(55.dp)
                            .border(1.5.dp, Color(0xFF00C46F), RoundedCornerShape(8.dp))
                            .background(Color.White, RoundedCornerShape(8.dp)),
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
                        StatIconBox("30", R.drawable.ic_speed)
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                    deleteConfirmId = null
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color.White)
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.15f), Color.Transparent)
                            )
                        )
                )

                AnimatedVisibility(
                    visible = isArmorClassPanelVisible,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    ArmorClassPanel(
                        entries = armorClassEntries,
                        activeId = activeArmorClassId,
                        deleteConfirmId = deleteConfirmId,
                        onEntriesChanged = { armorClassEntries = it },
                        onActiveIdChanged = { activeArmorClassId = it },
                        onDeleteConfirmIdChanged = { deleteConfirmId = it }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEADDFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Характеристики", fontSize = 12.sp, color = Color(0xFF4A4459), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text("Характеристики", fontSize = 18.sp, fontWeight = FontWeight.Normal, color = Color.Black, textAlign = TextAlign.Center, modifier = Modifier.weight(1.5f))
                        Text("Характеристики", fontSize = 12.sp, color = Color(0xFF4A4459), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                    }
                }

                Button(
                    onClick = { /* TODO */ },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 12.dp, bottom = 20.dp)
                        .width(220.dp)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEADDFF)),
                    shape = RoundedCornerShape(8.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text("Расширенный режим", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("Сила", strength, Modifier.weight(1f)) { strength = it }
                        StatCard("Интеллект", intelligence, Modifier.weight(1f)) { intelligence = it }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("Ловкость", dexterity, Modifier.weight(1f)) { dexterity = it }
                        StatCard("Мудрость", wisdom, Modifier.weight(1f)) { wisdom = it }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("Телосложение", constitution, Modifier.weight(1f)) { constitution = it }
                        StatCard("Харизма", charisma, Modifier.weight(1f)) { charisma = it }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("Пассивные проверки", modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), textAlign = TextAlign.Center, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(103, 80, 164, 20))
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
fun ArmorClassPanel(
    entries: List<ArmorClassEntry>,
    activeId: String?,
    deleteConfirmId: String?,
    onEntriesChanged: (List<ArmorClassEntry>) -> Unit,
    onActiveIdChanged: (String?) -> Unit,
    onDeleteConfirmIdChanged: (String?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .background(Color(0xFFFEF7FF), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFCAC4D0).copy(0.3f), RoundedCornerShape(12.dp))
            .animateContentSize()
    ) {
        entries.forEachIndexed { index, entry ->
            val isActive = entry.id == activeId
            ArmorClassEntryItem(
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
            Divider(color = Color.Black.copy(0.15f), thickness = 1.dp)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable {
                    onEntriesChanged(entries + ArmorClassEntry())
                    onDeleteConfirmIdChanged(null)
                }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Добавить Новое", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.Black)
        }
    }
}

@Composable
fun ArmorClassEntryItem(
    entry: ArmorClassEntry,
    isActive: Boolean,
    isDeleteConfirm: Boolean,
    onUpdate: (ArmorClassEntry) -> Unit,
    onDelete: () -> Unit,
    onDeleteConfirmRequest: () -> Unit,
    onToggleActive: () -> Unit
) {
    val backgroundColor = if (isActive) Color(0xFFD0BCFF) else Color.Transparent
    val separatorColor = Color.Black.copy(0.2f)
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
                    tint = if (isDeleteConfirm) Color.Red else Color.Black.copy(0.7f)
                )
            }
            
            Box(modifier = Modifier.width(separatorThickness).fillMaxHeight().background(separatorColor))

            Box(modifier = Modifier.weight(1f).padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                if (entry.name.isEmpty()) {
                    Text("Название", color = Color.Gray.copy(0.6f), fontSize = 16.sp)
                }
                BasicTextField(
                    value = entry.name,
                    onValueChange = { onUpdate(entry.copy(name = it)) },
                    textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 16.sp, color = Color.Black),
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
                    tint = Color.Black
                )
            }
        }
        Divider(color = separatorColor, thickness = separatorThickness)
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.CenterStart) {
            if (entry.formula.isEmpty()) {
                Text("Формула КД", color = Color.Gray.copy(0.6f), fontSize = 14.sp)
            }
            BasicTextField(
                value = entry.formula,
                onValueChange = { onUpdate(entry.copy(formula = it)) },
                textStyle = TextStyle(fontSize = 14.sp, color = Color.Black),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun StatIconBox(value: String, iconRes: Int, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier.size(42.dp).clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        ),
        contentAlignment = Alignment.Center
    ) {
        if (iconRes == R.drawable.ic_sword) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(painter = painterResource(id = R.drawable.ic_sword), contentDescription = null, modifier = Modifier.fillMaxSize(), colorFilter = ColorFilter.tint(Color(0xFFD0BCFF)))
                Image(painter = painterResource(id = R.drawable.ic_sword), contentDescription = null, modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = -1f), colorFilter = ColorFilter.tint(Color(0xFFD0BCFF)))
            }
        } else {
            Image(painter = painterResource(id = iconRes), contentDescription = null, modifier = Modifier.fillMaxSize(), colorFilter = ColorFilter.tint(Color(0xFFD0BCFF)))
        }
        Text(
            text = value,
            fontSize = 15.sp,
            color = Color.Black,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            style = TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.White,
                    offset = Offset(0f, 0f),
                    blurRadius = 8f
                )
            )
        )
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier, onValueChange: (String) -> Unit) {
    val score = value.toIntOrNull() ?: 10
    val mod = floor((score - 10) / 2.0).toInt()
    val modStr = if (mod >= 0) "+$mod" else mod.toString()
    Box(
        modifier = modifier
            .height(104.dp)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(8.dp))
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Black)
        Box(modifier = Modifier.align(Alignment.BottomStart).padding(start = 22.dp, bottom = 2.dp).size(38.dp).rotate(-45f).clip(RoundedCornerShape(10.dp)).background(Color(0xFFD0BCFF)), contentAlignment = Alignment.Center) {
            Text(modStr, modifier = Modifier.rotate(45f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
        }
        Column(modifier = Modifier.align(Alignment.TopEnd), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF3F1F8)).border(1.dp, Color.Black.copy(0.05f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                BasicTextField(
                    value = value,
                    onValueChange = { newValue ->
                        val filtered = newValue.filter { it.isDigit() }
                        if (filtered.isEmpty()) { onValueChange("") }
                        else { val num = filtered.toIntOrNull(); if (num != null && num in 1..30) onValueChange(filtered) }
                    },
                    textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(32.dp)
                )
            }
            Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)).background(Color.White).border(1.dp, Color.Black.copy(0.05f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Text(modStr, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
            }
        }
    }
}

@Composable
fun PassiveCheckRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().height(30.dp).clip(RoundedCornerShape(8.dp)).background(Color(103, 80, 164, 40)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.padding(start = 12.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(value, modifier = Modifier.padding(end = 12.dp), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
fun CreateWindowPreview() {
    quasarisTheme { CreateWindow(onNavigateBack = {}, onCharacterCreate = { _ -> }) }
}
