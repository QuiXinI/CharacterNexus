package ru.quasaris.characternexus.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.backend.ImageManager
import ru.quasaris.characternexus.backend.SettingsViewModel
import dev.chrisbanes.haze.HazeState
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import ru.quasaris.characternexus.util.PlatformUtils
import ru.quasaris.characternexus.util.log
import sh.calvin.reorderable.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BioTab(
    character: Character,
    onCharacterChange: (Character) -> Unit,
    onAvatarEditRequest: () -> Unit = {},
    onExportPortraitClick: () -> Unit = {},
    hazeState: HazeState? = null,
    popupHazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    blurPopups: Boolean = false,
    isEditMode: Boolean = false,
    onToggleEditMode: () -> Unit = {},
    onToggleAllExpansion: () -> Unit = {},
    anyCollapsed: Boolean = false,
    settingsViewModel: SettingsViewModel? = null,
    statsMap: Map<String, String> = emptyMap(),
    onFullscreenDialogOpenChange: (Boolean) -> Unit = {},
    onFullscreenVisibilityChanged: (Boolean) -> Unit = {},
    state: ru.quasaris.characternexus.ui.CharacterDetailState? = null,
    isDesktop: Boolean = false,
    header: @Composable () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    var shortFields by remember(character.bioShortFields) { mutableStateOf(character.bioShortFields) }
    var longSections by remember(character.bioLongSections) { mutableStateOf(character.bioLongSections) }
    var imageData by remember(character.imageData) { mutableStateOf(character.imageData) }

    var showPortraitMenu by remember { mutableStateOf(false) }
    var editingShortField by remember { mutableStateOf<BioShortField?>(null) }
    var newShortFieldTitle by remember { mutableStateOf("") }

    val portraitPath = remember(imageData, character.uuid) {
        if (imageData != null) {
            ImageManager.getPortraitFile(imageData!!, character.uuid)
        } else null
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
        popupHazeState = popupHazeState,
        forceBlurEnabled = forceBlurEnabled,
        blurPopups = blurPopups,
        isEditMode = isEditMode,
        onToggleEditMode = onToggleEditMode,
        onToggleAllExpansion = onToggleAllExpansion,
        anyCollapsed = anyCollapsed,
        collapseOnEdit = null,
        onFullscreenDialogOpenChange = onFullscreenDialogOpenChange,
        onFullscreenVisibilityChanged = onFullscreenVisibilityChanged,
        addButtonText = "ДОБАВИТЬ ОСОБОЕ ПОЛЕ",
        emptyListText = "Список разделов пуст",
        titlePlaceholder = "Название раздела",
        contentPlaceholder = "Текст раздела...",
        settingsViewModel = settingsViewModel,
        statsMap = statsMap,
        isDesktop = isDesktop,
        state = state,
        header = {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val containerWidth = maxWidth
                val portraitWidth = maxOf(260.dp, minOf(containerWidth * 0.35f, 600.dp))
                val isWideLayout = containerWidth >= 520.dp && (containerWidth - portraitWidth >= 240.dp)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 0.dp, end = 8.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    header()
                    Spacer(Modifier.height(0.dp))

                    if (isWideLayout) {
                        val portraitWidth = maxOf(260.dp, minOf(containerWidth * 0.35f, 600.dp))

                        val allRows = mutableListOf<List<BioShortField>>()
                        var currentRow = mutableListOf<BioShortField>()
                        var currentRowWidth = 0f

                        shortFields.forEach { field ->
                            if (currentRowWidth + field.widthRatio > 1.001f && currentRow.isNotEmpty()) {
                                allRows.add(currentRow)
                                currentRow = mutableListOf(field)
                                currentRowWidth = field.widthRatio
                            } else {
                                currentRow.add(field)
                                currentRowWidth += field.widthRatio
                            }
                        }
                        if (currentRow.isNotEmpty()) {
                            allRows.add(currentRow)
                        }

                        val maxBesideRows = 4
                        val besideRows = allRows.take(maxBesideRows)
                        val overflowFields = allRows.drop(maxBesideRows).flatten()

                        val fullWidthRows = mutableListOf<List<BioShortField>>()
                        var currentFullRow = mutableListOf<BioShortField>()
                        var currentFullRowWidth = 0f

                        overflowFields.forEach { field ->
                            val fullRatio = if (field.widthRatio >= 0.5f) 0.25f else 0.1666f
                            if (currentFullRowWidth + fullRatio > 1.001f && currentFullRow.isNotEmpty()) {
                                fullWidthRows.add(currentFullRow)
                                currentFullRow = mutableListOf(field)
                                currentFullRowWidth = fullRatio
                            } else {
                                currentFullRow.add(field)
                                currentFullRowWidth += fullRatio
                            }
                        }
                        if (currentFullRow.isNotEmpty()) {
                            fullWidthRows.add(currentFullRow)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            PortraitSection(
                                portraitPath = portraitPath,
                                imageData = imageData,
                                showPortraitMenu = showPortraitMenu,
                                onTogglePortraitMenu = { showPortraitMenu = !showPortraitMenu },
                                onAvatarEditRequest = {
                                    showPortraitMenu = false
                                    onAvatarEditRequest()
                                },
                                onExportPortraitClick = {
                                    showPortraitMenu = false
                                    onExportPortraitClick()
                                },
                                onDeletePortrait = {
                                    showPortraitMenu = false
                                    imageData = null
                                    saveChanges()
                                },
                                colorScheme = colorScheme,
                                modifier = Modifier.width(portraitWidth)
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                besideRows.forEach { rowFields ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        rowFields.forEach { field ->
                                            BioShortFieldItem(
                                                field = field,
                                                isEditMode = isEditMode,
                                                onValueChange = { newVal ->
                                                    shortFields = shortFields.map { if (it.id == field.id) it.copy(value = newVal) else it }
                                                    saveChanges()
                                                },
                                                onRenameRequest = {
                                                    editingShortField = field
                                                    newShortFieldTitle = field.title
                                                },
                                                onChangeRatio = {
                                                    val newRatio = if (field.widthRatio >= 0.5f) 0.33f else 0.5f
                                                    shortFields = shortFields.map { if (it.id == field.id) it.copy(widthRatio = newRatio) else it }
                                                    saveChanges()
                                                },
                                                onDelete = {
                                                    shortFields = shortFields.filter { it.id != field.id }
                                                    saveChanges()
                                                },
                                                colorScheme = colorScheme,
                                                modifier = Modifier.weight(field.widthRatio)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (fullWidthRows.isNotEmpty() || (besideRows.isEmpty() && isEditMode) || (fullWidthRows.isEmpty() && besideRows.isNotEmpty() && isEditMode)) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                fullWidthRows.forEach { rowFields ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        rowFields.forEach { field ->
                                            val fullRatio = if (field.widthRatio >= 0.5f) 0.25f else 0.1666f
                                            BioShortFieldItem(
                                                field = field,
                                                isEditMode = isEditMode,
                                                onValueChange = { newVal ->
                                                    shortFields = shortFields.map { if (it.id == field.id) it.copy(value = newVal) else it }
                                                    saveChanges()
                                                },
                                                onRenameRequest = {
                                                    editingShortField = field
                                                    newShortFieldTitle = field.title
                                                },
                                                onChangeRatio = {
                                                    val newRatio = if (field.widthRatio >= 0.5f) 0.33f else 0.5f
                                                    shortFields = shortFields.map { if (it.id == field.id) it.copy(widthRatio = newRatio) else it }
                                                    saveChanges()
                                                },
                                                onDelete = {
                                                    shortFields = shortFields.filter { it.id != field.id }
                                                    saveChanges()
                                                },
                                                colorScheme = colorScheme,
                                                modifier = Modifier.weight(fullRatio)
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
                        }
                    } else {
                        val mobilePortraitWidth = minOf(containerWidth - 32.dp, 280.dp)
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            PortraitSection(
                                portraitPath = portraitPath,
                                imageData = imageData,
                                showPortraitMenu = showPortraitMenu,
                                onTogglePortraitMenu = { showPortraitMenu = !showPortraitMenu },
                                onAvatarEditRequest = {
                                    showPortraitMenu = false
                                    onAvatarEditRequest()
                                },
                                onExportPortraitClick = {
                                    showPortraitMenu = false
                                    onExportPortraitClick()
                                },
                                onDeletePortrait = {
                                    showPortraitMenu = false
                                    imageData = null
                                    saveChanges()
                                },
                                colorScheme = colorScheme,
                                modifier = Modifier.width(mobilePortraitWidth)
                            )
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
                                        BioShortFieldItem(
                                            field = field,
                                            isEditMode = isEditMode,
                                            onValueChange = { newVal ->
                                                shortFields = shortFields.map { if (it.id == field.id) it.copy(value = newVal) else it }
                                                saveChanges()
                                            },
                                            onRenameRequest = {
                                                editingShortField = field
                                                newShortFieldTitle = field.title
                                            },
                                            onChangeRatio = {
                                                val newRatio = if (field.widthRatio >= 0.5f) 0.33f else 0.5f
                                                shortFields = shortFields.map { if (it.id == field.id) it.copy(widthRatio = newRatio) else it }
                                                saveChanges()
                                            },
                                            onDelete = {
                                                shortFields = shortFields.filter { it.id != field.id }
                                                saveChanges()
                                            },
                                            colorScheme = colorScheme,
                                            modifier = Modifier.weight(field.widthRatio)
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
                    }

                    HorizontalDivider(color = colorScheme.outlineVariant, thickness = 1.dp)
                }
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
private fun PortraitSection(
    portraitPath: Any?,
    imageData: String?,
    showPortraitMenu: Boolean,
    onTogglePortraitMenu: () -> Unit,
    onAvatarEditRequest: () -> Unit,
    onExportPortraitClick: () -> Unit,
    onDeletePortrait: () -> Unit,
    colorScheme: ColorScheme,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(colorScheme.surfaceVariant)
                .clickable { onTogglePortraitMenu() },
            contentAlignment = Alignment.Center
        ) {
            if (portraitPath != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalPlatformContext.current)
                        .data(portraitPath)
                        .memoryCacheKey("${portraitPath}_$imageData")
                        .crossfade(true)
                        .build(),
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
                color = colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                                onExportPortraitClick()
                            }
                        )
                        PortraitMenuItem(
                            icon = Icons.Default.PhotoCamera,
                            text = "Заменить портрет",
                            onClick = { onAvatarEditRequest() }
                        )
                        PortraitMenuItem(
                            icon = Icons.Default.Delete,
                            text = "Удалить портрет",
                            contentColor = colorScheme.error,
                            onClick = { onDeletePortrait() }
                        )
                    } else {
                        PortraitMenuItem(
                            icon = Icons.Default.AddAPhoto,
                            text = "Добавить портрет",
                            onClick = { onAvatarEditRequest() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BioShortFieldItem(
    field: BioShortField,
    isEditMode: Boolean,
    onValueChange: (String) -> Unit,
    onRenameRequest: () -> Unit,
    onChangeRatio: () -> Unit,
    onDelete: () -> Unit,
    colorScheme: ColorScheme,
    dragModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = field.value,
        onValueChange = onValueChange,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isEditMode) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Перетащить",
                        modifier = Modifier
                            .size(18.dp)
                            .then(dragModifier),
                        tint = colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(field.title)
                if (isEditMode) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Переименовать",
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { onRenameRequest() },
                        tint = colorScheme.primary
                    )
                }
            }
        },
        modifier = modifier.heightIn(min = 60.dp),
        shape = RoundedCornerShape(12.dp),
        trailingIcon = if (isEditMode) {
            {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.padding(end = 2.dp)
                ) {
                    IconButton(
                        onClick = onChangeRatio,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text(
                            text = if (field.widthRatio >= 0.5f) "1/2" else "1/3",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDelete,
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
