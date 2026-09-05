package ru.quasaris.characternexus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
import ru.quasaris.characternexus.util.LogEntry
import ru.quasaris.characternexus.util.logFlow
import ru.quasaris.characternexus.util.PlatformUtils
import ru.quasaris.characternexus.backend.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugLogScreen(
    onDismiss: () -> Unit,
    settingsViewModel: SettingsViewModel? = null
) {
    var logs by remember { mutableStateOf(listOf<LogEntry>()) }
    val listState = rememberLazyListState()
    val lastCrashLog by settingsViewModel?.lastCrashLog?.collectAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        logFlow.collectLatest { entry ->
            logs = (logs + entry).takeLast(500)
        }
    }

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Логи и Отладка", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val allLogs = logs.joinToString("\n") { "[${it.level}] ${it.tag}: ${it.message}" }
                        PlatformUtils.setClipboardText("Character Nexus Logs", allLogs)
                        PlatformUtils.showMessage("Логи скопированы")
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Копировать всё")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surface
                )
            )
        },
        containerColor = colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (lastCrashLog != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorScheme.errorContainer.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Последний краш",
                                style = MaterialTheme.typography.titleSmall,
                                color = colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Row {
                                IconButton(onClick = {
                                    PlatformUtils.setClipboardText("Character Nexus Crash Log", lastCrashLog!!)
                                    PlatformUtils.showMessage("Лог краша скопирован")
                                }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp), tint = colorScheme.onErrorContainer)
                                }
                                IconButton(onClick = {
                                    settingsViewModel?.updateLastCrashLog(null)
                                }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp), tint = colorScheme.onErrorContainer)
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = lastCrashLog!!.take(300) + if (lastCrashLog!!.length > 300) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onErrorContainer.copy(alpha = 0.8f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(logs) { log ->
                        val color = when (log.level) {
                            "E" -> Color.Red
                            "W" -> Color(0xFFFFA500)
                            "I" -> colorScheme.primary
                            else -> colorScheme.onSurfaceVariant
                        }
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = "[${log.level}] ${log.tag}: ${log.message}",
                                color = color,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (log.level == "E") FontWeight.Bold else FontWeight.Normal
                            )
                            log.throwable?.let {
                                Text(
                                    text = it.stackTraceToString(),
                                    color = color.copy(alpha = 0.6f),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(start = 12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
                
                if (logs.isEmpty()) {
                    Text(
                        "Логи пусты",
                        modifier = Modifier.align(Alignment.Center),
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
            
            Button(
                onClick = { logs = emptyList() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.surfaceVariant,
                    contentColor = colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Default.DeleteSweep, null)
                Spacer(Modifier.width(8.dp))
                Text("Очистить текущие логи")
            }
        }
    }
}
