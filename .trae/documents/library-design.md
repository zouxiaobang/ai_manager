# 资料库模块 — 产品需求文档与系统架构设计

> **角色：** @臭产品 (全能产品经理)
> **状态：** 设计阶段 v1.0
> **目标版本：** 与现有 admin-web / admin-backend 模块平行，复用同一套技术栈

---

## 一、产品概述

### 1.1 背景与目标

当前系统已有 **笔记本 (Notebook)** 模块，用于管理富文本笔记内容。但在实际业务中，用户还需要一个专门的文件管理中心，用于上传、存储、分类和预览各类文档文件（图片、PDF、Office 文档、代码文件、设计稿等）。**资料库 (Library)** 模块正是为此而生。

### 1.2 与 Notebook 的区分

| 维度 | Notebook (笔记本) | Library (资料库) |
|------|------------------|-----------------|
| **内容形态** | 富文本笔记，在线编辑 | 文件上传，已有文档管理 |
| **存储方式** | 文本内容存百度网盘/本地 | 文件存本地/对象存储 |
| **核心操作** | 创建/编辑/删除笔记 | 上传/下载/预览/移动/分享 |
| **预览方式** | 富文本渲染 | 图片预览、PDF预览、Office预览等 |
| **文件格式** | 无（纯文本 html） | 多格式支持（图片/文档/压缩包等） |

### 1.3 目标用户

- 需要统一管理产品图片、设计稿、合同文档的运营人员
- 需要在不同模块之间共享文件的管理员
- 需要备份和归档重要文件的系统使用者

---

## 二、功能需求清单 (PRD)

### 2.1 文件夹管理

| 功能 | 描述 | 优先级 |
|------|------|--------|
| 创建文件夹 | 在根目录或任意子文件夹下创建新文件夹 | P0 |
| 重命名文件夹 | 修改文件夹名称 | P0 |
| 删除文件夹 | 删除文件夹（含级联删除子文件和子文件夹） | P0 |
| 移动文件夹 | 拖拽或右键菜单移动到其他文件夹 | P1 |
| 树形目录浏览 | 左侧展开/折叠树结构导航 | P0 |

### 2.2 文档管理

| 功能 | 描述 | 优先级 |
|------|------|--------|
| 上传文件 | 单文件/批量上传，支持拖拽上传 | P0 |
| 下载文件 | 单文件下载，批量打包下载 | P0 |
| 重命名文件 | 修改文件名（保留扩展名） | P0 |
| 移动文件 | 将文件移动到其他文件夹 | P0 |
| 删除文件 | 软删除到回收站 | P0 |
| 复制文件 | 在同一个文件夹或跨文件夹复制 | P1 |
| 文件版本 | 同名文件上传时保存历史版本 | P2 |

### 2.3 文件预览

| 功能 | 描述 | 优先级 |
|------|------|--------|
| 图片预览 | 支持 JPG/PNG/GIF/SVG/WebP 缩略图和大图浏览 | P0 |
| PDF 预览 | 浏览器内嵌 PDF 渲染 | P1 |
| 文本预览 | TXT/MD/代码文件等纯文本渲染 | P1 |
| Office 预览 | Word/Excel/PPT 转为 HTML 或图片预览 | P2 |
| 视频预览 | MP4 等常见格式播放 | P2 |

### 2.4 搜索与检索

| 功能 | 描述 | 优先级 |
|------|------|--------|
| 文件名搜索 | 按文件名模糊匹配 | P0 |
| 标签筛选 | 按标签过滤文件 | P1 |
| 类型筛选 | 按文件类型分类（图片/文档/压缩包等） | P1 |
| 日期筛选 | 按上传/修改时间范围筛选 | P1 |

### 2.5 标签系统

| 功能 | 描述 | 优先级 |
|------|------|--------|
| 创建标签 | 创建自定义标签（名称+颜色） | P1 |
| 管理标签 | 编辑/删除标签 | P1 |
| 标记文件 | 给文件打标签（一个文件可多个标签） | P1 |
| 标签筛选 | 点击标签快速筛选文件 | P1 |

### 2.6 收藏与快捷操作

| 功能 | 描述 | 优先级 |
|------|------|--------|
| 收藏文件 | 星标标记重要文件 | P1 |
| 收藏夹视图 | 快速查看所有收藏文件 | P1 |
| 最近文档 | 按最后操作时间排序的最近文件列表 | P1 |

### 2.7 回收站

| 功能 | 描述 | 优先级 |
|------|------|--------|
| 查看回收站 | 列出所有软删除的文件 | P0 |
| 恢复文件 | 从回收站恢复到原位置 | P0 |
| 永久删除 | 彻底删除文件 | P0 |
| 清空回收站 | 批量彻底删除所有回收站文件 | P1 |

### 2.8 批量操作

| 功能 | 描述 | 优先级 |
|------|------|--------|
| 批量删除 | 勾选多个文件后批量删除 | P1 |
| 批量移动 | 勾选后移动到目标文件夹 | P1 |
| 批量下载 | 勾选后打包为 ZIP 下载 | P1 |
| 批量打标签 | 给多个文件同时添加标签 | P2 |

### 2.9 统计与信息

| 功能 | 描述 | 优先级 |
|------|------|--------|
| 文件夹统计 | 显示文件夹内文件数、总大小 | P1 |
| 空间使用 | 统计全部文件占用空间 | P1 |
| 文件详情侧栏 | 选中文件时显示详细信息 | P1 |

