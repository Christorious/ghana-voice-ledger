# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep all classes in the main package but allow obfuscation
-keep class com.voiceledger.ghana.** { *; }
-keep class com.voiceledger.ghana.data.local.entity.** { *; }
-keep class com.voiceledger.ghana.domain.model.** { *; }
-keep class com.voiceledger.ghana.presentation.** { *; }

# Keep Room database classes and annotations
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep @androidx.room.Database class *
-keep class * extends androidx.room.migration.Migration
-dontwarn androidx.room.paging.**
-keep class androidx.room.paging.** { *; }

# Keep all Room entities and model classes used in database
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public *** get*();
    public *** *();
}

# Keep Hilt/Dagger generated classes and annotations
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keep @dagger.hilt.android.AndroidEntryPoint class *
-keep @dagger.hilt.android.lifecycle.HiltViewModel class *
-keep @javax.inject.Singleton class *
-keep @dagger.Module class *
-keep @dagger.Component class *
-keep @dagger.Provides class *
-keep class * extends dagger.android.AndroidInjector

# Keep Hilt generated classes
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keep class * extends dagger.android.AndroidInjector
-keepclasseswithmembers class * {
    @dagger.hilt.android.AndroidEntryPoint <methods>;
}

# Keep Retrofit and OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface * extends <1>
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# Keep Gson classes
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Keep TensorFlow Lite and related classes
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.support.** { *; }
-keep class org.tensorflow.lite.gpu.** { *; }
-keep class org.tensorflow.lite.nnapi.** { *; }
-dontwarn org.tensorflow.lite.**

# Keep TensorFlow Lite interpreter and models
-keep class * extends org.tensorflow.lite.Interpreter
-keep class org.tensorflow.lite.Interpreter$Options
-keep class org.tensorflow.lite.Tensor

# Keep audio processing for TensorFlow Lite models
-keep class com.voiceledger.ghana.data.ml.** { *; }
-keep class com.voiceledger.ghana.domain.service.speech.** { *; }

# Keep Google Cloud Speech API
-keep class com.google.cloud.speech.** { *; }
-keep class com.google.api.gax.** { *; }
-keep class com.google.auth.** { *; }
-dontwarn com.google.cloud.speech.**
-dontwarn com.google.api.gax.**

# Keep Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Keep Compose classes and related components
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.material.** { *; }
-keep class androidx.compose.animation.** { *; }
-dontwarn androidx.compose.**

# Keep Compose runtime and compiler generated classes
-keepclassmembers class androidx.compose.** {
    @androidx.compose.runtime.Composable *;
}

# Keep navigation with Compose
-keep class androidx.navigation.compose.** { *; }

# Keep Compose UI classes that might be reflected
-keepclassmembers class * extends androidx.compose.ui.node.ModifierNode {
    *;
}

# Keep data classes used in Compose
-keepclassmembers class * {
    @androidx.compose.runtime.Immutable *;
    @androidx.compose.runtime.Stable *;
}

# Keep Coroutines
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

# Keep WorkManager
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.InputMerger
-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}

# Keep audio processing libraries
-keep class com.github.wendykierp.JTransforms.** { *; }
-dontwarn com.github.wendykierp.JTransforms.**

# Keep WebRTC VAD
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# Keep encryption libraries
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }
-dontwarn javax.crypto.**
-dontwarn java.security.**

# Keep Timber logging
-keep class timber.log.** { *; }
-dontwarn timber.log.**

# Keep Biometric authentication
-keep class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**

# Keep Navigation Component
-keep class androidx.navigation.** { *; }
-keepnames class androidx.navigation.fragment.NavHostFragment
-keepnames class * extends androidx.fragment.app.Fragment

# Keep Lifecycle components
-keep class androidx.lifecycle.** { *; }
-keep class * implements androidx.lifecycle.LifecycleObserver {
    <init>(...);
}
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keepclassmembers class androidx.lifecycle.Lifecycle$State { *; }
-keepclassmembers class androidx.lifecycle.Lifecycle$Event { *; }

# Keep Paging library
-keep class androidx.paging.** { *; }
-dontwarn androidx.paging.**

# Keep DataStore
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# Keep ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Keep ExoPlayer (if used for audio playback)
-keep class com.google.android.exoplayer2.** { *; }
-dontwarn com.google.android.exoplayer2.**

# Keep CameraX (if used)
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

-assumenosideeffects class timber.log.Timber {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}

# Optimization settings
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose