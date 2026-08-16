import { describe, expect, it } from 'vitest'
import { addHeadingIds, parseTocItems, renderNoteBody } from '../mobileNoteRender'

describe('renderNoteBody（与 PC 端全屏预览同源渲染）', () => {
  it('isMd=true 走 marked 渲染，标题补自增 id', () => {
    const html = renderNoteBody(true, '# 一级\n\n## 二级')
    expect(html).toContain('<h1 id="nb-heading-0">一级</h1>')
    expect(html).toContain('<h2 id="nb-heading-1">二级</h2>')
  })

  it('isMd=true 渲染表格（gfm，PC 端同库同配置）', () => {
    const html = renderNoteBody(true, '| a | b |\n|---|---|\n| 1 | 2 |')
    expect(html).toContain('<table>')
    expect(html).toContain('<td>1</td>')
  })

  it('isMd=true 渲染任务列表（checkbox 可命中 note-content.scss 的 input[type=checkbox] 通用样式）', () => {
    const html = renderNoteBody(true, '- [x] 已完成\n- [ ] 待办')
    // 与 PC 端全屏预览（marked 同源）输出一致：checked 项为 disabled checkbox
    expect(html).toMatch(/<input checked=""? disabled=""? type="checkbox">/)
    expect(html.match(/type="checkbox"/g)).toHaveLength(2)
  })

  it('isMd=true 渲染引用块', () => {
    const html = renderNoteBody(true, '> 引用内容')
    expect(html).toContain('<blockquote>')
  })

  it('isMd=false 富文本 HTML 原样输出，标题补自增 id', () => {
    const html = renderNoteBody(false, '<h2>章节</h2><p>正文</p>')
    expect(html).toContain('<h2 id="nb-heading-0">章节</h2>')
    expect(html).toContain('<p>正文</p>')
  })

  it('空内容返回空串', () => {
    expect(renderNoteBody(true, '')).toBe('')
    expect(renderNoteBody(true, null)).toBe('')
    expect(renderNoteBody(true, undefined)).toBe('')
  })

  it('已带 id 的标题不被覆盖', () => {
    const html = renderNoteBody(true, '# 标题')
    // marked v18 默认不给标题加 id，此处验证 HTML 输入场景下自定义 id 保留
    const raw = renderNoteBody(false, '<h3 id="custom">锚点</h3>')
    expect(raw).toContain('<h3 id="custom">锚点</h3>')
    expect(html).toContain('id="nb-heading-0"')
  })
})

describe('addHeadingIds', () => {
  it('无 id 标题按文档序补自增 id（h1-h6 全覆盖）', () => {
    const html = addHeadingIds('<h1>一</h1><h4>四</h4><h6>六</h6>')
    expect(html).toBe('<h1 id="nb-heading-0">一</h1><h4 id="nb-heading-1">四</h4><h6 id="nb-heading-2">六</h6>')
  })

  it('已有 id 的标题跳过', () => {
    const html = addHeadingIds('<h2 id="keep">保留</h2><h2>新增</h2>')
    expect(html).toBe('<h2 id="keep">保留</h2><h2 id="nb-heading-0">新增</h2>')
  })
})

describe('parseTocItems', () => {
  it('提取 h1-h6 文本与层级，id 与 addHeadingIds 对齐', () => {
    const html = addHeadingIds('<h1>一级</h1><h2>二级</h2>')
    expect(parseTocItems(html)).toEqual([
      { id: 'nb-heading-0', level: 1, text: '一级' },
      { id: 'nb-heading-1', level: 2, text: '二级' },
    ])
  })

  it('标题内联标签（如 code）被剥离', () => {
    const html = addHeadingIds('<h2>安装 <code>npm</code></h2>')
    expect(parseTocItems(html)).toEqual([{ id: 'nb-heading-0', level: 2, text: '安装 npm' }])
  })

  it('空/无标题返回空数组', () => {
    expect(parseTocItems('')).toEqual([])
    expect(parseTocItems('   ')).toEqual([])
    expect(parseTocItems('<p>无标题</p>')).toEqual([])
  })
})
