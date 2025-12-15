package ru.quasaris.characters.master

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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ru.quasaris.characters.master.ui.theme.quasarisTheme
import kotlin.math.floor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWindow(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    onCharacterCreate: (Character) -> Unit
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

    val context = LocalContext.current

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
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

            Button(
                onClick = {
                     val imageDataString = tempImageUri?.let { uri ->
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            val bytes = inputStream.readBytes()
                            Base64.encodeToString(bytes, Base64.DEFAULT)
                        }
                    }

                    val newCharacter = Character(
                        id = (0..100000).random(),
                        name = name,
                        characterClass = characterClass,
                        order = order.ifEmpty { "Не указан" },
                        imageData = imageDataString,
                        level = level.ifEmpty { "1" },
                        strength = strength,
                        dexterity = dexterity,
                        constitution = constitution,
                        intelligence = intelligence,
                        wisdom = wisdom,
                        charisma = charisma
                    )
                    onCharacterCreate(newCharacter)
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
fun statEditor(label: String, value: String): String {
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
            fontSize = 18.sp
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
                modifier = Modifier.weight(2f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = modifierString,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }
    }
    return statValue
}


@Preview(showBackground = true, widthDp = 380)
@Composable
fun CreateWindowPreview() {
    quasarisTheme {
        CreateWindow(
            onNavigateBack = {},
            onCharacterCreate = { _ -> }
        )
    }
}
