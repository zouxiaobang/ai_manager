# 在 Windows 开发机构建后端并部署到应用节点 114
# 一键部署（Web 弹窗）由后端 Java 使用密码登录完成，无需 SSH 免密。
# 手动执行本脚本时：设置环境变量 PI_PASSWORD，或配置 SSH 公钥。
# 用法：powershell -ExecutionPolicy Bypass -File deploy/scripts/deploy-backend.ps1

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$PiUser = if ($env:PI_USER) { $env:PI_USER } else { "kyle" }
$PiHost = if ($env:PI_HOST) { $env:PI_HOST } else { "192.168.0.114" }
$PiPassword = $env:PI_PASSWORD
$PiTarget = "${PiUser}@${PiHost}"
$BackendDir = if ($env:BACKEND_DIR) { $env:BACKEND_DIR } else { "/opt/ai-manager/backend" }
$Root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$LibDir = Join-Path $PSScriptRoot "lib"
$RemoteLib = Join-Path $LibDir "remote-ssh.ps1"

function Get-SshOpts {
    return @(
        "-o", "BatchMode=yes",
        "-o", "ConnectTimeout=20",
        "-o", "StrictHostKeyChecking=accept-new",
        "-o", "ServerAliveInterval=15",
        "-o", "ServerAliveCountMax=3"
    )
}

if (-not [string]::IsNullOrWhiteSpace($PiPassword) -and (Test-Path $RemoteLib)) {
    . $RemoteLib
    Test-PiSshConnection -HostName $PiHost -UserName $PiUser
} else {
    Write-Host "==> 检查 SSH 免密连接 $PiTarget ..."
    & ssh @(Get-SshOpts) $PiTarget "echo ssh-ok"
    if ($LASTEXITCODE -ne 0) {
        throw @"
SSH 无法连接 $PiTarget。
一键部署请使用 Web 界面（后端 Java 密码登录），或设置环境变量 PI_PASSWORD 后重试本脚本。
"@
    }
    Write-Host "SSH 检查通过"
}

Write-Host "==> 构建后端..." -ForegroundColor Cyan
Set-Location "$Root\admin-backend"
& mvn clean package -DskipTests -pl admin-server -am
if ($LASTEXITCODE -ne 0) { throw "Maven 构建失败，退出码 $LASTEXITCODE" }

$Jar = "$Root\admin-backend\admin-server\target\admin-server-1.0.0-SNAPSHOT.jar"
if (-not (Test-Path $Jar)) { throw "未找到 JAR: $Jar" }

$jarSizeMb = [math]::Round((Get-Item $Jar).Length / 1MB, 2)
$StagingJar = "/tmp/admin-server.jar"
$RemoteJar = "$BackendDir/admin-server.jar"
$InstallCmd = "sudo mv $StagingJar $RemoteJar && sudo chown aimanager:aimanager $RemoteJar"

if (-not [string]::IsNullOrWhiteSpace($PiPassword) -and (Test-Path $RemoteLib)) {
    Write-Host "==> 上传 JAR 到 /tmp ($jarSizeMb MB)，请稍候..." -ForegroundColor Cyan
    Send-PiFile -HostName $PiHost -UserName $PiUser -LocalPath $Jar -RemotePath $StagingJar
    Write-Host "==> 安装到 $RemoteJar ..." -ForegroundColor Cyan
    Invoke-PiCommand -HostName $PiHost -UserName $PiUser -Command $InstallCmd
} else {
    Write-Host "==> 上传 JAR 到 /tmp ($jarSizeMb MB)，请稍候..." -ForegroundColor Cyan
    & scp @(Get-SshOpts) $Jar "${PiTarget}:${StagingJar}"
    if ($LASTEXITCODE -ne 0) { throw "SCP 上传失败，退出码 $LASTEXITCODE" }
    Write-Host "==> 安装到 $RemoteJar ..." -ForegroundColor Cyan
    & ssh @(Get-SshOpts) $PiTarget $InstallCmd
    if ($LASTEXITCODE -ne 0) { throw "JAR 安装失败，退出码 $LASTEXITCODE" }
}
Write-Host "JAR 安装完成"

Write-Host "==> 重启服务..." -ForegroundColor Cyan
$RestartCmd = "sudo systemctl restart ai-manager-backend && sudo systemctl status ai-manager-backend --no-pager"
if (-not [string]::IsNullOrWhiteSpace($PiPassword) -and (Test-Path $RemoteLib)) {
    Invoke-PiCommand -HostName $PiHost -UserName $PiUser -Command $RestartCmd
} else {
    & ssh @(Get-SshOpts) $PiTarget $RestartCmd
    if ($LASTEXITCODE -ne 0) { throw "服务重启失败，退出码 $LASTEXITCODE" }
}

Write-Host "完成。健康检查：curl http://${PiHost}/api/health" -ForegroundColor Green
