package ru.quasaris.characters.master.tabs.glossary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.quasaris.characters.master.backend.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlossaryDetailWindow(
    file: File,
    category: ru.quasaris.characters.master.GlossaryCategory,
    onBack: () -> Unit
) {
    val gson = remember { GsonFactory.create() }
    val content = remember(file) {
        try {
            val json = file.readText()
            when (category) {
                ru.quasaris.characters.master.GlossaryCategory.SPECIES -> gson.fromJson(json, GameSpecies::class.java)
                ru.quasaris.characters.master.GlossaryCategory.FEATS -> gson.fromJson(json, GameFeat::class.java)
                else -> null
            }
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = when (content) {
                            is GameSpecies -> content.name ?: "Вид"
                            is GameFeat -> content.name ?: "Черта"
                            else -> "Детали"
                        }, 
                        fontWeight = FontWeight.Black 
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
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
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}
