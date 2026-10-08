package ru.quasaris.characternexus.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import ru.quasaris.characternexus.ui.colourpicker.ColourPickerDialog
import ru.quasaris.characternexus.ui.colourpicker.ColourUtils
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.quasaris.characternexus.backend.SettingsViewModel
import ru.quasaris.characternexus.getDynamicColorScheme
import ru.quasaris.characternexus.model.AppInterfaceMode
import ru.quasaris.characternexus.model.AppThemeBehavior
import ru.quasaris.characternexus.model.AppThemeMode
import ru.quasaris.characternexus.ui.util.PayWall
import kotlin.math.roundToInt

private const val SHOW_DEBUG_SETTINGS = true

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsWindow(
    settingsViewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val themeMode by settingsViewModel.themeMode.collectAsState()
    val isPremium by settingsViewModel.isPremium.collectAsState()
    val colorScheme = MaterialTheme.colorScheme
    val scaleFactor by settingsViewModel.scaleFactor.collectAsState()
    val useOldAvatarStyle by settingsViewModel.useOldAvatarStyle.collectAsState()

    var showFlexibleColorsDialog by remember { mutableStateOf(false) }

    if (showFlexibleColorsDialog) {
        FlexibleThemeColorsDialog(
            settingsViewModel = settingsViewModel,
            onDismiss = { showFlexibleColorsDialog = false }
        )
    }

    BackHandler(onBack = onDismiss)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Внешний вид", fontWeight = FontWeight.Bold) },
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
                // 1. Цвета приложения и поведение темы
                Text(
                    text = "Цвета приложения",
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val cornerRadius = 16.dp
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy((-9).dp)
                    ) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = themeMode == AppThemeMode.STOCK,
                                onClick = { settingsViewModel.updateThemeMode(AppThemeMode.STOCK) },
                                shape = RoundedCornerShape(topStart = cornerRadius, topEnd = 0.dp, bottomEnd = 0.dp, bottomStart = 0.dp),
                                modifier = Modifier.weight(1f)
                            ) { Text("Stock") }
                            SegmentedButton(
                                selected = themeMode == AppThemeMode.M3,
                                onClick = { settingsViewModel.updateThemeMode(AppThemeMode.M3) },
                                shape = RoundedCornerShape(topStart = 0.dp, topEnd = cornerRadius, bottomEnd = 0.dp, bottomStart = 0.dp),
                                modifier = Modifier.weight(1f)
                            ) { Text("Material You") }
                        }
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            PayWall(isLocked = !isPremium, modifier = Modifier.weight(1f)) {
                                SegmentedButton(
                                    selected = themeMode == AppThemeMode.OFF,
                                    onClick = { settingsViewModel.updateThemeMode(AppThemeMode.OFF) },
                                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomEnd = 0.dp, bottomStart = 0.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("BLACK") }
                            }
                            PayWall(isLocked = !isPremium, modifier = Modifier.weight(1f)) {
                                SegmentedButton(
                                    selected = themeMode == AppThemeMode.CHARACTER,
                                    onClick = { settingsViewModel.updateThemeMode(AppThemeMode.CHARACTER) },
                                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomEnd = 0.dp, bottomStart = 0.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Персонаж") }
                            }
                        }
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = themeMode == AppThemeMode.WHITE,
                                onClick = { settingsViewModel.updateThemeMode(AppThemeMode.WHITE) },
                                shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomEnd = 0.dp, bottomStart = cornerRadius),
                                modifier = Modifier.weight(1f)
                            ) { Text("WHITE") }
                            SegmentedButton(
                                selected = themeMode == AppThemeMode.CUSTOM,
                                onClick = { settingsViewModel.updateThemeMode(AppThemeMode.CUSTOM) },
                                shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomEnd = cornerRadius, bottomStart = 0.dp),
                                modifier = Modifier.weight(1f)
                            ) { Text("Своя") }
                        }
                    }

                    if (themeMode == AppThemeMode.M3) {
                        val behavior by settingsViewModel.themeBehavior.collectAsState()
                        val darkTheme = if (behavior == AppThemeBehavior.SYSTEM) isSystemInDarkTheme() else behavior == AppThemeBehavior.DARK
                        val dynamicScheme = getDynamicColorScheme(darkTheme)

                        if (dynamicScheme == null) {
                            val m3SeedColor by settingsViewModel.m3SeedColor.collectAsState()
                            var hexText by remember(m3SeedColor) { mutableStateOf(m3SeedColor) }
                            var showM3Picker by remember { mutableStateOf(false) }

                            if (showM3Picker) {
                                val currentColor = ColourUtils.parseHexColor(hexText) ?: colorScheme.primary
                                ColourPickerDialog(
                                    initialColor = currentColor,
                                    onDismiss = { showM3Picker = false },
                                    onColorConfirmed = { chosenColor ->
                                        val hex = ColourUtils.colorToHex(chosenColor, includeAlpha = false)
                                        hexText = hex
                                        settingsViewModel.updateM3SeedColor(hex)
                                    },
                                    showAlpha = false,
                                    title = "Material You цвет темы"
                                )
                            }

                            OutlinedTextField(
                                value = hexText,
                                onValueChange = {
                                    hexText = it
                                    if (it.length == 7 && it.startsWith("#")) {
                                        settingsViewModel.updateM3SeedColor(it)
                                    }
                                },
                                label = { Text("HEX цвет темы (напр. #6750A4)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = {
                                    val displayColor = ColourUtils.parseHexColor(hexText) ?: colorScheme.primary
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .clickable { showM3Picker = true }
                                            .background(displayColor, CircleShape)
                                            .border(1.dp, colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                                    )
                                }
                            )
                        }
                    }

                    AnimatedVisibility(visible = themeMode == AppThemeMode.CUSTOM) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val customSeed by settingsViewModel.customThemeSeed.collectAsState()
                            var customSeedText by remember(customSeed) { mutableStateOf(customSeed) }
                            var showCustomSeedPicker by remember { mutableStateOf(false) }

                            if (showCustomSeedPicker) {
                                val currentColor = ColourUtils.parseHexColor(customSeedText) ?: colorScheme.primary
                                ColourPickerDialog(
                                    initialColor = currentColor,
                                    onDismiss = { showCustomSeedPicker = false },
                                    onColorConfirmed = { chosenColor ->
                                        val hex = ColourUtils.colorToHex(chosenColor, includeAlpha = false)
                                        customSeedText = hex
                                        settingsViewModel.updateCustomThemeSeed(hex)
                                    },
                                    showAlpha = false,
                                    title = "Сид цвет темы"
                                )
                            }

                            OutlinedTextField(
                                value = customSeedText,
                                onValueChange = {
                                    customSeedText = it
                                    if (it.length == 7 && it.startsWith("#")) {
                                        settingsViewModel.updateCustomThemeSeed(it)
                                    }
                                },
                                label = { Text("HEX цвет сида (напр. #6750A4)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = {
                                    val displayColor = ColourUtils.parseHexColor(customSeedText) ?: colorScheme.primary
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .clickable { showCustomSeedPicker = true }
                                            .background(displayColor, CircleShape)
                                            .border(1.dp, colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                                    )
                                }
                            )

                            Button(
                                onClick = { showFlexibleColorsDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorScheme.secondaryContainer,
                                    contentColor = colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Гибкая настройка цветов", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Поведение темы",
                        fontSize = 16.sp,
                        color = colorScheme.onSurface
                    )

                    val themeBehavior by settingsViewModel.themeBehavior.collectAsState()
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = themeBehavior == AppThemeBehavior.LIGHT && themeMode != AppThemeMode.OFF,
                            onClick = { settingsViewModel.updateThemeBehavior(AppThemeBehavior.LIGHT) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                            enabled = themeMode != AppThemeMode.OFF,
                            icon = { Icon(Icons.Default.LightMode, null) }
                        ) { Text("Светлая") }
                        SegmentedButton(
                            selected = themeBehavior == AppThemeBehavior.SYSTEM && themeMode != AppThemeMode.OFF,
                            onClick = { settingsViewModel.updateThemeBehavior(AppThemeBehavior.SYSTEM) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                            enabled = themeMode != AppThemeMode.OFF,
                            icon = { Icon(Icons.Default.SettingsBrightness, null) }
                        ) { Text("Система") }
                        SegmentedButton(
                            selected = themeBehavior == AppThemeBehavior.DARK || themeMode == AppThemeMode.OFF,
                            onClick = { settingsViewModel.updateThemeBehavior(AppThemeBehavior.DARK) },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                            enabled = themeMode != AppThemeMode.OFF,
                            icon = { Icon(Icons.Default.DarkMode, null) }
                        ) { Text("Темная") }
                    }
                }

                HorizontalDivider(color = colorScheme.outlineVariant)

                // 2. Режим интерфейса
                Text(
                    text = "Режим интерфейса",
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                val interfaceMode by settingsViewModel.interfaceMode.collectAsState()
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = interfaceMode == AppInterfaceMode.MOBILE,
                        onClick = { settingsViewModel.updateInterfaceMode(AppInterfaceMode.MOBILE) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                        icon = { Icon(Icons.Default.Smartphone, null) }
                    ) { Text("Мобильный") }
                    SegmentedButton(
                        selected = interfaceMode == AppInterfaceMode.AUTO,
                        onClick = { settingsViewModel.updateInterfaceMode(AppInterfaceMode.AUTO) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                        icon = { Icon(Icons.Default.BrightnessAuto, null) }
                    ) { Text("Авто") }
                    SegmentedButton(
                        selected = interfaceMode == AppInterfaceMode.DESKTOP,
                        onClick = { settingsViewModel.updateInterfaceMode(AppInterfaceMode.DESKTOP) },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                        icon = { Icon(Icons.Default.DesktopWindows, null) }
                    ) { Text("Десктоп") }
                }

                HorizontalDivider(color = colorScheme.outlineVariant)

                // 3. Масштаб интерфейса
                ScaleSettingsSection(
                    scaleFactor = scaleFactor,
                    onScaleChange = { settingsViewModel.updateScaleFactor(it) }
                )

                HorizontalDivider(color = colorScheme.outlineVariant)

                // 4. Эффекты размытия
                BlurSettingsSection(
                    settingsViewModel = settingsViewModel
                )

                HorizontalDivider(color = colorScheme.outlineVariant)

                // 5. Старое отображение аватарок
                Text(
                    text = "Отображение персонажей",
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
                            text = "Старое отображение аватарок",
                            fontSize = 16.sp,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Возвращает классический вид иконок персонажей в списке",
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = useOldAvatarStyle,
                        onCheckedChange = { settingsViewModel.updateUseOldAvatarStyle(it) }
                    )
                }

                HorizontalDivider(color = colorScheme.outlineVariant)

                // 6. Новый/старый интерфейс заголовков
                NewHeaderInterfaceSettingsSection(
                    settingsViewModel = settingsViewModel
                )
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
    val blurCards by settingsViewModel.blurCards.collectAsState()
    val blurDynamicFields by settingsViewModel.blurDynamicFields.collectAsState()
    val rollAlpha by settingsViewModel.rollInterfaceAlpha.collectAsState()
    val diceFabAlpha by settingsViewModel.diceFabAlpha.collectAsState()
    val diceFabBlur by settingsViewModel.diceFabBlurEnabled.collectAsState()
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
                    label = "Кнопка броска (к20)",
                    checked = diceFabBlur,
                    onCheckedChange = { onToggle(it) { settingsViewModel.updateDiceFabBlurEnabled(it) } }
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

                BlurSwitchRow(
                    label = "Карточки",
                    checked = blurCards,
                    onCheckedChange = { onToggle(it) { settingsViewModel.updateBlurCards(it) } }
                )

                BlurSwitchRow(
                    label = "Большие поля",
                    checked = blurDynamicFields,
                    onCheckedChange = { onToggle(it) { settingsViewModel.updateBlurDynamicFields(it) } }
                )

                HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)

                BlurRadiusSection(settingsViewModel)
            }
        }

        HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val isAlphaDisabled = masterBlurEnabled && blurRolls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Прозрачность интерфейса броска",
                    fontSize = 16.sp,
                    color = if (isAlphaDisabled) colorScheme.onSurface.copy(alpha = 0.38f) else colorScheme.onSurface
                )
                Text(
                    text = "${((1f - rollAlpha) * 100).roundToInt()}%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isAlphaDisabled) colorScheme.primary.copy(alpha = 0.38f) else colorScheme.primary
                )
            }
            Slider(
                value = 1f - rollAlpha,
                onValueChange = { settingsViewModel.updateRollInterfaceAlpha(1f - it) },
                enabled = !isAlphaDisabled,
                valueRange = 0f..1f,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val isFabAlphaDisabled = masterBlurEnabled && diceFabBlur
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Прозрачность кнопки броска",
                    fontSize = 16.sp,
                    color = if (isFabAlphaDisabled) colorScheme.onSurface.copy(alpha = 0.38f) else colorScheme.onSurface
                )
                Text(
                    text = "${((1f - diceFabAlpha) * 100).roundToInt()}%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isFabAlphaDisabled) colorScheme.primary.copy(alpha = 0.38f) else colorScheme.primary
                )
            }
            Slider(
                value = 1f - diceFabAlpha,
                onValueChange = { settingsViewModel.updateDiceFabAlpha(1f - it) },
                enabled = !isFabAlphaDisabled,
                valueRange = 0f..1f,
                modifier = Modifier.fillMaxWidth()
            )
        }

        HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.5.dp)

        Button(
            onClick = { settingsViewModel.updateDiceFabPosition(-10f, -10f) },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.surfaceVariant,
                contentColor = colorScheme.primary
            )
        ) {
            Text("Сбросить положение кнопки броска кубов")
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
fun BlurRadiusSection(settingsViewModel: SettingsViewModel) {
    val colorScheme = MaterialTheme.colorScheme
    val blurRadius by settingsViewModel.blurRadius.collectAsState()
    val customBlurRadius by settingsViewModel.customBlurRadius.collectAsState()
    var customBlurText by remember(customBlurRadius) { mutableStateOf(customBlurRadius.toString()) }
    val isCustomActive = blurRadius >= 48

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
                text = "Сила размытия (радиус)",
                fontSize = 16.sp,
                color = colorScheme.onSurface
            )
            Text(
                text = if (isCustomActive) "$customBlurRadius dp (Своё)" else "$blurRadius dp",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.primary
            )
        }

        Slider(
            value = blurRadius.coerceIn(1, 48).toFloat(),
            onValueChange = {
                val newVal = it.roundToInt()
                settingsViewModel.updateBlurRadius(newVal)
            },
            valueRange = 1f..48f,
            modifier = Modifier.fillMaxWidth()
        )

        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
        OutlinedTextField(
            value = customBlurText,
            onValueChange = {
                customBlurText = it.filter { it.isDigit() }
            },
            enabled = isCustomActive,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    val n = customBlurText.toIntOrNull() ?: 48
                    settingsViewModel.updateCustomBlurRadius(maxOf(1, n))
                    focusManager.clearFocus()
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused) {
                        val n = customBlurText.toIntOrNull() ?: 48
                        settingsViewModel.updateCustomBlurRadius(maxOf(1, n))
                    }
                },
            label = { Text("Своё значение в dp (активно при 48+)") },
            singleLine = true
        )
    }
}

