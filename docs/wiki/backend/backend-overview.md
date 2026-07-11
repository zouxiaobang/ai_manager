# 后端架构总览

## 项目简介

AI Manager 管理后台后端基于 **Spring Boot 3 + MyBatis-Plus + Redis** 构建，采用 Maven 多模块架构，提供电商管理、笔记本、番茄钟、部署中心等完整业务能力。

## 技术栈

| 类别 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 语言 | Java | 17 | LTS版本 |
| 框架 | Spring Boot | 3.3.7 | 核心框架 |
| ORM | MyBatis-Plus | 3.5.9 | ORM增强 |
| 数据库 | MySQL | 8.x | 关系型数据库 |
| 缓存 | Redis | 6.x | 缓存与会话 |
| 连接池 | HikariCP | — | Spring Boot默认 |
| JSON | Jackson | — | 序列化 |
| Excel | Apache POI | 5.3.0 | 导入导出 |
| SSH | SSHJ | 0.38.0 | 远程部署 |
| 构建 | Maven | 3.9+ | 项目构建 |

## 模块架构

### 四层模块划分

```
admin-backend/
├── admin-common       ← 公共基础层（最底层）
│   ├── result/        ApiResult / PageResult / ResultCode
│   ├── exception/     BusinessException
│   └── time/          DisplayTime
│
├── admin-framework    ← 框架配置层
│   ├── config/        MybatisPlusConfig / RedisConfig / WebMvcConfig
│   ├── web/           GlobalExceptionHandler
│   └── aspect/        ControllerLogAspect
│
├── admin-system       ← 业务实现层（最核心）
│   ├── controller/    40+ Controller
│   ├── service/       Service接口
│   ├── service/impl/  Service实现
│   ├── mapper/        50+ Mapper
│   ├── domain/        Entity / DTO / VO / Enum
│   ├── config/        业务配置
│   ├── client/        第三方客户端
│   ├── job/           定时任务
│   ├── runner/        启动运行器
│   └── util/          工具类
│
└── admin-server       ← 启动入口层
    ├── AdminApplication.java
    └── resources/
        ├── application.yml
        ├── application-dev.yml
        └── application-prod.yml
```

### 模块职责

| 模块 | 职责 | 依赖 |
|------|------|------|
| **admin-common** | 统一响应、异常、时间工具 | 无（最底层） |
| **admin-framework** | 框架配置、全局异常、日志切面 | admin-common |
| **admin-system** | 所有业务逻辑实现 | admin-framework |
| **admin-server** | 启动入口、配置文件 | admin-system |

## 核心设计模式

### 1. 统一响应模式

所有API接口返回 `ApiResult<T>` 统一格式：

```json
{
  "code": 0,
  "message": "success",
  "data": { ... },
  "timestamp": 1710000000000
}
```

### 2. 全局异常处理

通过 `@RestControllerAdvice` 统一捕获并处理异常：
- `BusinessException` → 业务异常，返回code+message
- 参数校验异常 → 400 + 字段错误
- 兜底异常 → 500 + 日志记录

### 3. Service层通用接口

所有业务Service继承 `IService<T>`（MyBatis-Plus提供），自动获得：
- save / saveBatch
- removeById / removeByIds
- updateById / updateBatchById
- getById / listByIds
- page / list / count
- ... 等17+通用方法

### 4. Mapper层通用接口

所有Mapper继承 `BaseMapper<T>`，自动获得基础CRUD方法。
复杂查询使用 `@Select` 注解编写SQL，无需XML文件。

## 业务模块概览

| 模块 | 前缀 | 核心功能 | 主要实体 |
|------|------|---------|---------|
| 电商管理 | `Ec` | 商品/订单/库存/结算 | EcProduct, EcSalesOrder, EcInventory 等28+ |
| 笔记本 | `Nb` | 笔记/标签/待办/网盘 | NbNote, NbNotebook, NbTodoItem 等6 |
| 番茄钟 | `Pomodoro` | 计划/会话/记录/统计 | PomodoroPlan, PomodoroRecord |
| **24小时重启** | `DailyChecklist` | **时段习惯追踪/检查清单** | **DailyChecklist** |
| 部署中心 | `Deploy` | 远程部署/SQL终端/日志 | 无持久化实体 |
| 存储中心 | `Storage` | 文件管理/图片空间 | 无独立实体 |
| 系统管理 | `Sys` | 用户/通用导入 | SysUser, SysImportBatch |

## 启动流程

```
1. 启动 AdminApplication
   │
   ├─ Spring Boot 初始化
   │  ├─ 扫描 com.ai.manager 包
   │  ├─ 加载 application.yml + application-{profile}.yml
   │  └─ 自动配置（DataSource/Redis/Web等）
   │
   ├─ MyBatis-Plus 初始化
   │  ├─ 扫描 Mapper 接口
   │  ├─ 注册分页插件
   │  └─ 配置逻辑删除
   │
   ├─ 业务 Bean 初始化
   │  ├─ Controller
   │  ├─ Service
   │  └─ Mapper
   │
   ├─ 启动完成
   │  ├─ 运行 CommandLineRunner (NoteContentMigrationRunner等)
   │  ├─ 启动定时任务 (@Scheduled)
   │  └─ 监听 8080 端口
   │
   └─ 对外提供 API 服务
```

## 代码组织约定

### 命名规范

| 类型 | 命名规则 | 示例 |
|------|---------|------|
| Controller | `{模块}{业务}Controller` | `EcSalesOrderController` |
| Service接口 | `{模块}{业务}Service` | `EcSalesOrderService` |
| Service实现 | `{模块}{业务}ServiceImpl` | `EcSalesOrderServiceImpl` |
| Mapper | `{模块}{业务}Mapper` | `EcSalesOrderMapper` |
| Entity | `{模块}{业务}` | `EcSalesOrder` |
| DTO | `{模块}{业务}{操作}DTO` | `EcSalesOrderCreateDTO` |
| VO | `{模块}{业务}{视图}VO` | `EcSalesOrderDetailVO` |

### 包结构规范

```
com.ai.manager.system/
├── controller/          # 控制层
├── service/             # 服务接口
│   └── impl/            # 服务实现
│   └── support/         # 业务辅助类（复杂逻辑拆分）
├── mapper/              # 数据访问
├── domain/
│   ├── entity/          # 数据库实体
│   ├── dto/             # 数据传输对象（请求）
│   ├── vo/              # 视图对象（响应）
│   ├── enums/           # 枚举
│   └── storage/         # 存储相关模型
├── config/              # 业务配置类
├── client/              # 第三方客户端
├── job/                 # 定时任务
├── runner/              # 启动运行器
└── util/                # 工具类
```

## 配置管理

### 多环境配置

| 环境 | Profile | 说明 |
|------|---------|------|
| 开发 | dev | 默认激活，调试日志，远程部署到Pi |
| 生产 | prod | 生产环境，info日志，本地自部署 |

### 配置加载顺序

1. `application.yml` - 公共基础配置
2. `application-{profile}.yml` - 环境特定配置（覆盖）
3. 环境变量 - 最高优先级（敏感信息）

### 主要配置项

- 数据源：支持环境变量覆盖
- Redis：主机、端口、数据库号
- 文件上传：电商图片、笔记本图片路径
- 部署中心：SSH连接配置
- 百度网盘：AppKey/SecretKey
- 笔记存储：本地/百度网盘切换

## 安全与约束

- CORS跨域配置：`/api/**` 允许全部方法，`/uploads/**` 仅GET
- 无认证体系（当前单用户模式）
- 业务异常使用 `BusinessException` 抛出
- 参数校验使用 `@Valid` + JSR-303 注解
