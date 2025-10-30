# Minimal Android Development Setup Script
# This script sets up a lightweight Android development environment

Write-Host "🚀 Setting up Minimal Android Development Environment" -ForegroundColor Cyan
Write-Host "=" * 60

# Check if Java is installed
Write-Host "☕ Checking Java installation..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1 | Select-String "version"
    if ($javaVersion -match "17\.") {
        Write-Host "✅ Java 17 is installed" -ForegroundColor Green
    } else {
        Write-Host "⚠️  Java 17 not found. Please install from: https://adoptium.net/" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ Java not found. Please install JDK 17" -ForegroundColor Red
}

# Check Android SDK
Write-Host "`n📱 Checking Android SDK..." -ForegroundColor Yellow
$androidHome = $env:ANDROID_HOME
if ($androidHome -and (Test-Path $androidHome)) {
    Write-Host "✅ Android SDK found at: $androidHome" -ForegroundColor Green
} else {
    Write-Host "⚠️  Android SDK not found. Download command line tools from:" -ForegroundColor Yellow
    Write-Host "   https://developer.android.com/studio#command-tools" -ForegroundColor White
}

# Update local.properties
Write-Host "`n📝 Updating local.properties..." -ForegroundColor Yellow
if ($androidHome) {
    $localProps = @"
# Android SDK location
sdk.dir=$($androidHome -replace '\\', '\\')

# API Keys (add your actual keys here)
GOOGLE_CLOUD_API_KEY=your_google_cloud_api_key_here
FIREBASE_PROJECT_ID=your_firebase_project_id
FIREBASE_API_KEY=your_firebase_api_key
# Database encryption keys are generated automatically via the Android Keystore

# Development settings
DEBUG_MODE=true
LOGGING_ENABLED=true
OFFLINE_MODE_ENABLED=true
SPEAKER_IDENTIFICATION_ENABLED=true
MULTI_LANGUAGE_ENABLED=true
"@
    
    $localProps | Out-File -FilePath "local.properties" -Encoding UTF8
    Write-Host "✅ local.properties updated" -ForegroundColor Green
} else {
    Write-Host "⚠️  Please set ANDROID_HOME environment variable first" -ForegroundColor Yellow
}

# Check Gradle
Write-Host "`n🔧 Checking Gradle..." -ForegroundColor Yellow
if (Test-Path "gradlew.bat") {
    Write-Host "✅ Gradle wrapper found" -ForegroundColor Green
} else {
    Write-Host "⚠️  Gradle wrapper not found" -ForegroundColor Yellow
}

# VS Code Extensions
Write-Host "`n💻 Recommended VS Code Extensions:" -ForegroundColor Cyan
$extensions = @(
    "mathiasfrohlich.Kotlin",
    "vscjava.vscode-gradle", 
    "vscjava.vscode-java-pack",
    "adelphes.android-dev-ext"
)

foreach ($ext in $extensions) {
    Write-Host "   - $ext" -ForegroundColor White
}

Write-Host "`nTo install all extensions, run:" -ForegroundColor Yellow
Write-Host "code --install-extension mathiasfrohlich.Kotlin" -ForegroundColor Gray
Write-Host "code --install-extension vscjava.vscode-gradle" -ForegroundColor Gray
Write-Host "code --install-extension vscjava.vscode-java-pack" -ForegroundColor Gray

# Build commands
Write-Host "`n🏗️  Build Commands:" -ForegroundColor Cyan
Write-Host "   Debug build:    .\gradlew.bat assembleDebug" -ForegroundColor White
Write-Host "   Release build:  .\gradlew.bat assembleRelease" -ForegroundColor White
Write-Host "   Run tests:      .\gradlew.bat test" -ForegroundColor White
Write-Host "   Install on device: .\gradlew.bat installDebug" -ForegroundColor White

# Next steps
Write-Host "`n📋 Next Steps:" -ForegroundColor Cyan
Write-Host "1. Install JDK 17 if not already installed" -ForegroundColor White
Write-Host "2. Download Android SDK command line tools" -ForegroundColor White
Write-Host "3. Set ANDROID_HOME environment variable" -ForegroundColor White
Write-Host "4. Install VS Code extensions" -ForegroundColor White
Write-Host "5. Update local.properties with your API keys" -ForegroundColor White
Write-Host "6. Run: .\gradlew.bat build" -ForegroundColor White

Write-Host "`n✨ Lightweight setup complete!" -ForegroundColor Green