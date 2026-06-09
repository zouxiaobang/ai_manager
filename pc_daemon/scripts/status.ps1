$Root = Split-Path -Parent $PSScriptRoot
$PidFile = Join-Path $Root ".runtime\daemon.pid"
$LogFile = Join-Path $Root ".runtime\logs\daemon.log"
$TaskName = "AIManagerDaemon"

Write-Host "AI Manager Daemon status"
Write-Host "------------------------"

if (Test-Path $PidFile) {
    $DaemonPid = Get-Content $PidFile -ErrorAction SilentlyContinue
    if ($DaemonPid -and (Get-Process -Id $DaemonPid -ErrorAction SilentlyContinue)) {
        Write-Host "Running: yes (PID $DaemonPid)"
    } else {
        Write-Host "Running: no (stale pid file)"
    }
} else {
    $Connection = Get-NetTCPConnection -LocalPort 8765 -State Listen -ErrorAction SilentlyContinue
    if ($Connection) {
        Write-Host "Running: yes (PID $($Connection.OwningProcess), no pid file)"
    } else {
        Write-Host "Running: no"
    }
}

$Task = Get-ScheduledTask -TaskName $TaskName -ErrorAction SilentlyContinue
if ($Task) {
    Write-Host "Autostart: enabled ($TaskName)"
} else {
    Write-Host "Autostart: disabled"
}

Write-Host "Log file: $LogFile"
if (Test-Path $LogFile) {
    Write-Host ""
    Write-Host "Last log lines:"
    Get-Content $LogFile -Tail 8
}
