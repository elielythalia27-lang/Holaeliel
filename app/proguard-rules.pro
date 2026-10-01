# ProGuard / R8 Security & Obfuscation Rules for Download Free

# Keep data models intact for JSON reflection and serialization
-keep class com.example.data.model.** { *; }

# Keep Gson annotations and classes
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn com.google.gson.**
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.code.gson.annotations.SerializedName <fields>;
}

# Keep Retrofit & OkHttp
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Exceptions, InnerClasses
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# Keep Moshi
-dontwarn com.squareup.moshi.**
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# Keep Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep AndroidX WorkManager
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# Keep Kotlin Coroutines
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# Obfuscation settings to protect source code against decompilers (Jadx / Apktool)
-repackageclasses ''
-allowaccessmodification
