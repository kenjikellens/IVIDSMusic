@echo off
setlocal enabledelayedexpansion

echo ============================================================
echo   IVIDS Music - Native Multiplatform Build Script
echo ============================================================

echo [1/5] Updating versionCode in composeApp\build.gradle.kts...
powershell -NoProfile -Command "$p='composeApp\build.gradle.kts'; if (Test-Path $p) { $c = Get-Content $p -Raw; $c = [regex]::Replace($c, 'versionCode\s*=\s*(\d+)', { param($m) 'versionCode = ' + ([int]$m.Groups[1].Value + 1) }); Set-Content $p $c -NoNewline -Encoding UTF8; Write-Host 'OK versionCode updated.' }"

echo.
echo [2/5] Compiling Mobile Release APK...
call gradlew.bat :composeApp:assembleMobileRelease
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Mobile APK compilation failed!
    exit /b %ERRORLEVEL%
)
if exist "composeApp\build\outputs\apk\mobile\release\composeApp-mobile-release.apk" (
    copy /Y "composeApp\build\outputs\apk\mobile\release\composeApp-mobile-release.apk" "IVIDSMusic_Mobile.apk" >nul
    copy /Y "composeApp\build\outputs\apk\mobile\release\composeApp-mobile-release.apk" "IVIDSMusic.apk" >nul
    echo OK Mobile APK compiled and copied to root directory!
)

echo.
echo [3/5] Compiling TV Release APK...
call gradlew.bat :composeApp:assembleTvRelease
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] TV APK compilation failed!
    exit /b %ERRORLEVEL%
)
if exist "composeApp\build\outputs\apk\tv\release\composeApp-tv-release.apk" (
    copy /Y "composeApp\build\outputs\apk\tv\release\composeApp-tv-release.apk" "IVIDSMusic_TV.apk" >nul
    echo OK TV APK compiled and copied to root directory!
)

echo.
echo [4/5] Building Windows Desktop App Image & Installer (.exe)...
call gradlew.bat :composeApp:createDistributable
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Windows Desktop distributable build failed!
    exit /b %ERRORLEVEL%
)
if exist "composeApp\build\compose\binaries\main\app\IVIDS Music\IVIDS Music.exe" (
    if exist "dist\IVIDSMusic_PC" rmdir /s /q "dist\IVIDSMusic_PC"
    mkdir "dist\IVIDSMusic_PC" >nul 2>nul
    xcopy /E /I /Y /Q "composeApp\build\compose\binaries\main\app\IVIDS Music\*" "dist\IVIDSMusic_PC\" >nul
    echo OK Standalone Desktop app ready: dist\IVIDSMusic_PC\IVIDS Music.exe
)

call gradlew.bat :composeApp:packageExe
if %ERRORLEVEL% EQU 0 (
    if exist "composeApp\build\compose\binaries\main\exe\*.exe" (
        for %%f in ("composeApp\build\compose\binaries\main\exe\*.exe") do (
            copy /Y "%%f" "IVIDSMusic_PC.exe" >nul
        )
        echo OK Windows installer packaged: IVIDSMusic_PC.exe
    )
) else (
    echo [WARN] Windows installer packaging skipped or failed; standalone app remains ready at dist\IVIDSMusic_PC\IVIDS Music.exe
)

echo.
echo [5/5] Cleaning temporary staging directories...
if exist "..\ividsmusic_release_temp" (
    rmdir /s /q "..\ividsmusic_release_temp"
    echo OK Deleted temporary release directory
)

echo.
echo ============================================================
echo   SUCCESS: All native binaries built!
echo   - Mobile APK : IVIDSMusic_Mobile.apk / IVIDSMusic.apk
echo   - TV APK     : IVIDSMusic_TV.apk
echo   - Desktop PC : dist\IVIDSMusic_PC\IVIDS Music.exe / IVIDSMusic_PC.exe
echo ============================================================
