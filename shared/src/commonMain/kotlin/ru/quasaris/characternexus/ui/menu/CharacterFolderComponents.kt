package ru.quasaris.characternexus.ui.menu

import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.mohamedrejeb.compose.dnd.DragAndDropContainer
import com.mohamedrejeb.compose.dnd.DragAndDropState
import com.mohamedrejeb.compose.dnd.rememberDragAndDropState
import com.mohamedrejeb.compose.dnd.drag.draggableItem
import com.mohamedrejeb.compose.dnd.drag.dragHandle
import com.mohamedrejeb.compose.dnd.drop.dropTarget
import ru.quasaris.characternexus.backend.ImageManager
import ru.quasaris.characternexus.backend.getNextLevelThreshold
import ru.quasaris.characternexus.backend.getPreviousLevelThreshold
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.platformFileSystem
import ru.quasaris.characternexus.ui.outerShadow
import ru.quasaris.characternexus.ui.util.FolderColors
import ru.quasaris.characternexus.util.charactersCountLabel
import androidx.compose.foundation.isSystemInDarkTheme

@Composable
fun HierarchyGuideLine(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outlineVariant
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(color)
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun CharacterCardComposable(
    character: CharacterSummary,
    modifier: Modifier = Modifier,
    isDragging: Boolean = false,
    isSelected: Boolean = false,
    isHovered: Boolean = false,
    isEditMode: Boolean = false,
    folderColorArgb: Int? = null,
    dragHandleModifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()

    val adaptedFolderColor = remember(folderColorArgb, isDark) {
        FolderColors.getThemeAdaptedColor(folderColorArgb, isDark)
    }

    val thumbPath = remember(character.imageData, character.uuid) {
        ImageManager.getThumbnailFile(character.imageData ?: "", character.uuid)
    }
    val portraitPath = remember(character.imageData, character.uuid) {
        ImageManager.getPortraitFile(character.imageData ?: "", character.uuid)
    }
    val imagePath = remember(thumbPath, portraitPath) {
        if (platformFileSystem.exists(thumbPath)) thumbPath
        else if (platformFileSystem.exists(portraitPath)) portraitPath
        else null
    }

    val imageExists = imagePath != null

    val cardColor by animateColorAsState(
        targetValue = when {
            isSelected -> colorScheme.primaryContainer
            isHovered -> colorScheme.tertiaryContainer.copy(alpha = 0.5f)
            isDragging -> colorScheme.surfaceContainerHigh
            else -> adaptedFolderColor?.let { FolderColors.getCardContainerColor(it, isDark) } ?: colorScheme.surfaceContainerLow
        }
    )
    
    val progressBarColor = adaptedFolderColor?.let { FolderColors.getProgressBarColor(it, isDark) } 
        ?: colorScheme.primary.copy(alpha = 0.2f)

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
    val baseHpColor = if (maxHp > 0 && currentHp.toFloat() / maxHp.toFloat() > 0.5f) {
        Color(0xFF4CAF50)
    } else {
        Color(0xFFF44336)
    }
    val hpColor = lerp(baseHpColor, colorScheme.onSurfaceVariant, 0.5f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .outerShadow(RoundedCornerShape(16.dp), blur = if (isSelected) 4.dp else 2.dp),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) BorderStroke(2.dp, colorScheme.primary) else null,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val avatarSizeDp = 90.dp
                    val avatarOffsetDp = (-18).dp
                    val startPointPx = (avatarOffsetDp + avatarSizeDp / 2).toPx()
                    drawRect(
                        color = progressBarColor,
                        topLeft = Offset(startPointPx, 0f),
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
                    .height(80.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                    if (imageExists) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalPlatformContext.current)
                                .data(imagePath)
                                .memoryCacheKey("${imagePath}_${character.imageData}")
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
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
                        color = colorScheme.onSurface
                    )
                    FlowRow(
                        verticalArrangement = Arrangement.Center,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(
                            text = "Уровень ${character.level}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant
                        )

                        character.characterClass.split(" • ").forEach { part ->
                            if (part.isNotBlank()) {
                                Text(
                                    text = " • $part",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = " • $currentHp/$maxHp",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = hpColor
                        )
                    }
                }
                
                if (isEditMode) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Перетащить",
                        modifier = dragHandleModifier
                            .padding(end = 12.dp)
                            .size(24.dp),
                        tint = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderHeader(
    folder: CharacterFolder,
    characterCount: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHovered: Boolean = false,
    isSelected: Boolean = false,
    isEditMode: Boolean = false,
    dragHandleModifier: Modifier = Modifier,
    onLongClick: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    
    val adaptedColor = remember(folder.colorArgb, isDark) {
        FolderColors.getThemeAdaptedColor(folder.colorArgb, isDark)
    }

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> colorScheme.primaryContainer
            isHovered -> colorScheme.tertiaryContainer
            else -> adaptedColor?.copy(alpha = 0.4f) ?: colorScheme.surfaceContainerLow
        }
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .outerShadow(RoundedCornerShape(12.dp), blur = if (isSelected) 4.dp else 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onToggleExpand,
                onLongClick = onLongClick
            ),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) BorderStroke(2.dp, colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                contentDescription = null,
                tint = adaptedColor ?: colorScheme.primary
            )
            
            Spacer(Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = charactersCountLabel(characterCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onMoreClick) {
                Icon(Icons.Default.MoreVert, contentDescription = null)
            }
            
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )

            if (isEditMode) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Перетащить",
                    modifier = dragHandleModifier.size(24.dp).padding(start = 8.dp),
                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

sealed class TreeItem {
    abstract val id: String
    abstract val indentation: Int
    abstract val parentId: String?
    
    data class Character(
        val summary: CharacterSummary,
        override val indentation: Int,
        override val parentId: String?
    ) : TreeItem() {
        override val id: String = summary.uuid
    }
    
    data class Folder(
        val folder: CharacterFolder,
        val count: Int,
        override val indentation: Int,
        override val parentId: String?
    ) : TreeItem() {
        override val id: String = folder.uuid
    }
}

@Composable
fun CharacterFolderTree(
    characters: List<CharacterSummary>,
    folders: List<CharacterFolder>,
    globalOrder: List<String>,
    selectedIds: List<String>,
    isEditMode: Boolean,
    onMoveItem: (itemId: String, targetFolderId: String?, afterItemId: String?) -> Unit,
    onCharacterClick: (String) -> Unit,
    onCharacterLongClick: (String) -> Unit,
    onFolderToggle: (String) -> Unit,
    onFolderLongClick: (String) -> Unit,
    onFolderMoreClick: (CharacterFolder) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberDragAndDropState<DragItem>()
    
    val treeItems = remember(characters, folders, globalOrder) {
        val result = mutableListOf<TreeItem>()
        
        fun addItemsRecursive(parentUuid: String?, depth: Int) {
            val itemsInFolder = globalOrder.filter { id ->
                characters.any { it.uuid == id && it.folderUuid == parentUuid } ||
                folders.any { it.uuid == id && it.parentFolderUuid == parentUuid }
            }
            
            itemsInFolder.forEach { id ->
                val char = characters.find { it.uuid == id }
                val folder = folders.find { it.uuid == id }
                
                if (char != null) {
                    result.add(TreeItem.Character(char, depth, parentUuid))
                } else if (folder != null) {
                    val count = characters.count { it.folderUuid == folder.uuid }
                    result.add(TreeItem.Folder(folder, count, depth, parentUuid))
                    if (folder.isExpanded) {
                        addItemsRecursive(folder.uuid, depth + 1)
                    }
                }
            }
        }
        
        addItemsRecursive(null, 0)
        result
    }

    DragAndDropContainer(
        state = state,
        modifier = modifier.fillMaxSize()
    ) {
        TreeLazyColumn(
            treeItems = treeItems,
            state = state,
            folders = folders,
            selectedIds = selectedIds,
            isEditMode = isEditMode,
            onCharacterClick = onCharacterClick,
            onCharacterLongClick = onCharacterLongClick,
            onFolderToggle = onFolderToggle,
            onFolderLongClick = onFolderLongClick,
            onFolderMoreClick = onFolderMoreClick,
            onMoveItem = onMoveItem
        )
    }
}

@Composable
private fun TreeLazyColumn(
    treeItems: List<TreeItem>,
    state: DragAndDropState<DragItem>,
    folders: List<CharacterFolder>,
    selectedIds: List<String>,
    isEditMode: Boolean,
    onCharacterClick: (String) -> Unit,
    onCharacterLongClick: (String) -> Unit,
    onFolderToggle: (String) -> Unit,
    onFolderLongClick: (String) -> Unit,
    onFolderMoreClick: (CharacterFolder) -> Unit,
    onMoveItem: (itemId: String, targetFolderId: String?, afterItemId: String?) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(treeItems, key = { it.id }) { item ->
            val canDrop = remember(state.draggedItem, item) {
                val draggedItem = state.draggedItem ?: return@remember true
                val data = draggedItem.data
                if (data is DragItem.Folder && item is TreeItem.Folder) {
                    fun isDescendant(targetId: String, potentialAncestorId: String): Boolean {
                        if (targetId == potentialAncestorId) return true
                        val targetFolder = folders.find { it.uuid == targetId } ?: return false
                        val parentId = targetFolder.parentFolderUuid ?: return false
                        return isDescendant(parentId, potentialAncestorId)
                    }
                    !isDescendant(item.id, data.id)
                } else true
            }

            // Плавный "выезд" элемента из-под родительской папки при разворачивании:
            // при первом появлении в списке элемент едет вниз из-под шапки и одновременно проявляется.
            val enterProgress = remember { Animatable(0f) }
            LaunchedEffect(item.id) {
                enterProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // placementSpec плавно сдвигает соседние элементы, когда список
                    // "уплотняется" при сворачивании папки или "раздвигается" при разворачивании;
                    // fadeOutSpec плавно проявляет исчезновение элемента при сворачивании
                    // (Compose ненадолго оставляет его в композиции, чтобы доиграть анимацию).
                    .animateItem(
                        fadeInSpec = null, // альфу/сдвиг появления делаем сами через enterProgress
                        placementSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        fadeOutSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                    )
                    .graphicsLayer {
                        alpha = enterProgress.value
                        // старт чуть выше своей позиции — как будто выезжает из-под папки сверху
                        translationY = (1f - enterProgress.value) * -size.height * 0.35f
                    }
                    .padding(start = (item.indentation * 16).dp)
            ) {
                if (item.indentation > 0) {
                    HierarchyGuideLine(modifier = Modifier.height(IntrinsicSize.Min))
                    Spacer(Modifier.width(12.dp))
                }
                
                val data = when(item) {
                    is TreeItem.Character -> DragItem.Character(item.id, item.summary)
                    is TreeItem.Folder -> DragItem.Folder(item.id, item.folder)
                }
                
                var itemModifier: Modifier = Modifier.weight(1f)
                
                if (isEditMode) {
                    itemModifier = itemModifier
                        .draggableItem(
                            key = item.id,
                            data = data,
                            state = state,
                            dragAfterLongPress = true,
                            hasDragHandle = true,
                            draggableContent = {
                                Box(modifier = Modifier.graphicsLayer { alpha = 0.8f; scaleX = 1.05f; scaleY = 1.05f }) {
                                    when (item) {
                                        is TreeItem.Character -> CharacterCardComposable(item.summary, isDragging = true)
                                        is TreeItem.Folder -> FolderHeader(
                                            folder = item.folder,
                                            characterCount = item.count,
                                            isExpanded = item.folder.isExpanded,
                                            onToggleExpand = {},
                                            onMoreClick = {}
                                        )
                                    }
                                }
                            }
                        )
                        .dropTarget(
                            key = item.id,
                            state = state,
                            canDrop = canDrop && state.draggedItem?.key != item.id,
                            onDrop = { draggedItem ->
                                if (item is TreeItem.Folder) {
                                    onMoveItem(draggedItem.data.id, item.id, null)
                                } else {
                                    onMoveItem(draggedItem.data.id, item.parentId, item.id)
                                }
                            }
                        )
                }

                Box(modifier = itemModifier) {
                    when (item) {
                        is TreeItem.Character -> {
                            val folderColor = folders.find { it.uuid == item.summary.folderUuid }?.colorArgb
                            CharacterCardComposable(
                                character = item.summary,
                                isDragging = state.draggedItem?.key == item.id,
                                isSelected = item.id in selectedIds,
                                isHovered = state.hoveredDropTargetKey == item.id,
                                isEditMode = isEditMode,
                                folderColorArgb = folderColor,
                                dragHandleModifier = if (isEditMode) Modifier.dragHandle(key = item.id, state = state) else Modifier,
                                onClick = { onCharacterClick(item.id) },
                                onLongClick = { onCharacterLongClick(item.id) },
                                modifier = Modifier
                            )
                        }
                        is TreeItem.Folder -> {
                            FolderHeader(
                                folder = item.folder,
                                characterCount = item.count,
                                isExpanded = item.folder.isExpanded,
                                isSelected = item.id in selectedIds,
                                isEditMode = isEditMode,
                                dragHandleModifier = if (isEditMode) Modifier.dragHandle(key = item.id, state = state) else Modifier,
                                onToggleExpand = { onFolderToggle(item.id) },
                                onLongClick = { onFolderLongClick(item.id) },
                                onMoreClick = { onFolderMoreClick(item.folder) },
                                isHovered = state.hoveredDropTargetKey == item.id && canDrop,
                                modifier = Modifier
                            )
                        }
                    }
                }
            }
        }
        
        if (isEditMode) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .dropTarget(
                            key = "root",
                            state = state,
                            onDrop = { draggedItem ->
                                onMoveItem(draggedItem.data.id, null, null)
                            }
                        )
                        .background(
                            if (state.hoveredDropTargetKey == "root") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.draggedItem != null) {
                        Text("Переместить в корень", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
