package ru.quasaris.characters.master

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import ru.quasaris.characters.master.ui.SettingsViewModel
import kotlin.math.roundToInt

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
                .padding(16.dp),
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
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) { Text("M3") }
                    SegmentedButton(
                        selected = themeMode == AppThemeMode.OFF,
                        onClick = { 
                            themeMode = AppThemeMode.OFF
                            settingsManager.themeMode = AppThemeMode.OFF 
                            onThemeModeChange(AppThemeMode.OFF)
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) { Text("Oled") }
                    SegmentedButton(
                        selected = themeMode == AppThemeMode.CHARACTER,
                        onClick = { 
                            themeMode = AppThemeMode.CHARACTER
                            settingsManager.themeMode = AppThemeMode.CHARACTER 
                            onThemeModeChange(AppThemeMode.CHARACTER)
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) { Text("Персонаж BETA") }
                }
            }

            HorizontalDivider(color = colorScheme.outlineVariant)

            ScaleSettingsSection(
                scaleFactor = scaleFactor,
                onScaleChange = { settingsViewModel.updateScaleFactor(it) }
            )
            
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