### 2.10 视图模式

| 功能 | 描述 | 优先级 |
|------|------|--------|
| 网格视图 | 卡片式文件展示，适合图片类 | P0 |
| 列表视图 | 表格列表展示，含详细信息列 | P0 |
| 排序 | 按名称/大小/修改时间排序 | P1 |

---

## 三、数据模型设计

### 3.1 数据库表结构

#### `doc_library_folder` — 资料库文件夹表

```sql
CREATE TABLE IF NOT EXISTS doc_library_folder (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    parent_id   BIGINT       DEFAULT NULL COMMENT '父文件夹 ID，NULL 为根级',
    name        VARCHAR(128) NOT NULL COMMENT '文件夹名称',
    icon        VARCHAR(32)  DEFAULT NULL COMMENT '图标标识',
    color       VARCHAR(16)  DEFAULT NULL COMMENT '颜色',
    sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id),
    KEY idx_sort (parent_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资料库文件夹';
```

#### `doc_library_file` — 资料库文件表

```sql
CREATE TABLE IF NOT EXISTS doc_library_file (
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    folder_id           BIGINT       DEFAULT NULL COMMENT '所属文件夹 ID',
    name                VARCHAR(256) NOT NULL COMMENT '文件名（含扩展名）',
    original_name       VARCHAR(256) NOT NULL COMMENT '原始文件名（上传时的原名）',
    extension           VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '文件扩展名（小写，不含点）',
    mime_type           VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'MIME 类型',
    file_size           BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
    storage_type        VARCHAR(16)  NOT NULL DEFAULT 'LOCAL' COMMENT '存储方式：LOCAL/BAIDU_PAN/S3',
    storage_path        VARCHAR(512) NOT NULL DEFAULT '' COMMENT '存储路径',
    storage_key         VARCHAR(256) DEFAULT NULL COMMENT '对象存储 key',
    content_hash        CHAR(64)     DEFAULT NULL COMMENT '文件 SHA-256 哈希',
    thumbnail_path      VARCHAR(512) DEFAULT NULL COMMENT '缩略图路径（图片类）',
    is_pinned           TINYINT      NOT NULL DEFAULT 0 COMMENT '收藏/星标',
    description         VARCHAR(512) DEFAULT NULL COMMENT '文件描述/备注',
    sort_order          INT          NOT NULL DEFAULT 0 COMMENT '排序',
    -- 知识库相关字段
    kb_status           VARCHAR(16)  NOT NULL DEFAULT 'NONE' COMMENT '知识库状态：NONE/PENDING/PROCESSING/READY/FAILED',
    kb_error            VARCHAR(512) DEFAULT NULL COMMENT '知识库处理失败原因',
    kb_processed_at     DATETIME     DEFAULT NULL COMMENT '知识库处理完成时间',
    -- 埋点相关字段
    view_count          INT          NOT NULL DEFAULT 0 COMMENT '预览次数',
    download_count      INT          NOT NULL DEFAULT 0 COMMENT '下载次数',
    deleted             TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_folder_id (folder_id),
    KEY idx_extension (extension),
    KEY idx_is_pinned (is_pinned),
    KEY idx_kb_status (kb_status),
    KEY idx_update_time (update_time),
    KEY idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资料库文件';
```

#### `doc_library_tag` — 资料库标签表

```sql
CREATE TABLE IF NOT EXISTS doc_library_tag (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(64)  NOT NULL COMMENT '标签名',
    color       VARCHAR(16)  DEFAULT NULL COMMENT '颜色',
    deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资料库标签';
```

#### `doc_library_file_tag` — 文件-标签关联表

```sql
CREATE TABLE IF NOT EXISTS doc_library_file_tag (
    id      BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    file_id BIGINT NOT NULL COMMENT '文件 ID',
    tag_id  BIGINT NOT NULL COMMENT '标签 ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_file_tag (file_id, tag_id),
    KEY idx_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资料库文件-标签关联';
```

### 3.2 实体关系图

```
doc_library_folder (1) ──< (N) doc_library_file
                               │
                               │ N:N
                               │
                          doc_library_tag
                               │
                               │ (via doc_library_file_tag)
                               │
                          doc_library_file_tag
```

---

## 四、系统架构设计

### 4.1 模块定位

资料库作为一个独立业务模块，与现有 Notebook、Ecommerce、Pomodoro 等模块平行。

