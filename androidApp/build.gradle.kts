import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
        // Базовые настройки для всех сборок
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

// Настройка оптимизаций Kotlin компилятора в зависимости от таска
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    val isReleaseOrDebugFull = name.contains("Release", ignoreCase = true) || 
                               name.contains("DebugFull", ignoreCase = true)
    
    compilerOptions {
        if (isReleaseOrDebugFull) {
            // Включаем оптимизации для тяжелых сборок
            freeCompilerArgs.add("-Xbackend-threads=0") // Параллельная компиляция
            if (name.contains("Release", ignoreCase = true)) {
                // В релизе отсекаем отладочные метаданные, где это допустимо
                freeCompilerArgs.add("-Xno-call-assertions")
                freeCompilerArgs.add("-Xno-receiver-assertions")
                freeCompilerArgs.add("-Xno-param-assertions")
            }
        }
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.okio)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "ru.quasaris.characternexus"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "ru.quasaris.characternexus"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = rootProject.extra["appVersionCode"] as Int
        versionName = rootProject.extra["appVersionName"] as String
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        /*
         * debugCheck:
         * Цель: Максимально быстрая сборка только для проверки компиляции (без глубоких оптимизаций).
         * Android: minificationEnabled = false, shrinkResources = false.
         */
        create("debugCheck") {
            initWith(getByName("debug"))
            isMinifyEnabled = false
            matchingFallbacks += listOf("debug")
        }

        /*
         * debugFull:
         * Цель: Максимальная производительность рантайма (как в релизе), но с возможностью полноценного дебага.
         * Android: Включить R8 (minificationEnabled = true), но оставить debuggable = true.
         */
        create("debugFull") {
            initWith(getByName("debug"))
            isMinifyEnabled = true
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            matchingFallbacks += listOf("release")
        }

        /*
         * release:
         * Цель: Итоговый релизный APK.
         * Android: Включить R8, minificationEnabled = true, shrinkResources = true.
         * Используется signingConfigs.debug, так как ключа разработчика пока нет.
         */
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

androidComponents {
    onVariants { variant ->
        val variantName = variant.name
        val versionName = rootProject.extra["appVersionName"] as String
        val buildsDir = rootProject.projectDir.resolve("builds")
        val taskName = variantName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        val copyTask = tasks.register<Copy>("copy${taskName}ApkToBuilds") {
            from(variant.artifacts.get(com.android.build.api.artifact.SingleArtifact.APK))
            into(buildsDir)
            include("**/*.apk")
            rename { fileName ->
                if (fileName.endsWith(".apk")) {
                    "${variantName}-${versionName}.apk"
                } else {
                    fileName
                }
            }
            eachFile {
                path = name // flatten directory structure
            }
            includeEmptyDirs = false
        }

        // Use a more robust way to hook into the assemble task
        tasks.matching { it.name == "assemble$taskName" }.configureEach {
            finalizedBy(copyTask)
        }
    }
}