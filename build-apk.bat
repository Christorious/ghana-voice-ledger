@echo off
echo Building Ghana Voice Ledger APK...
echo.

REM Check if gradlew exists
if not exist "gradlew.bat" (
    echo Error: gradlew.bat not found
    echo Please run this from the project root directory
    pause
    exit /b 1
)

REM Build debug APK
echo Building debug APK...
call gradlew.bat assembleDebug

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✅ Build successful!
    echo APK location: app\build\outputs\apk\debug\app-debug.apk
    echo.
    if exist "app\build\outputs\apk\debug\app-debug.apk" (
        echo Opening APK folder...
        explorer "app\build\outputs\apk\debug"
    )
) else (
    echo.
    echo ❌ Build failed!
    echo Check the error messages above
)

echo.
pause