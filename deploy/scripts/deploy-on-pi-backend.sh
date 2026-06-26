#!/usr/bin/env bash
# 在应用节点 114 本机构建并安装后端（供 Web 一键部署 local 模式调用）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
BACKEND_DIR="${BACKEND_DIR:-/opt/ai-manager/backend}"
GIT_PULL="${GIT_PULL:-true}"
RESTART_DELAY_SEC="${RESTART_DELAY_SEC:-5}"

if [[ "${GIT_PULL}" == "true" ]] && [[ -d "${ROOT}/.git" ]]; then
  echo "==> 拉取最新代码..."
  git -C "${ROOT}" pull --ff-only
fi

echo "==> 构建后端..."
cd "${ROOT}/admin-backend"
mvn clean package -DskipTests -pl admin-server -am

JAR="${ROOT}/admin-backend/admin-server/target/admin-server-1.0.0-SNAPSHOT.jar"
if [[ ! -f "${JAR}" ]]; then
  echo "未找到 JAR: ${JAR}" >&2
  exit 1
fi

jar_size_mb=$(( ($(stat -c%s "${JAR}" 2>/dev/null || stat -f%z "${JAR}") + 1048575) / 1048576 ))
echo "==> 安装 JAR 到 ${BACKEND_DIR}（约 ${jar_size_mb} MB）..."
cp "${JAR}" /tmp/admin-server.jar
sudo mv /tmp/admin-server.jar "${BACKEND_DIR}/admin-server.jar"
sudo chown aimanager:aimanager "${BACKEND_DIR}/admin-server.jar"

echo "==> ${RESTART_DELAY_SEC} 秒后重启服务（以便部署日志先返回）..."
nohup bash -c "sleep ${RESTART_DELAY_SEC} && sudo systemctl restart ai-manager-backend" >/tmp/ai-manager-deploy-restart.log 2>&1 &
echo "完成。服务即将重启，请稍后刷新页面并检查：curl http://127.0.0.1:8080/api/health"
