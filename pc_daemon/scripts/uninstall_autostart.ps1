$ErrorActionPreference = "Stop"

$TaskName = "AIManagerDaemon"

Unregister-ScheduledTask -TaskName $TaskName -Confirm:$false -ErrorAction SilentlyContinue
Write-Host "Autostart removed: $TaskName"
