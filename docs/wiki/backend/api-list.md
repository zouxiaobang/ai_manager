# API 接口清单

## 概述

所有 API 接口遵循 RESTful 风格，统一使用 `ApiResult<T>` 响应格式。
基础路径：`/api`

## 统一响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": { ... },
  "timestamp": 1710000000000
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | 状态码，0=成功 |
| message | string | 消息 |
| data | object/array/null | 响应数据 |
| timestamp | long | 响应时间戳（毫秒） |

## 健康检查

### GET /api/health

健康检查接口，返回应用及依赖服务状态。

**响应数据**：
```json
{
  "status": "UP",
  "redis": "UP",
  "mysql": "UP",
  "deployTime": "...",
  "startupTime": "...",
  "timezone": "Asia/Shanghai"
}
```

## 用户管理

### GET /api/system/users

获取用户列表。

**查询参数**：
- `page` - 页码（默认1）
- `pageSize` - 每页大小（默认20）
- `keyword` - 搜索关键词

---

## 番茄钟

### 计划管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/pomodoro/plans` | 计划列表 |
| GET | `/api/pomodoro/plans/{id}` | 计划详情 |
| GET | `/api/pomodoro/plans/default` | 默认计划 |
| POST | `/api/pomodoro/plans` | 创建计划 |
| PUT | `/api/pomodoro/plans/{id}` | 更新计划 |
| DELETE | `/api/pomodoro/plans/{id}` | 删除计划 |

### 实时会话

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/pomodoro/session` | 获取当前会话 |
| PUT | `/api/pomodoro/session` | 同步会话状态 |

**GET 响应数据**：
```json
{
  "phase": "FOCUS_RUNNING",
  "runState": "RUNNING",
  "remainingSeconds": 1500,
  "controller": "ADMIN",
  "source": "ADMIN",
  "planId": 1,
  "sessionWorkRounds": 2
}
```

**PUT 请求体**：
```json
{
  "phase": "FOCUS_RUNNING",
  "runState": "RUNNING",
  "remainingSeconds": 1500,
  "source": "ADMIN",
  "takeControl": true
}
```

### 完成记录

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/pomodoro/records` | 写入完成记录 |
| GET | `/api/pomodoro/records` | 记录列表 |
| GET | `/api/pomodoro/stats/daily` | 按日统计 |
| GET | `/api/pomodoro/stats/summary` | 区间汇总 |
| GET | `/api/pomodoro/stats/today` | 今日进度 |

---

## 笔记本

### 笔记本管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/notebooks` | 笔记本列表（树形） |
| GET | `/api/notebooks/{id}` | 笔记本详情 |
| POST | `/api/notebooks` | 创建笔记本 |
| PUT | `/api/notebooks/{id}` | 更新笔记本 |
| DELETE | `/api/notebooks/{id}` | 删除笔记本 |

### 笔记管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/notes/search` | 搜索笔记 |
| GET | `/api/notes/recent` | 最近笔记 |
| POST | `/api/notes/meta` | 批量获取元信息 |
| GET | `/api/notes/trash` | 回收站列表 |
| GET | `/api/notes/{id}` | 笔记详情 |
| POST | `/api/notes` | 创建笔记 |
| PUT | `/api/notes/{id}` | 更新笔记 |
| DELETE | `/api/notes/{id}` | 逻辑删除（入回收站） |
| POST | `/api/notes/{id}/restore` | 从回收站恢复 |
| DELETE | `/api/notes/{id}/purge` | 永久删除 |
| DELETE | `/api/notes/trash` | 清空回收站 |

### 标签管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/note-tags` | 标签列表 |
| POST | `/api/note-tags` | 创建标签 |
| PUT | `/api/note-tags/{id}` | 更新标签 |
| DELETE | `/api/note-tags/{id}` | 删除标签 |

### 待办事项

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/todos` | 待办列表 |
| GET | `/api/todos/today` | 今日待办 |
| GET | `/api/todos/{id}` | 待办详情 |
| POST | `/api/todos` | 创建待办 |
| PUT | `/api/todos/{id}` | 更新待办 |
| DELETE | `/api/todos/{id}` | 删除待办 |
| POST | `/api/todos/{id}/toggle` | 切换完成状态 |

### 笔记图片上传

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/notes/images/upload` | 上传笔记图片 |

---

## 电商管理

### 平台管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ecommerce/platforms` | 平台列表 |
| GET | `/api/ecommerce/platforms/{id}` | 平台详情 |
| POST | `/api/ecommerce/platforms` | 创建平台 |
| PUT | `/api/ecommerce/platforms/{id}` | 更新平台 |
| DELETE | `/api/ecommerce/platforms/{id}` | 删除平台 |

### 店铺管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ecommerce/shops` | 店铺列表 |
| GET | `/api/ecommerce/shops/{id}` | 店铺详情 |
| POST | `/api/ecommerce/shops` | 创建店铺 |
| PUT | `/api/ecommerce/shops/{id}` | 更新店铺 |
| DELETE | `/api/ecommerce/shops/{id}` | 删除店铺 |

