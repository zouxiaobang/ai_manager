# AI Manager 管理后台（后端）

Spring Boot 3 + MyBatis-Plus + Redis，Maven 多模块。

## 模块说明

| 模块 | 说明 |
|------|------|
| `admin-common` | 统一响应 `ApiResult`、错误码、业务异常 |
| `admin-framework` | Redis / MyBatis-Plus / 跨域 / 全局异常 |
| `admin-system` | 业务模块（用户等 Controller/Service） |
| `admin-server` | 启动入口、`application.yml` |

## 前置条件

- JDK 17+
- Maven 3.9+
- MySQL 8（全新库执行 `sql/deploy-all.sql` 一次即可；或按模块分别执行 `schema.sql`、`pomodoro.sql`、`notebook.sql` 等）
- Redis 6+

## Maven

全局 `~/.m2/settings.xml` 已配置为 `mirrorOf=central`（勿用 `*`）。若仍拉取失败，可临时使用项目内备用配置：

```bash
mvn -s settings-aliyun.xml -pl admin-server -am clean package -DskipTests
```

## 启动

必须在**父工程目录** `admin-backend` 下构建，并带上 `-am`（同时编译依赖模块）。  
不要只在 `admin-server` 子目录单独执行 `mvn spring-boot:run`，否则会去阿里云找 `admin-system` 而报错。

```powershell
cd admin-backend

# 首次或改代码后：编译并安装到本地仓库
mvn clean install -DskipTests

# 启动（推荐：先编译依赖，再仅对 admin-server 执行 run）
mvn -pl admin-server -am package -DskipTests
mvn -pl admin-server spring-boot:run
```

或直接运行脚本：`.\run.ps1`

注意：不要使用 `mvn -pl admin-server -am spring-boot:run`（单条命令），否则 `spring-boot:run` 会绑到父工程 `admin-backend`（无 main 类）而失败。

默认端口 `8080`，健康检查：`GET http://localhost:8080/api/health`

## API 示例

- `GET /api/health` — 健康检查（含 Redis ping）
- `GET /api/system/users` — 用户列表

### 番茄钟 `/api/pomodoro`

- `GET/POST /api/pomodoro/plans`、`PUT/DELETE /api/pomodoro/plans/{id}` — 计划 CRUD
- `GET /api/pomodoro/plans/default` — 默认计划
- `POST /api/pomodoro/records` — 写入完成记录（专注/休息）
- `GET /api/pomodoro/records?startDate=&endDate=` — 记录列表
- `GET /api/pomodoro/stats/daily` — 按日统计（轮次、时长）
- `GET /api/pomodoro/stats/summary` — 区间汇总
- `GET /api/pomodoro/stats/today` — 今日进度
- `GET/PUT /api/pomodoro/session` — 浏览器与 ESP 双向同步（Redis：`phase`/`runState`/剩余秒/`source=ADMIN|DEVICE`）

统一响应格式：

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": 1710000000000
}
```