```
admin-backend/
  ├── admin-common/          # 公共层（已有）
  ├── admin-framework/       # 框架层（已有）
  ├── admin-system/
  │   ├── controller/
  │   │   ├── DocLibraryFolderController.java   # 文件夹接口
  │   │   ├── DocLibraryFileController.java      # 文件接口
  │   │   ├── DocLibraryTagController.java       # 标签接口
  │   │   ├── DocLibraryUploadController.java    # 上传接口
  │   │   ├── DocLibraryEventController.java     # 埋点事件接收接口
  │   │   └── DocLibraryKbController.java        # 知识库接口（标记/查询/重试）
  │   ├── domain/
  │   │   ├── entity/
  │   │   │   ├── DocLibraryFolder.java
  │   │   │   ├── DocLibraryFile.java
  │   │   │   ├── DocLibraryTag.java
  │   │   │   └── DocLibraryFileTag.java
  │   │   ├── dto/
  │   │   │   ├── DocLibraryFolderCreateRequest.java
  │   │   │   ├── DocLibraryFolderUpdateRequest.java
  │   │   │   ├── DocLibraryFileMoveRequest.java
  │   │   │   ├── DocLibraryFileRenameRequest.java
  │   │   │   ├── DocLibraryFileBatchMoveRequest.java
  │   │   │   ├── DocLibraryTagSaveRequest.java
  │   │   │   └── DocLibrarySearchRequest.java
  │   │   ├── vo/
  │   │   │   ├── DocLibraryTreeVO.java          # 树节点 VO
  │   │   │   ├── DocLibraryFileVO.java           # 文件列表 VO
  │   │   │   ├── DocLibraryFileDetailVO.java     # 文件详情 VO
  │   │   │   ├── DocLibraryTagVO.java            # 标签 VO
  │   │   │   └── DocLibraryStatsVO.java          # 统计 VO
  │   │   └── enums/
  │   │       └── DocLibrarySortField.java        # 排序字段枚举
  │   ├── service/
  │   │   ├── DocLibraryFolderService.java
  │   │   ├── DocLibraryFileService.java
  │   │   ├── DocLibraryTagService.java
  │   │   └── impl/
  │   │       ├── DocLibraryFolderServiceImpl.java
  │   │       ├── DocLibraryFileServiceImpl.java
  │   │       └── DocLibraryTagServiceImpl.java
  │   ├── mapper/
  │   │   ├── DocLibraryFolderMapper.java
  │   │   ├── DocLibraryFileMapper.java
  │   │   ├── DocLibraryTagMapper.java
  │   │   └── DocLibraryFileTagMapper.java
  │   └── storage/           # 文件存储策略（复用笔记本存储模式）
  │       ├── LibraryFileStorage.java             # 文件存储抽象接口
  │       ├── LocalLibraryFileStorage.java        # 本地文件存储
  │       └── BaiduPanLibraryFileStorage.java     # 百度网盘存储（可选）
  └── admin-server/          # 启动层（无需修改）
```

### 4.2 前端模块结构

```
admin-web/
  ├── src/
  │   ├── api/
  │   │   └── library/
  │   │       ├── index.ts              # API 统一导出
  │   │       ├── folder.ts             # 文件夹 API
  │   │       ├── file.ts               # 文件 API
  │   │       ├── tag.ts                # 标签 API
  │   │       └── types.ts              # TypeScript 类型定义
  │   ├── views/
  │   │   └── library/
  │   │       ├── LibraryView.vue        # 资料库主页面（布局容器）
  │   │       ├── LibrarySidebar.vue     # 左侧树形目录面板
  │   │       ├── LibraryContent.vue     # 中间内容区（文件列表）
  │   │       ├── LibraryFileCard.vue    # 文件卡片（网格视图）
  │   │       ├── LibraryFileRow.vue     # 文件行（列表视图）
  │   │       ├── LibraryToolbar.vue     # 顶部工具栏（搜索/视图切换/排序）
  │   │       ├── LibraryUploader.vue    # 上传面板（拖拽区域）
  │   │   ├── LibraryPreview.vue     # 文件预览弹窗/侧栏
  │   │   ├── LibraryDetailPanel.vue # 文件详情侧栏
  │   │   ├── LibraryTagManager.vue  # 标签管理弹窗
  │   │   ├── LibraryTrashView.vue   # 回收站视图
  │   │   ├── LibraryKnowledgeView.vue # 知识库视图（kb_status=READY 文件展示 + 状态管理）
  │   │   └── LibraryContextMenu.vue # 右键菜单
  │   └── stores/
  │       └── library.ts                # 资料库状态管理（Pinia）
  ├── public/
  │   └── icons/
  │       └── nav/
  │           └── library.svg           # 资料库导航图标
  └── src/
      └── router/
          └── index.ts                  # 添加 /library 路由
```

### 4.3 RESTful API 设计

#### 文件夹 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/library/tree` | 获取完整目录树 |
| POST | `/api/library/folders` | 创建文件夹 |
| PUT | `/api/library/folders/{id}` | 更新文件夹（重命名/移动） |
| DELETE | `/api/library/folders/{id}` | 删除文件夹 |

#### 文件 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/library/files?folderId=&page=&size=&sort=&order=` | 分页查询文件列表 |
| GET | `/api/library/files/{id}` | 获取文件详情 |
| GET | `/api/library/files/{id}/download` | 下载文件（返回文件流） |
| GET | `/api/library/files/{id}/preview` | 获取文件预览内容（文本/图片base64） |
| PUT | `/api/library/files/{id}/rename` | 重命名文件 |
| PUT | `/api/library/files/{id}/move` | 移动文件 |
| PUT | `/api/library/files/{id}/pin` | 切换收藏状态 |
| PUT | `/api/library/files/{id}/description` | 更新文件描述 |
| DELETE | `/api/library/files/{id}` | 删除文件（移入回收站） |
| POST | `/api/library/files/batch/move` | 批量移动 |
| POST | `/api/library/files/batch/delete` | 批量删除 |
| POST | `/api/library/files/batch/download` | 批量打包下载 |

#### 上传 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/library/upload` | 单文件上传 |
| POST | `/api/library/upload/batch` | 批量上传 |
| POST | `/api/library/upload/image` | 上传图片（自动生成缩略图） |

#### 标签 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/library/tags` | 获取全部标签 |
| POST | `/api/library/tags` | 创建标签 |
| PUT | `/api/library/tags/{id}` | 更新标签 |
| DELETE | `/api/library/tags/{id}` | 删除标签 |
| POST | `/api/library/files/{id}/tags` | 设置文件标签 |
| DELETE | `/api/library/files/{id}/tags/{tagId}` | 移除文件的某个标签 |

