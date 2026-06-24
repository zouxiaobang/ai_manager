#!/usr/bin/env bash
# 在数据节点 192.168.0.116 上卸载原生 MariaDB / Redis（安装 Docker 版前执行）
# 用法：sudo bash uninstall-native-db-on-116.sh [--purge-data]

set -euo pipefail

PURGE_DATA=0
if [[ "${1:-}" == "--purge-data" ]]; then
  PURGE_DATA=1
fi

echo "=========================================="
echo " 卸载原生 MariaDB / Redis（116 数据节点）"
echo "=========================================="
echo
echo "将停止并卸载 apt 安装的 mariadb-server、redis-server。"
if [[ "$PURGE_DATA" -eq 1 ]]; then
  echo "【警告】--purge-data：将删除 /var/lib/mysql 等原生数据目录（不可恢复）"
else
  echo "原生数据目录默认保留；确认已备份后再加 --purge-data 删除。"
fi
echo
read -r -p "确认继续？[y/N] " ans
if [[ "${ans,,}" != "y" ]]; then
  echo "已取消"
  exit 0
fi

echo
echo "--- 停止服务 ---"
for svc in mariadb mysql redis-server; do
  if systemctl list-unit-files "${svc}.service" &>/dev/null; then
    sudo systemctl stop "${svc}" 2>/dev/null || true
    sudo systemctl disable "${svc}" 2>/dev/null || true
  fi
done

echo
echo "--- 卸载软件包 ---"
sudo apt-get update
sudo apt-get purge -y \
  mariadb-server \
  mariadb-client \
  mariadb-client-core \
  mariadb-server-core \
  mariadb-common \
  mysql-common \
  redis-server \
  redis-tools \
  2>/dev/null || true
sudo apt-get autoremove -y
sudo apt-get autoclean

if [[ "$PURGE_DATA" -eq 1 ]]; then
  echo
  echo "--- 删除原生数据目录 ---"
  sudo rm -rf /var/lib/mysql /var/lib/redis /etc/mysql /etc/redis
  echo "已删除 /var/lib/mysql、/var/lib/redis"
fi

echo
echo "完成。3306/6379 端口应已释放，可运行 setup-data-node-docker.sh 安装 Docker 版。"
