package ru.quasaris.characternexus.tabs.glossary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.quasaris.characternexus.backend.*

@Composable
fun RichText(text: String) {
    // For now, just basic text. In future, parse ref:// links
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        lineHeight = 24.sp,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun GameTableRenderer(table: GameTable) {
    val columns = table.schema?.columns
    val rows = table.rows
    
    if (columns.isNullOrEmpty() || rows.isNullOrEmpty()) return

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
                    val value = row[col.key ?: ""]?.toString() ?: ""
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
        Text(
            text = if (feature.level != null) "${feature.name ?: "Умение"} (${feature.level} ур.)" else feature.name ?: "Умение",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))
        RichText(feature.description ?: "")
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
