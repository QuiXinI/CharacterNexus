package ru.quasaris.characters.master

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import ru.quasaris.characters.master.backend.AppThemeMode
import ru.quasaris.characters.master.backend.SettingsManager
import ru.quasaris.characters.master.backend.SettingsViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.FolderOpen
import ru.quasaris.characters.master.backend.ExportFormat
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher
import android.net.Uri
import ru.quasaris.characters.master.backend.ImageExporter
import ru.quasaris.characters.master.backend.DiceRollPosition
import kotlin.math.roundToInt

private const val SHOW_DEBUG_SETTINGS = true

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsWindow(
    onOpenDrawer: () -> Unit,
    onThemeModeChange: (AppThemeMode) -> Unit,
    settingsViewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    var themeMode by remember { mutableStateOf(settingsManager.themeMode) }
    val colorScheme = MaterialTheme.colorScheme

    val scaleFactor by settingsViewModel.scaleFactor.collectAsState()
    val debugInfoEnabled by settingsViewModel.debugInfoEnabled.collectAsState()

    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Сброс настроек") },
            text = { Text("Вы уверены, что хотите сбросить все настройки к заводским значениям? Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        settingsViewModel.resetToDefaults()
                        themeMode = AppThemeMode.M3
                        onThemeModeChange(AppThemeMode.M3)
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Сбросить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Меню")
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
            Text(
                text = "Интерфейс",
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Цвета приложения",
                    fontSize = 16.sp,
                    color = colorScheme.onSurface
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = themeMode == AppThemeMode.M3,
                        onClick = { 
                            themeMode = AppThemeMode.M3
                            settingsManager.themeMode = AppThemeMode.M3
                            onThemeModeChange(AppThemeMode.M3)
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                        modifier = Modifier.weight(1.4f)
                    ) { Text("Material You") }
                    SegmentedButton(
                        selected = themeMode == AppThemeMode.OFF,
                        onClick = { 
                            themeMode = AppThemeMode.OFF
                            settingsManager.themeMode = AppThemeMode.OFF
                            onThemeModeChange(AppThemeMode.OFF)
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                    ) { Text("BLACK") }
                    SegmentedButton(
                        selected = themeMode == AppThemeMode.CHARACTER,
                        onClick = { 
                            themeMode = AppThemeMode.CHARACTER
                            settingsManager.themeMode = AppThemeMode.CHARACTER
                            onThemeModeChange(AppThemeMode.CHARACTER)
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                        modifier = Modifier.weight(1.4f)
                    ) { Text("Персонаж") }
                }
            }

            HorizontalDivider(color = colorScheme.outlineVariant)

            ExportSettingsSection(settingsViewModel = settingsViewModel)

            HorizontalDivider(color = colorScheme.outlineVariant)

            RollHistorySettingsSection(
                settingsViewModel = settingsViewModel
            )

            HorizontalDivider(color = colorScheme.outlineVariant)

            ScaleSettingsSection(
                scaleFactor = scaleFactor,
                onScaleChange = { settingsViewModel.updateScaleFactor(it) }
            )

            HorizontalDivider(color = colorScheme.outlineVariant)

            BlurSettingsSection(
                settingsViewModel = settingsViewModel
            )

            HorizontalDivider(color = colorScheme.outlineVariant)

            DiceRollSettingsSection(
                settingsViewModel = settingsViewModel
            )

            HorizontalDivider(color = colorScheme.outlineVariant)

            DeletionWarningSettingsSection(
                settingsViewModel = settingsViewModel
            )

            HorizontalDivider(color = colorScheme.outlineVariant)

            FullscreenEditingSettingsSection(
                settingsViewModel = settingsViewModel
            )

            HorizontalDivider(color = colorScheme.outlineVariant)

            LssImportSettingsSection(
                settingsViewModel = settingsViewModel
            )

            HorizontalDivider(color = colorScheme.outlineVariant)

            Text(
                text = "Форматирование текста",
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            TopMarginSettingsSection(settingsViewModel = settingsViewModel)

            HorizontalDivider(color = colorScheme.outlineVariant)

            SlotAlignmentSettingsSection(settingsViewModel = settingsViewModel)

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
            }
            
            HorizontalDivider(color = colorScheme.outlineVariant)
            
            Text(
                text = "О приложении",
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Мастер Персонажей\nВерсия ${BuildConfig.VERSION_NAME}",
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            HorizontalDivider(color = colorScheme.outlineVariant)

            Button(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text(
                    text = "Сбросить настройки к заводским",
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSettingsSection(settingsViewModel: SettingsViewModel) {
    val colorScheme = MaterialTheme.colorScheme
    val exportFormat by settingsViewModel.exportFormat.collectAsState()
    val exportDirectoryUri by settingsViewModel.exportDirectoryUri.collectAsState()
    
    val directoryPicker = rememberDirectoryPickerLauncher(
        title = "Выберите папку для экспорта"
    ) { directory ->
        directory?.let {
            settingsViewModel.updateExportDirectoryUri(it.uri.toString())
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Экспорт аватарок",
            style = MaterialTheme.typography.titleMedium,
            color = colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Формат изображения", fontSize = 16.sp, color = colorScheme.onSurface)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = exportFormat == ExportFormat.WEBP,
                    onClick = { settingsViewModel.updateExportFormat(ExportFormat.WEBP) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                ) { Text("WebP") }
                SegmentedButton(
                    selected = exportFormat == ExportFormat.PNG,
                    onClick = { settingsViewModel.updateExportFormat(ExportFormat.PNG) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                ) { Text("PNG") }
                SegmentedButton(
                    selected = exportFormat == ExportFormat.JPG,
                    onClick = { settingsViewModel.updateExportFormat(ExportFormat.JPG) },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                ) { Text("JPG") }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Папка для экспорта", fontSize = 16.sp, color = colorScheme.onSurface)
                Text(
                    text = if (exportDirectoryUri != null) {
                        "Пользовательская папка"
                    } else {
                        "Downloads/Characters Master"
                    },
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { directoryPicker.launch() }) {
                Icon(Icons.Default.FolderOpen, contentDescription = "Выбрать папку", tint = colorScheme.primary)
            }
        }
        
        if (exportDirectoryUri != null) {
            TextButton(
                onClick = { settingsViewModel.updateExportDirectoryUri(null) },
                colors = ButtonDefaults.textButtonColors(contentColor = colorScheme.error),
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text("Сбросить к папке по умолчанию")
            }
        }
    }
}

@Composable
fun BlurSettingsSection(
    settingsViewModel: SettingsViewModel
) {
    val colorScheme = MaterialTheme.colorScheme
    val masterBlurEnabled by settingsViewModel.masterBlurEnabled.collectAsState()
    val blurRolls by settingsViewModel.blurRolls.collectAsState()
    val blurFullscreen by settingsViewModel.blurFullscreen.collectAsState()
    val blurPopups by settingsViewModel.blurPopups.collectAsState()
    val rollAlpha by settingsViewModel.rollInterfaceAlpha.collectAsState()
    val debugInfoEnabled by settingsViewModel.debugInfoEnabled.collectAsState()
    val performanceClass = settingsViewModel.performanceClass
    
    var showWarningDialog by remember { mutableStateOf(false) }
    var pendingSetting by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }

    val onToggle: (Boolean, (Boolean) -> Unit) -> Unit = { checked, updateFn ->
        if (checked && performanceClass < 33) {
            pendingSetting = updateFn
            showWarningDialog = true
        } else {
            updateFn(checked)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Эффекты размытия",
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Switch(
                checked = masterBlurEnabled,
                onCheckedChange = { settingsViewModel.updateMasterBlurEnabled(it) }
            )
        }

        if (SHOW_DEBUG_SETTINGS && debugInfoEnabled) {
            Text(
                text = "Класс мощности устройства: $performanceClass",
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(visible = masterBlurEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BlurSwitchRow(
                    label = "Интерфейс броска",
                    checked = blurRolls,
                    onCheckedChange = { onToggle(it) { settingsViewModel.updateBlurRolls(it) } }
                )

                BlurSwitchRow(
                    label = "Полноэкранные окна",
                    checked = blurFullscreen,
                    onCheckedChange = { onToggle(it) { settingsViewModel.updateBlurFullscreen(it) } }
                )

                BlurSwitchRow(
                    label = "Всплывающие окна",
                    checked = blurPopups,
                    onCheckedChange = { onToggle(it) { settingsViewModel.updateBlurPopups(it) } }
                )

                HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Прозрачность интерфейса броска",
                            fontSize = 16.sp,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "${((1f - rollAlpha) * 100).roundToInt()}%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                    }
                    Slider(
                        value = 1f - rollAlpha,
                        onValueChange = { settingsViewModel.updateRollInterfaceAlpha(1f - it) },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showWarningDialog) {
        WarningBlurDialog(
            onConfirm = {
                pendingSetting?.invoke(true)
                showWarningDialog = false
            },
            onDismiss = {
                showWarningDialog = false
            }
        )
    }
}

@Composable
fun DiceRollSettingsSection(
    settingsViewModel: SettingsViewModel
) {
    val colorScheme = MaterialTheme.colorScheme
    val rollPassThrough by settingsViewModel.rollPassThrough.collectAsState()
    val rollPosition by settingsViewModel.rollPosition.collectAsState()

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
            // TOP ROW
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

            // BOTTOM ROW
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
    }
}

@Composable
fun BlurSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = colorScheme.onSurface
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun DeletionWarningSettingsSection(
    settingsViewModel: SettingsViewModel
) {
    val colorScheme = MaterialTheme.colorScheme
    val deletionWarningEnabled by settingsViewModel.deletionWarningEnabled.collectAsState()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Предупреждение об удалении",
                    fontSize = 16.sp,
                    color = colorScheme.onSurface
                )
                Text(
                    text = "Показывать подтверждение при удалении элементов",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = deletionWarningEnabled,
                onCheckedChange = { settingsViewModel.updateDeletionWarningEnabled(it) }
            )
        }
    }
}

@Composable
fun FullscreenEditingSettingsSection(
    settingsViewModel: SettingsViewModel
) {
    val colorScheme = MaterialTheme.colorScheme
    val fullscreenEditingOnly by settingsViewModel.fullscreenEditingOnly.collectAsState()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Редактирование только в полноэкранном режиме",
                    fontSize = 16.sp,
                    color = colorScheme.onSurface
                )
                Text(
                    text = "Блокирует редактирование полей в обычном режиме просмотра (заметки, черты, инвентарь, заклинания)",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = fullscreenEditingOnly,
                onCheckedChange = { settingsViewModel.updateFullscreenEditingOnly(it) }
            )
        }
    }
}

@Composable
fun LssImportSettingsSection(
    settingsViewModel: SettingsViewModel
) {
    val colorScheme = MaterialTheme.colorScheme
    val autoDownloadLssAvatar by settingsViewModel.autoDownloadLssAvatar.collectAsState()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Авто-загрузка аватарок LSS",
                    fontSize = 16.sp,
                    color = colorScheme.onSurface
                )
                Text(
                    text = "Автоматически скачивать аватарки при импорте из LongStoryShort",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = autoDownloadLssAvatar,
                onCheckedChange = { settingsViewModel.updateAutoDownloadLssAvatar(it) }
            )
        }
    }
}

@Composable
fun WarningBlurDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .border(2.dp, Color.Red, RoundedCornerShape(28.dp)),
        containerColor = Color(0xFF121212),
        tonalElevation = 8.dp,
        title = {
            Text(
                text = "⚠️ ВНИМАНИЕ: ОПАСНО ДЛЯ УСТРОЙСТВА",
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            val annotatedString = buildAnnotatedString {
                append("Вы собираетесь принудительно включить эффекты размытия на неподдерживаемом устройстве. Это может привести к дикому троттлингу, лагам, критическому перегреву и даже выходу из строя железа при долгой партии. Вы рискуете ")
                withStyle(style = SpanStyle(color = Color.Red, fontWeight = FontWeight.Bold)) {
                    append("своим железом")
                }
                append(" по собственной воле! Команда ")
                withStyle(style = SpanStyle(color = Color(0xFF00E1FF), fontWeight = FontWeight.Black)) {
                    append("Quasaris")
                }
                append(" не несет вообще никакой ответственности за ваши расплавленные процессоры и вздувшиеся аккумуляторы. Продолжаем?")
            }
            Text(
                text = annotatedString,
                color = Color.White,
                fontSize = 16.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Да, я понимаю риски и беру ответственность на себя", color = Color.Red, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Не включать", color = Color.White)
            }
        },
        shape = RoundedCornerShape(28.dp),
    )
}