### 工厂管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ecommerce/factories` | 工厂列表 |
| GET | `/api/ecommerce/factories/{id}` | 工厂详情 |
| POST | `/api/ecommerce/factories` | 创建工厂 |
| PUT | `/api/ecommerce/factories/{id}` | 更新工厂 |
| DELETE | `/api/ecommerce/factories/{id}` | 删除工厂 |

### 商品管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ecommerce/products` | 产品列表 |
| GET | `/api/ecommerce/products/{id}` | 产品详情 |
| POST | `/api/ecommerce/products` | 创建产品 |
| PUT | `/api/ecommerce/products/{id}` | 更新产品 |
| DELETE | `/api/ecommerce/products/{id}` | 删除产品 |

### 纸箱管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ecommerce/cartons` | 纸箱列表 |
| GET | `/api/ecommerce/cartons/{id}` | 纸箱详情 |
| POST | `/api/ecommerce/cartons` | 创建纸箱 |
| PUT | `/api/ecommerce/cartons/{id}` | 更新纸箱 |
| DELETE | `/api/ecommerce/cartons/{id}` | 删除纸箱 |
| GET | `/api/ecommerce/cartons/match` | 匹配合适纸箱 |

### 销售订单

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ecommerce/sales-orders` | 订单列表 |
| GET | `/api/ecommerce/sales-orders/monthly-overview` | 月度概览 |
| GET | `/api/ecommerce/sales-orders/{id}` | 订单详情 |
| POST | `/api/ecommerce/sales-orders` | 创建订单 |
| PUT | `/api/ecommerce/sales-orders/{id}` | 更新订单 |
| DELETE | `/api/ecommerce/sales-orders/{id}` | 删除订单 |
| POST | `/api/ecommerce/sales-orders/{id}/confirm` | 确认订单 |
| POST | `/api/ecommerce/sales-orders/{id}/ship` | 整单发货 |
| POST | `/api/ecommerce/sales-orders/{id}/lines/{lineId}/ship` | 单行发货 |
| POST | `/api/ecommerce/sales-orders/{id}/lines/{lineId}/refund` | 单行退款 |
| POST | `/api/ecommerce/sales-orders/{id}/lines/{lineId}/cancel` | 单行取消 |

### 订单导入

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/ecommerce/sales-orders/import/upload` | 上传导入文件 |
| GET | `/api/ecommerce/sales-orders/import/{batchId}` | 获取导入预览 |
| POST | `/api/ecommerce/sales-orders/import/preview` | 预览导入 |
| POST | `/api/ecommerce/sales-orders/import/{batchId}/commit` | 提交导入 |
| POST | `/api/ecommerce/sales-orders/import/{batchId}/reparse` | 重新解析 |
| POST | `/api/ecommerce/sales-orders/import/{batchId}/replace-file` | 替换文件 |
| POST | `/api/ecommerce/sales-orders/import/{batchId}/manual-costs` | 更新人工成本 |

### 采购入库

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ecommerce/inbound-orders` | 入库单列表 |
| GET | `/api/ecommerce/inbound-orders/{id}` | 入库单详情 |
| POST | `/api/ecommerce/inbound-orders` | 创建入库单 |
| PUT | `/api/ecommerce/inbound-orders/{id}` | 更新入库单 |
| DELETE | `/api/ecommerce/inbound-orders/{id}` | 删除入库单 |

### 出库发货

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ecommerce/outbound-orders` | 出库单列表 |
| GET | `/api/ecommerce/outbound-orders/{id}` | 出库单详情 |
| POST | `/api/ecommerce/outbound-orders` | 创建出库单 |
| PUT | `/api/ecommerce/outbound-orders/{id}` | 更新出库单 |
| DELETE | `/api/ecommerce/outbound-orders/{id}` | 删除出库单 |

### 库存管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ecommerce/inventory` | 库存列表 |
| GET | `/api/ecommerce/inventory/{skuId}` | SKU库存详情 |
| GET | `/api/ecommerce/inventory/stats` | 库存统计 |
| GET | `/api/ecommerce/inventory/logs` | 库存变动日志 |
| GET | `/api/ecommerce/inventory/health` | 库存健康度 |

### 库存盘点

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ecommerce/stocktake-orders` | 盘点单列表 |
| GET | `/api/ecommerce/stocktake-orders/{id}` | 盘点单详情 |
| POST | `/api/ecommerce/stocktake-orders` | 创建盘点单 |
| PUT | `/api/ecommerce/stocktake-orders/{id}` | 更新盘点单 |
| POST | `/api/ecommerce/stocktake-orders/{id}/commit` | 提交盘点 |

### 快递站点

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ecommerce/express-stations` | 快递站点列表 |
| GET | `/api/ecommerce/express-stations/{id}` | 站点详情 |
| POST | `/api/ecommerce/express-stations` | 创建站点 |
| PUT | `/api/ecommerce/express-stations/{id}` | 更新站点 |
| DELETE | `/api/ecommerce/express-stations/{id}` | 删除站点 |

