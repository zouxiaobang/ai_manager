# 树莓派部署指南

本文说明如何在局域网两台树莓派上从零部署 AI Manager，以及日常代码更新时的发布流程。

---

## 1. 架构与节点

| 节点 | IP | 网卡 | 角色 |
|------|-----|------|------|
| **应用节点** | **192.168.0.114** | — | Nginx :80、前端静态资源、Spring Boot :8080 |
| **数据节点（无线）** | **192.168.0.116** | wlan0 | 与 118 同一台机器 |
| **数据节点（有线）** | **192.168.0.118** | eth0 | Docker：MySQL 8、Redis 7（**后端优先连此 IP**） |
| **开发机** | 192.168.0.119 | — | Windows，构建代码、Chat2DB、上传部署 |

```
浏览器 / 手机
    │
    ▼  http://192.168.0.114
┌─────────────────────────────────────┐
│  应用节点 192.168.0.114             │
│  Nginx :80                          │
│  ├─ /           → 前端 dist         │
│  ├─ /api/*      → 后端 :8080        │
│  ├─ /api/deploy/stream → SSE 长连接 │
│  ├─ /oauth/*    → 后端 :8080        │
│  └─ /uploads/*  → 后端 :8080        │
│  Spring Boot :8080 (profile=prod)   │
└──────────────┬──────────────────────┘
               │ 局域网（优先 118 有线）
               ▼
┌─────────────────────────────────────┐
│  数据节点 116/118（同一台树莓派）   │
│  Docker                             │
│  ├─ MySQL  :3306                    │
│  └─ Redis  :6379                    │
└─────────────────────────────────────┘
```

---

## 2. 凭据速查（局域网内网）

| 项目 | 值 |
|------|-----|
| 树莓派 SSH 用户 | `kyle` |
| 树莓派 SSH 密码 | `Asd123456` |
| MySQL root 密码 | `123456` |
| MySQL 应用用户 | `ai_manager` |
| MySQL 应用密码 | `123456` |
| MySQL 数据库名 | `ai_manager_admin` |
| 后端运行用户（systemd） | `aimanager` |

> 生产环境建议后续改为更强密码；当前为内网演示配置。

---

## 3. 访问地址与接口速查

### 3.1 页面

| 用途 | URL |
|------|-----|
| 管理后台（自动识别 PC/手机） | http://192.168.0.114/#/home |
| 部署中心 | http://192.168.0.114/#/deploy |
| 强制移动版 | http://192.168.0.114/?shell=mobile |
| 强制 PC 版 | http://192.168.0.114/?shell=pc |

> 生产环境请用 `index.html` + Hash 路由（`/#/home`），不要用 `index_pc.html`。

### 3.2 HTTP 接口（经 Nginx）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/health` | GET | 健康检查（含 MySQL / Redis 状态） |
| `/api/todos/today` | GET | 今日待办（验证前后端联通） |
| `/api/deploy/runner/status` | GET | 一键部署环境状态 |
| `/api/deploy/stream?target=backend` 或 `frontend` | GET (SSE) | 部署日志流 |
| `/oauth/baidu/callback` | GET | 百度网盘 OAuth 回调（Nginx 反代） |

**快速验证（Windows PowerShell 或树莓派上）：**

```powershell
curl.exe -s http://192.168.0.114/api/health
curl.exe -s http://192.168.0.114/api/todos/today
```

```bash
# 114 本机
curl -s http://127.0.0.1/api/health
curl -s http://127.0.0.1:8080/api/health   # 直连后端
```

### 3.3 一键健康检查脚本（Windows 开发机）

```powershell
cd G:\projects\ai_project\ai_manager
powershell -ExecutionPolicy Bypass -File deploy/scripts/health-check.ps1
```

---

## 4. 从零部署

按顺序执行：**数据节点 → 应用节点环境 → 后端 → 前端与 Nginx → 验证**。

### 4.0 前置准备

**开发机（119，Windows）需安装：**

- Git、JDK 17、Maven、Node.js 18+
- OpenSSH 客户端（`ssh` / `scp`）
- 可选：Chat2DB、mysql 客户端

**将代码弄到树莓派（任选一种）：**

