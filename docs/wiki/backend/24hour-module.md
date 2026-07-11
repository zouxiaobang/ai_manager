# 24小时重启系统模块

## 概述

24小时重启系统是一个日常习惯追踪与时间管理模块，将一天划分为 6 个时段，每个时段包含若干检查项（Checklist Item），用户通过勾选完成各项任务。该系统帮助用户建立规律作息、专注工作与定期复盘的习惯闭环。

## 时间段划分

| 时段 | 时间段 | 检查项数 | 核心理念 |
|------|--------|---------|---------|
| 前夜铺垫 | 20:00~24:00 | 3项 | 写明日任务、整理环境、写行为暗示 |
| 晨起黄金60分钟 | 06:00~08:00 | 5项 | 卧床静息、见光喝水、拉伸、阅读、定核心任务 |
| 深度专注 | 08:00~12:00 | 3项 | 番茄钟专注、休息总结、明日提醒 |
| 中段重置 | 12:00~14:00 | 3项 | 午饭休息、三行复盘、桌面整理 |
| 下午推进 | 14:00~18:00 | 2项 | 可交付完结内容、明日提醒 |
| 晚间复盘 | 18:00~20:00 | 3项 | 工作总结、可改进处、习惯保留/删除 |

## 后端实现

### 模块结构

```
24小时模块（后端）
├── DailyChecklistController.java    # REST控制器
├── DailyChecklistService.java       # 服务接口
├── DailyChecklistServiceImpl.java   # 服务实现
├── DailyChecklistMapper.java        # MyBatis-Plus Mapper
├── domain/
│   ├── entity/DailyChecklist.java   # 实体类
│   ├── vo/DailyChecklistVO.java     # 视图对象
│   └── dto/DailyChecklistSaveRequest.java  # 保存请求DTO
└── sql/daily_checklist.sql          # 数据库建表脚本
```

### 数据库表

**表名**：`daily_checklist`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键（自增） |
| checklist_date | DATE | 日期 |
| item_key | VARCHAR(64) | 检查项唯一标识 |
| completed | TINYINT | 是否完成（0/1） |
| content | VARCHAR(512) | 填写内容（复盘文本等） |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

**唯一索引**：`uk_date_key (checklist_date, item_key)`

### API 接口

**基础路径**：`/api/24hour`

#### 获取指定日期的检查清单

**GET /api/24hour?date=2025-07-10**

响应数据：
```json
[
  {
    "itemKey": "prep_task",
    "completed": 1,
    "content": "完成订单导入模块测试"
  },
  {
    "itemKey": "morning_rest",
    "completed": 0,
    "content": null
  }
]
```

#### 保存检查清单（新增/更新）

**POST /api/24hour**

请求体：
```json
{
  "date": "2025-07-10",
  "items": [
    { "itemKey": "prep_task", "completed": 1, "content": "完成订单导入模块测试" },
    { "itemKey": "morning_rest", "completed": 1, "content": null }
  ]
}
```

响应：`ApiResult<Void>`，无数据体。

### 核心业务流程

```
GET /api/24hour?date=YYYY-MM-DD
  → DailyChecklistServiceImpl.getByDate()
    → SELECT * FROM daily_checklist WHERE checklist_date = date
    → 映射为 DailyChecklistVO 列表返回

POST /api/24hour
  → DailyChecklistServiceImpl.saveByDate() [@Transactional]
    → 遍历 items:
      → SELECT ... WHERE checklist_date=date AND item_key=key
      → IF 存在 → UPDATE
      → ELSE → INSERT
```

采用 upsert 策略：根据 `checklist_date + item_key` 唯一索引判断，已存在则更新 `completed/content`，不存在则新增。

## 前端实现

### 模块结构

```
前端24小时模块
├── views/TwentyFourHourView.vue    # 主页面
├── data/24hour-phases.ts           # 时间段配置定义
├── api/dailyChecklist.ts           # API 请求
├── composables/use24HourNotification.ts  # 通知提醒
└── layouts/AdminLayout.vue         # 侧栏导航入口
```

### 主页面功能区域

