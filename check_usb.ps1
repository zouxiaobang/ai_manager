# Check USB devices with errors
Write-Host "=== USB/SD Devices with Errors ==="
Get-PnpDevice | Where-Object { $_.Status -eq 'Error' } | Select-Object Status, Class, FriendlyName, InstanceId | Format-Table -AutoSize

Write-Host ""
Write-Host "=== All USB Devices ==="
Get-PnpDevice | Where-Object { $_.Class -eq 'USB' -or $_.Class -eq 'DiskDrive' -or $_.Class -eq 'Volume' } | Select-Object Status, Class, FriendlyName | Format-Table -AutoSize

Write-Host ""
Write-Host "=== Unknown USB Devices ==="
Get-PnpDevice | Where-Object { $_.FriendlyName -like '*未知*' -or $_.FriendlyName -like '*Unknown*' -or $_.FriendlyName -like '*设备描述符*' } | Select-Object Status, Class, FriendlyName, InstanceId | Format-Table -AutoSize