### 快递价格

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ecommerce/express-prices` | 快递价格列表 |
| GET | `/api/ecommerce/express-prices/calculate` | 计算运费 |
| POST | `/api/ecommerce/express-prices` | 新增价格 |
| PUT | `/api/ecommerce/express-prices/{id}` | 更新价格 |
| DELETE | `/api/ecommerce/express-prices/{id}` | 删除价格 |

### Listing链接

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ecommerce/listing-links` | Listing链接列表 |
| GET | `/api/ecommerce/listing-links/{id}` | 详情 |
| POST | `/api/ecommerce/listing-links` | 创建 |
| PUT | `/api/ecommerce/listing-links/{id}` | 更新 |
| DELETE | `/api/ecommerce/listing-links/{id}` | 删除 |
| POST | `/api/ecommerce/listing-links/{id}/recalc-pricing` | 重新计算定价 |

### 月度结算

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ecommerce/monthly-settlements` | 月度结算列表 |
| GET | `/api/ecommerce/monthly-settlements/{id}` | 结算详情 |
| POST | `/api/ecommerce/monthly-settlements/create-snapshot` | 创建结算快照 |
| POST | `/api/ecommerce/monthly-settlements/{id}/finalize` | 确认结算 |

### 电商图片上传

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/ecommerce/images/upload` | 上传电商图片 |

### 系统设置

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ecommerce/settings` | 获取设置 |
| PUT | `/api/ecommerce/settings` | 更新设置 |
| GET | `/api/ecommerce/settings/purchase-order-config` | 采购配置 |
| PUT | `/api/ecommerce/settings/purchase-order-config` | 更新采购配置 |

---
## 24小时重启系统

### 每日检查清单

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/24hour` | 获取指定日期检查清单（参数：date） |
| POST | `/api/24hour` | 保存指定日期检查清单（upsert） |

---

## 部署中心

### 部署运行

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/deploy/run/start` | 开始部署 |
| POST | `/api/deploy/run/stop` | 停止部署 |
| GET | `/api/deploy/run/status` | 部署状态 |

### 部署日志

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/deploy/logs` | 获取部署日志 |
| GET | `/api/deploy/logs/stream` | 日志流（SSE） |
| GET | `/api/deploy/logs/stats` | 日志统计 |
| POST | `/api/deploy/logs/analyze` | AI日志分析 |

### 数据库终端

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/deploy/database/execute` | 执行SQL |
| GET | `/api/deploy/database/tables` | 表列表 |
| GET | `/api/deploy/database/table/{name}` | 表结构 |

### 部署检查清单

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/deploy/checklist` | 检查清单 |
| POST | `/api/deploy/checklist/run` | 运行检查 |
| POST | `/api/deploy/checklist/verify` | 验证检查项 |

### 部署版本

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/deploy/version` | 当前版本信息 |
| GET | `/api/deploy/version/history` | 部署历史 |

---

## 存储中心

### 图片空间

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/image-space` | 图片列表 |
| GET | `/api/image-space/stats` | 空间统计 |
| GET | `/api/image-space/orphans` | 孤儿文件列表 |
| POST | `/api/image-space/orphans/clean` | 清理孤儿文件 |
| POST | `/api/image-space/rename` | 重命名图片 |

### 存储中心

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/storage-center/overview` | 存储概览 |
| GET | `/api/storage-center/zones` | 存储分区 |
| PUT | `/api/storage-center/zones/{id}/quota` | 设置配额 |

### 百度网盘

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/baidu-pan/auth/status` | 授权状态 |
| GET | `/api/baidu-pan/auth/url` | 获取授权URL |
| POST | `/api/baidu-pan/sync/notes` | 同步笔记 |
| GET | `/api/baidu-pan/files` | 文件列表 |
| POST | `/api/baidu-pan/upload` | 上传文件 |
| GET | `/api/baidu-pan/download` | 下载文件 |

---

## 通用导入

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/sys-import/profiles` | 导入配置列表 |
| GET | `/api/sys-import/profiles/{id}` | 导入配置详情 |
| POST | `/api/sys-import/profiles` | 创建导入配置 |
| PUT | `/api/sys-import/profiles/{id}` | 更新导入配置 |
| DELETE | `/api/sys-import/profiles/{id}` | 删除导入配置 |
| GET | `/api/sys-import/batches` | 导入批次列表 |
| GET | `/api/sys-import/batches/{id}` | 导入批次详情 |

---

## 分页查询参数

所有列表接口统一的分页参数：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| page | number | 1 | 页码 |
| pageSize | number | 20 | 每页大小（最大100） |
| keyword | string | - | 搜索关键词 |

**统一分页响应**：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [...],
    "total": 100,
    "page": 1,
    "pageSize": 20,
    "extra": { ... }
  },
  "timestamp": ...
}
```
