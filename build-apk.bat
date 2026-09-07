@echo off
setlocal
where gradle >nul 2>nul
if errorlevel 1 (
  echo Gradle n'est pas installe. Installe Android Studio ou Gradle, puis relance ce fichier.
  pause
  exit /b 1
)
gradle --no-daemon assembleDebug
if errorlevel 1 exit /b 1
echo.
echo APK genere : app\build\outputs\apk\debug\app-debug.apk
pause
