# 查询并结束占用端口的进程（默认 8080，admin-server）
# Query and kill processes listening on a TCP port.
param(
    [int]$Port = 8080,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

function Get-ListeningProcessIds {
    param([int]$LocalPort)

    $found = @()
    try {
        $conns = @(Get-NetTCPConnection -LocalPort $LocalPort -State Listen -ErrorAction Stop)
        $found = $conns | ForEach-Object { $_.OwningProcess } | Where-Object { $_ -gt 0 } | Select-Object -Unique
    } catch {
        $needle = ":$LocalPort "
        $found = netstat -ano |
            Select-String $needle |
            Select-String "LISTENING" |
            ForEach-Object {
                if ($_.Line -match '\s+(\d+)\s*$') { [int]$Matches[1] }
            } |
            Select-Object -Unique
    }

    return @($found)
}

$processIds = @(Get-ListeningProcessIds -LocalPort $Port)

if ($processIds.Count -eq 0) {
    Write-Host "No process is listening on port $Port."
    exit 0
}

Write-Host "Port $Port is in use by:"
foreach ($procId in $processIds) {
    $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
    if ($proc) {
        Write-Host "  PID $procId  $($proc.ProcessName)  $($proc.Path)"
    } else {
        Write-Host "  PID $procId  (process details unavailable)"
    }
}

if ($DryRun) {
    Write-Host "DryRun: no process was killed."
    exit 0
}

foreach ($procId in $processIds) {
    Write-Host "Killing PID $procId ..."
    & taskkill.exe /PID $procId /F
}

Write-Host "Done. Port $Port should be free now."
