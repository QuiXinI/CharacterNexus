package ru.quasaris.characternexus.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalDensity
import ru.quasaris.characternexus.ui.outerShadow
import ru.quasaris.characternexus.ui.DialogDimStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.backend.SettingsViewModel
import ru.quasaris.characternexus.ui.DeleteConfirmationDialog
import ru.quasaris.characternexus.ui.BackHandler
import ru.quasaris.characternexus.ui.TabControlHeader
import ru.quasaris.characternexus.ui.theme.rememberEffectiveBlurRadius
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import sh.calvin.reorderable.*
import kotlinx.coroutines.launch

@Composable
fun HyperlinkDialog(
    initialText: String,
    initialUrl: String,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var url by remember { mutableStateOf(initialUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Вставить гиперссылку") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Текст") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Ссылка (URL)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text, url) }) {
                Text("ОК")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

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
    isContentVisible: Boolean = true,
    collapseOnEdit: Boolean? = null,
    onFullscreenDialogOpenChange: (Boolean) -> Unit = {},
    onFullscreenVisibilityChanged: (Boolean) -> Unit = {},
    state: ru.quasaris.characternexus.ui.CharacterDetailState? = null,
    header: @Composable () -> Unit = {},
    footer: @Composable () -> Unit = {},
    extraContent: @Composable (DynamicNoteState) -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
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
            onFieldsChange(items.toList())
        }
    }

    var fullscreenFieldIndex by remember { mutableStateOf<Int?>(null) }
    
    LaunchedEffect(fullscreenFieldIndex) {
        onFullscreenVisibilityChanged(fullscreenFieldIndex != null)
        state?.activeDynamicField = fullscreenFieldIndex?.let { items.getOrNull(it) }
        state?.isFullscreenDynamicFieldOpen = fullscreenFieldIndex != null
    }

    LaunchedEffect(state?.isFullscreenDynamicFieldOpen) {
        if (state?.isFullscreenDynamicFieldOpen == false) {
            fullscreenFieldIndex = null
        }
    }

    var fieldToDeleteIndex by remember { mutableStateOf<Int?>(null) }

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
                        isContentVisible = isContentVisible,
                        isLockedGlobal = fullscreenEditingOnly,
                        collapseOnEdit = collapseOnEditActual,
                        hazeState = hazeState,
                        popupHazeState = popupHazeState,
                        forceBlurEnabled = forceBlurEnabled,
                        blurPopups = blurPopups,
                        settingsViewModel = settingsViewModel,
                        statsMap = statsMap,
                        onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
                        state = state
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
                onFullscreenDialogOpenChange = onFullscreenDialogOpenChange
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
    extraContent: @Composable (DynamicNoteState) -> Unit = {}
) {
    val internalHazeState = remember { HazeState() }
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

    val focusManager = LocalFocusManager.current
    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val focusRequester = remember { FocusRequester() }
    val canEdit = !isEditMode && !isLockedGlobal && !field.isLocked

    var contentValue by remember { mutableStateOf(TextFieldValue(field.content)) }

    val contentBringIntoViewRequester = remember { BringIntoViewRequester() }
    val titleBringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var isFocused by remember { mutableStateOf(false) }
    var isTitleFocused by remember { mutableStateOf(false) }

    // Реальная (анимируемая системой) высота клавиатуры. Читаем её прямо в composition,
    // чтобы этот composable перекомпоновывался на каждом кадре анимации IME и
    // пересчитывал bringIntoView по актуальному viewport'у, а не по угаданной задержке.
    val imeBottomPx = WindowInsets.ime.getBottom(density)

    val scrollMarginPx = with(density) { 40.dp.toPx() }

    var toolbarState by remember { mutableStateOf(Triple(contentValue, false, false)) }

    LaunchedEffect(contentValue, isFocused) {
        if (!isFocused) {
            toolbarState = Triple(contentValue, false, false)
            return@LaunchedEffect
        }
        toolbarState = Triple(contentValue, true, contentValue.selection.length > 0)
    }

    // Единый источник правды для автоскролла к курсору: перезапускается и при смене
    // текста/выделения/фокуса, И при каждом изменении высоты клавиатуры (imeBottomPx),
    // поэтому корректно доводит скролл до конца уже после того, как IME анимация
    // реально завершилась, а не через фиксированные 150мс.
    LaunchedEffect(imeBottomPx, contentValue.selection, contentValue.text, isFocused) {
        if (!isFocused) return@LaunchedEffect
        val layoutResult = textLayoutResult
        if (layoutResult != null && contentValue.selection.collapsed) {
            val cursorRect = layoutResult.getCursorRect(contentValue.selection.start)
            contentBringIntoViewRequester.bringIntoView(
                cursorRect.copy(
                    top = cursorRect.top - scrollMarginPx,
                    bottom = cursorRect.bottom + scrollMarginPx
                )
            )
        } else {
            contentBringIntoViewRequester.bringIntoView()
        }
    }

    LaunchedEffect(imeBottomPx, isTitleFocused) {
        if (isTitleFocused) {
            titleBringIntoViewRequester.bringIntoView(
                androidx.compose.ui.geometry.Rect(0f, 0f, 0f, scrollMarginPx * 2f)
            )
        }
    }

    LaunchedEffect(field.content) {
        if (field.content != contentValue.text) {
            contentValue = contentValue.copy(text = field.content)
        }
    }
    var showLinkDialog by remember { mutableStateOf(false) }

    if (showLinkDialog) {
        val selection = contentValue.selection
        val selectedText = contentValue.text.substring(selection.min, selection.max)
        HyperlinkDialog(
            initialText = selectedText,
            initialUrl = "",
            onConfirm = { text, url ->
                val prefix = "["
                val middle = "]("
                val suffix = ")"
                val newText = contentValue.text.substring(0, selection.min) + prefix + text + middle + url + suffix + contentValue.text.substring(selection.max)
                val newSelection = TextRange(selection.min + prefix.length + text.length + middle.length + url.length + suffix.length)
                val new = contentValue.copy(text = newText, selection = newSelection)
                contentValue = new
                onFieldChange(field.copy(content = new.text))
                showLinkDialog = false
            },
            onDismiss = { showLinkDialog = false }
        )
    }

    val useHaze = hazeState != null && blurDynamicFields

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .then(
                if (backgroundBlur > 0.dp) 
                    Modifier.blur(backgroundBlur, edgeTreatment = BlurredEdgeTreatment.Unbounded) 
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
                    val targetState = if (isDragging) (popupHazeState ?: hazeState) else hazeState
                    this.clip(RoundedCornerShape(16.dp))
                        .hazeEffect(
                            state = targetState,
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
                                if (field.isLocked || isLockedGlobal) {
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = colorScheme.primary.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    )

                    if (!isEditMode || !collapseOnEdit) {
                        if (isCollapsible) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
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

                AnimatedVisibility(visible = isExpanded && (!isEditMode || !collapseOnEdit)) {
                    Column {
                        if (isCollapsible) {
                            extraContent(field)
                        }
                        if (isContentVisible) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .outerShadow(
                                        shape = RoundedCornerShape(12.dp),
                                        blur = 2.dp,
                                        offsetY = 1.dp
                                    ),
                                shape = RoundedCornerShape(12.dp),
                                color = if (useHaze) colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                                else colorScheme.surfaceContainerHigh,
                                shadowElevation = 0.dp,
                                tonalElevation = 0.dp
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    FormattingToolbar(
                                        value = toolbarState.first,
                                        onValueChange = {
                                            contentValue = it
                                            onFieldChange(field.copy(content = it.text))
                                        },
                                        isFocused = toolbarState.second,
                                        isSelectionActive = toolbarState.third,
                                        onLinkRequest = { showLinkDialog = true },
                                        onSave = { focusManager.clearFocus() }
                                    )

                                    key(field.id) {
                                        BasicTextField(
                                            value = contentValue,
                                            onValueChange = {
                                                val oldText = contentValue.text
                                                contentValue = it
                                                if (oldText != it.text) {
                                                    onFieldChange(field.copy(content = it.text))
                                                }
                                            },
                                            enabled = canEdit,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .hazeSource(state = internalHazeState)
                                                .focusRequester(focusRequester)
                                                .onFocusChanged { isFocused = it.isFocused },
                                            onTextLayout = { textLayoutResult = it },
                                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                                fontSize = 17.sp,
                                                lineHeight = 24.sp,
                                                color = if (isFocused) colorScheme.onSurface else Color.Transparent
                                            ),
                                            cursorBrush = SolidColor(colorScheme.primary),
                                            decorationBox = { innerTextField ->
                                                Box(modifier = Modifier.fillMaxWidth()) {
                                                    val onSurface = colorScheme.onSurface
                                                    val uriHandler = LocalUriHandler.current
                                                    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

                                                    val blocks = remember(field.content) {
                                                        DynamicContentParser.parse(field.content)
                                                    }

                                                    val marginStep by settingsViewModel?.topMarginStep?.collectAsState() ?: remember { mutableStateOf(2) }
                                                    val customMargin by settingsViewModel?.customTopMargin?.collectAsState() ?: remember { mutableStateOf(96) }
                                                    val topMargin = if (marginStep == 5) customMargin.dp else (marginStep * 48).dp

                                                    if (isFocused) {
                                                        Column {
                                                            if (toolbarState.second) {
                                                                Spacer(modifier = Modifier.height(topMargin))
                                                            }
                                                            Box(Modifier.bringIntoViewRequester(contentBringIntoViewRequester)) {
                                                                innerTextField()
                                                            }
                                                            Spacer(modifier = Modifier.height(8.dp))
                                                        }
                                                    } else {
                                                        Box(
                                                            Modifier
                                                                .alpha(0f)
                                                                .layout { measurable, constraints ->
                                                                    val placeable = measurable.measure(constraints)
                                                                    layout(placeable.width, 0) {
                                                                        placeable.place(0, 0)
                                                                    }
                                                                }
                                                        ) {
                                                            innerTextField()
                                                        }

                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                        ) {
                                                            val displayBlocks = remember(blocks) { DynamicContentParser.getDisplayBlocks(blocks) }
                                                            displayBlocks.forEachIndexed { index, (absoluteIndex, block) ->
                                                                when (block) {
                                                                    is DynamicContentBlock.Text -> {
                                                                        val annotated = remember(block.content, onSurface) {
                                                                            MarkdownHelper.parseMarkdown(block.content, onSurface, isEditing = false)
                                                                        }
                                                                        Text(
                                                                            text = annotated,
                                                                            fontSize = 17.sp,
                                                                            lineHeight = 24.sp,
                                                                            color = onSurface,
                                                                            modifier = Modifier
                                                                                .fillMaxWidth()
                                                                                .pointerInput(annotated) {
                                                                                    detectTapGestures(
                                                                                        onTap = { offset ->
                                                                                            layoutResult?.let { textLayoutResult ->
                                                                                                val position = textLayoutResult.getOffsetForPosition(offset)
                                                                                                val line = textLayoutResult.getLineForOffset(position)
                                                                                                val isWithinBounds = offset.x <= textLayoutResult.getLineRight(line)

                                                                                                if (isWithinBounds) {
                                                                                                    val annotation = annotated.getLinkAnnotations(position, position)
                                                                                                        .firstOrNull()
                                                                                                    if (annotation != null && annotation.item is LinkAnnotation.Url) {
                                                                                                        uriHandler.openUri((annotation.item as LinkAnnotation.Url).url)
                                                                                                    } else {
                                                                                                        if (canEdit) focusRequester.requestFocus()
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    )
                                                                                },
                                                                            onTextLayout = { layoutResult = it }
                                                                        )
                                                                    }
                                                                    is DynamicContentBlock.Divider -> {
                                                                        val prevIsDivider = index > 0 && displayBlocks[index - 1].second is DynamicContentBlock.Divider
                                                                        HorizontalDivider(
                                                                            modifier = Modifier.padding(
                                                                                top = if (prevIsDivider) 0.dp else 4.dp,
                                                                                bottom = 4.dp
                                                                            ),
                                                                            thickness = 1.dp,
                                                                            color = colorScheme.outlineVariant
                                                                        )
                                                                    }
                                                                    is DynamicContentBlock.Spoiler -> {
                                                                        val annotated = remember(block.content, onSurface) {
                                                                            MarkdownHelper.parseMarkdown(block.content, onSurface, isEditing = false)
                                                                        }
                                                                        SpoilerComponent(content = annotated)
                                                                    }
                                                                    is DynamicContentBlock.Quote -> {
                                                                        val annotated = remember(block.content, onSurface) {
                                                                            MarkdownHelper.parseMarkdown(block.content, onSurface, isEditing = false)
                                                                        }
                                                                        QuoteComponent(content = annotated)
                                                                    }
                                                                    is DynamicContentBlock.Resource -> {
                                                                        ResourceBlock(
                                                                            resource = block,
                                                                            statsMap = statsMap,
                                                                            onUpdate = { updatedResource ->
                                                                                val newBlocks = blocks.toMutableList()
                                                                                if (absoluteIndex != -1 && absoluteIndex < newBlocks.size) {
                                                                                    newBlocks[absoluteIndex] = updatedResource
                                                                                    val newContent = DynamicContentParser.render(newBlocks)
                                                                                    onFieldChange(field.copy(content = newContent))
                                                                                }
                                                                            },
                                                                            hazeState = hazeState,
                                                                            forceBlurEnabled = forceBlurEnabled,
                                                                            blurDynamicFields = blurDynamicFields,
                                                                            blurPopups = blurPopups,
                                                                            settingsViewModel = settingsViewModel,
                                                                            onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
                                                                            onSubDialogOpenChange = { /* Item doesn't blur on sub-dialog */ },
                                                                            state = state,
                                                                            onOpenConfig = state?.let { s ->
                                                                                { res ->
                                                                                    s.activeResourceConfig = res
                                                                                    s.activeResourceIndex = absoluteIndex
                                                                                    s.isResourceConfigOpen = true
                                                                                }
                                                                            },
                                                                            onDeleteRequest = {
                                                                                val newBlocks = blocks.toMutableList()
                                                                                if (absoluteIndex != -1 && absoluteIndex < newBlocks.size) {
                                                                                    newBlocks.removeAt(absoluteIndex)
                                                                                    val newContent = DynamicContentParser.render(newBlocks)
                                                                                    onFieldChange(field.copy(content = newContent))
                                                                                }
                                                                            }
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                            Spacer(modifier = Modifier.height(8.dp))
                                                        }

                                                        if (field.content.isEmpty()) {
                                                            Text(
                                                                contentPlaceholder,
                                                                fontSize = 17.sp,
                                                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.align(Alignment.BottomEnd),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (canEdit) {
                                            IconButton(
                                                onClick = {
                                                    if (isFocused) focusManager.clearFocus()
                                                    else focusRequester.requestFocus()
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isFocused) Icons.Default.Check else Icons.Default.Edit,
                                                    contentDescription = "Edit/Save",
                                                    modifier = Modifier.size(20.dp),
                                                    tint = colorScheme.primary.copy(alpha = 0.6f)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        IconButton(
                                            onClick = { onFullscreenRequest() },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.OpenInFull,
                                                contentDescription = "Fullscreen",
                                                modifier = Modifier.size(20.dp),
                                                tint = colorScheme.primary.copy(alpha = 0.6f)
                                            )
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
    state: ru.quasaris.characternexus.ui.CharacterDetailState? = null
) {
    var title by remember { mutableStateOf(field.title) }
    var contentValue by remember { mutableStateOf(TextFieldValue(field.content)) }
    var isLocked by remember { mutableStateOf(field.isLocked) }

    if (isDesktop) {
        DynamicFieldFullscreenContent(
            field = field,
            title = title,
            onTitleChange = { title = it },
            contentValue = contentValue,
            onContentValueChange = { contentValue = it },
            isLocked = isLocked,
            onIsLockedChange = { isLocked = it },
            titlePlaceholder = titlePlaceholder,
            contentPlaceholder = contentPlaceholder,
            onFieldChange = onFieldChange,
            onDelete = onDelete,
            onDismiss = onDismiss,
            hazeState = hazeState,
            forceBlurEnabled = forceBlurEnabled,
            blurDynamicFields = blurDynamicFields,
            blurPopups = blurPopups,
            settingsViewModel = settingsViewModel,
            statsMap = statsMap,
            onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
            isDesktop = true,
            state = state
        )
    } else {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            DialogDimStyle(0f)
            DynamicFieldFullscreenContent(
                field = field,
                title = title,
                onTitleChange = { title = it },
                contentValue = contentValue,
                onContentValueChange = { contentValue = it },
                isLocked = isLocked,
                onIsLockedChange = { isLocked = it },
                titlePlaceholder = titlePlaceholder,
                contentPlaceholder = contentPlaceholder,
                onFieldChange = onFieldChange,
                onDelete = onDelete,
                onDismiss = onDismiss,
                hazeState = hazeState,
                forceBlurEnabled = forceBlurEnabled,
                blurDynamicFields = blurDynamicFields,
                blurPopups = blurPopups,
                settingsViewModel = settingsViewModel,
                statsMap = statsMap,
                onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
                isDesktop = false,
                state = state
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicFieldFullscreenContent(
    field: DynamicNoteState,
    title: String,
    onTitleChange: (String) -> Unit,
    contentValue: TextFieldValue,
    onContentValueChange: (TextFieldValue) -> Unit,
    isLocked: Boolean,
    onIsLockedChange: (Boolean) -> Unit,
    titlePlaceholder: String,
    contentPlaceholder: String,
    onFieldChange: (DynamicNoteState) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    hazeState: HazeState?,
    forceBlurEnabled: Boolean,
    blurDynamicFields: Boolean,
    blurPopups: Boolean,
    settingsViewModel: SettingsViewModel?,
    statsMap: Map<String, String>,
    onFullscreenDialogOpenChange: (Boolean) -> Unit,
    isDesktop: Boolean = false,
    state: ru.quasaris.characternexus.ui.CharacterDetailState? = null
) {
    val isOled = MaterialTheme.colorScheme.background == Color.Black
    val effectiveBlur = forceBlurEnabled && !isOled

    val currentOnFullscreenDialogOpenChange by rememberUpdatedState(onFullscreenDialogOpenChange)

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val contentBringIntoViewRequester = remember { BringIntoViewRequester() }
    val titleBringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val localHazeState = remember { HazeState() }
    var isFocused by remember { mutableStateOf(false) }
    var isTitleFocused by remember { mutableStateOf(false) }

    // Реальная (анимируемая системой) высота клавиатуры — читаем прямо в composition,
    // чтобы перекомпоновываться на каждый кадр анимации IME.
    val imeBottomPx = WindowInsets.ime.getBottom(density)

    val scrollMarginPx = with(density) { 40.dp.toPx() }

    var toolbarState by remember { mutableStateOf(Triple(contentValue, false, false)) }

    LaunchedEffect(contentValue, isFocused) {
        if (!isFocused) {
            toolbarState = Triple(contentValue, false, false)
            return@LaunchedEffect
        }
        toolbarState = Triple(contentValue, true, contentValue.selection.length > 0)
    }

    // Единый источник правды для автоскролла к курсору: реагирует и на смену
    // текста/выделения/фокуса, и на изменение высоты клавиатуры, поэтому докручивает
    // список уже после того, как IME анимация реально завершилась.
    LaunchedEffect(imeBottomPx, contentValue.selection, contentValue.text, isFocused) {
        if (!isFocused) return@LaunchedEffect
        val layoutResult = textLayoutResult
        if (layoutResult != null && contentValue.selection.collapsed) {
            val cursorRect = layoutResult.getCursorRect(contentValue.selection.start)
            contentBringIntoViewRequester.bringIntoView(
                cursorRect.copy(
                    top = cursorRect.top - scrollMarginPx,
                    bottom = cursorRect.bottom + scrollMarginPx
                )
            )
        } else {
            contentBringIntoViewRequester.bringIntoView()
        }
    }

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
    var isPreviewMode by remember { mutableStateOf(true) }
    var showLinkDialog by remember { mutableStateOf(false) }

    if (showLinkDialog) {
        val selection = contentValue.selection
        val selectedText = contentValue.text.substring(selection.min, selection.max)
        HyperlinkDialog(
            initialText = selectedText,
            initialUrl = "",
            onConfirm = { text, url ->
                val prefix = "["
                val middle = "]("
                val suffix = ")"
                val newText = contentValue.text.substring(0, selection.min) + prefix + text + middle + url + suffix + contentValue.text.substring(selection.max)
                val newSelection = TextRange(selection.min + prefix.length + text.length + middle.length + url.length + suffix.length)
                onContentValueChange(contentValue.copy(text = newText, selection = newSelection))
                showLinkDialog = false
            },
            onDismiss = { showLinkDialog = false }
        )
    }

    val isSubDialogOpen = showDeleteConfirm || showLinkDialog || state?.isResourceConfigOpen == true
    val masterBlurEnabled by settingsViewModel?.masterBlurEnabled?.collectAsState() ?: remember { mutableStateOf(true) }
    val blurRadius = rememberEffectiveBlurRadius(settingsViewModel)

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
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
                            IconButton(onClick = {
                                if (isPreviewMode) {
                                    isPreviewMode = false
                                    coroutineScope.launch {
                                        kotlinx.coroutines.delay(100)
                                        focusRequester.requestFocus()
                                    }
                                } else {
                                    isPreviewMode = true
                                }
                            }) {
                                Icon(
                                    if (isPreviewMode) Icons.Default.Edit else Icons.Default.Visibility,
                                    contentDescription = "Toggle Preview"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = if (effectiveBlur && hazeState != null && !isSubDialogOpen) colorScheme.background.copy(alpha = 0.01f) else colorScheme.surface
                        )
                    )
                },
                containerColor = if (effectiveBlur && hazeState != null && !isSubDialogOpen) colorScheme.background.copy(alpha = 0.01f) else colorScheme.background,
                modifier = Modifier
                    .fillMaxSize()
                    .run {
                        if (effectiveBlur && hazeState != null) {
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
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = if (effectiveBlur) colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
                            else colorScheme.surfaceContainerHighest,
                            shadowElevation = 0.dp,
                            tonalElevation = 0.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                val onSurface = colorScheme.onSurface
                                val uriHandler = LocalUriHandler.current
                                var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

                                val blocks = remember(contentValue.text) {
                                    DynamicContentParser.parse(contentValue.text)
                                }

                                if (isPreviewMode) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        val displayBlocks = remember(blocks) { DynamicContentParser.getDisplayBlocks(blocks) }
                                        displayBlocks.forEachIndexed { index, (absoluteIndex, block) ->
                                            when (block) {
                                                is DynamicContentBlock.Text -> {
                                                    val annotated = remember(block.content, onSurface) {
                                                        MarkdownHelper.parseMarkdown(block.content, onSurface, isEditing = false)
                                                    }
                                                    Text(
                                                        text = annotated,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .pointerInput(annotated) {
                                                                detectTapGestures { offset ->
                                                                    layoutResult?.let { textLayoutResult ->
                                                                        val position = textLayoutResult.getOffsetForPosition(offset)
                                                                        annotated.getLinkAnnotations(position, position)
                                                                            .firstOrNull()?.let { annotation ->
                                                                                if (annotation.item is LinkAnnotation.Url) {
                                                                                    uriHandler.openUri((annotation.item as LinkAnnotation.Url).url)
                                                                                }
                                                                            }
                                                                    }
                                                                }
                                                            },
                                                        fontSize = 18.sp,
                                                        lineHeight = 26.sp,
                                                        color = colorScheme.onSurface,
                                                        onTextLayout = { layoutResult = it }
                                                    )
                                                }
                                                is DynamicContentBlock.Divider -> {
                                                    val prevIsDivider = index > 0 && displayBlocks[index - 1].second is DynamicContentBlock.Divider
                                                    HorizontalDivider(
                                                        modifier = Modifier.padding(
                                                            top = if (prevIsDivider) 0.dp else 4.dp,
                                                            bottom = 4.dp
                                                        ),
                                                        thickness = 1.dp,
                                                        color = colorScheme.outlineVariant
                                                    )
                                                }
                                                is DynamicContentBlock.Spoiler -> {
                                                    val annotated = remember(block.content, onSurface) {
                                                        MarkdownHelper.parseMarkdown(block.content, onSurface, isEditing = false)
                                                    }
                                                    SpoilerComponent(content = annotated)
                                                }
                                                is DynamicContentBlock.Quote -> {
                                                    val annotated = remember(block.content, onSurface) {
                                                        MarkdownHelper.parseMarkdown(block.content, onSurface, isEditing = false)
                                                    }
                                                    QuoteComponent(content = annotated)
                                                }
                                                is DynamicContentBlock.Resource -> {
                                                    ResourceBlock(
                                                        resource = block,
                                                        statsMap = statsMap,
                                                        onUpdate = { updatedResource ->
                                                            val newBlocks = blocks.toMutableList()
                                                            if (absoluteIndex != -1 && absoluteIndex < newBlocks.size) {
                                                                newBlocks[absoluteIndex] = updatedResource
                                                                val newContent = DynamicContentParser.render(newBlocks)
                                                                onContentValueChange(contentValue.copy(text = newContent))
                                                            }
                                                        },
                                                        hazeState = null, // Handled by overlay
                                                        forceBlurEnabled = effectiveBlur,
                                                        blurDynamicFields = blurDynamicFields,
                                                        blurPopups = blurPopups,
                                                        settingsViewModel = settingsViewModel,
                                                        onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
                                                        onSubDialogOpenChange = { },
                                                        onDeleteRequest = {
                                                            val newBlocks = blocks.toMutableList()
                                                            if (absoluteIndex != -1 && absoluteIndex < newBlocks.size) {
                                                                newBlocks.removeAt(absoluteIndex)
                                                                val newContent = DynamicContentParser.render(newBlocks)
                                                                onContentValueChange(contentValue.copy(text = newContent))
                                                            }
                                                        },
                                                        isNested = true,
                                                        state = state,
                                                        onOpenConfig = state?.let { s ->
                                                            { res ->
                                                                s.activeResourceConfig = res
                                                                s.activeResourceIndex = absoluteIndex
                                                                s.isResourceConfigOpen = true
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    FormattingToolbar(
                                        value = toolbarState.first,
                                        onValueChange = {
                                            onContentValueChange(it)
                                        },
                                        isFocused = toolbarState.second,
                                        isSelectionActive = toolbarState.third,
                                        onLinkRequest = { showLinkDialog = true },
                                        onSave = {
                                            focusManager.clearFocus()
                                            isPreviewMode = true
                                            onFieldChange(field.copy(title = title, content = contentValue.text, isLocked = isLocked))
                                        }
                                    )

                                    key(field.id + "_fullscreen") {
                                        BasicTextField(
                                            value = contentValue,
                                            onValueChange = {
                                                onContentValueChange(it)
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .focusRequester(focusRequester)
                                                .onFocusChanged { isFocused = it.isFocused },
                                            onTextLayout = { textLayoutResult = it },
                                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                                fontSize = 18.sp,
                                                lineHeight = 26.sp,
                                                color = if (toolbarState.second) colorScheme.onSurface else Color.Transparent
                                            ),
                                            cursorBrush = SolidColor(colorScheme.primary),
                                            decorationBox = { innerTextField ->
                                                Box(modifier = Modifier.fillMaxWidth()) {
                                                    val onSurface = colorScheme.onSurface
                                                    val uriHandler = LocalUriHandler.current
                                                    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

                                                    val blocks = remember(contentValue.text) {
                                                        DynamicContentParser.parse(contentValue.text)
                                                    }

                                                    val marginStep by settingsViewModel?.topMarginStep?.collectAsState() ?: remember { mutableStateOf(2) }
                                                    val customMargin by settingsViewModel?.customTopMargin?.collectAsState() ?: remember { mutableStateOf(96) }
                                                    val topMargin = if (marginStep == 5) customMargin.dp else (marginStep * 48).dp

                                                    if (isFocused) {
                                                        Column {
                                                            if (toolbarState.second) {
                                                                Spacer(modifier = Modifier.height(topMargin))
                                                            }
                                                            Box(Modifier.bringIntoViewRequester(contentBringIntoViewRequester)) {
                                                                innerTextField()
                                                            }
                                                            Spacer(modifier = Modifier.height(32.dp))
                                                        }
                                                    } else {
                                                        Box(
                                                            Modifier
                                                                .alpha(0f)
                                                                .layout { measurable, constraints ->
                                                                    val placeable = measurable.measure(constraints)
                                                                    layout(placeable.width, 0) {
                                                                        placeable.place(0, 0)
                                                                    }
                                                                }
                                                        ) {
                                                            innerTextField()
                                                        }

                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                        ) {
                                                            val displayBlocks = remember(blocks) { DynamicContentParser.getDisplayBlocks(blocks) }
                                                            displayBlocks.forEachIndexed { index, (absoluteIndex, block) ->
                                                                when (block) {
                                                                    is DynamicContentBlock.Text -> {
                                                                        val annotated = remember(block.content, onSurface) {
                                                                            MarkdownHelper.parseMarkdown(block.content, onSurface, isEditing = false)
                                                                        }
                                                                        Text(
                                                                            text = annotated,
                                                                            fontSize = 18.sp,
                                                                            lineHeight = 26.sp,
                                                                            color = colorScheme.onSurface,
                                                                            modifier = Modifier
                                                                                .fillMaxWidth()
                                                                                .pointerInput(annotated) {
                                                                                    detectTapGestures(
                                                                                        onTap = { offset ->
                                                                                            layoutResult?.let { textLayoutResult ->
                                                                                                val position = textLayoutResult.getOffsetForPosition(offset)
                                                                                                val line = textLayoutResult.getLineForOffset(position)
                                                                                                val isWithinBounds = offset.x <= textLayoutResult.getLineRight(line)

                                                                                                if (isWithinBounds) {
                                                                                                    val annotation = annotated.getLinkAnnotations(position, position)
                                                                                                        .firstOrNull()
                                                                                                    if (annotation != null && annotation.item is LinkAnnotation.Url) {
                                                                                                        uriHandler.openUri((annotation.item as LinkAnnotation.Url).url)
                                                                                                    } else {
                                                                                                        focusRequester.requestFocus()
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    )
                                                                                },
                                                                            onTextLayout = { layoutResult = it }
                                                                        )
                                                                    }
                                                                    is DynamicContentBlock.Divider -> {
                                                                        val prevIsDivider = index > 0 && displayBlocks[index - 1].second is DynamicContentBlock.Divider
                                                                        HorizontalDivider(
                                                                            modifier = Modifier.padding(
                                                                                top = if (prevIsDivider) 0.dp else 4.dp,
                                                                                bottom = 4.dp
                                                                            ),
                                                                            thickness = 1.dp,
                                                                            color = colorScheme.outlineVariant
                                                                        )
                                                                    }
                                                                    is DynamicContentBlock.Spoiler -> {
                                                                        val annotated = remember(block.content, onSurface) {
                                                                            MarkdownHelper.parseMarkdown(block.content, onSurface, isEditing = false)
                                                                        }
                                                                        SpoilerComponent(content = annotated)
                                                                    }
                                                                    is DynamicContentBlock.Quote -> {
                                                                        val annotated = remember(block.content, onSurface) {
                                                                            MarkdownHelper.parseMarkdown(block.content, onSurface, isEditing = false)
                                                                        }
                                                                        QuoteComponent(content = annotated)
                                                                    }
                                                                    is DynamicContentBlock.Resource -> {
                                                                        ResourceBlock(
                                                                            resource = block,
                                                                            statsMap = statsMap,
                                                                            onUpdate = { updatedResource ->
                                                                                val newBlocks = blocks.toMutableList()
                                                                                if (absoluteIndex != -1 && absoluteIndex < newBlocks.size) {
                                                                                    newBlocks[absoluteIndex] = updatedResource
                                                                                    val newContent = DynamicContentParser.render(newBlocks)
                                                                                    onContentValueChange(contentValue.copy(text = newContent))
                                                                                }
                                                                            },
                                                                            hazeState = null, // Handled by overlay
                                                                            forceBlurEnabled = effectiveBlur,
                                                                            settingsViewModel = settingsViewModel,
                                                                            onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
                                                                            onSubDialogOpenChange = { },
                                                                            onDeleteRequest = {
                                                                                val newBlocks = blocks.toMutableList()
                                                                                if (absoluteIndex != -1 && absoluteIndex < newBlocks.size) {
                                                                                    newBlocks.removeAt(absoluteIndex)
                                                                                    val newContent = DynamicContentParser.render(newBlocks)
                                                                                    onFieldChange(field.copy(content = newContent))
                                                                                }
                                                                            },
                                                                            isNested = true,
                                                                            state = state,
                                                                            onOpenConfig = state?.let { s ->
                                                                                { res ->
                                                                                    s.activeResourceConfig = res
                                                                                    s.activeResourceIndex = absoluteIndex
                                                                                    s.isResourceConfigOpen = true
                                                                                }
                                                                            }
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                            Spacer(modifier = Modifier.height(80.dp))
                                                        }

                                                        if (contentValue.text.isEmpty()) {
                                                            Text(contentPlaceholder, fontSize = 18.sp, color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                                        }
                                                    }
                                                }
                                            }
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
                    }

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            onFieldChange(field.copy(title = title, content = contentValue.text, isLocked = isLocked))
                            onDismiss()
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Сохранить", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (state?.isResourceConfigOpen == true && state.activeResourceConfig != null) {
                ResourceConfigDialog(
                    resource = state.activeResourceConfig!!,
                    onDismiss = {
                        state.isResourceConfigOpen = false
                        state.activeResourceConfig = null
                    },
                    onSave = { updated ->
                        state.updateResource(updated)
                        val currentBlocks = DynamicContentParser.parse(contentValue.text).toMutableList()
                        val resIndex = state.activeResourceIndex
                        if (resIndex != -1 && resIndex < currentBlocks.size) {
                            currentBlocks[resIndex] = updated
                            onContentValueChange(contentValue.copy(text = DynamicContentParser.render(currentBlocks)))
                        } else {
                            // Fallback to ID matching
                            onContentValueChange(contentValue.copy(text = DynamicContentParser.render(currentBlocks.map {
                                if (it is DynamicContentBlock.Resource && it.id == updated.id && it.id.isNotEmpty()) updated else it
                            })))
                        }
                        state.isResourceConfigOpen = false
                        state.activeResourceConfig = null
                        state.activeResourceIndex = -1
                    },
                    onDelete = { deleted ->
                        state.deleteResource(deleted)
                        val currentBlocks = DynamicContentParser.parse(contentValue.text).toMutableList()
                        val resIndex = state.activeResourceIndex
                        if (resIndex != -1 && resIndex < currentBlocks.size) {
                            currentBlocks.removeAt(resIndex)
                            onContentValueChange(contentValue.copy(text = DynamicContentParser.render(currentBlocks)))
                        } else {
                            // Fallback to ID matching
                            onContentValueChange(contentValue.copy(text = DynamicContentParser.render(currentBlocks.filter {
                                !(it is DynamicContentBlock.Resource && it.id == deleted.id && it.id.isNotEmpty())
                            })))
                        }
                        state.isResourceConfigOpen = false
                        state.activeResourceConfig = null
                        state.activeResourceIndex = -1
                    },
                    forceBlurEnabled = effectiveBlur,
                    settingsViewModel = settingsViewModel,
                    hazeState = localHazeState,
                    isNested = true,
                    asOverlay = true,
                    isDesktop = isDesktop
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
