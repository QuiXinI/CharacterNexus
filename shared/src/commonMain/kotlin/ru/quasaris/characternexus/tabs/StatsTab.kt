package ru.quasaris.characternexus.tabs

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
import androidx.compose.ui.graphics.graphicsLayer
import org.jetbrains.compose.resources.painterResource
import characternexus.shared.generated.resources.Res
import characternexus.shared.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.draw.rotate
import ru.quasaris.characternexus.ui.outerShadow
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.floor
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.MainWindow.AttributesSection
import ru.quasaris.characternexus.backend.getProficiencyBonus
import ru.quasaris.characternexus.backend.calculateModifier
import dev.chrisbanes.haze.HazeState
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import ru.quasaris.characternexus.ui.TabControlHeader
import ru.quasaris.characternexus.ui.CharacterDetailState

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
    val skilledExpertise: List<String> = emptyList(),
    val statBonuses: List<StatBonus> = emptyList(),
    val skillBonuses: List<SkillBonus> = emptyList()
) {
    fun toStatsMap(level: String, proficiencyBonus: String) = mapOf(
        "strength" to strength, "dexterity" to dexterity, "constitution" to constitution,
        "intelligence" to intelligence, "wisdom" to wisdom, "charisma" to charisma,
        "level" to level, "proficiencyBonus" to proficiencyBonus
    )

    fun toAttributeModifiers(): Map<Attribute, Int> = mapOf<Attribute, Int>(
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
    onStatsStateChange: (StatsState) -> Unit,
    onRoll: (RollResult) -> Unit = {},
    hazeState: HazeState? = null,
    popupHazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false,
    blurPopups: Boolean = false,
    isAdvancedMode: Boolean = false,
    advantageLogic: AdvantageLogic = AdvantageLogic.TOTAL,
    attributeModifiers: Map<Attribute, Int> = emptyMap(),
    statsMap: Map<String, String> = emptyMap(),
    exhaustion: Int = 0,
    onBonusConfigOpenChange: (Boolean) -> Unit = {},
    state: CharacterDetailState? = null,
    header: @Composable () -> Unit = {},
    isDesktop: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(colorScheme.surface)
            .verticalScroll(rememberScrollState())
    ) {
        header()
        
        if (isDesktop) {
            Surface(
                onClick = { if (state != null) state.isAdvancedMode = !state.isAdvancedMode },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .outerShadow(
                        shape = RoundedCornerShape(16.dp),
                        blur = 4.dp,
                        offsetY = 2.dp
                    ),
                shape = RoundedCornerShape(16.dp),
                color = colorScheme.surfaceContainer,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isAdvancedMode) Icons.Default.ViewList else Icons.Default.GridView,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (isAdvancedMode) "Расширенный Режим" else "Компактный режим",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = colorScheme.primary
                    )
                }
            }
        } else {
            Spacer(Modifier.height(8.dp))
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
                statBonuses = statsState.statBonuses,
                skillBonuses = statsState.skillBonuses,
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
                },
                onStatClick = { attr ->
                    if (state != null) {
                        state.closeFullscreenDialogs()
                        state.activeBonusConfigAttribute = attr
                        state.isBonusConfigOpen = true
                    }
                },
                onSkillLongClick = { skillName ->
                    if (state != null) {
                        state.closeFullscreenDialogs()
                        state.activeBonusConfigSkill = skillName
                        state.isBonusConfigOpen = true
                    }
                },
                onRoll = onRoll,
                statsMap = statsMap,
                attributeModifiers = attributeModifiers,
                exhaustion = exhaustion,
                hazeState = hazeState,
                isOled = colorScheme.background == Color.Black,
                advantageLogic = advantageLogic
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun StatIconBoxDetail(value: String, iconRes: DrawableResource, isActive: Boolean = true) {
    val colorScheme = MaterialTheme.colorScheme
    Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
        val tint = if (isActive) colorScheme.primary.copy(alpha = 0.38f) else colorScheme.onSurface.copy(alpha = 0.12f)
        if (iconRes == Res.drawable.ic_sword) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(painter = painterResource(Res.drawable.ic_sword), contentDescription = null, modifier = Modifier.fillMaxSize(), colorFilter = ColorFilter.tint(tint))
                Image(painter = painterResource(Res.drawable.ic_sword), contentDescription = null, modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = -1f), colorFilter = ColorFilter.tint(tint))
            }
        } else {
            Image(painter = painterResource(iconRes), contentDescription = null, modifier = Modifier.fillMaxSize(), colorFilter = ColorFilter.tint(tint))
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
        .outerShadow(
            shape = RoundedCornerShape(12.dp),
            blur = 2.dp,
            offsetY = 1.dp
        )
        .background(colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
        .padding(8.dp)) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colorScheme.onSurface)
        Box(modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = 25.dp, bottom = 5.dp)
            .size(40.dp)
            .rotate(-45f)
            .outerShadow(
                shape = RoundedCornerShape(12.dp),
                blur = 2.dp,
                offsetY = 1.dp
            )
            .clip(RoundedCornerShape(12.dp))
            .background(colorScheme.primaryContainer)
            .clickable {}, contentAlignment = Alignment.Center) {
            Text(modStr, modifier = Modifier.rotate(45f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.onPrimaryContainer)
        }
        Column(modifier = Modifier.align(Alignment.TopEnd), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier
                .size(44.dp)
                .outerShadow(
                    shape = RoundedCornerShape(12.dp),
                    blur = 2.dp,
                    offsetY = 1.dp
                )
                .clip(RoundedCornerShape(12.dp))
                .background(colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                BasicTextField(
                    value = value, 
                    onValueChange = { 
                        val filtered = it.filter { it.isDigit() }
                        if (filtered.length <= 2) onValueChange(filtered)
                    }, 
                    textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface), 
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                    modifier = Modifier.width(36.dp),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.Center) {
                            if (value.isEmpty()) {
                                Text(
                                    "10",
                                    style = TextStyle(
                                        textAlign = TextAlign.Center,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
            Box(modifier = Modifier
                .size(44.dp)
                .outerShadow(
                    shape = RoundedCornerShape(12.dp),
                    blur = 2.dp,
                    offsetY = 1.dp
                )
                .clip(RoundedCornerShape(12.dp))
                .background(colorScheme.surfaceContainerLow)
                .clickable { /* Action implemented by parent */ }, contentAlignment = Alignment.Center) {
                Text(modStr, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.onSurface)
            }
        }
    }
}
