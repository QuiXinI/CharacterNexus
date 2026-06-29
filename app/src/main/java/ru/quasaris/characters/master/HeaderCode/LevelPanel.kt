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
import kotlinx.coroutines.delay

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
    
    // Internal states for immediate UI updates and debouncing
    var localLevel by remember { mutableStateOf(level) }
    var localExp by remember { mutableStateOf(exp) }
    var localProf by remember { mutableStateOf(prof) }

    // TextFieldValues for editing
    var ltv by remember { mutableStateOf(TextFieldValue(level, selection = TextRange(level.length))) }
    var etv by remember { mutableStateOf(TextFieldValue(exp, selection = TextRange(exp.length))) }
    var ptv by remember { mutableStateOf(TextFieldValue(prof, selection = TextRange(prof.length))) }
    var isPFocused by remember { mutableStateOf(false) }

    // Update local states when props change (sync from outside)
    LaunchedEffect(level) { 
        if (localLevel != level) { 
            localLevel = level
            if (ltv.text != level) ltv = ltv.copy(text = level, selection = TextRange(level.length))
        } 
    }
    LaunchedEffect(exp) { 
        if (localExp != exp) { 
            localExp = exp
            if (etv.text != exp) etv = etv.copy(text = exp, selection = TextRange(exp.length))
        } 
    }
    LaunchedEffect(prof) { 
        if (localProf != prof) { 
            localProf = prof
        } 
    }

    // Debounce for Level
    LaunchedEffect(localLevel) {
        if (localLevel != level) {
            delay(500)
            onLevelChange(localLevel)
        }
    }
    // Debounce for Exp
    LaunchedEffect(localExp) {
        if (localExp != exp) {
            delay(500)
            onExpChange(localExp)
        }
    }
    // Debounce for Prof
    LaunchedEffect(localProf) {
        if (localProf != prof) {
            delay(500)
            onProfChange(localProf)
        }
    }
    
    // Flush changes on dispose to ensure "save on exit"
    val currentOnLevelChange by rememberUpdatedState(onLevelChange)
    val currentOnExpChange by rememberUpdatedState(onExpChange)
    val currentOnProfChange by rememberUpdatedState(onProfChange)
    
    DisposableEffect(Unit) {
        onDispose {
            if (localLevel != level) currentOnLevelChange(localLevel)
            if (localExp != exp) currentOnExpChange(localExp)
            if (localProf != prof) currentOnProfChange(localProf)
        }
    }

    // Logic for Prof display (formulas)
    LaunchedEffect(localProf, isPFocused, stats) {
        val d = if (isPFocused) localProf else evaluateFormula(localProf, stats).toString()
        if (ptv.text != d) ptv = TextFieldValue(text = d, selection = if (isPFocused) TextRange(d.length) else TextRange.Zero)
    }

    val fl = remember { FocusRequester() }
    val fe = remember { FocusRequester() }
    val fp = remember { FocusRequester() }

    val currentLvlInt = localLevel.toIntOrNull() ?: 1
    val targetLvl = calculateLevelFromExperience(localExp)

    // Button Logic
    val buttonText: String
    val buttonEnabled: Boolean
    val buttonColor: Color
    
    when {
        currentLvlInt >= 20 -> {
            if (targetLvl < 20) {
                buttonText = "Понизить уровень"
                buttonEnabled = true
                buttonColor = colorScheme.error
            } else {
                buttonText = "вы стали божеством"
                buttonEnabled = false
                buttonColor = colorScheme.outline.copy(alpha = 0.12f)
            }
        }
        targetLvl > currentLvlInt -> {
            buttonText = "Повысить уровень"
            buttonEnabled = true
            buttonColor = colorScheme.primary
        }
        targetLvl < currentLvlInt -> {
            buttonText = "Понизить уровень"
            buttonEnabled = true
            buttonColor = colorScheme.error
        }
        else -> {
            buttonText = "недостаточно опыта"
            buttonEnabled = false
            buttonColor = colorScheme.outline.copy(alpha = 0.12f)
        }
    }

    val finalContainerColor = if (buttonEnabled) buttonColor else colorScheme.outline.copy(alpha = 0.12f)
    val finalContentColor = if (buttonEnabled) {
        if (buttonColor == colorScheme.error) colorScheme.onError else colorScheme.onPrimary
    } else {
        colorScheme.onSurface.copy(alpha = 0.38f)
    }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).shadow(4.dp, RoundedCornerShape(12.dp)).background(colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).border(1.dp, colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).animateContentSize()) {
        Text("Уровень и Опыт", modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally), style = MaterialTheme.typography.titleLarge, color = colorScheme.onSurfaceVariant)
        
        // Character Level Row
        Row(modifier = Modifier.fillMaxWidth().height(48.dp).clickable { ltv = ltv.copy(selection = TextRange(ltv.text.length)); fl.requestFocus() }, verticalAlignment = Alignment.CenterVertically) {
            Text("Уровень персонажа", modifier = Modifier.padding(start = 16.dp).weight(1f), fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
            BasicTextField(
                value = ltv, 
                onValueChange = { textFieldValue -> 
                    ltv = textFieldValue
                    val filtered = textFieldValue.text.filter { it.isDigit() }
                    if (filtered.isNotEmpty()) {
                        val n = filtered.toInt()
                        if (n in 0..100) {
                            localLevel = n.toString()
                            // When level is manually entered, set experience to the threshold for that level
                            val milestone = getPreviousLevelThreshold(n.toString())
                            localExp = milestone
                            etv = TextFieldValue(text = milestone, selection = TextRange(milestone.length))
                        }
                    } else {
                        localLevel = ""
                    }
                }, 
                textStyle = TextStyle(textAlign = TextAlign.End, fontSize = 16.sp, color = colorScheme.onSurface, fontWeight = FontWeight.Bold), 
                modifier = Modifier.width(100.dp).padding(end = 16.dp).focusRequester(fl).onFocusChanged { state -> 
                    if (!state.isFocused && localLevel.isEmpty()) { 
                        localLevel = "1"
                        ltv = TextFieldValue("1", selection = TextRange(1))
                        val milestone = getPreviousLevelThreshold("1")
                        localExp = milestone
                        etv = TextFieldValue(text = milestone, selection = TextRange(milestone.length))
                    } 
                }, 
                cursorBrush = SolidColor(colorScheme.primary), 
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
        
        HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.15f))
        
        // Experience Row
        Row(modifier = Modifier.fillMaxWidth().height(48.dp).clickable { etv = etv.copy(selection = TextRange(etv.text.length)); fe.requestFocus() }, verticalAlignment = Alignment.CenterVertically) {
            Text("Текущий опыт", modifier = Modifier.padding(start = 16.dp).weight(1f), fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
            BasicTextField(
                value = etv, 
                onValueChange = { textFieldValue -> 
                    etv = textFieldValue
                    localExp = textFieldValue.text.filter { it.isDigit() }
                }, 
                textStyle = TextStyle(textAlign = TextAlign.End, fontSize = 16.sp, color = colorScheme.onSurface, fontWeight = FontWeight.Bold), 
                modifier = Modifier.width(100.dp).padding(end = 4.dp).focusRequester(fe).onFocusChanged { state -> 
                    if (!state.isFocused && localExp.isEmpty()) {
                        localExp = "0"
                        etv = TextFieldValue("0", selection = TextRange(1))
                    } 
                }, 
                cursorBrush = SolidColor(colorScheme.primary), 
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Text("/ $nextExp", modifier = Modifier.padding(end = 16.dp), fontSize = 14.sp, color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
        
        HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.15f))
        
        // Proficiency Bonus Row
        Row(modifier = Modifier.fillMaxWidth().height(48.dp).clickable { fp.requestFocus() }, verticalAlignment = Alignment.CenterVertically) {
            Text("Бонус Мастерства", modifier = Modifier.padding(start = 16.dp).weight(1f), fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!isPFocused && evaluateFormula(localProf, stats) >= 0) Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                BasicTextField(
                    value = ptv, 
                    onValueChange = { textFieldValue -> 
                        if (isPFocused) { 
                            ptv = textFieldValue
                            localProf = textFieldValue.text 
                        } 
                    }, 
                    textStyle = TextStyle(textAlign = if (isPFocused) TextAlign.Start else TextAlign.End, fontSize = 16.sp, color = colorScheme.onSurface, fontWeight = FontWeight.Bold), 
                    modifier = Modifier.width(IntrinsicSize.Min).defaultMinSize(10.dp).focusRequester(fp).onFocusChanged { state -> 
                        if (isPFocused != state.isFocused) { 
                            isPFocused = state.isFocused
                            if (!state.isFocused && localProf.isEmpty()) {
                                localProf = "[НАСТ БМ]"
                            }
                        } 
                    }, 
                    cursorBrush = SolidColor(colorScheme.primary)
                )
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Level Up/Down Button
        Button(
            onClick = { 
                if (buttonEnabled) {
                    localLevel = targetLvl.toString()
                    ltv = TextFieldValue(targetLvl.toString(), selection = TextRange(targetLvl.toString().length))
                } 
            }, 
            enabled = buttonEnabled, 
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(40.dp), 
            shape = RoundedCornerShape(8.dp), 
            colors = ButtonDefaults.buttonColors(
                containerColor = finalContainerColor,
                contentColor = finalContentColor,
                disabledContainerColor = finalContainerColor,
                disabledContentColor = finalContentColor
            )
        ) { 
            Text(buttonText, fontSize = 14.sp) 
        }
    }
}
