#!/usr/bin/env bash
# 在应用节点 114 本机构建并安装前端（供 Web 一键部署 local 模式调用）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
WEB_ROOT="${WEB_ROOT:-/var/www/ai-manager}"
GIT_PULL="${GIT_PULL:-true}"

if [[ "${GIT_PULL}" == "true" ]] && [[ -d "${ROOT}/.git" ]]; then
  echo "==> 拉取最新代码..."
  git -C "${ROOT}" pull --ff-only
fi

echo "==> 构建前端..."
cd "${ROOT}/admin-web"
npm install
npm run build

if [[ ! -d "${ROOT}/admin-web/dist" ]]; then
  echo "未找到 dist 目录" >&2
  exit 1
fi

echo "==> 安装到 Nginx 目录 ${WEB_ROOT} ..."
sudo rsync -av --delete "${ROOT}/admin-web/dist/" "${WEB_ROOT}/"
sudo chown -R www-data:www-data "${WEB_ROOT}"

echo "完成。访问 http://127.0.0.1/#/home"
