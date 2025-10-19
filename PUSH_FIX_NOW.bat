@echo off
echo ========================================
echo PUSHING BUILD FIX TO GITHUB
echo ========================================
echo.

git add app\build.gradle.kts
git add GITHUB_BUILD_INVESTIGATION.md
git add BUILD_FIX_APPLIED.md
git add quick-push.bat
git add PUSH_FIX_NOW.bat

git commit -m "Fix build configuration for GitHub Actions"

echo.
echo Pushing to GitHub...
git push origin main

echo.
echo ========================================
echo DONE! Build fix pushed to GitHub
echo ========================================
echo.
echo The new build should start automatically.
echo Check: https://github.com/Christorious/ghana-voice-ledger/actions
echo.
pause
