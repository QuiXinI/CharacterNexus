package ru.quasaris.characternexus

import okio.FileSystem
import okio.Path
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect val platformFileSystem: FileSystem
expect fun getAppDataDir(): Path
expect fun getCacheDir(): Path

expect val ioDispatcher: CoroutineDispatcher

expect val performanceClass: Int

expect fun <T> runBlockingPlatform(block: suspend CoroutineScope.() -> T): T

expect fun createDataStore(): DataStore<Preferences>

/**
 * Interface to provide platform-specific context (like Android Context).
 */
expect class PlatformContext

/**
 * Global accessor for the platform context. 
 * Must be initialized in platform-specific entry point.
 */
expect var platformContext: PlatformContext

@androidx.compose.runtime.Composable
expect fun ApplySystemBarEffects(color: androidx.compose.ui.graphics.Color, darkTheme: Boolean)
