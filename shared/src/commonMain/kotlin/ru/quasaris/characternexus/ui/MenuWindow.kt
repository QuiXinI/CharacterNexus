package ru.quasaris.characternexus.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    
    var pendingImportResult by remember { mutableStateOf<ru.quasaris.characternexus.backend.ImportResult?>(null) }
    var imageToCrop by remember { mutableStateOf<ImageBitmap?>(null) }

    val isAnyFullscreenDialogOpen = imageToCrop != null || lssAvatarToDownload != null || importErrorMessage != null
    LaunchedEffect(isAnyFullscreenDialogOpen) {
        onFullscreenDialogOpenChange(isAnyFullscreenDialogOpen)
    }

    CommonFilePicker(show = showFilePicker, fileExtensions = listOf("charbook", "lsskiller", "json")) { file ->
        showFilePicker = false
        if (file == null) return@CommonFilePicker
        
        scope.launch {
            try {
                val bytes = file.readBytes()
                val result = ArchiveManager.importCharacter(bytes)
                
                if (result != null) {
                    val importedCharacter = result.character
                    val portraitBytes = result.portraitBytes ?: result.originalBytes
                    
                    if (portraitBytes != null) {
                        // Image present, go to cropper
                        try {
                            imageToCrop = decodeImageBitmap(portraitBytes)
                            pendingImportResult = result
                        } catch (e: Exception) {
                            // If decoding fails, just import without image
                            onImportCharacter(importedCharacter)
                        }
                    } else if (importedCharacter.avatarUrl != null && importedCharacter.imageData == null) {
                        // LSS Avatar handling
                        if (autoDownload) {
                            val avatarBytes = LssAvatarService.downloadAvatar(importedCharacter)
                            if (avatarBytes != null) {
                                try {
                                    imageToCrop = decodeImageBitmap(avatarBytes)
                                    pendingImportResult = ImportResult(
                                        character = importedCharacter,
                                        portraitBytes = avatarBytes,
                                        originalBytes = avatarBytes
                                    )
                                } catch (e: Exception) {
                                    onImportCharacter(importedCharacter)
                                }
                            } else {
                                onImportCharacter(importedCharacter)
                            }
                        } else {
                            lssAvatarToDownload = importedCharacter
                        }
                    } else {
                        onImportCharacter(importedCharacter)
                    }
                } else {
                    importErrorMessage = "Не удалось распознать файл. Пожалуйста, выберите другой файл персонажа."
                }
            } catch (e: Exception) {
                importErrorMessage = "Ошибка при чтении файла: ${e.message}"
            }
        }
    }

    if (imageToCrop != null && pendingImportResult != null) {
        val result = pendingImportResult!!
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
                    pendingImportResult = null
                }
            },
            onDismiss = {
                onImportCharacter(result.character.copy(imageData = null))
                imageToCrop = null
                pendingImportResult = null
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
                                pendingImportResult = ImportResult(
                                    character = char,
                                    portraitBytes = avatarBytes,
                                    originalBytes = avatarBytes
                                )
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
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(characters, key = { it.uuid }) { character ->
                        val isSelected = character.uuid in selectedIds
                        CharacterCard(
                            character = character,
                            isSelected = isSelected,
                            useOldAvatarStyle = useOldAvatarStyle,
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
            Button(
                onClick = {
                    PlatformUtils.performHapticFeedback(HapticType.ERROR)
                    onDeleteCharacters(selectedIds.toList())
                    selectedIds.clear()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .outerShadow(RoundedCornerShape(12.dp), blur = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.error,
                    contentColor = colorScheme.onError
                )
            ) {
                Row(
                    modifier = Modifier.wrapContentWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Удалить",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    val buttonText = if (selectedIds.size > 1) {
                        "Удалить персонажей (${selectedIds.size})"
                    } else {
                        "Удалить персонажа"
                    }

                    Text(
                        text = buttonText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
