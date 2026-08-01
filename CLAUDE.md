# ai_manager

全栈智能管理系统：Spring Boot 后端 + Vue 3 前端 + claude-relay 中继 + ESP32 子屏固件，部署目标树莓派 `192.168.0.114`。

## 仓库地图

| 目录 | 说明 |
|---|---|
| `admin-backend/` | Spring Boot 3.3.7 / Java 17 后端，端口 **8080** |
| `admin-web/` | Vue 3.5 + Vite 6 前端，端口 **5173** |
| `claude-relay/` | Claude 中继服务，端口 **3001** |
| `deploy/` | 部署配置（docker / nginx / systemd），目标 Pi 192.168.0.114 |
| `firmware/` | ESP32-S3 子屏固件 |
| `admin-backend/sql/` | 数据库脚本（约 70+ 文件；新库执行 `sql/deploy-all.sql`） |

## 后端 admin-backend

- Maven 四模块依赖链：`admin-common → admin-framework → admin-system → admin-server`
- 启动（**两步，禁止带 `-am` 的 spring-boot:run**）：

  ```powershell
  cd admin-backend
  mvn clean install -DskipTests          # 编译并安装到本地 .m2
  mvn -pl admin-server spring-boot:run   # 单独启动 admin-server
  ```

  ⚠️ 不要执行 `mvn -pl admin-server -am spring-boot:run`（单条）——`-am` 会把 spring-boot:run 绑到父 POM，无 main 类而失败。
- 一键脚本：`admin-backend/run.ps1`（内部即上面的两步）；杀端口：`admin-backend/kill-port.ps1 -Port 8080`
- 健康检查：`GET http://localhost:8080/api/health`
- 依赖：MySQL 8（127.0.0.1:3306，库 `ai_manager_admin`）、Redis 6+（127.0.0.1:6379）、pgvector（192.168.0.118:5432，RAG）
- 详细模块说明见 `admin-backend/README.md`

## 前端 admin-web

- 技术栈：Vue 3.5 / Vite 6 / TypeScript(strict) / Element Plus 2.9 / Pinia
- 命令：

  ```powershell
  cd admin-web
  npm install
  npm run dev          # http://127.0.0.1:5173
  npm run dev:pc       # 打开 /index_pc.html
  npm run dev:mobile   # 打开 /mobile.html
  npm run build        # vue-tsc -b && vite build（含类型检查）
  npm run build:pi     # 仅 vite build（Pi 部署用，跳过类型检查）
  ```

- 端口 **5173**（strictPort），代理：`/api`、`/uploads` → 127.0.0.1:8080；`/claude-relay` → 127.0.0.1:3001(WS)
- 代理目标支持环境变量 `VITE_API_TARGET` 覆盖（见 vite.config.ts），用于 worktree 独立端口联调

## 开发协议（Git Worktree 主力）

- **每任务一个 worktree + 独立 Claude 会话**，并行开发互不干扰：

  ```powershell
  claude --worktree <name>   # 如 feat-notebook-search / fix-rag-pgvector
  ```

- worktree 落在 `.claude/worktrees/<name>/`（分支 `worktree-<name>`），已 gitignore
- **端口约定**：主 checkout 占 8080/5173；worktree 任务 N 用 **808N/517N**（`dev.ps1` 辅助启动）
- 本地环境变量 `.env` / `.env.local` 通过根目录 `.worktreeinclude` 带入 worktree
- 完成：合并回 master → 退出 Claude 会话时自动清理 worktree
- 一键 4 窗格布局：仓库根 `launch-dev.ps1 -TaskA feat-x -TaskB fix-y`

## 约定

- 改完代码优先模块级编译/单测：`mvn -pl admin-system -am test`
- 前端改动在对应 worktree 内单独起 vite 验证（主 checkout 的 vite 看不到 worktree 改动）
- 生产部署：profile `prod`（`SPRING_PROFILES_ACTIVE`），见 `deploy/`，目标 Pi 192.168.0.114
