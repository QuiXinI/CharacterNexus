package ru.quasaris.characters.master.MainWindow

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.quasaris.characters.master.HeaderCode.calculateModifier

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
    onSkillClick: (String) -> Unit
) {
    val animDuration = 600

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            androidx.compose.animation.AnimatedVisibility(
                visible = !isAdvancedMode,
                enter = slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(animDuration)) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(animDuration)) + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Сила", strength, evalPB, strProf, Modifier.weight(1f), onStrengthChange, onStrProfChange)
                        StatCard("Интеллект", intelligence, evalPB, intProf, Modifier.weight(1f), onIntelligenceChange, onIntProfChange)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Ловкость", dexterity, evalPB, dexProf, Modifier.weight(1f), onDexterityChange, onDexProfChange)
                        StatCard("Мудрость", wisdom, evalPB, wisProf, Modifier.weight(1f), onWisdomChange, onWisProfChange)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Тело.", constitution, evalPB, conProf, Modifier.weight(1f), onConstitutionChange, onConProfChange)
                        StatCard("Харизма", charisma, evalPB, chaProf, Modifier.weight(1f), onCharismaChange, onChaProfChange)
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = isAdvancedMode,
                enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(animDuration)) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(animDuration)) + fadeOut()
            ) {
                val stats = listOf(
                    StatInfo("STR", "Сила", strength, strProf, onStrengthChange, onStrProfChange, listOf("Атлетика")),
                    StatInfo("DEX", "Ловкость", dexterity, dexProf, onDexterityChange, onDexProfChange, listOf("Акробатика", "Ловкость рук", "Скрытность")),
                    StatInfo("CON", "Телосложение", constitution, conProf, onConstitutionChange, onConProfChange, emptyList()),
                    StatInfo("INT", "Интеллект", intelligence, intProf, onIntelligenceChange, onIntProfChange, listOf("Анализ", "История", "Магия", "Природа", "Религия")),
                    StatInfo("WIS", "Мудрость", wisdom, wisProf, onWisdomChange, onWisProfChange, listOf("Внимательность", "Выживание", "Медицина", "Проницательность", "Уход за животными")),
                    StatInfo("CHA", "Харизма", charisma, chaProf, onCharismaChange, onChaProfChange, listOf("Выступление", "Запугивание", "Обман", "Убеждение"))
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    stats.forEach { stat ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            StatCard(stat.label, stat.value, evalPB, stat.isProf, Modifier.fillMaxWidth(), stat.onValueChange, stat.onProfChange)
                            stat.skills.forEach { skill ->
                                SkillSubPlate(skill, skilledProficiencies.contains(skill), skilledExpertise.contains(skill), evalPB, stat.value, onSkillClick)
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
        ) {
            Text(
                "Пассивные проверки",
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )
            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
            PassiveCheckRow("Пассивный Анализ", intelligence, evalPB, skilledProficiencies.contains("Анализ"), skilledExpertise.contains("Анализ"))
            androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            PassiveCheckRow("Пассивная Внимательность", wisdom, evalPB, skilledProficiencies.contains("Внимательность"), skilledExpertise.contains("Внимательность"))
            androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            PassiveCheckRow("Пассивная Проницательность", wisdom, evalPB, skilledProficiencies.contains("Проницательность"), skilledExpertise.contains("Проницательность"))
        }
    }
}

data class StatInfo(
    val id: String,
    val label: String,
    val value: String,
    val isProf: Boolean,
    val onValueChange: (String) -> Unit,
    val onProfChange: (Boolean) -> Unit,
    val skills: List<String>
)

@Composable
fun StatCard(label: String, value: String, profB: String, isP: Boolean, modifier: Modifier = Modifier, onValue: (String) -> Unit, onPToggle: (Boolean) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val base = calculateModifier(value)
    val pb = profB.toIntOrNull() ?: 0
    val total = base + (if (isP) pb else 0)
    
    Box(modifier = modifier
        .height(96.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(colorScheme.surfaceVariant.copy(alpha = 0.4f))
        .border(1.dp, colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
        .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
                        value = value,
                        onValueChange = {
                            val f = it.filter { it.isDigit() }; if (f.isEmpty()) onValue("") else {
                            val n = f.toIntOrNull(); if (n != null && n in 1..30) onValue(f)
                        }
                        },
                        textStyle = TextStyle(
                            textAlign = TextAlign.Center,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onPToggle(!isP) }
                        .background(if (isP) colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (isP) colorScheme.primary else colorScheme.outline.copy(alpha = 0.3f))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Спас",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    ModifierBubble(
                        text = if (total >= 0) "+$total" else "$total",
                        color = if (isP) colorScheme.primary else colorScheme.onSurface
                    )
                }

                ModifierBubble(
                    text = if (base >= 0) "+$base" else "$base",
                    color = colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ModifierBubble(
    text: String,
    color: Color,
    clickable: Boolean = true,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .then(if (clickable) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = color
        )
    }
}

@Composable
fun SkillSubPlate(
    name: String,
    isProficient: Boolean,
    isExpert: Boolean,
    pbStr: String,
    attrValue: String,
    onClick: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val pb = pbStr.toIntOrNull() ?: 0
    val baseMod = calculateModifier(attrValue)
    val total = baseMod + (if (isExpert) pb * 2 else if (isProficient) pb else 0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .padding(start = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isExpert) colorScheme.primary.copy(alpha = 0.18f)
                else if (isProficient) colorScheme.primary.copy(alpha = 0.1f)
                else colorScheme.surfaceVariant.copy(alpha = 0.25f)
            )
            .clickable { onClick(name) }
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isProficient || isExpert) colorScheme.primary else colorScheme.outline.copy(alpha = 0.2f))
            )
            Box(modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isExpert) colorScheme.primary else Color.Transparent)
                .border(0.8.dp, if (isExpert) Color.Transparent else colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
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
            color = if (isExpert || isProficient) colorScheme.primary else colorScheme.onSurface
        )
    }
}

@Composable
fun PassiveCheckRow(
    label: String,
    attrValue: String,
    pbStr: String,
    isProficient: Boolean,
    isExpert: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val pb = pbStr.toIntOrNull() ?: 0
    val baseMod = calculateModifier(attrValue)
    val total = 10 + baseMod + (if (isExpert) pb * 2 else if (isProficient) pb else 0)

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
