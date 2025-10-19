# Simple PowerShell script to push changes
Write-Host "Adding files..." -ForegroundColor Green
git add .

Write-Host "Committing..." -ForegroundColor Green
git commit -m "Fix build configuration for GitHub Actions"

Write-Host "Pushing to GitHub..." -ForegroundColor Green
git push origin main

Write-Host ""
Write-Host "DONE! Check GitHub Actions:" -ForegroundColor Cyan
Write-Host "https://github.com/Christorious/ghana-voice-ledger/actions" -ForegroundColor Yellow
