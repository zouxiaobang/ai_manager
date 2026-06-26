# 在 Windows 开发机构建前端并部署到应用节点 114
# 用法：powershell -ExecutionPolicy Bypass -File deploy/scripts/deploy-frontend.ps1

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$PiUser = if ($env:PI_USER) { $env:PI_USER } else { "kyle" }
$PiHost = if ($env:PI_HOST) { $env:PI_HOST } else { "192.168.0.114" }
$PiTarget = "${PiUser}@${PiHost}"
$Root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent

function Get-SshOpts {
    return @(
        "-o", "BatchMode=yes",
        "-o", "ConnectTimeout=20",
        "-o", "StrictHostKeyChecking=accept-new",
        "-o", "ServerAliveInterval=15",
        "-o", "ServerAliveCountMax=3"
    )
}

function Test-RemoteSsh {
    param([string]$Target)
    Write-Host "==> 检查 SSH 免密连接 $Target ..."
    & ssh @(Get-SshOpts) $Target "echo ssh-ok"
    if ($LASTEXITCODE -ne 0) {
        throw @"
SSH 无法免密连接 $Target（一键部署不能交互输入密码）。
请先在 PowerShell 中手动执行: ssh $Target
若提示输入密码，请把本机公钥加入 114：
  type `$env:USERPROFILE\.ssh\id_rsa.pub | ssh $Target "mkdir -p ~/.ssh && chmod 700 ~/.ssh && cat >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys"
配置完成后再执行: ssh $Target echo ok
"@
    }
    Write-Host "SSH 检查通过"
}

Test-RemoteSsh -Target $PiTarget

Write-Host "==> 构建前端..." -ForegroundColor Cyan
Set-Location "$Root\admin-web"
& npm install
if ($LASTEXITCODE -ne 0) { throw "npm install 失败" }
& npm run build
if ($LASTEXITCODE -ne 0) { throw "npm run build 失败" }

Write-Host "==> 上传到 $PiTarget ..." -ForegroundColor Cyan
& ssh @(Get-SshOpts) $PiTarget "mkdir -p /tmp/ai-manager-new"
if ($LASTEXITCODE -ne 0) { throw "远程 mkdir 失败" }

Write-Host "==> SCP 传输 dist 文件（无进度条，请稍候）..." -ForegroundColor Cyan
& scp @(Get-SshOpts) -r "$Root\admin-web\dist\*" "${PiTarget}:/tmp/ai-manager-new/"
if ($LASTEXITCODE -ne 0) { throw "SCP 上传失败" }
Write-Host "静态文件上传完成"

Write-Host "==> 安装到 Nginx 目录..." -ForegroundColor Cyan
& ssh @(Get-SshOpts) $PiTarget "sudo rsync -av --delete /tmp/ai-manager-new/ /var/www/ai-manager/ && sudo chown -R www-data:www-data /var/www/ai-manager"
if ($LASTEXITCODE -ne 0) { throw "rsync 安装失败" }

Write-Host "完成。访问 http://${PiHost}/#/home" -ForegroundColor Green
