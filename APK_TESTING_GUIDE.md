# 📱 APK Testing Guide - Ghana Voice Ledger

## 🎯 **Quick Testing Checklist**

### Before You Start:
- [ ] Android device with version 7.0+ (API 24+)
- [ ] USB cable (optional, for debugging)
- [ ] 50MB+ free storage space
- [ ] Microphone access available

## 📥 **Step 1: Download & Install APK**

### Download from GitHub:
1. Go to: `https://github.com/yourusername/ghana-voice-ledger/actions`
2. Click the latest successful build (green checkmark ✅)
3. Scroll down to **Artifacts** section
4. Download `debug-apk.zip`
5. Extract to get `app-ghana-debug.apk`

### Install on Android Device:

#### Method 1: Direct Install (Recommended)
1. **Transfer APK to phone**:
   - Email to yourself and download on phone
   - Use USB cable and copy to Downloads folder
   - Use cloud storage (Google Drive, Dropbox)

2. **Enable Unknown Sources**:
   - Android 8+: Settings → Apps → Special Access → Install Unknown Apps → Enable for your file manager
   - Android 7: Settings → Security → Unknown Sources → Enable

3. **Install APK**:
   - Open file manager on phone
   - Navigate to Downloads folder
   - Tap `app-ghana-debug.apk`
   - Tap "Install" when prompted
   - Wait for installation to complete

#### Method 2: ADB Install (Advanced)
```bash
# Connect phone via USB with Developer Options enabled
adb devices
adb install app-ghana-debug.apk
```

## 🧪 **Step 2: Basic Functionality Testing**

### First Launch Test:
- [ ] **App Icon**: Appears in app drawer
- [ ] **Launch**: Taps opens app without crash
- [ ] **Splash Screen**: Shows briefly on startup
- [ ] **Main Screen**: Dashboard loads successfully
- [ ] **No Immediate Crashes**: App stays open for 30+ seconds

### Permission Testing:
- [ ] **Microphone Permission**: Prompted when needed
- [ ] **Storage Permission**: Granted for data storage
- [ ] **Notification Permission**: Asked for transaction alerts
- [ ] **Permission Denial**: App handles gracefully if denied

### Navigation Testing:
- [ ] **Bottom Navigation**: All tabs accessible
- [ ] **Dashboard Tab**: Shows transaction summary
- [ ] **History Tab**: Shows transaction list (may be empty)
- [ ] **Settings Tab**: Opens settings screen
- [ ] **Back Button**: Works correctly throughout app

### Core Feature Testing:
- [ ] **Voice Recording**: Can access microphone
- [ ] **Transaction Entry**: Can manually add transactions
- [ ] **Data Persistence**: Data survives app restart
- [ ] **Offline Mode**: Works without internet connection

## 🎤 **Step 3: Voice Feature Testing**

### Voice Recording Test:
1. **Grant Microphone Permission** when prompted
2. **Find Voice Button** (usually floating action button)
3. **Tap and Hold** to start recording
4. **Speak Clearly**: "I sold 5 bags of rice for 50 cedis"
5. **Release Button** to stop recording
6. **Check Result**: Should show transaction or processing indicator

### Voice Commands to Test:
```
✅ Basic Sales:
- "I sold 3 fish for 15 cedis"
- "Customer bought 2 bags of rice for 20 cedis"
- "Sold 1 bottle of water for 2 cedis"

✅ Purchases:
- "I bought 10 tomatoes for 5 cedis"
- "Purchased fuel for 50 cedis"

✅ Different Languages (if supported):
- Try Twi phrases if you speak it
- Test English with Ghanaian accent
```

### Expected Voice Behavior:
- [ ] **Recording Indicator**: Shows when recording
- [ ] **Processing Feedback**: Indicates when processing speech
- [ ] **Transaction Creation**: Creates transaction from speech
- [ ] **Error Handling**: Shows helpful message if speech unclear
- [ ] **Offline Processing**: Works without internet (may be limited)

## 📊 **Step 4: Data & Storage Testing**

### Transaction Management:
- [ ] **Add Transaction**: Manually create test transaction
- [ ] **View Transactions**: See transactions in history
- [ ] **Edit Transaction**: Modify existing transaction
- [ ] **Delete Transaction**: Remove transaction
- [ ] **Data Persistence**: Transactions survive app restart

### Settings Testing:
- [ ] **Language Settings**: Can change app language
- [ ] **Voice Settings**: Can adjust voice recognition settings
- [ ] **Privacy Settings**: Can control data sharing
- [ ] **Notification Settings**: Can enable/disable notifications
- [ ] **Export Data**: Can export transaction data (if available)

## 🔍 **Step 5: Performance Testing**

