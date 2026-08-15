package ru.quasaris.characternexus.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import ru.quasaris.characternexus.backend.ImageManager
import ru.quasaris.characternexus.backend.getNextLevelThreshold
import ru.quasaris.characternexus.backend.getPreviousLevelThreshold
import ru.quasaris.characternexus.CharacterSummary
import ru.quasaris.characternexus.platformFileSystem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CharacterCard(
    character: CharacterSummary,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    useOldAvatarStyle: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    val thumbPath = remember(character.imageData, character.uuid) {
        ImageManager.getThumbnailFile(character.imageData ?: "", character.uuid)
    }
    
    val thumbExists = remember(thumbPath) {
        platformFileSystem.exists(thumbPath)
    }

    val cardColor = if (isSelected) {
        colorScheme.primaryContainer
    } else {
        colorScheme.surfaceVariant
    }

    val levelStr = character.level.filter { it.isDigit() }.ifEmpty { "1" }
    val expStr = character.experience.filter { it.isDigit() }.ifEmpty { "0" }
    
    val exp = expStr.toLongOrNull() ?: 0L
    val prevThreshold = getPreviousLevelThreshold(levelStr).toLongOrNull() ?: 0L
    val nextThreshold = getNextLevelThreshold(levelStr).toLongOrNull() ?: 300L

    val progress = if (nextThreshold > prevThreshold) {
        ((exp - prevThreshold).toFloat() / (nextThreshold - prevThreshold).toFloat()).coerceIn(0f, 1f)
    } else 1f

    val currentHp = character.currentHp.toIntOrNull() ?: 0
    val maxHp = character.maxHp.toIntOrNull() ?: 1
    val tempHp = character.tempHp.toIntOrNull() ?: 0

    val baseHpColor = if (maxHp > 0 && currentHp.toFloat() / maxHp.toFloat() > 0.5f) {
        Color(0xFF4CAF50) // Green
    } else {
        Color(0xFFF44336) // Red
    }
    
    val hpColor = lerp(baseHpColor, colorScheme.onSurfaceVariant, 0.5f)

    val hpText = buildString {
        append("$currentHp/$maxHp")
        if (tempHp > 0) append(" (+$tempHp)")
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) BorderStroke(2.dp, colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val startPointPx = if (useOldAvatarStyle) 0f else {
                        val avatarSizeDp = 90.dp
                        val avatarOffsetDp = (-18).dp
                        (avatarOffsetDp + avatarSizeDp / 2).toPx()
                    }
                    drawRect(
                        color = colorScheme.primary.copy(alpha = 0.2f),
                        topLeft = androidx.compose.ui.geometry.Offset(startPointPx, 0f),
                        size = Size((size.width - startPointPx) * progress, size.height)
                    )
                }
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(if (useOldAvatarStyle) 12.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (useOldAvatarStyle) {
                    Box(
                        modifier = Modifier
                            .requiredSize(60.dp)
                            .clip(CircleShape)
                            .background(colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        if (thumbExists) {
                            AsyncImage(
                                model = thumbPath,
                                contentDescription = "Иконка",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                null,
                                modifier = Modifier.size(28.dp),
                                tint = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = character.name.ifEmpty { "Без имени" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface,
                            maxLines = 1
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Уровень ${character.level} • ${character.characterClass} • ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Text(
                                text = hpText,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = hpColor,
                                maxLines = 1
                            )
                        }
                    }
                } else {
                    val avatarSize = 90.dp
                    val avatarOffset = (-18).dp
                    val amplifiedSize = 90.dp
                    val imageOffset = 7.dp

                    Box(
                        modifier = Modifier
                            .requiredSize(avatarSize)
                            .offset(x = avatarOffset)
                            .clip(CircleShape)
                            .background(colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        if (thumbExists) {
                            AsyncImage(
                                model = thumbPath,
                                contentDescription = "Иконка",
                                modifier = Modifier
                                    .requiredSize(amplifiedSize)
                                    .offset(x = imageOffset),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                null,
                                modifier = Modifier.size(40.dp),
                                tint = colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp)
                            .padding(end = 12.dp)
                            .offset(x = (-8).dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = character.name.ifEmpty { "Без имени" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface,
                            maxLines = 1
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Уровень ${character.level} • ${character.characterClass} • ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Text(
                                text = hpText,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = hpColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
