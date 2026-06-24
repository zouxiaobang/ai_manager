#!/usr/bin/env bash
# 在开发机构建前端并同步到树莓派（需已配置 SSH 免密或手动输入密码）
set -euo pipefail

PI_HOST="${PI_HOST:-kyle@192.168.0.114}"
WEB_ROOT="${WEB_ROOT:-/var/www/ai-manager}"

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

echo "==> 构建前端..."
cd "$ROOT/admin-web"
npm install
npm run build

echo "==> 上传到树莓派 $PI_HOST ..."
ssh "$PI_HOST" "mkdir -p /tmp/ai-manager-new"
rsync -av --delete dist/ "$PI_HOST:/tmp/ai-manager-new/"

echo "==> 安装到 Nginx 目录（需要 sudo）..."
ssh "$PI_HOST" "sudo rsync -av --delete /tmp/ai-manager-new/ $WEB_ROOT/ && sudo chown -R www-data:www-data $WEB_ROOT"

echo "完成。访问 http://192.168.0.114/#/home"
