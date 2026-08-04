@echo off
echo ============================================================
echo   IVIDS Music - Local Multi-Platform Build Script
echo ============================================================

echo [1/6] Updating versionCode in app/build.gradle.kts...
node -e "const fs = require('fs'); const p = 'app/build.gradle.kts'; if (fs.existsSync(p)) { let c = fs.readFileSync(p, 'utf8'); c = c.replace(/versionCode\s*=\s*(\d+)/, (m, v) => 'versionCode = ' + (parseInt(v, 10) + 1)); fs.writeFileSync(p, c, 'utf8'); console.log('✔ versionCode updated.'); }"

echo.
echo [2/6] Compiling Mobile Release APK...
call gradlew.bat assembleMobileRelease
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Mobile APK compilation failed!
    exit /b %ERRORLEVEL%
)

echo.
echo [3/6] Compiling TV Release APK...
call gradlew.bat assembleTvRelease
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] TV APK compilation failed!
    exit /b %ERRORLEVEL%
)

echo.
echo [4/6] Compiling PC Desktop Executable...
call npm --prefix app run dist
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] PC Desktop executable compilation failed!
    exit /b %ERRORLEVEL%
)

echo.
echo [5/6] Copying compiled binaries to project root...
if exist "app\build\outputs\apk\mobile\release\app-mobile-release.apk" (
    copy /Y "app\build\outputs\apk\mobile\release\app-mobile-release.apk" "IVIDSMusic_Mobile.apk"
    copy /Y "app\build\outputs\apk\mobile\release\app-mobile-release.apk" "IVIDSMusic.apk"
    echo ✔ Updated IVIDSMusic_Mobile.apk and IVIDSMusic.apk
)

if exist "app\build\outputs\apk\tv\release\app-tv-release.apk" (
    copy /Y "app\build\outputs\apk\tv\release\app-tv-release.apk" "IVIDSMusic_TV.apk"
    echo ✔ Updated IVIDSMusic_TV.apk
)

for %%F in (app\dist\*.exe) do (
    copy /Y "%%F" "IVIDSMusic_PC.exe"
    echo ✔ Updated IVIDSMusic_PC.exe
)

echo.
echo [6/6] Cleaning up temporary build directories...
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
