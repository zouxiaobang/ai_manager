# 查询并结束占用端口的进程（默认 5173，Vite 开发服务）
param(
    [int]$Port = 5173,
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
    Write-Host "端口 $Port 当前无进程监听。"
    exit 0
}

Write-Host "端口 $Port 占用："
foreach ($procId in $processIds) {
    $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
    if ($proc) {
        Write-Host "  PID $procId  $($proc.ProcessName)  $($proc.Path)"
    } else {
        Write-Host "  PID $procId"
    }
}

if ($DryRun) {
    Write-Host "DryRun：未结束进程。"
    exit 0
}

foreach ($procId in $processIds) {
    Write-Host "结束 PID $procId ..."
    & taskkill.exe /PID $procId /F
}

Write-Host "完成，端口 $Port 已释放。"
