package ru.quasaris.characters.master.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.quasaris.characters.master.AttackEntry
import ru.quasaris.characters.master.Attribute
import ru.quasaris.characters.master.attacks.AttackBonusIndicator
import ru.quasaris.characters.master.attacks.AttackConfigDialog
import ru.quasaris.characters.master.attacks.DicePart
import ru.quasaris.characters.master.attacks.formatFullDamage
import ru.quasaris.characters.master.attacks.parseFormulaParts

@Composable
fun AttacksTab(
    attacks: List<AttackEntry>,
    proficiencyBonus: Int,
    attributeModifiers: Map<Attribute, Int>,
    onUpdateAttacks: (List<AttackEntry>) -> Unit
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
                        onClick = { editingAttack = attack }
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
            }
        )
    }
}

@Composable
fun AttackItem(
    attack: AttackEntry,
    proficiencyBonus: Int,
    attributeModifiers: Map<Attribute, Int>,
    onClick: () -> Unit
) {
    val attackCalculation = remember(attack, proficiencyBonus, attributeModifiers) {
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attack.name.ifBlank { "Безымянная атака" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$fullDamageText ${attack.damageType}".trim(),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                if (attack.showNotes && attack.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = attack.notes,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            
            AttackBonusIndicator(
                bonus = totalAttackBonus,
                dice = attackDice,
                size = 48.dp,
                fontSize = 18.sp,
                showLabel = false
            )
        }
    }
}
