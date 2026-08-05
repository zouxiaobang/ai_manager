<#
.SYNOPSIS
    监听后端源码变化，自动重新构建并重启后端（任务端口 808N）。

.DESCRIPTION
    worktree 开发用：后端代码（.java / .yml / .xml / .properties）一有修改，
    自动执行 mvn clean install -DskipTests 并重启 spring-boot:run，无需手动重启。
    端口与 dev.ps1 约定一致：任务 N -> 808N。

.PARAMETER task
    任务编号（1..9），后端端口 = 8080 + task。

.PARAMETER Skip
    跳过首次构建，直接启动后端（已构建过时用）。

.USAGE
    .\auto-restart.ps1 1        # 任务 1 -> 8081
    .\auto-restart.ps1 1 -Skip  # 跳过首次构建直接启动
#>
param(
    [int]$task = 1,
    [switch]$Skip
)

$ErrorActionPreference = "Stop"
$RepoRoot = $PSScriptRoot   # 即 admin-backend
$bp = 8080 + $task
# 日志放系统临时目录（不在 target/ 内，避免 mvn clean 删除被占用日志导致构建失败）
$logDir = Join-Path $env:TEMP "ai-manager-backend"
$logFile = Join-Path $logDir "backend-$bp.log"
if (-not (Test-Path $logDir)) { New-Item -ItemType Directory -Force $logDir | Out-Null }

$script:proc = $null
$script:lastRebuildAt = [DateTime]::MinValue

function Invoke-Build {
    Write-Host "`n==> [$(Get-Date -Format HH:mm:ss)] 重新构建后端..." -ForegroundColor Cyan
    Push-Location $RepoRoot
    # 捕获 mvn 输出后再逐行显示，避免 stdout 泄漏进函数返回值导致 if(Invoke-Build) 恒真
    $output = & mvn clean install -DskipTests 2>&1
    $ok = $LASTEXITCODE -eq 0
    Pop-Location
    $output | ForEach-Object { Write-Host $_ }
    if (-not $ok) {
        Write-Host "==> 构建失败，等待下次变更。" -ForegroundColor Red
    }
    return $ok
}

function Stop-Backend {
    if ($script:proc -and -not $script:proc.HasExited) {
        Write-Host "==> 停止旧后端 (PID $($script:proc.Id))" -ForegroundColor Yellow
        & taskkill /F /T /PID $script:proc.Id 2>$null | Out-Null
        Start-Sleep -Seconds 2
    }
    $script:proc = $null
}

function Start-Backend {
    Stop-Backend
    Write-Host "==> 启动后端 http://localhost:$bp（日志 $logFile）" -ForegroundColor Cyan
    if (-not (Test-Path $logDir)) { New-Item -ItemType Directory -Force $logDir | Out-Null }
    # 经 cmd 包装，重定向在 cmd 内完成，规避 PS5.1 中 -NoNewWindow 与 -Redirect 的组合限制
    $cmd = "mvn -pl admin-server spring-boot:run -Dspring-boot.run.arguments=--server.port=$bp > `"$logFile`" 2>&1"
    Push-Location $RepoRoot
    $script:proc = Start-Process -FilePath "cmd.exe" -ArgumentList @("/c", $cmd) -PassThru -WindowStyle Hidden
    Pop-Location
}

function Get-NewestSource {
    Get-ChildItem -Path $RepoRoot -Recurse -File -ErrorAction SilentlyContinue |
        Where-Object {
            $_.Extension -in @(".java", ".yml", ".yaml", ".xml", ".properties") -and
            $_.FullName -notmatch "\\target\\" -and
            $_.FullName -notmatch "\\.mvn\\"
        } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
}

if (-not $Skip) {
    if (-not (Invoke-Build)) {
        Write-Host "首次构建失败，仍启动后端（可能沿用旧产物）。" -ForegroundColor DarkYellow
    }
}
Start-Backend

Write-Host "==> 开始监听后端源码变更（Ctrl+C 退出）。" -ForegroundColor Green
$script:lastRebuildAt = Get-Date

while ($true) {
    $newest = Get-NewestSource
    if ($newest -and $newest.LastWriteTime -gt $script:lastRebuildAt.AddSeconds(2)) {
        if (Invoke-Build) {
            Start-Backend
        }
        $script:lastRebuildAt = Get-Date
    }
    Start-Sleep -Seconds 1
}
