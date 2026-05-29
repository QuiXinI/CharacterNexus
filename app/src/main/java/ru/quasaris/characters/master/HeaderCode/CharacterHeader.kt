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
import coil.compose.AsyncImage
import ru.quasaris.characters.master.ArmorClassEntry
import ru.quasaris.characters.master.InitiativeEntry
import ru.quasaris.characters.master.R
import ru.quasaris.characters.master.SpeedEntry
import androidx.compose.animation.AnimatedVisibility
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
    activeInitValue: String,
    onInitClick: () -> Unit,
    currentHp: String,
    maxHp: String,
    tempHp: String,
    healthColor: Color,
    healthIcon: Int,
    onHealthClick: () -> Unit,
    conditionsCount: String,
    onConditionsClick: () -> Unit,
    activeSpeedValue: String,
    onSpeedClick: () -> Unit,
    showAvatarMenu: Boolean,
    onDismissAvatarMenu: () -> Unit,
    onImagePickerClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.background(colorScheme.surface).statusBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) { Icon(Icons.Default.Menu, null, modifier = Modifier.size(32.dp), tint = colorScheme.onSurface) }
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
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(colorScheme.primaryContainer).clickable { onAvatarClick() }, contentAlignment = Alignment.Center) {
                    if (selectedImageUri != null) AsyncImage(model = selectedImageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else {
                        val bm = remember(characterImageData) {
                            if (characterImageData != null) {
                                try { val d = Base64.decode(characterImageData, Base64.DEFAULT); BitmapFactory.decodeByteArray(d, 0, d.size)?.asImageBitmap() } catch (e: Exception) { null }
                            } else null
                        }
                        if (bm != null) Image(bitmap = bm, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else Icon(Icons.Default.Person, null, tint = colorScheme.onPrimaryContainer)
                    }
                }
                DropdownMenu(expanded = showAvatarMenu, onDismissRequest = onDismissAvatarMenu) {
                    DropdownMenuItem(text = { Text("Выбор изображения") }, leadingIcon = { Icon(Icons.Default.Image, null) }, onClick = onImagePickerClick)
                    DropdownMenuItem(text = { Text("Скачать персонажа") }, leadingIcon = { Icon(Icons.Default.Download, null) }, onClick = onDownloadClick)
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).height(24.dp).shadow(2.dp, RoundedCornerShape(20.dp)).background(colorScheme.surface, RoundedCornerShape(20.dp)).padding(2.dp).clickable { onLevelClick() }) {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(90.dp).fillMaxHeight().clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)).background(colorScheme.primaryContainer), contentAlignment = Alignment.Center) { Text("$level уровень", fontSize = 11.sp, color = colorScheme.onPrimaryContainer) }
                val pr = remember(experience, nextLevelExp) { val c = experience.toFloatOrNull() ?: 0f; val n = nextLevelExp.toFloatOrNull() ?: 0f; if (n <= 0f) 1f else (c / n).coerceIn(0f, 1f) }
                Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)).background(colorScheme.surface)) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(pr).background(colorScheme.primaryContainer))
                    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.weight(0.4f)); Text("$experience | $nextLevelExp", fontSize = 11.sp, color = colorScheme.onSurface); Spacer(Modifier.weight(0.6f))
                        val nxt = (level.toIntOrNull() ?: 0) + 1; Text(if (nxt <= 20) "$nxt" else "", fontSize = 11.sp, color = colorScheme.onSurface)
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatIconBox(activeACValue, R.drawable.ic_shield, onClick = onACClick)
                StatIconBox(activeInitValue, R.drawable.ic_sword, onClick = onInitClick)
            }
            Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp).height(55.dp).border(1.5.dp, healthColor, RoundedCornerShape(8.dp)).background(colorScheme.surface, RoundedCornerShape(8.dp)).clickable { onHealthClick() }, contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(healthIcon), null, modifier = Modifier.size(32.dp), colorFilter = ColorFilter.tint(healthColor))
                    Spacer(Modifier.width(6.dp)); Text("$currentHp / ${maxHp.toIntOrNull() ?: 0}", color = healthColor, fontSize = 16.sp)
                    if ((tempHp.toIntOrNull() ?: 0) > 0) Text(" (+$tempHp)", color = healthColor.copy(alpha = 0.7f), fontSize = 14.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatIconBox(conditionsCount, R.drawable.ic_conditions, onClick = onConditionsClick)
                StatIconBox(activeSpeedValue, R.drawable.ic_speed, onClick = onSpeedClick)
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
    Column {
        Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Brush.verticalGradient(listOf(colorScheme.onSurface.copy(alpha = 0.15f), Color.Transparent))))
        AnimatedVisibility(isLevelPanelVisible, enter = expandVertically(), exit = shrinkVertically()) { LevelPanel(level, onLevelChange, experience, onExpChange, proficiencyBonus, onProfChange, nextLevelExp, statsMap) }
        AnimatedVisibility(isHealthPanelVisible, enter = expandVertically(), exit = shrinkVertically()) { HealthPanel(maxHp, onMaxHpChange, tempHp, onTempHpChange, currentHp, onCurrentHpChange, onHealClick, onDamageClick, onTempClick, healthColor, clampHp) }
        AnimatedVisibility(isArmorClassPanelVisible, enter = expandVertically(), exit = shrinkVertically()) { FormulaPanel("Класс Доспеха", armorClassEntries, activeArmorClassId, acDeleteConfirmId, { updated -> onArmorClassEntries(updated.filterIsInstance<ArmorClassEntry>()) }, onActiveArmorClass, onAcDeleteReq, onAddArmorClass) }
        AnimatedVisibility(isInitiativePanelVisible, enter = expandVertically(), exit = shrinkVertically()) { FormulaPanel("Инициатива", initiativeEntries, activeInitiativeId, initDeleteConfirmId, { updated -> onInitiativeEntries(updated.filterIsInstance<InitiativeEntry>()) }, onActiveInitiative, onInitDeleteReq, onAddInitiative) }
        AnimatedVisibility(isConditionsPanelVisible, enter = expandVertically(), exit = shrinkVertically()) { ConditionsPanel(allConditions, selectedConditions, onToggleCondition) }
        AnimatedVisibility(isSpeedPanelVisible, enter = expandVertically(), exit = shrinkVertically()) { FormulaPanel("Скорость", speedEntries, activeSpeedId, speedDeleteConfirmId, { updated -> onSpeedEntries(updated.filterIsInstance<SpeedEntry>()) }, onActiveSpeed, onSpeedDeleteReq, onAddSpeed) }
        
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).height(40.dp).clip(RoundedCornerShape(8.dp)).background(colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Характеристики", fontSize = 12.sp, color = colorScheme.onPrimaryContainer.copy(alpha = 0.7f), modifier = Modifier.weight(1f))
                Text("Характеристики", fontSize = 18.sp, color = colorScheme.onPrimaryContainer, modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center)
                Text("Характеристики", fontSize = 12.sp, color = colorScheme.onPrimaryContainer.copy(alpha = 0.7f), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }
        }
    }
}
