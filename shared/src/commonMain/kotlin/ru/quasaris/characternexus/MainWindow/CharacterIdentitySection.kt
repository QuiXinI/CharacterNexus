package ru.quasaris.characternexus.MainWindow

import androidx.compose.runtime.Composable
import ru.quasaris.characternexus.HeaderCode.CharacterHeader
import ru.quasaris.characternexus.backend.ArchiveManager
import org.jetbrains.compose.resources.DrawableResource

@Composable
fun CharacterIdentitySection(
    name: String,
    onNameChange: (String) -> Unit,
    level: String,
    experience: String,
    nextLevelExp: String,
    characterImageData: String?,
    characterUuid: String = "",
    showAvatarMenu: Boolean,
    onAvatarClick: () -> Unit,
    onDismissAvatarMenu: () -> Unit,
    onLevelClick: () -> Unit,
    onNavigateBack: () -> Unit,
    onOpenDrawer: () -> Unit,
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
    healthIcon: DrawableResource,
    onHealthClick: () -> Unit,
    conditionsCount: String,
    selectedConditions: List<String>,
    onConditionsClick: () -> Unit,
    exhaustion: Int,
    activeSpeedValue: String,
    onSpeedClick: () -> Unit,
    onImagePickerClick: () -> Unit,
    onExportSheetClick: () -> Unit,
    onExportPortraitClick: () -> Unit = {},
    onShortRest: () -> Unit = {},
    onLongRest: () -> Unit = {},
    onDawn: () -> Unit = {},
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    blurPopups: Boolean = false
) {
    CharacterHeader(
        name = name,
        onNameChange = onNameChange,
        level = level,
        experience = experience,
        nextLevelExp = nextLevelExp,
        characterImageData = characterImageData,
        characterUuid = characterUuid,
        onAvatarClick = onAvatarClick,
        onLevelClick = onLevelClick,
        onNavigateBack = onNavigateBack,
        onOpenDrawer = onOpenDrawer,
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
            onImagePickerClick()
        },
        onExportSheetClick = {
            onDismissAvatarMenu()
            onExportSheetClick()
        },
        onExportPortraitClick = {
            onDismissAvatarMenu()
            onExportPortraitClick()
        },
        onShortRest = onShortRest,
        onLongRest = onLongRest,
        onDawn = onDawn,
        hazeState = hazeState,
        blurPopups = blurPopups
    )
}
