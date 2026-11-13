package ru.quasaris.characternexus.tabs.folders

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import ru.quasaris.characternexus.backend.SettingsViewModel
import ru.quasaris.characternexus.model.DynamicFolder
import ru.quasaris.characternexus.ui.CharacterDetailState
import ru.quasaris.characternexus.ui.outerShadow

@Composable
fun DynamicFolderCard(
    folderId: String,
    state: CharacterDetailState?,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    settingsViewModel: SettingsViewModel? = null,
    // сколько блоков лежит внутри (дети рисует редактор под карточкой)
    childCount: Int = 0
) {
    val colorScheme = MaterialTheme.colorScheme
    val folderManager = state?.folderManager
    val folder = folderManager?.get(folderId) ?: DynamicFolder(id = folderId, title = "Папка")

    val isExpanded: Boolean = folder.isExpanded

    var title by remember(folder.title) { mutableStateOf(folder.title) }
    var isEditMode by remember { mutableStateOf(false) }

    val blurCards by settingsViewModel?.blurCards?.collectAsState() ?: remember { mutableStateOf(true) }

    LaunchedEffect(isEditMode) {
        if (isEditMode && !isExpanded) {
            folderManager?.upsert(folder.copy(isExpanded = true))
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .outerShadow(
                shape = RoundedCornerShape(12.dp),
                blur = 4.dp,
                offsetY = 2.dp
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!isEditMode) {
                    folderManager?.upsert(folder.copy(isExpanded = !isExpanded))
                }
            },
        shape = RoundedCornerShape(12.dp),
        color = if (blurCards && hazeState != null) colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
        else colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .clipToBounds()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = title,
                    onValueChange = { newTitle: String ->
                        title = newTitle
                        folderManager?.upsert(folder.copy(title = newTitle))
                    },
                    enabled = isEditMode,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(colorScheme.primary),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp, end = 8.dp),
                    decorationBox = { innerTextField ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (title.isEmpty()) {
                                Text(
                                    "Название папки",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = 18.sp,
                                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                if (childCount > 0) {
                    Text(
                        text = childCount.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.width(4.dp))
                }

                IconButton(
                    onClick = { isEditMode = !isEditMode },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isEditMode) Icons.Default.Done else Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isEditMode) colorScheme.primary else colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded && childCount == 0,
                enter = fadeIn() + expandVertically(clip = true),
                exit = fadeOut() + shrinkVertically(clip = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                ) {
                    Text(
                        "Папка пуста",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
