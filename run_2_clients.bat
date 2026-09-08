@echo off
cd /d "%~dp0"
title Elestial Jumpscare Mod - Multi-Client Test Launcher (2 Players)
echo ====================================================================
echo 🎮 MEMBUKA 2 CLIENT MINECRAFT FORGE UNTUK TES MULTIPLAYER 🎮
echo ====================================================================

if exist "..\AMONGUS-GTW\gradle\wrapper\gradle-wrapper.jar" (
    if not exist "gradle\wrapper\gradle-wrapper.jar" (
        if not exist "gradle\wrapper" mkdir "gradle\wrapper"
        copy /Y "..\AMONGUS-GTW\gradle\wrapper\gradle-wrapper.jar" "gradle\wrapper\gradle-wrapper.jar" > nul
    )
)

if exist "run\mods\voicechat*.jar" del /f /q "run\mods\voicechat*.jar" > nul 2>&1
if exist "run2\mods\voicechat*.jar" del /f /q "run2\mods\voicechat*.jar" > nul 2>&1
echo [VoiceChat] Simple Voice Chat aktif terintegrasi via Gradle!


echo [1/2] Membuka Client 1 (Host / Sender)...
start "MC Client 1 - Host" cmd /k "cd /d ""%~dp0"" && .\gradlew.bat runClient"

echo Menunggu 8 detik sebelum membuka Client 2...
ping 127.0.0.1 -n 9 > nul 2>&1

echo [2/2] Membuka Client 2 (Target / Victim_Test)...
start "MC Client 2 - Victim_Test" cmd /k "cd /d ""%~dp0"" && .\gradlew.bat runClient2"

echo.
echo ====================================================================
echo ✔ 2 Client Minecraft Forge sedang dijalankan!
echo 💡 Petunjuk Tes Multiplayer:
echo 1. Di Client 1: Buat Singleplayer World lalu klik "Open to LAN".
echo 2. Di Client 2: Masuk ke Multiplayer -> Direct Connect / LAN -> Join World Client 1.
echo 3. Di Client 1: Ketik di chat: !jumpscare Victim_Test spooky
echo 4. Lihat di layar Client 2: Efek Jumpscare + Audio Scream akan muncul! 😱
echo ====================================================================

pause
