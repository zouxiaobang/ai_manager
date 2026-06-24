# 部署资源

完整步骤见：[docs/raspberry-pi-deploy.md](../docs/raspberry-pi-deploy.md)

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
| `nginx/` | Nginx 站点（114） |
| `systemd/` | 后端 systemd（114） |
| `env/` | 后端环境变量示例（指向 118） |
| `scripts/` | 部署与健康检查脚本 |

## 快速命令（Windows 开发机）

```powershell
# 健康检查
powershell -ExecutionPolicy Bypass -File deploy/scripts/health-check.ps1

# 部署后端
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
