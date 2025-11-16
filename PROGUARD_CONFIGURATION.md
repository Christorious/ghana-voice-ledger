# ProGuard/R8 Configuration Guide

## Overview

This document explains the ProGuard/R8 configuration for the Ghana Voice Ledger app and provides guidance for maintaining and extending the rules.

## What is ProGuard/R8?

ProGuard/R8 is a code shrinker and obfuscator that:
- **Shrinks** the code by removing unused classes, methods, and fields
- **Obfuscates** the code by renaming classes and members with short, meaningless names
- **Optimizes** the bytecode for better performance
- **Removes** debug logging in release builds

## Configuration File

The ProGuard rules are defined in: `app/proguard-rules.pro`

The configuration is applied in release builds as specified in `app/build.gradle.kts`:
```kotlin
release {
    isMinifyEnabled = true
    isShrinkResources = true
    proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
    )
}
```

## Why We Need Keep Rules

Many Android libraries use reflection, annotation processing, or serialization which requires keeping certain classes from being obfuscated or removed. Without proper keep rules, your app may crash in release builds.

## Current Keep Rules Explained

### 1. Debug Information
```proguard
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
```
**Why**: Preserves line numbers for crash reports in App Center and other crash reporting tools, making debugging production issues possible while hiding the actual source file names.

### 2. Reflection & Serialization Attributes
```proguard
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
```
**Why**: Required for libraries that use reflection (Room, Hilt, Gson, kotlinx.serialization) to access class metadata at runtime.

### 3. Room Database
```proguard
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
```
**Why**: Room uses annotation processing and generates code at compile time. Entity classes, DAOs, and database classes must be kept because Room accesses them via reflection.

**Entity Classes Protected**:
- `Transaction`
- `DailySummary`
- `SpeakerProfile`
- `AudioMetadata`
- `OfflineOperation`
- `ProductVocabulary`

### 4. Hilt Dependency Injection
```proguard
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class *
-keep @dagger.hilt.android.lifecycle.HiltViewModel class *
```
**Why**: Hilt generates code at compile time and uses reflection for dependency injection. All annotated classes and generated code must be preserved.

**Special Note on Assisted Injection**:
```proguard
-keep @dagger.assisted.AssistedFactory class *
-keep @dagger.assisted.AssistedInject class *
```
This is critical for `OfflineSyncWorker` which uses `@AssistedInject` for WorkManager integration.

### 5. WorkManager
```proguard
-keep class * extends androidx.work.Worker
-keep @androidx.hilt.work.HiltWorker class *
```
**Why**: WorkManager instantiates Worker classes by name using reflection. All Worker subclasses must be kept.

**Worker Classes Protected**:
- `OfflineSyncWorker` (uses @HiltWorker annotation)

### 6. Jetpack Compose
```proguard
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** {
    @androidx.compose.runtime.Composable *;
}
```
**Why**: Compose uses code generation and reflection for state management. Composable functions and state holders must be preserved.

### 7. TensorFlow Lite
```proguard
-keep class org.tensorflow.lite.** { *; }
-keep class com.voiceledger.ghana.data.ml.** { *; }
```
**Why**: TensorFlow Lite models use JNI (Java Native Interface) which requires keeping model inference classes and native method signatures.

**ML Components Protected**:
- Speech recognition models
- Speaker identification models
- Transaction parsing models
- Entity extraction services

### 8. App Center (Analytics & Crashes)
```proguard
-keep class com.microsoft.appcenter.** { *; }
-keep class com.microsoft.appcenter.analytics.** { *; }
-keep class com.microsoft.appcenter.crashes.** { *; }
```
**Why**: App Center SDK uses reflection to access callback interfaces and event tracking methods. Without these rules, analytics and crash reporting will fail silently.

**Critical**: Also keeps exception constructors for proper crash stack traces:
```proguard
-keepclassmembers class * extends java.lang.Throwable {
    <init>(...);
}
```

### 9. kotlinx.serialization
```proguard
-keep @kotlinx.serialization.Serializable class * { *; }
-keep,includedescriptorclasses class com.voiceledger.ghana.**$$serializer { *; }
```
**Why**: kotlinx.serialization generates serializer classes at compile time with `$$serializer` suffix. These must be kept for JSON serialization/deserialization.

### 10. Parcelable Classes
```proguard
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keep @kotlinx.parcelize.Parcelize class * { *; }
```
**Why**: Kotlin's @Parcelize generates Parcelable implementation code that must be preserved for passing data between Android components.

### 11. Enum Classes
```proguard
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
    public *;
}
```
**Why**: Enums have special methods (values(), valueOf()) that are accessed reflectively. Without this rule, enum serialization and Room converters will break.

**Enums Used**:
- `OperationType`
- `OperationPriority`
- `OperationStatus`
- `TransactionType`
- Various other domain enums

### 12. SQLCipher (Encrypted Database)
```proguard
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
```
**Why**: SQLCipher uses JNI for native encryption. All classes interfacing with native code must be preserved.

### 13. Networking (Retrofit & OkHttp)
```proguard
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
```
**Why**: Retrofit uses reflection to parse HTTP annotations. API interface methods must be kept while still allowing some obfuscation for security.

