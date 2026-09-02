package ru.quasaris.characternexus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
                                    launch { pagerState.animateScrollToPage(currentP + diff) }
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
