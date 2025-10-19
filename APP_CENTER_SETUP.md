# 🚀 App Center Setup Guide

## What is App Center?
Visual Studio App Center is Microsoft's cloud service that can build Android apps automatically.

---

## ✅ STEP-BY-STEP SETUP

### Step 1: Push Code to GitHub (DO THIS FIRST!)

**Run this file:**
```
ULTRA_SIMPLE_PUSH.bat
```

This will push all your code to GitHub.

---

### Step 2: Sign Up for App Center

1. Go to: **https://appcenter.ms/**
2. Click **"Get Started"** or **"Sign In"**
3. Sign in with your **GitHub account** (easiest)
4. This will connect App Center to your GitHub

---

### Step 3: Create New App in App Center

1. Click **"Add new app"** button
2. Fill in:
   - **App name:** Ghana Voice Ledger
   - **Release Type:** Beta
   - **OS:** Android
   - **Platform:** Java / Kotlin
3. Click **"Add new app"**

---

### Step 4: Connect to GitHub Repository

1. In your new app, go to **"Build"** tab (left sidebar)
2. Click **"Connect to repository"**
3. Select **"GitHub"**
4. Find and select: **Christorious/ghana-voice-ledger**
5. Click **"Connect"**

---

### Step 5: Configure Build

1. Select the **"main"** branch
2. Click **"Configure build"**
3. Configure these settings:

**Build Configuration:**
- **Build variant:** debug (or release if you have signing)
- **Build frequency:** Build this branch on every push
- **Automatically increment version code:** ✓ (optional)

**Environment variables:** (Add these)
```
GOOGLE_CLOUD_API_KEY = (leave empty for now)
FIREBASE_PROJECT_ID = ghana-voice-ledger
ENCRYPTION_KEY = 12345678901234567890123456789012
DB_ENCRYPTION_KEY = 98765432109876543210987654321098
```

**Build scripts:**
- **Post-clone script:** (leave empty)
- **Pre-build script:** (leave empty)
- **Post-build script:** (leave empty)

4. Click **"Save & Build"**

---

### Step 6: Wait for Build

1. App Center will start building your APK
2. This takes **10-20 minutes**
3. You'll see the build progress in real-time
4. When done, you'll see **"Build succeeded"** ✅

---

### Step 7: Download APK

1. Click on the successful build
2. Click **"Download"** button
3. You'll get the APK file directly!
4. Install it on your Android device

---

## 🎯 ADVANTAGES OF APP CENTER

✅ **Easy to use** - Visual interface, no command line
✅ **Automatic builds** - Builds on every push to GitHub
✅ **Distribution** - Can send APK directly to testers
✅ **Crash reporting** - Built-in crash analytics
✅ **Free tier** - 240 build minutes per month (enough for testing)

---

## 🔧 IF BUILD FAILS IN APP CENTER

App Center will show you the exact error. Common issues:

### Issue 1: Missing google-services.json
**Solution:** Already included in your project ✓

### Issue 2: Gradle build fails
**Solution:** The build.gradle.kts fix we made should handle this ✓

### Issue 3: Missing dependencies
**Solution:** App Center will download them automatically ✓

---

## 📱 DISTRIBUTE TO TESTERS

Once build succeeds:

1. Go to **"Distribute"** tab
2. Click **"Distribute new release"**
3. Select the APK you just built
4. Add tester emails
5. Click **"Distribute"**
6. Testers get email with download link!

---

## 💡 QUICK START CHECKLIST

- [ ] Run `ULTRA_SIMPLE_PUSH.bat` to push code to GitHub
- [ ] Go to https://appcenter.ms/
- [ ] Sign in with GitHub
- [ ] Create new Android app
- [ ] Connect to your GitHub repository
- [ ] Configure build settings
- [ ] Click "Save & Build"
- [ ] Wait 10-20 minutes
- [ ] Download APK!

---

## 🆘 NEED HELP?

If you get stuck at any step, tell me:
1. Which step you're on
2. What error you see (if any)
3. Screenshot if possible

I'll guide you through it!

---

**App Center is much easier than GitHub Actions for beginners!**

**Start here:** https://appcenter.ms/
