package ru.quasaris.characternexus.HeaderCode.Fullscreen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.*
import ru.quasaris.characternexus.HeaderCode.LevelPanel
import ru.quasaris.characternexus.HeaderCode.HealthPanel
import ru.quasaris.characternexus.ui.RestPanel
import ru.quasaris.characternexus.model.HitDiceEntry
import ru.quasaris.characternexus.ui.theme.rememberEffectiveBlurRadius

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelPanelOverlay(
    level: String,
    onLevelChange: (String) -> Unit,
    experience: String,
    onExpChange: (String) -> Unit,
    proficiencyBonus: String,
    onProfChange: (String) -> Unit,
    nextLevelExp: String,
    statsMap: Map<String, String>,
    onDismiss: () -> Unit,
    hazeState: HazeState? = null,
    settingsViewModel: ru.quasaris.characternexus.backend.SettingsViewModel? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val isOled = colorScheme.background == Color.Black
    val blurRadius = rememberEffectiveBlurRadius(settingsViewModel)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .run {
                if (hazeState != null && !isOled) {
                    this.hazeEffect(state = hazeState) {
                        style = HazeStyle(
                            blurRadius = blurRadius,
                            tints = listOf(HazeTint(Color.Black.copy(alpha = 0.2f)))
                        )
                    }
                } else this
            },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Уровень и Опыт", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = if (hazeState != null && !isOled) Color.Transparent.copy(alpha = 0.0f) else colorScheme.surface
                )
            )
        },
        containerColor = if (hazeState != null && !isOled) Color.Transparent.copy(alpha = 0.0f) else colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            LevelPanel(
                level = level,
                onLevelChange = onLevelChange,
                exp = experience,
                onExpChange = onExpChange,
                prof = proficiencyBonus,
                onProfChange = onProfChange,
                nextExp = nextLevelExp,
                stats = statsMap,
                standalone = false
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthPanelOverlay(
    maxHp: String,
    onMaxHpChange: (String) -> Unit,
    tempHp: String,
    onTempHpChange: (String) -> Unit,
    currentHp: String,
    onCurrentHpChange: (String) -> Unit,
    onHealClick: () -> Unit,
    onDamageClick: () -> Unit,
    onTempClick: () -> Unit,
    healthColor: Color,
    hitDiceEntries: List<HitDiceEntry>,
    onSpentHitDiceChange: (Int, Int) -> Unit,
    onOpenHealthSettings: () -> Unit,
    onDismiss: () -> Unit,
    hazeState: HazeState? = null,
    settingsViewModel: ru.quasaris.characternexus.backend.SettingsViewModel? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val isOled = colorScheme.background == Color.Black
    val blurRadius = rememberEffectiveBlurRadius(settingsViewModel)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .run {
                if (hazeState != null && !isOled) {
                    this.hazeEffect(state = hazeState) {
                        style = HazeStyle(
                            blurRadius = blurRadius,
                            tints = listOf(HazeTint(Color.Black.copy(alpha = 0.2f)))
                        )
                    }
                } else this
            },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Здоровье", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = if (hazeState != null && !isOled) Color.Transparent.copy(alpha = 0.0f) else colorScheme.surface
                )
            )
        },
        containerColor = if (hazeState != null && !isOled) Color.Transparent.copy(alpha = 0.0f) else colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            HealthPanel(
                maxHp = maxHp,
                onMaxHpChange = onMaxHpChange,
                tempHp = tempHp,
                onTempHpChange = onTempHpChange,
                currentHp = currentHp,
                onCurrentHpChange = onCurrentHpChange,
                onHealClick = onHealClick,
                onDamageClick = onDamageClick,
                onTempClick = onTempClick,
                healthColor = healthColor,
                onFocusLost = {},
                hitDiceEntries = hitDiceEntries,
                onSpentHitDiceChange = onSpentHitDiceChange,
                onOpenSettings = onOpenHealthSettings
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestPanelOverlay(
    onRestPanelDismiss: () -> Unit,
    onRestPanelHitDiceChange: (List<HitDiceEntry>) -> Unit,
    hitDiceEntries: List<HitDiceEntry>,
    onHealAmount: (Int) -> Unit,
    onShortRestConfirmed: () -> Unit,
    onLongRest: () -> Unit,
    onDawn: () -> Unit,
    defaultHitDie: Int,
    hazeState: HazeState? = null,
    settingsViewModel: ru.quasaris.characternexus.backend.SettingsViewModel? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val isOled = colorScheme.background == Color.Black
    val blurRadius = rememberEffectiveBlurRadius(settingsViewModel)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .run {
                if (hazeState != null && !isOled) {
                    this.hazeEffect(state = hazeState) {
                        style = HazeStyle(
                            blurRadius = blurRadius,
                            tints = listOf(HazeTint(Color.Black.copy(alpha = 0.2f)))
                        )
                    }
                } else this
            },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Отдых", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onRestPanelDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = if (hazeState != null && !isOled) Color.Transparent.copy(alpha = 0.0f) else colorScheme.surface
                )
            )
        },
        containerColor = if (hazeState != null && !isOled) Color.Transparent.copy(alpha = 0.0f) else colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RestPanel(
                hitDiceEntries = hitDiceEntries,
                onHitDiceChange = onRestPanelHitDiceChange,
                onHeal = onHealAmount,
                onShortRestConfirmed = onShortRestConfirmed,
                onDismiss = onRestPanelDismiss,
                statsMap = emptyMap(),
                defaultHitDie = defaultHitDie
            )
            
            Button(
                onClick = onLongRest,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Длинный отдых")
            }
            
            Button(
                onClick = onDawn,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Рассвет")
            }
        }
    }
}
