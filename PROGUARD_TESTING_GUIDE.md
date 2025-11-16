# ProGuard/R8 Testing Guide

This guide walks you through testing the ProGuard/R8 configuration to ensure your release build works correctly.

## Quick Verification

Run the automated verification script:

```bash
./scripts/verify-proguard.sh
```

This script checks:
- ✅ ProGuard rules file exists and is valid
- ✅ All critical library rules are present
- ✅ Debug information is preserved for crash reports
- ✅ Optimization settings are configured
- ✅ Debug logging removal is enabled
- ✅ Build configuration is correct

## Step 1: Build Release APK

### Prerequisites
- Android SDK installed (API 34 minimum)
- Java 17 installed
- Signing key configured (optional for testing)

### Build Commands

**Build all release variants:**
```bash
./gradlew assembleRelease
```

**Build specific flavor:**
```bash
./gradlew assembleProdRelease
./gradlew assembleDevRelease
./gradlew assembleStagingRelease
```

**Clean and build:**
```bash
./gradlew clean assembleProdRelease
```

### Expected Output

Build should complete without ProGuard warnings. Look for:
```
BUILD SUCCESSFUL in Xs
```

Release APKs will be in:
```
app/build/outputs/apk/prod/release/app-prod-release.apk
app/build/outputs/apk/dev/release/app-dev-release.apk
app/build/outputs/apk/staging/release/app-staging-release.apk
```

## Step 2: Verify Build Artifacts

### Check APK Size

```bash
ls -lh app/build/outputs/apk/prod/release/*.apk
```

Expected size: ~20-40MB (depending on included models and resources)

### Inspect Mapping File

The mapping file shows which classes were obfuscated:

```bash
cat app/build/outputs/mapping/release/mapping.txt | head -50
```

**What to verify:**
- ✅ Internal classes ARE obfuscated (short names like `a`, `b`, `c`)
- ✅ Entity classes are NOT obfuscated (should see full names)
- ✅ DAO classes are NOT obfuscated
- ✅ Model classes are NOT obfuscated
- ✅ Public API classes are preserved

Example of good obfuscation:
```
com.voiceledger.ghana.data.local.entity.Transaction -> com.voiceledger.ghana.data.local.entity.Transaction
com.voiceledger.ghana.presentation.internal.HelperClass -> a.b.c
```

### Check ProGuard Configuration

```bash
cat app/build/outputs/mapping/release/configuration.txt
```

This shows the final ProGuard configuration used for the build.

### Verify Resource Shrinking

```bash
cat app/build/outputs/mapping/release/resources.txt
```

Shows which resources were removed to reduce APK size.

## Step 3: Install and Test

### Install Release APK

**On connected device/emulator:**
```bash
adb install -r app/build/outputs/apk/prod/release/app-prod-release.apk
```

**List installed packages:**
```bash
adb shell pm list packages | grep voiceledger
```

**Launch app:**
```bash
adb shell am start -n com.voiceledger.ghana/.MainActivity
```

## Step 4: Feature Testing Checklist

Test all features to ensure ProGuard hasn't broken anything:

### ✅ Voice Recording & Processing
- [ ] Open app and grant microphone permission
- [ ] Record a voice transaction
- [ ] Verify audio is captured correctly
- [ ] Check transcription works
- [ ] Verify TensorFlow Lite models load

### ✅ Transaction Management
- [ ] Create a new transaction manually
- [ ] View transaction list
- [ ] Edit a transaction
- [ ] Delete a transaction
- [ ] Filter/search transactions
- [ ] Export transactions

### ✅ Database Operations (Room)
- [ ] App persists data between restarts
- [ ] Transactions are saved correctly
- [ ] Daily summaries are generated
- [ ] Speaker profiles are stored (if enabled)
- [ ] Product vocabulary is accessible

### ✅ Offline Queue & Sync
- [ ] Turn off network
- [ ] Create transactions offline
- [ ] View pending operations
- [ ] Turn on network
- [ ] Verify automatic sync works
- [ ] Check WorkManager background job

### ✅ Biometric Authentication
- [ ] Enable biometric lock in settings
- [ ] Lock the app
- [ ] Verify biometric prompt appears
- [ ] Authenticate successfully
- [ ] Test failed authentication

