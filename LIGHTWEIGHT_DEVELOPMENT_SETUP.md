# Lightweight Android Development Setup

## 🎯 VS Code + Command Line Development

### Required Extensions for VS Code:
```bash
# Install these VS Code extensions:
- Kotlin Language
- Android iOS Emulator
- Gradle for Java
- Extension Pack for Java
- Android Debug Bridge (ADB)
```

### Setup Steps:

#### 1. Install Java Development Kit (JDK 17)
```bash
# Download from: https://adoptium.net/temurin/releases/
# Or use package manager:
winget install EclipseAdoptium.Temurin.17.JDK
```

#### 2. Install Android Command Line Tools Only
```bash
# Download SDK Command Line Tools (much smaller than full Android Studio)
# From: https://developer.android.com/studio#command-tools
# Extract to: C:\Android\cmdline-tools\latest\
```

#### 3. Set Environment Variables
```bash
# Add to your system PATH:
ANDROID_HOME=C:\Android
JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.x-hotspot
PATH=%PATH%;%ANDROID_HOME%\cmdline-tools\latest\bin;%ANDROID_HOME%\platform-tools;%JAVA_HOME%\bin
```

#### 4. Install Required SDK Components
```bash
# Run these commands in PowerShell:
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
sdkmanager "system-images;android-34;google_apis;x86_64"
```

## 🔧 Alternative Development Environments

### Option 2: IntelliJ IDEA Community Edition
- **Size:** ~800MB (much smaller than Android Studio)
- **Features:** Full Kotlin support, Gradle integration
- **Download:** https://www.jetbrains.com/idea/download/

### Option 3: Cloud-Based Development

#### GitHub Codespaces
```bash
# Create .devcontainer/devcontainer.json:
{
  "name": "Android Development",
  "image": "mcr.microsoft.com/devcontainers/java:17",
  "features": {
    "ghcr.io/devcontainers/features/android-sdk:1": {}
  }
}
```

#### Gitpod
```yaml
# Create .gitpod.yml:
image: gitpod/workspace-android
tasks:
  - init: ./gradlew build
```

### Option 4: Docker Development Environment
```dockerfile
# Use the provided Dockerfile in your project
docker build -t ghana-voice-ledger .
docker run -it -v ${PWD}:/workspace ghana-voice-ledger
```

## 📱 Building and Testing

### Command Line Build Commands:
```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run lint checks
./gradlew lint

# Generate release APK
./gradlew assembleRelease
```

### Testing on Device:
```bash
# Install ADB (Android Debug Bridge)
# Enable USB Debugging on your Android device
adb devices
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 🌐 Online Development Platforms

### 1. Replit
- Create Android project online
- No local installation required
- Limited but functional

### 2. CodeSandbox
- Web-based development
- Good for smaller projects

### 3. Gitiles/GitLab CI/CD
- Use CI/CD pipelines to build
- Download APKs from artifacts

## 💡 Minimal Local Setup

### What You Actually Need:
1. **JDK 17** (~200MB)
2. **Android SDK Command Line Tools** (~100MB)
3. **Platform Tools** (~10MB)
4. **Build Tools** (~50MB)
5. **Android Platform API 34** (~50MB)

**Total:** ~400MB instead of 4-8GB!

### VS Code Workspace Settings:
```json
{
  "java.home": "C:\\Program Files\\Eclipse Adoptium\\jdk-17.0.x-hotspot",
  "android.home": "C:\\Android",
  "gradle.nestedProjects": true,
  "kotlin.languageServer.enabled": true
}
```

## 🚀 Quick Start Commands

### 1. Setup Android SDK:
```bash
# Create local.properties with your paths
echo "sdk.dir=C:\\Android" > local.properties
```

### 2. Download Gradle Wrapper:
```bash
# Download gradle-wrapper.jar manually or use:
gradle wrapper --gradle-version 8.4
```

### 3. Build Project:
```bash
./gradlew build
```

### 4. Install on Device:
```bash
./gradlew installDebug
```

## 📋 Development Workflow

1. **Code in VS Code** with Kotlin extensions
2. **Build via command line** using Gradle
3. **Test on real device** via ADB
4. **Debug using logs** and VS Code debugger
5. **Use browser-based emulators** for testing

## 🔍 Debugging Options

### 1. Chrome DevTools (for web debugging)
### 2. ADB Logcat for Android logs:
```bash
adb logcat | grep "VoiceLedger"
```

### 3. VS Code Debugger with proper configuration

## 📦 Deployment

### GitHub Actions (Automated):
- Push code to GitHub
- Actions build APK automatically
- Download from releases

### Manual Build:
```bash
./gradlew assembleRelease
# APK location: app/build/outputs/apk/release/
```