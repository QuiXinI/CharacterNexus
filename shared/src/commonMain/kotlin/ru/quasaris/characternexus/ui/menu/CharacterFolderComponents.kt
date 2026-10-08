package ru.quasaris.characternexus.ui.menu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.mohamedrejeb.compose.dnd.DragAndDropContainer
import com.mohamedrejeb.compose.dnd.DragAndDropState
import com.mohamedrejeb.compose.dnd.annotation.ExperimentalDndApi
import com.mohamedrejeb.compose.dnd.rememberDragAndDropState
import com.mohamedrejeb.compose.dnd.reorder.reorderableItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.quasaris.characternexus.backend.ImageManager
import ru.quasaris.characternexus.backend.getNextLevelThreshold
import ru.quasaris.characternexus.backend.getPreviousLevelThreshold
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.platformFileSystem
import ru.quasaris.characternexus.ui.outerShadow
import ru.quasaris.characternexus.ui.util.FolderColors
import ru.quasaris.characternexus.util.charactersCountLabel
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
//  Геометрия дерева
// ─────────────────────────────────────────────────────────────────────────────

private val ItemGap = 12.dp
private val IndentStep = 26.dp      // 12 (отступ) + 2 (полоска) + 12 (зазор) — как в прежней версии
private val StripeInset = 12.dp
private val StripeWidth = 2.dp
private val MinCardWidth = 280.dp
private val MaxCardWidth = 560.dp   // шире не растягиваем, чтобы строка не превращалась в «простыню»
private val CardProbeHeight = 80.dp

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

// ─────────────────────────────────────────────────────────────────────────────
//  Модель дерева (порядок + родители). Только относительный порядок среди
//  соседей имеет значение.
// ─────────────────────────────────────────────────────────────────────────────

