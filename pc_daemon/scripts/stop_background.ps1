$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
$PidFile = Join-Path $Root ".runtime\daemon.pid"

function Stop-ByPidFile {
    if (-not (Test-Path $PidFile)) {
        return $false
    }

    $DaemonPid = [int](Get-Content $PidFile -ErrorAction SilentlyContinue)
    if ($DaemonPid -le 0) {
        Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
        return $false
    }

    $Process = Get-Process -Id $DaemonPid -ErrorAction SilentlyContinue
    if (-not $Process) {
        Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
        return $false
    }

    Stop-Process -Id $DaemonPid -Force
    Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
    Write-Host "Stopped daemon (PID $DaemonPid)."
    return $true
}

if (Stop-ByPidFile) {
    exit 0
}

$Connections = Get-NetTCPConnection -LocalPort 8765 -State Listen -ErrorAction SilentlyContinue
if ($Connections) {
    $Pid = ($Connections | Select-Object -First 1).OwningProcess
    Stop-Process -Id $Pid -Force
    Write-Host "Stopped process listening on port 8765 (PID $Pid)."
    exit 0
}

Write-Host "Daemon is not running."
