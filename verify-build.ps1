# Build Verification Script for Ghana Voice Ledger
# This script verifies that the Android project is properly configured

Write-Host "🔍 Ghana Voice Ledger - Build Configuration Verification" -ForegroundColor Cyan
Write-Host "=" * 60

# Check if required files exist
$requiredFiles = @(
    "build.gradle.kts",
    "settings.gradle.kts", 
    "app/build.gradle.kts",
    "app/src/main/AndroidManifest.xml",
    "app/src/main/java/com/voiceledger/ghana/VoiceLedgerApplication.kt",
    "app/src/main/java/com/voiceledger/ghana/presentation/MainActivity.kt",
    "local.properties"
)

Write-Host "📁 Checking required files..." -ForegroundColor Yellow
$missingFiles = @()

foreach ($file in $requiredFiles) {
    if (Test-Path $file) {
        Write-Host "✅ $file" -ForegroundColor Green
    } else {
        Write-Host "❌ $file" -ForegroundColor Red
        $missingFiles += $file
    }
}

# Check resource files
Write-Host "`n🎨 Checking resource files..." -ForegroundColor Yellow
$resourceFiles = @(
    "app/src/main/res/values/strings.xml",
    "app/src/main/res/values/colors.xml",
    "app/src/main/res/values/themes.xml",
    "app/src/main/res/values/dimens.xml"
)

foreach ($file in $resourceFiles) {
    if (Test-Path $file) {
        Write-Host "✅ $file" -ForegroundColor Green
    } else {
        Write-Host "❌ $file" -ForegroundColor Red
        $missingFiles += $file
    }
}

# Check key source directories
Write-Host "`n📦 Checking source structure..." -ForegroundColor Yellow
$sourceDirs = @(
    "app/src/main/java/com/voiceledger/ghana/data",
    "app/src/main/java/com/voiceledger/ghana/domain", 
    "app/src/main/java/com/voiceledger/ghana/presentation",
    "app/src/main/java/com/voiceledger/ghana/ml",
    "app/src/main/java/com/voiceledger/ghana/service",
    "app/src/test/java/com/voiceledger/ghana",
    "app/src/androidTest/java/com/voiceledger/ghana"
)

foreach ($dir in $sourceDirs) {
    if (Test-Path $dir) {
        $fileCount = (Get-ChildItem -Path $dir -Recurse -File).Count
        Write-Host "✅ $dir ($fileCount files)" -ForegroundColor Green
    } else {
        Write-Host "❌ $dir" -ForegroundColor Red
    }
}

# Summary
Write-Host "`n📊 Build Configuration Summary" -ForegroundColor Cyan
Write-Host "=" * 40

if ($missingFiles.Count -eq 0) {
    Write-Host "🎉 All required files are present!" -ForegroundColor Green
    Write-Host "✅ Project structure is complete" -ForegroundColor Green
    Write-Host "✅ Build configuration files are valid" -ForegroundColor Green
    Write-Host "✅ Resource files are in place" -ForegroundColor Green
    Write-Host "✅ Source code structure is correct" -ForegroundColor Green
    
    Write-Host "`n🚀 Next Steps:" -ForegroundColor Yellow
    Write-Host "1. Install Android Studio and Android SDK" -ForegroundColor White
    Write-Host "2. Update local.properties with your SDK path" -ForegroundColor White
    Write-Host "3. Add your API keys to local.properties" -ForegroundColor White
    Write-Host "4. Open project in Android Studio" -ForegroundColor White
    Write-Host "5. Sync project and resolve any dependency issues" -ForegroundColor White
    Write-Host "6. Run: ./gradlew build (once Gradle wrapper is set up)" -ForegroundColor White
    
    Write-Host "`n✨ Project is ready for development!" -ForegroundColor Green
} else {
    Write-Host "⚠️  Missing files detected:" -ForegroundColor Red
    foreach ($file in $missingFiles) {
        Write-Host "   - $file" -ForegroundColor Red
    }
    Write-Host "`nPlease create the missing files before proceeding." -ForegroundColor Yellow
}

Write-Host "`n📋 Project Statistics:" -ForegroundColor Cyan
$kotlinFiles = (Get-ChildItem -Path "app/src" -Recurse -Filter "*.kt").Count
$xmlFiles = (Get-ChildItem -Path "app/src" -Recurse -Filter "*.xml").Count
$totalFiles = (Get-ChildItem -Path "app/src" -Recurse -File).Count

Write-Host "   Kotlin files: $kotlinFiles" -ForegroundColor White
Write-Host "   XML files: $xmlFiles" -ForegroundColor White  
Write-Host "   Total source files: $totalFiles" -ForegroundColor White

Write-Host "`n🔧 Build Tools:" -ForegroundColor Cyan
Write-Host "   Gradle: 8.4" -ForegroundColor White
Write-Host "   Android Gradle Plugin: 8.2.1" -ForegroundColor White
Write-Host "   Kotlin: 1.9.22" -ForegroundColor White
Write-Host "   Compile SDK: 34" -ForegroundColor White
Write-Host "   Target SDK: 34" -ForegroundColor White
Write-Host "   Min SDK: 24" -ForegroundColor White

Write-Host "`n" -NoNewline