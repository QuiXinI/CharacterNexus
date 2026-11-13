package ru.quasaris.characternexus.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.quasaris.characternexus.model.DynamicContentBlock
import ru.quasaris.characternexus.model.NoteBlockState
import ru.quasaris.characternexus.ui.menu.TreeModel
import kotlin.math.floor
import kotlin.math.roundToInt

internal val BlockIndent = 24.dp
private val BtGap = 4.dp
private val BtStripeInset = 10.dp
private val BtStripeWidth = 2.dp

internal class BlockTree(
    val model: TreeModel,
    val byKey: Map<String, NoteBlockState>,
    val ends: Map<String, NoteBlockState>,
    val folderIdByKey: Map<String, String>
) {
    val folderIds: Set<String> by lazy(LazyThreadSafetyMode.NONE) { folderIdByKey.values.toSet() }
}

internal fun isFolderEndBlock(b: NoteBlockState): Boolean =
    (b.block as? DynamicContentBlock.FolderRef)?.isEnd == true

internal fun parseBlockTree(blocks: List<NoteBlockState>): BlockTree {
    val hasEndAfter = BooleanArray(blocks.size)
    val endsRight = HashMap<String, Int>()
    for (i in blocks.indices.reversed()) {
        val fr = blocks[i].block as? DynamicContentBlock.FolderRef ?: continue
        if (fr.isEnd) endsRight[fr.id] = (endsRight[fr.id] ?: 0) + 1
        else hasEndAfter[i] = fr.id.isNotEmpty() && (endsRight[fr.id] ?: 0) > 0
    }

    val order = ArrayList<String>(blocks.size)
    val parentOf = LinkedHashMap<String, String?>(blocks.size)
    val byKey = LinkedHashMap<String, NoteBlockState>(blocks.size)
    val ends = HashMap<String, NoteBlockState>()
    val folderIdByKey = HashMap<String, String>()
    val stack = ArrayList<NoteBlockState>()

    blocks.forEachIndexed { i, b ->
        val fr = b.block as? DynamicContentBlock.FolderRef
        if (fr != null && fr.isEnd) {
            val pos = stack.indexOfLast { (it.block as DynamicContentBlock.FolderRef).id == fr.id }
            if (pos >= 0) {
                ends[stack[pos].key] = b
                while (stack.size > pos) stack.removeAt(stack.lastIndex)
            }
            return@forEachIndexed
        }
        order += b.key
        byKey[b.key] = b
        parentOf[b.key] = stack.lastOrNull()?.key
        if (fr != null) {
            if (fr.id.isNotEmpty()) folderIdByKey[b.key] = fr.id
            if (hasEndAfter[i]) stack += b
        }
    }
    return BlockTree(TreeModel(order, parentOf), byKey, ends, folderIdByKey)
}

internal fun BlockTree.rebuild(newModel: TreeModel): List<NoteBlockState> {
    val out = ArrayList<NoteBlockState>(byKey.size + ends.size)
    fun walk(parent: String?, depth: Int) {
        if (depth > 32) return
        newModel.childrenOf(parent).forEach { k ->
            val b = byKey[k] ?: return@forEach
            out += b
            val fr = b.block as? DynamicContentBlock.FolderRef
            if (fr != null && fr.id.isNotEmpty()) {
                val kids = newModel.childrenOf(k)
                if (kids.isNotEmpty()) {
                    walk(k, depth + 1)
                    out += ends[k] ?: NoteBlockState(block = DynamicContentBlock.FolderRef(id = fr.id, isEnd = true))
                }
            }
        }
    }
    walk(null, 0)
    return out
}

internal data class BlockRowInfo(
    val key: String,
    val depth: Int,
    val parentKey: String?,
    val isFolder: Boolean,
    val folderId: String?,
    val childCount: Int
)

internal fun BlockTree.visibleRows(model: TreeModel, expandedById: Map<String, Boolean>): List<BlockRowInfo> {
    val out = ArrayList<BlockRowInfo>(model.order.size)
    fun walk(parent: String?, depth: Int) {
        if (depth > 32) return
        model.childrenOf(parent).forEach { k ->
            val b = byKey[k] ?: return@forEach
            val isFolder = b.block is DynamicContentBlock.FolderRef
            val kids = if (isFolder) model.childrenOf(k).size else 0
            val fid = folderIdByKey[k]
            out += BlockRowInfo(k, depth, parent, isFolder, fid, kids)
            val open = fid?.let { expandedById[it] } == true
            if (isFolder && kids > 0 && open) walk(k, depth + 1)
        }
    }
    walk(null, 0)
    return out
}