#### 搜索 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/library/search?keyword=&type=&tagId=&dateFrom=&dateTo=` | 搜索文件 |
| GET | `/api/library/recent?limit=` | 最近操作文件 |
| GET | `/api/library/favorites` | 收藏文件列表 |

#### 回收站 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/library/trash` | 回收站列表 |
| POST | `/api/library/trash/{id}/restore` | 恢复文件 |
| DELETE | `/api/library/trash/{id}` | 永久删除 |
| DELETE | `/api/library/trash` | 清空回收站 |

#### 统计 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/library/stats` | 总体统计（总文件数/总大小/按类型分布） |
| GET | `/api/library/folders/{id}/stats` | 文件夹统计 |

#### 埋点事件 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/library/events` | 批量上报埋点事件（内部接口） |

#### 知识库 API

| 方法 | 路径 | 说明 |
|------|------|------|
| PUT | `/api/library/files/{id}/kb-status` | 切换文件知识库标记状态 |
| GET | `/api/library/kb-files` | 获取已纳入知识库的文件列表（kb_status=READY） |
| GET | `/api/library/kb-stats` | 知识库统计（总数/按类型分布/处理状态分布） |

---

## 五、前端核心交互流程

### 5.1 布局结构

```
┌──────────────────────────────────────────────────────────┐
│  LibraryView (资料库)                                      │
│  ┌──────────┬───────────────────────────────────────────┐ │
│  │          │  LibraryToolbar                            │ │
│  │ Sidebar  │  [搜索框] [上传按钮] [视图切换] [排序]       │ │
│  │ (树目录)  ├───────────────────────────────────────────┤ │
│  │          │  LibraryContent                            │ │
│  │  📁 全部  │  ┌────┐ ┌────┐ ┌────┐                    │ │
│  │  📁 图片  │  │文件1│ │文件2│ │文件3│   (网格视图)      │ │
│  │  📁 文档  │  └────┘ └────┘ └────┘                    │ │
│  │  📁 设计  │  ─── 或 ───                               │ │
│  │          │  ├───────┬──────┬──────┬──────┤           │ │
│  │          │  │ 名称   │ 大小  │ 类型  │ 时间  │ (列表视图)│ │
│  │          │  ├───────┼──────┼──────┼──────┤           │ │
│  │          └───────────────────────────────────────────┘ │
│  └──────────┴─────────────────────────────────────────────┘
└──────────────────────────────────────────────────────────┘
```

### 5.2 用户操作流程

#### 上传文件流程
1. 用户点击"上传"按钮或拖拽文件到内容区
2. LibraryUploader 弹出，支持选择多个文件
3. 文件上传过程中显示进度条
4. 上传完成后自动刷新当前目录文件列表
5. 上传失败的文件显示错误提示

#### 文件预览流程
1. 用户双击文件卡片/行
2. 根据文件类型判断预览方式：
   - 图片 → 弹窗 Lightbox 大图浏览
   - PDF → 内嵌 PDF.js 渲染
   - 文本 → 读取内容后渲染为格式化文本
   - 其他 → 提示"无法预览"，提供下载按钮
3. 预览弹窗支持键盘快捷键（← → 切换、Esc 关闭）

#### 文件管理流程
1. 右键菜单提供：下载/重命名/移动/删除/收藏/打标签
2. 重命名采用行内编辑模式（点击文件名直接修改）
3. 移动采用弹出文件夹树选择器
4. 删除确认弹窗，确认后移入回收站

---

## 六、埋点设计

### 6.1 设计原则

- **轻量无侵入**：不引入第三方分析 SDK，自建轻量事件记录体系
- **业务驱动**：只采集对产品优化和知识库运营有实际意义的事件
- **隐私合规**：不采集用户身份标识之外的隐私信息，不上报文件内容
- **可扩展**：事件格式统一，后续可平滑对接自建分析平台或第三方（如 Umami）

### 6.2 事件类型定义

所有埋点事件统一格式：

```typescript
interface LibraryEvent {
  event: string          // 事件名称
  params: Record<string, string | number | boolean>  // 事件参数
  timestamp: number     // 事件发生时间戳
}
```

### 6.3 事件清单

#### 6.3.1 文件操作事件

| 事件名称 | 触发时机 | 参数 | 用途 |
|---------|---------|------|------|
| `file_upload` | 文件上传成功 | `{ count, totalSize, extensions }` | 了解用户上传习惯和文件类型分布 |
| `file_upload_failed` | 文件上传失败 | `{ fileName, errorType, fileSize }` | 监控上传质量，排查存储问题 |
| `file_download` | 单文件下载 | `{ fileId, extension, fileSize }` | 统计文件热度 |
| `file_batch_download` | 批量打包下载 | `{ count, totalSize, extensions[] }` | 了解批量下载场景使用率 |
| `file_preview` | 打开文件预览 | `{ fileId, extension, previewType }` | 统计预览频率，优化预览策略 |
| `file_rename` | 重命名文件 | `{ fileId }` | 了解文件重命名频率 |
| `file_move` | 移动文件到其他文件夹 | `{ fileId, fromFolderId, toFolderId }` | 分析目录结构调整行为 |
| `file_delete` | 删除文件（移入回收站） | `{ fileId, extension }` | 追踪删除行为模式 |
| `file_restore` | 从回收站恢复 | `{ fileId }` | 了解误删率 |
| `file_purge` | 永久删除 | `{ fileId, extension }` | 监控永久删除量级 |
| `file_pin` | 收藏/取消收藏 | `{ fileId, pinned: boolean }` | 了解收藏功能使用情况 |
| `file_tag` | 添加/移除标签 | `{ fileId, tagId, tagName, action: 'add'\|'remove' }` | 了解标签使用习惯 |

