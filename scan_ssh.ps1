# Scan all devices for SSH port 22
Write-Host "Scanning 192.168.0.x for SSH (port 22)..."
Write-Host "=========================================="

$found = @()

# Test each IP in parallel batches
$ips = 1..254 | ForEach-Object { "192.168.0.$_" }

foreach ($ip in $ips) {
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
