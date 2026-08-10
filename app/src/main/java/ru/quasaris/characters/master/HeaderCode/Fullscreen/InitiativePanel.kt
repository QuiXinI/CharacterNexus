package ru.quasaris.characters.master.HeaderCode.Fullscreen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.HazeInputScale
import ru.quasaris.characters.master.FormulaEntry
import ru.quasaris.characters.master.InitiativeEntry
import ru.quasaris.characters.master.tabs.attacks.SectionHeader
import ru.quasaris.characters.master.tabs.attacks.AttackBonusIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitiativeDialog(
    activeEntry: FormulaEntry?,
    allEntries: List<FormulaEntry>,
    onAllEntriesChange: (List<FormulaEntry>) -> Unit,
    onActiveIdChange: (String?) -> Unit,
    statsMap: Map<String, String>,
    hazeState: HazeState?,
    forceBlurEnabled: Boolean,
    onDismiss: () -> Unit
) {
    var editingEntry by remember { mutableStateOf<FormulaEntry?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.setDimAmount(0f)
        }
        val colorScheme = MaterialTheme.colorScheme
        val isOled = colorScheme.background == Color.Black

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Инициатива", fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = if (forceBlurEnabled && !isOled) Color.Transparent else colorScheme.surface
                    )
                )
            },
            containerColor = if (forceBlurEnabled && !isOled) Color.Transparent else colorScheme.background
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Total Indicator
                    val totalCalc = remember(activeEntry, statsMap) {
                        calculateEntryTotal(activeEntry, statsMap, "INIT")
                    }

                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        AttackBonusIndicator(
                            bonus = totalCalc.first,
                            dice = totalCalc.second,
                            size = 140.dp,
                            fontSize = 54.sp,
                            showLabel = false,
                            showPlus = true,
                            diceSize = 44.dp
                        )
                    }

                    // Variants Section
                    SectionHeader("Варианты")
                    allEntries.forEach { entry ->
                        StatVariantItem(
                            entry = entry,
                            isActive = entry.id == activeEntry?.id,
                            statsMap = statsMap,
                            statType = "INIT",
                            onClick = { onActiveIdChange(entry.id) },
                            onLongClick = { editingEntry = entry }
                        )
                    }

                    Button(
                        onClick = {
                            onAllEntriesChange(allEntries + InitiativeEntry())
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Добавить вариант")
                    }

                    Spacer(modifier = Modifier.height(100.dp))
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Готово", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Sub-Dialogs for Editing
    editingEntry?.let { entry ->
        EditVariantDialog(
            title = "Настройка: ${entry.name.ifBlank { "Инициатива" }}",
            entry = entry,
            statsMap = statsMap,
            statType = "INIT",
            onSave = { updated ->
                val newList = allEntries.toMutableList()
                val idx = newList.indexOfFirst { it.id == updated.id }
                if (idx != -1) {
                    newList[idx] = updated
                    onAllEntriesChange(newList)
                }
                editingEntry = null
            },
            onDelete = {
                val newList = allEntries.toMutableList()
                newList.removeAll { it.id == entry.id }
                if (entry.id == activeEntry?.id) onActiveIdChange(null)
                onAllEntriesChange(newList)
                editingEntry = null
            },
            onDismiss = { editingEntry = null },
            hazeState = hazeState,
            forceBlurEnabled = forceBlurEnabled
        )
    }
}
