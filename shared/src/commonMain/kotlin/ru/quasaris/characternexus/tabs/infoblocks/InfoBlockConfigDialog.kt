package ru.quasaris.characternexus.tabs.infoblocks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.chrisbanes.haze.*
import ru.quasaris.characternexus.ui.theme.rememberEffectiveHazeStyle
import ru.quasaris.characternexus.backend.SettingsViewModel
import ru.quasaris.characternexus.model.DynamicContentBlock
import ru.quasaris.characternexus.ui.BackHandler
import ru.quasaris.characternexus.ui.CharacterDetailState
import ru.quasaris.characternexus.ui.DeleteConfirmationDialog
import ru.quasaris.characternexus.ui.DialogDimStyle
import ru.quasaris.characternexus.ui.PredictiveBackBox
import ru.quasaris.characternexus.ui.theme.rememberEffectiveBlurRadius

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoBlockConfigDialog(
    infoBlock: DynamicContentBlock.InfoBlock,
    onDismiss: () -> Unit,
    onSave: (DynamicContentBlock.InfoBlock) -> Unit,
    onConvertToResource: (DynamicContentBlock.InfoBlock) -> Unit,
    onDelete: (DynamicContentBlock.InfoBlock) -> Unit,
    forceBlurEnabled: Boolean = false,
    settingsViewModel: SettingsViewModel? = null,
    onFullscreenDialogOpenChange: (Boolean) -> Unit = {},
    isDesktop: Boolean = false,
    hazeState: HazeState? = null,
    isNested: Boolean = false,
    asOverlay: Boolean = false
) {
    var configState by remember { mutableStateOf(infoBlock) }

    LaunchedEffect(configState) {
        if (configState != infoBlock) {
            onSave(configState)
        }
    }

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val blurRadius = rememberEffectiveBlurRadius(settingsViewModel)

    val handleDismiss = {
        focusManager.clearFocus()
        onDismiss()
    }

    val currentOnFullscreenDialogOpenChange by rememberUpdatedState(onFullscreenDialogOpenChange)

    DisposableEffect(Unit) {
        currentOnFullscreenDialogOpenChange(true)
        onDispose {
            currentOnFullscreenDialogOpenChange(false)
        }
    }

    val content = @Composable {
        InfoBlockConfigDialogContent(
            state = configState,
            onStateChange = { newState ->
                configState = newState
                onSave(newState)
            },
            onDismiss = handleDismiss,
            onConvertToResource = {
                onConvertToResource(it)
                handleDismiss()
            },
            onDelete = { blockToDelete ->
                onDelete(blockToDelete)
                handleDismiss()
            },
            forceBlurEnabled = forceBlurEnabled,
            hazeState = hazeState,
            blurRadius = blurRadius,
            isNested = isNested,
            isDesktop = isDesktop,
            asOverlay = asOverlay,
            settingsViewModel = settingsViewModel
        )
    }

    if (isDesktop || asOverlay) {
        content()
    } else {
        Dialog(
            onDismissRequest = handleDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            DialogDimStyle(0f)
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoBlockConfigDialogContent(
    state: DynamicContentBlock.InfoBlock,
    onStateChange: (DynamicContentBlock.InfoBlock) -> Unit,
    onDismiss: () -> Unit,
    onConvertToResource: (DynamicContentBlock.InfoBlock) -> Unit,
    onDelete: (DynamicContentBlock.InfoBlock) -> Unit,
    forceBlurEnabled: Boolean,
    hazeState: HazeState?,
    blurRadius: androidx.compose.ui.unit.Dp = 24.dp,
    isNested: Boolean = false,
    isDesktop: Boolean = false,
    asOverlay: Boolean = false,
    settingsViewModel: SettingsViewModel? = null
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var isInputFocused by remember { mutableStateOf(false) }

    val handleBack = {
        if (isInputFocused) {
            focusManager.clearFocus()
        } else {
            onDismiss()
        }
    }

    PredictiveBackBox(
        onBack = handleBack,
        modifier = Modifier
            .fillMaxSize()
            .onFocusChanged { focusState ->
                isInputFocused = focusState.hasFocus
            }
            .onPreviewKeyEvent { event ->
                if (event.key == Key.Escape && event.type == KeyEventType.KeyDown) {
                    if (isInputFocused) {
                        focusManager.clearFocus()
                        return@onPreviewKeyEvent true
                    }
                }
                false
            }
    ) { _ ->
        val colorScheme = MaterialTheme.colorScheme
        val isOled = colorScheme.background == Color.Black
        val masterBlurEnabled by settingsViewModel?.masterBlurEnabled?.collectAsState() ?: remember { mutableStateOf(true) }
        var showDeleteConfirm by remember { mutableStateOf(false) }

        val innerContent = @Composable {
            InfoBlockConfigDialogInner(
                state = state,
                onStateChange = onStateChange,
                onDismiss = onDismiss,
                onConvertToResource = { onConvertToResource(state) },
                onDelete = { showDeleteConfirm = true }
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .run {
                        if (showDeleteConfirm && masterBlurEnabled) {
                            this.blur(blurRadius)
                        } else this
                    }
            ) {
                if (asOverlay) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = { Text("Настройка инфоблока", fontWeight = FontWeight.Black) },
                                navigationIcon = {
                                    IconButton(onClick = onDismiss) {
                                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                                    }
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = if (masterBlurEnabled && !showDeleteConfirm) Color.Transparent else colorScheme.surface
                                )
                            )
                        },
                        containerColor = if (masterBlurEnabled && !showDeleteConfirm) Color.Transparent else colorScheme.background
                    ) { paddingValues ->
                        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                            innerContent()
                        }
                    }
                } else {
                    val hazeStyle = rememberEffectiveHazeStyle(blurRadius = blurRadius)
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .run {
                                if (forceBlurEnabled && hazeState != null && !isOled && !isNested) {
                                    this.hazeEffect(state = hazeState, style = hazeStyle)
                                } else this
                            }
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) { focusManager.clearFocus() },
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = { Text("Настройка инфоблока", fontWeight = FontWeight.Black) },
                                navigationIcon = {
                                    IconButton(onClick = onDismiss) {
                                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                                    }
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = if (forceBlurEnabled && !isOled && hazeState != null && !isNested && !showDeleteConfirm) Color.Transparent else colorScheme.surface
                                )
                            )
                        },
                        containerColor = if (forceBlurEnabled && !isOled && hazeState != null && !isNested && !showDeleteConfirm) Color.Transparent else colorScheme.background
                    ) { paddingValues ->
                        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                            innerContent()
                        }
                    }
                }
            }

            if (showDeleteConfirm) {
                DeleteConfirmationDialog(
                    showDialog = showDeleteConfirm,
                    onDismiss = { showDeleteConfirm = false },
                    onConfirm = {
                        onDelete(state)
                        showDeleteConfirm = false
                    },
                    title = "Удалить инфоблок?",
                    text = "Инфоблок будет полностью удалён.",
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}

@Composable
fun InfoBlockConfigDialogInner(
    state: DynamicContentBlock.InfoBlock,
    onStateChange: (DynamicContentBlock.InfoBlock) -> Unit,
    onDismiss: () -> Unit,
    onConvertToResource: () -> Unit,
    onDelete: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = { onStateChange(state.copy(title = it)) },
                label = { Text("Название") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )

            OutlinedTextField(
                value = state.link ?: "",
                onValueChange = { onStateChange(state.copy(link = it.ifBlank { null })) },
                label = { Text("Ссылка (опционально)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )

            val infoDescState = remember(state.id, state.description) {
                ru.quasaris.characternexus.model.DynamicNoteState(id = "infoblock_desc_${state.id}", content = state.description)
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Описание", style = MaterialTheme.typography.labelMedium, color = colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        ru.quasaris.characternexus.tabs.NotionBlockEditor(
                            field = infoDescState,
                            onFieldChange = { updated -> onStateChange(state.copy(description = updated.content)) },
                            canEdit = true,
                            isReorderMode = false,
                            contentPlaceholder = "Описание инфоблока...",
                            isBasicMode = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onConvertToResource,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.primary),
                border = BorderStroke(1.dp, colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddBox,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp).padding(end = 6.dp)
                )
                Text("Преобразовать в ресурс")
            }

            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                border = BorderStroke(1.dp, Color.Red),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Удалить")
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        Button(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.compositeOver(MaterialTheme.colorScheme.background),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Text("Закрыть", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}
