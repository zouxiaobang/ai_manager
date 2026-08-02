<#
.SYNOPSIS
    清理 .claude/worktrees/ 下的 worktree（自动解锁、删目录、删分支）。
    安全规则：
      1. 有未提交改动 → 默认跳过（避免丢工作），-force 才强制
      2. 被锁定（locked）→ 可能 claude 会话在用，默认跳过，-force 强制
      3. 只处理 .claude/worktrees/，绝不触碰主 checkout
    用法（全小写）：
        .\clean.ps1               # 清闲置
        .\clean.ps1 -keep feat-a  # 保留 feat-a
        .\clean.ps1 -force        # 强制（慎用，会丢未提交改动）
#>
param(
    [string[]]$keep = @(),
    [switch]$force
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $RepoRoot
$RootNorm = $RepoRoot.Replace('\', '/')

# 解析 git worktree list --porcelain
$entries = @()
$cur = $null
foreach ($line in @(git worktree list --porcelain)) {
    if ($line.Trim() -eq '') {
        if ($cur) { $entries += $cur; $cur = $null }
        continue
    }
    if ($line -like 'worktree *') {
        $cur = @{ Path = $line.Substring("worktree ".Length).Trim(); Branch = $null; Locked = $false }
    }
    elseif ($line -like 'branch refs/heads/*') {
        $cur.Branch = $line.Substring("branch refs/heads/".Length).Trim()
    }
    elseif ($line -like 'locked*') {
        $cur.Locked = $true
    }
}

$removed = 0
$skipped = 0
foreach ($e in $entries) {
    $path = $e.Path
    $name = Split-Path $path -Leaf
    if (-not $path.StartsWith("$RootNorm/.claude/worktrees", [System.StringComparison]::OrdinalIgnoreCase)) { continue }
    if ($keep -contains $name) { Write-Host "保留: $name" -ForegroundColor Yellow; continue }

    $dirty = @(git -C $path status --porcelain)
    if ($dirty.Count -gt 0 -and -not $force) {
        Write-Host "跳过(有改动，用 -force 可强制): $name" -ForegroundColor Yellow
        $skipped++
        continue
    }
    if ($e.Locked -and -not $force) {
        Write-Host "跳过(被锁定，claude 会话可能还在跑，先退出会话或用 -force): $name" -ForegroundColor Yellow
        $skipped++
        continue
    }

    git worktree unlock $path 2>$null
    git worktree remove $path --force | Out-Null
    if ($e.Branch) { git branch -D $e.Branch 2>$null | Out-Null }
    if ($dirty.Count -gt 0) {
        Write-Host "强制删除(含 $($dirty.Count) 个改动文件): $name (分支 $($e.Branch))" -ForegroundColor Red
    } else {
        Write-Host "已删除: $name (分支 $($e.Branch))" -ForegroundColor Green
    }
    $removed++
}

Write-Host "`n完成：删除 $removed 个，跳过 $skipped 个。"
