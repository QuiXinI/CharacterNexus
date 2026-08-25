package ru.quasaris.characternexus.tabs.glossary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import ru.quasaris.characternexus.backend.*
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.tabs.spells.SpellCardItem

@Composable
fun RichText(text: JsonElement?) {
    if (text == null) return
    val content = when (text) {
        is JsonPrimitive -> text.content
        is JsonArray -> text.joinToString("\n") { (it as? JsonPrimitive)?.content ?: it.toString() }
        else -> text.toString()
    }
    
    // Basic cleanup of refs for display if they aren't parsed yet
    val displayState = remember(content) {
        content.replace(Regex("\\[(.*?)\\]\\(ref://.*?\\)"), "$1")
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1") // Simple bold removal for now
    }
    
    Text(
        text = displayState,
        style = MaterialTheme.typography.bodyLarge,
        lineHeight = 24.sp,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun RichText(text: String) {
    RichText(JsonPrimitive(text))
}

@Composable
fun SpellCardRenderer(spell: SpellCard) {
    var isExpanded by remember { mutableStateOf(true) }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        SpellCardItem(
            spell = spell,
            isExpanded = isExpanded,
            onToggleExpand = { isExpanded = !isExpanded },
            isEditable = false,
            isSelected = false
        )
    }
}

@Composable
fun GameTableRenderer(table: GameTable) {
    val schema = table.schema
    val columns = schema?.columns ?: emptyList()
    val rows = table.rows ?: emptyList()
    
    if (columns.isEmpty() || rows.isEmpty()) {
        // Fallback for simple data or missing schema
        if (rows.isNotEmpty()) {
            Text("Данные таблицы (схема отсутствует)", style = MaterialTheme.typography.labelSmall)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
    ) {
        // Title
        if (!table.title.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = table.title,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            columns.forEach { col ->
                Text(
                    text = col.label ?: "",
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Rows
        rows.forEach { row ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(modifier = Modifier.fillMaxWidth()) {
                columns.forEach { col ->
                    val element = row[col.key ?: ""]
                    val value = when (element) {
                        is JsonPrimitive -> element.content
                        is JsonArray -> element.joinToString("\n") { 
                            if (it is JsonPrimitive) it.content.replace(Regex("\\[(.*?)\\]\\(ref://.*?\\)"), "$1") 
                            else it.toString() 
                        }
                        else -> element?.toString() ?: ""
                    }
                    Text(
                        text = value,
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun FeatureRenderer(feature: GameFeature) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        val title = when {
            feature.levelLine != null -> "${feature.name} (${feature.levelLine})"
            feature.level != null -> "${feature.name} (${feature.level} ур.)"
            else -> feature.name ?: "Умение"
        }
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))
        RichText(feature.description)
        
        feature.tables?.forEach { table ->
            ProgressionTableRenderer(table)
        }
    }
}

@Composable
fun ProgressionTableRenderer(table: ProgressionTable) {
    val headers = table.headers ?: emptyList()
    val rows = table.rows ?: emptyList()
    
    if (headers.isEmpty() || rows.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            headers.forEach { header ->
                Text(
                    text = header,
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Rows
        rows.forEach { row ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { cell ->
                    Text(
                        text = cell,
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun BecomingClassRenderer(becoming: BecomingClass) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = becoming.title ?: "СТАНОВЛЕНИЕ КЛАССОМ",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        becoming.lines?.forEach { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}

@Composable
fun ClassTraitsRenderer(traits: ClassTraits) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = traits.title ?: "ОСОБЕННОСТИ КЛАССА",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        
        traits.parsed?.forEach { (key, value) ->
            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                Text(
                    text = "$key: ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                RichText(value)
            }
        }
        
        if (traits.parsed == null) {
            traits.lines?.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun SpellcastingRenderer(spellcasting: Spellcasting) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = spellcasting.tabTitle ?: "Заклинания",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(8.dp))
        
        spellcasting.groups?.forEach { group ->
            Text(
                text = group.group ?: "",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )
            group.items?.forEach { item ->
                Text(
                    text = "• $item",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp).padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun InvocationsRenderer(invocations: Invocations) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = invocations.tabTitle ?: "Воззвания",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(8.dp))
        
        invocations.intro?.forEach { line ->
            RichText(line)
            Spacer(Modifier.height(4.dp))
        }
        
        invocations.items?.forEach { item ->
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = item.name ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (!item.requirements.isNullOrBlank()) {
                    Text(
                        text = "Требования: ${item.requirements}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Spacer(Modifier.height(4.dp))
                RichText(JsonArray(item.description?.map { JsonPrimitive(it) } ?: emptyList()))
            }
            HorizontalDivider()
        }
    }
}

@Composable
fun ItemPlansRenderer(plans: ItemPlans) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = plans.tabTitle ?: "Планы предметов",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(8.dp))
        
        plans.groups?.forEach { group ->
            Text(
                text = group.group ?: "",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )
            group.items?.forEach { item ->
                Text(
                    text = "• $item",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp).padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun GlossaryImage(imagePath: String?) {
    if (imagePath == null) return
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(vertical = 8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text("Изображение: $imagePath", style = MaterialTheme.typography.labelSmall)
    }
}
