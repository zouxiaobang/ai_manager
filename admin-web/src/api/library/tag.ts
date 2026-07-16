import { getData, postData, putData, deleteData } from '../request'
import type { DocTag, DocTagSaveRequest } from './types'

export function fetchAllTags() {
  return getData<DocTag[]>('/api/library/tags')
}

export function createTag(body: DocTagSaveRequest) {
  return postData<DocTag>('/api/library/tags', body)
}

export function updateTag(id: number, body: DocTagSaveRequest) {
  return putData<DocTag>(`/api/library/tags/${id}`, body)
}

export function removeTag(id: number) {
  return deleteData(`/api/library/tags/${id}`)
}

export function syncFileTags(fileId: number, tagIds: number[]) {
  return postData(`/api/library/tags/file/${fileId}/tags`, { tagIds })
}
