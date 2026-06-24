#!/usr/bin/env bash
# 兼容旧脚本名，转发到 check-mysql-lan.sh
exec "$(dirname "$0")/check-mysql-lan.sh" "$@"
