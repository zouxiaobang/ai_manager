#!/usr/bin/env python3
"""
将本地 uploads/ecommerce 图片全量同步到百度网盘（按当前文件名上传）。

用法:
  py -3 sync_ecommerce_images_to_pan.py --dry-run
  py -3 sync_ecommerce_images_to_pan.py
  py -3 sync_ecommerce_images_to_pan.py --pan-dev
"""
from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

APP_KEY = "0FzEpqFsRX50bkxhGls0WUZfXvnDLL59"
SECRET_KEY = "mgOo20RU4AzgH8VBzKYV3aIH3CjVtxco"
XPAN_FILE = "https://pan.baidu.com/rest/2.0/xpan/file"
PAN_PROD_DIR = "/apps/ai_blog/ecommerce-images"
PAN_DEV_DIR = "/apps/ai_blog/dev/ecommerce-images"
IMAGE_EXT = re.compile(r"\.(jpg|jpeg|png|webp|gif)$", re.I)

SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ADMIN_BACKEND = SCRIPT_DIR.parent
LOCAL_UPLOAD_DIR = REPO_ADMIN_BACKEND / "uploads" / "ecommerce"


def mysql_query(sql: str) -> list[list[str]]:
    r = subprocess.run(
        [
            "mysql",
            "-u",
            "root",
            "-p123456",
            "-N",
            "-B",
            "--default-character-set=utf8mb4",
            "-D",
            "ai_manager_admin",
            "-e",
            sql,
        ],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    if r.returncode != 0:
        raise RuntimeError(r.stderr or r.stdout)
    rows: list[list[str]] = []
    for line in r.stdout.strip().splitlines():
        if line.strip():
            rows.append(line.split("\t"))
    return rows


def mysql_tokens() -> tuple[str, str]:
    rows = mysql_query("SELECT access_token, refresh_token FROM nb_baidu_pan_auth WHERE id=1 LIMIT 1;")
    if not rows:
        raise RuntimeError("未找到百度网盘授权记录 nb_baidu_pan_auth")
    return rows[0][0], rows[0][1]


def refresh_access_token(refresh_token: str) -> str:
    q = urllib.parse.urlencode(
        {
            "grant_type": "refresh_token",
            "refresh_token": refresh_token,
            "client_id": APP_KEY,
            "client_secret": SECRET_KEY,
        }
    )
    with urllib.request.urlopen(f"https://openapi.baidu.com/oauth/2.0/token?{q}", timeout=30) as resp:
        data = json.loads(resp.read().decode())
    token = data.get("access_token")
    if not token:
        raise RuntimeError(f"刷新 token 失败: {data}")
    return token


def api_post(access_token: str, params: dict, form: dict) -> dict:
    q = urllib.parse.urlencode({**params, "access_token": access_token})
    encoded = urllib.parse.urlencode(form).encode()
    req = urllib.request.Request(
        f"{XPAN_FILE}?{q}",
        data=encoded,
        method="POST",
        headers={"Content-Type": "application/x-www-form-urlencoded"},
    )
    with urllib.request.urlopen(req, timeout=180) as resp:
        body = resp.read().decode()
    data = json.loads(body)
    if data.get("errno", 0) != 0:
        raise RuntimeError(f"百度 API errno={data.get('errno')} body={body[:500]}")
    return data


def dir_exists(access_token: str, dir_path: str) -> bool:
    q = urllib.parse.urlencode(
        {"method": "list", "dir": dir_path, "limit": 1, "access_token": access_token}
    )
    with urllib.request.urlopen(f"{XPAN_FILE}?{q}", timeout=60) as resp:
        data = json.loads(resp.read().decode())
    return data.get("errno") != -9


def ensure_pan_dir(access_token: str, dir_path: str) -> None:
    parts = [p for p in dir_path.split("/") if p]
    current = ""
    for part in parts:
        current += "/" + part
        if dir_exists(access_token, current):
            continue
        api_post(
            access_token,
            {"method": "create"},
            {"path": current, "isdir": "1", "rtype": "2"},
        )


def pan_upload(access_token: str, pan_dir: str, file_name: str, content: bytes) -> str:
    """precreate → superfile2 → create。返回 uploaded / skipped。"""
    import hashlib
    from uuid import uuid4

    path = f"{pan_dir}/{file_name}"
    md5 = hashlib.md5(content).hexdigest()
    block_list = json.dumps([md5])

    pre = api_post(
        access_token,
        {"method": "precreate"},
        {
            "path": path,
            "size": str(len(content)),
            "isdir": "0",
            "autoinit": "1",
            "rtype": "3",
            "block_list": block_list,
        },
    )
    if pre.get("return_type") == 2:
        return "skipped"

    upload_id = pre.get("uploadid")
    if not upload_id:
        raise RuntimeError(f"precreate 无 uploadid: {pre}")

    locate_q = urllib.parse.urlencode(
        {
            "method": "locateupload",
            "appid": "250528",
            "access_token": access_token,
            "path": path,
            "uploadid": upload_id,
            "upload_version": "2.0",
        }
    )
    with urllib.request.urlopen(f"https://d.pcs.baidu.com/rest/2.0/pcs/file?{locate_q}", timeout=60) as resp:
        locate = json.loads(resp.read().decode())
    host = "https://c3.pcs.baidu.com"
    if locate.get("servers"):
        s = locate["servers"][0].get("server", "")
        if s:
            host = s if s.startswith("http") else f"https://{s}"
    elif locate.get("host"):
        h = locate["host"]
        host = h if h.startswith("http") else f"https://{h}"

    boundary = f"----BaiduPan{uuid4().hex}"
    part_header = (
        f"--{boundary}\r\n"
        f'Content-Disposition: form-data; name="file"; filename="{file_name}"\r\n'
        f"Content-Type: application/octet-stream\r\n\r\n"
    ).encode()
    part_footer = f"\r\n--{boundary}--\r\n".encode()
    body = part_header + content + part_footer

    upload_url = (
        f"{host.rstrip('/')}/rest/2.0/pcs/superfile2?method=upload"
        f"&access_token={urllib.parse.quote(access_token)}"
        f"&type=tmpfile&path={urllib.parse.quote(path)}"
        f"&uploadid={urllib.parse.quote(upload_id)}&partseq=0"
    )
    req = urllib.request.Request(
        upload_url,
        data=body,
        method="POST",
        headers={"Content-Type": f"multipart/form-data; boundary={boundary}"},
    )
    with urllib.request.urlopen(req, timeout=180) as resp:
        up_body = resp.read().decode()
    up_data = json.loads(up_body)
    if up_data.get("errno", 0) != 0:
        raise RuntimeError(f"分片上传失败: {up_body[:300]}")

    api_post(
        access_token,
        {"method": "create"},
        {
            "path": path,
            "size": str(len(content)),
            "isdir": "0",
            "rtype": "3",
            "uploadid": upload_id,
            "block_list": block_list,
        },
    )
    return "uploaded"


def list_local_images() -> list[Path]:
    if not LOCAL_UPLOAD_DIR.is_dir():
        return []
    files = [p for p in LOCAL_UPLOAD_DIR.iterdir() if p.is_file() and IMAGE_EXT.search(p.name)]
    return sorted(files, key=lambda p: p.name.lower())


def main() -> int:
    parser = argparse.ArgumentParser(description="本地电商图片同步到百度网盘")
    parser.add_argument("--dry-run", action="store_true", help="仅列出待上传文件")
    parser.add_argument("--pan-dev", action="store_true", help="同步到开发目录")
    parser.add_argument("--skip-existing", action="store_true", default=True, help="网盘已存在则跳过")
    args = parser.parse_args()

    files = list_local_images()
    pan_dir = PAN_DEV_DIR if args.pan_dev else PAN_PROD_DIR

    print(f"本地图片: {len(files)} 张")
    print(f"本地目录: {LOCAL_UPLOAD_DIR}")
    print(f"网盘目录: {pan_dir}")
    print(f"模式: {'预览' if args.dry_run else '执行'}")

    if args.dry_run:
        for path in files[:30]:
            print(f"  {path.name} ({path.stat().st_size} bytes)")
        if len(files) > 30:
            print(f"  ... 另有 {len(files) - 30} 张")
        return 0

    at, rt = mysql_tokens()
    try:
        ensure_pan_dir(at, pan_dir)
    except Exception:
        at = refresh_access_token(rt)
        ensure_pan_dir(at, pan_dir)

    uploaded = skipped = failed = 0
    failures: list[str] = []

    for i, path in enumerate(files, 1):
        try:
            content = path.read_bytes()
            result = pan_upload(at, pan_dir, path.name, content)
            if result == "skipped":
                skipped += 1
            else:
                uploaded += 1
        except Exception as ex:
            err = str(ex)
            if args.skip_existing and ("errno=-8" in err or "文件已存在" in err):
                skipped += 1
            else:
                failed += 1
                failures.append(f"{path.name}: {ex}")

        if i % 20 == 0 or i == len(files):
            print(f"进度 {i}/{len(files)} (上传 {uploaded}, 跳过 {skipped}, 失败 {failed})")
        time.sleep(0.15)

    print("\n=== 完成 ===")
    print(f"上传: {uploaded}, 跳过: {skipped}, 失败: {failed}")
    if failures:
        print("\n失败明细（前 20）:")
        for line in failures[:20]:
            print(f"  {line}")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
