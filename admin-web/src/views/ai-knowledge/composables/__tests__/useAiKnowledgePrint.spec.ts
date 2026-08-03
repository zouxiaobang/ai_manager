import { describe, expect, it } from 'vitest'
import { buildPrintableHtml, formatPdfBody, useAiKnowledgePrint } from '../useAiKnowledgePrint'

describe('useAiKnowledgePrint', () => {
  describe('formatPdfBody', () => {
    it('将 markdown 标题转为 HTML 并添加图标前缀', () => {
      const html = formatPdfBody('# 一级标题\n\n## 二级标题')
      expect(html).toContain('<h1')
      expect(html).toContain('🗂️ ')
      expect(html).toContain('<h2')
      expect(html).toContain('📌 ')
    })

    it('为强标签注入打印内联样式', () => {
      const html = formatPdfBody('**重要**')
      expect(html).toContain('<strong style="color:#b91c1c"')
      expect(html).toContain('重要')
    })

    it('输出标题与日期 meta 头', () => {
      const html = formatPdfBody('正文', { title: '文档标题', date: '2026年8月3日' })
      expect(html).toContain('文档标题')
      expect(html).toContain('📅 2026年8月3日')
      expect(html).toContain('正文')
    })

    it('无 meta 时不输出标题/日期块', () => {
      const html = formatPdfBody('正文')
      expect(html).not.toContain('📅')
      expect(html).toContain('正文')
    })
  })

  describe('buildPrintableHtml', () => {
    it('构建完整可打印 HTML 文档', () => {
      const html = buildPrintableHtml('# 你好', '测试文档')
      expect(html).toContain('<!DOCTYPE html>')
      expect(html).toContain('<html lang="zh-CN">')
      expect(html).toContain('<title>测试文档</title>')
      expect(html).toContain('@page { margin: 22mm 18mm; }')
      expect(html).toContain('window.print()')
      expect(html).toContain('<h1')
      expect(html).toContain('你好')
    })

    it('日期取自当前时间并渲染到文档中', () => {
      const html = buildPrintableHtml('内容', '标题')
      expect(html).toContain('📅 ')
    })
  })

  describe('useAiKnowledgePrint', () => {
    it('返回两个打印函数', () => {
      const api = useAiKnowledgePrint()
      expect(typeof api.formatPdfBody).toBe('function')
      expect(typeof api.buildPrintableHtml).toBe('function')
    })
  })
})
