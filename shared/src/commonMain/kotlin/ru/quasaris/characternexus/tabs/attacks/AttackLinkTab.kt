package ru.quasaris.characternexus.tabs.attacks

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
import ru.quasaris.characternexus.model.AttackEntry

data class AttackUsage(
    val label: String
)

data class AttackLinkCandidate(
    val attack: AttackEntry,
    val usages: List<AttackUsage>
)

data class AttackLinkConfig(
    val candidates: List<AttackLinkCandidate>,
    val sharedWith: List<AttackUsage>,
    val onLink: (targetId: String) -> Unit,
    val onUnlink: () -> Unit
)

fun buildAttackLinkConfig(
    currentAttack: AttackEntry,
    existingAttacks: List<AttackEntry>,
    attacksTabIds: List<String>?,
    editingIndex: Int = -1,
    onLink: (targetId: String) -> Unit,
    onUnlink: () -> Unit
): AttackLinkConfig {
    val tabIds = attacksTabIds ?: existingAttacks.map { it.id }

    val currentOccurrences = tabIds.indices.filter { tabIds[it] == currentAttack.id }

    val activeIndex = if (editingIndex in currentOccurrences) editingIndex
    else currentOccurrences.firstOrNull() ?: -1

    val sharedWith = mutableListOf<AttackUsage>()
    currentOccurrences
        .filter { it != activeIndex }
        .forEach { index ->
            sharedWith.add(AttackUsage(label = "Вкладка атак (позиция ${index + 1})"))
        }

    if (currentAttack.linkedTo != null) {
        val primaryAtk = existingAttacks.find { it.id == currentAttack.linkedTo }
        sharedWith.add(AttackUsage(label = "Связана с атакой «${primaryAtk?.name ?: "Главная атака"}»"))
    }

    if (currentAttack.linkedIds.isNotEmpty()) {
        currentAttack.linkedIds.forEach { secId ->
            val secAtk = existingAttacks.find { it.id == secId }
            sharedWith.add(AttackUsage(label = "Связана с ведомой атакой «${secAtk?.name ?: "Атака"}»"))
        }
    }

    val candidates = existingAttacks
        .filter { it.id != currentAttack.id }
        .map { candidateAttack ->
            val usagesCount = tabIds.count { it == candidateAttack.id }
            val candidateUsages = if (usagesCount > 1) {
                listOf(AttackUsage(label = "На вкладке атак ($usagesCount места)"))
            } else if (usagesCount == 1) {
                listOf(AttackUsage(label = "На вкладке атак"))
            } else emptyList()

            AttackLinkCandidate(
                attack = candidateAttack,
                usages = candidateUsages
            )
        }
        .sortedBy { it.attack.name.lowercase() }

    return AttackLinkConfig(
        candidates = candidates,
        sharedWith = sharedWith,
        onLink = onLink,
        onUnlink = onUnlink
    )
}

@Composable
fun AttackLinkTab(
    config: AttackLinkConfig,
    onLink: (String) -> Unit,
    onUnlink: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<AttackLinkCandidate?>(null) }

    val filtered = remember(config.candidates, query) {
        val q = query.trim()
        if (q.isEmpty()) config.candidates
        else config.candidates.filter { it.attack.name.contains(q, ignoreCase = true) }
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
                    Text("Связана с другими атаками", fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
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
                "Связать с существующей атакой",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
            Text(
                "Название, урон, параметры и характеристики станут общими: изменение в одном месте сразу появится во всех связанных.",
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
                if (config.candidates.isEmpty()) "Других атак в этом листе пока нет." else "Ничего не найдено.",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
        }

        filtered.forEach { candidate ->
            AttackCandidateCard(candidate = candidate, onClick = { pending = candidate })
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    pending?.let { candidate ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Связать с «${candidate.attack.name.ifBlank { "Безымянная атака" }}»?") },
            text = {
                Text(
                    "Все данные этой атаки (название, урон, бонусы и настройки) будут заменены данными выбранной атаки. " +
                        "В дальнейшем изменения в любой из них будут применяться ко всем связанным."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    onLink(candidate.attack.id)
                }) { Text("Связать") }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun AttackCandidateCard(
    candidate: AttackLinkCandidate,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val atk = candidate.attack
    val shape = RoundedCornerShape(12.dp)

    val damageText = buildString {
        if (atk.damageFormula.isNotBlank()) {
            append(atk.damageFormula)
        }
        if (atk.damageType.isNotBlank()) {
            if (isNotEmpty()) append(" ")
            append(atk.damageType)
        }
    }.ifBlank { "Без урона" }

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
                    atk.name.ifBlank { "Безымянная атака" },
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                if (candidate.usages.isNotEmpty()) {
                    val first = candidate.usages.first()
                    val extra = candidate.usages.size - 1
                    Text(
                        if (extra > 0) "${first.label} и ещё $extra" else first.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "В общем списке атак",
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                damageText,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.primary
            )
        }
    }
}