private fun sameItems(a: TreeModel, b: TreeModel): Boolean =
    a.order.size == b.order.size && b.order.all { it in a.parentOf }

internal fun Modifier.blockStripes(depth: Int, color: Color): Modifier =
    if (depth <= 0) this else this.drawBehind {
        val indent = BlockIndent.toPx()
        val inset = BtStripeInset.toPx()
        val w = BtStripeWidth.toPx()
        val ext = BtGap.toPx() / 2f
        for (level in 0 until depth) {
            drawRoundRect(
                color = color,
                topLeft = Offset(level * indent + inset - w / 2f, -ext),
                size = Size(w, size.height + 2 * ext),
                cornerRadius = CornerRadius(w / 2f)
            )
        }
    }

private data class BtGeo(
    val id: String,
    val isFolder: Boolean,
    val expanded: Boolean,
    val depth: Int,
    val parentId: String?,
    val rect: Rect
)

private data class BtStripe(val folderKey: String, val memberKeys: List<String>)

private data class BtGeometry(
    val entries: List<BtGeo> = emptyList(),
    val stripes: List<BtStripe> = emptyList(),
    val indentPx: Float = 0f
) {
    val byId: Map<String, BtGeo> by lazy(LazyThreadSafetyMode.NONE) { entries.associateBy { it.id } }
}

private class BtPlaced(val key: String, val placeable: Placeable, val x: Int, val y: Int)

@Stable
private class BtMotion(private val scope: CoroutineScope) {
    private val slots = HashMap<String, Animatable<Offset, AnimationVector2D>>()
    private val spec = spring<Offset>(dampingRatio = 0.88f, stiffness = 380f)
    val geometry = mutableStateOf(BtGeometry())

    fun retarget(id: String, target: Offset) {
        val slot = slots[id]
        if (slot == null) slots[id] = Animatable(target, Offset.VectorConverter)
        else if (slot.targetValue != target) scope.launch { slot.animateTo(target, spec) }
    }

    fun current(id: String): Offset = slots[id]?.value ?: Offset.Zero
    fun prune(alive: Set<String>) { slots.keys.retainAll(alive) }
}

private enum class BtZone { Before, Inside, After }

private data class BtIntent(
    val parentId: String?,
    val beforeId: String?,
    val zone: BtZone? = null,
    val zoneFolderKey: String? = null,
    val intoCollapsed: Boolean = false
)

private data class BtHint(val text: String, val icon: ImageVector)

private fun btDistanceSq(r: Rect, p: Offset): Float {
    val dx = maxOf(r.left - p.x, 0f, p.x - r.right)
    val dy = maxOf(r.top - p.y, 0f, p.y - r.bottom)
    return dx * dx + dy * dy
}

private fun btComputeIntent(p: Offset, geo: BtGeometry, model: TreeModel, draggedId: String): BtIntent? {
    if (geo.entries.isEmpty()) return null
    for (e in geo.entries) {
        if (model.isDescendant(e.id, draggedId) && e.rect.contains(p)) return null
    }
    val seq = geo.entries.filter { !model.isDescendant(it.id, draggedId) }
    if (seq.isEmpty()) return null
    val t = seq.minByOrNull { btDistanceSq(it.rect, p) } ?: return null
    val indent = geo.indentPx

    fun siblings(parent: String?) = model.childrenOf(parent).filter { it != draggedId }
    fun nextSibling(id: String): String? {
        val s = siblings(model.parentOf[id])
        return s.getOrNull(s.indexOf(id) + 1)
    }

    fun before(target: BtGeo, zone: BtZone? = null, folderKey: String? = null) =
        BtIntent(target.parentId, target.id, zone, folderKey)

    fun after(target: BtGeo, px: Float, zone: BtZone? = null, folderKey: String? = null): BtIntent {
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
        return BtIntent(model.parentOf[anchor], nextSibling(anchor), zone, folderKey)
    }

    if (!t.isFolder) {
        val inMargin = p.x < t.rect.left - 2f
        return when {
            inMargin && p.y > t.rect.center.y -> after(t, p.x)
            inMargin -> before(t)
            else -> {
                val isBefore = when {
                    p.y < t.rect.top -> true
                    p.y > t.rect.bottom -> false
                    else -> p.y < t.rect.center.y
                }
                if (isBefore) before(t) else after(t, p.x)
            }
        }
    }

    val rel = ((p.y - t.rect.top) / t.rect.height.coerceAtLeast(1f)).coerceIn(0f, 1f)
    val idx = seq.indexOfFirst { it.id == t.id }
    val next = seq.getOrNull(idx + 1)
    val firstChild = if (t.expanded && next != null && next.parentId == t.id) next else null

    return when {
        rel < 0.30f -> before(t, BtZone.Before, t.id)
        firstChild != null -> BtIntent(t.id, firstChild.id, BtZone.Inside, t.id)
        rel > 0.70f -> after(t, p.x, BtZone.After, t.id)
        t.expanded -> BtIntent(t.id, null, BtZone.Inside, t.id)
        else -> BtIntent(t.id, null, BtZone.Inside, t.id, intoCollapsed = true)
    }
}

