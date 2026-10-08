package ru.quasaris.characternexus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import ru.quasaris.characternexus.backend.SettingsViewModel
import ru.quasaris.characternexus.generated.BuildConstants
import ru.quasaris.characternexus.model.ExportFormat
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsWindow(
    onOpenDrawer: () -> Unit,
    settingsViewModel: SettingsViewModel,
    hazeState: HazeState? = null,
    onFullscreenDialogOpenChange: (Boolean) -> Unit = {},
    onCleanupDuplicateFolders: () -> Unit = {}
) {
    val isPremium by settingsViewModel.isPremium.collectAsState()
    val colorScheme = MaterialTheme.colorScheme

    var showResetDialog by remember { mutableStateOf(false) }
    var showKeybindSettings by remember { mutableStateOf(false) }
    var showAppearanceSettings by remember { mutableStateOf(false) }
    var showAdvancedSettings by remember { mutableStateOf(false) }

    val forceBlurEnabled by settingsViewModel.blurFullscreen.collectAsState()
    val masterBlurEnabled by settingsViewModel.masterBlurEnabled.collectAsState()
    val effectiveBlurFullscreen = masterBlurEnabled && forceBlurEnabled

    LaunchedEffect(showKeybindSettings, showAppearanceSettings, showAdvancedSettings) {
        onFullscreenDialogOpenChange(showKeybindSettings || showAppearanceSettings || showAdvancedSettings)
    }

    if (showAppearanceSettings) {
        AppearanceSettingsWindow(
            settingsViewModel = settingsViewModel,
            onDismiss = { showAppearanceSettings = false }
        )
    }

    if (showAdvancedSettings) {
        AdvancedSettingsWindow(
            settingsViewModel = settingsViewModel,
            onDismiss = { showAdvancedSettings = false },
            onCleanupDuplicateFolders = onCleanupDuplicateFolders,
            onFullscreenDialogOpenChange = onFullscreenDialogOpenChange
        )
    }

    if (showKeybindSettings) {
        KeybindSettingsWindow(
            viewModel = settingsViewModel,
            onDismiss = { showKeybindSettings = false },
            forceBlurEnabled = effectiveBlurFullscreen
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Сброс настроек") },
            text = { Text("Вы уверены, что хотите сбросить все настройки к заводским значениям? Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        settingsViewModel.resetToDefaults()
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
            // Кнопки перехода в категорийные окна
            Text(
                text = "Разделы настроек",
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = { showAppearanceSettings = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.secondaryContainer,
                    contentColor = colorScheme.onSecondaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Palette, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Внешний вид", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { showAdvancedSettings = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.secondaryContainer,
                    contentColor = colorScheme.onSecondaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Tune, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Продвинутые настройки", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { showKeybindSettings = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.secondaryContainer,
                    contentColor = colorScheme.onSecondaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Keyboard, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Изменить горячие клавиши", fontWeight = FontWeight.Bold)
            }

            HorizontalDivider(color = colorScheme.outlineVariant)

            // Character Nexus Premium
            Text(
                text = "Character Nexus Premium",
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
                    text = "Премиум аккаунт",
                    fontSize = 16.sp,
                    color = colorScheme.onSurface
                )
                Switch(
                    checked = isPremium,
                    onCheckedChange = { settingsViewModel.updateIsPremium(it) }
                )
            }

            HorizontalDivider(color = colorScheme.outlineVariant)

            // Экспорт
            ExportSettingsSection(settingsViewModel = settingsViewModel)

            HorizontalDivider(color = colorScheme.outlineVariant)

            // История бросков
            RollHistorySettingsSection(
                settingsViewModel = settingsViewModel
            )

            HorizontalDivider(color = colorScheme.outlineVariant)

            // Предупреждение об удалении
            DeletionWarningSettingsSection(
                settingsViewModel = settingsViewModel
            )

            HorizontalDivider(color = colorScheme.outlineVariant)

            // Полноэкранное редактирование
            FullscreenEditingSettingsSection(
                settingsViewModel = settingsViewModel
            )

            HorizontalDivider(color = colorScheme.outlineVariant)

            // LSS импорт
            LssImportSettingsSection(
                settingsViewModel = settingsViewModel
            )

            HorizontalDivider(color = colorScheme.outlineVariant)

            // Форматирование текста
            Text(
                text = "Форматирование текста",
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            TopMarginSettingsSection(settingsViewModel = settingsViewModel)

            HorizontalDivider(color = colorScheme.outlineVariant)

            // О приложении
            Text(
                text = "О приложении",
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Мастер Персонажей\nВерсия ${BuildConstants.VERSION}",
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
                        "Downloads/Character Nexus"
                    },
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { /* directoryPicker.launch() */ }) {
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
fun TopMarginSettingsSection(
    settingsViewModel: SettingsViewModel
) {
    val colorScheme = MaterialTheme.colorScheme
    val marginStep by settingsViewModel.topMarginStep.collectAsState()
    val customMargin by settingsViewModel.customTopMargin.collectAsState()
    var customMarginText by remember(customMargin) { mutableStateOf(customMargin.toString()) }
    val isCustomActive = marginStep >= 4

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
                marginStep == 0 -> "0 dp (Без отступа)"
                marginStep == 1 -> "48 dp (1 строка)"
                marginStep == 2 -> "96 dp (2 строки - рекомендуемый)"
                marginStep == 3 -> "144 dp (3 строки)"
                else -> "$customMargin dp (Своё)"
            }
            Text(
                text = displayText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.primary
            )
        }

        Slider(
            value = marginStep.coerceIn(0, 4).toFloat(),
            onValueChange = {
                val newStep = it.roundToInt()
                settingsViewModel.updateTopMarginStep(newStep)
            },
            valueRange = 0f..4f,
            steps = 3,
            modifier = Modifier.fillMaxWidth()
        )

        val focusManager = LocalFocusManager.current
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
            label = { Text("Свой отступ в dp (активно при 4+)") },
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