#### 6.3.2 文件夹操作事件

| 事件名称 | 触发时机 | 参数 | 用途 |
|---------|---------|------|------|
| `folder_create` | 创建文件夹 | `{ parentId, depth }` | 了解目录树结构增长 |
| `folder_rename` | 重命名文件夹 | `{ folderId }` | 了解重命名频率 |
| `folder_delete` | 删除文件夹 | `{ folderId, fileCount, depth }` | 追踪级联删除规模 |

#### 6.3.3 浏览与搜索事件

| 事件名称 | 触发时机 | 参数 | 用途 |
|---------|---------|------|------|
| `folder_open` | 点击进入文件夹 | `{ folderId, depth, childCount }` | 分析目录浏览热区 |
| `search_execute` | 执行搜索 | `{ keyword, hasResult, resultCount }` | 了解搜索需求分布 |
| `view_mode_switch` | 切换视图模式 | `{ fromMode, toMode }` | 了解用户视图偏好 |
| `sort_change` | 切换排序方式 | `{ field, order }` | 了解用户排序偏好 |

#### 6.3.4 知识库相关事件

| 事件名称 | 触发时机 | 参数 | 用途 |
|---------|---------|------|------|
| `kb_file_mark` | 标记/取消标记文件入知识库 | `{ fileId, extension, marked: boolean }` | 了解用户将哪些文件纳入知识库 |
| `kb_file_processing` | 知识库解析开始 | `{ fileId, extension, fileSize }` | 监控知识库处理负载 |
| `kb_file_ready` | 知识库解析完成 | `{ fileId, extension, chunkCount }` | 统计知识库文档量级 |
| `kb_file_failed` | 知识库解析失败 | `{ fileId, extension, errorType }` | 监控解析失败率和失败原因 |

### 6.4 数据存储方案

**第一期**：埋点数据直接写入数据库 `doc_library_event_log` 表，轻量够用

```sql
CREATE TABLE IF NOT EXISTS doc_library_event_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    event       VARCHAR(64)  NOT NULL COMMENT '事件名称',
    file_id     BIGINT       DEFAULT NULL COMMENT '关联文件 ID',
    folder_id   BIGINT       DEFAULT NULL COMMENT '关联文件夹 ID',
    params_json JSON         DEFAULT NULL COMMENT '事件参数（JSON）',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '事件发生时间',
    PRIMARY KEY (id),
    KEY idx_event (event),
    KEY idx_file_id (file_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资料库事件日志';
```

**后续演进**：当数据量增长后，可迁移到独立的时序存储或接入第三方分析平台。

### 6.5 前端埋点架构

```
LibraryEventTracker (单例)
  ├── track(event, params)      → 通用事件上报
  ├── trackFileAction(...)       → 文件操作快捷方法
  ├── trackFolderAction(...)     → 文件夹操作快捷方法
  ├── trackSearch(...)           → 搜索事件快捷方法
  └── setBatchContext(ctx)       → 批量操作上下文（避免重复参数）
```

- 文件 `src/composables/useLibraryTracker.ts`，封装 Pinia store 调用
- 后端提供 `POST /api/library/events` 批量接收埋点事件
- 前端可合并多个事件后定时批量提交（每 30s 或累计 20 条），减少请求数

### 6.6 埋点数据应用场景

| 用途 | 说明 | 依赖事件 |
|------|------|---------|
| **文件热力图** | 统计哪些文件被预览/下载最多，调整内容策略 | `file_preview`, `file_download` |
| **搜索趋势分析** | 分析用户搜索关键词分布，优化搜索算法 | `search_execute` |
| **目录结构优化** | 了解哪些目录层级最活跃，辅助用户快速导航 | `folder_open` |
| **知识库质量监控** | 追踪知识库解析成功率、文档类型分布 | `kb_file_*` |
| **功能使用率** | 衡量各功能模块的采用率（标签、收藏、批量操作等） | 所有事件聚合 |

---

## 七、知识库集成设计

### 7.1 定位与目标

```
资料库 (Library)          AI 知识库 (aiKnowledge)
    │                             │
    │  文件存储与管理              │  知识检索与问答
    │  ┌─────────────────┐        │  ┌─────────────────┐
    │  │ 图片 / PDF / 文档│───────┼──→│ 向量化索引        │
    │  │ TXT / MD / 代码 │        │  │ 语义搜索          │
    │  │ Office / 视频   │        │  │ AI 问答           │
    │  └─────────────────┘        │  └─────────────────┘
    │                             │
    └── 提供原始数据源 ────────────┘
```

**核心思路**：资料库负责文件的**存储、管理和预处理**。当用户将文件标记为"纳入知识库"后，系统对文件进行解析、分块、向量化，将处理后的数据交给 AI 知识库模块做检索和问答。两个模块职责分离，通过 **kb_status 状态机** 和 **统一的数据管道** 衔接。

### 7.2 知识库状态机