private fun btBuildHint(intent: BtIntent, titleOfKey: (String) -> String, originalParent: String?): BtHint? {
    fun name(key: String?) = key?.let(titleOfKey)?.ifBlank { null } ?: "Папка"
    val zk = intent.zoneFolderKey
    return when {
        zk != null && intent.zone == BtZone.Before ->
            BtHint("Перед папкой «${name(zk)}»", Icons.Default.VerticalAlignTop)
        zk != null && intent.zone == BtZone.After ->
            BtHint("После папки «${name(zk)}»", Icons.Default.VerticalAlignBottom)
        zk != null && intent.zone == BtZone.Inside ->
            BtHint("В папку «${name(zk)}»", Icons.Default.FolderOpen)
        intent.parentId != originalParent ->
            if (intent.parentId == null) BtHint("В корень заметки", Icons.Default.Home)
            else BtHint("В папку «${name(intent.parentId)}»", Icons.Default.FolderOpen)
        else -> null
    }
}

@Composable
internal fun BlockReorderTree(
    tree: BlockTree,
    expandedById: Map<String, Boolean>,
    folderTitle: (folderId: String) -> String,
    onApply: (List<NoteBlockState>) -> Unit,
    modifier: Modifier = Modifier,
    rowContent: @Composable (item: NoteBlockState, row: BlockRowInfo, handle: Modifier) -> Unit
) {
    val density = LocalDensity.current
    val scheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val motion = remember { BtMotion(scope) }

    var draggedKey by remember { mutableStateOf<String?>(null) }
    var pointerWin by remember { mutableStateOf(Offset.Zero) }
    var treeWin by remember { mutableStateOf(Offset.Zero) }
    var grabDy by remember { mutableFloatStateOf(0f) }
    var preview by remember { mutableStateOf<TreeModel?>(null) }
    var intent by remember { mutableStateOf<BtIntent?>(null) }

    val base = tree.model
    val latestBase by rememberUpdatedState(base)
    val latestTree by rememberUpdatedState(tree)
    val model = preview?.takeIf { sameItems(base, it) } ?: base
    val isDragging = draggedKey != null

    LaunchedEffect(base) {
        if (draggedKey == null) preview = null
    }

    val rows = remember(model, tree, expandedById) { tree.visibleRows(model, expandedById) }

    fun applyIntent(new: BtIntent?, dragged: String) {
        if (new != intent) intent = new
        if (new == null || new.intoCollapsed) return
        val cur = preview?.takeIf { sameItems(latestBase, it) } ?: latestBase
        val updated = cur.move(dragged, new.parentId, new.beforeId)
        if (updated !== cur) preview = updated
    }

    fun startDrag(key: String, pointerAtStart: Offset) {
        pointerWin = pointerAtStart
        val slotTop = motion.geometry.value.byId[key]?.rect?.top ?: 0f
        grabDy = (pointerAtStart.y - treeWin.y) - slotTop
        intent = null
        preview = null
        draggedKey = key
    }

    fun cancelDrag() {
        draggedKey = null
        intent = null
        preview = null
    }

    fun endDrag() {
        val dragged = draggedKey ?: return
        val dropIntent = intent
        val finalModel = preview
        draggedKey = null
        intent = null
        val result: TreeModel? = when {
            dropIntent != null && dropIntent.intoCollapsed ->
                latestBase.move(dragged, dropIntent.parentId, null).takeIf { it !== latestBase }
            finalModel != null && finalModel != latestBase -> finalModel
            else -> null
        }
        if (result == null) {
            preview = null
            return
        }
        preview = result
        onApply(latestTree.rebuild(result))
        scope.launch {
            delay(600)
            if (draggedKey == null) preview = null
        }
    }

    LaunchedEffect(draggedKey) {
        val dragged = draggedKey ?: return@LaunchedEffect
        snapshotFlow { (pointerWin - treeWin) to motion.geometry.value }
            .collect { (p, geo) ->
                val m = preview?.takeIf { sameItems(latestBase, it) } ?: latestBase
                applyIntent(btComputeIntent(p, geo, m, dragged), dragged)
            }
    }

    val activeParent = intent?.parentId
    val activeAmount by animateFloatAsState(
        targetValue = if (isDragging && activeParent != null) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
    )
    val activeIntent = intent
    val outline = scheme.outlineVariant
    val accent = scheme.primary

    Box(modifier = modifier.fillMaxWidth()) {
        Layout(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { if (it.isAttached) treeWin = it.positionInWindow() }
                .drawBehind {
                    btDrawStripes(motion.geometry.value, motion, outline, accent, activeParent, activeAmount)
                    val dk = draggedKey
                    val e = dk?.let { motion.geometry.value.byId[it] }
                    if (dk != null && e != null) {
                        val o = motion.current(dk)
                        val r = CornerRadius(12.dp.toPx())
                        drawRoundRect(accent.copy(alpha = 0.10f), topLeft = o, size = Size(e.rect.width, e.rect.height), cornerRadius = r)
                        drawRoundRect(
                            color = accent.copy(alpha = 0.55f),
                            topLeft = o,
                            size = Size(e.rect.width, e.rect.height),
                            cornerRadius = r,
                            style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
                            )
                        )
                    }
                },
            content = {
                rows.forEach { row ->
                    val item = tree.byKey[row.key] ?: return@forEach
                    key(row.key) {
                        val handle = rememberBlockDragHandle(
                            key = row.key,
                            onStart = { startDrag(row.key, it) },
                            onDelta = { pointerWin += it },
                            onEnd = { endDrag() },
                            onCancel = { cancelDrag() }
                        )
                        val isGhost = draggedKey == row.key
                        Box(Modifier.layoutId(row.key)) {
                            Box(
                                Modifier.graphicsLayer {
                                    alpha = if (isGhost) 0.92f else 1f
                                }
                            ) {
                                rowContent(item, row, handle)
                            }
                            if (row.isFolder) {
                                BtFolderDropOverlay(
                                    zone = activeIntent?.takeIf { it.zoneFolderKey == row.key }?.zone,
                                    accent = accent,
                                    insideLabel = if (row.folderId?.let { expandedById[it] } == true) "В начало папки" else "В папку"
                                )
                            }
                        }
                    }
                }
            }
        ) { measurables, constraints ->
            val width = constraints.maxWidth
            val gap = BtGap.roundToPx()
            val indent = BlockIndent.roundToPx()

            val byId = HashMap<String, Measurable>(measurables.size)
            measurables.forEach { m -> (m.layoutId as? String)?.let { byId[it] = m } }

            val placed = ArrayList<BtPlaced>(rows.size)
            val entries = ArrayList<BtGeo>(rows.size)
            var y = 0
            for (row in rows) {
                val x0 = row.depth * indent
                val avail = (width - x0).coerceAtLeast(0)
                val pl = byId[row.key]?.measure(Constraints.fixedWidth(avail)) ?: continue
                placed += BtPlaced(row.key, pl, x0, y)
                entries += BtGeo(
                    id = row.key,
                    isFolder = row.isFolder,
                    expanded = row.folderId?.let { expandedById[it] } == true,
                    depth = row.depth,
                    parentId = row.parentKey,
                    rect = Rect(x0.toFloat(), y.toFloat(), (x0 + pl.width).toFloat(), (y + pl.height).toFloat())
                )
                y += pl.height + gap
            }
            val totalH = (y - gap).coerceAtLeast(0)

            val stripes = ArrayList<BtStripe>()
            for (idx in rows.indices) {
                val r0 = rows[idx]
                if (!r0.isFolder || r0.childCount == 0 || r0.folderId?.let { expandedById[it] } != true) continue
                var e = idx + 1
                while (e < rows.size && rows[e].depth > r0.depth) e++
                if (e > idx + 1) stripes += BtStripe(r0.key, rows.subList(idx + 1, e).map { it.key })
            }

            val alive = HashSet<String>(placed.size)
            placed.forEach {
                alive += it.key
                motion.retarget(it.key, Offset(it.x.toFloat(), it.y.toFloat()))
            }
            motion.prune(alive)
            motion.geometry.value = BtGeometry(entries, stripes, indent.toFloat())

            layout(width, totalH) {
                placed.forEach { pi ->
                    val o = motion.current(pi.key)
                    if (pi.key == draggedKey) {
                        val fy = (pointerWin.y - treeWin.y - grabDy).roundToInt()
                        pi.placeable.placeWithLayer(o.x.roundToInt(), fy, zIndex = 1f) {
                            scaleX = 1.02f
                            scaleY = 1.02f
                            shadowElevation = 12.dp.toPx()
                        }
                    } else {
                        pi.placeable.place(o.x.roundToInt(), o.y.roundToInt())
                    }
                }
            }
        }
    }

    if (isDragging) {
        val hint = activeIntent?.let {
            btBuildHint(
                it,
                titleOfKey = { k -> tree.folderIdByKey[k]?.let(folderTitle).orEmpty() },
                originalParent = draggedKey?.let { k -> base.parentOf[k] }
            )
        }
        val topPx = WindowInsets.statusBars.getTop(density) + with(density) { 8.dp.roundToPx() }
        BtDropHintPopup(hint = hint, topPx = topPx)
    }
}

