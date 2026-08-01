<#
.SYNOPSIS
    在 Windows Terminal 里一键打开 4 窗格开发布局：
        左列（整高） : Claude worktree 会话 A（任务 A）
        右列 上       : Claude worktree 会话 B（任务 B）
        右列 中       : 后端 dev（主 checkout，8080）
        右列 下       : 前端 dev（主 checkout，5173）

.DESCRIPTION
    依赖：Windows Terminal（wt）+ Claude CLI（均已安装）。
    布局采用顺序分屏（右列 B/后端/前端 三行），保证各版本 Windows Terminal 兼容；
    如需严格 2x2 分屏，把文件末尾注释里的 move-focus 变体替换掉对应命令。

.PARAMETER TaskA
    Claude worktree 会话 A 的任务名（默认 feat-task-a）。

.PARAMETER TaskB
    Claude worktree 会话 B 的任务名（默认 feat-task-b）。

.EXAMPLE
    .\launch-dev.ps1 -TaskA feat-notebook-search -TaskB fix-rag-pgvector
#>
param(
    [string]$TaskA = "feat-task-a",
    [string]$TaskB = "feat-task-b"
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

if (-not (Get-Command wt -ErrorAction SilentlyContinue)) {
    throw "未找到 Windows Terminal（wt）。请先从 Microsoft Store 安装。"
}

Write-Host "==> 打开 Windows Terminal 4 窗格开发布局" -ForegroundColor Cyan
Write-Host "    Claude A: $TaskA (worktree)" -ForegroundColor Gray
Write-Host "    Claude B: $TaskB (worktree)" -ForegroundColor Gray
Write-Host "    Backend @ 8080 / Frontend @ 5173 (主 checkout)" -ForegroundColor Gray

& wt new-tab --title "Claude A: $TaskA" -d "$RepoRoot" powershell -NoExit -Command "claude --worktree $TaskA" `
    ';' split-pane -H --title "Claude B: $TaskB" -d "$RepoRoot" powershell -NoExit -Command "claude --worktree $TaskB" `
    ';' split-pane -V --title "Backend 8080" -d "$RepoRoot\admin-backend" powershell -NoExit -Command ".\run.ps1" `
    ';' split-pane -V --title "Frontend 5173" -d "$RepoRoot\admin-web" powershell -NoExit -Command "npm run dev"

# ── 严格 2x2 变体（Windows Terminal 较新版本支持 move-focus 命令）───────────────
# & wt new-tab --title "Claude A: $TaskA" -d "$RepoRoot" powershell -NoExit -Command "claude --worktree $TaskA" `
#     ';' split-pane -H --title "Claude B: $TaskB" -d "$RepoRoot" powershell -NoExit -Command "claude --worktree $TaskB" `
#     ';' split-pane -V --title "Backend 8080" -d "$RepoRoot\admin-backend" powershell -NoExit -Command ".\run.ps1" `
#     ';' move-focus left `
#     ';' split-pane -V --title "Frontend 5173" -d "$RepoRoot\admin-web" powershell -NoExit -Command "npm run dev"
