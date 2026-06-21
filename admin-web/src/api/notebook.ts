import { deleteData, getData, postData, putData } from './request'

export interface NbTreeNode {

  nodeKey: string
  nodeType: 'FOLDER' | 'NOTE'
  notebookId?: number
  noteId?: number
  parentId?: number
  name: string
  isPinned?: number
  isFavorite?: number
  children?: NbTreeNode[]
}

export interface NbNoteTag {
  id: number
  name: string
  color?: string
  createTime?: string
}

export interface NbNoteDetail {
  id: number
  notebookId?: number
  title: string
  content?: string
  contentExcerpt?: string
  contentSize?: number
  syncStatus?: string
  syncError?: string
  noteType: string
  isPinned: number
  isFavorite: number
  sortOrder?: number
  status: string
  createTime?: string
  updateTime?: string
  tags?: NbNoteTag[]
}

export interface NbNoteListMeta {
  id: number
  contentExcerpt?: string
  contentSize?: number
  syncStatus?: string
  createTime?: string
}

export interface BaiduPanAuthStatus {
  authorized: boolean
  authorizeUrl?: string
  baiduUid?: number
  expiresAt?: string
}

export interface NbNoteTrashItem {
  id: number
  title: string
  notebookId?: number
  notebookName?: string
  updateTime?: string
}

export interface NbNotebookSaveRequest {
  parentId?: number | null
  name: string
  icon?: string
  color?: string
  sortOrder?: number
}

export interface NbNoteSaveRequest {
  notebookId?: number | null
  title?: string
  content?: string
  noteType?: string
  pinned?: boolean
  favorite?: boolean
  sortOrder?: number
  status?: string
  tagIds?: number[]
}

export interface NbNoteTagSaveRequest {
  name: string
  color?: string
}

export function fetchNotebookTree() {
  return getData<NbTreeNode[]>('/api/notebooks/tree')
}

export function createNotebook(body: NbNotebookSaveRequest) {
  return postData<{ id: number }>('/api/notebooks', body)
}

export function updateNotebook(id: number, body: NbNotebookSaveRequest) {
  return putData(`/api/notebooks/${id}`, body)
}

export function removeNotebook(id: number) {
  return deleteData(`/api/notebooks/${id}`)
}

export function fetchNote(id: number) {
  return getData<NbNoteDetail>(`/api/notes/${id}`)
}

export function fetchNotesMeta(ids: number[]) {
  return postData<NbNoteListMeta[]>('/api/notes/meta', { ids })
}

export function fetchBaiduPanStatus() {
  return getData<BaiduPanAuthStatus>('/api/baidu-pan/status')
}

export function fetchRecentNotes(limit = 20) {
  return getData<NbNoteDetail[]>('/api/notes/recent', { limit })
}

export async function createNoteRequest(body: NbNoteSaveRequest) {
  return postData<NbNoteDetail>('/api/notes', body)
}

export function updateNote(id: number, body: NbNoteSaveRequest) {
  return putData<NbNoteDetail>(`/api/notes/${id}`, body)
}

/** 页面关闭/刷新时用 keepalive 尽力保存，不等待响应 */
export function updateNoteKeepalive(id: number, body: NbNoteSaveRequest) {
  const base = import.meta.env.VITE_API_BASE || ''
  try {
    fetch(`${base}/api/notes/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
      keepalive: true,
    })
  } catch {
    // 页面卸载阶段忽略错误
  }
}

export function removeNote(id: number) {
  return deleteData(`/api/notes/${id}`)
}

export function fetchTrashNotes() {
  return getData<NbNoteTrashItem[]>('/api/notes/trash')
}

export function restoreNote(id: number) {
  return postData(`/api/notes/${id}/restore`)
}

export function purgeNote(id: number) {
  return deleteData(`/api/notes/${id}/purge`)
}

export function fetchNoteTags() {
  return getData<NbNoteTag[]>('/api/note-tags')
}

export function createNoteTag(body: NbNoteTagSaveRequest) {
  return postData<NbNoteTag>('/api/note-tags', body)
}

export function updateNoteTag(id: number, body: NbNoteTagSaveRequest) {
  return putData<NbNoteTag>(`/api/note-tags/${id}`, body)
}

export function removeNoteTag(id: number) {
  return deleteData(`/api/note-tags/${id}`)
}
