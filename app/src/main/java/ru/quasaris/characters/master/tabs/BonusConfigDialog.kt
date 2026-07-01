package ru.quasaris.characters.master.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.HazeInputScale
import ru.quasaris.characters.master.*
import ru.quasaris.characters.master.tabs.attacks.AddBonusButton
import ru.quasaris.characters.master.tabs.attacks.AttackBonusIndicator
import ru.quasaris.characters.master.tabs.attacks.DicePart
import ru.quasaris.characters.master.tabs.attacks.SectionHeader
import ru.quasaris.characters.master.tabs.attacks.parseFormulaParts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BonusConfigDialog(
    title: String,
    attribute: Attribute,
    proficiencyBonus: Int,
    attributeModifiers: Map<Attribute, Int>,
    
    // Initial State
    initialStatBonuses: List<StatBonus>,
    initialIsStatProficient: Boolean,
    
    initialSkillBonuses: List<SkillBonus>,
    initialSkillProficiencies: List<String>,
    initialSkillExpertise: List<String>,
    
    skillsToDisplay: List<String>, // If empty, only stat bonuses are shown
    showStatBonuses: Boolean = true,
    
    onDismiss: () -> Unit,
    onSave: (
        statBonuses: List<StatBonus>,
        isStatProficient: Boolean,
        skillBonuses: List<SkillBonus>,
        skillProficiencies: List<String>,
        skillExpertise: List<String>
    ) -> Unit,
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false
) {
    var statBonuses by remember { mutableStateOf(initialStatBonuses) }
    var isStatProficient by remember { mutableStateOf(initialIsStatProficient) }
    
    var skillBonuses by remember { mutableStateOf(initialSkillBonuses) }
    var skillProficiencies by remember { mutableStateOf(initialSkillProficiencies) }
    var skillExpertise by remember { mutableStateOf(initialSkillExpertise) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
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
            containerColor = if (forceBlurEnabled && !isOled) Color.Transparent else colorScheme.background,
            modifier = Modifier.run {
                if (forceBlurEnabled && hazeState != null && !isOled) {
                    hazeEffect(state = hazeState) {
                        style = HazeStyle(blurRadius = 24.dp, tints = listOf(HazeTint(Color.Black.copy(alpha = 0.2f))))
                        inputScale = HazeInputScale.Fixed(0.7f)
                    }
                } else this
            }
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (showStatBonuses) {
                        // --- SAVING THROW SECTION ---
                        SectionHeader("Спасбросок")
                        ProficiencyToggle(
                            isProficient = isStatProficient,
                            proficiencyBonus = proficiencyBonus,
                            onToggle = { isStatProficient = it }
                        )

                        val saveBonuses = statBonuses.filter {
                            it.type == StatBonusType.SAVING_THROW && it.attribute == attribute
                        }
                        saveBonuses.forEach { bonus ->
                            BonusField(
                                bonusName = bonus.name,
                                bonusFormula = bonus.formula,
                                onUpdate = { name, formula ->
                                    statBonuses = statBonuses.map {
                                        if (it.id == bonus.id) it.copy(name = name, formula = formula) else it
                                    }
                                },
                                onDelete = {
                                    statBonuses = statBonuses.filter { it.id != bonus.id }
                                }
                            )
                        }
                        AddBonusButton {
                            statBonuses =
                                statBonuses + StatBonus(attribute = attribute, type = StatBonusType.SAVING_THROW)
                        }

                        // --- ABILITY CHECK SECTION ---
                        SectionHeader("Проверка характеристики")
                        val checkBonuses = statBonuses.filter {
                            it.type == StatBonusType.ABILITY_CHECK && it.attribute == attribute
                        }
                        checkBonuses.forEach { bonus ->
                            BonusField(
                                bonusName = bonus.name,
                                bonusFormula = bonus.formula,
                                onUpdate = { name, formula ->
                                    statBonuses = statBonuses.map {
                                        if (it.id == bonus.id) it.copy(name = name, formula = formula) else it
                                    }
                                },
                                onDelete = {
                                    statBonuses = statBonuses.filter { it.id != bonus.id }
                                }
                            )
                        }
                        AddBonusButton {
                            statBonuses =
                                statBonuses + StatBonus(attribute = attribute, type = StatBonusType.ABILITY_CHECK)
                        }
                    }

                    // --- SKILLS SECTION ---
                    if (skillsToDisplay.isNotEmpty()) {
                        SectionHeader("Навыки")
                        skillsToDisplay.forEach { skillName ->
                            SkillBonusSection(
                                name = skillName,
                                isProficient = skillProficiencies.contains(skillName),
                                isExpert = skillExpertise.contains(skillName),
                                proficiencyBonus = proficiencyBonus,
                                bonuses = skillBonuses.filter { it.skillName == skillName },
                                onLevelSelected = { level ->
                                    when (level) {
                                        0 -> {
                                            skillProficiencies = skillProficiencies - skillName
                                            skillExpertise = skillExpertise - skillName
                                        }
                                        1 -> {
                                            skillProficiencies = skillProficiencies + skillName
                                            skillExpertise = skillExpertise - skillName
                                        }
                                        2 -> {
                                            skillProficiencies = skillProficiencies - skillName
                                            skillExpertise = skillExpertise + skillName
                                        }
                                    }
                                },
                                onAddBonus = {
                                    skillBonuses = skillBonuses + SkillBonus(skillName = skillName)
                                },
                                onUpdateBonus = { bonusId, name, formula ->
                                    skillBonuses = skillBonuses.map {
                                        if (it.id == bonusId) it.copy(name = name, formula = formula) else it
                                    }
                                },
                                onDeleteBonus = { bonusId ->
                                    skillBonuses = skillBonuses.filter { it.id != bonusId }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }

                Button(
                    onClick = { 
                        onSave(statBonuses, isStatProficient, skillBonuses, skillProficiencies, skillExpertise) 
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Сохранить", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ProficiencyToggle(
    isProficient: Boolean,
    proficiencyBonus: Int,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    RoundedCornerShape(8.dp)
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val activeColor = MaterialTheme.colorScheme.primaryContainer
            val inactiveColor = Color.Transparent

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (!isProficient) activeColor else inactiveColor, RoundedCornerShape(6.dp))
                    .clickable { onToggle(false) },
                contentAlignment = Alignment.Center
            ) {
                Text("Нет", fontSize = 16.sp)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (isProficient) activeColor else inactiveColor, RoundedCornerShape(6.dp))
                    .clickable { onToggle(true) },
                contentAlignment = Alignment.Center
            ) {
                Text("+$proficiencyBonus", fontSize = 16.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun SkillProficiencyToggle(
    isProficient: Boolean,
    isExpert: Boolean,
    proficiencyBonus: Int,
    onLevelSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val activeColor = MaterialTheme.colorScheme.primaryContainer
        val inactiveColor = Color.Transparent

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(if (!isProficient && !isExpert) activeColor else inactiveColor, RoundedCornerShape(6.dp))
                .clickable { onLevelSelected(0) },
            contentAlignment = Alignment.Center
        ) {
            Text("Нет", fontSize = 14.sp)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(if (isProficient) activeColor else inactiveColor, RoundedCornerShape(6.dp))
                .clickable { onLevelSelected(1) },
            contentAlignment = Alignment.Center
        ) {
            Text("+$proficiencyBonus", fontSize = 14.sp, textAlign = TextAlign.Center)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(if (isExpert) activeColor else inactiveColor, RoundedCornerShape(6.dp))
                .clickable { onLevelSelected(2) },
            contentAlignment = Alignment.Center
        ) {
            Text("+${proficiencyBonus * 2}", fontSize = 14.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun BonusField(
    bonusName: String,
    bonusFormula: String,
    onUpdate: (String, String) -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = bonusName,
                onValueChange = { onUpdate(it, bonusFormula) },
                label = { Text("Название") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Close, contentDescription = "Удалить", tint = Color.Red)
            }
        }
        OutlinedTextField(
            value = bonusFormula,
            onValueChange = { onUpdate(bonusName, it) },
            label = { Text("Формула") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
fun SkillBonusSection(
    name: String,
    isProficient: Boolean,
    isExpert: Boolean,
    proficiencyBonus: Int,
    bonuses: List<SkillBonus>,
    onLevelSelected: (Int) -> Unit,
    onAddBonus: () -> Unit,
    onUpdateBonus: (String, String, String) -> Unit,
    onDeleteBonus: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            
            SkillProficiencyToggle(
                isProficient = isProficient,
                isExpert = isExpert,
                proficiencyBonus = proficiencyBonus,
                onLevelSelected = onLevelSelected
            )
            
            bonuses.forEach { bonus ->
                BonusField(
                    bonusName = bonus.name,
                    bonusFormula = bonus.formula,
                    onUpdate = { n, f -> onUpdateBonus(bonus.id, n, f) },
                    onDelete = { onDeleteBonus(bonus.id) }
                )
            }
            
            AddBonusButton(onClick = onAddBonus)
        }
    }
}
