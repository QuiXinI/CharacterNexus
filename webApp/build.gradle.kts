import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                devServer = (devServer ?: org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.DevServer()).apply {
                    port = 8080
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        val webMain by creating {
            kotlin.srcDirs("src/webMain/kotlin")
            resources.srcDirs("src/webMain/resources")
            dependencies {
                implementation(project(":shared"))
                implementation(libs.compose.ui)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.material.icons)
                implementation(libs.compose.components.resources)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)
            }
        }
        wasmJsMain {
            dependsOn(webMain)
        }
    }
}

tasks.matching { it.name == "wasmJsProcessResources" }.configureEach {
    val copyTask = this as? Copy
    copyTask?.duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
