#!/usr/bin/env bash
# 在开发机构建后端 JAR 并同步到树莓派（支持 PI_PASSWORD 密码登录或 SSH 密钥）
set -euo pipefail

PI_USER="${PI_USER:-kyle}"
PI_HOST_ADDR="${PI_HOST_ADDR:-192.168.0.114}"
PI_HOST="${PI_HOST:-${PI_USER}@${PI_HOST_ADDR}}"
BACKEND_DIR="${BACKEND_DIR:-/opt/ai-manager/backend}"
PI_PASSWORD="${PI_PASSWORD:-}"

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

SSH_COMMON_OPTS="-o ConnectTimeout=20 -o StrictHostKeyChecking=accept-new -o ServerAliveInterval=15"
SCP_COMMON_OPTS="$SSH_COMMON_OPTS"

run_ssh() {
  if [[ -n "$PI_PASSWORD" ]]; then
    if ! command -v sshpass >/dev/null 2>&1; then
      echo "需要 sshpass 才能使用密码登录，请安装后重试，或配置 SSH 公钥。" >&2
      exit 1
    fi
    sshpass -p "$PI_PASSWORD" ssh $SSH_COMMON_OPTS "$@"
  else
    ssh $SSH_COMMON_OPTS -o BatchMode=yes "$@"
  fi
}

run_scp() {
  if [[ -n "$PI_PASSWORD" ]]; then
    sshpass -p "$PI_PASSWORD" scp $SCP_COMMON_OPTS "$@"
  else
    scp $SCP_COMMON_OPTS -o BatchMode=yes "$@"
  fi
}

echo "==> 检查 SSH 连接 $PI_HOST ..."
run_ssh "$PI_HOST" "echo ssh-ok"

echo "==> 构建后端..."
cd "$ROOT/admin-backend"
mvn clean package -DskipTests -pl admin-server -am

JAR="$ROOT/admin-backend/admin-server/target/admin-server-1.0.0-SNAPSHOT.jar"

echo "==> 上传 JAR 到 /tmp ..."
run_scp "$JAR" "$PI_HOST:/tmp/admin-server.jar"

echo "==> 安装到 $BACKEND_DIR ..."
run_ssh "$PI_HOST" "sudo mv /tmp/admin-server.jar $BACKEND_DIR/admin-server.jar && sudo chown aimanager:aimanager $BACKEND_DIR/admin-server.jar"

echo "==> 重启服务..."
run_ssh "$PI_HOST" "sudo systemctl restart ai-manager-backend && sudo systemctl status ai-manager-backend --no-pager"

echo "完成。健康检查：curl http://${PI_HOST_ADDR}/api/health"
