# ProGuard/R8 Quick Reference

## Quick Commands

```bash
# Verify ProGuard configuration
./scripts/verify-proguard.sh

# Build release APK
./gradlew assembleProdRelease

# Install release APK
adb install -r app/build/outputs/apk/prod/release/app-prod-release.apk

# Monitor logs
adb logcat | grep -i "voiceledger\|FATAL"

# View mapping file
cat app/build/outputs/mapping/release/mapping.txt
```

## File Locations

| File | Location |
|------|----------|
| ProGuard Rules | `app/proguard-rules.pro` |
| Mapping File | `app/build/outputs/mapping/release/mapping.txt` |
| Configuration | `app/build/outputs/mapping/release/configuration.txt` |
| Resources | `app/build/outputs/mapping/release/resources.txt` |
| Release APK | `app/build/outputs/apk/prod/release/app-prod-release.apk` |

## Common ProGuard Rules Patterns

### Keep a Class
```proguard
-keep class com.example.MyClass { *; }
```

### Keep Classes with Annotation
```proguard
-keep @com.example.MyAnnotation class * { *; }
```

### Keep Class Members
```proguard
-keepclassmembers class com.example.MyClass {
    public <fields>;
    public <methods>;
}
```

### Keep Native Methods
```proguard
-keepclasseswithmembernames class * {
    native <methods>;
}
```

### Don't Warn
```proguard
-dontwarn com.example.problematic.**
```

## Troubleshooting Quick Fixes

### ClassNotFoundException
```proguard
-keep class com.example.MissingClass { *; }
```

### NoSuchMethodException
```proguard
-keepclassmembers class com.example.MyClass {
    public <init>(...);
}
```

### Reflection Issues
```proguard
-keepattributes RuntimeVisibleAnnotations
-keep class com.example.ReflectedClass { *; }
```

### Serialization Issues
```proguard
-keepclassmembers class com.example.SerializedClass {
    <fields>;
    <init>(...);
}
```

## Test Checklist

- [ ] Run verification script
- [ ] Build release APK successfully
- [ ] Install on device
- [ ] Test voice recording
- [ ] Test database operations
- [ ] Test offline sync
- [ ] Test biometric auth
- [ ] Check analytics events
- [ ] Verify no crashes for 30 minutes

## Key Rules in This Project

| Library | Rule Pattern |
|---------|--------------|
| Room | `-keep @androidx.room.Entity class *` |
| Hilt | `-keep @dagger.hilt.android.AndroidEntryPoint class *` |
| Compose | `-keep class androidx.compose.** { *; }` |
| TensorFlow | `-keep class org.tensorflow.lite.** { *; }` |
| App Center | `-keep class com.microsoft.appcenter.** { *; }` |
| WorkManager | `-keep @androidx.hilt.work.HiltWorker class *` |
| Serialization | `-keep @kotlinx.serialization.Serializable class *` |
| Enums | `-keepclassmembers enum * { public static **[] values(); }` |

## Important Attributes to Keep

```proguard
-keepattributes SourceFile,LineNumberTable          # Crash reports
-keepattributes Signature                            # Generics
-keepattributes *Annotation*                         # Annotations
-keepattributes InnerClasses,EnclosingMethod        # Inner classes
```

## Documentation Files

1. **PROGUARD_CONFIGURATION.md** - Detailed configuration explanation
2. **PROGUARD_TESTING_GUIDE.md** - Step-by-step testing procedures
3. **PROGUARD_COMPLETION_SUMMARY.md** - Summary of what was completed
4. **PROGUARD_QUICK_REFERENCE.md** - This quick reference guide

## When to Update ProGuard Rules

- ✅ Adding a new library dependency
- ✅ Using reflection or dynamic class loading
- ✅ Implementing serialization
- ✅ Adding annotation processors
- ✅ Using JNI/native code
- ✅ When release build crashes but debug works

## Verification Results

**Status:** ✅ All Checks Passed

- Configuration file: 353 lines
- Keep rules: 127
- Libraries covered: 25+
- Documentation: Complete

---

For detailed information, see:
- Full configuration details: `PROGUARD_CONFIGURATION.md`
- Testing procedures: `PROGUARD_TESTING_GUIDE.md`
- Completion summary: `PROGUARD_COMPLETION_SUMMARY.md`
