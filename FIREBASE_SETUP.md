# 🔥 Firebase App Distribution Setup Guide

Firebase App Distribution is a great alternative to GitHub Actions for building and distributing your APK.

## 📋 Prerequisites

1. **Firebase Account**: Create account at [Firebase Console](https://console.firebase.google.com/)
2. **Node.js**: Install from [nodejs.org](https://nodejs.org/)
3. **Firebase CLI**: Install globally with `npm install -g firebase-tools`

## 🚀 Quick Setup Steps

### Step 1: Create Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Create a project" or use existing project
3. Follow the setup wizard

### Step 2: Add Android App
1. In Firebase console, click "Add app" → Android
2. Enter package name: `com.voiceledger.ghana`
3. Download `google-services.json`
4. Place it in `app/` directory (already configured)

### Step 3: Enable App Distribution
1. In Firebase console, go to "App Distribution"
2. Click "Get started"
3. Note your Firebase App ID (format: `1:123456789:android:abc123def456`)

### Step 4: Install Firebase CLI
```bash
npm install -g firebase-tools
firebase login
```

### Step 5: Configure the Deploy Script
1. Edit `firebase-deploy.ps1`
2. Replace `YOUR_FIREBASE_APP_ID` with your actual Firebase App ID
3. Run the script: `.\firebase-deploy.ps1`

## 🎯 Alternative: Manual Upload

If you prefer manual upload:

1. **Build APK locally**:
   ```bash
   .\gradlew.bat assembleDebug
   ```

2. **Find APK**: `app\build\outputs\apk\debug\app-debug.apk`

3. **Upload manually**:
   - Go to Firebase Console → App Distribution
   - Click "Upload new release"
   - Drag and drop your APK
   - Add release notes
   - Select testers/groups
   - Click "Distribute"

## 📱 Benefits of Firebase App Distribution

✅ **Reliable**: Google's infrastructure, very stable
✅ **Easy**: Simple web interface for uploads
✅ **Fast**: Quick builds and distribution
✅ **Testing**: Easy to share with testers
✅ **Analytics**: Download and crash analytics
✅ **Integration**: Works with Firebase ecosystem

## 🔧 Troubleshooting

**Build fails locally?**
- Check Java version: `java -version` (should be 11+)
- Clean project: `.\gradlew.bat clean`
- Check for compilation errors

**Firebase CLI issues?**
- Update: `npm update -g firebase-tools`
- Re-login: `firebase logout && firebase login`

**APK not found?**
- Check build output: `app\build\outputs\apk\debug\`
- Ensure build completed successfully

## 🎉 Next Steps

Once set up, you can:
1. Build APK locally: `.\gradlew.bat assembleDebug`
2. Upload to Firebase: `.\firebase-deploy.ps1`
3. Share download link with testers
4. Get feedback and crash reports

This approach is often more reliable than GitHub Actions for Android builds!