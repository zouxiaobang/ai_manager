#!/usr/bin/env bash
# 在应用节点 114 本机构建并安装前端（供 Web 一键部署 local 模式调用）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=git-sync-repo.sh
source "${SCRIPT_DIR}/git-sync-repo.sh"
WEB_ROOT="${WEB_ROOT:-/var/www/ai-manager}"
GIT_PULL="${GIT_PULL:-true}"
BUILD_LOG="/tmp/ai-manager-frontend-build.log"

# Pi 内存有限：限制 Node 堆、单线程安装、低优先级构建，避免 rendering chunks 阶段 OOM 拖垮后端
export NODE_OPTIONS="${NODE_OPTIONS:---max-old-space-size=1024}"
export npm_config_jobs="${npm_config_jobs:-1}"
export PI_BUILD=1

if [[ "${GIT_PULL}" == "true" ]]; then
  git_sync_repo "${ROOT}"
fi

cd "${ROOT}/admin-web"

if [[ ! -d node_modules ]] \
  || [[ package.json -nt node_modules ]] \
  || [[ package-lock.json -nt node_modules ]]; then
  echo "==> 安装依赖..."
  npm install
else
  echo "==> 跳过 npm install（依赖未变更）"
fi

echo "==> 构建前端（Pi 上可能需要 8～20 分钟，rendering chunks 阶段日志可能暂停数分钟）..."
echo "    完整日志：${BUILD_LOG}"
: > "${BUILD_LOG}"

run_build() {
  if command -v systemd-run >/dev/null 2>&1; then
    echo "==> 使用 systemd-run 限制构建内存（OOM 时仅终止构建进程，不影响后端）..."
    systemd-run --wait --pipe --collect \
      -p MemoryMax=1500M \
      -p MemoryHigh=1300M \
      -p OOMScoreAdjust=500 \
      -p CPUQuota=90% \
      --working-directory="${ROOT}/admin-web" \
      --setenv=NODE_OPTIONS="${NODE_OPTIONS}" \
      --setenv=PI_BUILD=1 \
      --setenv=npm_config_jobs=1 \
      /usr/bin/nice -n 15 /usr/bin/npm run build:pi
  else
    nice -n 15 npm run build:pi
  fi
}

set +o pipefail
run_build 2>&1 | tee -a "${BUILD_LOG}"
build_status=${PIPESTATUS[0]}
set -o pipefail

if [[ "${build_status}" -ne 0 ]]; then
  echo "前端构建失败，退出码：${build_status}" >&2
  echo "请查看 ${BUILD_LOG}；若出现 Killed，多为内存不足，建议在 Windows 执行 deploy-frontend.ps1" >&2
  exit "${build_status}"
fi

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
