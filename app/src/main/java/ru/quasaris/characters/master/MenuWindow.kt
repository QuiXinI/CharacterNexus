package ru.quasaris.characters.master

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.quasaris.characters.master.ui.theme.quasarisTheme
import ru.quasaris.characters.master.backend.ArchiveManager
import ru.quasaris.characters.master.backend.ImageManager
import ru.quasaris.characters.master.backend.SettingsViewModel
import android.graphics.BitmapFactory
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

import dev.chrisbanes.haze.HazeState
import ru.quasaris.characters.master.ui.cropper.AvatarCropperWindow

@Composable
fun MenuWindow(
    characters: List<Character>,
    onNavigateToCreate: () -> Unit,
    onCharacterClick: (Int) -> Unit,
    onImportCharacter: (Character) -> Unit,
    onDeleteCharacters: (List<Int>) -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel? = null,
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    blurPopups: Boolean = false
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    val selectedIds = remember { mutableStateListOf<Int>() }

    val autoDownload by settingsViewModel?.autoDownloadLssAvatar?.collectAsState() ?: remember { mutableStateOf(false) }
    val debugEnabled by settingsViewModel?.debugInfoEnabled?.collectAsState() ?: remember { mutableStateOf(false) }
    var lssAvatarToDownload by remember { mutableStateOf<Character?>(null) }
    var bitmapToCrop by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var lssCharacter by remember { mutableStateOf<Character?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val importedCharacter = ArchiveManager.importCharacter(context, uri)
            if (importedCharacter != null) {
                val charWithNewId = importedCharacter.copy(id = (0..100000).random())
                if (charWithNewId.avatarUrl != null && charWithNewId.imageData == null) {
                    if (autoDownload) {
                        downloadLssAvatar(context, charWithNewId, scope, debugEnabled) { bitmap ->
                            lssCharacter = charWithNewId
                            bitmapToCrop = bitmap
                        }
                    } else {
                        lssAvatarToDownload = charWithNewId
                    }
                } else {
                    onImportCharacter(charWithNewId)
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
                    downloadLssAvatar(context, char, scope, debugEnabled) { bitmap ->
                        lssCharacter = char
                        bitmapToCrop = bitmap
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
                        IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
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
                    items(characters) { character ->
                        val isSelected = character.id in selectedIds
                        CharacterCard(
                            character = character,
                            isSelected = isSelected,
                            onClick = {
                                if (selectedIds.isNotEmpty()) {
                                    if (isSelected) selectedIds.remove(character.id)
                                    else selectedIds.add(character.id)
                                } else {
                                    onCharacterClick(character.id)
                                }
                            },
                            onLongClick = {
                                if (character.id !in selectedIds) {
                                    selectedIds.add(character.id)
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

        if (bitmapToCrop != null && lssCharacter != null) {
            AvatarCropperWindow(
                imageToCrop = bitmapToCrop!!,
                hazeState = hazeState,
                forceBlurEnabled = blurPopups,
                onCropSuccess = { cropped ->
                    scope.launch {
                        val id = ImageManager.saveBitmapAsOriginal(context, bitmapToCrop!!)
                        ImageManager.saveCropped(context, id, cropped)

                        val portraitFile = ImageManager.getPortraitFile(context, id)
                        var seedColor: Int? = null
                        if (portraitFile.exists()) {
                            val bitmap = BitmapFactory.decodeFile(portraitFile.absolutePath)
                            if (bitmap != null) {
                                seedColor = PaletteHelper.extractSeedColor(bitmap)
                            }
                        }
                        onImportCharacter(
                            lssCharacter!!.copy(
                                imageData = id,
                                themeSeedColorArgb = seedColor
                            )
                        )
                        bitmapToCrop = null
                        lssCharacter = null
                    }
                },
                onCancel = {
                    onImportCharacter(lssCharacter!!)
                    bitmapToCrop = null
                    lssCharacter = null
                }
            )
        }
    }
}

private fun downloadLssAvatar(
    context: android.content.Context,
    character: Character,
    scope: kotlinx.coroutines.CoroutineScope,
    isDebugEnabled: Boolean = false,
    onSuccess: (android.graphics.Bitmap) -> Unit
) {
    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val url = character.avatarUrl ?: return@launch
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.apply {
                connectTimeout = 10000
                readTimeout = 15000
                setRequestProperty("User-Agent", "Mozilla/5.0")
                connect()
            }

            if (connection.responseCode == 200) {
                val inputStream = connection.inputStream
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onSuccess(bitmap)
                    }
                }
            } else {
                if (isDebugEnabled) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "LSS Avatar Download Failed: HTTP ${connection.responseCode}", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CharacterCard(
    character: Character,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    val thumbFile = remember(character.imageData) {
        character.imageData?.let { ImageManager.getThumbnailFile(context, it) }
    }

    val cardColor = if (isSelected) {
        colorScheme.primaryContainer
    } else {
        colorScheme.surfaceVariant
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) BorderStroke(2.dp, colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (thumbFile != null && thumbFile.exists()) {
                    AsyncImage(
                        model = thumbFile,
                        contentDescription = "Иконка",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(32.dp), tint = colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = character.name.ifEmpty { "Без имени" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Text(
                    text = "Уровень ${character.level} • ${character.characterClass}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MenuWindowPreview() {
    quasarisTheme {
        val previewCharacters = listOf(
            Character(1, "Гаррик", "Воин", "Человек"),
            Character(2, "Лиара", "Плут", "Эльф", imageData = null)
        )
        MenuWindow(
            characters = previewCharacters,
            onNavigateToCreate = {},
            onCharacterClick = {},
            onImportCharacter = {},
            onDeleteCharacters = {},
            onOpenDrawer = {}
        )
    }
}
