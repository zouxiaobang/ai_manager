# 后端核心类说明

## 统一响应类

### ApiResult<T>

**位置**：`admin-common/src/main/java/com/ai/manager/common/result/ApiResult.java`

全局统一API响应包装类。

**字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | `int` | 状态码，0=成功，非0=失败 |
| `message` | `String` | 消息文本 |
| `data` | `T` | 响应数据，可为null（null时不序列化） |
| `timestamp` | `long` | 响应时间戳（毫秒） |

**常用静态方法**：

```java
// 成功响应（无数据）
ApiResult<Void> ok()

// 成功响应（带数据）
ApiResult<T> ok(T data)

// 失败响应（错误码枚举）
ApiResult<T> fail(ResultCode resultCode)

// 失败响应（自定义码和消息）
ApiResult<T> fail(int code, String message)
```

**特点**：
- 使用 `@JsonInclude(NON_NULL)`：data为null时不输出到JSON
- 实现 `Serializable` 接口
- 与HTTP状态码解耦，使用业务状态码

---

### ResultCode

**位置**：`admin-common/src/main/java/com/ai/manager/common/result/ResultCode.java`

错误码枚举类。

**常用错误码**：

| 枚举 | code | message | 说明 |
|------|------|---------|------|
| `SUCCESS` | 0 | success | 成功 |
| `BAD_REQUEST` | 400 | 请求参数错误 | 参数校验失败 |
| `UNAUTHORIZED` | 401 | 未登录或登录已过期 | 认证失败 |
| `FORBIDDEN` | 403 | 无权限访问 | 权限不足 |
| `NOT_FOUND` | 404 | 资源不存在 | 资源未找到 |
| `INTERNAL_ERROR` | 500 | 服务器内部错误 | 系统异常 |

---

### PageResult<T>

**位置**：`admin-common/src/main/java/com/ai/manager/common/result/PageResult.java`

分页响应结果类。

**字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `records` | `List<T>` | 数据列表 |
| `total` | `long` | 总记录数 |
| `page` | `long` | 当前页码 |
| `pageSize` | `long` | 每页大小 |
| `extra` | `Map<String, Object>` | 扩展统计字段（如库存合计等） |

---

### PageUtils

**位置**：`admin-common/src/main/java/com/ai/manager/common/result/PageUtils.java`

分页工具类。

**核心方法**：

```java
// 页码归一化（默认1）
long normalizePage(Long page)

// 页大小归一化（默认20，最大100）
long normalizePageSize(Long pageSize)

// 构建PageResult（实体→VO转换）
<E, V> PageResult<V> of(IPage<E> page, Function<E, V> converter)
```

---

## 异常类

### BusinessException

**位置**：`admin-common/src/main/java/com/ai/manager/common/exception/BusinessException.java`

业务异常类，继承 `RuntimeException`。

**构造方法**：

```java
// 通过错误码枚举创建
BusinessException(ResultCode resultCode)

// 自定义码和消息
BusinessException(int code, String message)
```

**使用场景**：
- 业务逻辑校验失败时抛出
- Controller层统一捕获处理

---

## 全局异常处理

### GlobalExceptionHandler

**位置**：`admin-framework/src/main/java/com/ai/manager/framework/web/GlobalExceptionHandler.java`

使用 `@RestControllerAdvice` 实现全局异常统一处理。

**处理的异常类型**：

| 异常类型 | HTTP状态 | 返回格式 | 说明 |
|---------|---------|---------|------|
| `BusinessException` | 200 | ApiResult | 业务异常，返回业务错误码 |
| `MethodArgumentNotValidException` | 200 | ApiResult(400) | 参数校验失败 |
| `BindException` | 200 | ApiResult(400) | 参数绑定失败 |
| `Exception` | 200 | ApiResult(500) | 兜底异常，记录error日志 |

---

## 核心Service类

### PomodoroSessionService（番茄钟会话服务）

**位置**：`admin-system/src/main/java/com/ai/manager/system/service/PomodoroSessionService.java`

番茄钟实时会话服务，基于Redis实现多端同步。

