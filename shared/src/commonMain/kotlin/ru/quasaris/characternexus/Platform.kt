package ru.quasaris.characternexus

import okio.FileSystem
import okio.Path

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect val platformFileSystem: FileSystem
expect fun getAppDataDir(): Path
expect fun getCacheDir(): Path
