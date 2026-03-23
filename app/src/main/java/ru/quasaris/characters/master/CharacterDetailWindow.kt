package ru.quasaris.characters.master

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ru.quasaris.characters.master.ui.theme.quasarisTheme
import com.google.gson.Gson
import kotlin.math.floor

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
    var experience by remember { mutableStateOf("50") }
    var nextLevelExp by remember { mutableStateOf("300") }
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
    ) { uri: Uri? -> tempImageUri = uri }

    if (character == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Персонаж не найден!")
        }
        return
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Column(modifier = Modifier.background(Color.White).statusBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                    Text(
                        text = name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    IconButton(onClick = { fileCreatorLauncher.launch("MP_${name}.json") }) {
                        Icon(Icons.Filled.Download, contentDescription = "Скачать .json")
                    }
                }
                
                // Experience Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .height(24.dp)
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(20.dp))
                        .background(Color.White, RoundedCornerShape(20.dp))
                        .padding(2.dp)
                ) {
                    Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 0.dp, bottomEnd = 0.dp))
                                .background(Color(0xFFEADDFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$level уровень", fontSize = 11.sp, color = Color.Black)
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 20.dp, bottomEnd = 20.dp))
                                .background(Color.White)
                        ) {
                            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.35f).background(Color(0xFFEADDFF)))
                            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Spacer(Modifier.weight(0.4f))
                                Text("$experience | $nextLevelExp", fontSize = 11.sp, color = Color.Black)
                                Spacer(Modifier.weight(0.6f))
                                Text("${level.toInt() + 1}", fontSize = 11.sp, color = Color.Black)
                            }
                        }
                    }
                }

                // Quick Stats
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatIconBoxDetail("15", R.drawable.ic_shield)
                        StatIconBoxDetail("+2", R.drawable.ic_sword)
                    }
                    Box(
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp).height(55.dp).border(1.5.dp, Color(0xFF00C46F), RoundedCornerShape(8.dp)).background(Color.White, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(painter = painterResource(id = R.drawable.ic_health), contentDescription = null, modifier = Modifier.size(32.dp), colorFilter = ColorFilter.tint(Color(0xFF00C46F)))
                            Spacer(Modifier.width(6.dp))
                            Text("10 / 10", color = Color(0xFF00C46F), fontSize = 16.sp)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatIconBoxDetail("1", R.drawable.ic_conditions)
                        StatIconBoxDetail("30", R.drawable.ic_speed)
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color.White)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color.White)
                    .verticalScroll(rememberScrollState())
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(12.dp).background(Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha = 0.15f), Color.Transparent))))

                // Characteristics Header
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).height(44.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFEADDFF)), contentAlignment = Alignment.Center) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Характеристики", fontSize = 12.sp, color = Color(0xFF4A4459), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text("Характеристики", fontSize = 18.sp, fontWeight = FontWeight.Normal, color = Color.Black, textAlign = TextAlign.Center, modifier = Modifier.weight(1.5f))
                        Text("Характеристики", fontSize = 12.sp, color = Color(0xFF4A4459), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                    }
                }

                // Profile Section
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.LightGray).clickable { imagePickerLauncher.launch("image/*") }, contentAlignment = Alignment.Center) {
                        if (tempImageUri != null) {
                            AsyncImage(model = tempImageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else if (character.imageData != null) {
                            val decodedString = Base64.decode(character.imageData, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                            Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Icon(Icons.Default.Person, null, modifier = Modifier.size(40.dp))
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Имя") }, modifier = Modifier.fillMaxWidth().height(56.dp))
                        OutlinedTextField(value = characterClass, onValueChange = { characterClass = it }, label = { Text("Класс") }, modifier = Modifier.fillMaxWidth().height(56.dp))
                    }
                }

                // Compact Stats Grid
                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCardDetail("Сила", strength, Modifier.weight(1f)) { strength = it }
                        StatCardDetail("Интеллект", intelligence, Modifier.weight(1f)) { intelligence = it }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCardDetail("Ловкость", dexterity, Modifier.weight(1f)) { dexterity = it }
                        StatCardDetail("Мудрость", wisdom, Modifier.weight(1f)) { wisdom = it }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCardDetail("Телосложение", constitution, Modifier.weight(1f)) { constitution = it }
                        StatCardDetail("Харизма", charisma, Modifier.weight(1f)) { charisma = it }
                    }
                }

                Spacer(Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { onDeleteCharacter(character) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(8.dp)) { Text("Удалить") }
                    Button(onClick = {
                        val imageDataString = tempImageUri?.let { uri -> context.contentResolver.openInputStream(uri)?.use { Base64.encodeToString(it.readBytes(), Base64.DEFAULT) } } ?: character.imageData
                        onSaveChanges(character.copy(name = name, characterClass = characterClass, order = order, level = level, imageData = imageDataString, strength = strength, dexterity = dexterity, constitution = constitution, intelligence = intelligence, wisdom = wisdom, charisma = charisma))
                    }, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(8.dp)) { Text("Сохранить") }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun StatIconBoxDetail(value: String, iconRes: Int) {
    Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
        if (iconRes == R.drawable.ic_sword) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(painter = painterResource(id = R.drawable.ic_sword), contentDescription = null, modifier = Modifier.fillMaxSize(), colorFilter = ColorFilter.tint(Color(0xFFD0BCFF)))
                Image(painter = painterResource(id = R.drawable.ic_sword), contentDescription = null, modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = -1f), colorFilter = ColorFilter.tint(Color(0xFFD0BCFF)))
            }
        } else {
            Image(painter = painterResource(id = iconRes), contentDescription = null, modifier = Modifier.fillMaxSize(), colorFilter = ColorFilter.tint(Color(0xFFD0BCFF)))
        }
        Text(text = value, fontSize = 15.sp, color = Color.Black, fontWeight = FontWeight.Bold, style = TextStyle(shadow = Shadow(color = Color.White, offset = Offset(0f, 0f), blurRadius = 8f)))
    }
}

@Composable
fun StatCardDetail(label: String, value: String, modifier: Modifier = Modifier, onValueChange: (String) -> Unit) {
    val score = value.toIntOrNull() ?: 10
    val modStr = if (floor((score - 10) / 2.0).toInt() >= 0) "+${floor((score - 10) / 2.0).toInt()}" else floor((score - 10) / 2.0).toInt().toString()
    Box(modifier = modifier.height(104.dp).shadow(2.dp, RoundedCornerShape(8.dp)).background(Color.White, RoundedCornerShape(8.dp)).border(1.dp, Color(0xFFCAC4D0).copy(0.5f), RoundedCornerShape(8.dp)).padding(8.dp)) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Box(modifier = Modifier.align(Alignment.BottomStart).padding(start = 25.dp, bottom = 5.dp).size(40.dp).rotate(-45f).clip(RoundedCornerShape(12.dp)).background(Color(0xFFD0BCFF)), contentAlignment = Alignment.Center) {
            Text(modStr, modifier = Modifier.rotate(45f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(modifier = Modifier.align(Alignment.TopEnd), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF3F1F8)).border(1.dp, Color.Black.copy(0.05f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                BasicTextField(value = value, onValueChange = { val num = it.filter { it.isDigit() }.toIntOrNull(); if (it.isEmpty()) onValueChange(""); else if (num != null && num in 1..30) onValueChange(it.filter { it.isDigit() }) }, textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 16.sp, fontWeight = FontWeight.SemiBold), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.width(36.dp))
            }
            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(Color.White).border(1.dp, Color.Black.copy(0.05f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Text(modStr, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
