package ru.quasaris.characternexus.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.platform.LocalDensity
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.backend.calculateModifier
import ru.quasaris.characternexus.backend.calculateTotalBonus
import ru.quasaris.characternexus.ui.util.outerShadow
import dev.chrisbanes.haze.HazeState

@Composable
fun AttributesSection(
    strength: String,
    onStrengthChange: (String) -> Unit,
    strProf: Boolean,
    onStrProfChange: (Boolean) -> Unit,
    intelligence: String,
    onIntelligenceChange: (String) -> Unit,
    intProf: Boolean,
    onIntProfChange: (Boolean) -> Unit,
    dexterity: String,
    onDexterityChange: (String) -> Unit,
    dexProf: Boolean,
    onDexProfChange: (Boolean) -> Unit,
    wisdom: String,
    onWisdomChange: (String) -> Unit,
    wisProf: Boolean,
    onWisProfChange: (Boolean) -> Unit,
    constitution: String,
    onConstitutionChange: (String) -> Unit,
    conProf: Boolean,
    onConProfChange: (Boolean) -> Unit,
    charisma: String,
    onCharismaChange: (String) -> Unit,
    chaProf: Boolean,
    onChaProfChange: (Boolean) -> Unit,
    evalPB: String,
    isAdvancedMode: Boolean,
    skilledProficiencies: List<String>,
    skilledExpertise: List<String>,
    onSkillClick: (String) -> Unit,
    statsMap: Map<String, String> = emptyMap(),
    attributeModifiers: Map<Attribute, Int> = emptyMap(),
    exhaustion: Int = 0,
    hazeState: HazeState? = null
) {
    val pbVal = evalPB.replace("+", "").toIntOrNull() ?: 0
    val colorScheme = MaterialTheme.colorScheme

    val effStrength = statsMap["strength"] ?: strength
    val effDexterity = statsMap["dexterity"] ?: dexterity
    val effConstitution = statsMap["constitution"] ?: constitution
    val effIntelligence = statsMap["intelligence"] ?: intelligence
    val effWisdom = statsMap["wisdom"] ?: wisdom
    val effCharisma = statsMap["charisma"] ?: charisma

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        AnimatedContent(
            targetState = isAdvancedMode,
            transitionSpec = {
                (slideInVertically { if (targetState) -it else it } + fadeIn()).togetherWith(slideOutVertically { if (targetState) it else -it } + fadeOut())
            },
            label = "mode_switch",
            modifier = Modifier.fillMaxWidth()
        ) { advanced ->
            if (advanced) {
                val stats = listOf(
                    StatInfo(Attribute.STRENGTH, "Сила", effStrength, strProf, onStrengthChange, onStrProfChange, listOf("Атлетика")),
                    StatInfo(Attribute.DEXTERITY, "Ловкость", effDexterity, dexProf, onDexterityChange, onDexProfChange, listOf("Акробатика", "Ловкость рук", "Скрытность")),
                    StatInfo(Attribute.CONSTITUTION, "Телосложение", effConstitution, conProf, onConstitutionChange, onConProfChange, emptyList()),
                    StatInfo(Attribute.INTELLIGENCE, "Интеллект", effIntelligence, intProf, onIntelligenceChange, onIntProfChange, listOf("Анализ", "История", "Магия", "Природа", "Религия")),
                    StatInfo(Attribute.WISDOM, "Мудрость", effWisdom, wisProf, onWisdomChange, onWisProfChange, listOf("Внимательность", "Выживание", "Медицина", "Проницательность", "Уход за животными")),
                    StatInfo(Attribute.CHARISMA, "Харизма", effCharisma, chaProf, onCharismaChange, onChaProfChange, listOf("Выступление", "Запугивание", "Обман", "Убеждение"))
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    stats.forEach { stat ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatCard(stat.label, stat.value, evalPB, stat.isProf, Modifier.fillMaxWidth(), stat.onValueChange, stat.onProfChange, onClick = { },
                                exhaustion = exhaustion,
                                hazeState = hazeState,
                                attribute = stat.attribute
                            )
                            stat.skills.forEach { skill ->
                                SkillSubPlate(skill, stat.attribute, skilledProficiencies.contains(skill), skilledExpertise.contains(skill), evalPB, attributeModifiers, onSkillClick,
                                    exhaustion = exhaustion,
                                    hazeState = hazeState
                                )
                            }
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Сила", effStrength, evalPB, strProf, Modifier.weight(1f), onStrengthChange, onStrProfChange, onClick = { },
                            exhaustion = exhaustion,
                            hazeState = hazeState,
                            attribute = Attribute.STRENGTH
                        )
                        StatCard("Интеллект", effIntelligence, evalPB, intProf, Modifier.weight(1f), onIntelligenceChange, onIntProfChange, onClick = { },
                            exhaustion = exhaustion,
                            hazeState = hazeState,
                            attribute = Attribute.INTELLIGENCE
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Ловкость", effDexterity, evalPB, dexProf, Modifier.weight(1f), onDexterityChange, onDexProfChange, onClick = { },
                            exhaustion = exhaustion,
                            hazeState = hazeState,
                            attribute = Attribute.DEXTERITY
                        )
                        StatCard("Мудрость", effWisdom, evalPB, wisProf, Modifier.weight(1f), onWisdomChange, onWisProfChange, onClick = { },
                            exhaustion = exhaustion,
                            hazeState = hazeState,
                            attribute = Attribute.WISDOM
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Телосложение", effConstitution, evalPB, conProf, Modifier.weight(1f), onConstitutionChange, onConProfChange, onClick = { },
                            exhaustion = exhaustion,
                            hazeState = hazeState,
                            attribute = Attribute.CONSTITUTION
                        )
                        StatCard("Харизма", effCharisma, evalPB, chaProf, Modifier.weight(1f), onCharismaChange, onChaProfChange, onClick = { },
                            exhaustion = exhaustion,
                            hazeState = hazeState,
                            attribute = Attribute.CHARISMA
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .outerShadow(
                    shape = RoundedCornerShape(16.dp),
                    blur = 4.dp
                )
                .clip(RoundedCornerShape(16.dp))
                .background(colorScheme.surfaceContainerLow)
        ) {
            Text(
                "Пассивные проверки",
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp),
                thickness = 1.dp,
                color = colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
            PassiveCheckRow("Пассивный Анализ", Attribute.INTELLIGENCE, evalPB, attributeModifiers, skilledProficiencies.contains("Анализ"), skilledExpertise.contains("Анализ"), exhaustion = exhaustion)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), thickness = 0.5.dp, color = colorScheme.outlineVariant.copy(alpha = 0.2f))
            PassiveCheckRow("Пассивная Внимательность", Attribute.WISDOM, evalPB, attributeModifiers, skilledProficiencies.contains("Внимательность"), skilledExpertise.contains("Внимательность"), exhaustion = exhaustion)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), thickness = 0.5.dp, color = colorScheme.outlineVariant.copy(alpha = 0.2f))
            PassiveCheckRow("Пассивная Проницательность", Attribute.WISDOM, evalPB, attributeModifiers, skilledProficiencies.contains("Проницательность"), skilledExpertise.contains("Проницательность"), exhaustion = exhaustion)
        }
    }
}