```bash
# 方式 A：在树莓派上 git clone（114 本机一键部署必需）
git clone <你的仓库地址> ~/ai_manager

# 方式 B：从 Windows 同步整个仓库
scp -r G:\projects\ai_project\ai_manager kyle@192.168.0.118:~/ai_manager
scp -r G:\projects\ai_project\ai_manager kyle@192.168.0.114:~/ai_manager
```

---

### 4.1 数据节点（116/118）— Docker MySQL + Redis

在 **数据节点树莓派**（116 或 118 均可 SSH）上执行。

#### 4.1.1 卸载原生 MariaDB / Redis（若曾用 apt 安装过）

```bash
cd ~/ai_manager
# 有数据先备份
mariadb-dump -u root -p ai_manager_admin > ~/backup_$(date +%F).sql 2>/dev/null || true

sudo bash deploy/scripts/uninstall-native-db-on-116.sh
# 确认无重要数据后可清目录：
# sudo bash deploy/scripts/uninstall-native-db-on-116.sh --purge-data
```

#### 4.1.2 安装 Docker 并启动

```bash
cd ~/ai_manager
bash deploy/scripts/setup-data-node-docker.sh
```

首次会生成 `/opt/ai-manager/data-node/.env`，编辑密码后**再执行一次**启动脚本：

```bash
nano /opt/ai-manager/data-node/.env
```

`.env` 内容（按当前环境）：

```env
MYSQL_ROOT_PASSWORD=123456
MYSQL_DATABASE=ai_manager_admin
MYSQL_USER=ai_manager
MYSQL_PASSWORD=123456
```

```bash
bash deploy/scripts/setup-data-node-docker.sh
```

#### 4.1.3 验证 Docker 服务

```bash
cd /opt/ai-manager/data-node
docker compose ps
docker exec ai-manager-mysql mysql -u ai_manager -p123456 ai_manager_admin -e "SELECT 1;"
docker exec ai-manager-redis redis-cli ping
```

#### 4.1.4 导入数据库（全量 SQL，新环境一次执行）

```bash
cd ~/ai_manager
sudo docker exec -i ai-manager-mysql mysql -uroot -p123456 < admin-backend/sql/deploy-all.sql
```

或（推荐，自动读取 `.env` 密码）：

```bash
bash deploy/scripts/import-sql-to-docker-mysql.sh admin-backend/sql/deploy-all.sql
```

#### 4.1.5 从应用节点测试连通（114 上执行）

```bash
mysql -h 192.168.0.118 -u ai_manager -p123456 ai_manager_admin -e "SELECT 1;"
redis-cli -h 192.168.0.118 ping
```

> 若 118 不通，可试 116（`192.168.0.116`），同一台机器双网卡。

#### 4.1.6 防火墙（可选）

```bash
sudo ufw allow from 192.168.0.0/24 to any port 3306 proto tcp
sudo ufw allow from 192.168.0.0/24 to any port 6379 proto tcp
```

---

### 4.2 应用节点（114）— 运行环境

SSH 登录 `kyle@192.168.0.114`：

#### 4.2.1 安装系统依赖

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk nginx maven nodejs npm git mysql-client redis-tools rsync

java -version   # 应 >= 17
node -v && npm -v
```

#### 4.2.2 同步时区（建议）

```bash
sudo timedatectl set-timezone Asia/Shanghai
sudo timedatectl set-ntp true
timedatectl status
```

#### 4.2.3 克隆仓库（本机一键部署必需）

```bash
git clone <你的仓库地址> ~/ai_manager
cd ~/ai_manager && git pull --ff-only
```

#### 4.2.4 创建运行用户与目录

```bash
sudo useradd -r -m -d /opt/ai-manager -s /bin/bash aimanager 2>/dev/null || true
sudo mkdir -p /opt/ai-manager/backend /opt/ai-manager/backend/uploads
sudo mkdir -p /var/www/ai-manager
sudo chown -R aimanager:aimanager /opt/ai-manager
sudo chown -R www-data:www-data /var/www/ai-manager
```

#### 4.2.5 确认能连数据节点

```bash
mysql -h 192.168.0.118 -u ai_manager -p123456 ai_manager_admin -e "SELECT 1;"
redis-cli -h 192.168.0.118 ping
```

#### 4.2.6 本机一键部署 sudo 权限（Web 部署必需）

```bash
sudo cp ~/ai_manager/deploy/sudoers/ai-manager-deploy.example /etc/sudoers.d/ai-manager-deploy
sudo chmod 440 /etc/sudoers.d/ai-manager-deploy
sudo visudo -c