@Composable
fun TopMarginSettingsSection(
    settingsViewModel: SettingsViewModel
) {
    val colorScheme = MaterialTheme.colorScheme
    val marginStep by settingsViewModel.topMarginStep.collectAsState()
    val customMargin by settingsViewModel.customTopMargin.collectAsState()
    var customMarginText by remember(customMargin) { mutableStateOf(customMargin.toString()) }
    val isCustomActive = marginStep >= 5

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Верхний отступ текста",
                fontSize = 16.sp,
                color = colorScheme.onSurface
            )
            val displayText = when {
                isCustomActive -> "$customMargin dp (Своё)"
                else -> "${marginStep * 48} dp"
            }
            Text(
                text = displayText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.primary
            )
        }

        Slider(
            value = marginStep.coerceIn(1, 5).toFloat(),
            onValueChange = { 
                val newStep = it.roundToInt()
                settingsViewModel.updateTopMarginStep(newStep)
            },
            valueRange = 1f..5f,
            steps = 3,
            modifier = Modifier.fillMaxWidth()
        )

        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
        OutlinedTextField(
            value = customMarginText,
            onValueChange = {
                customMarginText = it.filter { it.isDigit() }
            },
            enabled = isCustomActive,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    val n = customMarginText.toIntOrNull() ?: 96
                    settingsViewModel.updateCustomTopMargin(maxOf(0, n))
                    focusManager.clearFocus()
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused) {
                        val n = customMarginText.toIntOrNull() ?: 96
                        settingsViewModel.updateCustomTopMargin(maxOf(0, n))
                    }
                },
            label = { Text("Свой отступ в dp (активно при 5+)") },
            singleLine = true
        )
    }
}

