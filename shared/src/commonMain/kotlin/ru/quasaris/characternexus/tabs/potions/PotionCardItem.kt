package ru.quasaris.characternexus.tabs.potions

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.ui.outerShadow
import ru.quasaris.characternexus.tabs.spells.SpellDamageButton
import ru.quasaris.characternexus.tabs.resources.ResourceActionButton
import ru.quasaris.characternexus.tabs.resources.RestIndicator
import characternexus.shared.generated.resources.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import ru.quasaris.characternexus.ui.theme.rememberEffectiveBlurRadius
import ru.quasaris.characternexus.ui.theme.hazePopover
import ru.quasaris.characternexus.backend.SettingsViewModel
import ru.quasaris.characternexus.backend.evaluateFormulaDouble
import ru.quasaris.characternexus.backend.getProficiencyBonus
import ru.quasaris.characternexus.util.HapticType
import ru.quasaris.characternexus.util.PlatformUtils
import kotlin.math.round
import kotlin.math.pow

@Composable
fun PotionCardItem(
    potion: PotionState,
    onUpdate: (PotionState) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onRoll: (String, String, AdvantageType) -> Unit,
    isEditMode: Boolean = false,
    isDragging: Boolean = false,
    isAnyItemDragging: Boolean = false,
    statsMap: Map<String, String> = emptyMap(),
    hazeState: HazeState? = null,
    popupHazeState: HazeState? = null,
    blurCards: Boolean = true,
    settingsViewModel: SettingsViewModel? = null,
    dragModifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    var isExpanded by remember { mutableStateOf(false) }
    val blurRadius = rememberEffectiveBlurRadius(settingsViewModel)
    
    val scale by androidx.compose.animation.core.animateFloatAsState(targetValue = when {
        isDragging -> 1.02f
        isEditMode -> 0.95f
        else -> 1f
    })
    val backgroundBlur by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isAnyItemDragging && !isDragging) 6.dp else 0.dp,
        label = "backgroundBlur"
    )

    val useHaze = hazeState != null && blurCards
    
    val potionColor = remember(potion.colorHex) {
        try {
            val hex = potion.colorHex.removePrefix("#")
            when (hex.length) {
                6 -> Color(
                    red = hex.substring(0, 2).toInt(16),
                    green = hex.substring(2, 4).toInt(16),
                    blue = hex.substring(4, 6).toInt(16)
                )
                8 -> Color(
                    red = hex.substring(0, 2).toInt(16),
                    green = hex.substring(2, 4).toInt(16),
                    blue = hex.substring(4, 6).toInt(16),
                    alpha = hex.substring(6, 8).toInt(16)
                )
                else -> Color.Gray
            }
        } catch (e: Exception) {
            Color.Gray
        }
    }

    val iconRes = when (potion.iconIndex) {
        1 -> Res.drawable.small_potion
        2 -> Res.drawable.potion
        3 -> Res.drawable.big_potion
        4 -> Res.drawable.huge_potion
        else -> Res.drawable.small_potion
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .then(
                if (backgroundBlur > 0.dp) 
                    Modifier.blur(backgroundBlur) 
                else Modifier
            )
            .outerShadow(
                shape = RoundedCornerShape(16.dp),
                blur = if (isDragging) 6.dp else 2.dp,
                offsetY = if (isDragging) 3.dp else 1.dp
            )
            .clip(RoundedCornerShape(16.dp))
            .run {
                if (useHaze) {
                    this.hazePopover(
                        state = hazeState!!,
                        blurRadius = blurRadius,
                        isOled = colorScheme.background == Color.Black
                    )
                } else this
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!isEditMode) isExpanded = !isExpanded
            },
        shape = RoundedCornerShape(16.dp),
        color = if (useHaze) Color.Transparent else colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Line 1: Icon and Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isEditMode) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Перетащить",
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(24.dp)
                            .then(dragModifier),
                        tint = colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }

                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = potionColor
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = potion.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                if (isEditMode) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Редактировать", modifier = Modifier.size(18.dp), tint = colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить", modifier = Modifier.size(18.dp), tint = colorScheme.error)
                        }
                    }
                }
            }

            if (!isEditMode) {
                // Line 2: Formula
                if (potion.formula.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val mockSpell = SpellCard(
                        name = potion.name,
                        damageFormula = potion.formula,
                        damageTypes = potion.damageTypes
                    )
                    SpellDamageButton(
                        spell = mockSpell,
                        formula = potion.formula,
                        damageTypes = potion.damageTypes,
                        statsMap = statsMap,
                        title = potion.name,
                        onRoll = onRoll,
                        hazeState = hazeState,
                        popupHazeState = popupHazeState,
                        settingsViewModel = settingsViewModel
                    )
                }

                // Line 3: Quantity Counter
                Spacer(modifier = Modifier.height(8.dp))
                PotionCounterUI(
                    resource = potion.quantity,
                    onUpdate = { res -> onUpdate(potion.copy(quantity = res)) },
                    statsMap = statsMap,
                    settingsViewModel = settingsViewModel
                )
            }

            AnimatedVisibility(
                visible = isExpanded && !isEditMode,
                enter = expandIn(expandFrom = Alignment.TopStart) + fadeIn(),
                exit = shrinkOut(shrinkTowards = Alignment.TopStart) + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = potion.description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            lineHeight = 20.sp
                        ),
                        color = colorScheme.onSurface
                    )
                    if (potion.sourceModuleId != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Редкость: ${potion.rarity.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PotionCounterUI(
    resource: DynamicContentBlock.Resource,
    onUpdate: (DynamicContentBlock.Resource) -> Unit,
    statsMap: Map<String, String>,
    settingsViewModel: SettingsViewModel? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val veryResponsive by settingsViewModel?.veryResponsiveHaptics?.collectAsState() ?: remember { mutableStateOf(true) }
    
    fun performClickHaptic() {
        if (veryResponsive) {
            PlatformUtils.performHapticFeedback(HapticType.CLICK)
        }
    }

    val curValue = resource.current.toDoubleOrNull() ?: 0.0
    val maxValue = evaluateFormulaDouble(resource.max, statsMap)
    val hasMax = resource.max != "0" && resource.max.lowercase() != "null" && resource.max.isNotEmpty()

    val canIncrement = !hasMax || curValue < maxValue
    val canDecrement = curValue > 0

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

    val level = statsMap["level"] ?: "1"
    val pb = getProficiencyBonus(level)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (!resource.useSlider) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Rests info on the left
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (resource.shortRest != "0" && resource.shortRest.isNotEmpty()) {
                        RestIndicator(isShort = true, value = resource.shortRest, statsMap = statsMap, proficiencyBonus = pb)
                    }
                    if (resource.longRest != "0" && resource.longRest.isNotEmpty()) {
                        RestIndicator(isShort = false, value = resource.longRest, statsMap = statsMap, proficiencyBonus = pb)
                    }
                    if (resource.dawnRest != "0" && resource.dawnRest.isNotEmpty()) {
                        RestIndicator(isShort = false, isDawn = true, value = resource.dawnRest, statsMap = statsMap, proficiencyBonus = pb)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                    PotionValueDisplay(curValue, maxValue, resource.max, formatValue = { formatValue(it) })

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
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Rests
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (resource.shortRest != "0" && resource.shortRest.isNotEmpty()) {
                            RestIndicator(isShort = true, value = resource.shortRest, statsMap = statsMap, proficiencyBonus = pb)
                        }
                        if (resource.longRest != "0" && resource.longRest.isNotEmpty()) {
                            RestIndicator(isShort = false, value = resource.longRest, statsMap = statsMap, proficiencyBonus = pb)
                        }
                        if (resource.dawnRest != "0" && resource.dawnRest.isNotEmpty()) {
                            RestIndicator(isShort = false, isDawn = true, value = resource.dawnRest, statsMap = statsMap, proficiencyBonus = pb)
                        }
                    }

                    PotionValueDisplay(curValue, maxValue, resource.max, formatValue = { formatValue(it) })
                }

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
                            val maxLimit = if (hasMax) maxValue else curValue
                            val rawValue = it.toDouble().coerceIn(0.0, maxLimit)
                            val diff = maxLimit - rawValue
                            val snappedDiff = round(diff / step) * step
                            val snappedValue = (maxLimit - snappedDiff).coerceIn(0.0, maxLimit)
                            
                            val updatedResource = resource.copy(current = formatValue(snappedValue))
                            if (updatedResource.current != resource.current) {
                                performClickHaptic()
                            }
                            onUpdate(updatedResource)
                        },
                        valueRange = 0f..if (hasMax) maxValue.toFloat().coerceAtLeast(0.001f) else curValue.toFloat().coerceAtLeast(0.001f),
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
            }
        }
    }
}

@Composable
private fun PotionValueDisplay(
    curValue: Double,
    maxValue: Double,
    maxFormula: String,
    formatValue: (Double) -> String
) {
    val hasMax = maxFormula != "0" && maxFormula.lowercase() != "null" && maxFormula.isNotEmpty()
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
            text = if (hasMax) "${formatValue(curValue)}/${formatValue(maxValue)}" else formatValue(curValue),
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = colorScheme.primary,
                textAlign = TextAlign.Center
            )
        )
    }
}
