@echo off
cd /d "%~dp0"
git add -A
git commit -m "Ready for App Center build"
git push origin main
echo.
echo Code pushed! Now set up App Center.
pause
