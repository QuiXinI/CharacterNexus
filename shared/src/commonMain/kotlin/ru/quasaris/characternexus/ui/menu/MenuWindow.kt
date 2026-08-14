package ru.quasaris.characternexus.ui.menu

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch
import ru.quasaris.characternexus.backend.ArchiveManager
import ru.quasaris.characternexus.backend.ImageManager
import ru.quasaris.characternexus.backend.LssAvatarService
import ru.quasaris.characternexus.model.Character
import ru.quasaris.characternexus.model.CharacterSummary

@Composable
fun MenuWindow(
    characters: List<CharacterSummary>,
    onNavigateToCreate: () -> Unit,
    onCharacterClick: (String) -> Unit,
    onImportCharacter: (Character) -> Unit,
    onDeleteCharacters: (List<String>) -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    settingsViewModel: Any? = null,
    hazeState: HazeState? = null,
    popupHazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    blurPopups: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    val selectedIds = remember { mutableStateListOf<String>() }

    // Mock settings for now
    val autoDownload = false
    val useOldAvatarStyle = false
    
    var lssAvatarToDownload by remember { mutableStateOf<Character?>(null) }
    var showFilePicker by remember { mutableStateOf(false) }

    FilePickerWrapper(show = showFilePicker, fileExtensions = listOf("charbook", "json")) { bytes ->
        showFilePicker = false
        if (bytes == null) return@FilePickerWrapper
        
        scope.launch {
            val importedCharacters = ArchiveManager.importCharacters(bytes)
            
            for (importedCharacter in importedCharacters) {
                // Ensure a fresh random ID to avoid local conflicts, matching legacy behavior
                val charToImport = importedCharacter.copy(id = (0..1000000).random())
                
                if (charToImport.avatarUrl != null && charToImport.imageData == null) {
                    if (autoDownload) {
                        val avatarBytes = LssAvatarService.downloadAvatar(charToImport)
                        var finalChar = charToImport
                        if (avatarBytes != null) {
                             val imageId = ImageManager.saveImportedAvatar(charToImport.uuid, avatarBytes, null)
                             finalChar = charToImport.copy(imageData = imageId)
                        }
                        onImportCharacter(finalChar)
                    } else {
                        lssAvatarToDownload = charToImport
                    }
                } else {
                    onImportCharacter(charToImport)
                }
            }
        }
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
                        var finalChar = char
                        if (avatarBytes != null) {
                            val imageId = ImageManager.saveImportedAvatar(char.uuid, avatarBytes, null)
                            finalChar = char.copy(imageData = imageId)
                        }
                        onImportCharacter(finalChar)
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
                        IconButton(onClick = onOpenDrawer) {
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
                        IconButton(onClick = { showFilePicker = true }) {
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
                                if (selectedIds.isNotEmpty()) {
                                    if (isSelected) selectedIds.remove(character.uuid)
                                    else selectedIds.add(character.uuid)
                                } else {
                                    onCharacterClick(character.uuid)
                                }
                            },
                            onLongClick = {
                                if (character.uuid !in selectedIds) {
                                    selectedIds.add(character.uuid)
                                }
                            }
                        )
                    }
                }

                Button(
                    onClick = onNavigateToCreate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(4.dp, RoundedCornerShape(12.dp)),
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
                    onDeleteCharacters(selectedIds.toList())
                    selectedIds.clear()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(6.dp, RoundedCornerShape(12.dp)),
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
