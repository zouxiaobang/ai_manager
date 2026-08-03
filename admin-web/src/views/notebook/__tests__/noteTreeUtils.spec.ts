import { describe, expect, it } from 'vitest'
import type { NbTreeNode } from '@/api/notebook'
import {
  buildFolderOnlyTree,
  buildNoteStubFromTree,
  collectAncestorFolderKeys,
  collectFolderSubtreeIds,
  collectSearchExpandKeys,
  countTreeNodes,
  filterNode,
  findNodeByKey,
  findNodeByNoteId,
  subtreeMatches,
} from '../noteTreeUtils'

/** 构造树 fixture：根目录(folder-1) → 子目录(folder-2) → 笔记甲(note-1)；根目录另含笔记乙(note-2)；顶层独立笔记(note-3) */
function makeTree(): NbTreeNode[] {
  return [
    {
      nodeKey: 'folder-1',
      nodeType: 'FOLDER',
      notebookId: 1,
      name: '根目录',
      children: [
        {
          nodeKey: 'folder-2',
          nodeType: 'FOLDER',
          notebookId: 2,
          name: '子目录',
          children: [{ nodeKey: 'note-1', nodeType: 'NOTE', noteId: 10, name: '笔记甲' }],
        },
        { nodeKey: 'note-2', nodeType: 'NOTE', noteId: 20, name: '笔记乙' },
      ],
    },
    { nodeKey: 'note-3', nodeType: 'NOTE', noteId: 30, name: '顶层笔记' },
  ]
}

describe('noteTreeUtils 树纯函数', () => {
  describe('countTreeNodes', () => {
    it('递归统计各类节点数', () => {
      const tree = makeTree()
      expect(countTreeNodes(tree, 'FOLDER')).toBe(2)
      expect(countTreeNodes(tree, 'NOTE')).toBe(3)
    })

    it('空树返回 0', () => {
      expect(countTreeNodes([], 'NOTE')).toBe(0)
    })
  })

  describe('buildFolderOnlyTree', () => {
    it('只保留文件夹并递归', () => {
      const folders = buildFolderOnlyTree(makeTree())
      expect(folders).toHaveLength(1)
      expect(folders[0]?.nodeKey).toBe('folder-1')
      expect(folders[0]?.children).toHaveLength(1)
      expect(folders[0]?.children?.[0]?.nodeKey).toBe('folder-2')
    })

    it('仅含笔记的子文件夹递归后为空数组', () => {
      const folders = buildFolderOnlyTree(makeTree())
      expect(folders[0]?.children?.[0]?.children).toEqual([])
    })
  })

  describe('subtreeMatches', () => {
    it('名称匹配返回 true', () => {
      expect(subtreeMatches(makeTree()[0]!, '根目录')).toBe(true)
    })

    it('子孙节点匹配返回 true', () => {
      expect(subtreeMatches(makeTree()[0]!, '笔记甲')).toBe(true)
    })

    it('无关关键字返回 false', () => {
      expect(subtreeMatches(makeTree()[0]!, '不存在')).toBe(false)
    })

    it('忽略大小写', () => {
      const node: NbTreeNode = { nodeKey: 'x', nodeType: 'NOTE', noteId: 1, name: 'ABC' }
      expect(subtreeMatches(node, 'abc')).toBe(true)
    })
  })

  describe('filterNode', () => {
    it('空值全匹配', () => {
      expect(filterNode('', makeTree()[0]!)).toBe(true)
      expect(filterNode('  ', makeTree()[0]!)).toBe(true)
    })

    it('按子树关键字过滤', () => {
      expect(filterNode('笔记甲', makeTree()[0]!)).toBe(true)
      expect(filterNode('顶层', makeTree()[0]!)).toBe(false)
      expect(filterNode('顶层', makeTree()[1]!)).toBe(true)
    })
  })

  describe('collectSearchExpandKeys', () => {
    it('收集匹配节点的祖先文件夹 key', () => {
      const keys = collectSearchExpandKeys(makeTree(), '笔记甲')
      expect(keys.sort()).toEqual(['folder-1', 'folder-2'].sort())
    })

    it('匹配文件夹时自身也展开并继续下钻', () => {
      const keys = collectSearchExpandKeys(makeTree(), '子目录')
      expect(keys.sort()).toEqual(['folder-1', 'folder-2'].sort())
    })

    it('无匹配返回空数组', () => {
      expect(collectSearchExpandKeys(makeTree(), 'xxx')).toEqual([])
    })
  })

  describe('collectAncestorFolderKeys', () => {
    it('返回目标节点到根的文件夹路径', () => {
      expect(collectAncestorFolderKeys(makeTree(), 'note-1')).toEqual(['folder-1', 'folder-2'])
    })

    it('顶层节点返回空路径', () => {
      expect(collectAncestorFolderKeys(makeTree(), 'note-3')).toEqual([])
    })

    it('未找到返回 null', () => {
      expect(collectAncestorFolderKeys(makeTree(), 'ghost')).toBeNull()
    })
  })

  describe('findNodeByKey / findNodeByNoteId', () => {
    it('按 key 找到节点', () => {
      expect(findNodeByKey(makeTree(), 'note-1')?.name).toBe('笔记甲')
    })

    it('按 key 未找到返回 null', () => {
      expect(findNodeByKey(makeTree(), 'ghost')).toBeNull()
    })

    it('按 noteId 找到笔记节点', () => {
      expect(findNodeByNoteId(makeTree(), 20)?.nodeKey).toBe('note-2')
    })

    it('按 noteId 未找到返回 null', () => {
      expect(findNodeByNoteId(makeTree(), 999)).toBeNull()
    })
  })

  describe('collectFolderSubtreeIds', () => {
    it('收集文件夹自身与所有子孙文件夹 id', () => {
      expect(collectFolderSubtreeIds(makeTree(), 1).sort()).toEqual([1, 2])
    })

    it('叶文件夹仅自身', () => {
      expect(collectFolderSubtreeIds(makeTree(), 2)).toEqual([2])
    })

    it('节点不存在时返回仅自身 id', () => {
      expect(collectFolderSubtreeIds(makeTree(), 99)).toEqual([99])
    })
  })

  describe('buildNoteStubFromTree', () => {
    it('由树节点构造笔记桩对象', () => {
      const stub = buildNoteStubFromTree({
        nodeKey: 'note-9',
        nodeType: 'NOTE',
        noteId: 9,
        notebookId: 2,
        name: '桩笔记',
        isPinned: 1,
        isFavorite: 0,
      })
      expect(stub).toEqual({
        id: 9,
        notebookId: 2,
        title: '桩笔记',
        noteType: 'NOTE',
        isPinned: 1,
        isFavorite: 0,
        status: 'ACTIVE',
      })
    })
  })
})
