import { marked } from 'marked'

/** 标题级别 → 打印时添加的图标前缀 */
const HEADING_ICONS: Record<number, string> = { 1: '🗂️ ', 2: '📌 ', 3: '🔸 ', 4: '💡 ' }

/**
 * 将 AI 回答的 Markdown 转换为打印样式 HTML：
 * 标题加图标前缀、关键标签注入内联打印样式、可选的标题/日期 meta 头。
 */
export function formatPdfBody(md: string, options?: { title?: string; date?: string }): string {
  let html = marked.parse(md) as string

  // 为不同标题级别添加图标前缀
  for (let i = 1; i <= 4; i++) {
    if (HEADING_ICONS[i]) {
      html = html.replace(new RegExp(`<h${i}(\\s[^>]*)?>`, 'g'), `<h${i}$1>${HEADING_ICONS[i]}`)
    }
  }

  // 为打印 PDF 添加额外样式
  html = html
    .replace(/<h1\b/g, '<h1 style="color:#991b1b;font-size:21px;font-weight:700;border-bottom:2px solid #fca5a5;padding-bottom:8px"')
    .replace(/<h2\b/g, '<h2 style="color:#c2410c;font-size:18px;font-weight:700;border-bottom:2px solid #fdba74;padding-bottom:6px"')
    .replace(/<h3\b/g, '<h3 style="color:#a16207;font-size:16px;font-weight:600"')
    .replace(/<h4\b/g, '<h4 style="color:#7c3aed;font-size:15px;font-weight:600"')
    .replace(/<strong\b(?!\sstyle)/g, '<strong style="color:#b91c1c"')
    .replace(/<em\b(?!\sstyle)/g, '<em style="color:#d97706;font-style:italic"')
    .replace(/<blockquote\b/g, '<blockquote style="margin:12px 0;padding:10px 18px;border-left:4px solid #f59e0b;background:#fffbeb;color:#92400e;border-radius:0 8px 8px 0"')
    .replace(/<pre\b(?!\sstyle)/g, '<pre style="background:#18181b;color:#e4e4e7;padding:14px 18px;border-radius:8px;overflow-x:auto;margin:12px 0;font-size:13px;line-height:1.6;border:1px solid #27272a"')
    .replace(/<table\b/g, '<table style="border-collapse:collapse;width:100%;margin:14px 0;font-size:13px;border:1px solid #e5e7eb"')
    .replace(/<th\b/g, '<th style="background:#f59e0b;color:#fff;font-weight:600;padding:10px 14px;text-align:left;border:1px solid #d97706"')
    .replace(/<td\b/g, '<td style="padding:9px 14px;border:1px solid #e5e7eb"')
    .replace(/<hr\b/g, '<hr style="border:none;height:2px;background:linear-gradient(to right,transparent,#f59e0b,transparent);margin:18px 0"')

  const metaParts: string[] = []
  if (options?.title) {
    metaParts.push(`<div style="font-size:22px;font-weight:700;color:#991b1b;padding-bottom:10px;border-bottom:3px solid #fca5a5;margin-bottom:6px">${options.title}</div>`)
  }
  if (options?.date) {
    metaParts.push(`<div style="font-size:12px;color:#9ca3af;margin-bottom:20px">📅 ${options.date}</div>`)
  }

  return `<div>${metaParts.join('\n')}</div>${html}`
}

