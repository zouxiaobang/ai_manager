#!/usr/bin/env python3
"""Inspect and reorganize Baidu Pan files for ai_manager project."""
from __future__ import annotations

import json
import re
import subprocess
import sys
import urllib.parse
import urllib.request
from dataclasses import dataclass

APP_KEY = "0FzEpqFsRX50bkxhGls0WUZfXvnDLL59"
SECRET_KEY = "mgOo20RU4AzgH8VBzKYV3aIH3CjVtxco"
XPAN_FILE = "https://pan.baidu.com/rest/2.0/xpan/file"
PROD_ROOT = "/apps/ai_blog"
DEV_ROOT = "/apps/ai_blog/dev"

CANONICAL_PROD_DIRS = [
    f"{PROD_ROOT}/notes",
    f"{PROD_ROOT}/trash",
    f"{PROD_ROOT}/images",
    f"{PROD_ROOT}/ecommerce-images",
    f"{PROD_ROOT}/imports",
    f"{PROD_ROOT}/imports/sales-orders",
]

CANONICAL_DEV_DIRS = [
    f"{DEV_ROOT}/notes",
    f"{DEV_ROOT}/trash",
    f"{DEV_ROOT}/images",
    f"{DEV_ROOT}/ecommerce-images",
    f"{DEV_ROOT}/imports",
    f"{DEV_ROOT}/imports/sales-orders",
]

SUFFIX_DIR_PATTERN = re.compile(r"^(notes|images|trash|sales-orders)_20\d{6}_\d{6}$")
TOP_DUP_APP_PATTERN = re.compile(r"^ai_blog_20\d{6}_\d{6}$")
IMPORTS_SUFFIX_PATTERN = re.compile(r"^imports_20\d{6}_\d{6}$")

IMAGE_EXTS = {".jpg", ".jpeg", ".png", ".webp", ".gif"}
IMPORT_EXTS = {".xlsx", ".xls", ".csv"}


@dataclass
class PanEntry:
    path: str
    name: str
    is_dir: int
    size: int


