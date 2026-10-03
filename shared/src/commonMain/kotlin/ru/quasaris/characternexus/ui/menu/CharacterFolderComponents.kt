package ru.quasaris.characternexus.ui.menu

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.mohamedrejeb.compose.dnd.DragAndDropContainer
import com.mohamedrejeb.compose.dnd.DragAndDropState
import com.mohamedrejeb.compose.dnd.rememberDragAndDropState
import com.mohamedrejeb.compose.dnd.annotation.ExperimentalDndApi
import com.mohamedrejeb.compose.dnd.drop.dropTarget
import com.mohamedrejeb.compose.dnd.reorder.reorderableItem
import com.mohamedrejeb.compose.dnd.scroll.DragAutoScrollConfig
import com.mohamedrejeb.compose.dnd.scroll.dragAutoScroll
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
    color: Color = MaterialTheme.colorScheme.outlineVariant,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(2.dp)
            .background(color, RoundedCornerShape(50))
    )
}





@Immutable
data class TreeModel(
    val order: List<String>,
    val parentOf: Map<String, String?>
) {
    fun childrenOf(parent: String?): List<String> =
        order.filter { parentOf[it] == parent }

    fun isDescendant(id: String?, ancestor: String): Boolean {
        var current = id
        var guard = 0
        while (current != null && guard++ < 128) {
            if (current == ancestor) return true
            current = parentOf[current]
        }
        return false
    }

    fun move(id: String, newParent: String?, beforeId: String?): TreeModel {
        if (id == newParent) return this
        if (newParent != null && isDescendant(newParent, id)) return this

        val newOrder = order.toMutableList()
        if (!newOrder.remove(id)) return this
        val insertIndex = beforeId
            ?.let { newOrder.indexOf(it) }
            ?.takeIf { it >= 0 }
            ?: newOrder.size
        newOrder.add(insertIndex, id)

        val newParents = parentOf.toMutableMap()
        newParents[id] = newParent

        val result = TreeModel(newOrder, newParents)
        return if (result.order == order && result.parentOf == parentOf) this else result
    }
}

private fun buildTreeModel(
    characters: List<CharacterSummary>,
    folders: List<CharacterFolder>,
    globalOrder: List<String>
): TreeModel {
    val parentOf = LinkedHashMap<String, String?>()
    folders.forEach { parentOf[it.uuid] = it.parentFolderUuid }
    characters.forEach { parentOf[it.uuid] = it.folderUuid }

    val ordered = globalOrder.filter { parentOf.containsKey(it) }
    val rest = parentOf.keys.filter { it !in ordered }
    return TreeModel(ordered + rest, parentOf)
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

    data class DropSlot(
        override val parentId: String?,
        override val indentation: Int,
        val isStart: Boolean = false
    ) : TreeItem() {
        override val id: String =
            "drop-slot-${parentId ?: "root"}-${if (isStart) "start" else "end"}"
    }

    data class RowBreak(
        override val parentId: String?,
        override val indentation: Int
    ) : TreeItem() {
        override val id: String =
            "row-break-${parentId ?: "root"}-${indentation}"
    }
}

