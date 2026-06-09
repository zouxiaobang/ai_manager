param(
    [switch]$All,
    [int[]]$SongIds = @(189987, 188703, 32688998),
    [switch]$UseHttp
)

$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
$TriggerFile = Join-Path $Root ".runtime\clear_lyrics_cache.json"
$Port = 8765

$payload = if ($All) {
    @{ clear_all = $true }
} else {
    @{ song_ids = @($SongIds) }
}

if ($UseHttp) {
    $body = $payload | ConvertTo-Json -Compress
    $response = Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:$Port/admin/lyrics/cache/clear" -Body $body -ContentType "application/json; charset=utf-8"
    Write-Host "Lyrics cache cleared via HTTP. removed=$($response.removed)"
    exit 0
}

New-Item -ItemType Directory -Force -Path (Split-Path $TriggerFile) | Out-Null
$payload | ConvertTo-Json -Compress | Out-File -FilePath $TriggerFile -Encoding ascii -Force
Write-Host "Lyrics cache clear trigger written: $TriggerFile"
Write-Host "Daemon will pick it up within ~0.25s (no restart needed)."
