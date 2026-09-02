package ru.quasaris.characternexus.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.backend.Currency
import ru.quasaris.characternexus.backend.SettingsViewModel
import ru.quasaris.characternexus.ui.CurrencyDisplayRow
import ru.quasaris.characternexus.ui.CurrencyEditDialog
import dev.chrisbanes.haze.HazeState

@Composable
fun InventoryTab(
    inventory: List<DynamicNoteState>,
    onInventoryChange: (List<DynamicNoteState>) -> Unit,
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
