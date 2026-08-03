import { describe, expect, it } from 'vitest'
import { buildImportNoteMarkdown, deriveToolbarTitle, findTreeNode } from '../aiKnowledgeToolbar'

describe('aiKnowledgeToolbar 纯函数', () => {
  describe('deriveToolbarTitle', () => {
    it('取上一条用户消息内容作为标题', () => {
      const messages = [{ content: '第一个问题' }, { content: '第二个问题' }]
      expect(deriveToolbarTitle(messages, 1)).toBe('第一个问题')
    })

    it('折叠换行为空格', () => {
      const messages = [{ content: '多行\n问题' }, { content: '回答' }]
      expect(deriveToolbarTitle(messages, 1)).toBe('多行 问题')
    })

    it('超过 50 字符时截断', () => {
      const long = '问'.repeat(60)
      const messages = [{ content: long }, { content: '回答' }]
      expect(deriveToolbarTitle(messages, 1)).toBe('问'.repeat(50))
    })

    it('上一条内容为空时回退「AI 问答」', () => {
      expect(deriveToolbarTitle([{ content: '' }, { content: '回答' }], 1)).toBe('AI 问答')
      expect(deriveToolbarTitle([{ content: null }, { content: '回答' }], 1)).toBe('AI 问答')
    })

    it('无上一条消息（首条）时回退「AI 问答」', () => {
      expect(deriveToolbarTitle([{ content: '回答' }], 0)).toBe('AI 问答')
    })
  })

  describe('buildImportNoteMarkdown', () => {
    it('按标题/日期/正文模板构建笔记内容', () => {
      const md = buildImportNoteMarkdown('问题', '回答内容', '2026年8月4日')
      expect(md).toBe('> **问题**  ·  📅 2026年8月4日\n\n---\n\n回答内容')
    })
  })

  describe('findTreeNode', () => {
    // 递归泛型对字面量推断有限制，显式声明树节点接口
    interface TestNode { id: number; children?: TestNode[] }
    const tree: TestNode[] = [
      { id: 1, children: [{ id: 2, children: [{ id: 3 }] }] },
      { id: 4 },
    ]

    it('命中根节点', () => {
      expect(findTreeNode(tree, (n) => n.id === 1)?.id).toBe(1)
    })

    it('递归命中深层节点', () => {
      expect(findTreeNode(tree, (n) => n.id === 3)?.id).toBe(3)
      expect(findTreeNode(tree, (n) => n.id === 4)?.id).toBe(4)
    })

    it('未命中返回 null', () => {
      expect(findTreeNode(tree, (n) => n.id === 99)).toBeNull()
    })

    it('空数组返回 null', () => {
      expect(findTreeNode([], () => true)).toBeNull()
    })

    it('children 为 undefined 时安全跳过', () => {
      expect(findTreeNode(tree, (n) => n.id === 4)?.id).toBe(4)
      const leafOnly: TestNode[] = [{ id: 4 }]
      expect(findTreeNode(leafOnly, (n) => n.id === 3)).toBeNull()
    })
  })
})
