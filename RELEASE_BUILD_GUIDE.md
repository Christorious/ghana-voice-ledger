# Ghana Voice Ledger - Release Build Guide

This guide covers the complete process of creating production-ready release builds for the Ghana Voice Ledger Android app.

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Keystore Generation](#keystore-generation)
4. [Signing Configuration](#signing-configuration)
5. [Build Variants](#build-variants)
6. [Building Releases](#building-releases)
7. [Testing Release Builds](#testing-release-builds)
8. [Troubleshooting](#troubleshooting)
9. [Security Best Practices](#security-best-practices)

## Overview

The Ghana Voice Ledger app uses a comprehensive release build configuration that includes:

- **Code Signing**: Secure APK/AAB signing with configurable keystore
- **Code Obfuscation**: ProGuard/R8 minification and obfuscation
- **Build Variants**: Multiple flavors for different environments
- **Optimization**: Resource shrinking and build optimizations
- **Security**: Proper handling of sensitive configuration

## Prerequisites

Before building release versions, ensure you have:

1. **Android Studio** or **Android SDK** installed
2. **Java 17** (JDK 17) or higher
3. **Gradle** (included with Android Studio)
4. **Physical Android device** for testing
5. **Git** for version control

## Keystore Generation

### Option 1: Using the Automated Script (Recommended)

Run the keystore generation script:

```bash
./scripts/generate-keystore.sh
```

The script will:
- Create a `keystore/` directory
- Generate a JKS keystore with 2048-bit RSA keys
- Set validity for 25 years
- Prompt for secure passwords

### Option 2: Manual Generation

Generate a keystore manually using `keytool`:

```bash
keytool -genkeypair \
    -v \
    -storetype JKS \
    -keystore keystore/ghana-voice-ledger-release.jks \
    -keyalg RSA \
    -keysize 2048 \
    -validity 9125 \
    -alias ghana-voice-ledger-key \
    -dname "CN=Ghana Voice Ledger, OU=Development, O=Voice Ledger, L=Accra, ST=Greater Accra, C=GH"
```

### Security Notes

- **Never commit keystore files** to version control
- **Use strong, unique passwords** (consider a password manager)
- **Store backups** in secure, separate locations
- **Limit access** to keystore files to authorized personnel only

## Signing Configuration

### Setting up keystore.properties

1. Copy the example file:
   ```bash
   cp keystore.properties.example keystore.properties
   ```

2. Edit `keystore.properties` with your credentials:
   ```properties
   RELEASE_KEYSTORE_FILE=keystore/ghana-voice-ledger-release.jks
   RELEASE_KEYSTORE_PASSWORD=your-secure-keystore-password
   RELEASE_KEY_ALIAS=ghana-voice-ledger-key
   RELEASE_KEY_PASSWORD=your-secure-key-password
   ```

3. Ensure `keystore.properties` is in `.gitignore` (already configured)

### Configuration Details

The build configuration automatically:
- Loads signing credentials from `keystore.properties`
- Enables APK Signature Scheme v2, v3, and v4
- Falls back to unsigned builds if keystore is not configured
- Applies signing only to release builds

## Build Variants

The app supports three build flavors:

### Development (dev)
- **Application ID**: `com.voiceledger.ghana.dev`
- **API Base URL**: `https://api-dev.voiceledger.com/`
- **Features**: All features enabled including beta features
- **Use Case**: Development and testing with debug features

### Staging (staging)
- **Application ID**: `com.voiceledger.ghana.staging`
- **API Base URL**: `https://api-staging.voiceledger.com/`
- **Features**: Production-like environment without beta features
- **Use Case**: Pre-production testing

### Production (prod)
- **Application ID**: `com.voiceledger.ghana`
- **API Base URL**: `https://api.voiceledger.com/`
- **Features**: Production configuration
- **Use Case**: Production deployment

## Building Releases

### Option 1: Using the Build Script (Recommended)

Build all variants:
```bash
./scripts/build-release.sh prod all
```

Build specific flavor:
```bash
./scripts/build-release.sh staging apk
./scripts/build-release.sh dev aab
```

### Option 2: Using Gradle Directly

Build APK:
```bash
./gradlew assembleProdRelease
./gradlew assembleStagingRelease
./gradlew assembleDevRelease
```

Build AAB (for Google Play):
```bash
./gradlew bundleProdRelease
./gradlew bundleStagingRelease
./gradlew bundleDevRelease
```

### Build Outputs

- **APK Location**: `app/build/outputs/apk/<flavor>/release/`
- **AAB Location**: `app/build/outputs/bundle/<flavor>/release/`
- **Mapping File**: `app/build/outputs/mapping/<flavor>/release/mapping.txt`

## Testing Release Builds

### Installation

Install APK on device:
```bash
adb install -r app/build/outputs/apk/prod/release/app-prod-release.apk
```

### Verification Checklist

Test the following features on release builds:

#### Core Functionality
- [ ] App launches successfully
- [ ] Voice recording and processing
- [ ] Transaction logging and display
- [ ] Offline mode functionality
- [ ] Data synchronization

#### Security Features
- [ ] Biometric authentication
- [ ] Data encryption/decryption
- [ ] Secure storage functionality
- [ ] Permission handling

#### UI/UX
- [ ] All screens render correctly
- [ ] Material 3 theme applied
- [ ] Navigation works properly
- [ ] Responsive design on different screen sizes

#### Performance
- [ ] App startup time
- [ ] Memory usage
- [ ] Battery consumption
- [ ] Network efficiency

#### ML/AI Features
- [ ] TensorFlow Lite model loading
- [ ] Speech recognition
- [ ] Speaker identification (if enabled)
- [ ] Audio processing

### Debugging Release Builds

Enable debug logging temporarily:
```kotlin
// In your Application class
if (BuildConfig.DEBUG_MODE) {
    Timber.plant(Timber.DebugTree())
}
```

Check ProGuard mapping:
```bash
# Re-obfuscate stack traces
./gradlew retraceProdRelease \
    app/build/outputs/mapping/prod/release/mapping.txt \
    stacktrace.txt
```

## Troubleshooting

### Common Issues

#### Signing Issues
```
Failed to read key from keystore
```
**Solution**: Verify keystore.properties contains correct credentials and keystore file exists.

#### ProGuard Issues
```
Warning: can't find referenced class
```
**Solution**: Add appropriate keep rules to `proguard-rules.pro`

#### Build Failures
```
Execution failed for task ':app:lintVitalProdRelease'
```
**Solution**: Fix lint issues or temporarily disable lint checks:
```gradle
android {
    lintOptions {
        abortOnError false
    }
}
```

### Getting Help

1. Check build logs for detailed error messages
2. Review ProGuard mapping files for obfuscation issues
3. Test with debug build to isolate signing/obfuscation problems
4. Consult Android Studio's Build Analyzer

## Security Best Practices

### Keystore Management
- Store keystore files in secure, backed-up locations
- Use hardware security modules (HSM) for enterprise deployments
- Implement key rotation policies
- Monitor keystore access logs

### Build Security
- Never commit sensitive credentials to version control
- Use environment variables for CI/CD systems
- Implement code signing verification
- Regularly update dependencies for security patches

### Release Process
- Perform thorough testing before release
- Use staged rollouts for production
- Monitor crash reports and analytics
- Have rollback procedures ready

## Advanced Configuration

### Custom ProGuard Rules

Add custom rules for specific libraries:
```proguard
# Keep custom model classes
-keep class com.voiceledger.ghana.data.model.** { *; }

# Keep third-party SDK classes
-keep class com.thirdparty.sdk.** { *; }
```

### Build Optimization

Enable additional optimizations in `gradle.properties`:
```properties
# Enable R8 full mode
android.enableR8.fullMode=true

# Enable resource shrinking
android.enableResourceOptimizations=true

# Enable build cache
org.gradle.caching=true
```

### CI/CD Integration

For automated builds, use environment variables:
```bash
export RELEASE_KEYSTORE_FILE=$KEYSTORE_PATH
export RELEASE_KEYSTORE_PASSWORD=$KEYSTORE_PASSWORD
export RELEASE_KEY_ALIAS=$KEY_ALIAS
export RELEASE_KEY_PASSWORD=$KEY_PASSWORD
```

## Support

For additional help:
1. Review Android Studio documentation
2. Check Android Developer guides
3. Consult the project's issue tracker
4. Review build logs and error messages

---

**Last Updated**: October 2024
**Version**: 1.0.0
**Maintainer**: Ghana Voice Ledger Development Team