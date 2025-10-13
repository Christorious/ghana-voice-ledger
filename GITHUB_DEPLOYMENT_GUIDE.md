# 🚀 GitHub Deployment Guide - Ghana Voice Ledger

## Quick Start (5 Minutes to APK!)

### Step 1: Push to GitHub
```bash
# Run the setup script:
.\setup-git-and-push.ps1

# Or manually:
git init
git add .
git commit -m "Initial commit: Ghana Voice Ledger Android App"
git remote add origin https://github.com/yourusername/ghana-voice-ledger.git
git push -u origin main
```

### Step 2: Watch GitHub Actions Build
1. Go to: `https://github.com/yourusername/ghana-voice-ledger/actions`
2. Click on the latest workflow run
3. Watch the build progress (takes ~10-15 minutes)

### Step 3: Download Your APK
1. When build completes, scroll down to "Artifacts"
2. Download `debug-apk.zip`
3. Extract to get `app-ghana-debug.apk`

### Step 4: Install on Android Device
1. Enable "Unknown Sources" in Android settings
2. Transfer APK to your phone
3. Install and test!

## 🔄 What GitHub Actions Does Automatically

### ✅ **Build Process**
- Sets up Android SDK and Java 17
- Downloads all dependencies
- Compiles 142+ Kotlin files
- Runs unit tests
- Performs security scans
- Builds debug APK
- Creates downloadable artifacts

### ✅ **Quality Checks**
- Code compilation verification
- Unit test execution
- Lint analysis
- Security vulnerability scanning
- Performance checks

### ✅ **Multiple Build Types**
- **Debug APK**: For development and testing
- **Staging APK**: For pre-production testing (on develop branch)
- **Release APK**: For production (on main branch)
- **App Bundle (AAB)**: For Google Play Store

## 📱 **APK Download Locations**

### From GitHub Actions:
1. **Repository** → **Actions** tab
2. Click latest workflow run
3. Scroll to **Artifacts** section
4. Download:
   - `debug-apk` - For testing
   - `release-apk` - For distribution
   - `release-aab` - For Play Store

### Direct Links (after build):
- Debug APK: `https://github.com/yourusername/ghana-voice-ledger/actions`
- Release builds: Available on `main` branch pushes

## 🧪 **Testing Your APK**

### Installation Steps:
1. **Enable Developer Options** on Android:
   - Settings → About Phone → Tap "Build Number" 7 times
   
2. **Enable Unknown Sources**:
   - Settings → Security → Unknown Sources → Enable
   
3. **Install APK**:
   - Transfer APK file to phone
   - Open file manager, tap APK
   - Follow installation prompts

### Testing Checklist:
- [ ] App launches successfully
- [ ] Main dashboard loads
- [ ] Navigation works between screens
- [ ] Voice recording permission requested
- [ ] Settings screen accessible
- [ ] App doesn't crash on basic interactions

## 🔧 **Troubleshooting**

### Build Fails?
1. **Check Actions Log**:
   - Go to Actions tab
   - Click failed build
   - Expand failed step to see error

2. **Common Issues**:
   - **Gradle sync failed**: Usually resolves on retry
   - **Test failures**: Check test logs in artifacts
   - **Lint errors**: Download lint report from artifacts

3. **Fix and Retry**:
   ```bash
   # Make fixes, then:
   git add .
   git commit -m "Fix build issues"
   git push
   ```

### APK Won't Install?
1. **Check Android Version**: Minimum Android 7.0 (API 24)
2. **Enable Unknown Sources**: Required for non-Play Store apps
3. **Clear Storage**: If updating, clear app data first
4. **Check Permissions**: Ensure file access permissions

### App Crashes?
1. **Check Logs**: Use `adb logcat` if device connected
2. **Permissions**: Grant microphone permission when prompted
3. **Storage**: Ensure device has sufficient storage
4. **Compatibility**: Test on different Android versions

## 🚀 **Advanced Features**

### Automatic Deployment:
- **Internal Testing**: Pushes to `main` auto-deploy to Play Store internal track
- **Production**: GitHub releases auto-deploy to Play Store production

### Branch Strategy:
- `main`: Production releases
- `develop`: Staging builds
- `feature/*`: Development branches

### Secrets Configuration:
Add these to GitHub repository secrets for advanced features:
- `KEYSTORE_BASE64`: Base64 encoded signing keystore
- `KEYSTORE_PASSWORD`: Keystore password
- `KEY_ALIAS`: Signing key alias
- `KEY_PASSWORD`: Signing key password
- `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`: Play Store deployment

## 📊 **Build Status**

### Check Build Status:
- **Badge**: Add to README: `![Build Status](https://github.com/yourusername/ghana-voice-ledger/workflows/CI%2FCD%20Pipeline/badge.svg)`
- **Notifications**: Enable GitHub notifications for build results
- **Slack**: Configure Slack notifications (optional)

### Build Artifacts:
- **Debug APK**: ~15-25 MB
- **Release APK**: ~10-20 MB (optimized)
- **App Bundle**: ~8-15 MB (Play Store format)
- **Test Reports**: HTML format
- **Lint Reports**: Code quality analysis

## 🎯 **Next Steps After First Build**

1. **Test Core Features**:
   - Voice recording
   - Transaction entry
   - Data persistence
   - Offline functionality

2. **Gather Feedback**:
   - Install on multiple devices
   - Test with different Android versions
   - Document any issues

3. **Iterate**:
   - Fix bugs found during testing
   - Push updates to GitHub
   - Download new builds automatically

4. **Prepare for Distribution**:
   - Set up signing keys for release builds
   - Configure Play Store deployment
   - Create app store listings

## 🏆 **Success Metrics**

Your deployment is successful when:
- ✅ GitHub Actions builds complete without errors
- ✅ APK downloads and installs on Android device
- ✅ App launches and basic navigation works
- ✅ Voice permissions can be granted
- ✅ No immediate crashes or critical errors

## 🆘 **Need Help?**

### Resources:
- **GitHub Actions Logs**: Detailed build information
- **Android Developer Docs**: https://developer.android.com/
- **Kotlin Documentation**: https://kotlinlang.org/docs/
- **Jetpack Compose**: https://developer.android.com/jetpack/compose

### Common Commands:
```bash
# Check build status
git status

# View recent commits
git log --oneline -10

# Force rebuild
git commit --allow-empty -m "Trigger rebuild"
git push

# Download artifacts via CLI (with GitHub CLI)
gh run download
```

---

🎉 **Congratulations!** You now have a fully automated Android app build and deployment pipeline!