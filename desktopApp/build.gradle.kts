import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.file.DuplicatesStrategy

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// Определение профиля сборки через property: -PbuildProfile=debugCheck/debugFull/release/releasePortable
val buildProfile = project.findProperty("buildProfile")?.toString() ?: "debugCheck"
val displayProfile = buildProfile.replace("Portable", "", ignoreCase = true)
val appVariant = if (buildProfile.contains("release", ignoreCase = true)) "main-release" else "main"
val appVersion = rootProject.extra["appVersionName"] as String
val rootBuildsDir = rootProject.projectDir.resolve("builds")

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        
        when (buildProfile) {
            "debugCheck" -> {
            }
            "debugFull" -> {
                freeCompilerArgs.add("-Xbackend-threads=0")
            }
            "release", "releasePortable" -> {
                freeCompilerArgs.add("-Xbackend-threads=0")
                freeCompilerArgs.add("-Xno-call-assertions")
                freeCompilerArgs.add("-Xno-receiver-assertions")
                freeCompilerArgs.add("-Xno-param-assertions")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "ru.quasaris.characternexus.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Character Nexus"
            packageVersion = rootProject.extra["appVersion3Part"] as String
            vendor = "Quasaris"
            description = "Character Nexus Desktop Application"
            copyright = "© 2026 Quasaris"
            
            modules("jdk.unsupported")

            windows {
                dirChooser = true
                shortcut = true
                menu = true
                perUserInstall = false
                iconFile.set(project.file("src/main/resources/icon.ico"))
                upgradeUuid = "F6B2A8B1-4A5D-4D9E-B1A2-F6B2A8B14A5D"
            }

            // Настройка в зависимости от профиля
            if (buildProfile == "debugCheck") {
                // In debugCheck we can disable creation of heavy distributions if needed
            }
        }
        
        // Для JVM оптимизаций можно добавить jvmArgs
        val profileJvmArgs = when (buildProfile) {
            "debugCheck" -> listOf("-Xms256m", "-Xmx512m")
            "debugFull", "release" -> listOf(
                "-Xms512m", "-Xmx2g",
                "-XX:+UseParallelGC",
                "-XX:+OptimizeStringConcat"
            )
            "releasePortable" -> listOf(
                "-Xms512m", "-Xmx2g",
                "-XX:+UseParallelGC",
                "-XX:+OptimizeStringConcat",
                "-Dportable=true",
            )
            else -> emptyList()
        }
        
        jvmArgs += profileJvmArgs
        jvmArgs += listOf(
            "--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED",
            "--add-exports", "jdk.unsupported/sun.misc=ALL-UNNAMED"
        )

        buildTypes.release.proguard {
            configurationFiles.from(project.file("proguard-rules.pro"))
        }
    }
}

/*
 * Пресеты для Desktop:
 * 1. debugCheck: ./gradlew :desktopApp:run -PbuildProfile=debugCheck (Быстрая отладка)
 * 2. debugFull: ./gradlew :desktopApp:run -PbuildProfile=debugFull (Производительность + Дебаг)
 * 3. release: ./gradlew :desktopApp:packageReleaseDistribution -PbuildProfile=release (Релиз)
 * 4. releasePortable: ./gradlew :desktopApp:packagePortableZip -PbuildProfile=releasePortable (Портативный ZIP)
 */

tasks.register<Zip>("packagePortableZip") {
    group = "compose desktop"
    description = "Packages the portable application into a ZIP file"

    val currentProfile = displayProfile
    val currentVersion = appVersion
    val targetDir = rootBuildsDir
    val variant = appVariant

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    val archiveName = "portable-${currentProfile}-${currentVersion}.zip"
    archiveFileName.set(archiveName)
    destinationDirectory.set(targetDir)
    
    from(layout.buildDirectory.dir("compose/binaries/$variant/app"))
    includeEmptyDirs = false

    dependsOn(tasks.matching { it.name.contains("create") && it.name.contains("Distributable") })
}

tasks.register<Copy>("copyPortableFolder") {
    group = "compose desktop"
    description = "Copies the portable application folder to builds"

    val currentProfile = displayProfile
    val currentVersion = appVersion
    val targetDir = rootBuildsDir
    val variant = appVariant

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    val folderName = "portable-${currentProfile}-${currentVersion}"
    into(targetDir.resolve(folderName))
    
    from(layout.buildDirectory.dir("compose/binaries/$variant/app"))
    includeEmptyDirs = false

    dependsOn(tasks.matching { it.name.contains("create") && it.name.contains("Distributable") })
}

tasks.register<Copy>("copyDistributions") {
    group = "compose desktop"
    description = "Copies the native distributions to builds"

    val currentProfile = displayProfile
    val currentVersion = appVersion
    val targetDir = rootBuildsDir
    val variant = appVariant

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(layout.buildDirectory.dir("compose/binaries/$variant"))
    include("msi/*.msi", "dmg/*.dmg", "deb/*.deb")
    
    into(targetDir)
    rename { fileName ->
        val ext = fileName.substringAfterLast(".")
        "${currentProfile}-${currentVersion}.${ext}"
    }
    eachFile {
        path = name // flatten
    }
    includeEmptyDirs = false
}

// Hook into lifecycle
tasks.matching { it.name.contains("create") && it.name.contains("Distributable") }.configureEach {
    finalizedBy("copyPortableFolder")
}

tasks.matching { it.name.contains("package") && (it.name.contains("Distribution") || it.name.contains("Distributable")) }.configureEach {
    finalizedBy("copyDistributions")
}

tasks.withType<org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask>().configureEach {
    if (name.contains("Msi", ignoreCase = true) || name.contains("Exe", ignoreCase = true)) {
        freeArgs.add("--win-shortcut-prompt")
    }
}