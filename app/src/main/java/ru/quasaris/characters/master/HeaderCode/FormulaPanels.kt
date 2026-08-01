package ru.quasaris.characters.master.HeaderCode

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.IntSize
import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.text.TextStyle
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
    val animationSpec = spring<IntSize>(stiffness = Spring.StiffnessMedium)
    
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).shadow(4.dp, RoundedCornerShape(12.dp)).background(colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).border(1.dp, colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).animateContentSize(animationSpec)) {
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp)) {
            Text(title, modifier = Modifier.align(Alignment.Center), style = MaterialTheme.typography.titleLarge, color = colorScheme.onSurfaceVariant)
            if (headerTrailing != null) {
                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                    headerTrailing()
                }
            }
        }
        
        Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
            entries.forEachIndexed { i, entry ->
                FormulaEntryItem(entry, entry.id == activeId, entry.id == deleteId, { updated -> val nl = entries.toMutableList(); nl[i] = updated; onEntries(nl) }, { val nl = entries.toMutableList(); nl.removeAt(i); if (entry.id == activeId) onActive(null); onEntries(nl); onDeleteReq(null) }, { onDeleteReq(entry.id) }, { onActive(if (entry.id == activeId) null else entry.id); onDeleteReq(null) })
                HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.15f))
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth().height(48.dp).clickable { onAdd(); onDeleteReq(null) }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) { Icon(Icons.Default.AddCircleOutline, null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Добавить Новое", fontSize = 16.sp) }
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
    val colorScheme = MaterialTheme.colorScheme; val sep = colorScheme.outline.copy(alpha = 0.2f)
    val animationSpec = spring<IntSize>(stiffness = Spring.StiffnessMedium)
    
    Column(modifier = Modifier.fillMaxWidth().background(if (isActive) colorScheme.primaryContainer else Color.Transparent).animateContentSize(animationSpec)) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(44.dp).fillMaxHeight().clickable { if (isDelete) onDelete() else onDeleteReq() }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Delete, null, modifier = Modifier.size(20.dp), tint = if (isDelete) colorScheme.error else colorScheme.onSurface.copy(alpha = 0.7f)) }
            Box(modifier = Modifier.width(1.2.dp).fillMaxHeight().background(sep))
            Box(modifier = Modifier.weight(1f).padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                if (entry.name.isEmpty()) Text("Название", color = colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 16.sp)
                BasicTextField(value = entry.name, onValueChange = { s -> 
                    val u: FormulaEntry = when(entry) { 
                        is ArmorClassEntry -> entry.copy(name = s)
                        is InitiativeEntry -> entry.copy(name = s)
                        is SpeedEntry -> entry.copy(name = s)
                        is ShieldEntry -> entry.copy(name = s)
                        else -> entry 
                    }
                    onUpdate(u) 
                }, textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 16.sp, color = colorScheme.onSurface), modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp))
            }
            if (entry is InitiativeEntry) {
                Box(modifier = Modifier.width(1.2.dp).fillMaxHeight().background(sep))
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .fillMaxHeight()
                        .clickable { onUpdate(entry.copy(hasAdvantage = !entry.hasAdvantage)) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        null,
                        tint = if (entry.hasAdvantage) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Box(modifier = Modifier.width(1.2.dp).fillMaxHeight().background(sep))
            Box(modifier = Modifier.width(44.dp).fillMaxHeight().clickable { onToggle() }, contentAlignment = Alignment.Center) { Icon(if (isActive) Icons.Default.Close else Icons.Default.Check, null, modifier = Modifier.size(20.dp)) }
        }
        HorizontalDivider(color = sep, thickness = 1.2.dp); Box(modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(horizontal = 16.dp, vertical = 8.dp)) {
            if (entry.formula.isEmpty() && entry.bonuses.none { it.isActive }) Text("Формула", color = colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 14.sp)
            BasicTextField(value = getFullFormula(entry), onValueChange = { s -> 
                val u: FormulaEntry = when(entry) { 
                    is ArmorClassEntry -> entry.copy(formula = s, bonuses = entry.bonuses.map { it.copy(isActive = false) })
                    is InitiativeEntry -> entry.copy(formula = s, bonuses = entry.bonuses.map { it.copy(isActive = false) })
                    is SpeedEntry -> entry.copy(formula = s, bonuses = entry.bonuses.map { it.copy(isActive = false) })
                    is ShieldEntry -> entry.copy(formula = s, bonuses = entry.bonuses.map { it.copy(isActive = false) })
                    else -> entry 
                }
                onUpdate(u) 
            }, textStyle = TextStyle(fontSize = 14.sp, color = colorScheme.onSurface), modifier = Modifier.fillMaxWidth())
        }
    }
}
