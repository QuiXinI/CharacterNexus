package ru.quasaris.characternexus.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.backend.evaluateFormula
import ru.quasaris.characternexus.backend.evaluateFormulaDouble
import ru.quasaris.characternexus.backend.getProficiencyBonus
import ru.quasaris.characternexus.tabs.attacks.DiceIcon
import ru.quasaris.characternexus.backend.parseFormulaParts
import ru.quasaris.characternexus.ui.outerShadow
import ru.quasaris.characternexus.util.HapticType
import ru.quasaris.characternexus.util.PlatformUtils
import kotlin.math.round
import kotlin.math.pow
import kotlin.math.max

@Composable
fun ResourceBlock(
    resource: DynamicContentBlock.Resource,
    statsMap: Map<String, String>,
    onUpdate: (DynamicContentBlock.Resource) -> Unit,
    hazeState: HazeState? = null,
    onDeleteRequest: () -> Unit,
    forceBlurEnabled: Boolean = false,
    blurDynamicFields: Boolean = true,
    blurPopups: Boolean = false,
    settingsViewModel: ru.quasaris.characternexus.backend.SettingsViewModel? = null,
    onFullscreenDialogOpenChange: (Boolean) -> Unit = {},
    onSubDialogOpenChange: (Boolean) -> Unit = {}
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    var showConfig by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var infoIconPosition by remember { mutableStateOf(Offset.Zero) }

    val curValue = resource.current.toDoubleOrNull() ?: 0.0
    val maxValue = evaluateFormulaDouble(resource.max, statsMap)

    val level = statsMap["level"] ?: "1"
    val pb = getProficiencyBonus(level)

    val canIncrement = curValue < maxValue || resource.max == "0"
    val canDecrement = curValue > 0
    
    val useHaze = hazeState != null && blurDynamicFields

    fun formatValue(value: Double, step: Double? = resource.sliderStep): String {
        val actualStep = step ?: 1.0
        val stepStr = actualStep.toString()
        val precision = if (stepStr.contains('.')) {
            val decimals = stepStr.substringAfter('.')
            if (decimals == "0") 0 else decimals.length
        } else 0
        
        val factor = 10.0.pow(precision)
        val rounded = round(value * factor) / factor
        return if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            val s = rounded.toString()
            if (s.contains('.')) {
                val parts = s.split('.')
                val decimals = parts[1].take(precision)
                if (decimals.isEmpty() || decimals.all { it == '0' }) parts[0]
                else "${parts[0]}.${decimals.trimEnd('0')}"
            } else s
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    val veryResponsive by settingsViewModel?.veryResponsiveHaptics?.collectAsState() ?: remember { mutableStateOf(true) }
    
    fun performClickHaptic() {
        if (veryResponsive) {
            PlatformUtils.performHapticFeedback(HapticType.CLICK)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .outerShadow(
                shape = RoundedCornerShape(16.dp),
                blur = 2.dp,
                offsetY = 1.dp
            )
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.surfaceVariant.copy(alpha = if (useHaze) 0.6f else 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                performClickHaptic()
                showConfig = true
            }
            .padding(12.dp)
    ) {
        if (!resource.useSlider) {
            // Original layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = resource.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )

                    // Rests info
                    ResourceRestsInfo(resource, statsMap, pb)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ResourceActionButtons(
                        resource = resource,
                        showInfo = { showInfo = true },
                        onInfoPos = { infoIconPosition = it },
                        uriHandler = uriHandler
                    )

                    // Minus button
                    ResourceActionButton(
                        icon = Icons.Default.Remove,
                        enabled = canDecrement,
                        onClick = { 
                            performClickHaptic()
                            val step = resource.sliderStep ?: 1.0
                            onUpdate(resource.copy(current = formatValue((curValue - step).coerceAtLeast(0.0)))) 
                        }
                    )

                    // Current/Max display
                    ResourceValueDisplay(curValue, maxValue, resource.max, formatValue = { formatValue(it) })

                    // Plus button
                    ResourceActionButton(
                        icon = Icons.Default.Add,
                        enabled = canIncrement,
                        onClick = { 
                            performClickHaptic()
                            val step = resource.sliderStep ?: 1.0
                            onUpdate(resource.copy(current = formatValue(curValue + step))) 
                        }
                    )
                }
            }
        } else {
            // Slider layout
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Top row: Name and Info/Link/Value
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = resource.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ResourceActionButtons(
                            resource = resource,
                            showInfo = { showInfo = true },
                            onInfoPos = { infoIconPosition = it },
                            uriHandler = uriHandler
                        )

                        ResourceValueDisplay(curValue, maxValue, resource.max, formatValue = { formatValue(it) })
                    }
                }

                // Middle row: Slider with buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ResourceActionButton(
                        icon = Icons.Default.Remove,
                        enabled = canDecrement,
                        onClick = { 
                            performClickHaptic()
                            val step = resource.sliderStep ?: 1.0
                            onUpdate(resource.copy(current = formatValue((curValue - step).coerceAtLeast(0.0)))) 
                        }
                    )

                    Slider(
                        value = curValue.toFloat(),
                        onValueChange = { 
                            val step = resource.sliderStep ?: 1.0
                            val max = maxValue
                            val rawValue = it.toDouble().coerceIn(0.0, max)
                            
                            // Alignment from maximum:
                            // We want values like: max, max - step, max - 2*step, ...
                            val diff = max - rawValue
                            val snappedDiff = round(diff / step) * step
                            val snappedValue = (max - snappedDiff).coerceIn(0.0, max)
                            
                            val updatedResource = resource.copy(current = formatValue(snappedValue))
                            if (updatedResource.current != resource.current) {
                                performClickHaptic()
                            }
                            onUpdate(updatedResource)
                        },
                        valueRange = 0f..maxValue.toFloat().coerceAtLeast(0.001f),
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = colorScheme.primary,
                            activeTrackColor = colorScheme.primary,
                            inactiveTrackColor = colorScheme.primary.copy(alpha = 0.2f)
                        )
                    )

                    ResourceActionButton(
                        icon = Icons.Default.Add,
                        enabled = canIncrement,
                        onClick = { 
                            performClickHaptic()
                            val step = resource.sliderStep ?: 1.0
                            onUpdate(resource.copy(current = formatValue(curValue + step))) 
                        }
                    )
                }

                // Bottom row: Rests
                ResourceRestsInfo(resource, statsMap, pb)
            }
        }
    }

    val currentOnFullscreenDialogOpenChange by rememberUpdatedState(onFullscreenDialogOpenChange)
    val currentOnSubDialogOpenChange by rememberUpdatedState(onSubDialogOpenChange)

    if (showConfig) {
        ResourceConfigDialog(
            resource = resource,
            onDismiss = {
                showConfig = false
            },
            onSave = {
                onUpdate(it)
                showConfig = false
            },
            onDelete = {
                onDeleteRequest()
                showConfig = false
            },
            forceBlurEnabled = forceBlurEnabled,
            settingsViewModel = settingsViewModel,
            onFullscreenDialogOpenChange = { opened ->
                currentOnFullscreenDialogOpenChange(opened)
                currentOnSubDialogOpenChange(opened)
            }
        )
    }

    if (showInfo) {
        ResourceInfoPopover(
            title = resource.name,
            notes = resource.notes,
            anchorPosition = infoIconPosition,
            onDismiss = { showInfo = false },
            hazeState = hazeState,
            forceBlurEnabled = blurPopups
        )
    }
}

