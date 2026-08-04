@echo off
setlocal enabledelayedexpansion

echo ============================================================
echo   IVIDS Music - Local Multi-Platform Build Script
echo ============================================================

set NODE_OPTIONS=--max-old-space-size=8192

echo [1/5] Updating versionCode in app/build.gradle.kts...
node -e "const fs = require('fs'); const p = 'app/build.gradle.kts'; if (fs.existsSync(p)) { let c = fs.readFileSync(p, 'utf8'); c = c.replace(/versionCode\s*=\s*(\d+)/, (m, v) => 'versionCode = ' + (parseInt(v, 10) + 1)); fs.writeFileSync(p, c, 'utf8'); console.log('✔ versionCode updated.'); }"

echo.
echo [2/5] Compiling Mobile Release APK...
call gradlew.bat assembleMobileRelease
if %ERRORLEVEL% EQU 0 (
    if exist "app\build\outputs\apk\mobile\release\app-mobile-release.apk" (
        copy /Y "app\build\outputs\apk\mobile\release\app-mobile-release.apk" "IVIDSMusic_Mobile.apk" >nul
        copy /Y "app\build\outputs\apk\mobile\release\app-mobile-release.apk" "IVIDSMusic.apk" >nul
        echo ✔ Mobile APK compiled and updated in root directory!
    )
) else (
    echo [ERROR] Mobile APK compilation failed!
    exit /b %ERRORLEVEL%
)

echo.
echo [3/5] Compiling TV Release APK...
call gradlew.bat assembleTvRelease
if %ERRORLEVEL% EQU 0 (
    if exist "app\build\outputs\apk\tv\release\app-tv-release.apk" (
        copy /Y "app\build\outputs\apk\tv\release\app-tv-release.apk" "IVIDSMusic_TV.apk" >nul
        echo ✔ TV APK compiled and updated in root directory!
    )
) else (
    echo [ERROR] TV APK compilation failed!
    exit /b %ERRORLEVEL%
)

echo.
echo [4/5] Compiling PC Desktop Executable...
call npm --prefix app run dist
if %ERRORLEVEL% EQU 0 (
    if exist "app\dist\*.exe" (
        copy /Y "app\dist\*.exe" "IVIDSMusic_PC.exe" >nul
        echo ✔ PC Desktop executable compiled and updated in root directory!
    )
) else (
    echo [ERROR] PC Desktop executable compilation failed!
    exit /b %ERRORLEVEL%
)

echo.
echo [5/5] Cleaning up temporary build directories...
if exist "app\dist" (
    rmdir /s /q "app\dist"
    echo ✔ Deleted temporary app\dist directory
)

if exist "..\ividsmusic_release_temp" (
    rmdir /s /q "..\ividsmusic_release_temp"
    echo ✔ Deleted temporary release directory
)

echo.
echo ============================================================
echo   SUCCESS: All platform binaries compiled, updated, and cleaned!
echo   - IVIDSMusic_Mobile.apk / IVIDSMusic.apk
echo   - IVIDSMusic_TV.apk
echo   - IVIDSMusic_PC.exe
echo ============================================================
