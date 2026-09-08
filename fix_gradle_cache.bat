@echo off
title Fix Gradle Cache & Stop Daemons
echo ====================================================================
echo 🧹 MEMBERSIHKAN DAEMON & CACHE GRADLE YANG TERKUNCI 🧹
echo ====================================================================

echo [1/3] Menghentikan semua Gradle Daemon yang sedang berjalan...
call gradlew.bat --stop

echo [2/3] Membersihkan cache transforms temporary yang terkunci...
if exist "D:\codingan\.gradle\caches\8.8\transforms" (
    rmdir /S /Q "D:\codingan\.gradle\caches\8.8\transforms" 2>nul
)
if exist ".gradle\transforms" (
    rmdir /S /Q ".gradle\transforms" 2>nul
)

echo [3/3] Menjalankan Minecraft Client tanpa daemon terkunci...
call gradlew.bat runClient --no-daemon

pause
