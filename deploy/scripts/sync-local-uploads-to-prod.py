#!/usr/bin/env python3
"""将本地 admin-backend/uploads 同步到应用节点 /opt/ai-manager/backend/uploads"""

from __future__ import annotations

import argparse
import os
import stat
import sys
from pathlib import Path

try:
    import paramiko
except ImportError:
    print("请先安装: py -3 -m pip install paramiko", file=sys.stderr)
    sys.exit(1)

ROOT = Path(__file__).resolve().parents[2]
LOCAL_UPLOADS = ROOT / "admin-backend" / "uploads"
REMOTE_HOST = os.environ.get("PI_HOST", "192.168.0.114")
REMOTE_USER = os.environ.get("PI_USER", "kyle")
REMOTE_PASSWORD = os.environ.get("PI_PASSWORD", "Asd123456")
REMOTE_STAGING = "/tmp/ai-manager-uploads-sync"
REMOTE_UPLOADS = "/opt/ai-manager/backend/uploads"


def iter_files(base: Path):
    for path in base.rglob("*"):
        if path.is_file():
            yield path


def upload_tree(sftp: paramiko.SFTPClient, local_root: Path, remote_root: str) -> int:
    count = 0
    for local_file in iter_files(local_root):
        rel = local_file.relative_to(local_root).as_posix()
        remote_file = f"{remote_root}/{rel}".replace("//", "/")
        remote_dir = os.path.dirname(remote_file)
        parts = remote_dir.strip("/").split("/")
        cur = ""
        for part in parts:
            cur = f"{cur}/{part}" if cur else f"/{part}"
            try:
                sftp.stat(cur)
            except FileNotFoundError:
                sftp.mkdir(cur)
        sftp.put(str(local_file), remote_file)
        count += 1
        if count % 50 == 0:
            print(f"  已上传 {count} 个文件...")
    return count


def main() -> int:
    parser = argparse.ArgumentParser(description="同步本地 uploads 到线上应用节点")
    parser.add_argument("--dry-run", action="store_true", help="仅统计文件数")
    args = parser.parse_args()

    if not LOCAL_UPLOADS.is_dir():
        print(f"本地目录不存在: {LOCAL_UPLOADS}", file=sys.stderr)
        return 1

    files = list(iter_files(LOCAL_UPLOADS))
    print(f"本地 uploads 文件数: {len(files)}")
    if args.dry_run:
        return 0

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    print(f">>> 连接 {REMOTE_USER}@{REMOTE_HOST} ...")
    client.connect(REMOTE_HOST, username=REMOTE_USER, password=REMOTE_PASSWORD, timeout=30)

    stdin, stdout, stderr = client.exec_command(f"rm -rf {REMOTE_STAGING} && mkdir -p {REMOTE_STAGING}")
    stdout.channel.recv_exit_status()
    err = stderr.read().decode()
    if err.strip():
        print(err, file=sys.stderr)

    sftp = client.open_sftp()
    print(f">>> 上传到 {REMOTE_STAGING} ...")
    uploaded = upload_tree(sftp, LOCAL_UPLOADS, REMOTE_STAGING)
    sftp.close()

    install_cmd = (
        f"sudo rsync -av {REMOTE_STAGING}/ {REMOTE_UPLOADS}/ "
        f"&& sudo chown -R aimanager:aimanager {REMOTE_UPLOADS} "
        f"&& rm -rf {REMOTE_STAGING}"
    )
    print(f">>> 安装到 {REMOTE_UPLOADS} ...")
    stdin, stdout, stderr = client.exec_command(install_cmd)
    exit_code = stdout.channel.recv_exit_status()
    out = stdout.read().decode()
    err = stderr.read().decode()
    if out.strip():
        print(out)
    if err.strip():
        print(err, file=sys.stderr)
    client.close()

    if exit_code != 0:
        print(f"安装失败，退出码 {exit_code}", file=sys.stderr)
        return exit_code

    print(f"完成：共同步 {uploaded} 个文件到 {REMOTE_UPLOADS}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
