import { describe, expect, it } from 'vitest'
import {
  hasMarkdownSyntax,
  markdownToText,
  markdownToTextSnippet,
  truncateText,
} from '@/utils/markdownToText'

describe('markdownToText', () => {
  it('表格：去掉管道符与分隔行，单元格按空格展开', () => {
    const md = [
      '| 视口 | 重构前 | 重构后 | 改善 |',
      '|---|---|---|---|',
      '| 769 | 7px | **539px** | ≈77× |',
      '| 834（iPad Air 竖屏） | 72px | **604px** | 8.4× |',
    ].join('\n')
    expect(markdownToText(md)).toBe(
      '视口  重构前  重构后  改善\n769  7px  539px  ≈77×\n834（iPad Air 竖屏）  72px  604px  8.4×',
    )
  })

  it('表格：仅含管道与横线的分隔行被丢弃', () => {
    expect(markdownToText('|---|---|---|')).toBe('')
    expect(markdownToText('|:--|--:|:---:|')).toBe('')
  })

  it('标题：去掉 # 前缀，保留标题文字', () => {
    expect(markdownToText('# 响应式布局\n## 视口对比\n### 结论')).toBe(
      '响应式布局\n视口对比\n结论',
    )
  })

  it('加粗/斜体/删除线/行内代码：去掉标记符号保留内容', () => {
    const md = '**重点** 与 *次要*、`code`、~~旧值~~ 与 __加粗__'
    expect(markdownToText(md)).toBe('重点 与 次要、code、旧值 与 加粗')
  })

  it('链接/图片：保留文字与 alt，去掉 URL 与图片语法', () => {
    const md = '参考[官方文档](https://example.com/doc)与![示意图](https://example.com/a.png)'
    expect(markdownToText(md)).toBe('参考官方文档与示意图')
  })

  it('列表：无序去掉前缀，有序保留编号，任务列表转为勾选框', () => {
    const md = ['- 苹果', '- 香蕉', '1. 第一步', '2. 第二步', '- [x] 已完成', '- [ ] 待办'].join('\n')
    expect(markdownToText(md)).toBe('苹果\n香蕉\n1. 第一步\n2. 第二步\n✓ 已完成\n☐ 待办')
  })

  it('引用：去掉 > 前缀', () => {
    expect(markdownToText('> 这是一条引用\n\n正文')).toBe('这是一条引用\n\n正文')
  })

  it('代码围栏：内容原样保留，围栏标记去掉', () => {
    const md = ['```ts', 'const a: number = 1', '```', '说明文字'].join('\n')
    expect(markdownToText(md)).toBe('const a: number = 1\n说明文字')
  })

  it('frontmatter：开头的 YAML 元信息被去掉', () => {
    const md = ['---', 'title: 测试', 'tags: [a, b]', '---', '# 正文标题', '正文内容'].join('\n')
    expect(markdownToText(md)).toBe('正文标题\n正文内容')
  })

  it('HTML 残留标签被移除', () => {
    expect(markdownToText('<div class="x">内容</div><br>')).toBe('内容')
  })

  it('连续空行被合并为段落间的一空行', () => {
    expect(markdownToText('第一段\n\n\n\n第二段')).toBe('第一段\n\n第二段')
  })

  it('空输入返回空串', () => {
    expect(markdownToText('')).toBe('')
    expect(markdownToText(undefined as unknown as string)).toBe('')
  })
})

describe('truncateText', () => {
  it('短文本不截断', () => {
    expect(truncateText('abc', 100)).toBe('abc')
  })

  it('超长文本截断并保留完整行，末尾加省略号', () => {
    const text = '第一行内容\n第二行内容\n第三行内容'
    expect(truncateText(text, 12)).toBe('第一行内容\n第二行内容…')
  })

  it('首行即超预算时按字符截断', () => {
    expect(truncateText('abcdefghij', 5)).toBe('abcde…')
  })

  it('空输入返回原样', () => {
    expect(truncateText('', 10)).toBe('')
  })
})

describe('markdownToTextSnippet', () => {
  it('把含表格的 markdown 转为紧凑片段', () => {
    const md = ['| 视口 | 重构前 | 重构后 | 改善 |', '|---|---|---|---|', '| 769 | 7px | **539px** | ≈77× |'].join('\n')
    const snippet = markdownToTextSnippet(md, 40)
    expect(snippet).not.toContain('|')
    expect(snippet).not.toContain('---')
    expect(snippet).not.toContain('**')
    expect(snippet.length).toBeLessThanOrEqual(40 + 1) // 允许省略号
    expect(snippet).toContain('539px')
  })
})

describe('hasMarkdownSyntax', () => {
  it('表格/标题/加粗/代码围栏/链接识别为 markdown', () => {
    expect(hasMarkdownSyntax('| a | b |')).toBe(true)
    expect(hasMarkdownSyntax('# 标题')).toBe(true)
    expect(hasMarkdownSyntax('**加粗**')).toBe(true)
    expect(hasMarkdownSyntax('```ts')).toBe(true)
    expect(hasMarkdownSyntax('[链接](https://x.com)')).toBe(true)
    expect(hasMarkdownSyntax('---')).toBe(true)
  })

  it('纯文本与空串识别为非 markdown', () => {
    expect(hasMarkdownSyntax('这是一段普通文本')).toBe(false)
    expect(hasMarkdownSyntax('')).toBe(false)
  })
})