# 验证
sudo -u aimanager sudo -n -u kyle test -d ~/ai_manager/deploy/scripts && echo OK
```

---

### 4.3 后端部署（114）

#### 4.3.1 方式 A — 在 Windows 开发机构建并上传

```powershell
cd G:\projects\ai_project\ai_manager
mvn clean package -DskipTests -pl admin-server -am -f admin-backend\pom.xml

scp admin-backend\admin-server\target\admin-server-1.0.0-SNAPSHOT.jar kyle@192.168.0.114:/tmp/admin-server.jar
scp deploy\env\backend.env.example kyle@192.168.0.114:/tmp/backend.env
```

#### 4.3.2 在 114 上安装 JAR 与环境变量

```bash
sudo mv /tmp/admin-server.jar /opt/ai-manager/backend/admin-server.jar
sudo mv /tmp/backend.env /opt/ai-manager/backend/backend.env
sudo chown aimanager:aimanager /opt/ai-manager/backend/admin-server.jar /opt/ai-manager/backend/backend.env
```

确认 `backend.env` 内容：

```ini
SPRING_PROFILES_ACTIVE=prod

MYSQL_HOST=192.168.0.118
MYSQL_PORT=3306
MYSQL_DATABASE=ai_manager_admin
MYSQL_USER=ai_manager
MYSQL_PASSWORD=123456

REDIS_HOST=192.168.0.118
REDIS_PORT=6379
REDIS_DATABASE=0

NOTE_STORAGE_TYPE=LOCAL
```

#### 4.3.3 注册 systemd 服务

```bash
cd ~/ai_manager
sudo cp deploy/systemd/ai-manager-backend.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable ai-manager-backend
sudo systemctl start ai-manager-backend
sudo systemctl status ai-manager-backend
```

验证：

```bash
curl -s http://127.0.0.1:8080/api/health
# 响应中 mysql、redis 应为 UP
```

#### 4.3.4 方式 B — 114 Web 一键部署后端

前提：已完成 4.2 节（含 Maven、sudoers、仓库 clone），且已安装含 deploy runner 的 JAR。

1. 浏览器打开 http://192.168.0.114/#/deploy → **部署步骤**
2. 点击 **一键部署后端**
3. 实际执行 `deploy/scripts/deploy-on-pi-backend.sh`（git pull → mvn → 安装 JAR → 延迟重启）

生产配置见 `admin-backend/admin-server/src/main/resources/application-prod.yml`：

```yaml
ai-manager:
  deploy:
    runner:
      enabled: true
      mode: local
      project-root: /home/kyle/ai_manager
      run-as-user: kyle
