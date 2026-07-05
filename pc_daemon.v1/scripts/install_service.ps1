$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
$PythonExe = Join-Path $Root ".venv\Scripts\python.exe"
$ServerPy = Join-Path $Root "server.py"
$LogDir = Join-Path $Root "logs"
$ServiceName = "NetEaseMediaBridge"

# 创建日志目录
if (-not (Test-Path $LogDir)) {
    New-Item -ItemType Directory -Path $LogDir | Out-Null
}

# 检查虚拟环境
if (-not (Test-Path $PythonExe)) {
    Write-Host "[ERROR] Virtual env not found at: $PythonExe"
    Write-Host "Run: cd pc_daemon; py -3.11 -m venv .venv; .\.venv\Scripts\Activate.ps1; pip install -r requirements.txt"
    exit 1
}

# 检查 nssm
$nssmPath = (Get-Command nssm -ErrorAction SilentlyContinue).Source
if (-not $nssmPath) {
    Write-Host "[ERROR] nssm not found. Install with: winget install nssm"
    exit 1
}

Write-Host "[1/3] Removing old service (if exists)..."
nssm stop $ServiceName 2>$null | Out-Null
nssm remove $ServiceName confirm 2>$null | Out-Null
Start-Sleep -Seconds 1

Write-Host "[2/3] Registering service '$ServiceName'..."
nssm install $ServiceName $PythonExe $ServerPy
nssm set $ServiceName AppDirectory $Root
nssm set $ServiceName AppStdout (Join-Path $LogDir "daemon.log")
nssm set $ServiceName AppStderr (Join-Path $LogDir "daemon.err.log")
nssm set $ServiceName Start SERVICE_AUTO_START
nssm set $ServiceName AppRestartDelay 5000

Write-Host "[3/3] Starting service..."
nssm start $ServiceName
Start-Sleep -Seconds 2

# 验证状态
$status = nssm status $ServiceName
if ($status -eq "SERVICE_RUNNING") {
    Write-Host "`n[SUCCESS] Service '$ServiceName' is running!"
    Write-Host "  Logs: $LogDir\daemon.log"
    Write-Host "  Test: curl http://localhost:8765/health"
} else {
    Write-Host "[WARNING] Service status: $status"
    Write-Host "Check logs: $LogDir\daemon.err.log"
}
