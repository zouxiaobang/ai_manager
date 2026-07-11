# 像素狗模块

## 概述

像素狗是 ESP32 副屏上的虚拟宠物功能，提供成长系统、陪伴值、情绪系统和多种动画状态，
与番茄钟联动（专注完成获得经验值），增加用户使用番茄钟的动力和趣味性。

## 后端实现

### 模块结构

像素狗状态通过后端 API 与 Web 端和固件端双向同步。

### API 接口

**基础路径**：`/api/pixel-dog`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/pixel-dog/state` | 获取像素狗当前状态 |
| PUT | `/api/pixel-dog/state` | 上报像素狗本地状态 |
| POST | `/api/pixel-dog/interact?action=xxx` | 互动操作（pet/greet/nuzzle/hug） |

### 核心数据

#### 成长系统

| 属性 | 说明 | 获取方式 |
|------|------|---------|
| 等级 (level) | 像素狗等级 | 经验值累积升级 |
| 经验值 (xp) | 当前经验 | 番茄钟专注完成、互动 |
| 下一级经验 (xp_next) | 升级所需经验 | 随等级递增 |

#### 陪伴系统

| 属性 | 说明 | 获取方式 |
|------|------|---------|
| 陪伴值 (bond) | 亲密度（上限100） | 摸头、打招呼、蹭蹭、抱抱、日常陪伴 |

#### 情绪系统

| 属性 | 说明 | 范围 |
|------|------|------|
| 情绪值 (emotion) | 当前情绪 | -100 ~ 100 |
| - | 低落 | < 0 |
| - | 平静 | 0 ~ 30 |
| - | 开心 | 30 ~ 70 |
| - | 非常开心 | > 70 |

### 状态枚举

```
DogStatus
├── IDLE          待机/空闲
├── WALKING       行走
├── HAPPY         开心
├── SLEEPING      睡觉
├── EATING        吃东西
├── PETTING       被抚摸中      ← 新增
├── GREETING      打招呼中      ← 新增
├── FOCUS         专注模式      ← 新增（番茄钟运行时）
└── MAX           状态数量上限
```

## 固件实现

### 模块结构

```
像素狗模块（固件）
├── pixel_dog_model.cpp / .h        # 数据模型
├── pixel_dog_sprite.cpp / .h       # 精灵动画
├── pixel_dog_sync.cpp / .h         # 同步任务
├── pixel_dog_api_config.cpp / .h   # API配置
├── pixel_ui.cpp                    # 像素UI渲染
└── Kconfig.projbuild               # Kconfig配置菜单
```

### 数据模型（pixel_dog_model.h）

#### DogState 结构体

```c
typedef struct {
    uint32_t level;               ///< 等级
    uint32_t xp;                  ///< 当前经验值
    uint32_t xp_next;             ///< 升级所需经验值
    uint32_t bond;                ///< 陪伴值（亲密度，上限100）
    int8_t emotion;               ///< 情绪值（-100 ~ 100）
    int64_t last_interact_ts;     ///< 上次互动时间戳
    int64_t last_greet_ts;        ///< 上次打招呼时间戳
    DogStatus status;             ///< 当前行为状态
    uint8_t unlocked_items;       ///< 已解锁的物品（位标志）
    uint64_t equipped_items;      ///< 已装备的物品位掩码  ← 新增
} DogState;
```

#### API 函数列表

| 函数 | 说明 | 版本 |
|------|------|------|
| `dog_model_init()` | 从 NVS 初始化模型 | 原始 |
| `dog_model_get()` | 获取当前状态（只读指针） | 原始 |
| `dog_model_add_xp(amount)` | 增加经验值，触发升级检查 + NVS持久化 + 同步标记 | 原始 |
| `dog_model_add_bond(amount)` | 增加陪伴值 | 原始 |
| `dog_model_add_emotion(amount)` | 调整情绪值 | 原始 |
| `dog_model_pet()` | 摸头互动：+陪伴值 +情绪 | 原始 |
| `dog_model_greet()` | 打招呼互动，有冷却时间 | 原始 |
| `dog_model_nuzzle()` | **蹭蹭互动**：亲密度较高时解锁，+情绪较多 | **新增** |
| `dog_model_hug()` | **抱抱互动**：亲密度高时解锁，大幅增加情绪 | **新增** |
| `dog_model_on_pomodoro_complete(min)` | 番茄钟完成回调（分档奖励经验/陪伴/情绪） | 原始+增强 |
| `dog_model_tick()` | 时钟滴答：衰减计算 + 番茄钟专注状态监控 | 增强 |
| `dog_model_set_status(status)` | 设置状态 | 原始 |
| `dog_model_apply_remote_state(remote)` | **应用远程同步状态**（max合并、夜间/专注保护） | **新增** |
| `dog_model_override_state(remote)` | **强制覆盖本地状态**（后端权威数据覆盖） | **新增** |
| `dog_model_get_speech()` | **获取当前对话文本**（亲密度分档对话系统） | **新增** |

### 同步函数（pixel_dog_sync.h）

| 函数 | 说明 |
|------|------|
| `dog_sync_start()` | 启动同步任务（创建 FreeRTOS 任务） |
| `dog_sync_mark_dirty()` | 标记同步脏数据，唤醒同步任务 |
| `dog_sync_interact(action)` | 互动后立即同步到后端（push + pull 双边同步） |

### 成长系统

#### 经验值获取

| 来源 | 经验值 | 冷却 |
|------|--------|------|
| 番茄钟专注 ≥20分钟 | 15 XP | - |
| 番茄钟专注 ≥10分钟 | 5 XP | - |
| 番茄钟专注 <10分钟 | 3 XP | - |
| 摸头互动 | 少量 | 短冷却 |
| 打招呼 | 中量 | 长冷却（每日1次） |

#### 升级公式

```
xp_next = level × 100 + (level - 1) × 50
```

升级时自动重置 xp = xp - xp_next。

### 陪伴系统

- 1小时无互动后开始衰减：`bond -= elapsed_hours`
- 互动增加：摸头、打招呼、蹭蹭、抱抱、番茄钟完成
- 陪伴值上限 100
- 高陪伴值解锁蹭蹭/抱抱互动和特殊对话

### 情绪系统

#### 情绪影响因素

| 因素 | 情绪变化 | 说明 |
|------|---------|------|
| 摸头 | +5 | 每次摸头 |
| 打招呼 | +10 | 每日首次 |
| 蹭蹭 | +8 | 亲密度较高时解锁 |
| 抱抱 | +15 | 亲密度高时解锁 |
| 番茄钟完成 | +5 | 专注完成 |
| 30分钟无互动 | -elapsed_minutes | 超过30分钟后每分钟-1 |
| 3天无互动 | 每天-1 | 长期不理的缓慢衰减 |

#### 情绪影响的表现

- **非常开心**（>70）：蹦蹦跳跳、开心动画
- **开心**（30~70）：正常活动、摇尾巴
- **平静**（0~30）：待机、偶尔走动
- **低落**（<0）：动作缓慢、低头、消极对话

### 智能对话系统（dog_model_get_speech）

根据陪伴值和情绪值自动生成对话文本，分为多个亲密度等级：

```
亲密度公式（intimacy_x10）：
  (emotion+100)/2 × 0.4 + bond × 0.6
  整数形式：intimacy_x10 = (emotion+100)×2 + bond×6（范围0~1000）