@Composable
fun RollHistorySettingsSection(
    settingsViewModel: SettingsViewModel
) {
    val colorScheme = MaterialTheme.colorScheme
    val historySize by settingsViewModel.rollHistorySize.collectAsState()
    val customSize by settingsViewModel.customRollHistorySize.collectAsState()
    var customSizeText by remember(customSize) { mutableStateOf(customSize.toString()) }
    val isCustomActive = historySize >= 10

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Количество прошлых бросков",
                fontSize = 16.sp,
                color = colorScheme.onSurface
            )
            Text(
                text = if (isCustomActive) "$customSize (Своё)" else historySize.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.primary
            )
        }

        Slider(
            value = historySize.coerceIn(1, 10).toFloat(),
            onValueChange = { 
                val newSize = it.roundToInt()
                settingsViewModel.updateRollHistorySize(newSize)
            },
            valueRange = 1f..10f,
            steps = 8,
            modifier = Modifier.fillMaxWidth()
        )

        val focusManager = LocalFocusManager.current
        OutlinedTextField(
            value = customSizeText,
            onValueChange = {
                customSizeText = it.filter { it.isDigit() }
            },
            enabled = isCustomActive,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    val n = customSizeText.toIntOrNull() ?: 10
                    settingsViewModel.updateCustomRollHistorySize(maxOf(1, n))
                    focusManager.clearFocus()
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused) {
                        val n = customSizeText.toIntOrNull() ?: 10
                        settingsViewModel.updateCustomRollHistorySize(maxOf(1, n))
                    }
                },
            label = { Text("Свое количество (активно при 10+)") },
            singleLine = true
        )
    }
}

@Composable
fun ScaleSettingsSection(
    scaleFactor: Float,
    onScaleChange: (Float) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Масштаб интерфейса",
                fontSize = 16.sp,
                color = colorScheme.onSurface
            )
            Text(
                text = "${(scaleFactor * 100).roundToInt()}%",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.primary
            )
        }
        
        Slider(
            value = scaleFactor,
            onValueChange = onScaleChange,
            valueRange = 0.7f..1.5f,
            steps = 7, // (1.5 - 0.7) / 0.1 - 1 = 8 - 1 = 7 steps
            modifier = Modifier.fillMaxWidth()
        )
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
            }
        }
    }
}
