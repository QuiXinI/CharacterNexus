package ru.quasaris.characternexus.util

import android.os.Build
import ru.quasaris.characternexus.backend.SettingsManager
import ru.quasaris.characternexus.generated.BuildConstants
import java.io.PrintWriter
import java.io.StringWriter

class AndroidCrashHandler(
    private val settingsManager: SettingsManager,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        if (settingsManager.debugInfoEnabled) {
            try {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                throwable.printStackTrace(pw)
                val stackTrace = sw.toString()

                val deviceInfo = """
                    App Version: ${BuildConstants.VERSION}
                    Device: ${Build.MANUFACTURER} ${Build.MODEL}
                    Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
                    Thread: ${thread.name}
                    
                    Stacktrace:
                    $stackTrace
                """.trimIndent()

                PlatformUtils.setClipboardText("Character Nexus Crash Log", deviceInfo)
                settingsManager.lastCrashLog = deviceInfo
            } catch (e: Exception) {
                // Prevent infinite loop if clipboard fails
                e.printStackTrace()
            }
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
