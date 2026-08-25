package ru.quasaris.characternexus.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch
import ru.quasaris.characternexus.backend.ArchiveManager
import ru.quasaris.characternexus.backend.ImportResult
import ru.quasaris.characternexus.backend.ImageManager
import ru.quasaris.characternexus.backend.LssAvatarService
import ru.quasaris.characternexus.backend.SettingsViewModel
import ru.quasaris.characternexus.backend.cropper.AvatarCropperWindow
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.ui.components.ReorderableAdaptiveGrid
import ru.quasaris.characternexus.util.decodeImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import ru.quasaris.characternexus.util.ImageProcessor
import ru.quasaris.characternexus.util.HapticType
import ru.quasaris.characternexus.util.PlatformUtils

@Composable
fun MenuWindow(
    characters: List<CharacterSummary>,
    onNavigateToCreate: () -> Unit,
    onCharacterClick: (String) -> Unit,
    onImportCharacter: (Character) -> Unit,
    onDeleteCharacters: (List<String>) -> Unit,
    onReorderCharacters: (List<String>) -> Unit,
    getFullCharacter: suspend (String) -> Character?,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    onFullscreenDialogOpenChange: (Boolean) -> Unit = {},
    settingsViewModel: SettingsViewModel? = null,
    hazeState: HazeState? = null,
    popupHazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    blurPopups: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    val selectedIds = remember { mutableStateListOf<String>() }

    val autoDownload by settingsViewModel?.autoDownloadLssAvatar?.collectAsState() ?: remember { mutableStateOf(false) }
    val useOldAvatarStyle by settingsViewModel?.useOldAvatarStyle?.collectAsState() ?: remember { mutableStateOf(false) }
    val veryResponsive by settingsViewModel?.veryResponsiveHaptics?.collectAsState() ?: remember { mutableStateOf(true) }

    fun performClickHaptic() {
        if (veryResponsive) {
            PlatformUtils.performHapticFeedback(HapticType.CLICK)
        }
    }
    
    var lssAvatarToDownload by remember { mutableStateOf<Character?>(null) }
    var importErrorMessage by remember { mutableStateOf<String?>(null) }
    var showFilePicker by remember { mutableStateOf(false) }
    var showExportSaver by remember { mutableStateOf(false) }
    var exportUuids by remember { mutableStateOf<List<String>>(emptyList()) }
    
    val pendingImportResults = remember { mutableStateListOf<ru.quasaris.characternexus.backend.ImportResult>() }
    var imageToCrop by remember { mutableStateOf<ImageBitmap?>(null) }

    val isAnyFullscreenDialogOpen = imageToCrop != null || lssAvatarToDownload != null || importErrorMessage != null || pendingImportResults.isNotEmpty()
    LaunchedEffect(isAnyFullscreenDialogOpen) {
        onFullscreenDialogOpenChange(isAnyFullscreenDialogOpen)
    }

    fun processNextImport() {
        if (pendingImportResults.isEmpty()) return
        val next = pendingImportResults.first()
        val portraitBytes = next.portraitBytes ?: next.originalBytes
        
        if (portraitBytes != null) {
            try {
                imageToCrop = decodeImageBitmap(portraitBytes)
            } catch (e: Exception) {
                onImportCharacter(next.character)
                pendingImportResults.removeAt(0)
                processNextImport()
            }
        } else if (next.character.avatarUrl != null && next.character.imageData == null) {
            if (autoDownload) {
                scope.launch {
                    val avatarBytes = LssAvatarService.downloadAvatar(next.character)
                    if (avatarBytes != null) {
                        try {
                            imageToCrop = decodeImageBitmap(avatarBytes)
                            pendingImportResults[0] = next.copy(portraitBytes = avatarBytes, originalBytes = avatarBytes)
                        } catch (e: Exception) {
                            onImportCharacter(next.character)
                            pendingImportResults.removeAt(0)
                            processNextImport()
                        }
                    } else {
                        onImportCharacter(next.character)
                        pendingImportResults.removeAt(0)
                        processNextImport()
                    }
                }
            } else if (pendingImportResults.size == 1) {
                lssAvatarToDownload = next.character
                pendingImportResults.clear()
            } else {
                onImportCharacter(next.character)
                pendingImportResults.removeAt(0)
                processNextImport()
            }
        } else {
            onImportCharacter(next.character)
            pendingImportResults.removeAt(0)
            processNextImport()
        }
    }

    CommonFilePicker(show = showFilePicker, fileExtensions = listOf("cb", "charbook", "lsskiller", "json")) { file ->
        showFilePicker = false
        if (file == null) return@CommonFilePicker
        
        scope.launch {
            try {
                val bytes = file.readBytes()
                val results = ArchiveManager.importCharacters(bytes)
                
                if (results.isNotEmpty()) {
                    pendingImportResults.clear()
                    pendingImportResults.addAll(results)
                    processNextImport()
                } else {
                    importErrorMessage = "Не удалось распознать файл. Пожалуйста, выберите другой файл персонажа."
                }
            } catch (e: Exception) {
                importErrorMessage = "Ошибка при чтении файла: ${e.message}"
            }
        }
    }

    CommonFileSaver(
        show = showExportSaver,
        fileName = "CharactersBundle",
        fileExtension = ArchiveManager.EXPORT_EXTENSION
    ) { saver ->
        showExportSaver = false
        if (saver == null) return@CommonFileSaver
        
        scope.launch {
            val charsToExport = mutableListOf<Character>()
            exportUuids.forEach { uuid ->
                val fullChar = getFullCharacter(uuid)
                if (fullChar != null) {
                    charsToExport.add(fullChar)
                }
            }
            if (charsToExport.isNotEmpty()) {
                val bytes = ArchiveManager.getExportBundleBytes(charsToExport)
                saver.save(bytes)
            }
        }
    }

    if (imageToCrop != null && pendingImportResults.isNotEmpty()) {
        val result = pendingImportResults.first()
        AvatarCropperWindow(
            imageBitmap = imageToCrop!!,
            onCrop = { cropped ->
                val char = result.character
                scope.launch {
                    val croppedBytes = ImageProcessor.encodeToByteArray(cropped)
                    ImageManager.saveCharacterImages(
                        characterUuid = char.uuid,
                        originalBytes = result.originalBytes ?: result.portraitBytes,
                        portraitBytes = result.portraitBytes ?: result.originalBytes,
                        croppedBytes = croppedBytes
                    )
                    onImportCharacter(char)
                    imageToCrop = null
                    pendingImportResults.removeAt(0)
                    processNextImport()
                }
            },
            onDismiss = {
                onImportCharacter(result.character.copy(imageData = null))
                imageToCrop = null
                pendingImportResults.removeAt(0)
                processNextImport()
            }
        )
    }

    if (importErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { importErrorMessage = null },
            title = { Text("Ошибка импорта") },
            text = { Text(importErrorMessage!!) },
            confirmButton = {
                TextButton(onClick = { importErrorMessage = null }) { Text("OK") }
            }
        )
    }

    if (lssAvatarToDownload != null) {
        AlertDialog(
            onDismissRequest = { 
                onImportCharacter(lssAvatarToDownload!!)
                lssAvatarToDownload = null 
            },
            title = { Text("Загрузить аватарку?") },
            text = { Text("Персонаж из Long Story Short имеет аватарку. Хотите скачать её?") },
            confirmButton = {
                TextButton(onClick = {
                    val char = lssAvatarToDownload!!
                    lssAvatarToDownload = null
                    scope.launch {
                        val avatarBytes = LssAvatarService.downloadAvatar(char)
                        if (avatarBytes != null) {
                            try {
                                imageToCrop = decodeImageBitmap(avatarBytes)
                                pendingImportResults.add(ImportResult(
                                    character = char,
                                    portraitBytes = avatarBytes,
                                    originalBytes = avatarBytes
                                ))
                            } catch (e: Exception) {
                                onImportCharacter(char)
                            }
                        } else {
                            onImportCharacter(char)
                        }
                    }
                }) { Text("Да") }
            },
            dismissButton = {
                TextButton(onClick = {
                    onImportCharacter(lssAvatarToDownload!!)
                    lssAvatarToDownload = null
                }) { Text("Нет") }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = colorScheme.background,
            topBar = {
                Column(
                    modifier = Modifier
                        .background(colorScheme.surface)
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            performClickHaptic()
                            onOpenDrawer()
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = null, modifier = Modifier.size(32.dp), tint = colorScheme.onSurface)
                        }
                        Text(
                            text = "Персонажи",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            color = colorScheme.onSurface
                        )
                        IconButton(onClick = { 
                            performClickHaptic()
                            showFilePicker = true 
                        }) {
                            Icon(Icons.Default.UploadFile, contentDescription = "Загрузить", tint = colorScheme.onSurface)
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(colorScheme.surface)
                    .fillMaxSize()
                    .padding(16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (selectedIds.isNotEmpty()) selectedIds.clear()
                    }
            ) {
                ReorderableAdaptiveGrid(
                    items = characters,
                    key = { it.uuid },
                    onReorder = { newList ->
                        onReorderCharacters(newList.map { it.uuid })
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    isReorderEnabled = selectedIds.isNotEmpty()
                ) { character, isDragging, dragModifier ->
                    val isSelected = character.uuid in selectedIds
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedIds.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "Drag",
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(32.dp)
                                    .then(dragModifier),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }

                        CharacterCard(
                            character = character,
                            isSelected = isSelected,
                            useOldAvatarStyle = useOldAvatarStyle,
                            modifier = Modifier.weight(1f).graphicsLayer {
                                alpha = if (isDragging) 0.5f else 1f
                                scaleX = if (isDragging) 1.05f else 1f
                                scaleY = if (isDragging) 1.05f else 1f
                            },
                            onClick = {
                                performClickHaptic()
                                if (selectedIds.isNotEmpty()) {
                                    if (isSelected) selectedIds.remove(character.uuid)
                                    else selectedIds.add(character.uuid)
                                } else {
                                    onCharacterClick(character.uuid)
                                }
                            },
                            onLongClick = {
                                PlatformUtils.performHapticFeedback(HapticType.LONG_PRESS)
                                if (character.uuid !in selectedIds) {
                                    selectedIds.add(character.uuid)
                                }
                            }
                        )
                    }
                }

                Button(
                    onClick = {
                        performClickHaptic()
                        onNavigateToCreate()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .outerShadow(RoundedCornerShape(12.dp), blur = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primaryContainer,
                        contentColor = colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Создать нового персонажа", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        AnimatedVisibility(
            visible = selectedIds.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
                .padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        exportUuids = selectedIds.toList()
                        showExportSaver = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .outerShadow(RoundedCornerShape(12.dp), blur = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.secondaryContainer,
                        contentColor = colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Экспорт", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }

                Button(
                    onClick = {
                        PlatformUtils.performHapticFeedback(HapticType.ERROR)
                        onDeleteCharacters(selectedIds.toList())
                        selectedIds.clear()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .outerShadow(RoundedCornerShape(12.dp), blur = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.error,
                        contentColor = colorScheme.onError
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Удалить", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
