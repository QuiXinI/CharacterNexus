package ru.quasaris.characternexus.HeaderCode.Fullscreen

import ru.quasaris.characternexus.model.*
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.quasaris.characternexus.ui.DialogDimStyle
import ru.quasaris.characternexus.ui.BackHandler
import ru.quasaris.characternexus.*
import ru.quasaris.characternexus.tabs.attacks.SectionHeader
import ru.quasaris.characternexus.tabs.attacks.AttackBonusIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitiativeDialog(
    activeEntry: FormulaEntry?,
    allEntries: List<FormulaEntry>,
    onAllEntriesChange: (List<FormulaEntry>) -> Unit,
    onActiveIdChange: (String?) -> Unit,
    statsMap: Map<String, String>,
    forceBlurEnabled: Boolean,
    onDismiss: () -> Unit,
    onSubDialogOpenChange: (Boolean) -> Unit = {}
) {
    var editingEntry by remember { mutableStateOf<FormulaEntry?>(null) }

    val isSubDialogOpen = editingEntry != null
    LaunchedEffect(isSubDialogOpen) {
        onSubDialogOpenChange(isSubDialogOpen)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        DialogDimStyle(0f)
        val colorScheme = MaterialTheme.colorScheme
        val isOled = colorScheme.background == Color.Black

    BackHandler(onBack = onDismiss)

    Scaffold(
            modifier = Modifier
                .blur(if (isSubDialogOpen && forceBlurEnabled && !isOled) 24.dp else 0.dp)
                .run {
                    if (isSubDialogOpen && forceBlurEnabled && !isOled) {
                        this.drawWithContent {
                            drawContent()
                            drawRect(colorScheme.surface.copy(alpha = 0.1f))
                        }
                    } else this
                },
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Инициатива", fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = if (forceBlurEnabled && !isOled) Color.Transparent.copy(alpha = 0.1f) else colorScheme.surface
                    )
                )
            },
            containerColor = if (forceBlurEnabled && !isOled) Color.Transparent.copy(alpha = 0.1f) else colorScheme.background
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
            forceBlurEnabled = forceBlurEnabled
        )
    }
}
