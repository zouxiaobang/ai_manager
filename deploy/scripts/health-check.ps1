# 快速检查应用节点服务是否正常
# 用法：powershell -ExecutionPolicy Bypass -File deploy/scripts/health-check.ps1

$PiHost = if ($env:PI_HOST) { $env:PI_HOST } else { "192.168.0.114" }
$DataHost = if ($env:DATA_HOST) { $env:DATA_HOST } else { "192.168.0.118" }

Write-Host "=== 应用节点 $PiHost ===" -ForegroundColor Cyan
curl.exe -s "http://${PiHost}/api/health"
Write-Host ""
curl.exe -s "http://${PiHost}/api/todos/today" | Select-Object -First 1
Write-Host ""

Write-Host "=== 数据节点 $DataHost :3306 ===" -ForegroundColor Cyan
powershell -Command "Test-NetConnection $DataHost -Port 3306 | Select-Object ComputerName,RemotePort,TcpTestSucceeded"
