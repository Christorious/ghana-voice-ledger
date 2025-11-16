# Voice Ledger ProGuard / R8 configuration
# ------------------------------------------------------------------------------
# This configuration keeps the minimum surface area needed for reflection-based
# frameworks while allowing the remainder of the app to be obfuscated and
# optimised for release builds.

# ----------------------------------------------------------------------------
# Core attributes retained for libraries that rely on runtime annotations or
# Kotlin metadata.
# ----------------------------------------------------------------------------
-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature,KotlinMetadata

# ----------------------------------------------------------------------------
# Android entry points referenced from the manifest.
# ----------------------------------------------------------------------------
-keep class com.voiceledger.ghana.VoiceLedgerApplication { *; }
-keep class com.voiceledger.ghana.presentation.MainActivity { *; }

# ----------------------------------------------------------------------------
# Dagger / Hilt generated components, entry points, and managers.
# ----------------------------------------------------------------------------
-keep class dagger.hilt.internal.aggregatedroot.codegen.* { *; }
-keep class dagger.hilt.internal.aggregateddeps.* { *; }
-keep class dagger.hilt.internal.processedrootsentinel.codegen.* { *; }
-keep class dagger.hilt.android.internal.builders.* { *; }
-keep class dagger.hilt.android.internal.lifecycle.* { *; }
-keep class dagger.hilt.android.internal.managers.* { *; }
-keep class dagger.hilt.android.internal.modules.* { *; }
-keep class dagger.hilt.android.internal.scoping.* { *; }
-keep class hilt_aggregated_deps.* { *; }
-keep interface dagger.hilt.internal.GeneratedComponent
-keep class * implements dagger.hilt.internal.GeneratedComponent { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponentManager { *; }
-keep class androidx.hilt.work.HiltWorkerFactory { *; }

# ----------------------------------------------------------------------------
# WorkManager & Hilt workers (instantiated via reflection by WorkManager).
# ----------------------------------------------------------------------------
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keepclassmembers class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ----------------------------------------------------------------------------
# Room persistence library (entities, DAOs, database implementations).
# ----------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomDatabase_Impl { *; }
-keep class * extends androidx.room.migration.Migration { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.TypeConverters class * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-dontwarn androidx.room.paging.**
-keep class androidx.room.paging.** { *; }

# ----------------------------------------------------------------------------
# JSON serialisation (Gson + kotlinx.serialization).
# ----------------------------------------------------------------------------
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
    @kotlinx.serialization.SerialName *;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# ----------------------------------------------------------------------------
# Retrofit / OkHttp networking stack.
# ----------------------------------------------------------------------------
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation interface retrofit2.Call
-keep,allowobfuscation interface * extends retrofit2.Call
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# ----------------------------------------------------------------------------
# Kotlin coroutines (ensure dispatchers and debug helpers are retained).
# ----------------------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembers class kotlin.coroutines.SafeContinuation {
    volatile <fields>;
}
-dontwarn java.lang.ClassValue
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# ----------------------------------------------------------------------------
# WorkManager, DataStore, and Paging integrations.
# ----------------------------------------------------------------------------
-dontwarn androidx.work.**
-dontwarn androidx.datastore.**
-dontwarn androidx.paging.**

# ----------------------------------------------------------------------------
# TensorFlow Lite models and audio ML pipelines.
# ----------------------------------------------------------------------------
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.support.** { *; }
-keep class org.tensorflow.lite.gpu.** { *; }
-keep class org.tensorflow.lite.nnapi.** { *; }
-dontwarn org.tensorflow.lite.**

# ----------------------------------------------------------------------------
# Google Cloud Speech (optional feature) & supporting libraries.
# ----------------------------------------------------------------------------
-keep class com.google.cloud.speech.** { *; }
-keep class com.google.api.gax.** { *; }
-keep class com.google.auth.** { *; }
-dontwarn com.google.cloud.speech.**
-dontwarn com.google.api.gax.**
-dontwarn io.grpc.**
-dontwarn org.threeten.bp.**

# ----------------------------------------------------------------------------
# Firebase (optional feature toggles).
# ----------------------------------------------------------------------------
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ----------------------------------------------------------------------------
# App Center analytics & diagnostics.
# ----------------------------------------------------------------------------
-keep class com.microsoft.appcenter.** { *; }
-dontwarn com.microsoft.appcenter.**

# ----------------------------------------------------------------------------
# SQLCipher & security libraries.
# ----------------------------------------------------------------------------
-keep class net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**
-keep class javax.crypto.** { *; }
-dontwarn javax.crypto.**
-keep class java.security.** { *; }
-dontwarn java.security.**

# ----------------------------------------------------------------------------
# Optional third-party integrations (WebRTC VAD, audio transforms, etc.).
# ----------------------------------------------------------------------------
-keep class com.github.wendykierp.JTransforms.** { *; }
-dontwarn com.github.wendykierp.JTransforms.**
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# ----------------------------------------------------------------------------
# Logging removal for release builds.
# ----------------------------------------------------------------------------
-assumenosideeffects class android.util.Log {
    public static int v(java.lang.String, java.lang.String);
    public static int v(java.lang.String, java.lang.String, java.lang.Throwable);
    public static int d(java.lang.String, java.lang.String);
    public static int d(java.lang.String, java.lang.String, java.lang.Throwable);
    public static int i(java.lang.String, java.lang.String);
    public static int i(java.lang.String, java.lang.String, java.lang.Throwable);
    public static int w(java.lang.String, java.lang.String);
    public static int w(java.lang.String, java.lang.String, java.lang.Throwable);
    public static int e(java.lang.String, java.lang.String);
    public static int e(java.lang.String, java.lang.String, java.lang.Throwable);
    public static boolean isLoggable(java.lang.String, int);
}

-assumenosideeffects class timber.log.Timber {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}