```
                        ┌──────────┐
                        │  NONE    │ ← 用户未标记，或不支持的文件
                        └────┬─────┘
                             │ 用户标记「纳入知识库」
                             ▼
                        ┌──────────┐
                        │  PENDING  │ ← 待处理，进入队列
                        └────┬─────┘
                             │ 调度器消费
                             ▼
                        ┌────────────┐
                        │ PROCESSING │ ← 正在解析、分块、向量化
                        └────┬─────┬─┘
                             │     │
                   成功      │     │  失败
                             ▼     ▼
                        ┌───────┐ ┌────────┐
                        │ READY │ │ FAILED │ ← 可重试
                        └───────┘ └────────┘
                             │
                             │ 用户取消标记 / 文件更新
                             ▼
                        ┌──────────┐
                        │  NONE    │ ← 重新开始
                        └──────────┘
```

### 7.3 文件→知识库处理流程

```
用户标记文件纳入知识库
         │
         ▼
┌─────────────────────┐
│ 1. 文件类型判断       │
│    - 可解析文本类     │
│      (PDF/TXT/MD/    │
│       Office/代码等)  │
│    - 不可解析类       │
│      (图片/视频/      │
│       压缩包等)       │
└─────────┬───────────┘
          │
    ┌─────┴─────┐
    │           │
    可解析      不可解析
    │           │
    ▼           ▼
┌────────┐  ┌──────────┐
│ 2. 解析 │  │ 提取元数据  │
│    PDF  │  │ - OCR文本  │← 图片类使用OCR
│    文本 │  │ - 描述/标签│
│    Office│  │ - 文件名  │
│    Markdown│  └────┬─────┘
└───┬────┘       │
    │            │
    ▼            │
┌────────┐      │
│ 3. 分块 │      │
│    - 按段落     │
│    - 按章节     │
│    - 按语义     │
│    - 重叠窗口   │
└───┬────┘      │
    │            │
    ▼            ▼
┌────────────────────┐
│ 4. 向量化          │
│    - Embedding API │
│    - 存入向量数据库│
│    - 关联原始文件ID│
└────────────────────┘
         │
         ▼
┌────────────────────┐
│ 5. 更新 kb_status  │
│    = READY         │
│    记录 chunk 数量  │
└────────────────────┘
```

### 7.4 文件类型与知识库支持

| 文件类型 | 支持知识库 | 处理方式 | 优先级 |
|---------|-----------|---------|-------|
| PDF | ✅ | 文本提取 + 分块 | P0 |
| TXT / MD | ✅ | 直接读取 + 分块 | P0 |
| Office (DOCX/XLSX/PPTX) | ✅ | Apache POI 解析 + 分块 | P1 |
| 代码文件 (.java/.py/.ts 等) | ✅ | 按行/函数分块 | P1 |
| HTML | ✅ | 去除标签提取纯文本 | P1 |
| 图片 (JPG/PNG/SVG) | ✅ (需 OCR) | OCR 提取文字，或人工标注描述 | P2 |
| 视频 / 音频 | ⚠️ | 暂不支持，仅保存元数据 | P3 |
| 压缩包 (ZIP/RAR) | ❌ | 不纳入知识库 | — |
| 可执行文件 (.exe/.msi) | ❌ | 禁止纳入知识库 | — |

### 7.5 数据结构扩展

#### doc_library_file 新增知识库字段

已在 `doc_library_file` 表中预埋了以下字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `kb_status` | VARCHAR(16) | `NONE` / `PENDING` / `PROCESSING` / `READY` / `FAILED` |
| `kb_error` | VARCHAR(512) | 处理失败原因 |
| `kb_processed_at` | DATETIME | 处理完成时间戳 |

#### 知识库解析块表（后续 aiKnowledge 模块实现）

```sql
CREATE TABLE IF NOT EXISTS ai_knowledge_chunk (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    source_file_id  BIGINT       NOT NULL COMMENT '来源资料库文件 ID',
    chunk_index     INT          NOT NULL COMMENT '块序号',
    content         TEXT         NOT NULL COMMENT '文本内容',
    content_hash    CHAR(64)     DEFAULT NULL COMMENT '内容 SHA-256',
    token_count     INT          DEFAULT 0 COMMENT 'Token 数量估算',
    embedding_id    VARCHAR(128) DEFAULT NULL COMMENT '向量 ID（外部向量库）',
    metadata_json   JSON         DEFAULT NULL COMMENT '元数据（来源页码/章节等）',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_source_file_id (source_file_id),
    KEY idx_embedding_id (embedding_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 知识库文本块';
```

### 7.6 与现有 aiKnowledge 模块的集成

项目前端已预设了 `aiKnowledge` 的入口和数据配置：

| 集成点 | 现有状态 | 联动方案 |
|--------|---------|---------|
| `function-items.ts` | 已定义 `aiKnowledge` 功能项 | 资料库实现后，补充 `route` 字段指向 `/ai-knowledge` |
| `module-visuals.ts` | 已配置图标和颜色 | 不变 |
| `HomeView.vue` | 已在轮播和聚焦模块中展示 | 点击时跳转到资料库 → 知识库标签页 |
| i18n 翻译 | 中英文均已配置 | 不变 |

**集成流程**：

```
资料库文件列表
  │
  ├── [右键菜单 / 操作栏] "纳入知识库"
  │     └── 更新 kb_status = PENDING
  │
  ├── 知识库标签页（LibraryView 内嵌 tab）
  │     └── 专门展示 kb_status = READY 的文件
  │
  └── AI 知识库独立页面 (/ai-knowledge)
        └── 汇总来自资料库 + 笔记本的知识库文档
            └── 提供语义搜索 / AI 问答界面
```

