package ru.quasaris.characternexus.ui.glossary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import ru.quasaris.characternexus.backend.glossary.*
import ru.quasaris.characternexus.model.DynamicNoteState

/** Показ форматированного текста тем же компонентом, что и в редакторах (только чтение). */
@Composable
fun GlossaryRichText(text: String, modifier: Modifier = Modifier) {
    if (text.isBlank()) return
    val field = remember(text) { DynamicNoteState(content = text) }
    Box(modifier = modifier.fillMaxWidth()) {
        ru.quasaris.characternexus.tabs.NotionBlockEditor(
            field = field,
            onFieldChange = {},
            canEdit = false,
            isReorderMode = false,
            contentPlaceholder = "",
            isBasicMode = true
        )
    }
}

/** Крупная верхняя карточка сущности: название, подзаголовок, чипы. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HeroCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    chips: List<String> = emptyList(),
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    content: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Surface(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = container, contentColor = content) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            if (!subtitle.isNullOrBlank()) Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            if (chips.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    chips.forEach { chip ->
                        Surface(shape = RoundedCornerShape(50), color = content.copy(alpha = 0.12f), contentColor = content) {
                            Text(chip, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DisplaySectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    )
}

/** Раскрывающаяся карточка умения. */
@Composable
fun FeatureCard(feature: FeatureDef, showLevel: Boolean, initiallyExpanded: Boolean = false) {
    var expanded by remember(feature.id) { mutableStateOf(initiallyExpanded) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(feature.name.ifBlank { "Без названия" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (feature.requirement.isNotBlank()) {
                        Text(feature.requirement, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (showLevel && feature.level != null) {
                    SmallBadge("${feature.level} ур.")
                    Spacer(Modifier.width(8.dp))
                }
                Icon(Icons.Default.ExpandMore, null, modifier = Modifier.rotate(rotation), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    GlossaryRichText(feature.description)
                }
            }
        }
    }
}

@Composable
fun FeatureList(features: List<FeatureDef>, showLevel: Boolean, initiallyExpanded: Boolean = false, sortByLevel: Boolean = false) {
    val list = if (sortByLevel) features.sortedBy { it.level ?: 0 } else features
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        list.forEach { FeatureCard(it, showLevel, initiallyExpanded) }
    }
}

/** Простая таблица с горизонтальной прокруткой и чередованием строк. */
@Composable
fun SimpleTable(headers: List<String>, rows: List<List<String>>, modifier: Modifier = Modifier) {
    val columnCount = maxOf(headers.size, rows.maxOfOrNull { it.size } ?: 0)
    if (columnCount == 0) return
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            if (headers.isNotEmpty()) {
                Row(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                    for (i in 0 until columnCount) TableCell(headers.getOrElse(i) { "" }, bold = true)
                }
            }
            rows.forEachIndexed { index, row ->
                Row(modifier = Modifier.background(if (index % 2 == 1) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent)) {
                    for (i in 0 until columnCount) TableCell(row.getOrElse(i) { "" }, bold = false)
                }
            }
        }
    }
}

@Composable
private fun TableCell(text: String, bold: Boolean) {
    Box(modifier = Modifier.widthIn(min = 80.dp, max = 220.dp).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

/** Блоки кастомной вкладки. */
@Composable
fun TabBlocksView(blocks: List<TabBlock>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        blocks.forEach { block ->
            when (block) {
                is TabBlock.Text -> GlossaryRichText(block.text)
                is TabBlock.Table -> Column {
                    if (block.title.isNotBlank()) DisplaySectionTitle(block.title)
                    SimpleTable(block.headers, block.rows)
                }
                is TabBlock.Entries -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (block.title.isNotBlank()) DisplaySectionTitle(block.title)
                    block.items.forEach { EntryCard(it) }
                }
            }
        }
    }
}

@Composable
private fun EntryCard(entry: GlossaryEntry) {
    var expanded by remember(entry.id) { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(entry.name.ifBlank { "Без названия" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (entry.requirement.isNotBlank()) {
                        Text(entry.requirement, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (entry.repeatable) SmallBadge("Повторяемо")
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) { GlossaryRichText(entry.description) }
            }
        }
    }
}
