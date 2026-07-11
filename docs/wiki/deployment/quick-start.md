# 快速开始

## 环境准备

### 后端环境

| 软件 | 版本要求 | 说明 |
|------|---------|------|
| JDK | 17+ | LTS版本 |
| Maven | 3.9+ | 项目构建 |
| MySQL | 8.x | 数据库 |
| Redis | 6.x | 缓存 |

### 前端环境

| 软件 | 版本要求 | 说明 |
|------|---------|------|
| Node.js | 18+ | 推荐LTS版本 |
| npm | 9+ | 随Node.js安装 |

### 固件环境

| 软件 | 说明 |
|------|------|
| ESP-IDF | v5.x（官方IoT开发框架） |
| Python | 3.8+（IDF依赖） |
| 串口驱动 | CP2102/CH340等 |

## 一键启动（推荐）

### 1. 启动 MySQL 和 Redis

确保 MySQL 和 Redis 已启动并可访问。

### 2. 初始化数据库

```bash
# 使用命令行或图形化工具执行
mysql -u root -p < admin-backend/sql/deploy-all.sql
```

> `deploy-all.sql` 包含全量建表 + 演示数据，推荐新环境使用。

### 3. 启动后端

```powershell
cd admin-backend

# 编译并安装
mvn clean install -DskipTests

# 启动服务
mvn -pl admin-server spring-boot:run
```

或直接运行脚本：
```powershell
cd admin-backend
.\run.ps1
```

后端启动后访问：http://localhost:8080/api/health

### 4. 启动前端

```powershell
cd admin-web

# 安装依赖
npm install

# PC端开发
npm run dev:pc

# 或 移动端开发
npm run dev:mobile
```

- PC端：http://127.0.0.1:5173/index_pc.html
- 移动端：http://127.0.0.1:5173/mobile.html

## 后端详细启动

### 首次构建

必须在 **父工程目录** `admin-backend` 下执行：

```powershell
cd admin-backend

# 1. 编译所有模块并安装到本地仓库
mvn clean install -DskipTests

# 2. 启动 admin-server
mvn -pl admin-server spring-boot:run
```

### 常见问题

**问题：找不到 admin-common/admin-framework/admin-system 依赖**

解决：先执行 `mvn clean install -DskipTests` 安装所有模块到本地仓库。

**问题：`mvn -pl admin-server -am spring-boot:run` 失败**

原因：`spring-boot:run` 会绑到父工程（无main类）。

解决：分两步执行：
```powershell
mvn -pl admin-server -am package -DskipTests
mvn -pl admin-server spring-boot:run
```

**问题：Maven 下载依赖慢**

使用阿里云镜像配置：
```powershell
mvn -s settings-aliyun.xml clean install -DskipTests
```

### 配置数据库连接

修改 `admin-backend/admin-server/src/main/resources/application-dev.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_manager?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
```

或使用环境变量（推荐，不修改代码）：
```bash
set MYSQL_HOST=localhost
set MYSQL_PORT=3306
set MYSQL_DB=ai_manager
set MYSQL_USER=root
set MYSQL_PASSWORD=your_password
set REDIS_HOST=localhost
set REDIS_PORT=6379
```

## 前端详细启动

### PC端

```powershell
cd admin-web
npm install
npm run dev:pc
```

打开浏览器访问：http://127.0.0.1:5173/index_pc.html

### 移动端

```powershell
cd admin-web
npm install
npm run dev:mobile
```

打开浏览器访问：http://127.0.0.1:5173/mobile.html

> 移动端建议使用手机或浏览器开发者工具的移动端模式访问。

### 端口冲突

如果 5173 端口被占用：

```powershell
cd admin-web
.\kill-port.ps1
npm run dev
```

### 常见问题

**问题：Element Plus 的 .mjs.map / Syntax error "\\x00"**

解决：删除 `node_modules/.vite` 后重试。
> `vite.config.ts` 已关闭预构建 sourcemap 避免该问题。

**问题：API 请求失败**

检查后端是否启动，默认代理到 `http://127.0.0.1:8080`。

## 固件编译烧录

### 环境准备

1. 安装 ESP-IDF v5.x
2. 连接 ESP32-S3 开发板到电脑
3. 确认串口驱动已安装

### 编译

```powershell
cd firmware/esp32_s3_sub_display
.\scripts\build.ps1
```

### 烧录

```powershell
cd firmware/esp32_s3_sub_display
.\scripts\flash-monitor.ps1
```

### SD卡准备

1. 准备一张 Micro SD 卡（FAT32 格式）
2. 复制 `sdcard_assets/` 目录内容到 SD 卡根目录
3. 或使用部署脚本：
```powershell
.\scripts\deploy-sdcard.ps1 -DriveLetter D:
```

### WiFi配置

在SD卡的配置文件中设置后端API地址：

`sdcard/config/pomodoro_host.txt`：
```
http://192.168.1.100:8080
```

> 将 IP 替换为电脑的局域网 IP，ESP32 和电脑需在同一局域网。

## 验证安装

### 后端验证

访问健康检查接口：
```
GET http://localhost:8080/api/health
```

预期返回：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "status": "UP",
    "redis": "UP",
    "mysql": "UP"
  },
  "timestamp": 1710000000000
}
```

### 前端验证

- PC端：打开首页，能看到仪表盘
- 移动端：打开首页，能看到手绘风格界面

### 固件验证

- 屏幕显示首页界面
- 触摸有响应（点击底部Dock切换页面）
- WiFi连接成功（状态栏WiFi图标亮起）
- 番茄钟能与Web端同步

## 开发建议

### 后端开发

- 使用 IDEA 或 Eclipse 打开 Maven 项目
- 调试时启动 admin-server
- 使用 MyBatis-Plus 提供的代码生成器（如有需要）

### 前端开发

- 使用 VS Code 打开 admin-web 目录
- 推荐安装 Volar、Element Plus Snippets 等插件
- PC端和移动端可同时开发，互不干扰

### 固件开发

- 使用 VS Code + ESP-IDF 插件
- 或使用 ESP-IDF 命令行工具
- 善用串口监视器调试

## 项目结构速查

```
ai_manager/
├── admin-backend/       # 后端（Java）
│   └── 启动：mvn -pl admin-server spring-boot:run
├── admin-web/           # 前端（Vue）
│   └── 启动：npm run dev:pc 或 dev:mobile
└── firmware/
    └── esp32_s3_sub_display/  # 固件（ESP32）
        └── 编译：.\scripts\build.ps1
```
