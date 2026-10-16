package ru.quasaris.characternexus.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class LogEntry(
    val level: String,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null,
    val timestamp: Long = 0L
)

private val _logFlow = MutableSharedFlow<LogEntry>(extraBufferCapacity = 500)
val logFlow = _logFlow.asSharedFlow()

expect object Logger {
    fun d(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
    fun i(tag: String, message: String)
}

fun logToFlow(level: String, tag: String, message: String, throwable: Throwable? = null) {
    _logFlow.tryEmit(LogEntry(level, tag, message, throwable))
}

fun Throwable.log(tag: String = "Error", message: String = "An error occurred") {
    Logger.e(tag, message, this)
}

fun Throwable.toShortReason(): String {
    val className = this::class.simpleName ?: this::class.qualifiedName ?: "Throwable"
    val msg = message
    val primary = if (msg.isNullOrBlank()) className else "$className: $msg"
    val c = cause
    return if (c != null && c != this) {
        val causeName = c::class.simpleName ?: "Throwable"
        val causeMsg = c.message
        val causePrimary = if (causeMsg.isNullOrBlank()) causeName else "$causeName: $causeMsg"
        "$primary\nCaused by: $causePrimary"
    } else {
        primary
    }
}
