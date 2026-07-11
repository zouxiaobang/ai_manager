# 后端模块说明

## 模块总览

项目采用 Maven 多模块架构，共分为 4 个子模块，遵循严格的分层依赖原则。

```
admin-backend (父工程)
├── admin-common      ← 最底层，无Spring依赖
├── admin-framework   ← 依赖 admin-common
├── admin-system      ← 依赖 admin-framework
└── admin-server      ← 依赖 admin-system，启动入口
```

**依赖方向**：从上到下，上层依赖下层，下层不能依赖上层。

---

## admin-common（公共基础层）

### 模块职责

提供全局通用的基础类，是最轻量化的模块，不依赖 Spring 框架，可被任何模块引用。

### 目录结构

```
admin-common/src/main/java/com/ai/manager/common/
├── exception/
│   └── BusinessException.java      # 业务异常类
├── result/
│   ├── ApiResult.java              # 统一响应结果
│   ├── PageResult.java             # 分页响应结果
│   ├── PageUtils.java              # 分页工具类
│   └── ResultCode.java             # 错误码枚举
└── time/
    └── DisplayTime.java            # 时间显示工具
```

### 核心类说明

| 类名 | 类型 | 说明 |
|------|------|------|
| `ApiResult<T>` | 类 | 全局统一API响应封装 |
| `ResultCode` | 枚举 | 错误码定义 |
| `PageResult<T>` | 类 | 分页响应结果 |
| `PageUtils` | 工具类 | 分页工具方法 |
| `BusinessException` | 异常类 | 业务运行时异常 |
| `DisplayTime` | 工具类 | 上海时区时间格式化 |

### 依赖

- jackson-annotations（JSON注解）
- lombok（简化POJO）

---

## admin-framework（框架配置层）

### 模块职责

提供 Spring Boot 框架级的配置和中间件集成，是横切关注点的集中地。

### 目录结构

```
admin-framework/src/main/java/com/ai/manager/framework/
├── aspect/
│   └── ControllerLogAspect.java     # Controller日志切面
├── config/
│   ├── JacksonConfig.java           # Jackson序列化配置
│   ├── MybatisPlusConfig.java       # MyBatis-Plus配置
│   ├── RedisConfig.java             # Redis配置
│   ├── TimezoneConfig.java          # 时区配置
│   └── WebMvcConfig.java            # Web MVC配置（CORS等）
└── web/
    ├── FaviconController.java       # 网站图标
    └── GlobalExceptionHandler.java  # 全局异常处理器
```

### 核心配置类

| 配置类 | 说明 |
|--------|------|
| `MybatisPlusConfig` | Mapper扫描、分页插件 |
| `RedisConfig` | RedisTemplate序列化配置 |
| `WebMvcConfig` | CORS跨域、静态资源映射 |
| `JacksonConfig` | 时间格式化、反序列化兼容 |
| `TimezoneConfig` | JVM默认时区设为Asia/Shanghai |

### 核心切面和处理器

| 类 | 类型 | 说明 |
|----|------|------|
| `ControllerLogAspect` | AOP切面 | 记录所有Controller的请求响应日志 |
| `GlobalExceptionHandler` | 异常处理器 | 统一捕获并处理所有异常 |

### CORS 跨域规则

| 路径 | 允许方法 | 允许凭证 | 说明 |
|------|---------|---------|------|
| `/api/**` | GET/POST/PUT/DELETE/PATCH/OPTIONS | 是 | API接口，支持所有方法 |
| `/uploads/**` | GET + OPTIONS | 否 | 静态资源，只读 |

### 日志切面智能跳过

高频轮询接口不记录日志，避免日志污染：
- 番茄钟 session 接口
- 番茄钟今日记录
- 待办今日列表
- 部署日志流

### 依赖

- admin-common
- spring-boot-starter-web
- spring-boot-starter-validation
- spring-boot-starter-aop
- spring-boot-starter-data-redis
- mybatis-plus-spring-boot3-starter
- mysql-connector-j

---

