# Scan all IPs on 192.168.0.x for SSH port 22
Write-Host "Scanning 192.168.0.x for SSH (port 22)..."
Write-Host "=========================================="

$found = @()
$jobs = @()

for ($i = 1; $i -le 254; $i++) {
    $ip = "192.168.0.$i"
    $jobs += Start-Job -ScriptBlock {
        param($ip)
        $result = Test-NetConnection -ComputerName $ip -Port 22 -WarningAction SilentlyContinue -ErrorAction SilentlyContinue
        if ($result.TcpTestSucceeded) {
            return $ip
        }
    } -ArgumentList $ip
}

Write-Host "Waiting for scan to complete..."
$jobs | Wait-Job -Timeout 120 | Out-Null

foreach ($job in $jobs) {
    $result = Receive-Job $job
    if ($result) {
        Write-Host "Found SSH at: $result" -ForegroundColor Green
        $found += $result
    }
    Remove-Job $job -Force
}

Write-Host ""
Write-Host "=== ARP Table ==="
arp -a

Write-Host ""
Write-Host "=== Summary ==="
if ($found.Count -eq 0) {
    Write-Host "No SSH devices found on 192.168.0.x"
} else {
    Write-Host "Found SSH on: $($found -join ', ')"
}
