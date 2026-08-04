package ru.quasaris.characters.master.HeaderCode

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.quasaris.characters.master.ArmorClassEntry
import ru.quasaris.characters.master.FormulaEntry
import ru.quasaris.characters.master.InitiativeEntry
import ru.quasaris.characters.master.SpeedEntry
import ru.quasaris.characters.master.ShieldEntry

fun getFullFormula(entry: FormulaEntry): String {
    var full = entry.formula
    val isInitiative = entry is InitiativeEntry
    
    if (!isInitiative) {
        full = full.replace(Regex("\\b\\d*d\\d+\\b", RegexOption.IGNORE_CASE), "").trim()
        full = full.replace(Regex("\\+\\s*$"), "").replace(Regex("^\\s*\\+"), "").trim()
    }
    
    entry.bonuses.filter { it.isActive }.forEach {
        var f = it.formula.trim()
        if (!isInitiative) {
            f = f.replace(Regex("\\b\\d*d\\d+\\b", RegexOption.IGNORE_CASE), "").trim()
            f = f.replace(Regex("^\\s*\\+\\s*"), "").replace(Regex("\\s*\\+\\s*$"), "")
        }
        
        if (f.isNotEmpty()) {
            val prefix = if (f.startsWith("+") || f.startsWith("-")) " " else " + "
            full += "$prefix$f"
        }
    }
    return full
}

@Composable
fun FormulaPanel(
    title: String,
    entries: List<FormulaEntry>,
    activeId: String?,
    deleteId: String?,
    onEntries: (List<FormulaEntry>) -> Unit,
    onActive: (String?) -> Unit,
    onDeleteReq: (String?) -> Unit,
    onAdd: () -> Unit,
    headerTrailing: @Composable (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(colorScheme.surface.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .border(1.dp, colorScheme.outlineVariant.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = title,
                modifier = Modifier.align(Alignment.Center),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = colorScheme.primary
            )
            if (headerTrailing != null) {
                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                    headerTrailing()
                }
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
                .verticalScroll(rememberScrollState())
        ) {
            entries.forEachIndexed { i, entry ->
                FormulaEntryItem(
                    entry = entry,
                    isActive = entry.id == activeId,
                    isDelete = entry.id == deleteId,
                    onUpdate = { updated -> 
                        val nl = entries.toMutableList()
                        nl[i] = updated
                        onEntries(nl) 
                    },
                    onDelete = { 
                        val nl = entries.toMutableList()
                        nl.removeAt(i)
                        if (entry.id == activeId) onActive(null)
                        onEntries(nl)
                        onDeleteReq(null) 
                    },
                    onDeleteReq = { onDeleteReq(entry.id) },
                    onToggle = { 
                        onActive(if (entry.id == activeId) null else entry.id)
                        onDeleteReq(null) 
                    }
                )
                if (i < entries.size - 1) {
                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        Surface(
            onClick = { 
                onAdd()
                onDeleteReq(null) 
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(44.dp),
            shape = RoundedCornerShape(8.dp),
            color = colorScheme.primary.copy(alpha = 0.1f),
            border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.AddCircleOutline, null, modifier = Modifier.size(20.dp), tint = colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Добавить Новое", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
            }
        }
    }
}

@Composable
fun FormulaEntryItem(
    entry: FormulaEntry,
    isActive: Boolean,
    isDelete: Boolean,
    onUpdate: (FormulaEntry) -> Unit,
    onDelete: () -> Unit,
    onDeleteReq: () -> Unit,
    onToggle: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val sep = colorScheme.outlineVariant.copy(alpha = 0.3f)
    val animationSpec = spring<IntSize>(stiffness = Spring.StiffnessMedium)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isActive) colorScheme.primary.copy(alpha = 0.05f) else Color.Transparent)
            .animateContentSize(animationSpec)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .fillMaxHeight()
                    .clickable { if (isDelete) onDelete() else onDeleteReq() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isDelete) colorScheme.error else colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            
            VerticalDivider(modifier = Modifier.fillMaxHeight().width(1.dp), color = sep)
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (entry.name.isEmpty()) {
                    Text(
                        text = "Название",
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        fontSize = 16.sp
                    )
                }
                BasicTextField(
                    value = entry.name,
                    onValueChange = { s -> 
                        val u: FormulaEntry = when(entry) { 
                            is ArmorClassEntry -> entry.copy(name = s)
                            is InitiativeEntry -> entry.copy(name = s)
                            is SpeedEntry -> entry.copy(name = s)
                            is ShieldEntry -> entry.copy(name = s)
                            else -> entry 
                        }
                        onUpdate(u) 
                    },
                    textStyle = TextStyle(
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                        color = if (isActive) colorScheme.primary else colorScheme.onSurface,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    cursorBrush = SolidColor(colorScheme.primary)
                )
            }
            
            if (entry is InitiativeEntry) {
                VerticalDivider(modifier = Modifier.fillMaxHeight().width(1.dp), color = sep)
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .fillMaxHeight()
                        .clickable { onUpdate(entry.copy(hasAdvantage = !entry.hasAdvantage)) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = if (entry.hasAdvantage) colorScheme.primary else colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            VerticalDivider(modifier = Modifier.fillMaxHeight().width(1.dp), color = sep)
            
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .fillMaxHeight()
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isActive) Icons.Default.Close else Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isActive) colorScheme.primary else colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        
        AnimatedVisibility(visible = isActive) {
            Column {
                HorizontalDivider(color = sep, thickness = 1.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .background(colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (entry.formula.isEmpty() && entry.bonuses.none { it.isActive }) {
                        Text(
                            text = "Формула",
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            fontSize = 14.sp
                        )
                    }
                    BasicTextField(
                        value = getFullFormula(entry),
                        onValueChange = { s -> 
                            val u: FormulaEntry = when(entry) { 
                                is ArmorClassEntry -> entry.copy(formula = s, bonuses = entry.bonuses.map { it.copy(isActive = false) })
                                is InitiativeEntry -> entry.copy(formula = s, bonuses = entry.bonuses.map { it.copy(isActive = false) })
                                is SpeedEntry -> entry.copy(formula = s, bonuses = entry.bonuses.map { it.copy(isActive = false) })
                                is ShieldEntry -> entry.copy(formula = s, bonuses = entry.bonuses.map { it.copy(isActive = false) })
                                else -> entry 
                            }
                            onUpdate(u) 
                        },
                        textStyle = TextStyle(fontSize = 14.sp, color = colorScheme.onSurface, fontWeight = FontWeight.Medium),
                        modifier = Modifier.fillMaxWidth(),
                        cursorBrush = SolidColor(colorScheme.primary)
                    )
                }
            }
        }
    }
}