### 7.7 后端任务调度

知识库解析为异步任务，不阻塞用户操作：

```java
// 解析任务调度器
@Component
public class KnowledgeBaseProcessor {
    
    @Scheduled(fixedDelay = 30_000)  // 每 30 秒轮询一次
    public void processPendingFiles() {
        // 1. 查询 kb_status = PENDING 的文件，取前 5 个
        // 2. 更新状态为 PROCESSING
        // 3. 根据文件类型调用不同的解析器
        //    - PdfParser / TextParser / OfficeParser ...
        // 4. 解析后分块，调用 Embedding API 向量化
        // 5. 存储到 ai_knowledge_chunk 表
        // 6. 更新 kb_status = READY
        //    - 失败则更新为 FAILED，记录 kb_error
    }
}
```

### 7.8 知识库数据流架构

```
┌──────────────┐    ┌─────────────────┐    ┌──────────────────┐
│   用户操作    │    │   资料库服务      │    │   知识库管道      │
│              │    │                 │    │                  │
│ 上传文件      │───→│ 保存文件 + 元数据 │───→│ kb_status 初始   │
│ 标记知识库    │───→│ kb_status=      │    │ = PENDING       │
│              │    │ PENDING         │    │                  │
└──────────────┘    └─────────────────┘    └────────┬─────────┘
                                                     │
                                                     ▼
                                            ┌──────────────────┐
                                            │   解析引擎        │
                                            │                  │
                                            │ PDF 解析器        │
                                            │ 文本解析器        │
                                            │ Office 解析器     │
                                            │ 代码解析器        │
                                            └────────┬─────────┘
                                                     │
                                                     ▼
                                            ┌──────────────────┐
                                            │   嵌入向量化      │
                                            │                  │
                                            │ Embedding API    │
                                            │ 向量数据库写入    │
                                            └────────┬─────────┘
                                                     │
                                                     ▼
                                            ┌──────────────────┐
                                            │   知识库检索      │
                                            │   (aiKnowledge)  │
                                            │                  │
                                            │ 语义搜索          │
                                            │ AI 问答          │
                                            │ 文档关联推荐      │
                                            └──────────────────┘
```

---

## 八、技术实现方案

### 6.1 文件上传处理

1. 前端使用 `multipart/form-data` 上传
2. 后端使用 Spring `MultipartFile` 接收
3. 文件存储路径规则：`library/{yyyy/MM/dd}/{uuid}.{ext}`
4. 图片文件自动生成缩略图（`thumbnail_` 前缀，最大 300px 宽）
5. 文件元信息通过 `@RequestPart` 与文件同时提交

### 8.1 文件上传处理

### 8.2 文件预览实现

- **图片预览**：后端直接返回文件流或缩略图，前端 `<img>` 渲染 + 点击放大
- **PDF 预览**：后端返回文件流，前端使用 `<iframe>` 或 `pdf.js` 库渲染
- **文本预览**：后端读取文件内容为 UTF-8 字符串返回，前端 `<pre>` 渲染
- **大文件预览**：支持 Range 请求头，分片加载

### 8.3 文件下载处理

- 单文件下载：后端返回 `Content-Disposition: attachment`
- 批量下载：后端使用 `ZipOutputStream` 实时打包为 ZIP
- 大文件断点续传：支持 `Accept-Ranges: bytes`

### 8.4 搜索实现

- **P0**：文件名 LIKE 模糊搜索 + 文件类型 + 标签筛选（SQL 级）
- **P2**：引入 Elasticsearch 或数据库全文索引提升搜索体验

### 8.5 回收站策略

- 每次软删除记录 `deleted` 标记 + `deleted_at` 时间戳
- 回收站文件保留 30 天后自动永久清理（定时 Job）
- `DocLibraryFile` 表增加 `deleted_at` 和 `original_folder_id` 字段（可选）

---

## 七、国际化与配置

### 7.1 i18n 键命名规范

遵循现有模块风格：

```json
{
  "library": {
    "title": "资料库",
    "upload": "上传",
    "download": "下载",
    "rename": "重命名",
    "move": "移动",
    "delete": "删除",
    "restore": "恢复",
    "purge": "永久删除",
    "favorite": "收藏",
    "tag": "标签",
    "search": "搜索文件",
    "folderCreate": "新建文件夹",
    "uploadFile": "上传文件",
    "viewGrid": "网格视图",
    "viewList": "列表视图",
    "sortName": "按名称排序",
    "sortSize": "按大小排序",
    "sortTime": "按修改时间排序",
    "folderEmpty": "文件夹为空",
    "trashTitle": "回收站",
    "totalCount": "共 {count} 个文件",
    "totalSize": "占用 {size}",
    "confirmDelete": "确定删除该文件？",
    "confirmPurge": "确定永久删除？此操作不可恢复。",
    "uploadSuccess": "上传成功",
    "fileType_image": "图片",
    "fileType_document": "文档",
    "fileType_archive": "压缩包",
    "fileType_video": "视频",
    "fileType_other": "其他",
    "kbTab": "知识库",
    "kbMark": "纳入知识库",
    "kbUnmark": "移出知识库",
    "kbStatus_none": "未纳入",
    "kbStatus_pending": "处理中",
    "kbStatus_processing": "解析中",
    "kbStatus_ready": "已就绪",
    "kbStatus_failed": "处理失败",
    "kbRetry": "重新处理",
    "kbFileCount": "已纳入 {count} 个文件",
    "kbStatsTitle": "知识库概览",
    "kbChunkCount": "已分块 {count} 段"
  }
}
```

