#!/usr/bin/env bash
# 在开发机构建后端 JAR 并同步到树莓派
set -euo pipefail

PI_HOST="${PI_HOST:-kyle@192.168.0.114}"
BACKEND_DIR="${BACKEND_DIR:-/opt/ai-manager/backend}"

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

echo "==> 构建后端..."
cd "$ROOT/admin-backend"
mvn clean package -DskipTests -pl admin-server -am

JAR="$ROOT/admin-backend/admin-server/target/admin-server-1.0.0-SNAPSHOT.jar"

echo "==> 上传 JAR 到 $PI_HOST ..."
scp "$JAR" "$PI_HOST:$BACKEND_DIR/admin-server.jar"

echo "==> 重启服务..."
ssh "$PI_HOST" "sudo systemctl restart ai-manager-backend && sudo systemctl status ai-manager-backend --no-pager"

echo "完成。健康检查：curl http://192.168.0.114/api/health"