**核心特性**：
- 纯Redis存储，无数据库依赖
- 支持Web端(ADMIN)和ESP32设备(DEVICE)双向同步
- 控制器抢占机制
- 自动状态推算（RUNNING状态下自动计算剩余时间）

**核心方法**：

| 方法 | 说明 |
|------|------|
| `getActiveSession()` | 获取当前活动会话（自动推算剩余时间） |
| `syncSession(request)` | 同步会话状态 |
| `isPlanEditBlocked()` | 判断计划编辑是否被锁定 |

**Redis Key设计**：
- `pomodoro:session:active` - 活动会话（TTL 24h）
- `pomodoro:device:last_seen_ms` - 设备最后心跳时间

---

### EcSalesOrderService（电商销售订单服务）

**位置**：`admin-system/src/main/java/com/ai/manager/system/service/EcSalesOrderService.java`

电商销售订单核心服务，继承 `IService<EcSalesOrder>`。

**主要功能分类**：

| 分类 | 方法 | 说明 |
|------|------|------|
| 查询 | `pageOrders()` | 分页查询订单 |
| | `getOrderDetail()` | 获取订单详情 |
| | `getMonthlyOverview()` | 月度概览统计 |
| CRUD | `createOrder()` | 创建订单 |
| | `updateOrder()` | 更新订单 |
| | `deleteOrder()` | 删除订单 |
| 状态流转 | `confirmOrder()` | 确认订单 |
| | `shipOrder()` | 整单发货 |
| | `shipLine()` | 单行发货 |
| | `refundLine()` | 单行退款 |
| | `cancelLine()` | 单行取消 |
| Excel导入 | `uploadImport()` | 上传导入文件 |
| | `previewImport()` | 预览导入 |
| | `commitImport()` | 提交导入 |
| | `reparseImport()` | 重解析 |
| | `updateImportManualCosts()` | 更新人工成本 |

---

### NbNoteService（笔记本笔记服务）

**位置**：`admin-system/src/main/java/com/ai/manager/system/service/NbNoteService.java`

笔记管理服务，继承 `IService<NbNote>`。

**核心方法**：

| 方法 | 说明 |
|------|------|
| `getNoteDetail(id)` | 获取笔记详情（含内容） |
| `listRecent(limit)` | 最近笔记列表 |
| `searchNotes(keyword)` | 全文搜索笔记 |
| `createNote(dto)` | 创建笔记 |
| `updateNote(id, dto)` | 更新笔记 |
| `deleteNote(id)` | 逻辑删除（入回收站） |
| `listTrash()` | 回收站列表 |
| `restoreNote(id)` | 从回收站恢复 |
| `purgeNote(id)` | 永久删除 |
| `purgeAllTrash()` | 清空回收站 |

**设计特点**：
- 逻辑删除 + 物理删除双模式
- 笔记内容独立存储（本地/百度网盘）
- 内容哈希去重

---

### DeployRunnerService（部署运行服务）

**位置**：`admin-system/src/main/java/com/ai/manager/system/service/DeployRunnerService.java`

基于SSH的远程部署服务。

**核心功能**：
- Git pull 拉取最新代码
- Maven/npm 构建项目
- 重启应用服务
- 部署模式：local（本地）/ remote（远程SSH）
- 部署历史记录

---

## 核心Controller类

### 命名与路径规范

所有Controller遵循：
- 注解：`@RestController` + `@RequestMapping("/api/xxx")`
- 返回值：全部包装为 `ApiResult<T>`
- 依赖注入：`@RequiredArgsConstructor`（Lombok）

### 代表性Controller列表

