# 前端架构总览

## 项目简介

AI Manager 管理后台前端基于 **Vue 3 + TypeScript + Vite** 构建，使用 Element Plus 组件库，支持 PC 端管理界面和移动端 H5 界面双端适配。

## 技术栈

| 类别 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 框架 | Vue | 3.5 | 组合式API |
| 语言 | TypeScript | 5.6 | 类型安全 |
| 构建工具 | Vite | 6.0 | 极速构建 |
| UI组件库 | Element Plus | 2.9 | PC端组件 |
| 状态管理 | Pinia | 2.3 | Vue官方推荐 |
| 路由 | Vue Router | 4.5 | 前端路由 |
| 国际化 | Vue I18n | 10.0 | 多语言支持 |
| HTTP客户端 | Axios | 1.7 | API请求 |
| 图表 | ECharts | 6.1 | 数据可视化 |
| 3D渲染 | Three.js | 0.185 | 纸箱3D预览 |
| Excel | ExcelJS / XLSX | — | Excel导入导出 |
| 富文本 | WangEditor | 5.1 | 笔记编辑器 |
| PWA | vite-plugin-pwa | 1.3 | 离线应用 |
| CSS预处理器 | Sass | 1.83 | 样式编写 |

## 双入口架构

项目采用 **PC端 + 移动端** 双入口设计，共享组件和工具库。

### 入口文件

| 入口 | HTML | 主入口 | App组件 | 说明 |
|------|------|--------|---------|------|
| PC端 | `index_pc.html` | `main_pc.ts` | `App_pc.vue` | 桌面管理后台 |
| 移动端 | `mobile.html` | `main_mobile.ts` | `App_mobile.vue` | 手机H5页面 |

### 启动命令

```bash
# PC端开发
npm run dev:pc

# 移动端开发
npm run dev:mobile

# 生产构建
npm run build
```

## 目录结构

```
admin-web/
├── public/                    # 静态资源（不经过构建）
│   ├── icons/                 # SVG图标
│   ├── mobile-home/           # 移动端首页资源
│   └── pwa/                   # PWA图标
│
├── src/
│   ├── api/                   # API接口层
│   │   ├── request.ts         # Axios封装
│   │   ├── types.ts           # 通用类型
│   │   ├── ecommerce/         # 电商API
│   │   ├── notebook/          # 笔记本API
│   │   ├── sys/               # 系统API
│   │   └── ...                # 其他模块API
│   │
│   ├── assets/                # 资源文件（经过构建）
│   │   └── ecommerce/         # 电商资源
│   │
│   ├── components/            # 通用组件
│   │   ├── deploy/            # 部署中心组件
│   │   ├── ecommerce/         # 电商组件
│   │   ├── storage/           # 存储组件
│   │   ├── war-room/          # 指挥室风格组件
│   │   └── ...                # 其他通用组件
│   │
│   ├── composables/           # 组合式函数
│   │   ├── usePagination.ts   # 分页Hook
│   │   ├── useSystemHealth.ts # 系统健康检查
│   │   └── ...                # 其他
│   │
│   ├── constants/             # 常量定义
│   │
│   ├── data/                  # 静态数据
│   │
│   ├── i18n/                  # 国际化
│   │   ├── index.ts           # i18n实例
│   │   └── locales/           # 语言包
│   │       ├── zh-CN.ts
│   │       └── en-US.ts
│   │
│   ├── layouts/               # 布局组件
│   │   ├── AdminLayout.vue    # PC端管理布局
│   │   └── EcommerceLayout.vue # 电商布局
│   │
│   ├── mobile/                # 移动端专属
│   │   ├── components/        # 移动端组件
│   │   ├── layouts/           # 移动端布局
│   │   ├── views/             # 移动端页面
│   │   │   ├── home/          # 首页
│   │   │   ├── ecommerce/     # 电商
│   │   │   ├── notebook/      # 笔记本
│   │   │   ├── pomodoro/      # 番茄钟
│   │   │   ├── carton/        # 纸箱
│   │   │   ├── express/       # 快递
│   │   │   ├── factory/       # 工厂
│   │   │   ├── products/      # 商品
│   │   │   ├── shop/          # 店铺
│   │   │   ├── todos/         # 待办
│   │   │   └── ...            # 其他页面
│   │   ├── utils/             # 移动端工具
│   │   └── styles/            # 移动端样式
│   │
│   ├── plugins/               # Vue插件
│   │
│   ├── router/                # 路由配置
│   │   ├── index.ts           # 总入口
│   │   ├── pc/                # PC端路由
│   │   └── mobile/            # 移动端路由
│   │
│   ├── stores/                # Pinia状态
│   │   ├── app.ts             # 应用状态
│   │   └── ecSettings.ts      # 电商设置
│   │
│   ├── styles/                # 全局样式
│   │   ├── index.scss
│   │   ├── fonts.scss
│   │   └── ...
│   │
│   ├── utils/                 # 工具函数
│   │   ├── date.ts
│   │   ├── formatMoney.ts
│   │   ├── echarts.ts
│   │   └── ...
│   │
│   ├── views/                 # PC端页面
│   │
│   ├── App_pc.vue             # PC端根组件
│   ├── App_mobile.vue         # 移动端根组件
│   ├── main_pc.ts             # PC端入口
│   ├── main_mobile.ts         # 移动端入口
│   └── bootstrap.ts           # 引导脚本
│
├── .env.development           # 开发环境变量
├── .env.production            # 生产环境变量
├── package.json
└── vite.config.ts
```

