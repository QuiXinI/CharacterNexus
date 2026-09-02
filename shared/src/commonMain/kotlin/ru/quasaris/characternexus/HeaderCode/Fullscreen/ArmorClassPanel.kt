package ru.quasaris.characternexus.HeaderCode.Fullscreen

import ru.quasaris.characternexus.model.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.blur
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.chrisbanes.haze.*
import ru.quasaris.characternexus.ui.DialogDimStyle
import ru.quasaris.characternexus.ui.BackHandler
import ru.quasaris.characternexus.*
import ru.quasaris.characternexus.tabs.attacks.SectionHeader
import ru.quasaris.characternexus.tabs.attacks.AttackBonusIndicator
import ru.quasaris.characternexus.ui.theme.rememberEffectiveBlurRadius

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArmorClassDialog(
    activeEntry: FormulaEntry?,
    allEntries: List<FormulaEntry>,
    onAllEntriesChange: (List<FormulaEntry>) -> Unit,
    onActiveIdChange: (String?) -> Unit,
    statsMap: Map<String, String>,
    forceBlurEnabled: Boolean,
    onDismiss: () -> Unit,
    onSubDialogOpenChange: (Boolean) -> Unit = {},
    isShieldActive: Boolean,
    onShieldActiveChange: (Boolean) -> Unit,
    activeShield: ShieldEntry?,
    allShields: List<ShieldEntry>,
    onShieldChange: (ShieldEntry) -> Unit,
    onAllShieldsChange: (List<ShieldEntry>) -> Unit,
    onActiveShieldIdChange: (String?) -> Unit,
    isDesktop: Boolean = false,
    hazeState: HazeState? = null,
    popupHazeState: HazeState? = null,
    settingsViewModel: ru.quasaris.characternexus.backend.SettingsViewModel? = null
) {
    var editingEntry by remember { mutableStateOf<FormulaEntry?>(null) }
    var editingShield by remember { mutableStateOf<ShieldEntry?>(null) }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val blurRadius = rememberEffectiveBlurRadius(settingsViewModel)
    
    val handleDismiss = {
        focusManager.clearFocus()
        onDismiss()
    }

    val isSubDialogOpen = editingEntry != null || editingShield != null
    LaunchedEffect(isSubDialogOpen) {
        onSubDialogOpenChange(isSubDialogOpen)
    }

    if (isDesktop) {
        ArmorClassDialogContent(
            onDismiss = handleDismiss,
            isSubDialogOpen = isSubDialogOpen,
            forceBlurEnabled = forceBlurEnabled,
            activeEntry = activeEntry,
            allEntries = allEntries,
            onAllEntriesChange = onAllEntriesChange,
            onActiveIdChange = onActiveIdChange,
            statsMap = statsMap,
            isShieldActive = isShieldActive,
            onShieldActiveChange = onShieldActiveChange,
            activeShield = activeShield,
            allShields = allShields,
            onActiveShieldIdChange = onActiveShieldIdChange,
            onAllShieldsChange = onAllShieldsChange,
            editingEntry = editingEntry,
            onEditingEntryChange = { editingEntry = it },
            editingShield = editingShield,
            onEditingShieldChange = { editingShield = it },
            hazeState = popupHazeState ?: hazeState,
            blurRadius = blurRadius,
            settingsViewModel = settingsViewModel,
            isDesktop = isDesktop
        )
    } else {
        Dialog(
            onDismissRequest = handleDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            DialogDimStyle(0f)
            ArmorClassDialogContent(
                onDismiss = handleDismiss,
                isSubDialogOpen = isSubDialogOpen,
                forceBlurEnabled = forceBlurEnabled,
                activeEntry = activeEntry,
                allEntries = allEntries,
                onAllEntriesChange = onAllEntriesChange,
                onActiveIdChange = onActiveIdChange,
                statsMap = statsMap,
                isShieldActive = isShieldActive,
                onShieldActiveChange = onShieldActiveChange,
                activeShield = activeShield,
                allShields = allShields,
                onActiveShieldIdChange = onActiveShieldIdChange,
                onAllShieldsChange = onAllShieldsChange,
                editingEntry = editingEntry,
                onEditingEntryChange = { editingEntry = it },
                editingShield = editingShield,
                onEditingShieldChange = { editingShield = it },
                hazeState = popupHazeState ?: hazeState,
                blurRadius = blurRadius,
                settingsViewModel = settingsViewModel,
                isDesktop = isDesktop
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArmorClassDialogContent(
    onDismiss: () -> Unit,
    isSubDialogOpen: Boolean,
    forceBlurEnabled: Boolean,
    activeEntry: FormulaEntry?,
    allEntries: List<FormulaEntry>,
    onAllEntriesChange: (List<FormulaEntry>) -> Unit,
    onActiveIdChange: (String?) -> Unit,
    statsMap: Map<String, String>,
    isShieldActive: Boolean,
    onShieldActiveChange: (Boolean) -> Unit,
    activeShield: ShieldEntry?,
    allShields: List<ShieldEntry>,
    onActiveShieldIdChange: (String?) -> Unit,
    onAllShieldsChange: (List<ShieldEntry>) -> Unit,
    editingEntry: FormulaEntry?,
    onEditingEntryChange: (FormulaEntry?) -> Unit,
    editingShield: ShieldEntry?,
    onEditingShieldChange: (ShieldEntry?) -> Unit,
    hazeState: HazeState? = null,
    blurRadius: androidx.compose.ui.unit.Dp = 24.dp,
    settingsViewModel: ru.quasaris.characternexus.backend.SettingsViewModel? = null,
    isDesktop: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme
    val isOled = colorScheme.background == Color.Black
    val masterBlurEnabled by settingsViewModel?.masterBlurEnabled?.collectAsState() ?: remember { mutableStateOf(true) }

    BackHandler(onBack = onDismiss)

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .run {
                    if (isSubDialogOpen && masterBlurEnabled) {
                        this.blur(blurRadius)
                    } else this
                }
        ) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("Класс Доспеха", fontWeight = FontWeight.Black) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Закрыть")
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = if (forceBlurEnabled && !isOled && hazeState != null && !isSubDialogOpen) Color.Transparent.copy(alpha = 0.0f) else colorScheme.surface
                        )
                    )
                },
                containerColor = if (forceBlurEnabled && !isOled && hazeState != null && !isSubDialogOpen) Color.Transparent.copy(alpha = 0.0f) else colorScheme.background
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
                                onLongClick = { onEditingEntryChange(entry) }
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
                                onLongClick = { onEditingShieldChange(shield) }
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
                    onEditingEntryChange(null)
                },
                onDelete = {
                    val newList = allEntries.toMutableList()
                    newList.removeAll { it.id == entry.id }
                    if (entry.id == activeEntry?.id) onActiveIdChange(null)
                    onAllEntriesChange(newList)
                    onEditingEntryChange(null)
                },
                onDismiss = { onEditingEntryChange(null) },
                forceBlurEnabled = forceBlurEnabled,
                settingsViewModel = settingsViewModel,
                asOverlay = isDesktop
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
                    onEditingShieldChange(null)
                },
                onDelete = {
                    val newList = allShields.toMutableList()
                    newList.removeAll { it.id == shield.id }
                    if (shield.id == activeShield?.id) onActiveShieldIdChange(null)
                    onAllShieldsChange(newList)
                    onEditingShieldChange(null)
                },
                onDismiss = { onEditingShieldChange(null) },
                forceBlurEnabled = forceBlurEnabled,
                settingsViewModel = settingsViewModel,
                asOverlay = isDesktop
            )
        }
    }
}
