package ru.quasaris.characternexus.util

import android.os.Build
import ru.quasaris.characternexus.backend.CrashLogEntry
import ru.quasaris.characternexus.backend.SettingsManager
import ru.quasaris.characternexus.generated.BuildConstants
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AndroidCrashHandler(
    private val settingsManager: SettingsManager,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            val stackTrace = sw.toString()

            val timestamp = System.currentTimeMillis()
            val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
            val shortReason = throwable.toShortReason()

            val deviceInfo = """
                App Version: ${BuildConstants.VERSION}
                Device: ${Build.MANUFACTURER} ${Build.MODEL}
                Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
                Thread: ${thread.name}
                
                Stacktrace:
                $stackTrace
            """.trimIndent()

            val crashEntry = CrashLogEntry(
                id = generateUuid(),
                timestamp = timestamp,
                formattedDate = formattedDate,
                shortReason = shortReason,
                fullStackTrace = deviceInfo,
                threadName = thread.name
            )

            val updatedLogs = (listOf(crashEntry) + settingsManager.crashLogs).take(50)
            settingsManager.crashLogs = updatedLogs
            settingsManager.lastCrashLog = deviceInfo

            if (settingsManager.debugInfoEnabled) {
                PlatformUtils.setClipboardText("Character Nexus Crash Log", deviceInfo)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Always call the default handler
        defaultHandler?.uncaughtException(thread, throwable)
    }

    companion object {
        fun initialize(settingsManager: SettingsManager) {
            val oldHandler = Thread.getDefaultUncaughtExceptionHandler()
            if (oldHandler !is AndroidCrashHandler) {
                Thread.setDefaultUncaughtExceptionHandler(AndroidCrashHandler(settingsManager, oldHandler))
            }
        }
    }
}
