# Local APK Build Script
# Simple script to build APK locally without any cloud services

Write-Host "📱 Local APK Build" -ForegroundColor Green
Write-Host "==================" -ForegroundColor Green

# Check Java version
Write-Host "☕ Checking Java version..." -ForegroundColor Blue
try {
    $javaVersion = java -version 2>&1 | Select-String "version" | ForEach-Object { $_.ToString() }
    Write-Host "Java version: $javaVersion" -ForegroundColor Yellow
    
    # Check if Java 8 is being used with modern Gradle
    if ($javaVersion -match "1\.8\.") {
        Write-Host "⚠️  Warning: Java 8 detected. This project requires Java 11+ for the current Gradle/Android setup." -ForegroundColor Yellow
        Write-Host "💡 Options:" -ForegroundColor Cyan
        Write-Host "   1. Install Java 11+ and set JAVA_HOME" -ForegroundColor Cyan
        Write-Host "   2. Use GitHub Actions to build (already configured)" -ForegroundColor Cyan
        Write-Host "   3. Continue anyway (may fail)" -ForegroundColor Cyan
        
        $choice = Read-Host "Continue anyway? (y/N)"
        if ($choice -ne "y" -and $choice -ne "Y") {
            Write-Host "❌ Build cancelled. Please install Java 11+ or use GitHub Actions." -ForegroundColor Red
            exit 1
        }
    }
} catch {
    Write-Host "❌ Java not found. Please install Java 11 or higher." -ForegroundColor Red
    exit 1
}

# Clean project
Write-Host "🧹 Cleaning project..." -ForegroundColor Blue
.\gradlew.bat clean

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Clean failed!" -ForegroundColor Red
    exit 1
}

# Build debug APK
Write-Host "🔨 Building debug APK..." -ForegroundColor Blue
.\gradlew.bat assembleDebug --stacktrace

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Build successful!" -ForegroundColor Green
    
    # Find the APK
    $apkPath = "app\build\outputs\apk\debug\app-debug.apk"
    if (Test-Path $apkPath) {
        $apkSize = (Get-Item $apkPath).Length / 1MB
        Write-Host "📦 APK created: $apkPath" -ForegroundColor Green
        Write-Host "📏 APK size: $([math]::Round($apkSize, 2)) MB" -ForegroundColor Yellow
        
        # Open the folder containing the APK
        Write-Host "📂 Opening APK folder..." -ForegroundColor Blue
        Start-Process "explorer.exe" -ArgumentList "/select,`"$(Resolve-Path $apkPath)`""
        
        Write-Host "🎉 Success! You can now install the APK on your device." -ForegroundColor Green
        Write-Host "💡 To install: Enable 'Unknown sources' in Android settings, then install the APK." -ForegroundColor Yellow
    } else {
        Write-Host "❌ APK not found at expected location: $apkPath" -ForegroundColor Red
    }
} else {
    Write-Host "❌ Build failed!" -ForegroundColor Red
    Write-Host "💡 Check the error messages above for details." -ForegroundColor Yellow
    Write-Host "💡 Common fixes:" -ForegroundColor Yellow
    Write-Host "   - Ensure Java 11+ is installed and JAVA_HOME is set" -ForegroundColor Yellow
    Write-Host "   - Run: .\gradlew.bat --version" -ForegroundColor Yellow
    Write-Host "   - Check for compilation errors in the output" -ForegroundColor Yellow
    Write-Host "" -ForegroundColor Yellow
    Write-Host "🚀 Alternative: Use GitHub Actions (recommended)" -ForegroundColor Green
    Write-Host "   1. Push your changes to GitHub" -ForegroundColor Cyan
    Write-Host "   2. Go to Actions tab in your repository" -ForegroundColor Cyan
    Write-Host "   3. Download the APK from the successful build artifacts" -ForegroundColor Cyan
    Write-Host "   GitHub Actions uses Java 17 and builds successfully!" -ForegroundColor Cyan
}

Write-Host "Done!" -ForegroundColor Green