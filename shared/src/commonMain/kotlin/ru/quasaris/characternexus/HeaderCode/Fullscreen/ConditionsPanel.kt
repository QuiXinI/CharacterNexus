package ru.quasaris.characternexus.HeaderCode.Fullscreen

import dev.chrisbanes.haze.*
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import ru.quasaris.characternexus.ui.DialogDimStyle
import ru.quasaris.characternexus.ui.BackHandler
import ru.quasaris.characternexus.model.Condition
import ru.quasaris.characternexus.ui.outerShadow
import ru.quasaris.characternexus.ui.theme.rememberEffectiveBlurRadius
import ru.quasaris.characternexus.ui.util.formatConditionDescription
import ru.quasaris.characternexus.tabs.attacks.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConditionsDialog(
    allConditions: List<Condition>,
    selectedConditions: List<String>,
    onToggleCondition: (String) -> Unit,
    exhaustion: Int,
    onExhaustionChange: (Int) -> Unit,
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
        ConditionsDialogContent(
            allConditions = allConditions,
            selectedConditions = selectedConditions,
            onToggleCondition = onToggleCondition,
            exhaustion = exhaustion,
            onExhaustionChange = onExhaustionChange,
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
            ConditionsDialogContent(
                allConditions = allConditions,
                selectedConditions = selectedConditions,
                onToggleCondition = onToggleCondition,
                exhaustion = exhaustion,
                onExhaustionChange = onExhaustionChange,
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
fun ConditionsDialogContent(
    allConditions: List<Condition>,
    selectedConditions: List<String>,
    onToggleCondition: (String) -> Unit,
    exhaustion: Int,
    onExhaustionChange: (Int) -> Unit,
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
                title = { Text("Состояния и Истощение", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = if (forceBlurEnabled && !isOled && hazeState != null) Color.Transparent.copy(alpha = 0.1f) else colorScheme.surface
                )
            )
        },
        containerColor = if (forceBlurEnabled && !isOled && hazeState != null) Color.Transparent.copy(alpha = 0.1f) else colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Exhaustion Section
                val isDark = colorScheme.background.red + colorScheme.background.green + colorScheme.background.blue < 1.5f
                val startColor = if (isDark) Color(0xFF220707) else Color(0xFFFFF5F5)
                val endColor = if (isDark) Color(0xFF700404) else Color(0xFFFFCCCC)

                val cardColor = when {
                    exhaustion == 0 -> colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    else -> {
                        val progress = ((exhaustion - 1) / 5f).coerceIn(0f, 1f)
                        androidx.compose.ui.graphics.lerp(startColor, endColor, progress)
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .outerShadow(RoundedCornerShape(24.dp), blur = 8.dp, offsetY = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Уровень Истощения", color = if (exhaustion > 0) colorScheme.onErrorContainer else colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 16.dp)) {
                            IconButton(onClick = { if (exhaustion > 0) onExhaustionChange(exhaustion - 1) }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowLeft, 
                                    null, 
                                    tint = if (exhaustion > 0) colorScheme.error else colorScheme.onSurfaceVariant.copy(alpha = 0.3f), 
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                            Text(
                                exhaustion.toString(), 
                                color = if (exhaustion > 0) colorScheme.error else colorScheme.onSurfaceVariant, 
                                fontSize = 72.sp, 
                                fontWeight = FontWeight.Black, 
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                            IconButton(onClick = { onExhaustionChange(exhaustion + 1) }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight, 
                                    null, 
                                    tint = if (exhaustion >= 6) colorScheme.error.copy(alpha = 0.3f) else colorScheme.error, 
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                        
                        if (exhaustion > 0) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 8.dp)) {
                                if (exhaustion >= 6) Text("СМЕРТЬ", color = colorScheme.error, fontWeight = FontWeight.Black, fontSize = 20.sp)
                                Text("-${exhaustion * 2} к проверкам к20", color = if (exhaustion >= 6) colorScheme.error else colorScheme.onSurfaceVariant)
                                Text("-${exhaustion * 5} фт к скорости", color = if (exhaustion >= 6) colorScheme.error else colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                SectionHeader("Активные состояния")
                
                allConditions.forEach { condition ->
                    val isSelected = selectedConditions.contains(condition.name)
                    ConditionItem(condition, isSelected) { onToggleCondition(condition.name) }
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

@Composable
fun ConditionItem(condition: Condition, isSelected: Boolean, onToggle: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .outerShadow(RoundedCornerShape(12.dp), blur = 4.dp, offsetY = 2.dp)
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) colorScheme.primaryContainer.copy(alpha = 0.5f) else colorScheme.surfaceVariant.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isSelected, onCheckedChange = { onToggle() })
                Text(
                    condition.name, 
                    modifier = Modifier.weight(1f).padding(start = 8.dp), 
                    fontSize = 18.sp, 
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) colorScheme.primary else colorScheme.onSurface
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint = colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            
            AnimatedVisibility(visible = expanded) {
                Text(
                    formatConditionDescription(condition.description, colorScheme.primary),
                    modifier = Modifier.padding(start = 48.dp, top = 8.dp, end = 8.dp),
                    fontSize = 16.sp,
                    color = colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}