### ✅ Analytics & Crash Reporting
- [ ] Open settings and verify App Center is initialized
- [ ] Navigate through different screens (generates events)
- [ ] Force a test crash (if dev mode enabled)
- [ ] Check App Center portal for events

### ✅ UI & Navigation (Compose)
- [ ] All screens render correctly
- [ ] Navigation works smoothly
- [ ] Bottom navigation functions
- [ ] Dialog boxes display properly
- [ ] Animations work correctly
- [ ] Theme switching works (if enabled)

### ✅ Multi-language Support
- [ ] Change language in settings
- [ ] Verify UI updates to new language
- [ ] Test voice recognition in different language
- [ ] Verify translations are loaded

### ✅ Background Services
- [ ] Check VoiceAgentService can start
- [ ] Verify notifications appear
- [ ] Test daily summary notifications
- [ ] Check power optimization doesn't kill service

### ✅ Security Features
- [ ] Encrypted database loads correctly
- [ ] Encrypted SharedPreferences work
- [ ] API keys are protected
- [ ] No sensitive data in logs

## Step 5: Crash Testing

### Monitor LogCat

```bash
adb logcat -c  # Clear logs
adb logcat | grep -i "voiceledger\|FATAL\|AndroidRuntime"
```

### Common ProGuard Issues to Watch For

**ClassNotFoundException:**
```
java.lang.ClassNotFoundException: com.example.SomeClass
```
**Solution:** Add keep rule for the missing class

**NoSuchMethodException:**
```
java.lang.NoSuchMethodException: <init> [class android.content.Context]
```
**Solution:** Add keep rule for constructors

**NullPointerException in serialization:**
```
NullPointerException at kotlinx.serialization...
```
**Solution:** Verify kotlinx.serialization rules

**Room crashes:**
```
IllegalStateException: Cannot access database on the main thread
```
**Solution:** This is not a ProGuard issue, but verify Room rules

**Reflection failures:**
```
IllegalAccessException: Class ref com.example...
```
**Solution:** Add keep rules for reflected classes

## Step 6: Performance Testing

### APK Size Comparison

Compare debug vs release APK sizes:
```bash
ls -lh app/build/outputs/apk/prod/debug/*.apk
ls -lh app/build/outputs/apk/prod/release/*.apk
```

Release should be 30-50% smaller than debug.

### App Startup Time

Use Android Studio Profiler or:
```bash
adb shell am start -W com.voiceledger.ghana/.MainActivity
```

Measure:
- TotalTime: Full startup time
- WaitTime: Time to display first frame

### Memory Usage

```bash
adb shell dumpsys meminfo com.voiceledger.ghana
```

Check for memory leaks after repeated operations.

## Step 7: App Center Integration Test

### Verify Analytics

1. Open App Center dashboard: https://appcenter.ms
2. Navigate to your app
3. Check Analytics → Events
4. Verify events are being tracked:
   - App launches
   - Screen views
   - Transaction created
   - Voice recording started/completed

### Verify Crash Reporting

1. Force a test crash (if dev mode):
   ```kotlin
   throw RuntimeException("Test crash for ProGuard verification")
   ```

2. Check App Center → Diagnostics → Crashes
3. Verify stack trace is deobfuscated (shows real class names)
4. Verify line numbers are correct

**If crashes are obfuscated:**
- Ensure mapping.txt is uploaded to App Center
- Check `-keepattributes SourceFile,LineNumberTable` is in ProGuard rules

## Step 8: Verify Specific Libraries

### TensorFlow Lite Models

Test model inference:
```bash
adb logcat | grep -i "tflite\|tensorflow"
```

Look for:
- ✅ Models loading successfully
- ✅ Inference results
- ❌ JNI errors
- ❌ UnsatisfiedLinkError

### Hilt Dependency Injection

Verify all dependencies are injected:
```bash
adb logcat | grep -i "hilt\|dagger"
```

Look for:
- ✅ Hilt components created
- ❌ No such component
- ❌ Binding not found

### Room Database

Test database queries:
```bash
adb logcat | grep -i "room\|sqlite"
```

