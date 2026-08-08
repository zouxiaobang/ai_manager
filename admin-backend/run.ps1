# 在父工程目录执行：先编译依赖模块，再仅在 admin-server 上 spring-boot:run
# （勿用 -am spring-boot:run，否则会在父 pom 上执行 run 并报找不到 main class）
# 统一控制台/子进程为 UTF-8，避免 Spring Boot 中文日志在 936/GBK 控制台乱码
chcp 65001 > $null
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
Set-Location $PSScriptRoot
# 加载仓库根目录 .env（本地环境变量，gitignored；含 AI_MANAGER_CONFIG_MASTER_KEY 等）。
# 不加载的话后端会回退内置开发密钥，导致与已有配置加密密钥不一致而解密失败。
$envFile = Join-Path (Split-Path -Parent $PSScriptRoot) '.env'
if (Test-Path $envFile) {
    Get-Content $envFile | Where-Object { $_ -match '^\s*[^#].*=.*' } | ForEach-Object {
        $kv = $_ -split '=', 2
        $k = $kv[0].Trim()
        $v = $kv[1].Trim()
        if ($v.Length -ge 2 -and $v[0] -eq '"' -and $v[-1] -eq '"') { $v = $v.Substring(1, $v.Length - 2) }
        [Environment]::SetEnvironmentVariable($k, $v, 'Process')
    }
}
mvn -pl admin-server -am package -DskipTests
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
mvn -pl admin-server spring-boot:run
