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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState
import sh.calvin.reorderable.ReorderableItem
import ru.quasaris.characternexus.util.decodeImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import ru.quasaris.characternexus.util.ImageProcessor
import ru.quasaris.characternexus.util.HapticType
import ru.quasaris.characternexus.util.PlatformUtils

import ru.quasaris.characternexus.ui.menu.FolderCard
import ru.quasaris.characternexus.ui.util.FolderColors
import ru.quasaris.characternexus.ui.util.PayWall
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape

sealed class MenuItem {
    abstract val key: String
    abstract val indentation: Int
    abstract val parentUuid: String?

    data class Character(
        val summary: CharacterSummary,
        override val indentation: Int = 0,
        override val parentUuid: String? = null
    ) : MenuItem() {
        override val key: String = summary.uuid
    }
    data class Folder(
        val folder: CharacterFolder,
        val count: Int,
        override val indentation: Int = 0,
        override val parentUuid: String? = null
    ) : MenuItem() {
        override val key: String = folder.uuid
    }
}

@Composable
fun MenuWindow(
    characters: List<CharacterSummary>,
    folders: List<CharacterFolder> = emptyList(),
    globalOrder: List<String> = emptyList(),
    onNavigateToCreate: () -> Unit,
    onCharacterClick: (String) -> Unit,
    onImportCharacter: (Character) -> Unit,
    onDeleteCharacters: (List<String>) -> Unit,
    onReorderCharacters: (List<String>) -> Unit,
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

    val autoDownload by settingsViewModel?.autoDownloadLssAvatar?.collectAsState() ?: remember { mutableStateOf(false) }
    val useOldAvatarStyle by settingsViewModel?.useOldAvatarStyle?.collectAsState() ?: remember { mutableStateOf(false) }
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

    var lastDraggingKey by remember { mutableStateOf<String?>(null) }

    val displayItems = remember(characters, folders, globalOrder, lastDraggingKey) {
        val result = mutableListOf<MenuItem>()
        
        // Find which folder is currently being dragged as a monolith
        val draggingFolderUuid = lastDraggingKey?.let { key ->
            if (folders.any { it.uuid == key }) key else null
        }

        fun getDescendantsRecursive(folderUuid: String, depth: Int = 0): List<MenuItem> {
            val res = mutableListOf<MenuItem>()
            val folder = folders.find { it.uuid == folderUuid } ?: return emptyList()
            
            // If this folder is the one being dragged, we HIDE its descendants from the main list
            // because they will be rendered inside the folder's dragging item.
            if (folderUuid == draggingFolderUuid) return emptyList()

            if (folder.isExpanded) {
                val childFolders = folders.filter { it.parentFolderUuid == folderUuid }
                val childChars = characters.filter { it.folderUuid == folderUuid }
                
                val childrenUuids = globalOrder.filter { uuid ->
                    childFolders.any { it.uuid == uuid } || childChars.any { it.uuid == uuid }
                }
                
                childrenUuids.forEach { uuid ->
                    val childFolder = childFolders.find { it.uuid == uuid }
                    if (childFolder != null) {
                        res.add(MenuItem.Folder(childFolder, characters.count { it.folderUuid == childFolder.uuid }, indentation = depth + 1, parentUuid = folderUuid))
                        res.addAll(getDescendantsRecursive(childFolder.uuid, depth + 1))
                    } else {
                        val childChar = childChars.find { it.uuid == uuid }
                        if (childChar != null) {
                            res.add(MenuItem.Character(childChar, indentation = depth + 1, parentUuid = folderUuid))
                        }
                    }
                }
            }
            return res
        }

        val rootItems = globalOrder.filter { uuid ->
            val isRootFolder = folders.any { it.uuid == uuid && it.parentFolderUuid == null }
            val isRootChar = characters.any { it.uuid == uuid && it.folderUuid == null }
            isRootFolder || isRootChar
        }
        
        fun getDescendantsForMonolith(folderUuid: String, depth: Int = 0): List<MenuItem> {
            val res = mutableListOf<MenuItem>()
            val childFolders = folders.filter { it.parentFolderUuid == folderUuid }
            val childChars = characters.filter { it.folderUuid == folderUuid }
            val childrenUuids = globalOrder.filter { uuid ->
                childFolders.any { it.uuid == uuid } || childChars.any { it.uuid == uuid }
            }
            childrenUuids.forEach { uuid ->
                val childFolder = childFolders.find { it.uuid == uuid }
                if (childFolder != null) {
                    res.add(MenuItem.Folder(childFolder, characters.count { it.folderUuid == childFolder.uuid }, indentation = depth + 1, parentUuid = folderUuid))
                    res.addAll(getDescendantsForMonolith(childFolder.uuid, depth + 1))
                } else {
                    val childChar = childChars.find { it.uuid == uuid }
                    if (childChar != null) {
                        res.add(MenuItem.Character(childChar, indentation = depth + 1, parentUuid = folderUuid))
                    }
                }
            }
            return res
        }

        rootItems.forEach { uuid ->
            val folder = folders.find { it.uuid == uuid }
            if (folder != null) {
                result.add(MenuItem.Folder(folder, characters.count { it.folderUuid == folder.uuid }, indentation = 0, parentUuid = null))
                result.addAll(getDescendantsRecursive(folder.uuid, depth = 0))
            } else {
                val char = characters.find { it.uuid == uuid }
                if (char != null) {
                    result.add(MenuItem.Character(char, indentation = 0, parentUuid = null))
                }
            }
        }
        result
    }
    
    // Expose a way to get descendants for monolithic rendering
    fun getFullFolderMonolith(folderUuid: String): List<MenuItem> {
        val folder = folders.find { it.uuid == folderUuid } ?: return emptyList()
        val descendants = mutableListOf<MenuItem>()
        
        fun collect(fUuid: String, depth: Int) {
            val childFolders = folders.filter { it.parentFolderUuid == fUuid }
            val childChars = characters.filter { it.folderUuid == fUuid }
            val childrenUuids = globalOrder.filter { uuid ->
                childFolders.any { it.uuid == uuid } || childChars.any { it.uuid == uuid }
            }
            childrenUuids.forEach { uuid ->
                val childFolder = childFolders.find { it.uuid == uuid }
                if (childFolder != null) {
                    descendants.add(MenuItem.Folder(childFolder, characters.count { it.folderUuid == childFolder.uuid }, indentation = depth + 1, parentUuid = fUuid))
                    collect(childFolder.uuid, depth + 1)
                } else {
                    val childChar = childChars.find { it.uuid == uuid }
                    if (childChar != null) {
                        descendants.add(MenuItem.Character(childChar, indentation = depth + 1, parentUuid = fUuid))
                    }
                }
            }
        }
        collect(folderUuid, 0)
        return descendants
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
                    Text("Премиум цвета", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant)
                    
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
        val density = LocalDensity.current
        val listState = rememberLazyListState()
        
        var horizontalDisplacement by remember { mutableStateOf(0f) }

        val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
            val visibleItems = displayItems.toList()
            val fromItem = visibleItems[from.index]
            
            // 1. Identify the block to move
            val blockToMoveUuids = mutableListOf<String>()
            blockToMoveUuids.add(fromItem.key)
            
            if (fromItem is MenuItem.Folder) {
                var i = from.index + 1
                while (i < visibleItems.size && visibleItems[i].indentation > fromItem.indentation) {
                    blockToMoveUuids.add(visibleItems[i].key)
                    i++
                }
            }
            
            // Atomic move in the visible list for consistent UI feedback
            val currentVisibleUuids = visibleItems.map { it.key }.toMutableList()
            
            // Remove whole block
            currentVisibleUuids.removeAll { it in blockToMoveUuids }
            
            // Calculate target index
            val targetItemKey = visibleItems[to.index].key
            val newTargetIndex = currentVisibleUuids.indexOf(targetItemKey)
            val targetIndex = if (to.index > from.index) newTargetIndex + 1 else newTargetIndex

            currentVisibleUuids.addAll(targetIndex.coerceIn(0, currentVisibleUuids.size), blockToMoveUuids)
            
            onReorderCharacters(currentVisibleUuids)
        }

        val pointerModifier = Modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.first()
                    if (reorderableState.isAnyItemDragging) {
                        horizontalDisplacement += (change.position.x - change.previousPosition.x)
                    } else {
                        horizontalDisplacement = 0f
                    }
                }
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize().then(pointerModifier),
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
            
            var lastDraggingKey by remember { mutableStateOf<String?>(null) }
            var currentDraggingIndex by remember { mutableStateOf<Int?>(null) }

            LaunchedEffect(reorderableState.isAnyItemDragging) {
                if (!reorderableState.isAnyItemDragging) {
                    currentDraggingIndex = null
                    if (lastDraggingKey != null) {
                        val draggedUuid = lastDraggingKey!!
                        val draggedItem = displayItems.find { it.key == draggedUuid }
                        val draggedIndex = displayItems.indexOfFirst { it.key == draggedUuid }
                        
                        if (draggedItem != null && draggedIndex != -1) {
                            val threshold = with(density) { 24.dp.toPx() }
                            val prevItem = if (draggedIndex > 0) displayItems[draggedIndex - 1] else null
                            
                            var newParentUuid: String? = draggedItem.parentUuid
                            var afterUuid: String? = null
                            var moved = false

                            if (horizontalDisplacement > threshold) {
                                if (prevItem is MenuItem.Folder) {
                                    newParentUuid = prevItem.key
                                    moved = true
                                }
                            } else if (horizontalDisplacement < -threshold) {
                                if (draggedItem.indentation > 0) {
                                    val currentParent = folders.find { it.uuid == draggedItem.parentUuid }
                                    newParentUuid = currentParent?.parentFolderUuid
                                    afterUuid = currentParent?.uuid
                                    moved = true
                                }
                            } else {
                                // Adopt parent from position (neighbor context)
                                val potentialParent = when (prevItem) {
                                    is MenuItem.Folder -> prevItem.key
                                    is MenuItem.Character -> prevItem.parentUuid
                                    null -> null
                                }
                                if (potentialParent != draggedItem.parentUuid) {
                                    newParentUuid = potentialParent
                                    moved = true
                                }
                            }

                            if (moved && newParentUuid != draggedUuid) {
                                val isChar = characters.any { it.uuid == draggedUuid }
                                if (isChar) {
                                    onMoveCharactersToFolder(listOf(draggedUuid), newParentUuid, afterUuid)
                                } else {
                                    onMoveFolderToFolder(draggedUuid, newParentUuid, afterUuid)
                                }
                            }
                        }
                        lastDraggingKey = null
                        horizontalDisplacement = 0f
                    }
                }
            }

            // Clear dragging state
            LaunchedEffect(reorderableState.isAnyItemDragging) {
                if (!reorderableState.isAnyItemDragging) {
                    currentDraggingIndex = null
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(displayItems, key = { _, item -> item.key }) { index, item ->
                    ReorderableItem(reorderableState, item.key) { isDragging ->
                        if (isDragging) {
                            lastDraggingKey = item.key
                            SideEffect { currentDraggingIndex = index }
                        }
                        
                        // We need to know if SOME item is dragging to show drop zones
                        val isAnyItemDragging = reorderableState.isAnyItemDragging
                        
                        val dragModifier = if (selectedIds.isNotEmpty()) {
                            Modifier.draggableHandle()
                        } else {
                            Modifier
                        }
                        
                        val isSameGroupAsPrev = index > 0 && displayItems[index - 1].parentUuid == item.parentUuid
                        val isSameGroupAsNext = index < displayItems.size - 1 && displayItems[index + 1].parentUuid == item.parentUuid
                        
                        val groupShape = when {
                            !isSameGroupAsPrev && !isSameGroupAsNext -> RoundedCornerShape(12.dp)
                            !isSameGroupAsPrev -> RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                            !isSameGroupAsNext -> RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                            else -> RoundedCornerShape(0.dp)
                        }

                        when (item) {
                            is MenuItem.Character -> {
                                val character = item.summary
                                val isSelected = character.uuid in selectedIds
                                
                                val isExitHovered = if (isDragging) {
                                    horizontalDisplacement < with(density) { -20.dp.toPx() } && item.indentation > 0
                                } else false
                                
                                val isNestHovered = if (isDragging) {
                                    val prevItem = if (index > 0) displayItems[index - 1] else null
                                    horizontalDisplacement > with(density) { 20.dp.toPx() } && prevItem is MenuItem.Folder
                                } else false

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = ((item.indentation + (if (isNestHovered) 1 else if (isExitHovered) -1 else 0)) * 16).coerceAtLeast(0).dp)
                                        .drawBehind {
                                            val effectiveIndentation = (item.indentation + (if (isNestHovered) 1 else if (isExitHovered) -1 else 0)).coerceAtLeast(0)
                                            if (effectiveIndentation > 0) {
                                                // Draw hierarchical guide line
                                                val lineX = (-8).dp.toPx()
                                                drawLine(
                                                    color = colorScheme.primary.copy(alpha = if (isExitHovered || isNestHovered) 0.5f else 0.2f),
                                                    start = Offset(lineX, 0f),
                                                    end = Offset(lineX, this.size.height),
                                                    strokeWidth = 2.dp.toPx()
                                                )
                                                
                                                // Horizontal connector
                                                drawLine(
                                                    color = colorScheme.primary.copy(alpha = if (isExitHovered || isNestHovered) 0.5f else 0.2f),
                                                    start = Offset(lineX, this.size.height / 2),
                                                    end = Offset(0f, this.size.height / 2),
                                                    strokeWidth = 2.dp.toPx()
                                                )
                                            }
                                        }
                                        .background(
                                            if (item.indentation > 0 || isNestHovered) colorScheme.primary.copy(alpha = if (isExitHovered || isNestHovered) 0.08f else 0.02f)
                                            else androidx.compose.ui.graphics.Color.Transparent,
                                            groupShape
                                        ),
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

                                    val folderColor = remember(character.folderUuid, folders) {
                                        folders.find { it.uuid == character.folderUuid }?.colorArgb
                                    }

                                    CharacterCard(
                                        character = character,
                                        isSelected = isSelected,
                                        useOldAvatarStyle = useOldAvatarStyle,
                                        folderColorArgb = folderColor,
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
                            is MenuItem.Folder -> {
                                val isExitHovered = if (isDragging) {
                                    horizontalDisplacement < with(density) { -20.dp.toPx() } && item.indentation > 0
                                } else false
                                
                                val isNestHovered = if (isDragging) {
                                    val prevItem = if (index > 0) displayItems[index - 1] else null
                                    horizontalDisplacement > with(density) { 20.dp.toPx() } && prevItem is MenuItem.Folder
                                } else false

                                val monolithDescendants = if (isDragging) getFullFolderMonolith(item.folder.uuid) else emptyList()

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = ((item.indentation + (if (isNestHovered) 1 else if (isExitHovered) -1 else 0)) * 16).coerceAtLeast(0).dp)
                                        .drawBehind {
                                            val effectiveIndentation = (item.indentation + (if (isNestHovered) 1 else if (isExitHovered) -1 else 0)).coerceAtLeast(0)
                                            if (effectiveIndentation > 0) {
                                                val lineX = (-8).dp.toPx()
                                                drawLine(
                                                    color = colorScheme.primary.copy(alpha = if (isExitHovered || isNestHovered) 0.5f else 0.2f),
                                                    start = Offset(lineX, 0f),
                                                    end = Offset(lineX, this.size.height),
                                                    strokeWidth = 2.dp.toPx()
                                                )
                                                
                                                drawLine(
                                                    color = colorScheme.primary.copy(alpha = if (isExitHovered || isNestHovered) 0.5f else 0.2f),
                                                    start = Offset(lineX, this.size.height / 2),
                                                    end = Offset(0f, this.size.height / 2),
                                                    strokeWidth = 2.dp.toPx()
                                                )
                                            }
                                        }
                                        .background(
                                            if (item.indentation > 0 || isNestHovered) colorScheme.primary.copy(alpha = if (isExitHovered || isNestHovered) 0.08f else 0.02f)
                                            else androidx.compose.ui.graphics.Color.Transparent,
                                            groupShape
                                        )
                                ) {
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

                                        val isHovered = if (isAnyItemDragging && !isDragging) {
                                            val draggingIdx = currentDraggingIndex
                                            draggingIdx != null && draggingIdx == index + 1 && horizontalDisplacement > with(density) { 20.dp.toPx() }
                                        } else false

                                        FolderCard(
                                            folder = item.folder,
                                            characterCount = item.count,
                                            isSelected = item.folder.uuid in selectedIds,
                                            isDropZoneHovered = isHovered,
                                            onToggleExpand = { onToggleFolderExpansion(item.folder.uuid) },
                                            modifier = Modifier.weight(1f)
                                                .graphicsLayer {
                                                    alpha = if (isDragging) 0.5f else 1f
                                                },
                                            onClick = {
                                                performClickHaptic()
                                                if (selectedIds.isNotEmpty()) {
                                                    if (item.folder.uuid in selectedIds) selectedIds.remove(item.folder.uuid)
                                                    else selectedIds.add(item.folder.uuid)
                                                } else {
                                                    onToggleFolderExpansion(item.folder.uuid)
                                                }
                                            },
                                            onLongClick = {
                                                PlatformUtils.performHapticFeedback(HapticType.LONG_PRESS)
                                                if (item.folder.uuid !in selectedIds) {
                                                    selectedIds.add(item.folder.uuid)
                                                }
                                                folderToManage = null
                                            },
                                            onMoreClick = {
                                                folderToManage = item.folder
                                            }
                                        )
                                    }
                                    
                                    // Monolithic rendering: show children inside the dragged folder
                                    if (isDragging && monolithDescendants.isNotEmpty()) {
                                        Column(
                                            modifier = Modifier.padding(top = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            monolithDescendants.forEach { child ->
                                                when (child) {
                                                    is MenuItem.Character -> {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(start = (child.indentation * 16).dp)
                                                        ) {
                                                            CharacterCard(
                                                                character = child.summary,
                                                                isSelected = false,
                                                                useOldAvatarStyle = useOldAvatarStyle,
                                                                modifier = Modifier.weight(1f).graphicsLayer { alpha = 0.7f },
                                                                onClick = {},
                                                                onLongClick = {}
                                                            )
                                                        }
                                                    }
                                                    is MenuItem.Folder -> {
                                                        // Simplify sub-folders in monolith if needed, or recursive render
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(start = (child.indentation * 16).dp)
                                                        ) {
                                                            FolderCard(
                                                                folder = child.folder,
                                                                characterCount = child.count,
                                                                isSelected = false,
                                                                modifier = Modifier.weight(1f).graphicsLayer { alpha = 0.7f },
                                                                onClick = {},
                                                                onLongClick = {}
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
                    }
                }
            }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
