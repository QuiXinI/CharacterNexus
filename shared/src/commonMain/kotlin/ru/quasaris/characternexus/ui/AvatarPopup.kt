package ru.quasaris.characternexus.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import ru.quasaris.characternexus.backend.AppScaleProvider
import ru.quasaris.characternexus.backend.LocalAppScale

@Composable
fun AvatarPopup(
    hasImage: Boolean,
    onSettingsClick: () -> Unit,
    onImagePickerClick: () -> Unit,
    onExportSheetClick: () -> Unit,
    onExportPortraitClick: () -> Unit = {},
    onDeleteClick: () -> Unit,
    onDismiss: () -> Unit,
    hazeState: HazeState? = null,
    isOled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val offsetX = with(density) { (-180).dp.roundToPx() }
    val offsetY = with(density) { (40).dp.roundToPx() }

    Popup(
        onDismissRequest = onDismiss,
        offset = androidx.compose.ui.unit.IntOffset(x = offsetX, y = offsetY),
        properties = PopupProperties(focusable = true)
    ) {
        AppScaleProvider(LocalAppScale.current) {
            val colorScheme = MaterialTheme.colorScheme

            Surface(
                modifier = modifier
                    .width(220.dp)
                    .outerShadow(RoundedCornerShape(12.dp), blur = 8.dp)
                    .run {
                        if (hazeState != null && !isOled) {
                            this.clip(RoundedCornerShape(12.dp))
                                .hazeEffect(state = hazeState) {
                                    style = HazeStyle(blurRadius = 24.dp, tints = listOf(HazeTint(colorScheme.surface.copy(alpha = 0.4f))))
                                    inputScale = HazeInputScale.Fixed(0.6f)
                                }
                        } else this
                    },
                shape = RoundedCornerShape(12.dp),
                color = if (isOled) Color.Black else if (hazeState != null) colorScheme.surface.copy(alpha = 0.4f) else colorScheme.surface,
                tonalElevation = 8.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = if (isOled) 0.3f else 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    AvatarPopupItem(
                        icon = Icons.Default.Settings,
                        text = "Настройки персонажа",
                        onClick = {
                            onSettingsClick()
                            onDismiss()
                        }
                    )

                    AvatarPopupItem(
                        icon = Icons.Default.Download,
                        text = "Экспортировать лист",
                        onClick = {
                            onExportSheetClick()
                            onDismiss()
                        }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), color = colorScheme.outlineVariant.copy(alpha = 0.3f))

                    if (hasImage) {
                        AvatarPopupItem(
                            icon = Icons.Default.Portrait,
                            text = "Экспортировать портрет",
                            onClick = {
                                onExportPortraitClick()
                                onDismiss()
                            }
                        )
                        AvatarPopupItem(
                            icon = Icons.Default.Image,
                            text = "Заменить портрет",
                            onClick = {
                                onImagePickerClick()
                                onDismiss()
                            }
                        )
                        AvatarPopupItem(
                            icon = Icons.Default.Delete,
                            text = "Удалить портрет",
                            contentColor = colorScheme.error,
                            onClick = {
                                onDeleteClick()
                                onDismiss()
                            }
                        )
                    } else {
                        AvatarPopupItem(
                            icon = Icons.Default.AddAPhoto,
                            text = "Добавить портрет",
                            onClick = {
                                onImagePickerClick()
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AvatarPopupItem(
    icon: ImageVector,
    text: String,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = contentColor.copy(alpha = 0.8f)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}
