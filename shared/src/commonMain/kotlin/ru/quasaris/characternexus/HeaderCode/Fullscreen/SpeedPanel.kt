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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.chrisbanes.haze.*
import ru.quasaris.characternexus.ui.DialogDimStyle
import ru.quasaris.characternexus.ui.BackHandler
import ru.quasaris.characternexus.tabs.attacks.SectionHeader
import ru.quasaris.characternexus.tabs.attacks.AttackBonusIndicator
import ru.quasaris.characternexus.ui.theme.rememberEffectiveBlurRadius

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedDialog(
    activeEntry: FormulaEntry?,
    allEntries: List<FormulaEntry>,
    onAllEntriesChange: (List<FormulaEntry>) -> Unit,
    onActiveIdChange: (String?) -> Unit,
    statsMap: Map<String, String>,
    forceBlurEnabled: Boolean,
    onDismiss: () -> Unit,
    onSubDialogOpenChange: (Boolean) -> Unit = {},
    isDesktop: Boolean = false,
    hazeState: HazeState? = null,
    popupHazeState: HazeState? = null,
    settingsViewModel: ru.quasaris.characternexus.backend.SettingsViewModel? = null
) {
    var editingEntry by remember { mutableStateOf<FormulaEntry?>(null) }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val blurRadius = rememberEffectiveBlurRadius(settingsViewModel)

    val handleDismiss = {
        focusManager.clearFocus()
        onDismiss()
    }

    val isSubDialogOpen = editingEntry != null
    LaunchedEffect(isSubDialogOpen) {
        onSubDialogOpenChange(isSubDialogOpen)
    }

    if (isDesktop) {
        SpeedDialogContent(
            onDismiss = handleDismiss,
            isSubDialogOpen = isSubDialogOpen,
            forceBlurEnabled = forceBlurEnabled,
            activeEntry = activeEntry,
            allEntries = allEntries,
            onAllEntriesChange = onAllEntriesChange,
            onActiveIdChange = onActiveIdChange,
            statsMap = statsMap,
            editingEntry = editingEntry,
            onEditingEntryChange = { editingEntry = it },
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
            SpeedDialogContent(
                onDismiss = handleDismiss,
                isSubDialogOpen = isSubDialogOpen,
                forceBlurEnabled = forceBlurEnabled,
                activeEntry = activeEntry,
                allEntries = allEntries,
                onAllEntriesChange = onAllEntriesChange,
                onActiveIdChange = onActiveIdChange,
                statsMap = statsMap,
                editingEntry = editingEntry,
                onEditingEntryChange = { editingEntry = it },
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
fun SpeedDialogContent(
    onDismiss: () -> Unit,
    isSubDialogOpen: Boolean,
    forceBlurEnabled: Boolean,
    activeEntry: FormulaEntry?,
    allEntries: List<FormulaEntry>,
    onAllEntriesChange: (List<FormulaEntry>) -> Unit,
    onActiveIdChange: (String?) -> Unit,
    statsMap: Map<String, String>,
    editingEntry: FormulaEntry?,
    onEditingEntryChange: (FormulaEntry?) -> Unit,
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
                        title = { Text("Скорость", fontWeight = FontWeight.Black) },
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
                containerColor = if (forceBlurEnabled && !isOled && hazeState != null && !isSubDialogOpen) Color.Transparent.copy(alpha = 0.0f) else colorScheme.background,
                modifier = Modifier
                    .fillMaxSize()
                    .run {
                        if (forceBlurEnabled && hazeState != null && !isOled) {
                            this.hazeEffect(state = hazeState) {
                                style = HazeStyle(
                                    blurRadius = blurRadius,
                                    tints = listOf(HazeTint(Color.Black.copy(alpha = 0.2f)))
                                )
                            }
                        } else this
                    }
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Indicator
                        val calc = remember(activeEntry, statsMap) { calculateEntryTotal(activeEntry, statsMap, "SPEED") }
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            AttackBonusIndicator(
                                bonus = calc.first,
                                dice = calc.second,
                                size = 140.dp,
                                fontSize = 54.sp,
                                showLabel = false,
                                showPlus = false,
                                diceSize = 24.dp
                            )
                        }

                        SectionHeader("Варианты")
                        allEntries.forEach { entry ->
                            StatVariantItem(
                                entry = entry,
                                isActive = entry.id == activeEntry?.id,
                                statsMap = statsMap,
                                statType = "SPEED",
                                onClick = { onActiveIdChange(entry.id) },
                                onLongClick = { onEditingEntryChange(entry) }
                            )
                        }

                        Button(
                            onClick = { onAllEntriesChange(allEntries + SpeedEntry()) },
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

        editingEntry?.let { entry ->
            EditVariantDialog(
                title = "Настройка: ${entry.name.ifBlank { "Скорость" }}",
                entry = entry,
                statsMap = statsMap,
                statType = "SPEED",
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
    }
}
