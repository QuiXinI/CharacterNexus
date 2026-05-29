package ru.quasaris.characters.master.MainWindow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.quasaris.characters.master.HeaderCode.SquirclePath
import ru.quasaris.characters.master.HeaderCode.calculateModifier

@Composable
fun AttributesSection(
    strength: String, onStrengthChange: (String) -> Unit, strProf: Boolean, onStrProfChange: (Boolean) -> Unit,
    intelligence: String, onIntelligenceChange: (String) -> Unit, intProf: Boolean, onIntProfChange: (Boolean) -> Unit,
    dexterity: String, onDexterityChange: (String) -> Unit, dexProf: Boolean, onDexProfChange: (Boolean) -> Unit,
    wisdom: String, onWisdomChange: (String) -> Unit, wisProf: Boolean, onWisProfChange: (Boolean) -> Unit,
    constitution: String, onConstitutionChange: (String) -> Unit, conProf: Boolean, onConProfChange: (Boolean) -> Unit,
    charisma: String, onCharismaChange: (String) -> Unit, chaProf: Boolean, onChaProfChange: (Boolean) -> Unit,
    evalPB: String
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Сила", strength, evalPB, strProf, Modifier.weight(1f), onStrengthChange, onStrProfChange)
            StatCard("Интеллект", intelligence, evalPB, intProf, Modifier.weight(1f), onIntelligenceChange, onIntProfChange)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Ловкость", dexterity, evalPB, dexProf, Modifier.weight(1f), onDexterityChange, onDexProfChange)
            StatCard("Мудрость", wisdom, evalPB, wisProf, Modifier.weight(1f), onWisdomChange, onWisProfChange)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Телосложение", constitution, evalPB, conProf, Modifier.weight(1f), onConstitutionChange, onConProfChange)
            StatCard("Харизма", charisma, evalPB, chaProf, Modifier.weight(1f), onCharismaChange, onChaProfChange)
        }
    }
    
    Spacer(Modifier.height(16.dp))
    Text("Пассивные проверки", modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), textAlign = TextAlign.Center, fontSize = 15.sp, color = colorScheme.onSurface)
    Column(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(colorScheme.primary.copy(alpha = 0.1f)).padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        PassiveCheckRow("Анализ (Интеллект)", (10 + calculateModifier(intelligence)).toString())
        PassiveCheckRow("Внимательность (Мудрость)", (10 + calculateModifier(wisdom)).toString())
        PassiveCheckRow("Проницательность (Мудрость)", (10 + calculateModifier(wisdom)).toString())
    }
}

@Composable
fun StatCard(label: String, value: String, profB: String, isP: Boolean, modifier: Modifier = Modifier, onValue: (String) -> Unit, onPToggle: (Boolean) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme; val base = calculateModifier(value); val total = base + (if (isP) profB.toIntOrNull() ?: 0 else 0)
    Box(modifier = modifier.height(104.dp).shadow(2.dp, RoundedCornerShape(8.dp)).background(colorScheme.surface, RoundedCornerShape(8.dp)).border(1.dp, colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(8.dp)) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Box(modifier = Modifier.align(Alignment.BottomStart).padding(start = 22.dp, bottom = 2.dp).size(38.dp).rotate(if (isP) -45f else 0f).clip(if (isP) SquirclePath else RoundedCornerShape(8.dp)).background(if (isP) colorScheme.primaryContainer else colorScheme.surfaceVariant).border(1.dp, if (isP) colorScheme.primary else colorScheme.outline.copy(alpha = 0.2f), if (isP) SquirclePath else RoundedCornerShape(8.dp)).clickable { onPToggle(!isP) }, contentAlignment = Alignment.Center) {
            Text(if (total >= 0) "+$total" else "$total", modifier = Modifier.rotate(if (isP) 45f else 0f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(modifier = Modifier.align(Alignment.TopEnd), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)).background(colorScheme.surfaceVariant).border(1.dp, colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                BasicTextField(value = value, onValueChange = { val f = it.filter { it.isDigit() }; if (f.isEmpty()) onValue("") else { val n = f.toIntOrNull(); if (n != null && n in 1..30) onValue(f) } }, textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface), modifier = Modifier.width(32.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)).background(colorScheme.surface).border(1.dp, colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Text(if (base >= 0) "+$base" else "$base", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun PassiveCheckRow(label: String, value: String) {
    val colorScheme = MaterialTheme.colorScheme
    Row(modifier = Modifier.fillMaxWidth().height(30.dp).clip(RoundedCornerShape(8.dp)).background(colorScheme.primary.copy(alpha = 0.2f)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.padding(start = 12.dp), fontSize = 13.sp, color = colorScheme.onSurface)
        Text(value, modifier = Modifier.padding(end = 12.dp), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}
