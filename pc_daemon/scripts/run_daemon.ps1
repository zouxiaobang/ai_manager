$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
$VenvPython = Join-Path $Root ".venv\Scripts\python.exe"
$Server = Join-Path $Root "server.py"

if (-not (Test-Path $VenvPython)) {
    Write-Host "Virtual env not found. Run: py -3.11 -m venv .venv && pip install -r requirements.txt"
    exit 1
}

$env:MEDIA_DAEMON_HOST = "0.0.0.0"
$env:MEDIA_DAEMON_PORT = "8765"

Set-Location $Root
& $VenvPython $Server