```
TwentyFourHourView
├── 顶栏 (tfh-bar)
│   ├── 日期选择器（可切换日期查看历史）
│   └── 当天完成进度条（完成数/总数 + 百分比进度条）
├── 卡片网格 (tfh-grid)
│   ├── 前夜铺垫卡片（4列，紫色 #6366f1）
│   │   ├── 时段标识 + 环形进度
│   │   ├── 时段标题与描述
│   │   └── 3个检查项（勾选框 + 标签 + 内容预览/编辑按钮）
│   ├── 晨起黄金60分钟卡片（黄色 #f59e0b）
│   │   └── 5个检查项
│   ├── 深度专注卡片（蓝色 #3b82f6）
│   │   └── 3个检查项
│   ├── 中段重置卡片（绿色 #10b981）
│   │   └── 3个检查项
│   ├── 下午推进卡片（紫色 #8b5cf6）
│   │   └── 2个检查项
│   └── 晚间复盘卡片（红色 #ef4444）
│       └── 3个检查项
├── 底部提示 (tfh-foot)
│   ├── ⚠ 伪努力陷阱（强度透支 · 工具沉迷）
│   └── ✓ 成长路径（低摩擦 · 持续 · 复盘 · 深耕 · 不断线）
└── 弹窗
    ├── 写作弹窗（有 hasContent 的检查项点击后打开）
    │   ├── 时段标识 + 检查项名称
    │   ├── 文本输入框
    │   └── 提交/取消按钮
    └── 撤销确认弹窗（已勾选项点击撤销时弹出）
        └── 确认撤销/取消
```

### 页面交互逻辑

#### 勾选/撤销
```
点击检查项
├── 已完成（isChecked=true）
│   └── 弹出撤销确认弹窗 → 确认 → 置为未完成 → 自动保存
└── 未完成（isChecked=false）
    ├── 检查项有 hasContent=true
    │   └── 弹出写作弹窗 → 填写内容 → 提交 → 自动保存
    └── 检查项无 hasContent
        └── 直接置为已完成 → 自动保存
```

#### 自动保存

```
状态变更 → 开启 debounce 定时器（600ms）
  → 到期执行 doSave()
    → POST /api/24hour
      → 构建所有检查项数据
      → 静默保存（try/catch 捕获异常）
```

### 时间段配置（24hour-phases.ts）

```
PhasesDef 结构
├── key       - 唯一标识（如 EVENING_PREP, MORNING）
├── badge     - 时段简称（如 前夜, 晨起）
├── title     - 时段标题（如前夜前置铺垫）
├── desc      - 可选描述（如 起身后五分钟启动并隔离手机诱惑）
├── accent    - 主题色（CSS颜色值）
├── hourStart - 起始小时数
├── hourEnd   - 结束小时数
├── items     - 检查项数组
│   ├── key         - 唯一标识
│   ├── label       - 显示文本
│   └── hasContent  - 是否需要填写内容

辅助函数
├── getCurrentPhase() - 根据当前时间返回对应时段
└── getPhaseByKey(key) - 根据key查找时段
```

### 路由与导航

- 路由路径：`/24hour`
- 路由名称：`24hour`
- 侧栏导航：位于 AdminLayout 侧栏 `railItems` 中，图标 `24hour`

### 通知提醒（use24HourNotification）

```
自动提醒
├── 每60秒轮询检查当前时段
├── 时段变更时弹出通知
│   ├── 浏览器原生桌面通知（优先）
│   └── Element Plus ElNotification 降级
├── 通知内容：当前时段名 + 待完成项数
└── 打开24小时页面时消除通知

导出函数
├── use24HourNotification() - Vue组合式函数
├── dismiss24HourNotification() - 消除通知
└── markPhaseNotified(key) - 标记时段已通知
```

### 核心状态管理

```typescript
// Reactive 状态映射（前端数据源）
const checklistMap = reactive<Record<string, DailyChecklistItem>>({})

// 计算属性
const totalCount = computed(() => allItemKeys.length)      // 总项数
const completedCount = computed(() => ...)                  // 已完成数
const progressPercent = computed(() => ...)                 // 完成百分比

// 环形进度计算（SVG circle stroke-dashoffset）
function phaseOffset(phase)  // 返回各时段的环形进度偏移量
```
