# 作战屏图标目录

所有作战屏 SVG 图标放在此目录，构建后通过 `/icons/...` 访问。

## 目录结构

```
public/icons/
├── nav/          # 左侧导航、顶部设置齿轮
├── stats/        # 首页顶部 6 张指标卡小图标
└── modules/      # 模块大卡片、功能列表
```

## 替换方式

1. 保持**文件名不变**，直接覆盖对应 `.svg` 文件
2. 导航 / 指标卡图标建议使用**单色**（黑色描边或填充），组件通过 `currentColor` 着色
3. 模块图标可使用彩色 SVG，通过 `<img>` 加载

## 本地验证

```bash
cd admin-web
npm run dev
# 浏览器访问 http://127.0.0.1:5173/icons/nav/home.svg 确认文件可访问
```

## 部署

执行 `npm run build` 后，`icons/` 会原样复制到 `dist/icons/`。
