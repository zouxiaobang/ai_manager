import type { NbNoteDetail, NbTreeNode } from '@/api/notebook'

/**
 * 笔记本文件夹树的纯操作函数
 *
 * <p>从 {@code NotebookView.vue} 提取：树节点统计、文件夹过滤、关键字匹配、
 * 搜索展开键收集、节点查找、子树 id 收集、笔记桩对象构建等无副作用函数，
 * 便于独立单元测试。</p>
 */

/** 递归统计树中指定类型节点数 */
export function countTreeNodes(nodes: NbTreeNode[], type: NbTreeNode['nodeType']): number {
  let count = 0
  for (const node of nodes) {
    if (node.nodeType === type) count += 1
    if (node.children?.length) count += countTreeNodes(node.children, type)
  }
  return count
}

/** 仅保留文件夹节点（递归），供移动目标树使用 */
export function buildFolderOnlyTree(nodes: NbTreeNode[]): NbTreeNode[] {
  return nodes
    .filter((node) => node.nodeType === 'FOLDER')
    .map((node) => ({
      ...node,
      children: node.children?.length ? buildFolderOnlyTree(node.children) : undefined,
    }))
}

/** 节点名称或其子孙节点名称是否包含关键字 */
export function subtreeMatches(node: NbTreeNode, keyword: string): boolean {
  const lower = keyword.toLowerCase()
  if (node.name.toLowerCase().includes(lower)) return true
  return node.children?.some((child) => subtreeMatches(child, keyword)) ?? false
}

/** el-tree filterNode 方法：空值全匹配，否则按子树匹配 */
export function filterNode(value: string, data: NbTreeNode) {
  if (!value) return true
  return subtreeMatches(data, value.trim())
}

/**
 * 收集搜索匹配节点的所有祖先文件夹 key（用于展开）
 * 匹配节点自身若是文件夹也一并展开
 */
export function collectSearchExpandKeys(nodes: NbTreeNode[], keyword: string): string[] {
  const keys = new Set<string>()

  function walk(nodes: NbTreeNode[], ancestorFolders: string[]) {
    for (const node of nodes) {
      if (!subtreeMatches(node, keyword)) continue

      ancestorFolders.forEach((key) => keys.add(key))

      if (node.nodeType === 'FOLDER') {
        keys.add(node.nodeKey)
        if (node.children?.length) {
          walk(node.children, [...ancestorFolders, node.nodeKey])
        }
      } else if (node.children?.length) {
        walk(node.children, ancestorFolders)
      }
    }
  }

  walk(nodes, [])
  return [...keys]
}

/** 收集目标节点到根的文件夹祖先 key 路径；未找到返回 null */
export function collectAncestorFolderKeys(
  nodes: NbTreeNode[],
  targetKey: string,
  ancestors: string[] = [],
): string[] | null {
  for (const node of nodes) {
    if (node.nodeKey === targetKey) {
      return ancestors
    }
    if (node.children?.length) {
      const nextAncestors =
        node.nodeType === 'FOLDER' ? [...ancestors, node.nodeKey] : ancestors
      const found = collectAncestorFolderKeys(node.children, targetKey, nextAncestors)
      if (found) return found
    }
  }
  return null
}

/** 按 nodeKey 深度查找节点；未找到返回 null */
export function findNodeByKey(nodes: NbTreeNode[], key: string): NbTreeNode | null {
  for (const node of nodes) {
    if (node.nodeKey === key) return node
    if (node.children?.length) {
      const found = findNodeByKey(node.children, key)
      if (found) return found
    }
  }
  return null
}

/** 按 noteId 深度查找笔记节点；未找到返回 null */
export function findNodeByNoteId(nodes: NbTreeNode[], noteId: number): NbTreeNode | null {
  for (const node of nodes) {
    if (node.nodeType === 'NOTE' && node.noteId === noteId) return node
    if (node.children?.length) {
      const found = findNodeByNoteId(node.children, noteId)
      if (found) return found
    }
  }
  return null
}

/** 收集文件夹及其所有子孙文件夹的 notebookId（含自身）；节点不存在时仅返回自身 */
export function collectFolderSubtreeIds(nodes: NbTreeNode[], folderId: number): number[] {
  const root = findNodeByKey(nodes, `folder-${folderId}`)
  if (!root) return [folderId]
  const ids: number[] = []
  const walk = (node: NbTreeNode) => {
    if (node.nodeType === 'FOLDER' && node.notebookId) {
      ids.push(node.notebookId)
      node.children?.forEach(walk)
    }
  }
  walk(root)
  return ids
}

/** 由树节点构造轻量笔记桩对象（打开笔记前先用桩展示标题/固定态） */
export function buildNoteStubFromTree(data: NbTreeNode): NbNoteDetail {
  return {
    id: data.noteId!,
    notebookId: data.notebookId,
    title: data.name,
    noteType: 'NOTE',
    isPinned: data.isPinned ?? 0,
    isFavorite: data.isFavorite ?? 0,
    status: 'ACTIVE',
  }
}
