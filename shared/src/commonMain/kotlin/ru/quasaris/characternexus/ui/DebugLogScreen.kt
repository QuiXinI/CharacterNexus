package ru.quasaris.characternexus.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import ru.quasaris.characternexus.backend.CrashLogEntry
import ru.quasaris.characternexus.backend.SettingsViewModel
import ru.quasaris.characternexus.util.LogEntry
import ru.quasaris.characternexus.util.PlatformUtils
import ru.quasaris.characternexus.util.logFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugLogScreen(
    onDismiss: () -> Unit,
    settingsViewModel: SettingsViewModel? = null
) {
    var logs by remember { mutableStateOf(listOf<LogEntry>()) }
    val listState = rememberLazyListState()
    
    val crashLogsState by settingsViewModel?.crashLogs?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    
    var selectedTab by remember { mutableIntStateOf(if (crashLogsState.isNotEmpty()) 0 else 1) }
    var selectedCrash by remember { mutableStateOf<CrashLogEntry?>(null) }

    LaunchedEffect(Unit) {
        logFlow.collectLatest { entry ->
            logs = (logs + entry).takeLast(500)
        }
    }

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty() && selectedTab == 1) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    val colorScheme = MaterialTheme.colorScheme

    BackHandler(enabled = selectedCrash != null) {
        selectedCrash = null
    }

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
                        val content = if (selectedTab == 0) {
                            crashLogsState.joinToString("\n\n---\n\n") { 
                                "DATED: ${it.formattedDate}\nREASON: ${it.shortReason}\n${it.fullStackTrace}" 
                            }
                        } else {
                            logs.joinToString("\n") { "[${it.level}] ${it.tag}: ${it.message}" }
                        }
                        PlatformUtils.setClipboardText("Character Nexus Logs", content)
                        PlatformUtils.showMessage("Скопировано в буфер обмена")
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = colorScheme.surface,
                    contentColor = colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.BugReport, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Краши (${crashLogsState.size})", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Terminal, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Логи (${logs.size})", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                when (selectedTab) {
                    0 -> {
                        // Crash Logs Tab
                        if (crashLogsState.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = colorScheme.primary.copy(alpha = 0.6f)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Вылетов не зафиксировано",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(crashLogsState, key = { it.id }) { crash ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedCrash = crash },
                                        colors = CardDefaults.cardColors(
                                            containerColor = colorScheme.errorContainer.copy(alpha = 0.6f)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Default.Warning,
                                                        contentDescription = null,
                                                        tint = colorScheme.error,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(
                                                        text = if (crash.formattedDate.isNotBlank()) crash.formattedDate else "Дата не указана",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = colorScheme.onErrorContainer,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Row {
                                                    IconButton(
                                                        onClick = { selectedCrash = crash },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Fullscreen,
                                                            contentDescription = "На весь экран",
                                                            tint = colorScheme.onErrorContainer,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            PlatformUtils.setClipboardText("Character Nexus Crash Log", crash.fullStackTrace)
                                                            PlatformUtils.showMessage("Лог краша скопирован")
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.ContentCopy,
                                                            contentDescription = "Копировать",
                                                            tint = colorScheme.onErrorContainer,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            settingsViewModel?.deleteCrashLog(crash.id)
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Delete,
                                                            contentDescription = "Удалить",
                                                            tint = colorScheme.onErrorContainer,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(Modifier.height(6.dp))

                                            Text(
                                                text = crash.shortReason.ifBlank { "Причина не указана" },
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = colorScheme.onErrorContainer,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                maxLines = 3
                                            )

                                            Spacer(Modifier.height(6.dp))

                                            Text(
                                                text = "Нажмите, чтобы открыть на весь экран...",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colorScheme.onErrorContainer.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = { settingsViewModel?.clearAllCrashLogs() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorScheme.error,
                                    contentColor = colorScheme.onError
                                )
                            ) {
                                Icon(Icons.Default.DeleteSweep, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Очистить историю вылетов")
                            }
                        }
                    }

                    1 -> {
                        // Live Logs Tab
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

            // Fullscreen Crash Detail View Overlay
            AnimatedVisibility(
                visible = selectedCrash != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                selectedCrash?.let { crash ->
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = colorScheme.background
                    ) {
                        Scaffold(
                            topBar = {
                                CenterAlignedTopAppBar(
                                    title = { Text("Детали краша", fontWeight = FontWeight.Bold) },
                                    navigationIcon = {
                                        IconButton(onClick = { selectedCrash = null }) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                                        }
                                    },
                                    actions = {
                                        IconButton(onClick = {
                                            PlatformUtils.setClipboardText("Character Nexus Crash Log", crash.fullStackTrace)
                                            PlatformUtils.showMessage("Скопировано в буфер обмена")
                                        }) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Копировать")
                                        }
                                        IconButton(onClick = {
                                            settingsViewModel?.deleteCrashLog(crash.id)
                                            selectedCrash = null
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Удалить")
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = colorScheme.surface
                                    )
                                )
                            }
                        ) { innerPadding ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Date & Thread Info Card
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Дата и время:",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = crash.formattedDate.ifBlank { "Зафиксировано во время работы" },
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Поток:",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = crash.threadName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }

                                // Short Reason Card
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = colorScheme.errorContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Причина вылета (Short Reason)",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = colorScheme.onErrorContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = crash.shortReason.ifBlank { "Описание причины отсутствует" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colorScheme.onErrorContainer,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Full StackTrace Section
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.Black.copy(alpha = 0.05f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Полный стектрейс и системная информация",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        SelectionContainer {
                                            Text(
                                                text = crash.fullStackTrace,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp,
                                                color = colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            PlatformUtils.setClipboardText("Character Nexus Crash Log", crash.fullStackTrace)
                                            PlatformUtils.showMessage("Лог краша скопирован")
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Скопировать")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            settingsViewModel?.deleteCrashLog(crash.id)
                                            selectedCrash = null
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = colorScheme.error
                                        )
                                    ) {
                                        Icon(Icons.Default.Delete, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Удалить")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
