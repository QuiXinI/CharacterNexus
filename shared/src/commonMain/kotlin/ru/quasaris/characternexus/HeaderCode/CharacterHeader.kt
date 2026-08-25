package ru.quasaris.characternexus.HeaderCode

import ru.quasaris.characternexus.model.*
import androidx.compose.animation.*
import characternexus.shared.generated.resources.Res
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import ru.quasaris.characternexus.ui.outerShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.DrawableResource
import characternexus.shared.generated.resources.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.IntSize
import ru.quasaris.characternexus.util.HapticType
import ru.quasaris.characternexus.util.PlatformUtils
import coil3.compose.AsyncImage
import ru.quasaris.characternexus.*
import ru.quasaris.characternexus.ui.*
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import dev.chrisbanes.haze.HazeState
import ru.quasaris.characternexus.backend.ImageManager
import ru.quasaris.characternexus.backend.getPreviousLevelThreshold
import ru.quasaris.characternexus.util.log
import ru.quasaris.characternexus.platformFileSystem
import kotlinx.serialization.json.Json

@Composable
fun CharacterHeader(
    name: String,
    onNameChange: (String) -> Unit,
    level: String,
    experience: String,
    nextLevelExp: String,
    characterImageData: String?,
    characterUuid: String = "",
    onAvatarClick: () -> Unit,
    onLevelClick: () -> Unit,
    onOpenDrawer: () -> Unit,
    activeACValue: String,
    onACClick: () -> Unit,
    onACLongClick: () -> Unit,
    isShieldActive: Boolean,
    activeInitValue: String,
    onInitClick: () -> Unit,
    onInitLongClick: () -> Unit = {},
    currentHp: String,
    maxHp: String,
    tempHp: String,
    healthColor: Color,
    healthIcon: DrawableResource,
    onHealthClick: () -> Unit,
    conditionsCount: String,
    selectedConditions: List<String>,
    onConditionsClick: () -> Unit,
    activeSpeedValue: String,
    onSpeedClick: () -> Unit,
    showAvatarMenu: Boolean,
    onDismissAvatarMenu: () -> Unit,
    onImagePickerClick: () -> Unit,
    onExportSheetClick: () -> Unit,
    onExportPortraitClick: () -> Unit = {},
    onDeletePortraitClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onNavigateBack: () -> Unit,
    exhaustion: Int,
    hasInspiration: Boolean = false,
    onInspirationChange: (Boolean) -> Unit = {},
    onShortRest: () -> Unit = {},
    onLongRest: () -> Unit = {},
    onDawn: () -> Unit = {},
    showRestPopup: Boolean = false,
    onShowRestPopupChange: (Boolean) -> Unit = {},
    hazeState: HazeState? = null,
    blurPopups: Boolean = false,
    settingsViewModel: ru.quasaris.characternexus.backend.SettingsViewModel? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val veryResponsive by settingsViewModel?.veryResponsiveHaptics?.collectAsState() ?: remember { mutableStateOf(true) }

    val inspirationRotation by androidx.compose.animation.core.animateFloatAsState(if (hasInspiration) 0f else 45f)
    val inspirationScale by androidx.compose.animation.core.animateFloatAsState(if (hasInspiration) 1f else 0.8f)
    val isOled = colorScheme.background == Color.Black

    var totalDrag by remember { mutableStateOf(0f) }

    fun performClickHaptic() {
        if (veryResponsive) {
            PlatformUtils.performHapticFeedback(ru.quasaris.characternexus.util.HapticType.CLICK)
        }
    }

    val panelsSpringSpec = remember {
        spring<IntSize>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
    }

    Column(
        modifier = Modifier
            .background(colorScheme.surface)
            .statusBarsPadding()
            .animateContentSize(animationSpec = panelsSpringSpec)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount
                    },
                    onDragEnd = {
                        if (totalDrag > 150) {
                            PlatformUtils.performHapticFeedback(HapticType.LONG_PRESS)
                            onOpenDrawer()
                        }
                    }
                )
            }
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                performClickHaptic()
                onOpenDrawer()
            }) { Icon(Icons.Default.Menu, null, modifier = Modifier.size(32.dp), tint = colorScheme.onSurface) }
            
            Box(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = name, onValueChange = onNameChange,
                    textStyle = TextStyle(fontSize = 22.sp, textAlign = TextAlign.Start, color = colorScheme.onSurface, fontWeight = FontWeight.Bold),
                    cursorBrush = SolidColor(colorScheme.primary),
                    decorationBox = { innerTextField -> if (name.isEmpty()) Text("Имя персонажа", fontSize = 22.sp, textAlign = TextAlign.Start, color = colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()); innerTextField() },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Inspiration
                Box(contentAlignment = Alignment.Center) {
                    IconButton(
                        onClick = {
                            performClickHaptic()
                            onInspirationChange(!hasInspiration)
                        },
                        modifier = Modifier
                            .scale(inspirationScale)
                            .rotate(inspirationRotation)
                    ) {
                        if (hasInspiration && !isOled) {
                            Surface(
                                modifier = Modifier.size(24.dp).outerShadow(CircleShape, blur = 12.dp),
                                color = colorScheme.primary.copy(alpha = 0.3f),
                                shape = CircleShape,
                                shadowElevation = 0.dp,
                                tonalElevation = 8.dp
                            ) {}
                        }
                        Icon(
                            painter = painterResource(Res.drawable.ic_inspiration),
                            contentDescription = "Героическое вдохновение",
                            modifier = Modifier.size(32.dp),
                            tint = if (hasInspiration) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }

                // Rest
                Box(contentAlignment = Alignment.Center) {
                    IconButton(onClick = { onShowRestPopupChange(!showRestPopup) }) {
                        Icon(Icons.Default.WbSunny, null, modifier = Modifier.size(32.dp), tint = colorScheme.primary)
                    }

                    if (showRestPopup) {
                        RestPopup(
                            onShortRest = { onShortRest() },
                            onLongRest = { onLongRest() },
                            onDawn = { onDawn() },
                            onDismiss = { onShowRestPopupChange(false) },
                            hazeState = hazeState,
                            isOled = colorScheme.background == Color.Black
                        )
                    }
                }

                // Avatar
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable {
                                performClickHaptic()
                                onAvatarClick()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            val thumbFile = remember(characterImageData, characterUuid) {
                                if (characterImageData != null && characterImageData.length < 100) {
                                    ImageManager.getThumbnailFile(characterImageData, characterUuid)
                                } else null
                            }

                            val portraitFile = remember(characterImageData, characterUuid) {
                                if (characterImageData != null && characterImageData.length < 100) {
                                    ImageManager.getPortraitFile(characterImageData, characterUuid)
                                } else null
                            }

                            val imageFile = remember(thumbFile, portraitFile) {
                                if (thumbFile != null && platformFileSystem.exists(thumbFile)) thumbFile
                                else if (portraitFile != null && platformFileSystem.exists(portraitFile)) portraitFile
                                else null
                            }

                            if (imageFile != null) {
                                AsyncImage(
                                    model = imageFile,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.Person, null, tint = colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                    if (showAvatarMenu) {
                        AvatarPopup(
                            hasImage = characterImageData != null,
                            onSettingsClick = onSettingsClick,
                            onImagePickerClick = onImagePickerClick,
                            onExportSheetClick = onExportSheetClick,
                            onExportPortraitClick = onExportPortraitClick,
                            onDeleteClick = onDeletePortraitClick,
                            onDismiss = onDismissAvatarMenu,
                            hazeState = hazeState,
                            isOled = isOled
                        )
                    }
                }
            }
        }
        Box(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable { 
                performClickHaptic()
                onLevelClick() 
            }
        ) {
            val pr = remember(experience, nextLevelExp, level) {
                val currentXp = experience.toFloatOrNull() ?: 0f
                val nextXp = nextLevelExp.toFloatOrNull() ?: 0f
                val prevLevelXp = getPreviousLevelThreshold(level).toFloatOrNull() ?: 0f
                val totalNeededForLevel = nextXp - prevLevelXp
                val progressInLevel = currentXp - prevLevelXp
                if (totalNeededForLevel <= 0f) 1f else (progressInLevel / totalNeededForLevel).coerceIn(0f, 1f)
            }
            
            // Progress Fill
            Box(modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(pr)
                .background(colorScheme.primary.copy(alpha = 0.2f))
            )
            
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), 
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$level ${stringResource(Res.string.level_label)}",
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Black,
                    color = colorScheme.primary
                )
                
                val xpText = if ((level.toIntOrNull() ?: 0) >= 20) "$experience ${stringResource(Res.string.xp_label)}" else "$experience / $nextLevelExp ${stringResource(Res.string.xp_label)}"
                Text(
                    text = xpText,
                    fontSize = 11.sp, 
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurfaceVariant
                )
                
                val nxt = (level.toIntOrNull() ?: 0) + 1
                if ((level.toIntOrNull() ?: 0) < 20) {
                    Text(
                        text = "$nxt", 
                        fontSize = 12.sp, 
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.outline
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatIconBox(activeACValue,
                    Res.drawable.ic_shield, onClick = onACClick, onLongClick = onACLongClick, isHighlighted = isShieldActive, settingsViewModel = settingsViewModel)
                StatIconBox(activeInitValue, Res.drawable.ic_sword, onClick = onInitClick, onLongClick = onInitLongClick, isHighlighted = true, settingsViewModel = settingsViewModel)
            }
            Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp).height(55.dp).border(1.5.dp, healthColor, RoundedCornerShape(8.dp)).background(colorScheme.surface, RoundedCornerShape(8.dp)).clickable { 
                performClickHaptic()
                onHealthClick() 
            }, contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(healthIcon), null, modifier = Modifier.size(32.dp), colorFilter = ColorFilter.tint(healthColor))
                    Spacer(Modifier.width(6.dp)); Text("$currentHp / ${maxHp.toIntOrNull() ?: 0}", color = healthColor, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    if ((tempHp.toIntOrNull() ?: 0) > 0) Text(" (+$tempHp)", color = healthColor.copy(alpha = 0.7f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatIconBox(
                    value = if (conditionsCount == "0" || conditionsCount.isEmpty()) "" else conditionsCount,
                    iconRes = Res.drawable.ic_conditions,
                    onClick = onConditionsClick,
                    isHighlighted = selectedConditions.isNotEmpty(),
                    settingsViewModel = settingsViewModel
                )
                StatIconBox(activeSpeedValue, Res.drawable.ic_speed, onClick = onSpeedClick, isHighlighted = true, settingsViewModel = settingsViewModel)
            }
        }
    }
}