### 7.2 文件类型分类

```typescript
enum DocFileCategory {
  IMAGE = 'image',
  DOCUMENT = 'document',     // PDF, Office
  ARCHIVE = 'archive',       // ZIP, RAR, 7Z
  VIDEO = 'video',
  AUDIO = 'audio',
  CODE = 'code',
  OTHER = 'other',
}
```

---

## 八、路由与导航

### 8.1 路由配置

```typescript
{
  path: '/library',
  name: 'library',
  component: () => import('@/views/library/LibraryView.vue'),
  meta: { titleKey: 'library.title', icon: 'library' }
}

// 子路由
{
  path: '/library/trash',
  name: 'library-trash',
  component: () => import('@/views/library/LibraryTrashView.vue'),
  meta: { titleKey: 'library.trashTitle' }
}
{
  path: '/library/knowledge',
  name: 'library-knowledge',
  component: () => import('@/views/library/LibraryKnowledgeView.vue'),
  meta: { titleKey: 'library.kbTab' }
}
```

### 8.2 导航栏集成

- 在功能列表页 (FunctionListView) 添加资料库入口
- 在 PC 端左侧导航菜单添加资料库链接
- sidebar 导航图标使用已有的 `library.svg`

---

## 九、分期实施计划

### 第一期（核心功能 — P0）

| 编号 | 功能 | 估算工时 |
|------|------|---------|
| 1.1 | 数据库建表 + 实体/Mapper | 1天 |
| 1.2 | 文件夹 CRUD + 树形接口 | 1天 |
| 1.3 | 文件上传 + 文件列表接口 + 文件元数据管理 | 2天 |
| 1.4 | 文件删除 + 回收站 | 1天 |
| 1.5 | 前端 Layout + 左侧树 + 右侧文件列表 + 拖拽上传 | 2天 |
| 1.6 | 网格/列表视图切换 + 图片预览 | 1天 |
| 1.7 | 路由集成 + 导航入口 | 0.5天 |
| 1.8 | 埋点基础设施（事件表 + 前端 tracker + 后端事件接收接口） | 1天 |
| 1.9 | 文件操作/浏览核心事件埋点（上传/下载/预览/删除/文件夹操作） | 0.5天 |
| | **合计** | **10天** |

### 第二期（增强功能 — P1）

| 编号 | 功能 | 估算工时 |
|------|------|---------|
| 2.1 | 标签系统（CRUD + 文件关联 + 筛选） | 1.5天 |
| 2.2 | 收藏功能 + 收藏夹视图 + 最近文档 | 1天 |
| 2.3 | 文件搜索（文件名+标签+类型+日期） | 1天 |
| 2.4 | 文件移动/复制 + 批量操作 | 1天 |
| 2.5 | 文件详情侧栏 + 统计信息 | 1天 |
| 2.6 | 批量下载（ZIP打包） | 1天 |
| 2.7 | 右键菜单 + 键盘快捷键 | 0.5天 |
| 2.8 | 国际化 + 深色模式适配 | 0.5天 |
| 2.9 | 知识库基础能力（kb_status 标记/切换/API/文件列表页知识库标签页） | 1.5天 |
| 2.10 | 知识库搜索/埋点事件 + view_count / download_count 自增 | 0.5天 |
| | **合计** | **9.5天** |

### 第三期（高级功能 — P2）

| 编号 | 功能 | 估算工时 |
|------|------|---------|
| 3.1 | PDF 预览 + 文本预览 | 1天 |
| 3.2 | Office 文档预览 | 2天 |
| 3.3 | 百度网盘存储策略 | 1.5天 |
| 3.4 | 文件版本管理 | 2天 |
| 3.5 | 数据统计图表 + 埋点分析看板 | 1.5天 |
| 3.6 | 知识库异步解析引擎（PDF/文本/Office解析 + 分块） | 3天 |
| 3.7 | 知识库 Embedding 向量化 + 向量数据库接入 | 2天 |
| 3.8 | 知识库语义搜索 / AI 问答界面（aiKnowledge 模块） | 3天 |
| | **合计** | **16天** |

---

## 十、设计约束与注意事项

### 10.1 遵循的规范

1. **命名规范**：数据库表前缀 `doc_library_`，Java 类前缀 `DocLibrary`，前端目录 `library`
2. **代码风格**：与现有 Notebook 模块保持一致，不加多余注释
3. **API 风格**：RESTful，统一返回 `ApiResult<T>` 格式
4. **权限控制**：复用已有的 `SysUser` 鉴权体系
5. **错误处理**：使用 `BusinessException` + 全局异常处理器

### 10.2 安全考虑

1. 上传文件校验扩展名和 MIME 类型，防止恶意文件上传
2. 文件路径不能包含 `..` 等路径穿越字符
3. 下载文件时验证用户权限
4. 敏感文档类型（如 `.exe`）默认禁止上传
5. 上传文件大小限制（建议 50MB 单文件）

### 10.3 与现有系统的集成

- 复用 `admin-common` 中的 `ApiResult` / `BusinessException`
- 复用 `admin-framework` 中的 MyBatis-Plus / Jackson / 跨域配置
- 复用前端的 `request.ts` / Element Plus 组件 / Pinia 状态管理
- 与 Notebook 模块共享百度网盘存储基础设施（可选）