@OptIn(ExperimentalDndApi::class)
@Composable
fun CharacterFolderTree(
    characters: List<CharacterSummary>,
    folders: List<CharacterFolder>,
    globalOrder: List<String>,
    selectedIds: List<String>,
    isEditMode: Boolean,
    onMoveItem: (itemId: String, targetFolderId: String?, beforeItemId: String?) -> Unit,
    onCharacterClick: (String) -> Unit,
    onCharacterLongClick: (String) -> Unit,
    onFolderToggle: (String) -> Unit,
    onFolderExpansionToggle: (String) -> Unit,
    onFolderLongClick: (String) -> Unit,
    onFolderMoreClick: (CharacterFolder) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val dndState = rememberDragAndDropState<DragItem>(dragAfterLongPress = true)

    val baseModel = remember(characters, folders, globalOrder) {
        buildTreeModel(characters, folders, globalOrder)
    }

    var preview by remember { mutableStateOf<TreeModel?>(null) }
    var pendingMoveId by remember { mutableStateOf<String?>(null) }
    var pendingCollapsedDrop by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Пришли новые данные снаружи — preview больше не нужен
    LaunchedEffect(baseModel) { preview = null }

    val model = preview ?: baseModel
    val draggedItem = dndState.draggedItem
    val isDragging = draggedItem != null

    LaunchedEffect(isDragging) {
        if (!isDragging) {
            val collapsedDrop = pendingCollapsedDrop
            pendingCollapsedDrop = null

            if (collapsedDrop != null) {
                val (draggedId, folderId) = collapsedDrop
                onMoveItem(draggedId, folderId, null)
                preview = null
                pendingMoveId = null
                return@LaunchedEffect
            }

            val movedId = pendingMoveId
            val finalModel = preview
            pendingMoveId = null
            if (movedId != null && finalModel != null) {
                val newParent = finalModel.parentOf[movedId]
                val siblings = finalModel.childrenOf(newParent)
                val beforeId = siblings.getOrNull(siblings.indexOf(movedId) + 1)
                onMoveItem(movedId, newParent, beforeId)
            }
        }
    }

    fun applyMove(draggedId: String, newParent: String?, beforeId: String?) {
        pendingCollapsedDrop = null
        val current = preview ?: baseModel
        val updated = current.move(draggedId, newParent, beforeId)
        if (updated !== current) {
            preview = updated
            pendingMoveId = draggedId
        }
    }

    fun hoverCollapsedFolder(draggedId: String, folderId: String) {
        val current = preview ?: baseModel
        if (draggedId == folderId || current.isDescendant(folderId, draggedId)) {
            pendingCollapsedDrop = null
            return
        }
        pendingCollapsedDrop = draggedId to folderId
    }

    fun moveToStart(draggedId: String) {
        val current = preview ?: baseModel
        applyMove(draggedId, null, current.order.firstOrNull { it != draggedId })
    }

    fun reorderAround(draggedId: String, target: TreeItem) {
        if (draggedId == target.id) return
        val current = preview ?: baseModel
        val from = current.order.indexOf(draggedId)
        val to = current.order.indexOf(target.id)
        if (from < 0 || to < 0) return
        val beforeId = if (from < to) current.order.getOrNull(to + 1) else target.id
        applyMove(draggedId, target.parentId, beforeId)
    }

    DragAndDropContainer(
        state = dndState,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FolderSectionContent(
                parentUuid = null,
                depth = 0,
                model = model,
                characters = characters,
                folders = folders,
                selectedIds = selectedIds,
                isEditMode = isEditMode,
                dndState = dndState,
                isDark = isDark,
                onCharacterClick = onCharacterClick,
                onCharacterLongClick = onCharacterLongClick,
                onFolderToggle = onFolderToggle,
                onFolderExpansionToggle = onFolderExpansionToggle,
                onFolderLongClick = onFolderLongClick,
                onFolderMoreClick = onFolderMoreClick,
                reorderAround = ::reorderAround,
                hoverCollapsedFolder = ::hoverCollapsedFolder,
                moveToStart = ::moveToStart,
                applyMove = ::applyMove
            )
        }
    }
}

