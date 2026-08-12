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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.HazeInputScale
import ru.quasaris.characters.master.ArmorClassEntry
import ru.quasaris.characters.master.FormulaEntry
import ru.quasaris.characters.master.ShieldEntry
import ru.quasaris.characters.master.tabs.attacks.SectionHeader
import ru.quasaris.characters.master.tabs.attacks.AttackBonusIndicator

@OptIn(ExperimentalMaterial3Api::class, dev.chrisbanes.haze.ExperimentalHazeApi::class)
@Composable
fun ArmorClassDialog(
    activeEntry: FormulaEntry?,
    allEntries: List<FormulaEntry>,
    onAllEntriesChange: (List<FormulaEntry>) -> Unit,
    onActiveIdChange: (String?) -> Unit,
    statsMap: Map<String, String>,
    hazeState: HazeState?,
    forceBlurEnabled: Boolean,
    onDismiss: () -> Unit,
    onSubDialogOpenChange: (Boolean) -> Unit = {},
    isShieldActive: Boolean,
    onShieldActiveChange: (Boolean) -> Unit,
    activeShield: ShieldEntry?,
    allShields: List<ShieldEntry>,
    onShieldChange: (ShieldEntry) -> Unit,
    onAllShieldsChange: (List<ShieldEntry>) -> Unit,
    onActiveShieldIdChange: (String?) -> Unit
) {
    var editingEntry by remember { mutableStateOf<FormulaEntry?>(null) }
    var editingShield by remember { mutableStateOf<ShieldEntry?>(null) }

    val isSubDialogOpen = editingEntry != null || editingShield != null
    LaunchedEffect(isSubDialogOpen) {
        onSubDialogOpenChange(isSubDialogOpen)
    }

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
            modifier = Modifier
                .blur(if (isSubDialogOpen && forceBlurEnabled) 24.dp else 0.dp)
                .run {
                    if (isSubDialogOpen && forceBlurEnabled && !isOled) {
                        this.drawWithContent {
                            drawContent()
                            drawRect(colorScheme.surface.copy(alpha = 0.2f))
                        }
                    } else this
                }
                .run {
                    if (isSubDialogOpen && forceBlurEnabled && hazeState != null && !isOled) {
                        this.hazeEffect(state = hazeState) {
                            style = HazeStyle(
                                blurRadius = 24.dp,
                                tints = listOf(HazeTint(colorScheme.surface.copy(alpha = 0.4f)))
                            )
                            inputScale = HazeInputScale.Fixed(0.6f)
                        }
                    } else this
                },
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Класс Доспеха", fontWeight = FontWeight.Black) },
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
                    val totalCalc = remember(activeEntry, activeShield, isShieldActive, statsMap) {
                        val baseVal = calculateEntryTotal(activeEntry, statsMap, "AC")
                        if (isShieldActive && activeShield != null) {
                            val sVal = calculateEntryTotal(activeShield, statsMap, "SHIELD")
                            Pair(baseVal.first + sVal.first, baseVal.second + sVal.second)
                        } else baseVal
                    }

                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        AttackBonusIndicator(
                            bonus = totalCalc.first,
                            dice = totalCalc.second,
                            size = 140.dp,
                            fontSize = 54.sp,
                            showLabel = false,
                            showPlus = false,
                            diceSize = 24.dp
                        )
                    }

                    // Variants Section
                    SectionHeader("Варианты")
                    allEntries.forEach { entry ->
                        StatVariantItem(
                            entry = entry,
                            isActive = entry.id == activeEntry?.id,
                            statsMap = statsMap,
                            statType = "AC",
                            onClick = { onActiveIdChange(entry.id) },
                            onLongClick = { editingEntry = entry }
                        )
                    }

                    Button(
                        onClick = {
                            onAllEntriesChange(allEntries + ArmorClassEntry())
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Добавить вариант")
                    }

                    // Shield Section
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SectionHeader("Щит")
                        Switch(
                            checked = isShieldActive,
                            onCheckedChange = onShieldActiveChange,
                            modifier = Modifier.scale(0.8f)
                        )
                    }

                    allShields.forEach { shield ->
                        StatVariantItem(
                            entry = shield,
                            isActive = shield.id == activeShield?.id,
                            statsMap = statsMap,
                            statType = "SHIELD",
                            onClick = { onActiveShieldIdChange(shield.id) },
                            onLongClick = { editingShield = shield }
                        )
                    }

                    Button(
                        onClick = { onAllShieldsChange(allShields + ShieldEntry()) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Добавить щит")
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
            title = "Настройка: ${entry.name.ifBlank { "Класс Доспеха" }}",
            entry = entry,
            statsMap = statsMap,
            statType = "AC",
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

    editingShield?.let { shield ->
        EditVariantDialog(
            title = "Настройка щита: ${shield.name.ifBlank { "Без названия" }}",
            entry = shield,
            statsMap = statsMap,
            statType = "SHIELD",
            onSave = { updated ->
                val newList = allShields.toMutableList()
                val idx = newList.indexOfFirst { it.id == updated.id }
                if (idx != -1) {
                    newList[idx] = updated as ShieldEntry
                    onAllShieldsChange(newList)
                }
                editingShield = null
            },
            onDelete = {
                val newList = allShields.toMutableList()
                newList.removeAll { it.id == shield.id }
                if (shield.id == activeShield?.id) onActiveShieldIdChange(null)
                onAllShieldsChange(newList)
                editingShield = null
            },
            onDismiss = { editingShield = null },
            hazeState = hazeState,
            forceBlurEnabled = forceBlurEnabled
        )
    }
}
