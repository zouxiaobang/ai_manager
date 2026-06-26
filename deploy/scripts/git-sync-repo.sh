#!/usr/bin/env bash
# 将仓库同步到远端（ff-only 失败时 fetch + reset --hard，丢弃本地修改）
# 用法: source 本文件后调用 git_sync_repo "${ROOT}"

git_sync_repo() {
  local root="${1:?repo root required}"

  if [[ ! -d "${root}/.git" ]]; then
    return 0
  fi

  echo "==> 拉取最新代码..."
  if git -C "${root}" pull --ff-only; then
    echo "==> 当前 commit: $(git -C "${root}" rev-parse --short HEAD)"
    return 0
  fi

  echo "==> 拉取失败（本地有修改或无法快进），执行 git fetch + reset --hard 同步远端..."
  git -C "${root}" fetch origin

  local branch upstream
  branch="$(git -C "${root}" rev-parse --abbrev-ref HEAD)"
  upstream="$(git -C "${root}" rev-parse --abbrev-ref --symbolic-full-name '@{u}' 2>/dev/null || true)"

  if [[ -n "${upstream}" ]]; then
    git -C "${root}" reset --hard "${upstream}"
  elif git -C "${root}" show-ref --verify --quiet "refs/remotes/origin/${branch}"; then
    git -C "${root}" reset --hard "origin/${branch}"
  else
    git -C "${root}" reset --hard HEAD
    git -C "${root}" pull --ff-only
  fi

  echo "==> 已重置到: $(git -C "${root}" rev-parse --short HEAD)"
}