def mysql_tokens() -> tuple[str, str]:
    r = subprocess.run(
        [
            "mysql",
            "-u",
            "root",
            "-p123456",
            "-N",
            "-D",
            "ai_manager_admin",
            "-e",
            "SELECT access_token, refresh_token FROM nb_baidu_pan_auth WHERE id=1;",
        ],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    if r.returncode != 0 or not r.stdout.strip():
        raise RuntimeError(f"mysql failed: {r.stderr}")
    parts = r.stdout.strip().split("\t")
    return parts[0], parts[1]


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
        raise RuntimeError(f"refresh token failed: {data}")
    return token


def api_get(access_token: str, params: dict) -> dict:
    q = urllib.parse.urlencode({**params, "access_token": access_token})
    with urllib.request.urlopen(f"{XPAN_FILE}?{q}", timeout=60) as resp:
        body = resp.read().decode()
    data = json.loads(body)
    errno = data.get("errno", 0)
    if errno not in (0, -9):
        raise RuntimeError(f"API errno={errno} body={body[:500]}")
    if data.get("error_code", 0) not in (0, None):
        raise RuntimeError(f"API error_code={data.get('error_code')} body={body[:500]}")
    return data


def api_post(access_token: str, params: dict, form: dict) -> dict:
    q = urllib.parse.urlencode({**params, "access_token": access_token})
    encoded = urllib.parse.urlencode(form).encode()
    req = urllib.request.Request(
        f"{XPAN_FILE}?{q}",
        data=encoded,
        method="POST",
        headers={"Content-Type": "application/x-www-form-urlencoded"},
    )
    with urllib.request.urlopen(req, timeout=120) as resp:
        body = resp.read().decode()
    data = json.loads(body)
    if data.get("errno", 0) != 0:
        raise RuntimeError(f"API errno={data.get('errno')} body={body[:800]}")
    return data


def list_dir(access_token: str, dir_path: str) -> list[PanEntry]:
    data = api_get(
        access_token,
        {"method": "list", "dir": dir_path, "limit": 1000, "start": 0},
    )
    if data.get("errno") == -9:
        return []
    entries: list[PanEntry] = []
    for item in data.get("list") or []:
        path = item.get("path") or f"{dir_path.rstrip('/')}/{item.get('server_filename', '')}"
        entries.append(
            PanEntry(
                path=path,
                name=item.get("server_filename", ""),
                is_dir=int(item.get("isdir", 0)),
                size=int(item.get("size", 0)),
            )
        )
    return entries


def dir_exists(access_token: str, dir_path: str) -> bool:
    data = api_get(
        access_token,
        {"method": "list", "dir": dir_path, "limit": 1, "start": 0},
    )
    return data.get("errno") != -9


def ensure_dir(access_token: str, dir_path: str) -> None:
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


def move_file(access_token: str, src_path: str, dest_dir: str, newname: str) -> None:
    filelist = json.dumps([{"path": src_path, "dest": dest_dir, "newname": newname}], ensure_ascii=False)
    api_post(
        access_token,
        {"method": "filemanager", "opera": "move", "async": "0"},
        {"filelist": filelist},
    )


def delete_path(access_token: str, path: str) -> None:
    filelist = json.dumps([{"path": path}], ensure_ascii=False)
    api_post(
        access_token,
        {"method": "filemanager", "opera": "delete", "async": "0"},
        {"filelist": filelist},
    )


def canonical_target_for_root(root: str, dir_name: str) -> str:
    prefix = dir_name.split("_")[0]
    if prefix == "sales-orders":
        return f"{root}/imports/sales-orders"
    if prefix in {"notes", "images", "trash"}:
        return f"{root}/{prefix}"
    return f"{root}/notes"


def classify_loose_file(name: str) -> str:
    lower = name.lower()
    if lower.endswith(".html"):
        return f"{PROD_ROOT}/notes"
    for ext in IMAGE_EXTS:
        if lower.endswith(ext):
            return f"{PROD_ROOT}/ecommerce-images"
    for ext in IMPORT_EXTS:
        if lower.endswith(ext):
            return f"{PROD_ROOT}/imports/sales-orders"
    return f"{PROD_ROOT}/notes"


def merge_duplicate_app_root(access_token: str, moves: list[dict]) -> None:
    """Merge /apps/ai_blog_YYYYMMDD_HHMMSS/* into canonical /apps/ai_blog/*"""
    for e in list_dir(access_token, "/apps"):
        if e.is_dir != 1 or not TOP_DUP_APP_PATTERN.match(e.name):
            continue
        for child in list_dir(access_token, e.path):
            if child.is_dir == 1:
                if child.name in {"notes", "images", "trash", "imports"}:
                    if child.name == "imports":
                        target = f"{PROD_ROOT}/imports/sales-orders"
                        for grand in list_dir(access_token, child.path):
                            if grand.is_dir == 0:
                                moves.append(
                                    {
                                        "action": "move",
                                        "src": grand.path,
                                        "dest_dir": target,
                                        "newname": grand.name,
                                        "reason": f"from {e.name}/imports",
                                    }
                                )
                    else:
                        for grand in list_dir(access_token, child.path):
                            if grand.is_dir == 0:
                                moves.append(
                                    {
                                        "action": "move",
                                        "src": grand.path,
                                        "dest_dir": f"{PROD_ROOT}/{child.name}",
                                        "newname": grand.name,
                                        "reason": f"from {e.name}/{child.name}",
                                    }
                                )
                else:
                    for grand in list_dir(access_token, child.path):
                        if grand.is_dir == 0:
                            moves.append(
                                {
                                    "action": "move",
                                    "src": grand.path,
                                    "dest_dir": classify_loose_file(grand.name),
                                    "newname": grand.name,
                                    "reason": f"from {e.name}/{child.name}",
                                }
                            )
            else:
                moves.append(
                    {
                        "action": "move",
                        "src": child.path,
                        "dest_dir": classify_loose_file(child.name),
                        "newname": child.name,
                        "reason": f"from {e.name}",
                    }
                )


def plan_reorganize(access_token: str) -> list[dict]:
    actions: list[dict] = []

    for d in CANONICAL_PROD_DIRS + CANONICAL_DEV_DIRS:
        actions.append({"action": "ensure", "path": d, "reason": "canonical structure"})

    merge_duplicate_app_root(access_token, actions)

    for root in (PROD_ROOT, DEV_ROOT):
        for e in list_dir(access_token, root):
            if e.is_dir == 1 and (
                SUFFIX_DIR_PATTERN.match(e.name) or IMPORTS_SUFFIX_PATTERN.match(e.name)
            ):
                children = list_dir(access_token, e.path)
                for child in children:
                    if child.is_dir == 0:
                        dest = (
                            canonical_target_for_root(root, e.name)
                            if SUFFIX_DIR_PATTERN.match(e.name)
                            else f"{root}/imports/sales-orders"
                        )
                        actions.append(
                            {
                                "action": "move",
                                "src": child.path,
                                "dest_dir": dest or f"{root}/notes",
                                "newname": child.name,
                                "reason": f"suffix dir {e.name}",
                            }
                        )
                if not children:
                    actions.append({"action": "delete", "path": e.path, "reason": f"empty suffix dir {e.name}"})
            elif e.is_dir == 0 and root == PROD_ROOT:
                actions.append(
                    {
                        "action": "move",
                        "src": e.path,
                        "dest_dir": classify_loose_file(e.name),
                        "newname": e.name,
                        "reason": "loose file at prod root",
                    }
                )

    imports_dir = f"{PROD_ROOT}/imports"
    for e in list_dir(access_token, imports_dir):
        if e.is_dir == 1 and e.name.startswith("sales-orders_"):
            children = list_dir(access_token, e.path)
            for child in children:
                if child.is_dir == 0:
                    actions.append(
                        {
                            "action": "move",
                            "src": child.path,
                            "dest_dir": f"{PROD_ROOT}/imports/sales-orders",
                            "newname": child.name,
                            "reason": f"suffix imports dir {e.name}",
                        }
                    )
            if not children:
                actions.append({"action": "delete", "path": e.path, "reason": f"empty imports suffix {e.name}"})

    for e in list_dir(access_token, "/apps"):
        if e.is_dir == 1 and TOP_DUP_APP_PATTERN.match(e.name):
            if not list_dir(access_token, e.path):
                actions.append({"action": "delete", "path": e.path, "reason": f"empty duplicate app {e.name}"})

    # re-check empty suffix dirs after moves
    for root in (PROD_ROOT, DEV_ROOT):
        for e in list_dir(access_token, root):
            if e.is_dir == 1 and (
                SUFFIX_DIR_PATTERN.match(e.name) or IMPORTS_SUFFIX_PATTERN.match(e.name)
            ) and not list_dir(access_token, e.path):
                actions.append({"action": "delete", "path": e.path, "reason": f"empty suffix dir {e.name}"})

    # dedupe
    seen = set()
    unique = []
    for a in actions:
        key = (a.get("action"), a.get("path"), a.get("src"))
        if key in seen:
            continue
        seen.add(key)
        unique.append(a)
    return unique


def execute_actions(access_token: str, actions: list[dict], dry_run: bool) -> None:
    for a in actions:
        action = a["action"]
        if action == "ensure":
            if dry_run:
                print(f"[ensure] {a['path']}")
            else:
                ensure_dir(access_token, a["path"])
                print(f"ENSURED {a['path']}")
            continue
        if action == "move":
            if dry_run:
                print(f"[move] {a['src']} -> {a['dest_dir']}/{a['newname']} ({a['reason']})")
            else:
                ensure_dir(access_token, a["dest_dir"])
                move_file(access_token, a["src"], a["dest_dir"], a["newname"])
                print(f"MOVED {a['src']} -> {a['dest_dir']}/{a['newname']}")
            continue
        if action == "delete":
            if dry_run:
                print(f"[delete] {a['path']} ({a['reason']})")
            else:
                delete_path(access_token, a["path"])
                print(f"DELETED {a['path']}")


def scan_summary(access_token: str) -> None:
    print("=== /apps top-level ===")
    for e in list_dir(access_token, "/apps"):
        if e.is_dir != 1:
            continue
        kids = list_dir(access_token, e.path)
        print(f"{e.name}: {len(kids)} children")

    print(f"\n=== {PROD_ROOT} canonical usage ===")
    for sub in ["notes", "images", "trash", "ecommerce-images", "imports/sales-orders"]:
        path = f"{PROD_ROOT}/{sub}"
        try:
            items = list_dir(access_token, path)
            print(f"{sub}: {len(items)} items")
        except Exception as ex:
            print(f"{sub}: {ex}")

    print(f"\n=== {DEV_ROOT} structure ===")
    for e in list_dir(access_token, DEV_ROOT):
        if e.is_dir == 1:
            sub = list_dir(access_token, e.path)
            print(f"  {e.name}: {len(sub)} items")
    dup = [e.name for e in list_dir(access_token, PROD_ROOT) if SUFFIX_DIR_PATTERN.match(e.name) or IMPORTS_SUFFIX_PATTERN.match(e.name)]
    print(f"\nSuffix duplicate dirs under prod: {len(dup)}")
    dev_dup = [e.name for e in list_dir(access_token, DEV_ROOT) if SUFFIX_DIR_PATTERN.match(e.name) or IMPORTS_SUFFIX_PATTERN.match(e.name)]
    print(f"Suffix duplicate dirs under dev: {len(dev_dup)}")
    top_dup = [e.name for e in list_dir(access_token, "/apps") if TOP_DUP_APP_PATTERN.match(e.name)]
    print(f"Top-level duplicate app dirs: {len(top_dup)}")


def main() -> None:
    mode = sys.argv[1] if len(sys.argv) > 1 else "scan"
    access_token, refresh_token = mysql_tokens()
    try:
        list_dir(access_token, PROD_ROOT)
    except Exception:
        access_token = refresh_access_token(refresh_token)
        print("Refreshed access token")

    if mode == "scan":
        scan_summary(access_token)
        return

    actions = plan_reorganize(access_token)

    if mode == "plan":
        print(json.dumps(actions, ensure_ascii=False, indent=2))
        print(f"\nTotal actions: {len(actions)}")
        return

    if mode == "execute":
        # moves first, then deletes
        for pass_name, predicate in [
            ("ensure/move", lambda a: a["action"] in {"ensure", "move"}),
            ("delete", lambda a: a["action"] == "delete"),
        ]:
            batch = [a for a in actions if predicate(a)]
            print(f"\n--- {pass_name} ({len(batch)}) ---")
            execute_actions(access_token, batch, dry_run=False)
        print("\nDone.")
        scan_summary(access_token)
        return

    print("Usage: baidu_pan_reorganize.py [scan|plan|execute]")


if __name__ == "__main__":
    main()
