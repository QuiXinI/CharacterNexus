import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.file.DuplicatesStrategy

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// Профили: debug (быстро) или release (полная оптимизация)
val buildProfile = project.findProperty("buildProfile")?.toString() ?: "debug"
val isRelease = buildProfile.equals("release", ignoreCase = true)
val appVariant = if (isRelease) "main-release" else "main"
val appVersion = rootProject.extra["appVersionName"] as String
val rootBuildsDir = rootProject.projectDir.resolve("builds")

// Определение платформы для именования архивов
val osName = System.getProperty("os.name").lowercase().let {
    when {
        it.contains("win") -> "windows"
        it.contains("mac") -> "macos"
        else -> "linux"
    }
}
val osArch = System.getProperty("os.arch").lowercase().let {
    when {
        it.contains("aarch64") || it.contains("arm64") -> "arm64"
        it.contains("64") -> "x64"
        else -> it
    }
}
val platformName = "${osName}-${osArch}"

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.compose.material3)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        
        if (isRelease) {
            // Оптимизации для релиза
            freeCompilerArgs.add("-Xbackend-threads=0")
            freeCompilerArgs.add("-Xno-call-assertions")
            freeCompilerArgs.add("-Xno-receiver-assertions")
            freeCompilerArgs.add("-Xno-param-assertions")
        }
    }
}

compose.desktop {
    application {
        mainClass = "ru.quasaris.characternexus.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            
            packageName = "Character Nexus"
            packageVersion = rootProject.extra["appVersion3Part"] as String
            vendor = "Quasaris"
            description = "Character Nexus Desktop Application"
            copyright = "© 2026 Quasaris"
            
            modules("jdk.unsupported")

            windows {
                iconFile.set(project.file("src/main/resources/icon.ico"))
            }
        }
        
        // JVM оптимизации
        val profileJvmArgs = if (isRelease) {
            listOf(
                "-Xms512m", "-Xmx2g",
                "-XX:+UseParallelGC",
                "-XX:+OptimizeStringConcat"
            )
        } else {
            listOf("-Xms256m", "-Xmx512m", "-Ddebug=true")
        }
        
        jvmArgs += profileJvmArgs
        jvmArgs += listOf(
            "--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED",
            "--add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED"
        )

        buildTypes.release.proguard {
            configurationFiles.from(project.file("proguard-rules.pro"))
        }
    }
}

/*
 * Команды для Desktop:
 * 1. debug: ./gradlew :desktopApp:run -PbuildProfile=debug
 * 2. release zip: ./gradlew :desktopApp:packagePortableZip -PbuildProfile=release
 */
tasks.register<Zip>("packagePortableZip") {
    group = "compose desktop"
    description = "Packages the portable application into a ZIP file (Release only)"

    // Отключаем выполнение для не-release сборки без создания лямбды-захвата
    enabled = isRelease

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    val archiveName = "portable-${platformName}-${buildProfile}-${appVersion}.zip"
    archiveFileName.set(archiveName)
    destinationDirectory.set(rootBuildsDir)

    from(layout.buildDirectory.dir("compose/binaries/$appVariant/app"))
    includeEmptyDirs = false

    // Безопасное связывание задач по имени без захвата TaskContainer
    dependsOn("createReleaseDistributable")
}

tasks.register<Copy>("copyPortableFolder") {
    group = "compose desktop"
    description = "Copies the portable application folder to builds"

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    val folderName = "portable-${platformName}-${buildProfile}-${appVersion}"
    into(rootBuildsDir.resolve(folderName))
    
    from(layout.buildDirectory.dir("compose/binaries/$appVariant/app"))
    includeEmptyDirs = false

    // Привязываем строго к конкретному варианту
    if (isRelease) {
        dependsOn(tasks.matching { it.name == "createReleaseDistributable" })
    } else {
        dependsOn(tasks.matching { it.name == "createDistributable" })
    }
}

// Запускаем копирование в папку builds только при явном вызове создания distributable
// Убираем finalizedBy, который мог срабатывать слишком часто
