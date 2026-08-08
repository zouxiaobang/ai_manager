# ai_manager

全栈智能管理系统：Spring Boot 后端 + Vue 3 前端 + Codex-relay 中继 + ESP32 子屏固件，部署目标树莓派 `192.168.0.114`。

## 仓库地图

| 目录 | 说明 |
|---|---|
| `admin-backend/` | Spring Boot 3.3.7 / Java 17 后端，端口 **8080** |
| `admin-web/` | Vue 3.5 + Vite 6 前端，端口 **5173** |
| `Codex-relay/` | Codex 中继服务，端口 **3001** |
| `deploy/` | 部署配置（docker / nginx / systemd），目标 Pi 192.168.0.114 |
| `firmware/` | ESP32-S3 子屏固件 |
| `admin-backend/sql/` | 数据库脚本（约 70+ 文件；新库执行 `sql/deploy-all.sql`。RAG 表未含在 deploy-all：另执行 `ai_knowledge_config.sql`+`rag_knowledge_base.sql`，PG 库执行 `rag_pgvector.sql`；本地 dev 独立向量库建库见 `rag_pgvector_dev.sql`） |

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
- 依赖：MySQL 8（127.0.0.1:3306，库 `ai_manager_admin`）、Redis 6+（127.0.0.1:6379）、pgvector（192.168.0.118:5432，RAG。**本地 dev 用独立向量库 `ai_manager_rag_dev`，生产用 `ai_manager_rag`**，见 application-dev.yml）
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

- 端口 **5173**（strictPort），代理：`/api`、`/uploads` → 127.0.0.1:8080；`/Codex-relay` → 127.0.0.1:3001(WS)
- 代理目标支持环境变量 `vite_api_target` 覆盖（见 vite.config.ts），用于 worktree 独立端口联调

## 开发协议（Git Worktree 主力）

- **每任务一个 worktree + 独立 Codex 会话**，并行开发互不干扰：

  ```powershell
  Codex --worktree <name>   # 如 feat-notebook-search / fix-rag-pgvector
  ```

- worktree 落在 `.Codex/worktrees/<name>/`（分支 `worktree-<name>`），已 gitignore
- **端口约定**：主 checkout 占 8080/5173；worktree 任务 N 用 **808N/517N**（`dev.ps1 1 backend` 起任务 1 后端 8081，`dev.ps1 1 frontend` 起前端 5174）
- 本地环境变量 `.env` / `.env.local` 通过根目录 `.worktreeinclude` 带入 worktree
- 提交：`.\commit.ps1 "feat: xxx"`；完成后合并回 master，清理用 `.\clean.ps1`
- 一键 4 窗格布局：任意仓库根 `panes feat-x fix-y`（全局命令，脚本 `~/scripts/panes.ps1`，各项目 `.Codex/panes.json` 配置每格命令）

## 约定

- 改完代码优先模块级编译/单测：`mvn -pl admin-system -am test`
- 前端改动在对应 worktree 内单独起 vite 验证（主 checkout 的 vite 看不到 worktree 改动）
- **`.ps1` 脚本必须存为 UTF-8 with BOM**（PowerShell 5.1 读无 BOM 的 UTF-8 会按 ANSI/GBK 解码，中文注释/字符串会把脚本解析坏）
- **子代理团队**（全局用户级 `~/.Codex/agents/`，中文名自然语言调起）：架构师-老杨、测试工程师-小郭、代码审查官-老周、调研员-小吴、后端开发-小林、前端开发-小美
- 生产部署：profile `prod`（`SPRING_PROFILES_ACTIVE`），见 `deploy/`，目标 Pi 192.168.0.114
