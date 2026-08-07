import type { NbTreeNode } from '@/api/notebook'
import { importNoteToRag, importNotesToRag, type RagBatchImportResult } from '@/api/aiKnowledge'
import { collectDirectNotes } from './exportFolderPdf'

/**
 * 收集文件夹「直属」笔记的 id 数组（不含子文件夹内笔记）。
 * 复用 collectDirectNotes 的过滤规则（nodeType=NOTE 且 noteId 存在），再抽出 id 列表。
 */
export function collectDirectNoteIds(folderNode: NbTreeNode): number[] {
  return collectDirectNotes(folderNode)
    .map((n) => n.noteId)
    .filter((id): id is number => id != null)
}

/** 单篇导入接口签名，便于单测注入替身 */
export type RagSingleImporter = (noteId: number) => Promise<unknown>

/** 单篇导入编排：noteId 缺失时不下发请求（右键事件入口已前置过滤，此处兜底防误发） */
export async function importSingleNoteToRag(
  node: NbTreeNode,
  api: RagSingleImporter = importNoteToRag,
): Promise<void> {
  if (!node.noteId) return
  await api(node.noteId)
}

/** 批量导入接口签名，便于单测注入替身 */
export type RagBatchImporter = (noteIds: number[]) => Promise<RagBatchImportResult>

/**
 * 文件夹批量导入编排：收集直属 id 后一次性提交。
 * 空文件夹直接返回全 0 统计，不发空请求（避免后端空列表无意义的处理）。
 */
export async function importFolderNotesToRag(
  folderNode: NbTreeNode,
  api: RagBatchImporter = importNotesToRag,
): Promise<RagBatchImportResult> {
  const noteIds = collectDirectNoteIds(folderNode)
  if (!noteIds.length) {
    return { imported: 0, failed: [] }
  }
  return api(noteIds)
}