@Immutable
data class TreeModel(
    val order: List<String>,
    val parentOf: Map<String, String?>
) {
    private val childrenIndex: Map<String?, List<String>> by lazy(LazyThreadSafetyMode.NONE) {
        order.groupBy { parentOf[it] }
    }

    fun childrenOf(parent: String?): List<String> = childrenIndex[parent].orEmpty()

    /** true, если [id] == [ancestor] или лежит где-то внутри него. */
    fun isDescendant(id: String?, ancestor: String): Boolean {
        var current = id
        var guard = 0
        while (current != null && guard++ < 128) {
            if (current == ancestor) return true
            current = parentOf[current]
        }
        return false
    }

    /** beforeId == null → в конец списка соседей нового родителя. */
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

private fun TreeModel.hasSameItemsAs(other: TreeModel): Boolean =
    order.size == other.order.size && other.order.all { it in parentOf }

private fun buildTreeModel(
    characters: List<CharacterSummary>,
    folders: List<CharacterFolder>,
    globalOrder: List<String>
): TreeModel {
    val folderIds = folders.mapTo(HashSet()) { it.uuid }
    val parentOf = LinkedHashMap<String, String?>()
    // «Осиротевшие» элементы (родитель-папка пропала) показываем в корне, а не теряем
    folders.forEach { parentOf[it.uuid] = it.parentFolderUuid?.takeIf { p -> p in folderIds && p != it.uuid } }
    characters.forEach { parentOf[it.uuid] = it.folderUuid?.takeIf { p -> p in folderIds } }

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
}

private fun flattenTree(
    model: TreeModel,
    characters: Map<String, CharacterSummary>,
    folders: Map<String, CharacterFolder>
): List<TreeItem> {
    val out = ArrayList<TreeItem>(model.order.size)
    fun walk(parent: String?, depth: Int) {
        if (depth > 32) return
        model.childrenOf(parent).forEach { id ->
            val c = characters[id]
            if (c != null) {
                out += TreeItem.Character(c, depth, parent)
                return@forEach
            }
            val f = folders[id] ?: return@forEach
            val count = model.childrenOf(f.uuid).count { it in characters }
            out += TreeItem.Folder(f, count, depth, parent)
            if (f.isExpanded) walk(f.uuid, depth + 1)
        }
    }
    walk(null, 0)
    return out
}

// ─────────────────────────────────────────────────────────────────────────────
//  Геометрия, которую считает layout (нужна для хит-теста и полосок)
// ─────────────────────────────────────────────────────────────────────────────

@Immutable
private data class GeoEntry(
    val id: String,
    val isFolder: Boolean,
    val expanded: Boolean,
    val depth: Int,
    val parentId: String?,
    val rect: Rect,           // целевая (не анимированная!) позиция
    val multiCol: Boolean,    // карточки идут >1 в строку
    val lineRect: Rect        // прямоугольник всей строки карточек
)

@Immutable
private data class StripeDef(val folderId: String, val memberIds: List<String>)

@Immutable
private data class TreeGeometry(
    val entries: List<GeoEntry> = emptyList(),
    val stripes: List<StripeDef> = emptyList(),
    val indentPx: Float = 0f
) {
    val byId: Map<String, GeoEntry> by lazy(LazyThreadSafetyMode.NONE) { entries.associateBy { it.id } }
}

private class PlacedItem(
    val item: TreeItem,
    val placeable: Placeable,
    val x: Int,
    val y: Int
)

/**
 * Плавное «перетекание»: у каждого элемента своя пружина на позицию.
 * Элементы лежат в одном плоском Layout под стабильными ключами, поэтому переход между
 * папками/строками — это просто смена целевой позиции (композиция не пересоздаётся,
 * и жест перетаскивания не обрывается).
 */
@Stable
private class TreeMotion(private val scope: CoroutineScope) {
    private val slots = HashMap<String, Animatable<Offset, AnimationVector2D>>()
    private val spec = spring<Offset>(dampingRatio = 0.88f, stiffness = 380f)

    val geometry = mutableStateOf(TreeGeometry())

    fun retarget(id: String, target: Offset) {
        val slot = slots[id]
        if (slot == null) {
            slots[id] = Animatable(target, Offset.VectorConverter)
        } else if (slot.targetValue != target) {
            scope.launch { slot.animateTo(target, spec) }
        }
    }

    fun current(id: String): Offset = slots[id]?.value ?: Offset.Zero

    fun prune(alive: Set<String>) {
        slots.keys.retainAll(alive)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Логика «куда упадёт элемент»
// ─────────────────────────────────────────────────────────────────────────────

private enum class FolderZone { Before, Inside, After }

@Immutable
private data class DropIntent(
    val parentId: String?,                 // в какого родителя попадёт элемент
    val beforeId: String?,                 // перед кем (null → в конец родителя)
    val zone: FolderZone? = null,          // зона относительно папки (для индикации)
    val zoneFolderId: String? = null,
    val intoCollapsed: Boolean = false     // падение в свёрнутую папку: без превью, применяется при отпускании
)

@Immutable
private data class DropHint(val text: String, val icon: ImageVector)

private fun distanceSq(r: Rect, p: Offset): Float {
    val dx = maxOf(r.left - p.x, 0f, p.x - r.right)
    val dy = maxOf(r.top - p.y, 0f, p.y - r.bottom)
    return dx * dx + dy * dy
}

/**
 * Единственное место, где принимается решение о положении элемента.
 *
 *  • карточка: левая/правая (или верхняя/нижняя) половина → «до»/«после»;
 *  • папка: верх → «до папки», середина → «внутрь», низ → «после папки»
 *    (у раскрытой папки всё, что ниже верхней зоны, = «первым внутрь»);
 *  • «после» последнего элемента вложенной папки: если указатель левее колонки контента —
 *    элемент «выходит» на уровень выше (можно выйти сразу на несколько уровней).
 *
 * Хит-тест идёт по ЦЕЛЕВЫМ (не анимированным) прямоугольникам, поэтому «мерцания» при
 * перетекании нет. Если указатель над самим перетаскиваемым элементом — ничего не меняем.
 */
private fun computeIntent(
    p: Offset,
    geo: TreeGeometry,
    model: TreeModel,
    draggedId: String
): DropIntent? {
    if (geo.entries.isEmpty()) return null

    for (e in geo.entries) {
        if (model.isDescendant(e.id, draggedId) && e.rect.contains(p)) return null
    }

    val seq = geo.entries.filter { !model.isDescendant(it.id, draggedId) }
    if (seq.isEmpty()) return null
    val t = seq.minByOrNull { distanceSq(it.rect, p) } ?: return null
    val indent = geo.indentPx

    fun siblings(parent: String?) = model.childrenOf(parent).filter { it != draggedId }
    fun nextSibling(id: String): String? {
        val s = siblings(model.parentOf[id])
        return s.getOrNull(s.indexOf(id) + 1)
    }

    fun before(target: GeoEntry, zone: FolderZone? = null, folderId: String? = null) =
        DropIntent(target.parentId, target.id, zone, folderId)

    fun after(target: GeoEntry, px: Float, zone: FolderZone? = null, folderId: String? = null): DropIntent {
        val idx = seq.indexOfFirst { it.id == target.id }
        val next = seq.getOrNull(idx + 1)
        val natural = target.depth
        val lowest = minOf(next?.depth ?: 0, natural)
        val level = if (indent <= 0f || px >= natural * indent) natural
        else floor(px / indent).toInt().coerceIn(lowest, natural)

        var anchor = target.id
        var d = natural
        while (d > level) {
            anchor = model.parentOf[anchor] ?: break
            d--
        }
        return DropIntent(model.parentOf[anchor], nextSibling(anchor), zone, folderId)
    }

    // ── карточка ──
    if (!t.isFolder) {
        val line = seq.filter { !it.isFolder && it.lineRect == t.lineRect }
        val first = line.firstOrNull() ?: t
        val last = line.lastOrNull() ?: t
        val inMargin = p.x < t.lineRect.left - 2f
        return when {
            inMargin && p.y > t.lineRect.center.y -> after(last, p.x)
            inMargin -> before(first)
            else -> {
                val isBefore = when {
                    p.y < t.rect.top -> true
                    p.y > t.rect.bottom -> false
                    t.multiCol -> p.x < t.rect.center.x
                    else -> p.y < t.rect.center.y
                }
                if (isBefore) before(t) else after(t, p.x)
            }
        }
    }

    // ── папка ──
    val rel = ((p.y - t.rect.top) / t.rect.height.coerceAtLeast(1f)).coerceIn(0f, 1f)
    val idx = seq.indexOfFirst { it.id == t.id }
    val next = seq.getOrNull(idx + 1)
    val firstChild = if (t.expanded && next != null && next.parentId == t.id) next else null

    return when {
        rel < 0.30f -> before(t, FolderZone.Before, t.id)
        firstChild != null -> DropIntent(t.id, firstChild.id, FolderZone.Inside, t.id)
        rel > 0.70f -> after(t, p.x, FolderZone.After, t.id)
        t.expanded -> DropIntent(t.id, null, FolderZone.Inside, t.id)
        else -> DropIntent(t.id, null, FolderZone.Inside, t.id, intoCollapsed = true)
    }
}

private fun buildHint(
    intent: DropIntent,
    folders: Map<String, CharacterFolder>,
    originalParent: String?
): DropHint? {
    val zoneFolder = intent.zoneFolderId?.let { folders[it] }
    return when {
        zoneFolder != null && intent.zone == FolderZone.Before ->
            DropHint("Перед папкой «${zoneFolder.name}»", Icons.Default.VerticalAlignTop)
        zoneFolder != null && intent.zone == FolderZone.After ->
            DropHint("После папки «${zoneFolder.name}»", Icons.Default.VerticalAlignBottom)
        zoneFolder != null && intent.zone == FolderZone.Inside ->
            DropHint("В папку «${zoneFolder.name}»", Icons.Default.FolderOpen)
        intent.parentId != originalParent -> {
            val parent = intent.parentId?.let { folders[it] }
            if (parent == null) DropHint("В корень списка", Icons.Default.Home)
            else DropHint("В папку «${parent.name}»", Icons.Default.FolderOpen)
        }
        else -> null
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Дерево
// ─────────────────────────────────────────────────────────────────────────────

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
    onDragActiveChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val density = LocalDensity.current
    val scheme = MaterialTheme.colorScheme

    val dndState = rememberDragAndDropState<DragItem>(dragAfterLongPress = true)
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val motion = remember { TreeMotion(scope) }

    val baseModel = remember(characters.toList(), folders, globalOrder) {
        buildTreeModel(characters, folders, globalOrder)
    }
    val latestBase by rememberUpdatedState(baseModel)

    var preview by remember { mutableStateOf<TreeModel?>(null) }
    var intent by remember { mutableStateOf<DropIntent?>(null) }
    var pointer by remember { mutableStateOf(Offset.Zero) }
    var viewportHeight by remember { mutableIntStateOf(0) }
    var lastDraggedId by remember { mutableStateOf<String?>(null) }

    // preview действителен, только пока состав элементов совпадает с реальными данными
    // (иначе, например, после импорта новый персонаж не попал бы в список)
    val model = preview?.takeIf { it.hasSameItemsAs(baseModel) } ?: baseModel
    val isDragging = dndState.draggedItem != null
    val draggedId = dndState.draggedItem?.data?.id

    val dragCallback by rememberUpdatedState(onDragActiveChange)
    LaunchedEffect(isDragging) { dragCallback(isDragging) }
    DisposableEffect(Unit) { onDispose { dragCallback(false) } }

    // Пришли новые данные снаружи (например, после коммита) — preview больше не нужен
    LaunchedEffect(baseModel) {
        if (dndState.draggedItem == null) preview = null
    }

    val charactersById = remember(characters.toList()) { characters.associateBy { it.uuid } }
    val foldersById = remember(folders) { folders.associateBy { it.uuid } }
    val items = remember(model, charactersById, foldersById) {
        flattenTree(model, charactersById, foldersById)
    }
    val folderTint = remember(folders, isDark) {
        folders.associate { it.uuid to FolderColors.getThemeAdaptedColor(it.colorArgb, isDark) }
    }

    fun applyIntent(new: DropIntent?, dragged: String) {
        if (new != intent) intent = new
        if (new == null || new.intoCollapsed) return
        val cur = preview?.takeIf { it.hasSameItemsAs(latestBase) } ?: latestBase
        val updated = cur.move(dragged, new.parentId, new.beforeId)
        if (updated !== cur) preview = updated
    }

    LaunchedEffect(isDragging) {
        if (isDragging) {
            lastDraggedId = dndState.draggedItem?.data?.id

            // Автоскролл у краёв
            launch {
                val edge = with(density) { 88.dp.toPx() }
                val maxSpeed = with(density) { 1100.dp.toPx() } // px/сек
                var last = withFrameNanos { it }
                while (true) {
                    val now = withFrameNanos { it }
                    val dt = (now - last) / 1_000_000_000f
                    last = now
                    val h = viewportHeight.toFloat()
                    val y = pointer.y
                    val k = when {
                        h <= 0f -> 0f
                        y < edge -> -((edge - y) / edge).coerceIn(0f, 1f)
                        y > h - edge -> ((y - (h - edge)) / edge).coerceIn(0f, 1f)
                        else -> 0f
                    }
                    if (k != 0f) scrollState.dispatchRawDelta(k * abs(k) * maxSpeed * dt)
                }
            }

            // Решение «куда упадёт» пересчитывается при движении, скролле и любой смене раскладки
            snapshotFlow { Offset(pointer.x, pointer.y + scrollState.value) to motion.geometry.value }
                .collect { (p, geo) ->
                    val dragged = dndState.draggedItem?.data?.id ?: return@collect
                    applyIntent(computeIntent(p, geo, preview?.takeIf { it.hasSameItemsAs(latestBase) } ?: latestBase, dragged), dragged)
                }
        } else {
            val dragged = lastDraggedId ?: return@LaunchedEffect
            lastDraggedId = null
            val dropIntent = intent
            val finalModel = preview
            intent = null

            when {
                dropIntent != null && dropIntent.intoCollapsed -> {
                    onMoveItem(dragged, dropIntent.parentId, null)
                }
                finalModel != null && finalModel != latestBase -> {
                    val newParent = finalModel.parentOf[dragged]
                    val siblings = finalModel.childrenOf(newParent)
                    val beforeId = siblings.getOrNull(siblings.indexOf(dragged) + 1)
                    onMoveItem(dragged, newParent, beforeId)
                }
                else -> {
                    preview = null
                    return@LaunchedEffect
                }
            }
            // Страховка: если данные снаружи не изменились, не оставляем preview навсегда
            delay(600)
            if (dndState.draggedItem == null) preview = null
        }
    }

    val activeParent = intent?.parentId
    val activeAmount by animateFloatAsState(
        targetValue = if (isDragging && activeParent != null) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
    )
    val dragEnabled = isEditMode || selectedIds.isNotEmpty()
    val activeIntent = intent
    val outline = scheme.outlineVariant

    DragAndDropContainer(
        state = dndState,
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { viewportHeight = it.height }
                .pointerInput(Unit) {
                    // Только наблюдаем за указателем (ничего не потребляем)
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            event.changes.firstOrNull()?.let { pointer = it.position }
                        }
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(bottom = 120.dp)
            ) {
                Layout(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawTreeStripes(
                                geo = motion.geometry.value,
                                motion = motion,
                                tints = folderTint,
                                fallback = outline,
                                activeParent = activeParent,
                                activeAmount = activeAmount
                            )
                        },
                    content = {
                        items.forEach { item ->
                            key(item.id) {
                                when (item) {
                                    is TreeItem.Character -> CardHost(
                                        item = item,
                                        folderColorArgb = item.parentId?.let { foldersById[it]?.colorArgb },
                                        accent = scheme.primary,
                                        isSelected = item.id in selectedIds,
                                        isEditMode = isEditMode,
                                        dragEnabled = dragEnabled,
                                        dndState = dndState,
                                        onClick = { onCharacterClick(item.id) },
                                        onLongClick = { onCharacterLongClick(item.id) }
                                    )

                                    is TreeItem.Folder -> FolderHost(
                                        item = item,
                                        accent = folderTint[item.id] ?: scheme.primary,
                                        zone = activeIntent?.takeIf { it.zoneFolderId == item.id }?.zone,
                                        isSelected = item.id in selectedIds,
                                        isEditMode = isEditMode,
                                        dragEnabled = dragEnabled,
                                        dndState = dndState,
                                        onToggle = { onFolderToggle(item.id) },
                                        onExpansionToggle = { onFolderExpansionToggle(item.id) },
                                        onLongClick = { onFolderLongClick(item.id) },
                                        onMoreClick = { onFolderMoreClick(item.folder) }
                                    )
                                }
                            }
                        }
                    }
                ) { measurables, constraints ->
                    val width = constraints.maxWidth
                    val gap = ItemGap.roundToPx()
                    val indent = IndentStep.roundToPx()
                    val minCard = MinCardWidth.roundToPx()

                    val byId = HashMap<String, Measurable>(measurables.size)
                    measurables.forEach { m -> (m.layoutId as? String)?.let { byId[it] = m } }

                    val placed = ArrayList<PlacedItem>(items.size)
                    val entries = ArrayList<GeoEntry>(items.size)
                    var y = 0
                    var i = 0
                    while (i < items.size) {
                        val item = items[i]
                        val x0 = item.indentation * indent
                        val avail = (width - x0).coerceAtLeast(0)

                        if (item is TreeItem.Character) {
                            // подряд идущие карточки одного родителя → сетка «>1 в строку»
                            var j = i + 1
                            while (j < items.size) {
                                val n = items[j]
                                if (n is TreeItem.Character && n.parentId == item.parentId &&
                                    n.indentation == item.indentation
                                ) j++ else break
                            }
                            // Желаемая ширина = сколько нужно, чтобы самая «длинная» карточка группы
                            // поместилась в одну строку текста (в пределах Min..Max).
                            val probeH = CardProbeHeight.roundToPx()
                            var ideal = minCard
                            for (m in i until j) {
                                val w = byId[items[m].id]?.maxIntrinsicWidth(probeH) ?: continue
                                ideal = maxOf(ideal, w)
                            }
                            ideal = ideal.coerceIn(minCard, maxOf(minCard, minOf(MaxCardWidth.roundToPx(), avail)))
                            val cols = ((avail + gap) / (ideal + gap)).coerceAtLeast(1)

                            var k = i
                            while (k < j) {
                                val end = minOf(k + cols, j)
                                // каждая строка растягивается на всю ширину (в т.ч. неполная последняя)
                                val n = end - k
                                val cardW = ((avail - (n - 1) * gap) / n).coerceAtLeast(0)
                                val line = ArrayList<PlacedItem>(cols)
                                var lineH = 0
                                for (m in k until end) {
                                    val ci = items[m]
                                    val pl = byId[ci.id]?.measure(Constraints.fixedWidth(cardW)) ?: continue
                                    line += PlacedItem(ci, pl, x0 + (m - k) * (cardW + gap), y)
                                    lineH = maxOf(lineH, pl.height)
                                }
                                if (line.isNotEmpty()) {
                                    val lineRect = Rect(
                                        line.first().x.toFloat(), y.toFloat(),
                                        (line.last().x + cardW).toFloat(), (y + lineH).toFloat()
                                    )
                                    line.forEach { pi ->
                                        placed += pi
                                        entries += GeoEntry(
                                            id = pi.item.id,
                                            isFolder = false,
                                            expanded = false,
                                            depth = pi.item.indentation,
                                            parentId = pi.item.parentId,
                                            rect = Rect(
                                                pi.x.toFloat(), pi.y.toFloat(),
                                                (pi.x + pi.placeable.width).toFloat(),
                                                (pi.y + pi.placeable.height).toFloat()
                                            ),
                                            multiCol = line.size > 1,
                                            lineRect = lineRect
                                        )
                                    }
                                    y += lineH + gap
                                }
                                k = end
                            }
                            i = j
                        } else {
                            val pl = byId[item.id]?.measure(Constraints.fixedWidth(avail))
                            if (pl != null) {
                                val pi = PlacedItem(item, pl, x0, y)
                                placed += pi
                                val r = Rect(x0.toFloat(), y.toFloat(), (x0 + pl.width).toFloat(), (y + pl.height).toFloat())
                                entries += GeoEntry(
                                    id = item.id,
                                    isFolder = true,
                                    expanded = (item as TreeItem.Folder).folder.isExpanded,
                                    depth = item.indentation,
                                    parentId = item.parentId,
                                    rect = r,
                                    multiCol = false,
                                    lineRect = r
                                )
                                y += pl.height + gap
                            }
                            i++
                        }
                    }
                    val totalH = (y - gap).coerceAtLeast(0)

                    // полоски раскрытых папок
                    val stripes = ArrayList<StripeDef>()
                    for (idx in items.indices) {
                        val it0 = items[idx] as? TreeItem.Folder ?: continue
                        if (!it0.folder.isExpanded) continue
                        var e = idx + 1
                        while (e < items.size && items[e].indentation > it0.indentation) e++
                        if (e > idx + 1) {
                            stripes += StripeDef(it0.id, items.subList(idx + 1, e).map { it.id })
                        }
                    }

                    val alive = HashSet<String>(placed.size)
                    placed.forEach {
                        alive += it.item.id
                        motion.retarget(it.item.id, Offset(it.x.toFloat(), it.y.toFloat()))
                    }
                    motion.prune(alive)
                    motion.geometry.value = TreeGeometry(entries, stripes, indent.toFloat())

                    layout(width, totalH) {
                        placed.forEach { pi ->
                            val o = motion.current(pi.item.id)
                            pi.placeable.place(o.x.roundToInt(), o.y.roundToInt())
                        }
                    }
                }
            }

            // Подсказка «куда упадёт» — вне потока, не двигает интерфейс
            val hint = if (isDragging) {
                activeIntent?.let { buildHint(it, foldersById, draggedId?.let { id -> baseModel.parentOf[id] }) }
            } else null
            DropHintCapsule(hint)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Полоски иерархии
// ─────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawTreeStripes(
    geo: TreeGeometry,
    motion: TreeMotion,
    tints: Map<String, Color?>,
    fallback: Color,
    activeParent: String?,
    activeAmount: Float
) {
    val inset = StripeInset.toPx()
    val base = StripeWidth.toPx()
    geo.stripes.forEach { s ->
        var top = Float.MAX_VALUE
        var bottom = -Float.MAX_VALUE
        s.memberIds.forEach { id ->
            val e = geo.byId[id] ?: return@forEach
            val o = motion.current(id)
            top = minOf(top, o.y)
            bottom = maxOf(bottom, o.y + e.rect.height)
        }
        if (bottom <= top) return@forEach

        val color = tints[s.folderId] ?: fallback
        val k = if (s.folderId == activeParent) activeAmount else 0f
        val w = base + 3.dp.toPx() * k
        val left = motion.current(s.folderId).x + inset - (w - base) / 2f
        val height = bottom - top

        if (k > 0f) {
            // мягкое свечение вокруг активной полоски
            drawRoundRect(
                color = color.copy(alpha = 0.18f * k),
                topLeft = Offset(left - 3.dp.toPx() * k, top),
                size = Size(w + 6.dp.toPx() * k, height),
                cornerRadius = CornerRadius(w + 3.dp.toPx() * k)
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(color.copy(alpha = 0.45f), color, color.copy(alpha = 0.45f)),
                    startY = top,
                    endY = bottom
                ),
                topLeft = Offset(left, top),
                size = Size(w, height),
                cornerRadius = CornerRadius(w / 2f)
            )
        } else {
            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(w, height),
                cornerRadius = CornerRadius(w / 2f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Элементы списка
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun Modifier.fadeInOnEnter(): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis = 240, easing = FastOutSlowInEasing))
    }
    return this.graphicsLayer { alpha = progress.value }
}

/** «Лунка» на месте перетаскиваемого элемента: показывает, куда он ляжет. */
private fun Modifier.dropGhost(active: Boolean, color: Color, radius: Dp): Modifier =
    if (!active) this else this.drawBehind {
        val r = CornerRadius(radius.toPx())
        drawRoundRect(color.copy(alpha = 0.10f), cornerRadius = r)
        drawRoundRect(
            color = color.copy(alpha = 0.55f),
            cornerRadius = r,
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
            )
        )
    }

@OptIn(ExperimentalDndApi::class)
@Composable
private fun Modifier.dragSource(
    enabled: Boolean,
    key: String,
    data: DragItem,
    state: DragAndDropState<DragItem>,
    content: @Composable () -> Unit
): Modifier =
    if (!enabled) this
    else this.reorderableItem(
        key = key,
        data = data,
        state = state,
        onDragEnter = { },   // решение «куда класть» принимает computeIntent, а не библиотека
        draggableContent = content
    )

@Composable
private fun CardHost(
    item: TreeItem.Character,
    folderColorArgb: Int?,
    accent: Color,
    isSelected: Boolean,
    isEditMode: Boolean,
    dragEnabled: Boolean,
    dndState: DragAndDropState<DragItem>,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isGhost = dndState.draggedItem?.key == item.id
    Box(
        modifier = Modifier
            .layoutId(item.id)
            .fadeInOnEnter()
            .dragSource(
                enabled = dragEnabled,
                key = item.id,
                data = DragItem.Character(item.id, item.summary),
                state = dndState
            ) {
                Box(Modifier.graphicsLayer { alpha = 0.85f; scaleX = 1.04f; scaleY = 1.04f }) {
                    CharacterCardComposable(character = item.summary, isDragging = true)
                }
            }
            .dropGhost(isGhost, accent, 16.dp)
    ) {
        CharacterCardComposable(
            character = item.summary,
            modifier = Modifier.graphicsLayer { alpha = if (isGhost) 0f else 1f },
            isSelected = isSelected,
            isEditMode = isEditMode,
            folderColorArgb = folderColorArgb,
            onClick = onClick,
            onLongClick = onLongClick
        )
    }
}

@Composable
private fun FolderHost(
    item: TreeItem.Folder,
    accent: Color,
    zone: FolderZone?,
    isSelected: Boolean,
    isEditMode: Boolean,
    dragEnabled: Boolean,
    dndState: DragAndDropState<DragItem>,
    onToggle: () -> Unit,
    onExpansionToggle: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val isGhost = dndState.draggedItem?.key == item.id
    val scale by animateFloatAsState(
        targetValue = if (zone == FolderZone.Inside) 1.025f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
    )
    Box(
        modifier = Modifier
            .layoutId(item.id)
            .fadeInOnEnter()
            .dragSource(
                enabled = dragEnabled,
                key = item.id,
                data = DragItem.Folder(item.id, item.folder),
                state = dndState
            ) {
                Box(Modifier.graphicsLayer { alpha = 0.85f; scaleX = 1.04f; scaleY = 1.04f }) {
                    FolderHeader(
                        folder = item.folder,
                        characterCount = item.count,
                        isExpanded = item.folder.isExpanded,
                        onToggleExpand = {},
                        onExpansionToggle = {},
                        onMoreClick = {}
                    )
                }
            }
            .dropGhost(isGhost, accent, 12.dp)
    ) {
        Box(
            Modifier.graphicsLayer {
                alpha = if (isGhost) 0f else 1f
                scaleX = scale
                scaleY = scale
            }
        ) {
            FolderHeader(
                folder = item.folder,
                characterCount = item.count,
                isExpanded = item.folder.isExpanded,
                isSelected = isSelected,
                isEditMode = isEditMode,
                onToggleExpand = onToggle,
                onExpansionToggle = onExpansionToggle,
                onLongClick = onLongClick,
                onMoreClick = onMoreClick
            )
            FolderDropOverlay(
                zone = zone,
                accent = accent,
                insideLabel = if (item.folder.isExpanded) "В начало папки" else "В папку"
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Индикация «до / внутрь / после»
//  Рисуется поверх папки, не влияет на раскладку: ничего не двигается.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BoxScope.FolderDropOverlay(
    zone: FolderZone?,
    accent: Color,
    insideLabel: String
) {
    val spec = spring<Float>(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium)
    val before by animateFloatAsState(if (zone == FolderZone.Before) 1f else 0f, spec)
    val inside by animateFloatAsState(if (zone == FolderZone.Inside) 1f else 0f, spec)
    val after by animateFloatAsState(if (zone == FolderZone.After) 1f else 0f, spec)
    val any = maxOf(before, inside, after)
    if (any <= 0.01f) return

    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .matchParentSize()
            .drawBehind {
                val r = CornerRadius(12.dp.toPx())
                val pad = 12.dp.toPx()

                // приглушаем содержимое, чтобы подсветка читалась
                drawRoundRect(scheme.surface.copy(alpha = 0.50f * any), cornerRadius = r)

                // еле заметные границы зон — подсказывают, что их три
                val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                listOf(0.30f, 0.70f).forEach { f ->
                    drawLine(
                        color = accent.copy(alpha = 0.28f * any),
                        start = Offset(pad, size.height * f),
                        end = Offset(size.width - pad, size.height * f),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dash
                    )
                }

                if (inside > 0f) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            listOf(
                                accent.copy(alpha = 0.10f * inside),
                                accent.copy(alpha = 0.40f * inside),
                                accent.copy(alpha = 0.10f * inside)
                            )
                        ),
                        cornerRadius = r
                    )
                    drawRoundRect(
                        color = accent.copy(alpha = inside),
                        cornerRadius = r,
                        style = Stroke(2.dp.toPx())
                    )
                }

                val bandH = size.height * 0.34f
                if (before > 0f) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(accent.copy(alpha = 0.70f * before), Color.Transparent),
                            startY = 0f,
                            endY = bandH
                        ),
                        size = Size(size.width, bandH),
                        cornerRadius = r
                    )
                    drawRoundRect(
                        color = accent.copy(alpha = before),
                        topLeft = Offset(pad, 0f),
                        size = Size(size.width - 2 * pad, 3.dp.toPx()),
                        cornerRadius = CornerRadius(1.5.dp.toPx())
                    )
                }
                if (after > 0f) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(Color.Transparent, accent.copy(alpha = 0.70f * after)),
                            startY = size.height - bandH,
                            endY = size.height
                        ),
                        topLeft = Offset(0f, size.height - bandH),
                        size = Size(size.width, bandH),
                        cornerRadius = r
                    )
                    drawRoundRect(
                        color = accent.copy(alpha = after),
                        topLeft = Offset(pad, size.height - 3.dp.toPx()),
                        size = Size(size.width - 2 * pad, 3.dp.toPx()),
                        cornerRadius = CornerRadius(1.5.dp.toPx())
                    )
                }
            }
    ) {
        DropLabel("Перед папкой", before, Alignment.TopCenter)
        DropLabel(insideLabel, inside, Alignment.Center)
        DropLabel("После папки", after, Alignment.BottomCenter)
    }
}

