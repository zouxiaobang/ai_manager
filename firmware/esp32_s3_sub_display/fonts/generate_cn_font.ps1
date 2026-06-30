# Generate LVGL subset Chinese font for sub-display UI.
# Requires: Node.js, lv_font_conv (via npx)
# Usage: .\fonts\generate_cn_font.ps1

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$GlyphsFile = Join-Path $PSScriptRoot "glyphs_cn.txt"
$OutC = Join-Path $Root "main\fonts\lv_font_cn_16.c"
$OutDir = Split-Path -Parent $OutC

$FontCandidates = @(
    "$env:WINDIR\Fonts\simhei.ttf",
    "$env:WINDIR\Fonts\msyhbd.ttc",
    "$env:WINDIR\Fonts\simsun.ttc"
)
$FontPath = $FontCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $FontPath) {
    throw "No Chinese TTF/TTC found under $env:WINDIR\Fonts"
}

$Symbols = (Get-Content -Raw -Encoding UTF8 $GlyphsFile) -replace "[\r\n]", ""
if ([string]::IsNullOrWhiteSpace($Symbols)) {
    throw "glyphs_cn.txt is empty"
}

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

Write-Host "Font:    $FontPath"
Write-Host "Glyphs:  $($Symbols.Length) chars"
Write-Host "Output:  $OutC"

$SkipCodepoints = @(0x25B6, 0x25C0, 0x266A)
$CnSymbols = -join ($Symbols.ToCharArray() | Where-Object { $SkipCodepoints -notcontains [int][char]$_ })
$SymbolRanges = "0x25B6,0x25C0,0x266A"

npx --yes lv_font_conv `
    --font $FontPath `
    --symbols $CnSymbols `
    --font "$env:WINDIR\Fonts\seguisym.ttf" `
    -r $SymbolRanges `
    --size 16 `
    --bpp 4 `
    --format lvgl `
    --no-compress `
    --no-prefilter `
    --force-fast-kern-format `
    --lv-font-name lv_font_cn_16 `
    -o $OutC

if (-not (Test-Path $OutC)) {
    throw "Font generation failed: $OutC not created"
}

$sizeKb = [math]::Round((Get-Item $OutC).Length / 1KB, 1)
Write-Host "Done. lv_font_cn_16.c = ${sizeKb} KB"
Write-Host "Rebuild firmware: idf.py build flash"
