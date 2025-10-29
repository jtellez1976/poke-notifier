@echo off
echo ========================================
echo  POKE NOTIFIER - OFFLINE BUILD
echo ========================================
echo.
echo Attempting offline build (for Mojang outages)...
echo.

gradlew build --offline --no-daemon

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✅ BUILD SUCCESSFUL - Offline mode worked!
    echo.
) else (
    echo.
    echo ❌ BUILD FAILED - Try online mode or check cache
    echo.
    echo Attempting online build as fallback...
    gradlew build --no-daemon
)

pause