## admin-system（业务实现层）

### 模块职责

所有业务逻辑的实现层，是项目最核心、最庞大的模块。

### 目录结构

```
admin-system/src/main/java/com/ai/manager/system/
├── client/                    # 第三方客户端
│   └── BaiduPanClient.java    # 百度网盘API客户端
│
├── config/                    # 业务配置类
│   ├── BaiduPanProperties.java
│   ├── EcUploadResourceConfig.java
│   ├── NoteStorageProperties.java
│   └── NotebookImageUploadResourceConfig.java
│
├── controller/                # 控制层（40+）
│   ├── HealthController.java
│   ├── SysUserController.java
│   ├── Ec*Controller.java      # 电商系列
│   ├── Nb*Controller.java      # 笔记本系列
│   ├── Pomodoro*Controller.java # 番茄钟系列
│   ├── Deploy*Controller.java   # 部署中心系列
│   ├── ImageSpaceController.java
│   ├── StorageCenterController.java
│   └── ...
│
├── domain/                    # 领域模型
│   ├── entity/                 # 数据库实体
│   ├── dto/                    # 数据传输对象
│   ├── vo/                     # 视图对象
│   ├── enums/                  # 枚举
│   └── storage/                # 存储相关模型
│
├── job/                       # 定时任务
│   ├── EcListingLinkPricingRecalcJob.java
│   └── NoteContentReconcileJob.java
│
├── mapper/                    # 数据访问层（50+）
│   ├── SysUserMapper.java
│   ├── Ec*Mapper.java
│   ├── Nb*Mapper.java
│   ├── Pomodoro*Mapper.java
│   └── ...
│
├── runner/                    # 启动运行器
│   ├── NoteContentMigrationRunner.java
│   └── NoteContentReconcileRunner.java
│
├── service/                   # 服务层
│   ├── impl/                   # 服务实现
│   └── support/                # 业务辅助类（复杂逻辑拆分）
│
└── util/                      # 工具类
    ├── DeployLogLineParser.java
    ├── MysqlCharsetUtils.java
    ├── MysqlCommentEncodingFix.java
    └── NoteContentUtils.java
```

### 业务模块统计

| 业务域 | Controller | Service | Mapper | 实体数量 |
|--------|-----------|---------|--------|---------|
| 电商 (Ec) | 20+ | 20+ | 28+ | 28+ |
| 笔记本 (Nb) | 5+ | 6+ | 6+ | 6+ |
| 番茄钟 (Pomodoro) | 3 | 3 | 2 | 2 |
| 部署中心 (Deploy) | 5 | 6+ | 0 | 0 |
| 存储中心 | 2 | 2+ | 0 | 0 |
| 系统 (Sys) | 2 | 2 | 3 | 3 |

### Service Support 模式

复杂业务逻辑拆分为 Support 辅助类，避免 Service 实现类过于臃肿：

| Support 类 | 所属模块 | 功能 |
|------------|---------|------|
| `EcCartonMatcher` | 电商 | 纸箱匹配算法 |
| `EcSalesOrderPricingSupport` | 电商 | 销售订单定价计算 |
| `EcSalesOrderInventorySupport` | 电商 | 销售订单库存处理 |
| `EcSalesOrderMatchSupport` | 电商 | 订单匹配逻辑 |
| `EcExpressBillParseSupport` | 电商 | 快递账单解析 |
| `EcImportStatusSupport` | 电商 | 导入状态映射 |
| `SysImportColumnMappingSupport` | 系统 | 导入列映射 |
| `SysImportParseSupport` | 系统 | 导入解析 |
| `StorageDualWriteSupport` | 存储 | 双写策略 |
| `StoragePathSupport` | 存储 | 路径处理 |
| `TodoRepeatSupport` | 笔记本 | 待办重复规则 |

### 依赖

- admin-framework
- poi-ooxml（Excel导入导出）
- sshj（SSH远程部署）

---

## admin-server（启动入口层）

### 模块职责

Spring Boot 启动入口，负责应用启动、配置文件管理和打包部署。

