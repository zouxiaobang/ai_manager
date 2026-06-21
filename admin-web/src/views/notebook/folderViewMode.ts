export type FolderContentViewMode =
  | 'folder-card'
  | 'folder-detail-list'
  | 'folder-title-list'
  | 'note-card'
  | 'note-detail-list'
  | 'note-title-list'

export const FOLDER_VIEW_MODE_STORAGE_KEY = 'notebook-folder-content-view-mode'

export const FOLDER_CONTENT_VIEW_MODES: FolderContentViewMode[] = [
  'folder-card',
  'folder-detail-list',
  'folder-title-list',
  'note-card',
  'note-detail-list',
  'note-title-list',
]

export function isFolderContentViewMode(value: string): value is FolderContentViewMode {
  return FOLDER_CONTENT_VIEW_MODES.includes(value as FolderContentViewMode)
}

export function loadFolderContentViewMode(): FolderContentViewMode {
  const saved = localStorage.getItem(FOLDER_VIEW_MODE_STORAGE_KEY)
  if (saved && isFolderContentViewMode(saved)) return saved
  return 'folder-card'
}

export function saveFolderContentViewMode(mode: FolderContentViewMode) {
  localStorage.setItem(FOLDER_VIEW_MODE_STORAGE_KEY, mode)
}
