# Build Ghana Voice Ledger using Docker
# This script builds your Android app in a Docker container

Write-Host "🐳 Building Ghana Voice Ledger in Docker..." -ForegroundColor Cyan

# Check if Docker is available
try {
    docker --version | Out-Null
    Write-Host "✅ Docker is available" -ForegroundColor Green
} catch {
    Write-Host "❌ Docker is not installed or not running" -ForegroundColor Red
    Write-Host "Please install Docker Desktop from: https://www.docker.com/products/docker-desktop" -ForegroundColor Yellow
    exit 1
}

# Build the Docker image
Write-Host "📦 Building Docker image..." -ForegroundColor Yellow
docker build -t ghana-voice-ledger:latest .

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Docker image built successfully!" -ForegroundColor Green
    
    # Run the container to build the APK
    Write-Host "🏗️  Building APK in container..." -ForegroundColor Yellow
    docker run --rm -v ${PWD}/build-output:/app/app/build/outputs ghana-voice-ledger:latest
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "🎉 Build completed successfully!" -ForegroundColor Green
        Write-Host "📱 APK files are available in: build-output/" -ForegroundColor White
        
        # List built files
        if (Test-Path "build-output") {
            Write-Host "`n📋 Built files:" -ForegroundColor Cyan
            Get-ChildItem -Path "build-output" -Recurse -Include "*.apk", "*.aab" | ForEach-Object {
                Write-Host "   $($_.FullName)" -ForegroundColor White
            }
        }
    } else {
        Write-Host "❌ Build failed in Docker container" -ForegroundColor Red
    }
} else {
    Write-Host "❌ Failed to build Docker image" -ForegroundColor Red
}

Write-Host "`n🚀 Alternative: Use GitHub Actions for automatic building!" -ForegroundColor Yellow
Write-Host "   1. Push your code to GitHub" -ForegroundColor White
Write-Host "   2. GitHub will automatically build APKs" -ForegroundColor White
Write-Host "   3. Download from Actions tab" -ForegroundColor White