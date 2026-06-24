# 在 Windows 开发机构建前端并部署到应用节点 114
# 用法：powershell -ExecutionPolicy Bypass -File deploy/scripts/deploy-frontend.ps1

$ErrorActionPreference = "Stop"

$PiUser = if ($env:PI_USER) { $env:PI_USER } else { "kyle" }
$PiHost = if ($env:PI_HOST) { $env:PI_HOST } else { "192.168.0.114" }
$PiTarget = "${PiUser}@${PiHost}"
$Root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent

Write-Host "==> 构建前端..." -ForegroundColor Cyan
Set-Location "$Root\admin-web"
npm install
npm run build

Write-Host "==> 上传到 $PiTarget ..." -ForegroundColor Cyan
ssh $PiTarget "mkdir -p /tmp/ai-manager-new"
scp -r "$Root\admin-web\dist\*" "${PiTarget}:/tmp/ai-manager-new/"

Write-Host "==> 安装到 Nginx 目录..." -ForegroundColor Cyan
ssh $PiTarget "sudo rsync -av --delete /tmp/ai-manager-new/ /var/www/ai-manager/ && sudo chown -R www-data:www-data /var/www/ai-manager"

Write-Host "完成。访问 http://${PiHost}/#/home" -ForegroundColor Green