| Controller | 路径前缀 | 模块 |
|-----------|---------|------|
| `HealthController` | `/api/health` | 健康检查 |
| `SysUserController` | `/api/system/users` | 用户管理 |
| `EcSalesOrderController` | `/api/ecommerce/sales-orders` | 销售订单 |
| `EcProductController` | `/api/ecommerce/products` | 商品管理 |
| `EcInventoryController` | `/api/ecommerce/inventory` | 库存管理 |
| `EcCartonController` | `/api/ecommerce/cartons` | 纸箱管理 |
| `EcFactoryController` | `/api/ecommerce/factories` | 工厂管理 |
| `EcShopController` | `/api/ecommerce/shops` | 店铺管理 |
| `EcPlatformController` | `/api/ecommerce/platforms` | 平台管理 |
| `EcInboundOrderController` | `/api/ecommerce/inbound-orders` | 入库单 |
| `EcOutboundOrderController` | `/api/ecommerce/outbound-orders` | 出库单 |
| `EcStocktakeOrderController` | `/api/ecommerce/stocktake-orders` | 盘点单 |
| `EcExpressStationController` | `/api/ecommerce/express-stations` | 快递站点 |
| `EcListingLinkController` | `/api/ecommerce/listing-links` | Listing链接 |
| `EcMonthlySettlementController` | `/api/ecommerce/monthly-settlements` | 月度结算 |
| `PomodoroPlanController` | `/api/pomodoro/plans` | 番茄钟计划 |
| `PomodoroSessionController` | `/api/pomodoro/session` | 番茄钟会话 |
| `PomodoroRecordController` | `/api/pomodoro/records` | 番茄钟记录 |
| `NbNotebookController` | `/api/notebooks` | 笔记本 |
| `NbNoteController` | `/api/notes` | 笔记 |
| `NbNoteTagController` | `/api/note-tags` | 笔记标签 |
| `NbTodoController` | `/api/todos` | 待办事项 |
| `DeployRunController` | `/api/deploy/run` | 部署运行 |
| `DeployDatabaseController` | `/api/deploy/database` | 数据库终端 |
| `DeployLogController` | `/api/deploy/logs` | 部署日志 |
| `DeployChecklistController` | `/api/deploy/checklist` | 部署检查清单 |
| `ImageSpaceController` | `/api/image-space` | 图片空间 |
| `StorageCenterController` | `/api/storage-center` | 存储中心 |
| `BaiduPanController` | `/api/baidu-pan` | 百度网盘 |

---

## 配置类

### MybatisPlusConfig

**位置**：`admin-framework/src/main/java/com/ai/manager/framework/config/MybatisPlusConfig.java`

MyBatis-Plus配置类。

**核心配置**：
- `@MapperScan("com.ai.manager.**.mapper")` - 通配符扫描所有模块Mapper
- 分页插件（PaginationInnerInterceptor，MySQL方言）

---

### RedisConfig

**位置**：`admin-framework/src/main/java/com/ai/manager/framework/config/RedisConfig.java`

Redis配置类。

**序列化策略**：
- Key / HashKey：`StringRedisSerializer`（字符串）
- Value / HashValue：`GenericJackson2JsonRedisSerializer`（JSON）

---

### WebMvcConfig

**位置**：`admin-framework/src/main/java/com/ai/manager/framework/config/WebMvcConfig.java`

Web MVC配置，主要配置CORS跨域：

| 路径 | 允许方法 | 凭证 | 说明 |
|------|---------|------|------|
| `/api/**` | GET/POST/PUT/DELETE/PATCH/OPTIONS | 是 | API接口 |
| `/uploads/**` | GET + OPTIONS | 否 | 静态资源 |

---

### JacksonConfig

**位置**：`admin-framework/src/main/java/com/ai/manager/framework/config/JacksonConfig.java`

Jackson序列化配置。

**时间格式化**：
- 序列化：`LocalDateTime` → `"yyyy-MM-dd HH:mm:ss"`
- 反序列化：兼容多种格式，缺省部分补0

---

### TimezoneConfig

**位置**：`admin-framework/src/main/java/com/ai/manager/framework/config/TimezoneConfig.java`

启动时设置JVM默认时区为 `Asia/Shanghai`。

---

## 切面类

### ControllerLogAspect

**位置**：`admin-framework/src/main/java/com/ai/manager/framework/aspect/ControllerLogAspect.java`

Controller日志切面，基于AOP拦截所有 `@RestController`。

**记录内容**：
- 请求：类名.方法名、IP、入参
- 响应：出参、耗时
- 异常：异常消息、耗时

**智能跳过**：
高频轮询接口不记录日志，避免污染：
- 番茄钟session接口
- 番茄钟今日记录
- 待办今日
- 部署日志流
