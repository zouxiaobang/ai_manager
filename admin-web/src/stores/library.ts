import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import type { DocFolder, DocFile, DocTag, DocStats, ViewMode, SortField, SortOrder, DocTrashItem, DocSearchRequest } from '@/api/library/types'
import * as folderApi from '@/api/library/folder'
import * as fileApi from '@/api/library/file'
import * as tagApi from '@/api/library/tag'
import { submitEvents } from '@/api/library/file'
import type { LibraryEvent } from '@/api/library/types'

export const useLibraryStore = defineStore('library', () => {
  const tree = ref<DocFolder[]>([])
  const currentFolder = ref<DocFolder | null>(null)
  const files = ref<DocFile[]>([])
  const totalFiles = ref(0)
  const currentPage = ref(1)
  const pageSize = ref(50)
  const viewMode = ref<ViewMode>('grid')
  const sortField = ref<SortField>('updateTime')
  const sortOrder = ref<SortOrder>('desc')
  const tags = ref<DocTag[]>([])
  const trashItems = ref<DocTrashItem[]>([])
  const stats = ref<DocStats | null>(null)
  const selectedFiles = ref<Set<number>>(new Set())
  const loading = ref(false)
  const searchKeyword = ref('')
  const eventQueue = ref<LibraryEvent[]>([])

  const isAllSelected = computed(() => {
    return files.value.length > 0 && selectedFiles.value.size === files.value.length
  })

  let flushTimer: ReturnType<typeof setTimeout> | null = null

  function flushEvents() {
    if (eventQueue.value.length === 0) return
    const batch = [...eventQueue.value]
    eventQueue.value = []
    submitEvents(batch).catch(() => {})
  }

  function track(event: LibraryEvent) {
    eventQueue.value.push(event)
    if (eventQueue.value.length >= 20) {
      flushEvents()
    } else if (!flushTimer) {
      flushTimer = setTimeout(() => {
        flushTimer = null
        flushEvents()
      }, 30000)
    }
  }

  async function loadTree() {
    tree.value = await folderApi.fetchFolderTree()
  }

  async function loadFiles(opts: { page?: number; pageSize?: number; folderId?: number | null; sortField?: string; sortOrder?: string; favorite?: boolean } = {}) {
    loading.value = true
    try {
      const params: Record<string, unknown> = {
        page: opts.page ?? currentPage.value,
        size: opts.pageSize ?? pageSize.value,
        sort: opts.sortField ?? sortField.value,
        order: opts.sortOrder ?? sortOrder.value,
      }
      if (opts.folderId !== undefined && opts.folderId !== null) params.folderId = opts.folderId
      if (opts.favorite) params.favorite = 'true'
      const result = await fileApi.fetchFiles(params)
      files.value = result.records
      totalFiles.value = result.total
      if (opts.page) currentPage.value = opts.page
    } finally {
      loading.value = false
    }
  }

  async function loadTags() {
    tags.value = await tagApi.fetchAllTags()
  }

  async function loadStats() {
    stats.value = await fileApi.fetchStats()
  }

  async function loadTrashFiles() {
    trashItems.value = await fileApi.fetchTrashFiles()
  }

  async function searchFiles(params: DocSearchRequest) {
    loading.value = true
    try {
      const result = await fileApi.searchFiles(params)
      files.value = result.records
      totalFiles.value = result.total
    } finally {
      loading.value = false
    }
  }

  async function createFolderAction(name: string, parentId: number | null = null) {
    const created = await folderApi.createFolder({ name, parentId })
    await loadTree()
    return created
  }

  async function renameFolderAction(id: number, name: string) {
    await folderApi.updateFolder(id, { name })
    await loadTree()
  }

  async function deleteFolderAction(id: number) {
    await folderApi.removeFolder(id)
    await loadTree()
  }

  function setCurrentFolder(folder: DocFolder | null) {
    currentFolder.value = folder
  }

  async function renameFileAction(id: number) {
    const { t } = useI18n()
    try {
      const { value } = await ElMessageBox.prompt(t('library.renamePlaceholder'), t('library.rename'), {
        inputPattern: /^.+$/,
        inputErrorMessage: t('library.renameRequired'),
      })
      await fileApi.renameFile(id, { name: value })
      ElMessage.success(t('library.renameSuccess'))
      await loadFiles({})
    } catch {
      // cancelled
    }
  }

  async function moveFileAction(id: number) {
    const { t } = useI18n()
    try {
      const { value } = await ElMessageBox.prompt(t('library.movePlaceholder'), t('library.move'), {
        inputValue: '0',
      })
      const folderId = value ? parseInt(value) : null
      await fileApi.moveFile(id, { folderId })
      ElMessage.success(t('library.moveSuccess'))
      await loadFiles({})
    } catch {
      // cancelled
    }
  }

  async function deleteFileAction(id: number) {
    const { t } = useI18n()
    try {
      await ElMessageBox.confirm(t('library.confirmDelete'), t('library.delete'), {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        type: 'warning',
      })
      await fileApi.removeFile(id)
      ElMessage.success(t('library.deleteSuccess'))
      await loadFiles({})
    } catch {
      // cancelled
    }
  }

  async function batchDeleteFilesAction() {
    const { t } = useI18n()
    if (selectedFiles.value.size === 0) {
      ElMessage.warning(t('library.selectFilesFirst'))
      return
    }
    try {
      const ids = Array.from(selectedFiles.value)
      await ElMessageBox.confirm(t('library.confirmBatchDelete', { count: ids.length }), t('library.delete'), {
        type: 'warning',
      })
      await fileApi.batchDeleteFiles(ids)
      selectedFiles.value = new Set()
      ElMessage.success(t('library.batchDeleteSuccess'))
      await loadFiles({})
    } catch {
      // cancelled
    }
  }

  async function batchMoveFilesAction() {
    const { t } = useI18n()
    if (selectedFiles.value.size === 0) {
      ElMessage.warning(t('library.selectFilesFirst'))
      return
    }
    try {
      const { value } = await ElMessageBox.prompt(t('library.movePlaceholder'), t('library.move'), {
        inputValue: '0',
      })
      const ids = Array.from(selectedFiles.value)
      const folderId = value ? parseInt(value) : null
      await fileApi.batchMoveFiles({ ids, folderId })
      selectedFiles.value = new Set()
      ElMessage.success(t('library.batchMoveSuccess'))
      await loadFiles({})
    } catch {
      // cancelled
    }
  }

  async function togglePinAction(id: number) {
    await fileApi.togglePin(id)
    await loadFiles({})
  }

  async function toggleKbStatusAction(id: number) {
    const status = await fileApi.toggleKbStatus(id)
    const { t } = useI18n()
    ElMessage.success(t(status === 'PENDING' ? 'library.kbMarked' : 'library.kbUnmarked'))
    await loadFiles({})
  }

  async function manageTagsAction(fileId: number) {
    const { t } = useI18n()
    try {
      const fileDetail = await fileApi.fetchFileDetail(fileId)
      const fileTagIds = fileDetail.tags?.map(tg => tg.id) ?? []
      const { value } = await ElMessageBox.prompt(
        t('library.tagInputPlaceholder'),
        t('library.tag'),
        { inputValue: fileTagIds.join(','), inputPlaceholder: t('library.tagInputPlaceholder') },
      )
      if (value) {
        const tagIds = value.split(',').map(s => parseInt(s.trim())).filter(n => !isNaN(n))
        await tagApi.syncFileTags(fileId, tagIds)
        ElMessage.success(t('library.tagSuccess'))
      }
    } catch {
      // cancelled
    }
  }

  async function copyFileLinkAction(id: number) {
    const { t } = useI18n()
    const base = import.meta.env.VITE_API_BASE || ''
    const url = `${window.location.origin}${base}/api/library/files/${id}/download`
    try {
      await navigator.clipboard.writeText(url)
      ElMessage.success(t('library.copySuccess'))
    } catch {
      ElMessage.error(t('library.copyFailed'))
    }
  }

  async function downloadFileAction(id: number) {
    fileApi.downloadFile(id)
  }

  async function restoreTrashAction(id: number) {
    await fileApi.restoreFile(id)
    await loadTrashFiles()
  }

  async function purgeTrashAction(id: number) {
    await fileApi.purgeFile(id)
    await loadTrashFiles()
  }

  async function purgeAllTrashAction() {
    const { t } = useI18n()
    try {
      await ElMessageBox.confirm(t('library.confirmPurgeAll'), t('library.purge'), { type: 'warning' })
      await fileApi.purgeAllTrash()
      ElMessage.success(t('library.purgeSuccess'))
      await loadTrashFiles()
    } catch {
      // cancelled
    }
  }

  function toggleSelect(id: number) {
    const newSet = new Set(selectedFiles.value)
    if (newSet.has(id)) newSet.delete(id)
    else newSet.add(id)
    selectedFiles.value = newSet
  }

  function toggleSelectAll() {
    if (isAllSelected.value) {
      selectedFiles.value = new Set()
    } else {
      selectedFiles.value = new Set(files.value.map(f => f.id))
    }
  }

  function clearSelection() {
    selectedFiles.value = new Set()
  }

  function setViewMode(mode: ViewMode) {
    viewMode.value = mode
  }

  function setSort(field: SortField, order: SortOrder) {
    sortField.value = field
    sortOrder.value = order
    loadFiles({})
  }

  return {
    tree, currentFolder, files, totalFiles, currentPage, pageSize,
    viewMode, sortField, sortOrder, tags, trashItems, stats,
    selectedFiles, loading, searchKeyword, isAllSelected,
    track, loadTree, loadFiles, loadTags, loadStats, loadTrashFiles, searchFiles,
    setCurrentFolder,
    createFolder: createFolderAction,
    renameFolder: renameFolderAction,
    deleteFolder: deleteFolderAction,
    renameFile: renameFileAction,
    moveFile: moveFileAction,
    deleteFile: deleteFileAction,
    batchDeleteFiles: batchDeleteFilesAction,
    batchMoveFiles: batchMoveFilesAction,
    togglePin: togglePinAction,
    toggleKbStatus: toggleKbStatusAction,
    manageTags: manageTagsAction,
    copyFileLink: copyFileLinkAction,
    downloadFile: downloadFileAction,
    restoreTrash: restoreTrashAction,
    purgeTrash: purgeTrashAction,
    purgeAllTrash: purgeAllTrashAction,
    toggleSelect, toggleSelectAll, clearSelection,
    setViewMode, setSort,
  }
})
