package ru.quasaris.characters.master.HeaderCode

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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

@Composable
fun FormulaPanel(
    title: String,
    entries: List<FormulaEntry>,
    activeId: String?,
    deleteId: String?,
    onEntries: (List<FormulaEntry>) -> Unit,
    onActive: (String?) -> Unit,
    onDeleteReq: (String?) -> Unit,
    onAdd: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).shadow(4.dp, RoundedCornerShape(12.dp)).background(colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).border(1.dp, colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).animateContentSize()) {
        Text(title, modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally), style = MaterialTheme.typography.titleMedium, color = colorScheme.onSurfaceVariant)
        entries.forEachIndexed { i, entry ->
            FormulaEntryItem(entry, entry.id == activeId, entry.id == deleteId, { updated -> val nl = entries.toMutableList(); nl[i] = updated; onEntries(nl) }, { val nl = entries.toMutableList(); nl.removeAt(i); if (entry.id == activeId) onActive(null); onEntries(nl); onDeleteReq(null) }, { onDeleteReq(entry.id) }, { onActive(if (entry.id == activeId) null else entry.id); onDeleteReq(null) })
            HorizontalDivider(color = colorScheme.outline.copy(alpha = 0.15f))
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
    Column(modifier = Modifier.fillMaxWidth().background(if (isActive) colorScheme.primaryContainer else Color.Transparent).animateContentSize()) {
        Row(modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(44.dp).fillMaxHeight().clickable { if (isDelete) onDelete() else onDeleteReq() }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Delete, null, modifier = Modifier.size(20.dp), tint = if (isDelete) colorScheme.error else colorScheme.onSurface.copy(alpha = 0.7f)) }
            Box(modifier = Modifier.width(1.2.dp).fillMaxHeight().background(sep))
            Box(modifier = Modifier.weight(1f).padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                if (entry.name.isEmpty()) Text("Название", color = colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 16.sp)
                BasicTextField(value = entry.name, onValueChange = { s -> 
                    val u: FormulaEntry = when(entry) { is ArmorClassEntry -> entry.copy(name = s); is InitiativeEntry -> entry.copy(name = s); is SpeedEntry -> entry.copy(name = s); else -> entry }
                    onUpdate(u) 
                }, textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 16.sp, color = colorScheme.onSurface), modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp))
            }
            Box(modifier = Modifier.width(1.2.dp).fillMaxHeight().background(sep))
            Box(modifier = Modifier.width(44.dp).fillMaxHeight().clickable { onToggle() }, contentAlignment = Alignment.Center) { Icon(if (isActive) Icons.Default.Close else Icons.Default.Check, null, modifier = Modifier.size(20.dp)) }
        }
        HorizontalDivider(color = sep, thickness = 1.2.dp); Box(modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(horizontal = 16.dp, vertical = 8.dp)) {
            if (entry.formula.isEmpty()) Text("Формула", color = colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 14.sp)
            BasicTextField(value = entry.formula, onValueChange = { s -> 
                val u: FormulaEntry = when(entry) { is ArmorClassEntry -> entry.copy(formula = s); is InitiativeEntry -> entry.copy(formula = s); is SpeedEntry -> entry.copy(formula = s); else -> entry }
                onUpdate(u) 
            }, textStyle = TextStyle(fontSize = 14.sp, color = colorScheme.onSurface), modifier = Modifier.fillMaxWidth())
        }
    }
}
