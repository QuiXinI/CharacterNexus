package ru.quasaris.characters.master.HeaderCode

import android.net.Uri
import android.util.Base64
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import coil.compose.AsyncImage
import ru.quasaris.characters.master.R
import ru.quasaris.characters.master.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically

@Composable
fun CharacterHeader(
    name: String,
    onNameChange: (String) -> Unit,
    level: String,
    experience: String,
    nextLevelExp: String,
    selectedImageUri: Uri?,
    characterImageData: String?,
    onAvatarClick: () -> Unit,
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
    healthColor: Color,
    healthIcon: Int,
    onHealthClick: () -> Unit,
    conditionsCount: String,
    selectedConditions: List<String>,
    onConditionsClick: () -> Unit,
    exhaustion: Int,
    activeSpeedValue: String,
    onSpeedClick: () -> Unit,
    showAvatarMenu: Boolean,
    onDismissAvatarMenu: () -> Unit,
    onImagePickerClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    
    Column(modifier = Modifier.background(colorScheme.surface).statusBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onNavigateBack()
            }) { Icon(Icons.Default.Menu, null, modifier = Modifier.size(32.dp), tint = colorScheme.onSurface) }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                BasicTextField(
                    value = name, onValueChange = onNameChange,
                    textStyle = TextStyle(fontSize = 22.sp, textAlign = TextAlign.Center, color = colorScheme.onSurface, fontWeight = FontWeight.Normal),
                    cursorBrush = SolidColor(colorScheme.primary),
                    decorationBox = { innerTextField -> if (name.isEmpty()) Text("Имя персонажа", fontSize = 22.sp, textAlign = TextAlign.Center, color = colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()); innerTextField() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Box(contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(colorScheme.primaryContainer).clickable { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAvatarClick() 
                }, contentAlignment = Alignment.Center) {
                    val context = LocalContext.current
                    val portraitFile = remember(characterImageData) {
                        characterImageData?.let { ImageManager.getThumbnailFile(context, it) }
                    }
                    if (portraitFile != null && portraitFile.exists()) {
                        AsyncImage(model = portraitFile, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.Person, null, tint = colorScheme.onPrimaryContainer)
                    }
                }
                DropdownMenu(expanded = showAvatarMenu, onDismissRequest = onDismissAvatarMenu) {
                    DropdownMenuItem(text = { Text("Выбор изображения") }, leadingIcon = { Icon(Icons.Default.Image, null) }, onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onImagePickerClick()
                    })
                    DropdownMenuItem(text = { Text("Скачать персонажа") }, leadingIcon = { Icon(Icons.Default.Download, null) }, onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDownloadClick()
                    })
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
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                    text = "$level Уровень",
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Black,
                    color = colorScheme.primary
                )
                
                Text(
                    text = "$experience / $nextLevelExp Опыта",
                    fontSize = 11.sp, 
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurfaceVariant
                )
                
                val nxt = (level.toIntOrNull() ?: 0) + 1
                Text(
                    text = "$nxt", 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.outline
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatIconBox(activeACValue,
                    R.drawable.ic_shield, onClick = onACClick, onLongClick = onACLongClick, isHighlighted = isShieldActive)
                StatIconBox(activeInitValue, R.drawable.ic_sword, onClick = onInitClick, isHighlighted = true)
            }
            Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp).height(55.dp).border(1.5.dp, healthColor, RoundedCornerShape(8.dp)).background(colorScheme.surface, RoundedCornerShape(8.dp)).clickable { 
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onHealthClick() 
            }, contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(healthIcon), null, modifier = Modifier.size(32.dp), colorFilter = ColorFilter.tint(healthColor))
                    Spacer(Modifier.width(6.dp)); Text("$currentHp / ${maxHp.toIntOrNull() ?: 0}", color = healthColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    if ((tempHp.toIntOrNull() ?: 0) > 0) Text(" (+$tempHp)", color = healthColor.copy(alpha = 0.7f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatIconBox(
                    value = if (conditionsCount == "0" || conditionsCount.isEmpty()) "" else conditionsCount,
                    iconRes = R.drawable.ic_conditions,
                    onClick = onConditionsClick,
                    isHighlighted = selectedConditions.isNotEmpty()
                )
                StatIconBox(activeSpeedValue, R.drawable.ic_speed, onClick = onSpeedClick, isHighlighted = true)
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
    val colorScheme = MaterialTheme.colorScheme
    val animationSpec = spring<IntSize>(stiffness = Spring.StiffnessMedium)
    
    Column {
        Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Brush.verticalGradient(listOf(colorScheme.onSurface.copy(alpha = 0.15f), Color.Transparent))))
        AnimatedVisibility(isLevelPanelVisible, enter = expandVertically(animationSpec), exit = shrinkVertically(animationSpec)) { LevelPanel(level, onLevelChange, experience, onExpChange, proficiencyBonus, onProfChange, nextLevelExp, statsMap) }
        AnimatedVisibility(isHealthPanelVisible, enter = expandVertically(animationSpec), exit = shrinkVertically(animationSpec)) { HealthPanel(maxHp, onMaxHpChange, tempHp, onTempHpChange, currentHp, onCurrentHpChange, onHealClick, onDamageClick, onTempClick, healthColor, clampHp) }
        AnimatedVisibility(isArmorClassPanelVisible, enter = expandVertically(animationSpec), exit = shrinkVertically(animationSpec)) { 
            Column(modifier = Modifier.animateContentSize(animationSpec)) {
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
        AnimatedVisibility(isInitiativePanelVisible, enter = expandVertically(animationSpec), exit = shrinkVertically(animationSpec)) { FormulaPanel("Инициатива", initiativeEntries, activeInitiativeId, initDeleteConfirmId, { updated -> onInitiativeEntries(updated.filterIsInstance<InitiativeEntry>()) }, onActiveInitiative, onInitDeleteReq, onAddInitiative) }
        AnimatedVisibility(isConditionsPanelVisible, enter = expandVertically(animationSpec), exit = shrinkVertically(animationSpec)) { ConditionsPanel(allConditions, selectedConditions, onToggleCondition, exhaustion, onExhaustionChange) }
        AnimatedVisibility(isSpeedPanelVisible, enter = expandVertically(animationSpec), exit = shrinkVertically(animationSpec)) { FormulaPanel("Скорость", speedEntries, activeSpeedId, speedDeleteConfirmId, { updated -> onSpeedEntries(updated.filterIsInstance<SpeedEntry>()) }, onActiveSpeed, onSpeedDeleteReq, onAddSpeed) }
        
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).height(40.dp).clip(RoundedCornerShape(8.dp)).background(colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Характеристики", fontSize = 12.sp, color = colorScheme.onPrimaryContainer.copy(alpha = 0.7f), modifier = Modifier.weight(1f))
                Text("Характеристики", fontSize = 18.sp, color = colorScheme.onPrimaryContainer, modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center)
                Text("Характеристики", fontSize = 12.sp, color = colorScheme.onPrimaryContainer.copy(alpha = 0.7f), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }
        }
    }
}

