# 数据层设计

## 概述

数据层基于 **MyBatis-Plus + MySQL** 构建，采用注解 SQL 为主的开发模式，省去 XML 映射文件。

## MyBatis-Plus 配置

### 配置类

**位置**：`admin-framework/.../config/MybatisPlusConfig.java`

```java
@Configuration
@MapperScan("com.ai.manager.**.mapper")
public class MybatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

**关键配置**：
- **通配符 Mapper 扫描**：`com.ai.manager.**.mapper` 适配多模块结构
- **分页插件**：MySQL 方言的分页拦截器

### YAML 配置

```yaml
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true  # 下划线转驼峰
  global-config:
    db-config:
      logic-delete-field: deleted       # 全局逻辑删除字段
      logic-delete-value: 1
      logic-not-delete-value: 0
```

## 三层数据对象模型

```
Entity (数据库实体)
    ↑ 转换
DTO (数据传输对象 - 请求入参)
    ↑ 转换
VO (视图对象 - 响应出参)
```

### 各层职责

| 对象 | 位置 | 用途 | 示例 |
|------|------|------|------|
| **Entity** | `domain/entity/` | 数据库表映射，最底层 | `EcProduct` |
| **DTO** | `domain/dto/` | 接收前端请求参数 | `EcProductCreateDTO` |
| **VO** | `domain/vo/` | 返回前端的视图数据 | `EcProductDetailVO` |

### 转换时机

- Controller 接收参数：DTO
- Service 内部处理：Entity 为主
- Service 返回给 Controller：VO

## Mapper 层设计

### 继承结构

所有 Mapper 继承 `BaseMapper<T>`，自动获得 17+ 通用方法：

```java
@Mapper
public interface EcProductMapper extends BaseMapper<EcProduct> {
    // 自定义查询用 @Select 注解
}
```

### BaseMapper 通用方法

| 分类 | 方法 | 说明 |
|------|------|------|
| 新增 | `insert(entity)` | 插入一条记录 |
| 删除 | `deleteById(id)` | 根据ID删除 |
| | `deleteBatchIds(ids)` | 批量删除 |
| | `delete(queryWrapper)` | 条件删除 |
| 修改 | `updateById(entity)` | 根据ID更新 |
| | `update(entity, queryWrapper)` | 条件更新 |
| 查询 | `selectById(id)` | 根据ID查询 |
| | `selectBatchIds(ids)` | 批量查询 |
| | `selectOne(queryWrapper)` | 查询单条 |
| | `selectList(queryWrapper)` | 查询列表 |
| | `selectPage(page, queryWrapper)` | 分页查询 |
| | `selectCount(queryWrapper)` | 统计数量 |
| | `selectMaps(queryWrapper)` | 查询Map列表 |

### 自定义 SQL

复杂查询使用 `@Select` / `@Update` / `@Delete` 注解：

```java
@Mapper
public interface EcSalesOrderMapper extends BaseMapper<EcSalesOrder> {

    @Select("SELECT status, COUNT(*) AS count " +
            "FROM ec_sales_order " +
            "WHERE deleted = 0 AND create_time >= #{startTime} " +
            "GROUP BY status")
    List<Map<String, Object>> countOrdersByStatusGroup(
        @Param("startTime") LocalDateTime startTime
    );
}
```

## Service 层设计

### 继承结构

所有 Service 接口继承 `IService<T>`，实现类继承 `ServiceImpl<Mapper, Entity>`：

```java
// 接口
public interface EcProductService extends IService<EcProduct> {
    EcProductDetailVO getDetail(Long id);
}