```

#### 4.3.5 方式 C — Windows Web / 脚本一键部署后端

**Web 界面（开发机）：**

- 本地启动后端（`dev` profile，`runner.mode=remote`）
- `application-dev.yml` 配置 `ai-manager.deploy.pi.password`（与 SSH 密码一致）
- 浏览器 http://127.0.0.1:5173 → 部署中心 → 一键部署后端

**PowerShell 脚本：**

```powershell
cd G:\projects\ai_project\ai_manager
# 可选：$env:PI_PASSWORD = "Asd123456"
powershell -ExecutionPolicy Bypass -File deploy/scripts/deploy-backend.ps1
```

---

### 4.4 前端部署与 Nginx（114）

#### 4.4.1 方式 A — 在 Windows 开发机构建并上传

```powershell
cd G:\projects\ai_project\ai_manager\admin-web
npm install
npm run build
```

产物目录：`admin-web\dist\`

> **注意**：必须上传到**空目录**再 rsync，不要 `scp -r dist` 到已有目录（会变成双层 `dist/dist`）。

```powershell
ssh kyle@192.168.0.114 "mkdir -p /tmp/ai-manager-new"
scp -r G:\projects\ai_project\ai_manager\admin-web\dist\* kyle@192.168.0.114:/tmp/ai-manager-new/
ssh kyle@192.168.0.114 "sudo rsync -av --delete /tmp/ai-manager-new/ /var/www/ai-manager/ && sudo chown -R www-data:www-data /var/www/ai-manager"
```

#### 4.4.2 配置 Nginx（首次必做）

```bash
cd ~/ai_manager
sudo cp deploy/nginx/ai-manager.conf /etc/nginx/sites-available/ai-manager
sudo ln -sf /etc/nginx/sites-available/ai-manager /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl reload nginx
```

配置要点：

- `/api/deploy/stream` 需长超时（构建过程数分钟无输出）
- `index.html` 禁止长期缓存，避免部署后仍引用旧 JS hash

浏览器访问：**http://192.168.0.114/#/home**（部署后建议 **Ctrl+F5** 强制刷新）

#### 4.4.3 方式 B — 114 Web 一键部署前端

1. 打开 http://192.168.0.114/#/deploy → **部署步骤**
2. 点击 **一键部署前端**
3. 执行 `deploy/scripts/deploy-on-pi-frontend.sh`（git pull → npm build → rsync）

> Pi 上 `npm run build` 约 5～15 分钟。日志 SSE 中断后构建可能仍在后台继续，部署中心会自动轮询状态。

#### 4.4.4 方式 C — Windows 脚本或 Web 一键

```powershell
cd G:\projects\ai_project\ai_manager
powershell -ExecutionPolicy Bypass -File deploy/scripts/deploy-frontend.ps1
```

或在开发机部署中心点击 **一键部署前端**（Java SSH 密码上传）。

---

### 4.5 部署完成检查清单

| 检查项 | 命令 / 预期 |
|--------|-------------|
| 后端健康 | `curl http://192.168.0.114/api/health` → `mysql` / `redis` / `status` 均为 UP |
| 今日待办 API | `curl http://192.168.0.114/api/todos/today` → JSON |
| 前端页面 | 浏览器打开 `/#/home`，有侧边栏和首页 |
| 自动检查 | 部署中心 → 部署步骤 → **自动检查全部**（1～7 项） |
| Console 无红错 | F12 → 不应有 `SyntaxError` |
| 后端日志 | `journalctl -u ai-manager-backend -n 50 --no-pager` |

---

## 5. 代码更新部署（日常）

### 5.1 114 Web 一键部署（推荐）

```text
浏览器 → http://192.168.0.114/#/deploy → 部署步骤
→ 一键部署后端 / 一键部署前端
```

更新 Nginx 或 systemd 配置后，在 114 上：

```bash
cd ~/ai_manager && git pull
sudo cp deploy/nginx/ai-manager.conf /etc/nginx/sites-available/ai-manager
sudo cp deploy/systemd/ai-manager-backend.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo nginx -t && sudo systemctl reload nginx
```

### 5.2 仅更新后端（Windows 脚本）

```powershell
cd G:\projects\ai_project\ai_manager
powershell -ExecutionPolicy Bypass -File deploy/scripts/deploy-backend.ps1
```

或 Git Bash：

```bash
bash deploy/scripts/deploy-backend-to-pi.sh
```

### 5.3 仅更新前端（Windows 脚本）

```powershell
cd G:\projects\ai_project\ai_manager
powershell -ExecutionPolicy Bypass -File deploy/scripts/deploy-frontend.ps1
```

或 Git Bash：

```bash
bash deploy/scripts/deploy-frontend-to-pi.sh
```

### 5.4 前后端都更新

```powershell
cd G:\projects\ai_project\ai_manager
powershell -ExecutionPolicy Bypass -File deploy/scripts/deploy-backend.ps1
powershell -ExecutionPolicy Bypass -File deploy/scripts/deploy-frontend.ps1
powershell -ExecutionPolicy Bypass -File deploy/scripts/health-check.ps1
```

### 5.5 仅更新 SQL（有结构变更时）

在 **数据节点** 上，按 `admin-backend/sql/` 中增量脚本顺序执行：

```bash
cd ~/ai_manager
sudo docker exec -i ai-manager-mysql mysql -uroot -p123456 ai_manager_admin < admin-backend/sql/你的增量脚本.sql
```

或：

