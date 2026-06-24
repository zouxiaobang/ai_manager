#!/usr/bin/env bash
# 在数据节点 192.168.0.116 上一键安装 Docker + MySQL/Redis（compose）
# 用法（在 116 上，仓库已克隆或已 scp 本目录）：
#   bash deploy/scripts/setup-data-node-docker.sh
# 可选：先卸载原生库
#   sudo bash deploy/scripts/uninstall-native-db-on-116.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
SRC_COMPOSE="${REPO_ROOT}/deploy/docker/data-node"
INSTALL_DIR="/opt/ai-manager/data-node"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'
ok() { echo -e "${GREEN}[OK]${NC} $*"; }
warn() { echo -e "${YELLOW}[!!]${NC} $*"; }
fail() { echo -e "${RED}[FAIL]${NC} $*"; exit 1; }

echo "========== 数据节点 Docker 安装（MySQL + Redis）=========="
echo "源目录: ${SRC_COMPOSE}"
echo "安装目录: ${INSTALL_DIR}"
echo

if [[ ! -f "${SRC_COMPOSE}/docker-compose.yml" ]]; then
  fail "未找到 ${SRC_COMPOSE}/docker-compose.yml，请在仓库根目录执行或先同步 deploy/docker/data-node"
fi

# --- 安装 Docker ---
if ! command -v docker >/dev/null 2>&1; then
  warn "未检测到 docker，尝试 apt 安装..."
  sudo apt-get update
  sudo apt-get install -y docker.io docker-compose-plugin
  sudo systemctl enable --now docker
  ok "Docker 已安装"
else
  ok "Docker 已存在: $(docker --version)"
fi

if docker compose version >/dev/null 2>&1; then
  COMPOSE="docker compose"
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE="docker-compose"
else
  fail "未找到 docker compose，请安装 docker-compose-plugin"
fi

# 当前用户加入 docker 组（需重新登录生效；本脚本后续用 sudo）
if id -nG "${USER}" | grep -qw docker; then
  ok "用户 ${USER} 已在 docker 组"
else
  sudo usermod -aG docker "${USER}" || true
  warn "已将 ${USER} 加入 docker 组，重新 SSH 登录后可不用 sudo 运行 docker"
fi

# --- 检查端口占用 ---
for port in 3306 6379; do
  if ss -tln 2>/dev/null | grep -q ":${port} "; then
    warn "端口 ${port} 已被占用，若仍是 mariadb/redis 请先运行 uninstall-native-db-on-116.sh"
    ss -tlnp | grep ":${port} " || true
  fi
done

# --- 同步 compose 到安装目录 ---
sudo mkdir -p "${INSTALL_DIR}"
if command -v rsync >/dev/null 2>&1; then
  sudo rsync -a --delete \
    --exclude '.env' \
    "${SRC_COMPOSE}/" "${INSTALL_DIR}/"
else
  warn "未安装 rsync，使用 cp（建议: sudo apt install -y rsync）"
  sudo rm -rf "${INSTALL_DIR:?}/"*
  sudo cp -a "${SRC_COMPOSE}/." "${INSTALL_DIR}/"
  sudo rm -f "${INSTALL_DIR}/.env" 2>/dev/null || true
fi
sudo chown -R "${USER}:${USER}" "${INSTALL_DIR}"

if [[ ! -f "${INSTALL_DIR}/.env" ]]; then
  cp "${INSTALL_DIR}/.env.example" "${INSTALL_DIR}/.env"
  warn "已创建 ${INSTALL_DIR}/.env ，请编辑 MYSQL_ROOT_PASSWORD / MYSQL_PASSWORD 后重新运行本脚本"
  echo "  nano ${INSTALL_DIR}/.env"
  exit 0
fi

# 检查是否仍为默认占位符
if grep -q '请修改' "${INSTALL_DIR}/.env" 2>/dev/null; then
  fail "请先编辑 ${INSTALL_DIR}/.env 设置强密码（勿使用 .env.example 占位符）"
fi

# --- 启动 ---
cd "${INSTALL_DIR}"
${COMPOSE} pull
${COMPOSE} up -d

echo
echo "--- 等待 MySQL 就绪 ---"
for i in $(seq 1 60); do
  if ${COMPOSE} exec -T mysql mysqladmin ping -h 127.0.0.1 -uroot -p"$(grep ^MYSQL_ROOT_PASSWORD= .env | cut -d= -f2-)" --silent 2>/dev/null; then
    ok "MySQL 已就绪"
    break
  fi
  sleep 2
  if [[ "$i" -eq 60 ]]; then
    fail "MySQL 启动超时，查看日志: cd ${INSTALL_DIR} && ${COMPOSE} logs mysql"
  fi
done

echo
${COMPOSE} ps
echo
ok "Docker 数据节点已启动"
echo
echo "========== 下一步 =========="
echo "1. 本机验证："
echo "   docker exec ai-manager-mysql mysql -u ai_manager -p ai_manager_admin -e \"SELECT 1;\""
echo "   docker exec ai-manager-redis redis-cli ping"
echo
echo "2. 局域网验证（112 或 Windows）："
echo "   mysql -h 192.168.0.116 -u ai_manager -p ai_manager_admin -e \"SELECT 1;\""
echo "   redis-cli -h 192.168.0.116 ping"
echo
echo "3. 导入 SQL（在开发机或 116 上，密码与 .env 中 MYSQL_PASSWORD 一致）："
echo "   bash ${REPO_ROOT}/deploy/scripts/import-sql-to-docker-mysql.sh /path/to/admin-backend/sql/schema.sql"
echo
echo "4. 应用节点 backend.env 保持："
echo "   MYSQL_HOST=192.168.0.116  REDIS_HOST=192.168.0.116"
echo
echo "日常命令："
echo "  cd ${INSTALL_DIR} && ${COMPOSE} logs -f"
echo "  cd ${INSTALL_DIR} && ${COMPOSE} restart"
echo "  cd ${INSTALL_DIR} && ${COMPOSE} down   # 停止（数据在 volume 中保留）"
