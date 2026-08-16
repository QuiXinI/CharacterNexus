# OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn android.security.**
-dontwarn android.net.**
-dontwarn android.net.http.**
-dontwarn android.os.**
-dontwarn android.util.Log

# LWJGL
-dontwarn javax.annotation.**

# Haze
-dontwarn dev.chrisbanes.haze.**

# Ktor
-dontwarn io.ktor.utils.io.jvm.javaio.PollersKt

# Skiko / Compose Multiplatform Interop
-dontwarn androidx.compose.ui.graphics.ShaderBrush
-dontwarn org.jetbrains.skiko.swing.JbrSharedTexturesAdapter
-dontwarn com.jetbrains.**

# GraalVM / SVM (OkHttp)
-dontwarn okhttp3.internal.graal.**
-dontwarn org.graalvm.nativeimage.**
-dontwarn com.oracle.svm.core.annotate.**

# Okio
-keep class okio.** { *; }
-dontwarn okio.**
# Keep Kotlin internal names to prevent issues with extension functions
-keepclassmembernames class okio.** {
    *** *;
}
