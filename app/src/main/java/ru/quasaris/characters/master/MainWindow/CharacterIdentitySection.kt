package ru.quasaris.characters.master.MainWindow

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.runtime.Composable
import ru.quasaris.characters.master.HeaderCode.CharacterHeader

@Composable
fun CharacterIdentitySection(
    name: String,
    onNameChange: (String) -> Unit,
    level: String,
    experience: String,
    nextLevelExp: String,
    selectedImageUri: Uri?,
    characterImageData: String?,
    showAvatarMenu: Boolean,
    onAvatarClick: () -> Unit,
    onDismissAvatarMenu: () -> Unit,
    onLevelClick: () -> Unit,
    onNavigateBack: () -> Unit,
    activeACValue: String,
    onACClick: () -> Unit,
    activeInitValue: String,
    onInitClick: () -> Unit,
    currentHp: String,
    maxHp: String,
    tempHp: String,
    healthColor: androidx.compose.ui.graphics.Color,
    healthIcon: Int,
    onHealthClick: () -> Unit,
    conditionsCount: String,
    onConditionsClick: () -> Unit,
    activeSpeedValue: String,
    onSpeedClick: () -> Unit,
    imagePicker: ManagedActivityResultLauncher<String, Uri?>,
    onDownloadClick: (String) -> Unit
) {
    CharacterHeader(
        name = name,
        onNameChange = onNameChange,
        level = level,
        experience = experience,
        nextLevelExp = nextLevelExp,
        selectedImageUri = selectedImageUri,
        characterImageData = characterImageData,
        onAvatarClick = onAvatarClick,
        onLevelClick = onLevelClick,
        onNavigateBack = onNavigateBack,
        activeACValue = activeACValue,
        onACClick = onACClick,
        activeInitValue = activeInitValue,
        onInitClick = onInitClick,
        currentHp = currentHp,
        maxHp = maxHp,
        tempHp = tempHp,
        healthColor = healthColor,
        healthIcon = healthIcon,
        onHealthClick = onHealthClick,
        conditionsCount = conditionsCount,
        onConditionsClick = onConditionsClick,
        activeSpeedValue = activeSpeedValue,
        onSpeedClick = onSpeedClick,
        showAvatarMenu = showAvatarMenu,
        onDismissAvatarMenu = onDismissAvatarMenu,
        onImagePickerClick = { 
            onDismissAvatarMenu()
            imagePicker.launch("image/*") 
        },
        onDownloadClick = {
            onDismissAvatarMenu()
            onDownloadClick("${if (name.isEmpty()) "character" else name}.json")
        }
    )
}
