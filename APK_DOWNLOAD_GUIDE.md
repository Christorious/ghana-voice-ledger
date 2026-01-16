# How to Download Your APK from GitHub Actions

## 📥 Quick Guide (3 Simple Steps)

### Step 1: Go to GitHub Actions
Open your browser and go to:
```
https://github.com/Christorious/ghana-voice-ledger/actions
```

### Step 2: Find the Latest Successful Build
Look for the most recent workflow run with a **green checkmark** ✅

The most recent successful runs should be:
- **Workflow #134** - "fix(ci): Use dev variant-specific task names in workflow"
- **Workflow #133** - "fix(database): Fix SQL syntax error in offline_operations table migration"

Click on either one (both have the APK).

### Step 3: Download the APK
1. Scroll down to the **"Artifacts"** section at the bottom
2. Look for **"debug-apk"** or **"app-dev-debug"**
3. Click to download (it will download as a ZIP file)
4. Extract the ZIP file
5. You'll find: `app-dev-debug.apk` (~65-75 MB)

---

## 📱 Installing on Your Android Device

### Option A: Direct Transfer (USB Cable)
1. Connect your Android phone to PC via USB
2. Copy `app-dev-debug.apk` to your phone's Downloads folder
3. On your phone, open **Files** or **Downloads** app
4. Tap the APK file
5. If prompted, enable "Install from Unknown Sources"
6. Tap **Install**

### Option B: Cloud Transfer (Google Drive/Dropbox)
1. Upload `app-dev-debug.apk` to Google Drive or Dropbox
2. On your phone, download the APK from the cloud
3. Tap the downloaded file
4. If prompted, enable "Install from Unknown Sources"
5. Tap **Install**

### Option C: Email
1. Email the APK to yourself
2. Open email on your phone
3. Download the attachment
4. Tap to install

---

## 🔒 Security Note

When installing, Android will warn you about "Unknown Sources" because this isn't from the Play Store. This is normal for development APKs. 

**To enable installation**:
- Android 8+: Settings → Apps → Special Access → Install Unknown Apps → Select your browser/file manager → Allow
- Android 7: Settings → Security → Unknown Sources → Enable

---

## ✅ What You'll Get

**App Name**: Ghana Voice Ledger  
**Version**: 1.0.0 (dev build)  
**Size**: ~65-75 MB  
**Features**: All core features enabled for testing

---

## 🧪 Testing Checklist

Once installed, try these features:

- [ ] Open the app
- [ ] Complete onboarding
- [ ] Grant microphone permission
- [ ] Record a test transaction (say: "I sold 2 tilapia for 30 cedis")
- [ ] View transaction history
- [ ] Check daily summary
- [ ] Try offline mode (enable airplane mode)
- [ ] Test language switching (Settings → Language)

---

## 🐛 If You Encounter Issues

**App won't install**:
- Make sure "Install from Unknown Sources" is enabled
- Check you have enough storage (~100 MB free)
- Try restarting your phone

**App crashes on launch**:
- Check Android version (needs Android 7.0+)
- Clear app data: Settings → Apps → Ghana Voice Ledger → Clear Data
- Reinstall

**Microphone not working**:
- Check microphone permission: Settings → Apps → Ghana Voice Ledger → Permissions
- Test microphone in another app to confirm it works

---

## 📊 Disk Space Summary

**On PC**: 65-75 MB (just the APK file)  
**On Phone**: ~150 MB (app + data after installation)  
**Total**: ~225 MB

---

## 🎉 Next Steps After Testing

1. **Test the app** thoroughly
2. **Gather feedback** (what works, what doesn't)
3. **Report issues** (if any)
4. **Share with potential users** for real-world testing

If you want to make changes to the app after testing, we can then install Java 17 for local development.

---

**Need help?** Let me know if you have any questions during the download or installation process!
