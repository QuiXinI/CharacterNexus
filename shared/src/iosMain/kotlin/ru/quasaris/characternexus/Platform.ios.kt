package ru.quasaris.characternexus

import platform.UIKit.UIDevice
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSCachesDirectory
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

actual val platformFileSystem: FileSystem = FileSystem.SYSTEM

@OptIn(ExperimentalForeignApi::class)
actual fun getAppDataDir(): Path {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )
    return documentDirectory!!.path!!.toPath()
}

@OptIn(ExperimentalForeignApi::class)
actual fun getCacheDir(): Path {
    val cacheDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSCachesDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )
    return cacheDirectory!!.path!!.toPath()
}

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default

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
}

@Composable
actual fun getDynamicColorScheme(darkTheme: Boolean): ColorScheme? {
    return null
}
