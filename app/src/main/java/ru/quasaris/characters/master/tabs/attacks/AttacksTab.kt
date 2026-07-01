package ru.quasaris.characters.master.tabs.attacks

import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import ru.quasaris.characters.master.AttackEntry
import ru.quasaris.characters.master.Attribute
import ru.quasaris.characters.master.backend.DiceRoller
import ru.quasaris.characters.master.backend.RollResult
import ru.quasaris.characters.master.backend.RollSourceType
import androidx.compose.foundation.shape.RoundedCornerShape
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeStyle

import androidx.compose.ui.draw.clip
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.LocalHazeStyle
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Стиль размытия для инфо-панелей атак.
 */
val AttackInfoHazeStyle = HazeStyle(
    blurRadius = 20.dp,
    tints = listOf(HazeTint(Color.Black.copy(alpha = 0.2f)))
)

@Composable
fun AttacksTab(
    attacks: List<AttackEntry>,
    proficiencyBonus: Int,
    attributeModifiers: Map<Attribute, Int>,
    onUpdateAttacks: (List<AttackEntry>) -> Unit,
    onRoll: (RollResult) -> Unit = {},
    stats: Map<String, String> = emptyMap(),
    exhaustion: Int = 0,
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false
) {
    var editingAttack by remember { mutableStateOf<AttackEntry?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (attacks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Список атак пуст", 
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(attacks, key = { it.id }) { attack ->
                    AttackItem(
                        attack = attack,
                        proficiencyBonus = proficiencyBonus,
                        attributeModifiers = attributeModifiers,
                        onClick = { editingAttack = attack },
                        onRoll = onRoll,
                        stats = stats,
                        exhaustion = exhaustion,
                        hazeState = hazeState,
                        forceBlurEnabled = forceBlurEnabled
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { editingAttack = AttackEntry() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Добавить атаку")
        }
    }

    editingAttack?.let { attack ->
        AttackConfigDialog(
            attack = attack,
            proficiencyBonus = proficiencyBonus,
            attributeModifiers = attributeModifiers,
            onDismiss = { editingAttack = null },
            onSave = { updatedAttack ->
                val newAttacks = if (attacks.any { it.id == updatedAttack.id }) {
                    attacks.map { if (it.id == updatedAttack.id) updatedAttack else it }
                } else {
                    attacks + updatedAttack
                }
                onUpdateAttacks(newAttacks)
                editingAttack = null
            },
            onDelete = { attackToDelete ->
                onUpdateAttacks(attacks.filter { it.id != attackToDelete.id })
                editingAttack = null
            },
            hazeState = hazeState,
            forceBlurEnabled = forceBlurEnabled
        )
    }
}

@Composable
fun AttackItem(
    attack: AttackEntry,
    proficiencyBonus: Int,
    attributeModifiers: Map<Attribute, Int>,
    onClick: () -> Unit,
    onRoll: (RollResult) -> Unit = {},
    stats: Map<String, String> = emptyMap(),
    exhaustion: Int = 0,
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false
) {
    val attackCalculation = remember(attack, proficiencyBonus, attributeModifiers) {
        if (attack.attribute == Attribute.NONE) {
            return@remember Pair(0, emptyList<DicePart>())
        }
        val attrMod = attributeModifiers[attack.attribute] ?: 0
        val prof = if (attack.isProficient) proficiencyBonus else 0
        var totalFlat = attrMod + prof + attack.attackBonus
        val allDice = mutableMapOf<Int, Int>()
        
        attack.attackBonuses.forEach { bonus ->
            val (fFlat, fDice) = parseFormulaParts(bonus.formula, attributeModifiers, proficiencyBonus)
            totalFlat += fFlat
            fDice.forEach { allDice[it.sides] = (allDice[it.sides] ?: 0) + it.count }
        }
        Pair(totalFlat, allDice.map { DicePart(it.value, it.key) }.sortedBy { it.sides })
    }
    
    val totalAttackBonus = attackCalculation.first
    val attackDice = attackCalculation.second

    val fullDamageText = formatFullDamage(
        baseFormula = attack.damageFormula,
        baseDamageBonus = attack.damageBonus,
        bonusFormulas = attack.damageBonuses.map { it.formula },
        attributeModifiers = attributeModifiers,
        proficiencyBonus = proficiencyBonus
    )

    var showInfo by remember { mutableStateOf(false) }
    var iconPosition by remember { mutableStateOf(Offset.Zero) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = attack.name.ifBlank { "Безымянная атака" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Box {
                    IconButton(
                        onClick = { showInfo = true },
                        modifier = Modifier
                            .size(32.dp)
                            .onGloballyPositioned { coordinates ->
                                iconPosition = coordinates.positionInWindow()
                            }
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Описание",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    }

                    if (showInfo) {
                        val colorScheme = MaterialTheme.colorScheme
                        val isOled = colorScheme.background == Color.Black
                        
                        Dialog(
                            onDismissRequest = { showInfo = false },
                            properties = DialogProperties(
                                usePlatformDefaultWidth = false,
                                dismissOnBackPress = true,
                                dismissOnClickOutside = true
                            )
                        ) {
                            val view = LocalView.current
                            val window = (view.parent as? DialogWindowProvider)?.window
                            
                            SideEffect {
                                window?.let { w ->
                                    w.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
                                    w.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                                    w.addFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH)
                                    w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                                    w.setDimAmount(0f)
                                    
                                    val params = w.attributes
                                    params.width = WindowManager.LayoutParams.WRAP_CONTENT
                                    params.height = WindowManager.LayoutParams.WRAP_CONTENT
                                    
                                    // Position precisely to the left of the button
                                    params.gravity = Gravity.TOP or Gravity.END
                                    val screenWidth = view.context.resources.displayMetrics.widthPixels
                                    params.x = (screenWidth - iconPosition.x).toInt() + 8 // 8px margin from icon
                                    params.y = iconPosition.y.toInt() - 16 // Slight upward offset for better alignment
                                    
                                    w.attributes = params

                                    w.decorView.setOnTouchListener { _, event ->
                                        if (event.action == MotionEvent.ACTION_OUTSIDE) {
                                            showInfo = false
                                        }
                                        false
                                    }

                                    if (!isOled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        w.setBackgroundBlurRadius(80)
                                    }
                                    w.setBackgroundDrawableResource(android.R.color.transparent)
                                }
                            }
                            
                            CompositionLocalProvider(LocalHazeStyle provides AttackInfoHazeStyle) {
                                Surface(
                                    modifier = Modifier
                                        .padding(8.dp) // Smaller padding for "popover" look
                                        .widthIn(max = 260.dp)
                                        .run {
                                            if (forceBlurEnabled && hazeState != null && !isOled) {
                                                this.clip(RoundedCornerShape(16.dp))
                                                    .hazeEffect(state = hazeState) {
                                                        inputScale = HazeInputScale.Fixed(0.6f)
                                                    }
                                            } else this
                                        }
                                        .clickable { showInfo = false },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isOled) Color.Black else colorScheme.surface.copy(alpha = if (forceBlurEnabled) 0.4f else 0.85f),
                                    tonalElevation = 8.dp,
                                    border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = if (isOled) 0.3f else 0.15f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = attack.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = colorScheme.primary,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        Text(
                                            text = attack.notes.ifBlank { "Нет описания" },
                                            fontSize = 14.sp,
                                            lineHeight = 18.sp,
                                            color = colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Damage Section
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            val damageFlat = attack.damageBonus
                            val bonusFormulas = attack.damageBonuses.map { it.formula } + attack.damageFormula
                            onRoll(DiceRoller.roll(
                                title = "Урон: ${attack.name}",
                                baseModifier = damageFlat,
                                bonusFormulas = bonusFormulas,
                                isDamage = true,
                                stats = stats,
                                sourceType = RollSourceType.ATTACK
                            ))
                        },
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$fullDamageText ${attack.damageType}".trim(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Modifier Section
                if (attack.attribute != Attribute.NONE) {
                    Surface(
                        modifier = Modifier
                            .clickable {
                                onRoll(DiceRoller.roll(
                                    title = "Атака: ${attack.name}",
                                    baseModifier = totalAttackBonus,
                                    bonusFormulas = attack.attackBonuses.map { it.formula },
                                    isDamage = false,
                                    stats = stats,
                                    exhaustion = exhaustion,
                                    sourceType = RollSourceType.ATTACK
                                ))
                            },
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AttackBonusIndicator(
                                bonus = totalAttackBonus,
                                dice = attackDice,
                                size = 48.dp,
                                fontSize = 18.sp,
                                showLabel = false,
                                showDice = false
                            )
                            
                            if (attackDice.isNotEmpty()) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    attackDice.forEach { DiceIcon(it) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
