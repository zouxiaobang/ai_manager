<#
.SYNOPSIS
    在当前 checkout（主 checkout 或 worktree）里启动后端/前端开发服务，使用独立端口避免冲突。

.DESCRIPTION
    端口约定：
        Task 0（默认，主 checkout） : 后端 8080 / 前端 5173
        Task N（worktree 任务 N）    : 后端 808N / 前端 517N
    前端在 Task>0 时会设置 VITE_API_TARGET 指向对应的后端端口（vite.config.ts 读取该变量）。
    建议后端/前端分别放在两个终端窗格里并行运行（-Side backend / -Side frontend 分开跑）。

.PARAMETER Task
    任务编号 0..9；0 = 主 checkout，N = worktree 任务 N。

.PARAMETER Side
    backend / frontend / both（默认 both；注意 both 是顺序执行：先后端前台阻塞，停止后才起前端）。

.PARAMETER Install
    后端启动前先执行 mvn clean install -DskipTests（首次或在 worktree 里首跑时需要）。

.EXAMPLE
    .\dev.ps1 -Task 1 -Side backend     # worktree 任务 1：起后端 8081
    .\dev.ps1 -Task 1 -Side frontend    # worktree 任务 1：起前端 5174（代理到 8081）
    .\dev.ps1 -Install -Side backend    # 主 checkout：install 后起后端 8080
#>
param(
    [int]$Task = 0,
    [ValidateSet("backend", "frontend", "both")]
    [string]$Side = "both",
    [switch]$Install
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

if ($Task -lt 0 -or $Task -gt 9) {
    throw "Task 必须在 0..9 之间（0 = 主 checkout）。"
}

$BackendPort = 8080 + $Task
$FrontendPort = 5173 + $Task

if ($Side -ne "frontend") {
    Set-Location "$RepoRoot\admin-backend"
    if ($Install) {
        Write-Host "==> mvn clean install -DskipTests" -ForegroundColor Cyan
        mvn clean install -DskipTests
        if ($LASTEXITCODE -ne 0) { throw "mvn install 失败（exit $LASTEXITCODE）" }
    }
    if ($Task -eq 0) {
        Write-Host "==> 后端启动: admin-server @ http://localhost:$BackendPort" -ForegroundColor Cyan
        mvn -pl admin-server spring-boot:run
    } else {
        Write-Host "==> 后端启动: admin-server @ http://localhost:$BackendPort (worktree 任务 $Task)" -ForegroundColor Cyan
        mvn -pl admin-server spring-boot:run "-Dspring-boot.run.arguments=--server.port=$BackendPort"
    }
}

if ($Side -ne "backend") {
    Set-Location "$RepoRoot\admin-web"
    if ($Task -gt 0) {
        $env:VITE_API_TARGET = "http://127.0.0.1:$BackendPort"
        Write-Host "==> VITE_API_TARGET=$env:VITE_API_TARGET（前端代理指向任务后端）" -ForegroundColor Cyan
    }
    if ($Task -eq 0) {
        Write-Host "==> 前端启动: vite @ http://localhost:$FrontendPort" -ForegroundColor Cyan
        npm run dev
    } else {
        Write-Host "==> 前端启动: vite @ http://localhost:$FrontendPort (worktree 任务 $Task)" -ForegroundColor Cyan
        npm run dev -- --port $FrontendPort
    }
}
