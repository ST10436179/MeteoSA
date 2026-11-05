# R8 keep rules for the release build.

# Gson reflects over our DTOs (Retrofit request/response bodies); keep fields + names.
-keep class com.meteosa.app.data.remote.dto.** { *; }
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod

# Retrofit interfaces use generic signatures resolved at runtime.
-keep,allowobfuscation interface com.meteosa.app.data.remote.*Api
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Room entities/DAOs are referenced by generated code.
-keep class com.meteosa.app.data.local.db.** { *; }

# WorkManager instantiates workers reflectively.
-keep class * extends androidx.work.ListenableWorker { *; }

# Firebase Messaging service is declared in the manifest.
-keep class com.meteosa.app.notifications.** { *; }

# osmdroid
-dontwarn org.osmdroid.**

# Keep line numbers for readable crash reports.
-keepattributes SourceFile, LineNumberTable

# Gson: keep generic type info and TypeToken subclasses (R8 full mode strips them otherwise).
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.meteosa.app.data.local.db.CachedDailyForecast { *; }
