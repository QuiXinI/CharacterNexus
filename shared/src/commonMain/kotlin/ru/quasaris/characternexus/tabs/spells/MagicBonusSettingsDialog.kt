package ru.quasaris.characternexus.tabs.spells

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import ru.quasaris.characternexus.ui.DialogDimStyle
import ru.quasaris.characternexus.ui.BackHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.tabs.attacks.AttackBonusField
import ru.quasaris.characternexus.tabs.attacks.AttackBonusIndicator
import ru.quasaris.characternexus.tabs.attacks.AddBonusButton
import ru.quasaris.characternexus.backend.DicePart
import ru.quasaris.characternexus.backend.parseFormulaParts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagicBonusSettingsDialog(
    title: String,
    bonuses: List<AttackBonus>,
    baseModifier: Int, // pb + mod
    attributeModifiers: Map<Attribute, Int> = emptyMap(),
    proficiencyBonus: Int = 0,
    stats: Map<String, String> = emptyMap(),
    onDismiss: () -> Unit,
    onSave: (List<AttackBonus>) -> Unit,
    forceBlurEnabled: Boolean = false
) {
    var currentBonuses by remember { mutableStateOf(bonuses) }

    val calculation = remember(currentBonuses, baseModifier, attributeModifiers, proficiencyBonus, stats) {
        var totalFlat = baseModifier
        val allDice = mutableMapOf<Int, Int>()
        
        currentBonuses.forEach { bonus ->
            val (fFlat, fDice) = parseFormulaParts(bonus.formula, stats)
            totalFlat += fFlat
            fDice.forEach { allDice[it.sides] = (allDice[it.sides] ?: 0) + it.count }
        }
        Pair(totalFlat, allDice.map { DicePart(it.value, it.key) }.sortedBy { it.sides })
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        DialogDimStyle(0f)
        BackHandler(onBack = onDismiss)
        val colorScheme = MaterialTheme.colorScheme
        val isOled = colorScheme.background == Color.Black

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(title, fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = if (forceBlurEnabled && !isOled) Color.Transparent.copy(alpha = 0.0f) else colorScheme.surface
                    )
                )
            },
            containerColor = if (forceBlurEnabled && !isOled) Color.Transparent.copy(alpha = 0.0f) else colorScheme.background
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Indicator
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        AttackBonusIndicator(
                            bonus = calculation.first,
                            dice = calculation.second,
                            showLabel = false
                        )
                    }

                    Text(
                        "ДОПОЛНИТЕЛЬНЫЕ БОНУСЫ",
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    currentBonuses.forEachIndexed { index, bonus ->
                        AttackBonusField(
                            bonus = bonus,
                            onUpdate = { updated ->
                                val newList = currentBonuses.toMutableList()
                                newList[index] = updated
                                currentBonuses = newList
                            },
                            onDelete = {
                                val newList = currentBonuses.toMutableList()
                                newList.removeAt(index)
                                currentBonuses = newList
                            }
                        )
                    }

                    AddBonusButton {
                        currentBonuses = currentBonuses + AttackBonus(advantagePreference = AdvantagePreference.NONE)
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }

                Button(
                    onClick = { onSave(currentBonuses) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Сохранить", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
