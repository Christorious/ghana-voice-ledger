# Setup Git and Push to GitHub Script
# This script initializes git, commits all files, and pushes to GitHub

Write-Host "🚀 Setting up Git and pushing Ghana Voice Ledger to GitHub..." -ForegroundColor Cyan
Write-Host "=" * 60

# Check if git is installed
try {
    git --version | Out-Null
    Write-Host "✅ Git is available" -ForegroundColor Green
} catch {
    Write-Host "❌ Git is not installed" -ForegroundColor Red
    Write-Host "Please install Git from: https://git-scm.com/download/win" -ForegroundColor Yellow
    exit 1
}

# Initialize git repository if not already initialized
if (!(Test-Path ".git")) {
    Write-Host "📁 Initializing Git repository..." -ForegroundColor Yellow
    git init
    Write-Host "✅ Git repository initialized" -ForegroundColor Green
} else {
    Write-Host "✅ Git repository already exists" -ForegroundColor Green
}

# Configure git user (if not already configured)
$gitUser = git config --global user.name 2>$null
$gitEmail = git config --global user.email 2>$null

if (!$gitUser) {
    $userName = Read-Host "Enter your GitHub username"
    git config --global user.name "$userName"
    Write-Host "✅ Git username configured: $userName" -ForegroundColor Green
}

if (!$gitEmail) {
    $userEmail = Read-Host "Enter your GitHub email"
    git config --global user.email "$userEmail"
    Write-Host "✅ Git email configured: $userEmail" -ForegroundColor Green
}

# Add all files to git
Write-Host "📦 Adding all files to Git..." -ForegroundColor Yellow
git add .

# Check if there are changes to commit
$status = git status --porcelain
if ($status) {
    # Commit the changes
    Write-Host "💾 Committing changes..." -ForegroundColor Yellow
    git commit -m "Initial commit: Ghana Voice Ledger Android App

- Complete Android project structure with 142+ Kotlin files
- Jetpack Compose UI with Material 3 design
- Voice recognition and ML capabilities
- Offline-first architecture with Room database
- Security features with encryption and biometric auth
- Multi-language support (English, Twi, Ga, Ewe)
- Comprehensive testing suite
- CI/CD pipeline with GitHub Actions
- Docker support for containerized builds
- Complete documentation and setup guides

Ready for automated building and deployment! 🚀"

    Write-Host "✅ Changes committed successfully" -ForegroundColor Green
} else {
    Write-Host "ℹ️  No changes to commit" -ForegroundColor Blue
}

# Check if remote origin exists
$remoteUrl = git remote get-url origin 2>$null
if (!$remoteUrl) {
    Write-Host "🔗 Setting up GitHub remote..." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "📋 Next steps to connect to GitHub:" -ForegroundColor Cyan
    Write-Host "1. Go to https://github.com/new" -ForegroundColor White
    Write-Host "2. Create a new repository named 'ghana-voice-ledger'" -ForegroundColor White
    Write-Host "3. Copy the repository URL (e.g., https://github.com/yourusername/ghana-voice-ledger.git)" -ForegroundColor White
    Write-Host ""
    
    $repoUrl = Read-Host "Enter your GitHub repository URL"
    
    if ($repoUrl) {
        git remote add origin $repoUrl
        Write-Host "✅ Remote origin added: $repoUrl" -ForegroundColor Green
        
        # Push to GitHub
        Write-Host "⬆️  Pushing to GitHub..." -ForegroundColor Yellow
        try {
            git push -u origin main
            Write-Host "🎉 Successfully pushed to GitHub!" -ForegroundColor Green
        } catch {
            # Try with master branch if main fails
            try {
                git branch -M main
                git push -u origin main
                Write-Host "🎉 Successfully pushed to GitHub!" -ForegroundColor Green
            } catch {
                Write-Host "⚠️  Push failed. You may need to authenticate with GitHub." -ForegroundColor Yellow
                Write-Host "Try running: git push -u origin main" -ForegroundColor White
            }
        }
    }
} else {
    Write-Host "✅ Remote origin already configured: $remoteUrl" -ForegroundColor Green
    
    # Push to GitHub
    Write-Host "⬆️  Pushing to GitHub..." -ForegroundColor Yellow
    try {
        git push
        Write-Host "🎉 Successfully pushed to GitHub!" -ForegroundColor Green
    } catch {
        Write-Host "⚠️  Push failed. You may need to authenticate with GitHub." -ForegroundColor Yellow
        Write-Host "Try running: git push" -ForegroundColor White
    }
}

Write-Host ""
Write-Host "🎯 What happens next:" -ForegroundColor Cyan
Write-Host "1. ✅ Your code is now on GitHub" -ForegroundColor Green
Write-Host "2. 🔄 GitHub Actions will automatically start building your APK" -ForegroundColor Green
Write-Host "3. 📱 You can download the built APK from the Actions tab" -ForegroundColor Green
Write-Host "4. 🧪 Install and test the APK on your Android device" -ForegroundColor Green

Write-Host ""
Write-Host "📋 GitHub Actions Status:" -ForegroundColor Yellow
Write-Host "• Go to: https://github.com/yourusername/ghana-voice-ledger/actions" -ForegroundColor White
Write-Host "• Watch the build progress in real-time" -ForegroundColor White
Write-Host "• Download APK from 'Artifacts' section when complete" -ForegroundColor White

Write-Host ""
Write-Host "🚀 Your Ghana Voice Ledger is ready for automated building!" -ForegroundColor Green