# 部署资源

完整步骤见：[docs/raspberry-pi-deploy.md](../docs/raspberry-pi-deploy.md)

Web 界面部署中心（含一键部署与自动检查清单）：http://192.168.0.114/#/deploy

## 节点

| 节点 | IP | 角色 |
|------|-----|------|
| 应用节点 | 192.168.0.114 | Nginx、前端、Spring Boot |
| 数据节点（无线） | 192.168.0.116 | 与 118 同机 |
| 数据节点（有线） | 192.168.0.118 | Docker：MySQL 8、Redis 7 |

## 凭据（内网）

| 项目 | 值 |
|------|-----|
| SSH 用户 / 密码 | `kyle` / `Asd123456` |
| MySQL root / 应用密码 | `123456` / `123456` |

## 目录

| 目录 | 说明 |
|------|------|
| `docker/data-node/` | 数据节点 docker-compose、MySQL/Redis 配置 |
| `nginx/` | Nginx 站点（114，含 SSE 长超时） |
| `systemd/` | 后端 systemd（114） |
| `sudoers/` | 114 本机一键部署 sudo 示例 |
| `env/` | 后端环境变量示例（指向 118） |
| `scripts/` | 部署与健康检查脚本 |

## 快速命令（114 本机 Web 一键）

浏览器 → http://192.168.0.114/#/deploy → 部署步骤 → 一键部署后端/前端

前提：`git clone` 到 `~/ai_manager`，安装 Maven/Node，配置 `deploy/sudoers/ai-manager-deploy.example`。

## 快速命令（Windows 开发机）

```powershell
# 健康检查
powershell -ExecutionPolicy Bypass -File deploy/scripts/health-check.ps1

# 部署后端（或部署中心 Web 一键）
powershell -ExecutionPolicy Bypass -File deploy/scripts/deploy-backend.ps1

# 部署前端
powershell -ExecutionPolicy Bypass -File deploy/scripts/deploy-frontend.ps1
```

## 快速命令（数据节点 118）

```bash
bash deploy/scripts/setup-data-node-docker.sh
bash deploy/scripts/import-sql-to-docker-mysql.sh admin-backend/sql/deploy-all.sql
bash deploy/scripts/check-mysql-lan.sh
```