// 实现
@Service
public class EcProductServiceImpl 
    extends ServiceImpl<EcProductMapper, EcProduct> 
    implements EcProductService {
    
    @Override
    public EcProductDetailVO getDetail(Long id) {
        EcProduct entity = getById(id);
        // ... 转换为 VO
    }
}
```

### IService 通用方法

| 分类 | 方法 | 说明 |
|------|------|------|
| 新增 | `save(entity)` | 保存 |
| | `saveBatch(entities)` | 批量保存 |
| | `saveOrUpdate(entity)` | 保存或更新 |
| 删除 | `removeById(id)` | 删除 |
| | `removeBatchByIds(ids)` | 批量删除 |
| 修改 | `updateById(entity)` | 更新 |
| | `updateBatchById(entities)` | 批量更新 |
| 查询 | `getById(id)` | 根据ID获取 |
| | `listByIds(ids)` | 批量获取 |
| | `list(queryWrapper)` | 列表查询 |
| | `page(page, queryWrapper)` | 分页查询 |
| | `count(queryWrapper)` | 统计 |
| | `getOne(queryWrapper)` | 获取单条 |

## 实体类规范

### 注解规范

```java
@Data
@TableName("ec_product")
public class EcProduct implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    @TableField("sku_code")
    private String skuCode;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

### 常用注解

| 注解 | 用途 |
|------|------|
| `@TableName("xxx")` | 指定表名 |
| `@TableId(type = IdType.AUTO)` | 主键自增 |
| `@TableField("column_name")` | 字段映射（非必须，下划线转驼峰自动） |
| `@TableLogic` | 逻辑删除字段 |
| `@TableField(exist = false)` | 非数据库字段 |
| `@TableField(fill = FieldFill.INSERT)` | 插入时自动填充 |

### 约定字段

所有业务表基本都包含以下字段：

| 字段 | Java类型 | 数据库类型 | 说明 |
|------|---------|-----------|------|
| `id` | `Long` | BIGINT | 主键，自增 |
| `create_time` | `LocalDateTime` | DATETIME | 创建时间 |
| `update_time` | `LocalDateTime` | DATETIME | 更新时间 |
| `deleted` | `Integer` | TINYINT | 逻辑删除（0=未删，1=已删） |

## 分页查询设计

### PageResult

统一分页响应结果：

```java
@Data
public class PageResult<T> implements Serializable {
    private List<T> records;    // 数据列表
    private long total;         // 总记录数
    private long page;          // 当前页码
    private long pageSize;      // 每页大小
    private Map<String, Object> extra;  // 扩展统计字段
}
```

### PageUtils 工具

```java
// 基础分页
PageResult<VO> result = PageUtils.of(page, converter);

// 带扩展统计
PageResult<VO> result = PageUtils.of(page, converter, extraMap);
```

### 分页参数规范

| 参数 | 类型 | 默认值 | 最大值 | 说明 |
|------|------|--------|--------|------|
| `page` | Long | 1 | - | 页码 |
| `pageSize` | Long | 20 | 100 | 每页大小 |

## 数据库表设计规范

### 命名规范

| 类型 | 命名规则 | 示例 |
|------|---------|------|
| 表名 | 模块前缀_业务名 | `ec_product`, `nb_note` |
| 字段名 | 小写+下划线 | `product_name` |
| 主键 | `id` | `id` |
| 外键 | 关联表_id | `product_id` |
| 时间字段 | xxx_time | `create_time` |
| 状态字段 | xxx_status | `order_status` |

### 模块前缀

| 前缀 | 模块 | 示例 |
|------|------|------|
| `ec_` | 电商 | `ec_sales_order`, `ec_product` |
| `nb_` | 笔记本 | `nb_note`, `nb_notebook` |
| `pomodoro_` | 番茄钟 | `pomodoro_plan`, `pomodoro_record` |
| `sys_` | 系统 | `sys_user`, `sys_import_batch` |

### 索引规范

- 主键索引：`PRIMARY KEY (id)`
- 外键字段：建议加索引
- 常用查询字段：联合索引
- 唯一约束：唯一索引

## 主要表清单

### 电商模块（28+ 表）

