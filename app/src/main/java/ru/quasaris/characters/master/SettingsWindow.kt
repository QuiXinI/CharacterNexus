package ru.quasaris.characters.master

import androidx.compose.foundation.border
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import ru.quasaris.characters.master.backend.AppThemeMode
import ru.quasaris.characters.master.backend.SettingsManager
import ru.quasaris.characters.master.backend.SettingsViewModel
import kotlin.math.roundToInt

private const val SHOW_DEBUG_SETTINGS = false

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
                    ) { Text("OLED") }
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

            DeletionWarningSettingsSection(
                settingsViewModel = settingsViewModel
            )

            HorizontalDivider(color = colorScheme.outlineVariant)

            FullscreenEditingSettingsSection(
                settingsViewModel = settingsViewModel
            )

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
        }
    }
}

@Composable
fun BlurSettingsSection(
    settingsViewModel: SettingsViewModel
) {
    val colorScheme = MaterialTheme.colorScheme
    val forceBlurEnabled by settingsViewModel.forceBlurEnabled.collectAsState()
    val debugInfoEnabled by settingsViewModel.debugInfoEnabled.collectAsState()
    val performanceClass = settingsViewModel.performanceClass
    var showWarningDialog by remember { mutableStateOf(false) }

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
                    text = "Эффекты размытия.",
                    fontSize = 16.sp,
                    color = colorScheme.onSurface
                )
                val isDebugMode = SHOW_DEBUG_SETTINGS && debugInfoEnabled
                val description = if (isDebugMode) {
                    "Использовать эффекты размытия \nКласс мощности: $performanceClass"
                } else {
                    "Использовать эффекты размытия"
                }
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = forceBlurEnabled,
                onCheckedChange = { checked ->
                    if (checked && performanceClass < 33) {
                        showWarningDialog = true
                    } else {
                        settingsViewModel.updateForceBlurEnabled(checked)
                    }
                }
            )
        }
    }

    if (showWarningDialog) {
        WarningBlurDialog(
            onConfirm = {
                settingsViewModel.updateForceBlurEnabled(true)
                showWarningDialog = false
            },
            onDismiss = {
                showWarningDialog = false
            }
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
                Text("Да, я готов рисковать", color = Color.Red, fontWeight = FontWeight.Bold)
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
