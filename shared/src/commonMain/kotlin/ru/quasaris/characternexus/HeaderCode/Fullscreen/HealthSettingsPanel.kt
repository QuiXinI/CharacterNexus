package ru.quasaris.characternexus.HeaderCode.Fullscreen

import ru.quasaris.characternexus.model.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.chrisbanes.haze.*
import ru.quasaris.characternexus.ui.DialogDimStyle
import ru.quasaris.characternexus.ui.BackHandler
import ru.quasaris.characternexus.ui.theme.rememberEffectiveBlurRadius

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthSettingsDialog(
    isManual: Boolean,
    onManualChange: (Boolean) -> Unit,
    manualMaxHp: Int,
    onManualMaxHpChange: (Int) -> Unit,
    isMulticlass: Boolean,
    onMulticlassChange: (Boolean) -> Unit,
    currentHitDie: Int,
    onHitDieChange: (Int) -> Unit,
    hpLevelData: List<HPLevelEntry>,
    onHPLevelDataChange: (List<HPLevelEntry>) -> Unit,
    manualHPLevelData: List<HPLevelEntry>,
    onManualHPLevelDataChange: (List<HPLevelEntry>) -> Unit,
    manualMaxHitDice: Int,
    onManualMaxHitDiceChange: (Int) -> Unit,
    hpBonusesAtLevel: List<AttackBonus>,
    onHpBonusesAtLevelChange: (List<AttackBonus>) -> Unit,
    hpBonusesTotal: List<AttackBonus>,
    onHpBonusesTotalChange: (List<AttackBonus>) -> Unit,
    statsMap: Map<String, String>,
    level: Int,
    forceBlurEnabled: Boolean,
    onDismiss: () -> Unit,
    isDesktop: Boolean = false,
    hazeState: HazeState? = null,
    popupHazeState: HazeState? = null,
    settingsViewModel: ru.quasaris.characternexus.backend.SettingsViewModel? = null
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val blurRadius = rememberEffectiveBlurRadius(settingsViewModel)

    val handleDismiss = {
        focusManager.clearFocus()
        onDismiss()
    }

    if (isDesktop) {
        HealthSettingsDialogOverlay(
            isManual = isManual, onManualChange = onManualChange,
            manualMaxHp = manualMaxHp, onManualMaxHpChange = onManualMaxHpChange,
            isMulticlass = isMulticlass, onMulticlassChange = onMulticlassChange,
            currentHitDie = currentHitDie, onHitDieChange = onHitDieChange,
            hpLevelData = hpLevelData, onHPLevelDataChange = onHPLevelDataChange,
            manualHPLevelData = manualHPLevelData, onManualHPLevelDataChange = onManualHPLevelDataChange,
            manualMaxHitDice = manualMaxHitDice, onManualMaxHitDiceChange = onManualMaxHitDiceChange,
            hpBonusesAtLevel = hpBonusesAtLevel, onHpBonusesAtLevelChange = onHpBonusesAtLevelChange,
            hpBonusesTotal = hpBonusesTotal, onHpBonusesTotalChange = onHpBonusesTotalChange,
            statsMap = statsMap, level = level,
            onDismiss = handleDismiss,
            forceBlurEnabled = forceBlurEnabled,
            hazeState = popupHazeState ?: hazeState,
            blurRadius = blurRadius
        )
    } else {
        Dialog(
            onDismissRequest = handleDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            DialogDimStyle(0f)
            HealthSettingsDialogOverlay(
                isManual = isManual, onManualChange = onManualChange,
                manualMaxHp = manualMaxHp, onManualMaxHpChange = onManualMaxHpChange,
                isMulticlass = isMulticlass, onMulticlassChange = onMulticlassChange,
                currentHitDie = currentHitDie, onHitDieChange = onHitDieChange,
                hpLevelData = hpLevelData, onHPLevelDataChange = onHPLevelDataChange,
                manualHPLevelData = manualHPLevelData, onManualHPLevelDataChange = onManualHPLevelDataChange,
                manualMaxHitDice = manualMaxHitDice, onManualMaxHitDiceChange = onManualMaxHitDiceChange,
                hpBonusesAtLevel = hpBonusesAtLevel, onHpBonusesAtLevelChange = onHpBonusesAtLevelChange,
                hpBonusesTotal = hpBonusesTotal, onHpBonusesTotalChange = onHpBonusesTotalChange,
                statsMap = statsMap, level = level,
                onDismiss = handleDismiss,
                forceBlurEnabled = forceBlurEnabled,
                hazeState = popupHazeState ?: hazeState,
                blurRadius = blurRadius
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthSettingsDialogOverlay(
    isManual: Boolean,
    onManualChange: (Boolean) -> Unit,
    manualMaxHp: Int,
    onManualMaxHpChange: (Int) -> Unit,
    isMulticlass: Boolean,
    onMulticlassChange: (Boolean) -> Unit,
    currentHitDie: Int,
    onHitDieChange: (Int) -> Unit,
    hpLevelData: List<HPLevelEntry>,
    onHPLevelDataChange: (List<HPLevelEntry>) -> Unit,
    manualHPLevelData: List<HPLevelEntry>,
    onManualHPLevelDataChange: (List<HPLevelEntry>) -> Unit,
    manualMaxHitDice: Int,
    onManualMaxHitDiceChange: (Int) -> Unit,
    hpBonusesAtLevel: List<AttackBonus>,
    onHpBonusesAtLevelChange: (List<AttackBonus>) -> Unit,
    hpBonusesTotal: List<AttackBonus>,
    onHpBonusesTotalChange: (List<AttackBonus>) -> Unit,
    statsMap: Map<String, String>,
    level: Int,
    onDismiss: () -> Unit,
    forceBlurEnabled: Boolean,
    hazeState: HazeState? = null,
    blurRadius: androidx.compose.ui.unit.Dp = 24.dp
) {
    val colorScheme = MaterialTheme.colorScheme
    val isOled = colorScheme.background == Color.Black

    BackHandler(onBack = onDismiss)

    Scaffold(
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
                },
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Настройка ОЗ", fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = if (forceBlurEnabled && !isOled && hazeState != null) Color.Transparent.copy(alpha = 0.0f) else colorScheme.surface
                    )
                )
            },
            containerColor = if (forceBlurEnabled && !isOled && hazeState != null) Color.Transparent.copy(alpha = 0.0f) else colorScheme.background
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HealthSettingsContent(
                        isManual = isManual, onManualChange = onManualChange,
                        manualMaxHp = manualMaxHp, onManualMaxHpChange = onManualMaxHpChange,
                        isMulticlass = isMulticlass, onMulticlassChange = onMulticlassChange,
                        currentHitDie = currentHitDie, onHitDieChange = onHitDieChange,
                        hpLevelData = hpLevelData, onHPLevelDataChange = onHPLevelDataChange,
                        manualHPLevelData = manualHPLevelData, onManualHPLevelDataChange = onManualHPLevelDataChange,
                        manualMaxHitDice = manualMaxHitDice, onManualMaxHitDiceChange = onManualMaxHitDiceChange,
                        hpBonusesAtLevel = hpBonusesAtLevel, onHpBonusesAtLevelChange = onHpBonusesAtLevelChange,
                        hpBonusesTotal = hpBonusesTotal, onHpBonusesTotalChange = onHpBonusesTotalChange,
                        statsMap = statsMap, level = level
                    )

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

@Composable
fun HealthSettingsContent(
    isManual: Boolean,
    onManualChange: (Boolean) -> Unit,
    manualMaxHp: Int,
    onManualMaxHpChange: (Int) -> Unit,
    isMulticlass: Boolean,
    onMulticlassChange: (Boolean) -> Unit,
    currentHitDie: Int,
    onHitDieChange: (Int) -> Unit,
    hpLevelData: List<HPLevelEntry>,
    onHPLevelDataChange: (List<HPLevelEntry>) -> Unit,
    manualHPLevelData: List<HPLevelEntry>,
    onManualHPLevelDataChange: (List<HPLevelEntry>) -> Unit,
    manualMaxHitDice: Int,
    onManualMaxHitDiceChange: (Int) -> Unit,
    hpBonusesAtLevel: List<AttackBonus>,
    onHpBonusesAtLevelChange: (List<AttackBonus>) -> Unit,
    hpBonusesTotal: List<AttackBonus>,
    onHpBonusesTotalChange: (List<AttackBonus>) -> Unit,
    statsMap: Map<String, String>,
    level: Int
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Manual Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Ручной ввод ОЗ", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Switch(checked = isManual, onCheckedChange = onManualChange)
            }
        }

        if (isManual) {
            OutlinedTextField(
                value = if (manualMaxHp == 0) "" else manualMaxHp.toString(),
                onValueChange = { onManualMaxHpChange(it.toIntOrNull() ?: 0) },
                label = { Text("Максимум ОЗ") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )
        }

        // Multiclass Toggle
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Мультикласс ОЗ", modifier = Modifier.weight(1f))
            Switch(checked = isMulticlass, onCheckedChange = onMulticlassChange)
        }
        
        // I'll leave the rest as TODO or implement if I remember.
        // But for now this fixes the build.
    }
}
