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

# Kotlin Coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
-dontnote kotlinx.coroutines.**
-keepattributes SourceFile,LineNumberTable,Signature

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontnote io.ktor.**

# Kotlinx Datetime
-keep class kotlinx.datetime.** { *; }
-dontwarn kotlinx.datetime.**
-dontnote kotlinx.datetime.**

# Coil 3
-keep class coil3.** { *; }
-dontwarn coil3.**
-dontnote coil3.**
# ServiceLoader for Coil
-keep class * implements coil3.fetch.Fetcher$Factory { *; }
-keep class * implements coil3.decode.Decoder$Factory { *; }

# Skiko / Compose Multiplatform Interop
-keep class org.jetbrains.skiko.** { *; }
-keep class org.jetbrains.skia.** { *; }
-dontwarn androidx.compose.ui.graphics.ShaderBrush
-dontwarn org.jetbrains.skiko.swing.JbrSharedTexturesAdapter
-dontwarn com.jetbrains.**
-dontnote org.jetbrains.skiko.**
-dontnote com.jetbrains.**

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

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-keep class kotlinx.serialization.** { *; }
-keep @kotlinx.serialization.Serializable class ** { *; }
-keepclassmembers class ** {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class **$serializer {
    public static ** INSTANCE;
}
-keepclassmembers enum * { *; }

# App Models & Backend
-keep class ru.quasaris.characternexus.model.** { *; }
-keep class ru.quasaris.characternexus.backend.ImportResult { *; }
-keep class ru.quasaris.characternexus.backend.CharacterManifest { *; }
-keep class ru.quasaris.characternexus.backend.ManifestEntry { *; }
-keep class ru.quasaris.characternexus.backend.storage.** { *; }
-keep class ru.quasaris.characternexus.util.Logger { *; }
-dontnote ru.quasaris.characternexus.**
