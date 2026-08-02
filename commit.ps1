<#
.SYNOPSIS
    一键提交：git add -A 然后 git commit。
    用法（全小写）：.\commit.ps1 "提交信息"

.EXAMPLE
    .\commit.ps1 "feat: notebook full-text search"
#>
param([string]$message)

$ErrorActionPreference = "Stop"
if (-not $message) {
    Write-Host '用法: .\commit.ps1 "提交信息"'
    exit 1
}

git add -A
if ($LASTEXITCODE -ne 0) { throw "git add 失败（exit $LASTEXITCODE）" }
git commit -m $message
if ($LASTEXITCODE -eq 0) {
    Write-Host "已提交: $message"
} else {
    Write-Host "未提交（可能没有改动需要提交）"
}
