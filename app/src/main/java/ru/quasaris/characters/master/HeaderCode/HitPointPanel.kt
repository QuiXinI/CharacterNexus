package ru.quasaris.characters.master.HeaderCode

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    onFocusLost: () -> Unit,
    // Hit Dice Support
    spentHitDice: Int,
    maxHitDice: Int,
    onSpentHitDiceChange: (Int) -> Unit,
    onOpenSettings: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .background(colorScheme.surface, RoundedCornerShape(16.dp))
            .border(1.dp, colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .animateContentSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Заголовок с шестеренкой и костями хитов
        Box(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.Default.Settings, null, tint = colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
            
            Text(
                text = "Хиты",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = healthColor,
                textAlign = TextAlign.Center
            )

            // Hit Dice Counter
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                IconButton(
                    onClick = { onSpentHitDiceChange((spentHitDice + 1).coerceAtMost(maxHitDice)) },
                    modifier = Modifier.size(32.dp),
                    enabled = spentHitDice < maxHitDice
                ) {
                    Icon(androidx.compose.material.icons.Icons.Default.Remove, null, modifier = Modifier.size(16.dp), tint = colorScheme.primary)
                }
                
                Surface(
                    color = colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp).widthIn(min = 60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text(
                            "${maxHitDice - spentHitDice}/$maxHitDice КХ",
                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                        )
                    }
                }

                IconButton(
                    onClick = { onSpentHitDiceChange((spentHitDice - 1).coerceAtLeast(0)) },
                    modifier = Modifier.size(32.dp),
                    enabled = spentHitDice > 0
                ) {
                    Icon(androidx.compose.material.icons.Icons.Default.Add, null, modifier = Modifier.size(16.dp), tint = colorScheme.primary)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HealthValueBlock("Максимум", maxHp, colorScheme.onSurfaceVariant, Modifier.weight(1f)) {
                onMaxHpChange(it.filter { c -> c.isDigit() }) 
            }
            HealthValueBlock("Текущие", currentHp, healthColor, Modifier.weight(1f)) {
                onCurrentHpChange(it.filter { c -> c.isDigit() }) 
            }
            HealthValueBlock("Временные", tempHp, Color(0xFF64B5F6), Modifier.weight(1f)) {
                onTempHpChange(it.filter { c -> c.isDigit() }) 
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HealthActionButton("Лечение", Color(0xFF00C46F), Modifier.weight(1f), onHealClick)
            HealthActionButton("Урон", Color(0xFFE57373), Modifier.weight(1f), onDamageClick)
            HealthActionButton("Укрепление", Color(0xFF64B5F6), Modifier.weight(1f), onTempClick)
        }
    }
}

@Composable
fun HealthValueBlock(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var tv by remember { mutableStateOf(TextFieldValue(value)) }
    val fr = remember { FocusRequester() }

    LaunchedEffect(value) {
        if (tv.text != value) tv = tv.copy(text = value, selection = TextRange(value.length))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        
        Spacer(Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .clickable { fr.requestFocus() },
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = tv,
                onValueChange = { 
                    tv = it
                    onValueChange(it.text)
                },
                textStyle = TextStyle(
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(fr),
                cursorBrush = SolidColor(accentColor),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
}

@Composable
fun HealthActionButton(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f),
        border = borderStroke(color.copy(alpha = 0.3f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun borderStroke(color: Color) = androidx.compose.foundation.BorderStroke(1.dp, color)
