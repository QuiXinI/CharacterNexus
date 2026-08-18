package ru.quasaris.characternexus.tabs

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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import ru.quasaris.characternexus.ui.DialogDimStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.quasaris.characternexus.ui.DialogDimStyle
import ru.quasaris.characternexus.model.*
import ru.quasaris.characternexus.backend.calculateModifier
import ru.quasaris.characternexus.tabs.attacks.AddBonusButton
import ru.quasaris.characternexus.tabs.attacks.AttackBonusIndicator
import ru.quasaris.characternexus.backend.DicePart
import ru.quasaris.characternexus.tabs.attacks.SectionHeader
import ru.quasaris.characternexus.backend.parseFormulaParts
import ru.quasaris.characternexus.tabs.attacks.AdvantagePreferenceLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BonusConfigDialog(
    title: String,
    attribute: Attribute,
    proficiencyBonus: Int,
    attributeModifiers: Map<Attribute, Int>,
    
    // Initial State
    initialBaseScore: String = "10",
    initialStatBonuses: List<StatBonus>,
    initialIsStatProficient: Boolean,
    
    initialSkillBonuses: List<SkillBonus>,
    initialSkillProficiencies: List<String>,
    initialSkillExpertise: List<String>,
    
    skillsToDisplay: List<String>, // If empty, only stat bonuses are shown
    showStatBonuses: Boolean = true,
    
    onDismiss: () -> Unit,
    onSave: (
        baseScore: String,
        statBonuses: List<StatBonus>,
        isStatProficient: Boolean,
        skillBonuses: List<SkillBonus>,
        skillProficiencies: List<String>,
        skillExpertise: List<String>
    ) -> Unit,
    forceBlurEnabled: Boolean = false
) {
    var baseScore by remember { mutableStateOf(initialBaseScore) }
    var statBonuses by remember { mutableStateOf(initialStatBonuses) }
    var isStatProficient by remember { mutableStateOf(initialIsStatProficient) }
    
    var skillBonuses by remember { mutableStateOf(initialSkillBonuses) }
    var skillProficiencies by remember { mutableStateOf(initialSkillProficiencies) }
    var skillExpertise by remember { mutableStateOf(initialSkillExpertise) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        DialogDimStyle(0f)
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
                        containerColor = if (forceBlurEnabled && !isOled) Color.Transparent.copy(alpha = 0.0f) else colorScheme.surface
                    )
                )
            },
            containerColor = if (forceBlurEnabled && !isOled) Color.Transparent.copy(alpha = 0.0f) else colorScheme.background
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
                    if (showStatBonuses) {
                        // --- BASE SCORE SECTION ---
                        SectionHeader("Базовое значение")
                        OutlinedTextField(
                            value = baseScore,
                            onValueChange = { 
                                val f = it.filter { c -> c.isDigit() }
                                if (f.isEmpty()) baseScore = "" else {
                                    val n = f.toIntOrNull()
                                    if (n != null && n in 1..30) baseScore = f
                                }
                            },
                            label = { Text("Значение характеристики") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            trailingIcon = {
                                Text(
                                    text = calculateModifier(baseScore).let { if (it >= 0) "+$it" else it.toString() },
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            }
                        )

                        // --- CHARACTERISTIC VALUE SECTION ---
                        SectionHeader("Бонусы к значению")
                        val valueBonuses = statBonuses.filter {
                            it.type == StatBonusType.CHARACTERISTIC_VALUE && it.attribute == attribute
                        }
                        val valueHasOverride = valueBonuses.any { it.isActive && it.operation == BonusOperation.OVERRIDE }
                        valueBonuses.forEach { bonus ->
                            BonusField(
                                bonus = bonus,
                                isOverrideDisabled = valueHasOverride && bonus.operation != BonusOperation.OVERRIDE,
                                onUpdate = { n, f, act, op, adv ->
                                    statBonuses = statBonuses.map {
                                        if (it.id == bonus.id) it.copy(name = n, formula = f, isActive = act, operation = op, advantagePreference = adv) else it
                                    }
                                },
                                onDelete = {
                                    statBonuses = statBonuses.filter { it.id != bonus.id }
                                }
                            )
                        }
                        AddBonusButton {
                            statBonuses =
                                statBonuses + StatBonus(attribute = attribute, type = StatBonusType.CHARACTERISTIC_VALUE, advantagePreference = AdvantagePreference.NONE)
                        }

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
                        val saveHasOverride = saveBonuses.any { it.isActive && it.operation == BonusOperation.OVERRIDE }
                        saveBonuses.forEach { bonus ->
                            BonusField(
                                bonus = bonus,
                                isOverrideDisabled = saveHasOverride && bonus.operation != BonusOperation.OVERRIDE,
                                onUpdate = { n, f, act, op, adv ->
                                    statBonuses = statBonuses.map {
                                        if (it.id == bonus.id) it.copy(name = n, formula = f, isActive = act, operation = op, advantagePreference = adv) else it
                                    }
                                },
                                onDelete = {
                                    statBonuses = statBonuses.filter { it.id != bonus.id }
                                }
                            )
                        }
                        AddBonusButton {
                            statBonuses =
                                statBonuses + StatBonus(attribute = attribute, type = StatBonusType.SAVING_THROW, advantagePreference = AdvantagePreference.NONE)
                        }

                        // --- ABILITY CHECK SECTION ---
                        SectionHeader("Проверка характеристики")
                        val checkBonuses = statBonuses.filter {
                            it.type == StatBonusType.ABILITY_CHECK && it.attribute == attribute
                        }
                        val checkHasOverride = checkBonuses.any { it.isActive && it.operation == BonusOperation.OVERRIDE }
                        checkBonuses.forEach { bonus ->
                            BonusField(
                                bonus = bonus,
                                applyToSkills = bonus.applyToSkills,
                                isOverrideDisabled = checkHasOverride && bonus.operation != BonusOperation.OVERRIDE,
                                onApplyToSkillsChange = { apply ->
                                    statBonuses = statBonuses.map {
                                        if (it.id == bonus.id) it.copy(applyToSkills = apply) else it
                                    }
                                },
                                onUpdate = { n, f, act, op, adv ->
                                    statBonuses = statBonuses.map {
                                        if (it.id == bonus.id) it.copy(name = n, formula = f, isActive = act, operation = op, advantagePreference = adv) else it
                                    }
                                },
                                onDelete = {
                                    statBonuses = statBonuses.filter { it.id != bonus.id }
                                }
                            )
                        }
                        AddBonusButton {
                            statBonuses =
                                statBonuses + StatBonus(attribute = attribute, type = StatBonusType.ABILITY_CHECK, advantagePreference = AdvantagePreference.NONE)
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
                                    skillBonuses = skillBonuses + SkillBonus(skillName = skillName, advantagePreference = AdvantagePreference.NONE)
                                },
                                onUpdateBonus = { bonusId, n, f, act, op, adv ->
                                    skillBonuses = skillBonuses.map {
                                        if (it.id == bonusId) it.copy(name = n, formula = f, isActive = act, operation = op, advantagePreference = adv) else it
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
                        onSave(baseScore, statBonuses, isStatProficient, skillBonuses, skillProficiencies, skillExpertise) 
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BonusField(
    bonus: IBonus,
    applyToSkills: Boolean? = null,
    isOverrideDisabled: Boolean = false,
    onApplyToSkillsChange: ((Boolean) -> Unit)? = null,
    onUpdate: (name: String, formula: String, isActive: Boolean, operation: BonusOperation, advantagePreference: AdvantagePreference) -> Unit,
    onDelete: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (bonus.isActive) 1f else 0.5f),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = bonus.isActive,
                onCheckedChange = { onUpdate(bonus.name, bonus.formula, it, bonus.operation, bonus.advantagePreference) },
                modifier = Modifier.scale(0.8f)
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = bonus.name,
                onValueChange = { onUpdate(it, bonus.formula, bonus.isActive, bonus.operation, bonus.advantagePreference) },
                label = { Text("Название") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Close, contentDescription = "Удалить", tint = Color.Red)
            }
        }

        if (applyToSkills != null && onApplyToSkillsChange != null) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onApplyToSkillsChange(!applyToSkills) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = applyToSkills,
                    onCheckedChange = onApplyToSkillsChange,
                    modifier = Modifier.scale(0.9f)
                )
                Text("Применять ко всем навыкам характеристики", fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
             // Operation selector (+, -, =)
             SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(0.4f)) {
                 SegmentedButton(
                     selected = bonus.operation == BonusOperation.ADD,
                     onClick = { onUpdate(bonus.name, bonus.formula, bonus.isActive, BonusOperation.ADD, bonus.advantagePreference) },
                     shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                 ) { Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                 SegmentedButton(
                     selected = bonus.operation == BonusOperation.SUBTRACT,
                     onClick = { onUpdate(bonus.name, bonus.formula, bonus.isActive, BonusOperation.SUBTRACT, bonus.advantagePreference) },
                     shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                 ) { Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                 SegmentedButton(
                     selected = bonus.operation == BonusOperation.OVERRIDE,
                     onClick = { if (!isOverrideDisabled) onUpdate(bonus.name, bonus.formula, bonus.isActive, BonusOperation.OVERRIDE, bonus.advantagePreference) },
                     enabled = !isOverrideDisabled || bonus.operation == BonusOperation.OVERRIDE,
                     shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                 ) { Text("=", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
             }
             
             OutlinedTextField(
                 value = bonus.formula,
                 onValueChange = { onUpdate(bonus.name, it, bonus.isActive, bonus.operation, bonus.advantagePreference) },
                 label = { Text("Формула") },
                 modifier = Modifier.weight(0.6f),
                 shape = RoundedCornerShape(8.dp)
             )
        }
        
        if ((bonus as? StatBonus)?.type != StatBonusType.CHARACTERISTIC_VALUE) {
            // Advantage Preference
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Логика преимущества/помехи", fontSize = 12.sp, color = colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = bonus.advantagePreference == AdvantagePreference.NONE,
                        onClick = { onUpdate(bonus.name, bonus.formula, bonus.isActive, bonus.operation, AdvantagePreference.NONE) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 6)
                    ) { AdvantagePreferenceLabel(AdvantagePreference.NONE) }
                    SegmentedButton(
                        selected = bonus.advantagePreference == AdvantagePreference.IGNORE_ADVANTAGE,
                        onClick = { onUpdate(bonus.name, bonus.formula, bonus.isActive, bonus.operation, AdvantagePreference.IGNORE_ADVANTAGE) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 6)
                    ) { AdvantagePreferenceLabel(AdvantagePreference.IGNORE_ADVANTAGE) }
                    SegmentedButton(
                        selected = bonus.advantagePreference == AdvantagePreference.ALWAYS_ADVANTAGE,
                        onClick = { onUpdate(bonus.name, bonus.formula, bonus.isActive, bonus.operation, AdvantagePreference.ALWAYS_ADVANTAGE) },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 6)
                    ) { AdvantagePreferenceLabel(AdvantagePreference.ALWAYS_ADVANTAGE) }
                    SegmentedButton(
                        selected = bonus.advantagePreference == AdvantagePreference.IGNORE_DISADVANTAGE,
                        onClick = { onUpdate(bonus.name, bonus.formula, bonus.isActive, bonus.operation, AdvantagePreference.IGNORE_DISADVANTAGE) },
                        shape = SegmentedButtonDefaults.itemShape(index = 3, count = 6)
                    ) { AdvantagePreferenceLabel(AdvantagePreference.IGNORE_DISADVANTAGE) }
                    SegmentedButton(
                        selected = bonus.advantagePreference == AdvantagePreference.ALWAYS_DISADVANTAGE,
                        onClick = { onUpdate(bonus.name, bonus.formula, bonus.isActive, bonus.operation, AdvantagePreference.ALWAYS_DISADVANTAGE) },
                        shape = SegmentedButtonDefaults.itemShape(index = 4, count = 6)
                    ) { AdvantagePreferenceLabel(AdvantagePreference.ALWAYS_DISADVANTAGE) }
                    SegmentedButton(
                        selected = bonus.advantagePreference == AdvantagePreference.IGNORE_BOTH,
                        onClick = { onUpdate(bonus.name, bonus.formula, bonus.isActive, bonus.operation, AdvantagePreference.IGNORE_BOTH) },
                        shape = SegmentedButtonDefaults.itemShape(index = 5, count = 6)
                    ) { AdvantagePreferenceLabel(AdvantagePreference.IGNORE_BOTH) }
                }
            }
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = colorScheme.outlineVariant)
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
    onUpdateBonus: (bonusId: String, name: String, formula: String, isActive: Boolean, operation: BonusOperation, advantagePreference: AdvantagePreference) -> Unit,
    onDeleteBonus: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            
            SkillProficiencyToggle(
                isProficient = isProficient,
                isExpert = isExpert,
                proficiencyBonus = proficiencyBonus,
                onLevelSelected = onLevelSelected
            )
            
            val skillHasOverride = bonuses.any { it.isActive && it.operation == BonusOperation.OVERRIDE }
            bonuses.forEach { bonus ->
                BonusField(
                    bonus = bonus,
                    isOverrideDisabled = skillHasOverride && bonus.operation != BonusOperation.OVERRIDE,
                    onUpdate = { n, f, act, op, adv -> onUpdateBonus(bonus.id, n, f, act, op, adv) },
                    onDelete = { onDeleteBonus(bonus.id) }
                )
            }
            
            AddBonusButton(onClick = onAddBonus)
        }
    }
}
