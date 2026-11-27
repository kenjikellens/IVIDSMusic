@echo off
setlocal
cd /d "%~dp0"

if /i "%~1"=="--dev" goto :run_gradle

if exist "dist\IVIDSMusic_PC\IVIDS Music.exe" (
    start "" "dist\IVIDSMusic_PC\IVIDS Music.exe"
    exit /b 0
)

if exist "composeApp\build\compose\binaries\main\app\IVIDS Music\IVIDS Music.exe" (
    start "" "composeApp\build\compose\binaries\main\app\IVIDS Music\IVIDS Music.exe"
    exit /b 0
)

:run_gradle
echo Launching IVIDS Music Desktop via Gradle...
call gradlew.bat :composeApp:run --no-daemon
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Failed to launch IVIDS Music Desktop (exit code %ERRORLEVEL%).
    pause
    exit /b %ERRORLEVEL%
)
exit /b 0
