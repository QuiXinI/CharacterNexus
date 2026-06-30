package ru.quasaris.characters.master.tabs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import ru.quasaris.characters.master.R
import kotlin.math.floor
import ru.quasaris.characters.master.Attribute
import ru.quasaris.characters.master.HeaderCode.calculateModifier
import ru.quasaris.characters.master.Character
import ru.quasaris.characters.master.MainWindow.AttributesSection
import ru.quasaris.characters.master.HeaderCode.getProficiencyBonus

data class StatsState(
    val strength: String = "10",
    val dexterity: String = "10",
    val constitution: String = "10",
    val intelligence: String = "10",
    val wisdom: String = "10",
    val charisma: String = "10",
    val strProf: Boolean = false,
    val dexProf: Boolean = false,
    val conProf: Boolean = false,
    val intProf: Boolean = false,
    val wisProf: Boolean = false,
    val chaProf: Boolean = false,
    val skilledProficiencies: List<String> = emptyList(),
    val skilledExpertise: List<String> = emptyList()
) {
    fun toStatsMap(level: String) = mapOf(
        "strength" to strength, "dexterity" to dexterity, "constitution" to constitution,
        "intelligence" to intelligence, "wisdom" to wisdom, "charisma" to charisma,
        "level" to level
    )

    fun toAttributeModifiers() = mapOf(
        Attribute.STRENGTH to calculateModifier(strength),
        Attribute.DEXTERITY to calculateModifier(dexterity),
        Attribute.CONSTITUTION to calculateModifier(constitution),
        Attribute.INTELLIGENCE to calculateModifier(intelligence),
        Attribute.WISDOM to calculateModifier(wisdom),
        Attribute.CHARISMA to calculateModifier(charisma)
    )
}

@Composable
fun StatsTab(
    character: Character,
    level: String,
    statsState: StatsState,
    onStatsStateChange: (StatsState) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var isAdvancedMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(colorScheme.surface)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(12.dp))

        // Advanced Mode Toggle
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 12.dp, bottom = 20.dp)
                .width(220.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colorScheme.secondaryContainer)
                .clickable { isAdvancedMode = !isAdvancedMode },
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (isAdvancedMode) "Обычный режим" else "Расширенный режим",
                fontSize = 16.sp,
                color = colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold
            )
        }

        // Stats Section
        Column(modifier = Modifier.padding(horizontal = 3.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AttributesSection(
                strength = statsState.strength, onStrengthChange = { onStatsStateChange(statsState.copy(strength = it)) }, 
                strProf = statsState.strProf, onStrProfChange = { onStatsStateChange(statsState.copy(strProf = it)) },
                intelligence = statsState.intelligence, onIntelligenceChange = { onStatsStateChange(statsState.copy(intelligence = it)) }, 
                intProf = statsState.intProf, onIntProfChange = { onStatsStateChange(statsState.copy(intProf = it)) },
                dexterity = statsState.dexterity, onDexterityChange = { onStatsStateChange(statsState.copy(dexterity = it)) }, 
                dexProf = statsState.dexProf, onDexProfChange = { onStatsStateChange(statsState.copy(dexProf = it)) },
                wisdom = statsState.wisdom, onWisdomChange = { onStatsStateChange(statsState.copy(wisdom = it)) }, 
                wisProf = statsState.wisProf, onWisProfChange = { onStatsStateChange(statsState.copy(wisProf = it)) },
                constitution = statsState.constitution, onConstitutionChange = { onStatsStateChange(statsState.copy(constitution = it)) }, 
                conProf = statsState.conProf, onConProfChange = { onStatsStateChange(statsState.copy(conProf = it)) },
                charisma = statsState.charisma, onCharismaChange = { onStatsStateChange(statsState.copy(charisma = it)) }, 
                chaProf = statsState.chaProf, onChaProfChange = { onStatsStateChange(statsState.copy(chaProf = it)) },
                evalPB = remember(level) { getProficiencyBonus(level).let { if (it >= 0) "+$it" else it.toString() } },
                isAdvancedMode = isAdvancedMode,
                skilledProficiencies = statsState.skilledProficiencies,
                skilledExpertise = statsState.skilledExpertise,
                onSkillClick = { skill: String ->
                    val newProf = if (!statsState.skilledProficiencies.contains(skill) && !statsState.skilledExpertise.contains(skill)) {
                        statsState.skilledProficiencies + skill
                    } else if (statsState.skilledProficiencies.contains(skill)) {
                        statsState.skilledProficiencies - skill
                    } else statsState.skilledProficiencies

                    val newExp = if (statsState.skilledProficiencies.contains(skill)) {
                        statsState.skilledExpertise + skill
                    } else if (statsState.skilledExpertise.contains(skill)) {
                        statsState.skilledExpertise - skill
                    } else statsState.skilledExpertise

                    onStatsStateChange(statsState.copy(skilledProficiencies = newProf, skilledExpertise = newExp))
                }
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun StatIconBoxDetail(value: String, iconRes: Int, isActive: Boolean = true) {
    val colorScheme = MaterialTheme.colorScheme
    Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
        val tint = if (isActive) colorScheme.primary.copy(alpha = 0.38f) else colorScheme.onSurface.copy(alpha = 0.12f)
        if (iconRes == R.drawable.ic_sword) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(painter = painterResource(id = R.drawable.ic_sword), contentDescription = null, modifier = Modifier.fillMaxSize(), colorFilter = ColorFilter.tint(tint))
                Image(painter = painterResource(id = R.drawable.ic_sword), contentDescription = null, modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = -1f), colorFilter = ColorFilter.tint(tint))
            }
        } else {
            Image(painter = painterResource(id = iconRes), contentDescription = null, modifier = Modifier.fillMaxSize(), colorFilter = ColorFilter.tint(tint))
        }
        Text(
            text = value,
            fontSize = 15.sp,
            color = colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            style = TextStyle(
                shadow = Shadow(
                    color = colorScheme.surface,
                    offset = Offset(0f, 0f),
                    blurRadius = 14f
                )
            )
        )
    }
}

