# AI Manager Code Wiki

智能副屏固件与管理后台系统完整代码文档。

## 项目概述

**AI Manager** 是一个综合性的智能管理系统，包含三大核心部分：
- **管理后台后端**（Java Spring Boot）：提供电商管理、笔记本、番茄钟、部署中心等业务API
- **管理后台前端**（Vue 3）：PC端管理界面 + 移动端H5界面，双端适配
- **ESP32副屏固件**（C/C++ + LVGL）：7寸触摸屏副屏，展示番茄钟、歌词、像素狗等

## 快速导航

### 架构总览
- [整体架构](./architecture/overall-architecture.md) - 系统整体架构与技术栈
- [模块依赖关系](./architecture/module-dependencies.md) - 各模块间的依赖关系

### 后端文档
- [后端架构总览](./backend/backend-overview.md) - Spring Boot后端整体架构
- [后端模块说明](./backend/modules.md) - admin-common/framework/system/server四大模块
- [核心类说明](./backend/core-classes.md) - ApiResult、Service、Controller等核心类
- [数据层设计](./backend/data-layer.md) - MyBatis-Plus与数据库设计
- [业务模块详解](./backend/business-modules.md) - 电商/笔记本/番茄钟/24小时/部署中心
- [24小时重启系统](./backend/24hour-module.md) - 日常习惯追踪与时段管理
- [API接口清单](./backend/api-list.md) - 主要API接口列表
- [配置说明](./backend/configuration.md) - 配置文件与环境变量

### 前端文档
- [前端架构总览](./frontend/frontend-overview.md) - Vue前端整体架构
- [目录结构](./frontend/directory-structure.md) - 源码目录组织
- [路由系统](./frontend/router.md) - PC端与移动端路由设计
- [状态管理](./frontend/stores.md) - Pinia状态管理
- [API请求封装](./frontend/api-request.md) - Axios封装与接口层
- [布局组件](./frontend/layouts.md) - 主要布局组件
- [移动端架构](./frontend/mobile.md) - 移动端页面架构
- [核心组件](./frontend/components.md) - 通用业务组件
- [工具函数](./frontend/utils.md) - 工具函数与常量

### 固件文档
- [固件架构总览](./firmware/firmware-overview.md) - ESP32固件整体架构
- [目录结构](./firmware/directory-structure.md) - 固件源码组织
- [主程序流程](./firmware/main-flow.md) - 启动流程与主循环
- [显示与触摸](./firmware/display-touch.md) - LCD显示与GT911触摸驱动
- [UI系统](./firmware/ui-system.md) - LVGL界面与静态布局
- [番茄钟模块](./firmware/pomodoro.md) - 番茄钟同步与显示
- [像素狗模块](./firmware/pixel-dog.md) - 像素宠物系统
- [媒体模块](./firmware/media.md) - 歌词与媒体控制
- [WiFi与网络](./firmware/wifi-network.md) - WiFi连接与HTTP通信
- [存储系统](./firmware/storage.md) - NVS与SD卡存储
- [配置文件](./firmware/configuration.md) - 编译与运行时配置

### 部署与运行
- [快速开始](./deployment/quick-start.md) - 本地开发环境搭建
- [后端部署](./deployment/backend-deploy.md) - 后端部署方式
- [前端部署](./deployment/frontend-deploy.md) - 前端构建与部署
- [固件烧录](./deployment/firmware-flash.md) - ESP32固件编译烧录

## 技术栈总览

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端语言 | Java | 17 |
| 后端框架 | Spring Boot | 3.3.7 |
| ORM | MyBatis-Plus | 3.5.9 |
| 数据库 | MySQL | 8.x |
| 缓存 | Redis | 6.x |
| 前端框架 | Vue | 3.5 |
| 前端语言 | TypeScript | 5.6 |
| 构建工具 | Vite | 6.0 |
| UI组件库 | Element Plus | 2.9 |
| 状态管理 | Pinia | 2.3 |
| 固件平台 | ESP-IDF | v5.x |
| 固件UI | LVGL | v9.x |
| 固件语言 | C/C++ | C++17 |

## 项目结构

```
ai_manager/
├── admin-backend/          # Java后端（Spring Boot多模块）
│   ├── admin-common/       # 公共基础层
│   ├── admin-framework/    # 框架配置层
│   ├── admin-system/       # 业务实现层
│   └── admin-server/       # 启动入口层
├── admin-web/              # Vue前端（PC+移动双端）
│   ├── src/
│   │   ├── api/            # API接口层
│   │   ├── components/     # 通用组件
│   │   ├── layouts/        # 布局组件
│   │   ├── mobile/         # 移动端页面
│   │   ├── router/         # 路由配置
│   │   ├── stores/         # Pinia状态
│   │   └── views/          # PC端页面
│   └── ...
├── firmware/
│   └── esp32_s3_sub_display/  # ESP32-S3副屏固件
│       ├── main/           # 主程序源码
│       ├── sdcard_assets/  # SD卡资源
│       └── scripts/        # 构建烧录脚本
└── docs/wiki/              # 本文档
```

## 核心功能模块

### 电商管理系统
- 商品/SKU/纸箱/工厂管理
- 销售订单与采购入库
- 库存管理与盘点
- 快递站点与价格管理
- 月度结算与对账
- Listing链接管理

### 笔记本系统
- 树形笔记本/笔记管理
- 富文本编辑器
- 标签与搜索
- 待办事项（支持重复、提醒）
- 本地+百度网盘双写同步
- 回收站机制

### 番茄钟系统
- 自定义专注/休息计划
- 浏览器计时与统计
- Web端与ESP32副屏双向同步
- 完成记录与报表统计

### 24小时重启系统
- 一天6时段习惯追踪（前夜/晨起/专注/中段/下午/晚间）
- 检查项勾选与自动保存
- 时段变更通知提醒
- 内容填写（复盘总结等）

### 像素狗（副屏）
- 成长系统（等级、经验值、升级公式）
- 陪伴值与情绪系统（多级衰减、对话系统）
- 多种动画状态（行走、待机、开心、睡觉、抚摸、专注）
- 智能对话系统（8级亲密度分档对话）
- 互动即时同步（摸头/打招呼/蹭蹭/抱抱）
- 与番茄钟联动（专注获得经验 + 专注模式实时同步）

### 部署中心
- SSH远程部署（Git pull + 构建 + 重启）
- 部署历史与日志
- 数据库远程终端
- 部署检查清单
- AI日志分析

### 存储中心
- 电商图片/笔记本图片统一管理
- 空间使用统计
- 孤儿文件清理
- 百度网盘集成
