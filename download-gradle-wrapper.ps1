# Download Gradle Wrapper Script
# This script downloads the actual Gradle wrapper jar file

Write-Host "📦 Downloading Gradle Wrapper..." -ForegroundColor Cyan

$gradleWrapperUrl = "https://services.gradle.org/distributions/gradle-8.4-bin.zip"
$wrapperJarUrl = "https://github.com/gradle/gradle/raw/v8.4.0/gradle/wrapper/gradle-wrapper.jar"

# Create gradle/wrapper directory if it doesn't exist
if (!(Test-Path "gradle/wrapper")) {
    New-Item -ItemType Directory -Path "gradle/wrapper" -Force
    Write-Host "✅ Created gradle/wrapper directory" -ForegroundColor Green
}

# Download gradle-wrapper.jar
try {
    Write-Host "⬇️  Downloading gradle-wrapper.jar..." -ForegroundColor Yellow
    Invoke-WebRequest -Uri $wrapperJarUrl -OutFile "gradle/wrapper/gradle-wrapper.jar"
    Write-Host "✅ gradle-wrapper.jar downloaded successfully" -ForegroundColor Green
} catch {
    Write-Host "❌ Failed to download gradle-wrapper.jar" -ForegroundColor Red
    Write-Host "Please download manually from: $wrapperJarUrl" -ForegroundColor Yellow
}

# Test gradle wrapper
Write-Host "`n🧪 Testing Gradle wrapper..." -ForegroundColor Yellow
try {
    $gradleVersion = & .\gradlew.bat --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Gradle wrapper is working!" -ForegroundColor Green
        Write-Host $gradleVersion -ForegroundColor Gray
    } else {
        Write-Host "⚠️  Gradle wrapper test failed" -ForegroundColor Yellow
    }
} catch {
    Write-Host "⚠️  Could not test Gradle wrapper" -ForegroundColor Yellow
}

Write-Host "`n🎉 Gradle setup complete!" -ForegroundColor Green
Write-Host "You can now run: .\gradlew.bat build" -ForegroundColor White