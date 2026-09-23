package ru.quasaris.characternexus.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.backend.Currency
import ru.quasaris.characternexus.backend.SettingsViewModel
import ru.quasaris.characternexus.backend.MagicItemManager
import ru.quasaris.characternexus.ui.CurrencyDisplayRow
import ru.quasaris.characternexus.ui.CurrencyEditDialog
import dev.chrisbanes.haze.HazeState
import ru.quasaris.characternexus.tabs.potions.PotionSection

@Composable
fun InventoryTab(
    inventory: List<DynamicNoteState>,
    onInventoryChange: (List<DynamicNoteState>) -> Unit,
    potions: List<PotionState> = emptyList(),
    onPotionsChange: (List<PotionState>) -> Unit = {},
    wallet: Wallet = Wallet(),
    onWalletChange: (Wallet) -> Unit = {},
    hazeState: HazeState? = null,
    popupHazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    blurPopups: Boolean = false,
    isEditMode: Boolean = false,
    onToggleEditMode: () -> Unit = {},
    onToggleAllExpansion: () -> Unit = {},
    anyCollapsed: Boolean = false,
    settingsViewModel: SettingsViewModel? = null,
    statsMap: Map<String, String> = emptyMap(),
    onFullscreenDialogOpenChange: (Boolean) -> Unit = {},
    onFullscreenVisibilityChanged: (Boolean) -> Unit = {},
    onWalletDialogOpenChange: (Boolean) -> Unit = {},
    state: ru.quasaris.characternexus.ui.CharacterDetailState? = null,
    onRoll: (RollResult) -> Unit = {},
    magicItemManager: MagicItemManager? = null,
    isDesktop: Boolean = false,
    header: @Composable () -> Unit = {}
) {
    var editingCurrency by remember { mutableStateOf<Currency?>(null) }

    LaunchedEffect(editingCurrency) {
        onWalletDialogOpenChange(editingCurrency != null)
        state?.isWalletDialogOpen = editingCurrency != null
    }

    LaunchedEffect(state?.isWalletDialogOpen) {
        if (state?.isWalletDialogOpen == false) {
            editingCurrency = null
        }
    }

    DynamicFieldsTab(
        fields = inventory,
        onFieldsChange = onInventoryChange,
        hazeState = hazeState,
        popupHazeState = popupHazeState,
        forceBlurEnabled = forceBlurEnabled,
        blurPopups = blurPopups,
        isEditMode = isEditMode,
        onToggleEditMode = onToggleEditMode,
        onToggleAllExpansion = onToggleAllExpansion,
        anyCollapsed = anyCollapsed,
        onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
        onFullscreenVisibilityChanged = onFullscreenVisibilityChanged,
        addButtonText = "ДОБАВИТЬ ОСОБОЕ ПОЛЕ",
        emptyListText = "Инвентарь пуст",
        titlePlaceholder = "Название раздела",
        contentPlaceholder = "Содержимое раздела...",
        settingsViewModel = settingsViewModel,
        statsMap = statsMap,
        state = state,
        isContentVisible = { it.tag != "potions" },
        isAddButtonVisible = false,
        header = {
            Column {
                header()
                Spacer(Modifier.height(12.dp))
                CurrencyDisplayRow(
                    wallet = wallet,
                    onCurrencyClick = {
                        state?.selectedCurrency = it
                        editingCurrency = it
                    }
                )
            }
        },
        footer = {
            val hasPotionsSection = inventory.any { it.tag == "potions" }
            var footerWidth by remember { mutableStateOf(0.dp) }
            val density = LocalDensity.current

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .onSizeChanged { size ->
                        footerWidth = with(density) { size.width.toDp() }
                    }
            ) {
                val estimatedButtonWidth = if (hasPotionsSection) footerWidth else footerWidth * 0.6f
                val estimatedPotionWidth = footerWidth * 0.4f

                val addButtonText = when {
                    estimatedButtonWidth < 180.dp -> "ПОЛЕ"
                    estimatedButtonWidth < 270.dp -> "ОСОБОЕ ПОЛЕ"
                    else -> "ДОБАВИТЬ ОСОБОЕ ПОЛЕ"
                }

                val potionButtonText = when {
                    estimatedPotionWidth < 140.dp -> "ЗЕЛЬЯ"
                    else -> "БЛОК ЗЕЛИЙ"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Add Special Field Button
                    Button(
                        onClick = {
                            val newFields = inventory + DynamicNoteState()
                            onInventoryChange(newFields)
                        },
                        modifier = if (hasPotionsSection) Modifier.fillMaxWidth() else Modifier.weight(0.6f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(addButtonText, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    // Add Potions Section Button
                    if (!hasPotionsSection) {
                        Button(
                            onClick = {
                                val newFields = inventory + DynamicNoteState(title = "Зелья", tag = "potions")
                                onInventoryChange(newFields)
                            },
                            modifier = Modifier.weight(0.4f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(potionButtonText, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
        },
        extraContent = { field ->
            if (field.tag == "potions") {
                PotionSection(
                    potions = potions,
                    onPotionsChange = onPotionsChange,
                    statsMap = statsMap,
                    settingsViewModel = settingsViewModel,
                    hazeState = hazeState,
                    popupHazeState = popupHazeState,
                    onRoll = onRoll,
                    state = state,
                    isTabEditMode = isEditMode,
                    isExpanded = field.isExpanded,
                    onExpandedChange = { expanded ->
                        onInventoryChange(inventory.map { if (it.id == field.id) it.copy(isExpanded = expanded) else it })
                    }
                )
            }
        }
    )

    if (editingCurrency != null && state == null) {
        CurrencyEditDialog(
            wallet = wallet,
            initialCurrency = editingCurrency!!,
            onWalletChange = onWalletChange,
            onDismiss = { editingCurrency = null },
            hazeState = hazeState,
            forceBlurEnabled = forceBlurEnabled
        )
    }
}
