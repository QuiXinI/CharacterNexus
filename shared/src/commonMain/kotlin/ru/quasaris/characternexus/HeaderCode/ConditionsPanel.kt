package ru.quasaris.characternexus.HeaderCode

import ru.quasaris.characternexus.model.*
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
import ru.quasaris.characternexus.model.Condition

@Composable
fun ConditionsPanel(
    allConditions: List<Condition>,
    selectedConditions: List<String>,
    onToggleCondition: (String) -> Unit,
    exhaustion: Int,
    onExhaustionChange: (Int) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(colorScheme.surface.copy(alpha = 0.0f), RoundedCornerShape(16.dp))
            .border(1.dp, colorScheme.outlineVariant.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
    ) {
        Text(
            text = "Состояния",
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.CenterHorizontally),
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = colorScheme.primary
        )

        ExhaustionSection(exhaustion, onExhaustionChange)
        HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 1.dp)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp)
                .verticalScroll(rememberScrollState())
        ) {
            allConditions.forEachIndexed { i, condition ->
                ConditionItem(condition, selectedConditions.contains(condition.name)) { onToggleCondition(condition.name) }
                if (i < allConditions.size - 1) {
                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.15f), thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
fun ExhaustionSection(exhaustion: Int, onExhaustionChange: (Int) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val animationSpec = remember {
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
    
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Истощение", 
                modifier = Modifier.weight(1f), 
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    onClick = { if (exhaustion > 0) onExhaustionChange(exhaustion - 1) },
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = colorScheme.primary.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Decrease",
                            modifier = Modifier.size(20.dp),
                            tint = colorScheme.primary
                        )
                    }
                }

                var textValue by remember(exhaustion) { mutableStateOf(exhaustion.toString()) }
                Box(
                    modifier = Modifier
                        .size(height = 44.dp, width = 48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorScheme.surfaceVariant.copy(alpha = 0.3f)),
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
                            fontSize = 20.sp, 
                            color = colorScheme.primary, 
                            fontWeight = FontWeight.Bold
                        ),
                        singleLine = true,
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(colorScheme.primary)
                    )
                }

                Surface(
                    onClick = { if (exhaustion < 6) onExhaustionChange(exhaustion + 1) },
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = colorScheme.primary.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Increase",
                            modifier = Modifier.size(20.dp),
                            tint = colorScheme.primary
                        )
                    }
                }
            }
        }
        
        AnimatedVisibility(
            visible = exhaustion > 0,
            enter = expandVertically(animationSpec) + fadeIn(floatSpring),
            exit = shrinkVertically(animationSpec) + fadeOut(floatSpring)
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
    val sep = colorScheme.outlineVariant.copy(alpha = 0.2f)
    val animationSpec = remember {
        spring<IntSize>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) colorScheme.primary.copy(alpha = 0.05f) else Color.Transparent)
            .animateContentSize(animationSpec)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .heightIn(min = 48.dp)
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = condition.name,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                fontSize = 16.sp,
                color = if (isSelected) colorScheme.primary else colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
            VerticalDivider(modifier = Modifier.fillMaxHeight().width(1.dp), color = sep)
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .fillMaxHeight()
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.Close else Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isSelected) colorScheme.primary else colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec),
            exit = shrinkVertically(animationSpec)
        ) {
            Column {
                HorizontalDivider(color = sep, thickness = 1.2.dp)
                Text(formatDescription(condition.description, colorScheme.primary), modifier = Modifier.padding(16.dp), fontSize = 14.sp, color = colorScheme.onSurface.copy(alpha = 0.8f))
            }
        }
    }
}

fun formatDescription(text: String, primaryColor: Color = Color.Unspecified): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.lines()
        lines.forEachIndexed { i, line ->
            var l = line.trim()
            if (l.startsWith("- ")) {
                withStyle(SpanStyle(fontWeight = FontWeight.Black, color = if (primaryColor != Color.Unspecified) primaryColor else Color.Unspecified)) {
                    append("• ")
                }
                l = l.substring(2)
            }
            val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
            var last = 0
            boldRegex.findAll(l).forEach { m ->
                append(l.substring(last, m.range.first))
                withStyle(SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = if (primaryColor != Color.Unspecified) primaryColor.copy(alpha = 0.9f) else Color.Unspecified
                )) { 
                    append(m.groupValues[1]) 
                }
                last = m.range.last + 1
            }
            append(l.substring(last))
            if (i < lines.size - 1) append("\n")
        }
    }
}
