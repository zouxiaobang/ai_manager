# admin-web

Vue 3.5 + Vite 6 + TypeScript(strict) + Element Plus 2.9 + Pinia + vue-router 4 + vue-i18n 10。

## 命令

```powershell
cd admin-web
npm install
npm run dev          # http://127.0.0.1:5173
npm run dev:pc       # 打开 /index_pc.html
npm run dev:mobile   # 打开 /mobile.html
npm run build        # vue-tsc -b && vite build（含类型检查，改代码后必跑）
npm run build:pi     # 仅 vite build（Pi 部署，跳过类型检查）
```

## 端口与代理（vite.config.ts）

- 端口 **5173**（`strictPort`），`host: true`（局域网可通过本机 IP 访问）
- 三个 HTML 入口：`index.html`（主）、`index_pc.html`（PC 版）、`mobile.html`（移动版）
- 代理：
  - `/api`、`/uploads` → `http://127.0.0.1:8080`
  - `/claude-relay` → `http://127.0.0.1:3001`（WebSocket）
  - 代理目标可用环境变量 `vite_api_target` 覆盖（默认 8080），用于 worktree 独立端口联调

## 开发约定

- 路径别名 `@/` → `./src/*`；TypeScript strict，`noUnusedLocals`/`noUnusedParameters` 开启
- **worktree 内验证前端必须单独起 vite**（主 checkout 的 vite 看不到 worktree 改动）：
  `.\dev.ps1 N frontend` → 端口 517N，`vite_api_target` 自动指向 808N
- 构建体积相关：vite.config 有手写 `manualChunks`（xterm/echarts/exceljs/three/wangeditor 等独立分包），新加大依赖时注意别破坏
