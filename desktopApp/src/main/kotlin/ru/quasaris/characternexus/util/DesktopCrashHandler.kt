package ru.quasaris.characternexus.util

import ru.quasaris.characternexus.backend.CrashLogEntry
import ru.quasaris.characternexus.backend.SettingsManager
import ru.quasaris.characternexus.generated.BuildConstants
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DesktopCrashHandler(
    private val settingsManager: SettingsManager,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            val stackTraceStr = sw.toString()

            val timestamp = System.currentTimeMillis()
            val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
            val shortReason = throwable.toShortReason()

            val sysInfo = """
                App Version: ${BuildConstants.VERSION}
                OS: ${System.getProperty("os.name")} ${System.getProperty("os.version")} (${System.getProperty("os.arch")})
                Java: ${System.getProperty("java.version")} (${System.getProperty("java.vendor")})
                Thread: ${thread.name}
                
                Stacktrace:
                $stackTraceStr
            """.trimIndent()

            val crashEntry = CrashLogEntry(
                id = generateUuid(),
                timestamp = timestamp,
                formattedDate = formattedDate,
                shortReason = shortReason,
                fullStackTrace = sysInfo,
                threadName = thread.name
            )

            val updatedLogs = (listOf(crashEntry) + settingsManager.crashLogs).take(50)
            settingsManager.crashLogs = updatedLogs
            settingsManager.lastCrashLog = sysInfo

            val logFile = File("crash-log.txt")
            logFile.appendText("========================================\nCRASH AT $formattedDate [Thread: ${thread.name}]\nReason: $shortReason\n\n$sysInfo\n\n")
            
            throwable.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        defaultHandler?.uncaughtException(thread, throwable)
    }

    companion object {
        fun initialize(settingsManager: SettingsManager) {
            val oldHandler = Thread.getDefaultUncaughtExceptionHandler()
            if (oldHandler !is DesktopCrashHandler) {
                Thread.setDefaultUncaughtExceptionHandler(DesktopCrashHandler(settingsManager, oldHandler))
            }
        }
    }
}
