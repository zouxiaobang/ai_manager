# kyle-e-commerce → ai_manager_admin 数据迁移 SQL

> 对照文档：[电商数据迁移对照-kyle-e-commerce.md](../../../docs/电商数据迁移对照-kyle-e-commerce.md)

## 说明

- **源库**：`kyle-e-commerce`（只读，不修改）
- **目标库**：`ai_manager_admin`（清理后写入）
- **保留**：`ec_system_config`、`ec_purchase_order_config` 不清理
- **不迁移**：刷单 `e_self_purchase_order`、库存流水 `e_inventory_records`、新系统独有表（出货/盘点/导入中间行/快递账单等）

## 文件清单

| 文件 | 内容 |
|------|------|
| `00_cleanup.sql` | 清空目标库电商业务表 |
| `01_master_data.sql` | 平台、店铺、工厂、快递 |
| `02_products_listing.sql` | 纸箱、SPU、SKU、库存、上架链接 |
| `03_sales_orders.sql` | 销售订单头 + 明细 + 成本补录 |
| `04_inbound.sql` | 采购记录 → 入库单 |
| `05_settlement.sql` | 月结决策 + 月度快照 JSON |
| `06_reset_auto_increment.sql` | 重置自增起点 |
| `08_product_image_from_first_sku.sql` | SPU 头像取首个 SKU 的 `image_name` |
| `run_all.sql` | 按序执行并输出行数校验 |

## 本地执行

```bash
cd admin-backend/sql/migration/kyle-e-commerce
mysql -u root -p < run_all.sql
```

或在 PowerShell：

```powershell
Get-Content run_all.sql -Raw | mysql -u root -p123456 --default-character-set=utf8mb4
```

> `SOURCE` 指令需在 `mysql` 客户端内、且当前目录为本文件夹。

## 关键映射规则

- 纸箱供应商 `e_box_company` → `ec_factory`，`id + 10000`，`factory_type=CUSTOMER`
- 纸箱 `e_box_item.parent_id` → `ec_carton.factory_id = parent_id + 10000`
- 销售订单 `order_no` = `SOM` + 旧 `e_real_order.id`
- 平台订单号写入 `platform_order_no`（原 `order_no`）
- 入库单 `order_no` = `IB` + 8 位采购记录 id

## 预期行数（迁移后实测）

| 表 | 约数 | 备注 |
|----|------|------|
| ec_platform | 3 | 源库未删除记录 |
| ec_shop | 5 | |
| ec_factory | 19 | 17 生产 + 2 纸箱商（ID+10000） |
| ec_carton | 15 | |
| ec_product | 32 | |
| ec_sku | 204 | |
| ec_inventory | 204 | |
| ec_listing_link | 53 | |
| ec_listing_link_sku | 407 | |
| ec_listing_link_product | 66 | 由货号反查去重 |
| ec_sales_order | 2142 | 源 2403 条去重后（同店铺+平台单号保留最小 id） |
| ec_sales_order_line | 2142 | |
| ec_inbound_order | 335 | 每条采购记录一张入库单 |
| ec_settlement_order_decision | 138 | |
| ec_settlement_snapshot | 4 | 按月份聚合 JSON |

## 注意事项

- 跨库字符串比较使用 `utf8mb4_unicode_ci` 避免排序规则冲突
- 销售订单按 `(shop_id, platform_order_no)` 去重，旧系统存在重复平台单号
- `ec_system_config`、`ec_purchase_order_config` **不清理、不覆盖**
