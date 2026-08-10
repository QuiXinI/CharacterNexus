package ru.quasaris.characters.master.tabs.spells

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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.HazeInputScale
import ru.quasaris.characters.master.AdvantagePreference
import ru.quasaris.characters.master.AttackBonus
import ru.quasaris.characters.master.Attribute
import ru.quasaris.characters.master.tabs.attacks.AttackBonusField
import ru.quasaris.characters.master.tabs.attacks.AttackBonusIndicator
import ru.quasaris.characters.master.tabs.attacks.AddBonusButton
import ru.quasaris.characters.master.backend.DicePart
import ru.quasaris.characters.master.backend.parseFormulaParts

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
    hazeState: HazeState? = null,
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
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.setDimAmount(0f)
        }
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
                        containerColor = if (forceBlurEnabled && !isOled) Color.Transparent else colorScheme.surface
                    )
                )
            },
            containerColor = if (forceBlurEnabled && !isOled) Color.Transparent else colorScheme.background
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
