package ru.quasaris.characternexus.tabs.proficiencies

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.ui.CharacterDetailState
import sh.calvin.reorderable.ReorderableColumn
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

private object PlaceholderKey

private val ChipShape = RoundedCornerShape(16.dp)

private val LineGap = 4.dp

@Composable
private fun NoMinTouchTarget(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        content()
    }
}

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

@Composable
private fun Modifier.trackBounds(registry: MutableMap<Any, LayoutCoordinates>, key: Any): Modifier {
    val holder = remember(key) { arrayOfNulls<LayoutCoordinates>(1) }
    DisposableEffect(registry, key) {
        onDispose { if (registry[key] === holder[0]) registry.remove(key) }
    }
    return this.onPlaced {
        holder[0] = it
        registry[key] = it
    }
}

@Composable
private fun Modifier.animatePlacement(): Modifier {
    val scope = rememberCoroutineScope()
    val shift = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val lastPosition = remember { arrayOfNulls<Offset>(1) }
    return this
        .onPlaced { coordinates ->
            val position = coordinates.positionInParent()
            val old = lastPosition[0]
            lastPosition[0] = position
            if (old != null && old != position) {
                scope.launch {
                    shift.snapTo(shift.value + (old - position))
                    shift.animateTo(
                        Offset.Zero,
                        spring(
                            dampingRatio = 0.9f,
                            stiffness = Spring.StiffnessLow,
                            visibilityThreshold = Offset(0.5f, 0.5f)
                        )
                    )
                }
            }
        }
        .graphicsLayer {
            translationX = shift.value.x
            translationY = shift.value.y
        }
}

