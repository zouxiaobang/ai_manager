import { afterEach, describe, expect, it, vi } from 'vitest'
import type { NbTreeNode } from '@/api/notebook'
import {
  collectDirectNoteIds,
  importFolderNotesToRag,
  importSingleNoteToRag,
} from '../importRagNote'

/** 构造文件夹节点 */
function folderNode(name: string, children: NbTreeNode[] = []): NbTreeNode {
  return { nodeKey: name, nodeType: 'FOLDER', name, children }
}

/** 构造笔记节点 */
function noteNode(id: number, name: string): NbTreeNode {
  return { nodeKey: `n${id}`, nodeType: 'NOTE', noteId: id, name }
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('collectDirectNoteIds', () => {
  it('只收集直属笔记 id，排除子文件夹及其内部笔记', () => {
    const folder = folderNode('folder', [
      noteNode(1, 'a.md'),
      folderNode('sub', [noteNode(2, 'b.md')]),
      noteNode(3, 'c'),
    ])
    expect(collectDirectNoteIds(folder)).toEqual([1, 3])
  })

  it('跳过 noteId 缺失的节点；空文件夹返回 []', () => {
    const folder = folderNode('folder', [{ nodeKey: 'x', nodeType: 'NOTE', name: 'x' }])
    expect(collectDirectNoteIds(folder)).toEqual([])
    expect(collectDirectNoteIds(folderNode('empty'))).toEqual([])
  })
})

describe('importSingleNoteToRag', () => {
  it('noteId 缺失时不下发请求', async () => {
    const api = vi.fn()
    await importSingleNoteToRag({ nodeKey: 'x', nodeType: 'NOTE', name: 'x' }, api)
    expect(api).not.toHaveBeenCalled()
  })

  it('按节点 noteId 调用导入接口', async () => {
    const api = vi.fn().mockResolvedValue({ documentId: 1, fileName: 'a.md', status: 'pending', message: '' })
    await importSingleNoteToRag(noteNode(42, 'a.md'), api)
    expect(api).toHaveBeenCalledWith(42)
  })
})

describe('importFolderNotesToRag', () => {
  it('空文件夹不下发请求，返回全 0 统计', async () => {
    const api = vi.fn()
    const result = await importFolderNotesToRag(folderNode('empty'), api)
    expect(result).toEqual({ imported: 0, failed: [] })
    expect(api).not.toHaveBeenCalled()
  })

  it('收集直属 id 批量提交，透传接口返回的统计', async () => {
    const api = vi.fn().mockResolvedValue({
      imported: 2,
      failed: [{ noteId: 3, message: '已存在' }],
    })
    const folder = folderNode('资料', [
      noteNode(1, 'a'),
      noteNode(2, 'b'),
      noteNode(3, 'bad'),
      folderNode('sub', [noteNode(9, 's')]),
    ])
    const result = await importFolderNotesToRag(folder, api)
    expect(api).toHaveBeenCalledWith([1, 2, 3])
    expect(result).toEqual({ imported: 2, failed: [{ noteId: 3, message: '已存在' }] })
  })
})