@Composable
fun ExpandingPanelsSection(
    isLevelPanelVisible: Boolean,
    level: String,
    onLevelChange: (String) -> Unit,
    experience: String,
    onExpChange: (String) -> Unit,
    proficiencyBonus: String,
    onProfChange: (String) -> Unit,
    nextLevelExp: String,
    statsMap: Map<String, String>,

    isHealthPanelVisible: Boolean,
    maxHp: String,
    onMaxHpChange: (String) -> Unit,
    tempHp: String,
    onTempHpChange: (String) -> Unit,
    currentHp: String,
    onCurrentHpChange: (String) -> Unit,
    onHealClick: () -> Unit,
    onDamageClick: () -> Unit,
    onTempClick: () -> Unit,
    healthColor: Color,
    clampHp: () -> Unit,
    hpPanelHitDice: List<HitDiceEntry>,
    onSpentHitDiceChange: (Int, Int) -> Unit,
    onOpenHealthSettings: () -> Unit,

    isRestPanelVisible: Boolean,
    onRestPanelDismiss: () -> Unit,
    onRestPanelHitDiceChange: (List<HitDiceEntry>) -> Unit,
    onHealAmount: (Int) -> Unit,
    onShortRestConfirmed: () -> Unit,
    onLongRest: () -> Unit,
    onDawn: () -> Unit,
    defaultHitDie: Int,

    isArmorClassPanelVisible: Boolean,
    armorClassEntries: List<ArmorClassEntry>,
    activeArmorClassId: String?,
    acDeleteConfirmId: String?,
    onArmorClassEntries: (List<ArmorClassEntry>) -> Unit,
    onActiveArmorClass: (String?) -> Unit,
    onAcDeleteReq: (String?) -> Unit,
    onAddArmorClass: () -> Unit,

    isInitiativePanelVisible: Boolean,
    initiativeEntries: List<InitiativeEntry>,
    activeInitiativeId: String?,
    initDeleteConfirmId: String?,
    onInitiativeEntries: (List<InitiativeEntry>) -> Unit,
    onActiveInitiative: (String?) -> Unit,
    onInitDeleteReq: (String?) -> Unit,
    onAddInitiative: () -> Unit,

    isConditionsPanelVisible: Boolean,
    allConditions: List<Condition>,
    selectedConditions: List<String>,
    onToggleCondition: (String) -> Unit,
    exhaustion: Int,
    onExhaustionChange: (Int) -> Unit,

    isShieldActive: Boolean,
    onShieldActiveChange: (Boolean) -> Unit,
    shieldEntries: List<ShieldEntry>,
    activeShieldId: String?,
    shieldDeleteConfirmId: String?,
    onShieldEntries: (List<ShieldEntry>) -> Unit,
    onActiveShield: (String?) -> Unit,
    onShieldDeleteReq: (String?) -> Unit,
    onAddShield: () -> Unit,

    isSpeedPanelVisible: Boolean,
    speedEntries: List<SpeedEntry>,
    activeSpeedId: String?,
    speedDeleteConfirmId: String?,
    onSpeedEntries: (List<SpeedEntry>) -> Unit,
    onActiveSpeed: (String?) -> Unit,
    onSpeedDeleteReq: (String?) -> Unit,
    onAddSpeed: () -> Unit
) {
    val panelsSpringSpec = remember {
        spring<IntSize>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
    }
    val floatSpring = remember {
        spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
    }

    val enterSpec = expandVertically(animationSpec = panelsSpringSpec) + fadeIn(animationSpec = floatSpring)
    val exitSpec = shrinkVertically(animationSpec = panelsSpringSpec) + fadeOut(animationSpec = floatSpring)

    Column(modifier = Modifier.fillMaxWidth()) {
        AnimatedVisibility(isHealthPanelVisible, enter = enterSpec, exit = exitSpec) {
            HealthPanel(
                maxHp, onMaxHpChange, tempHp, onTempHpChange, currentHp, onCurrentHpChange, 
                onHealClick, onDamageClick, onTempClick, healthColor, clampHp, hpPanelHitDice, 
                onSpentHitDiceChange, onOpenHealthSettings
            )
        }

        AnimatedVisibility(isRestPanelVisible, enter = enterSpec, exit = exitSpec) {
            ru.quasaris.characternexus.ui.RestPanel(
                hpPanelHitDice, onRestPanelHitDiceChange, onHealAmount, 
                onShortRestConfirmed, onRestPanelDismiss, statsMap, defaultHitDie
            )
        }

        AnimatedVisibility(isLevelPanelVisible, enter = enterSpec, exit = exitSpec) { 
            LevelPanel(level, onLevelChange, experience, onExpChange, proficiencyBonus, onProfChange, nextLevelExp, statsMap) 
        }

        AnimatedVisibility(isArmorClassPanelVisible, enter = enterSpec, exit = exitSpec) {
            Column {
                FormulaPanel("Класс Доспеха", armorClassEntries, activeArmorClassId, acDeleteConfirmId, { updated -> onArmorClassEntries(updated.filterIsInstance<ArmorClassEntry>()) }, onActiveArmorClass, onAcDeleteReq, onAddArmorClass) 
                FormulaPanel(
                    title = "Щит",
                    entries = shieldEntries,
                    activeId = activeShieldId,
                    deleteId = shieldDeleteConfirmId,
                    onEntries = { updated -> onShieldEntries(updated.filterIsInstance<ShieldEntry>()) },
                    onActive = onActiveShield,
                    onDeleteReq = onShieldDeleteReq,
                    onAdd = onAddShield,
                    headerTrailing = {
                        Switch(
                            checked = isShieldActive,
                            onCheckedChange = onShieldActiveChange,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                )
            }
        }
        AnimatedVisibility(isInitiativePanelVisible, enter = enterSpec, exit = exitSpec) { FormulaPanel("Инициатива", initiativeEntries, activeInitiativeId, initDeleteConfirmId, { updated -> onInitiativeEntries(updated.filterIsInstance<InitiativeEntry>()) }, onActiveInitiative, onInitDeleteReq, onAddInitiative) }
        AnimatedVisibility(isConditionsPanelVisible, enter = enterSpec, exit = exitSpec) { ConditionsPanel(allConditions, selectedConditions, onToggleCondition, exhaustion, onExhaustionChange) }
        AnimatedVisibility(isSpeedPanelVisible, enter = enterSpec, exit = exitSpec) { FormulaPanel("Скорость", speedEntries, activeSpeedId, speedDeleteConfirmId, { updated -> onSpeedEntries(updated.filterIsInstance<SpeedEntry>()) }, onActiveSpeed, onSpeedDeleteReq, onAddSpeed) }
    }
}


@Composable
fun rememberAllConditions(): List<Condition> {
    var allConditions by remember { mutableStateOf(emptyList<Condition>()) }
    LaunchedEffect(Unit) {
        try {
            val bytes = Res.readBytes("files/conditions.json")
            val content = bytes.decodeToString()
            allConditions = Json.decodeFromString<List<Condition>>(content)
        } catch (e: Exception) {
            e.log()
        }
    }
    return allConditions
}

fun toggleCondition(selectedConditions: List<String>, conditionName: String): List<String> {
    return if (selectedConditions.contains(conditionName)) {
        selectedConditions - conditionName
    } else {
        selectedConditions + conditionName
    }
}