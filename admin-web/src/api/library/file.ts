import { getData, postData, putData, deleteData } from '../request'
import type { DocFile, DocFileDetail, DocFileRenameRequest, DocFileMoveRequest, DocFileBatchMoveRequest, DocSearchRequest, DocTrashItem, DocStats, LibraryEvent } from './types'

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

export function fetchFiles(params: { folderId?: number | null; sort?: string; order?: string; page?: number; size?: number }) {
  return getData<PageResult<DocFile>>('/api/library/files', params as Record<string, unknown>)
}

export function fetchFileDetail(id: number) {
  return getData<DocFileDetail>(`/api/library/files/${id}`)
}

export function downloadFile(id: number) {
  const base = import.meta.env.VITE_API_BASE || ''
  window.open(`${base}/api/library/files/${id}/download`, '_blank')
}

export function previewFile(id: number) {
  const base = import.meta.env.VITE_API_BASE || ''
  return `${base}/api/library/files/${id}/preview`
}

export function renameFile(id: number, body: DocFileRenameRequest) {
  return putData(`/api/library/files/${id}/rename`, body)
}

export function moveFile(id: number, body: DocFileMoveRequest) {
  return putData(`/api/library/files/${id}/move`, body)
}

export function togglePin(id: number) {
  return putData(`/api/library/files/${id}/pin`)
}

export function updateFileDescription(id: number, description: string) {
  return putData(`/api/library/files/${id}/description`, { description })
}

export function removeFile(id: number) {
  return deleteData(`/api/library/files/${id}`)
}

export function batchDeleteFiles(ids: number[]) {
  return postData('/api/library/files/batch/delete', { ids })
}

export function batchMoveFiles(body: DocFileBatchMoveRequest) {
  return postData('/api/library/files/batch/move', body)
}

export function batchDownloadFiles(ids: number[]) {
  const base = import.meta.env.VITE_API_BASE || ''
  const params = new URLSearchParams()
  ids.forEach(id => params.append('ids', String(id)))
  window.open(`${base}/api/library/files/batch/download?${params.toString()}`, '_blank')
}

export function fetchTrashFiles() {
  return getData<DocTrashItem[]>('/api/library/trash')
}

export function restoreFile(id: number) {
  return postData(`/api/library/trash/${id}/restore`)
}

export function purgeFile(id: number) {
  return deleteData(`/api/library/trash/${id}`)
}

export function purgeAllTrash() {
  return deleteData('/api/library/trash')
}

export function toggleKbStatus(id: number) {
  return putData<string>(`/api/library/kb/files/${id}/status`)
}

export function fetchKbFiles(params: { page?: number; size?: number }) {
  return getData<PageResult<DocFile>>('/api/library/kb/files', params as Record<string, unknown>)
}

export function fetchKbStats() {
  return getData<DocStats>('/api/library/kb/stats')
}

export function fetchFavorites(params: { page?: number; size?: number }) {
  return getData<PageResult<DocFile>>('/api/library/favorites', params as Record<string, unknown>)
}

export function fetchRecentFiles(limit?: number) {
  return getData<DocFile[]>('/api/library/recent', { limit })
}

export function searchFiles(params: DocSearchRequest) {
  return getData<PageResult<DocFile>>('/api/library/search', params as Record<string, unknown>)
}

export function fetchStats() {
  return getData<DocStats>('/api/library/stats')
}

export function fetchFolderStats(folderId: number) {
  return getData<DocStats>(`/api/library/folders/${folderId}/stats`)
}

export function uploadFile(folderId: number | null, file: File) {
  const formData = new FormData()
  if (folderId !== null) formData.append('folderId', String(folderId))
  formData.append('file', file)
  const base = import.meta.env.VITE_API_BASE || ''
  return fetch(`${base}/api/library/upload`, {
    method: 'POST',
    body: formData,
  }).then(r => r.json())
}

export function uploadFiles(folderId: number | null, files: File[]) {
  const formData = new FormData()
  if (folderId !== null) formData.append('folderId', String(folderId))
  files.forEach(f => formData.append('files', f))
  const base = import.meta.env.VITE_API_BASE || ''
  return fetch(`${base}/api/library/upload/batch`, {
    method: 'POST',
    body: formData,
  }).then(r => r.json())
}

export function incrementView(id: number) {
  return postData(`/api/library/files/${id}/view`)
}

export function submitEvents(events: LibraryEvent[]) {
  return postData('/api/library/events', events, { silent: true })
}
