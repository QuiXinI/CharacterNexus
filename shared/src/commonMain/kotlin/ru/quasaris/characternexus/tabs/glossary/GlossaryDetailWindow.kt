package ru.quasaris.characternexus.tabs.glossary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.quasaris.characternexus.*
import ru.quasaris.characternexus.ui.GlossaryCategory
import ru.quasaris.characternexus.ui.NavNode
import ru.quasaris.characternexus.ui.NavigationPathManager
import ru.quasaris.characternexus.ui.BackHandler
import ru.quasaris.characternexus.backend.*
import ru.quasaris.characternexus.model.SpellCard
import okio.Path

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlossaryDetailWindow(
    file: Path,
    category: GlossaryCategory,
    onTitleChange: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val content = remember(file) {
        try {
            val json = platformFileSystem.read(file) { readUtf8() }
            val item = when (category) {
                GlossaryCategory.SPECIES -> JsonConfig.json.decodeFromString<GameSpecies>(json)
                GlossaryCategory.FEATS -> JsonConfig.json.decodeFromString<GameFeat>(json)
                GlossaryCategory.SPELLS -> JsonConfig.json.decodeFromString<SpellCard>(json)
                else -> null
            }
            val name = when (item) {
                is GameSpecies -> item.name
                is GameFeat -> item.name
                is SpellCard -> item.name
                else -> "Детали"
            } ?: "Детали"
            onTitleChange(name)
            item
        } catch (e: Exception) {
            null
        }
    }

    if (content == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Ошибка загрузки")
        }
        return
    }

    // Report path labels
    LaunchedEffect(content) {
        val path = mutableListOf<NavNode>()
        path.add(NavNode("hub", "Глоссарий", 0) { onBack() })
        path.add(NavNode("cat", category.title, 1) { onBack() })
        val name = when (content) {
            is GameSpecies -> content.name
            is GameFeat -> content.name
            is SpellCard -> content.name
            else -> "Детали"
        } ?: "Детали"
        path.add(NavNode("detail", name, 2))
        
        // No features in sidebar as requested
        
        NavigationPathManager.updatePath("glossary", path)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        when (content) {
            is GameSpecies -> {
                GlossaryImage(content.image)
                if (!content.description.isNullOrBlank()) {
                    RichText(content.description!!)
                    Spacer(Modifier.height(16.dp))
                }
                
                if (content.creatureType != null) {
                    Text("Тип существа: ${content.creatureType}", style = MaterialTheme.typography.bodySmall)
                }
                if (content.size != null) {
                    Text("Размер: ${content.size}", style = MaterialTheme.typography.bodySmall)
                }
                if (content.speed != null) {
                    Text("Скорость: ${content.speed}", style = MaterialTheme.typography.bodySmall)
                }
                
                Spacer(Modifier.height(16.dp))
                content.tables?.forEach { table ->
                    GameTableRenderer(table)
                    Spacer(Modifier.height(8.dp))
                }
                
                content.features?.forEach { feat ->
                    FeatureRenderer(feat)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
            is GameFeat -> {
                if (!content.description.isNullOrBlank()) {
                    RichText(content.description!!)
                    Spacer(Modifier.height(16.dp))
                }
                
                if (content.prerequisites != null) {
                    Text("Требования: ${content.prerequisites}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                }
                
                content.features?.forEach { feat ->
                    FeatureRenderer(feat)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
            is SpellCard -> {
                SpellCardRenderer(content)
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}
