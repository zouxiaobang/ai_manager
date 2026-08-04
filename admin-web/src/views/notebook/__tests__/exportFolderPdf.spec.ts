import { afterEach, describe, expect, it, vi } from 'vitest'
import type { NbNoteDetail, NbTreeNode } from '@/api/notebook'
import {
  absolutizeImageSrc,
  buildFolderPdfHtml,
  buildNoteBodyHtml,
  collectDirectNotes,
  escapeHtml,
  exportFolderToPdf,
  fetchNotesConcurrently,
  parseLeadingNumber,
  sortNotesByName,
} from '../exportFolderPdf'

/** 构造文件夹节点 */
function folderNode(name: string, children: NbTreeNode[] = []): NbTreeNode {
  return { nodeKey: name, nodeType: 'FOLDER', name, children }
}

/** 构造笔记节点 */
function noteNode(id: number, name: string): NbTreeNode {
  return { nodeKey: `n${id}`, nodeType: 'NOTE', noteId: id, name }
}

/** 构造笔记详情 */
function detail(id: number, title: string, content: string): NbNoteDetail {
  return { id, title, content, noteType: 'NOTE', isPinned: 0, isFavorite: 0, status: 'NORMAL' }
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('collectDirectNotes', () => {
  it('只收集直属笔记，排除子文件夹及其内部笔记', () => {
    const folder = folderNode('folder', [
      noteNode(1, 'a.md'),
      folderNode('sub', [noteNode(2, 'b.md')]),
      noteNode(3, 'c'),
    ])
    const notes = collectDirectNotes(folder)
    expect(notes.map((n) => n.noteId)).toEqual([1, 3])
  })

  it('跳过 noteId 缺失的节点与空 children', () => {
    const folder = folderNode('folder', [
      { nodeKey: 'x', nodeType: 'NOTE', name: 'x' },
    ])
    expect(collectDirectNotes(folder)).toEqual([])
    expect(collectDirectNotes(folderNode('empty'))).toEqual([])
  })
})

describe('sortNotesByName', () => {
  it('阿拉伯数字开头按数值自然序：1、2、10', () => {
    const notes = [noteNode(1, '10. 结尾'), noteNode(2, '2. 概述'), noteNode(3, '1. 开头')]
    expect(sortNotesByName(notes).map((n) => n.name)).toEqual(['1. 开头', '2. 概述', '10. 结尾'])
  })

  it('数字不在开头时视为非数字标题，保持树接口顺序', () => {
    const notes = [noteNode(1, 'note10'), noteNode(2, 'note2'), noteNode(3, 'note1')]
    expect(sortNotesByName(notes).map((n) => n.name)).toEqual(['note10', 'note2', 'note1'])
  })

  it('中文数字开头按数值序：一、二、三、十、十一', () => {
    const notes = [
      noteNode(1, '三、总结'),
      noteNode(2, '一、概述'),
      noteNode(3, '二、原理'),
      noteNode(4, '十、扩展'),
      noteNode(5, '十一、附录'),
    ]
    expect(sortNotesByName(notes).map((n) => n.name)).toEqual([
      '一、概述',
      '二、原理',
      '三、总结',
      '十、扩展',
      '十一、附录',
    ])
  })

  it('非数字标题保持树接口原始顺序，不按拼音', () => {
    const notes = [noteNode(1, '张三'), noteNode(2, '李四'), noteNode(3, '王五')]
    expect(sortNotesByName(notes).map((n) => n.name)).toEqual(['张三', '李四', '王五'])
  })

  it('数字标题统一排在非数字标题之前', () => {
    const notes = [
      noteNode(1, 'README'),
      noteNode(2, '二、原理'),
      noteNode(3, '一、概述'),
      noteNode(4, 'TODO'),
    ]
    expect(sortNotesByName(notes).map((n) => n.name)).toEqual([
      '一、概述',
      '二、原理',
      'README',
      'TODO',
    ])
  })

  it('不修改原数组', () => {
    const notes = [noteNode(1, 'b'), noteNode(2, '一、a')]
    const snapshot = [...notes]
    sortNotesByName(notes)
    expect(notes).toEqual(snapshot)
  })
})

describe('parseLeadingNumber', () => {
  it('解析阿拉伯数字前缀', () => {
    expect(parseLeadingNumber('2. 概述')).toBe(2)
    expect(parseLeadingNumber('10章')).toBe(10)
  })

  it('解析中文数字：个位', () => {
    expect(parseLeadingNumber('一、概述')).toBe(1)
    expect(parseLeadingNumber('三')).toBe(3)
  })

  it('解析中文数字：十/百组合', () => {
    expect(parseLeadingNumber('十')).toBe(10)
    expect(parseLeadingNumber('十三')).toBe(13)
    expect(parseLeadingNumber('二十一')).toBe(21)
    expect(parseLeadingNumber('一百零五')).toBe(105)
    expect(parseLeadingNumber('两')).toBe(2)
  })

  it('非数字开头返回 null', () => {
    expect(parseLeadingNumber('张三')).toBeNull()
    expect(parseLeadingNumber('README')).toBeNull()
    expect(parseLeadingNumber('')).toBeNull()
  })
})

describe('absolutizeImageSrc', () => {
  it('把相对图片地址前缀上 base', () => {
    const html = '<img src="/uploads/notebook/images/a.png">'
    expect(absolutizeImageSrc(html, '/api')).toBe('<img src="/api/uploads/notebook/images/a.png">')
  })

  it('完整 URL 与 data URI 保持不变', () => {
    const html = '<img src="https://x.com/a.png"><img src="data:image/png;base64,xxx">'
    expect(absolutizeImageSrc(html, '/api')).toBe(html)
  })

  it('base 为空时原样返回', () => {
    const html = '<img src="/uploads/a.png">'
    expect(absolutizeImageSrc(html, '')).toBe(html)
  })
})

describe('escapeHtml', () => {
  it('转义特殊字符', () => {
    expect(escapeHtml(`<a href="x">&'</a>`)).toBe(
      '&lt;a href=&quot;x&quot;&gt;&amp;&#39;&lt;/a&gt;',
    )
  })
})

describe('buildNoteBodyHtml', () => {
  it('.md 标题走 marked 渲染', () => {
    const html = buildNoteBodyHtml(detail(1, 'a.md', '# 标题\n\n**加粗**'))
    expect(html).toContain('<h1')
    expect(html).toContain('<strong>')
  })

  it('非 md（富文本 HTML）原样返回', () => {
    const content = '<p>hello <b>world</b></p>'
    expect(buildNoteBodyHtml(detail(1, 'a', content))).toBe(content)
  })
})

describe('buildFolderPdfHtml', () => {
  it('每篇生成分页 section，注入标题、正文与打印样式', () => {
    const html = buildFolderPdfHtml(
      [{ node: noteNode(1, 'a.md'), detail: detail(1, 'a.md', '内容') }],
      { folderName: '资料', css: '.mock-css{}' },
    )
    expect(html).toContain('<!DOCTYPE html>')
    expect(html).toContain('资料')
    expect(html).toContain('note-export__note')
    expect(html).toContain('page-break-before')
    expect(html).toContain('note-export__note-title')
    expect(html).toContain('note-content-body')
    expect(html).toContain('.mock-css{}')
    expect(html).toContain('内容')
  })

  it('标题做 HTML 转义，防止注入破坏文档结构', () => {
    const html = buildFolderPdfHtml(
      [{ node: noteNode(1, '<img>'), detail: detail(1, '<img>', '') }],
      { css: '' },
    )
    expect(html).not.toContain('<img>')
    expect(html).toContain('&lt;img&gt;')
  })

  it('空列表生成空 body（无 section）', () => {
    const html = buildFolderPdfHtml([], { css: '' })
    // 仅断言没有 section 元素（CSS 规则里本就含 note-export__note 类名，不能匹配之）
    expect(html).not.toContain('class="note-export__note"')
  })

  it('默认内联公共正文样式（note-content-body）', () => {
    const html = buildFolderPdfHtml([{ node: noteNode(1, 'a'), detail: detail(1, 'a', 'x') }])
    expect(html).toContain('.note-content-body')
  })
})

describe('fetchNotesConcurrently', () => {
  it('并发受限：峰值并发不超过阈值且顺序与入参一致', async () => {
    let active = 0
    let peak = 0
    const fetcher = vi.fn((id: number) => {
      active += 1
      peak = Math.max(peak, active)
      return new Promise<NbNoteDetail>((resolve) => {
        setTimeout(() => {
          active -= 1
          resolve(detail(id, `n${id}`, `c${id}`))
        }, 5)
      })
    })
    const nodes = [noteNode(1, 'a'), noteNode(2, 'b'), noteNode(3, 'c'), noteNode(4, 'd'), noteNode(5, 'e')]
    const results = await fetchNotesConcurrently(nodes, fetcher)
    expect(peak).toBeLessThanOrEqual(4)
    expect(results.map((r) => r.node.noteId)).toEqual([1, 2, 3, 4, 5])
    expect(results.every((r) => r.detail != null)).toBe(true)
    expect(fetcher).toHaveBeenCalledTimes(5)
  })

  it('单篇失败以 detail=null 标记，不阻断整体', async () => {
    const fetcher = vi.fn((id: number) =>
      id === 2 ? Promise.reject(new Error('load fail')) : Promise.resolve(detail(id, `n${id}`, 'c')),
    )
    const results = await fetchNotesConcurrently([noteNode(1, 'a'), noteNode(2, 'b')], fetcher)
    expect(results[0].detail?.id).toBe(1)
    expect(results[1].detail).toBeNull()
  })

  it('触发进度回调', async () => {
    const fetcher = vi.fn((id: number) => Promise.resolve(detail(id, `n${id}`, 'c')))
    const onProgress = vi.fn()
    await fetchNotesConcurrently([noteNode(1, 'a'), noteNode(2, 'b')], fetcher, onProgress)
    expect(onProgress).toHaveBeenLastCalledWith(2, 2)
  })
})

describe('exportFolderToPdf', () => {
  it('空文件夹直接返回，不拉取正文', async () => {
    const fetcher = vi.fn()
    const result = await exportFolderToPdf(folderNode('empty'), { fetcher })
    expect(result).toEqual({ exported: 0, failed: [] })
    expect(fetcher).not.toHaveBeenCalled()
  })

  it('全部成功时按名称排序导出并返回统计', async () => {
    const folder = folderNode('资料', [noteNode(2, 'b.md'), noteNode(1, 'a.md')])
    const fetcher = vi.fn((id: number) => Promise.resolve(detail(id, id === 1 ? 'a.md' : 'b.md', '正文')))
    const result = await exportFolderToPdf(folder, { fetcher })
    expect(result.exported).toBe(2)
    expect(result.failed).toEqual([])
  })

  it('部分失败时返回失败清单，成功篇正常导出', async () => {
    const folder = folderNode('资料', [noteNode(1, 'a'), noteNode(2, 'bad'), noteNode(3, 'c')])
    const fetcher = vi.fn((id: number) =>
      id === 2 ? Promise.reject(new Error('x')) : Promise.resolve(detail(id, `n${id}`, '正文')),
    )
    const result = await exportFolderToPdf(folder, { fetcher })
    expect(result.exported).toBe(2)
    expect(result.failed).toEqual([{ noteId: 2, title: 'bad' }])
  })
})
