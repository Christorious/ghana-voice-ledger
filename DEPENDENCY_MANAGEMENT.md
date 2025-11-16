# Dependency Management Guide

## Overview

The Ghana Voice Ledger app uses a hybrid approach to dependency management that balances build flexibility with compilation stability. This guide explains how dependencies are organized and how to configure optional features.

## Core Dependencies (Always Included)

These dependencies are required for the app to compile and function properly, regardless of feature flags:

### Analytics & Monitoring
- **Firebase SDK** (BOM 32.7.1): Analytics, Crashlytics, Performance Monitoring, Messaging, Config
- **App Center SDK**: Analytics and crash reporting
- **Timber**: Logging framework

### Security & Encryption
- **AndroidX Security Crypto**: EncryptedSharedPreferences and MasterKey
- **AndroidX Biometric**: Biometric authentication
- **SQLCipher 4.5.4**: Database encryption

### Machine Learning & Audio
- **TensorFlow Lite**: Core ML inference engine
- **TensorFlow Lite Support**: Utilities and metadata
- **TensorFlow Lite GPU**: GPU acceleration
- **JTransforms**: Audio processing and FFT

### Core Android Libraries
- **Jetpack Compose**: UI framework
- **Room**: Database ORM
- **Hilt**: Dependency injection
- **WorkManager**: Background tasks
- **Navigation**: Navigation component
- **Retrofit**: Networking
- **Coroutines**: Asynchronous programming

## Optional Dependencies (Feature-Gated)

These dependencies can be enabled/disabled via build flags:

### WebRTC VAD
- **Dependency**: `org.webrtc:google-webrtc:1.0.32006`
- **Feature Flag**: `feature.webrtc.enabled` (default: false)
- **Fallback**: Custom `VADProcessor` implementation
- **Usage**: Enhanced voice activity detection in noisy environments

### Build Configuration

#### Feature Flags
Feature flags can be set via:
1. **Gradle Properties**: `gradle.properties` file
2. **Command Line**: `-Pfeature.webrtc.enabled=true`
3. **Environment Variables**: `FEATURE_WEBRTC_ENABLED=true`

#### Default Values
```kotlin
// app/build.gradle.kts
val firebaseEnabled = project.findProperty("feature.firebase.enabled")?.toString()?.toBoolean() ?: false
val googleCloudSpeechEnabled = project.findProperty("feature.googleCloudSpeech.enabled")?.toString()?.toBoolean() ?: false
val webRtcEnabled = project.findProperty("feature.webrtc.enabled")?.toString()?.toBoolean() ?: false
```

## Build Variants

### Development (dev)
- All features enabled for testing
- Debug logging enabled
- Test configurations active

### Staging
- Selective feature enablement
- Production-like configuration
- Debug features available

### Production (prod)
- Minimal feature set for stability
- Optimized for performance and size
- Debug features disabled

## Enabling Optional Features

### WebRTC VAD
To enable WebRTC-based Voice Activity Detection:

1. **Via Gradle Properties** (recommended):
   ```properties
   # gradle.properties
   feature.webrtc.enabled=true
   ```

2. **Via Command Line**:
   ```bash
   ./gradlew assembleProdRelease -Pfeature.webrtc.enabled=true
   ```

3. **Via Environment Variable**:
   ```bash
   export FEATURE_WEBRTC_ENABLED=true
   ./scripts/build-release.sh prod
   ```

### Runtime Feature Control

While dependencies are included for compilation, services can be controlled at runtime:

```kotlin
// Example: Conditional service initialization
if (BuildConfig.FEATURE_WEBRTC_ENABLED) {
    // Use WebRTC VAD
    vadManager.useWebRTCVAD()
} else {
    // Use fallback VAD
    vadManager.useCustomVAD()
}
```

## Build Process

### Standard Build
```bash
# Build production release with default configuration
./scripts/build-release.sh prod

# This uses all core dependencies and excludes optional WebRTC
```

### Build with Optional Features
```bash
# Enable WebRTC VAD for enhanced voice detection
./gradlew assembleProdRelease -Pfeature.webrtc.enabled=true
```

### Verification
```bash
# Verify build setup
./scripts/verify-release-setup.sh

# Check Gradle configuration
./gradlew help
```

## Migration Notes

### Previous Approach
- Dependencies were feature-gated, causing compilation failures
- Build scripts required ad-hoc property overrides
- Optional services had missing dependencies

### Current Approach
- Core dependencies always included for compilation stability
- Optional dependencies limited to truly optional features with fallbacks
- Build process works with default configuration
- Runtime feature control provides flexibility

## Troubleshooting

### Compilation Errors
If you encounter compilation errors related to missing dependencies:

1. **Check Core Dependencies**: Ensure all required dependencies are outside feature gates
2. **Verify Import Statements**: Remove imports of gated dependencies from core classes
3. **Review Build Logs**: Look for unresolved references in Gradle output

### Build Failures
If the build fails with feature flag issues:

1. **Default Configuration**: Try building without any custom flags
2. **Clean Build**: Run `./gradlew clean` before building
3. **Verify Scripts**: Ensure build scripts don't override default behavior

### Missing Optional Features
If optional features aren't working:

1. **Check BuildConfig**: Verify `BuildConfig.FEATURE_*` flags are set correctly
2. **Runtime Initialization**: Ensure services check feature flags before use
3. **Dependency Availability**: Confirm optional dependencies are included when needed

## Best Practices

1. **Keep Core Dependencies Minimal**: Only include dependencies that are truly required
2. **Provide Fallbacks**: For optional features, always provide alternative implementations
3. **Runtime Control**: Use build flags for dependency inclusion, runtime flags for feature activation
4. **Documentation**: Clearly document which features are optional and how to enable them
5. **Testing**: Test both with and without optional features enabled

## Future Considerations

1. **Dynamic Feature Modules**: Consider using Android Dynamic Features for truly optional components
2. **R8 Optimization**: Leverage R8 to remove unused code from optional dependencies
3. **Size Analysis**: Monitor APK size impact of included dependencies
4. **Feature Toggles**: Implement a more sophisticated feature toggle system for runtime control