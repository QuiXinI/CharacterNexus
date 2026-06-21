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
    onACLongClick: () -> Unit,
    isShieldActive: Boolean,
    activeInitValue: String,
    onInitClick: () -> Unit,
    currentHp: String,
    maxHp: String,
    tempHp: String,
    healthColor: androidx.compose.ui.graphics.Color,
    healthIcon: Int,
    onHealthClick: () -> Unit,
    conditionsCount: String,
    selectedConditions: List<String>,
    onConditionsClick: () -> Unit,
    exhaustion: Int,
    activeSpeedValue: String,
    onSpeedClick: () -> Unit,
    imagePicker: androidx.activity.result.ActivityResultLauncher<String>,
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
        onACLongClick = onACLongClick,
        isShieldActive = isShieldActive,
        activeInitValue = activeInitValue,
        onInitClick = onInitClick,
        currentHp = currentHp,
        maxHp = maxHp,
        tempHp = tempHp,
        healthColor = healthColor,
        healthIcon = healthIcon,
        onHealthClick = onHealthClick,
        conditionsCount = conditionsCount,
        selectedConditions = selectedConditions,
        onConditionsClick = onConditionsClick,
        exhaustion = exhaustion,
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
            onDownloadClick("${name.ifEmpty { "character" }}.lsskiller")
        }
    )
}
