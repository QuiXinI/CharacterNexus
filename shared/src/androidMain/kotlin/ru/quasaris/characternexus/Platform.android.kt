package ru.quasaris.characternexus

import android.os.Build
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import ru.quasaris.characternexus.util.PlatformUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual val platformFileSystem: FileSystem = FileSystem.SYSTEM

actual fun getAppDataDir(): Path {
    return PlatformUtils.androidContext.filesDir.absolutePath.toPath()
}

actual fun getCacheDir(): Path {
    return PlatformUtils.androidContext.cacheDir.absolutePath.toPath()
}

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
