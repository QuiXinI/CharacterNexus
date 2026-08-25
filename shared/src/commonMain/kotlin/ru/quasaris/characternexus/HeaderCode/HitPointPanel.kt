package ru.quasaris.characternexus.HeaderCode

import ru.quasaris.characternexus.model.*
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.quasaris.characternexus.model.HitDiceEntry
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import characternexus.shared.generated.resources.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import kotlin.math.abs

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
    onFocusLost: () -> Unit,
    hitDiceEntries: List<HitDiceEntry>,
    onSpentHitDiceChange: (Int, Int) -> Unit,
    onOpenSettings: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(colorScheme.surface.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .border(androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.2f)), RoundedCornerShape(16.dp))
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
                                    // Порог в 10dp отсекает случайные микро-сдвиги при тапе по кнопкам
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
                        val diceIcon = when (dieSize) {
                            2 -> Res.drawable.ic_d2_dice
                            4 -> Res.drawable.ic_d4_dice
                            6 -> Res.drawable.ic_d6_dice
                            8 -> Res.drawable.ic_d8_dice
                            10 -> Res.drawable.ic_d10_dice
                            12 -> Res.drawable.ic_d12_dice
                            20 -> Res.drawable.ic_d20_dice
                            else -> Res.drawable.ic_d20_dice
                        }

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
                                    Icon(
                                        painter = painterResource(diceIcon),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = colorScheme.primary
                                    )
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
        border = borderStroke(color.copy(alpha = 0.3f))
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
private fun borderStroke(color: Color) = androidx.compose.foundation.BorderStroke(1.dp, color)
