# Fast ping sweep
Write-Host "Pinging all 192.168.0.x addresses..."
1..254 | ForEach-Object { Start-Process -FilePath "ping" -ArgumentList "-n 1 -w 100 192.168.0.$_" -WindowStyle Hidden }
Start-Sleep -Seconds 8
Write-Host ""
Write-Host "=== ARP Table ==="
arp -a