@Composable
fun WarningBlurDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .border(2.dp, colorScheme.error, RoundedCornerShape(28.dp)),
        containerColor = colorScheme.surface,
        tonalElevation = 8.dp,
        title = {
            Text(
                text = "⚠️ ВНИМАНИЕ: ОПАСНО ДЛЯ УСТРОЙСТВА",
                color = colorScheme.error,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            val annotatedString = buildAnnotatedString {
                append("Вы собираетесь принудительно включить эффекты размытия на неподдерживаемом устройстве. Это может привести к дикому троттлингу, лагам, критическому перегреву и даже выходу из строя железа при долгой партии. Вы рискуете ")
                withStyle(style = SpanStyle(color = colorScheme.error, fontWeight = FontWeight.Bold)) {
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
                color = colorScheme.onSurface,
                fontSize = 16.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Да, я понимаю риски и беру ответственность на себя", color = colorScheme.error, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Не включать", color = colorScheme.onSurface)
            }
        },
        shape = RoundedCornerShape(28.dp),
    )
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
            steps = 7,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun NewHeaderInterfaceSettingsSection(
    settingsViewModel: SettingsViewModel
) {
    val colorScheme = MaterialTheme.colorScheme
    val useNewAC by settingsViewModel.useNewACInterface.collectAsState()
    val useNewInit by settingsViewModel.useNewInitInterface.collectAsState()
    val useNewCond by settingsViewModel.useNewCondInterface.collectAsState()
    val useNewSpeed by settingsViewModel.useNewSpeedInterface.collectAsState()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Новый интерфейс заголовков",
            style = MaterialTheme.typography.titleMedium,
            color = colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        InterfaceSwitchRow("Класс Доспеха (КД)", useNewAC) { settingsViewModel.updateUseNewACInterface(it) }
        InterfaceSwitchRow("Инициатива", useNewInit) { settingsViewModel.updateUseNewInitInterface(it) }
        InterfaceSwitchRow("Состояния", useNewCond) { settingsViewModel.updateUseNewCondInterface(it) }
        InterfaceSwitchRow("Скорость", useNewSpeed) { settingsViewModel.updateUseNewSpeedInterface(it) }
    }
}