### 目录结构

```
admin-server/
├── src/main/java/com/ai/manager/
│   └── AdminApplication.java        # 启动类
└── src/main/resources/
    ├── application.yml              # 主配置
    ├── application-dev.yml          # 开发环境配置
    └── application-prod.yml         # 生产环境配置
```

### 启动类

**AdminApplication.java**

```java
@SpringBootApplication(scanBasePackages = "com.ai.manager")
@EnableScheduling
public class AdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
```

**关键点**：
- `scanBasePackages = "com.ai.manager"` — 跨模块扫描所有包
- `@EnableScheduling` — 启用定时任务

### 配置文件加载顺序

优先级从高到低：
1. 环境变量（最高优先级）
2. `application-{profile}.yml`（环境配置）
3. `application.yml`（公共配置）

### 主要配置项

```yaml
server:
  port: 8080
  address: 0.0.0.0    # 允许局域网访问（ESP/手机）

spring:
  profiles.active: ${SPRING_PROFILES_ACTIVE:dev}
  datasource:
    url: jdbc:mysql://...
    username: ${MYSQL_USER:root}
    password: ${MYSQL_PASSWORD:}
  data.redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
  servlet.multipart:
    max-file-size: 10MB

mybatis-plus:
  configuration.map-underscore-to-camel-case: true
  global-config.db-config:
    logic-delete-field: deleted
    logic-delete-value: 1
    logic-not-delete-value: 0

# 业务配置
ai-manager:
  upload.ecommerce-path: uploads/ecommerce
  upload.notebook-images-path: uploads/notebook/images
  deploy:
    enabled: true
    mode: remote   # local / remote
    pi:
      host: ...
      user: ...
      password: ...
  baidu-pan:
    app-key: ...
    secret-key: ...
  note-storage:
    type: LOCAL   # LOCAL / BAIDU_PAN
    local-root: ...
```

### 构建方式

```bash
# 编译打包（在父工程目录执行）
mvn clean package -DskipTests

# 生成的 Jar 包
admin-server/target/admin-server-1.0.0-SNAPSHOT.jar
```

### 运行方式

```bash
# 开发环境
mvn -pl admin-server spring-boot:run

# 生产环境
java -jar admin-server.jar --spring.profiles.active=prod
```

### 依赖

- admin-system
- spring-boot-starter-test

---

## 模块间调用规范

### Controller → Service

```java
@RestController
@RequestMapping("/api/xxx")
@RequiredArgsConstructor
public class XxxController {

    private final XxxService xxxService;

    @GetMapping
    public ApiResult<PageResult<XxxVO>> page(...) {
        PageResult<XxxVO> result = xxxService.page(...);
        return ApiResult.ok(result);
    }
}
```

### Service → Mapper

```java
@Service
@RequiredArgsConstructor
public class XxxServiceImpl implements XxxService {

    private final XxxMapper xxxMapper;

    @Override
    public XxxVO getDetail(Long id) {
        Xxx entity = xxxMapper.selectById(id);
        // 业务逻辑...
        return convertToVO(entity);
    }
}
```

### Service 间调用

Service 之间可以相互调用，但应避免循环依赖。
复杂逻辑优先拆分为 Support 类而非跨 Service 调用。

---

## 数据层设计规范

### 实体类命名

- 电商：`Ec{业务名}`
- 笔记本：`Nb{业务名}`
- 番茄钟：`Pomodoro{业务名}`
- 系统：`Sys{业务名}`

### 约定字段

所有表基本都包含以下字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT | 主键，自增 |
| `create_time` | DATETIME | 创建时间 |
| `update_time` | DATETIME | 更新时间 |
| `deleted` | TINYINT | 逻辑删除标记（0=未删，1=已删） |

### Mapper 开发原则

1. 优先使用 BaseMapper 提供的通用方法
2. 简单单表查询不写 SQL，用 QueryWrapper
3. 复杂多表查询用 `@Select` 注解
4. 不写 XML 映射文件
