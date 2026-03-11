@echo off
echo Killing all Android Emulator processes...
taskkill /F /IM qemu-system-x86_64.exe /T 2>nul
taskkill /F /IM qemu-system-arm.exe /T 2>nul
taskkill /F /IM emulator.exe /T 2>nul
taskkill /F /IM adb.exe /T 2>nul
timeout /t 2 /nobreak >nul
echo.
echo Scanning for remaining processes...
for /f "tokens=2" %%a in ('tasklist ^| findstr /i "qemu emulator"') do taskkill /F /PID %%a 2>nul
echo.
echo Done! You can now start the emulator again.
timeout /t 3
