@echo off
cd /d "%~dp0"
title Elestial Jumpscare Mod - Multi-Client Test Launcher (5 Players)
echo ====================================================================
echo 🎮 MEMBUKA 5 CLIENT MINECRAFT FORGE UNTUK TES MULTIPLAYER 🎮
echo ====================================================================

if exist "..\AMONGUS-GTW\gradle\wrapper\gradle-wrapper.jar" (
    if not exist "gradle\wrapper\gradle-wrapper.jar" (
        if not exist "gradle\wrapper" mkdir "gradle\wrapper"
        copy /Y "..\AMONGUS-GTW\gradle\wrapper\gradle-wrapper.jar" "gradle\wrapper\gradle-wrapper.jar" > nul
    )
)

if exist "run\mods\voicechat*.jar" del /f /q "run\mods\voicechat*.jar" > nul 2>&1
if exist "run2\mods\voicechat*.jar" del /f /q "run2\mods\voicechat*.jar" > nul 2>&1
if exist "run3\mods\voicechat*.jar" del /f /q "run3\mods\voicechat*.jar" > nul 2>&1
if exist "run4\mods\voicechat*.jar" del /f /q "run4\mods\voicechat*.jar" > nul 2>&1
if exist "run5\mods\voicechat*.jar" del /f /q "run5\mods\voicechat*.jar" > nul 2>&1
echo [VoiceChat] Simple Voice Chat aktif terintegrasi via Gradle!

echo [Sync] Menyinkronkan daftar jumpscare dan cache ke semua client...
if exist "run\config\jumpscares.json" (
    if not exist "run2\config" mkdir "run2\config"
    if not exist "run3\config" mkdir "run3\config"
    if not exist "run4\config" mkdir "run4\config"
    if not exist "run5\config" mkdir "run5\config"
    copy /Y "run\config\jumpscares.json" "run2\config\jumpscares.json" > nul 2>&1
    copy /Y "run\config\jumpscares.json" "run3\config\jumpscares.json" > nul 2>&1
    copy /Y "run\config\jumpscares.json" "run4\config\jumpscares.json" > nul 2>&1
    copy /Y "run\config\jumpscares.json" "run5\config\jumpscares.json" > nul 2>&1
)
if exist "run\jumpscares_cache" (
    if not exist "run2\jumpscares_cache" mkdir "run2\jumpscares_cache"
    if not exist "run3\jumpscares_cache" mkdir "run3\jumpscares_cache"
    if not exist "run4\jumpscares_cache" mkdir "run4\jumpscares_cache"
    if not exist "run5\jumpscares_cache" mkdir "run5\jumpscares_cache"
    xcopy /Y /D /Q "run\jumpscares_cache\*.*" "run2\jumpscares_cache\" > nul 2>&1
    xcopy /Y /D /Q "run\jumpscares_cache\*.*" "run3\jumpscares_cache\" > nul 2>&1
    xcopy /Y /D /Q "run\jumpscares_cache\*.*" "run4\jumpscares_cache\" > nul 2>&1
    xcopy /Y /D /Q "run\jumpscares_cache\*.*" "run5\jumpscares_cache\" > nul 2>&1
)


echo [1/5] Membuka Client 1 (Host / Dev)...
start "MC Client 1 - Host" cmd /k "cd /d ""%~dp0"" && .\gradlew.bat runClient"

echo Menunggu 8 detik sebelum membuka Client 2...
ping 127.0.0.1 -n 9 > nul 2>&1

echo [2/5] Membuka Client 2 (Target 1 / Victim_Test)...
start "MC Client 2 - Victim_Test" cmd /k "cd /d ""%~dp0"" && .\gradlew.bat runClient2"

echo Menunggu 8 detik sebelum membuka Client 3...
ping 127.0.0.1 -n 9 > nul 2>&1

echo [3/5] Membuka Client 3 (Target 2 / Victim_Test2)...
start "MC Client 3 - Victim_Test2" cmd /k "cd /d ""%~dp0"" && .\gradlew.bat runClient3"

echo Menunggu 8 detik sebelum membuka Client 4...
ping 127.0.0.1 -n 9 > nul 2>&1

echo [4/5] Membuka Client 4 (Target 3 / Victim_Test3)...
start "MC Client 4 - Victim_Test3" cmd /k "cd /d ""%~dp0"" && .\gradlew.bat runClient4"

echo Menunggu 8 detik sebelum membuka Client 5...
ping 127.0.0.1 -n 9 > nul 2>&1

echo [5/5] Membuka Client 5 (Target 4 / Victim_Test4)...
start "MC Client 5 - Victim_Test4" cmd /k "cd /d ""%~dp0"" && .\gradlew.bat runClient5"

echo.
echo ====================================================================
echo ✔ 5 Client Minecraft Forge sedang dijalankan!
echo 💡 Petunjuk Tes Multi-Target:
echo 1. Di Client 1: Buat Singleplayer World lalu klik "Open to LAN".
echo 2. Di Client 2, 3, 4, dan 5: Masuk ke Multiplayer -> Direct Connect / LAN -> Join World Client 1.
echo 3. Di Client 1: Buka GUI (Tombol J), sekarang Anda bisa memilih hingga 5 target sekaligus!
echo 4. Klik "TEMBAK SEMUA!" atau "TEMBAK AUDIO!" untuk menguji reaksi semua pemain bersamaan! 😱
echo ====================================================================

pause