/** 构建可打印的 HTML（用于 PDF 导出） */
export function buildPrintableHtml(md: string, title: string): string {
  const today = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' })
  const bodyHtml = formatPdfBody(md, { title, date: today })

  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <title>${title}</title>
  <style>
    @page { margin: 22mm 18mm; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Noto Sans SC", "PingFang SC", "Microsoft YaHei", sans-serif;
      font-size: 14px;
      line-height: 1.75;
      color: #1f2937;
      background: #fff;
      max-width: 800px;
      margin: 0 auto;
    }

    /* ===== 文档标题 ===== */
    .doc-title {
      font-size: 22px;
      font-weight: 700;
      color: #991b1b;
      padding-bottom: 10px;
      border-bottom: 3px solid #fca5a5;
      margin-bottom: 6px;
    }
    .doc-date {
      font-size: 12px;
      color: #9ca3af;
      margin-bottom: 20px;
    }

    /* ===== 段落 ===== */
    p { margin: 0 0 10px; line-height: 1.75; }

    /* 标题 + 图标 */
    h1, h2, h3, h4 { margin: 22px 0 10px; font-weight: 700; }
    h1 { font-size: 21px; color: #991b1b; padding-bottom: 8px; border-bottom: 2px solid #fca5a5; }
    h2 { font-size: 18px; color: #c2410c; padding-bottom: 6px; border-bottom: 2px solid #fdba74; }
    h3 { font-size: 16px; color: #a16207; }
    h4 { font-size: 15px; color: #7c3aed; }
    .hi { margin-right: 6px; font-size: 1.1em; }

    /* 文字样式 */
    strong { font-weight: 700; color: #b91c1c; }
    em { color: #d97706; font-style: italic; }
    del { color: #9ca3af; text-decoration: line-through; }
    code {
      background: #fef2f2; color: #b91c1c; padding: 2px 8px;
      border-radius: 4px; font-size: 0.85em; border: 1px solid #fecaca;
      font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace;
    }
    kbd {
      background: linear-gradient(180deg, #f9fafb, #f3f4f6);
      border: 1px solid #d1d5db; border-radius: 5px;
      padding: 1px 6px; font-size: 12px; color: #374151;
      box-shadow: 0 1px 2px rgba(0,0,0,0.06);
    }
    mark { background: #fef3c7; color: #92400e; padding: 1px 4px; border-radius: 3px; }

    /* 代码块 */
    .code-header {
      font-size: 11px;
      font-family: "SFMono-Regular", Consolas, monospace;
      color: #a1a1aa;
      background: #27272a;
      padding: 4px 14px;
      border-radius: 8px 8px 0 0;
      margin-top: 12px;
    }
    pre {
      background: #18181b; color: #e4e4e7; padding: 14px 18px;
      border-radius: 0 0 8px 8px; overflow-x: auto; margin: 0 0 12px;
      font-size: 13px; line-height: 1.6; border: 1px solid #27272a; border-top: none;
    }
    pre code { background: none; padding: 0; border: none; color: inherit; font-size: inherit; }
    pre:not(.code-header + pre) { border-radius: 8px; border-top: 1px solid #27272a; }

    /* 引用块 */
    blockquote {
      margin: 12px 0; padding: 10px 18px;
      border-left: 4px solid #f59e0b;
      background: linear-gradient(135deg, #fffbeb, #fef3c7);
      color: #92400e; border-radius: 0 8px 8px 0;
      line-height: 1.6; box-shadow: 0 1px 3px rgba(245,158,11,0.1);
    }
    blockquote p { margin: 0; }
    blockquote blockquote { margin: 8px 0; border-left-color: #f97316; background: #fff7ed; color: #9a3412; }

    /* 列表 */
    ul, ol { margin: 6px 0; padding-left: 24px; }
    li { margin: 4px 0; line-height: 1.6; }
    ul li::marker { color: #f59e0b; }
    ol li::marker { color: #f59e0b; font-weight: 600; }
    .task-list-item { list-style: none; margin-left: -24px; }
    .task-list-item input[type="checkbox"] { margin-right: 6px; }

    /* 表格 */
    table {
      border-collapse: separate; border-spacing: 0;
      width: 100%; margin: 14px 0; font-size: 13px;
      border: 1px solid #e5e7eb; border-radius: 8px; overflow: hidden;
    }
    thead { background: linear-gradient(135deg, #f59e0b, #d97706); color: #fff; }
    th { font-weight: 600; padding: 10px 14px; text-align: left; letter-spacing: 0.02em; }
    td { padding: 9px 14px; border-top: 1px solid #f3f4f6; }
    tbody tr:nth-child(even) td { background: #fafaf9; }

    /* 图片 */
    img { max-width: 100%; border-radius: 8px; margin: 10px 0; border: 1px solid #f3f4f6; }
    a { color: #2563eb; text-decoration: none; border-bottom: 1px solid #bfdbfe; }

    /* 分隔线 */
    hr { border: none; height: 2px; background: linear-gradient(to right, transparent, #f59e0b, #d97706, #f59e0b, transparent); margin: 18px 0; opacity: 0.6; }
  </style>
</head>
<body>
  ${bodyHtml}
  <script>window.onload = function () { window.print(); setTimeout(function () { window.close(); }, 200); }<\/script>
</body>
</html>`
}

/**
 * 打印能力组合函数：向组件暴露纯函数，便于单元测试与后续按需扩展。
 */
export function useAiKnowledgePrint() {
  return { formatPdfBody, buildPrintableHtml }
}