@Composable
private fun ResourceRestsInfo(
    resource: DynamicContentBlock.Resource,
    statsMap: Map<String, String>,
    pb: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        if (resource.shortRest != "0" && resource.shortRest.isNotEmpty()) {
            RestIndicator(
                isShort = true,
                value = resource.shortRest,
                statsMap = statsMap,
                proficiencyBonus = pb
            )
        }
        if (resource.longRest != "0" && resource.longRest.isNotEmpty()) {
            RestIndicator(
                isShort = false,
                value = resource.longRest,
                statsMap = statsMap,
                proficiencyBonus = pb
            )
        }
        if (resource.dawnRest != "0" && resource.dawnRest.isNotEmpty()) {
            RestIndicator(
                isShort = false,
                isDawn = true,
                value = resource.dawnRest,
                statsMap = statsMap,
                proficiencyBonus = pb
            )
        }
    }
}

@Composable
private fun ResourceActionButtons(
    resource: DynamicContentBlock.Resource,
    showInfo: () -> Unit,
    onInfoPos: (Offset) -> Unit,
    uriHandler: androidx.compose.ui.platform.UriHandler
) {
    val colorScheme = MaterialTheme.colorScheme
    // Info button
    if (resource.notes.isNotEmpty() && resource.showNotes) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .outerShadow(
                    shape = RoundedCornerShape(10.dp),
                    blur = 2.dp,
                    offsetY = 1.dp
                )
                .clip(RoundedCornerShape(10.dp))
                .background(colorScheme.primary.copy(alpha = 0.12f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = showInfo
                )
                .onGloballyPositioned { coords ->
                    onInfoPos(coords.positionInWindow())
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = "Info",
                tint = colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }

    // Link button
    if (!resource.link.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .outerShadow(
                    shape = RoundedCornerShape(10.dp),
                    blur = 2.dp,
                    offsetY = 1.dp
                )
                .clip(RoundedCornerShape(10.dp))
                .background(colorScheme.primary.copy(alpha = 0.12f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        try {
                            val trimmedLink = resource.link.trim()
                            val formattedLink = if (!trimmedLink.startsWith("http://") && !trimmedLink.startsWith("https://")) {
                                "https://$trimmedLink"
                            } else {
                                trimmedLink
                            }
                            uriHandler.openUri(formattedLink)
                        } catch (e: Exception) {
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Link,
                contentDescription = "Link",
                tint = colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ResourceValueDisplay(
    curValue: Double,
    maxValue: Double,
    maxFormula: String,
    formatValue: (Double) -> String
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(height = 36.dp, width = 64.dp)
            .outerShadow(
                shape = RoundedCornerShape(10.dp),
                blur = 2.dp,
                offsetY = 1.dp
            )
            .clip(RoundedCornerShape(10.dp))
            .background(colorScheme.primary.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (maxFormula != "0") "${formatValue(curValue)}/${formatValue(maxValue)}" else formatValue(curValue),
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = colorScheme.primary,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
fun ResourceActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(36.dp)
            .outerShadow(
                shape = RoundedCornerShape(10.dp),
                blur = 2.dp,
                offsetY = 1.dp
            )
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) colorScheme.primary.copy(alpha = 0.12f) else colorScheme.onSurface.copy(alpha = 0.05f))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (enabled) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.3f)
        )
    }
}

@Composable
fun RestIndicator(
    isShort: Boolean,
    isDawn: Boolean = false,
    value: String,
    statsMap: Map<String, String>,
    proficiencyBonus: Int
) {
    val colorScheme = MaterialTheme.colorScheme
    val icon = when {
        isDawn -> Icons.Outlined.WbTwilight
        isShort -> Icons.Outlined.WbSunny
        else -> Icons.Outlined.NightsStay
    }
    val color = when {
        isDawn -> Color(0xFFCE93D8)
        isShort -> Color(0xFFFFB300)
        else -> Color(0xFF42A5F5)
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = color
        )
        if (value.lowercase() != "all" && value.lowercase() != "все") {
            val (flat, dice) = parseFormulaParts(value, stats = statsMap)
            if (dice.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dice.forEach { die ->
                        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                            DiceIcon(die)
                        }
                    }
                    if (flat != 0) {
                        Text(
                            text = if (flat > 0) "+$flat" else flat.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = colorScheme.onSurface
                        )
                    }
                }
            } else {
                val evaluatedValue = evaluateFormula(value, statsMap)
                Text(
                    text = evaluatedValue.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.onSurface
                )
            }
        }
    }
}
