package ru.quasaris.characternexus.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
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
import ru.quasaris.characternexus.util.decodeImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import ru.quasaris.characternexus.util.ImageProcessor
import ru.quasaris.characternexus.util.HapticType
import ru.quasaris.characternexus.util.PlatformUtils
import ru.quasaris.characternexus.util.generateUuid

import ru.quasaris.characternexus.ui.menu.CharacterFolderTree
import ru.quasaris.characternexus.ui.util.FolderColors
import ru.quasaris.characternexus.ui.util.PayWall
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape


@Composable
fun MenuWindow(
    characters: List<CharacterSummary>,
    folders: List<CharacterFolder> = emptyList(),
    globalOrder: List<String> = emptyList(),
    onNavigateToCreate: () -> Unit,
    onCharacterClick: (String) -> Unit,
    onImportCharacter: (Character) -> Unit,
    onDeleteCharacters: (List<String>) -> Unit,
    getFullCharacter: suspend (String) -> Character?,
    onOpenDrawer: () -> Unit,
    onCreateFolder: (String, Int?) -> Unit = { _, _ -> },
    onUpdateFolder: (CharacterFolder) -> Unit = {},
    onDeleteFolder: (String, Boolean) -> Unit = { _, _ -> },
    onMoveCharactersToFolder: (List<String>, String?, String?) -> Unit = { _, _, _ -> },
    onMoveFolderToFolder: (String, String?, String?) -> Unit = { _, _, _ -> },
    onToggleFolderExpansion: (String) -> Unit = {},
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
    var isEditMode by remember { mutableStateOf(false) }
    var isManualEditMode by remember { mutableStateOf(false) }

    val autoDownload by settingsViewModel?.autoDownloadLssAvatar?.collectAsState() ?: remember { mutableStateOf(false) }
    val veryResponsive by settingsViewModel?.veryResponsiveHaptics?.collectAsState() ?: remember { mutableStateOf(true) }
    val isPremium by settingsViewModel?.isPremium?.collectAsState() ?: remember { mutableStateOf(false) }

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

    var showCreateFolderDialog by remember { mutableStateOf(false) }

    val isAnyFullscreenDialogOpen = imageToCrop != null || lssAvatarToDownload != null || importErrorMessage != null || pendingImportResults.isNotEmpty() || showCreateFolderDialog
    LaunchedEffect(isAnyFullscreenDialogOpen) {
        onFullscreenDialogOpenChange(isAnyFullscreenDialogOpen)
    }

    LaunchedEffect(selectedIds.size, isManualEditMode) {
        if (selectedIds.isEmpty() && !isManualEditMode && isEditMode) {
            isEditMode = false
        }
    }

    var folderNameInput by remember { mutableStateOf("") }
    var folderColorInput by remember { mutableStateOf<Int?>(null) }
    var customColorHex by remember { mutableStateOf("") }

    val isDark = isSystemInDarkTheme()

    @Composable
    fun ColorSelectionGrid(
        selectedColorArgb: Int?,
        onColorSelect: (Int?) -> Unit,
        customHex: String,
        onCustomHexChange: (String) -> Unit
    ) {
        val freeColors = FolderColors.predefinedColors.filter { !it.isPremium }
        val premiumColors = FolderColors.predefinedColors.filter { it.isPremium }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                freeColors.forEach { folderColor ->
                    val color = if (isDark) folderColor.dark else folderColor.light
                    val argb = color.toArgb()
                    val isSelected = selectedColorArgb == argb

                    Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                        Surface(
                            onClick = { onColorSelect(argb) },
                            shape = CircleShape,
                            color = color,
                            border = if (isSelected) BorderStroke(3.dp, colorScheme.primary) else null,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (isSelected) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Check,
                                        null,
                                        tint = FolderColors.getBestIconTint(color),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            PayWall(
                isLocked = !isPremium,
                modifier = Modifier.fillMaxWidth().padding(horizontal = (2).dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        premiumColors.chunked(4).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { folderColor ->
                                    val color = if (isDark) folderColor.dark else folderColor.light
                                    val argb = color.toArgb()
                                    val isSelected = selectedColorArgb == argb

                                    Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                                        Surface(
                                            onClick = { onColorSelect(argb) },
                                            shape = CircleShape,
                                            color = color,
                                            border = if (isSelected) BorderStroke(3.dp, colorScheme.primary) else null,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            if (isSelected) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        null,
                                                        tint = FolderColors.getBestIconTint(color),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = customHex,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() || it.uppercaseChar() in 'A'..'F' }.take(6)
                            onCustomHexChange(filtered)
                            if (filtered.length == 6) {
                                try {
                                    val argb = (0xFF000000 or filtered.toLong(16)).toInt()
                                    onColorSelect(argb)
                                } catch (e: Exception) {}
                            }
                        },
                        label = { Text("Свой HEX цвет") },
                        placeholder = { Text("RRGGBB") },
                        prefix = { Text("#") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            TextButton(
                onClick = { onColorSelect(null); onCustomHexChange("") },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Сбросить цвет")
            }
        }
    }

    var folderToManage by remember { mutableStateOf<CharacterFolder?>(null) }
    var showFolderDeleteConfirm by remember { mutableStateOf(false) }

    var showMoveToFolderSheet by remember { mutableStateOf(false) }

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
                            pendingImportResults[0] = next.copy(
                                character = next.character.copy(imageData = generateUuid()),
                                portraitBytes = avatarBytes,
                                originalBytes = avatarBytes
                            )
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
                    val updatedChar = char.copy(imageData = char.imageData ?: generateUuid())
                    ImageManager.saveCharacterImages(
                        characterUuid = updatedChar.uuid,
                        originalBytes = result.originalBytes ?: result.portraitBytes,
                        portraitBytes = result.portraitBytes ?: result.originalBytes,
                        croppedBytes = croppedBytes
                    )
                    onImportCharacter(updatedChar)
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
                                    character = char.copy(imageData = generateUuid()),
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
                            isEditMode = !isEditMode
                            isManualEditMode = isEditMode
                            if (!isEditMode) selectedIds.clear()
                        }) {
                            Icon(
                                imageVector = if (isEditMode) Icons.Default.Close else Icons.Default.Edit,
                                contentDescription = if (isEditMode) "Закрыть" else "Редактировать",
                                tint = if (isEditMode) colorScheme.primary else colorScheme.onSurface
                            )
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
                        if (selectedIds.isNotEmpty()) {
                            selectedIds.clear()
                            if (!isManualEditMode) isEditMode = false
                        }
                    }
            ) {
                CharacterFolderTree(
                    characters = characters,
                    folders = folders,
                    globalOrder = globalOrder,
                    selectedIds = selectedIds,
                    isEditMode = isEditMode,
                    onMoveItem = { itemId, targetFolderId, beforeItemId ->
                        val isChar = characters.any { it.uuid == itemId }
                        if (isChar) {
                            onMoveCharactersToFolder(listOf(itemId), targetFolderId, beforeItemId)
                        } else {
                            onMoveFolderToFolder(itemId, targetFolderId, beforeItemId)
                        }
                    },
                    onCharacterClick = { uuid ->
                        performClickHaptic()
                        if (isEditMode) {
                            if (uuid in selectedIds) selectedIds.remove(uuid)
                            else selectedIds.add(uuid)
                        } else {
                            onCharacterClick(uuid)
                        }
                    },
                    onCharacterLongClick = { uuid ->
                        PlatformUtils.performHapticFeedback(HapticType.LONG_PRESS)
                        if (!isEditMode) {
                            isEditMode = true
                            isManualEditMode = false
                            selectedIds.add(uuid)
                        } else {
                            if (uuid in selectedIds) selectedIds.remove(uuid)
                            else selectedIds.add(uuid)
                        }
                    },
                    onFolderToggle = { uuid ->
                        performClickHaptic()
                        if (isEditMode) {
                            if (uuid in selectedIds) selectedIds.remove(uuid)
                            else selectedIds.add(uuid)
                        } else {
                            onToggleFolderExpansion(uuid)
                        }
                    },
                    onFolderExpansionToggle = { uuid ->
                        performClickHaptic()
                        onToggleFolderExpansion(uuid)
                    },
                    onFolderLongClick = { uuid ->
                        PlatformUtils.performHapticFeedback(HapticType.LONG_PRESS)
                        if (!isEditMode) {
                            isEditMode = true
                            isManualEditMode = false
                            selectedIds.add(uuid)
                        } else {
                            if (uuid in selectedIds) selectedIds.remove(uuid)
                            else selectedIds.add(uuid)
                        }
                    },
                    onFolderMoreClick = { folder ->
                        performClickHaptic()
                        folderToManage = folder
                    },
                    modifier = Modifier.weight(1f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            performClickHaptic()
                            showFilePicker = true
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .outerShadow(RoundedCornerShape(12.dp), blur = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.secondaryContainer,
                            contentColor = colorScheme.onSecondaryContainer
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Загрузить")
                    }

                    Button(
                        onClick = {
                            performClickHaptic()
                            onNavigateToCreate()
                        },
                        modifier = Modifier
                            .weight(2f)
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
                        Text("Создать персонажа", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = {
                            performClickHaptic()
                            showCreateFolderDialog = true
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .outerShadow(RoundedCornerShape(12.dp), blur = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.secondaryContainer,
                            contentColor = colorScheme.onSecondaryContainer
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                    }
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            showMoveToFolderSheet = true
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(52.dp)
                            .outerShadow(RoundedCornerShape(12.dp), blur = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.tertiaryContainer,
                            contentColor = colorScheme.onTertiaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("В папку", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = {
                            PlatformUtils.performHapticFeedback(HapticType.ERROR)
                            val selectedChars = selectedIds.filter { uuid -> characters.any { it.uuid == uuid } }
                            val selectedFolders = selectedIds.filter { uuid -> folders.any { it.uuid == uuid } }

                            if (selectedChars.isNotEmpty()) {
                                onDeleteCharacters(selectedChars)
                            }
                            if (selectedFolders.isNotEmpty()) {
                                selectedFolders.forEach { onDeleteFolder(it, false) }
                            }
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
                }
            }
        }

        if (showCreateFolderDialog) {
            AlertDialog(
                onDismissRequest = {
                    showCreateFolderDialog = false
                    folderNameInput = ""
                    folderColorInput = null
                    customColorHex = ""
                },
                title = { Text("Создать папку") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = folderNameInput,
                            onValueChange = { folderNameInput = it },
                            label = { Text("Название") },
                            placeholder = { Text("Напр. Кампания 1") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ColorSelectionGrid(
                            selectedColorArgb = folderColorInput,
                            onColorSelect = { folderColorInput = it },
                            customHex = customColorHex,
                            onCustomHexChange = { customColorHex = it }
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (folderNameInput.isNotBlank()) {
                                onCreateFolder(folderNameInput, folderColorInput)
                                folderNameInput = ""
                                folderColorInput = null
                                customColorHex = ""
                                showCreateFolderDialog = false
                            }
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("Создать") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showCreateFolderDialog = false
                        folderNameInput = ""
                        folderColorInput = null
                        customColorHex = ""
                    }) { Text("Отмена") }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }

        var showRenameDialog by remember { mutableStateOf(false) }
        var newFolderName by remember { mutableStateOf("") }

        if (folderToManage != null) {
            AlertDialog(
                onDismissRequest = { folderToManage = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, null, tint = colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text(folderToManage!!.name, style = MaterialTheme.typography.headlineSmall)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            onClick = {
                                newFolderName = folderToManage!!.name
                                folderColorInput = folderToManage!!.colorArgb
                                customColorHex = folderToManage!!.colorArgb?.let {
                                    val hex = it.toUInt().toString(16).uppercase()
                                    if (hex.length >= 6) hex.takeLast(6) else hex.padStart(6, '0')
                                } ?: ""
                                showRenameDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Palette, null, tint = colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(16.dp))
                                Text("Настроить папку", style = MaterialTheme.typography.bodyLarge)
                            }
                        }

                        Surface(
                            onClick = {
                                selectedIds.clear()
                                selectedIds.add(folderToManage!!.uuid)
                                showMoveToFolderSheet = true
                                folderToManage = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Filled.DriveFileMove, null, tint = colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(16.dp))
                                Text("Переместить", style = MaterialTheme.typography.bodyLarge)
                            }
                        }

                        Surface(
                            onClick = {
                                showFolderDeleteConfirm = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = colorScheme.errorContainer.copy(alpha = 0.2f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Delete, null, tint = colorScheme.error)
                                Spacer(Modifier.width(16.dp))
                                Text("Удалить папку", style = MaterialTheme.typography.bodyLarge, color = colorScheme.error)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { folderToManage = null }) { Text("Готово") }
                },
                shape = RoundedCornerShape(28.dp)
            )
        }

        if (showRenameDialog && folderToManage != null) {
            AlertDialog(
                onDismissRequest = {
                    showRenameDialog = false
                    folderColorInput = null
                    customColorHex = ""
                },
                title = { Text("Настройка папки") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = newFolderName,
                            onValueChange = { newFolderName = it },
                            label = { Text("Новое название") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ColorSelectionGrid(
                            selectedColorArgb = folderColorInput,
                            onColorSelect = { folderColorInput = it },
                            customHex = customColorHex,
                            onCustomHexChange = { customColorHex = it }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (newFolderName.isNotBlank()) {
                            onUpdateFolder(folderToManage!!.copy(name = newFolderName, colorArgb = folderColorInput))
                            showRenameDialog = false
                            folderToManage = null
                            folderColorInput = null
                            customColorHex = ""
                        }
                    }) { Text("Сохранить") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showRenameDialog = false
                        folderColorInput = null
                        customColorHex = ""
                    }) { Text("Отмена") }
                }
            )
        }

        if (showFolderDeleteConfirm && folderToManage != null) {
            AlertDialog(
                onDismissRequest = { showFolderDeleteConfirm = false },
                title = { Text("Удалить папку?") },
                text = { Text("Вы хотите удалить папку '${folderToManage!!.name}'? Персонажи останутся в общем списке.") },
                confirmButton = {
                    TextButton(onClick = {
                        onDeleteFolder(folderToManage!!.uuid, false)
                        showFolderDeleteConfirm = false
                        folderToManage = null
                    }) { Text("Да, удалить папку") }
                },
                dismissButton = {
                    TextButton(onClick = { showFolderDeleteConfirm = false }) { Text("Отмена") }
                }
            )
        }

        if (showMoveToFolderSheet) {
            AlertDialog(
                onDismissRequest = { showMoveToFolderSheet = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.DriveFileMove, null, tint = colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("Переместить", style = MaterialTheme.typography.headlineSmall)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val selectedItemsParents = selectedIds.map { uuid ->
                            characters.find { it.uuid == uuid }?.folderUuid
                                ?: folders.find { it.uuid == uuid }?.parentFolderUuid
                        }.toSet()

                        val isAlreadyAtRoot = selectedItemsParents.size == 1 && selectedItemsParents.first() == null

                        Surface(
                            onClick = {
                                val selectedChars = selectedIds.filter { uuid -> characters.any { it.uuid == uuid } }
                                val selectedFolders = selectedIds.filter { uuid -> folders.any { it.uuid == uuid } }

                                if (selectedChars.isNotEmpty()) onMoveCharactersToFolder(selectedChars, null, null)
                                selectedFolders.forEach { onMoveFolderToFolder(it, null, null) }

                                selectedIds.clear()
                                showMoveToFolderSheet = false
                            },
                            enabled = !isAlreadyAtRoot,
                            shape = RoundedCornerShape(12.dp),
                            color = colorScheme.secondaryContainer.copy(alpha = if (isAlreadyAtRoot) 0.1f else 0.3f),
                            modifier = Modifier.fillMaxWidth().graphicsLayer {
                                alpha = if (isAlreadyAtRoot) 0.5f else 1f
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Home, null, tint = colorScheme.onSecondaryContainer.copy(alpha = if (isAlreadyAtRoot) 0.5f else 1f))
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    text = "В корень (без папки)",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colorScheme.onSurface.copy(alpha = if (isAlreadyAtRoot) 0.5f else 1f)
                                )
                            }
                        }

                        folders.forEach { folder ->
                            // Circular nesting prevention
                            val isCircularTarget = selectedIds.any { selectedUuid ->
                                if (selectedUuid == folder.uuid) return@any true
                                var curr: String? = folder.parentFolderUuid
                                while(curr != null) {
                                    if (curr == selectedUuid) return@any true
                                    curr = folders.find { it.uuid == curr }?.parentFolderUuid
                                }
                                false
                            }

                            val isAlreadyParent = selectedItemsParents.size == 1 && selectedItemsParents.first() == folder.uuid
                            val isEnabled = !isCircularTarget && !isAlreadyParent

                            Surface(
                                onClick = {
                                    val selectedChars = selectedIds.filter { uuid -> characters.any { it.uuid == uuid } }
                                    val selectedFolders = selectedIds.filter { uuid -> folders.any { it.uuid == uuid } }

                                    if (selectedChars.isNotEmpty()) onMoveCharactersToFolder(selectedChars, folder.uuid, null)
                                    selectedFolders.forEach { onMoveFolderToFolder(it, folder.uuid, null) }

                                    selectedIds.clear()
                                    showMoveToFolderSheet = false
                                },
                                enabled = isEnabled,
                                shape = RoundedCornerShape(12.dp),
                                color = colorScheme.surfaceVariant.copy(alpha = if (isEnabled) 0.5f else 0.1f),
                                modifier = Modifier.fillMaxWidth().graphicsLayer {
                                    alpha = if (isEnabled) 1f else 0.5f
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = colorScheme.primary.copy(alpha = if (isEnabled) 1f else 0.5f)
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    Text(
                                        text = folder.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = colorScheme.onSurface.copy(alpha = if (isEnabled) 1f else 0.5f)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showMoveToFolderSheet = false }) { Text("Отмена") }
                },
                shape = RoundedCornerShape(28.dp)
            )
        }
    }
}