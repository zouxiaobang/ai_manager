Write-Host "Removing problematic USB devices..."
Get-PnpDevice | Where-Object { $_.FriendlyName -like '*Device Descriptor Request Failed*' -and $_.Status -eq 'Error' } | ForEach-Object {
    Write-Host "Removing: $($_.FriendlyName) [$($_.InstanceId)]"
    try {
        $_ | Disable-PnpDevice -Confirm:$false -ErrorAction Stop
        $_ | Enable-PnpDevice -Confirm:$false -ErrorAction Stop
        Write-Host "  -> Reset OK"
    } catch {
        Write-Host "  -> Error: $_"
    }
}
Write-Host ""
Write-Host "Done. Please re-plug the SD card reader."