@Composable
private fun BoxScope.DropLabel(text: String, progress: Float, alignment: Alignment) {
    if (progress <= 0.01f) return
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        modifier = Modifier
            .align(alignment)
            .padding(vertical = 3.dp)
            .graphicsLayer {
                alpha = progress
                scaleX = 0.9f + 0.1f * progress
                scaleY = 0.9f + 0.1f * progress
            }
    )
}

/** Капсула с текстом «куда упадёт» — видна даже когда палец закрывает папку. */
@Composable
private fun BoxScope.DropHintCapsule(hint: DropHint?) {
    val holder = remember { arrayOfNulls<DropHint>(1) }
    if (hint != null) holder[0] = hint
    val shown = holder[0]
    val scheme = MaterialTheme.colorScheme

    AnimatedVisibility(
        visible = hint != null,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 8.dp),
        enter = fadeIn() + scaleIn(
            animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
            initialScale = 0.85f
        ) + slideInVertically { -it / 2 },
        exit = fadeOut() + scaleOut(targetScale = 0.9f)
    ) {
        if (shown != null) {
            Surface(
                shape = RoundedCornerShape(50),
                color = scheme.inverseSurface.copy(alpha = 0.94f),
                contentColor = scheme.inverseOnSurface,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(shown.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = shown.text,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
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
    val imagePath by produceState(
        initialValue = if (platformFileSystem.exists(thumbPath)) thumbPath
        else if (platformFileSystem.exists(portraitPath)) portraitPath
        else null,
        thumbPath, portraitPath
    ) {
        fun resolve() = if (platformFileSystem.exists(thumbPath)) thumbPath
        else if (platformFileSystem.exists(portraitPath)) portraitPath
        else null

        value = resolve()
        // файл только что импортированного персонажа может дописаться чуть позже первой композиции
        var attempts = 0
        while (value == null && !character.imageData.isNullOrEmpty() && attempts++ < 40) {
            delay(250)
            value = resolve()
        }
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