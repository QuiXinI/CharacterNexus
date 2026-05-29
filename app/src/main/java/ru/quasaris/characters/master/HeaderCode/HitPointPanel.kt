package ru.quasaris.characters.master.HeaderCode

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.border

@Composable
fun HealthPanel(
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
    onFocusLost: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).shadow(4.dp, RoundedCornerShape(12.dp)).background(colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).border(1.dp, colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).animateContentSize()) {
        Text("Хиты", modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally), style = MaterialTheme.typography.titleMedium, color = healthColor)
        HealthRow("Максимум Хитов", maxHp, { s -> onMaxHpChange(minOf(999, s.toIntOrNull() ?: 0).toString()) }, onFocusLost)
        HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.15f))
        HealthRow("Текущие Хиты", currentHp, { s -> onCurrentHpChange(minOf(999, s.toIntOrNull() ?: 0).toString()) }, onFocusLost)
        HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.15f))
        HealthRow("Временные Хиты", tempHp, { s -> onTempHpChange(minOf(9999, s.toIntOrNull() ?: 0).toString()) })
        Spacer(Modifier.height(12.dp))
        HealthActionRow("Лечение", Color(0xFF00C46F), onHealClick)
        HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.15f))
        HealthActionRow("Получение урона", Color(0xFFE57373), onDamageClick)
        HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.15f))
        HealthActionRow("Укрепление", Color(0xFF64B5F6), onTempClick)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun HealthRow(label: String, value: String, onValueChange: (String) -> Unit, onFocusLost: () -> Unit = {}) {
    val colorScheme = MaterialTheme.colorScheme
    var tv by remember { mutableStateOf(TextFieldValue(value)) }
    val fr = remember { FocusRequester() }
    LaunchedEffect(value) { if (tv.text != value) tv = tv.copy(text = value, selection = TextRange(value.length)) }
    Row(modifier = Modifier.fillMaxWidth().height(48.dp).clickable { tv = tv.copy(selection = TextRange(value.length)); fr.requestFocus() }, verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.padding(start = 16.dp).weight(1f), fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
        BasicTextField(value = tv, onValueChange = { textFieldValue -> tv = textFieldValue; onValueChange(textFieldValue.text.filter { c -> c.isDigit() || c == '-' }) }, textStyle = TextStyle(textAlign = TextAlign.End, fontSize = 16.sp, color = colorScheme.onSurface, fontWeight = FontWeight.Bold), modifier = Modifier.width(100.dp).padding(end = 16.dp).focusRequester(fr).onFocusChanged { state -> if (!state.isFocused) onFocusLost() }, cursorBrush = SolidColor(colorScheme.primary), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
    }
}

@Composable
fun HealthActionRow(text: String, color: Color, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(48.dp).clickable { onClick() }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) { Text(text, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = color) }
}
