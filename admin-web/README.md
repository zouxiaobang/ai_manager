# AI Manager 管理后台（前端）

Vue 3 + Vite + Element Plus + Pinia + Vue Router + Vue I18n，支持中英文与深浅色主题。

## 技术栈

- Vue 3 + TypeScript
- Element Plus（含 `dark/css-vars` 深色变量）
- Pinia 状态（主题、语言）
- Vue Router
- Axios（统一解析 `ApiResult`）

## 开发

```bash
cd admin-web
npm install
npm run dev
```

若报 `Port 5173 is already in use`，先释放端口再启动：

```powershell
.\kill-port.ps1
npm run dev
```

默认 `http://127.0.0.1:5173`（须先 `npm run dev` 保持终端运行）。API 代理到 `http://127.0.0.1:8080`。

若 `npm run dev` 报 `element-plus` 的 `.mjs.map` / `Syntax error "\x00"`，删除 `node_modules/.vite` 后重试（`vite.config.ts` 已关闭预构建 sourcemap 以避免该问题）。

## 主题与语言

- 顶栏可切换 **浅色 / 深色**（`html.dark` + Element Plus 暗色 CSS 变量）
- 顶栏可切换 **中文 / English**

设置保存在 `localStorage`。

## 界面分区

- **顶栏**：Logo、标题、搜索、通知、语言/主题、管理员头像
- **快捷导航**：财务 / CRM / OA / 资产等快捷入口（演示）
- **左侧菜单**：首页、功能列表、笔记本、用户中心、权限中心、部署文档、存储能力、全局设置
- **主内容**：首页为数据大盘；**功能列表**以卡片展示各模块；**番茄钟**（`/pomodoro`）含计划管理、浏览器计时、报表统计；**笔记本**（`/notebook`）树形文件夹 + 笔记编辑；权限中心为用户列表

### 番茄钟

需后端已执行 `admin-backend/sql/pomodoro.sql` 并启动 API。功能列表 → 番茄钟 → 三个标签：专注计时、计划管理、报表统计。

**与 ESP 副屏同步**（控制方 `controller` + 用户操作 `takeControl`）：

- 谁在副屏/本页**点击开始、暂停、重置**，谁成为控制方；另一方只跟随，不会用定时心跳抢控制权
- 副屏点击后立即上报（`takeControl=true`），本页约 1 秒内跟显；本页操作时副屏约 2 秒内跟显
- 顶栏标签：**已与副屏同步** = 副屏控制中；**本页控制中** = 浏览器控制中

ESP 需配置 `panel_config.h` 的 `ADMIN_API_HOST` 为电脑局域网 IP，并重新烧录固件；后端需重启以加载新会话逻辑。

专注计时页会每 2 秒从 `GET /api/pomodoro/session` 拉取 ESP32 直连上报的状态（写入 Redis），在设备端开始/暂停计时时与 Web 倒计时保持一致。番茄钟不依赖 `pc_daemon`。

### 笔记本

需后端已执行 `admin-backend/sql/notebook.sql` 并启动 API。左侧树形文件夹/笔记列表，右侧编辑区支持自动保存、置顶、收藏、标签与回收站。
- **底栏**：技术支持、邮箱、版本号

## 目录

```
src/
  api/          # 请求封装与接口
  i18n/         # 多语言
  layouts/      # 门户布局 AdminLayout
  router/       # 路由
  stores/       # Pinia
  views/        # 页面
  styles/       # 全局与 portal 样式
```
