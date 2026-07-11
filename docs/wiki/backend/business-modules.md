# 业务模块详解

## 电商模块（E-commerce）

电商模块是系统中最复杂的业务模块，涵盖完整的电商运营流程。

### 模块结构

```
电商模块
├── 基础数据
│   ├── 平台管理 (EcPlatform)
│   ├── 店铺管理 (EcShop)
│   ├── 工厂管理 (EcFactory)
│   └── 快递站点 (EcExpressStation)
├── 商品中心
│   ├── 产品管理 (EcProduct)
│   ├── SKU管理 (EcSku)
│   └── 纸箱管理 (EcCarton)
├── 订单管理
│   ├── 销售订单 (EcSalesOrder)
│   └── 订单行 (EcSalesOrderLine)
├── 仓储管理
│   ├── 采购入库 (EcInboundOrder)
│   ├── 出库发货 (EcOutboundOrder)
│   ├── 库存管理 (EcInventory)
│   └── 库存盘点 (EcStocktakeOrder)
├── 链接管理
│   └── Listing链接 (EcListingLink)
└── 财务管理
    ├── 月度结算 (EcMonthlySettlement)
    ├── 快递价格 (EcExpressPrice)
    └── 快递通知 (EcExpressNotice)
```

### 核心实体关系

```
EcPlatform (平台)
    └── EcShop (店铺)
          └── EcSalesOrder (销售订单)
                └── EcSalesOrderLine (订单行)
                      ├── EcProduct (产品)
                      │     └── EcSku (SKU)
                      │           └── EcCarton (纸箱)
                      └── EcInventory (库存扣减)

EcFactory (工厂)
    └── EcProduct (产品)

EcInboundOrder (入库单)
    └── EcInboundOrderLine (入库单行)
          └── EcSku → EcInventory (库存增加)

EcListingLink (Listing链接)
    ├── EcListingLinkProduct (关联产品)
    └── EcListingLinkSku (关联SKU)
```

### 关键业务流程

#### 销售订单流程

```
创建订单 → 确认订单 → 库存检查 → 出库发货 → 签收完成
     │          │          │          │
     │          │          │          └─ 更新库存、记录库存日志
     │          │          └─ 检查SKU库存是否充足
     │          └─ 锁定订单，进入待发货状态
     └─ 初始状态：待确认
```

#### 库存变动来源

| 类型 | 来源 | 库存变化 |
|------|------|---------|
| 入库 | 采购入库单 | +增加 |
| 出库 | 销售订单发货 | -减少 |
| 盘点 | 盘点单调整 | 按实盘调整 |
| 导入 | 订单导入库存扣减 | -减少（批量） |

---

## 笔记本模块（Notebook）

笔记本模块提供完整的笔记管理功能，支持本地和百度网盘双写。

### 模块结构

```
笔记本模块
├── 笔记本管理 (NbNotebook)
│   └── 树形结构
├── 笔记管理 (NbNote)
│   ├── 元数据（数据库）
│   └── 内容（存储层）
├── 标签管理 (NbNoteTag)
│   └── 笔记-标签关联 (NbNoteTagRel)
├── 待办事项 (NbTodoItem)
│   ├── 重复规则
│   ├── 提醒设置
│   └── 置顶功能
└── 存储层
    ├── 本地文件存储
    └── 百度网盘存储
```

### 笔记内容存储设计

**设计思想**：笔记正文不存入数据库，独立存储在文件系统中。

**数据库存储的元数据**：

| 字段 | 说明 |
|------|------|
| `storageType` | 存储类型：LOCAL / BAIDU_PAN |
| `storagePath` | 存储路径 |
| `contentHash` | 内容哈希（SHA-256） |
| `contentSize` | 内容大小（字节） |
| `contentVersion` | 内容版本号 |
| `syncStatus` | 同步状态 |

**优势**：
- 数据库压力小
- 支持多存储后端切换
- 内容哈希去重
- 便于备份和迁移

### 待办事项功能

**NbTodoItem 核心字段**：

| 字段 | 说明 |
|------|------|
| `title` | 标题 |
| `content` | 内容 |
| `status` | 状态：TODO / DONE |
| `pinned` | 是否置顶 |
| `dueDate` | 截止日期 |
| `remindAt` | 提醒时间 |
| `repeatType` | 重复类型：NONE / DAILY / WEEKLY / MONTHLY / CUSTOM |
| `repeatDays` | 重复日（周重复时使用） |
| `notebookId` | 所属笔记本 |
| `parentTodoId` | 父待办（支持子任务） |

---

## 番茄钟模块（Pomodoro）

番茄钟模块提供专注计时功能，支持Web端与ESP32副屏双向同步。

### 模块结构