对话等级划分：
├── bond < 20 且 emotion ≥ 20    → 礼貌型对话
├── bond < 20 且 emotion ≥ 0     → 冷淡型对话
├── bond < 20 且 emotion ≥ -30   → 委屈型对话
├── bond < 20 且 emotion < -30   → 生气型对话
├── intimacy < 600                → 普通朋友型对话
├── intimacy < 850                → 亲密伙伴型对话
├── intimacy ≥ 850 且 emotion ≥ 0 → 热恋型对话
└── intimacy ≥ 850 且 emotion < 0 → 依赖型对话

长期不理检测（≥3天）→ 思念型对话
```

### 状态动画

#### 精灵动画系统（pixel_dog_sprite.cpp）

使用像素精灵图实现动画效果：

| 状态 | 帧率 | 帧数 | 说明 |
|------|------|------|------|
| IDLE | 2 fps | 4帧 | 待机呼吸动画 |
| WALKING | 8 fps | 8帧 | 行走循环 |
| HAPPY | 10 fps | 6帧 | 开心跳跃 |
| SLEEPING | 1 fps | 4帧 | 睡觉呼吸 |
| EATING | 4 fps | 4帧 | 吃东西 |
| PETTING | 6 fps | 4帧 | 被抚摸反应 |
| GREETING | 8 fps | 5帧 | 打招呼动作 |
| FOCUS | 2 fps | 3帧 | 专注凝视 |

#### 精灵图格式

- 格式：PNG（RGBA）
- 存储：SD卡资源，启动时加载到内存
- 排列：水平排列，每帧大小相同
- 尺寸：64×64 或 48×48 像素

### 状态机

```
                    ┌─────────┐
              ┌────▶│  IDLE   │◀────┐
              │     └────┬────┘     │
              │          │          │
     一段时间无操作   走动一下   心情好时
              │          │          │
              │          ▼          │
           ┌──────┐  ┌───────┐  ┌───────┐
           │ SLEEP│  │ WALK  │  │ HAPPY │
           │ ING  │  │ ING   │  │       │
           └──────┘  └───────┘  └───────┘
                         │
                      吃东西时
                         ▼
                      ┌───────┐
                      │ EAT-  │
                      │ ING   │
                      └───────┘

