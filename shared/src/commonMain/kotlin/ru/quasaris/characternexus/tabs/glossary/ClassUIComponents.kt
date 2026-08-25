package ru.quasaris.characternexus.tabs.glossary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ru.quasaris.characternexus.ui.outerShadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.quasaris.characternexus.backend.SpellbookManager
import ru.quasaris.characternexus.tabs.spells.SpellCardItem

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ClassTraitsSection(traits: DtoClassTraits) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = traits.title ?: "Особенности класса",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            traits.parsed?.forEach { (key, value) ->
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = {
                        Text(
                            text = "$key: $value",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
            
            if (traits.parsed == null) {
                traits.lines.forEach { line ->
                    OutlinedCard(
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = line,
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressionTableHeader(headers: List<String>, scrollState: ScrollState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .horizontalScroll(scrollState)
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        headers.forEachIndexed { index, header ->
            Text(
                text = header,
                modifier = Modifier
                    .width(if (index == 0) 80.dp else 120.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = if (header.any { it.isDigit() }) TextAlign.Center else TextAlign.Start
            )
        }
    }
}

@Composable
fun ProgressionTableRow(
    row: List<String>, 
    headers: List<String>, 
    scrollState: ScrollState, 
    isZebra: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isZebra) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) 
                else MaterialTheme.colorScheme.surface
            )
            .horizontalScroll(scrollState)
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        row.forEachIndexed { colIndex, cell ->
            Text(
                text = cell,
                modifier = Modifier
                    .width(if (colIndex == 0) 80.dp else 120.dp),
                style = MaterialTheme.typography.bodySmall,
                textAlign = if (headers.getOrNull(colIndex)?.any { it.isDigit() } == true) 
                                TextAlign.Center else TextAlign.Start
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureCard(feature: DtoFeature) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .outerShadow(RoundedCornerShape(12.dp), blur = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = feature.name ?: "Умение",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                feature.levelLine?.let {
                    AssistChip(
                        onClick = {},
                        label = { Text(it, fontSize = 10.sp) },
                        modifier = Modifier.height(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            feature.description.forEach { paragraph ->
                Text(
                    text = paragraph,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SpellcastingView(spellcasting: DtoSpellcasting, spellbookManager: SpellbookManager?) {
    var searchQuery by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            placeholder = { Text("Поиск заклинаний...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = { 
                IconButton(onClick = { /* Open filters if needed */ }) {
                    Icon(Icons.Default.FilterList, null)
                }
            },
            shape = RoundedCornerShape(12.dp)
        )

        spellcasting.groups.forEach { group ->
            val filteredItems = remember(group.items, searchQuery) {
                if (searchQuery.isBlank()) group.items
                else group.items.filter { it.contains(searchQuery, ignoreCase = true) }
            }

            if (filteredItems.isNotEmpty()) {
                var isExpanded by remember { mutableStateOf(true) }
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    onClick = { isExpanded = !isExpanded }
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${group.group ?: "Группа"} (${filteredItems.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }
                        
                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                filteredItems.forEach { spellName ->
                                    val spellCard = remember(spellName) {
                                        spellbookManager?.resolveRef(spellName)
                                    }
                                    
                                    if (spellCard != null) {
                                        var cardExpanded by remember { mutableStateOf(false) }
                                        SpellCardItem(
                                            spell = spellCard,
                                            isExpanded = cardExpanded,
                                            onToggleExpand = { cardExpanded = !cardExpanded },
                                            isEditable = false,
                                            isSelected = false
                                        )
                                        Spacer(Modifier.height(8.dp))
                                    } else {
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text(spellName, style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClassDetailScreen(
    classData: DtoClassData,
    selectedSubclass: DtoSubclassData? = null,
    linkedSubclasses: List<DtoSubclassData> = emptyList(),
    allSubclasses: List<DtoSubclassData> = emptyList(),
    onSubclassSelect: (DtoSubclassData?) -> Unit = {},
    spellbookManager: SpellbookManager? = null
) {
    var mainTabIndex by remember { mutableStateOf(0) }
    var subclassTabIndex by remember { mutableStateOf(0) }
    var showSubclassSwitcher by remember { mutableStateOf(false) }
    
    val tabs = remember(classData) {
        mutableListOf("Класс").apply {
            if (classData.spellcasting != null) add("Заклинания")
            if (classData.invocations != null) add("Воззвания")
            if (classData.itemPlans != null) add("Планы")
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Main Tabs + Subclass Toggle
        PrimaryTabRow(selectedTabIndex = if (mainTabIndex == -1) tabs.size else mainTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = mainTabIndex == index,
                    onClick = { 
                        mainTabIndex = index
                        showSubclassSwitcher = false 
                    },
                    text = { Text(title) }
                )
            }
            // Subclass Selector Button
            Tab(
                selected = showSubclassSwitcher,
                onClick = { 
                    showSubclassSwitcher = !showSubclassSwitcher
                    if (showSubclassSwitcher) mainTabIndex = -1
                    else mainTabIndex = 0
                },
                text = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Подклассы")
                        Icon(
                            if (showSubclassSwitcher) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            null
                        )
                    }
                }
            )
        }

        // Subclass Switcher Row (Scrollable)
        if (showSubclassSwitcher) {
            PrimaryScrollableTabRow(
                selectedTabIndex = allSubclasses.indexOfFirst { it.name == selectedSubclass?.name }.let { if (it == -1 && selectedSubclass == null) 0 else it + 1 },
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
            ) {
                Tab(
                    selected = selectedSubclass == null,
                    onClick = { 
                        onSubclassSelect(null)
                        showSubclassSwitcher = false
                        mainTabIndex = 0
                    },
                    text = { Text("Основной класс") }
                )
                allSubclasses.forEach { sub ->
                    Tab(
                        selected = selectedSubclass?.name == sub.name,
                        onClick = {
                            onSubclassSelect(sub)
                            showSubclassSwitcher = false
                            mainTabIndex = 0
                        },
                        text = { Text(sub.name ?: "Без имени") }
                    )
                }
            }
        }

        // Sub-Subclass Nested Tabs (linked subclasses)
        if (selectedSubclass != null && linkedSubclasses.isNotEmpty() && mainTabIndex == 0 && !showSubclassSwitcher) {
            val subTabs = remember(selectedSubclass, linkedSubclasses) {
                listOf(selectedSubclass.name ?: "Подкласс") + linkedSubclasses.map { it.name ?: "Доп." }
            }
            
            SecondaryTabRow(selectedTabIndex = subclassTabIndex) {
                subTabs.forEachIndexed { index, title ->
                    Tab(
                        selected = subclassTabIndex == index,
                        onClick = { subclassTabIndex = index },
                        text = { Text(title, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }
        }

        val progressionScrollState = rememberScrollState()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showSubclassSwitcher) return@LazyColumn

            when {
                mainTabIndex == 0 -> {
                    if (selectedSubclass != null) {
                        val currentSub = if (subclassTabIndex == 0) selectedSubclass else linkedSubclasses[subclassTabIndex - 1]
                        
                        item {
                            Text(
                                text = currentSub.name ?: "",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black
                            )
                        }
                        
                        items(currentSub.description) { paragraph ->
                            Text(paragraph, style = MaterialTheme.typography.bodyMedium)
                        }
                        
                        items(currentSub.features) { feat ->
                            FeatureCard(feat)
                        }
                    } else {
                        // Base Class View
                        classData.classTab?.let { tab ->
                            item {
                                Text(
                                    text = classData.name ?: "Класс",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            
                            tab.progressionTable?.let { table ->
                                stickyHeader {
                                    ProgressionTableHeader(table.headers, progressionScrollState)
                                }
                                itemsIndexed(table.rows) { index, row ->
                                    ProgressionTableRow(row, table.headers, progressionScrollState, index % 2 != 0)
                                }
                            }

                            tab.classTraits?.let { traits ->
                                item { ClassTraitsSection(traits) }
                            }
                            
                            items(tab.features) { feat ->
                                FeatureCard(feat)
                            }
                        }
                    }
                }
                mainTabIndex != -1 && tabs.getOrNull(mainTabIndex) == "Заклинания" -> {
                    classData.spellcasting?.let {
                        item { SpellcastingView(it, spellbookManager) }
                    }
                }
                mainTabIndex != -1 && tabs.getOrNull(mainTabIndex) == "Воззвания" -> {
                    classData.invocations?.let { inv ->
                        items(inv.items) { item ->
                            FeatureCard(DtoFeature(item.name, null, item.requirements, item.description))
                        }
                    }
                }
                mainTabIndex != -1 && tabs.getOrNull(mainTabIndex) == "Планы" -> {
                    classData.itemPlans?.let { item { SpellcastingView(DtoSpellcasting(it.tabTitle, it.groups), spellbookManager) } }
                }
            }
            
            item { Spacer(modifier = Modifier.height(64.dp)) }
        }
    }
}