@Stable
private class ChipDragController(
    private val scope: CoroutineScope,
    private val haptic: HapticFeedback,
    private val retargetSlopPx: Float,
) {
    var container: LayoutCoordinates? = null
    val chipCoords = HashMap<Any, LayoutCoordinates>()
    val sectionCoords = HashMap<Any, LayoutCoordinates>()

    var sectionsProvider: () -> List<ProficiencySection> = { emptyList() }
    var onCommit: (itemId: Any, sectionId: Any, index: Int) -> Unit = { _, _, _ -> }

    var draggedId by mutableStateOf<Any?>(null)
        private set
    var draggedItem by mutableStateOf<ProficiencyItem?>(null)
        private set
    var draggedSize by mutableStateOf(IntSize.Zero)
        private set
    var targetSectionId by mutableStateOf<Any?>(null)
        private set
    var targetIndex by mutableIntStateOf(0)
        private set
    var overlayTopLeft by mutableStateOf(Offset.Zero)
        private set
    var overlayScale by mutableFloatStateOf(1f)
        private set

    val isDragging: Boolean get() = draggedId != null

    private var pointer = Offset.Zero
    private var grabOffset = Offset.Zero
    private var lastChangePointer = Offset.Zero
    private var settling = false
    private var pickupJob: Job? = null

    private fun rectOf(coords: LayoutCoordinates): Rect? {
        val c = container ?: return null
        if (!c.isAttached || !coords.isAttached) return null
        return Rect(c.localPositionOf(coords, Offset.Zero), coords.size.toSize())
    }

    private fun placeholderRect(): Rect? = chipCoords[PlaceholderKey]?.let { rectOf(it) }

    fun chipAt(p: Offset): Any? {
        for ((id, coords) in chipCoords) {
            if (id === PlaceholderKey) continue
            val r = rectOf(coords) ?: continue
            if (r.contains(p)) return id
        }
        return null
    }

    fun startDrag(id: Any, p: Offset): Boolean {
        if (draggedId != null) return false
        val sections = sectionsProvider()
        val section = sections.firstOrNull { s -> s.items.any { it.id == id } } ?: return false
        val item = section.items.first { it.id == id }
        val rect = chipCoords[id]?.let { rectOf(it) } ?: return false

        pointer = p
        grabOffset = p - rect.topLeft
        lastChangePointer = p
        settling = false

        draggedItem = item
        draggedSize = IntSize(rect.width.roundToInt(), rect.height.roundToInt())
        overlayTopLeft = rect.topLeft
        overlayScale = 1f
        targetSectionId = section.id
        targetIndex = section.items.indexOfFirst { it.id == id }
        draggedId = id

        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        pickupJob?.cancel()
        pickupJob = scope.launch {
            Animatable(1f).animateTo(
                1.04f,
                spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
            ) { overlayScale = value }
        }
        return true
    }

    fun onPointerMove(p: Offset) {
        if (draggedId == null || settling) return
        pointer = p
        overlayTopLeft = p - grabOffset
        retarget()
    }
    fun onScrolled(consumed: Float) {
        if (draggedId == null || settling || consumed == 0f) return
        pointer += Offset(0f, consumed)
        overlayTopLeft = pointer - grabOffset
        retarget()
    }

    fun endDrag() {
        if (draggedId == null || settling) return
        settling = true
        pickupJob?.cancel()
        scope.launch {
            try {
                val from = overlayTopLeft
                val to = placeholderRect()?.topLeft ?: from
                val fromScale = overlayScale
                Animatable(0f).animateTo(
                    1f,
                    spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessLow)
                ) {
                    overlayTopLeft = lerp(from, to, value)
                    overlayScale = lerp(fromScale, 1f, value)
                }
            } finally {
                commit()
            }
        }
    }

    private fun commit() {
        val id = draggedId
        val sectionId = targetSectionId
        val index = targetIndex
        if (id != null && sectionId != null) onCommit(id, sectionId, index)
        reset()
    }

    fun reset() {
        pickupJob?.cancel()
        draggedId = null
        draggedItem = null
        targetSectionId = null
        overlayScale = 1f
        settling = false
    }

    private fun retarget() {
        if ((pointer - lastChangePointer).getDistance() < retargetSlopPx) return
        val (sectionId, index) = resolveTarget(pointer) ?: return
        if (sectionId == targetSectionId && index == targetIndex) return
        targetSectionId = sectionId
        targetIndex = index
        lastChangePointer = pointer
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    private fun resolveTarget(p: Offset): Pair<Any, Int>? {
        var section: ProficiencySection? = null
        var best = Float.MAX_VALUE
        for (s in sectionsProvider()) {
            val r = sectionCoords[s.id]?.let { rectOf(it) } ?: continue
            val d = when {
                p.y < r.top -> r.top - p.y
                p.y > r.bottom -> p.y - r.bottom
                else -> 0f
            }
            if (d < best) {
                best = d
                section = s
            }
        }
        val target = section ?: return null

        if (target.id == targetSectionId) {
            val ph = placeholderRect()
            if (ph != null && ph.inflate(2f).contains(p)) return null
        }

        val others = target.items.filter { it.id != draggedId }
        if (others.isEmpty()) return target.id to 0

        var nearestIndex = -1
        var nearestRect: Rect? = null
        var nearestDist = Float.MAX_VALUE
        others.forEachIndexed { i, item ->
            val r = chipCoords[item.id]?.let { rectOf(it) } ?: return@forEachIndexed
            val dx = max(max(r.left - p.x, p.x - r.right), 0f)
            val dy = max(max(r.top - p.y, p.y - r.bottom), 0f)
            val dist = dx * dx + dy * dy * 4f
            if (dist < nearestDist) {
                nearestDist = dist
                nearestIndex = i
                nearestRect = r
            }
        }
        val rect = nearestRect ?: return null
        val after = p.x > rect.center.x
        return target.id to (nearestIndex + if (after) 1 else 0)
    }

    fun autoScrollDelta(dt: Float, zone: Float, maxSpeed: Float): Float {
        val c = container ?: return 0f
        if (!c.isAttached) return 0f
        val origin = c.positionInRoot()
        val visible = c.boundsInRoot()
        val pointerY = origin.y + pointer.y
        val canScrollUp = visible.top > origin.y + 1f
        val canScrollDown = visible.bottom < origin.y + c.size.height - 1f
        val fromTop = pointerY - visible.top
        val fromBottom = visible.bottom - pointerY
        return when {
            canScrollUp && fromTop < zone -> -maxSpeed * dt * speedFactor(fromTop, zone)
            canScrollDown && fromBottom < zone -> maxSpeed * dt * speedFactor(fromBottom, zone)
            else -> 0f
        }
    }

    private fun speedFactor(distance: Float, zone: Float): Float {
        val t = ((zone - distance) / zone).coerceIn(0f, 1f)
        return 0.2f + 0.8f * t * t
    }
}

