# admin-backend

Spring Boot 3.3.7 / Java 17 / MyBatis-Plus 3.5.9 / Redis。Maven 多模块。

## 模块与依赖链

| 模块 | 说明 | 依赖 |
|---|---|---|
| `admin-common` | 统一响应 `ApiResult`、错误码、业务异常 | 无（叶子） |
| `admin-framework` | Redis / MyBatis-Plus / CORS / 全局异常 | admin-common |
| `admin-system` | 业务（用户、番茄钟、笔记、电商、AI 知识、RAG 等） | admin-common、admin-framework + Spring AI、PDFBox、POI、SSHj、PG 驱动 |
| `admin-server` | 启动入口 `AdminApplication`、`application.yml` | admin-system（传递全部） |

依赖链：`admin-server → admin-system → admin-framework → admin-common`。仅 admin-server 有 Spring Boot 打包插件。

## 启动（重要）

必须在父工程目录 `admin-backend` 下构建：

```powershell
cd admin-backend
mvn clean install -DskipTests          # 编译全部模块并安装到本地 .m2
mvn -pl admin-server spring-boot:run   # 单独启动
# 或一键：.\run.ps1
```

⚠️ **禁止** `mvn -pl admin-server -am spring-boot:run`（单条）——`-am` 会把 run 绑到父 POM（无 main 类）而失败。`-am` 只能用于 package/install/test。

端口 **8080**（`0.0.0.0`），健康检查 `GET http://localhost:8080/api/health`。
worktree 内联调用 `.\dev.ps1 -Task N -Side backend` 换端口（808N）。

## 常用验证命令

- 模块级编译+单测：`mvn -pl admin-system -am test`
- 只编译：`mvn compile` / `mvn -pl admin-system -am compile`

## 依赖

- MySQL 8：127.0.0.1:3306，库 `ai_manager_admin`（新库执行 `sql/deploy-all.sql`）
- Redis 6+：127.0.0.1:6379
- PostgreSQL（pgvector）：192.168.0.118:5432，用于 RAG

## 配置文件

- `admin-server/src/main/resources/application.yml`：默认 dev profile（MySQL/Redis 本机、PG 远程）
- `application-prod.yml`：树莓派部署（192.168.0.114），由 `SPRING_PROFILES_ACTIVE=prod` 切换
- Maven 拉取失败时可用 `mvn -s settings-aliyun.xml -pl admin-server -am clean package -DskipTests`
