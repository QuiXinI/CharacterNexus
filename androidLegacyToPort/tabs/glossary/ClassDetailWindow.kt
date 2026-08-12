package ru.quasaris.characters.master.tabs.glossary

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
import ru.quasaris.characters.master.backend.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDetailWindow(
    classFile: File,
    moduleManager: ModuleManager,
    onBack: () -> Unit
) {
    val gson = remember { GsonFactory.create() }
    val gameClass = remember(classFile) {
        try {
            gson.fromJson(classFile.readText(), GameClass::class.java)
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

    val context = androidx.compose.ui.platform.LocalContext.current
    val subclasses = remember(gameClass.id) {
        moduleManager.getSubclassesForClass(context, gameClass.id ?: "")
    }

    var selectedSubclass by remember { mutableStateOf<GameSubclass?>(null) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Right drawer state (custom)
    var showRightDrawer by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text("Умения класса", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    gameClass.features?.forEach { feat ->
                        NavigationDrawerItem(
                            label = { Text(feat.name ?: "Без названия") },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                // In future: scroll to feature
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(selectedSubclass?.name ?: gameClass.name ?: "Класс", fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    },
                    actions = {
                        if (subclasses.isNotEmpty()) {
                            IconButton(onClick = { showRightDrawer = true }) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "Подклассы")
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Список умений")
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    if (selectedSubclass == null) {
                        // Class Details
                        if (!gameClass.description.isNullOrBlank()) {
                            RichText(gameClass.description!!)
                            Spacer(Modifier.height(16.dp))
                        }
                        
                        gameClass.progressionTable?.let { table ->
                            GameTableRenderer(table)
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
    }
}
