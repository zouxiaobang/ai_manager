param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$IdfArgs
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot "idf-env.ps1")
Set-Location $ProjectRoot

if ($IdfArgs.Count -gt 0) {
    & idf.py @IdfArgs
} else {
    & idf.py build
}

exit $LASTEXITCODE
