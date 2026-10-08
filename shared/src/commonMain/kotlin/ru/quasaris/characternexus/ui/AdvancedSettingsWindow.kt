package ru.quasaris.characternexus.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.quasaris.characternexus.backend.SettingsViewModel
import ru.quasaris.characternexus.model.AdvantageLogic
import ru.quasaris.characternexus.model.DiceRollPosition
import ru.quasaris.characternexus.model.SlotAlignment
import ru.quasaris.characternexus.model.SlotFillDirection
import ru.quasaris.characternexus.ui.BackHandler

private const val SHOW_DEBUG_SETTINGS = true

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsWindow(
    settingsViewModel: SettingsViewModel,
    onDismiss: () -> Unit,
    onCleanupDuplicateFolders: () -> Unit = {},
    onFullscreenDialogOpenChange: (Boolean) -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val debugInfoEnabled by settingsViewModel.debugInfoEnabled.collectAsState()
    var showDebugLogs by remember { mutableStateOf(false) }

    LaunchedEffect(showDebugLogs) {
        onFullscreenDialogOpenChange(showDebugLogs)
    }

    BackHandler(onBack = onDismiss)

    AnimatedVisibility(
        visible = showDebugLogs,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            DebugLogScreen(
                onDismiss = { showDebugLogs = false },
                settingsViewModel = settingsViewModel
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Продвинутые настройки", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colorScheme.surface,
                        titleContentColor = colorScheme.onSurface
                    )
                )
            },
            containerColor = colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Настройки интерфейса броска
                DiceRollSettingsSection(
                    settingsViewModel = settingsViewModel
                )

                HorizontalDivider(color = colorScheme.outlineVariant)

                // 2. Хаптика
                HapticSettingsSection(
                    settingsViewModel = settingsViewModel
                )

                HorizontalDivider(color = colorScheme.outlineVariant)

                // 3. Выравнивание ячеек
                SlotAlignmentSettingsSection(
                    settingsViewModel = settingsViewModel
                )

                // 4. Отладка
                if (SHOW_DEBUG_SETTINGS) {
                    HorizontalDivider(color = colorScheme.outlineVariant)

                    Text(
                        text = "Отладка",
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Включить информацию для отладки",
                            fontSize = 16.sp,
                            color = colorScheme.onSurface
                        )
                        Switch(
                            checked = debugInfoEnabled,
                            onCheckedChange = { settingsViewModel.updateDebugInfoEnabled(it) }
                        )
                    }

                    if (debugInfoEnabled) {
                        Button(
                            onClick = {
                                ru.quasaris.characternexus.util.Logger.i("AdvancedSettingsWindow", "View Logs button clicked")
                                showDebugLogs = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.secondaryContainer,
                                contentColor = colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.BugReport, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Посмотреть логи приложения", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                onCleanupDuplicateFolders()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.secondaryContainer,
                                contentColor = colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Очистить дубликаты папок", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiceRollSettingsSection(
    settingsViewModel: SettingsViewModel
) {
    val colorScheme = MaterialTheme.colorScheme
    val rollPassThrough by settingsViewModel.rollPassThrough.collectAsState()
    val rollPosition by settingsViewModel.rollPosition.collectAsState()
    val diceFabEnabled by settingsViewModel.diceFabEnabled.collectAsState()
    val renderDiceInOrder by settingsViewModel.renderDiceInOrder.collectAsState()
    val collapseActionsOnEdit by settingsViewModel.collapseActionsOnEdit.collectAsState()
    val collapseSpellsOnEdit by settingsViewModel.collapseSpellsOnEdit.collectAsState()
    val collapseDynamicFieldsOnEdit by settingsViewModel.collapseDynamicFieldsOnEdit.collectAsState()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Интерфейс броска",
            style = MaterialTheme.typography.titleMedium,
            color = colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Сворачивать атаки при редактировании",
                    fontSize = 16.sp,
                    color = colorScheme.onSurface
                )
                Text(
                    text = "Автоматически сворачивает карточки атак в режиме сортировки",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = collapseActionsOnEdit,
                onCheckedChange = { settingsViewModel.updateCollapseActionsOnEdit(it) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Сворачивать заклинания при редактировании",
                    fontSize = 16.sp,
                    color = colorScheme.onSurface
                )
                Text(
                    text = "Автоматически сворачивает карточки заклинаний в режиме сортировки",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = collapseSpellsOnEdit,
                onCheckedChange = { settingsViewModel.updateCollapseSpellsOnEdit(it) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Сворачивать поля при редактировании",
                    fontSize = 16.sp,
                    color = colorScheme.onSurface
                )
                Text(
                    text = "Автоматически сворачивает динамические поля в режиме сортировки",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = collapseDynamicFieldsOnEdit,
                onCheckedChange = { settingsViewModel.updateCollapseDynamicFieldsOnEdit(it) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Кнопка броска (FAB)",
                    fontSize = 16.sp,
                    color = colorScheme.onSurface
                )
                Text(
                    text = "Позволяет быстро бросать кубы из любого места",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = diceFabEnabled,
                onCheckedChange = { settingsViewModel.updateDiceFabEnabled(it) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Отображать кубы по порядку",
                    fontSize = 16.sp,
                    color = colorScheme.onSurface
                )
                Text(
                    text = "Рендерит кубы в порядке их написания в формуле, а не по размеру",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = renderDiceInOrder,
                onCheckedChange = { settingsViewModel.updateRenderDiceInOrder(it) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Сквозное нажатие",
                    fontSize = 16.sp,
                    color = colorScheme.onSurface
                )
                Text(
                    text = "Позволяет нажимать на элементы под интерфейсом броска",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = rollPassThrough,
                onCheckedChange = { settingsViewModel.updateRollPassThrough(it) }
            )
        }

        Text(
            text = "Положение интерфейса",
            fontSize = 16.sp,
            color = colorScheme.onSurface
        )

        val cornerRadius = 16.dp
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy((-9).dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = rollPosition == DiceRollPosition.TOP_LEFT,
                    onClick = { settingsViewModel.updateRollPosition(DiceRollPosition.TOP_LEFT) },
                    shape = RoundedCornerShape(topStart = cornerRadius, topEnd = 0.dp, bottomEnd = 0.dp, bottomStart = 0.dp)
                ) { Text("Слева-вверху", fontSize = 12.sp) }

                SegmentedButton(
                    selected = rollPosition == DiceRollPosition.TOP_RIGHT,
                    onClick = { settingsViewModel.updateRollPosition(DiceRollPosition.TOP_RIGHT) },
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = cornerRadius, bottomEnd = 0.dp, bottomStart = 0.dp)
                ) { Text("Справа-вверху", fontSize = 12.sp) }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = rollPosition == DiceRollPosition.BOTTOM_LEFT,
                    onClick = { settingsViewModel.updateRollPosition(DiceRollPosition.BOTTOM_LEFT) },
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomEnd = 0.dp, bottomStart = cornerRadius)
                ) { Text("Слева-внизу", fontSize = 12.sp) }

                SegmentedButton(
                    selected = rollPosition == DiceRollPosition.BOTTOM_RIGHT,
                    onClick = { settingsViewModel.updateRollPosition(DiceRollPosition.BOTTOM_RIGHT) },
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomEnd = cornerRadius, bottomStart = 0.dp)
                ) { Text("Справа-внизу", fontSize = 12.sp) }
            }
        }

        Text(
            text = "Положение кнопки закрытия",
            fontSize = 16.sp,
            color = colorScheme.onSurface
        )

        val closeButtonPosition by settingsViewModel.rollCloseButtonPosition.collectAsState()
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy((-9).dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = closeButtonPosition == DiceRollPosition.TOP_LEFT,
                    onClick = { settingsViewModel.updateRollCloseButtonPosition(DiceRollPosition.TOP_LEFT) },
                    shape = RoundedCornerShape(topStart = cornerRadius, topEnd = 0.dp, bottomEnd = 0.dp, bottomStart = 0.dp)
                ) { Text("Слева-вверху", fontSize = 12.sp) }

                SegmentedButton(
                    selected = closeButtonPosition == DiceRollPosition.TOP_RIGHT,
                    onClick = { settingsViewModel.updateRollCloseButtonPosition(DiceRollPosition.TOP_RIGHT) },
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = cornerRadius, bottomEnd = 0.dp, bottomStart = 0.dp)
                ) { Text("Справа-вверху", fontSize = 12.sp) }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = closeButtonPosition == DiceRollPosition.BOTTOM_LEFT,
                    onClick = { settingsViewModel.updateRollCloseButtonPosition(DiceRollPosition.BOTTOM_LEFT) },
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomEnd = 0.dp, bottomStart = cornerRadius)
                ) { Text("Слева-внизу", fontSize = 12.sp) }

                SegmentedButton(
                    selected = closeButtonPosition == DiceRollPosition.BOTTOM_RIGHT,
                    onClick = { settingsViewModel.updateRollCloseButtonPosition(DiceRollPosition.BOTTOM_RIGHT) },
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomEnd = cornerRadius, bottomStart = 0.dp)
                ) { Text("Справа-внизу", fontSize = 12.sp) }
            }
        }

        HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)

        val advantageLogic by settingsViewModel.advantageLogic.collectAsState()
        Text(
            text = "Логика преимущества",
            fontSize = 16.sp,
            color = colorScheme.onSurface
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy((-9).dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = advantageLogic == AdvantageLogic.TOTAL,
                    onClick = { settingsViewModel.updateAdvantageLogic(AdvantageLogic.TOTAL) },
                    shape = RoundedCornerShape(topStart = cornerRadius, topEnd = 0.dp, bottomEnd = 0.dp, bottomStart = 0.dp)
                ) { Text("Общее", fontSize = 12.sp) }

                SegmentedButton(
                    selected = advantageLogic == AdvantageLogic.INDIVIDUAL,
                    onClick = { settingsViewModel.updateAdvantageLogic(AdvantageLogic.INDIVIDUAL) },
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = cornerRadius, bottomEnd = 0.dp, bottomStart = 0.dp)
                ) { Text("Покубово", fontSize = 12.sp) }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = advantageLogic == AdvantageLogic.SOURCE,
                    onClick = { settingsViewModel.updateAdvantageLogic(AdvantageLogic.SOURCE) },
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomEnd = 0.dp, bottomStart = cornerRadius)
                ) { Text("Источник", fontSize = 12.sp) }

                SegmentedButton(
                    selected = advantageLogic == AdvantageLogic.POOL,
                    onClick = { settingsViewModel.updateAdvantageLogic(AdvantageLogic.POOL) },
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomEnd = cornerRadius, bottomStart = 0.dp)
                ) { Text("Пулл", fontSize = 12.sp) }
            }
        }
        val description = when(advantageLogic) {
            AdvantageLogic.TOTAL -> "Сравнение двух полных сумм всех кубов и бонусов"
            AdvantageLogic.INDIVIDUAL -> "Выбор лучшего значения для каждого отдельного кубика"
            AdvantageLogic.SOURCE -> "Независимый выбор лучшей суммы для каждой части формулы"
            AdvantageLogic.POOL -> "Выбор N лучших кубиков из 2N брошенных для каждой группы"
        }
        Text(
            text = description,
            fontSize = 12.sp,
            color = colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun HapticSettingsSection(settingsViewModel: SettingsViewModel) {
    val veryResponsive by settingsViewModel.veryResponsiveHaptics.collectAsState()
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Хаптика",
            style = MaterialTheme.typography.titleMedium,
            color = colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Очень отзывчивая хаптика",
                    fontSize = 16.sp,
                    color = colorScheme.onSurface
                )
                Text(
                    text = "Добавляет тактильный отклик на каждое нажатие и свайп",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = veryResponsive,
                onCheckedChange = { settingsViewModel.updateVeryResponsiveHaptics(it) }
            )
        }
    }
}