## 核心架构特性

### 1. 双端共享机制

PC端和移动端共享以下资源：
- API接口层（`src/api/`）
- 状态管理（`src/stores/`）
- 工具函数（`src/utils/`）
- 常量定义（`src/constants/`）
- 国际化（`src/i18n/`）
- 部分通用组件

各端独立：
- 布局组件
- 页面组件
- 路由配置
- 部分专属组件

### 2. 移动端手绘风格

移动端采用手绘涂鸦（Doodle）风格设计：
- 手绘边框组件（MobileDoodleChip, SchemeADoodleFrame）
- 不规则形状和装饰元素
- 温暖的配色方案
- 可爱的吉祥物形象

### 3. PWA支持

移动端支持PWA：
- 可添加到手机桌面
- 离线缓存核心资源
- 类原生应用体验

### 4. 多主题支持

- 浅色/深色主题切换
- 通过 `html.dark` 类名控制
- Element Plus 暗色变量集成
- 主题设置保存在 localStorage

### 5. 多语言支持

- 中文 / English 双语
- Vue I18n 管理
- 语言设置保存在 localStorage

## 状态管理

### app store

**位置**：`src/stores/app.ts`

应用全局状态。

**核心状态**：
- 主题模式（light/dark）
- 语言（zh-CN/en-US）
- 侧边栏状态
- 用户信息

### ecSettings store

**位置**：`src/stores/ecSettings.ts`

电商设置状态，管理电商模块的系统配置。

## API请求封装

### request.ts

**位置**：`src/api/request.ts`

基于 Axios 的统一请求封装。

**特性**：
- 统一解析 `ApiResult<T>` 格式
- 自动错误提示（Element Plus Message）
- 支持静默错误（`X-Silent-Error` 头）
- 超时配置（默认15秒）
- 提供 `getData` / `postData` / `putData` / `deleteData` 便捷方法

**使用示例**：
```typescript
import { getData, postData } from '@/api/request'

// GET请求
const data = await getData<User>('/api/system/users', { page: 1 })

// POST请求
const result = await postData<void>('/api/notes', noteData)
```

## 路由系统

### PC端路由

使用嵌套路由，基于 AdminLayout 布局：
- 首页仪表盘
- 功能列表（卡片式导航）
- 电商管理（多级菜单）
- 笔记本
- 番茄钟
- 部署中心
- 存储中心
- 用户中心
- 权限中心
- 全局设置

### 移动端路由

扁平式路由，基于 MobileLayout 底部导航：
- 首页（功能入口）
- 笔记本
- 待办
- 更多（功能列表）
- 各业务模块页面

## 核心组件库

### 通用业务组件

