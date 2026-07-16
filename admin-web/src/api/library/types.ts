export interface DocFolder {
  id: number
  parentId: number | null
  name: string
  icon?: string
  color?: string
  sortOrder: number
  children: DocFolder[]
}

export interface DocFile {
  id: number
  folderId: number | null
  name: string
  originalName: string
  extension: string
  mimeType: string
  fileSize: number
  thumbnailPath?: string
  isPinned: number
  description?: string
  kbStatus: string
  viewCount: number
  downloadCount: number
  createTime: string
  updateTime: string
  tags?: DocTag[]
}

export interface DocFileDetail {
  id: number
  folderId: number | null
  name: string
  originalName: string
  extension: string
  mimeType: string
  fileSize: number
  storageType: string
  contentHash?: string
  thumbnailPath?: string
  isPinned: number
  description?: string
  sortOrder: number
  kbStatus: string
  kbError?: string
  kbProcessedAt?: string
  viewCount: number
  downloadCount: number
  createTime: string
  updateTime: string
  tags?: DocTag[]
}

export interface DocTag {
  id: number
  name: string
  color?: string
  createTime?: string
}

export interface DocStats {
  totalFiles: number
  totalSize: number
  imageCount: number
  documentCount: number
  archiveCount: number
  videoCount: number
  otherCount: number
  kbReadyCount: number
  kbProcessingCount: number
}

export interface DocFolderSaveRequest {
  parentId?: number | null
  name: string
  icon?: string
  color?: string
  sortOrder?: number
}

export interface DocFileRenameRequest {
  name: string
}

export interface DocFileMoveRequest {
  folderId: number | null
}

export interface DocFileBatchMoveRequest {
  ids: number[]
  folderId: number | null
}

export interface DocTagSaveRequest {
  name: string
  color?: string
}

export interface DocSearchRequest {
  keyword?: string
  extension?: string
  tagId?: number
  dateFrom?: string
  dateTo?: string
  page?: number
  size?: number
}

export interface DocTrashItem {
  id: number
  name: string
  extension: string
  fileSize: number
  deletedAt: string
  folderId: number | null
  folderName?: string
}

export interface DocFileTagBatchRequest {
  fileIds: number[]
  tagIds: number[]
}

export interface LibraryEvent {
  event: string
  fileId?: number | null
  folderId?: number | null
  paramsJson?: string | null
}

export type ViewMode = 'grid' | 'list'
export type SortField = 'name' | 'size' | 'updateTime'
export type SortOrder = 'asc' | 'desc'
