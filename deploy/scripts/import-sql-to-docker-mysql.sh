#!/usr/bin/env bash
# 向 Docker MySQL 导入 SQL 文件
# 用法：
#   bash deploy/scripts/import-sql-to-docker-mysql.sh admin-backend/sql/schema.sql
#   bash deploy/scripts/import-sql-to-docker-mysql.sh admin-backend/sql/*.sql

set -euo pipefail

CONTAINER="${MYSQL_CONTAINER:-ai-manager-mysql}"
ENV_FILE="${DATA_NODE_ENV:-/opt/ai-manager/data-node/.env}"

if [[ $# -lt 1 ]]; then
  echo "用法: $0 <file.sql> [file2.sql ...]"
  exit 1
fi

if [[ -f "${ENV_FILE}" ]]; then
  MYSQL_USER="$(grep ^MYSQL_USER= "${ENV_FILE}" | cut -d= -f2-)"
  MYSQL_PASSWORD="$(grep ^MYSQL_PASSWORD= "${ENV_FILE}" | cut -d= -f2-)"
  MYSQL_DATABASE="$(grep ^MYSQL_DATABASE= "${ENV_FILE}" | cut -d= -f2-)"
fi

MYSQL_USER="${MYSQL_USER:-ai_manager}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:?请设置 MYSQL_PASSWORD 或配置 ${ENV_FILE}}"
MYSQL_DATABASE="${MYSQL_DATABASE:-ai_manager_admin}"

if ! docker ps --format '{{.Names}}' | grep -qx "${CONTAINER}"; then
  echo "容器 ${CONTAINER} 未运行"
  exit 1
fi

for sql in "$@"; do
  if [[ ! -f "$sql" ]]; then
    echo "文件不存在: $sql"
    exit 1
  fi
  echo ">>> 导入 $sql"
  docker exec -i "${CONTAINER}" mysql \
    -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" \
    "${MYSQL_DATABASE}" < "$sql"
done

echo "完成"
