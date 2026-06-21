package ru.quasaris.characters.master.HeaderCode

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LevelPanel(
    level: String,
    onLevelChange: (String) -> Unit,
    exp: String,
    onExpChange: (String) -> Unit,
    prof: String,
    onProfChange: (String) -> Unit,
    nextExp: String,
    stats: Map<String, String>
) {
    val colorScheme = MaterialTheme.colorScheme
    var ltv by remember { mutableStateOf(TextFieldValue(level)) }
    var etv by remember { mutableStateOf(TextFieldValue(exp)) }
    var ptv by remember { mutableStateOf(TextFieldValue(prof)) }
    var isPFocused by remember { mutableStateOf(false) }

    LaunchedEffect(level) { if (ltv.text != level) ltv = ltv.copy(text = level, selection = TextRange(level.length)) }
    LaunchedEffect(exp) { if (etv.text != exp) etv = etv.copy(text = exp, selection = TextRange(exp.length)) }
    LaunchedEffect(prof, isPFocused, stats) {
        val d = if (isPFocused) prof else evaluateFormula(prof, stats).toString()
        if (ptv.text != d) ptv = TextFieldValue(text = d, selection = if (isPFocused) TextRange(d.length) else TextRange.Zero)
    }

    val fl = remember { FocusRequester() }
    val fe = remember { FocusRequester() }
    val fp = remember { FocusRequester() }
    val targetLvl = calculateLevelFromExperience(exp)
    val canUp = targetLvl != (level.toIntOrNull() ?: 1)

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).shadow(4.dp, RoundedCornerShape(12.dp)).background(colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).border(1.dp, colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).animateContentSize()) {
        Text("Уровень и Опыт", modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally), style = MaterialTheme.typography.titleMedium, color = colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.fillMaxWidth().height(48.dp).clickable { ltv = ltv.copy(selection = TextRange(level.length)); fl.requestFocus() }, verticalAlignment = Alignment.CenterVertically) {
            Text("Уровень персонажа", modifier = Modifier.padding(start = 16.dp).weight(1f), fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
            BasicTextField(value = ltv, onValueChange = { textFieldValue -> 
                ltv = textFieldValue
                val f = textFieldValue.text.filter { it.isDigit() }
                if (f.isEmpty()) {
                    onLevelChange("")
                } else {
                    val n = f.toIntOrNull()
                    if (n != null && n in 0..100) {
                        onLevelChange(n.toString())
                        val milestone = if (n >= 20) 355000 else getPreviousLevelThreshold(n.toString()).toIntOrNull() ?: 0
                        val currentExp = exp.toIntOrNull() ?: 0
                        if (n <= 20) {
                             onExpChange(milestone.toString())
                        } else if (currentExp < 355000) {
                             onExpChange("355000")
                        }
                    }
                }
            }, textStyle = TextStyle(textAlign = TextAlign.End, fontSize = 16.sp, color = colorScheme.onSurface, fontWeight = FontWeight.Bold), modifier = Modifier.width(100.dp).padding(end = 16.dp).focusRequester(fl).onFocusChanged { state -> if (!state.isFocused) { if (level.isEmpty()) onLevelChange("0") } }, cursorBrush = SolidColor(colorScheme.primary), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
        HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.15f))
        Row(modifier = Modifier.fillMaxWidth().height(48.dp).clickable { etv = etv.copy(selection = TextRange(exp.length)); fe.requestFocus() }, verticalAlignment = Alignment.CenterVertically) {
            Text("Текущий опыт", modifier = Modifier.padding(start = 16.dp).weight(1f), fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
            BasicTextField(value = etv, onValueChange = { textFieldValue -> etv = textFieldValue; onExpChange(textFieldValue.text.filter { it.isDigit() }) }, textStyle = TextStyle(textAlign = TextAlign.End, fontSize = 16.sp, color = colorScheme.onSurface, fontWeight = FontWeight.Bold), modifier = Modifier.width(100.dp).padding(end = 4.dp).focusRequester(fe).onFocusChanged { state -> if (!state.isFocused) { if (exp.isEmpty()) onExpChange("0") } }, cursorBrush = SolidColor(colorScheme.primary), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Text("/ $nextExp", modifier = Modifier.padding(end = 16.dp), fontSize = 14.sp, color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
        HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.15f))
        Row(modifier = Modifier.fillMaxWidth().height(48.dp).clickable { fp.requestFocus() }, verticalAlignment = Alignment.CenterVertically) {
            Text("Бонус Мастерства", modifier = Modifier.padding(start = 16.dp).weight(1f), fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!isPFocused && evaluateFormula(prof, stats) >= 0) Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                BasicTextField(value = ptv, onValueChange = { textFieldValue -> if (isPFocused) { ptv = textFieldValue; onProfChange(textFieldValue.text) } }, textStyle = TextStyle(textAlign = if (isPFocused) TextAlign.Start else TextAlign.End, fontSize = 16.sp, color = colorScheme.onSurface, fontWeight = FontWeight.Bold), modifier = Modifier.width(IntrinsicSize.Min).defaultMinSize(10.dp).focusRequester(fp).onFocusChanged { state -> if (isPFocused != state.isFocused) { isPFocused = state.isFocused; if (!state.isFocused && prof.isEmpty()) onProfChange("[НАСТ БМ]") } }, cursorBrush = SolidColor(colorScheme.primary))
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { if (canUp) onLevelChange(targetLvl.toString()) }, enabled = canUp, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(40.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = if (canUp) colorScheme.primary else colorScheme.outline.copy(alpha = 0.12f))) { Text(if (targetLvl > (level.toIntOrNull() ?: 1)) "Повысить уровень" else "Понизить уровень", fontSize = 14.sp) }
    }
}
