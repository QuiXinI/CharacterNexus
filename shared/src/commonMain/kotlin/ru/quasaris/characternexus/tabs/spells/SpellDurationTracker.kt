package ru.quasaris.characternexus.tabs.spells

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.ui.outerShadow
import ru.quasaris.characternexus.util.HapticType
import ru.quasaris.characternexus.util.PlatformUtils
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpellDurationTracker(
    trackedDurations: List<TrackedSpellDuration>,
    onUpdateTrackedDurations: (List<TrackedSpellDuration>) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    if (trackedDurations.isEmpty()) {
        Surface(
            modifier = modifier
                .fillMaxHeight()
                .outerShadow(shape = RoundedCornerShape(16.dp), blur = 2.dp, offsetY = 1.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            color = Color.Transparent,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 0.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Трекер длительности заклинаний",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.primary,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Нет активных",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }
        }
        return
    }

    var showDurationDialogFor by remember { mutableStateOf<TrackedSpellDuration?>(null) }
    var showAbortDialogFor by remember { mutableStateOf<TrackedSpellDuration?>(null) }

    val totalPages = 1000000
    val pagerState = rememberPagerState(
        initialPage = (totalPages / 2) - ((totalPages / 2) % trackedDurations.size),
        pageCount = { totalPages }
    )
    val coroutineScope = rememberCoroutineScope()
    var totalDragOffset by remember { mutableFloatStateOf(0f) }
    val currentActualIdx = pagerState.currentPage % trackedDurations.size
    val currentTracked = trackedDurations.getOrNull(currentActualIdx) ?: trackedDurations.first()

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .outerShadow(shape = RoundedCornerShape(16.dp), blur = 2.dp, offsetY = 1.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onLongClick = {
                    PlatformUtils.performHapticFeedback(HapticType.LONG_PRESS)
                    showAbortDialogFor = currentTracked
                }
            ),
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(trackedDurations.size) {
                    if (trackedDurations.size <= 1) return@pointerInput
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Scroll) {
                                val scrollDelta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                if (scrollDelta != 0f && !pagerState.isScrollInProgress) {
                                    coroutineScope.launch {
                                        if (scrollDelta > 0f) {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        } else {
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                .pointerInput(trackedDurations.size) {
                    if (trackedDurations.size <= 1) return@pointerInput
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
                },
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Dot indicators on left if multiple (up to 7 dots with outer reduction)
                if (trackedDurations.size > 1) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        val maxVisibleDots = 7
                        val totalDots = trackedDurations.size
                        val (startIndex, endIndex) = if (totalDots <= maxVisibleDots) {
                            0 to (totalDots - 1)
                        } else {
                            val half = maxVisibleDots / 2
                            var start = currentActualIdx - half
                            var end = currentActualIdx + half
                            if (start < 0) {
                                end += -start
                                start = 0
                            }
                            if (end >= totalDots) {
                                start -= (end - totalDots + 1)
                                end = totalDots - 1
                            }
                            start.coerceAtLeast(0) to end.coerceAtMost(totalDots - 1)
                        }

                        for (idx in startIndex..endIndex) {
                            val distance = abs(idx - currentActualIdx)
                            val isEdge = (idx == startIndex && startIndex > 0) || (idx == endIndex && endIndex < totalDots - 1)

                            val targetSize = when {
                                distance == 0 -> 4.5.dp
                                distance == 1 && !isEdge -> 3.5.dp
                                distance == 2 && !isEdge -> 2.5.dp
                                else -> 2.dp
                            }

                            val targetAlpha = when {
                                distance == 0 -> 1f
                                distance == 1 -> 0.7f
                                distance == 2 -> 0.4f
                                else -> 0.2f
                            }

                            val dotSize by animateDpAsState(targetValue = targetSize, label = "dotSize")
                            val dotAlpha by animateFloatAsState(targetValue = targetAlpha, label = "dotAlpha")
                            val dotColor by animateColorAsState(
                                targetValue = if (distance == 0) colorScheme.primary else colorScheme.primary.copy(alpha = dotAlpha),
                                label = "dotColor"
                            )

                            Box(
                                modifier = Modifier
                                    .size(dotSize)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                        }
                    }
                }

                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    userScrollEnabled = false
                ) { pageIdx ->
                    val actualIdx = pageIdx % trackedDurations.size
                    val tracked = trackedDurations.getOrNull(actualIdx) ?: return@VerticalPager

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top row: Name & Value Display
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (tracked.isConcentration) {
                                    Icon(
                                        imageVector = Icons.Default.SelfImprovement,
                                        contentDescription = "Концентрация",
                                        modifier = Modifier.size(12.dp),
                                        tint = colorScheme.error
                                    )
                                }
                                Text(
                                    text = tracked.spellName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface,
                                    maxLines = 1,
                                    fontSize = 11.sp
                                )
                            }

                            // Value display box
                            Box(
                                modifier = Modifier
                                    .size(height = 24.dp, width = 52.dp)
                                    .outerShadow(
                                        shape = RoundedCornerShape(8.dp),
                                        blur = 1.dp,
                                        offsetY = 1.dp
                                    )
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colorScheme.primary.copy(alpha = 0.1f))
                                    .clickable { showDurationDialogFor = tracked },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = formatRemainingDuration(tracked.remainingRounds),
                                    style = TextStyle(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = colorScheme.primary,
                                        textAlign = TextAlign.Center
                                    ),
                                    maxLines = 1
                                )
                            }
                        }

                        // Middle row: Very large Minus button, Progress Bar (slider-sized), very large Plus button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SpellTrackerButton(
                                icon = Icons.Default.Remove,
                                onClick = {
                                    PlatformUtils.performHapticFeedback(HapticType.CLICK)
                                    val updated = trackedDurations.map {
                                        it.copy(remainingRounds = (it.remainingRounds - 1).coerceAtLeast(0))
                                    }.filter { it.remainingRounds > 0 }
                                    onUpdateTrackedDurations(updated)
                                },
                                onLongClick = {
                                    PlatformUtils.performHapticFeedback(HapticType.LONG_PRESS)
                                    val updated = trackedDurations.map {
                                        if (it.id == tracked.id) {
                                            it.copy(remainingRounds = (it.remainingRounds - 1).coerceAtLeast(0))
                                        } else it
                                    }.filter { it.remainingRounds > 0 }
                                    onUpdateTrackedDurations(updated)
                                }
                            )

                            val progress = if (tracked.initialDurationRounds > 0) {
                                (tracked.remainingRounds.toFloat() / tracked.initialDurationRounds.toFloat()).coerceIn(0f, 1f)
                            } else 1f

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showDurationDialogFor = tracked },
                                color = colorScheme.primary,
                                trackColor = colorScheme.primary.copy(alpha = 0.2f)
                            )

                            SpellTrackerButton(
                                icon = Icons.Default.Add,
                                onClick = {
                                    PlatformUtils.performHapticFeedback(HapticType.CLICK)
                                    val updated = trackedDurations.map {
                                        it.copy(remainingRounds = it.remainingRounds + 1)
                                    }
                                    onUpdateTrackedDurations(updated)
                                },
                                onLongClick = {
                                    PlatformUtils.performHapticFeedback(HapticType.LONG_PRESS)
                                    val updated = trackedDurations.map {
                                        if (it.id == tracked.id) {
                                            it.copy(remainingRounds = it.remainingRounds + 1)
                                        } else it
                                    }
                                    onUpdateTrackedDurations(updated)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Abort Spell Dialog (on long press)
    if (showAbortDialogFor != null) {
        val target = showAbortDialogFor!!
        AlertDialog(
            onDismissRequest = { showAbortDialogFor = null },
            title = { Text("Прервать заклинание?") },
            text = { Text("Хотите ли прервать заклинание \"${target.spellName}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    val updated = trackedDurations.filter { it.id != target.id }
                    onUpdateTrackedDurations(updated)
                    showAbortDialogFor = null
                }) {
                    Text("Да, прервать", color = colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAbortDialogFor = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Duration Adjust Dialog (on click value display or progress bar)
    if (showDurationDialogFor != null) {
        var inputVal by remember { mutableStateOf("1") }
        var selectedUnitIndex by remember { mutableIntStateOf(0) }

        AlertDialog(
            onDismissRequest = { showDurationDialogFor = null },
            title = { Text("Изменить длительность всех заклинаний") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Введите величину изменения для всех активных заклинаний в трекере:", style = MaterialTheme.typography.bodySmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputVal,
                            onValueChange = { inputVal = it.filter { c -> c.isDigit() } },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            label = { Text("Число") }
                        )

                        SingleChoiceSegmentedButtonRow {
                            SegmentedButton(
                                selected = selectedUnitIndex == 0,
                                onClick = { selectedUnitIndex = 0 },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                            ) {
                                Text("Ходы", fontSize = 11.sp)
                            }
                            SegmentedButton(
                                selected = selectedUnitIndex == 1,
                                onClick = { selectedUnitIndex = 1 },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                            ) {
                                Text("Мин", fontSize = 11.sp)
                            }
                            SegmentedButton(
                                selected = selectedUnitIndex == 2,
                                onClick = { selectedUnitIndex = 2 },
                                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                            ) {
                                Text("Час", fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        val amount = inputVal.toIntOrNull() ?: 0
                        val roundsDelta = when (selectedUnitIndex) {
                            0 -> amount
                            1 -> amount * 10
                            2 -> amount * 600
                            else -> amount
                        }
                        val updated = trackedDurations.map {
                            it.copy(remainingRounds = (it.remainingRounds - roundsDelta).coerceAtLeast(0))
                        }.filter { it.remainingRounds > 0 }
                        onUpdateTrackedDurations(updated)
                        showDurationDialogFor = null
                    }) {
                        Text("Уменьшить")
                    }
                    TextButton(onClick = {
                        val amount = inputVal.toIntOrNull() ?: 0
                        val roundsDelta = when (selectedUnitIndex) {
                            0 -> amount
                            1 -> amount * 10
                            2 -> amount * 600
                            else -> amount
                        }
                        val updated = trackedDurations.map {
                            it.copy(remainingRounds = it.remainingRounds + roundsDelta)
                        }
                        onUpdateTrackedDurations(updated)
                        showDurationDialogFor = null
                    }) {
                        Text("Увеличить")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDurationDialogFor = null }) {
                    Text("Закрыть")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpellTrackerButton(
    icon: ImageVector,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(42.dp)
            .outerShadow(
                shape = RoundedCornerShape(12.dp),
                blur = 2.dp,
                offsetY = 1.dp
            )
            .clip(RoundedCornerShape(12.dp))
            .background(colorScheme.primary.copy(alpha = 0.15f))
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = colorScheme.primary
        )
    }
}
