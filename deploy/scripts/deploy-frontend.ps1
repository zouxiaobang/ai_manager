# Build admin-web on Windows and deploy dist to Pi 114
# Web one-click deploy uses Java password SSH; for manual runs set PI_PASSWORD or configure SSH keys.
# Usage: $env:PI_PASSWORD='...'; powershell -ExecutionPolicy Bypass -File deploy/scripts/deploy-frontend.ps1

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$PiUser = if ($env:PI_USER) { $env:PI_USER } else { "kyle" }
$PiHost = if ($env:PI_HOST) { $env:PI_HOST } else { "192.168.0.114" }
$PiPassword = $env:PI_PASSWORD
$PiTarget = "${PiUser}@${PiHost}"
$WebRoot = if ($env:WEB_ROOT) { $env:WEB_ROOT } else { "/var/www/ai-manager" }
$Root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$LibDir = Join-Path $PSScriptRoot "lib"
$RemoteLib = Join-Path $LibDir "remote-ssh.ps1"
$UsePasswordSsh = -not [string]::IsNullOrWhiteSpace($PiPassword) -and (Test-Path $RemoteLib)
$PythonDeploy = Join-Path $PSScriptRoot "deploy-frontend.py"

if ($UsePasswordSsh -and (Test-Path $PythonDeploy) -and (Get-Command py -ErrorAction SilentlyContinue)) {
    Write-Host "==> Using Python deploy (paramiko password SSH)..." -ForegroundColor Cyan
    & py -3 $PythonDeploy
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    exit 0
}

if ($UsePasswordSsh) {
    . $RemoteLib
}

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
    if ($UsePasswordSsh) {
        Test-PiSshConnection -HostName $PiHost -UserName $PiUser
        return
    }
    Write-Host "==> Checking SSH to $PiTarget ..."
    & ssh @(Get-SshOpts) $PiTarget "echo ssh-ok"
    if ($LASTEXITCODE -ne 0) {
        throw "SSH to $PiTarget failed. Set PI_PASSWORD or configure key-based login."
    }
    Write-Host "SSH OK"
}

function Invoke-RemoteCommand {
    param([string]$Command)
    if ($UsePasswordSsh) {
        Invoke-PiCommand -HostName $PiHost -UserName $PiUser -Command $Command
        return
    }
    & ssh @(Get-SshOpts) $PiTarget $Command
    if ($LASTEXITCODE -ne 0) { throw "Remote command failed" }
}

function Send-RemoteFile {
    param([string]$LocalPath, [string]$RemotePath)
    if ($UsePasswordSsh) {
        Send-PiFile -HostName $PiHost -UserName $PiUser -LocalPath $LocalPath -RemotePath $RemotePath
        return
    }
    & scp @(Get-SshOpts) $LocalPath "${PiTarget}:${RemotePath}"
    if ($LASTEXITCODE -ne 0) { throw "SCP upload failed" }
}

Test-RemoteSsh

Write-Host "==> Building frontend..." -ForegroundColor Cyan
Set-Location "$Root\admin-web"
& npm install
if ($LASTEXITCODE -ne 0) { throw "npm install failed" }
& npm run build
if ($LASTEXITCODE -ne 0) { throw "npm run build failed" }

$DistDir = "$Root\admin-web\dist"
if (-not (Test-Path $DistDir)) { throw "dist directory not found" }

$TarPath = Join-Path $env:TEMP "ai-manager-dist.tar.gz"
if (Test-Path $TarPath) { Remove-Item $TarPath -Force }
Write-Host "==> Packaging dist ($(Get-ChildItem $DistDir -Recurse -File | Measure-Object | Select-Object -ExpandProperty Count) files)..." -ForegroundColor Cyan
& tar -czf $TarPath -C "$Root\admin-web" dist
if ($LASTEXITCODE -ne 0) { throw "tar packaging failed" }

$TarSizeMb = [math]::Round((Get-Item $TarPath).Length / 1MB, 2)
$RemoteTar = "/tmp/ai-manager-dist.tar.gz"
Write-Host "==> Uploading archive ($TarSizeMb MB) to $PiTarget ..." -ForegroundColor Cyan
Send-RemoteFile -LocalPath $TarPath -RemotePath $RemoteTar
Write-Host "Upload complete"

$InstallCmd = @"
rm -rf /tmp/ai-manager-new && mkdir -p /tmp/ai-manager-new &&
tar -xzf $RemoteTar -C /tmp/ai-manager-new --strip-components=1 &&
sudo rsync -av --delete /tmp/ai-manager-new/ $WebRoot/ &&
sudo chown -R www-data:www-data $WebRoot &&
rm -f $RemoteTar
"@

Write-Host "==> Installing to Nginx web root $WebRoot ..." -ForegroundColor Cyan
Invoke-RemoteCommand -Command $InstallCmd

$homeUrl = "http://$PiHost/#/home"
Write-Host "Done. Open $homeUrl (Ctrl+F5 to hard refresh)" -ForegroundColor Green
