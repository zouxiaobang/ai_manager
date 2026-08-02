# 在父工程目录执行：先编译依赖模块，再仅在 admin-server 上 spring-boot:run
# （勿用 -am spring-boot:run，否则会在父 pom 上执行 run 并报找不到 main class）
# 统一控制台/子进程为 UTF-8，避免 Spring Boot 中文日志在 936/GBK 控制台乱码
chcp 65001 > $null
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
Set-Location $PSScriptRoot
mvn -pl admin-server -am package -DskipTests
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
mvn -pl admin-server spring-boot:run
