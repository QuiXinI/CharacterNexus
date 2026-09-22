package ru.quasaris.characternexus.tabs.resources

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.quasaris.characternexus.model.DynamicContentBlock

data class ResourceLinkConfig(
    val candidates: List<ResourceLinkCandidate>,
    val sharedWith: List<ResourceUsage>,
    val onLink: (targetId: String) -> Unit,
    val onUnlink: () -> Unit
)

@Composable
fun ResourceLinkTab(
    config: ResourceLinkConfig,
    onLink: (String) -> Unit,
    onUnlink: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<ResourceLinkCandidate?>(null) }

    val filtered = remember(config.candidates, query) {
        val q = query.trim()
        if (q.isEmpty()) config.candidates
        else config.candidates.filter { it.resource.name.contains(q, ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (config.sharedWith.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Связан с другими ресурсами", fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                    config.sharedWith.forEach { usage ->
                        Text(
                            "• ${usage.label}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = onUnlink,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Отвязать (сделать независимую копию)")
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Связать с существующим ресурсом",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
            Text(
                "Название, значения и настройки станут общими: изменение в одном месте сразу появится во всех связанных.",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant
            )
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Поиск по названию") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )

        if (filtered.isEmpty()) {
            Text(
                if (config.candidates.isEmpty()) "Других ресурсов в этом листе пока нет." else "Ничего не найдено.",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
        }

        filtered.forEach { candidate ->
            ResourceCandidateCard(candidate = candidate, onClick = { pending = candidate })
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    pending?.let { candidate ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Связать с «${candidate.resource.name}»?") },
            text = {
                Text(
                    "Название, значения и настройки этого ресурса будут заменены данными выбранного. " +
                        "Дальше изменения в любом из них будут применяться ко всем связанным."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    onLink(candidate.resource.id)
                }) { Text("Связать") }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun ResourceCandidateCard(
    candidate: ResourceLinkCandidate,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val res: DynamicContentBlock.Resource = candidate.resource
    val hasMax = res.max != "0" && res.max.isNotEmpty() && res.max.lowercase() != "null"
    val shape = RoundedCornerShape(12.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = shape
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    res.name,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                val first = candidate.usages.first()
                val extra = candidate.usages.size - 1
                Text(
                    if (extra > 0) "${first.label} и ещё $extra" else first.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }
            Text(
                if (hasMax) "${res.current}/${res.max}" else res.current,
                fontWeight = FontWeight.Black,
                color = colorScheme.primary
            )
        }
    }
}
