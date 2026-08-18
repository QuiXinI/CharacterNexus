package ru.quasaris.characternexus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.quasaris.characternexus.backend.KeybindAction
import ru.quasaris.characternexus.backend.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeybindSettingsWindow(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit,
    forceBlurEnabled: Boolean = false
) {
    val keybinds by viewModel.keybinds.collectAsState()
    val colorScheme = MaterialTheme.colorScheme
    val isOled = colorScheme.background == Color.Black
    
    var listeningAction by remember { mutableStateOf<KeybindAction?>(null) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(listeningAction) {
        if (listeningAction != null) {
            focusRequester.requestFocus()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        DialogDimStyle(0.6f)
        
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Горячие клавиши", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = if (forceBlurEnabled && !isOled) Color.Transparent.copy(alpha = 0.0f) else colorScheme.surface
                    )
                )
            },
            containerColor = if (forceBlurEnabled && !isOled) Color.Transparent.copy(alpha = 0.0f) else colorScheme.background,
            modifier = Modifier
                .fillMaxSize()
                .onKeyEvent { event ->
                    if (listeningAction != null && event.type == KeyEventType.KeyDown) {
                        viewModel.updateKeybind(listeningAction!!, event.key)
                        listeningAction = null
                        true
                    } else false
                }
                .focusRequester(focusRequester)
                .focusable()
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                Text(
                    "Нажмите на клавишу действия, чтобы изменить её.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(KeybindAction.entries) { action ->
                            val currentKey = keybinds[action] ?: Key.Unknown
                            val conflicts = keybinds.filter { it.value == currentKey && it.key != action }.keys
                            val isListening = listeningAction == action
                            
                            KeybindItem(
                                action = action,
                                currentKey = currentKey,
                                isListening = isListening,
                                conflicts = conflicts.toList(),
                                onListenRequest = { listeningAction = action },
                                onReset = { viewModel.resetKeybind(action) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.resetAllKeybinds() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.errorContainer,
                        contentColor = colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Сбросить все до стандартных", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (listeningAction != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { listeningAction = null },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = colorScheme.primaryContainer,
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Keyboard, null, modifier = Modifier.size(48.dp), tint = colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Нажмите клавишу для:", fontWeight = FontWeight.Bold)
                        Text(listeningAction?.displayName ?: "", style = MaterialTheme.typography.titleLarge, color = colorScheme.primary)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Для отмены нажмите вне окна", style = MaterialTheme.typography.labelSmall, color = colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

@Composable
fun KeybindItem(
    action: KeybindAction,
    currentKey: Key,
    isListening: Boolean,
    conflicts: List<KeybindAction>,
    onListenRequest: () -> Unit,
    onReset: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val hasConflict = conflicts.isNotEmpty()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (hasConflict) colorScheme.errorContainer.copy(alpha = 0.1f) else Color.Transparent)
            .border(
                1.dp, 
                if (hasConflict) colorScheme.error.copy(alpha = 0.5f) else colorScheme.outlineVariant.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = action.displayName,
                    fontWeight = FontWeight.Bold,
                    color = if (hasConflict) colorScheme.error else colorScheme.onSurface
                )
                if (hasConflict) {
                    Text(
                        text = "Уже используется для: ${conflicts.joinToString { it.displayName }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.error
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onListenRequest() },
                color = if (isListening) colorScheme.primary else if (hasConflict) colorScheme.error else colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = currentKey.toFriendlyName(),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isListening) colorScheme.onPrimary else if (hasConflict) colorScheme.onError else colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onReset) {
                Icon(
                    Icons.Default.SettingsBackupRestore, 
                    contentDescription = "Сброс",
                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

fun Key.toFriendlyName(): String {
    return when (this) {
        Key.DirectionLeft -> "←"
        Key.DirectionRight -> "→"
        Key.DirectionUp -> "↑"
        Key.DirectionDown -> "↓"
        Key.Escape -> "Esc"
        Key.Tab -> "Tab"
        Key.Spacebar -> "Space"
        Key.Enter -> "Enter"
        Key.Backspace -> "Backspace"
        Key.Delete -> "Delete"
        Key.A -> "A"
        Key.B -> "B"
        Key.C -> "C"
        Key.D -> "D"
        Key.E -> "E"
        Key.F -> "F"
        Key.G -> "G"
        Key.H -> "H"
        Key.I -> "I"
        Key.J -> "J"
        Key.K -> "K"
        Key.L -> "L"
        Key.M -> "M"
        Key.N -> "N"
        Key.O -> "O"
        Key.P -> "P"
        Key.Q -> "Q"
        Key.R -> "R"
        Key.S -> "S"
        Key.T -> "T"
        Key.U -> "U"
        Key.V -> "V"
        Key.W -> "W"
        Key.X -> "X"
        Key.Y -> "Y"
        Key.Z -> "Z"
        Key.Zero -> "0"
        Key.One -> "1"
        Key.Two -> "2"
        Key.Three -> "3"
        Key.Four -> "4"
        Key.Five -> "5"
        Key.Six -> "6"
        Key.Seven -> "7"
        Key.Eight -> "8"
        Key.Nine -> "9"
        Key.F1 -> "F1"
        Key.F2 -> "F2"
        Key.F3 -> "F3"
        Key.F4 -> "F4"
        Key.F5 -> "F5"
        Key.F6 -> "F6"
        Key.F7 -> "F7"
        Key.F8 -> "F8"
        Key.F9 -> "F9"
        Key.F10 -> "F10"
        Key.F11 -> "F11"
        Key.F12 -> "F12"
        else -> {
            val s = this.toString()
            if (s.contains("Key: ")) {
                s.substringAfter("Key: ").substringBefore(" (")
            } else {
                "Key $keyCode"
            }
        }
    }
}
