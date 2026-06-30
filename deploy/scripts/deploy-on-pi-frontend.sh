#!/usr/bin/env bash
# 在应用节点 114 本机构建并安装前端（供 Web 一键部署 local 模式调用）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=git-sync-repo.sh
source "${SCRIPT_DIR}/git-sync-repo.sh"
WEB_ROOT="${WEB_ROOT:-/var/www/ai-manager}"
GIT_PULL="${GIT_PULL:-true}"

# Pi 上 Vite 构建较吃内存，限制堆大小并降低对后端 JVM 的 CPU 抢占
export NODE_OPTIONS="${NODE_OPTIONS:---max-old-space-size=1536}"
export npm_config_jobs="${npm_config_jobs:-1}"

if [[ "${GIT_PULL}" == "true" ]]; then
  git_sync_repo "${ROOT}"
fi

echo "==> 构建前端（Pi 上可能需要 5～15 分钟，日志长时间无输出属正常）..."
cd "${ROOT}/admin-web"
npm install
nice -n 10 npm run build:pi

if [[ ! -d "${ROOT}/admin-web/dist" ]]; then
  echo "未找到 dist 目录" >&2
  exit 1
fi

STAGING_DIR="$(mktemp -d /tmp/ai-manager-web.XXXXXX)"
trap 'rm -rf "${STAGING_DIR}"' EXIT
rsync -a "${ROOT}/admin-web/dist/" "${STAGING_DIR}/"

echo "==> 安装到 Nginx 目录 ${WEB_ROOT} ..."
sudo rsync -av --delete "${STAGING_DIR}/" "${WEB_ROOT}/"
sudo chown -R www-data:www-data "${WEB_ROOT}"

echo "==> 前端部署完成"
echo "完成。访问 http://127.0.0.1/#/home"
