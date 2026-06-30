#!/usr/bin/env python3
"""
从旧系统腾讯云 COS 下载电商 SKU 图片到本地 uploads/ecommerce/，并同步到百度网盘。

用法:
  py -3 migrate_ecommerce_images.py              # 下载 + 网盘同步（生产目录）
  py -3 migrate_ecommerce_images.py --dry-run    # 仅预览
  py -3 migrate_ecommerce_images.py --local-only   # 只下载本地，不上传网盘
  py -3 migrate_ecommerce_images.py --pan-dev      # 网盘写入 /apps/ai_blog/dev/ecommerce-images
"""
from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

APP_KEY = "0FzEpqFsRX50bkxhGls0WUZfXvnDLL59"
SECRET_KEY = "mgOo20RU4AzgH8VBzKYV3aIH3CjVtxco"
XPAN_FILE = "https://pan.baidu.com/rest/2.0/xpan/file"
COS_PREFIX = "https://kyle-1257887000.cos.ap-nanjing.myqcloud.com/"
PAN_PROD_DIR = "/apps/ai_blog/ecommerce-images"
PAN_DEV_DIR = "/apps/ai_blog/dev/ecommerce-images"

SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ADMIN_BACKEND = SCRIPT_DIR.parent
LOCAL_UPLOAD_DIR = REPO_ADMIN_BACKEND / "uploads" / "ecommerce"


def mysql_query(sql: str) -> list[list[str]]:
    r = subprocess.run(
        ["mysql", "-u", "root", "-p123456", "-N", "-B", "--default-character-set=utf8mb4", "-D", "ai_manager_admin", "-e", sql],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    if r.returncode != 0:
        raise RuntimeError(r.stderr or r.stdout)
    rows = []
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


def pan_upload(access_token: str, pan_dir: str, file_name: str, content: bytes) -> None:
    """precreate → superfile2 → create（与 BaiduPanClient 一致）"""
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
        return

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


def normalize_image_name(raw: str) -> str:
    name = raw.strip().replace("\\", "/")
    if name.startswith("http://") or name.startswith("https://"):
        path = urllib.parse.urlparse(name).path
        name = path.rsplit("/", 1)[-1]
    return name.lstrip("/")


def cos_url(image_name: str) -> str:
    encoded = "/".join(urllib.parse.quote(seg) for seg in image_name.split("/"))
    return COS_PREFIX.rstrip("/") + "/" + encoded


def safe_local_path(base: Path, file_name: str) -> Path:
    target = (base / file_name).resolve()
    if not str(target).startswith(str(base.resolve())):
        raise ValueError(f"非法文件名: {file_name}")
    return target


def fetch_distinct_images() -> list[str]:
    rows = mysql_query(
        "SELECT DISTINCT image_name FROM ec_sku "
        "WHERE image_name IS NOT NULL AND TRIM(image_name) <> '' "
        "ORDER BY image_name;"
    )
    names = []
    seen = set()
    for row in rows:
        n = normalize_image_name(row[0])
        if not n or n in seen:
            continue
        if not re.search(r"\.(jpg|jpeg|png|webp|gif)$", n, re.I):
            continue
        seen.add(n)
        names.append(n)
    return names


def download_image(url: str, timeout: int = 60) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": "ai-manager-migration/1.0"})
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        data = resp.read()
    if len(data) < 16:
        raise RuntimeError("文件过小，可能不是有效图片")
    if data[:1] == b"{":
        raise RuntimeError("返回 JSON 错误而非图片")
    return data


def main() -> int:
    parser = argparse.ArgumentParser(description="迁移电商 SKU 图片 COS → 本地 → 百度网盘")
    parser.add_argument("--dry-run", action="store_true", help="仅列出待处理文件")
    parser.add_argument("--local-only", action="store_true", help="只下载到本地")
    parser.add_argument("--pan-dev", action="store_true", help="网盘写入开发目录")
    parser.add_argument("--skip-existing", action="store_true", default=True, help="本地已存在则跳过下载")
    args = parser.parse_args()

    images = fetch_distinct_images()
    pan_dir = PAN_DEV_DIR if args.pan_dev else PAN_PROD_DIR
    LOCAL_UPLOAD_DIR.mkdir(parents=True, exist_ok=True)

    print(f"待处理图片: {len(images)}")
    print(f"本地目录: {LOCAL_UPLOAD_DIR}")
    print(f"网盘目录: {pan_dir}")

    if args.dry_run:
        for name in images[:20]:
            print(f"  {name} <- {cos_url(name)}")
        if len(images) > 20:
            print(f"  ... 另有 {len(images) - 20} 张")
        return 0

    access_token = None
    if not args.local_only:
        at, rt = mysql_tokens()
        try:
            ensure_pan_dir(at, pan_dir)
        except Exception:
            at = refresh_access_token(rt)
            ensure_pan_dir(at, pan_dir)
        access_token = at

    ok_local = skip_local = fail = 0
    ok_pan = skip_pan = fail_pan = 0
    failures: list[str] = []

    for i, name in enumerate(images, 1):
        local_path = safe_local_path(LOCAL_UPLOAD_DIR, name)
        local_path.parent.mkdir(parents=True, exist_ok=True)
        url = cos_url(name)

        try:
            if args.skip_existing and local_path.is_file() and local_path.stat().st_size > 0:
                content = local_path.read_bytes()
                skip_local += 1
            else:
                content = download_image(url)
                local_path.write_bytes(content)
                ok_local += 1

            if not args.local_only and access_token:
                try:
                    pan_upload(access_token, pan_dir, name, content)
                    ok_pan += 1
                except Exception as pan_ex:
                    err = str(pan_ex)
                    if "errno=-8" in err or "文件已存在" in err:
                        skip_pan += 1
                    else:
                        fail_pan += 1
                        failures.append(f"PAN {name}: {pan_ex}")

            if i % 20 == 0 or i == len(images):
                print(f"进度 {i}/{len(images)} ...")

        except Exception as ex:
            fail += 1
            failures.append(f"DL {name}: {ex}")
            time.sleep(0.3)

    print("\n=== 完成 ===")
    print(f"本地: 下载 {ok_local}, 跳过 {skip_local}, 失败 {fail}")
    if not args.local_only:
        print(f"网盘: 上传 {ok_pan}, 跳过 {skip_pan}, 失败 {fail_pan}")
    if failures:
        print("\n失败明细（前 20）:")
        for line in failures[:20]:
            print(f"  {line}")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
