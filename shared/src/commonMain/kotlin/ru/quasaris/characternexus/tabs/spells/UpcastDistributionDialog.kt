package ru.quasaris.characternexus.tabs.spells

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ru.quasaris.characternexus.ui.outerShadow

@Composable
fun UpcastDistributionDialog(
    spellName: String,
    totalUpcastPoints: Int,
    damageFields: List<Triple<String, String, String>>, // Formula, Title, UpcastFormula
    onDismiss: () -> Unit,
    onConfirm: (List<Int>) -> Unit
) {
    var distributions by remember { mutableStateOf(List(damageFields.size) { 0 }) }
    val spentPoints = distributions.sum()
    val colorScheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(spellName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Очков апкаста: $totalUpcastPoints",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.primary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                damageFields.forEachIndexed { index, field ->
                    Surface(
                        color = colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(field.second, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = {
                                        if (distributions[index] > 0) {
                                            val newList = distributions.toMutableList()
                                            newList[index]--
                                            distributions = newList
                                        }
                                    },
                                    enabled = distributions[index] > 0,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Remove, null)
                                }

                                Surface(
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                    color = colorScheme.surface.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outlineVariant)
                                ) {
                                    val finalFormula = remember(distributions[index]) {
                                        var f = field.first
                                        repeat(distributions[index]) { f += " + ${field.third}" }
                                        f
                                    }
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                        Text(
                                            text = "${distributions[index]} : $finalFormula",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (distributions[index] > 0) colorScheme.primary else colorScheme.onSurface
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        if (spentPoints < totalUpcastPoints) {
                                            val newList = distributions.toMutableList()
                                            newList[index]++
                                            distributions = newList
                                        }
                                    },
                                    enabled = spentPoints < totalUpcastPoints,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, null)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Не распределено: ${totalUpcastPoints - spentPoints}",
                        fontWeight = FontWeight.Bold,
                        color = if (spentPoints == totalUpcastPoints) Color(0xFF00C46F) else colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(distributions) },
                enabled = spentPoints == totalUpcastPoints
            ) {
                Text("Бросить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
