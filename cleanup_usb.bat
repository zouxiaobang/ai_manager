@echo off
echo === Cleaning up USB and volume mount points ===
echo.

echo 1. Re-enabling automatic mounting of new volumes...
mountvol /E
echo.

echo 2. Removing stale volume mount points...
mountvol /R
echo.

echo 3. Scanning for hardware changes...
pnputil /scan-devices
echo.

echo 4. Checking for USB errors...
pnputil /enum-devices /class USB /problem
echo.

echo === Done! ===
echo Please unplug and re-plug your SD card reader now.
pause
