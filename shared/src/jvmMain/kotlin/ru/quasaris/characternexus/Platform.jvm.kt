package ru.quasaris.characternexus

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class JVMPlatform: Platform {
    override val name: String = "JVM ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual val platformFileSystem: FileSystem = FileSystem.SYSTEM

actual fun getAppDataDir(): Path {
    val isPortable = System.getProperty("portable") == "true"
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
    val isPortable = System.getProperty("portable") == "true"
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

actual fun <T> runBlockingPlatform(block: suspend CoroutineScope.() -> T): T = kotlinx.coroutines.runBlocking { block() }
