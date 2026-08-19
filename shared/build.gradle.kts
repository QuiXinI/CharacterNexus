import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

val appVersionName = rootProject.extra["appVersionName"] as String

val generateBuildConstants = tasks.register("generateBuildConstants") {
    val version = appVersionName
    inputs.property("appVersionName", version)
    val outputDir = layout.buildDirectory.dir("generated/buildConstants/kotlin/commonMain")
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("ru/quasaris/characternexus/generated/BuildConstants.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            package ru.quasaris.characternexus.generated

            object BuildConstants {
                const val VERSION = "$version"
            }
            """.trimIndent()
        )
    }
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    jvm()
    
    android {
       namespace = "ru.quasaris.characternexus.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.mpfilepicker)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.palette)
        }
        commonMain {
            kotlin.srcDir(generateBuildConstants)
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.material.icons)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.uiToolingPreview)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)
                implementation(libs.androidx.savedstate)
                implementation(libs.androidx.datastore.preferences)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.navigation.compose)
                implementation(libs.haze)
                implementation(libs.kotlinx.datetime)
                implementation(libs.okio)
                implementation(libs.coil.compose)
                implementation(libs.coil.network.ktor)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.json)
                implementation(libs.reorderable)
            }
        }
        jvmMain.dependencies {
            implementation(libs.mpfilepicker)
            implementation(libs.ktor.client.okhttp)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
