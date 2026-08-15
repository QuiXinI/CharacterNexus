package ru.quasaris.characternexus

import web.navigator.navigator
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class JsPlatform: Platform {
    private val userAgent = navigator.userAgent
    private val browserList = listOf("Chrome", "Firefox", "Safari", "Edge")

    override val name: String = userAgent.findAnyOf(browserList, ignoreCase = true)
            ?.let { (startIndex) -> userAgent.substring(startIndex).substringBefore(" ") }
            ?: "Unknown"
}

actual fun getPlatform(): Platform = JsPlatform()

actual val platformFileSystem: FileSystem get() = error("FileSystem not supported on JS Browser")

actual fun getAppDataDir(): Path = error("AppDataDir not supported on JS Browser")
actual fun getCacheDir(): Path = error("CacheDir not supported on JS Browser")

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default

actual fun <T> runBlockingPlatform(block: suspend CoroutineScope.() -> T): T = error("runBlocking not supported on JS Browser")
