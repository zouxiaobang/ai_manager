# 将本地 ai_manager_admin 全库同步到线上数据节点 MySQL（192.168.0.118）
# 用法：powershell -ExecutionPolicy Bypass -File deploy/scripts/sync-local-db-to-prod.ps1
# 可选环境变量：PROD_MYSQL_HOST / PROD_MYSQL_USER / PROD_MYSQL_PASSWORD / LOCAL_MYSQL_USER / LOCAL_MYSQL_PASSWORD

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$Root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$BackupDir = Join-Path $Root "admin-backend/sql/backups"
New-Item -ItemType Directory -Force -Path $BackupDir | Out-Null

$ProdHost = if ($env:PROD_MYSQL_HOST) { $env:PROD_MYSQL_HOST } else { "192.168.0.118" }
$ProdUser = if ($env:PROD_MYSQL_USER) { $env:PROD_MYSQL_USER } else { "ai_manager" }
$ProdPass = if ($env:PROD_MYSQL_PASSWORD) { $env:PROD_MYSQL_PASSWORD } else { "123456" }
$LocalUser = if ($env:LOCAL_MYSQL_USER) { $env:LOCAL_MYSQL_USER } else { "root" }
$LocalPass = if ($env:LOCAL_MYSQL_PASSWORD) { $env:LOCAL_MYSQL_PASSWORD } else { "123456" }
$Db = "ai_manager_admin"

$ts = Get-Date -Format "yyyyMMdd_HHmmss"
$ProdBackup = Join-Path $BackupDir "prod_before_$ts.sql"

$DumpArgs = @(
    "--default-character-set=utf8mb4",
    "--single-transaction",
    "--routines",
    "--triggers",
    "--add-drop-table",
    "--set-gtid-purged=OFF",
    $Db
)

Write-Host ">>> 备份线上库到 $ProdBackup ..." -ForegroundColor Cyan
& mysqldump -h"$ProdHost" -u"$ProdUser" -p"$ProdPass" @DumpArgs | Set-Content -Path $ProdBackup -Encoding utf8
Write-Host "线上备份大小: $([math]::Round((Get-Item $ProdBackup).Length / 1KB, 1)) KB"

Write-Host ">>> 将本地库导入线上（会覆盖线上业务数据）..." -ForegroundColor Yellow
$pipeCmd = "mysqldump -u$LocalUser -p$LocalPass $($DumpArgs -join ' ') | mysql -h$ProdHost -u$ProdUser -p$ProdPass --default-character-set=utf8mb4 $Db"
cmd /c $pipeCmd
if ($LASTEXITCODE -ne 0) { throw "数据库同步失败，退出码 $LASTEXITCODE" }

Write-Host ">>> 校验行数 ..." -ForegroundColor Cyan
mysql -h"$ProdHost" -u"$ProdUser" -p"$ProdPass" -e @"
USE $Db;
SELECT 'ec_sales_order' AS t, COUNT(*) AS c FROM ec_sales_order
UNION ALL SELECT 'ec_sku', COUNT(*) FROM ec_sku
UNION ALL SELECT 'ec_product', COUNT(*) FROM ec_product
UNION ALL SELECT 'ec_inventory', COUNT(*) FROM ec_inventory;
"@

Write-Host "数据库同步完成。请继续执行 uploads 同步：" -ForegroundColor Green
Write-Host "  py -3 deploy/scripts/sync-local-uploads-to-prod.py" -ForegroundColor Green
