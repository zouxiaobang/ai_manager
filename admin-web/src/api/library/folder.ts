import { getData, postData, putData, deleteData } from '../request'
import type { DocFolder, DocFolderSaveRequest } from './types'

export function fetchFolderTree() {
  return getData<DocFolder[]>('/api/library/folders/tree')
}

export function createFolder(body: DocFolderSaveRequest) {
  return postData<DocFolder>('/api/library/folders', body)
}

export function updateFolder(id: number, body: DocFolderSaveRequest) {
  return putData<DocFolder>(`/api/library/folders/${id}`, body)
}

export function removeFolder(id: number) {
  return deleteData(`/api/library/folders/${id}`)
}
