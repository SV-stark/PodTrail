# Room
-keepclassmembers class * extends androidx.room3.RoomDatabase {
    <init>(...);
}
-keep class * extends androidx.room3.migration.Migration {
    <init>(...);
}
-keep @androidx.room3.Entity class *
-keep class * { @androidx.room3.PrimaryKey *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.stream.** { *; }
-keep class com.stark.podtrail.data.** { *; }
-keep class com.stark.podtrail.network.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.android.HandlerContext {
    volatile <fields>;
}

# General
-keepattributes SourceFile,LineNumberTable
-keep class androidx.compose.material.icons.** { *; }
