import type { NbTreeNode } from '@/api/notebook'

export interface NoteItem {
  id: number
  title: string
  folderPath: string
  isPinned: boolean
  contentExcerpt?: string
}

export interface TreeNode {
  nodeKey: string
  nodeType: 'FOLDER' | 'NOTE'
  name: string
  noteId?: number
  isPinned?: number
  children?: TreeNode[]
}

export function buildNoteTree(nodes: NbTreeNode[]): {
  pinnedNotes: NoteItem[]
  rootFolders: TreeNode[]
} {
  const pinnedNotes: NoteItem[] = []

  function convert(node: NbTreeNode, folderPath: string): TreeNode {
    const treeNode: TreeNode = {
      nodeKey: node.nodeKey,
      nodeType: node.nodeType,
      name: node.name,
      noteId: node.noteId,
      isPinned: node.isPinned,
    }

    if (node.nodeType === 'NOTE' && node.noteId) {
      if (node.isPinned === 1) {
        pinnedNotes.push({
          id: node.noteId,
          title: node.name,
          folderPath,
          isPinned: true,
        })
      }
    }

    if (node.children?.length) {
      const nextPath =
        node.nodeType === 'FOLDER'
          ? folderPath
            ? `${folderPath} / ${node.name}`
            : node.name
          : folderPath

      treeNode.children = node.children.map((child) => convert(child, nextPath))
    }

    return treeNode
  }

  const rootFolders = nodes.map((node) => convert(node, ''))

  return { pinnedNotes, rootFolders }
}

export function filterTreeByKeyword(tree: TreeNode[], keyword: string): TreeNode[] {
  if (!keyword) return tree

  const q = keyword.trim().toLowerCase()

  function filter(node: TreeNode): TreeNode | null {
    const matches =
      node.name.toLowerCase().includes(q) ||
      (node.nodeType === 'NOTE' && node.name.toLowerCase().includes(q))

    if (node.children?.length) {
      const filteredChildren = node.children.map(filter).filter((n): n is TreeNode => n !== null)
      if (filteredChildren.length || matches) {
        return { ...node, children: filteredChildren }
      }
    } else if (matches) {
      return node
    }

    return null
  }

  return tree.map(filter).filter((n): n is TreeNode => n !== null)
}

export function countNotesInFolder(node: TreeNode): number {
  let count = 0
  if (node.nodeType === 'NOTE') {
    count = 1
  }
  if (node.children?.length) {
    count += node.children.reduce((acc, child) => acc + countNotesInFolder(child), 0)
  }
  return count
}

export function countSubfolders(node: TreeNode): number {
  if (node.nodeType === 'NOTE') return 0
  let count = 0
  if (node.children?.length) {
    for (const child of node.children) {
      if (child.nodeType === 'FOLDER') {
        count += 1 + countSubfolders(child)
      }
    }
  }
  return count
}
