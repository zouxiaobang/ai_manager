# Activate ESP-IDF for this machine (PowerShell).
# Usage (from any directory):
#   . G:\projects\ai_project\ai_manager\firmware\esp32_s3_sub_display\scripts\idf-env.ps1
#
# Or from firmware/esp32_s3_sub_display:
#   . .\scripts\idf-env.ps1

$ErrorActionPreference = "Stop"

if (-not $env:IDF_PATH) {
    $env:IDF_PATH = "G:\projects\iot\Espressif\frameworks\esp-idf-v5.5.2"
}

if (-not $env:IDF_TOOLS_PATH) {
    $env:IDF_TOOLS_PATH = "G:\projects\iot\Espressif"
}

$idfPython = Join-Path $env:IDF_TOOLS_PATH "python_env\idf5.5_py3.11_env\Scripts\python.exe"
if (-not (Test-Path $idfPython)) {
    $idfPython = "C:\Python311\python.exe"
}
if (-not (Test-Path $idfPython)) {
    throw "ESP-IDF Python not found. Install ESP-IDF tools or set IDF_TOOLS_PATH."
}

Set-Alias -Name python -Value $idfPython -Scope Global -Force -Option AllScope

$exportScript = Join-Path $env:IDF_PATH "export.ps1"
if (-not (Test-Path $exportScript)) {
    throw "export.ps1 not found at $exportScript"
}

. $exportScript

Write-Host "ESP-IDF ready: IDF_PATH=$env:IDF_PATH" -ForegroundColor Green
