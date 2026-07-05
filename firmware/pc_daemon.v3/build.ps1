# NetEase SMTC Controller v3 - Build Script
# Usage: .\build.ps1
# Prerequisites: Python 3.10+, pip install -r requirements.txt

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$OutputDir = Join-Path $ProjectRoot "dist"

Write-Host "=== NetEase SMTC Controller v3 Build ===" -ForegroundColor Cyan

# Check dependencies
$depsOk = $true
try {
    $null = python -c "import winsdk" 2>$null
} catch {
    $depsOk = $false
}
try {
    $null = python -c "import PyInstaller" 2>$null
} catch {
    $depsOk = $false
}

if (-not $depsOk) {
    Write-Host "[*] Installing dependencies ..." -ForegroundColor Yellow
    pip install -r (Join-Path $ProjectRoot "requirements.txt")
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[!] pip install failed" -ForegroundColor Red
        exit 1
    }
}

# Clean old build
if (Test-Path $OutputDir) {
    Remove-Item -Recurse -Force $OutputDir
}
if (Test-Path (Join-Path $ProjectRoot "build")) {
    Remove-Item -Recurse -Force (Join-Path $ProjectRoot "build")
}

# PyInstaller package
Write-Host "[*] Packaging as single-file EXE ..." -ForegroundColor Green
python -m PyInstaller --noconfirm `
    --onefile `
    --noconsole `
    --name "NetEaseSMTCController" `
    --distpath $OutputDir `
    --workpath (Join-Path $ProjectRoot "build") `
    --specpath $ProjectRoot `
    --hidden-import winsdk `
    (Join-Path $ProjectRoot "main.py")

if ($LASTEXITCODE -eq 0) {
    $ExePath = Join-Path $OutputDir "NetEaseSMTCController.exe"
    $size = (Get-Item $ExePath).Length / 1MB
    Write-Host "[OK] Build success: $ExePath" -ForegroundColor Green
    Write-Host "     Size: $([math]::Round($size, 1)) MB"
} else {
    Write-Host "[!] Build failed, check error messages above" -ForegroundColor Red
    exit 1
}
