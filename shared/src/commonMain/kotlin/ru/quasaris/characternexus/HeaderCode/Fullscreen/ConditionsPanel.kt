package ru.quasaris.characternexus.HeaderCode.Fullscreen

import ru.quasaris.characternexus.model.*
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import ru.quasaris.characternexus.ui.DialogDimStyle
import ru.quasaris.characternexus.ui.BackHandler
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
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        DialogDimStyle(0f)
        val colorScheme = MaterialTheme.colorScheme
        val isOled = colorScheme.background == Color.Black

    BackHandler(onBack = onDismiss)

    Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Состояния и Истощение", fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = if (forceBlurEnabled && !isOled) Color.Transparent.copy(alpha = 0.1f) else colorScheme.surface
                    )
                )
            },
            containerColor = if (forceBlurEnabled && !isOled) Color.Transparent.copy(alpha = 0.1f) else colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Exhaustion Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.errorContainer.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Уровень Истощения", color = colorScheme.error, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 16.dp)) {
                            IconButton(onClick = { if (exhaustion > 0) onExhaustionChange(exhaustion - 1) }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = colorScheme.error, modifier = Modifier.size(48.dp))
                            }
                            Text(exhaustion.toString(), color = colorScheme.error, fontSize = 72.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 32.dp))
                            IconButton(onClick = { if (exhaustion < 6) onExhaustionChange(exhaustion + 1) }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = colorScheme.error, modifier = Modifier.size(48.dp))
                            }
                        }
                        
                        if (exhaustion > 0) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 8.dp)) {
                                if (exhaustion == 6) Text("СМЕРТЬ", color = colorScheme.error, fontWeight = FontWeight.Black, fontSize = 20.sp)
                                Text("-${exhaustion * 2} к проверкам к20", color = colorScheme.onSurfaceVariant)
                                Text("-${exhaustion * 5} фт к скорости", color = colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                SectionHeader("Активные состояния")
                
                allConditions.forEach { condition ->
                    val isSelected = selectedConditions.contains(condition.name)
                    ConditionItem(condition, isSelected) { onToggleCondition(condition.name) }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ConditionItem(condition: Condition, isSelected: Boolean, onToggle: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) colorScheme.primaryContainer.copy(alpha = 0.5f) else colorScheme.surfaceVariant.copy(alpha = 0.3f)
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
