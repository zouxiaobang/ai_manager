#!/usr/bin/env python3
"""Build admin-web on Windows and deploy dist to Pi 114 via password SSH."""

from __future__ import annotations

import os
import subprocess
import sys
import tarfile
import tempfile
from pathlib import Path

try:
    import paramiko
except ImportError:
    print("Install paramiko: py -3 -m pip install paramiko", file=sys.stderr)
    sys.exit(1)

ROOT = Path(__file__).resolve().parents[2]
WEB_DIR = ROOT / "admin-web"
DIST_DIR = WEB_DIR / "dist"
REMOTE_HOST = os.environ.get("PI_HOST", "192.168.0.114")
REMOTE_USER = os.environ.get("PI_USER", "kyle")
REMOTE_PASSWORD = os.environ.get("PI_PASSWORD", "Asd123456")
WEB_ROOT = os.environ.get("WEB_ROOT", "/var/www/ai-manager")
REMOTE_TAR = "/tmp/ai-manager-dist.tar.gz"


def run(cmd: list[str], cwd: Path | None = None) -> None:
    print("$", " ".join(cmd))
    subprocess.run(cmd, cwd=cwd, check=True, shell=os.name == "nt")


def ssh_exec(client: paramiko.SSHClient, command: str) -> None:
    print(">>>", command)
    stdin, stdout, stderr = client.exec_command(command, timeout=600)
    out = stdout.read().decode(errors="replace")
    err = stderr.read().decode(errors="replace")
    code = stdout.channel.recv_exit_status()
    if out.strip():
        print(out.rstrip())
    if err.strip():
        print(err.rstrip(), file=sys.stderr)
    if code != 0:
        raise RuntimeError(f"Remote command failed ({code}): {command}")


def main() -> int:
    if not REMOTE_PASSWORD:
        print("Set PI_PASSWORD environment variable", file=sys.stderr)
        return 1

    print(f">>> Connect {REMOTE_USER}@{REMOTE_HOST} ...")
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(REMOTE_HOST, username=REMOTE_USER, password=REMOTE_PASSWORD, timeout=30)

    print(">>> Build frontend locally ...")
    run(["npm", "install"], cwd=WEB_DIR)
    run(["npm", "run", "build"], cwd=WEB_DIR)
    if not DIST_DIR.is_dir():
        print(f"Missing dist: {DIST_DIR}", file=sys.stderr)
        return 1

    with tempfile.NamedTemporaryFile(suffix=".tar.gz", delete=False) as tmp:
        tar_path = Path(tmp.name)
    try:
        print(f">>> Package dist -> {tar_path.name}")
        with tarfile.open(tar_path, "w:gz") as tar:
            tar.add(DIST_DIR, arcname="dist")

        size_mb = tar_path.stat().st_size / (1024 * 1024)
        print(f">>> Upload archive ({size_mb:.1f} MB) ...")
        sftp = client.open_sftp()
        try:
            sftp.put(str(tar_path), REMOTE_TAR)
        finally:
            sftp.close()

        install = (
            f"rm -rf /tmp/ai-manager-new && mkdir -p /tmp/ai-manager-new && "
            f"tar -xzf {REMOTE_TAR} -C /tmp/ai-manager-new --strip-components=1 && "
            f"sudo rsync -av --delete /tmp/ai-manager-new/ {WEB_ROOT}/ && "
            f"sudo chown -R www-data:www-data {WEB_ROOT} && "
            f"rm -f {REMOTE_TAR}"
        )
        print(f">>> Install to {WEB_ROOT} ...")
        ssh_exec(client, install)
    finally:
        tar_path.unlink(missing_ok=True)
        client.close()

    print(f"Done. Open http://{REMOTE_HOST}/#/home (Ctrl+F5)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
