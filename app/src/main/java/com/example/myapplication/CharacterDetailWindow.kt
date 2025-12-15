package com.example.myapplication

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.gson.Gson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterDetailWindow(
    character: Character?,
    onNavigateBack: () -> Unit,
    onDeleteCharacter: (Character) -> Unit,
    onSaveChanges: (Character) -> Unit
) {
    var name by remember { mutableStateOf(character?.name ?: "") }
    var characterClass by remember { mutableStateOf(character?.characterClass ?: "") }
    var order by remember { mutableStateOf(character?.order ?: "") }
    var level by remember { mutableStateOf(character?.level ?: "1") }
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }

    var strength by remember { mutableStateOf(character?.strength ?: "10") }
    var dexterity by remember { mutableStateOf(character?.dexterity ?: "10") }
    var constitution by remember { mutableStateOf(character?.constitution ?: "10") }
    var intelligence by remember { mutableStateOf(character?.intelligence ?: "10") }
    var wisdom by remember { mutableStateOf(character?.wisdom ?: "10") }
    var charisma by remember { mutableStateOf(character?.charisma ?: "10") }

    val context = LocalContext.current
    val gson = Gson()

    val fileCreatorLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        val currentCharacterState = character!!.copy(
            name = name, characterClass = characterClass, order = order, level = level,
            strength = strength, dexterity = dexterity, constitution = constitution,
            intelligence = intelligence, wisdom = wisdom, charisma = charisma
        )
        val jsonString = gson.toJson(currentCharacterState)

        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(jsonString.toByteArray())
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        tempImageUri = uri
    }

    if (character == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Персонаж не найден!")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(name) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        fileCreatorLauncher.launch("MP_${name}.json")
                    }) {
                        Icon(Icons.Filled.Download, contentDescription = "Скачать .json")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (tempImageUri != null) {
                        AsyncImage(
                            model = tempImageUri,
                            contentDescription = "Изображение персонажа",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (character.imageData != null) {
                        val decodedString = Base64.decode(character.imageData, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Иконка персонажа",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(painterResource(id = android.R.drawable.ic_menu_myplaces), "Заглушка")
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Имя") })
                    OutlinedTextField(value = characterClass, onValueChange = { characterClass = it }, label = { Text("Класс") })
                    OutlinedTextField(value = order, onValueChange = { order = it }, label = { Text("Вид") })
                    OutlinedTextField(value = level, onValueChange = { if (it.all(Char::isDigit)) level = it }, label = { Text("Уровень") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    strength = statEditor("Сила", strength)
                    dexterity = statEditor("Ловкость", dexterity)
                    constitution = statEditor("Телосложение", constitution)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    intelligence = statEditor("Интеллект", intelligence)
                    wisdom = statEditor("Мудрость", wisdom)
                    charisma = statEditor("Харизма", charisma)
                }
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { onDeleteCharacter(character) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("Удалить")
                }
                Button(
                    onClick = {
                         val imageDataString = tempImageUri?.let { uri ->
                            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                val bytes = inputStream.readBytes()
                                Base64.encodeToString(bytes, Base64.DEFAULT)
                            }
                        } ?: character.imageData

                        val updatedCharacter = character.copy(
                            name = name,
                            characterClass = characterClass,
                            order = order,
                            level = level,
                            imageData = imageDataString,
                            strength = strength,
                            dexterity = dexterity,
                            constitution = constitution,
                            intelligence = intelligence,
                            wisdom = wisdom,
                            charisma = charisma
                        )
                        onSaveChanges(updatedCharacter)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("Сохранить")
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun CharacterDetailWindowPreview() {
    MyApplicationTheme {
        val previewCharacter = Character(id = 1, name = "Арагорн", characterClass = "Следопыт", order = "Человек")
        CharacterDetailWindow(
            character = previewCharacter,
            onNavigateBack = {},
            onDeleteCharacter = {},
            onSaveChanges = { _ -> }
        )
    }
}