private fun Modifier.chipDragGestures(controller: ChipDragController): Modifier =
    pointerInput(controller) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (controller.isDragging) return@awaitEachGesture
            val chipId = controller.chipAt(down.position) ?: return@awaitEachGesture

            val slop = viewConfiguration.touchSlop
            var latest = down.position
            val cancelledEarly = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                var done = false
                while (!done) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.fastFirstOrNull { it.id == down.id }
                    if (change == null || change.isConsumed || !change.pressed) {
                        done = true
                    } else {
                        latest = change.position
                        done = (latest - down.position).getDistance() > slop
                    }
                }
                true
            }
            if (cancelledEarly != null) return@awaitEachGesture

            if (!controller.startDrag(chipId, latest)) return@awaitEachGesture
            try {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.fastFirstOrNull { it.id == down.id } ?: break
                    change.consume()
                    if (!change.pressed) break
                    controller.onPointerMove(change.position)
                }
            } finally {
                controller.endDrag()
            }
        }
    }

@Composable
fun ProficienciesSection(
    state: CharacterDetailState,
    isExpanded: Boolean,
    isEditMode: Boolean = false,
    isReorderMode: Boolean = false,
    scrollableState: ScrollableState? = null,
) {
    if (!isExpanded) return

    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val controller = remember {
        ChipDragController(scope, haptic, retargetSlopPx = with(density) { 6.dp.toPx() })
    }

    controller.sectionsProvider = { state.proficiencies.sections }
    controller.onCommit = { itemId, toSectionId, toIndex ->
        val sections = state.proficiencies.sections
        val item = sections.firstNotNullOfOrNull { s -> s.items.firstOrNull { it.id == itemId } }
        if (item != null) {
            val without = sections.map { s ->
                if (s.items.any { it.id == itemId }) s.copy(items = s.items.filter { it.id != itemId }) else s
            }
            val result = without.map { s ->
                if (s.id == toSectionId) {
                    val list = s.items.toMutableList()
                    list.add(toIndex.coerceIn(0, list.size), item)
                    s.copy(items = list)
                } else s
            }
            if (result != sections) {
                state.proficiencies = state.proficiencies.copy(sections = result)
            }
        }
    }

    DisposableEffect(controller) { onDispose { controller.reset() } }

    val isDragging = controller.isDragging
    LaunchedEffect(isDragging, scrollableState) {
        if (!isDragging || scrollableState == null) return@LaunchedEffect
        val zone = with(density) { 72.dp.toPx() }
        val maxSpeed = with(density) { 900.dp.toPx() }
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
            last = now
            val delta = controller.autoScrollDelta(dt, zone, maxSpeed)
            if (delta != 0f) controller.onScrolled(scrollableState.scrollBy(delta))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { controller.container = it }
                .chipDragGestures(controller)
        ) {
            val sections = state.proficiencies.sections

            ReorderableColumn(
                list = sections,
                onSettle = { from, to ->
                    val list = state.proficiencies.sections.toMutableList()
                    list.add(to, list.removeAt(from))
                    state.proficiencies = state.proficiencies.copy(sections = list)
                },
                onMove = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(LineGap)
            ) { _, section, isSectionDragging ->
                key(section.id) {
                    ReorderableItem(modifier = Modifier.fillMaxWidth()) {
                        ProficiencySectionRow(
                            section = section,
                            controller = controller,
                            isEditMode = isEditMode,
                            isReorderMode = isReorderMode,
                            isSectionDragging = isSectionDragging,
                            sectionHandle = Modifier.draggableHandle(
                                enabled = isEditMode || isReorderMode,
                                onDragStarted = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                            ),
                            onSectionChange = { updated ->
                                state.proficiencies = state.proficiencies.copy(
                                    sections = state.proficiencies.sections.map {
                                        if (it.id == updated.id) updated else it
                                    }
                                )
                            },
                            onDeleteSection = {
                                state.proficiencies = state.proficiencies.copy(
                                    sections = state.proficiencies.sections.filter { it.id != section.id }
                                )
                            }
                        )
                    }
                }
            }

            val dragged = controller.draggedItem
            if (dragged != null) {
                Box(
                    modifier = Modifier
                        .zIndex(10f)
                        .graphicsLayer {
                            translationX = controller.overlayTopLeft.x
                            translationY = controller.overlayTopLeft.y
                            scaleX = controller.overlayScale
                            scaleY = controller.overlayScale
                        }
                ) {
                    ProficiencyChipBody(
                        item = dragged,
                        isEditMode = isEditMode,
                        shadowElevation = 10.dp
                    )
                }
            }
        }

        if (isEditMode) {
            KeepStyleInput(
                placeholder = "Добавить свой раздел",
                onSave = { title ->
                    if (title.isNotBlank()) {
                        state.proficiencies = state.proficiencies.copy(
                            sections = state.proficiencies.sections + ProficiencySection(title = title)
                        )
                    }
                },
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProficiencySectionRow(
    section: ProficiencySection,
    controller: ChipDragController,
    isEditMode: Boolean,
    isReorderMode: Boolean,
    isSectionDragging: Boolean,
    sectionHandle: Modifier,
    onSectionChange: (ProficiencySection) -> Unit,
    onDeleteSection: () -> Unit,
) {
    val sectionShape = RoundedCornerShape(12.dp)
    var isEditingTitle by remember { mutableStateOf(false) }
    val lift by animateDpAsState(
        if (isSectionDragging) 10.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "sectionLift"
    )
    val liftColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val background by animateColorAsState(
        if (isSectionDragging) liftColor else liftColor.copy(alpha = 0f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "sectionBackground"
    )

    val draggedId = controller.draggedId
    val visibleItems = if (draggedId == null) section.items else section.items.filter { it.id != draggedId }
    val placeholderAt =
        if (draggedId != null && controller.targetSectionId == section.id) {
            controller.targetIndex.coerceIn(0, visibleItems.size)
        } else -1
    val entries = visibleItems.toMutableList<ProficiencyItem?>().also {
        if (placeholderAt >= 0) it.add(placeholderAt, null)
    }

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(lift, sectionShape, clip = false)
            .background(background, sectionShape)
            .trackBounds(controller.sectionCoords, section.id),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(LineGap, Alignment.CenterVertically)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(horizontal = 2.dp)
        ) {
            if (isEditMode || isReorderMode) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { if (isEditMode) isEditingTitle = true }
                        .then(sectionHandle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Перетащить раздел",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            EditableText(
                text = section.title,
                isEditMode = isEditMode,
                onSave = { onSectionChange(section.copy(title = it)) },
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                isEditingExternal = isEditingTitle,
                onEditingChanged = { isEditingTitle = it }
            )

            if (isEditMode) {
                IconButton(onClick = onDeleteSection, modifier = Modifier.size(20.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))
        }

        entries.forEach { item ->
            key(item?.id ?: PlaceholderKey) {
                if (item == null) {
                    ChipPlaceholder(
                        controller = controller,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                } else {
                    ProficiencyChip(
                        item = item,
                        controller = controller,
                        isEditMode = isEditMode,
                        onToggle = {
                            onSectionChange(section.copy(items = section.items.map {
                                if (it.id == item.id) it.copy(isActive = !it.isActive) else it
                            }))
                        },
                        onNameChange = { newName ->
                            onSectionChange(section.copy(items = section.items.map {
                                if (it.id == item.id) it.copy(name = newName) else it
                            }))
                        },
                        onDelete = {
                            onSectionChange(section.copy(items = section.items.filter { it.id != item.id }))
                        },
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }
        }

        if (isEditMode) {
            KeepStyleInput(
                placeholder = "Добавить",
                onSave = { name ->
                    if (name.isNotBlank()) {
                        onSectionChange(section.copy(items = section.items + ProficiencyItem(name = name)))
                    }
                },
                isChip = true,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
private fun ChipPlaceholder(controller: ChipDragController, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val size = controller.draggedSize
    Box(
        modifier = modifier
            .trackBounds(controller.chipCoords, PlaceholderKey)
            .animatePlacement()
            .size(
                width = with(density) { size.width.toDp() },
                height = with(density) { size.height.toDp() }
            )
            .dropGhost(true, MaterialTheme.colorScheme.primary, 16.dp)
    )
}

@Composable
private fun ProficiencyChip(
    item: ProficiencyItem,
    controller: ChipDragController,
    isEditMode: Boolean,
    onToggle: () -> Unit,
    onNameChange: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    ProficiencyChipBody(
        item = item,
        isEditMode = isEditMode,
        onToggle = onToggle,
        onNameChange = onNameChange,
        onDelete = onDelete,
        modifier = modifier
            .trackBounds(controller.chipCoords, item.id)
            .animatePlacement()
    )
}

@Composable
private fun ProficiencyChipBody(
    item: ProficiencyItem,
    isEditMode: Boolean,
    modifier: Modifier = Modifier,
    onToggle: (() -> Unit)? = null,
    onNameChange: (String) -> Unit = {},
    onDelete: () -> Unit = {},
    shadowElevation: Dp = 0.dp,
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val baseColor by animateColorAsState(
        if (item.isActive) accentColor.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "chipContainer"
    )
    val containerColor =
        if (shadowElevation > 0.dp) baseColor.compositeOver(MaterialTheme.colorScheme.surface) else baseColor
    val contentColor by animateColorAsState(
        if (item.isActive) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "chipContent"
    )
    val glowColor by animateColorAsState(
        if (item.isActive) accentColor else Color.Gray.copy(alpha = 0.5f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "chipGlow"
    )
    val borderColor by animateColorAsState(
        if (item.isActive) accentColor.copy(alpha = 0.3f) else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "chipBorder"
    )

    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(glowColor, CircleShape)
            )

            EditableText(
                text = item.name,
                isEditMode = isEditMode,
                onSave = onNameChange,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = contentColor
                )
            )

            if (isEditMode) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete",
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onDelete() },
                    tint = contentColor.copy(alpha = 0.6f)
                )
            }
        }
    }

    if (onToggle != null) {
        NoMinTouchTarget {
            Surface(
                onClick = onToggle,
                modifier = modifier,
                shape = ChipShape,
                color = containerColor,
                border = BorderStroke(1.dp, borderColor),
                content = content
            )
        }
    } else {
        Surface(
            modifier = modifier,
            shape = ChipShape,
            color = containerColor,
            shadowElevation = shadowElevation,
            border = BorderStroke(1.dp, borderColor),
            content = content
        )
    }
}

@Composable
fun EditableText(
    text: String,
    isEditMode: Boolean,
    onSave: (String) -> Unit,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    isEditingExternal: Boolean? = null,
    onEditingChanged: ((Boolean) -> Unit)? = null
) {
    var internalIsEditing by remember { mutableStateOf(false) }
    val isEditing = isEditingExternal ?: internalIsEditing
    val setIsEditing: (Boolean) -> Unit = { newVal ->
        if (isEditingExternal != null) {
            onEditingChanged?.invoke(newVal)
        } else {
            internalIsEditing = newVal
        }
    }

    var value by remember(text) { mutableStateOf(text) }
    val focusRequester = remember { FocusRequester() }
    var focusReady by remember { mutableStateOf(false) }

    if (isEditing) {
        BasicTextField(
            value = value,
            onValueChange = { value = it },
            textStyle = textStyle,
            modifier = modifier
                .focusRequester(focusRequester)
                .onFocusChanged {
                    if (focusReady && !it.isFocused && isEditing) {
                        if (value.isNotBlank()) {
                            setIsEditing(false)
                            onSave(value)
                        }
                    }
                }
                .onKeyEvent {
                    if ((it.key == Key.Escape || it.key == Key.Enter) && it.type == KeyEventType.KeyUp) {
                        setIsEditing(false)
                        onSave(value)
                        true
                    } else false
                },
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                setIsEditing(false)
                onSave(value)
            })
        )
        LaunchedEffect(isEditing) {
            if (isEditing) {
                focusReady = false
                repeat(3) {
                    focusRequester.requestFocus()
                    kotlinx.coroutines.delay(50.milliseconds)
                }
                focusReady = true
            }
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = modifier.run {
                if (isEditMode && isEditingExternal == null) clickable { setIsEditing(true); value = text } else this
            }
        ) {
            Text(
                text = text,
                style = textStyle
            )
        }
    }
}

@Composable
fun KeepStyleInput(
    placeholder: String,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle.Default,
    isChip: Boolean = false
) {
    var isEditing by remember { mutableStateOf(false) }
    var value by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    var focusReady by remember { mutableStateOf(false) }

    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    val inputTextStyle = if (isChip) {
        TextStyle(
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
    } else {
        textStyle.copy(color = MaterialTheme.colorScheme.onSurface)
    }

    if (isEditing) {
        val inputModifier = if (isChip) {
            Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(horizontal = 6.dp, vertical = 0.dp)
        } else {
            Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        }

        Row(
            modifier = modifier.then(inputModifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                textStyle = inputTextStyle,
                modifier = Modifier
                    .weight(1f, fill = !isChip)
                    .widthIn(min = 60.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        if (focusReady && !it.isFocused && isEditing) {
                            if (value.isNotBlank()) {
                                isEditing = false
                                onSave(value)
                                value = ""
                            }
                        }
                    }
                    .onKeyEvent {
                        if ((it.key == Key.Escape || it.key == Key.Enter) && it.type == KeyEventType.KeyUp) {
                            isEditing = false
                            if (value.isNotBlank()) onSave(value)
                            value = ""
                            true
                        } else false
                    },
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    isEditing = false
                    if (value.isNotBlank()) onSave(value)
                    value = ""
                })
            )

            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Save",
                modifier = Modifier
                    .size(18.dp)
                    .clickable {
                        isEditing = false
                        if (value.isNotBlank()) onSave(value)
                        value = ""
                    },
                tint = MaterialTheme.colorScheme.primary
            )
        }
        LaunchedEffect(isEditing) {
            if (isEditing) {
                focusReady = false
                repeat(3) {
                    focusRequester.requestFocus()
                    kotlinx.coroutines.delay(50.milliseconds)
                }
                focusReady = true
            }
        }
    } else {
        NoMinTouchTarget {
            Surface(
                onClick = { isEditing = true },
                shape = RoundedCornerShape(16.dp),
                color = if (isChip) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                modifier = modifier
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = if (isChip) 6.dp else 10.dp,
                        vertical = if (isChip) 0.dp else 6.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(if (isChip) 16.dp else 18.dp),
                        tint = if (isChip) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        placeholder,
                        fontSize = if (isChip) 13.sp else 14.sp,
                        fontWeight = if (isChip) FontWeight.Medium else FontWeight.Bold,
                        color = if (isChip) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}