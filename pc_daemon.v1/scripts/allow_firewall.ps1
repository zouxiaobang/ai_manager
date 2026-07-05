#Requires -RunAsAdministrator

Write-Host "Adding firewall rule for NetEase Media Bridge port 8765..." -ForegroundColor Cyan

netsh advfirewall firewall add rule `
    name="NetEase Media Bridge 8765" `
    dir=in `
    action=allow `
    protocol=TCP `
    localport=8765 `
    profile=any `
    description="Allow ESP32 to access NetEase media sync daemon (port 8765)"

if ($LASTEXITCODE -eq 0) {
    Write-Host "Done! Firewall rule added." -ForegroundColor Green
} else {
    Write-Host "Failed. Run this script as Administrator." -ForegroundColor Red
}
