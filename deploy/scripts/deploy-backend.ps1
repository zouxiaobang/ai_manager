# 在 Windows 开发机构建后端并部署到应用节点 114
# 用法：powershell -ExecutionPolicy Bypass -File deploy/scripts/deploy-backend.ps1

$ErrorActionPreference = "Stop"

$PiUser = if ($env:PI_USER) { $env:PI_USER } else { "kyle" }
$PiHost = if ($env:PI_HOST) { $env:PI_HOST } else { "192.168.0.114" }
$PiTarget = "${PiUser}@${PiHost}"
$BackendDir = if ($env:BACKEND_DIR) { $env:BACKEND_DIR } else { "/opt/ai-manager/backend" }
$Root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent

Write-Host "==> 构建后端..." -ForegroundColor Cyan
Set-Location "$Root\admin-backend"
mvn clean package -DskipTests -pl admin-server -am

$Jar = "$Root\admin-backend\admin-server\target\admin-server-1.0.0-SNAPSHOT.jar"

Write-Host "==> 上传 JAR 到 $PiTarget ..." -ForegroundColor Cyan
scp $Jar "${PiTarget}:${BackendDir}/admin-server.jar"

Write-Host "==> 重启服务..." -ForegroundColor Cyan
ssh $PiTarget "sudo systemctl restart ai-manager-backend && sudo systemctl status ai-manager-backend --no-pager"

Write-Host "完成。健康检查：curl http://${PiHost}/api/health" -ForegroundColor Green