@Composable
fun StatCardDetail(label: String, value: String, modifier: Modifier = Modifier, onValueChange: (String) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val score = value.toIntOrNull() ?: 10
    val mod = floor((score - 10) / 2.0).toInt()
    val modStr = if (mod >= 0) "+$mod" else mod.toString()
    Box(modifier = modifier
        .height(104.dp)
        .shadow(2.dp, RoundedCornerShape(8.dp))
        .background(colorScheme.surface, RoundedCornerShape(8.dp))
        .border(1.dp, colorScheme.outline.copy(0.5f), RoundedCornerShape(8.dp))
        .padding(8.dp)) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colorScheme.onSurface)
        Box(modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = 25.dp, bottom = 5.dp)
            .size(40.dp)
            .rotate(-45f)
            .clip(RoundedCornerShape(12.dp))
            .background(colorScheme.primaryContainer)
            .clickable { /* TODO: Implement action */ }, contentAlignment = Alignment.Center) {
            Text(modStr, modifier = Modifier.rotate(45f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.onPrimaryContainer)
        }
        Column(modifier = Modifier.align(Alignment.TopEnd), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colorScheme.surfaceVariant)
                .border(1.dp, colorScheme.outline.copy(0.05f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                BasicTextField(value = value, onValueChange = { val num = it.filter { it.isDigit() }.toIntOrNull(); if (it.isEmpty()) onValueChange(""); else if (num != null && num in 1..30) onValueChange(it.filter { it.isDigit() }) }, textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.width(36.dp))
            }
            Box(modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colorScheme.surface)
                .border(1.dp, colorScheme.outline.copy(0.05f), RoundedCornerShape(8.dp))
                .clickable { /* TODO: Implement action */ }, contentAlignment = Alignment.Center) {
                Text(modStr, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
            }
        }
    }
}
