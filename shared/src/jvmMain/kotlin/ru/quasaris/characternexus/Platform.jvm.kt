package ru.quasaris.characternexus

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import java.io.File

class JVMPlatform: Platform {
    override val name: String = "JVM ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual val platformFileSystem: FileSystem = FileSystem.SYSTEM

actual fun getAppDataDir(): Path {
    val userHome = System.getProperty("user.home")
    val appDir = File(userHome, ".characternexus")
    if (!appDir.exists()) appDir.mkdirs()
    return appDir.absolutePath.toPath()
}

actual fun getCacheDir(): Path {
    val tempDir = System.getProperty("java.io.tmpdir")
    val cacheDir = File(tempDir, "characternexus_cache")
    if (!cacheDir.exists()) cacheDir.mkdirs()
    return cacheDir.absolutePath.toPath()
}
