package ru.quasaris.characters.master.HeaderCode

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.IntSize
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.quasaris.characters.master.backend.Condition

@Composable
fun ConditionsPanel(
    allConditions: List<Condition>,
    selectedConditions: List<String>,
    onToggleCondition: (String) -> Unit,
    exhaustion: Int,
    onExhaustionChange: (Int) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val animationSpec = spring<IntSize>(stiffness = Spring.StiffnessMedium)
    
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).shadow(4.dp, RoundedCornerShape(12.dp)).background(colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).border(1.dp, colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).animateContentSize(animationSpec)) {
        Text("Состояния", modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally), style = MaterialTheme.typography.titleLarge, color = colorScheme.onSurfaceVariant)

        ExhaustionSection(exhaustion, onExhaustionChange)
        HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.3f), thickness = 1.dp)

        Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
            allConditions.forEach { condition ->
                ConditionItem(condition, selectedConditions.contains(condition.name)) { onToggleCondition(condition.name) }
                HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.15f), thickness = 1.dp)
            }
        }
    }
}

@Composable
fun ExhaustionSection(exhaustion: Int, onExhaustionChange: (Int) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val animationSpec = spring<IntSize>(stiffness = Spring.StiffnessMedium)
    
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                "Истощение", 
                modifier = Modifier.weight(1f), 
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
                color = colorScheme.onSurface
            )
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { if (exhaustion > 0) onExhaustionChange(exhaustion - 1) },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = colorScheme.secondaryContainer,
                        contentColor = colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Decrease")
                }

                var textValue by remember(exhaustion) { mutableStateOf(exhaustion.toString()) }
                Box(
                    modifier = Modifier
                        .size(height = 54.dp, width = 54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorScheme.primaryContainer)
                        .border(1.dp, colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
                        value = textValue,
                        onValueChange = {
                            textValue = it
                            it.toIntOrNull()?.let { v -> onExhaustionChange(v.coerceIn(0, 6)) }
                        },
                        textStyle = TextStyle(
                            textAlign = TextAlign.Center, 
                            fontSize = 22.sp, 
                            color = colorScheme.onPrimaryContainer, 
                            fontWeight = FontWeight.Bold
                        ),
                        singleLine = true
                    )
                }

                IconButton(
                    onClick = { if (exhaustion < 6) onExhaustionChange(exhaustion + 1) },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = colorScheme.secondaryContainer,
                        contentColor = colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Increase")
                }
            }
        }
        
        AnimatedVisibility(
            visible = exhaustion > 0,
            enter = expandVertically(animationSpec) + fadeIn(),
            exit = shrinkVertically(animationSpec) + fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                if (exhaustion == 6) {
                    Text("Смерть", fontSize = 18.sp, color = colorScheme.error, fontWeight = FontWeight.ExtraBold)
                }
                Text("-${exhaustion * 2} к проверкам к20", fontSize = 16.sp, color = colorScheme.error, fontWeight = FontWeight.Medium)
                Text("-${exhaustion * 5} фт к скорости", fontSize = 16.sp, color = colorScheme.error, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun ConditionItem(condition: Condition, isSelected: Boolean, onToggle: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme; var expanded by remember { mutableStateOf(false) }
    val sep = colorScheme.outline.copy(alpha = 0.2f)
    val animationSpec = spring<IntSize>(stiffness = Spring.StiffnessHigh)
    
    Column(modifier = Modifier.fillMaxWidth().background(if (isSelected) colorScheme.primaryContainer else Color.Transparent).animateContentSize(animationSpec)) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).heightIn(min = 44.dp).clickable { expanded = !expanded }, verticalAlignment = Alignment.CenterVertically) {
            Text(condition.name, modifier = Modifier.weight(1f).padding(horizontal = 12.dp), fontSize = 16.sp, color = colorScheme.onSurface, textAlign = TextAlign.Center)
            Box(modifier = Modifier.width(1.2.dp).fillMaxHeight().background(sep))
            Box(modifier = Modifier.width(44.dp).fillMaxHeight().clickable { onToggle() }, contentAlignment = Alignment.Center) {
                Icon(if (isSelected) Icons.Default.Close else Icons.Default.Check, null, modifier = Modifier.size(20.dp), tint = if (isSelected) colorScheme.error else colorScheme.onSurface)
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec),
            exit = shrinkVertically(animationSpec)
        ) {
            Column {
                HorizontalDivider(color = sep, thickness = 1.2.dp)
                Text(formatDescription(condition.description), modifier = Modifier.padding(16.dp), fontSize = 14.sp, color = colorScheme.onSurface.copy(alpha = 0.8f))
            }
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