@Composable
fun InterfaceSwitchRow(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlexibleThemeColorsDialog(
    settingsViewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val useFlexible by settingsViewModel.customUseFlexibleColors.collectAsState()
    val primary by settingsViewModel.customPrimary.collectAsState()
    val secondary by settingsViewModel.customSecondary.collectAsState()
    val tertiary by settingsViewModel.customTertiary.collectAsState()
    val background by settingsViewModel.customBackground.collectAsState()
    val surface by settingsViewModel.customSurface.collectAsState()
    val surfaceVariant by settingsViewModel.customSurfaceVariant.collectAsState()
    val outline by settingsViewModel.customOutline.collectAsState()
    val onPrimary by settingsViewModel.customOnPrimary.collectAsState()
    val onSurface by settingsViewModel.customOnSurface.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Гибкая настройка цветов", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Включить гибкую настройку цветов")
                    Switch(
                        checked = useFlexible,
                        onCheckedChange = { settingsViewModel.updateCustomUseFlexibleColors(it) }
                    )
                }

                if (useFlexible) {
                    ColorTextField("Primary (Основной)", primary) { settingsViewModel.updateCustomColor("primary", it) }
                    ColorTextField("On Primary (Текст на основном)", onPrimary) { settingsViewModel.updateCustomColor("onPrimary", it) }
                    ColorTextField("Secondary (Вторичный)", secondary) { settingsViewModel.updateCustomColor("secondary", it) }
                    ColorTextField("Tertiary (Третичный)", tertiary) { settingsViewModel.updateCustomColor("tertiary", it) }
                    ColorTextField("Background (Фон)", background) { settingsViewModel.updateCustomColor("background", it) }
                    ColorTextField("Surface (Поверхность)", surface) { settingsViewModel.updateCustomColor("surface", it) }
                    ColorTextField("Surface Variant (Вариант поверхности)", surfaceVariant) { settingsViewModel.updateCustomColor("surfaceVariant", it) }
                    ColorTextField("Outline (Контур/Аутлайн)", outline) { settingsViewModel.updateCustomColor("outline", it) }
                    ColorTextField("On Surface (Текст на поверхности)", onSurface) { settingsViewModel.updateCustomColor("onSurface", it) }
                }

                Button(
                    onClick = { settingsViewModel.resetCustomThemeColors() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.errorContainer,
                        contentColor = colorScheme.onErrorContainer
                    )
                ) {
                    Text("Сбросить цвета темы")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Готово")
            }
        }
    )
}

@Composable
fun ColorTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    var showColourPicker by remember { mutableStateOf(false) }

    if (showColourPicker) {
        val currentColor = ColourUtils.parseHexColor(text) ?: MaterialTheme.colorScheme.primary
        ColourPickerDialog(
            initialColor = currentColor,
            onDismiss = { showColourPicker = false },
            onColorConfirmed = { chosenColor ->
                val hex = ColourUtils.colorToHex(chosenColor, includeAlpha = false)
                text = hex
                onValueChange(hex)
            },
            showAlpha = false,
            title = label
        )
    }

    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            if (it.startsWith("#") && it.length <= 7) {
                onValueChange(it)
            }
        },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        trailingIcon = {
            val displayColor = ColourUtils.parseHexColor(text) ?: MaterialTheme.colorScheme.primary
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable { showColourPicker = true }
                    .background(displayColor, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
            )
        }
    )
}
