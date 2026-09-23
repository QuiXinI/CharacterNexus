package ru.quasaris.characternexus.tabs

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.layout.layout
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalDensity
import ru.quasaris.characternexus.ui.outerShadow
import ru.quasaris.characternexus.ui.DialogDimStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.backend.SettingsViewModel
import ru.quasaris.characternexus.tabs.resources.ResourceConfigDialog
import ru.quasaris.characternexus.tabs.resources.ResourceBlock
import ru.quasaris.characternexus.ui.DeleteConfirmationDialog
import ru.quasaris.characternexus.ui.BackHandler
import ru.quasaris.characternexus.ui.TabControlHeader
import ru.quasaris.characternexus.ui.theme.rememberEffectiveBlurRadius
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import sh.calvin.reorderable.*
import kotlinx.coroutines.launch

@Composable
fun DynamicFieldsTab(
    fields: List<DynamicNoteState>,
    onFieldsChange: (List<DynamicNoteState>) -> Unit,
    hazeState: HazeState? = null,
    popupHazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    blurPopups: Boolean = false,
    isEditMode: Boolean = false,
    onToggleEditMode: () -> Unit = {},
    onToggleAllExpansion: () -> Unit = {},
    anyCollapsed: Boolean = false,
    addButtonText: String = "ДОБАВИТЬ ПОЛЕ",
    emptyListText: String = "Список пуст",
    titlePlaceholder: String = "Заголовок",
    contentPlaceholder: String = "Текст...",
    settingsViewModel: SettingsViewModel? = null,
    statsMap: Map<String, String> = emptyMap(),
    isCollapsible: Boolean = true,
    isTitleReadOnly: Boolean = false,
    isAddButtonVisible: Boolean = true,
    isReorderButtonVisible: Boolean = true,
    isScrollEnabled: Boolean = true,
    isContentVisible: (DynamicNoteState) -> Boolean = { true },
    collapseOnEdit: Boolean? = null,
    isAdvancedMode: Boolean = false,
    isDesktop: Boolean = false,
    onFullscreenDialogOpenChange: (Boolean) -> Unit = {},
    onFullscreenVisibilityChanged: (Boolean) -> Unit = {},
    state: ru.quasaris.characternexus.ui.CharacterDetailState? = null,
    header: @Composable () -> Unit = {},
    footer: @Composable () -> Unit = {},
    extraContent: @Composable (DynamicNoteState) -> Unit = {}
) {
    val listState = rememberLazyListState()

    val items = remember { mutableStateListOf<DynamicNoteState>().apply { addAll(fields) } }

    LaunchedEffect(fields) {
        if (items.size != fields.size || items.indices.any { items[it].id != fields[it].id }) {
            items.clear()
            items.addAll(fields)
        } else {
            fields.forEachIndexed { index, field ->
                if (items[index] != field) {
                    items[index] = field
                }
            }
        }
    }

    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIdx = from.index - 1
        val toIdx = to.index - 1
        if (fromIdx in items.indices && toIdx in items.indices) {
            items.add(toIdx, items.removeAt(fromIdx))
        }
    }

    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging) {
            onFieldsChange(items.toList())
        }
    }

    var fullscreenFieldIndex by remember { mutableStateOf<Int?>(null) }
    
    LaunchedEffect(fullscreenFieldIndex, extraContent, isContentVisible) {
        onFullscreenVisibilityChanged(fullscreenFieldIndex != null)
        state?.activeDynamicField = fullscreenFieldIndex?.let { items.getOrNull(it) }
        state?.activeDynamicFieldExtraContent = if (fullscreenFieldIndex != null) extraContent else null
        state?.activeDynamicFieldContentVisible = if (fullscreenFieldIndex != null) isContentVisible(items[fullscreenFieldIndex!!]) else true
        state?.isFullscreenDynamicFieldOpen = fullscreenFieldIndex != null
    }

    LaunchedEffect(state?.isFullscreenDynamicFieldOpen) {
        if (state?.isFullscreenDynamicFieldOpen == false) {
            fullscreenFieldIndex = null
        }
    }

    var fieldToDeleteIndex by remember { mutableStateOf<Int?>(null) }
    
    var mainViewportTopY by remember { mutableFloatStateOf(0f) }

    val fullscreenEditingOnly by settingsViewModel?.fullscreenEditingOnly?.collectAsState() ?: remember { mutableStateOf(false) }
    val collapseDynamicFieldsOnEditSetting by settingsViewModel?.collapseDynamicFieldsOnEdit?.collectAsState() ?: remember { mutableStateOf(true) }
    val collapseOnEditActual = collapseOnEdit ?: collapseDynamicFieldsOnEditSetting

    var savedExpansionStates by remember { mutableStateOf<Map<String, Boolean>?>(null) }

    LaunchedEffect(isEditMode) {
        if (isEditMode) {
            if (collapseOnEditActual) {
                savedExpansionStates = items.associate { it.id to it.isExpanded }
                val collapsedList = items.map { it.copy(isExpanded = false) }
                if (collapsedList != items.toList()) {
                    items.clear()
                    items.addAll(collapsedList)
                    onFieldsChange(collapsedList)
                }
            }
        } else {
            savedExpansionStates?.let { saved ->
                if (collapseOnEditActual) {
                    val restoredList = items.map { it.copy(isExpanded = saved[it.id] ?: it.isExpanded) }
                    if (restoredList != items.toList()) {
                        items.clear()
                        items.addAll(restoredList)
                        onFieldsChange(restoredList)
                    }
                }
                savedExpansionStates = null
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .onGloballyPositioned { mainViewportTopY = it.positionInWindow().y }
    ) {
        if (items.isEmpty() && isScrollEnabled) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(emptyListText, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .clipToBounds(),
            userScrollEnabled = isScrollEnabled,
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    header()
                }
            }

            itemsIndexed(items, key = { _, field -> field.id }) { index, field ->
                ReorderableItem(reorderableState, key = field.id) { isDragging ->
                    val dragModifier = if (isEditMode && isReorderButtonVisible) {
                        Modifier.draggableHandle()
                    } else Modifier

                    DynamicFieldItem(
                        field = field,
                        isEditMode = isEditMode,
                        isDragging = isDragging,
                        isAnyItemDragging = reorderableState.isAnyItemDragging,
                        titlePlaceholder = titlePlaceholder,
                        contentPlaceholder = contentPlaceholder,
                        onFieldChange = { updatedField ->
                            val currentList = items.toList()
                            val idx = currentList.indexOfFirst { it.id == updatedField.id }
                            if (idx != -1) {
                                items[idx] = updatedField
                                onFieldsChange(items.toList())
                            }
                        },
                        onDelete = { fieldToDeleteIndex = index },
                        onFullscreenRequest = { fullscreenFieldIndex = index },
                        dragModifier = dragModifier,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .animateItem(),
                        extraContent = extraContent,
                        isCollapsible = isCollapsible,
                        isTitleReadOnly = isTitleReadOnly,
                        isReorderButtonVisible = isReorderButtonVisible,
                        isContentVisible = isContentVisible(field),
                        isLockedGlobal = fullscreenEditingOnly,
                        collapseOnEdit = collapseOnEditActual,
                        hazeState = hazeState,
                        popupHazeState = popupHazeState,
                        forceBlurEnabled = forceBlurEnabled,
                        blurPopups = blurPopups,
                        settingsViewModel = settingsViewModel,
                        statsMap = statsMap,
                        onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
                        state = state,
                        viewportTopY = mainViewportTopY
                    )
                }
            }

            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    footer()
                }
            }

            if (isAddButtonVisible) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val newFields = items.toList() + DynamicNoteState()
                            onFieldsChange(newFields)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(addButtonText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    DeleteConfirmationDialog(
        showDialog = fieldToDeleteIndex != null,
        onDismiss = { fieldToDeleteIndex = null },
        onConfirm = {
            fieldToDeleteIndex?.let { index ->
                if (index in items.indices) {
                    items.removeAt(index)
                    onFieldsChange(items.toList())
                }
            }
            fieldToDeleteIndex = null
        },
        settingsViewModel = settingsViewModel
    )

    if (fullscreenFieldIndex != null && state == null) {
        if (fullscreenFieldIndex!! in items.indices) {
            val field = items[fullscreenFieldIndex!!]
            DynamicFieldFullscreenDialog(
                field = field,
                titlePlaceholder = titlePlaceholder,
                contentPlaceholder = contentPlaceholder,
                onFieldChange = { updatedField ->
                    val currentList = items.toList()
                    val idx = currentList.indexOfFirst { it.id == updatedField.id }
                    if (idx != -1) {
                        items[idx] = updatedField
                        onFieldsChange(items.toList())
                    }
                },
                onDelete = {
                    val currentList = items.toList()
                    val idx = currentList.indexOfFirst { it.id == field.id }
                    if (idx != -1) {
                        items.removeAt(idx)
                        onFieldsChange(items.toList())
                    }
                    fullscreenFieldIndex = null
                },
                onDismiss = { 
                    fullscreenFieldIndex = null
                },
                hazeState = hazeState,
                forceBlurEnabled = forceBlurEnabled,
                blurPopups = blurPopups,
                settingsViewModel = settingsViewModel,
                statsMap = statsMap,
                onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
                isDesktop = isDesktop,
                popupHazeState = popupHazeState,
                extraContent = extraContent,
                isContentVisible = isContentVisible(field),
                state = state
            )
        }
    }
}

