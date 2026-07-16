@echo off
echo Assigning drive letter to bootfs partition...
powershell -ExecutionPolicy Bypass -Command "$p = Get-Partition | Where-Object { (Get-Volume -Partition $_ -ErrorAction SilentlyContinue).FileSystemLabel -eq 'bootfs' }; if ($p) { $p | Add-PartitionAccessPath -AccessPath 'D:\'; Write-Host 'Done! D: assigned to bootfs' } else { Write-Host 'bootfs not found' }"
pause
