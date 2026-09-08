@echo off
cd /d "%~dp0"
title Elestial Jumpscare Mod - Run Client Test
echo ====================================================================
echo 🎮 MEMBUKA MINECRAFT CLIENT UNTUK TES MOD JUMPSCARE 🎮
echo ====================================================================

if exist "..\AMONGUS-GTW\gradle\wrapper\gradle-wrapper.jar" (
    if not exist "gradle\wrapper\gradle-wrapper.jar" (
        if not exist "gradle\wrapper" mkdir "gradle\wrapper"
        copy /Y "..\AMONGUS-GTW\gradle\wrapper\gradle-wrapper.jar" "gradle\wrapper\gradle-wrapper.jar" > nul
    )
)

if exist "run\mods\voicechat*.jar" del /f /q "run\mods\voicechat*.jar" > nul 2>&1
echo [VoiceChat] Simple Voice Chat aktif terintegrasi via Gradle!

echo 🚀 Memulai Gradle Client...
call gradlew.bat runClient

pause
