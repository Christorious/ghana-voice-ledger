# Troubleshooting Guide

This guide helps you diagnose and resolve common issues in Ghana Voice Ledger. If you don't find your issue here, check the [FAQ](USER_GUIDE.md#frequently-asked-questions) or contact [support](#getting-help).

---

## Table of Contents

1. [Voice Recognition Issues](#voice-recognition-issues)
2. [Transaction Recording Problems](#transaction-recording-problems)
3. [App Performance & Battery](#app-performance--battery)
4. [Sync & Network Issues](#sync--network-issues)
5. [Permissions & Access](#permissions--access)
6. [Audio & Microphone Problems](#audio--microphone-problems)
7. [Data & Storage Issues](#data--storage-issues)
8. [Developer Issues](#developer-issues)
9. [Getting Help](#getting-help)

---

## Voice Recognition Issues

### Voice Agent Not Starting

**Symptoms:**
- Microphone icon stays gray
- "Failed to start recording" error
- No voice detection

**Causes & Solutions:**

1. **Missing Microphone Permission**
   ```
   Fix: Settings → Apps → Ghana Voice Ledger → Permissions → Enable Microphone
   ```

2. **Another App Using Microphone**
   - Close other apps that use microphone (recording apps, calls)
   - Restart Ghana Voice Ledger

3. **Outside Market Hours**
   - Default market hours: 6 AM - 6 PM
   - Fix: Settings → Voice Recognition → Adjust Market Hours
   - Or tap "Override Market Hours" toggle

4. **Battery Saver Blocking Background Service**
   ```
   Fix: Settings → Battery → Ghana Voice Ledger → Allow background activity
   ```

5. **App Not Initialized**
   - Force close and reopen app
   - If persists, clear app cache: Settings → Apps → Ghana Voice Ledger → Storage → Clear Cache

### Poor Voice Recognition Accuracy

**Symptoms:**
- Wrong words transcribed
- Missing words
- Wrong language detected

**Solutions:**

1. **Background Noise Too High**
   - Reduce competing sounds (radio, TV)
   - Move closer to phone
   - Increase microphone sensitivity: Settings → Voice Recognition → Sensitivity → High

2. **Speaking Too Quietly**
   - Speak at normal conversation volume
   - Test microphone in Settings → Voice Recognition → Test Microphone

3. **Language Mismatch**
   - Ensure selected language matches what you're speaking
   - Settings → Language & Region → Voice Languages
   - Enable multiple languages if you switch between Twi and English

4. **Internet Connection (for cloud recognition)**
   - Cloud recognition requires internet
   - Offline mode uses local models (slightly less accurate)
   - Check: Settings → Voice Recognition → Recognition Provider

5. **Re-train Voice Profile**
   - Settings → Speaker Profiles → My Profile → Record More Samples
   - Record in typical market environment (some background noise)

### Transactions Not Detected Automatically

**Symptoms:**
- Conversations happen but no transactions recorded
- Transactions in "Needs Review" section

**Solutions:**

1. **State Prices Clearly**
   - BAD: "That one... you know the price"
   - GOOD: "That's twenty cedis"
   - The app needs explicit price mentions

2. **Complete the Conversation**
   - State product name: "Two kilos of tilapia"
   - State price: "That's forty cedis"
   - Confirm payment: "Thank you" or "Paid"

3. **Check Confidence Threshold**
   - Settings → Advanced → Confidence Threshold
   - Lower threshold = more auto-recorded transactions (may have errors)
   - Higher threshold = fewer auto-records (higher accuracy)

4. **Review Pending Transactions**
   - Check "Needs Review" tab
   - Low-confidence transactions appear there
   - Manually approve or edit them

---

## Transaction Recording Problems

### Transactions Not Saving

**Symptoms:**
- Transaction appears then disappears
- "Failed to save" error
- Data loss

**Solutions:**

1. **Storage Full**
   ```
   Check: Settings → Storage
   Fix: Delete old photos/videos or move to SD card
   Require: At least 100 MB free space
   ```

2. **Database Error**
   - Backup data first: Settings → Backup & Sync → Export All Data
   - Then: Settings → Advanced → Clear Cache → Restart App
   - If persists: Settings → Advanced → Repair Database

3. **App Crash During Save**
   - Check for update: Settings → About → Check for Updates
   - View crash logs: Settings → Advanced → View Logs → Share with support

### Wrong Information Recorded

**Symptoms:**
- Wrong product name
- Wrong amount
- Wrong customer

**Solutions:**

1. **Edit Transaction**
   - Tap transaction → Edit button
   - Correct the information
   - Save changes

2. **Similar Product Names**
   - App may confuse similar names (e.g., "tilapia" vs "red tilapia")
   - Add distinct product variants: Settings → Products → Add Variant
   - Use full names in conversation

3. **Train Product Vocabulary**
   - Settings → Products → Your Products
   - Add commonly used terms as variants
   - Example: "Abataa" → Tilapia variant

4. **Check Speaker Assignment**
   - If wrong person credited with sale
   - Update speaker profile: tap transaction → Change Speaker

### Duplicate Transactions

**Symptoms:**
- Same transaction recorded twice
- Double counting in totals

**Solutions:**

1. **Delete Duplicate**
   - Long-press transaction → Delete
   - Or tap transaction → Delete button

2. **Prevent Duplicates**
   - Don't repeat the same conversation immediately
   - Wait for transaction to appear before stating another price
   - Enable "Duplicate Detection": Settings → Advanced → Prevent Duplicates

---

## App Performance & Battery

### High Battery Drain

**Symptoms:**
- Battery drains faster than usual
- Phone gets warm
- Battery percentage drops quickly

**Solutions:**

1. **Enable Power Saving Features**
   ```
   Settings → Power Management:
   - ✓ Smart Sleep Mode (pauses when silent)
   - ✓ Low Power Mode (reduces ML processing)
   - ✓ Respect Market Hours (stops outside 6am-6pm)
   ```

2. **Reduce Processing Frequency**
   - Settings → Voice Recognition → Processing Rate → Low
   - Increases chunk processing interval

3. **Disable Unused Features**
   - Disable speaker ID if not needed: Settings → Speaker Profiles → Disable
   - Disable cloud sync: Settings → Sync & Backup → Sync Disabled

4. **Check Battery Stats**
   - Phone Settings → Battery → App Usage
   - If Ghana Voice Ledger > 20%, consider these steps
   - Normal usage: 5-10% per 8-hour workday

### App Running Slowly

**Symptoms:**
- UI lags
- Delayed transaction recording
- Slow screen transitions

**Solutions:**

1. **Clear Cache**
   - Settings → Advanced → Clear Cache
   - Restart app

2. **Optimize Database**
   - Settings → Advanced → Optimize Database
   - Runs vacuum and reindex (may take 1-2 minutes)

3. **Reduce Transaction History**
   - Settings → Data Management → Archive Old Transactions
   - Moves transactions older than selected date to archive

4. **Free Up Storage**
   - Delete unnecessary photos/videos from phone
   - Ghana Voice Ledger performs best with 500+ MB free space

### App Crashes or Freezes

**Symptoms:**
- App closes unexpectedly
- Screen freezes
- "App not responding" dialog

**Solutions:**

1. **Update App**
   - Play Store → Ghana Voice Ledger → Update
   - Or Settings → About → Check for Updates

2. **Clear App Data** (⚠️ backs up first!)
   ```
   1. Settings → Backup & Sync → Export All Data
   2. Phone Settings → Apps → Ghana Voice Ledger
   3. Storage → Clear Data
   4. Reopen app and restore data
   ```

3. **Reinstall App**
   - Backup data first!
   - Uninstall Ghana Voice Ledger
   - Reinstall from Play Store
   - Restore backup

4. **Report Crash**
   - Settings → Help & Feedback → Report Issue
   - Include: What you were doing when it crashed
   - Crash logs sent automatically if enabled

---

## Sync & Network Issues

### Data Not Syncing to Cloud

**Symptoms:**
- Sync icon shows pending
- "Sync failed" notification
- Cloud backup not updating

**Solutions:**

1. **Check Internet Connection**
   - Ensure Wi-Fi or mobile data is on
   - Test: Open browser, visit website
   - Ghana Voice Ledger syncs on Wi-Fi by default

2. **Enable Mobile Data Sync**
   - Settings → Sync & Backup → Sync on Mobile Data → Enable
   - Note: Uses data (approx 1-5 MB per sync)

3. **Manual Sync**
   - Settings → Sync & Backup → Sync Now
   - Watch for success/failure message

4. **Check Sync Settings**
   - Settings → Sync & Backup → Auto-Sync → Enabled
   - Sync Frequency → 15 minutes (or preferred interval)

5. **Login Status**
   - Settings → Account → Check if logged in
   - May need to re-authenticate

### Offline Mode Not Working

**Symptoms:**
- App requires internet to function
- "Network unavailable" errors

**Solutions:**

1. **Enable Offline Mode**
   - Settings → Offline Mode → Enable
   - Ensures offline ML models are downloaded

2. **Download Offline Models**
   - Settings → Voice Recognition → Download Offline Models
   - Requires one-time internet connection
   - Size: ~50 MB

3. **Verify Offline Features**
   - All core features work offline except cloud sync
   - Test by enabling airplane mode

### Pending Operations Queue Growing

**Symptoms:**
- Many operations pending sync
- Sync queue count increasing
- Storage filling up

**Solutions:**

1. **Connect to Wi-Fi**
   - App syncs faster on Wi-Fi
   - Let it run for 5-10 minutes connected

2. **Clear Failed Operations**
   - Settings → Sync & Backup → Failed Operations → Clear All
   - Only use if operations are truly not needed

3. **Retry Failed Syncs**
   - Settings → Sync & Backup → Failed Operations → Retry All

---

## Permissions & Access

### Missing Permissions Error

**Symptoms:**
- "Permission denied" messages
- Features not working
- Red permission warnings

**Solutions:**

1. **Grant All Permissions**
   ```
   Phone Settings → Apps → Ghana Voice Ledger → Permissions:
   - Microphone: Allow
   - Storage: Allow
   - Notifications: Allow (optional)
   ```

2. **Special Permissions**
   - Battery Optimization: Exclude Ghana Voice Ledger
   - Background Activity: Allow
   - Overlay/Draw Over Apps: Allow (for floating controls)

3. **Re-request Permissions**
   - Settings → Permissions → Request All Permissions

### Biometric Lock Not Working

**Symptoms:**
- Fingerprint not recognized
- Face unlock fails
- PIN/Pattern not accepted

**Solutions:**

1. **Re-setup Biometric**
   - Settings → Privacy & Security → App Lock → Biometric Settings
   - Re-enroll fingerprint/face

2. **Use Backup PIN**
   - If biometric fails, use backup PIN
   - Settings → Privacy & Security → App Lock → Change PIN

3. **Disable Temporarily**
   - Settings → Privacy & Security → App Lock → Disable
   - Re-enable after troubleshooting

---

## Audio & Microphone Problems

### No Audio Being Captured

**Symptoms:**
- VAD score always 0
- No speech detected
- Silence in recordings

**Solutions:**

1. **Test Microphone**
   - Phone Settings → Sound → Record test audio
   - If phone mic works but app doesn't → app issue
   - If phone mic doesn't work → hardware issue

2. **Check Mic Permission**
   - Must allow "while using app" or "all the time"
   - Settings → Apps → Ghana Voice Ledger → Permissions → Microphone

3. **Bluetooth Microphone**
   - If using Bluetooth headset, ensure paired and connected
   - App may default to wrong audio input
   - Settings → Voice Recognition → Audio Source → Phone Mic / Bluetooth

4. **Restart Audio Service**
   - Stop voice agent
   - Force close app
   - Reopen and start voice agent

### Poor Audio Quality

**Symptoms:**
- Distorted sound
- Crackling/popping
- Low volume

**Solutions:**

1. **Clean Microphone**
   - Dust or debris may block mic holes
   - Gently clean with soft brush

2. **Adjust Sensitivity**
   - Settings → Voice Recognition → Microphone Sensitivity
   - Try different levels (Low/Medium/High)

3. **Disable Audio Enhancements**
   - Some phones have audio effects that interfere
   - Phone Settings → Sound → Disable effects/equalizer

---

## Data & Storage Issues

### Database Corruption

**Symptoms:**
- "Database error" messages
- Missing transactions
- App crashes on launch

**Solutions:**

1. **Automatic Repair**
   - App may auto-repair on next launch
   - Wait for "Repairing database..." message to complete

2. **Manual Repair**
   - Settings → Advanced → Repair Database
   - May take several minutes

3. **Restore from Backup**
   - Settings → Backup & Sync → Restore from Backup
   - Select most recent backup

4. **Last Resort: Fresh Start**
   - Export whatever data is accessible
   - Clear app data
   - Restore from backup or start fresh

### Running Out of Storage

**Symptoms:**
- "Storage full" warnings
- Can't record new transactions
- App sluggish

**Solutions:**

1. **Archive Old Transactions**
   - Settings → Data Management → Archive Transactions
   - Select cutoff date (e.g., transactions older than 6 months)

2. **Delete Audio Metadata**
   - Settings → Data Management → Audio Metadata → Delete Old
   - Audio metadata auto-deletes after 30 days by default

3. **Export and Clear**
   - Export transactions: Settings → Export Data
   - Clear old data: Settings → Data Management → Clear Old Transactions

---

## Developer Issues

### Build Failures

**Symptoms:**
- Gradle build errors
- Compilation failures
- APK not generating

**Solutions:**

1. **Clean Build**
   ```bash
   ./gradlew clean
   ./gradlew build --refresh-dependencies
   ```

2. **Check `local.properties`**
   - Ensure `sdk.dir` points to valid Android SDK
   - Ensure all required API keys present
   - No typos in property names

3. **Gradle Sync**
   - Android Studio → File → Sync Project with Gradle Files
   - Invalidate Caches: File → Invalidate Caches / Restart

4. **Dependencies**
   ```bash
   ./gradlew dependencies --refresh-dependencies
   ```

### Missing API Keys Error

**Symptoms:**
- Build succeeds but runtime errors
- "API key not configured" messages
- Cloud features not working

**Solutions:**

1. **Verify `local.properties`**
   ```properties
   GOOGLE_CLOUD_API_KEY=your_key_here
   FIREBASE_API_KEY=your_key_here
   ```

2. **Check BuildConfig**
   - Keys should be accessible as `BuildConfig.GOOGLE_CLOUD_API_KEY`
   - Rebuild after editing `local.properties`

3. **Environment Variables**
   - If using `.env`, ensure loaded correctly
   - Check `build.gradle.kts` for env variable reading logic

### Tests Failing

**Symptoms:**
- Unit tests fail
- Instrumentation tests crash
- Test APK won't install

**Solutions:**

1. **Run Specific Test**
   ```bash
   ./gradlew test --tests "TransactionStateMachineTest.testStateTransition"
   ```

2. **Check Test Database**
   - Tests use in-memory database
   - Ensure migrations applied correctly

3. **Mock Data Issues**
   - Verify test fixtures in `test/resources`
   - Check mock audio files exist

4. **Connected Tests**
   ```bash
   # Ensure emulator/device connected
   adb devices
   ./gradlew connectedDebugAndroidTest
   ```

---

## Getting Help

### Self-Service Resources

1. **In-App Help**
   - Settings → Help & Feedback
   - Contextual help (? icons throughout app)

2. **Documentation**
   - [User Guide](USER_GUIDE.md)
   - [Developer Guide](DEVELOPER_GUIDE.md)
   - [API Documentation](API.md)

3. **Community Forum**
   - [community.voiceledger.com](https://community.voiceledger.com)
   - Search existing topics or post question

### Contact Support

**For Users:**
- **Email**: support@voiceledger.com
- **WhatsApp**: +233 XX XXX XXXX
- **Response Time**: 24-48 hours

**For Developers:**
- **Email**: dev-support@voiceledger.com
- **Slack**: `#ghana-voice-ledger-dev`
- **GitHub Issues**: [github.com/voiceledger/issues](https://github.com/voiceledger/ghana-voice-ledger/issues)

### Reporting Bugs

Please include:
1. **Device Info**: Phone model, Android version
2. **App Version**: Settings → About → Version
3. **Steps to Reproduce**: What you did before the issue
4. **Expected vs Actual**: What should happen vs what happened
5. **Logs** (if possible): Settings → Advanced → Export Logs

**Template:**
```
Device: Samsung Galaxy A53, Android 13
App Version: 1.0.0
Issue: Voice agent won't start
Steps:
1. Open app
2. Tap microphone icon
3. Error: "Failed to start recording"
Expected: Voice agent starts listening
Actual: Error message appears
```

---

## Advanced Troubleshooting

### Enable Debug Mode

For developers:

1. Settings → About → Tap version number 7 times
2. Developer Options appear
3. Enable Debug Mode
4. Restart app

Debug Mode shows:
- ML confidence scores
- State machine transitions
- Network request/response
- Performance metrics

### Logcat Monitoring

Connect device and run:

```bash
adb logcat | grep -E "VoiceAgent|Transaction|VAD|Speaker"
```

Key tags:
- `VoiceAgentService` - Service lifecycle
- `TransactionStateMachine` - State transitions
- `VADManager` - Voice activity detection
- `SpeakerIdentifier` - Speaker recognition
- `OfflineQueue` - Sync operations

### Database Inspection

1. Android Studio → View → Tool Windows → App Inspection
2. Database Inspector → Select `voice_ledger.db`
3. Query tables:
   - `transactions` - All transaction records
   - `audio_metadata` - Processing metadata
   - `offline_operations` - Pending sync operations

---

## Common Error Messages

| Error | Meaning | Solution |
|-------|---------|----------|
| "Microphone permission denied" | App lacks microphone access | Grant permission in Settings |
| "Network unavailable" | No internet + offline mode disabled | Enable offline mode or connect to internet |
| "Database error: SQLITE_CORRUPT" | Database file damaged | Repair database or restore backup |
| "Failed to load ML model" | TensorFlow model file missing/corrupt | Re-download: Settings → Voice → Download Models |
| "Timeout: Transaction context expired" | Conversation took too long | Complete transaction or start new one |
| "Speaker ID confidence too low" | Can't identify speaker | Re-record voice profile |
| "Sync failed: 401 Unauthorized" | Authentication expired | Sign out and sign back in |
| "Insufficient storage" | Device storage full | Free up space (need 100+ MB) |

---

**Still stuck? Contact support with the details above, and we'll help you resolve the issue!**
