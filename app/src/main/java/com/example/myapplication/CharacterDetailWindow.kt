package com.example.myapplication

import android.net.Uri
import androidx.compose.material.icons.filled.Download
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
// ^^^ Добавьте этот импорт
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
// ^^^ Добавьте этот импорт
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.gson.Gson
// ^^^ Добавьте этот импорт

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterDetailWindow(
    character: Character?,
    onNavigateBack: () -> Unit,
    onDeleteCharacter: (Character) -> Unit,
    onSaveChanges: (Character, Uri?) -> Unit // Лямбда для сохранения изменений
) {
    // Состояния для хранения изменяемых данных
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

        // Создаем объект персонажа на основе текущих данных в полях
        val currentCharacterState = character!!.copy(
            name = name, characterClass = characterClass, order = order, level = level,
            strength = strength, dexterity = dexterity, constitution = constitution,
            intelligence = intelligence, wisdom = wisdom, charisma = charisma
        )
        val jsonString = gson.toJson(currentCharacterState)

        // Записываем JSON в выбранный файл
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
        // Заглушка, если персонаж не найден
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
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // Запускаем лаунчер с именем файла
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
            // --- Блок с основной информацией ---
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
                    val imageToShow = tempImageUri ?: character.imageUriString?.let { Uri.parse(it) }
                    if (imageToShow != null) {
                        AsyncImage(
                            model = imageToShow,
                            contentDescription = "Изображение персонажа",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(painterResource(id = android.R.drawable.ic_menu_myplaces), "Заглушка")
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Имя") }, textStyle = TextStyle(color = Color.White))
                    OutlinedTextField(value = characterClass, onValueChange = { characterClass = it }, label = { Text("Класс") }, textStyle = TextStyle(color = Color.White))
                    OutlinedTextField(value = order, onValueChange = { order = it }, label = { Text("Вид") }, textStyle = TextStyle(color = Color.White))
                    OutlinedTextField(value = level, onValueChange = { if (it.all(Char::isDigit)) level = it }, label = { Text("Уровень") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), textStyle = TextStyle(color = Color.White))
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Передаем белый цвет в StatEditor
                    strength = StatEditor("Сила", strength, textColor = Color.White)
                    dexterity = StatEditor("Ловкость", dexterity, textColor = Color.White)
                    constitution = StatEditor("Телосложение", constitution, textColor = Color.White)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    intelligence = StatEditor("Интеллект", intelligence, textColor = Color.White)
                    wisdom = StatEditor("Мудрость", wisdom, textColor = Color.White)
                    charisma = StatEditor("Харизма", charisma, textColor = Color.White)
                }
            }

            Spacer(Modifier.weight(1f))

            // --- Кнопки управления ---
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
                        val updatedCharacter = character.copy(
                            name = name,
                            characterClass = characterClass,
                            order = order,
                            level = level,
                            strength = strength,
                            dexterity = dexterity,
                            constitution = constitution,
                            intelligence = intelligence,
                            wisdom = wisdom,
                            charisma = charisma
                        )
                        onSaveChanges(updatedCharacter, tempImageUri)
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
            onSaveChanges = { _, _ -> }
        )
    }
}