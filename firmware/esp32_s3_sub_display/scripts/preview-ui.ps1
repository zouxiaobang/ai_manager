# Render home UI preview PNG
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$py = "py"
if (-not (Get-Command $py -ErrorAction SilentlyContinue)) { $py = "python" }
& $py -3 "$Root\scripts\preview_ui.py"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host "Open: $Root\preview\ui_home_preview.png"
