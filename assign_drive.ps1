$partitions = Get-Partition | Where-Object { (Get-Volume -Partition $_ -ErrorAction SilentlyContinue).FileSystemLabel -eq 'bootfs' }
foreach ($p in $partitions) {
    Write-Host "Found bootfs on Disk $($p.DiskNumber) Partition $($p.PartitionNumber)"
    try {
        $p | Add-PartitionAccessPath -AccessPath "D:\" -ErrorAction Stop
        Write-Host "Successfully assigned D: to bootfs"
    } catch {
        Write-Host "Error: $_"
    }
}
