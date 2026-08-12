package ru.quasaris.characternexus

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class WasmPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
}

actual fun getPlatform(): Platform = WasmPlatform()

actual val platformFileSystem: FileSystem get() = error("FileSystem not supported on Wasm Browser")

actual fun getAppDataDir(): Path = error("AppDataDir not supported on Wasm Browser")
actual fun getCacheDir(): Path = error("CacheDir not supported on Wasm Browser")
