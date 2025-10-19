@echo off
git add app/build.gradle.kts
git add GITHUB_BUILD_INVESTIGATION.md
git commit -m "Fix build configuration for GitHub Actions"
git push origin main
echo Done!
pause