@Composable
fun SlotAlignmentSettingsSection(
    settingsViewModel: SettingsViewModel
) {
    val colorScheme = MaterialTheme.colorScheme
    var isExpanded by remember { mutableStateOf(false) }

    val longRestAlignment by settingsViewModel.longRestAlignment.collectAsState()
    val longRestFillDirection by settingsViewModel.longRestFillDirection.collectAsState()
    val shortRestAlignment by settingsViewModel.shortRestAlignment.collectAsState()
    val shortRestFillDirection by settingsViewModel.shortRestFillDirection.collectAsState()
    val dawnRestAlignment by settingsViewModel.dawnRestAlignment.collectAsState()
    val dawnRestFillDirection by settingsViewModel.dawnRestFillDirection.collectAsState()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Настройки выравнивания ячеек",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.primary
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = colorScheme.primary
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Long Rest Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ячейки продолжительного отдыха", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    Text("Выравнивание", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = longRestAlignment == SlotAlignment.LEFT,
                            onClick = { settingsViewModel.updateLongRestAlignment(SlotAlignment.LEFT) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                        ) { Text("Слева") }
                        SegmentedButton(
                            selected = longRestAlignment == SlotAlignment.CENTER,
                            onClick = { settingsViewModel.updateLongRestAlignment(SlotAlignment.CENTER) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                        ) { Text("Центр") }
                        SegmentedButton(
                            selected = longRestAlignment == SlotAlignment.RIGHT,
                            onClick = { settingsViewModel.updateLongRestAlignment(SlotAlignment.RIGHT) },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                        ) { Text("Справа") }
                    }

                    Text("Заполнение", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = longRestFillDirection == SlotFillDirection.LTR,
                            onClick = { settingsViewModel.updateLongRestFillDirection(SlotFillDirection.LTR) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                        ) { Text("Слева") }
                        SegmentedButton(
                            selected = longRestFillDirection == SlotFillDirection.CENTER,
                            onClick = { settingsViewModel.updateLongRestFillDirection(SlotFillDirection.CENTER) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                        ) { Text("Центр") }
                        SegmentedButton(
                            selected = longRestFillDirection == SlotFillDirection.RTL,
                            onClick = { settingsViewModel.updateLongRestFillDirection(SlotFillDirection.RTL) },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                        ) { Text("Справа") }
                    }
                }

                HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)

                // Short Rest Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ячейки короткого отдыха / Договора", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    Text("Выравнивание", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = shortRestAlignment == SlotAlignment.LEFT,
                            onClick = { settingsViewModel.updateShortRestAlignment(SlotAlignment.LEFT) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                        ) { Text("Слева") }
                        SegmentedButton(
                            selected = shortRestAlignment == SlotAlignment.CENTER,
                            onClick = { settingsViewModel.updateShortRestAlignment(SlotAlignment.CENTER) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                        ) { Text("Центр") }
                        SegmentedButton(
                            selected = shortRestAlignment == SlotAlignment.RIGHT,
                            onClick = { settingsViewModel.updateShortRestAlignment(SlotAlignment.RIGHT) },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                        ) { Text("Справа") }
                    }

                    Text("Заполнение", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = shortRestFillDirection == SlotFillDirection.LTR,
                            onClick = { settingsViewModel.updateShortRestFillDirection(SlotFillDirection.LTR) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                        ) { Text("Слева") }
                        SegmentedButton(
                            selected = shortRestFillDirection == SlotFillDirection.CENTER,
                            onClick = { settingsViewModel.updateShortRestFillDirection(SlotFillDirection.CENTER) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                        ) { Text("Центр") }
                        SegmentedButton(
                            selected = shortRestFillDirection == SlotFillDirection.RTL,
                            onClick = { settingsViewModel.updateShortRestFillDirection(SlotFillDirection.RTL) },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                        ) { Text("Справа") }
                    }
                }

                HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)

                // Dawn Rest Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ячейки Рассвета", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    Text("Выравнивание", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = dawnRestAlignment == SlotAlignment.LEFT,
                            onClick = { settingsViewModel.updateDawnRestAlignment(SlotAlignment.LEFT) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                        ) { Text("Слева") }
                        SegmentedButton(
                            selected = dawnRestAlignment == SlotAlignment.CENTER,
                            onClick = { settingsViewModel.updateDawnRestAlignment(SlotAlignment.CENTER) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                        ) { Text("Центр") }
                        SegmentedButton(
                            selected = dawnRestAlignment == SlotAlignment.RIGHT,
                            onClick = { settingsViewModel.updateDawnRestAlignment(SlotAlignment.RIGHT) },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                        ) { Text("Справа") }
                    }

                    Text("Заполнение", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = dawnRestFillDirection == SlotFillDirection.LTR,
                            onClick = { settingsViewModel.updateDawnRestFillDirection(SlotFillDirection.LTR) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                        ) { Text("Слева") }
                        SegmentedButton(
                            selected = dawnRestFillDirection == SlotFillDirection.CENTER,
                            onClick = { settingsViewModel.updateDawnRestFillDirection(SlotFillDirection.CENTER) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                        ) { Text("Центр") }
                        SegmentedButton(
                            selected = dawnRestFillDirection == SlotFillDirection.RTL,
                            onClick = { settingsViewModel.updateDawnRestFillDirection(SlotFillDirection.RTL) },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                        ) { Text("Справа") }
                    }
                }
            }
        }
    }
}
