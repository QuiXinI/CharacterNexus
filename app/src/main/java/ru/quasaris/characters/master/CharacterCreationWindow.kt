package ru.quasaris.characters.master

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.quasaris.characters.master.ui.MorphingPolygonShape
import ru.quasaris.characters.master.ui.theme.quasarisTheme
import ru.quasaris.characters.master.backend.ImageManager
import ru.quasaris.characters.master.ui.cropper.AvatarCropperWindow

import dev.chrisbanes.haze.HazeState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterCreationWindow(
    onNavigateBack: () -> Unit,
    onCharacterCreate: (Character) -> Unit,
    hazeState: HazeState? = null,
    forceBlurEnabled: Boolean = false
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
    var name by remember { mutableStateOf("") }
    var charClass by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("1") }
    var order by remember { mutableStateOf("")}
    var imageData by remember { mutableStateOf<String?>(null) }
    var bitmapToCrop by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    val baseStats = remember { mutableStateListOf(8, 8, 8, 8, 8, 8) }
    val statNames = listOf("СИЛ", "ЛОВ", "ТЕЛ", "ИНТ", "МДР", "ХАР")

    val indicatorStates = remember { mutableStateListOf(0, 0, 0, 0, 0, 0) }
    val totalIndicatorTaps = indicatorStates.sum()

    val totalPoints = 27
    val spentPoints = baseStats.sumOf { value ->
        when (value) {
            8 -> 0; 9 -> 1; 10 -> 2; 11 -> 3; 12 -> 4; 13 -> 5; 14 -> 7; 15 -> 9; else -> 0
        }
    }

    val scope = rememberCoroutineScope()
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    bitmapToCrop = bitmap
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    val bitmap = remember(imageData) {
        if (imageData != null) {
            try {
                if (imageData!!.length > 100) {
                    val decoded = Base64.decode(imageData, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                } else {
                    val portraitFile = ImageManager.getPortraitFile(context, imageData!!)
                    if (portraitFile.exists()) {
                        BitmapFactory.decodeFile(portraitFile.absolutePath)
                    } else null
                }
            } catch (_: Exception) { null }
        } else null
    }
    val themeSeedColorArgb = rememberSeedColor(bitmap)

    fun getXpForLevel(level: Int): String {
        return when (level) {
            1 -> "0"
            2 -> "300"
            3 -> "900"
            4 -> "2700"
            5 -> "6500"
            6 -> "14000"
            7 -> "23000"
            8 -> "34000"
            9 -> "48000"
            10 -> "64000"
            11 -> "85000"
            12 -> "100000"
            13 -> "120000"
            14 -> "140000"
            15 -> "165000"
            16 -> "195000"
            17 -> "225000"
            18 -> "265000"
            19 -> "305000"
            20 -> "355000"
            else -> if (level > 20) "355000" else "0"
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Создание персонажа", fontSize = 20.sp) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colorScheme.surface,
                        titleContentColor = colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = colorScheme.surface
                ) {
                    Button(
                        onClick = {
                            val levelInt = level.toIntOrNull() ?: 1
                            val newChar = Character(
                                id = (0..Int.MAX_VALUE).random(),
                                name = name,
                                characterClass = charClass,
                                order = order,
                                imageData = imageData,
                                level = level,
                                experience = getXpForLevel(levelInt),
                                strength = (baseStats[0] + indicatorStates[0]).toString(),
                                dexterity = (baseStats[1] + indicatorStates[1]).toString(),
                                constitution = (baseStats[2] + indicatorStates[2]).toString(),
                                intelligence = (baseStats[3] + indicatorStates[3]).toString(),
                                wisdom = (baseStats[4] + indicatorStates[4]).toString(),
                                charisma = (baseStats[5] + indicatorStates[5]).toString(),
                                themeSeedColorArgb = themeSeedColorArgb
                            )
                            onCharacterCreate(newChar)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary,
                            contentColor = colorScheme.onPrimary
                        )
                    ) {
                        Text("Создать", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CharacterInfoSection(
                    name = name, onNameChange = { name = it },
                    charClass = charClass, onClassChange = { charClass = it },
                    level = level, onLevelChange = { level = it },
                    order = order, onOrderChange = { order = it },
                    imageData = imageData,
                    onAvatarClick = { imagePicker.launch("image/*") }
                )

                Spacer(Modifier.height(32.dp))

                StatsTable(
                    statNames = statNames,
                    baseStats = baseStats,
                    indicatorStates = indicatorStates,
                    spentPoints = spentPoints,
                    totalPoints = totalPoints,
                    totalIndicatorTaps = totalIndicatorTaps,
                    onStatChange = { index, newValue ->
                        if (newValue in 8..15) {
                            val tempStats = baseStats.toMutableList()
                            tempStats[index] = newValue
                            val newSpent = tempStats.sumOf { v ->
                                when (v) {
                                    8 -> 0; 9 -> 1; 10 -> 2; 11 -> 3; 12 -> 4; 13 -> 5; 14 -> 7; 15 -> 9; else -> 0
                                }
                            }
                            if (newSpent <= totalPoints) {
                                baseStats[index] = newValue
                            }
                        }
                    },
                    onIndicatorTap = { index ->
                        if (indicatorStates[index] < 2 && totalIndicatorTaps < 3) {
                            indicatorStates[index] += 1
                        } else if (indicatorStates[index] > 0) {
                            indicatorStates[index] = 0
                        }
                    }
                )
                
                Spacer(Modifier.height(32.dp))
            }
        }

        if (bitmapToCrop != null) {
            AvatarCropperWindow(
                imageToCrop = bitmapToCrop!!,
                hazeState = hazeState,
                forceBlurEnabled = forceBlurEnabled,
                onCropSuccess = { cropped ->
                    scope.launch {
                        val id = ImageManager.saveBitmapAsOriginal(context, bitmapToCrop!!)
                        ImageManager.saveCropped(context, id, cropped)
                        imageData = id
                        bitmapToCrop = null
                    }
                },
                onCancel = { bitmapToCrop = null }
            )
        }
    }
}

@Composable
fun CharacterInfoSection(
    name: String, onNameChange: (String) -> Unit,
    charClass: String, onClassChange: (String) -> Unit,
    level: String, onLevelChange: (String) -> Unit,
    order: String, onOrderChange: (String) -> Unit,
    imageData: String?,
    onAvatarClick: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    val bitmap = remember(imageData) {
        if (imageData != null) {
            try {
                if (imageData.length > 100) {
                    val decoded = Base64.decode(imageData, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(decoded, 0, decoded.size)?.asImageBitmap()
                } else {
                    val portraitFile = ImageManager.getPortraitFile(context, imageData)
                    if (portraitFile.exists()) {
                        BitmapFactory.decodeFile(portraitFile.absolutePath)?.asImageBitmap()
                    } else null
                }
            } catch (_: Exception) { null }
        } else null
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorScheme.surfaceVariant)
                    .clickable { onAvatarClick() },
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(48.dp), tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
                Box(modifier = Modifier.fillMaxSize().padding(4.dp), contentAlignment = Alignment.BottomEnd) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp), tint = colorScheme.primary)
                }
            }
            Spacer(Modifier.width(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Имя") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Spacer(Modifier.height(12.dp))
        Row {
            OutlinedTextField(
                value = charClass,
                onValueChange = onClassChange,
                label = { Text("Класс") },
                modifier = Modifier.weight(1.5f),
                singleLine = true
            )
            Spacer(Modifier.width(12.dp))
            OutlinedTextField(
                value = level,
                onValueChange = onLevelChange,
                label = { Text("Уровень") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Spacer(Modifier.height(12.dp))
        Row {
            OutlinedTextField(
                value = order,
                onValueChange = onOrderChange,
                label = { Text("Вид") },
                modifier = Modifier.weight(1.5f),
                singleLine = true
            )
        }
    }
}

@Composable
fun MorphingIndicator(state: Int, onClick: () -> Unit) {
    val color = animateColorAsState(
        when (state) {
            0 -> Color(0xFF4A4458)
            1 -> Color(0xFF6750A4)
            else -> Color(0xFFD0BCFF)
        },
        animationSpec = tween(600)
    )
    
    val sides = when (state) {
        0 -> 4
        1 -> 6
        else -> 8
    }
    
    // Using MorphingPolygonShape for premium transitions
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(MorphingPolygonShape(sides))
            .background(color.value)
            .clickable(onClick = onClick)
    )
}

@Composable
fun StatsTable(
    statNames: List<String>,
    baseStats: List<Int>,
    indicatorStates: List<Int>,
    spentPoints: Int,
    totalPoints: Int,
    totalIndicatorTaps: Int,
    onStatChange: (Int, Int) -> Unit,
    onIndicatorTap: (Int) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(12.dp)
    ) {
        // Aligned Indicators Row
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TableCell("", weight = 1.5f) // Spacer for the label column
            indicatorStates.forEachIndexed { index, state ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    MorphingIndicator(
                        state = state,
                        onClick = { onIndicatorTap(index) }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Headers
        Row(modifier = Modifier.fillMaxWidth()) {
            TableCell("Хар-ка:", weight = 1.5f, isHeader = true)
            statNames.forEach { TableCell(it, weight = 1f, isHeader = true) }
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
        
        // Values with Steppers
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TableCell("Значение:", weight = 1.5f)
            baseStats.forEachIndexed { index, value ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    VerticalStepper(
                        value = value,
                        onIncrement = { onStatChange(index, value + 1) },
                        onDecrement = { onStatChange(index, value - 1) }
                    )
                }
            }
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
        
        // Bonus row (from indicators)
        Row(modifier = Modifier.fillMaxWidth()) {
            TableCell("Бонус:", weight = 1.5f)
            indicatorStates.forEach { state ->
                TableCell(if (state > 0) "+$state" else "0", weight = 1f)
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth()) {
            TableCell("Моди:", weight = 1.5f)
            baseStats.forEachIndexed { index, value ->
                val totalValue = value + indicatorStates[index]
                val mod = (totalValue - 10) / 2
                TableCell(if (mod >= 0) "+$mod" else "$mod", weight = 1f)
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth()) {
            TableCell("Итого:", weight = 1.5f)
            baseStats.forEachIndexed { index, value ->
                TableCell((value + indicatorStates[index]).toString(), weight = 1f)
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Points Footer
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Очки $spentPoints/$totalPoints", 
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold, 
                color = if (spentPoints > totalPoints) Color.Red else colorScheme.primary
            )
            Text(
                "Бонус $totalIndicatorTaps/3", 
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold, 
                color = if (totalIndicatorTaps >= 3) colorScheme.primary else colorScheme.secondary
            )
        }
    }
}

@Composable
fun RowScope.TableCell(text: String, weight: Float, isHeader: Boolean = false) {
    Text(
        text = text,
        modifier = Modifier
            .weight(weight)
            .padding(vertical = 4.dp),
        textAlign = TextAlign.Center,
        fontSize = if (isHeader) 12.sp else 14.sp,
        fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
        color = if (isHeader) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun VerticalStepper(
    value: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        IconButton(onClick = onIncrement, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = colorScheme.primary)
        }
        
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(value.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colorScheme.onPrimaryContainer)
        }
        
        IconButton(onClick = onDecrement, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = colorScheme.primary)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CharacterCreationWindowPreview() {
    quasarisTheme {
        CharacterCreationWindow(onNavigateBack = {}, onCharacterCreate = {})
    }
}
