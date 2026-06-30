# AI Manager

智能副屏固件与管理后台。

## 子项目

| 目录 | 说明 |
|------|------|
| `firmware/esp32_s3_sub_display` | ESP32-S3 副屏固件（微雪 7 寸触摸屏） |
| `admin-backend` | 管理后台 API（Spring Boot 多模块） |
| `admin-web` | 管理后台前端（Vue 3 + Element Plus） |

## 管理后台快速开始

1. MySQL 执行 `admin-backend/sql/deploy-all.sql`（全量建表+演示数据，推荐新环境）
2. 启动 Redis
3. 后端：`cd admin-backend && mvn -pl admin-server -am spring-boot:run`
4. 前端：`cd admin-web && npm install && npm run dev`

详见各子目录 `README.md`。

## 副屏固件

当前为全新 Demo 工程，包含像素动画与触摸涟漪效果。编译烧录见 `firmware/esp32_s3_sub_display/README.md`。

> 原 `firmware/esp32_s3_lvgl` 与 `pc_daemon` 已移除，后续副屏功能将在此工程中重新开发。管理后台番茄钟 API 仍保留，待新固件接入。