```bash
bash deploy/scripts/import-sql-to-docker-mysql.sh admin-backend/sql/你的增量脚本.sql
```

> 全量重建（**会清空数据**）才用 `deploy-all.sql`。

### 5.6 更新 Nginx / systemd 配置

```bash
# 114 上
cd ~/ai_manager
sudo cp deploy/nginx/ai-manager.conf /etc/nginx/sites-available/ai-manager
sudo cp deploy/systemd/ai-manager-backend.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo nginx -t && sudo systemctl reload nginx
```

---

## 6. 运维命令速查

### 6.1 应用节点 114

```bash
# 后端
sudo systemctl status ai-manager-backend
sudo systemctl restart ai-manager-backend
journalctl -u ai-manager-backend -f

# Nginx
sudo nginx -t
sudo systemctl reload nginx

# 本机 API
curl -s http://127.0.0.1/api/health
curl -s http://127.0.0.1:8080/api/health

# 本机手动部署脚本
cd ~/ai_manager
bash deploy/scripts/deploy-on-pi-backend.sh
bash deploy/scripts/deploy-on-pi-frontend.sh
```

### 6.2 数据节点 118

```bash
cd /opt/ai-manager/data-node

docker compose ps
docker compose logs -f
docker compose restart

# MySQL 备份
docker exec ai-manager-mysql mysqldump -uroot -p123456 ai_manager_admin > backup_$(date +%F).sql

# Redis
docker exec ai-manager-redis redis-cli ping

# 局域网诊断
bash ~/ai_manager/deploy/scripts/check-mysql-lan.sh
```

---

## 7. Chat2DB / Windows 连库

### 7.1 直连（优先用有线 118）

| 项 | 值 |
|----|-----|
| 主机 | `192.168.0.118`（或 `192.168.0.116`） |
| 端口 | `3306` |
| 用户 | `ai_manager` |
| 密码 | `123456` |
| 数据库 | `ai_manager_admin` |

Windows `mysql` 客户端需加：

```bash
mysql -h 192.168.0.118 -u ai_manager -p123456 --ssl-mode=DISABLED ai_manager_admin -e "SELECT 1;"
```

JDBC URL：

```text
jdbc:mysql://192.168.0.118:3306/ai_manager_admin?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&connectTimeout=30000&socketTimeout=60000
```

### 7.2 SSH 隧道（AP 隔离 / 握手超时时）

| 项 | 值 |
|----|-----|
| SSH 主机 | `192.168.0.118`（或 `116`） |
| SSH 用户 | `kyle` |
| SSH 密码 | `Asd123456` |
| 数据库主机 | **`127.0.0.1`**（不是 118） |
| 端口 | `3306` |

JDBC URL：

```text
jdbc:mysql://127.0.0.1:3306/ai_manager_admin?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&connectTimeout=30000&socketTimeout=60000
```

---

## 8. 常见问题

| 现象 | 原因 / 处理 |
|------|-------------|
| 502 Bad Gateway | 114 上 `systemctl status ai-manager-backend`，确认 8080 正常 |
| API 失败、页面能开 | 检查 Nginx `/api/` 反代；`curl http://127.0.0.1:8080/api/health` |
| MySQL 连接失败 | 114 上 `mysql -h 192.168.0.118 ...`；118 上 `docker compose ps` |
| Redis DOWN | `redis-cli -h 192.168.0.118 ping`；检查 `backend.env` 中 `REDIS_HOST` |
| 前端白屏 + `Unexpected token '<'` | 旧 JS 缓存或 hash 不一致；重新 build 并 rsync；Ctrl+F5；勿用 `index_pc.html` |
| 前端白屏 + `SyntaxError` 等 | vue-i18n 文案含特殊字符；修 locale 后重新构建 |
| `scp -r dist` 后页面异常 | 变成双层目录；用 `dist\*` 传到 `/tmp/ai-manager-new/` |
| 一键部署日志中断 | 构建可能仍在后台；部署中心会轮询；或 SSH 查看 `ps aux \| grep -E "mvn\|vite"` |
| 一键部署 sudo 失败 | 安装 `deploy/sudoers/ai-manager-deploy.example`；systemd 需 `NoNewPrivileges=no` |
| 检查清单 journalctl 退出码 1 | `aimanager` 无 journal 读权限；`sudo usermod -aG systemd-journal aimanager` 并重启服务，或更新 sudoers 允许 `journalctl` |
| 服务器时间与本地不一致 | `sudo timedatectl set-timezone Asia/Shanghai && sudo timedatectl set-ntp true` |
| 上传失败 | `sudo chown -R aimanager:aimanager /opt/ai-manager/backend/uploads` |
| 百度网盘授权失败 | 回调须为 `http://192.168.0.114/oauth/baidu/callback`，Nginx 须反代 `/oauth/` |

