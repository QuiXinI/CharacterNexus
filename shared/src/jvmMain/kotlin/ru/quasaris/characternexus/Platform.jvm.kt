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
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

class JVMPlatform: Platform {
    override val name: String = "JVM ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual val platformFileSystem: FileSystem = FileSystem.SYSTEM

actual fun getAppDataDir(): Path {
    val isPortable = System.getProperty("portable") == "true" || File("data").exists()
    val appDir = if (isPortable) {
        File("data")
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
        File("data/cache")
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
