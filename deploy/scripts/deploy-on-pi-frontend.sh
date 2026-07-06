#!/usr/bin/env bash
# 在应用节点 114 上一键部署前端（供 Web 一键部署 local 模式调用）
#
# 策略 1（推荐）：通过 SSH 远程在 Windows 开发机构建并上传到本机
#   避免 Pi 内存不足。需 Windows 运行 OpenSSH Server 且 Pi 能免密登录。
#   环境变量：WINDOWS_DEV_HOST（默认 kyle@192.168.0.119）
#
# 策略 2（回退）：在 Pi 本地构建
#   自动检测内存并配置 swap，使用低内存模式避免 OOM。
#   环境变量：NODE_OPTIONS（默认 --max-old-space-size=512）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=git-sync-repo.sh
source "${SCRIPT_DIR}/git-sync-repo.sh"
WEB_ROOT="${WEB_ROOT:-/var/www/ai-manager}"
GIT_PULL="${GIT_PULL:-true}"

# Windows 开发机 SSH 目标（策略 1 使用）
WINDOWS_DEV_HOST="${WINDOWS_DEV_HOST:-kyle@192.168.0.119}"

# ============================================================
# 策略 1：通过 SSH 触发 Windows 开发机构建并上传
# ============================================================
try_remote_build() {
  local ssh_opts=(
    -o BatchMode=yes
    -o ConnectTimeout=5
    -o StrictHostKeyChecking=accept-new
  )

  if ! ssh "${ssh_opts[@]}" "${WINDOWS_DEV_HOST}" "echo ssh-ok" 2>/dev/null; then
    echo "==> Windows 开发机 SSH 不可用（${WINDOWS_DEV_HOST}），跳过远程构建"
    return 1
  fi

  echo "==> Windows 开发机 SSH 连接成功，触发远程构建..."
  echo "    开发机：${WINDOWS_DEV_HOST}"
  echo "    脚本：deploy/scripts/deploy-frontend.ps1"

  # Windows 上 PowerShell 执行部署脚本（构建 + scp 上传到本机）
  if ssh "${ssh_opts[@]}" "${WINDOWS_DEV_HOST}" \
    "powershell -ExecutionPolicy Bypass -File deploy/scripts/deploy-frontend.ps1"; then
    echo "==> Windows 远程构建并上传完成"
    return 0
  else
    echo "==> Windows 远程构建失败，回退到本机构建..." >&2
    return 1
  fi
}

# ============================================================
# 策略 2：Pi 本地构建
# ============================================================
ensure_swap() {
  # 可用内存 < 2GB 时添加 2GB swap
  local mem_mb
  mem_mb=$(free -m | awk '/^Mem:/{print $2}')
  if [[ -z "$mem_mb" || "$mem_mb" -ge 2048 ]]; then
    return 0
  fi

  echo "==> 可用内存 ${mem_mb}MB < 2GB，配置 2GB swap..."
  if grep -q '/swapfile' /proc/swaps 2>/dev/null; then
    echo "    已有 swapfile，跳过"
    return 0
  fi

  # 检查磁盘空间
  local avail_mb
  avail_mb=$(df -m / 2>/dev/null | awk 'NR==2{print $4}')
  if [[ -n "$avail_mb" && "$avail_mb" -lt 2560 ]]; then
    echo "    警告：磁盘剩余仅 ${avail_mb}MB，可能不足 2GB swap" >&2
  fi

  sudo fallocate -l 2G /swapfile 2>/dev/null || sudo dd if=/dev/zero of=/swapfile bs=1M count=2048 status=none
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile 2>/dev/null
  sudo swapon /swapfile 2>/dev/null && echo "    swap 已激活"
}

build_locally() {
  cd "${ROOT}/admin-web"

  # 极低内存模式 — 512MB 堆上限、单线程、低优先级
  export NODE_OPTIONS="${NODE_OPTIONS:---max-old-space-size=512}"
  export npm_config_jobs="${npm_config_jobs:-1}"
  export PI_BUILD=1

  if [[ ! -d node_modules ]] \
    || [[ package.json -nt node_modules ]] \
    || [[ package-lock.json -nt node_modules ]]; then
    echo "==> 安装依赖..."
    npm install
  else
    echo "==> 跳过 npm install（依赖未变更）"
  fi

  local build_log="/tmp/ai-manager-frontend-build.log"
  echo "==> 构建前端（Pi 上可能需 8～20 分钟，rendering chunks 阶段日志可能暂停数分钟）..."
  echo "    完整日志：${build_log}"
  : > "${build_log}"

  set +o pipefail
  nice -n 15 npm run build:pi 2>&1 | tee -a "${build_log}"
  local status=${PIPESTATUS[0]}
  set -o pipefail

  if [[ "$status" -ne 0 ]]; then
    echo "前端构建失败，退出码：${status}" >&2
    echo "请查看 ${build_log}；若出现 Killed，说明内存仍不足，建议配置 Windows 远程构建" >&2
    return "$status"
  fi
  return 0
}

# ============================================================
# 安装到 Nginx web 根目录
# ============================================================
install_to_webroot() {
  if [[ ! -d "${ROOT}/admin-web/dist" ]]; then
    echo "未找到 dist 目录" >&2
    return 1
  fi

  local staging_dir
  staging_dir="$(mktemp -d /tmp/ai-manager-web.XXXXXX)"
  trap 'rm -rf "${staging_dir}"' RETURN
  rsync -a "${ROOT}/admin-web/dist/" "${staging_dir}/"

  echo "==> 安装到 Nginx 目录 ${WEB_ROOT} ..."
  sudo rsync -av --delete "${staging_dir}/" "${WEB_ROOT}/"
  sudo chown -R www-data:www-data "${WEB_ROOT}"
}

# ============================================================
# 主流程
# ============================================================
if [[ "${GIT_PULL}" == "true" ]]; then
  git_sync_repo "${ROOT}"
fi

# 策略 1：远程 Windows 构建
if try_remote_build; then
  # 远程脚本已处理上传 + 安装，无需再次 install_to_webroot
  echo "完成。访问 http://127.0.0.1/#/home"
  exit 0
fi

# 策略 2：本地构建
ensure_swap
build_locally || exit $?
install_to_webroot

echo "==> 前端部署完成"
echo "完成。访问 http://127.0.0.1/#/home"