专注模式（新增）：
番茄钟专注运行中 → FOCUS状态（覆盖其余状态）
番茄钟结束/暂停  → 恢复IDLE
```

### 状态转换规则

| 当前状态 | 触发条件 | 目标状态 |
|---------|---------|---------|
| 任意 | 番茄钟专注运行中 | FOCUS |
| FOCUS | 番茄钟暂停/结束 | IDLE |
| 任意 | 番茄钟完成 | HAPPY（5秒）→ 恢复原状态 |
| IDLE | 随机时间（5-15秒） | WALKING |
| WALKING | 走到边缘/随机时间 | IDLE |
| IDLE | 深夜（22:00~6:00）/长时间无操作 | SLEEPING |
| SLEEPING | 有互动/白天 | IDLE |
| 任意 | 摸头互动 | PETTING → IDLE |
| 任意 | 打招呼互动 | GREETING → IDLE |

### 触摸交互

| 交互 | 效果 |
|------|------|
| 点击像素狗 | 摸头互动：+陪伴值 +情绪 → PETTING动画 |
| 长按 | 弹出互动菜单（摸头/打招呼/蹭蹭/抱抱） |
| 左右滑动 | 狗狗向滑动方向走动 |

### 同步任务（pixel_dog_sync.cpp）

与后端双工同步像素狗状态：

#### 同步策略

```
同步循环：
  1. 等待（定时15s 或 任务通知唤醒）
  2. 检查网络就绪
  3. 检查脏标记
  4. 如果有脏数据 → PUT /api/pixel-dog/state（先推）
  5. GET /api/pixel-dog/state（再拉，获取权威数据）
  6. 应用远程状态（dog_model_apply_remote_state）
```

| 操作 | 时机 |
|------|------|
| push（上报） | 脏数据标记时 + 引入变动后立即通知 |
| pull（拉取） | 每次心跳周期 + push之后 |
| 互动同步 | 触控操作后立即通过 `dog_sync_interact()` 同步 |

#### 重试策略

```
失败退避：
├── 连续0次失败 → 15s间隔
├── 连续1次失败 → 10s（第一次警报日志）
├── 连续2次失败 → 15s
├── 连续3次失败 → 20s
└── ≥4次失败   → 30s（最大退避）
```

#### 互动即时同步（dog_sync_interact）

```
用户操作（摸头/打招呼/蹭蹭/抱抱）
  → POST /api/pixel-dog/interact?action=xxx
  → 后端处理并返回权威状态
  → dog_model_override_state() 无条件覆盖本地状态
```

### NVS 持久化

像素狗所有状态持久化到 NVS Flash：

```
命名空间：pixel_dog

保存字段：
├── level          (u32)
├── xp             (u32)
├── bond           (u32)
├── emotion        (i8)
├── last_interact  (i64)
├── last_greet     (i64)
├── unlocked       (u8)
├── equipped_items (u64)   ← 新增（物品装备位掩码）
```

### Kconfig 配置

**菜单名**：`Pixel Dog`

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `PIXEL_DOG_SYNC_ENABLE` | y | 启用像素狗后端同步 |
| `PIXEL_DOG_API_HOST` | 无 | 后端主机（SD卡配置可覆盖） |
| `PIXEL_DOG_API_PORT` | 8080 | 后端HTTP端口 |

### 对话系统详解

```
dog_model_get_speech() 对话逻辑：
├── 长时间无互动（≥3天） → 思念型随机对话（4种）
├── bond < 20（低亲密度）
│   ├── emotion ≥ 20  → 礼貌型（5种）
│   ├── emotion ≥ 0   → 冷淡型（5种）
│   ├── emotion ≥ -30 → 委屈型（5种）
│   └── emotion < -30 → 生气型（5种）
├── intimacy_x10 < 600（普通朋友）
│   ├── emotion ≥ 20  → 积极日常（5种）
│   └── emotion < 20  → 消极日常（5种）
├── intimacy_x10 < 850（亲密伙伴）
│   ├── emotion ≥ 10  → 撒娇黏人（5种）
│   └── emotion < 10  → 思念依赖（5种）
└── intimacy_x10 ≥ 850（至亲）
    ├── emotion ≥ 0   → 热恋告白（5种）
    └── emotion < 0   → 委屈挽留（5种）
