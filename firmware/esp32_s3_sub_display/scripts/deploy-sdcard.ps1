# Copy generated assets to a removable TF card drive (e.g. D:)
param(
    [Parameter(Mandatory = $true)]
    [string]$DriveLetter
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$srcAssets = Join-Path $Root "sdcard_assets\assets"
$srcLyrics = Join-Path $Root "sdcard_assets\lyrics"
$srcConfig = Join-Path $Root "sdcard_assets\config"
$dst = ($DriveLetter.TrimEnd(':') + ":\")

if (-not (Test-Path $srcAssets)) {
    Write-Error "Run generate_assets.py first: $srcAssets missing"
}
if (-not (Test-Path $dst)) {
    Write-Error "Drive not found: $dst"
}

$dstAssets = Join-Path $dst "assets"
$dstLyrics = Join-Path $dst "lyrics"
$dstConfig = Join-Path $dst "config"
New-Item -ItemType Directory -Force -Path $dstAssets, $dstLyrics, $dstConfig | Out-Null
Copy-Item -Path (Join-Path $srcAssets "*") -Destination $dstAssets -Force
Copy-Item -Path (Join-Path $srcLyrics "*") -Destination $dstLyrics -Force
Copy-Item -Path (Join-Path $srcConfig "*") -Destination $dstConfig -Force
Write-Host "Deployed to $dst (assets + lyrics + config)"
