package ru.quasaris.characters.master.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    popupHazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    blurPopups: Boolean = false,
    isEditMode: Boolean = false,
    settingsViewModel: SettingsViewModel? = null,
    statsMap: Map<String, String> = emptyMap(),
    header: @Composable () -> Unit = {}
) {
    var editingCurrency by remember { mutableStateOf<Currency?>(null) }

    DynamicFieldsTab(
        fields = inventory,
        onFieldsChange = onInventoryChange,
        hazeState = hazeState,
        popupHazeState = popupHazeState,
        forceBlurEnabled = forceBlurEnabled,
        blurPopups = blurPopups,
        isEditMode = isEditMode,
        addButtonText = "ДОБАВИТЬ ОСОБОЕ ПОЛЕ",
        emptyListText = "Инвентарь пуст",
        titlePlaceholder = "Название раздела",
        contentPlaceholder = "Содержимое раздела...",
        settingsViewModel = settingsViewModel,
        statsMap = statsMap,
        header = {
            Column {
                header()
                Spacer(Modifier.height(12.dp))
                CurrencyDisplayRow(
                    wallet = wallet,
                    onCurrencyClick = { editingCurrency = it }
                )
            }
        }
    )

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