```
番茄钟模块
├── 计划管理 (PomodoroPlan)
│   ├── 专注时长
│   ├── 休息时长
│   └── 长休息设置
├── 实时会话 (Redis)
│   ├── 当前状态
│   ├── 剩余时间
│   ├── 控制方
│   └── 多端同步
└── 完成记录 (PomodoroRecord)
    ├── 专注记录
    ├── 休息记录
    └── 统计报表
```

### 会话状态机

```
IDLE (空闲)
  │
  ├─ 开始专注 → FOCUS_RUNNING (专注中)
  │                    │
  │                    ├─ 暂停 → FOCUS_PAUSED
  │                    │     └─ 继续 → FOCUS_RUNNING
  │                    │
  │                    └─ 时间到 → 记录完成 → BREAK_RUNNING (休息中)
  │                                          │
  │                                          ├─ 暂停 → BREAK_PAUSED
  │                                          │     └─ 继续 → BREAK_RUNNING
  │                                          │
  │                                          └─ 时间到 → 记录完成 → IDLE
  │
  └─ 重置 → IDLE
```

### 多端同步机制

**控制方（Controller）机制**：
- 谁点击开始/暂停/重置，谁成为控制方
- 另一方只能跟随显示，不能操作
- 空闲状态下任何一方都可发起控制

**同步方式**：
- Web端：每2秒轮询 GET /api/pomodoro/session
- ESP32端：每1-2秒轮询 GET /api/pomodoro/session
- 操作时：立即 PUT /api/pomodoro/session 上报状态

**Redis数据结构**：
```
pomodoro:session:active (Hash)
  ├── phase: IDLE/FOCUS_RUNNING/FOCUS_PAUSED/BREAK_RUNNING/BREAK_PAUSED
  ├── remainingSeconds: 剩余秒数
  ├── lastUpdateTime: 最后更新时间戳
  ├── controller: ADMIN/DEVICE (控制方)
  ├── source: 最后更新来源
  └── planId: 当前计划ID
```

---

## 部署中心模块（Deploy）

部署中心提供DevOps自助部署能力，基于SSH远程操作。

### 模块结构

```
部署中心模块
├── 部署运行 (DeployRunnerService)
│   ├── 本地部署模式
│   ├── 远程部署模式（SSH）
│   ├── Git pull 拉取
│   ├── 构建项目
│   └── 重启服务
├── 部署历史 (DeployHistoryService)
│   ├── 部署记录
│   └── 状态跟踪
├── 部署清单 (DeployChecklistService)
│   └── 检查项管理
├── 数据库终端 (DeploySqlTerminalService)
│   └── 远程SQL执行
└── 部署日志 (DeployLogService)
    ├── 实时日志流
    ├── 日志统计分析
    └── AI智能分析
```

### 部署流程

```
触发部署
    │
    ▼
1. SSH连接远程服务器
    │
    ▼
2. Git pull 拉取最新代码
    │
    ▼
3. 执行构建命令
    ├─ 后端：mvn clean package
    └─ 前端：npm run build
    │
    ▼
4. 重启应用服务
    ├─ 后端：systemctl restart 服务
    └─ 前端：替换静态资源
    │
    ▼
5. 健康检查确认成功
    │
    ▼
完成部署
```

---

## 存储中心模块（Storage）

存储中心统一管理各业务的文件存储。

### 模块结构

```
存储中心模块
├── 存储空间概览
│   ├── 电商图片
│   ├── 笔记本图片
│   └── 笔记内容
├── 图片空间 (ImageSpaceService)
│   ├── 图片分类管理
│   ├── 图片重命名
│   └── 孤儿文件检测
├── 配额管理
│   └── 空间限额配置
└── 百度网盘集成
    ├── OAuth授权
    ├── 文件上传下载
    └── 内容同步
```

### 孤儿文件检测

**定义**：存在于文件系统但数据库中无引用记录的文件。

**检测逻辑**：
1. 扫描文件系统中的所有文件
2. 查询数据库中所有引用的文件路径
3. 对比找出未被引用的文件
4. 提供清理功能（需确认）

---

## 系统模块（Sys）

### 用户管理（SysUser）

当前为简化版本，单用户模式。

**核心字段**：
- `username` - 用户名
- `password` - 密码（加密存储）
- `nickname` - 昵称
- `avatar` - 头像

### 通用导入框架（SysImport）

可配置化的Excel导入框架，电商订单等模块复用。

**核心概念**：

| 概念 | 说明 |
|------|------|
| `SysImportProfile` | 导入配置（字段映射、状态映射等） |
| `SysImportBatch` | 导入批次（一次上传为一个批次） |
| `EcOrderImportRow` | 导入行数据（具体业务的行数据） |

**导入流程**：
```
上传文件 → 解析预览 → 字段映射 → 确认提交 → 异步处理 → 查看结果
```
