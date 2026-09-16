package ru.quasaris.characternexus.ui.menu

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.quasaris.characternexus.model.CharacterFolder
import ru.quasaris.characternexus.ui.outerShadow
import ru.quasaris.characternexus.ui.util.FolderColors
import ru.quasaris.characternexus.util.charactersCountLabel
import androidx.compose.foundation.isSystemInDarkTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderCard(
    folder: CharacterFolder,
    characterCount: Int,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    isDropZoneHovered: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleExpand: () -> Unit = {},
    onMoreClick: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    
    val rotation by animateFloatAsState(if (folder.isExpanded) 90f else 0f)

    val folderBackground = FolderColors.getThemeAdaptedColor(folder.colorArgb, isDark)
        ?: colorScheme.primary.copy(alpha = 0.4f)
    
    val containerColor = if (isSelected) {
        colorScheme.primaryContainer
    } else if (isDropZoneHovered) {
        if (isDark) colorScheme.primary.copy(alpha = 0.15f)
        else colorScheme.primary.copy(alpha = 0.1f)
    } else {
        if (folder.colorArgb != null) FolderColors.getCardContainerColor(folderBackground, isDark)
        else colorScheme.surfaceContainerLow
    }
    
    val iconBackground = if (folder.colorArgb != null) folderBackground else colorScheme.primary.copy(alpha = 0.1f)
    
    val iconTint = if (folder.colorArgb != null) {
        FolderColors.getBestIconTint(folderBackground)
    } else {
        colorScheme.primary
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .outerShadow(RoundedCornerShape(16.dp), blur = if (isSelected) 4.dp else 2.dp),
            shape = RoundedCornerShape(16.dp),
            border = if (isSelected) BorderStroke(2.dp, colorScheme.primary) else null,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(rotation),
                        tint = colorScheme.onSurfaceVariant
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconBackground)
                        .clickable { onToggleExpand() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (folder.isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = iconTint
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folder.name.ifEmpty { "Папка без названия" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = charactersCountLabel(characterCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(onClick = onMoreClick) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Опции",
                        tint = colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
