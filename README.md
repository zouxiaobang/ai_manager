# AI Manager

智能副屏与 PC 守护进程，以及管理后台。

## 子项目

| 目录 | 说明 |
|------|------|
| `firmware/esp32_s3_lvgl` | ESP32-S3 + LVGL 副屏固件 |
| `pc_daemon` | PC 端 WebSocket 守护进程（仅网易云控制/歌词） |
| `admin-backend` | 管理后台 API（Spring Boot 多模块） |
| `admin-web` | 管理后台前端（Vue 3 + Element Plus） |

## 管理后台快速开始

1. MySQL 执行 `admin-backend/sql/deploy-all.sql`（全量建表+演示数据，推荐新环境）
2. 启动 Redis
3. 后端：`cd admin-backend && mvn -pl admin-server -am spring-boot:run`
4. 前端：`cd admin-web && npm install && npm run dev`

详见各子目录 `README.md`。
