package ru.quasaris.characternexus

import androidx.compose.ui.graphics.toArgb
import android.content.Context
import android.os.Build
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual val platformFileSystem: FileSystem = FileSystem.SYSTEM

actual fun getAppDataDir(): Path {
    return platformContext.androidContext.filesDir.absolutePath.toPath()
}

actual fun getCacheDir(): Path {
    return platformContext.androidContext.cacheDir.absolutePath.toPath()
}

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

actual val performanceClass: Int
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Build.VERSION.MEDIA_PERFORMANCE_CLASS
    } else {
        0
    }

actual fun createDataStore(): DataStore<Preferences> {
    return ru.quasaris.characternexus.backend.createDataStore {
        platformContext.androidContext.preferencesDataStoreFile("settings").absolutePath
    }
}

actual class PlatformContext(context: Context) {
    val androidContext: Context = context.applicationContext
}

private lateinit var _platformContext: PlatformContext
actual var platformContext: PlatformContext
    get() = _platformContext
    set(value) { _platformContext = value }

@androidx.compose.runtime.Composable
actual fun ApplySystemBarEffects(color: androidx.compose.ui.graphics.Color, darkTheme: Boolean) {
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as android.app.Activity).window
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
            window.statusBarColor = color.toArgb()
            window.navigationBarColor = color.toArgb()
        }
    }
}
