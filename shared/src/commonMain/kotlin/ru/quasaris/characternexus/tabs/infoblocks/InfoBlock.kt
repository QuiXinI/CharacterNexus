package ru.quasaris.characternexus.tabs.infoblocks

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import ru.quasaris.characternexus.backend.SettingsViewModel
import ru.quasaris.characternexus.model.DynamicContentBlock
import ru.quasaris.characternexus.tabs.InfoBoxIcon
import ru.quasaris.characternexus.ui.outerShadow
import ru.quasaris.characternexus.util.HapticType
import ru.quasaris.characternexus.util.PlatformUtils

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InfoBlockComponent(
    infoBlock: DynamicContentBlock.InfoBlock,
    onUpdate: (DynamicContentBlock.InfoBlock) -> Unit,
    onConvertToResource: (DynamicContentBlock.InfoBlock) -> Unit,
    onDeleteRequest: () -> Unit,
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    blurDynamicFields: Boolean = true,
    settingsViewModel: SettingsViewModel? = null,
    onFullscreenDialogOpenChange: (Boolean) -> Unit = {},
    onOpenConfig: ((DynamicContentBlock.InfoBlock) -> Unit)? = null
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    var showConfig by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

    val colorScheme = MaterialTheme.colorScheme
    val veryResponsive by settingsViewModel?.veryResponsiveHaptics?.collectAsState() ?: remember { mutableStateOf(true) }
    val useHaze = hazeState != null && blurDynamicFields

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
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    performClickHaptic()
                    isExpanded = !isExpanded
                },
                onLongClick = {
                    performClickHaptic()
                    if (onOpenConfig != null) {
                        onOpenConfig(infoBlock)
                    } else {
                        showConfig = true
                    }
                }
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = infoBlock.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            if (!infoBlock.link.isNullOrBlank()) {
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
                                    val trimmedLink = infoBlock.link.trim()
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

        AnimatedVisibility(
            visible = isExpanded && infoBlock.description.isNotEmpty(),
            enter = expandIn(expandFrom = Alignment.TopStart) + fadeIn(),
            exit = shrinkOut(shrinkTowards = Alignment.TopStart) + fadeOut()
        ) {
            ru.quasaris.characternexus.tabs.RenderMarkdownContent(
                text = infoBlock.description,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }

    if (showConfig && onOpenConfig == null) {
        InfoBlockConfigDialog(
            infoBlock = infoBlock,
            onDismiss = { showConfig = false },
            onSave = { onUpdate(it) },
            onConvertToResource = {
                onConvertToResource(it)
                showConfig = false
            },
            onDelete = {
                onDeleteRequest()
                showConfig = false
            },
            forceBlurEnabled = forceBlurEnabled,
            settingsViewModel = settingsViewModel,
            onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
            hazeState = hazeState
        )
    }
}
