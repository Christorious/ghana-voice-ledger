# Ghana Voice Ledger - Developer Guide

Welcome, engineers! This guide walks you through everything you need to build, test, and ship features for Ghana Voice Ledger. It supplements the project README with deeper detail on architecture, environment configuration, tooling, and common workflows.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture](#architecture)
3. [Development Environment](#development-environment)
4. [Configuration & Secrets](#configuration--secrets)
5. [Build & Run](#build--run)
6. [Testing Strategy](#testing-strategy)
7. [Voice Agent Pipeline](#voice-agent-pipeline)
8. [Feature Toggles](#feature-toggles)
9. [Offline & Sync Testing](#offline--sync-testing)
10. [Debugging Toolkit](#debugging-toolkit)
11. [Release Process](#release-process)
12. [Developer Resources](#developer-resources)

---

## Project Overview

- **Platform**: Android (Kotlin, Jetpack Compose)
- **Architecture**: Clean Architecture + MVVM
- **Core Modules**:
  - `app/`: Single Gradle module containing data, domain, and presentation layers
  - `ml/`: Machine learning components (speech, speaker ID, transaction parsing)
  - `service/`: Voice agent background services and managers
  - `offline/`: Offline queue, WorkManager integrations

Key technologies: Hilt DI, Room, WorkManager, Coroutines/Flow, TensorFlow Lite, App Center.

---

## Architecture

### Layered Structure

```
Presentation (Compose, ViewModels)
   │
Domain (Use cases, repositories, models)
   │
Data (Room DAOs, repository implementations)
   │
ML & Services (TensorFlow Lite, Audio processing)
```

### Important Components

- **VoiceAgentService**: Foreground service handling continuous audio capture
- **TransactionProcessor**: Coordinates state machine, vocabulary enhancement, persistence
- **TransactionStateMachine**: Conversation flow model for sales lifecycle
- **OfflineQueueManager**: Queues network operations when offline
- **SpeakerIdentifier**: TensorFlow Lite speaker classification

### Data Flow

```
Audio → VAD → Speaker ID → Speech Recognition → TransactionStateMachine → TransactionRepository → UI
```

---

## Development Environment

### Prerequisites

| Tool | Version |
|------|---------|
| Android Studio | Hedgehog (2023.1.1) or later |
| Android SDK | API 34 |
| Kotlin | 1.9.0+ |
| Java | 17 |
| Gradle | Wrapper included |

Optional: Docker, App Center CLI, Firebase CLI.

### Initial Setup

1. Clone repo and checkout desired branch
2. Copy config templates:
   ```bash
   cp local.properties.example local.properties
   cp .env.example .env
   ```
3. Fill in secrets (see next section)
4. Open project in Android Studio or use command line

---

## Configuration & Secrets

### `local.properties`

Required for Android builds:

```properties
sdk.dir=/Users/<you>/Library/Android/sdk
GOOGLE_CLOUD_API_KEY=<speech_api_key>
FIREBASE_PROJECT_ID=<firebase_project_id>
FIREBASE_API_KEY=<firebase_web_api_key>
ENCRYPTION_KEY=<32_char_key>
DB_ENCRYPTION_KEY=<database_key>
BASE_URL=https://api-dev.voiceledger.com/
OFFLINE_MODE_ENABLED=true
SPEAKER_IDENTIFICATION_ENABLED=true
```

> ⚠️ Never commit `local.properties`. Add it to `.gitignore` (already configured).

### `.env`

Environment variables for Gradle, CI tasks, and backend integrations:

```dotenv
BUILD_TYPE=debug
GOOGLE_CLOUD_SPEECH_API_KEY=<same_as_above>
FIREBASE_PROJECT_ID_DEV=<project>
ENABLE_ANALYTICS=false
ENABLE_CRASHLYTICS=true
ENABLE_OFFLINE_MODE=true
ENABLE_MULTI_LANGUAGE=true
ENABLE_SPEAKER_IDENTIFICATION=true
```

Access variables from Gradle or runtime via `BuildConfig` or custom helpers.

### Secrets Management Tips

- Use Android Studio **Gradle Properties** for local dev secrets
- In CI, inject secrets via pipeline variables (GitHub Actions → repository secrets)
- For runtime use, prefer encrypted SharedPreferences (see `SecureDataStorage`)

---

## Build & Run

### Command Line

```bash
# Clean build
./gradlew clean build

# Assemble debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run unit tests
./gradlew testDebugUnitTest

# Run instrumentation tests
./gradlew connectedAndroidTest
```

Use Gradle property flags for faster builds:

```bash
./gradlew assembleDebug -PfastBuild=true -PskipLint=true
```

### Android Studio

1. Select `app` configuration
2. Choose target device (emulator or physical)
3. Click **Run** ▶️
4. For instrumentation tests, select `Android Instrumented Tests`

---

## Testing Strategy

| Test Type | Location | Command | Notes |
|-----------|----------|---------|-------|
| Unit | `app/src/test` | `./gradlew test` | Kotlin/JUnit tests |
| Instrumentation | `app/src/androidTest` | `./gradlew connectedAndroidTest` | Requires emulator/device |
| UI (Compose) | `app/src/androidTest` | `./gradlew connectedDebugAndroidTest` | Uses Espresso/Compose testing |
| ML | `app/src/test` | `./gradlew test` | Mocks audio inputs |

### Suggested Workflow

1. Run unit tests on every change
2. Run instrumentation tests before PR merge
3. Use `SmokeTestSuite` (if available) for regression before release

---

## Voice Agent Pipeline

### Quick Overview

| Stage | Class | Notes |
|-------|-------|-------|
| Audio Capture | `VoiceAgentService` | 16kHz mono PCM using `AudioRecord` |
| Voice Activity Detection | `VADManager` | Custom + WebRTC VAD options |
| Speaker Identification | `SpeakerIdentifier` | TensorFlow Lite embedding matching |
| Speech Recognition | `SpeechRecognitionManager` | Google Cloud + offline TFLite |
| Transaction Parsing | `TransactionProcessor` | State machine + vocab enhancement |
| Persistence | `TransactionRepository` | Room database |

### Tuning Parameters

- `SILENCE_THRESHOLD` (VoiceAgentService) — adjust for noisy environments
- `MIN_CONFIDENCE_THRESHOLD` (TransactionStateMachine) — controls auto-save threshold
- `.env` flags (below) — enable/disable ML features

---

## Feature Toggles

Feature flags can be configured via `.env`, `local.properties`, or remote config (future).

| Flag | Default | Description |
|------|---------|-------------|
| `ENABLE_OFFLINE_MODE` | true | Enables offline queue & sync manager |
| `ENABLE_MULTI_LANGUAGE` | true | Loads multilingual speech models |
| `ENABLE_SPEAKER_IDENTIFICATION` | true | Activates TensorFlow Lite speaker ID |
| `ENABLE_VOICE_COMMANDS` | true | Enables command parsing in `VoiceCommandProcessor` |
| `ENABLE_DAILY_SUMMARIES` | true | Generates nightly summaries |
| `ENABLE_ANALYTICS` | false | Sends analytics events (App Center/Firebase) |
| `ENABLE_MOCK_DATA` | true (dev) | Seeds sample data for quick testing |

To override in code, use `FeatureToggle` helpers (see `domain/featureflags`).

---

## Offline & Sync Testing

### Simulating Offline Mode

```bash
adb shell svc wifi disable
adb shell svc data disable
```

Then:
1. Use the app to record transactions
2. Observe `OfflineQueueManager.queueState`
3. Re-enable network (`svc wifi enable`, `svc data enable`)
4. Verify queued operations sync automatically

### Inspecting Queue State

Use `QueueDebugViewModel` (if implemented) or log statements:

```kotlin
offlineQueueManager.queueState.collect { state ->
    Log.d("OfflineQueue", "Pending=${state.pendingOperations}")
}
```

You can also query Room DB tables (`offline_operations`) via Android Studio Database Inspector.

---

## Debugging Toolkit

### Useful Gradle Tasks

```bash
./gradlew lintDebug            # Static analysis
./gradlew detekt               # Kotlin lint (if configured)
./gradlew ktlintCheck          # Code style
./gradlew dependencyUpdates    # Report outdated dependencies
```

### Runtime Debugging

- **VoiceAgentService logs**: `adb logcat | grep VoiceAgent`
- **State machine transitions**: `TAG = TransactionStateMachine`
- **Audio metadata**: inspect `AudioMetadata` table
- **Speaker identification**: logs under `TensorFlowLiteSpeakerIdentifier`

### Visual Debugging Tools

- **Debug Panel** (if developer UI enabled): Settings → Developer Options → Enable Debug Panel
- **Transaction Timeline**: Shows state transitions and confidences for last conversation
- **ML Stats Overlay**: Displayed when `ENABLE_PERFORMANCE_LOGGING` flag is true

### Common Pitfalls

- Missing microphone permission → service enters `ListeningState.ERROR`
- Forgetting to initialize `VADManager` → no speech detection
- Low confidence utterances stuck in `Needs Review` tab → adjust threshold

---

## Release Process

1. **Version Bump**
   - Update `app/build.gradle.kts` `versionCode` & `versionName`
2. **Changelog**
   - Update `CHANGELOG.md` (if present) or release notes
3. **Run QA Checklist**
   - `./gradlew clean assembleRelease lintRelease test connectedAndroidTest`
4. **App Signing**
   - Ensure release keystore credentials in `gradle.properties`
5. **Upload to Play Console**
   - Use `./gradlew bundleRelease` for AAB
6. **App Center**
   - Optional: upload to App Center for internal distribution
7. **Tag Release**
   - `git tag v<X.Y.Z>` & push tag

### Beta Builds

- Use `beta` flavor if configured (check `build.gradle.kts`)
- Feature flags can target beta testers (`BETA_FEATURES_ENABLED=true`)

---

## Developer Resources

- **Design System**: `/app/src/main/res` (Material 3 theme, typography, components)
- **API Schemas**: `docs/API.md`
- **User Guide**: `docs/USER_GUIDE.md`
- **Troubleshooting**: `docs/TROUBLESHOOTING.md`
- **Beta Testing**: `docs/BETA_TESTING_GUIDE.md`
- **Support Docs**: `docs/CUSTOMER_SUPPORT.md`

### Contact & Support

- Tech Leads: tech-leads@voiceledger.com
- ML Team: ml@voiceledger.com
- Mobile Guild: mobile@voiceledger.com
- Slack: `#ghana-voice-ledger-dev`

### Onboarding Checklist

- [ ] Clone repo & setup environment
- [ ] Configure `local.properties` & `.env`
- [ ] Run unit tests (`./gradlew test`)
- [ ] Run app on emulator/device
- [ ] Review architecture docs (`docs/API.md`)
- [ ] Read KDoc on `TransactionStateMachine` & `VoiceAgentService`
- [ ] Join Slack channels + App Center organization

Welcome aboard! 🚀