### 重建 MySQL 用户（118 上）

```bash
source /opt/ai-manager/data-node/.env
docker exec -i ai-manager-mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
CREATE DATABASE IF NOT EXISTS ai_manager_admin
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
DROP USER IF EXISTS 'ai_manager'@'%';
CREATE USER 'ai_manager'@'%' IDENTIFIED BY '123456';
GRANT ALL PRIVILEGES ON ai_manager_admin.* TO 'ai_manager'@'%';
FLUSH PRIVILEGES;
SQL
```

---

## 9. 目录与配置清单

### 应用节点 114

```
/home/kyle/ai_manager/          # 仓库（本机一键部署 project-root）

/opt/ai-manager/backend/
  admin-server.jar
  backend.env              # MYSQL_HOST / REDIS_HOST → 192.168.0.118
  uploads/

/var/www/ai-manager/       # 前端 dist（index.html + assets/）

/etc/nginx/sites-available/ai-manager.conf
/etc/systemd/system/ai-manager-backend.service
/etc/sudoers.d/ai-manager-deploy
```

### 数据节点 118

```
/opt/ai-manager/data-node/
  docker-compose.yml
  .env                       # MYSQL_ROOT_PASSWORD / MYSQL_PASSWORD
  mysql/conf.d/custom.cnf
  redis/redis.conf
```

### 仓库内相关路径

| 路径 | 说明 |
|------|------|
| `docs/raspberry-pi-deploy.md` | 本文 |
| `admin-backend/sql/deploy-all.sql` | 全量 SQL |
| `deploy/nginx/ai-manager.conf` | Nginx 站点（含 SSE 长超时） |
| `deploy/systemd/ai-manager-backend.service` | 后端服务 |
| `deploy/env/backend.env.example` | 后端环境变量模板 |
| `deploy/sudoers/ai-manager-deploy.example` | 114 本机一键部署 sudo |
| `deploy/docker/data-node/` | MySQL + Redis Docker |
| `deploy/scripts/deploy-on-pi-backend.sh` | 114 本机构建部署后端 |
| `deploy/scripts/deploy-on-pi-frontend.sh` | 114 本机构建部署前端 |
| `deploy/scripts/deploy-backend.ps1` | Windows 一键部署后端 |
| `deploy/scripts/deploy-frontend.ps1` | Windows 一键部署前端 |
| `deploy/scripts/health-check.ps1` | Windows 健康检查 |
| `deploy/scripts/deploy-*-to-pi.sh` | Git Bash 部署脚本 |
| `deploy/scripts/import-sql-to-docker-mysql.sh` | Docker MySQL 导入 SQL |
| `deploy/scripts/setup-data-node-docker.sh` | 数据节点 Docker 安装 |

---

## 10. 本地开发（Windows 119）

开发机使用 `dev` profile，MySQL / Redis 默认连本机 `127.0.0.1`：

```bash
# 后端
cd admin-backend && mvn -pl admin-server -am spring-boot:run

# 前端
cd admin-web && npm run dev
```

本地访问：http://127.0.0.1:5173 ，API 由 Vite 代理到 http://127.0.0.1:8080 。

远程一键部署（dev profile）在 `application-dev.yml` 中配置 `ai-manager.deploy.pi.password` 与 `runner.mode: remote`。

---

## 11. ESP 副屏 / 局域网 API

- 后端监听 `0.0.0.0:8080`，局域网可直连 API。
- 番茄钟 ESP 固件：`ADMIN_API_HOST` 设为 `192.168.0.114`，端口 `80`（走 Nginx）或 `8080`（直连后端）。
- 手机访问 `http://192.168.0.114/` 自动加载移动版布局。
