package ru.quasaris.characters.master.tabs

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.quasaris.characters.master.BioShortField
import ru.quasaris.characters.master.Character
import ru.quasaris.characters.master.backend.ImageManager
import ru.quasaris.characters.master.backend.SettingsViewModel
import dev.chrisbanes.haze.HazeState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BioTab(
    character: Character,
    onCharacterChange: (Character) -> Unit,
    onAvatarEditRequest: () -> Unit = {},
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    blurPopups: Boolean = false,
    isEditMode: Boolean = false,
    settingsViewModel: SettingsViewModel? = null,
    statsMap: Map<String, String> = emptyMap(),
    header: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    var shortFields by remember(character.bioShortFields) { mutableStateOf(character.bioShortFields) }
    var longSections by remember(character.bioLongSections) { mutableStateOf(character.bioLongSections) }
    var imageData by remember(character.imageData) { mutableStateOf(character.imageData) }

    var showPortraitMenu by remember { mutableStateOf(false) }
    var editingShortField by remember { mutableStateOf<BioShortField?>(null) }
    var newShortFieldTitle by remember { mutableStateOf("") }

    var portraitBitmap by remember(imageData) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(imageData) {
        if (imageData != null) {
            withContext(Dispatchers.IO) {
                val file = ImageManager.getPortraitFile(context, imageData!!)
                if (file.exists()) {
                    portraitBitmap = BitmapFactory.decodeFile(file.absolutePath)
                } else {
                    portraitBitmap = null
                }
            }
        } else {
            portraitBitmap = null
        }
    }

    val saveChanges = {
        onCharacterChange(
            character.copy(
                bioShortFields = shortFields,
                bioLongSections = longSections,
                imageData = imageData
            )
        )
    }

    DynamicFieldsTab(
        fields = longSections,
        onFieldsChange = { updated ->
            longSections = updated
            saveChanges()
        },
        hazeState = hazeState,
        forceBlurEnabled = forceBlurEnabled,
        blurPopups = blurPopups,
        isEditMode = isEditMode,
        addButtonText = "ДОБАВИТЬ ОСОБОЕ ПОЛЕ",
        emptyListText = "Список разделов пуст",
        titlePlaceholder = "Название раздела",
        contentPlaceholder = "Текст раздела...",
        settingsViewModel = settingsViewModel,
        statsMap = statsMap,
        header = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 0.dp, end = 8.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                header()
                Spacer(Modifier.height(12.dp))
                // 1. Full-width square portrait container
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colorScheme.surfaceVariant)
                            .clickable { showPortraitMenu = !showPortraitMenu },
                        contentAlignment = Alignment.Center
                    ) {
                        if (portraitBitmap != null) {
                            Image(
                                bitmap = portraitBitmap!!.asImageBitmap(),
                                contentDescription = "Портрет персонажа",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Добавить портрет",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Portrait Actions Dropdown (immediately below portrait)
                    AnimatedVisibility(
                        visible = showPortraitMenu,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(4.dp),
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                if (imageData != null) {
                                    PortraitMenuItem(
                                    icon = Icons.Default.SaveAlt,
                                    text = "Экспортировать",
                                    onClick = {
                                        showPortraitMenu = false
                                        if (portraitBitmap != null) {
                                            scope.launch {
                                                val format = settingsViewModel?.exportFormat?.value ?: ru.quasaris.characters.master.backend.ExportFormat.WEBP
                                                val directoryUri = settingsViewModel?.exportDirectoryUri?.value
                                                
                                                val encodedData = ru.quasaris.characters.master.backend.ImageExporter.encodeImage(
                                                    portraitBitmap!!, 
                                                    format
                                                )
                                                
                                                val success = ru.quasaris.characters.master.backend.ImageExporter.saveToDirectory(
                                                    context = context,
                                                    data = encodedData,
                                                    fileName = "portrait_${character.name}_${System.currentTimeMillis()}",
                                                    format = format,
                                                    directoryUri = directoryUri
                                                )
                                                
                                                if (success) {
                                                    Toast.makeText(context, "Аватар экспортирован успешно", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Ошибка при экспорте", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                )
                                PortraitMenuItem(
                                        icon = Icons.Default.PhotoCamera,
                                        text = "Заменить портрет",
                                        onClick = {
                                            showPortraitMenu = false
                                            onAvatarEditRequest()
                                        }
                                    )
                                    PortraitMenuItem(
                                        icon = Icons.Default.Delete,
                                        text = "Удалить портрет",
                                        contentColor = colorScheme.error,
                                        onClick = {
                                            showPortraitMenu = false
                                            imageData = null
                                            saveChanges()
                                        }
                                    )
                                } else {
                                    PortraitMenuItem(
                                        icon = Icons.Default.AddAPhoto,
                                        text = "Добавить портрет",
                                        onClick = {
                                            showPortraitMenu = false
                                            onAvatarEditRequest()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Short text fields
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Group into rows or render with flex/grid.
                    val rows = mutableListOf<List<BioShortField>>()
                    var currentRow = mutableListOf<BioShortField>()
                    var currentRowWidth = 0f

                    shortFields.forEach { field ->
                        if (currentRowWidth + field.widthRatio > 1.001f && currentRow.isNotEmpty()) {
                            rows.add(currentRow)
                            currentRow = mutableListOf(field)
                            currentRowWidth = field.widthRatio
                        } else {
                            currentRow.add(field)
                            currentRowWidth += field.widthRatio
                        }
                    }
                    if (currentRow.isNotEmpty()) {
                        rows.add(currentRow)
                    }

                    rows.forEach { rowFields ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rowFields.forEach { field ->
                                val weight = field.widthRatio
                                OutlinedTextField(
                                    value = field.value,
                                    onValueChange = { newVal ->
                                        shortFields = shortFields.map { if (it.id == field.id) it.copy(value = newVal) else it }
                                        saveChanges()
                                    },
                                    label = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(field.title)
                                            if (isEditMode) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Переименовать",
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clickable {
                                                            editingShortField = field
                                                            newShortFieldTitle = field.title
                                                        },
                                                    tint = colorScheme.primary
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(weight)
                                        .heightIn(min = 60.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    trailingIcon = if (isEditMode) {
                                        {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.End,
                                                modifier = Modifier.padding(end = 2.dp)
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        val newRatio = if (field.widthRatio >= 0.5f) 0.33f else 0.5f
                                                        shortFields = shortFields.map { if (it.id == field.id) it.copy(widthRatio = newRatio) else it }
                                                        saveChanges()
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Text(
                                                        text = if (field.widthRatio >= 0.5f) "1/2" else "1/3",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = colorScheme.primary
                                                    )
                                                }
                                                // All fields are now deletable
                                                IconButton(
                                                    onClick = {
                                                        shortFields = shortFields.filter { it.id != field.id }
                                                        saveChanges()
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Удалить",
                                                        modifier = Modifier.size(14.dp),
                                                        tint = colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    } else null,
                                    singleLine = true
                                )
                            }
                        }
                    }

                    if (isEditMode) {
                        Button(
                            onClick = {
                                val newField = BioShortField(
                                    title = "Новое поле",
                                    widthRatio = 0.5f,
                                    isCustom = true
                                )
                                shortFields = shortFields + newField
                                saveChanges()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Добавить особое поле")
                        }
                    }
                }

                HorizontalDivider(color = colorScheme.outlineVariant, thickness = 1.dp)
            }
        }
    )

    // Rename short field dialog
    if (editingShortField != null) {
        AlertDialog(
            onDismissRequest = { editingShortField = null },
            title = { Text("Изменить название поля") },
            text = {
                OutlinedTextField(
                    value = newShortFieldTitle,
                    onValueChange = { newShortFieldTitle = it },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val targetId = editingShortField!!.id
                        shortFields = shortFields.map { if (it.id == targetId) it.copy(title = newShortFieldTitle) else it }
                        saveChanges()
                        editingShortField = null
                    }
                ) {
                    Text("ОК")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingShortField = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun PortraitMenuItem(
    icon: ImageVector,
    text: String,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = contentColor.copy(alpha = 0.8f)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}
