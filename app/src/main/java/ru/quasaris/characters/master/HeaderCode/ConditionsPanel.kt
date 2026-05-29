package ru.quasaris.characters.master.HeaderCode

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConditionsPanel(allConditions: List<Condition>, selectedConditions: List<String>, onToggleCondition: (String) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).shadow(4.dp, RoundedCornerShape(12.dp)).background(colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).border(1.dp, colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).animateContentSize()) {
        Text("Состояния", modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally), style = MaterialTheme.typography.titleMedium, color = colorScheme.onSurfaceVariant)
        allConditions.forEach { condition ->
            ConditionItem(condition, selectedConditions.contains(condition.name)) { onToggleCondition(condition.name) }
            HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.15f), thickness = 1.dp)
        }
    }
}

@Composable
fun ConditionItem(condition: Condition, isSelected: Boolean, onToggle: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme; var expanded by remember { mutableStateOf(false) }
    val sep = colorScheme.outline.copy(alpha = 0.2f)
    Column(modifier = Modifier.fillMaxWidth().background(if (isSelected) colorScheme.primaryContainer else Color.Transparent).animateContentSize()) {
        Row(modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable { expanded = !expanded }, verticalAlignment = Alignment.CenterVertically) {
            Text(condition.name, modifier = Modifier.weight(1f).padding(horizontal = 12.dp), fontSize = 16.sp, color = colorScheme.onSurface, textAlign = TextAlign.Center)
            Box(modifier = Modifier.width(1.2.dp).fillMaxHeight().background(sep))
            Box(modifier = Modifier.width(44.dp).fillMaxHeight().clickable { onToggle() }, contentAlignment = Alignment.Center) {
                Icon(if (isSelected) Icons.Default.Close else Icons.Default.Check, null, modifier = Modifier.size(20.dp), tint = if (isSelected) colorScheme.error else colorScheme.onSurface)
            }
        }
        if (expanded) {
            HorizontalDivider(color = sep, thickness = 1.2.dp)
            Text(formatDescription(condition.description), modifier = Modifier.padding(16.dp), fontSize = 14.sp, color = colorScheme.onSurface.copy(alpha = 0.8f))
        }
    }
}

fun formatDescription(text: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.lines()
        lines.forEachIndexed { i, line ->
            var l = line.trim()
            if (l.startsWith("- ")) l = l.substring(2)
            val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
            var last = 0
            boldRegex.findAll(l).forEach { m ->
                append(l.substring(last, m.range.first))
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(m.groupValues[1]) }
                last = m.range.last + 1
            }
            append(l.substring(last))
            if (i < lines.size - 1) append("\n")
        }
    }
}
