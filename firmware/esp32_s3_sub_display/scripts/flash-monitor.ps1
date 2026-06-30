param(
    [string]$Port = ""
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot "idf-env.ps1")
Set-Location $ProjectRoot

if ($Port) {
    & idf.py -p $Port flash monitor
} else {
    & idf.py flash monitor
}

exit $LASTEXITCODE
