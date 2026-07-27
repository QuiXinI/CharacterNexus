package ru.quasaris.characters.master.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ru.quasaris.characters.master.DynamicNoteState
import ru.quasaris.characters.master.Wallet
import ru.quasaris.characters.master.backend.Currency
import ru.quasaris.characters.master.backend.SettingsViewModel
import ru.quasaris.characters.master.ui.CurrencyDisplayRow
import ru.quasaris.characters.master.ui.CurrencyEditDialog
import dev.chrisbanes.haze.HazeState

@Composable
fun InventoryTab(
    inventory: List<DynamicNoteState>,
    onInventoryChange: (List<DynamicNoteState>) -> Unit,
    wallet: Wallet = Wallet(),
    onWalletChange: (Wallet) -> Unit = {},
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    blurPopups: Boolean = false,
    isEditMode: Boolean = false,
    settingsViewModel: SettingsViewModel? = null,
    statsMap: Map<String, String> = emptyMap()
) {
    var editingCurrency by remember { mutableStateOf<Currency?>(null) }

    Column {
        CurrencyDisplayRow(
            wallet = wallet,
            onCurrencyClick = { editingCurrency = it }
        )

        DynamicFieldsTab(
            fields = inventory,
            onFieldsChange = onInventoryChange,
            hazeState = hazeState,
            forceBlurEnabled = forceBlurEnabled,
            blurPopups = blurPopups,
            isEditMode = isEditMode,
            addButtonText = "ДОБАВИТЬ ОСОБОЕ ПОЛЕ",
            emptyListText = "Инвентарь пуст",
            titlePlaceholder = "Название раздела",
            contentPlaceholder = "Содержимое раздела...",
            settingsViewModel = settingsViewModel,
            statsMap = statsMap
        )
    }

    editingCurrency?.let { currency ->
        CurrencyEditDialog(
            wallet = wallet,
            initialCurrency = currency,
            onWalletChange = onWalletChange,
            onDismiss = { editingCurrency = null },
            hazeState = hazeState,
            forceBlurEnabled = forceBlurEnabled
        )
    }
}