Look for:
- ✅ Database opened successfully
- ✅ Queries executing
- ❌ Table doesn't exist
- ❌ Column not found

### WorkManager

Check background workers:
```bash
adb logcat | grep -i "workmanager\|worker"
```

Look for:
- ✅ Worker scheduled
- ✅ Worker executing
- ❌ Worker class not found

## Troubleshooting

### Build Fails with ProGuard Errors

**Error: Duplicate class found**
- Check for conflicting dependencies
- Update library versions
- Add exclusions in `build.gradle.kts`

**Error: Can't find referenced class**
- Add `-dontwarn` for the missing class
- Add the missing dependency
- Check if library needs ProGuard rules

### App Crashes on Launch

1. **Check logcat for exceptions**
2. **Temporarily disable obfuscation** to isolate issue:
   ```groovy
   -dontobfuscate
   -dontoptimize
   ```
3. **Re-enable one at a time** to find problematic rule
4. **Add specific keep rules** for failing classes

### Features Not Working

1. **Test in debug build first** - if it works there, it's ProGuard
2. **Check logs** for reflection/serialization errors
3. **Add keep rules** for affected classes
4. **Test again** after each rule addition

### Crashes in Analytics

1. **Verify App Center SDK rules** are present
2. **Check APP_CENTER_SECRET** is configured
3. **Enable verbose logging** temporarily:
   ```kotlin
   AppCenter.setLogLevel(Log.VERBOSE)
   ```

### Database Errors

1. **Verify all entities** have keep rules
2. **Check DAOs** are not obfuscated
3. **Verify migrations** are kept
4. **Test schema export** is correct

## Advanced Testing

### Automated UI Tests on Release

Run instrumented tests on release build:
```bash
./gradlew connectedProdReleaseAndroidTest
```

### Compare Debug vs Release Behavior

Run the same test scenario on both builds and compare:
```bash
# Debug
adb install app/build/outputs/apk/prod/debug/app-prod-debug.apk

# Release
adb install app/build/outputs/apk/prod/release/app-prod-release.apk
```

### ProGuard Verification Mode

Temporarily add to ProGuard rules for testing:
```proguard
-printconfiguration
-printusage
-printseeds
```

This generates files showing:
- `configuration.txt` - Final configuration
- `usage.txt` - Removed code
- `seeds.txt` - Entry points kept

## Checklist Summary

Before releasing to production:

- [ ] ProGuard verification script passes
- [ ] Release build completes without warnings
- [ ] APK size is reasonable (30-50% smaller than debug)
- [ ] Mapping file shows appropriate obfuscation
- [ ] App installs and launches successfully
- [ ] All voice features work correctly
- [ ] Database operations function properly
- [ ] Offline sync works as expected
- [ ] Biometric authentication functions
- [ ] Analytics events are tracked
- [ ] Crash reporting works and deobfuscates correctly
- [ ] No crashes in 30 minutes of testing
- [ ] Performance is acceptable
- [ ] Memory usage is normal
- [ ] Background services work
- [ ] Notifications display correctly

## Resources

- [ProGuard Documentation](https://www.guardsquare.com/manual/home)
- [R8 Documentation](https://developer.android.com/studio/build/shrink-code)
- [Common ProGuard Issues](https://www.guardsquare.com/manual/troubleshooting)
- [App Center Crash Reporting](https://docs.microsoft.com/en-us/appcenter/crashes/)

## Support

If you encounter issues:

1. Review `PROGUARD_CONFIGURATION.md` for rule explanations
2. Check library documentation for required ProGuard rules
3. Search for similar issues on Stack Overflow
4. Test with `-dontobfuscate` to isolate the problem
5. Add specific keep rules based on crash logs

## Maintenance

When adding new dependencies:

1. Check library documentation for ProGuard rules
2. Add rules to `app/proguard-rules.pro`
3. Update this guide with new test scenarios
4. Run full test suite on release build
5. Update `PROGUARD_CONFIGURATION.md` with new rules

---

**Last Updated:** 2024-11-14
**ProGuard Version:** R8 (bundled with AGP 8.1.4)
**App Version:** 1.0.0