### Stability Testing:
- [ ] **Extended Use**: Use app for 10+ minutes without crash
- [ ] **Memory Usage**: App doesn't consume excessive RAM
- [ ] **Battery Impact**: Reasonable battery consumption
- [ ] **Background Behavior**: Handles being backgrounded properly
- [ ] **Rotation**: Works in portrait and landscape (if supported)

### Edge Case Testing:
- [ ] **No Internet**: App functions offline
- [ ] **Low Storage**: Handles low device storage gracefully
- [ ] **Interrupted Recording**: Handles phone calls during recording
- [ ] **Permission Revocation**: Handles permission being revoked
- [ ] **App Updates**: Can be updated with newer APK

## 🐛 **Step 6: Bug Reporting**

### If You Find Issues:

#### Critical Issues (App Crashes):
1. **Note the Steps**: What were you doing when it crashed?
2. **Device Info**: Android version, device model
3. **Reproduction**: Can you make it crash again?
4. **Screenshots**: Take screenshots if possible

#### Minor Issues (Features Not Working):
1. **Expected Behavior**: What should happen?
2. **Actual Behavior**: What actually happens?
3. **Frequency**: Does it happen every time?
4. **Workarounds**: Is there another way to do it?

#### Report Format:
```
**Issue**: Brief description
**Steps to Reproduce**:
1. Step one
2. Step two
3. Issue occurs

**Expected**: What should happen
**Actual**: What actually happens
**Device**: Android version, device model
**APK Version**: Date downloaded from GitHub Actions
```

## 📱 **Step 7: Device Compatibility Testing**

### Test on Multiple Devices (if available):
- [ ] **Different Android Versions**: 7.0, 8.0, 9.0, 10, 11, 12, 13, 14
- [ ] **Different Screen Sizes**: Phone, tablet, small phone
- [ ] **Different Manufacturers**: Samsung, Google, Huawei, etc.
- [ ] **Different RAM Amounts**: 2GB, 4GB, 6GB+ devices

### Performance Expectations by Device:
- **High-End Devices** (6GB+ RAM): Smooth performance, fast voice processing
- **Mid-Range Devices** (3-6GB RAM): Good performance, acceptable voice processing
- **Budget Devices** (2-3GB RAM): Basic functionality, slower voice processing

## 🎯 **Step 8: Real-World Usage Testing**

### Simulate Actual Use Cases:
- [ ] **Market Vendor**: Record multiple sales quickly
- [ ] **Small Shop Owner**: Mix of sales and purchases
- [ ] **Service Provider**: Record service transactions
- [ ] **Daily Summary**: Use app throughout a full day

### Environmental Testing:
- [ ] **Noisy Environment**: Test voice recording in noisy area
- [ ] **Quiet Environment**: Test in quiet room
- [ ] **Different Accents**: Test with different speakers
- [ ] **Background Apps**: Test with other apps running

## ✅ **Success Criteria**

### Your APK is ready for use if:
- ✅ **Installs Successfully** on target Android devices
- ✅ **Launches Without Crashing** consistently
- ✅ **Core Features Work** (voice recording, transaction entry)
- ✅ **Data Persists** between app sessions
- ✅ **Permissions Work** correctly
- ✅ **Performance is Acceptable** for target devices
- ✅ **No Critical Bugs** that prevent basic usage

### Ready for Distribution if:
- ✅ **Tested on Multiple Devices** successfully
- ✅ **Real-World Usage** scenarios work
- ✅ **Edge Cases Handled** gracefully
- ✅ **User Feedback** is positive
- ✅ **Performance Optimized** for target market

## 🚀 **Next Steps After Testing**

### If Testing is Successful:
1. **Document Feedback**: Note what works well
2. **Plan Improvements**: List desired enhancements
3. **Prepare for Beta**: Consider wider testing group
4. **Plan Distribution**: Prepare for Play Store or direct distribution

### If Issues Found:
1. **Report Issues**: Create detailed bug reports
2. **Prioritize Fixes**: Critical vs. nice-to-have
3. **Wait for Updates**: New builds will be available on GitHub
4. **Retest**: Download and test updated APKs

## 📞 **Getting Help**

### Resources:
- **GitHub Issues**: Report bugs at repository issues page
- **Build Logs**: Check GitHub Actions for build details
- **Android Logs**: Use `adb logcat` for detailed error logs
- **Community**: Engage with other testers and developers

### Emergency Contacts:
- **Critical Issues**: Report immediately via GitHub issues
- **Security Concerns**: Report privately to repository maintainers
- **Performance Issues**: Include device specifications in reports

---

🎉 **Happy Testing!** Your feedback helps make Ghana Voice Ledger better for everyone!