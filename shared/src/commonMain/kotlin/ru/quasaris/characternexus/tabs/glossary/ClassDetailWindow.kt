package ru.quasaris.characternexus.tabs.glossary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import ru.quasaris.characternexus.*
import ru.quasaris.characternexus.backend.*
import ru.quasaris.characternexus.ui.NavNode
import ru.quasaris.characternexus.ui.NavigationPathManager
import okio.Path
import androidx.compose.ui.platform.LocalUriHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDetailWindow(
    classFile: Path,
    moduleManager: ModuleManager,
    onTitleChange: (String) -> Unit,
    onBack: () -> Unit
) {
    val gameClass = remember(classFile) {
        try {
            val content = JsonConfig.json.decodeFromString<GameClass>(platformFileSystem.read(classFile) { readUtf8() })
            onTitleChange(content.name ?: "Класс")
            content
        } catch (e: Exception) {
            null
        }
    }

    if (gameClass == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Ошибка загрузки класса")
        }
        return
    }

    val subclasses = remember(gameClass.id) {
        moduleManager.getSubclassesForClass(gameClass.id ?: "")
    }

    var selectedSubclass by remember { mutableStateOf<GameSubclass?>(null) }
    
    // Update title when subclass changes
    LaunchedEffect(selectedSubclass) {
        onTitleChange(selectedSubclass?.name ?: gameClass.name ?: "Класс")
    }

    // Report TOC to side menu
    LaunchedEffect(gameClass, selectedSubclass) {
        val path = mutableListOf<NavNode>()
        path.add(NavNode("hub", "Глоссарий", 0) { onBack() })
        path.add(NavNode("cat", "Классы", 1) { onBack() })
        path.add(NavNode("class", gameClass.name ?: "Класс", 2) { selectedSubclass = null })
        
        if (selectedSubclass != null) {
            path.add(NavNode("subclass", selectedSubclass!!.name ?: "Подкласс", 3))
            selectedSubclass!!.features?.forEach { feat ->
                path.add(NavNode("feat_${feat.id}", feat.name ?: "Умение", 4))
            }
        } else {
            gameClass.features?.forEach { feat ->
                path.add(NavNode("feat_${feat.id}", feat.name ?: "Умение", 3))
            }
        }
        NavigationPathManager.updatePath("glossary", path)
    }

    // Right drawer state (custom for subclasses)
    var showRightDrawer by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            if (selectedSubclass == null) {
                // Class Details
                Text(gameClass.name ?: "Класс", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                
                Row(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val hitDiceText = gameClass.hitDice ?: gameClass.hitDie
                    hitDiceText?.let { Text("Кость Хитов: ${if (it.startsWith("d")) it else "d$it"}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold) }
                    gameClass.primaryAbility?.let { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Осн. Характеристика: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            RichText(it) // Handling ref in ability if present
                        }
                    }
                }

                if (!gameClass.description.isNullOrBlank()) {
                    RichText(gameClass.description!!)
                    Spacer(Modifier.height(16.dp))
                }

                // HP and Proficiencies
                if (gameClass.hpAt1stLevel != null || gameClass.hpAtHigherLevels != null) {
                    Text("Хиты", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    gameClass.hpAt1stLevel?.let { 
                        Row(modifier = Modifier.padding(top = 4.dp)) {
                            Text("На 1 уровне: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            RichText(it)
                        }
                    }
                    gameClass.hpAtHigherLevels?.let { 
                        Row(modifier = Modifier.padding(top = 2.dp)) {
                            Text("На высших уровнях: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            RichText(it)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                if (gameClass.savingThrowProficiencies != null || gameClass.armorProficiencies != null || gameClass.weaponProficiencies != null) {
                    Text("Владения", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    gameClass.armorProficiencies?.let { 
                        Row(modifier = Modifier.padding(top = 4.dp)) {
                            Text("Доспехи: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            RichText(it.joinToString(", "))
                        }
                    }
                    gameClass.weaponProficiencies?.let { 
                        Row(modifier = Modifier.padding(top = 2.dp)) {
                            Text("Оружие: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            RichText(it.joinToString(", "))
                        }
                    }
                    gameClass.savingThrowProficiencies?.let { 
                        Row(modifier = Modifier.padding(top = 2.dp)) {
                            Text("Спасброски: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            RichText(it.joinToString(", "))
                        }
                    }
                    gameClass.skillProficiencies?.let { skills ->
                        Row(modifier = Modifier.padding(top = 2.dp)) {
                            Text("Навыки: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            val from = skills.from?.joinToString(", ") ?: ""
                            RichText("Выберите ${skills.choose} из $from")
                        }
                    }
                    gameClass.startingEquipment?.let { equip ->
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Text("Начальное снаряжение", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            equip.options?.forEach { opt ->
                                RichText("• $opt")
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                
                val table = remember(gameClass) {
                    if (gameClass.progressionTable != null && gameClass.headers != null) {
                        GameTable(
                            title = "Таблица развития: ${gameClass.name}",
                            schema = TableSchema(columns = gameClass.headers),
                            rows = gameClass.progressionTable
                        )
                    } else null
                }

                table?.let {
                    GameTableRenderer(it)
                    Spacer(Modifier.height(16.dp))
                }

                gameClass.features?.forEach { feat ->
                    FeatureRenderer(feat)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            } else {
                // Subclass Details
                if (!selectedSubclass!!.description.isNullOrBlank()) {
                    RichText(selectedSubclass!!.description!!)
                    Spacer(Modifier.height(16.dp))
                }
                selectedSubclass!!.features?.forEach { feat ->
                    FeatureRenderer(feat)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
                
                Button(
                    onClick = { selectedSubclass = null },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Text("Вернуться к основному классу")
                }
            }
            Spacer(Modifier.height(80.dp))
        }

        // Custom Right Drawer button (re-integrated since TopAppBar actions are now in parent)
        if (subclasses.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomEnd) {
                FloatingActionButton(onClick = { showRightDrawer = true }) {
                    Icon(Icons.Default.AutoAwesome, null)
                }
            }
        }

        // Custom Right Drawer
        if (showRightDrawer) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showRightDrawer = false }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(280.dp)
                        .align(Alignment.CenterEnd)
                        .clickable(enabled = false) {}, // Prevent click-through
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Подклассы", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        subclasses.forEach { sub ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { 
                                        selectedSubclass = sub
                                        showRightDrawer = false
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedSubclass?.id == sub.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(sub.name ?: "Без названия", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}
