$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
$Python = Join-Path $Root ".venv\Scripts\pythonw.exe"
$PidFile = Join-Path $Root ".runtime\daemon.pid"
$LogFile = Join-Path $Root ".runtime\logs\daemon.log"

if (-not (Test-Path $Python)) {
    Write-Error "Virtual environment not found. Run: python -m venv .venv && pip install -r requirements.txt"
}

if (Test-Path $PidFile) {
    $ExistingPid = Get-Content $PidFile -ErrorAction SilentlyContinue
    if ($ExistingPid -and (Get-Process -Id $ExistingPid -ErrorAction SilentlyContinue)) {
        Write-Host "Daemon already running (PID $ExistingPid)."
        Write-Host "Log file: $LogFile"
        exit 0
    }
}

$PortInUse = Get-NetTCPConnection -LocalPort 8765 -State Listen -ErrorAction SilentlyContinue
if ($PortInUse) {
    Write-Error "Port 8765 is already in use. Run scripts\stop_background.ps1 first."
}

New-Item -ItemType Directory -Force -Path (Split-Path $LogFile) | Out-Null

Start-Process `
    -FilePath $Python `
    -ArgumentList "main.py" `
    -WorkingDirectory $Root `
    -WindowStyle Hidden

Start-Sleep -Seconds 2

if (Test-Path $PidFile) {
    $DaemonPid = Get-Content $PidFile
    Write-Host "Daemon started in background (PID $DaemonPid)."
} else {
    Write-Host "Daemon start requested. Check log if service is unavailable:"
}

Write-Host "Log file: $LogFile"
