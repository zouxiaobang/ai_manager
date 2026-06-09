# 在父工程目录执行：先编译依赖模块，再仅在 admin-server 上 spring-boot:run
# （勿用 -am spring-boot:run，否则会在父 pom 上执行 run 并报找不到 main class）
Set-Location $PSScriptRoot
mvn -pl admin-server -am package -DskipTests
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
mvn -pl admin-server spring-boot:run
