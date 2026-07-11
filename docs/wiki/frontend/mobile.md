# 移动端架构

## 概述

移动端采用 Vue 3 + TypeScript 构建，使用手绘涂鸦（Doodle）风格设计，适配手机端操作。
与 PC 端共享 API 层、状态管理、工具函数等核心逻辑，仅页面和部分组件独立。

## 入口

### 入口文件

| 文件 | 说明 |
|------|------|
| `mobile.html` | HTML 模板 |
| `src/main_mobile.ts` | 主入口脚本 |
| `src/App_mobile.vue` | 根组件 |

### 启动命令

```bash
# 移动端开发模式
npm run dev:mobile
```

访问地址：`http://127.0.0.1:5173/mobile.html`

## 目录结构

```
src/mobile/
├── components/              # 移动端通用组件
│   ├── MobilePageHeader.vue       # 页面头部
│   ├── MobileDoodleChip.vue       # 手绘胶囊标签
│   ├── MobileDoodleSearch.vue     # 手绘搜索框
│   ├── MobileSectionHeader.vue    # 分区标题
│   ├── MobileCategoryTabs.vue     # 分类标签页
│   ├── MobileBottomSheet.vue      # 底部弹出面板
│   ├── MobileCardGrid.vue         # 卡片网格
│   └── MobileTodoHeaderIcon.vue   # 待办页头图标
│
├── layouts/                 # 移动端布局
│   └── MobileLayout.vue           # 带底部导航的主布局
│
├── views/                   # 页面视图
│   ├── home/                # 首页
│   │   ├── MobileHomeView.vue
│   │   ├── components/
│   │   │   ├── HomeModulesGrid.vue
│   │   │   ├── HomeOverview.vue
│   │   │   ├── HomeSearch.vue
│   │   │   └── HomeTodos.vue
│   │   ├── themes/
│   │   │   ├── scheme-a/   # Scheme A 手绘主题
│   │   │   ├── ThemeA.vue
│   │   │   ├── ThemeB.vue
│   │   │   ├── ThemeC.vue
│   │   │   └── ThemeD.vue
│   │   └── useMobileHome.ts
│   │
│   ├── ecommerce/           # 电商模块页
│   │   ├── MobileEcommerceView.vue
│   │   ├── MobileEcommerceModuleView.vue
│   │   ├── components/
│   │   └── useMobileEcommerce.ts
│   │
│   ├── notebook/            # 笔记本页
│   │   ├── MobileNotebookView.vue
│   │   ├── MobileNotebookFolderView.vue
│   │   ├── MobileNoteDetailView.vue
│   │   ├── MobileNoteSearchView.vue
│   │   └── components/
│   │
│   ├── carton/              # 纸箱页
│   ├── express/             # 快递页
│   ├── factory/             # 工厂页
│   ├── products/            # 商品页
│   ├── shop/                # 店铺页
│   ├── inventory/           # 库存页
│   ├── order/               # 订单页
│   ├── monthly-settlement/  # 月度结算页
│   ├── pomodoro/            # 番茄钟页
│   ├── todos/               # 待办页
│   ├── functions/           # 功能列表页
│   ├── more/                # 更多页
│   ├── settings/            # 设置页
│   └── users/               # 用户页
│
├── utils/                   # 移动端工具函数
│   ├── doodleSeed.ts        # 手绘随机种子工具
│   ├── headerDate.ts        # 页头日期格式化
│   ├── homeBackGuard.ts     # 首页返回守卫
│   ├── inputViewport.ts     # 输入框视口适配
│   └── noteTree.ts          # 笔记树工具
│
└── styles/                  # 移动端全局样式
    └── mobile.scss
```

## 底部导航（Tab Bar）

```
┌─────────────────────────────────┐
│          页面内容区               │
│                                 │
├─────────────────────────────────┤
│  🏠    📓    ✅    ⋯more        │
│ 首页   笔记   待办   更多         │
└─────────────────────────────────┘
```

| Tab | 路由 | 图标 | 说明 |
|-----|------|------|------|
| 首页 | `/m/home` | 🏠 | 功能入口 + 概览 |
| 笔记 | `/m/notebook` | 📓 | 笔记本/笔记列表 |
| 待办 | `/m/todos` | ✅ | 待办事项列表 |
| 更多 | `/m/more` | ⋯ | 全部功能列表 |

