# 番茄钟模块

## 概述

番茄钟模块提供专注计时功能，支持 Web 端与 ESP32 副屏双向同步，
是系统中实时性要求最高的模块之一。

## 后端实现

### 模块结构

```
番茄钟模块（后端）
├── PomodoroPlanController       # 计划管理接口
├── PomodoroSessionController    # 实时会话接口
├── PomodoroRecordController     # 完成记录接口
├── PomodoroPlanService          # 计划服务
├── PomodoroSessionService       # 会话服务（Redis）
├── PomodoroRecordService        # 记录服务
├── PomodoroPlanMapper           # 计划数据访问
├── PomodoroRecordMapper         # 记录数据访问
├── PomodoroPlan                 # 计划实体
└── PomodoroRecord               # 记录实体
```

### 数据库表

#### pomodoro_plan（番茄钟计划）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| name | VARCHAR | 计划名称 |
| work_duration_min | INT | 专注时长（分钟） |
| short_break_min | INT | 短休息时长（分钟） |
| long_break_min | INT | 长休息时长（分钟） |
| rounds_before_long_break | INT | 几次专注后长休息 |
| daily_goal_rounds | INT | 每日目标轮次 |
| sort_order | INT | 排序 |
| is_default | TINYINT | 是否默认 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| deleted | TINYINT | 逻辑删除 |

#### pomodoro_record（完成记录）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| plan_id | BIGINT | 计划ID |
| type | VARCHAR | 类型：FOCUS / SHORT_BREAK / LONG_BREAK |
| duration_sec | INT | 实际时长（秒） |
| planned_duration_sec | INT | 计划时长（秒） |
| record_date | DATE | 记录日期 |
| start_time | DATETIME | 开始时间 |
| end_time | DATETIME | 结束时间 |
| source | VARCHAR | 来源：ADMIN / DEVICE |
| create_time | DATETIME | 创建时间 |

### Redis 会话存储

实时会话完全存储在 Redis 中，不经过数据库。

#### Redis Key 设计

| Key | 类型 | TTL | 说明 |
|-----|------|-----|------|
| `pomodoro:session:active` | Hash | 24h | 当前活动会话 |
| `pomodoro:device:last_seen_ms` | String | - | 设备最后心跳时间 |

#### 会话数据结构（Hash）

| 字段 | 类型 | 说明 |
|------|------|------|
| `phase` | String | 当前阶段：IDLE/FOCUS_RUNNING/FOCUS_PAUSED/BREAK_RUNNING/BREAK_PAUSED |
| `remainingSeconds` | int | 剩余秒数 |
| `lastUpdateTime` | long | 最后更新时间戳（毫秒） |
| `runState` | String | 运行状态：IDLE/RUNNING/PAUSED |
| `controller` | String | 控制方：ADMIN / DEVICE |
| `source` | String | 最后更新来源 |
| `planId` | long | 当前计划ID |
| `sessionWorkRounds` | int | 会话已完成轮次 |
| `lastSeenSource` | String | 最后心跳来源 |

### 核心服务：PomodoroSessionService

**位置**：`admin-system/.../service/PomodoroSessionService.java`

#### 核心方法

| 方法 | 说明 |
|------|------|
| `getActiveSession()` | 获取当前活动会话（自动推算剩余时间） |
| `syncSession(request)` | 同步会话状态 |
| `isPlanEditBlocked()` | 判断计划编辑是否被锁定 |

#### 控制器抢占机制

为避免两端同时操作导致冲突，使用控制器（Controller）机制：

```
规则：
1. 谁先点击开始/暂停/重置，谁成为控制方
2. 非控制方只能跟随显示，不能操作
3. 空闲状态下任何一方都可发起控制
4. 副屏操作时若Web端空闲，自动抢占控制权
```

#### 智能覆盖规则

```
┌─────────────────────────────────────────────┐
│  副屏发起操作时                              │
│  ├─ Web端空闲(IDLE) → 直接覆盖，副屏控制    │
│  ├─ Web端运行中 → 需 take_control=true      │
│  └─ Web端暂停中 → 比较时间戳，新的覆盖旧的   │
└─────────────────────────────────────────────┘
```

### API 接口

#### 计划管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/pomodoro/plans` | 计划列表 |
| GET | `/api/pomodoro/plans/{id}` | 计划详情 |
| GET | `/api/pomodoro/plans/default` | 默认计划 |
| POST | `/api/pomodoro/plans` | 创建计划 |
| PUT | `/api/pomodoro/plans/{id}` | 更新计划 |
| DELETE | `/api/pomodoro/plans/{id}` | 删除计划 |

#### 实时会话

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/pomodoro/session` | 获取当前会话 |
| PUT | `/api/pomodoro/session` | 同步会话状态 |

#### 完成记录

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/pomodoro/records` | 写入完成记录 |
| GET | `/api/pomodoro/records` | 记录列表 |
| GET | `/api/pomodoro/stats/daily` | 按日统计 |
| GET | `/api/pomodoro/stats/summary` | 区间汇总 |
| GET | `/api/pomodoro/stats/today` | 今日进度 |

## 前端实现（PC 端）

### 页面结构

```
番茄钟页面（3个Tab）
├── 专注计时
│   ├── 倒计时圆环
│   ├── 开始/暂停/重置按钮
│   ├── 当前计划信息
│   └── 同步状态指示
├── 计划管理
│   ├── 计划列表
│   ├── 新增/编辑/删除
│   └── 设置默认计划
└── 报表统计
    ├── 今日进度
    ├── 本周统计
    ├── 月度趋势图
    └── 记录列表
```

