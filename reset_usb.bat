@echo off
echo Resetting USB devices...
echo.

:: Remove all problematic USB devices using pnputil
for /f "tokens=*" %%i in ('pnputil /enum-devices /class USB ^| findstr /C:"Device Descriptor Request Failed"') do (
    echo Found problematic device: %%i
)

echo.
echo Attempting to restart USB controllers...
pnputil /restart-device "USB\ROOT_HUB30"
pnputil /restart-device "USB\ROOT_HUB20"

echo.
echo Done! Please unplug and re-plug your SD card reader.
pause
