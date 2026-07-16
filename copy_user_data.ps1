[System.IO.File]::Copy('G:\projects\ai_project\ai_manager\user-data-new', 'D:\user-data', $true)
Write-Host "File copied successfully!"
Write-Host "=== Content of D:\user-data ==="
Get-Content 'D:\user-data'
