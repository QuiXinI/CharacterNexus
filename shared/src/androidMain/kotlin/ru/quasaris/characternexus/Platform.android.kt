package ru.quasaris.characternexus

import android.os.Build
import android.os.Environment
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import ru.quasaris.characternexus.util.PlatformUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File

import ru.quasaris.characternexus.util.ensureNomedia

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual val platformFileSystem: FileSystem = FileSystem.SYSTEM

actual fun getAppDataDir(): Path {
    // Use Documents folder for persistence across uninstalls
    val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
    val appDir = File(documentsDir, "CharacterNexus")
    if (!appDir.exists()) appDir.mkdirs()
    val path = appDir.absolutePath.toPath()
    path.ensureNomedia()
    return path
}

actual fun getCacheDir(): Path {
    return PlatformUtils.androidContext.cacheDir.absolutePath.toPath()
}

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

actual fun <T> runBlockingPlatform(block: suspend CoroutineScope.() -> T): T = kotlinx.coroutines.runBlocking { block() }
