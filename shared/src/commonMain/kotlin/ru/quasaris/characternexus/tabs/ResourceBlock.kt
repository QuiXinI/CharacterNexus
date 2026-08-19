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
import ru.quasaris.characternexus.backend.getProficiencyBonus
import ru.quasaris.characternexus.tabs.attacks.DiceIcon
import ru.quasaris.characternexus.backend.parseFormulaParts
import ru.quasaris.characternexus.ui.outerShadow

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
    onFullscreenDialogOpenChange: (Boolean) -> Unit = {}
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    var showConfig by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var infoIconPosition by remember { mutableStateOf(Offset.Zero) }

    val curValue = resource.current.toIntOrNull() ?: 0
    val maxValue = evaluateFormula(resource.max, statsMap)

    val level = statsMap["level"] ?: "1"
    val pb = getProficiencyBonus(level)

    val canIncrement = curValue < maxValue || resource.max == "0"
    val canDecrement = curValue > 0
    
    val useHaze = hazeState != null && blurDynamicFields

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
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (useHaze) 0.6f else 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showConfig = true
            }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = resource.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Rests info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    if (resource.shortRest != "0") {
                        RestIndicator(
                            isShort = true,
                            value = resource.shortRest,
                            statsMap = statsMap,
                            proficiencyBonus = pb
                        )
                    }
                    if (resource.longRest != "0") {
                        RestIndicator(
                            isShort = false,
                            value = resource.longRest,
                            statsMap = statsMap,
                            proficiencyBonus = pb
                        )
                    }
                    if (resource.dawnRest != "0") {
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Info button
                if (resource.notes.isNotEmpty() && resource.showNotes) {
                    IconButton(
                        onClick = { showInfo = true },
                        modifier = Modifier
                            .size(36.dp)
                            .onGloballyPositioned { coords ->
                                infoIconPosition = coords.positionInWindow()
                            }
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Info",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Link button
                if (!resource.link.isNullOrBlank()) {
                    IconButton(
                        onClick = {
                            try {
                                val trimmedLink = resource.link.trim()
                                // Automatic link scheming
                                val formattedLink = if (!trimmedLink.startsWith("http://") && !trimmedLink.startsWith("https://")) {
                                    "https://$trimmedLink"
                                } else {
                                    trimmedLink
                                }
                                uriHandler.openUri(formattedLink)
                            } catch (e: Exception) {
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = "Link",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Minus button
                ResourceActionButton(
                    icon = Icons.Default.Remove,
                    enabled = canDecrement,
                    onClick = { onUpdate(resource.copy(current = (curValue - 1).coerceAtLeast(0).toString())) }
                )

                // Current/Max display (Square)
                Box(
                    modifier = Modifier
                        .size(height = 42.dp, width = 64.dp) // Adjusted to be more "square-ish" but fit numbers
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (resource.max != "0") "$curValue/$maxValue" else curValue.toString(),
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    )
                }

                // Plus button
                ResourceActionButton(
                    icon = Icons.Default.Add,
                    enabled = canIncrement,
                    onClick = { onUpdate(resource.copy(current = (curValue + 1).toString())) }
                )
            }
        }
    }

    if (showConfig) {
        ResourceConfigDialog(
            resource = resource,
            onDismiss = {
                showConfig = false
                onFullscreenDialogOpenChange(false)
            },
            onSave = {
                onUpdate(it)
                showConfig = false
                onFullscreenDialogOpenChange(false)
            },
            onDelete = {
                onDeleteRequest()
                showConfig = false
                onFullscreenDialogOpenChange(false)
            },
            forceBlurEnabled = forceBlurEnabled,
            settingsViewModel = settingsViewModel,
            onFullscreenDialogOpenChange = onFullscreenDialogOpenChange
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
fun ResourceActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(36.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
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
                            color = color.copy(alpha = 0.9f)
                        )
                    }
                }
            } else {
                val evaluatedValue = evaluateFormula(value, statsMap)
                Text(
                    text = evaluatedValue.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = color.copy(alpha = 0.9f)
                )
            }
        }
    }
}
