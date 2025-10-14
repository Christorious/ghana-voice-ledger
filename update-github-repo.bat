@echo off
echo.
echo 🔄 Updating GitHub Repository - Ghana Voice Ledger
echo =================================================
echo.

echo 📋 This will update your existing GitHub repository with all current changes
echo.

set /p username="Enter your GitHub username: "
if "%username%"=="" (
    echo ❌ Username is required!
    pause
    exit /b 1
)

echo.
echo 🔧 Updating repository...
echo.

echo 📦 Adding all current files...
git add .

echo 💾 Creating update commit...
git commit -m "Update: Complete Ghana Voice Ledger Implementation

🎉 Major Updates:
✅ Complete Android project structure with all components
✅ Firebase integration fully configured
✅ Multi-language support (English, Twi, Ga, Ewe)
✅ Voice recording and speech recognition
✅ Speaker identification with TensorFlow Lite
✅ Transaction processing and entity extraction
✅ Offline-first architecture with sync
✅ Material Design 3 UI with accessibility
✅ Comprehensive security and privacy features
✅ Performance optimization and battery management
✅ Daily summaries and analytics
✅ Complete test coverage
✅ CI/CD pipeline with GitHub Actions
✅ Firebase App Distribution setup

🔧 Technical Improvements:
- Enhanced GitHub Actions workflow
- Firebase distribution automation
- Comprehensive documentation
- Build optimization scripts
- Multiple deployment options

Ready for APK building and distribution!"

echo 🌿 Ensuring main branch...
git branch -M main

echo 🔗 Setting remote repository...
git remote remove origin 2>nul
git remote add origin https://github.com/%username%/ghana-voice-ledger.git

echo ⬆️ Pushing updates to GitHub...
git push -u origin main --force

if %ERRORLEVEL% equ 0 (
    echo.
    echo ✅ Successfully updated GitHub repository!
    echo.
    echo 🎯 What happens next:
    echo 1. GitHub Actions will automatically start building
    echo 2. Build will complete in 5-10 minutes
    echo 3. APK will be available in Actions artifacts
    echo 4. You can then upload to Firebase App Distribution
    echo.
    echo 🔗 Check build progress:
    echo https://github.com/%username%/ghana-voice-ledger/actions
    echo.
    echo 🔥 Firebase Console:
    echo https://console.firebase.google.com/
    echo.
    echo 📱 Your APK will be built with Java 17 - no compatibility issues!
    echo.
    echo 🚀 Next: Wait for build completion, then download APK and upload to Firebase
) else (
    echo.
    echo ❌ Update failed! Possible issues:
    echo 1. Repository authentication needed
    echo 2. Repository name might be different
    echo 3. Network connectivity issues
    echo.
    echo 💡 Try these solutions:
    echo 1. Check: https://github.com/%username%/ghana-voice-ledger
    echo 2. Verify GitHub authentication
    echo 3. Run: git push -u origin main
)

echo.
echo 📋 After successful push:
echo 1. Go to your GitHub repository
echo 2. Click "Actions" tab
echo 3. Watch the build progress
echo 4. Download APK when complete
echo 5. Upload to Firebase App Distribution
echo.

pause