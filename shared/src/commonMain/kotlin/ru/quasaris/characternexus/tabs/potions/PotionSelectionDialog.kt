package ru.quasaris.characternexus.tabs.potions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import org.jetbrains.compose.resources.painterResource
import ru.quasaris.characternexus.backend.MagicItemManager
import ru.quasaris.characternexus.backend.GameMagicItem
import ru.quasaris.characternexus.backend.SettingsViewModel
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.ui.DialogDimStyle
import ru.quasaris.characternexus.ui.BackHandler
import ru.quasaris.characternexus.ui.theme.rememberEffectiveBlurRadius
import ru.quasaris.characternexus.ui.theme.hazePopover
import characternexus.shared.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PotionSelectionDialog(
    manager: MagicItemManager,
    initialSelectedIds: Set<String>,
    onDismiss: () -> Unit,
    onSelectBatch: (List<GameMagicItem>) -> Unit,
    settingsViewModel: SettingsViewModel? = null,
    hazeState: HazeState? = null,
    popupHazeState: HazeState? = null,
    forceBlurEnabled: Boolean = true,
    isDesktop: Boolean = false
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf(initialSelectedIds) }
    
    val allItems = remember { manager.loadItems() }
    val filteredItems = remember(searchQuery, allItems) {
        allItems.filter { 
            it.name?.contains(searchQuery, ignoreCase = true) == true ||
            it.description?.contains(searchQuery, ignoreCase = true) == true ||
            it.englishName?.contains(searchQuery, ignoreCase = true) == true
        }
    }

    val blurRadius = rememberEffectiveBlurRadius(settingsViewModel)
    val colorScheme = MaterialTheme.colorScheme
    val isOled = colorScheme.background == Color.Black
    val effectiveHazeState = popupHazeState ?: hazeState

    val content = @Composable {
        BackHandler(onBack = onDismiss)
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .hazePopover(
                    state = effectiveHazeState,
                    blurRadius = blurRadius,
                    forceBlurEnabled = forceBlurEnabled,
                    isOled = isOled
                ),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Выбор зелья", fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = if (forceBlurEnabled && effectiveHazeState != null && !isOled) Color.Transparent.copy(alpha = 0.0f) else colorScheme.surface
                    )
                )
            },
            containerColor = if (forceBlurEnabled && effectiveHazeState != null && !isOled) Color.Transparent.copy(alpha = 0.0f) else colorScheme.background
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        placeholder = { Text("Поиск...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    if (filteredItems.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Зелья не найдены", color = colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 0.dp,
                                bottom = 80.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredItems, key = { "${it.sourceModuleId}_${it.id}_${it.name}" }) { item ->
                                val isSelected = selectedIds.contains(item.id ?: "")
                                PotionSelectionItem(
                                    item = item,
                                    isSelected = isSelected,
                                    onSelect = { 
                                        val id = item.id ?: return@PotionSelectionItem
                                        selectedIds = if (selectedIds.contains(id)) selectedIds - id else selectedIds + id
                                    }
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        val result = allItems.filter { selectedIds.contains(it.id ?: "") }
                        onSelectBatch(result)
                        onDismiss()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp
                    )
                ) {
                    Text("ВЫБРАТЬ (${selectedIds.size})", fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }

    if (isDesktop) {
        content()
    } else {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            DialogDimStyle(0f)
            content()
        }
    }
}

@Composable
fun PotionSelectionItem(
    item: GameMagicItem,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    val potionColor = remember(item.colorHex) {
        try {
            val hex = item.colorHex.removePrefix("#")
            when (hex.length) {
                6 -> Color(
                    red = hex.substring(0, 2).toInt(16),
                    green = hex.substring(2, 4).toInt(16),
                    blue = hex.substring(4, 6).toInt(16)
                )
                8 -> Color(
                    red = hex.substring(0, 2).toInt(16),
                    green = hex.substring(2, 4).toInt(16),
                    blue = hex.substring(4, 6).toInt(16),
                    alpha = hex.substring(6, 8).toInt(16)
                )
                else -> Color.Gray
            }
        } catch (e: Exception) {
            Color.Gray
        }
    }

    val iconRes = when (item.iconIndex) {
        1 -> Res.drawable.small_potion
        2 -> Res.drawable.potion
        3 -> Res.drawable.big_potion
        4 -> Res.drawable.huge_potion
        else -> Res.drawable.small_potion
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) colorScheme.primary.copy(alpha = 0.15f) else colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) colorScheme.primary else colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = potionColor
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name ?: "Без названия",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface
                )
                Text(
                    text = "${item.rarity.displayName} • ${item.formula?.ifBlank { "Нет формулы" } ?: "Нет формулы"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) colorScheme.primary else colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
