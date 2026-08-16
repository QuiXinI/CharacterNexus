package ru.quasaris.characternexus

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import ru.quasaris.characternexus.util.LogEntry
import ru.quasaris.characternexus.util.logFlow
import kotlinx.coroutines.flow.collectLatest
import ru.quasaris.characternexus.ui.quasarisTheme
import ru.quasaris.characternexus.model.AppThemeMode

@Composable
fun DebugLogWindow() {
    var logs by remember { mutableStateOf(listOf<LogEntry>()) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        logFlow.collectLatest { entry ->
            logs = (logs + entry).takeLast(1000)
        }
    }

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Window(
        onCloseRequest = {}, 
        title = "Live Logs",
        state = androidx.compose.ui.window.rememberWindowState(width = 800.dp, height = 500.dp)
    ) {
        quasarisTheme(themeMode = AppThemeMode.OFF) { // Force black theme
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        items(logs) { log ->
                            val color = when (log.level) {
                                "E" -> Color.Red
                                "W" -> Color.Yellow
                                "I" -> Color.Cyan
                                else -> Color.LightGray
                            }
                            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text(
                                    text = "[${log.level}] ${log.tag}: ${log.message}",
                                    color = color,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                log.throwable?.let {
                                    Text(
                                        text = it.stackTraceToString(),
                                        color = color.copy(alpha = 0.7f),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(start = 16.dp),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