### 同步机制

- **轮询间隔**：2秒
- **操作上报**：立即发送
- **同步状态显示**：顶栏标签显示
  - "已与副屏同步" = 副屏控制中
  - "本页控制中" = 浏览器控制中

## 固件实现（ESP32）

### 模块结构

```
番茄钟模块（固件）
├── pomodoro_model.cpp / .h        # 状态模型
├── pomodoro_sync.cpp / .h         # 同步任务
├── pomodoro_bar.cpp / .h          # 进度条UI
├── pomodoro_plan_cache.cpp / .h   # 计划缓存（NVS）
└── pomodoro_api_config.cpp / .h   # API配置
```

### 状态模型（pomodoro_model.h）

#### 阶段枚举

```
PomodoroPhase
├── Idle          空闲
├── Focus         专注
├── ShortBreak    短休息
└── LongBreak     长休息
```

#### 用户动作枚举（6种）

```
PomodoroUserAction
├── StartFocus       开始专注
├── PauseFocus       暂停专注
├── StartShortBreak  开始短休息
├── PauseShortBreak  暂停短休息
├── StartLongBreak   开始长休息
└── PauseLongBreak   暂停长休息
```

#### 状态快照（PomodoroSnapshot）

UI 层使用的完整状态数据：

| 字段 | 类型 | 说明 |
|------|------|------|
| phase | PomodoroPhase | 当前阶段 |
| running | bool | 是否运行中 |
| remaining_sec | int | 剩余秒数 |
| total_sec | int | 阶段总秒数 |
| session_work_rounds | int | 会话轮次 |
| today_work_rounds | int | 今日轮次 |
| plan_id | int64_t | 计划ID |
| pending | PomodoroPendingPhase | 待执行阶段 |
| backend_connected | bool | 后端连接状态 |
| today_goal_done | bool | 今日目标是否完成 |

### 同步任务（pomodoro_sync.cpp）

FreeRTOS 任务，负责与后端 HTTP 通信。

#### 同步流程

```
每1-2秒执行一次：
1. 检查是否有本地脏数据需要上报
   ├─ 有 → PUT /api/pomodoro/session 上报
   └─ 无 → GET /api/pomodoro/session 拉取

2. 拉取到新数据后
   ├─ 检查是否应该应用（时间戳、控制方）
   └─ 应用到本地模型

3. 完成专注时
   └─ POST /api/pomodoro/records 上报记录
```

#### 轮询间隔策略

| 状态 | 轮询间隔 | 说明 |
|------|---------|------|
| 运行中 | 1-2秒 | 实时同步 |
| 暂停/空闲 | 5-10秒 | 降低频率 |
| 网络异常 | 指数退避 | 最长60秒 |

### UI 展示（pomodoro_bar.cpp）

LVGL 实现的番茄钟进度条界面：

- 圆形进度条（表示当前阶段进度）
- 剩余时间数字显示
- 阶段指示（专注/休息）
- 今日轮次显示
- 开始/暂停按钮（触摸交互）

### 计划缓存

- 存储位置：NVS Flash
- 作用：离线时也能使用本地计划
- 同步：联网后从后端拉取更新

## 状态流转

### 完整状态机

```
                    ┌──────────┐
                    │   IDLE   │  空闲
                    └────┬─────┘
                         │ 开始专注
                         ▼
            ┌──────────────────────┐
            │   FOCUS_RUNNING      │  专注中
            └──┬───────────────┬───┘
               │ 暂停          │ 时间到
               ▼               ▼
            ┌─────────┐   ┌──────────────┐
            │ FOCUS_  │   │ SHORT_BREAK_ │
            │ PAUSED  │   │ RUNNING      │  短休息中
            └────┬────┘   └──┬────────┬──┘
                 │ 继续       │ 暂停    │ 时间到
                 ▼            ▼        ▼
            (回到运行)   ┌─────────┐  (回到IDLE
                         │ SHORT_  │   记录完成)
                         │ BREAK_  │
                         │ PAUSED  │
                         └─────────┘
```

## 多端同步时序

### Web 端操作，副屏跟随

```
Web端用户点击开始
    │
    ├─ PUT /api/pomodoro/session
    │   { source: ADMIN, takeControl: true, ... }
    │
    ▼
后端更新 Redis 会话
    │
    ├─ Web端：本地计时 + 定期校准
    │
    └─ ESP32 轮询（2秒后）
        ├─ GET /api/pomodoro/session
        └─ 应用到本地显示
```

### 副屏操作，Web 端跟随

```
副屏用户点击开始
    │
    ├─ PUT /api/pomodoro/session
    │   { source: DEVICE, takeControl: true, ... }
    │
    ▼
后端更新 Redis 会话
    │
    ├─ 副屏：本地计时 + 定期上报
    │
    └─ Web端轮询（1-2秒后）
        ├─ GET /api/pomodoro/session
        └─ 应用到本地显示
```

## 与像素狗联动

番茄钟完成时会触发像素狗的奖励：

```
专注完成
    │
    ▼
dog_model_on_pomodoro_complete(duration_minutes)
    │
    ├─ 增加经验值（根据时长）
    ├─ 增加陪伴值
    ├─ 提升情绪
    └─ 播放开心动画
```

## 配置

### 后端配置

无特殊配置，使用 Redis 默认配置。

### 前端配置

- 轮询间隔：2000ms
- 静默错误：轮询请求使用静默模式

### 固件配置

- API 地址：SD卡 `config/pomodoro_host.txt`
- 轮询间隔：运行时 1-2s，空闲时更长
- 超时时间：5秒
