package com.example.myapplication

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.gson.Gson

data class Character(
    val id: Int,
    val name: String,
    val characterClass: String,
    val order: String,
    val imageUriString: String? = null,
    val level: String = "1",
    val strength: String = "10",
    val dexterity: String = "10",
    val constitution: String = "10",
    val intelligence: String = "10",
    val wisdom: String = "10",
    val charisma: String = "10"
)

@Composable
fun MenuWindow(
    modifier: Modifier = Modifier,
    characters: List<Character>,
    onNavigateToCreate: () -> Unit,
    onCharacterClick: (Int) -> Unit,
    onImportCharacter: (Character) -> Unit // ИСПРАВЛЕНИЕ 1: Добавлен параметр
) {
    val context = LocalContext.current
    val gson = Gson()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
            if (jsonString != null) {
                // ИСПРАВЛЕНИЕ 2: Используем наш data class `Character`
                val importedCharacter = gson.fromJson(jsonString, Character::class.java)
                // ИСПРАВЛЕНИЕ 3: Вызываем `onImportCharacter` и используем `copy()`
                onImportCharacter(importedCharacter.copy(id = (0..100000).random(), imageUriString = null))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Button(
                onClick = { filePickerLauncher.launch("application/json") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = "Загрузить")
                Spacer(Modifier.width(8.dp))
                Text("Загрузить .json")
            }
            Spacer(Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(characters) { character ->
                    CharacterCard(
                        character = character,
                        onClick = { onCharacterClick(character.id) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onNavigateToCreate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Создать нового персонажа", fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun CharacterCard(
    character: Character,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                if (character.imageUriString != null) {
                    AsyncImage(
                        model = Uri.parse(character.imageUriString),
                        contentDescription = "Иконка персонажа",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = android.R.drawable.ic_menu_myplaces),
                        contentDescription = "Иконка персонажа",
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = character.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${character.characterClass}, ${character.order}",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MenuWindowPreview() {
    MyApplicationTheme {
        val previewCharacters = listOf(
            Character(1, "Гаррик", "Воин", "Человек"),
            Character(2, "Лиара", "Плут", "Эльф", imageUriString = null)
        )
        // ИСПРАВЛЕНИЕ 4: Добавлен `onImportCharacter` в Preview
        MenuWindow(
            characters = previewCharacters,
            onNavigateToCreate = {},
            onCharacterClick = {},
            onImportCharacter = {}
        )
    }
}