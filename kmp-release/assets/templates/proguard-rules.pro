# ===========================================================================
# ProGuard / R8 rules for KMP Mobile Template
# Stack: Room, Ktor, Koin, Ktorfit, kotlinx.serialization, Coil, Compose
#
# CRITICAL: Every library that uses reflection, code generation, or
# ServiceLoader needs explicit keep rules. Without them, isMinifyEnabled=true
# causes ClassNotFoundException, NoBeanDefFoundException, and silent image
# loading failures in release builds only.
#
# Replace {your.package} with your actual package name.
# ===========================================================================

# ---------------------------------------------------------------------------
# Kotlin / KMP core
# ---------------------------------------------------------------------------
-dontwarn kotlinx.**
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ---------------------------------------------------------------------------
# kotlinx.serialization
# R8 strips $$serializer classes because they look unused at compile time.
# Runtime deserialization then crashes with ClassNotFoundException.
# ---------------------------------------------------------------------------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keep,includedescriptorclasses class {your.package}.**$$serializer { *; }
-keepclassmembers class {your.package}.** {
    *** Companion;
}
-keepclasseswithmembers class {your.package}.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static ** INSTANCE;
}

# ---------------------------------------------------------------------------
# Ktor
# ---------------------------------------------------------------------------
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keepclassmembers class io.ktor.** { volatile <fields>; }
-keep class io.ktor.client.engine.** { *; }
-keep class io.ktor.serialization.** { *; }
-dontwarn org.slf4j.**

# ---------------------------------------------------------------------------
# Ktorfit
# Generated _KtorfitImpl classes must be kept or API calls fail.
# ---------------------------------------------------------------------------
-keep class de.jensklingenberg.ktorfit.** { *; }
-keep interface de.jensklingenberg.ktorfit.** { *; }
-keep @de.jensklingenberg.ktorfit.http.* class * { *; }
-keepclassmembers class * {
    @de.jensklingenberg.ktorfit.http.* <methods>;
}
-keep class **_KtorfitImpl { *; }
-keep class **_KtorfitConverterHelper { *; }

# ---------------------------------------------------------------------------
# Koin
# Koin resolves dependencies via reflection. R8 sees registered classes as
# "unused" and removes them -> NoBeanDefFoundException in release only.
# ---------------------------------------------------------------------------
-keep class org.koin.** { *; }
-dontwarn org.koin.**
-keepclassmembers class * {
    @org.koin.core.annotation.* <methods>;
    @org.koin.core.annotation.* <fields>;
}

# ---------------------------------------------------------------------------
# Room
# Room generates _Impl classes that R8 strips.
# "Cannot find implementation for <DatabaseClass>" in release.
# ---------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase { <init>(...); }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-dontwarn androidx.room.paging.**

# ---------------------------------------------------------------------------
# Coil (image loading)
# Coil discovers components via ServiceLoader. R8 strips them, causing
# images to silently fail (no crash, just blank).
# ---------------------------------------------------------------------------
-keep class coil3.** { *; }
-dontwarn coil3.**
-keep class coil3.network.** { *; }

# ---------------------------------------------------------------------------
# Compose
# ---------------------------------------------------------------------------
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }

# ---------------------------------------------------------------------------
# General Android
# ---------------------------------------------------------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}