@Composable
fun DynamicFieldItem(
    field: DynamicNoteState,
    isEditMode: Boolean = false,
    isDragging: Boolean = false,
    isAnyItemDragging: Boolean = false,
    titlePlaceholder: String = "Заголовок",
    contentPlaceholder: String = "Текст...",
    onFieldChange: (DynamicNoteState) -> Unit,
    onDelete: () -> Unit = {},
    onFullscreenRequest: () -> Unit,
    dragModifier: Modifier = Modifier,
    modifier: Modifier = Modifier,
    isCollapsible: Boolean = true,
    isTitleReadOnly: Boolean = false,
    isReorderButtonVisible: Boolean = true,
    isContentVisible: Boolean = true,
    isLockedGlobal: Boolean = false,
    collapseOnEdit: Boolean = true,
    hazeState: HazeState? = null,
    popupHazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    blurPopups: Boolean = false,
    settingsViewModel: SettingsViewModel? = null,
    statsMap: Map<String, String> = emptyMap(),
    onFullscreenDialogOpenChange: (Boolean) -> Unit = {},
    state: ru.quasaris.characternexus.ui.CharacterDetailState? = null,
    viewportTopY: Float = 0f,
    extraContent: @Composable (DynamicNoteState) -> Unit = {}
) {
    val blurDynamicFields by settingsViewModel?.blurDynamicFields?.collectAsState() ?: remember { mutableStateOf(true) }
    val isExpanded = if (isCollapsible) field.isExpanded else true
    val rotation by animateFloatAsState(targetValue = if (isExpanded) 0f else 180f)
    val scale by animateFloatAsState(targetValue = when {
        isDragging -> 1.02f
        isEditMode -> 0.95f
        else -> 1f
    })
    val backgroundBlur by animateDpAsState(
        targetValue = if (isAnyItemDragging && !isDragging) 6.dp else 0.dp,
        label = "backgroundBlur"
    )
    val padding by animateDpAsState(targetValue = if (isEditMode) 8.dp else 0.dp)

    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val canEdit = !isEditMode && !isLockedGlobal && !field.isLocked
    val titleBringIntoViewRequester = remember { BringIntoViewRequester() }
    var isTitleFocused by remember { mutableStateOf(false) }

    var isReorderMode by remember(field.id) { mutableStateOf(false) }
    LaunchedEffect(isExpanded, isEditMode) { if (!isExpanded || isEditMode) isReorderMode = false }

    val imeBottomPx = WindowInsets.ime.getBottom(density)

    val scrollMarginPx = with(density) { 40.dp.toPx() }

    LaunchedEffect(imeBottomPx, isTitleFocused) {
        if (isTitleFocused) {
            titleBringIntoViewRequester.bringIntoView(
                androidx.compose.ui.geometry.Rect(0f, 0f, 0f, scrollMarginPx * 2f)
            )
        }
    }

    var contentHeightPx by remember { mutableIntStateOf(0) }
    val contentHeightDp = with(density) { contentHeightPx.toDp() }
    val isTallField = isExpanded && contentHeightDp >= 120.dp

    val useHaze = hazeState != null && (blurDynamicFields ?: true)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .then(
                if (backgroundBlur > 0.dp) 
                    Modifier.blur(backgroundBlur) 
                else Modifier
            )
            .padding(padding)
            .outerShadow(
                shape = RoundedCornerShape(16.dp),
                blur = 4.dp,
                offsetY = 2.dp
            )
            .run {
                if (useHaze) {
                    this.clip(RoundedCornerShape(16.dp))
                        .hazeEffect(
                            state = hazeState!!,
                            style = HazeStyle(
                                blurRadius = 24.dp,
                                tints = listOf(HazeTint(colorScheme.surfaceContainer.copy(alpha = 0.6f)))
                            )
                        )
                } else this
            },
        shape = RoundedCornerShape(16.dp),
        color = if (useHaze) colorScheme.surfaceContainer.copy(alpha = 0.6f)
            else colorScheme.surfaceContainer,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEditMode && isReorderButtonVisible) {
                Icon(
                    imageVector = Icons.Default.UnfoldMore,
                    contentDescription = "Drag",
                    modifier = Modifier
                        .padding(start = 12.dp, end = 4.dp)
                        .size(32.dp)
                        .then(dragModifier),
                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = if (isEditMode) 12.dp else 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .run {
                            if (isEditMode || !isCollapsible) this else this.clickable { onFieldChange(field.copy(isExpanded = !isExpanded)) }
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = field.title,
                        onValueChange = { onFieldChange(field.copy(title = it)) },
                        enabled = canEdit && !isTitleReadOnly,
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .bringIntoViewRequester(titleBringIntoViewRequester)
                            .onFocusChanged { isTitleFocused = it.isFocused },
                        decorationBox = { innerTextField ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (field.title.isEmpty()) {
                                    Text(
                                        titlePlaceholder,
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

                    if (!isEditMode || !collapseOnEdit) {
                        if (field.isLocked || isLockedGlobal) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .size(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Заблокировано",
                                    modifier = Modifier.size(18.dp),
                                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }

                        if (canEdit && isExpanded) {
                            IconToggleButton(
                                checked = isReorderMode,
                                onCheckedChange = { isReorderMode = it },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = "Режим сортировки",
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isReorderMode) colorScheme.primary else colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }

                        IconButton(
                            onClick = { onFullscreenRequest() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInFull,
                                contentDescription = "Fullscreen",
                                modifier = Modifier.size(20.dp),
                                tint = colorScheme.primary.copy(alpha = 0.6f)
                            )
                        }

                        if (isCollapsible) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { onFieldChange(field.copy(isExpanded = !isExpanded)) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .rotate(rotation),
                                    tint = colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            extraContent(field)
                        }
                    }
                }

                AnimatedVisibility(
                    visible = isExpanded && (!isEditMode || !collapseOnEdit),
                    enter = fadeIn() + expandVertically(clip = true),
                    exit = fadeOut() + shrinkVertically(clip = true),
                    modifier = Modifier.graphicsLayer { clip = true }
                ) {
                    Column {
                        if (isCollapsible) {
                            extraContent(field)
                        }
                        if (isContentVisible) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .onGloballyPositioned { coords ->
                                        contentHeightPx = coords.size.height
                                    }
                                    .outerShadow(shape = RoundedCornerShape(12.dp), blur = 2.dp, offsetY = 1.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = if (useHaze) colorScheme.surfaceContainerHigh.copy(alpha = 0.5f) else colorScheme.surfaceContainerHigh,
                                shadowElevation = 0.dp,
                                tonalElevation = 0.dp
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                    NotionBlockEditor(
                                        field = field,
                                        onFieldChange = onFieldChange,
                                        canEdit = canEdit,
                                        isReorderMode = isReorderMode,
                                        contentPlaceholder = contentPlaceholder,
                                        statsMap = statsMap,
                                        hazeState = hazeState,
                                        forceBlurEnabled = forceBlurEnabled,
                                        blurDynamicFields = blurDynamicFields ?: true,
                                        settingsViewModel = settingsViewModel,
                                        onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
                                        state = state,
                                        viewportTopY = viewportTopY,
                                        popupHazeState = popupHazeState
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.padding(start = 8.dp)) {
                                    if (canEdit && !isReorderMode) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        val b = BlockContentParser.toBlocks(field.content).toMutableList()
                                                        b.add(DynamicContentBlock.Text(""))
                                                        onFieldChange(field.copy(content = BlockContentParser.toText(b)))
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PostAdd,
                                                        contentDescription = "Добавить строку",
                                                        modifier = Modifier.size(18.dp),
                                                        tint = colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val b = BlockContentParser.toBlocks(field.content).toMutableList()
                                                        b.add(DynamicContentBlock.Divider)
                                                        onFieldChange(field.copy(content = BlockContentParser.toText(b)))
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.HorizontalRule,
                                                        contentDescription = "Добавить разделитель",
                                                        modifier = Modifier.size(18.dp),
                                                        tint = colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val b = BlockContentParser.toBlocks(field.content).toMutableList()
                                                        val res = state?.resourceManager?.create("Новый ресурс")
                                                            ?: DynamicContentBlock.Resource(name = "Новый ресурс", current = "0", max = "0", id = ru.quasaris.characternexus.util.generateUuid())
                                                        b.add(res)
                                                        onFieldChange(field.copy(content = BlockContentParser.toText(b)))
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.AddBox,
                                                        contentDescription = "Добавить ресурс",
                                                        modifier = Modifier.size(18.dp),
                                                        tint = colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (isTallField && (!isEditMode || !collapseOnEdit)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (field.isLocked || isLockedGlobal) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(horizontal = 2.dp)
                                                    .size(36.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = "Заблокировано",
                                                    modifier = Modifier.size(18.dp),
                                                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                )
                                            }
                                        }

                                        if (canEdit) {
                                            IconToggleButton(
                                                checked = isReorderMode,
                                                onCheckedChange = { isReorderMode = it },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.SwapVert,
                                                    contentDescription = "Режим сортировки",
                                                    modifier = Modifier.size(20.dp),
                                                    tint = if (isReorderMode) colorScheme.primary else colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { onFullscreenRequest() },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.OpenInFull,
                                                contentDescription = "Fullscreen",
                                                modifier = Modifier.size(20.dp),
                                                tint = colorScheme.primary.copy(alpha = 0.6f)
                                            )
                                        }

                                        if (isCollapsible) {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) { onFieldChange(field.copy(isExpanded = false)) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowUp,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(28.dp),
                                                    tint = colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isEditMode) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Delete",
                        tint = colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun DynamicFieldFullscreenDialog(
    field: DynamicNoteState,
    titlePlaceholder: String = "Заголовок",
    contentPlaceholder: String = "Текст...",
    onFieldChange: (DynamicNoteState) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    blurDynamicFields: Boolean = true,
    blurPopups: Boolean = false,
    settingsViewModel: SettingsViewModel? = null,
    statsMap: Map<String, String> = emptyMap(),
    onFullscreenDialogOpenChange: (Boolean) -> Unit = {},
    isDesktop: Boolean = false,
    state: ru.quasaris.characternexus.ui.CharacterDetailState? = null,
    popupHazeState: HazeState? = null,
    extraContent: @Composable (DynamicNoteState) -> Unit = {},
    isContentVisible: Boolean = true
) {
    var title by remember(field.id) { mutableStateOf(field.title) }
    var isLocked by remember(field.id) { mutableStateOf(field.isLocked) }

    LaunchedEffect(title, isLocked) {
        onFieldChange(field.copy(title = title, isLocked = isLocked))
    }

    val content: @Composable () -> Unit = {
        DynamicFieldFullscreenContent(
            field = field,
            title = title,
            onTitleChange = { title = it },
            isLocked = isLocked,
            onIsLockedChange = { isLocked = it },
            titlePlaceholder = titlePlaceholder,
            contentPlaceholder = contentPlaceholder,
            onFieldChange = onFieldChange,
            onDelete = onDelete,
            onDismiss = onDismiss,
            hazeState = hazeState,
            popupHazeState = popupHazeState,
            forceBlurEnabled = forceBlurEnabled,
            blurDynamicFields = blurDynamicFields,
            blurPopups = blurPopups,
            settingsViewModel = settingsViewModel,
            statsMap = statsMap,
            onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
            isDesktop = isDesktop,
            state = state,
            extraContent = extraContent,
            isContentVisible = isContentVisible
        )
    }

    if (isDesktop) {
        content()
    } else {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            DialogDimStyle(0f)
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicFieldFullscreenContent(
    field: DynamicNoteState,
    title: String,
    onTitleChange: (String) -> Unit,
    isLocked: Boolean,
    onIsLockedChange: (Boolean) -> Unit,
    titlePlaceholder: String,
    contentPlaceholder: String,
    onFieldChange: (DynamicNoteState) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    hazeState: HazeState?,
    popupHazeState: HazeState? = null,
    forceBlurEnabled: Boolean,
    blurDynamicFields: Boolean,
    blurPopups: Boolean,
    settingsViewModel: SettingsViewModel?,
    statsMap: Map<String, String>,
    onFullscreenDialogOpenChange: (Boolean) -> Unit,
    isDesktop: Boolean = false,
    state: ru.quasaris.characternexus.ui.CharacterDetailState? = null,
    extraContent: @Composable (DynamicNoteState) -> Unit = {},
    isContentVisible: Boolean = true
) {
    val isOled = MaterialTheme.colorScheme.background == Color.Black
    val effectiveBlur = forceBlurEnabled && !isOled

    val currentOnFullscreenDialogOpenChange by rememberUpdatedState(onFullscreenDialogOpenChange)

    val focusManager = LocalFocusManager.current
    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val titleBringIntoViewRequester = remember { BringIntoViewRequester() }
    val localHazeState = remember { HazeState() }
    var isTitleFocused by remember { mutableStateOf(false) }
    
    var localViewportTopY by remember { mutableFloatStateOf(0f) }

    var isReorderMode by remember { mutableStateOf(false) }

    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val scrollMarginPx = with(density) { 40.dp.toPx() }

    LaunchedEffect(imeBottomPx, isTitleFocused) {
        if (isTitleFocused) {
            titleBringIntoViewRequester.bringIntoView(
                androidx.compose.ui.geometry.Rect(0f, 0f, 0f, scrollMarginPx * 2f)
            )
        }
    }

    BackHandler(onBack = onDismiss)

    DisposableEffect(Unit) {
        currentOnFullscreenDialogOpenChange(true)
        onDispose {
            currentOnFullscreenDialogOpenChange(false)
        }
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    val isSubDialogOpen = showDeleteConfirm || state?.isResourceConfigOpen == true
    val masterBlurEnabled by settingsViewModel?.masterBlurEnabled?.collectAsState() ?: remember { mutableStateOf(true) }
    val blurRadius = rememberEffectiveBlurRadius(settingsViewModel)

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .run {
                    if (isSubDialogOpen && masterBlurEnabled) {
                        this.blur(blurRadius)
                    } else this
                }
        ) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            BasicTextField(
                                value = title,
                                onValueChange = onTitleChange,
                                textStyle = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .bringIntoViewRequester(titleBringIntoViewRequester)
                                    .onFocusChanged { isTitleFocused = it.isFocused },
                                decorationBox = { innerTextField ->
                                    if (title.isEmpty()) {
                                        Text(titlePlaceholder, color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                                    }
                                    innerTextField()
                                }
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                focusManager.clearFocus()
                                onDismiss()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Закрыть")
                            }
                        },
                        actions = {
                            if (!isLocked && isContentVisible) {
                                IconToggleButton(checked = isReorderMode, onCheckedChange = { isReorderMode = it }) {
                                    Icon(
                                        imageVector = Icons.Default.SwapVert,
                                        contentDescription = "Режим сортировки",
                                        tint = if (isReorderMode) colorScheme.primary else colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = if (effectiveBlur && hazeState != null && !isSubDialogOpen) Color.Transparent.copy(alpha = 0.0f) else colorScheme.surface
                        )
                    )
                },
                containerColor = if (effectiveBlur && hazeState != null && !isSubDialogOpen) Color.Transparent.copy(alpha = 0.0f) else colorScheme.background,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val keyboardOffset = imeBottomPx
                        alpha = if (keyboardOffset >= 0) 1f else 1f
                    }
                    .run {
                        if (effectiveBlur && hazeState != null && !isSubDialogOpen) {
                            this.hazeEffect(state = hazeState) {
                                style = HazeStyle(
                                    blurRadius = blurRadius,
                                    tints = listOf(HazeTint(Color.Black.copy(alpha = 0.2f)))
                                )
                            }
                        } else this
                    }
                    .hazeSource(state = localHazeState)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, _ ->
                                change.consume()
                                focusManager.clearFocus()
                            }
                        )
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { focusManager.clearFocus() }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .onGloballyPositioned { localViewportTopY = it.positionInWindow().y }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        extraContent(field)

                        if (isContentVisible) {
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = if (effectiveBlur) colorScheme.surfaceContainerHighest.copy(alpha = 0.6f) else colorScheme.surfaceContainerHighest,
                                shadowElevation = 0.dp,
                                tonalElevation = 0.dp
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                    NotionBlockEditor(
                                        field = field.copy(title = title, isLocked = isLocked),
                                        onFieldChange = onFieldChange,
                                        canEdit = !isLocked,
                                        isReorderMode = isReorderMode,
                                        contentPlaceholder = contentPlaceholder,
                                        statsMap = statsMap,
                                        hazeState = hazeState,
                                        forceBlurEnabled = effectiveBlur,
                                        blurDynamicFields = blurDynamicFields,
                                        settingsViewModel = settingsViewModel,
                                        onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
                                        state = state,
                                        viewportTopY = localViewportTopY,
                                        popupHazeState = localHazeState
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!isLocked) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    val b = BlockContentParser.toBlocks(field.content).toMutableList()
                                                    b.add(DynamicContentBlock.Text(""))
                                                    onFieldChange(field.copy(title = title, isLocked = isLocked, content = BlockContentParser.toText(b)))
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PostAdd,
                                                    contentDescription = "Добавить строку",
                                                    modifier = Modifier.size(18.dp),
                                                    tint = colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    val b = BlockContentParser.toBlocks(field.content).toMutableList()
                                                    b.add(DynamicContentBlock.Divider)
                                                    onFieldChange(field.copy(title = title, isLocked = isLocked, content = BlockContentParser.toText(b)))
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.HorizontalRule,
                                                    contentDescription = "Добавить разделитель",
                                                    modifier = Modifier.size(18.dp),
                                                    tint = colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    val b = BlockContentParser.toBlocks(field.content).toMutableList()
                                                    val res = state?.resourceManager?.create("Новый ресурс")
                                                        ?: DynamicContentBlock.Resource(name = "Новый ресурс", current = "0", max = "0", id = ru.quasaris.characternexus.util.generateUuid())
                                                    b.add(res)
                                                    onFieldChange(field.copy(title = title, isLocked = isLocked, content = BlockContentParser.toText(b)))
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AddBox,
                                                    contentDescription = "Добавить ресурс",
                                                    modifier = Modifier.size(18.dp),
                                                    tint = colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(Modifier.width(1.dp))
                                }

                                if (!isLocked) {
                                    IconToggleButton(
                                        checked = isReorderMode,
                                        onCheckedChange = { isReorderMode = it },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SwapVert,
                                            contentDescription = "Режим сортировки",
                                            modifier = Modifier.size(20.dp),
                                            tint = if (isReorderMode) colorScheme.primary else colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        OutlinedButton(
                            onClick = { onIsLockedChange(!isLocked) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isLocked) colorScheme.primary else colorScheme.onSurfaceVariant
                            ),
                            border = BorderStroke(1.dp, if (isLocked) colorScheme.primary else colorScheme.outline),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isLocked) "Заблокировано" else "Разблокировано",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = colorScheme.error
                            ),
                            border = BorderStroke(1.dp, colorScheme.error),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Удалить", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(80.dp))
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.surfaceVariant.compositeOver(colorScheme.background),
                            contentColor = colorScheme.onSurfaceVariant
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            focusedElevation = 0.dp,
                            hoveredElevation = 0.dp
                        )
                    ) {
                        Text("Закрыть", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            if (state?.isResourceConfigOpen == true && state.activeResourceConfig != null) {
                ResourceConfigDialog(
                    resource = state.activeResourceConfig!!,
                    onDismiss = {
                        state.isResourceConfigOpen = false
                        state.activeResourceConfig = null
                        state.activeNoteId = ""
                    },
                    onSave = { updated: DynamicContentBlock.Resource ->
                        state.updateResource(updated)
                        state.resourceManager.normalize()
                        state.isResourceConfigOpen = false
                        state.activeResourceConfig = null
                        state.activeResourceIndex = -1
                        state.activeNoteId = ""
                    },
                    onDelete = { res: DynamicContentBlock.Resource ->
                        if (state.activeNoteId.isNotEmpty() && state.activeResourceIndex != -1) {
                            state.resourceManager.removePlacement(state.activeNoteId, state.activeResourceIndex, res.id)
                        } else {
                            state.resourceManager.deleteResourceCompletely(res.id)
                        }
                        state.isResourceConfigOpen = false
                        state.activeResourceConfig = null
                        state.activeResourceIndex = -1
                        state.activeNoteId = ""
                    },
                    forceBlurEnabled = effectiveBlur,
                    settingsViewModel = settingsViewModel,
                    hazeState = localHazeState,
                    isNested = true,
                    asOverlay = true,
                    isDesktop = isDesktop,
                    owner = state,
                    noteId = state.activeNoteId,
                    blockIndex = state.activeResourceIndex
                )
            }
        }

        DeleteConfirmationDialog(
            showDialog = showDeleteConfirm,
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                onDelete()
                showDeleteConfirm = false
            },
            settingsViewModel = settingsViewModel
        )
    }
}
