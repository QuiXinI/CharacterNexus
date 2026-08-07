package ru.quasaris.characters.master.tabs.spells

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.quasaris.characters.master.SpellCard
import ru.quasaris.characters.master.MaterialComponentType
import ru.quasaris.characters.master.MagicAttackType
import ru.quasaris.characters.master.CastingTimeType
import ru.quasaris.characters.master.DamageType
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import ru.quasaris.characters.master.SpellVersion
import ru.quasaris.characters.master.backend.AdvantageType
import ru.quasaris.characters.master.backend.DicePart
import ru.quasaris.characters.master.ui.DiceRollAdvantagePopup
import ru.quasaris.characters.master.tabs.attacks.AttackBonusIndicator
import ru.quasaris.characters.master.tabs.attacks.DiceIcon
import ru.quasaris.characters.master.tabs.attacks.formatFullDamage
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import dev.chrisbanes.haze.HazeState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpellCardItem(
    spell: SpellCard,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onEdit: () -> Unit = {},
    onRollDamage: (formula: String, title: String, AdvantageType) -> Unit = { _, _, _ -> },
    onRollAttack: (AdvantageType) -> Unit = {},
    isEditable: Boolean = false,
    statsMap: Map<String, String> = emptyMap(),
    isSelected: Boolean = false,
    onLongClick: () -> Unit = {},
    spellAttackBonus: Int = 0,
    spellAttackDice: List<DicePart> = emptyList(),
    spellSaveDc: Int = 0,
    spellSaveDice: List<DicePart> = emptyList(),
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    highlightRitual: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme
    
    var showAttackPopup by remember { mutableStateOf(false) }
    var attackBtnSize by remember { mutableStateOf(IntSize.Zero) }
    
    Card(
        modifier = modifier
            .combinedClickable(
                onClick = { onToggleExpand() },
                onLongClick = onLongClick
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp, 
                color = if (isSelected) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.1f), 
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> colorScheme.primary.copy(alpha = if (forceBlurEnabled) 0.15f else 0.1f)
                forceBlurEnabled -> colorScheme.surfaceVariant.copy(alpha = 0.15f)
                else -> colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = buildAnnotatedString {
                            append(spell.name)
                            if (spell.version != SpellVersion.NONE && !spell.name.contains(spell.version.displayName)) {
                                withStyle(SpanStyle(color = colorScheme.primary.copy(alpha = 0.6f), fontSize = 12.sp)) {
                                    append(" [${spell.version.displayName}]")
                                }
                            }
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (spell.showEnglishName && spell.englishName.isNotBlank()) {
                        Text(
                            text = spell.englishName,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (spell.isCircle) ComponentMarker("О")
                    if (spell.isRitual) ComponentMarker("Р", isHighlighted = highlightRitual)
                    if (spell.hasVerbalComponent) ComponentMarker("В")
                    if (spell.hasSomaticComponent) ComponentMarker("С")
                    val mText = when(spell.materialComponentType) {
                        MaterialComponentType.M -> "М"
                        MaterialComponentType.M_PLUS -> "М+"
                        MaterialComponentType.M_PLUS_PLUS -> "М++"
                        else -> null
                    }
                    if (mText != null) ComponentMarker(mText)
                    
                    if (isEditable) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val castingTimeText = if (spell.castingTimeType == CastingTimeType.OTHER && spell.castingTime.isNotBlank()) {
                    spell.castingTime
                } else {
                    spell.castingTimeType.displayName
                }

                if (castingTimeText.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = colorScheme.primary)
                        Text(
                            text = "Время наложения: $castingTimeText",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (spell.duration.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp), tint = colorScheme.primary)
                        Text(
                            text = "Длительность: ${spell.duration}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                if (spell.hasConcentration) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.SelfImprovement, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFE57373))
                        Text("Концентрация", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE57373))
                    }
                }
            }

            val hasActionArea = spell.hasDamage || spell.attackTypes.isNotEmpty()
            if (hasActionArea) {
                val isOled = colorScheme.background == Color.Black
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Left column: Damage buttons
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (spell.hasDamage && spell.damageFormula.isNotBlank()) {
                            SpellDamageButton(
                                formula = spell.damageFormula,
                                damageTypes = spell.damageTypes,
                                statsMap = statsMap,
                                title = "Урон: ${spell.name}",
                                onRoll = onRollDamage,
                                hazeState = hazeState,
                                isOled = isOled
                            )
                        }
                        spell.additionalDamageFormulas.forEachIndexed { index, formula ->
                            if (formula.isNotBlank()) {
                                SpellDamageButton(
                                    formula = formula,
                                    damageTypes = spell.additionalDamageTypesList.getOrNull(index) ?: emptyList(),
                                    statsMap = statsMap,
                                    title = "Доп. урон: ${spell.name}",
                                    onRoll = onRollDamage,
                                    hazeState = hazeState,
                                    isOled = isOled
                                )
                            }
                        }
                    }

                    // Right column: Attack/Save
                    if (spell.attackTypes.isNotEmpty()) {
                        val actionSize = 64.dp
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            if (spell.attackTypes.contains(MagicAttackType.ATTACK)) {
                                Surface(
                                    modifier = Modifier
                                        .size(actionSize)
                                        .onGloballyPositioned { coords -> attackBtnSize = coords.size }
                                        .combinedClickable(
                                            onClick = { onRollAttack(AdvantageType.NONE) },
                                            onLongClick = { showAttackPopup = true }
                                        ),
                                    color = colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        AttackBonusIndicator(
                                            bonus = spellAttackBonus,
                                            dice = spellAttackDice,
                                            size = 44.dp,
                                            fontSize = 17.sp,
                                            showLabel = false,
                                            showDice = true,
                                            useXForZero = true
                                        )

                                        if (showAttackPopup) {
                                            val density = LocalDensity.current
                                            val sizeDp = with(density) { attackBtnSize.toSize().let { DpSize((it.width / density.density).dp, (it.height / density.density).dp) } }
                                            DiceRollAdvantagePopup(
                                                onAdvantage = { onRollAttack(AdvantageType.ADVANTAGE) },
                                                onDisadvantage = { onRollAttack(AdvantageType.DISADVANTAGE) },
                                                onDismiss = { showAttackPopup = false },
                                                hazeState = hazeState,
                                                isOled = isOled,
                                                modifier = Modifier.size(sizeDp)
                                            )
                                        }
                                    }
                                }
                            }
                            
                            if (spell.attackTypes.contains(MagicAttackType.SAVE)) {
                                Surface(
                                    modifier = Modifier.size(actionSize),
                                    color = colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "Сложность",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colorScheme.primary,
                                            fontSize = 9.sp,
                                            maxLines = 1
                                        )
                                        
                                        Text(
                                            text = if (spellSaveDc > 0) spellSaveDc.toString() else "X",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 18.sp
                                        )

                                        val attrText = spell.savingThrowAttributes.joinToString("/") { it.shortName }
                                        Text(
                                            text = attrText.ifBlank { "—" },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            fontSize = 9.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = colorScheme.onSurface.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (spell.materialComponentType != MaterialComponentType.NONE && spell.materialComponents.isNotBlank()) {
                        val mPrefix = when(spell.materialComponentType) {
                            MaterialComponentType.M_PLUS -> "М+ : "
                            MaterialComponentType.M_PLUS_PLUS -> "М++ : "
                            else -> "М : "
                        }
                        Text(
                            text = "$mPrefix${spell.materialComponents}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    
                    if (spell.distance.isNotBlank()) {
                        Text(
                            text = "Дистанция: ${spell.distance}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    if (spell.castingTimeType == CastingTimeType.REACTION || 
                        ((spell.castingTimeType == CastingTimeType.ACTION || spell.castingTimeType == CastingTimeType.BONUS_ACTION) && spell.castingTime.isNotBlank())) {
                        val prefix = spell.castingTimeType.displayName
                        val combinedText = if (spell.castingTime.isNotBlank()) {
                            val firstChar = spell.castingTime.firstOrNull()
                            val separator = if (firstChar != null && !firstChar.isLetterOrDigit()) "" else " "
                            "$prefix$separator${spell.castingTime}"
                        } else {
                            prefix
                        }
                        Text(
                            text = combinedText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    
                    Text(
                        text = spell.description,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                    
                    if (spell.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = colorScheme.primary.copy(alpha = 0.05f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = spell.notes,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp)
                                    .fillMaxWidth()
                            )
                        }
                    }

                    if (!spell.link.isNullOrBlank()) {
                        SpellLinkItem(name = "Основная ссылка", url = spell.link)
                    }

                    spell.additionalLinks.forEach { addLink ->
                        if (addLink.url.isNotBlank()) {
                            SpellLinkItem(name = addLink.name.ifBlank { "Доп. ссылка" }, url = addLink.url)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpellDamageButton(
    formula: String,
    damageTypes: List<DamageType>,
    statsMap: Map<String, String>,
    title: String,
    onRoll: (String, String, AdvantageType) -> Unit,
    hazeState: HazeState? = null,
    isOled: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme
    var showPopup by remember { mutableStateOf(false) }
    var btnSize by remember { mutableStateOf(IntSize.Zero) }

    val fullDamageText = remember(formula, damageTypes, statsMap) {
        val typesText = damageTypes.joinToString("/") { it.displayName }
        val baseDamage = formatFullDamage(
            baseFormula = formula,
            baseDamageBonus = 0,
            bonuses = emptyList(),
            stats = statsMap
        )
        if (typesText.isNotBlank()) "$baseDamage $typesText".trim() else baseDamage.trim()
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords -> btnSize = coords.size }
            .combinedClickable(
                onClick = { onRoll(formula, title, AdvantageType.NONE) },
                onLongClick = { showPopup = true }
            ),
        color = colorScheme.primary.copy(alpha = 0.08f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.Whatshot, contentDescription = null, modifier = Modifier.size(16.dp), tint = colorScheme.primary)
                Text(
                    text = fullDamageText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface
                )
            }

            if (showPopup) {
                val density = LocalDensity.current
                val sizeDp = with(density) { btnSize.toSize().let { DpSize((it.width / density.density).dp, (it.height / density.density).dp) } }
                DiceRollAdvantagePopup(
                    onAdvantage = { onRoll(formula, title, AdvantageType.ADVANTAGE) },
                    onDisadvantage = { onRoll(formula, title, AdvantageType.DISADVANTAGE) },
                    onCritical = { onRoll(formula, title, AdvantageType.CRITICAL) },
                    onDismiss = { showPopup = false },
                    hazeState = hazeState,
                    isOled = isOled,
                    modifier = Modifier.size(sizeDp)
                )
            }
        }
    }
}

@Composable
fun SpellLinkItem(name: String, url: String) {
    val context = LocalContext.current
    TextButton(
        onClick = {
            try {
                val formatted = if (!url.startsWith("http")) "https://$url" else url
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(formatted)))
            } catch (e: Exception) {}
        },
        modifier = Modifier.height(32.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(name, fontSize = 12.sp)
    }
}

@Composable
fun ComponentMarker(text: String, isHighlighted: Boolean = false) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = if (isHighlighted) colorScheme.primary else colorScheme.primary.copy(alpha = 0.1f)
    val contentColor = if (isHighlighted) colorScheme.onPrimary else colorScheme.primary

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.height(24.dp).widthIn(min = 24.dp).run {
            if (isHighlighted) border(1.dp, colorScheme.primaryContainer, RoundedCornerShape(4.dp)) else this
        }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = contentColor)
        }
    }
}