@Composable
private fun rememberBlockDragHandle(
    key: String,
    onStart: (Offset) -> Unit,
    onDelta: (Offset) -> Unit,
    onEnd: () -> Unit,
    onCancel: () -> Unit
): Modifier {
    var origin by remember { mutableStateOf(Offset.Zero) }
    val start by rememberUpdatedState(onStart)
    val delta by rememberUpdatedState(onDelta)
    val end by rememberUpdatedState(onEnd)
    val cancel by rememberUpdatedState(onCancel)
    return Modifier
        .onGloballyPositioned { if (it.isAttached) origin = it.positionInWindow() }
        .pointerInput(key) {
            detectDragGestures(
                onDragStart = { off -> start(origin + off) },
                onDrag = { change, amount ->
                    change.consume()
                    delta(amount)
                },
                onDragEnd = { end() },
                onDragCancel = { cancel() }
            )
        }
}

private fun DrawScope.btDrawStripes(
    geo: BtGeometry,
    motion: BtMotion,
    fallback: Color,
    accent: Color,
    activeParent: String?,
    activeAmount: Float
) {
    val inset = BtStripeInset.toPx()
    val base = BtStripeWidth.toPx()
    geo.stripes.forEach { s ->
        var top = Float.MAX_VALUE
        var bottom = -Float.MAX_VALUE
        s.memberKeys.forEach { id ->
            val e = geo.byId[id] ?: return@forEach
            val o = motion.current(id)
            top = minOf(top, o.y)
            bottom = maxOf(bottom, o.y + e.rect.height)
        }
        if (bottom <= top) return@forEach

        val k = if (s.folderKey == activeParent) activeAmount else 0f
        val color = lerp(fallback, accent, k)
        val w = base + 3.dp.toPx() * k
        val left = motion.current(s.folderKey).x + inset - (w - base) / 2f
        val height = bottom - top

        if (k > 0f) {
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

@Composable
private fun BoxScope.BtFolderDropOverlay(zone: BtZone?, accent: Color, insideLabel: String) {
    val spec = spring<Float>(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium)
    val before by animateFloatAsState(if (zone == BtZone.Before) 1f else 0f, spec)
    val inside by animateFloatAsState(if (zone == BtZone.Inside) 1f else 0f, spec)
    val after by animateFloatAsState(if (zone == BtZone.After) 1f else 0f, spec)
    val any = maxOf(before, inside, after)
    if (any <= 0.01f) return

    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .matchParentSize()
            .drawBehind {
                val r = CornerRadius(12.dp.toPx())
                val pad = 12.dp.toPx()

                drawRoundRect(scheme.surface.copy(alpha = 0.50f * any), cornerRadius = r)

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
                    drawRoundRect(color = accent.copy(alpha = inside), cornerRadius = r, style = Stroke(2.dp.toPx()))
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
        BtDropLabel("Перед папкой", before, Alignment.TopCenter)
        BtDropLabel(insideLabel, inside, Alignment.Center)
        BtDropLabel("После папки", after, Alignment.BottomCenter)
    }
}

@Composable
private fun BoxScope.BtDropLabel(text: String, progress: Float, alignment: Alignment) {
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

private class BtTopCenterProvider(private val topPx: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset = IntOffset((windowSize.width - popupContentSize.width) / 2, topPx)
}
@Composable
private fun BtDropHintPopup(hint: BtHint?, topPx: Int) {
    val holder = remember { arrayOfNulls<BtHint>(1) }
    if (hint != null) holder[0] = hint
    val shown = holder[0]
    val scheme = MaterialTheme.colorScheme

    Popup(
        popupPositionProvider = remember(topPx) { BtTopCenterProvider(topPx) },
        properties = PopupProperties(focusable = false, clippingEnabled = false)
    ) {
        AnimatedVisibility(
            visible = hint != null,
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
}
