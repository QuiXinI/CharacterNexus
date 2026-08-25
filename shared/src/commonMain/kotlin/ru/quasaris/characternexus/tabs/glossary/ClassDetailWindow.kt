package ru.quasaris.characternexus.tabs.glossary

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ru.quasaris.characternexus.*
import ru.quasaris.characternexus.backend.*
import ru.quasaris.characternexus.ui.NavNode
import ru.quasaris.characternexus.ui.NavigationPathManager
import ru.quasaris.characternexus.ui.BackHandler
import okio.Path
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDetailWindow(
    classFile: Path,
    moduleManager: ModuleManager,
    spellbookManager: SpellbookManager?,
    onTitleChange: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val gameClass = remember(classFile) {
        try {
            val content = JsonConfig.json.decodeFromString<DtoClassData>(platformFileSystem.read(classFile) { readUtf8() })
            onTitleChange(content.name ?: "Класс")
            content
        } catch (e: Exception) {
            println("Error decoding class: $e")
            null
        }
    }

    if (gameClass == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Ошибка загрузки класса")
        }
        return
    }

    // Mapping helper
    fun mapSubclass(sub: GameSubclass): DtoSubclassData {
        return DtoSubclassData(
            classId = sub.classId,
            className = sub.className,
            name = sub.name,
            description = when (val desc = sub.description) {
                is JsonArray -> desc.mapNotNull { (it as? JsonPrimitive)?.content }
                is JsonPrimitive -> listOf(desc.content)
                else -> emptyList()
            },
            features = sub.features?.map { feat ->
                DtoFeature(
                    name = feat.name,
                    type = feat.type,
                    levelLine = feat.levelLine,
                    description = when (val d = feat.description) {
                        is JsonArray -> d.mapNotNull { (it as? JsonPrimitive)?.content }
                        is JsonPrimitive -> listOf(d.content)
                        else -> emptyList()
                    }
                )
            } ?: emptyList(),
            linkedSubclasses = sub.linkedSubclasses ?: emptyList()
        )
    }

    val allSubclasses = remember(gameClass.id) {
        moduleManager.getSubclassesForClass(gameClass.id ?: "").map { mapSubclass(it) }
    }

    var selectedSubclass by remember { mutableStateOf<DtoSubclassData?>(null) }
    
    val linkedSubclasses = remember(selectedSubclass) {
        selectedSubclass?.linkedSubclasses?.mapNotNull { id ->
            moduleManager.getSubclassById(id)?.let { mapSubclass(it) }
        } ?: emptyList()
    }

    LaunchedEffect(gameClass, selectedSubclass) {
        val path = mutableListOf<NavNode>()
        path.add(NavNode("hub", "Глоссарий", 0) { onBack() })
        path.add(NavNode("cat", "Классы", 1) { onBack() })
        path.add(NavNode("class", gameClass.name ?: "Класс", 2) { 
            selectedSubclass = null 
        })
        
        if (selectedSubclass != null) {
            path.add(NavNode("subclass", selectedSubclass!!.name ?: "Подкласс", 3))
        }
        
        NavigationPathManager.updatePath("glossary", path)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ClassDetailScreen(
            classData = gameClass,
            selectedSubclass = selectedSubclass,
            linkedSubclasses = linkedSubclasses,
            allSubclasses = allSubclasses,
            onSubclassSelect = { 
                selectedSubclass = it
                onTitleChange(it?.name ?: gameClass.name ?: "Класс")
            },
            spellbookManager = spellbookManager
        )
    }
}
