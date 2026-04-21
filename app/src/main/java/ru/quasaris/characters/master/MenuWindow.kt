package ru.quasaris.characters.master

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.quasaris.characters.master.ui.theme.quasarisTheme
import com.google.gson.Gson

@Composable
fun MenuWindow(
    modifier: Modifier = Modifier,
    characters: List<Character>,
    onNavigateToCreate: () -> Unit,
    onCharacterClick: (Int) -> Unit,
    onImportCharacter: (Character) -> Unit
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
                val importedCharacter = gson.fromJson(jsonString, Character::class.java)
                onImportCharacter(importedCharacter.copy(id = (0..100000).random()))
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
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (character.imageData != null) {
                    val decodedString = Base64.decode(character.imageData, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                    Image(
                        bitmap = bitmap.asImageBitmap(),
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
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${character.characterClass}, ${character.order}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MenuWindowPreview() {
    quasarisTheme {
        val previewCharacters = listOf(
            Character(1, "Гаррик", "Воин", "Человек"),
            Character(2, "Лиара", "Плут", "Эльф", imageData = null)
        )
        MenuWindow(
            characters = previewCharacters,
            onNavigateToCreate = {},
            onCharacterClick = {},
            onImportCharacter = {}
        )
    }
}
