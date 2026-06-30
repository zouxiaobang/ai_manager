# 密码 SSH 辅助（手动执行 deploy-*.ps1 时使用，依赖 SSH.NET）
$ErrorActionPreference = "Stop"

$script:SshNetVersion = "2024.1.0"
$script:SshNetPackageDir = Join-Path $PSScriptRoot ".nuget/SSH.NET.$($script:SshNetVersion)"

function Resolve-SshNetDll {
    $candidates = @(
        (Join-Path $script:SshNetPackageDir "lib/net8.0/Renci.SshNet.dll"),
        (Join-Path $script:SshNetPackageDir "lib/netstandard2.1/Renci.SshNet.dll"),
        (Join-Path $script:SshNetPackageDir "lib/netstandard2.0/Renci.SshNet.dll"),
        (Join-Path $script:SshNetPackageDir "lib/net45/Renci.SshNet.dll"),
        (Join-Path $script:SshNetPackageDir "lib/net40/Renci.SshNet.dll")
    )
    foreach ($dll in $candidates) {
        if (Test-Path $dll) {
            return $dll
        }
    }
    return $null
}

function Ensure-SshNet {
    $resolved = Resolve-SshNetDll
    if ($resolved) {
        $script:SshNetDll = $resolved
        return
    }
    $zipPath = Join-Path $env:TEMP "SSH.NET.$($script:SshNetVersion).nupkg.zip"
    $url = "https://www.nuget.org/api/v2/package/SSH.NET/$($script:SshNetVersion)"
    Write-Host "==> Downloading SSH.NET (first run needs network)..."
    Invoke-WebRequest -Uri $url -OutFile $zipPath -UseBasicParsing
    if (Test-Path $script:SshNetPackageDir) {
        Remove-Item $script:SshNetPackageDir -Recurse -Force
    }
    New-Item -ItemType Directory -Path $script:SshNetPackageDir -Force | Out-Null
    Expand-Archive -Path $zipPath -DestinationPath $script:SshNetPackageDir -Force
    Remove-Item $zipPath -Force
    $resolved = Resolve-SshNetDll
    if (-not $resolved) {
        throw "SSH.NET install failed"
    }
    $script:SshNetDll = $resolved
}

function Get-PiCredential {
    param([string]$UserName)
    $password = $env:PI_PASSWORD
    if ([string]::IsNullOrWhiteSpace($password)) {
        throw "未设置 PI_PASSWORD 环境变量"
    }
    $secure = ConvertTo-SecureString $password -AsPlainText -Force
    return New-Object System.Management.Automation.PSCredential ($UserName, $secure)
}

function Get-PiConnectionInfo {
    param([string]$HostName, [string]$UserName)
    Ensure-SshNet
    Add-Type -Path $script:SshNetDll
    $password = $env:PI_PASSWORD
    $auth = New-Object Renci.SshNet.PasswordAuthenticationMethod($UserName, $password)
    return New-Object Renci.SshNet.ConnectionInfo($HostName, 22, $UserName, $auth)
}

function Test-PiSshConnection {
    param([string]$HostName, [string]$UserName)
    Write-Host "==> 检查 SSH 密码连接 ${UserName}@${HostName} ..."
    $conn = Get-PiConnectionInfo -HostName $HostName -UserName $UserName
    $client = New-Object Renci.SshNet.SshClient($conn)
    try {
        $client.Connect()
        if (-not $client.IsConnected) {
            throw "SSH 连接失败"
        }
        Write-Host "SSH 检查通过（密码登录）"
    } finally {
        if ($client.IsConnected) { $client.Disconnect() }
        $client.Dispose()
    }
}

function Invoke-PiCommand {
    param([string]$HostName, [string]$UserName, [string]$Command)
    $conn = Get-PiConnectionInfo -HostName $HostName -UserName $UserName
    $client = New-Object Renci.SshNet.SshClient($conn)
    try {
        $client.Connect()
        $cmd = $client.CreateCommand($Command)
        $async = $cmd.BeginExecute()
        while (-not $async.IsCompleted) {
            Start-Sleep -Milliseconds 200
        }
        $cmd.EndExecute($async) | Out-Null
        if ($cmd.ExitStatus -ne 0) {
            throw "远程命令失败: $($cmd.Error)"
        }
        if ($cmd.Result) {
            Write-Host $cmd.Result
        }
    } finally {
        if ($client.IsConnected) { $client.Disconnect() }
        $client.Dispose()
    }
}

function Send-PiFile {
    param([string]$HostName, [string]$UserName, [string]$LocalPath, [string]$RemotePath)
    $conn = Get-PiConnectionInfo -HostName $HostName -UserName $UserName
    $client = New-Object Renci.SshNet.ScpClient($conn)
    try {
        $client.Connect()
        $stream = [System.IO.File]::OpenRead($LocalPath)
        try {
            $client.Upload($stream, $RemotePath)
        } finally {
            $stream.Dispose()
        }
    } finally {
        if ($client.IsConnected) { $client.Disconnect() }
        $client.Dispose()
    }
}
