package ru.quasaris.characternexus

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

class WasmPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
}

actual fun getPlatform(): Platform = WasmPlatform()

private val _fakeFileSystem = FakeFileSystem().apply {
    try {
        createDirectories("/data".toPath())
    } catch (_: Exception) {}
}

actual val platformFileSystem: FileSystem = _fakeFileSystem

actual fun getAppDataDir(): Path = "/data".toPath()
actual fun getCacheDir(): Path = "/data/cache".toPath()

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default

actual fun <T> runBlockingPlatform(block: suspend CoroutineScope.() -> T): T {
    CoroutineScope(Dispatchers.Default).launch {
        block()
    }
    @Suppress("UNCHECKED_CAST")
    return null as T
}

actual val performanceClass: Int = 10

actual class PlatformContext

private var _platformContext = PlatformContext()
actual var platformContext: PlatformContext
    get() = _platformContext
    set(value) { _platformContext = value }

@Composable
actual fun ApplySystemBarEffects(color: Color, darkTheme: Boolean) {
    // No-op for Web
}

@Composable
actual fun getDynamicColorScheme(darkTheme: Boolean): ColorScheme? {
    return null
}
