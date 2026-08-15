package ru.quasaris.characternexus.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import ru.quasaris.characternexus.backend.*
import ru.quasaris.characternexus.model.*
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
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
    hpPanelHitDice: List<HitDiceEntry>,
    onSpentHitDiceChange: (Int, Int) -> Unit,
    onOpenHealthSettings: () -> Unit,

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
    exhaustion: Int,
    onExhaustionChange: (Int) -> Unit,

    isShieldActive: Boolean,
    onShieldActiveChange: (Boolean) -> Unit,
    shieldEntries: List<ShieldEntry>,
    activeShieldId: String?,
    shieldDeleteConfirmId: String?,
    onShieldEntries: (List<ShieldEntry>) -> Unit,
    onActiveShield: (String?) -> Unit,
    onShieldDeleteReq: (String?) -> Unit,
    onAddShield: () -> Unit,

    isSpeedPanelVisible: Boolean,
    speedEntries: List<SpeedEntry>,
    activeSpeedId: String?,
    speedDeleteConfirmId: String?,
    onSpeedEntries: (List<SpeedEntry>) -> Unit,
    onActiveSpeed: (String?) -> Unit,
    onSpeedDeleteReq: (String?) -> Unit,
    onAddSpeed: () -> Unit,
    
    // Icons
    diceIcons: Map<Int, DrawableResource> = emptyMap()
) {
    val panelsSpringSpec = remember {
        spring<IntSize>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
    }
    val floatSpring = remember {
        spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
    }

    val enterSpec = expandVertically(animationSpec = panelsSpringSpec) + fadeIn(animationSpec = floatSpring)
    val exitSpec = shrinkVertically(animationSpec = panelsSpringSpec) + fadeOut(animationSpec = floatSpring)

    Column(modifier = Modifier.fillMaxWidth()) {
        AnimatedVisibility(isHealthPanelVisible, enter = enterSpec, exit = exitSpec) {
            HealthPanel(
                maxHp, onMaxHpChange, tempHp, onTempHpChange, currentHp, onCurrentHpChange, 
                onHealClick, onDamageClick, onTempClick, healthColor, clampHp, hpPanelHitDice, 
                onSpentHitDiceChange, onOpenHealthSettings, diceIcons
            )
        }

        AnimatedVisibility(isLevelPanelVisible, enter = enterSpec, exit = exitSpec) { 
            LevelPanel(level, onLevelChange, experience, onExpChange, proficiencyBonus, onProfChange, nextLevelExp, statsMap) 
        }

        AnimatedVisibility(isArmorClassPanelVisible, enter = enterSpec, exit = exitSpec) {
            Column {
                FormulaPanel("Класс Доспеха", armorClassEntries, activeArmorClassId, acDeleteConfirmId, { updated -> onArmorClassEntries(updated.filterIsInstance<ArmorClassEntry>()) }, onActiveArmorClass, onAcDeleteReq, onAddArmorClass) 
                FormulaPanel(
                    title = "Щит",
                    entries = shieldEntries,
                    activeId = activeShieldId,
                    deleteId = shieldDeleteConfirmId,
                    onEntries = { updated -> onShieldEntries(updated.filterIsInstance<ShieldEntry>()) },
                    onActive = onActiveShield,
                    onDeleteReq = onShieldDeleteReq,
                    onAdd = onAddShield,
                    headerTrailing = {
                        Switch(
                            checked = isShieldActive,
                            onCheckedChange = onShieldActiveChange,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                )
            }
        }
        AnimatedVisibility(isInitiativePanelVisible, enter = enterSpec, exit = exitSpec) { FormulaPanel("Инициатива", initiativeEntries, activeInitiativeId, initDeleteConfirmId, { updated -> onInitiativeEntries(updated.filterIsInstance<InitiativeEntry>()) }, onActiveInitiative, onInitDeleteReq, onAddInitiative) }
        AnimatedVisibility(isConditionsPanelVisible, enter = enterSpec, exit = exitSpec) { ConditionsPanel(allConditions, selectedConditions, onToggleCondition, exhaustion, onExhaustionChange) }
        AnimatedVisibility(isSpeedPanelVisible, enter = enterSpec, exit = exitSpec) { FormulaPanel("Скорость", speedEntries, activeSpeedId, speedDeleteConfirmId, { updated -> onSpeedEntries(updated.filterIsInstance<SpeedEntry>()) }, onActiveSpeed, onSpeedDeleteReq, onAddSpeed) }
    }
}

@Composable
fun LevelPanel(
    level: String,
    onLevelChange: (String) -> Unit,
    exp: String,
    onExpChange: (String) -> Unit,
    prof: String,
    onProfChange: (String) -> Unit,
    nextExp: String,
    stats: Map<String, String>
) {
    val colorScheme = MaterialTheme.colorScheme
    
    var localLevel by remember { mutableStateOf(level) }
    var localExp by remember { mutableStateOf(exp) }
    var localProf by remember { mutableStateOf(prof) }

    var ltv by remember { mutableStateOf(TextFieldValue(level, selection = TextRange(level.length))) }
    var etv by remember { mutableStateOf(TextFieldValue(exp, selection = TextRange(exp.length))) }
    var ptv by remember { mutableStateOf(TextFieldValue(prof, selection = TextRange(prof.length))) }
    var isPFocused by remember { mutableStateOf(false) }

    LaunchedEffect(level) { 
        if (localLevel != level) { 
            localLevel = level
            if (ltv.text != level) ltv = ltv.copy(text = level, selection = TextRange(level.length))
        } 
    }
    LaunchedEffect(exp) { 
        if (localExp != exp) { 
            localExp = exp
            if (etv.text != exp) etv = etv.copy(text = exp, selection = TextRange(exp.length))
        } 
    }
    LaunchedEffect(prof) { 
        if (localProf != prof) { 
            localProf = prof
        } 
    }

    LaunchedEffect(localLevel) {
        if (localLevel != level) {
            delay(500)
            onLevelChange(localLevel)
        }
    }
    LaunchedEffect(localExp) {
        if (localExp != exp) {
            delay(500)
            onExpChange(localExp)
        }
    }
    LaunchedEffect(localProf) {
        if (localProf != prof) {
            delay(500)
            onProfChange(localProf)
        }
    }
    
    val currentOnLevelChange by rememberUpdatedState(onLevelChange)
    val currentOnExpChange by rememberUpdatedState(onExpChange)
    val currentOnProfChange by rememberUpdatedState(onProfChange)
    
    DisposableEffect(Unit) {
        onDispose {
            if (localLevel != level) currentOnLevelChange(localLevel)
            if (localExp != exp) currentOnExpChange(localExp)
            if (localProf != prof) currentOnProfChange(localProf)
        }
    }

    LaunchedEffect(localProf, isPFocused, stats) {
        val d = if (isPFocused) localProf else evaluateFormula(localProf, stats).toString()
        if (ptv.text != d) ptv = TextFieldValue(text = d, selection = if (isPFocused) TextRange(d.length) else TextRange.Zero)
    }

    val fl = remember { FocusRequester() }
    val fe = remember { FocusRequester() }
    val fp = remember { FocusRequester() }

    val currentLvlInt = localLevel.toIntOrNull() ?: 1
    val targetLvl = calculateLevelFromExperience(localExp)

    val buttonText: String
    val buttonEnabled: Boolean
    val buttonColor: Color
    
    when {
        currentLvlInt >= 20 -> {
            if (targetLvl < 20) {
                buttonText = "Понизить уровень"
                buttonEnabled = true
                buttonColor = colorScheme.error
            } else {
                buttonText = "Вы стали божеством"
                buttonEnabled = false
                buttonColor = colorScheme.outline.copy(alpha = 0.12f)
            }
        }
        targetLvl > currentLvlInt -> {
            buttonText = "Повысить уровень"
            buttonEnabled = true
            buttonColor = colorScheme.primary
        }
        targetLvl < currentLvlInt -> {
            buttonText = "Понизить уровень"
            buttonEnabled = true
            buttonColor = colorScheme.error
        }
        else -> {
            buttonText = "Недостаточно опыта"
            buttonEnabled = false
            buttonColor = colorScheme.outline.copy(alpha = 0.12f)
        }
    }

    val finalContainerColor = if (buttonEnabled) buttonColor else colorScheme.outline.copy(alpha = 0.12f)
    val finalContentColor = if (buttonEnabled) {
        if (buttonColor == colorScheme.error) colorScheme.onError else colorScheme.onPrimary
    } else {
        colorScheme.onSurface.copy(alpha = 0.38f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(colorScheme.surface.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.2f)), RoundedCornerShape(16.dp))
    ) {
        Text(
            text = "Уровень и Опыт",
            modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally),
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = colorScheme.primary
        )
        
        Row(modifier = Modifier.fillMaxWidth().height(48.dp).clickable { ltv = ltv.copy(selection = TextRange(ltv.text.length)); fl.requestFocus() }, verticalAlignment = Alignment.CenterVertically) {
            Text("Уровень персонажа", modifier = Modifier.padding(start = 16.dp).weight(1f), fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
            BasicTextField(
                value = ltv, 
                onValueChange = { textFieldValue -> 
                    ltv = textFieldValue
                    val filtered = textFieldValue.text.filter { it.isDigit() }
                    if (filtered.isNotEmpty()) {
                        val n = filtered.toInt()
                        if (n in 0..100) {
                            localLevel = n.toString()
                            val milestone = getPreviousLevelThreshold(n.toString())
                            localExp = milestone
                            etv = TextFieldValue(text = milestone, selection = TextRange(milestone.length))
                        }
                    } else {
                        localLevel = ""
                    }
                }, 
                textStyle = TextStyle(textAlign = TextAlign.End, fontSize = 16.sp, color = colorScheme.onSurface, fontWeight = FontWeight.Bold), 
                modifier = Modifier.width(100.dp).padding(end = 16.dp).focusRequester(fl).onFocusChanged { state -> 
                    if (!state.isFocused && localLevel.isEmpty()) { 
                        localLevel = "1"
                        ltv = TextFieldValue("1", selection = TextRange(1))
                        val milestone = getPreviousLevelThreshold("1")
                        localExp = milestone
                        etv = TextFieldValue(text = milestone, selection = TextRange(milestone.length))
                    } 
                }, 
                cursorBrush = SolidColor(colorScheme.primary), 
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
        
        HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.15f))
        
        Row(modifier = Modifier.fillMaxWidth().height(48.dp).clickable { etv = etv.copy(selection = TextRange(etv.text.length)); fe.requestFocus() }, verticalAlignment = Alignment.CenterVertically) {
            Text("Текущий опыт", modifier = Modifier.padding(start = 16.dp).weight(1f), fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
            BasicTextField(
                value = etv, 
                onValueChange = { textFieldValue -> 
                    etv = textFieldValue
                    localExp = textFieldValue.text.filter { it.isDigit() }
                }, 
                textStyle = TextStyle(textAlign = TextAlign.End, fontSize = 16.sp, color = colorScheme.onSurface, fontWeight = FontWeight.Bold), 
                modifier = Modifier.width(100.dp).padding(end = 4.dp).focusRequester(fe).onFocusChanged { state -> 
                    if (!state.isFocused && localExp.isEmpty()) {
                        localExp = "0"
                        etv = TextFieldValue("0", selection = TextRange(1))
                    } 
                }, 
                cursorBrush = SolidColor(colorScheme.primary), 
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            if (currentLvlInt < 20) {
                Text("/ $nextExp", modifier = Modifier.padding(end = 16.dp), fontSize = 14.sp, color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
        }
        
        HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.15f))
        
        Row(modifier = Modifier.fillMaxWidth().height(48.dp).clickable { fp.requestFocus() }, verticalAlignment = Alignment.CenterVertically) {
            Text("Бонус Мастерства", modifier = Modifier.padding(start = 16.dp).weight(1f), fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!isPFocused && evaluateFormula(localProf, stats) >= 0) Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                BasicTextField(
                    value = ptv, 
                    onValueChange = { textFieldValue -> 
                        if (isPFocused) { 
                            ptv = textFieldValue
                            localProf = textFieldValue.text 
                        } 
                    }, 
                    textStyle = TextStyle(textAlign = if (isPFocused) TextAlign.Start else TextAlign.End, fontSize = 16.sp, color = colorScheme.onSurface, fontWeight = FontWeight.Bold), 
                    modifier = Modifier.width(IntrinsicSize.Min).defaultMinSize(10.dp).focusRequester(fp).onFocusChanged { state -> 
                        if (isPFocused != state.isFocused) { 
                            isPFocused = state.isFocused
                            if (!state.isFocused && localProf.isEmpty()) {
                                localProf = "[НАСТ БМ]"
                            }
                        } 
                    }, 
                    cursorBrush = SolidColor(colorScheme.primary)
                )
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        Button(
            onClick = { 
                if (buttonEnabled) {
                    localLevel = targetLvl.toString()
                    ltv = TextFieldValue(targetLvl.toString(), selection = TextRange(targetLvl.toString().length))
                } 
            }, 
            enabled = buttonEnabled, 
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(40.dp), 
            shape = RoundedCornerShape(8.dp), 
            colors = ButtonDefaults.buttonColors(
                containerColor = finalContainerColor,
                contentColor = finalContentColor,
                disabledContainerColor = finalContainerColor,
                disabledContentColor = finalContentColor
            )
        ) { 
            Text(buttonText, fontSize = 14.sp) 
        }
    }
}

@Composable
fun HealthPanel(
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
    hitDiceEntries: List<HitDiceEntry>,
    onSpentHitDiceChange: (Int, Int) -> Unit,
    onOpenSettings: () -> Unit,
    diceIcons: Map<Int, DrawableResource>
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(colorScheme.surface.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.2f)), RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.Default.Settings, null, tint = colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
            
            Text(
                text = "Хиты",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = healthColor,
                textAlign = TextAlign.Center
            )

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(130.dp)
                    .height(34.dp)
            ) {
                val totalPages = if (hitDiceEntries.isNotEmpty()) 1000000 else 0
                val pagerState = rememberPagerState(
                    initialPage = if (totalPages > 0) (totalPages / 2) - (totalPages / 2 % hitDiceEntries.size) else 0,
                    pageCount = { totalPages }
                )
                val coroutineScope = rememberCoroutineScope()
                var totalDragOffset by remember { mutableFloatStateOf(0f) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(hitDiceEntries.size) {
                            if (hitDiceEntries.size <= 1) return@pointerInput
                            detectVerticalDragGestures(
                                onDragStart = { totalDragOffset = 0f },
                                onDragEnd = {
                                    val minThreshold = 10.dp.toPx()
                                    if (abs(totalDragOffset) >= minThreshold) {
                                        coroutineScope.launch {
                                            if (totalDragOffset < 0) {
                                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                            } else {
                                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                            }
                                        }
                                    }
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDragOffset += dragAmount
                                }
                            )
                        }
                ) {
                    VerticalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.End,
                        userScrollEnabled = false
                    ) { pageIdx ->
                        val actualIdx = if (hitDiceEntries.isNotEmpty()) pageIdx % hitDiceEntries.size else 0
                        val entry = hitDiceEntries[actualIdx]
                        val maxHD = entry.formula.split('d').firstOrNull()?.toIntOrNull() ?: 0
                        val dieSize = entry.formula.split('d').lastOrNull()?.toIntOrNull() ?: 8
                        val diceIcon = diceIcons[dieSize]

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = { onSpentHitDiceChange(actualIdx, (entry.spent + 1).coerceAtMost(maxHD)) },
                                modifier = Modifier.size(24.dp),
                                enabled = entry.spent < maxHD
                            ) {
                                Icon(Icons.Default.Remove, null, modifier = Modifier.size(14.dp), tint = colorScheme.primary)
                            }

                            Surface(
                                color = colorScheme.primary.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(horizontal = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "${maxHD - entry.spent}/$maxHD",
                                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                                    )
                                    Spacer(Modifier.width(2.dp))
                                    if (diceIcon != null) {
                                        Icon(
                                            painter = painterResource(diceIcon),
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = colorScheme.primary
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { onSpentHitDiceChange(actualIdx, (entry.spent - 1).coerceAtLeast(0)) },
                                modifier = Modifier.size(24.dp),
                                enabled = entry.spent > 0
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp), tint = colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HealthValueBlock("Максимум", maxHp, colorScheme.onSurfaceVariant, Modifier.weight(1f), readOnly = true) {
                onMaxHpChange(it.filter { c -> c.isDigit() }) 
            }
            HealthValueBlock("Текущие", currentHp, healthColor, Modifier.weight(1f)) {
                onCurrentHpChange(it.filter { c -> c.isDigit() }) 
            }
            HealthValueBlock("Временные", tempHp, Color(0xFF64B5F6), Modifier.weight(1f)) {
                onTempHpChange(it.filter { c -> c.isDigit() }) 
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HealthActionButton("Лечение", Color(0xFF00C46F), Modifier.weight(1f), onHealClick)
            HealthActionButton("Урон", Color(0xFFE57373), Modifier.weight(1f), onDamageClick)
            HealthActionButton("Укрепление", Color(0xFF64B5F6), Modifier.weight(1f), onTempClick)
        }
    }
}

@Composable
fun HealthValueBlock(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    onValueChange: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var tv by remember { mutableStateOf(TextFieldValue(value)) }
    val fr = remember { FocusRequester() }

    LaunchedEffect(value) {
        if (tv.text != value) tv = tv.copy(text = value, selection = TextRange(value.length))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        
        Spacer(Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .clickable { fr.requestFocus() },
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = tv,
                onValueChange = { 
                    if (!readOnly) {
                        tv = it
                        onValueChange(it.text)
                    }
                },
                readOnly = readOnly,
                textStyle = TextStyle(
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(fr),
                cursorBrush = SolidColor(accentColor),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
}

@Composable
fun HealthActionButton(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun FormulaPanel(
    title: String,
    entries: List<FormulaEntry>,
    activeId: String?,
    deleteId: String?,
    onEntries: (List<FormulaEntry>) -> Unit,
    onActive: (String?) -> Unit,
    onDeleteReq: (String?) -> Unit,
    onAdd: () -> Unit,
    headerTrailing: @Composable (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(colorScheme.surface.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.2f)), RoundedCornerShape(16.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = title,
                modifier = Modifier.align(Alignment.Center),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = colorScheme.primary
            )
            if (headerTrailing != null) {
                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                    headerTrailing()
                }
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
                .verticalScroll(rememberScrollState())
        ) {
            entries.forEachIndexed { i, entry ->
                FormulaEntryItem(
                    entry = entry,
                    isActive = entry.id == activeId,
                    isDelete = entry.id == deleteId,
                    onUpdate = { updated -> 
                        val nl = entries.toMutableList()
                        nl[i] = updated
                        onEntries(nl) 
                    },
                    onDelete = { 
                        val nl = entries.toMutableList()
                        nl.removeAt(i)
                        if (entry.id == activeId) onActive(null)
                        onEntries(nl)
                        onDeleteReq(null) 
                    },
                    onDeleteReq = { onDeleteReq(entry.id) },
                    onToggle = { 
                        onActive(if (entry.id == activeId) null else entry.id)
                        onDeleteReq(null) 
                    }
                )
                if (i < entries.size - 1) {
                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        Surface(
            onClick = { 
                onAdd()
                onDeleteReq(null) 
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(44.dp),
            shape = RoundedCornerShape(8.dp),
            color = colorScheme.primary.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.AddCircleOutline, null, modifier = Modifier.size(20.dp), tint = colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Добавить Новое", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
            }
        }
    }
}

@Composable
fun FormulaEntryItem(
    entry: FormulaEntry,
    isActive: Boolean,
    isDelete: Boolean,
    onUpdate: (FormulaEntry) -> Unit,
    onDelete: () -> Unit,
    onDeleteReq: () -> Unit,
    onToggle: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val sep = colorScheme.outlineVariant.copy(alpha = 0.3f)
    val animationSpec = remember {
        spring<IntSize>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
    }
    val floatSpring = remember {
        spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isActive) colorScheme.primary.copy(alpha = 0.05f) else Color.Transparent)
            .animateContentSize(animationSpec)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .fillMaxHeight()
                    .clickable { if (isDelete) onDelete() else onDeleteReq() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isDelete) colorScheme.error else colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            
            VerticalDivider(modifier = Modifier.fillMaxHeight().width(1.dp), color = sep)
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (entry.name.isEmpty()) {
                    Text(
                        text = "Название",
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        fontSize = 16.sp
                    )
                }
                BasicTextField(
                    value = entry.name,
                    onValueChange = { s -> 
                        val u: FormulaEntry = when(entry) { 
                            is ArmorClassEntry -> entry.copy(name = s)
                            is InitiativeEntry -> entry.copy(name = s)
                            is SpeedEntry -> entry.copy(name = s)
                            is ShieldEntry -> entry.copy(name = s)
                        }
                        onUpdate(u) 
                    },
                    textStyle = TextStyle(
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                        color = if (isActive) colorScheme.primary else colorScheme.onSurface,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    cursorBrush = SolidColor(colorScheme.primary)
                )
            }
            
            if (entry is InitiativeEntry) {
                VerticalDivider(modifier = Modifier.fillMaxHeight().width(1.dp), color = sep)
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .fillMaxHeight()
                        .clickable { onUpdate(entry.copy(hasAdvantage = !entry.hasAdvantage)) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = if (entry.hasAdvantage) colorScheme.primary else colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            VerticalDivider(modifier = Modifier.fillMaxHeight().width(1.dp), color = sep)
            
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .fillMaxHeight()
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isActive) Icons.Default.Close else Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isActive) colorScheme.primary else colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        
        AnimatedVisibility(
            visible = isActive,
            enter = expandVertically(animationSpec) + fadeIn(floatSpring),
            exit = shrinkVertically(animationSpec) + fadeOut(floatSpring)
        ) {
            Column {
                HorizontalDivider(color = sep, thickness = 1.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .background(colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (entry.formula.isEmpty() && entry.bonuses.none { it.isActive }) {
                        Text(
                            text = "Формула",
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            fontSize = 14.sp
                        )
                    }
                    BasicTextField(
                        value = getFullFormula(entry),
                        onValueChange = { s -> 
                            val u: FormulaEntry = when(entry) { 
                                is ArmorClassEntry -> entry.copy(formula = s, bonuses = entry.bonuses.map { it.copy(isActive = false) })
                                is InitiativeEntry -> entry.copy(formula = s, bonuses = entry.bonuses.map { it.copy(isActive = false) })
                                is SpeedEntry -> entry.copy(formula = s, bonuses = entry.bonuses.map { it.copy(isActive = false) })
                                is ShieldEntry -> entry.copy(formula = s, bonuses = entry.bonuses.map { it.copy(isActive = false) })
                            }
                            onUpdate(u) 
                        },
                        textStyle = TextStyle(fontSize = 14.sp, color = colorScheme.onSurface, fontWeight = FontWeight.Medium),
                        modifier = Modifier.fillMaxWidth(),
                        cursorBrush = SolidColor(colorScheme.primary)
                    )
                }
            }
        }
    }
}

fun getFullFormula(entry: FormulaEntry): String {
    var full = entry.formula
    val isInitiative = entry is InitiativeEntry
    
    if (!isInitiative) {
        full = full.replace(Regex("\\b\\d*d\\d+\\b", RegexOption.IGNORE_CASE), "").trim()
        full = full.replace(Regex("\\+\\s*$"), "").replace(Regex("^\\s*\\+"), "").trim()
    }
    
    entry.bonuses.filter { it.isActive }.forEach {
        var f = it.formula.trim()
        if (!isInitiative) {
            f = f.replace(Regex("\\b\\d*d\\d+\\b", RegexOption.IGNORE_CASE), "").trim()
            f = f.replace(Regex("^\\s*\\+\\s*"), "").replace(Regex("\\s*\\+\\s*$"), "")
        }
        
        if (f.isNotEmpty()) {
            val prefix = if (f.startsWith("+") || f.startsWith("-")) " " else " + "
            full += "$prefix$f"
        }
    }
    return full
}

@Composable
fun ConditionsPanel(
    allConditions: List<Condition>,
    selectedConditions: List<String>,
    onToggleCondition: (String) -> Unit,
    exhaustion: Int,
    onExhaustionChange: (Int) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(colorScheme.surface.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.2f)), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Состояния",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = colorScheme.primary
        )
        
        Spacer(Modifier.height(16.dp))
        
        // Exhaustion Row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Степень истощения", modifier = Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Medium)
            IconButton(onClick = { if (exhaustion > 0) onExhaustionChange(exhaustion - 1) }) {
                Icon(Icons.Default.Remove, null, tint = colorScheme.primary)
            }
            Text("$exhaustion", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
            IconButton(onClick = { if (exhaustion < 6) onExhaustionChange(exhaustion + 1) }) {
                Icon(Icons.Default.Add, null, tint = colorScheme.primary)
            }
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = colorScheme.outlineVariant.copy(alpha = 0.3f))
        
        // Conditions Flow
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            allConditions.forEach { condition ->
                val isSelected = selectedConditions.contains(condition.name)
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggleCondition(condition.name) },
                    label = { Text(condition.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = colorScheme.primary
                    )
                )
            }
        }
    }
}
