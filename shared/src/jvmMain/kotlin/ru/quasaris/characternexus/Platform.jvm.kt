package ru.quasaris.characternexus

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import ru.quasaris.characternexus.ui.buildColorSchemeFromSeed

class JVMPlatform: Platform {
    override val name: String = "JVM ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual val platformFileSystem: FileSystem = FileSystem.SYSTEM

actual fun getAppDataDir(): Path {
    val isPortable = System.getProperty("portable") == "true" || File("data").exists()
    val appDir = if (isPortable) {
        val rootDir = System.getProperty("compose.application.resources.dir")?.let {
            File(it).parentFile
        } ?: File(".")
        File(rootDir, "data")
    } else {
        val userHome = System.getProperty("user.home")
        File(userHome, ".characternexus")
    }
    if (!appDir.exists()) appDir.mkdirs()
    return appDir.absolutePath.toPath()
}

actual fun getCacheDir(): Path {
    val isPortable = System.getProperty("portable") == "true" || File("data").exists()
    val cacheDir = if (isPortable) {
        val rootDir = System.getProperty("compose.application.resources.dir")?.let {
            File(it).parentFile
        } ?: File(".")
        File(rootDir, "data/cache")
    } else {
        val tempDir = System.getProperty("java.io.tmpdir")
        File(tempDir, "characternexus_cache")
    }
    if (!cacheDir.exists()) cacheDir.mkdirs()
    return cacheDir.absolutePath.toPath()
}

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

actual fun <T> runBlockingPlatform(block: suspend CoroutineScope.() -> T): T {
    return kotlinx.coroutines.runBlocking(block = block)
}

actual val performanceClass: Int = 99

actual fun createDataStore(): DataStore<Preferences> {
    return ru.quasaris.characternexus.backend.createDataStore {
        getAppDataDir().div("settings.preferences_pb").toString()
    }
}

actual class PlatformContext

private var _platformContext = PlatformContext()
actual var platformContext: PlatformContext
    get() = _platformContext
    set(value) { _platformContext = value }

@Composable
actual fun ApplySystemBarEffects(color: Color, darkTheme: Boolean) {
    // No-op for Desktop
}

@Composable
actual fun getDynamicColorScheme(darkTheme: Boolean): ColorScheme? {
    val osName = System.getProperty("os.name").lowercase()
    if (osName.contains("win")) {
        try {
            val process = Runtime.getRuntime().exec("reg query \"HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\DWM\" /v AccentColor")
            val output = process.inputStream.bufferedReader().readText()
            val match = Regex("AccentColor\\s+REG_DWORD\\s+0x([0-9a-fA-F]+)").find(output)
            if (match != null) {
                val colorHex = match.groupValues[1]
                val colorInt = colorHex.toLong(16)
                // Windows uses ABGR (0xAABBGGRR)
                val a = ((colorInt shr 24) and 0xFF).toInt()
                val b = ((colorInt shr 16) and 0xFF).toInt()
                val g = ((colorInt shr 8) and 0xFF).toInt()
                val r = (colorInt and 0xFF).toInt()
                
                val seedColor = Color(r, g, b, a)
                return buildColorSchemeFromSeed(seedColor, darkTheme)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
    return null
}
