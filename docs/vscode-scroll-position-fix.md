# VS Code 滚动位置保持配置方案

## 问题描述

当从 Markdown 文件预览切换到其他文件，再切换回来时，Markdown 文件会自动滚动到顶部，需要重新定位到之前阅读的位置。

## 解决方案

### 方法一：通过设置界面配置（推荐）

1. 打开 VS Code 设置界面
   - Windows/Linux: `Ctrl + ,`
   - macOS: `Cmd + ,`

2. 在搜索框中输入 `restoreScrollPosition`

3. 勾选 **Editor: Restore Scroll Position** 选项

4. 继续搜索 `scrollEditorWithPreview`

5. 勾选以下选项：
   - **Markdown: Preview: Scroll Editor With Preview**
   - **Markdown: Preview: Scroll Preview With Editor**

### 方法二：手动编辑 settings.json

1. 打开命令面板
   - Windows/Linux: `Ctrl + Shift + P`
   - macOS: `Cmd + Shift + P`

2. 输入 `Preferences: Open Settings (JSON)` 并回车

3. 在 settings.json 中添加以下配置：

```json
{
  "editor.restoreScrollPosition": true,
  "markdown.preview.scrollEditorWithPreview": true,
  "markdown.preview.scrollPreviewWithEditor": true
}
```

### 方法三：使用书签功能

即使配置了滚动位置恢复，在某些情况下仍可能失效。可以使用书签作为备用方案：

- **添加/移除书签**: `Ctrl + Shift + K`（Windows/Linux）或 `Cmd + Shift + K`（macOS）
- **显示所有书签**: `Ctrl + K Ctrl + J`
- **跳转到下一个书签**: `Ctrl + K Ctrl + N`
- **跳转到上一个书签**: `Ctrl + K Ctrl + P`

### 方法四：使用大纲视图快速导航

1. 打开大纲视图：`Ctrl + Shift + O`（Windows/Linux）或 `Cmd + Shift + O`（macOS）

2. 在大纲视图中点击章节标题即可快速跳转

### 方法五：安装扩展增强体验

推荐安装以下扩展：

1. **Markdown All in One**
   - 提供更好的 Markdown 支持
   - 支持目录生成
   - 支持表格格式化

2. **Markdown Preview Enhanced**
   - 增强预览功能
   - 支持滚动同步
   - 支持导出为 PDF

## 配置说明

| 配置项 | 作用 |
|--------|------|
| `editor.restoreScrollPosition` | 在文件切换时保持编辑器滚动位置 |
| `markdown.preview.scrollEditorWithPreview` | 编辑器滚动时同步预览窗口 |
| `markdown.preview.scrollPreviewWithEditor` | 预览窗口滚动时同步编辑器 |

## 注意事项

1. 这些设置会应用到所有打开的工作区
2. 如果使用远程开发（Remote-SSH/WSL），需要在远程环境中单独配置
3. 某些扩展可能会影响滚动行为，如果配置后仍有问题，可以尝试禁用其他扩展测试

## 键盘快捷键参考

| 快捷键 | 功能 |
|--------|------|
| `Ctrl + Home` | 跳转到文档开头 |
| `Ctrl + End` | 跳转到文档结尾 |
| `Ctrl + G` | 跳转到指定行号 |
| `Ctrl + F` | 搜索内容 |

## 总结

通过配置 `editor.restoreScrollPosition` 和 Markdown 预览滚动同步选项，可以有效解决文件切换时滚动位置丢失的问题。结合书签和大纲视图功能，可以进一步提升文档阅读体验。