@Composable
private fun FolderSectionContent(
    parentUuid: String?,
    depth: Int,
    model: TreeModel,
    characters: List<CharacterSummary>,
    folders: List<CharacterFolder>,
    selectedIds: List<String>,
    isEditMode: Boolean,
    dndState: DragAndDropState<DragItem>,
    isDark: Boolean,
    onCharacterClick: (String) -> Unit,
    onCharacterLongClick: (String) -> Unit,
    onFolderToggle: (String) -> Unit,
    onFolderExpansionToggle: (String) -> Unit,
    onFolderLongClick: (String) -> Unit,
    onFolderMoreClick: (CharacterFolder) -> Unit,
    reorderAround: (String, TreeItem) -> Unit,
    hoverCollapsedFolder: (String, String) -> Unit,
    moveToStart: (String) -> Unit,
    applyMove: (String, String?, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val childIds = remember(model, parentUuid) { model.childrenOf(parentUuid) }
    if (childIds.isEmpty()) return

    val parentFolder = remember(parentUuid, folders) { folders.find { it.uuid == parentUuid } }
    val defaultOutline = MaterialTheme.colorScheme.outlineVariant
    val lineColor = remember(parentFolder, isDark, defaultOutline) {
        FolderColors.getThemeAdaptedColor(parentFolder?.colorArgb, isDark)
            ?: defaultOutline
    }

    val isRoot = parentUuid == null

    val content = @Composable {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val cardChunk = remember(childIds, characters) { mutableStateListOf<CharacterSummary>() }

            @Composable
            fun FlushCards() {
                if (cardChunk.isNotEmpty()) {
                    val currentCards = cardChunk.toList()
                    cardChunk.clear()
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        currentCards.forEach { character ->
                            val item = TreeItem.Character(character, depth, parentUuid)
                            val isActualDragging = dndState.draggedItem?.key == character.uuid
                            val folderColor = parentFolder?.colorArgb

                            var itemModifier: Modifier = Modifier
                                .widthIn(min = 280.dp, max = 350.dp)
                                .weight(1f)
                            val dragEnabled = isEditMode || selectedIds.isNotEmpty()
                            if (dragEnabled) {
                                itemModifier = itemModifier
                                    .reorderableItem(
                                        key = character.uuid,
                                        data = DragItem.Character(character.uuid, character),
                                        state = dndState,
                                        onDragEnter = { dragged ->
                                            reorderAround(dragged.data.id, item)
                                        },
                                        draggableContent = {
                                            Box(
                                                modifier = Modifier.graphicsLayer {
                                                    alpha = 0.8f; scaleX = 1.05f; scaleY = 1.05f
                                                }
                                            ) {
                                                CharacterCardComposable(
                                                    character = character,
                                                    isDragging = true
                                                )
                                            }
                                        }
                                    )
                                    .graphicsLayer { alpha = if (isActualDragging) 0f else 1f }
                            }

                            Box(modifier = itemModifier) {
                                CharacterCardComposable(
                                    character = character,
                                    isSelected = character.uuid in selectedIds,
                                    isEditMode = isEditMode,
                                    folderColorArgb = folderColor,
                                    onClick = { onCharacterClick(character.uuid) },
                                    onLongClick = { onCharacterLongClick(character.uuid) }
                                )
                            }
                        }
                    }
                }
            }

            childIds.forEach { id ->
                val character = characters.find { it.uuid == id }
                if (character != null) {
                    cardChunk.add(character)
                } else {
                    val folder = folders.find { it.uuid == id }
                    if (folder != null) {
                        FlushCards()
                        val count = model.childrenOf(folder.uuid)
                            .count { childId -> characters.any { it.uuid == childId } }
                        val item = TreeItem.Folder(folder, count, depth, parentUuid)
                        val isActualDragging = dndState.draggedItem?.key == folder.uuid

                        var itemModifier: Modifier = Modifier.fillMaxWidth()
                        val dragEnabled = isEditMode || selectedIds.isNotEmpty()
                        if (dragEnabled) {
                            itemModifier = itemModifier
                                .reorderableItem(
                                    key = folder.uuid,
                                    data = DragItem.Folder(folder.uuid, folder),
                                    state = dndState,
                                    onDragEnter = { dragged ->
                                        if (!folder.isExpanded) {
                                            hoverCollapsedFolder(dragged.data.id, folder.uuid)
                                        } else {
                                            reorderAround(dragged.data.id, item)
                                        }
                                    },
                                    draggableContent = {
                                        Box(
                                            modifier = Modifier.graphicsLayer {
                                                alpha = 0.8f; scaleX = 1.05f; scaleY = 1.05f
                                            }
                                        ) {
                                            FolderHeader(
                                                folder = folder,
                                                characterCount = count,
                                                isExpanded = folder.isExpanded,
                                                onToggleExpand = {},
                                                onExpansionToggle = {},
                                                onMoreClick = {}
                                            )
                                        }
                                    }
                                )
                                .graphicsLayer { alpha = if (isActualDragging) 0f else 1f }
                        }

                        Box(modifier = itemModifier) {
                            FolderHeader(
                                folder = folder,
                                characterCount = count,
                                isExpanded = folder.isExpanded,
                                isSelected = folder.uuid in selectedIds,
                                isEditMode = isEditMode,
                                isHovered = dndState.hoveredDropTargetKey == folder.uuid,
                                onToggleExpand = { onFolderToggle(folder.uuid) },
                                onExpansionToggle = { onFolderExpansionToggle(folder.uuid) },
                                onLongClick = { onFolderLongClick(folder.uuid) },
                                onMoreClick = { onFolderMoreClick(folder) }
                            )
                        }

                        if (folder.isExpanded) {
                            FolderSectionContent(
                                parentUuid = folder.uuid,
                                depth = depth + 1,
                                model = model,
                                characters = characters,
                                folders = folders,
                                selectedIds = selectedIds,
                                isEditMode = isEditMode,
                                dndState = dndState,
                                isDark = isDark,
                                onCharacterClick = onCharacterClick,
                                onCharacterLongClick = onCharacterLongClick,
                                onFolderToggle = onFolderToggle,
                                onFolderExpansionToggle = onFolderExpansionToggle,
                                onFolderLongClick = onFolderLongClick,
                                onFolderMoreClick = onFolderMoreClick,
                                reorderAround = reorderAround,
                                hoverCollapsedFolder = hoverCollapsedFolder,
                                moveToStart = moveToStart,
                                applyMove = applyMove
                            )
                        }
                    }
                }
            }
            FlushCards()
        }
    }

    if (isRoot) {
        Box(modifier = modifier.fillMaxWidth()) {
            content()
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 12.dp)
                .height(IntrinsicSize.Min)
        ) {
            HierarchyGuideLine(
                color = lineColor,
                modifier = Modifier.fillMaxHeight()
            )
            Spacer(Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                content()
            }
        }
    }
}