## 首页架构

### 多主题方案

首页支持 4 种主题方案，可在设置中切换：

| 主题 | 风格 | 特点 |
|------|------|------|
| **Scheme A** | 手绘涂鸦 | 手绘边框 + 不规则形状 + 可爱吉祥物 |
| **Scheme B** | 卡片式 | 简洁卡片 + 网格布局 |
| **Scheme C** | 中心枢纽 | 圆形导航 + 放射状布局 |
| **Scheme D** | 时间线 | 纵向时间线 + 日程式布局 |

### Scheme A 手绘主题（默认）

Scheme A 是最具特色的主题，采用手绘涂鸦风格：

#### 核心组件

| 组件 | 说明 |
|------|------|
| `SchemeADoodleFrame` | 手绘边框容器（核心组件） |
| `SchemeAHeaderStack` | 页头堆叠装饰 |
| `SchemeAModules` | 模块图标网格 |
| `SchemeAOverview` | 概览统计卡片 |
| `SchemeASearch` | 手绘搜索框 |
| `SchemeATodos` | 待办事项展示 |

#### 视觉元素

- **手绘边框**：不规则 SVG 路径，模拟手绘效果
- **装饰元素**：星星、回形针、波浪线等小装饰
- **吉祥物**：可爱的小熊形象
- **配色**：蓝色主调 (#2563eb) + 红色点缀 (#991b1b)
- **字体**：站酷快乐体（ZCOOL KuaiLe）

#### 手绘边框组件（SchemeADoodleFrame）

```
┌─ ~ ~ ~ ─────────┐
│  ⭐             │
│                  │
│   内容区域       │
│                  │
│             📎   │
└── ~ ─── ~ ──────┘
```

**Props**：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `shape` | `'pill' \| 'rect' \| 'round'` | `'rect'` | 边框形状 |
| `color` | `string` | `'#cbd5e1'` | 边框颜色 |
| `seed` | `number` | `0` | 随机种子（控制不规则程度） |
| `strokeWidth` | `number` | `2.5` | 线宽 |
| `shadow` | `boolean` | `true` | 是否有阴影 |
| `sketch` | `boolean` | `false` | 是否素描风格 |

## 移动端通用组件

### MobilePageHeader

页面头部组件，包含返回按钮和标题。

**功能**：
- 可配置的返回按钮
- 左侧/右侧插槽
- 安全区域适配（顶部刘海屏）
- 手绘风格返回按钮

**Props**:
- `title` - 标题文字
- `showBack` - 是否显示返回按钮（默认 true）
- `backIcon` - 返回图标（默认 ←）

### MobileDoodleChip

手绘风格胶囊标签，基于 SchemeADoodleFrame 封装。

**用途**：
- 标签/Tag
- 按钮
- 分类切换

**Props**:
- `shape` - 形状 pill/rect/round
- `color` - 边框颜色
- `inline` - 是否行内（默认 true）
- `filled` - 是否实心填充
- `fillColor` - 填充颜色

### MobileDoodleSearch

手绘风格搜索框。

**功能**：
- 不规则手绘边框
- 搜索图标
- 清除按钮

### MobileCategoryTabs

分类标签页组件，支持横向滚动。

**特点**：
- 手绘风格的选中/未选中状态
- 横向滚动，右侧完全可见
- 选中：深蓝色边框 + 深红色文字
- 未选中：灰色边框

### MobileBottomSheet

底部弹出面板。

**功能**：
- 从底部滑入
- 拖拽关闭
- 半透明遮罩
- 圆角顶部

## 路由设计

### 路由结构

```
/m/                      # 移动端根路径
├── /home                # 首页
├── /notebook            # 笔记本首页
│   ├── /folder/:id      # 笔记本文件夹
│   ├── /note/:id        # 笔记详情
│   └── /search          # 笔记搜索
├── /todos               # 待办事项
├── /ecommerce           # 电商首页
│   ├── /products        # 商品
│   ├── /cartons         # 纸箱
│   ├── /factories       # 工厂
│   ├── /express         # 快递
│   ├── /shops           # 店铺
│   ├── /inventory       # 库存
│   ├── /orders          # 订单
│   └── /settlement      # 月度结算
├── /pomodoro            # 番茄钟
├── /functions           # 功能列表
├── /more                # 更多
├── /settings            # 设置
└── /users               # 用户中心
```

### 路由特点

- **扁平结构**：大部分页面是一级路由，减少层级
- **命名规范**：路径使用小写字母，多单词用连字符
- **滚动行为**：进入新页面时滚动到顶部

## 状态持久化

移动端设置保存在 `localStorage`：

| Key | 说明 |
|-----|------|
| `mobile-home-theme` | 首页主题（scheme-a/b/c/d） |
| `admin-theme` | 深浅色主题 |
| `admin-locale` | 语言设置 |

## PWA 支持

移动端支持 PWA（渐进式 Web 应用）：
- 可添加到手机桌面
- 离线缓存核心资源
- 类原生应用启动体验

配置文件：`src/mobile/pwa.ts`

## 响应式适配

### 视口设置

```html
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
```

### 安全区域

使用 CSS `env(safe-area-inset-*)` 适配刘海屏和底部横条：
- 顶部：`padding-top: max(16px, env(safe-area-inset-top))`
- 底部：`padding-bottom: max(16px, env(safe-area-inset-bottom))`

### 输入框适配

`utils/inputViewport.ts` 提供输入框弹出时的视口调整工具，
防止软键盘遮挡输入框。

## 手绘风格设计规范

### 边框规范

| 场景 | 形状 | 颜色 | 线宽 |
|------|------|------|------|
| 选中标签 | pill | #2563eb | 2.5px |
| 未选中标签 | pill | #cbd5e1 | 2.5px |
| 卡片容器 | round | #2563eb | 3px |
| 按钮 | pill | #2563eb | 2.5px |

### 配色方案

| 用途 | 颜色 | 色值 |
|------|------|------|
| 主色调 | 蓝色 | `#2563eb` |
| 强调色 | 深红 | `#991b1b` |
| 边框灰 | 浅灰 | `#cbd5e1` |
| 文字主色 | 深灰 | `#1e293b` |
| 背景色 | 白色 | `#ffffff` |
| 装饰黄 | 黄色 | `#eab308` |

### 字体规范

- **标题字体**：ZCOOL KuaiLe（站酷快乐体）
- **正文字体**：系统默认 sans-serif
- **标题字号**：24px
- **正文字号**：14-16px

## 各业务模块页面

### 电商模块

**入口页**：`MobileEcommerceView.vue`
- 海报式头部
- 功能模块网格（商品/订单/库存/纸箱/快递等）
- 数据概览面板

**子页面**：
- 商品管理：`MobileProductsView.vue`
- 纸箱管理：`MobileCartonView.vue`
- 工厂管理：`MobileFactoryView.vue`
- 快递管理：`MobileExpressView.vue`
- 店铺管理：`MobileShopView.vue`
- 库存管理：`MobileInventoryView.vue`
- 订单管理：`MobileOrderView.vue`
- 月度结算：`MobileMonthlySettlementView.vue`

### 笔记本模块

**页面结构**：
- 笔记本列表 → 文件夹 → 笔记详情
- 支持侧边抽屉式导航
- 支持搜索功能

**核心组件**：
- `NoteTree.vue` - 笔记树
- `NoteCard.vue` - 笔记卡片
- `DrawerTree.vue` - 抽屉式树
- `SwipeableCard.vue` - 可滑动卡片

### 番茄钟模块

**页面**：`MobilePomodoroView.vue`

**功能**：
- 专注/休息计时
- 计划切换
- 今日进度
- 与 ESP32 副屏同步

### 待办模块

**页面**：`MobileTodosView.vue`

**功能**：
- 待办列表
- 添加/编辑/删除
- 标记完成
- 重复待办
- 提醒设置

## 与 PC 端共享内容

| 类别 | 共享内容 |
|------|---------|
| API层 | 所有 API 接口函数 |
| 状态管理 | Pinia stores |
| 工具函数 | 日期、金额、格式化等 |
| 常量定义 | 业务常量 |
| 国际化 | 多语言包 |
| 类型定义 | TypeScript 类型 |

| 类别 | 独立内容 |
|------|---------|
| 页面 | 所有页面组件 |
| 布局 | 移动端布局独立 |
| 组件 | 移动端专属组件 |
| 路由 | 移动端路由独立 |
| 样式 | 移动端样式独立 |
