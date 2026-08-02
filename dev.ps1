<#
.SYNOPSIS
    在当前 checkout（主 checkout 或 worktree）里启动后端/前端 dev 服务，独立端口避免冲突。
    用法（全小写，位置参数）：
        .\dev.ps1 1 backend       # 任务 1 后端 -> 8081
        .\dev.ps1 1 frontend      # 任务 1 前端 -> 5174（代理自动指向 8081）
        .\dev.ps1 0 backend -install   # 主 checkout：先 install 再起 8080

.PARAMETER task
    0 = 主 checkout（8080/5173）；N = 任务 N（808N/517N）。

.PARAMETER side
    backend / frontend / both。

.PARAMETER install
    后端启动前先 mvn clean install -DskipTests。
#>
param(
    [int]$task = 0,
    [ValidateSet("backend", "frontend", "both")]
    [string]$side = "both",
    [switch]$install
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

if ($task -lt 0 -or $task -gt 9) { throw "task 必须在 0..9（0 = 主 checkout）。" }

$bp = 8080 + $task
$fp = 5173 + $task

if ($side -ne "frontend") {
    Set-Location "$RepoRoot\admin-backend"
    if ($install) {
        Write-Host "==> mvn clean install -DskipTests" -ForegroundColor Cyan
        mvn clean install -DskipTests
        if ($LASTEXITCODE -ne 0) { throw "mvn install 失败（exit $LASTEXITCODE）" }
    }
    if ($task -eq 0) {
        Write-Host "==> 后端: http://localhost:$bp" -ForegroundColor Cyan
        mvn -pl admin-server spring-boot:run
    } else {
        Write-Host "==> 后端(任务 $task): http://localhost:$bp" -ForegroundColor Cyan
        mvn -pl admin-server spring-boot:run "-Dspring-boot.run.arguments=--server.port=$bp"
    }
}

if ($side -ne "backend") {
    Set-Location "$RepoRoot\admin-web"
    if ($task -gt 0) {
        $env:vite_api_target = "http://127.0.0.1:$bp"
        Write-Host "==> vite_api_target=$env:vite_api_target（前端代理指向任务后端）" -ForegroundColor Cyan
    }
    if ($task -eq 0) {
        Write-Host "==> 前端: http://localhost:$fp" -ForegroundColor Cyan
        npm run dev
    } else {
        Write-Host "==> 前端(任务 $task): http://localhost:$fp" -ForegroundColor Cyan
        npm run dev -- --port $fp
    }
}
