#!/usr/bin/env bash
# 在数据节点 192.168.0.116 上运行，排查 MySQL 局域网无法连接（原生 MariaDB 或 Docker MySQL）
# 用法：bash check-mysql-lan.sh

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

ok() { echo -e "${GREEN}[OK]${NC} $*"; }
warn() { echo -e "${YELLOW}[!!]${NC} $*"; }
fail() { echo -e "${RED}[FAIL]${NC} $*"; }

echo "========== MySQL 局域网连接诊断（本机 $(hostname -I 2>/dev/null | awk '{print $1}')）=========="
echo

USE_DOCKER=0
if docker ps --format '{{.Names}}' 2>/dev/null | grep -qx 'ai-manager-mysql'; then
  USE_DOCKER=1
  ok "检测到 Docker 容器 ai-manager-mysql"
elif systemctl is-active --quiet mariadb 2>/dev/null || systemctl is-active --quiet mysql 2>/dev/null; then
  ok "MariaDB/MySQL 原生服务运行中"
else
  fail "未检测到 ai-manager-mysql 容器，也未运行 mariadb/mysql 服务"
  warn "Docker 安装：bash deploy/scripts/setup-data-node-docker.sh"
  exit 1
fi

echo
echo "--- 端口监听 ---"
if command -v ss >/dev/null; then
  ss -tlnp | grep -E ':3306|:6379' || true
else
  netstat -tlnp 2>/dev/null | grep -E '3306|6379' || true
fi

LISTEN=$(ss -tln 2>/dev/null | grep ':3306 ' || true)
if echo "$LISTEN" | grep -q '127.0.0.1:3306'; then
  if ! echo "$LISTEN" | grep -qE '0\.0\.0\.0:3306|\*:3306|\[::\]:3306'; then
    fail "仅监听 127.0.0.1:3306，局域网无法连入"
    if [[ "$USE_DOCKER" -eq 1 ]]; then
      warn "检查 docker-compose.yml ports 是否为 0.0.0.0:3306:3306"
    else
      warn "修复：bind-address = 0.0.0.0 并 restart mariadb"
    fi
  fi
elif echo "$LISTEN" | grep -qE '0\.0\.0\.0:3306|\*:3306'; then
  ok "已监听 0.0.0.0:3306（允许局域网）"
else
  warn "未检测到 3306 监听"
fi

echo
echo "--- 远程用户 ai_manager ---"
if [[ "$USE_DOCKER" -eq 1 ]]; then
  ENV_FILE="/opt/ai-manager/data-node/.env"
  ROOT_PASS=""
  if [[ -f "$ENV_FILE" ]]; then
    ROOT_PASS=$(grep ^MYSQL_ROOT_PASSWORD= "$ENV_FILE" | cut -d= -f2-)
  fi
  if [[ -n "$ROOT_PASS" ]]; then
    docker exec ai-manager-mysql mysql -uroot -p"${ROOT_PASS}" -N -e \
      "SELECT User, Host FROM mysql.user WHERE User='ai_manager';" 2>/dev/null || warn "无法查询用户表"
  else
    warn "未找到 ${ENV_FILE}，跳过用户查询"
  fi
else
  sudo mariadb -N -e "SELECT User, Host FROM mysql.user WHERE User='ai_manager';" 2>/dev/null \
    || warn "无法查询用户表"
fi

echo
echo "--- Redis ---"
if docker ps --format '{{.Names}}' 2>/dev/null | grep -qx 'ai-manager-redis'; then
  docker exec ai-manager-redis redis-cli ping && ok "Docker Redis PONG"
elif systemctl is-active --quiet redis-server 2>/dev/null; then
  redis-cli ping && ok "原生 Redis PONG"
else
  warn "Redis 未运行"
fi

echo
echo "--- 防火墙 ---"
if command -v ufw >/dev/null && ufw status 2>/dev/null | grep -q 'Status: active'; then
  ufw status | grep -E '3306|6379' || warn "ufw 已启用但未放行 3306/6379"
else
  ok "ufw 未启用或未安装"
fi

echo
echo "--- 本机 IP ---"
hostname -I

echo
echo "========== 客户端测试 =========="
echo "  mysql -h 192.168.0.116 -u ai_manager -p ai_manager_admin -e \"SELECT 1;\""
echo "  redis-cli -h 192.168.0.116 ping"
echo "  powershell: Test-NetConnection 192.168.0.116 -Port 3306"
