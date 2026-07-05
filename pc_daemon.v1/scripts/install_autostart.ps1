$ErrorActionPreference = "Stop"

$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$RunScript = Join-Path $Root "scripts\run_daemon.ps1"
$TaskName = "NetEaseMediaBridge"
$User = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name

if (-not (Test-Path $RunScript)) {
    throw "Missing run script: $RunScript"
}

$Action = New-ScheduledTaskAction -Execute "powershell.exe" -Argument "-NoProfile -ExecutionPolicy Bypass -File `"$RunScript`""
$Trigger = New-ScheduledTaskTrigger -AtLogOn -User $User
$Settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable -RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 1)
$Principal = New-ScheduledTaskPrincipal -UserId $User -LogonType Interactive -RunLevel LeastPrivilege

Register-ScheduledTask -TaskName $TaskName -Action $Action -Trigger $Trigger -Settings $Settings -Principal $Principal -Force | Out-Null

Write-Host "Scheduled task '$TaskName' installed for $User"
Write-Host "It will run: $RunScript"
Write-Host "Test now with: Start-ScheduledTask -TaskName $TaskName"
