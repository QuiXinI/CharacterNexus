package ru.quasaris.characternexus.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.LocalHazeStyle
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.quasaris.characternexus.model.*

/**
 * Стиль размытия для нижней панели выбора вкладок.
 */
val TabSheetHazeStyle = HazeStyle(
    blurRadius = 24.dp,
    tints = listOf(HazeTint(Color.Black.copy(alpha = 0.25f)))
)

@Composable
fun DesktopTabNavigationBar(
    currentTab: CharacterTab,
    tabs: List<CharacterTab>,
    onTabSelected: (CharacterTab) -> Unit,
    actions: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    var availableWidthPx by remember { mutableStateOf(0) }
    val tabCoordinates = remember { mutableStateMapOf<CharacterTab, androidx.compose.ui.layout.LayoutCoordinates>() }
    val containerCoordinates = remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }

    // Procedurally calculate exact required width for each tab based on title length + icon + padding
    val maxRequiredTabWidth = remember(tabs) {
        val maxTitleLen = tabs.maxOfOrNull { it.title.length } ?: 6
        (maxTitleLen * 8.5f).dp + 52.dp
    }

    val totalNaturalWidth = remember(tabs) {
        val totalChars = tabs.sumOf { it.title.length }
        (totalChars * 8.5f).dp + (tabs.size * 52).dp
    }

    val availableWidthDp = with(density) { availableWidthPx.toDp() }
    val spacing = 4.dp
    val totalSpacing = spacing * (tabs.size - 1).coerceAtLeast(0)

    val equalTabWidth = if (tabs.isNotEmpty() && availableWidthDp > 16.dp) (availableWidthDp - 16.dp - totalSpacing) / tabs.size else 0.dp
    val canFitEqualStretched = availableWidthPx > 0 && equalTabWidth >= maxRequiredTabWidth
    val canFitNatural = availableWidthPx > 0 && availableWidthDp >= (totalNaturalWidth + 16.dp)

    val pointerInputModifier = Modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Scroll) {
                    val change = event.changes.firstOrNull()
                    if (change != null) {
                        val scrollDelta = change.scrollDelta
                        val delta = if (scrollDelta.y != 0f) scrollDelta.y else scrollDelta.x
                        if (delta != 0f) {
                            val pxToScroll = with(density) { delta * 36.dp.toPx() }
                            scrollState.dispatchRawDelta(pxToScroll)
                            change.consume()
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(currentTab) {
        val tabIndex = tabs.indexOf(currentTab)
        if (tabIndex >= 0 && tabs.isNotEmpty()) {
            val maxScroll = scrollState.maxValue
            if (maxScroll > 0) {
                val approxTabWidth = maxScroll.toFloat() / (tabs.size - 1).coerceAtLeast(1)
                val targetScroll = (tabIndex * approxTabWidth).coerceIn(0f, maxScroll.toFloat())
                scrollState.animateScrollTo(targetScroll.toInt())
            }
        }
    }

    Surface(
        color = colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .onSizeChanged { availableWidthPx = it.width },
                contentAlignment = Alignment.CenterStart
            ) {
                val containerCoords = containerCoordinates.value
                val activeTabCoords = tabCoordinates[currentTab]
                val targetOffsetX = if (containerCoords != null && activeTabCoords != null) {
                    containerCoords.localPositionOf(activeTabCoords, androidx.compose.ui.geometry.Offset.Zero).x
                } else {
                    0f
                }
                val targetWidth = activeTabCoords?.size?.width?.toFloat() ?: 0f

                val animatedOffsetX by animateFloatAsState(
                    targetValue = targetOffsetX,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                )
                val animatedWidth by animateFloatAsState(
                    targetValue = targetWidth,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                )

                if (canFitEqualStretched) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp)
                            .onGloballyPositioned { containerCoordinates.value = it },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (animatedWidth > 0f) {
                            Box(
                                modifier = Modifier
                                    .graphicsLayer { translationX = animatedOffsetX }
                                    .width(with(density) { animatedWidth.toDp() })
                                    .height(38.dp)
                                    .background(colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(spacing)
                        ) {
                            tabs.forEach { tab ->
                                val isSelected = tab == currentTab
                                val icon = when (tab) {
                                    CharacterTab.STATS -> Icons.Default.Person
                                    CharacterTab.ATTACKS -> Icons.Default.Gavel
                                    CharacterTab.BIO -> Icons.Default.Book
                                    CharacterTab.INVENTORY -> Icons.Default.Inventory
                                    CharacterTab.SPELLS -> Icons.Default.AutoFixHigh
                                    CharacterTab.NOTES -> Icons.AutoMirrored.Filled.Note
                                    CharacterTab.SKILLS_FEATS -> Icons.Default.Star
                                }

                                val textColor by animateColorAsState(if (isSelected) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.85f))
                                val iconColor by animateColorAsState(if (isSelected) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.6f))

                                Surface(
                                    onClick = { onTabSelected(tab) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.Transparent,
                                    contentColor = textColor,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .onGloballyPositioned { coordinates ->
                                            tabCoordinates[tab] = coordinates
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = iconColor
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = tab.title,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = textColor,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (canFitNatural) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp)
                            .onGloballyPositioned { containerCoordinates.value = it },
                        contentAlignment = Alignment.Center
                    ) {
                        if (animatedWidth > 0f) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .graphicsLayer { translationX = animatedOffsetX }
                                    .width(with(density) { animatedWidth.toDp() })
                                    .height(38.dp)
                                    .background(colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxHeight(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(spacing)
                        ) {
                            tabs.forEach { tab ->
                                val isSelected = tab == currentTab
                                val icon = when (tab) {
                                    CharacterTab.STATS -> Icons.Default.Person
                                    CharacterTab.ATTACKS -> Icons.Default.Gavel
                                    CharacterTab.BIO -> Icons.Default.Book
                                    CharacterTab.INVENTORY -> Icons.Default.Inventory
                                    CharacterTab.SPELLS -> Icons.Default.AutoFixHigh
                                    CharacterTab.NOTES -> Icons.AutoMirrored.Filled.Note
                                    CharacterTab.SKILLS_FEATS -> Icons.Default.Star
                                }

                                val textColor by animateColorAsState(if (isSelected) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.85f))
                                val iconColor by animateColorAsState(if (isSelected) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.6f))

                                Surface(
                                    onClick = { onTabSelected(tab) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.Transparent,
                                    contentColor = textColor,
                                    modifier = Modifier
                                        .height(38.dp)
                                        .onGloballyPositioned { coordinates ->
                                            tabCoordinates[tab] = coordinates
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .padding(horizontal = 14.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = iconColor
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = tab.title,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = textColor,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(pointerInputModifier)
                            .horizontalScroll(scrollState),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 8.dp)
                                .onGloballyPositioned { containerCoordinates.value = it }
                        ) {
                            if (animatedWidth > 0f) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .graphicsLayer { translationX = animatedOffsetX }
                                        .width(with(density) { animatedWidth.toDp() })
                                        .height(38.dp)
                                        .background(colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxHeight(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(spacing)
                            ) {
                                tabs.forEach { tab ->
                                    val isSelected = tab == currentTab
                                    val icon = when (tab) {
                                        CharacterTab.STATS -> Icons.Default.Person
                                        CharacterTab.ATTACKS -> Icons.Default.Gavel
                                        CharacterTab.BIO -> Icons.Default.Book
                                        CharacterTab.INVENTORY -> Icons.Default.Inventory
                                        CharacterTab.SPELLS -> Icons.Default.AutoFixHigh
                                        CharacterTab.NOTES -> Icons.AutoMirrored.Filled.Note
                                        CharacterTab.SKILLS_FEATS -> Icons.Default.Star
                                    }

                                    val textColor by animateColorAsState(if (isSelected) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.85f))
                                    val iconColor by animateColorAsState(if (isSelected) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.6f))

                                    Surface(
                                        onClick = { onTabSelected(tab) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.Transparent,
                                        contentColor = textColor,
                                        modifier = Modifier
                                            .height(38.dp)
                                            .onGloballyPositioned { coordinates ->
                                                tabCoordinates[tab] = coordinates
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .padding(horizontal = 14.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = iconColor
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = tab.title,
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = textColor,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(colorScheme.onSurface.copy(alpha = 0.12f))
            )

            Box(
                modifier = Modifier
                    .width(132.dp)
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                actions()
            }
        }
    }
}

@Composable
fun DesktopTabActions(
    tab: CharacterTab,
    state: CharacterDetailState,
    onShowSpellSettings: (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (tab == CharacterTab.SPELLS && onShowSpellSettings != null) {
            IconButton(
                onClick = onShowSpellSettings,
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = "Spell Settings", tint = colorScheme.primary)
            }
        }

        val supportExpansion = tab in listOf(
            CharacterTab.BIO, CharacterTab.SKILLS_FEATS,
            CharacterTab.INVENTORY, CharacterTab.SPELLS, CharacterTab.NOTES
        )

        if (supportExpansion) {
            val anyCollapsed = when (tab) {
                CharacterTab.BIO -> state.bioLongSections.any { !it.isExpanded }
                CharacterTab.SKILLS_FEATS -> state.skillsAndTraits.any { !it.isExpanded }
                CharacterTab.INVENTORY -> state.inventory.any { !it.isExpanded }
                CharacterTab.SPELLS -> state.spells.any { !it.isExpanded }
                CharacterTab.NOTES -> state.notes.any { !it.isExpanded }
                else -> false
            }

            IconButton(
                onClick = {
                    when (tab) {
                        CharacterTab.BIO -> state.bioLongSections = state.bioLongSections.map { it.copy(isExpanded = anyCollapsed) }
                        CharacterTab.SKILLS_FEATS -> state.skillsAndTraits = state.skillsAndTraits.map { it.copy(isExpanded = anyCollapsed) }
                        CharacterTab.INVENTORY -> state.inventory = state.inventory.map { it.copy(isExpanded = anyCollapsed) }
                        CharacterTab.SPELLS -> state.spells = state.spells.map { it.copy(isExpanded = anyCollapsed) }
                        CharacterTab.NOTES -> state.notes = state.notes.map { it.copy(isExpanded = anyCollapsed) }
                        else -> {}
                    }
                },
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Icon(
                    imageVector = if (anyCollapsed) Icons.Default.UnfoldMore else Icons.Default.UnfoldLess,
                    contentDescription = "Toggle All Expansion",
                    tint = colorScheme.primary
                )
            }
        }

        val showEdit = tab != CharacterTab.STATS

        if (showEdit) {
            IconButton(
                onClick = { state.isEditMode = !state.isEditMode },
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Icon(
                    if (state.isEditMode) Icons.Default.EditOff else Icons.Default.Edit,
                    contentDescription = "Toggle Edit Mode",
                    tint = if (state.isEditMode) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun TabNavigationBar(
    currentTab: CharacterTab,
    onShowTabSheet: () -> Unit,
    actions: @Composable () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(colorScheme.surface)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(modifier = Modifier.weight(1f))

        Surface(
            color = colorScheme.primary.copy(alpha = 0.1f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onShowTabSheet() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentTab.title.uppercase(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            actions()
        }
    }
}

@Composable
fun TabActions(
    tab: CharacterTab,
    state: CharacterDetailState,
    onShowSpellSettings: (() -> Unit)? = null,
    isDesktop: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        if (tab == CharacterTab.SPELLS && onShowSpellSettings != null) {
            IconButton(onClick = onShowSpellSettings) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = "Spell Settings", tint = colorScheme.primary)
            }
        }

        if (tab == CharacterTab.STATS && !isDesktop) {
            IconButton(onClick = { state.isAdvancedMode = !state.isAdvancedMode }) {
                Icon(
                    imageVector = Icons.Default.UnfoldMore,
                    contentDescription = "Toggle Skills",
                    tint = colorScheme.primary
                )
            }
        }

        val supportExpansion = tab in listOf(
            CharacterTab.BIO, CharacterTab.SKILLS_FEATS, 
            CharacterTab.INVENTORY, CharacterTab.SPELLS, CharacterTab.NOTES
        )
        
        if (supportExpansion) {
            val anyCollapsed = when(tab) {
                CharacterTab.BIO -> state.bioLongSections.any { !it.isExpanded }
                CharacterTab.SKILLS_FEATS -> state.skillsAndTraits.any { !it.isExpanded }
                CharacterTab.INVENTORY -> state.inventory.any { !it.isExpanded }
                CharacterTab.SPELLS -> state.spells.any { !it.isExpanded }
                CharacterTab.NOTES -> state.notes.any { !it.isExpanded }
                else -> false
            }
            
            IconButton(onClick = {
                when(tab) {
                    CharacterTab.BIO -> state.bioLongSections = state.bioLongSections.map { it.copy(isExpanded = anyCollapsed) }
                    CharacterTab.SKILLS_FEATS -> state.skillsAndTraits = state.skillsAndTraits.map { it.copy(isExpanded = anyCollapsed) }
                    CharacterTab.INVENTORY -> state.inventory = state.inventory.map { it.copy(isExpanded = anyCollapsed) }
                    CharacterTab.SPELLS -> state.spells = state.spells.map { it.copy(isExpanded = anyCollapsed) }
                    CharacterTab.NOTES -> state.notes = state.notes.map { it.copy(isExpanded = anyCollapsed) }
                    else -> {}
                }
            }) {
                Icon(
                    imageVector = if (anyCollapsed) Icons.Default.UnfoldMore else Icons.Default.UnfoldLess,
                    contentDescription = "Toggle All Expansion",
                    tint = colorScheme.primary
                )
            }
        }

        val showEdit = tab != CharacterTab.STATS
        
        if (showEdit) {
            IconButton(onClick = { state.isEditMode = !state.isEditMode }) {
                Icon(
                    if (state.isEditMode) Icons.Default.EditOff else Icons.Default.Edit,
                    contentDescription = "Toggle Edit Mode",
                    tint = if (state.isEditMode) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun TabControlHeader(
    isEditMode: Boolean,
    onToggleEditMode: () -> Unit,
    hasContentToEdit: Boolean = true,
    isCollapsible: Boolean = false,
    anyCollapsed: Boolean = false,
    onToggleAllExpansion: () -> Unit = {},
    onShowSpellSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        if (onShowSpellSettings != null) {
            IconButton(onClick = onShowSpellSettings) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = "Spell Settings", tint = colorScheme.primary)
            }
        }

        if (isCollapsible) {
            IconButton(onClick = onToggleAllExpansion) {
                Icon(
                    imageVector = if (anyCollapsed) Icons.Default.UnfoldMore else Icons.Default.UnfoldLess,
                    contentDescription = "Toggle All Expansion",
                    tint = colorScheme.primary
                )
            }
        }

        if (hasContentToEdit) {
            IconButton(onClick = onToggleEditMode) {
                Icon(
                    if (isEditMode) Icons.Default.EditOff else Icons.Default.Edit,
                    contentDescription = "Toggle Edit Mode",
                    tint = if (isEditMode) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabSelectionSheet(
    showTabSheet: Boolean,
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    currentTab: CharacterTab,
    tabs: List<CharacterTab>,
    pagerState: PagerState,
    scope: CoroutineScope,
    hazeState: HazeState?,
    blurPopups: Boolean
) {
    if (!showTabSheet) return
    
    val colorScheme = MaterialTheme.colorScheme
    val isOled = colorScheme.background == Color.Black

    CompositionLocalProvider(LocalHazeStyle provides ru.quasaris.characternexus.ui.TabSheetHazeStyle) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            containerColor = if (isOled) Color.Black 
                             else if (blurPopups) colorScheme.surface.copy(alpha = 0.1f) 
                             else colorScheme.surface,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .run {
                        if (blurPopups && hazeState != null && !isOled) {
                            this.clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                                .hazeEffect(state = hazeState) {
                                    style = HazeStyle(blurRadius = 24.dp, tints = listOf(HazeTint(colorScheme.surface.copy(alpha = 0.1f))))
                                    inputScale = HazeInputScale.Fixed(0.6f)
                                }
                        } else this
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
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
                            leadingContent = {
                                val icon = when(tab) {
                                    CharacterTab.STATS -> Icons.Default.Person
                                    CharacterTab.ATTACKS -> Icons.Default.Gavel
                                    CharacterTab.BIO -> Icons.Default.Book
                                    CharacterTab.INVENTORY -> Icons.Default.Inventory
                                    CharacterTab.SPELLS -> Icons.Default.AutoFixHigh
                                    CharacterTab.NOTES -> Icons.AutoMirrored.Filled.Note
                                    CharacterTab.SKILLS_FEATS -> Icons.Default.Star
                                }
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = if (tab == currentTab) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                val currentP = pagerState.currentPage
                                val currentIdx = currentP % tabs.size
                                val diff = index - currentIdx

                                scope.launch {
                                    if (kotlin.math.abs(diff) <= 1) {
                                        pagerState.animateScrollToPage(currentP + diff)
                                    } else {
                                        pagerState.scrollToPage(currentP + diff)
                                    }
                                    sheetState.hide()
                                    onDismissRequest()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