| 组件 | 位置 | 说明 |
|------|------|------|
| `TablePagination` | `components/` | 表格分页组件 |
| `CnyAmount` | `components/` | 人民币金额显示 |
| `AutoFitCnyAmount` | `components/` | 自适应金额 |
| `ImportMappingDialog` | `components/` | 导入字段映射对话框 |
| `ImportStatusMappingEditor` | `components/` | 导入状态映射编辑器 |

### 电商组件

| 组件 | 位置 | 说明 |
|------|------|------|
| `CartonBox3DPreview` | `components/ecommerce/` | 纸箱3D预览 |
| `CartonBoxIllustration` | `components/ecommerce/` | 纸箱插画 |
| `CartonStylePicker` | `components/ecommerce/` | 纸箱样式选择器 |
| `EcImageField` | `components/ecommerce/` | 电商图片字段 |
| `ExpressStationAvatar` | `components/ecommerce/` | 快递站点头像 |
| `InventoryHealthChart` | `components/ecommerce/` | 库存健康图表 |
| `SkuDetailCardDialog` | `components/ecommerce/` | SKU详情卡片 |
| `MonthStepper` | `components/ecommerce/` | 月份步进器 |

### 移动端组件

| 组件 | 位置 | 说明 |
|------|------|------|
| `MobilePageHeader` | `mobile/components/` | 移动端页头 |
| `MobileDoodleChip` | `mobile/components/` | 手绘风格标签 |
| `MobileDoodleSearch` | `mobile/components/` | 手绘搜索框 |
| `MobileSectionHeader` | `mobile/components/` | 分区标题 |
| `MobileCategoryTabs` | `mobile/components/` | 分类标签页 |
| `MobileBottomSheet` | `mobile/components/` | 底部弹出面板 |
| `MobileCardGrid` | `mobile/components/` | 卡片网格 |
| `MobileTodoHeaderIcon` | `mobile/components/` | 待办页头图标 |

## 工具函数

### 常用工具

| 文件 | 说明 |
|------|------|
| `utils/date.ts` | 日期格式化 |
| `utils/formatMoney.ts` | 金额格式化 |
| `utils/echarts.ts` | ECharts工具 |
| `utils/cartonMatch.ts` | 纸箱匹配算法 |
| `utils/cartonPreviewImage.ts` | 纸箱预览图生成 |
| `utils/inventoryStats.ts` | 库存统计 |
| `utils/expressVisual.ts` | 快递可视化 |
| `utils/platformVisual.ts` | 平台可视化 |
| `utils/pomodoroSession.ts` | 番茄钟会话 |
| `utils/deployChecklistRunner.ts` | 部署检查清单 |

### 组合式函数（Composables）

| 文件 | 说明 |
|------|------|
| `composables/usePagination.ts` | 分页逻辑复用 |
| `composables/useSystemHealth.ts` | 系统健康检查 |
| `composables/useTodoReminders.ts` | 待办提醒 |
| `composables/useBaiduPanAutoAuth.ts` | 百度网盘自动授权 |
| `composables/useMobileEcDoodle.ts` | 移动电商手绘风格 |

## 构建配置

### 环境变量

| 变量 | 开发环境 | 生产环境 | 说明 |
|------|---------|---------|------|
| `VITE_API_BASE` | 空（代理到8080） | 空或API地址 | API基础路径 |

### Vite代理

开发环境下代理配置：
- `/api` → `http://127.0.0.1:8080`
- `/uploads` → `http://127.0.0.1:8080`

## 移动端页面架构

### 底部导航（Tab Bar）

```
首页 ── 笔记本 ── 待办 ── 更多
```

### 首页设计

多主题方案（Scheme A/B/C/D）：
- Scheme A：手绘涂鸦风格 + 模块卡片
- Scheme B：卡片式布局
- Scheme C：中心枢纽式
- Scheme D：时间线式

### 手绘涂鸦风格

核心视觉元素：
- 不规则手绘边框
- 装饰线条和图案
- 温暖的配色（蓝色主调 + 红色点缀）
- 吉祥物小熊形象
- 便签、回形针等文具装饰
