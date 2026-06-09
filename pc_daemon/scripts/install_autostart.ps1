$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
$Python = Join-Path $Root ".venv\Scripts\pythonw.exe"
$StartScript = Join-Path $Root "scripts\start_background.ps1"
$TaskName = "AIManagerDaemon"

if (-not (Test-Path $Python)) {
    Write-Error "Virtual environment not found. Run setup in pc_daemon first."
}

if (-not (Test-Path $StartScript)) {
    Write-Error "Missing start script: $StartScript"
}

$Action = New-ScheduledTaskAction `
    -Execute "powershell.exe" `
    -Argument "-NoProfile -ExecutionPolicy Bypass -File `"$StartScript`"" `
    -WorkingDirectory $Root

$Trigger = New-ScheduledTaskTrigger -AtLogOn -User $env:USERNAME
$Settings = New-ScheduledTaskSettingsSet `
    -AllowStartIfOnBatteries `
    -DontStopIfGoingOnBatteries `
    -StartWhenAvailable `
    -RestartCount 3 `
    -RestartInterval (New-TimeSpan -Minutes 1)

Register-ScheduledTask `
    -TaskName $TaskName `
    -Action $Action `
    -Trigger $Trigger `
    -Settings $Settings `
    -Description "Start AI Manager PC daemon for ESP32 smart display" `
    -Force | Out-Null

Write-Host "Autostart installed: $TaskName"
Write-Host "The daemon will start in background after you log in."
Write-Host "Log file: $(Join-Path $Root '.runtime\logs\daemon.log')"
