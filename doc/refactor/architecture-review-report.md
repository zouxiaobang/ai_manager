# AI Manager 项目架构评审报告

> **评审日期**: 2026-07-21
> **评审范围**: 全项目代码扫描（admin-web / admin-backend / firmware / deploy）
> **评审类型**: 架构级静态分析（未修改代码）

---

## 目录

1. [项目结构图](#1-项目结构图)
2. [模块职责](#2-模块职责)
3. [数据流](#3-数据流)
4. [核心业务流程](#4-核心业务流程)
5. [技术债务列表](#5-技术债务列表)

---

## 1. 项目结构图

### 1.1 顶层目录结构

```
ai_manager/
├── admin-backend/          # Java Spring Boot 后端
│   ├── admin-common/       #   公共基础层（POJO / 异常 / 统一响应）
│   ├── admin-framework/    #   框架配置层（Redis / MyBatis / CORS / JSON）
│   ├── admin-system/       #   业务实现层（Controller / Service / Mapper）
│   ├── admin-server/       #   启动入口 + 配置文件
│   ├── sql/                #   数据库 DDL / DML 脚本（模块化组织）
│   ├── uploads/            #   上传文件存储（ecommerce / notebook-content）
│   ├── scripts/            #   数据迁移工具（Python）
│   └── docs/               #   项目内部文档
│
├── admin-web/              # Vue 3 + TypeScript 前端
│   ├── src/
│   │   ├── api/            #   API 请求封装（axios）
│   │   ├── components/     #   通用业务组件
│   │   ├── composables/    #   Vue 3 组合式函数
│   │   ├── constants/      #   业务常量
│   │   ├── data/           #   静态配置数据
│   │   ├── i18n/           #   国际化（zh-CN / en-US）
│   │   ├── layouts/        #   布局组件
│   │   ├── router/         #   路由配置（PC / mobile / mobile-v2）
│   │   ├── stores/         #   Pinia 状态管理
│   │   ├── styles/         #   全局样式
│   │   ├── utils/          #   工具函数
│   │   ├── views/          #   PC 端页面组件
│   │   ├── mobile/         #   移动端 V1 页面
│   │   ├── mobile-v2/      #   移动端 V2 页面
│   │   ├── App_*.vue       #   多入口根组件
│   │   └── main_*.ts       #   多入口启动文件
│   ├── public/             #   静态资源
│   └── vite.config.ts      #   构建配置
│
├── firmware/               # ESP32-S3 副屏固件
│   └── esp32_s3_sub_display/
│       ├── main/           #   主程序（C++ / FreeRTOS / LVGL）
│       │   ├── include/    #   头文件
│       │   ├── assets_embed/ #  嵌入式资源（图片 / 字体）
│       │   └── fonts/      #   中文字体
│       ├── managed_components/ # 托管组件（lvgl / mdns）
│       └── sdcard_assets/  #   SD 卡资源（配置 / 歌词 / 图片）
│
├── deploy/                 # 部署基础设施
│   ├── docker/             #   Docker Compose（MySQL / Redis）
│   ├── nginx/              #   Nginx 站点配置
│   ├── scripts/            #   部署脚本（PowerShell / Shell）
│   ├── systemd/            #   Systemd 服务单元
│   ├── sudoers/            #   Sudo 权限配置
│   └── env/                #   环境变量模板
│
├── docs/                   # 项目文档
│   ├── wiki/               #   开发 Wiki
│   │   ├── architecture/   #     架构设计
│   │   ├── backend/        #     后端文档
│   │   ├── frontend/       #     前端文档
│   │   ├── firmware/       #     固件文档
│   │   └── deployment/     #     部署文档
│   ├── 笔记drawio/         #     DrawIO 架构图
│   └── ... .md             #     需求 / 优化文档
│
├── .trae/                  # Trae IDE 设计文档
│   └── documents/          #   PRD / 技术设计
│
└── tmp/                    # 临时截图
```

### 1.2 后端 Maven 模块依赖

```
┌─────────────────────────────────────┐
│           admin-server              │  Spring Boot 启动入口
│   application.yml / application-*.yml
└─────────────────┬───────────────────┘
                  │ depends on
                  ▼
┌─────────────────────────────────────┐
│           admin-system              │  业务实现层
│  Controller / Service / Mapper      │  46 个 Controller, 60+ Mapper
└─────────────────┬───────────────────┘
                  │ depends on
                  ▼
┌─────────────────────────────────────┐
│          admin-framework            │  框架配置层
│  Redis / MyBatis-Plus / Jackson     │
│  CORS / 全局异常处理 / AOP           │
└─────────────────┬───────────────────┘
                  │ depends on
                  ▼
┌─────────────────────────────────────┐
│           admin-common              │  公共基础层
│  ApiResult / PageResult / 异常      │
└─────────────────────────────────────┘
```

### 1.3 前端多入口架构

```
index.html (统一入口)
    │ bootstrap.ts → deviceShell.ts (UA/视口解析)
    ├── PC 壳:   main_pc.ts   → App_pc.vue   → router/pc → layouts/AdminLayout
    │                                                          └─ EcommerceLayout
    ├── Mobile V1: main_mobile.ts → App_mobile.vue → router/mobile → mobile/layouts/MobileLayout
    └── Mobile V2: main_mobileV2.ts → App_mobileV2.vue → router/mobile-v2 → mobile-v2/layouts/MobileV2Layout
                      │ 共享层
                      ├── api/       (axios 封装 + 模块化 API)
                      ├── composables/ (usePomodoroSound, useTodoReminders...)
                      ├── stores/    (Pinia: app, ecSettings, library)
                      ├── i18n/      (国际化)
                      └── data/      (静态配置)
```

### 1.4 系统整体部署架构

```
┌──────────────┐    ┌──────────────┐    ┌──────────────────┐
│  PC 浏览器   │    │  手机浏览器   │    │  ESP32-S3 副屏   │
│  (管理后台)  │    │  (移动端H5)  │    │  (7寸触摸屏)     │
└──────┬───────┘    └──────┬───────┘    └────────┬─────────┘
       │ HTTP              │ HTTP                │ HTTP (局域网轮询)
       ▼                   ▼                     ▼
┌──────────────────────────────────────────────────────┐
│                   Nginx (192.168.0.114:80)            │
│      静态资源 / API 反向代理 / SSE 流支持             │
└──────────────────────┬───────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────┐
│              Spring Boot 后端 (:8080)                  │
│           admin-server.jar / JDK 17                  │
└──────┬─────────────────────────────────┬─────────────┘
       │ JDBC                             │ TCP
       ▼                                  ▼
┌──────────────┐                ┌──────────────────┐
│  MySQL 8.0   │                │   Redis 7        │
│  (Docker)    │                │  (Docker)        │
│  数据节点     │                │  会话/缓存       │
│  192.168.0.116│               │  192.168.0.116  │
└──────────────┘                └──────────────────┘
```

---

## 2. 模块职责

### 2.1 后端模块

| 模块 | 子模块 | API 路径前缀 | 核心职责 | 关联表数量 |
|------|--------|-------------|---------|-----------|
| **电商核心** | 销售订单 | `/api/ecommerce/sales-orders` | 订单CRUD、状态流转(确认/发货/退款)、批量导入(Excel)、月度概览 | ~10 |
| | 库存管理 | `/api/ecommerce/inventories` | 库存CRUD、概览/汇总/SPU统计、入库/出库/调整、装箱估算 | ~5 |
| | 月度结算 | `/api/ecommerce/monthly-settlement` | 利润结算计算、快照管理、快递对账、买家排除、订单决策 | ~6 |
| | 商品管理 | `/api/ecommerce/products` | 商品CRUD | ~2 |
| | 平台店铺 | `/api/ecommerce/platforms` | 平台CRUD、选项列表 | ~2 |
| | 店铺管理 | `/api/ecommerce/shops` | 店铺CRUD、选项列表 | ~2 |
| | 工厂管理 | `/api/ecommerce/factories` | 工厂CRUD、统计 | ~2 |
| | 纸箱管理 | `/api/ecommerce/cartons` | 纸箱CRUD、尺寸计算、SKU回填、预览图 | ~3 |
| | 快递管理 | `/api/ecommerce/express/stations` | 站点CRUD、价格管理、通知管理、复制 | ~4 |
| | 入库/出库/盘点 | `/api/ecommerce/inbound-orders` | 入库/出库/盘点单管理、状态流转 | ~6 |
| | Listing链接 | `/api/ecommerce/listing-links` | 链接CRUD、定价计算、SKU关联 | ~3 |
| | 系统设置 | `/api/ecommerce/settings` | 多维度设置(库存/导入/快递/送货单/结算/返点/通知/公司) | ~2 |
| | 图片上传 | `/api/ecommerce/images` | 电商图片上传、纸箱预览图 | - |
| **笔记模块** | 笔记管理 | `/api/notes` | 笔记CRUD、搜索、回收站、批量元数据 | ~4 |
| | 笔记本 | `/api/notebooks` | 笔记本树/CRUD | ~1 |
| | 笔记标签 | `/api/note-tags` | 标签CRUD | ~2 |
| | 图片上传 | `/api/notebook/images` | 笔记图片上传 | - |
| **待办模块** | 待办事项 | `/api/todos` | 待办CRUD、今日待办、到期提醒、提醒确认 | ~3 |
| **文档库** | 文件管理 | `/api/library/files` | 文件CRUD、搜索/收藏/最近、回收站、批量下载ZIP | ~3 |
| | 文件夹 | `/api/library/folders` | 文件夹树/CRUD | ~1 |
| | 标签 | `/api/library/tags` | 标签CRUD、文件标签同步 | ~2 |
| | 知识库 | `/api/library/kb` | KB状态切换、统计 | ~1 |
| | 回收站 | `/api/library/trash` | 恢复/永久删除/清空 | ~1 |
| | 上传 | `/api/library/upload` | 单/批量文件上传 | - |
| **番茄钟** | 记录 | `/api/pomodoro` | 记录创建、每日/汇总/今日统计 | ~1 |
| | 计划 | `/api/pomodoro/plans` | 计划CRUD、启用列表、默认计划 | ~1 |
| | 会话 | `/api/pomodoro/session` | 活跃会话查询/同步 | Redis |
| **像素狗** | 状态 | `/api/pixel-dog` | 状态查询/更新、经验值、交互 | ~1 |
| | 物品 | `/api/pixel-dog/items` | 物品CRUD | ~1 |
| **每日清单** | 24小时 | `/api/24hour` | 按日期保存/查询、统计 | ~1 |
| **部署中心** | 运行器 | `/api/deploy` | 部署预检/执行、SSE日志流、版本管理 | ~1 |
| | 数据库 | `/api/deploy/database` | 数据库快照/SQL执行/同步 | - |
| | 日志 | `/api/deploy/logs` | 日志Tail/流式推送/AI分析 | - |
| **其他** | 存储中心 | `/api/storage-center` | 存储概览、孤儿文件清理、缓存清理 | ~1 |
| | 图片空间 | `/api/image-space` | 图片分类/浏览/重命名/规范化 | ~1 |
| | 百度网盘 | `/api/baidu-pan` | OAuth授权、网盘文件管理 | ~1 |
| | 健康检查 | `/api/health` | 系统健康检查 | - |
| | 用户管理 | `/api/system/users` | 用户分页/详情/昵称更新 | ~1 |
| | 导入配置 | `/api/sys/import` | 导入字段/配置CRUD | ~2 |

### 2.2 前端模块

| 模块 | PC端路径 | 移动端路径 | 核心职责 |
|------|---------|-----------|---------|
| **首页/工作台** | `/home` | `/home` | 系统概览、快捷入口、待办预览、状态监控 |
| **电商工作台** | `/ecommerce` | `/ecommerce` | 电商全功能入口、月度结算、订单/库存/商品/快递/纸箱/工厂管理 |
| **番茄钟** | `/pomodoro` | `/home`(合一) | 番茄钟计时、计划管理、统计报表、像素风格UI |
| **像素狗** | `/pixel-dog` | `/pixel-dog` | 虚拟宠物状态/交互/物品/经验系统 |
| **笔记** | `/notebook` | `/notebook` | Markdown富文本笔记、笔记本树、标签、搜索 |
| **文档库** | `/library` | (mobile-v2) | 文件管理/预览/上传/下载、知识库 |
| **待办** | `/todos` | `/todos` | 待办CRUD、重复规则、提醒、分类 |
| **24小时** | `/24hour` | `/24hour` | 每日清单、阶段卡片、统计 |
| **部署中心** | `/deploy-docs` | - | 一键部署、SSE日志流、SQL终端 |
| **存储中心** | `/storage` | - | 存储概览、孤儿文件清理、配额管理 |
| **图片空间** | `/image-space` | - | 电商图片浏览/管理/名称规范化 |
| **设置** | `/settings` | `/settings` | 主题/语言/主色、番茄钟/待办音效、PWA |

### 2.3 固件模块（ESP32-S3）

| 子系统 | 文件 | 核心职责 |
|--------|------|---------|
| **主入口** | `main.cpp` | 8阶段上电序列、硬件初始化编排 |
| **显示驱动** | `display.cpp` | LVGL初始化、800x480 RGB并行接口、双缓冲 |
| **触摸** | `gt911_touch.cpp` | GT911 I2C触摸驱动 |
| **WiFi** | `wifi_sta.cpp` | STA模式连接、mDNS解析、功率管理 |
| **主UI** | `app_ui.cpp` | 主屏幕/锁屏/覆盖层管理、Dock导航、二级菜单 |
| **番茄钟模型** | `pomodoro_model.cpp` | 专注/休息阶段管理、计划配置、冲突处理(3s权威窗口) |
| **番茄钟同步** | `pomodoro_sync.cpp` | HTTP轮询(PUT/GET)、脏数据标记、退避策略 |
| **番茄钟进度条** | `pomodoro_bar.cpp` | LVGL进度条UI渲染 |
| **像素狗模型** | `pixel_dog_model.cpp` | 状态管理(等级/经验/亲密度/情绪)、衰减逻辑、互动 |
| **像素狗精灵** | `pixel_dog_sprite.cpp` | 16x16像素动画帧、多状态(空闲/开心/行走/睡觉等) |
| **像素狗同步** | `pixel_dog_sync.cpp` | HTTP轮询、动作队列(POST)、合并策略 |
| **媒体同步** | `media_sync.cpp` | PC媒体状态轮询、LRC歌词解析、命令发送 |
| **时钟** | `app_clock.cpp` | NTP/后端时间同步、格式化显示 |
| **电源** | `app_power.cpp` | 亮度管理(亮/暗/睡眠)、空闲超时、背光PWM |
| **设置** | `app_settings.cpp` | NVS持久化设置(亮度/API主机/字号) |
| **SD存储** | `sd_storage.cpp` | SD卡挂载、素材种子写入 |

---

## 3. 数据流

### 3.1 请求-响应数据流（标准三层架构）

```
┌──────┐     HTTP      ┌────────────┐      ┌──────────┐      ┌─────────┐
│ 浏览器│ ──────────▶  │ Controller  │ ──▶ │ Service  │ ──▶ │ Mapper  │
│ /ESP32│ ◀──────────  │ (46个)      │ ◀── │ (业务编排)│ ◀── │ (数据)  │
└──────┘               └────────────┘      └──────────┘      └────┬────┘
     统一 ApiResult<T>                                              │ JDBC
     或 SseEmitter (部署流)                                         ▼
                                                           ┌────────────────┐
                                                           │    MySQL 8     │
                                                           │   (约50张表)   │
                                                           └────────────────┘
```

### 3.2 前端内部数据流

```
Views (页面)
  │
  ├── composition: composables()  ←── stores (Pinia: app/ecSettings/library)
  │       │                               │
  │       └── 本地数据 (ref/reactive)      └── localStorage 持久化
  │
  ├── data/constants  (静态配置数据)
  │
  └── api functions → request.ts (axios) → HTTP REST → 后端
       │                    │
       │                    └── 拦截器: 统一 ApiResult.code 校验
       │                    └── 静默错误模式 (X-Silent-Error header)
       │
       ├── ApiResult.data 直接返回 (getData / postData 二次封装)
       └── 调用方基于返回的 Promise 做局部状态更新
```

### 3.3 固件同步数据流

```
ESP32 副屏                         后端                      Redis / MySQL
─────────                      ──────────                ────────────────

[番茄钟同步 - 每2-3s轮询]
  ── GET /api/pomodoro/session ──▶  查询Redis活跃会话
  ◀── PomodoroRemoteSession ◀────  返回状态
  │
  ── PUT /api/pomodoro/session ──▶  上报本地状态 (sync_dirty)
  ◀── (冲突合并后返回) ◀────────    3s权威窗口决策

[像素狗同步 - 每5s轮询]
  ── GET /api/pixel-dog/state ────▶ 查询数据库状态
  ◀── DogStateDTO ◀───────────────  bond/emotion 合并策略
  │
  ── POST /api/pixel-dog/interact ─▶ 交互动作队列
  ◀── 最终状态 ◀──────────────────   后端计算后返回

[媒体同步 - 每 0.5-2s 轮询]
  ── GET /api/v1/media/state ─────▶ 查询PC媒体播放器状态
  ◀── MediaSnapshot ◀────────────── 标题/艺术家/进度/歌词
  │
  ── POST /api/v1/media/command ──▶ 发送控制命令
  ◀── cmd_result ◀───────────────── 上一曲/播放暂停/下一曲
```

### 3.4 部署流程数据流

```
PC浏览器                              后端                   远程服务器(树莓派)
───────                              ────                  ──────────────
  ── GET /api/deploy/runner/preflight ─▶ SSH连接检查
  ◀── 预检结果 ◀─────────────────────  ~~~~~~~~~~~
  │
  ── GET /api/deploy/stream ────────▶ (SSE 流建立)       SSH进入远程
                                     │                    │
                                     │  ── SSH ─────────▶ │ 拉取代码
                                     │  ◀── 日志流 ◀───── │ 构建前端
                                     │                    │ 打包后端
  ◀── SSE 事件流 ◀────────────────── │  ◀── 日志流 ◀───── │ 重启服务
  │                                  │                    │
  ── POST /api/deploy/database/sync ─▶ SSH进数据节点       同步数据库
  ◀── 结果 ◀─────────────────────────  ~~~~~~~~~~~
```

### 3.5 笔记内容双写数据流

```
前端编辑器 (wangeditor)
    │ 防抖触发保存
    ▼
PUT /api/notes/{id} { content, hash }
    │
    ▼
后端 Service:
    1. 计算 content hash 对比
    2. 更新 MySQL 笔记元数据 (title/summary/word_count/updated_at)
    3. 写入本地文件系统: uploads/notebook-content/notes/{id}.html
    4. 异步同步到百度网盘 (BaiduPanClient)
    5. 返回新版本号
    │
    ▼
前端更新: 版本号 / 保存时间 / word_count
```

---

## 4. 核心业务流程

### 4.1 电商销售订单管理流程

```
Excel 导入 ─────────────────────────────────── 手动创建
    │                                               │
    ▼                                               ▼
上传文件 → 字段匹配(配置驱动) → 预览 → 提交导入    填写表单 → 提交
    │                                               │
    ▼                                               ▼
订单导入批次 (EcSalesOrderImportBatch)       订单 (EcSalesOrder)
    │                                               │
    ▼                                               ▼
订单行 (EcSalesOrderLine) ──────────── 出货 ────▶ 发货 → 退款 → 取消
    │
    ▼
确认 → 库存预扣 → 出库单生成 → 快递结算
```

### 4.2 库存管理流程

```
入库 ───────────────────────────────── 出库 ────────────────── 盘点
  │                                      │                      │
  ▼                                      ▼                      ▼
入库单 → 确认 → 库存增加            出库单 → 确认 → 库存扣减   盘点单 → 确认
  │                                      │                    │ 差异调整
  ▼                                      ▼                      ▼
库存日志 (流水)                     库存日志 (流水)           库存调整记录
  │                                      │
  └──────────────────┬───────────────────┘
                     ▼
          库存概览 / 汇总 / SPU状态
          装箱估算 / 入库价值统计
          工厂库存汇总
```

### 4.3 月度结算流程

```
触发: 用户选择月份 + 店铺
    │
    ▼
1. 查询该月所有销售订单 (EcSalesOrder + EcSalesOrderLine)
2. 查询快递费用 (EcExpressPrice)
3. 加载买家排除列表 (EcSettlementBuyerExclude)
4. 加载订单决策 (EcSettlementOrderDecision)
5. 计算: 收入 - 成本 - 快递费 - 返点 = 利润
6. 生成快照 (EcSettlementSnapshot)
7. 输出: 月度结算视图 (含明细)
    │
    ▼
可选: 导入快递对账单 (EcSettlementExpressBill)
  → 匹配未匹配明细 → 手工录入 → 对账完成
```

### 4.4 番茄钟多端同步流程

```
用户(A) Web端点击"开始专注"
    │
    ▼
PUT /api/pomodoro/session { takeControl=true, source=ADMIN }
    │
    ▼
Redis: pomodoro:session:active (覆盖任何已有会话)
    │
    ├──────────────────────────────────┐
    │                                  │
    ▼                                  ▼
Web端: 本地倒计时(JS)           ESP32: 轮询 GET(/session) 每2s
    │                                  │ 同步显示剩余时间
    ▼                                  │ 本地 3s 权威窗口
阶段切换 (Focus→ShortBreak)            │ (忽略远程通知)
    │                                  │
    ▼                                  │
PUT 更新会话 (remaining_sec)           ▼
    │                           冲突时: take_control 覆盖
    │                                   │
    ▼                                   ▼
专注完成 → POST /work-record / 保存   同步显示 → 音效提示
```

### 4.5 像素狗状态管理流程

```
┌─────────────────────────────────────────────────────────────────┐
│                      状态驱动引擎                                │
│                                                                 │
│  bond (亲密度 0-100) ← 每日互动 + 番茄钟完成                   │
│  emotion (情绪 -100~100) ← 互动+  / 长时间不互动-              │
│  xp (经验值) ← 互动事件 + 番茄钟奖励                           │
│  level (等级) ← xp 达到阈值自动升级                             │
│                                                                 │
│  衰减逻辑:                                                      │
│  - 3天未互动: bond → 0                                         │
│  - 30分钟未触摸: emotion → -100                                 │
│  - 1小时后: emotion 自然回升                                    │
└─────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│                      交互动作                                    │
│  pet (摸头) - 基础                                              │
│  greet (打招呼) - 有冷却时间                                    │
│  nuzzle (蹭蹭) - 高亲密度解锁                                   │
│  hug (抱抱) - 最高亲密度解锁                                    │
└─────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│                      动画表现                                    │
│  状态机: idle ↔ walking ↔ happy ↔ sleeping ↔ eating ↔ petting  │
│         ↔ greeting ↔ focus                                     │
│  16x16 像素精灵: 2-3帧循环动画                                  │
│  LVGL canvas 缩放渲染                                           │
└─────────────────────────────────────────────────────────────────┘
```

---

## 5. 技术债务列表

### 5.1 架构层面

| 编号 | 类别 | 问题描述 | 严重程度 | 影响范围 | 建议修复方向 |
|------|------|---------|---------|---------|------------|
| #01 | **模块化** | admin-system 模块过于庞大，46个Controller + 60+Mapper全部堆在一个模块中 | 高 | 构建速度、维护成本 | 按业务域拆分为独立 Module：ecommerce / notebook / library / pomodoro / pixel-dog / deploy / system |
| #02 | **API设计** | API路径风格不统一：电商模块使用 `PUT /{id}` 更新，但部分模块使用 `POST /{id}/update` | 中 | API可维护性 | 统一使用 RESTful 风格：POST 创建、PUT 更新、DELETE 删除 |
| #03 | **跨域架构** | 没有明确的 API Gateway / BFF 层，Nginx 直接裸代理到 Spring Boot | 中 | 安全、限流 | 引入网关层处理鉴权/限流/路由聚合 |
| #04 | **微服务** | 单体架构，所有业务耦合在一个 JAR 包中，无法独立部署 | 中 | 弹性、扩展性 | 评估是否需拆分微服务，当前规模可维持单体 |
| #05 | **端到端类型** | 前端 TypeScript 类型与后端 Java DTO 手动维护，无自动同步 | 低 | 类型安全 | 引入 openapi-generator 从 Swagger 生成 TS 类型 |
| #06 | **移动端双版本** | mobile V1 和 V2 两套实现共存，组件大量重复，维护成本高 | 中 | 维护成本 | 逐步废弃 V1，统一到 V2 架构 |

### 5.2 代码质量

| 编号 | 类别 | 问题描述 | 严重程度 | 影响范围 | 建议修复方向 |
|------|------|---------|---------|---------|------------|
| #07 | **重复代码** | 移动端 V1 和 V2 存在大量相同逻辑的组件重复（如商品卡片、纸箱计算、确认弹窗等） | 高 | 维护成本翻倍 | 提取公共组件到 `mobile/shared/`，V1/V2 各自引用 |
| #08 | **Controller 层** | 部分 Controller 存在业务逻辑（非纯校验），违反分层规范 | 中 | 可测试性 | 将业务逻辑下沉到 Service 层 |
| #09 | **VO/DTO 转换** | 部分 VO/DTO 转换使用手动 setter，未统一使用 MapStruct | 低 | 样板代码 | 引入 MapStruct 统一转换 |
| #10 | **错误处理** | 部分异步/后台操作缺少完善的错误处理与重试机制 | 中 | 系统韧性 | 引入 Spring Retry / Resilience4j |
| #11 | **固件硬编码** | ESP32 固件中存在一些 magic number（如延时 2s、超时 3s 等） | 低 | 可配置性 | 提取到配置文件或 NVS 存储 |
| #12 | **前端 bundle** | 移动端打包包含了 PC 端组件代码（chunk 分割不够细粒度） | 中 | 移动端首屏加载 | 优化 manualChunks，确保移动端构建不包含 PC-only 组件 |

### 5.3 测试与质量保障

| 编号 | 类别 | 问题描述 | 严重程度 | 影响范围 | 建议修复方向 |
|------|------|---------|---------|---------|------------|
| #13 | **测试覆盖** | 项目中无单元测试文件（无 `test/` 或 `__tests__/` 目录） | **严重** | 质量保障 | 建立 JUnit 5 + Mockito 后端测试框架 + Vitest 前端测试 |
| #14 | **E2E测试** | 无端到端集成测试，关键业务流程（订单导入/结算计算）缺乏自动化回归 | 高 | 发布风险 | 引入 Playwright/Cypress 进行关键路径 E2E 测试 |
| #15 | **API文档** | 后端 API 无 Swagger/OpenAPI 文档生成 | 中 | 协作效率 | 引入 springdoc-openapi 自动生成 API 文档 |
| #16 | **CI/CD** | 无持续集成流水线配置 | 中 | 自动化 | 配置 GitHub Actions / GitLab CI 自动构建+测试 |

### 5.4 安全与合规

| 编号 | 类别 | 问题描述 | 严重程度 | 影响范围 | 建议修复方向 |
|------|------|---------|---------|---------|------------|
| #17 | **认证鉴权** | 系统无用户认证机制，API 可直接访问 | **严重** | 数据安全 | 引入 Spring Security + JWT 认证，前端路由守卫 |
| #18 | **密钥暴露** | 百度网盘 app-key / secret-key 硬编码在 application.yml 中 | 高 | 凭证泄露 | 迁移到环境变量或密钥管理服务 |
| #19 | **SQL注入** | 部分 Mapper 使用 `${}` 拼接参数（需验证） | 高 | 注入风险 | 全局替换为 `#{}` 参数化查询 |
| #20 | **文件上传** | 文件上传无文件类型校验和大小严格限制 | 中 | 安全风险 | 引入白名单扩展名校验 + 文件内容检测 |
| #21 | **CORS配置** | application.yml 中无显式 CORS 配置，依赖框架默认 | 中 | 跨域安全 | 显式配置 allowedOrigins |

### 5.5 性能与可维护性

| 编号 | 类别 | 问题描述 | 严重程度 | 影响范围 | 建议修复方向 |
|------|------|---------|---------|---------|------------|
| #22 | **数据库索引** | 部分大表（销售订单、库存日志）缺少关键查询字段的复合索引 | 中 | 查询性能 | 分析慢查询日志，补充覆盖索引 |
| #23 | **Redis 序列化** | Redis 中的番茄钟会话使用 JDK 序列化（默认），可读性差且体积大 | 低 | 可观测性 | 切换为 JSON 序列化（GenericJackson2JsonRedisSerializer） |
| #24 | **前端缓存** | 移动端页面没有 service worker 离线缓存策略 | 中 | 离线体验 | 配置 PWA precache 关键页面 |
| #25 | **固件OTA** | ESP32 固件无 OTA 升级机制，更新需烧录 | 中 | 维护成本 | 引入 ESP-IDF OTA 组件，实现远程固件升级 |
| #26 | **日志管理** | 日志仅本地文件，无集中收集 | 低 | 排障效率 | 引入 ELK/Loki 日志集中管理 |
| #27 | **监控告警** | 无应用监控（JVM/API QPS/慢请求） | 中 | 运维 | 引入 Spring Boot Actuator + Prometheus + Grafana |
| #28 | **数据库迁移** | SQL 脚本手动管理（文件名前缀排序），无 Flyway/Liquibase | 中 | 变更管理 | 引入 Flyway 自动化迁移 |

### 5.6 固件特定

| 编号 | 类别 | 问题描述 | 严重程度 | 影响范围 | 建议修复方向 |
|------|------|---------|---------|---------|------------|
| #29 | **断线重连** | WiFi 同步任务在断线后缺乏优雅重连与状态恢复 | 高 | 设备可用性 | 实现 WiFi 状态机 + 同步缓存队列 |
| #30 | **内存管理** | LVGL 帧缓冲 + HTTP 响应缓冲区使用 PSRAM，但未做低内存保护 | 中 | 稳定性 | 添加内存水位监控与保护机制 |

### 5.7 技术债务优先级矩阵（严重性 × 影响范围）

```
严重
  │
  │  #01 (模块拆分)      #13 (无测试)     #17 (无认证)
  │  #07 (移动端重复)     #14 (无E2E)     #18 (密钥硬编码)
  │                      #19 (SQL注入)
  │
  │  #02 (API风格)       #15 (API文档)    #20 (文件上传)
  │  #03 (无网关)        #16 (CI/CD)      #21 (CORS)
  │  #06 (V1/V2共存)    #22 (索引)       #28 (DB迁移)
  │                     #25 (固件OTA)    #29 (断线重连)
  │
  │  #05 (类型同步)      #23 (Redis)      #26 (日志)
  │  #09 (VO转换)        #24 (前端缓存)    #27 (监控)
  │  #10 (错误处理)      #30 (内存)
  │  #11 (硬编码)
  │  #12 (bundle)
  │
  └───────────────────────────────────────────▶ 影响范围
   低                 中                  高/全局
```

### 5.8 快速修复（低投入高收益）

以下技术债务可在较短时间内修复且收益显著：

| 编号 | 问题 | 预估工作量 | 收益 |
|------|------|-----------|------|
| #18 | 密钥迁移到环境变量 | 0.5天 | 消除凭证泄露风险 |
| #19 | SQL注入审查与修复 | 1天 | 消除注入风险 |
| #15 | 引入 springdoc-openapi | 1天 | 提升协作效率 |
| #22 | 关键表索引补充 | 1天 | 提升查询性能 |
| #23 | Redis JSON 序列化 | 0.5天 | 提升可观测性 |
| #28 | 引入 Flyway 迁移 | 2天 | 提升数据库变更管理 |

---

## 附录

### A. 技术栈汇总

| 层级 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 后端框架 | Spring Boot | 3.3.7 | Web服务 |
| ORM | MyBatis-Plus | 3.5.9 | 数据访问 |
| 数据库 | MySQL | 8.0 (Docker) | 持久化 |
| 缓存 | Redis | 7-alpine (Docker) | 会话/实时状态 |
| 前端框架 | Vue 3 | 3.5.13 | 前端UI |
| 构建工具 | Vite | 6.0.5 | 前端构建 |
| UI库 | Element Plus | 2.9.1 | PC端组件 |
| 状态管理 | Pinia | 2.3.0 | 前端状态 |
| 富文本 | wangEditor | 5.1.23 | 笔记编辑器 |
| 图表 | ECharts | 6.1.0 | 数据可视化 |
| 嵌入式GUI | LVGL | 9.x | ESP32显示 |
| 嵌入式框架 | ESP-IDF | 5.x | ESP32开发 |
| 反向代理 | Nginx | latest | 静态服务/API代理 |
| 容器化 | Docker Compose | - | MySQL/Redis 部署 |

### B. 数据量估算

| 表类型 | 约数量 | 核心表 |
|--------|-------|-------|
| 电商主表 | ~20张 | sales_order, inventory, product, shop, platform, carton, factory, express_station |
| 电商关联表 | ~15张 | listing_link, inbound/outbound/stocktake order, settlement |
| 笔记表 | ~5张 | note, notebook, note_tag, note_tag_rel |
| 待办表 | ~5张 | todo_item, todo_repeat, todo_remind |
| 文档库 | ~5张 | file, folder, tag, event_log |
| 番茄钟 | ~3张 | plan, record, session(Redis) |
| 像素狗 | ~2张 | state, item |
| 系统表 | ~5张 | user, import_profile, import_batch, config |

### C. API 端点统计

| 模块 | 端点数量 |
|------|---------|
| 电商 (Ecommerce) | ~90+ |
| 文档库 (Library) | ~20+ |
| 笔记 (Notebook) | ~15+ |
| 待办 (Todo) | ~7 |
| 番茄钟 (Pomodoro) | ~10 |
| 像素狗 (Pixel Dog) | ~6 |
| 部署 (Deploy) | ~10 |
| 其他 | ~20 |
| **总计** | **~180+** |

---

> **本报告基于静态代码分析生成，未运行任何代码。建议在实施重构前确认各项技术债务的实际情况。**
