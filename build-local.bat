@echo off
echo.
echo 📱 Local APK Build
echo ==================
echo.

echo ☕ Checking Java version...
java -version
if %ERRORLEVEL% neq 0 (
    echo ❌ Java not found. Please install Java 11 or higher.
    pause
    exit /b 1
)

echo.
echo 🧹 Cleaning project...
gradlew.bat clean
if %ERRORLEVEL% neq 0 (
    echo ❌ Clean failed!
    goto :error
)

echo.
echo 🔨 Building debug APK...
gradlew.bat assembleDebug --stacktrace
if %ERRORLEVEL% equ 0 (
    echo.
    echo ✅ Build successful!
    
    if exist "app\build\outputs\apk\debug\app-debug.apk" (
        echo 📦 APK created: app\build\outputs\apk\debug\app-debug.apk
        echo 📂 Opening APK folder...
        explorer "app\build\outputs\apk\debug"
        echo.
        echo 🎉 Success! You can now install the APK on your device.
        echo 💡 To install: Enable 'Unknown sources' in Android settings, then install the APK.
    ) else (
        echo ❌ APK not found at expected location
    )
) else (
    goto :error
)

echo.
echo Done!
pause
exit /b 0

:error
echo.
echo ❌ Build failed!
echo 💡 Common fixes:
echo    - Ensure Java 11+ is installed and JAVA_HOME is set
echo    - Check for compilation errors in the output above
echo.
echo 🚀 Alternative: Use GitHub Actions (recommended)
echo    1. Push your changes to GitHub
echo    2. Go to Actions tab in your repository  
echo    3. Download the APK from the successful build artifacts
echo    GitHub Actions uses Java 17 and builds successfully!
echo.
pause
exit /b 1