import type { NbTreeNode } from '@/api/notebook'

export interface FlatNoteItem {
  id: number
  title: string
  folderPath: string
  isPinned: boolean
}

export function flattenNotes(nodes: NbTreeNode[], folderPath = ''): FlatNoteItem[] {
  const result: FlatNoteItem[] = []
  for (const node of nodes) {
    if (node.nodeType === 'NOTE' && node.noteId) {
      result.push({
        id: node.noteId,
        title: node.name,
        folderPath,
        isPinned: node.isPinned === 1,
      })
    }
    if (node.children?.length) {
      const nextPath =
        node.nodeType === 'FOLDER'
          ? folderPath
            ? `${folderPath} / ${node.name}`
            : node.name
          : folderPath
      result.push(...flattenNotes(node.children, nextPath))
    }
  }
  return result
}