### 14. Security & Biometric
```proguard
-keep class androidx.security.crypto.** { *; }
-keep class androidx.biometric.** { *; }
```
**Why**: EncryptedSharedPreferences and biometric authentication use encryption libraries that rely on reflection and JNI.

### 15. Image Loading (Coil)
```proguard
-keep class coil.** { *; }
```
**Why**: Coil uses reflection for image transformations and caching strategies.

### 16. Charts (MPAndroidChart)
```proguard
-keep class com.github.mikephil.charting.** { *; }
```
**Why**: Chart library uses reflection for rendering and data binding.

## Logging Removal in Release Builds

```proguard
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
-assumenosideeffects class timber.log.Timber {
    public static *** v(...);
    public static *** d(...);
}
```
**Why**: Removes verbose and debug logging calls completely from release builds, reducing APK size and improving performance. Info, warning, and error logs are kept for crash reporting.

## Testing Release Builds

### Build Release APK
```bash
./gradlew assembleRelease
```

### Check for ProGuard Warnings
```bash
./gradlew assembleRelease 2>&1 | grep -i "warning"
```

### Inspect Mapping File
After building, check `app/build/outputs/mapping/release/mapping.txt` to verify:
- Critical classes are NOT obfuscated (entities, DAOs, models)
- Internal implementation classes ARE obfuscated
- No excessive "class not found" warnings

### Install and Test
```bash
adb install app/build/outputs/apk/release/app-release.apk
```

**Test all features**:
- ✅ Voice recording and transcription
- ✅ Transaction creation and storage
- ✅ Offline queue synchronization
- ✅ Biometric authentication
- ✅ Analytics event tracking
- ✅ Crash reporting (test by forcing a crash)
- ✅ Background WorkManager jobs
- ✅ Speaker identification
- ✅ Multi-language support

## Adding Rules for New Dependencies

When adding a new library, follow these steps:

1. **Check library documentation** for ProGuard rules
2. **Look for reflection usage**: APIs, annotations, serialization
3. **Test release build** thoroughly
4. **Add keep rules** if crashes occur

### Common Patterns

**For libraries with annotations**:
```proguard
-keep @com.library.SomeAnnotation class * { *; }
```

**For libraries with reflection**:
```proguard
-keep class com.library.** { *; }
-dontwarn com.library.**
```

**For libraries with native code (JNI)**:
```proguard
-keepclasseswithmembernames class * {
    native <methods>;
}
```

**For libraries with serialization**:
```proguard
-keepclassmembers class com.library.** {
    <fields>;
    <init>(...);
}
```

## Troubleshooting

### App Crashes on Release Build
1. Check logcat for `ClassNotFoundException` or `NoSuchMethodException`
2. Add keep rules for the missing class/method
3. Rebuild and test

### Features Not Working
1. Check if the feature uses reflection, serialization, or annotations
2. Add keep rules for related classes
3. Verify rules with mapping.txt

### APK Size Too Large
1. Review keep rules - avoid using `keep class ** { *; }` globally
2. Use more specific rules with `keepclassmembers` instead of `keep`
3. Enable R8 full mode for better optimization

### Crashes in Analytics/Crash Reporting
1. Ensure line numbers are preserved: `-keepattributes SourceFile,LineNumberTable`
2. Verify exception constructors are kept
3. Check App Center rules are properly configured

## R8 vs ProGuard

Since AGP 3.4.0, R8 is the default shrinker. R8 is faster and produces smaller APKs than ProGuard while using the same configuration format. All rules in this project are R8-compatible.

**Advantages of R8**:
- Faster compilation
- Better dead code elimination
- Improved optimization
- Better Kotlin support

## Best Practices

1. ✅ **Always test release builds** on real devices
2. ✅ **Keep rules specific** - avoid blanket `keep` statements
3. ✅ **Document why** each keep rule exists
4. ✅ **Use `-dontwarn` sparingly** - it can hide real issues
5. ✅ **Preserve line numbers** for crash reporting
6. ✅ **Version control mapping.txt** for each release
7. ✅ **Test all reflection-based features** after rule changes
8. ✅ **Run instrumented tests** on release builds when possible

## Mapping File

The mapping file (`mapping.txt`) is generated during release builds and contains:
- Original class names → obfuscated names
- Original method names → obfuscated names
- Removed code information

**Keep this file** for each release to deobfuscate crash reports!

Location: `app/build/outputs/mapping/release/mapping.txt`

## Resources

- [Android ProGuard Documentation](https://developer.android.com/studio/build/shrink-code)
- [R8 Documentation](https://developer.android.com/studio/build/shrink-code#r8)
- [ProGuard Manual](https://www.guardsquare.com/manual/home)
- [Common ProGuard Rules](https://github.com/krschultz/android-proguard-snippets)

## Support

For issues with ProGuard configuration:
1. Check this documentation
2. Review library-specific ProGuard rules in library documentation
3. Test with `-dontobfuscate` to isolate obfuscation issues
4. Check mapping.txt for unexpected removals
5. Enable verbose ProGuard output: `-verbose`
