# Firebase App Distribution Deployment Script
# This script builds the APK and uploads it to Firebase App Distribution

Write-Host "🔥 Firebase App Distribution Deployment" -ForegroundColor Green
Write-Host "=======================================" -ForegroundColor Green

# Check if Firebase CLI is installed
try {
    firebase --version | Out-Null
    Write-Host "✅ Firebase CLI found" -ForegroundColor Green
} catch {
    Write-Host "❌ Firebase CLI not found. Installing..." -ForegroundColor Red
    Write-Host "Please install Firebase CLI first:" -ForegroundColor Yellow
    Write-Host "npm install -g firebase-tools" -ForegroundColor Yellow
    Write-Host "Then run: firebase login" -ForegroundColor Yellow
    exit 1
}

# Clean and build the APK
Write-Host "🧹 Cleaning project..." -ForegroundColor Blue
.\gradlew.bat clean

Write-Host "🔨 Building debug APK..." -ForegroundColor Blue
.\gradlew.bat assembleDebug

# Check if build was successful
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ APK built successfully!" -ForegroundColor Green
    
    # Upload to Firebase App Distribution
    Write-Host "🚀 Uploading to Firebase App Distribution..." -ForegroundColor Blue
    
    # You can customize these parameters
    $apkPath = "app\build\outputs\apk\debug\app-debug.apk"
    $releaseNotes = "Debug build from $(Get-Date -Format 'yyyy-MM-dd HH:mm')"
    
    firebase appdistribution:distribute $apkPath `
        --app "YOUR_FIREBASE_APP_ID" `
        --release-notes "$releaseNotes" `
        --groups "testers"
        
    if ($LASTEXITCODE -eq 0) {
        Write-Host "🎉 Successfully uploaded to Firebase App Distribution!" -ForegroundColor Green
        Write-Host "Check your Firebase console for the download link." -ForegroundColor Yellow
    } else {
        Write-Host "❌ Failed to upload to Firebase" -ForegroundColor Red
    }
} else {
    Write-Host "❌ Build failed!" -ForegroundColor Red
    Write-Host "Check the error messages above." -ForegroundColor Yellow
}

Write-Host "Done!" -ForegroundColor Green