@Composable
private fun FolderEndDivider(
    folderColorArgb: Int?,
    isRoot: Boolean,
    isHovered: Boolean,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()

    val baseColor = remember(folderColorArgb, isDark) {
        FolderColors.getThemeAdaptedColor(folderColorArgb, isDark)
    } ?: colorScheme.outlineVariant

    val lineColor by animateColorAsState(
        targetValue = if (isHovered) baseColor else baseColor.copy(alpha = 0.55f)
    )
    val thickness by animateDpAsState(targetValue = if (isHovered) 4.dp else 2.dp)

    Box(
        modifier = modifier.height(if (isRoot) 28.dp else 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = if (isRoot) 0.dp else 8.dp)
                .height(thickness)
                .background(lineColor, RoundedCornerShape(50))
        )
    }
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
                .then(
                    if (isEditMode) Modifier.clickable(onClick = onClick)
                    else Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
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
                        color = colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
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
    onExpansionToggle: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHovered: Boolean = false,
    isSelected: Boolean = false,
    isEditMode: Boolean = false,
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
            .then(
                if (isEditMode) Modifier.clickable(onClick = onToggleExpand)
                else Modifier.combinedClickable(onClick = onToggleExpand, onLongClick = onLongClick)
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
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
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

            IconButton(onClick = onExpansionToggle) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
