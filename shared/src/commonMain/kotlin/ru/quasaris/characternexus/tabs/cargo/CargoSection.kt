package ru.quasaris.characternexus.tabs.cargo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.quasaris.characternexus.backend.CombatCalculations
import ru.quasaris.characternexus.model.*

@Composable
fun CargoSection(
    Cargo: CargoState,
    statsMap: Map<String, String>,
    isExpanded: Boolean,
    onEditClick: () -> Unit
) {
    if (!isExpanded) return

    val (carry, push) = CombatCalculations.calculateCargo(Cargo, statsMap)
    val jumps = CombatCalculations.calculateJumping(Cargo, statsMap)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Size and Settings Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.clickable { onEditClick() }) {
                Text(
                    text = if (Cargo.useOverrideSize) "Размер (расчёт: ${Cargo.overrideSize.displayName})" else "Размер существа",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = Cargo.size.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Настройки",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        // Weight Section
        CargoCard(
            title = "Грузоподъёмность",
            items = listOf(
                "Перенос груза" to "$carry фунтов",
                "Толкание / Подъём / Перетаскивание" to "$push фунтов"
            ),
            onClick = onEditClick
        )

        // Jumping Section
        CargoCard(
            title = "Прыжки",
            items = listOf(
                "В длину (с разбега / с места)" to "${jumps["runningLong"]} / ${jumps["standingLong"]} футов",
                "В высоту (с разбега / с места)" to "${jumps["runningHigh"]} / ${jumps["standingHigh"]} футов"
            ),
            onClick = onEditClick
        )
    }
}

@Composable
private fun CargoCard(
    title: String,
    items: List<Pair<String, String>>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            items.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = value,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