data class StatInfo(
    val attribute: Attribute,
    val label: String,
    val value: String,
    val isProf: Boolean,
    val onValueChange: (String) -> Unit,
    val onProfChange: (Boolean) -> Unit,
    val skills: List<String>
)

private fun getAttributeNames(attribute: Attribute?, label: String): Pair<String, String> {
    if (attribute != null && attribute != Attribute.NONE) {
        return when (attribute) {
            Attribute.STRENGTH -> "Сила" to "Сила"
            Attribute.DEXTERITY -> "Ловкость" to "Ловк."
            Attribute.CONSTITUTION -> "Телосложение" to "Тело."
            Attribute.INTELLIGENCE -> "Интеллект" to "Инт."
            Attribute.WISDOM -> "Мудрость" to "Мудр."
            Attribute.CHARISMA -> "Харизма" to "Хар."
            Attribute.NONE -> label to label
        }
    }
    val clean = label.lowercase().trim().removeSuffix(".")
    return when (clean) {
        "сила", "str", "strength" -> "Сила" to "Сила"
        "ловкость", "ловк", "dex", "dexterity" -> "Ловкость" to "Ловк."
        "телосложение", "тело", "con", "constitution" -> "Телосложение" to "Тело."
        "интеллект", "инт", "int", "intelligence" -> "Интеллект" to "Инт."
        "мудрость", "мудр", "wis", "wisdom" -> "Мудрость" to "Мудр."
        "харизма", "хар", "cha", "charisma" -> "Харизма" to "Хар."
        else -> label to label
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StatCard(
    label: String, 
    value: String, 
    profB: String, 
    isP: Boolean, 
    modifier: Modifier = Modifier, 
    onValue: (String) -> Unit, 
    onPToggle: (Boolean) -> Unit, 
    onClick: () -> Unit,
    saveBonus: Int = 0,
    checkBonus: Int = 0,
    exhaustion: Int = 0,
    hazeState: HazeState? = null,
    isEditable: Boolean = true,
    attribute: Attribute? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val base = calculateModifier(value)
    val pb = profB.toIntOrNull() ?: 0
    val totalSave = base + (if (isP) pb else 0) + saveBonus - (exhaustion * 2)
    val totalCheck = base + checkBonus - (exhaustion * 2)
    
    val (fullName, shortName) = remember(label, attribute) {
        getAttributeNames(attribute, label)
    }

    var cardWidthPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()

    val statLabelStyle = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.Black
    )
    val fullLabelWidthPx = remember(fullName, statLabelStyle) {
        textMeasurer.measure(fullName, statLabelStyle).size.width
    }

    val saveLabelStyle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold
    )
    val fullSaveWidthPx = remember(saveLabelStyle) {
        textMeasurer.measure("Спасбросок", saveLabelStyle).size.width
    }
    val shortSaveWidthPx = remember(saveLabelStyle) {
        textMeasurer.measure("Спас.", saveLabelStyle).size.width
    }

    val displayLabel = if (cardWidthPx > 0) {
        val availableStatWidthPx = cardWidthPx - with(density) { 64.dp.roundToPx() }
        if (fullLabelWidthPx <= availableStatWidthPx) fullName else shortName
    } else {
        fullName
    }

    val displaySaveLabel = if (cardWidthPx > 0) {
        val availableSaveWidthPx = cardWidthPx - with(density) { 144.dp.roundToPx() }
        if (fullSaveWidthPx <= availableSaveWidthPx) {
            "Спасбросок"
        } else if (shortSaveWidthPx <= availableSaveWidthPx) {
            "Спас."
        } else {
            ""
        }
    } else {
        "Спасбросок"
    }

    Box(modifier = modifier
        .heightIn(min = 100.dp)
        .onGloballyPositioned { coords ->
            cardWidthPx = coords.size.width
        }
        .outerShadow(
            shape = RoundedCornerShape(16.dp),
            blur = 4.dp
        )
        .clip(RoundedCornerShape(16.dp))
        .background(colorScheme.surfaceContainer)
        .clickable { onClick() }
        .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    displayLabel,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1
                )

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .run {
                            if (isEditable) {
                                this.outerShadow(
                                    shape = RoundedCornerShape(10.dp),
                                    blur = 2.dp
                                )
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colorScheme.onSurface.copy(alpha = 0.05f))
                            } else this
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isEditable) {
                        BasicTextField(
                            value = value,
                            onValueChange = {
                                val f = it.filter { it.isDigit() }
                                if (f.length <= 2) onValue(f)
                            },
                            textStyle = TextStyle(
                                textAlign = TextAlign.Center,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = colorScheme.onSurface
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.Center) {
                                    if (value.isEmpty()) {
                                        Text(
                                            "10",
                                            style = TextStyle(
                                                textAlign = TextAlign.Center,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Black,
                                                color = colorScheme.onSurface.copy(alpha = 0.3f)
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    } else {
                        Text(
                            text = value.ifEmpty { "10" },
                            style = TextStyle(
                                textAlign = TextAlign.Center,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = if (value.isEmpty()) colorScheme.onSurface.copy(alpha = 0.3f) else colorScheme.onSurface
                            ),
                            modifier = Modifier.clickable { onClick() }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .outerShadow(
                            shape = RoundedCornerShape(12.dp),
                            blur = 2.dp
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onPToggle(!isP) }
                        .background(if (isP) colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (isP) colorScheme.primary else colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    )
                    if (displaySaveLabel.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            displaySaveLabel,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    ModifierBubble(
                        text = if (totalSave >= 0) "+$totalSave" else "$totalSave",
                        color = if (isP) colorScheme.primary else colorScheme.onSurface
                    )
                }

                ModifierBubble(
                    text = if (totalCheck >= 0) "+$totalCheck" else "$totalCheck",
                    color = colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModifierBubble(
    text: String,
    color: Color,
    clickable: Boolean = true,
    hazeState: HazeState? = null
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .size(40.dp)
            .outerShadow(
                shape = RoundedCornerShape(10.dp),
                blur = 2.dp
            )
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .then(if (clickable) {
                Modifier.clickable {  }
            } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = color
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SkillSubPlate(
    name: String,
    attribute: Attribute,
    isProficient: Boolean,
    isExpert: Boolean,
    pbStr: String,
    attributeModifiers: Map<Attribute, Int>,
    onClick: (String) -> Unit,
    bonus: Int = 0,
    exhaustion: Int = 0,
    hazeState: HazeState? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val pb = pbStr.toIntOrNull() ?: 0
    val baseMod = attributeModifiers[attribute] ?: 0
    val total = baseMod + (if (isExpert) pb * 2 else if (isProficient) pb else 0) + bonus - (exhaustion * 2)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(start = 8.dp)
            .outerShadow(
                shape = RoundedCornerShape(10.dp),
                blur = 6.dp
            )
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isExpert) colorScheme.primaryContainer.copy(alpha = 0.6f)
                else if (isProficient) colorScheme.primaryContainer.copy(alpha = 0.3f)
                else colorScheme.surfaceContainerHigh.copy(alpha = 0.4f)
            )
            .clickable { onClick(name) }
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isProficient || isExpert) colorScheme.primary else colorScheme.outlineVariant.copy(alpha = 0.4f))
            )
            Box(modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isExpert) colorScheme.primary else Color.Transparent)
                .border(1.dp, if (isExpert) Color.Transparent else colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
            )
        }
        
        Spacer(Modifier.width(12.dp))
        Text(
            name, 
            modifier = Modifier.weight(1f), 
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold, 
            color = colorScheme.onSurface
        )
        ModifierBubble(
            text = if (total >= 0) "+$total" else "$total",
            color = if (isExpert || isProficient) colorScheme.primary else colorScheme.onSurface,
            hazeState = hazeState
        )
    }
}


@Composable
fun PassiveCheckRow(
    label: String,
    attribute: Attribute,
    pbStr: String,
    attributeModifiers: Map<Attribute, Int>,
    isProficient: Boolean,
    isExpert: Boolean,
    exhaustion: Int = 0
) {
    val colorScheme = MaterialTheme.colorScheme
    val pb = pbStr.toIntOrNull() ?: 0
    val baseMod = attributeModifiers[attribute] ?: 0
    val total = 10 + baseMod + (if (isExpert) pb * 2 else if (isProficient) pb else 0) - (exhaustion * 2)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurfaceVariant
        )
        Text(
            "$total",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = colorScheme.primary
        )
    }
}
