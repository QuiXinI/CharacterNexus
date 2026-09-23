package ru.quasaris.characternexus.tabs.cargo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.chrisbanes.haze.HazeState
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.ui.BackHandler
import ru.quasaris.characternexus.ui.DialogDimStyle
import ru.quasaris.characternexus.ui.theme.rememberEffectiveBlurRadius
import ru.quasaris.characternexus.ui.theme.hazePopover
import ru.quasaris.characternexus.tabs.attacks.AddBonusButton
import ru.quasaris.characternexus.tabs.attacks.SectionHeader
import ru.quasaris.characternexus.tabs.BonusField

@Composable
fun CargoConfigDialog(
    Cargo: CargoState,
    onDismiss: () -> Unit,
    onSave: (CargoState) -> Unit,
    hazeState: HazeState? = null,
    popupHazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    settingsViewModel: ru.quasaris.characternexus.backend.SettingsViewModel? = null,
    isDesktop: Boolean = false
) {
    var state by remember { mutableStateOf(Cargo) }
    
    LaunchedEffect(state) {
        if (state != Cargo) {
            onSave(state)
        }
    }
    
    val blurRadius = rememberEffectiveBlurRadius(settingsViewModel)
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    val handleDismiss = {
        focusManager.clearFocus()
        onDismiss()
    }

    if (isDesktop) {
        CargoConfigDialogContent(
            state = state,
            onStateChange = { state = it },
            onDismiss = handleDismiss,
            hazeState = hazeState,
            forceBlurEnabled = forceBlurEnabled,
            blurRadius = blurRadius
        )
    } else {
        Dialog(
            onDismissRequest = handleDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            DialogDimStyle(0f)
            CargoConfigDialogContent(
                state = state,
                onStateChange = { state = it },
                onDismiss = handleDismiss,
                hazeState = popupHazeState ?: hazeState,
                forceBlurEnabled = forceBlurEnabled,
                blurRadius = blurRadius
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CargoConfigDialogContent(
    state: CargoState,
    onStateChange: (CargoState) -> Unit,
    onDismiss: () -> Unit,
    hazeState: HazeState?,
    forceBlurEnabled: Boolean,
    blurRadius: androidx.compose.ui.unit.Dp
) {
    val colorScheme = MaterialTheme.colorScheme
    val isOled = colorScheme.background == Color.Black

    // Единое условие для модификатора и для прозрачности контейнеров —
    // раньше они были рассинхронизированы: hazeEffect навешивался только при
    // hazeState != null, а прозрачность топбара/фона включалась и без него,
    // из-за чего на десктопе (где hazeState часто null) диалог оставался
    // прозрачным БЕЗ реального блюра под ним.
    val isBlurred = forceBlurEnabled && hazeState != null && !isOled

    BackHandler(onBack = onDismiss)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .hazePopover(
                state = hazeState,
                blurRadius = blurRadius,
                forceBlurEnabled = forceBlurEnabled,
                isOled = isOled
            ),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Настройка существа", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isBlurred) Color.Transparent.copy(alpha = 0.0f) else colorScheme.surface
                )
            )
        },
        containerColor = if (isBlurred) Color.Transparent.copy(alpha = 0.0f) else colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // --- SIZE SECTION ---
                SectionHeader("Размер существа")
                
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onStateChange(state.copy(useOverrideSize = !state.useOverrideSize)) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Переопределить размер для расчётов", fontWeight = FontWeight.Medium)
                    Switch(
                        checked = state.useOverrideSize,
                        onCheckedChange = { onStateChange(state.copy(useOverrideSize = it)) }
                    )
                }

                if (state.useOverrideSize) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.fillMaxWidth(0.5f)) {
                            Text(
                                "Размер персонажа",
                                fontSize = 12.sp,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            CreatureSizeSelector(
                                selected = state.size,
                                onSelect = { onStateChange(state.copy(size = it)) }
                            )
                        }
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "Размер для расчётов",
                                fontSize = 12.sp,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            CreatureSizeSelector(
                                selected = state.overrideSize,
                                onSelect = { onStateChange(state.copy(overrideSize = it)) }
                            )
                        }
                    }
                } else {
                    CreatureSizeSelector(
                        selected = state.size,
                        onSelect = { onStateChange(state.copy(size = it)) }
                    )
                }

                // --- CARRY WEIGHT SECTION ---
                SectionHeader("Грузоподъёмность")
                AttributeSelector(
                    label = "Характеристика для веса",
                    selected = state.carryAttribute,
                    onSelect = { onStateChange(state.copy(carryAttribute = it)) }
                )
                
                Text("Бонусы к переносу груза", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                BonusList(
                    bonuses = state.carryBonuses,
                    onUpdate = { newList -> onStateChange(state.copy(carryBonuses = newList)) }
                )

                Text("Бонусы к толканию / подъёму / перетаскиванию", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                BonusList(
                    bonuses = state.pushBonuses,
                    onUpdate = { newList -> onStateChange(state.copy(pushBonuses = newList)) }
                )

                // JUMPING SECTION
                SectionHeader("Прыжки")
                AttributeSelector(
                    label = "Характеристика для прыжков",
                    selected = state.jumpAttribute,
                    onSelect = { onStateChange(state.copy(jumpAttribute = it)) }
                )

                Text("Бонусы к прыжку в длину", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                BonusList(
                    bonuses = state.longJumpBonuses,
                    onUpdate = { newList -> onStateChange(state.copy(longJumpBonuses = newList)) }
                )

                Text("Бонусы к прыжку в высоту", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                BonusList(
                    bonuses = state.highJumpBonuses,
                    onUpdate = { newList -> onStateChange(state.copy(highJumpBonuses = newList)) }
                )

                Spacer(modifier = Modifier.height(80.dp))
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.compositeOver(MaterialTheme.colorScheme.background),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp
                )
            ) {
                Text("Закрыть", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun CreatureSizeSelector(
    selected: CreatureSize,
    onSelect: (CreatureSize) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CreatureSize.entries.forEach { size ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (selected == size) MaterialTheme.colorScheme.primaryContainer 
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                    .clickable { onSelect(size) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selected == size, onClick = { onSelect(size) })
                Spacer(Modifier.width(8.dp))
                Text(size.displayName)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttributeSelector(
    label: String,
    selected: Attribute,
    onSelect: (Attribute) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected.fullName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Attribute.entries.filter { it != Attribute.NONE }.forEach { attr ->
                DropdownMenuItem(
                    text = { Text(attr.fullName) },
                    onClick = {
                        onSelect(attr)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun BonusList(
    bonuses: List<SimpleBonus>,
    onUpdate: (List<SimpleBonus>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        bonuses.forEach { bonus ->
            BonusField(
                bonus = bonus,
                onUpdate = { n, f, act, op, adv ->
                    onUpdate(bonuses.map {
                        if (it.id == bonus.id) it.copy(name = n, formula = f, isActive = act, operation = op, advantagePreference = adv) else it
                    })
                },
                onDelete = {
                    onUpdate(bonuses.filter { it.id != bonus.id })
                }
            )
        }
        AddBonusButton {
            onUpdate(bonuses + SimpleBonus(name = "Бонус"))
        }
    }
}
