#!/usr/bin/env python3
"""
将电商图片重命名为：SPU名称-SKU名称.后缀名（SKU 主图）；SPU 独有主图为 SPU名称-主图.后缀名。

用法:
  py -3 rename_ecommerce_image_names.py --dry-run
  py -3 rename_ecommerce_image_names.py
"""
from __future__ import annotations

import argparse
import re
import shutil
import subprocess
import sys
from datetime import datetime
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ADMIN_BACKEND = SCRIPT_DIR.parent
LOCAL_UPLOAD_DIR = REPO_ADMIN_BACKEND / "uploads" / "ecommerce"
IMAGE_EXT = {".jpg", ".jpeg", ".png", ".webp", ".gif"}
INVALID_CHARS = re.compile(r'[\\/:*?"<>|]')


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


def mysql_exec(sql: str) -> None:
    r = subprocess.run(
        [
            "mysql",
            "-u",
            "root",
            "-p123456",
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


def sql_escape(value: str) -> str:
    return value.replace("\\", "\\\\").replace("'", "''")


def extract_file_name(raw: str) -> str:
    name = raw.strip().replace("\\", "/")
    if "/" in name:
        name = name.rsplit("/", 1)[-1]
    return name


def extract_extension(file_name: str) -> str:
    dot = file_name.rfind(".")
    if dot <= 0:
        return ".jpg"
    ext = file_name[dot:].lower()
    return ext if ext in IMAGE_EXT else ".jpg"


def sanitize_segment(value: str) -> str:
    if not value or not value.strip():
        return "未命名"
    sanitized = value.strip().replace("\\", "-").replace("/", "-")
    sanitized = INVALID_CHARS.sub("", sanitized)
    sanitized = re.sub(r"\s+", " ", sanitized).strip()
    while "--" in sanitized:
        sanitized = sanitized.replace("--", "-")
    sanitized = sanitized.strip("-")
    return sanitized or "未命名"


def resolve_sku_display_name(spec_name: str, sku_code: str, sku_id: str) -> str:
    if spec_name and spec_name.strip():
        return spec_name.strip()
    if sku_code and sku_code.strip():
        return sku_code.strip()
    return f"SKU{sku_id}" if sku_id else "SKU"


def build_sku_image_name(spu_name: str, sku_name: str, old_name: str) -> str:
    return f"{sanitize_segment(spu_name)}-{sanitize_segment(sku_name)}{extract_extension(old_name)}"


def build_spu_main_image_name(spu_name: str, old_name: str) -> str:
    return f"{sanitize_segment(spu_name)}-主图{extract_extension(old_name)}"


def build_platform_avatar_name(platform_name: str, old_name: str) -> str:
    return f"{sanitize_segment(platform_name)}-头像{extract_extension(old_name)}"


def build_shop_avatar_name(platform_name: str, shop_name: str, old_name: str) -> str:
    return (
        f"{sanitize_segment(platform_name)}-{sanitize_segment(shop_name)}-头像"
        f"{extract_extension(old_name)}"
    )


def build_express_avatar_name(station_name: str, old_name: str) -> str:
    return f"{sanitize_segment(station_name)}-头像{extract_extension(old_name)}"


def build_carton_preview_name(carton_name: str, old_name: str) -> str:
    return f"{sanitize_segment(carton_name)}-预览{extract_extension(old_name)}"


def build_orphan_name(date_key: str, sequence: int, old_name: str) -> str:
    return f"未分类-{sanitize_segment(date_key)}-{sequence:03d}{extract_extension(old_name)}"


def allocate_unique(base_name: str, reserved: set[str]) -> str:
    key = base_name.lower()
    if key not in reserved:
        reserved.add(key)
        return base_name
    ext = extract_extension(base_name)
    stem = base_name[: -len(ext)]
    index = 2
    while True:
        candidate = f"{stem}-{index}{ext}"
        ckey = candidate.lower()
        if ckey not in reserved:
            reserved.add(ckey)
            return candidate
        index += 1


def collect_rename_plans() -> tuple[dict[str, str], dict[str, str]]:
    product_names: dict[str, str] = {}
    for row in mysql_query("SELECT id, name FROM ec_product WHERE deleted=0;"):
        product_names[row[0]] = row[1]

    plans: dict[str, str] = {}
    sources: dict[str, str] = {}

    sku_rows = mysql_query(
        "SELECT s.id, s.product_id, s.sku_code, s.spec_name, s.image_name "
        "FROM ec_sku s WHERE s.deleted=0 AND s.image_name IS NOT NULL AND TRIM(s.image_name)<>'';"
    )
    for sku_id, product_id, sku_code, spec_name, image_name in sku_rows:
        old_name = extract_file_name(image_name)
        if not any(old_name.lower().endswith(ext) for ext in IMAGE_EXT):
            continue
        spu_name = product_names.get(product_id, f"SPU#{product_id}")
        sku_display = resolve_sku_display_name(spec_name, sku_code, sku_id)
        new_name = build_sku_image_name(spu_name, sku_display, old_name)
        if old_name not in plans:
            plans[old_name] = new_name
            sources[old_name] = "SKU"

    sku_image_names = set(plans.keys())
    product_rows = mysql_query(
        "SELECT id, name, image_name FROM ec_product "
        "WHERE deleted=0 AND image_name IS NOT NULL AND TRIM(image_name)<>'';"
    )
    for _pid, spu_name, image_name in product_rows:
        old_name = extract_file_name(image_name)
        if not any(old_name.lower().endswith(ext) for ext in IMAGE_EXT):
            continue
        if old_name in sku_image_names:
            continue
        new_name = build_spu_main_image_name(spu_name, old_name)
        if old_name not in plans:
            plans[old_name] = new_name
            sources[old_name] = "SPU_MAIN"

    platform_names: dict[str, str] = {}
    for row in mysql_query(
        "SELECT id, name, avatar_url FROM ec_platform WHERE deleted=0;"
    ):
        platform_names[row[0]] = row[1]
        if not row[2] or not row[2].strip():
            continue
        old_name = extract_file_name(row[2])
        if not any(old_name.lower().endswith(ext) for ext in IMAGE_EXT):
            continue
        new_name = build_platform_avatar_name(row[1], old_name)
        if old_name not in plans:
            plans[old_name] = new_name
            sources[old_name] = "PLATFORM"

    for row in mysql_query(
        "SELECT platform_id, name, avatar_url FROM ec_shop "
        "WHERE deleted=0 AND avatar_url IS NOT NULL AND TRIM(avatar_url)<>'';"
    ):
        old_name = extract_file_name(row[2])
        if not any(old_name.lower().endswith(ext) for ext in IMAGE_EXT):
            continue
        platform_name = platform_names.get(row[0], f"平台#{row[0]}")
        new_name = build_shop_avatar_name(platform_name, row[1], old_name)
        if old_name not in plans:
            plans[old_name] = new_name
            sources[old_name] = "SHOP"

    for row in mysql_query(
        "SELECT name, avatar_url FROM ec_express_station "
        "WHERE deleted=0 AND avatar_url IS NOT NULL AND TRIM(avatar_url)<>'';"
    ):
        old_name = extract_file_name(row[1])
        if not any(old_name.lower().endswith(ext) for ext in IMAGE_EXT):
            continue
        new_name = build_express_avatar_name(row[0], old_name)
        if old_name not in plans:
            plans[old_name] = new_name
            sources[old_name] = "EXPRESS"

    for row in mysql_query(
        "SELECT name, preview_image FROM ec_carton "
        "WHERE deleted=0 AND preview_image IS NOT NULL AND TRIM(preview_image)<>'';"
    ):
        old_name = extract_file_name(row[1])
        if not any(old_name.lower().endswith(ext) for ext in IMAGE_EXT):
            continue
        new_name = build_carton_preview_name(row[0], old_name)
        if old_name not in plans:
            plans[old_name] = new_name
            sources[old_name] = "CARTON"

    referenced: set[str] = set()
    ref_queries = [
        "SELECT image_name FROM ec_product WHERE image_name IS NOT NULL AND TRIM(image_name)<>''",
        "SELECT image_name FROM ec_sku WHERE image_name IS NOT NULL AND TRIM(image_name)<>''",
        "SELECT preview_image FROM ec_carton WHERE preview_image IS NOT NULL AND TRIM(preview_image)<>''",
        "SELECT avatar_url FROM ec_platform WHERE avatar_url IS NOT NULL AND TRIM(avatar_url)<>''",
        "SELECT avatar_url FROM ec_shop WHERE avatar_url IS NOT NULL AND TRIM(avatar_url)<>''",
        "SELECT avatar_url FROM ec_express_station WHERE avatar_url IS NOT NULL AND TRIM(avatar_url)<>''",
    ]
    for sql in ref_queries:
        for row in mysql_query(sql):
            referenced.add(extract_file_name(row[0]))

    orphan_day_counters: dict[str, int] = {}
    for path in sorted(LOCAL_UPLOAD_DIR.iterdir(), key=lambda p: p.name.lower()):
        if not path.is_file() or not any(path.name.lower().endswith(ext) for ext in IMAGE_EXT):
            continue
        if path.name in plans or path.name in referenced:
            continue
        date_key = datetime.fromtimestamp(path.stat().st_mtime).strftime("%Y%m%d")
        orphan_day_counters[date_key] = orphan_day_counters.get(date_key, 0) + 1
        new_name = build_orphan_name(date_key, orphan_day_counters[date_key], path.name)
        plans[path.name] = new_name
        sources[path.name] = "ORPHAN"

    return plans, sources


def update_references(old_name: str, new_name: str) -> None:
    old_sql = sql_escape(old_name)
    new_sql = sql_escape(new_name)
    statements = [
        f"UPDATE ec_product SET image_name='{new_sql}' WHERE image_name='{old_sql}';",
        f"UPDATE ec_sku SET image_name='{new_sql}' WHERE image_name='{old_sql}';",
        f"UPDATE ec_carton SET preview_image='{new_sql}' WHERE preview_image='{old_sql}';",
        f"UPDATE ec_platform SET avatar_url='{new_sql}' WHERE avatar_url='{old_sql}';",
        f"UPDATE ec_shop SET avatar_url='{new_sql}' WHERE avatar_url='{old_sql}';",
        f"UPDATE ec_express_station SET avatar_url='{new_sql}' WHERE avatar_url='{old_sql}';",
    ]
    for platform in mysql_query(
        "SELECT id, avatar_url FROM ec_platform WHERE avatar_url IS NOT NULL AND avatar_url<>'';"
    ):
        if extract_file_name(platform[1]) == old_name:
            statements.append(
                f"UPDATE ec_platform SET avatar_url='{new_sql}' WHERE id={platform[0]};"
            )
    for shop in mysql_query(
        "SELECT id, avatar_url FROM ec_shop WHERE avatar_url IS NOT NULL AND avatar_url<>'';"
    ):
        if extract_file_name(shop[1]) == old_name:
            statements.append(f"UPDATE ec_shop SET avatar_url='{new_sql}' WHERE id={shop[0]};")
    for station in mysql_query(
        "SELECT id, avatar_url FROM ec_express_station WHERE avatar_url IS NOT NULL AND avatar_url<>'';"
    ):
        if extract_file_name(station[1]) == old_name:
            statements.append(
                f"UPDATE ec_express_station SET avatar_url='{new_sql}' WHERE id={station[0]};"
            )
    mysql_exec(" ".join(statements))


def main() -> int:
    parser = argparse.ArgumentParser(description="清洗电商图片命名")
    parser.add_argument("--dry-run", action="store_true", help="仅预览不重命名")
    args = parser.parse_args()

    LOCAL_UPLOAD_DIR.mkdir(parents=True, exist_ok=True)
    plans, sources = collect_rename_plans()
    reserved = {p.name.lower() for p in LOCAL_UPLOAD_DIR.iterdir() if p.is_file()}

    planned = renamed = skipped = failed = 0
    print(f"待处理映射: {len(plans)}")
    print(f"本地目录: {LOCAL_UPLOAD_DIR}")
    print(f"模式: {'预览' if args.dry_run else '执行'}")

    for old_name, target_name in plans.items():
        source = sources.get(old_name, "SKU")
        if old_name.lower() == target_name.lower():
            skipped += 1
            print(f"[SKIP] {old_name} ({source}) 已符合规则")
            continue

        new_name = allocate_unique(target_name, reserved)
        planned += 1
        source_path = LOCAL_UPLOAD_DIR / old_name
        exists = source_path.is_file()

        if args.dry_run:
            flag = "FILE" if exists else "DB_ONLY"
            print(f"[PLAN] {old_name} -> {new_name} ({source}, {flag})")
            continue

        try:
            if exists:
                target_path = LOCAL_UPLOAD_DIR / new_name
                if target_path.exists() and target_path.resolve() != source_path.resolve():
                    raise RuntimeError(f"目标文件已存在: {new_name}")
                shutil.move(str(source_path), str(target_path))
            update_references(old_name, new_name)
            renamed += 1
            print(f"[OK] {old_name} -> {new_name} ({source})")
        except Exception as ex:
            failed += 1
            print(f"[FAIL] {old_name} -> {new_name}: {ex}")

    print("\n=== 完成 ===")
    print(f"计划: {planned}, 成功: {renamed}, 跳过: {skipped}, 失败: {failed}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
