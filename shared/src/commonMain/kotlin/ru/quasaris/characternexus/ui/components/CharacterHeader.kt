package ru.quasaris.characternexus.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import ru.quasaris.characternexus.util.HapticType
import ru.quasaris.characternexus.util.PlatformUtils
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.chrisbanes.haze.HazeState
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import ru.quasaris.characternexus.backend.getPreviousLevelThreshold
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.ui.RestPopup

@Composable
fun CharacterHeader(
    name: String,
    onNameChange: (String) -> Unit,
    level: String,
    experience: String,
    nextLevelExp: String,
    characterImageData: Any?,
    onAvatarClick: () -> Unit,
    onLevelClick: () -> Unit,
    onOpenDrawer: () -> Unit,
    activeACValue: String,
    onACClick: () -> Unit,
    onACLongClick: () -> Unit,
    isShieldActive: Boolean,
    activeInitValue: String,
    onInitClick: () -> Unit,
    onInitLongClick: () -> Unit = {},
    currentHp: String,
    maxHp: String,
    tempHp: String,
    healthColor: Color,
    healthIcon: DrawableResource,
    onHealthClick: () -> Unit,
    conditionsCount: String,
    selectedConditions: List<String>,
    onConditionsClick: () -> Unit,
    activeSpeedValue: String,
    onSpeedClick: () -> Unit,
    showAvatarMenu: Boolean,
    onDismissAvatarMenu: () -> Unit,
    onImagePickerClick: () -> Unit,
    onDownloadClick: (String) -> Unit,
    onDeletePortraitClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    exhaustion: Int,
    hasInspiration: Boolean = false,
    onInspirationChange: (Boolean) -> Unit = {},
    onShortRest: () -> Unit = {},
    onLongRest: () -> Unit = {},
    onDawn: () -> Unit = {},
    hazeState: HazeState? = null,
    // Resource placeholders (should be replaced with actual Res.drawable.*)
    inspirationIcon: DrawableResource,
    shieldIcon: DrawableResource,
    swordIcon: DrawableResource,
    conditionsIcon: DrawableResource,
    speedIcon: DrawableResource
) {
    val colorScheme = MaterialTheme.colorScheme

    val inspirationRotation by animateFloatAsState(if (hasInspiration) 0f else 45f)
    val inspirationScale by animateFloatAsState(if (hasInspiration) 1f else 0.8f)
    val isOled = colorScheme.background == Color.Black

    var totalDrag by remember { mutableStateOf(0f) }
    var showRestPopup by remember { mutableStateOf(false) }

    val panelsSpringSpec = remember {
        spring<IntSize>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
    }

    Column(
        modifier = Modifier
            .background(colorScheme.surface)
            .animateContentSize(animationSpec = panelsSpringSpec)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount
                    },
                    onDragEnd = {
                        if (totalDrag > 150) {
                            PlatformUtils.performHapticFeedback(HapticType.LONG_PRESS)
                            onOpenDrawer()
                        }
                    }
                )
            }
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                PlatformUtils.performHapticFeedback(HapticType.CLICK)
                onOpenDrawer()
            }) { Icon(Icons.Default.Menu, null, modifier = Modifier.size(32.dp), tint = colorScheme.onSurface) }
            
            Box(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = name, onValueChange = onNameChange,
                    textStyle = TextStyle(fontSize = 22.sp, textAlign = TextAlign.Start, color = colorScheme.onSurface, fontWeight = FontWeight.Bold),
                    cursorBrush = SolidColor(colorScheme.primary),
                    decorationBox = { innerTextField -> if (name.isEmpty()) Text("Имя персонажа", fontSize = 22.sp, textAlign = TextAlign.Start, color = colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()); innerTextField() },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Inspiration
                IconButton(
                    onClick = {
                        PlatformUtils.performHapticFeedback(HapticType.CLICK)
                        onInspirationChange(!hasInspiration)
                    },
                    modifier = Modifier
                        .scale(inspirationScale)
                        .rotate(inspirationRotation)
                ) {
                    Icon(
                        painter = painterResource(inspirationIcon),
                        contentDescription = "Героическое вдохновение",
                        modifier = Modifier.size(32.dp),
                        tint = if (hasInspiration) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }

                // Rest
                Box(contentAlignment = Alignment.Center) {
                    IconButton(onClick = { showRestPopup = !showRestPopup }) {
                        Icon(Icons.Default.WbSunny, null, modifier = Modifier.size(32.dp), tint = colorScheme.primary)
                    }

                    if (showRestPopup) {
                        RestPopup(
                            onShortRest = { onShortRest() },
                            onLongRest = { onLongRest() },
                            onDawn = { onDawn() },
                            onDismiss = { showRestPopup = false },
                            hazeState = hazeState,
                            isOled = isOled
                        )
                    }
                }

                // Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable {
                            PlatformUtils.performHapticFeedback(HapticType.CLICK)
                            onAvatarClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (characterImageData != null) {
                            AsyncImage(
                                model = characterImageData,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, null, tint = colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        }
        
        // Progress Bar
        Box(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable { 
                PlatformUtils.performHapticFeedback(HapticType.CLICK)
                onLevelClick() 
            }
        ) {
            val pr = remember(experience, nextLevelExp, level) {
                val currentXp = experience.toFloatOrNull() ?: 0f
                val nextXp = nextLevelExp.toFloatOrNull() ?: 0f
                val prevLevelXp = getPreviousLevelThreshold(level).toFloatOrNull() ?: 0f
                val totalNeededForLevel = nextXp - prevLevelXp
                val progressInLevel = currentXp - prevLevelXp
                if (totalNeededForLevel <= 0f) 1f else (progressInLevel / totalNeededForLevel).coerceIn(0f, 1f)
            }
            
            Box(modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(pr)
                .background(colorScheme.primary.copy(alpha = 0.2f))
            )
            
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), 
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$level Уровень",
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Black,
                    color = colorScheme.primary
                )
                
                val xpText = if ((level.toIntOrNull() ?: 0) >= 20) "$experience Опыта" else "$experience / $nextLevelExp Опыта"
                Text(
                    text = xpText,
                    fontSize = 11.sp, 
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurfaceVariant
                )
                
                val nxt = (level.toIntOrNull() ?: 0) + 1
                if ((level.toIntOrNull() ?: 0) < 20) {
                    Text(
                        text = "$nxt", 
                        fontSize = 12.sp, 
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.outline
                    )
                }
            }
        }

        // Stats Row
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatIconBox(activeACValue, shieldIcon, onClick = onACClick, onLongClick = onACLongClick, isHighlighted = isShieldActive)
                StatIconBox(activeInitValue, swordIcon, onClick = onInitClick, onLongClick = onInitLongClick, isHighlighted = true)
            }
            Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp).height(55.dp).border(1.5.dp, healthColor, RoundedCornerShape(8.dp)).background(colorScheme.surface, RoundedCornerShape(8.dp)).clickable { 
                PlatformUtils.performHapticFeedback(HapticType.CLICK)
                onHealthClick() 
            }, contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(healthIcon), null, modifier = Modifier.size(32.dp), colorFilter = ColorFilter.tint(healthColor))
                    Spacer(Modifier.width(6.dp)); Text("$currentHp / ${maxHp.toIntOrNull() ?: 0}", color = healthColor, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    if ((tempHp.toIntOrNull() ?: 0) > 0) Text(" (+$tempHp)", color = healthColor.copy(alpha = 0.7f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatIconBox(
                    value = if (conditionsCount == "0" || conditionsCount.isEmpty()) "" else conditionsCount,
                    icon = conditionsIcon,
                    onClick = onConditionsClick,
                    isHighlighted = selectedConditions.isNotEmpty()
                )
                StatIconBox(activeSpeedValue, speedIcon, onClick = onSpeedClick, isHighlighted = true)
            }
        }
    }
}
