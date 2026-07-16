Write-Host "Waiting 120 seconds for Pi to boot..."
Start-Sleep -Seconds 120

Write-Host "Pinging all devices..."
1..254 | ForEach-Object { Start-Process -FilePath "ping" -ArgumentList "-n 1 -w 100 192.168.0.$_" -WindowStyle Hidden }
Start-Sleep -Seconds 8

Write-Host ""
Write-Host "=== ARP Table ==="
arp -a

Write-Host ""
Write-Host "=== Scanning SSH ==="
$found = @()
for ($i = 1; $i -le 254; $i++) {
    $ip = "192.168.0.$i"
    try {
        $tcp = New-Object System.Net.Sockets.TcpClient
        $result = $tcp.BeginConnect($ip, 22, $null, $null)
        $wait = $result.AsyncWaitHandle.WaitOne(300, $false)
        if ($wait -and $tcp.Connected) {
            Write-Host "Found SSH at: $ip" -ForegroundColor Green
            $found += $ip
        }
        $tcp.Close()
    } catch { }
}

Write-Host ""
Write-Host "=== Summary ==="
if ($found.Count -eq 0) {
    Write-Host "No SSH devices found."
} else {
    Write-Host "Found SSH on: $($found -join ', ')"
}
