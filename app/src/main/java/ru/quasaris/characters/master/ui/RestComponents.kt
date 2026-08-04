package ru.quasaris.characters.master.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Popup
import ru.quasaris.characters.master.R
import ru.quasaris.characters.master.HitDiceEntry
import ru.quasaris.characters.master.backend.AppScaleProvider
import ru.quasaris.characters.master.backend.LocalAppScale
import ru.quasaris.characters.master.backend.evaluateFormula
import ru.quasaris.characters.master.backend.calculateModifier
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.HazeInputScale
import java.util.UUID

data class RestRoll(
    val id: String = UUID.randomUUID().toString(),
    val hitDiceId: String?,
    val formula: String,
    val result: Int,
    val diceSpent: Int = 1
)

@Composable
fun RestPopup(
    onShortRest: () -> Unit,
    onLongRest: () -> Unit,
    onDawn: () -> Unit,
    onDismiss: () -> Unit,
    hazeState: HazeState? = null,
    isOled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val offsetX = with(density) { (-135).dp.roundToPx() }
    
    Popup(
        onDismissRequest = onDismiss,
        offset = androidx.compose.ui.unit.IntOffset(x = offsetX, y = 0),
        properties = androidx.compose.ui.window.PopupProperties(focusable = true)
    ) {
        AppScaleProvider(LocalAppScale.current) {
            val colorScheme = MaterialTheme.colorScheme

            Surface(
                modifier = modifier
                    .width(180.dp)
                    .shadow(8.dp, RoundedCornerShape(12.dp))
                    .run {
                        if (hazeState != null && !isOled) {
                            this.clip(RoundedCornerShape(12.dp))
                                .hazeEffect(state = hazeState) {
                                    style = HazeStyle(blurRadius = 24.dp, tints = listOf(HazeTint(colorScheme.surface.copy(alpha = 0.4f))))
                                    inputScale = HazeInputScale.Fixed(0.6f)
                                }
                        } else this
                    },
                shape = RoundedCornerShape(12.dp),
                color = if (isOled) Color.Black else if (hazeState != null) colorScheme.surface.copy(alpha = 0.4f) else colorScheme.surface,
                tonalElevation = 8.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = if (isOled) 0.3f else 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            onShortRest()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.WbSunny,
                            contentDescription = "Короткий отдых",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    VerticalDivider(modifier = Modifier.height(20.dp), color = colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    IconButton(
                        onClick = {
                            onDawn()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.WbTwilight,
                            contentDescription = "Рассвет",
                            tint = Color(0xFFCE93D8),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    VerticalDivider(modifier = Modifier.height(20.dp), color = colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    IconButton(
                        onClick = {
                            onLongRest()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Bedtime,
                            contentDescription = "Продолжительный отдых",
                            tint = Color(0xFF42A5F5),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RestPanel(
    hitDiceEntries: List<HitDiceEntry>,
    onHitDiceChange: (List<HitDiceEntry>) -> Unit,
    onHeal: (Int) -> Unit,
    onShortRestConfirmed: () -> Unit,
    onDismiss: () -> Unit,
    statsMap: Map<String, String>,
    defaultHitDie: Int = 8
) {
    val colorScheme = MaterialTheme.colorScheme
    var currentHitDice by remember(hitDiceEntries) { mutableStateOf(hitDiceEntries) }
    
    // Update dice formula if default Hit Die changes and entry is the default one
    LaunchedEffect(defaultHitDie) {
        if (currentHitDice.isEmpty()) {
            currentHitDice = listOf(HitDiceEntry(name = "Кости Хитов", formula = "[LVL]d$defaultHitDie"))
        } else {
            currentHitDice = currentHitDice.map { entry ->
                if (entry.name == "Кости Хитов" || entry.name.isEmpty()) {
                    val count = entry.formula.split('d').firstOrNull() ?: "[LVL]"
                    entry.copy(formula = "${count}d$defaultHitDie")
                } else entry
            }
        }
    }

    var restRolls by remember { mutableStateOf(listOf<RestRoll>()) }
    var showManualInput by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .background(colorScheme.surface, RoundedCornerShape(16.dp))
            .border(1.dp, colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Короткий отдых",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = colorScheme.primary
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, null)
            }
        }

        // History of rolls
        if (restRolls.isNotEmpty()) {
            Box(modifier = Modifier.heightIn(max = 150.dp)) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(restRolls.reversed(), key = { it.id }) { roll ->
                        Column {
                            AnimatedVisibility(
                                visible = true,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Surface(
                                    color = colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(roll.formula, style = MaterialTheme.typography.bodySmall)
                                            Text("+${roll.result} HP", fontWeight = FontWeight.Bold)
                                        }
                                        IconButton(onClick = {
                                            onHeal(-roll.result)
                                            if (roll.hitDiceId != null) {
                                                currentHitDice = currentHitDice.map { 
                                                    if (it.id == roll.hitDiceId) it.copy(spent = (it.spent - roll.diceSpent).coerceAtLeast(0))
                                                    else it
                                                }
                                            }
                                            restRolls = restRolls.filter { it.id != roll.id }
                                        }) {
                                            Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.3f))
        }

        currentHitDice.forEachIndexed { index, entry ->
            val maxHD = evaluateFormula(entry.formula.split('d').firstOrNull() ?: "0", statsMap)
            val dieSize = entry.formula.split('d').lastOrNull()?.toIntOrNull() ?: defaultHitDie
            val available = maxHD - entry.spent
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(entry.name.ifBlank { "Кости Хитов" }, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(4.dp))
                        val diceIcon = when (dieSize) {
                            2 -> R.drawable.ic_d2_dice
                            4 -> R.drawable.ic_d4_dice
                            6 -> R.drawable.ic_d6_dice
                            8 -> R.drawable.ic_d8_dice
                            10 -> R.drawable.ic_d10_dice
                            12 -> R.drawable.ic_d12_dice
                            20 -> R.drawable.ic_d20_dice
                            else -> R.drawable.ic_d20_dice
                        }
                        Icon(painterResource(diceIcon), null, modifier = Modifier.size(16.dp), tint = colorScheme.primary)
                    }
                    Text("$available / $maxHD доступно", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant)
                }
                
                Button(
                    onClick = {
                        val rollVal = (1..dieSize).random()
                        val conMod = calculateModifier(statsMap["constitution"] ?: "10")
                        val total = rollVal + conMod
                        
                        onHeal(total)
                        restRolls = restRolls + RestRoll(
                            hitDiceId = entry.id,
                            formula = "1d$dieSize + $conMod",
                            result = total
                        )
                        
                        currentHitDice = currentHitDice.toMutableList().also {
                            it[index] = entry.copy(spent = entry.spent + 1)
                        }
                    },
                    enabled = available > 0,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Бросок")
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { showManualInput = true }) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Ввести значения")
            }
            
            Button(
                onClick = {
                    onHitDiceChange(currentHitDice)
                    onShortRestConfirmed()
                },
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
            ) {
                Text("Завершить отдых")
            }
        }
    }

    if (showManualInput) {
        var hpText by remember { mutableStateOf("") }
        var diceToSpend by remember { mutableStateOf("1") }
        var selectedDiceIndex by remember { mutableIntStateOf(0) }

        AlertDialog(
            onDismissRequest = { showManualInput = false },
            title = { Text("Ручной ввод") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = hpText,
                        onValueChange = { hpText = it.filter { c -> c.isDigit() } },
                        label = { Text("Восстановлено HP") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (currentHitDice.size > 1) {
                        Text("Тип кости для списания", style = MaterialTheme.typography.labelMedium)
                        currentHitDice.forEachIndexed { i, entry ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selectedDiceIndex == i, onClick = { selectedDiceIndex = i })
                                Text(entry.name.ifBlank { "Кость ${i+1}" })
                            }
                        }
                    }

                    OutlinedTextField(
                        value = diceToSpend,
                        onValueChange = { diceToSpend = it.filter { c -> c.isDigit() } },
                        label = { Text("Списать костей") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val heal = hpText.toIntOrNull() ?: 0
                    val count = diceToSpend.toIntOrNull() ?: 0
                    if (heal > 0) onHeal(heal)
                    
                    if (count > 0 && currentHitDice.isNotEmpty()) {
                        val entry = currentHitDice[selectedDiceIndex]
                        val maxHD = evaluateFormula(entry.formula.split('d').firstOrNull() ?: "0", statsMap)
                        currentHitDice = currentHitDice.toMutableList().also {
                            it[selectedDiceIndex] = entry.copy(spent = (entry.spent + count).coerceAtMost(maxHD))
                        }
                        
                        restRolls = restRolls + RestRoll(
                            hitDiceId = entry.id,
                            formula = "Ручной ввод ($count КХ)",
                            result = heal,
                            diceSpent = count
                        )
                    }
                    showManualInput = false
                }) { Text("Применить") }
            },
            dismissButton = {
                TextButton(onClick = { showManualInput = false }) { Text("Отмена") }
            }
        )
    }
}
