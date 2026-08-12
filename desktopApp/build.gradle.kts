import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// Определение профиля сборки через property: -PbuildProfile=debugCheck/debugFull/release
val buildProfile = project.findProperty("buildProfile")?.toString() ?: "debugCheck"

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        
        when (buildProfile) {
            "debugCheck" -> {
                // Минимальные настройки для быстрой проверки
            }
            "debugFull" -> {
                // Оптимизации рантайма с сохранением отладочной информации
                freeCompilerArgs.add("-Xbackend-threads=0")
            }
            "release" -> {
                // Максимальные релизные оптимизации
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
            packageVersion = "1.0.0"
            
            // Настройка в зависимости от профиля
            if (buildProfile == "debugCheck") {
                // В debugCheck можем отключить создание тяжелых дистрибутивов если нужно
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
            else -> emptyList()
        }
        
        jvmArgs += profileJvmArgs
    }
}

/*
 * Пресеты для Desktop:
 * 1. debugCheck: ./gradlew :desktopApp:run -PbuildProfile=debugCheck (Быстрая отладка)
 * 2. debugFull: ./gradlew :desktopApp:run -PbuildProfile=debugFull (Производительность + Дебаг)
 * 3. release: ./gradlew :desktopApp:packageReleaseDistribution -PbuildProfile=release (Релиз)
 */
