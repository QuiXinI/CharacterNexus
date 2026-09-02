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
import dev.chrisbanes.haze.*
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
import ru.quasaris.characternexus.ui.theme.rememberEffectiveBlurRadius

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
    forceBlurEnabled: Boolean = false,
    isDesktop: Boolean = false,
    hazeState: HazeState? = null,
    popupHazeState: HazeState? = null,
    settingsViewModel: ru.quasaris.characternexus.backend.SettingsViewModel? = null,
    isNested: Boolean = false,
    asOverlay: Boolean = false
) {
    var currentBonuses by remember { mutableStateOf(bonuses) }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val blurRadius = rememberEffectiveBlurRadius(settingsViewModel)
    
    val handleDismiss = {
        focusManager.clearFocus()
        onDismiss()
    }
    
    val handleSave = {
        focusManager.clearFocus()
        onSave(currentBonuses)
    }

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

    if (isDesktop || asOverlay) {
        MagicBonusSettingsContent(
            title = title,
            calculation = calculation,
            currentBonuses = currentBonuses,
            onBonusesChange = { currentBonuses = it },
            onDismiss = handleDismiss,
            onSave = handleSave,
            forceBlurEnabled = forceBlurEnabled,
            hazeState = hazeState,
            blurRadius = blurRadius,
            isNested = isNested,
            asOverlay = asOverlay,
            settingsViewModel = settingsViewModel
        )
    } else {
        Dialog(
            onDismissRequest = handleDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = !isNested)
        ) {
            DialogDimStyle(if (isNested) 0.5f else 0f)
            MagicBonusSettingsContent(
                title = title,
                calculation = calculation,
                currentBonuses = currentBonuses,
                onBonusesChange = { currentBonuses = it },
                onDismiss = handleDismiss,
                onSave = handleSave,
                forceBlurEnabled = forceBlurEnabled,
                hazeState = popupHazeState ?: hazeState,
                blurRadius = blurRadius,
                isNested = isNested,
                isDesktop = isDesktop,
                settingsViewModel = settingsViewModel
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagicBonusSettingsContent(
    title: String,
    calculation: Pair<Int, List<DicePart>>,
    currentBonuses: List<AttackBonus>,
    onBonusesChange: (List<AttackBonus>) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    forceBlurEnabled: Boolean,
    hazeState: HazeState?,
    blurRadius: androidx.compose.ui.unit.Dp = 24.dp,
    isNested: Boolean = false,
    isDesktop: Boolean = false,
    asOverlay: Boolean = false,
    settingsViewModel: ru.quasaris.characternexus.backend.SettingsViewModel? = null
) {
    BackHandler(onBack = onDismiss)
    val colorScheme = MaterialTheme.colorScheme
    val isOled = colorScheme.background == Color.Black
    val masterBlurEnabled by settingsViewModel?.masterBlurEnabled?.collectAsState() ?: remember { mutableStateOf(true) }

    if (asOverlay) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(title, fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = if (masterBlurEnabled) Color.Transparent else colorScheme.surface
                    )
                )
            },
            containerColor = if (masterBlurEnabled) Color.Transparent else colorScheme.background
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
            MagicBonusSettingsInner(
                calculation = calculation,
                currentBonuses = currentBonuses,
                onBonusesChange = onBonusesChange,
                onSave = onSave
            )
            }
        }
    } else if (isNested && !isDesktop) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(title, fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = colorScheme.surface
                    )
                )
            },
            containerColor = colorScheme.background
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
            MagicBonusSettingsInner(
                calculation = calculation,
                currentBonuses = currentBonuses,
                onBonusesChange = onBonusesChange,
                onSave = onSave
            )
            }
        }
    } else {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .run {
                    if (forceBlurEnabled && hazeState != null && !isOled && !isNested) {
                        this.hazeEffect(state = hazeState) {
                            style = HazeStyle(
                                blurRadius = blurRadius,
                                tints = listOf(HazeTint(Color.Black.copy(alpha = 0.2f)))
                            )
                        }
                    } else this
                },
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(title, fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = if (forceBlurEnabled && !isOled && hazeState != null && !isNested) Color.Transparent.copy(alpha = 0.0f) else colorScheme.surface
                    )
                )
            },
            containerColor = if (forceBlurEnabled && !isOled && hazeState != null && !isNested) Color.Transparent.copy(alpha = 0.0f) else colorScheme.background
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            MagicBonusSettingsInner(
                calculation = calculation,
                currentBonuses = currentBonuses,
                onBonusesChange = onBonusesChange,
                onSave = onSave
            )
            }
        }
    }
}

@Composable
fun MagicBonusSettingsInner(
    calculation: Pair<Int, List<DicePart>>,
    currentBonuses: List<AttackBonus>,
    onBonusesChange: (List<AttackBonus>) -> Unit,
    onSave: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
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
                        onBonusesChange(newList)
                    },
                    onDelete = {
                        val newList = currentBonuses.toMutableList()
                        newList.removeAt(index)
                        onBonusesChange(newList)
                    }
                )
            }

            AddBonusButton {
                onBonusesChange(currentBonuses + AttackBonus(advantagePreference = AdvantagePreference.NONE))
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        Button(
            onClick = onSave,
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