```

### UI 展示

像素狗显示在副屏的像素狗页面或首页小部件：

```
┌─────────────────────────────────┐
│  🐕  像素狗名字    Lv.5          │
│                                  │
│     ┌──────────────────┐        │
│     │                  │        │
│     │   像素狗动画区    │        │
│     │   (64×64 像素)    │        │
│     │                  │        │
│     └──────────────────┘        │
│   "今天过得不错！" ← 智能对话    │
│  经验: ████████░░ 75%            │
│  陪伴: ██████████ 100%           │
│  心情: 😊 开心                    │
│                                  │
│  [摸头] [蹭蹭] [抱抱] [喂食]      │
└─────────────────────────────────┘
```

## 与番茄钟联动

### 联动机制

番茄钟专注运行时，像素狗自动进入 FOCUS 状态（专注凝视动画）。
专注完成时自动调用奖励函数：

```cpp
void dog_model_on_pomodoro_complete(uint32_t duration_minutes) {
    uint32_t xp_reward = 0;
    uint32_t bond_reward = 0;

    if (duration_minutes >= 20) {        // 完整专注
        xp_reward = 15;  bond_reward = 2;
    } else if (duration_minutes >= 10) {  // 中等专注
        xp_reward = 5;   bond_reward = 1;
    } else {                               // 短专注
        xp_reward = 3;   bond_reward = 1;
    }

    // 增加经验、陪伴、情绪
    g_state.xp += xp_reward;
    g_state.bond = min(kMaxBond, g_state.bond + bond_reward);
    g_state.emotion = min(kMaxEmotion, g_state.emotion + 5);

    // 标记需要同步
    dog_sync_mark_dirty();
}
```

### 实时状态联动

```cpp
void dog_model_tick(void) {
    // ... 情感/陪伴衰减计算 ...

    // 监控番茄钟状态
    PomodoroSnapshot pomo = pomodoro_get();
    bool should_focus = (pomo.phase == PomodoroPhase::Focus && pomo.running);

    if (should_focus) {
        if (g_state.status != DOG_STATUS_FOCUS) {
            g_state.status = DOG_STATUS_FOCUS;
            dog_sync_mark_dirty();  // 进入专注模式
        }
    } else {
        if (g_state.status != DOG_STATUS_IDLE) {
            g_state.status = DOG_STATUS_IDLE;  // 恢复空闲
        }
    }
}
```

### 奖励公式

```
番茄钟专注 ≥20分钟 → +15 XP, +2 bond, +5 emotion
番茄钟专注 ≥10分钟 →  +5 XP, +1 bond, +5 emotion
番茄钟专注 <10分钟 →  +3 XP, +1 bond, +5 emotion
```

### 双向激励

- 用番茄钟 → 像素狗成长 → 更有动力用番茄钟
- 番茄钟运行中 → 像素狗专注陪伴（FOCUS状态）
- 形成正向循环，提高用户粘性

## 资源文件

### SD卡资源

```
sdcard_assets/pixel_dog/
├── idle_0.png        # 待机第1帧
├── idle_1.png
├── ...
├── focus_0.png       # 专注模式第1帧  ← 新增
├── focus_1.png
├── focus_2.png
└── ...
```

### 嵌入资源

核心资源编译进 Flash，确保无 SD 卡也能显示基础版本。

## 未来扩展

- [x] 物品系统（基础装备位掩码）
- [x] 多级亲密度对话系统
- [x] 互动即时同步（蹭蹭、抱抱）
- [ ] 迷你游戏（接飞盘等）
- [ ] 成就系统
- [ ] 多个宠物可选
- [ ] 与笔记/待办联动
- [ ] 天气系统（影响狗的状态）
- [ ] 社交功能（好友的狗狗互访）
