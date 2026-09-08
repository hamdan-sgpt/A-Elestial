@echo off
title Elestial Jumpscare Mod (Forge 1.20.1) - Build Jar
echo ====================================================================
echo 🛠️ MEMBUILD FILE FORGE MOD (.JAR) MINECRAFT 1.20.1 🛠️
echo ====================================================================

if exist "..\AMONGUS-GTW\gradle\wrapper\gradle-wrapper.jar" (
    if not exist "gradle\wrapper\gradle-wrapper.jar" (
        if not exist "gradle\wrapper" mkdir "gradle\wrapper"
        copy /Y "..\AMONGUS-GTW\gradle\wrapper\gradle-wrapper.jar" "gradle\wrapper\gradle-wrapper.jar" > nul
    )
)

echo 📦 Menjalankan perintah build Forge mod (clean build)...
call gradlew.bat clean build

echo.
echo ====================================================================
if exist "build\libs\elestialjumpscare-1.0.0.jar" (
    echo ✔ SUCCESS! File Forge Mod JAR berhasil dibuat di:
    echo 📂 D:\codingan\A-Elestial\build\libs\elestialjumpscare-1.0.0.jar
) else if exist "build\libs\elestial-jumpscare-1.0.0.jar" (
    echo ✔ SUCCESS! File Forge Mod JAR berhasil dibuat di:
    echo 📂 D:\codingan\A-Elestial\build\libs\elestial-jumpscare-1.0.0.jar
) else (
    echo ❌ Build selesai. Silakan periksa log di atas jika ada error.
)
echo ====================================================================

pause
