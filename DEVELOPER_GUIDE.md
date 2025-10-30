# Developer Guide (Quick Start)

This is the high-level onboarding checklist for engineers working on Ghana Voice Ledger. For comprehensive documentation, see [docs/DEVELOPER_GUIDE.md](docs/DEVELOPER_GUIDE.md).

---

## 1. Onboarding Checklist

1. **Clone & Checkout**
   ```bash
   git clone https://github.com/voiceledger/ghana-voice-ledger.git
   cd ghana-voice-ledger
   git checkout <assigned-branch>
   ```
2. **Configure locals**
   ```bash
   cp local.properties.example local.properties
   cp .env.example .env
   ```
3. **Install prerequisites**
   - Android Studio Hedgehog (2023.1.1)+
   - Android SDK 34 & build tools 34.0.0
   - Kotlin 1.9.x toolchain
   - Java 17
4. **Open project** in Android Studio and let Gradle sync
5. **Run unit tests** to verify setup
   ```bash
   ./gradlew test
   ```

---

## 2. Secrets & Configuration

### `local.properties`

Populate at minimum:
```properties
sdk.dir=/Users/<you>/Library/Android/sdk
GOOGLE_CLOUD_API_KEY=<speech_api_key>
FIREBASE_PROJECT_ID=<firebase_project_id>
ENCRYPTION_KEY=<32_char_key>
DB_ENCRYPTION_KEY=<db_key>
BASE_URL=https://api-dev.voiceledger.com/
```

### `.env`

Controls feature toggles and build flags:
```dotenv
BUILD_TYPE=debug
ENABLE_OFFLINE_MODE=true
ENABLE_MULTI_LANGUAGE=true
ENABLE_SPEAKER_IDENTIFICATION=true
ENABLE_VOICE_COMMANDS=true
ENABLE_DAILY_SUMMARIES=true
```

> ⚠️ Do **not** commit either file. Secrets should remain local or injected via CI secrets.

---

## 3. Core Gradle Commands

```bash
# Clean & build
./gradlew clean build

# Assemble & install debug APK
./gradlew assembleDebug
./gradlew installDebug

# Run unit tests
./gradlew testDebugUnitTest

# Run instrumentation tests (requires device/emulator)
./gradlew connectedAndroidTest

# Static checks (if enabled)
./gradlew lintDebug ktlintCheck detekt
```

Use the Gradle wrapper from project root. Avoid running Gradle as root; Android Studio will automatically pick up the wrapper.

---

## 4. Project Hotspots

| Area | Location | Notes |
|------|----------|-------|
| Voice agent service | `app/src/main/java/.../service/VoiceAgentService.kt` | Foreground audio pipeline |
| Transaction state machine | `app/src/main/java/.../ml/transaction/TransactionStateMachine.kt` | Conversation flow |
| Transaction processor | `app/src/main/java/.../ml/transaction/TransactionProcessor.kt` | Orchestrates ML components |
| Offline queue | `app/src/main/java/.../offline/OfflineQueueManager.kt` | WorkManager integration |
| Compose UI | `app/src/main/java/.../presentation` | Material 3 components |

---

## 5. Feature Toggles

Set via `.env`, `local.properties`, or runtime config:

| Flag | Default | Description |
|------|---------|-------------|
| `ENABLE_OFFLINE_MODE` | true | Activates offline queue + sync workers |
| `ENABLE_MULTI_LANGUAGE` | true | Loads multilingual speech models |
| `ENABLE_SPEAKER_IDENTIFICATION` | true | Enables TensorFlow Lite speaker ID |
| `ENABLE_VOICE_COMMANDS` | true | Enables command processing flow |
| `ENABLE_DAILY_SUMMARIES` | true | Generates nightly sales summaries |
| `ENABLE_ANALYTICS` | false | Sends analytics events (App Center/Firebase) |
| `ENABLE_MOCK_DATA` | true (debug) | Seeds sample data for demos |

Use `FeatureToggle` helpers in code to check flags at runtime.

---

## 6. Running the Voice Agent

1. Install debug build on physical device (preferred for microphone access)
2. Launch app → tap the microphone icon
3. Ensure these conditions:
   - Microphone permission granted
   - Market hours window (default 6 AM – 6 PM)
   - Background activity allowed in Android settings

Watch `adb logcat | grep VoiceAgent` for streaming diagnostics.

---

## 7. Testing Voice & Offline Flows

**Simulate offline:**
```bash
adb shell svc wifi disable
adb shell svc data disable
```
Record transactions; verify `OfflineQueueManager.queueState`. Re-enable network and confirm auto-sync (`svc wifi enable`).

**Inspect state machine:** enable debug logging or attach debugger to `TransactionStateMachine.processUtterance`.

---

## 8. Useful Links

- Full Developer Manual: [docs/DEVELOPER_GUIDE.md](docs/DEVELOPER_GUIDE.md)
- API Reference: [docs/API.md](docs/API.md)
- Voice Agent KDoc: `VoiceAgentService.kt`
- State Machine KDoc: `TransactionStateMachine.kt`
- Troubleshooting: [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md)

---

Need help? Ping `dev-support@voiceledger.com` or join Slack `#ghana-voice-ledger-dev`.
