plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
}

val versionFile = file("version.txt")
val versionStr = if (versionFile.exists()) versionFile.readText().trim() else "0.1.0.0"
val versionParts = versionStr.split(".")

val gen = if (versionParts.size >= 4) versionParts[0].toInt() else 0
val major = if (versionParts.size >= 4) versionParts[1].toInt() else (versionParts.getOrNull(0)?.toInt() ?: 0)
val minor = if (versionParts.size >= 4) versionParts[2].toInt() else (versionParts.getOrNull(1)?.toInt() ?: 0)
val patch = if (versionParts.size >= 4) versionParts[3].toInt() else (versionParts.getOrNull(2)?.toInt() ?: 0)

// Formula: (G * 100,000,000) + (MAJOR * 1,000,000) + (MINOR * 1,000) + PATCH
val calculatedVersionCode = (gen * 100000000) + (major * 1000000) + (minor * 1000) + patch

val version3Part = if (versionParts.size >= 4) {
    // For compatibility with platform packaging (like MSI/DMG) that might not like 4 parts
    // Combining GEN and MAJOR into the first part (limit 255 for MSI)
    "${(gen * 100 + major).coerceAtMost(255)}.${minor}.${patch}"
} else {
    versionStr
}

val appVersionName = versionStr
val appVersionCode = calculatedVersionCode
extra["appVersionName"] = appVersionName
extra["appVersionCode"] = appVersionCode
extra["appVersion3Part"] = version3Part
