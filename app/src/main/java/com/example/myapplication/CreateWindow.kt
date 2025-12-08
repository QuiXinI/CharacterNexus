package com.example.myapplication

import android.net.Uri
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlin.math.floor
import kotlin.text.toInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWindow(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    onCharacterCreate: (Character, Uri?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var characterClass by remember { mutableStateOf("") }
    var order by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("1") }
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }

    var strength by remember { mutableStateOf("10") }
    var dexterity by remember { mutableStateOf("10") }
    var constitution by remember { mutableStateOf("10") }
    var intelligence by remember { mutableStateOf("10") }
    var wisdom by remember { mutableStateOf("10") }
    var charisma by remember { mutableStateOf("10") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        tempImageUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Создание персонажа") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
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
                    } else {
                        Image(
                            painter = painterResource(id = android.R.drawable.ic_menu_camera),
                            contentDescription = "Загрузить изображение",
                            modifier = Modifier.size(50.dp)
                        )
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

            Button(
                onClick = {
                    val newCharacter = Character(
                        id = (0..100000).random(),
                        name = name,
                        characterClass = characterClass,
                        order = order.ifEmpty { "Не указан" },
                        imageUriString = null,
                        level = level.ifEmpty { "1" },
                        strength = strength,
                        dexterity = dexterity,
                        constitution = constitution,
                        intelligence = intelligence,
                        wisdom = wisdom,
                        charisma = charisma
                    )
                    onCharacterCreate(newCharacter, tempImageUri)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .height(50.dp)
            ) {
                Text("Сохранить персонажа", fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun StatEditor(label: String, value: String, textColor: Color = Color.Black): String {
    var statValue by remember { mutableStateOf(value) }

    fun calculateModifier(score: Int): Int {
        return floor((score - 10) / 2.0).toInt()
    }

    val score = statValue.toIntOrNull() ?: 10
    val modifier = calculateModifier(score)
    val modifierString = if (modifier >= 0) "+$modifier" else modifier.toString()

    Column {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = textColor
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = statValue,
                onValueChange = { newValue ->
                    val filtered = newValue.filter { it.isDigit() }
                    if (filtered.isEmpty() || (filtered.toIntOrNull() ?: 0) in 1..30) {
                        statValue = filtered
                    }
                },
                label = { Text("Показатель") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(2f),
                textStyle = TextStyle(color = Color.White)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = modifierString,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = Color.White
            )
        }
    }
    return statValue
}


@Preview(showBackground = true, widthDp = 380)
@Composable
fun CreateWindowPreview() {
    MyApplicationTheme {
        CreateWindow(
            onNavigateBack = {},
            onCharacterCreate = { _, _ -> }
        )
    }
}