| 表名 | 说明 |
|------|------|
| `ec_platform` | 电商平台 |
| `ec_shop` | 店铺 |
| `ec_factory` | 工厂 |
| `ec_factory_type` | 工厂类型 |
| `ec_factory_type_carton` | 工厂类型-纸箱关联 |
| `ec_product` | 产品 |
| `ec_sku` | SKU |
| `ec_carton` | 纸箱 |
| `ec_sales_order` | 销售订单 |
| `ec_sales_order_line` | 销售订单行 |
| `ec_sales_order_shortage` | 缺货记录 |
| `ec_sales_order_inventory_deduct` | 库存扣减记录 |
| `ec_inbound_order` | 入库单 |
| `ec_inbound_order_line` | 入库单行 |
| `ec_outbound_order` | 出库单 |
| `ec_outbound_order_line` | 出库单行 |
| `ec_inventory` | 库存 |
| `ec_inventory_log` | 库存日志 |
| `ec_stocktake_order` | 盘点单 |
| `ec_stocktake_order_line` | 盘点单行 |
| `ec_express_station` | 快递站点 |
| `ec_express_price` | 快递价格 |
| `ec_express_notice` | 快递通知 |
| `ec_listing_link` | Listing链接 |
| `ec_listing_link_product` | Listing-产品关联 |
| `ec_listing_link_sku` | Listing-SKU关联 |
| `ec_settlement_snapshot` | 结算快照 |
| `ec_settlement_express_bill` | 快递账单 |
| `ec_settlement_express_bill_line` | 快递账单行 |
| `ec_purchase_order_config` | 采购配置 |
| `ec_system_config` | 系统配置 |

### 笔记本模块（6 表）

| 表名 | 说明 |
|------|------|
| `nb_notebook` | 笔记本 |
| `nb_note` | 笔记 |
| `nb_note_tag` | 标签 |
| `nb_note_tag_rel` | 笔记-标签关联 |
| `nb_todo_item` | 待办事项 |
| `nb_baidu_pan_auth` | 百度网盘授权 |

### 番茄钟模块（2 表）

| 表名 | 说明 |
|------|------|
| `pomodoro_plan` | 番茄钟计划 |
| `pomodoro_record` | 完成记录 |

### 系统模块（3 表）

| 表名 | 说明 |
|------|------|
| `sys_user` | 用户 |
| `sys_import_batch` | 导入批次 |
| `sys_import_profile` | 导入配置 |

## 逻辑删除机制

### 工作原理

MyBatis-Plus 自动在查询和更新时追加 `deleted = 0` 条件：

```sql
-- 查询自动追加
SELECT * FROM ec_product WHERE id = 1 AND deleted = 0

-- 删除变成更新
UPDATE ec_product SET deleted = 1 WHERE id = 1
```

### 物理删除

需要物理删除时，使用自定义 SQL：

```java
@Delete("DELETE FROM nb_note WHERE id = #{id}")
int physicalDeleteById(@Param("id") Long id);
```

### 回收站模式

笔记本等模块使用逻辑删除实现回收站：
- 删除 → 设置 deleted = 1
- 回收站列表 → 查询 deleted = 1
- 恢复 → 设置 deleted = 0
- 永久删除 → 物理删除

## 性能优化

### 1. 合理使用索引

- 查询条件字段加索引
- 避免索引失效（函数、类型转换等）
- 联合索引遵循最左前缀原则

### 2. 分页查询优化

- 深分页问题：使用游标或子查询优化
- 只查询需要的字段

### 3. 缓存策略

- 番茄钟实时会话使用 Redis
- 不常变的数据可考虑本地缓存

### 4. 批量操作

- 批量插入使用 `saveBatch`
- 批量更新使用 `updateBatchById`
- 避免循环单条操作

## SQL 脚本管理

### 目录结构

```
admin-backend/sql/
├── schema.sql              # 基础表结构
├── deploy-all.sql          # 全量建表+演示数据
├── ecommerce_*.sql         # 电商模块建表
├── notebook.sql            # 笔记本模块
├── pomodoro.sql            # 番茄钟模块
├── sys_import.sql          # 系统导入模块
├── migration/              # 数据迁移脚本
│   └── kyle-e-commerce/    # 电商数据迁移
└── tools/                  # 工具脚本
```

### 版本管理

- 建表脚本：`xxx.sql`
- 修改脚本：`xxx_alter.sql`
- 数据迁移：`migration/` 目录
