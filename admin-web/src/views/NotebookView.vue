<template>
  <div class="notebook-page">
    <h2 class="notebook-page__title">{{ t('notebook.title') }}</h2>

    <el-alert
      v-if="baiduPanStatus && !baiduPanStatus.authorized"
      class="notebook-page__baidu-alert"
      type="warning"
      :closable="false"
      show-icon
      :title="t('notebook.baiduPanRequiredTitle')"
    >
      <template #default>
        <span>{{ t('notebook.baiduPanRequiredDesc') }}</span>
        <el-button
          v-if="baiduPanStatus.authorizeUrl"
          type="primary"
          link
          @click="openBaiduPanAuthorize"
        >
          {{ t('notebook.baiduPanConnect') }}
        </el-button>
      </template>
    </el-alert>

    <el-tabs v-model="activeTab" class="notebook-tabs" @tab-change="onTabChange">
      <el-tab-pane :label="t('notebook.tabs.all')" name="all">
        <div class="notebook-layout">
          <aside class="notebook-sidebar">
            <div class="notebook-sidebar__header">
              <div class="notebook-sidebar__tabs">
                <button
                  type="button"
                  class="notebook-sidebar__tab"
                  :class="{ 'is-active': sidebarTab === 'all' }"
                  @click="switchSidebarTab('all')"
                >
                  {{ t('notebook.tabs.all') }}
                </button>
                <button
                  type="button"
                  class="notebook-sidebar__tab"
                  :class="{ 'is-active': sidebarTab === 'toc' }"
                  :disabled="!currentNote"
                  @click="switchSidebarTab('toc')"
                >
                  {{ t('notebook.tabs.toc') }}
                </button>
              </div>
              <div
                class="notebook-sidebar__actions"
                :class="{ 'is-hidden': sidebarTab !== 'all' }"
              >
                <el-button
                  link
                  type="primary"
                  :title="t('notebook.newNote')"
                  @click="openCreateNote"
                >
                  <el-icon><Plus /></el-icon>
                </el-button>
                <el-button
                  link
                  type="primary"
                  :title="t('notebook.newFolder')"
                  @click="openCreateFolder"
                >
                  <el-icon><FolderAdd /></el-icon>
                </el-button>
                <el-button link :title="t('notebook.refresh')" @click="loadTree">
                  <el-icon><Refresh /></el-icon>
                </el-button>
              </div>
            </div>

            <div class="notebook-sidebar__body">
              <div v-show="sidebarTab === 'all'" class="notebook-sidebar__all-pane">
                <el-input
                  v-model="filterText"
                  size="small"
                  clearable
                  :placeholder="t('notebook.searchPlaceholder')"
                  :prefix-icon="Search"
                  class="notebook-sidebar__search"
                />

                <div
                  v-loading="treeLoading"
                  class="notebook-sidebar__tree-wrap"
                  @click="onTreeBlankClick"
                >
                <el-tree
                  v-if="treeData.length"
                  ref="treeRef"
                  :data="treeData"
                  node-key="nodeKey"
                  :props="treeProps"
                  :expand-on-click-node="false"
                  :filter-node-method="filterNode"
                  highlight-current
                  @node-click="onTreeNodeClick"
                  @node-expand="onTreeNodeExpand"
                  @node-collapse="onTreeNodeCollapse"
                >
                    <template #default="{ data }">
                      <span
                        class="notebook-tree-node"
                        :class="{
                          'is-note': data.nodeType === 'NOTE',
                          'is-active': data.nodeKey === activeNodeKey,
                        }"
                        @contextmenu="onTreeNodeContextMenu($event, data)"
                      >
                        <el-icon v-if="data.nodeType === 'FOLDER'" class="notebook-tree-node__icon">
                          <Folder />
                        </el-icon>
                        <el-icon v-else class="notebook-tree-node__icon">
                          <Document />
                        </el-icon>
                        <span class="notebook-tree-node__label">{{ data.name }}</span>
                        <el-icon v-if="data.isPinned === 1" class="notebook-tree-node__pin">
                          <Top />
                        </el-icon>
                      </span>
                    </template>
                  </el-tree>
                  <el-empty v-else :description="t('notebook.emptyTree')" :image-size="64" />
                </div>
              </div>

              <div v-show="sidebarTab === 'toc'" class="notebook-sidebar__toc-pane">
                <NoteTocPanel
                  :items="tocItems"
                  :empty-text="t('notebook.tocEmpty')"
                  @select="onTocItemClick"
                />
              </div>
            </div>
          </aside>

          <main class="notebook-main">
            <div v-if="currentNote" class="notebook-editor">
              <div class="notebook-editor__header">
              <div class="notebook-editor__toolbar">
                <div class="notebook-editor__meta">
                  <el-button
                    link
                    :type="currentNote.isPinned === 1 ? 'primary' : 'default'"
                    @click="togglePin"
                  >
                    <el-icon><Top /></el-icon>
                    {{ currentNote.isPinned === 1 ? t('notebook.unpin') : t('notebook.pin') }}
                  </el-button>
                  <el-button
                    link
                    :type="currentNote.isFavorite === 1 ? 'warning' : 'default'"
                    @click="toggleFavorite"
                  >
                    <el-icon><Star /></el-icon>
                    {{ currentNote.isFavorite === 1 ? t('notebook.unfavorite') : t('notebook.favorite') }}
                  </el-button>
                  <span v-if="contentLoading" class="notebook-editor__save-hint">
                    {{ t('notebook.contentLoading') }}
                  </span>
                  <span v-else-if="saveState === 'saving'" class="notebook-editor__save-hint">
                    {{ t('notebook.saving') }}
                  </span>
                  <span v-else-if="contentLoadBlocked" class="notebook-editor__save-hint is-error">
                    {{ t('notebook.contentLoadSaveBlocked') }}
                  </span>
                  <span v-else-if="saveState === 'saved'" class="notebook-editor__save-hint is-ok">
                    {{ t('notebook.saved') }}
                  </span>
                  <span
                    v-if="!contentLoadBlocked && currentNote.syncStatus === 'SYNCING'"
                    class="notebook-editor__save-hint"
                  >
                    {{ t('notebook.syncing') }}
                  </span>
                  <span
                    v-else-if="!contentLoadBlocked && currentNote.syncStatus === 'FAILED'"
                    class="notebook-editor__save-hint is-error"
                  >
                    {{ t('notebook.syncFailed') }}
                  </span>
                </div>
                <div v-if="currentNote.updateTime" class="notebook-editor__time">
                  {{ t('notebook.updatedAt') }}：{{ currentNote.updateTime }}
                </div>
              </div>

              <div class="notebook-editor__title-row">
                <el-input
                  v-model="editForm.title"
                  class="notebook-editor__title"
                  :placeholder="t('notebook.titlePlaceholder')"
                  :disabled="contentLoading"
                  @input="scheduleSave"
                />
                <div class="notebook-editor__optimize-wrap">
                  <el-popover placement="bottom-end" :width="360" trigger="hover">
                    <template #reference>
                      <el-button
                        text
                        class="notebook-editor__format-hint"
                        :title="t('notebook.formatStandardTitle')"
                      >
                        <el-icon><QuestionFilled /></el-icon>
                      </el-button>
                    </template>
                    <div class="notebook-format-standard">
                      <div class="notebook-format-standard__title">
                        {{ t('notebook.formatStandardTitle') }}
                      </div>
                      <ol class="notebook-format-standard__list">
                        <li
                          v-for="ruleId in NOTE_FORMAT_RULE_IDS"
                          :key="ruleId"
                        >
                          {{ t(`notebook.formatRules.${ruleId}`) }}
                        </li>
                      </ol>
                    </div>
                  </el-popover>
                  <el-button
                    plain
                    class="notebook-editor__optimize-btn"
                    :disabled="contentLoading"
                    @click="onOptimizeContent"
                  >
                    {{ t('notebook.optimize') }}
                  </el-button>
                </div>
              </div>

              <div class="notebook-editor__tags">
                <span class="notebook-editor__tags-label">{{ t('notebook.tags') }}</span>
                <el-select
                  v-model="editForm.tagIds"
                  multiple
                  filterable
                  collapse-tags
                  collapse-tags-tooltip
                  style="flex: 1"
                  :placeholder="t('notebook.tags')"
                  :disabled="contentLoading"
                  @change="scheduleSave"
                >
                  <el-option
                    v-for="tag in allTags"
                    :key="tag.id"
                    :label="tag.name"
                    :value="tag.id"
                  />
                </el-select>
                <el-button size="small" @click="tagDialogVisible = true">
                  {{ t('notebook.tagManage') }}
                </el-button>
              </div>
              </div>

              <div class="notebook-editor__content">
                <div v-if="contentLoading" class="notebook-editor__content-loading">
                  <el-icon class="notebook-editor__loading-icon is-loading"><Loading /></el-icon>
                  <span>{{ t('notebook.contentLoading') }}</span>
                </div>
                <NoteRichEditor
                  v-show="!contentLoading"
                  ref="editorRef"
                  :key="`${currentNote.id}-${editorRevision}`"
                  v-model="editForm.content"
                  class="notebook-editor__content-editor"
                  :placeholder="t('notebook.contentPlaceholder')"
                  @change="scheduleSave"
                />
              </div>
            </div>

            <NotebookFolderView
              v-else-if="selectedFolderNode"
              :folder-node="selectedFolderNode"
              :note-meta="folderNoteMeta"
              @open-folder="onFolderViewOpenFolder"
              @open-note="onFolderViewOpenNote"
            />

            <div v-else class="notebook-main__empty">
              <el-empty :description="t('notebook.selectNoteHint')" />
            </div>
          </main>
        </div>
      </el-tab-pane>

      <el-tab-pane :label="t('notebook.tabs.trash')" name="trash">
        <el-table v-loading="trashLoading" :data="trashItems" stripe border>
          <el-table-column prop="title" :label="t('notebook.titlePlaceholder')" min-width="200" />
          <el-table-column prop="notebookName" :label="t('notebook.notebookName')" width="160" />
          <el-table-column prop="updateTime" :label="t('notebook.updatedAt')" width="180" />
          <el-table-column :label="t('notebook.actions')" width="180" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="onRestore(row.id)">
                {{ t('notebook.restore') }}
              </el-button>
              <el-button link type="danger" @click="onPurge(row.id)">
                {{ t('notebook.purge') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!trashLoading && trashItems.length === 0" :description="t('notebook.trashEmpty')" />
      </el-tab-pane>
    </el-tabs>

    <NoteTreeContextMenu
      :visible="contextMenu.visible"
      :x="contextMenu.x"
      :y="contextMenu.y"
      :node="contextMenu.node"
      :can-paste="canPasteClipboard"
      @action="onContextMenuAction"
      @close="closeContextMenu"
    />

    <el-dialog
      v-model="moveNoteDialogVisible"
      :title="t('notebook.moveNoteTitle')"
      width="420px"
      destroy-on-close
    >
      <p class="notebook-move-hint">{{ t('notebook.moveNoteHint') }}</p>
      <el-tree
        ref="moveTreeRef"
        class="notebook-move-tree"
        :data="moveFolderTree"
        node-key="nodeKey"
        :props="treeProps"
        highlight-current
        default-expand-all
        :expand-on-click-node="false"
        @node-click="onMoveFolderPick"
      >
        <template #default="{ data }">
          <span class="notebook-move-folder-node">
            <el-icon v-if="data.nodeKey === 'folder-root'"><Folder /></el-icon>
            <el-icon v-else><Folder /></el-icon>
            <span>{{ data.name }}</span>
          </span>
        </template>
      </el-tree>
      <template #footer>
        <el-button @click="moveNoteDialogVisible = false">{{ t('pomodoro.common.cancel') }}</el-button>
        <el-button type="primary" :loading="moveNoteSubmitting" @click="submitMoveTarget">
          {{ t('pomodoro.common.save') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="folderDialogVisible"
      :title="folderDialogMode === 'create' ? t('notebook.createFolderTitle') : t('notebook.renameFolderTitle')"
      width="420px"
      destroy-on-close
    >
      <el-form @submit.prevent>
        <el-form-item :label="t('notebook.folderName')" required>
          <el-input v-model="folderForm.name" autofocus @keyup.enter="submitFolder" />
        </el-form-item>
        <el-form-item
          v-if="folderDialogMode === 'create'"
          :label="t('notebook.createFolderLocation')"
        >
          <el-radio-group v-model="folderForm.location" class="notebook-folder-location">
            <el-radio v-if="createFolderContext" value="inside">
              {{ t('notebook.createFolderInside', { name: createFolderContext.name }) }}
            </el-radio>
            <el-radio v-if="createFolderContext" value="sibling">
              {{ t('notebook.createFolderSibling', { name: createFolderContext.name }) }}
            </el-radio>
            <el-radio value="root">
              {{ t('notebook.createFolderRoot') }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="folderDialogVisible = false">{{ t('pomodoro.common.cancel') }}</el-button>
        <el-button type="primary" :loading="folderSubmitting" @click="submitFolder">
          {{ t('pomodoro.common.save') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="tagDialogVisible" :title="t('notebook.tagManage')" width="480px" destroy-on-close>
      <div class="tag-manage">
        <div class="tag-manage__new">
          <el-input v-model="newTagName" :placeholder="t('notebook.tagName')" />
          <el-button type="primary" :loading="tagSubmitting" @click="onCreateTag">
            {{ t('notebook.newTag') }}
          </el-button>
        </div>
        <el-table :data="allTags" size="small" max-height="280">
          <el-table-column prop="name" :label="t('notebook.tagName')" />
          <el-table-column :label="t('notebook.actions')" width="80">
            <template #default="{ row }">
              <el-button link type="danger" @click="onDeleteTag(row.id)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type ElTree } from 'element-plus'
import {
  Delete,
  Document,
  Folder,
  FolderAdd,
  Loading,
  Plus,
  QuestionFilled,
  Refresh,
  Search,
  Star,
  Top,
} from '@element-plus/icons-vue'
import NotebookFolderView from './notebook/NotebookFolderView.vue'
import NoteRichEditor from './notebook/NoteRichEditor.vue'
import NoteTocPanel from './notebook/NoteTocPanel.vue'
import NoteTreeContextMenu, { type TreeContextMenuAction } from './notebook/NoteTreeContextMenu.vue'
import { exportNoteAsWord } from './notebook/exportNoteWord'
import { parseNoteToc } from './notebook/noteToc'
import { hasNoteVisibleText, isSameNoteHtml, optimizeNoteHtml } from './notebook/noteContentOptimize'
import { NOTE_FORMAT_RULE_IDS } from './notebook/noteFormatStandard'
import type { NoteFolderListMeta } from './notebook/notePreview'
import { isContentLoadFailure } from './notebook/notePreview'
import {
  createNoteRequest,
  createNotebook,
  createNoteTag,
  fetchBaiduPanStatus,
  fetchNote,
  fetchNotesMeta,
  fetchNoteTags,
  fetchNotebookTree,
  fetchTrashNotes,
  purgeNote,
  removeNote,
  removeNoteTag,
  removeNotebook,
  restoreNote,
  updateNote,
  updateNoteKeepalive,
  updateNotebook,
  type NbNoteSaveRequest,
  type NbNoteDetail,
  type NbNoteTag,
  type NbNoteTrashItem,
  type NbTreeNode,
  type BaiduPanAuthStatus,
} from '@/api/notebook'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const baiduPanStatus = ref<BaiduPanAuthStatus | null>(null)

const activeTab = ref('all')
const sidebarTab = ref<'all' | 'toc'>('all')
const treeLoading = ref(false)
const trashLoading = ref(false)
const treeData = ref<NbTreeNode[]>([])
const trashItems = ref<NbNoteTrashItem[]>([])
const allTags = ref<NbNoteTag[]>([])
const filterText = ref('')
const treeRef = ref<InstanceType<typeof ElTree> | null>(null)
const moveTreeRef = ref<InstanceType<typeof ElTree> | null>(null)
const editorRef = ref<InstanceType<typeof NoteRichEditor> | null>(null)
const editorRevision = ref(0)
const contentLoadBlocked = ref(false)
const contentLoading = ref(false)
let noteLoadSeq = 0
const userExpandedKeys = ref<Set<string>>(new Set())
let searchExpandedSnapshot: Set<string> | null = null

const activeNodeKey = ref('')
const selectedFolderId = ref<number | null>(null)
const currentNote = ref<NbNoteDetail | null>(null)

const editForm = reactive({
  title: '',
  content: '',
  tagIds: [] as number[],
})

const saveState = ref<'idle' | 'saving' | 'saved'>('idle')
let saveTimer: ReturnType<typeof setTimeout> | null = null
let flushPromise: Promise<void> | null = null

type NoteSnapshot = { title: string; content: string; tagIds: string }

const savedSnapshot = ref<NoteSnapshot | null>(null)

function snapshotFromForm(): NoteSnapshot {
  return {
    title: editForm.title,
    content: editForm.content,
    tagIds: JSON.stringify([...editForm.tagIds].sort((a, b) => a - b)),
  }
}

function syncSavedSnapshot() {
  if (!currentNote.value) {
    savedSnapshot.value = null
    return
  }
  savedSnapshot.value = snapshotFromForm()
}

function isDirty(): boolean {
  if (!currentNote.value || !savedSnapshot.value) return false
  const cur = snapshotFromForm()
  const saved = savedSnapshot.value
  return (
    cur.title !== saved.title ||
    cur.content !== saved.content ||
    cur.tagIds !== saved.tagIds
  )
}

function clearSaveTimer() {
  if (saveTimer) {
    clearTimeout(saveTimer)
    saveTimer = null
  }
}

function buildSavePayload(): NbNoteSaveRequest {
  return {
    title: editForm.title,
    content: editForm.content,
    tagIds: editForm.tagIds,
    notebookId: currentNote.value!.notebookId,
  }
}

const folderDialogVisible = ref(false)
const folderDialogMode = ref<'create' | 'rename'>('create')
const folderSubmitting = ref(false)
const editingFolderId = ref<number | null>(null)
const folderForm = reactive({
  name: '',
  location: 'root' as 'inside' | 'sibling' | 'root',
})

const tagDialogVisible = ref(false)
const newTagName = ref('')
const tagSubmitting = ref(false)

const contextMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  node: null as NbTreeNode | null,
})

const copiedNoteClip = ref<{
  title: string
  content?: string
  tagIds?: number[]
} | null>(null)

const copiedFolderClip = ref<{
  name: string
} | null>(null)

const moveNoteDialogVisible = ref(false)
const moveNoteSubmitting = ref(false)
const moveDialogTarget = ref<{ kind: 'note' | 'folder'; id: number } | null>(null)
const moveTargetNotebookId = ref<number | null>(null)

const canPasteClipboard = computed(() => !!(copiedNoteClip.value || copiedFolderClip.value))

const treeProps = { label: 'name', children: 'children' }

const tocItems = computed(() => parseNoteToc(editForm.content))

const moveFolderTree = computed(() => {
  const rootOption: NbTreeNode = {
    nodeKey: 'folder-root',
    nodeType: 'FOLDER',
    name: t('notebook.moveNoteRoot'),
    children: buildFolderOnlyTree(treeData.value),
  }
  return [rootOption]
})

function buildFolderOnlyTree(nodes: NbTreeNode[]): NbTreeNode[] {
  return nodes
    .filter((node) => node.nodeType === 'FOLDER')
    .map((node) => ({
      ...node,
      children: node.children?.length ? buildFolderOnlyTree(node.children) : undefined,
    }))
}

function onTocItemClick(index: number) {
  editorRef.value?.scrollToHeading(index)
}

async function switchSidebarTab(tab: 'all' | 'toc') {
  if (tab === 'toc' && !currentNote.value) return
  sidebarTab.value = tab
  if (tab === 'all') {
    await restoreTreeSelectionView()
  }
}

async function restoreTreeSelectionView() {
  if (!activeNodeKey.value) return
  await nextTick()
  const ancestorKeys = collectAncestorFolderKeys(treeData.value, activeNodeKey.value) ?? []
  ancestorKeys.forEach((key) => {
    treeRef.value?.getNode(key)?.expand(null, true)
  })
  treeRef.value?.setCurrentKey(activeNodeKey.value)
  await nextTick()
  const treeEl = treeRef.value?.$el as HTMLElement | undefined
  treeEl?.querySelector('.el-tree-node.is-current')?.scrollIntoView({ block: 'nearest' })
}

watch(filterText, async (val) => {
  treeRef.value?.filter(val)
  await nextTick()
  const keyword = val.trim()
  if (keyword) {
    if (!searchExpandedSnapshot) {
      searchExpandedSnapshot = new Set(userExpandedKeys.value)
    }
    expandFoldersForSearch(keyword)
  } else {
    if (searchExpandedSnapshot) {
      userExpandedKeys.value = new Set(searchExpandedSnapshot)
      searchExpandedSnapshot = null
    }
    restoreUserTreeExpansion()
  }
})

function onTreeNodeExpand(data: NbTreeNode) {
  if (data.nodeType !== 'FOLDER' || filterText.value.trim()) return
  userExpandedKeys.value.add(data.nodeKey)
}

function onTreeNodeCollapse(data: NbTreeNode) {
  if (data.nodeType !== 'FOLDER' || filterText.value.trim()) return
  userExpandedKeys.value.delete(data.nodeKey)
}

function subtreeMatches(node: NbTreeNode, keyword: string): boolean {
  const lower = keyword.toLowerCase()
  if (node.name.toLowerCase().includes(lower)) return true
  return node.children?.some((child) => subtreeMatches(child, keyword)) ?? false
}

function filterNode(value: string, data: NbTreeNode) {
  if (!value) return true
  return subtreeMatches(data, value.trim())
}

function collectSearchExpandKeys(nodes: NbTreeNode[], keyword: string): string[] {
  const keys = new Set<string>()

  function walk(nodes: NbTreeNode[], ancestorFolders: string[]) {
    for (const node of nodes) {
      if (!subtreeMatches(node, keyword)) continue

      ancestorFolders.forEach((key) => keys.add(key))

      if (node.nodeType === 'FOLDER') {
        keys.add(node.nodeKey)
        if (node.children?.length) {
          walk(node.children, [...ancestorFolders, node.nodeKey])
        }
      } else if (node.children?.length) {
        walk(node.children, ancestorFolders)
      }
    }
  }

  walk(nodes, [])
  return [...keys]
}

function expandTreeNodes(keys: string[]) {
  const tree = treeRef.value
  if (!tree) return
  keys.forEach((key) => {
    tree.getNode(key)?.expand(null, true)
  })
}

function expandFoldersForSearch(keyword: string) {
  const keys = collectSearchExpandKeys(treeData.value, keyword)
  expandTreeNodes(keys)
}

function restoreUserTreeExpansion() {
  const tree = treeRef.value
  if (!tree) return

  tree.store._getAllNodes().forEach((node) => {
    if (node.childNodes?.length) node.collapse()
  })
  expandTreeNodes([...userExpandedKeys.value])
}

function ensureAncestorsExpanded(nodeKey: string) {
  const ancestorKeys = collectAncestorFolderKeys(treeData.value, nodeKey) ?? []
  ancestorKeys.forEach((key) => userExpandedKeys.value.add(key))
  expandTreeNodes(ancestorKeys)
}

async function loadTree(keepSelection = true) {
  treeLoading.value = true
  try {
    const prevKey = activeNodeKey.value
    treeData.value = await fetchNotebookTree()
    if (keepSelection && prevKey) {
      const stillExists = findNodeByKey(treeData.value, prevKey)
      if (stillExists) {
        activeNodeKey.value = prevKey
      } else {
        activeNodeKey.value = ''
      }
    }
    await nextTick()
    if (!filterText.value.trim()) {
      restoreUserTreeExpansion()
    }
    if (keepSelection && activeNodeKey.value) {
      treeRef.value?.setCurrentKey(activeNodeKey.value)
    }
  } finally {
    treeLoading.value = false
  }
}
function collectAncestorFolderKeys(
  nodes: NbTreeNode[],
  targetKey: string,
  ancestors: string[] = [],
): string[] | null {
  for (const node of nodes) {
    if (node.nodeKey === targetKey) {
      return ancestors
    }
    if (node.children?.length) {
      const nextAncestors =
        node.nodeType === 'FOLDER' ? [...ancestors, node.nodeKey] : ancestors
      const found = collectAncestorFolderKeys(node.children, targetKey, nextAncestors)
      if (found) return found
    }
  }
  return null
}

function findNodeByKey(nodes: NbTreeNode[], key: string): NbTreeNode | null {
  for (const node of nodes) {
    if (node.nodeKey === key) return node
    if (node.children?.length) {
      const found = findNodeByKey(node.children, key)
      if (found) return found
    }
  }
  return null
}

async function loadTags() {
  allTags.value = await fetchNoteTags()
}

async function loadTrash() {
  trashLoading.value = true
  try {
    trashItems.value = await fetchTrashNotes()
  } finally {
    trashLoading.value = false
  }
}

async function onTabChange(tab: string | number) {
  if (tab === 'trash') {
    void flushSaveInBackground()
    await loadTrash()
  }
}

function capturePendingSave(): { noteId: number; payload: NbNoteSaveRequest } | null {
  clearSaveTimer()
  if (contentLoadBlocked.value || contentLoading.value) {
    return null
  }
  if (!currentNote.value?.id || !isDirty()) {
    return null
  }
  const noteId = currentNote.value.id
  const payload = buildSavePayload()
  syncSavedSnapshot()
  return { noteId, payload }
}

async function saveNoteInBackground(noteId: number, payload: NbNoteSaveRequest) {
  try {
    await updateNote(noteId, payload)
  } catch {
    ElMessage.error(t('notebook.saveFailed'))
  }
}

function flushSaveInBackground() {
  const pending = capturePendingSave()
  if (!pending) return
  void saveNoteInBackground(pending.noteId, pending.payload)
}

async function onTreeNodeClick(data: NbTreeNode) {
  closeContextMenu()
  if (data.nodeKey === activeNodeKey.value) return

  const pending = capturePendingSave()

  if (data.nodeType === 'FOLDER') {
    selectedFolderId.value = data.notebookId ?? null
    activeNodeKey.value = data.nodeKey
    currentNote.value = null
    savedSnapshot.value = null
    contentLoadBlocked.value = false
    contentLoading.value = false
    sidebarTab.value = 'all'
  } else if (data.noteId) {
    beginOpenNote(data.noteId, buildNoteStubFromTree(data), data.nodeKey)
    ensureAncestorsExpanded(activeNodeKey.value)
  }

  if (pending) {
    void saveNoteInBackground(pending.noteId, pending.payload)
  }
}

function onTreeNodeContextMenu(event: MouseEvent, data: NbTreeNode) {
  if (data.nodeType === 'NOTE' && !data.noteId) return
  if (data.nodeType === 'FOLDER' && !data.notebookId) return
  if (data.nodeType !== 'NOTE' && data.nodeType !== 'FOLDER') return
  event.preventDefault()
  event.stopPropagation()
  contextMenu.visible = true
  contextMenu.x = event.clientX
  contextMenu.y = event.clientY
  contextMenu.node = data
}

function closeContextMenu() {
  contextMenu.visible = false
  contextMenu.node = null
}

function collectFolderSubtreeIds(folderId: number): number[] {
  const root = findNodeByKey(treeData.value, `folder-${folderId}`)
  if (!root) return [folderId]
  const ids: number[] = []
  const walk = (node: NbTreeNode) => {
    if (node.nodeType === 'FOLDER' && node.notebookId) {
      ids.push(node.notebookId)
      node.children?.forEach(walk)
    }
  }
  walk(root)
  return ids
}

async function onContextMenuAction(action: TreeContextMenuAction) {
  const node = contextMenu.node
  if (!node) return

  if (node.nodeType === 'FOLDER') {
    await onFolderContextMenuAction(action, node)
    return
  }

  if (!node.noteId) return

  switch (action) {
    case 'pin':
      await toggleNotePin(node)
      break
    case 'favorite':
      await toggleNoteFavorite(node)
      break
    case 'rename':
      await renameNoteFromMenu(node)
      break
    case 'move':
      openMoveNoteDialog(node.noteId)
      break
    case 'copy':
      await copyNoteToClipboard(node.noteId)
      break
    case 'paste':
      await pasteNoteFromClipboard(node)
      break
    case 'delete':
      await deleteNoteFromMenu(node)
      break
    case 'exportWord':
      await exportNoteFromMenu(node.noteId)
      break
  }
}

async function onFolderContextMenuAction(action: TreeContextMenuAction, node: NbTreeNode) {
  if (!node.notebookId) return

  switch (action) {
    case 'pin':
      await pinFolderFromMenu(node)
      break
    case 'rename':
      await renameFolderFromMenu(node)
      break
    case 'move':
      openMoveFolderDialog(node.notebookId)
      break
    case 'copy':
      copyFolderToClipboard(node)
      break
    case 'paste':
      await pasteIntoFolderFromMenu(node)
      break
    case 'delete':
      await deleteFolderFromMenu(node)
      break
    default:
      break
  }
}

async function toggleNotePin(node: NbTreeNode) {
  if (!node.noteId) return
  const pinned = node.isPinned !== 1
  await updateNote(node.noteId, { pinned })
  if (currentNote.value?.id === node.noteId) {
    currentNote.value = { ...currentNote.value, isPinned: pinned ? 1 : 0 }
  }
  await loadTree()
}

async function toggleNoteFavorite(node: NbTreeNode) {
  if (!node.noteId) return
  const favorite = node.isFavorite !== 1
  await updateNote(node.noteId, { favorite })
  if (currentNote.value?.id === node.noteId) {
    currentNote.value = { ...currentNote.value, isFavorite: favorite ? 1 : 0 }
  }
  await loadTree()
}

async function renameNoteFromMenu(node: NbTreeNode) {
  if (!node.noteId) return
  const { value } = await ElMessageBox.prompt(
    t('notebook.renameNoteTitle'),
    t('notebook.renameNote'),
    {
      inputValue: node.name,
      confirmButtonText: t('pomodoro.common.save'),
      cancelButtonText: t('pomodoro.common.cancel'),
    },
  )
  const title = value.trim()
  if (!title) return
  const updated = await updateNote(node.noteId, { title })
  if (currentNote.value?.id === node.noteId) {
    currentNote.value = updated
    editForm.title = updated.title
    syncSavedSnapshot()
  }
  await loadTree()
  treeRef.value?.setCurrentKey(node.nodeKey)
}

function openMoveNoteDialog(noteId: number) {
  moveDialogTarget.value = { kind: 'note', id: noteId }
  moveTargetNotebookId.value = null
  moveNoteDialogVisible.value = true
  nextTick(() => {
    moveTreeRef.value?.setCurrentKey('folder-root')
  })
}

function openMoveFolderDialog(folderId: number) {
  moveDialogTarget.value = { kind: 'folder', id: folderId }
  moveTargetNotebookId.value = null
  moveNoteDialogVisible.value = true
  nextTick(() => {
    moveTreeRef.value?.setCurrentKey('folder-root')
  })
}

function onMoveFolderPick(data: NbTreeNode) {
  moveTreeRef.value?.setCurrentKey(data.nodeKey)
  if (data.nodeKey === 'folder-root') {
    moveTargetNotebookId.value = null
    return
  }
  moveTargetNotebookId.value = data.notebookId ?? null
}

async function submitMoveTarget() {
  if (!moveDialogTarget.value) return
  moveNoteSubmitting.value = true
  try {
    const targetFolderId = moveTargetNotebookId.value
    if (moveDialogTarget.value.kind === 'folder') {
      const forbidden = collectFolderSubtreeIds(moveDialogTarget.value.id)
      if (targetFolderId != null && forbidden.includes(targetFolderId)) {
        ElMessage.warning(t('notebook.moveFolderInvalid'))
        return
      }
      const node = findNodeByKey(treeData.value, `folder-${moveDialogTarget.value.id}`)
      if (!node) return
      await updateNotebook(moveDialogTarget.value.id, {
        name: node.name,
        parentId: targetFolderId,
      })
    } else {
      await updateNote(moveDialogTarget.value.id, { notebookId: targetFolderId })
      if (currentNote.value?.id === moveDialogTarget.value.id) {
        currentNote.value.notebookId = targetFolderId ?? undefined
      }
    }
    moveNoteDialogVisible.value = false
    ElMessage.success(t('pomodoro.common.saved'))
    await loadTree()
  } finally {
    moveNoteSubmitting.value = false
  }
}

async function pinFolderFromMenu(node: NbTreeNode) {
  if (!node.notebookId) return
  await updateNotebook(node.notebookId, {
    name: node.name,
    parentId: node.parentId ?? null,
    sortOrder: -Date.now(),
  })
  await loadTree()
  treeRef.value?.setCurrentKey(node.nodeKey)
  ElMessage.success(t('notebook.pin'))
}

async function renameFolderFromMenu(node: NbTreeNode) {
  if (!node.notebookId) return
  const { value } = await ElMessageBox.prompt(
    t('notebook.renameFolderTitle'),
    t('notebook.renameFolder'),
    {
      inputValue: node.name,
      confirmButtonText: t('pomodoro.common.save'),
      cancelButtonText: t('pomodoro.common.cancel'),
    },
  )
  const name = value.trim()
  if (!name) return
  await updateNotebook(node.notebookId, {
    name,
    parentId: node.parentId ?? null,
  })
  await loadTree()
  treeRef.value?.setCurrentKey(node.nodeKey)
}

function copyFolderToClipboard(node: NbTreeNode) {
  copiedFolderClip.value = { name: node.name }
  ElMessage.success(t('notebook.copy'))
}

async function pasteIntoFolderFromMenu(node: NbTreeNode) {
  if (copiedNoteClip.value) {
    const created = await createNoteRequest({
      notebookId: node.notebookId ?? null,
      title: copiedNoteClip.value.title,
      content: copiedNoteClip.value.content,
      tagIds: copiedNoteClip.value.tagIds,
    })
    await loadTree()
    activeNodeKey.value = `note-${created.id}`
    ensureAncestorsExpanded(activeNodeKey.value)
    treeRef.value?.setCurrentKey(activeNodeKey.value)
    beginOpenNote(created.id, created, activeNodeKey.value)
    ElMessage.success(t('notebook.paste'))
    return
  }
  if (!copiedFolderClip.value) return
  await createNotebook({
    name: `${copiedFolderClip.value.name}${t('notebook.copySuffix')}`,
    parentId: node.notebookId ?? null,
  })
  await loadTree()
  ElMessage.success(t('notebook.paste'))
}

async function deleteFolderFromMenu(node: NbTreeNode) {
  if (!node.notebookId) return
  await ElMessageBox.confirm(t('notebook.deleteFolderConfirm'), { type: 'warning' })
  await removeNotebook(node.notebookId)
  if (activeNodeKey.value === node.nodeKey) {
    selectedFolderId.value = null
    activeNodeKey.value = ''
  }
  ElMessage.success(t('pomodoro.common.deleted'))
  await loadTree(false)
}

async function copyNoteToClipboard(noteId: number) {
  const detail = await fetchNote(noteId)
  copiedNoteClip.value = {
    title: `${detail.title}${t('notebook.copySuffix')}`,
    content: detail.content,
    tagIds: detail.tags?.map((tag) => tag.id),
  }
  ElMessage.success(t('notebook.copy'))
}

async function pasteNoteFromClipboard(node: NbTreeNode) {
  if (!copiedNoteClip.value) return
  const created = await createNoteRequest({
    notebookId: node.notebookId ?? null,
    title: copiedNoteClip.value.title,
    content: copiedNoteClip.value.content,
    tagIds: copiedNoteClip.value.tagIds,
  })
  await loadTree()
  activeNodeKey.value = `note-${created.id}`
  ensureAncestorsExpanded(activeNodeKey.value)
  treeRef.value?.setCurrentKey(activeNodeKey.value)
  beginOpenNote(created.id, created, activeNodeKey.value)
  ElMessage.success(t('notebook.paste'))
}

async function deleteNoteFromMenu(node: NbTreeNode) {
  if (!node.noteId) return
  await ElMessageBox.confirm(t('notebook.deleteNoteConfirm'), { type: 'warning' })
  await removeNote(node.noteId)
  if (currentNote.value?.id === node.noteId) {
    currentNote.value = null
    savedSnapshot.value = null
    contentLoading.value = false
    sidebarTab.value = 'all'
    activeNodeKey.value = ''
    editForm.title = ''
    editForm.content = ''
    editForm.tagIds = []
  }
  ElMessage.success(t('pomodoro.common.deleted'))
  await loadTree(false)
}

async function exportNoteFromMenu(noteId: number) {
  const detail = await fetchNote(noteId)
  exportNoteAsWord(detail.title, detail.content ?? '')
}

function buildNoteStubFromTree(data: NbTreeNode): NbNoteDetail {
  return {
    id: data.noteId!,
    notebookId: data.notebookId,
    title: data.name,
    noteType: 'RICH',
    isPinned: data.isPinned ?? 0,
    isFavorite: data.isFavorite ?? 0,
    status: 'ACTIVE',
  }
}

function beginOpenNote(noteId: number, stub: NbNoteDetail, nodeKey?: string) {
  const seq = ++noteLoadSeq
  if (nodeKey) {
    activeNodeKey.value = nodeKey
    selectedFolderId.value = stub.notebookId ?? null
  }
  currentNote.value = stub
  editForm.title = stub.title
  editForm.content = ''
  editForm.tagIds = []
  savedSnapshot.value = null
  saveState.value = 'idle'
  contentLoadBlocked.value = false
  contentLoading.value = true
  editorRevision.value += 1
  sidebarTab.value = 'toc'
  void loadNoteDetail(noteId, seq)
}

async function loadNoteDetail(noteId: number, seq?: number) {
  const expectedSeq = seq ?? ++noteLoadSeq
  if (seq === undefined) {
    contentLoading.value = true
  }
  contentLoadBlocked.value = false
  clearSaveTimer()
  try {
    const detail = await fetchNote(noteId)
    if (expectedSeq !== noteLoadSeq || currentNote.value?.id !== noteId) {
      return
    }
    currentNote.value = detail
    editForm.title = detail.title
    const rawContent = detail.content ?? ''
    let content = rawContent
    if (isContentLoadFailure(detail, rawContent)) {
      contentLoadBlocked.value = true
      ElMessage.warning(t('notebook.contentLoadFailed'))
      content = ''
      editorRevision.value += 1
    }
    editForm.content = content
    editForm.tagIds = detail.tags?.map((tag) => tag.id) ?? []
    syncSavedSnapshot()
    saveState.value = 'saved'
  } catch {
    if (expectedSeq !== noteLoadSeq || currentNote.value?.id !== noteId) {
      return
    }
    contentLoadBlocked.value = true
    ElMessage.warning(t('notebook.contentLoadFailed'))
    editForm.content = ''
    editorRevision.value += 1
    saveState.value = 'saved'
  } finally {
    if (expectedSeq === noteLoadSeq && currentNote.value?.id === noteId) {
      contentLoading.value = false
    }
  }
}

function scheduleSave() {
  if (!currentNote.value?.id || contentLoadBlocked.value || contentLoading.value) return
  saveState.value = 'idle'
  clearSaveTimer()
  saveTimer = setTimeout(() => {
    void saveCurrentNote()
  }, 1500)
}

function resolveEditorHtml(): string {
  const fromEditor = editorRef.value?.getHtml() ?? ''
  if (hasNoteVisibleText(fromEditor)) return fromEditor
  const fromForm = editForm.content ?? ''
  if (hasNoteVisibleText(fromForm)) return fromForm
  return fromEditor || fromForm
}

function onOptimizeContent() {
  if (!currentNote.value || contentLoadBlocked.value || contentLoading.value) return
  const source = resolveEditorHtml()
  if (!hasNoteVisibleText(source)) {
    ElMessage.info(t('notebook.optimizeNoChange'))
    return
  }
  const optimized = optimizeNoteHtml(source)
  if (!hasNoteVisibleText(optimized)) {
    ElMessage.warning(t('notebook.optimizeFailed'))
    return
  }
  if (isSameNoteHtml(optimized, source)) {
    ElMessage.info(t('notebook.optimizeNoChange'))
    return
  }
  editForm.content = optimized
  editorRevision.value += 1
  scheduleSave()
  ElMessage.success(t('notebook.optimizeDone'))
}

async function saveCurrentNote(options: { reloadTree?: boolean } = {}) {
  const { reloadTree = true } = options
  const noteId = currentNote.value?.id
  if (!noteId || contentLoadBlocked.value || contentLoading.value) return
  if (!isDirty()) {
    saveState.value = 'saved'
    return
  }
  const payload = buildSavePayload()
  saveState.value = 'saving'
  try {
    const updated = await updateNote(noteId, payload)
    if (currentNote.value?.id === noteId) {
      currentNote.value = updated
      syncSavedSnapshot()
    }
    saveState.value = 'saved'
    if (reloadTree && currentNote.value?.id === noteId) {
      await loadTree()
      treeRef.value?.setCurrentKey(activeNodeKey.value)
    }
  } catch {
    if (currentNote.value?.id === noteId) {
      saveState.value = 'idle'
    }
    ElMessage.error(t('notebook.saveFailed'))
    throw new Error('save failed')
  }
}

async function flushSave(options: { reloadTree?: boolean } = {}) {
  if (flushPromise) {
    await flushPromise
    if (!isDirty()) return
  }
  clearSaveTimer()
  if (!currentNote.value?.id || !isDirty()) return

  flushPromise = saveCurrentNote(options)
  try {
    await flushPromise
  } finally {
    flushPromise = null
  }
}

function flushSaveOnUnload() {
  clearSaveTimer()
  if (contentLoadBlocked.value || contentLoading.value) return
  const note = currentNote.value
  if (!note?.id || !isDirty()) return
  updateNoteKeepalive(note.id, buildSavePayload())
  syncSavedSnapshot()
}

async function openCreateNote() {
  const pending = capturePendingSave()
  if (pending) {
    void saveNoteInBackground(pending.noteId, pending.payload)
  }
  const created = await createNoteRequest({
    notebookId: selectedFolderId.value,
    title: t('notebook.untitled'),
    content: '',
    tagIds: [],
  })
  await loadTree()
  activeNodeKey.value = `note-${created.id}`
  ensureAncestorsExpanded(activeNodeKey.value)
  treeRef.value?.setCurrentKey(activeNodeKey.value)
  beginOpenNote(created.id, created, activeNodeKey.value)
}

const folderNoteMeta = ref<Record<number, NoteFolderListMeta>>({})

const selectedFolderNode = computed(() => {
  if (!activeNodeKey.value || currentNote.value) return null
  const node = findNodeByKey(treeData.value, activeNodeKey.value)
  return node?.nodeType === 'FOLDER' ? node : null
})

watch(
  selectedFolderNode,
  async (node) => {
    folderNoteMeta.value = {}
    if (!node?.children?.length) return
    const noteIds = node.children
      .filter((child) => child.nodeType === 'NOTE' && child.noteId)
      .map((child) => child.noteId as number)
    if (!noteIds.length) return
    try {
      const metaList = await fetchNotesMeta(noteIds)
      folderNoteMeta.value = Object.fromEntries(
        metaList.map((item) => [
          item.id,
          {
            contentExcerpt: item.contentExcerpt,
            createTime: item.createTime,
            size: item.contentSize ?? 0,
          },
        ]),
      )
    } catch {
      folderNoteMeta.value = {}
    }
  },
  { immediate: true },
)

async function loadBaiduPanStatus() {
  try {
    baiduPanStatus.value = await fetchBaiduPanStatus()
  } catch {
    baiduPanStatus.value = null
  }
}

function openBaiduPanAuthorize() {
  const url = baiduPanStatus.value?.authorizeUrl
  if (!url) return
  window.open(url, '_blank', 'noopener,noreferrer,width=600,height=720')
}

async function onFolderViewOpenFolder(node: NbTreeNode) {
  await onTreeNodeClick(node)
}

async function onFolderViewOpenNote(node: NbTreeNode) {
  await onTreeNodeClick(node)
}

const createFolderContext = computed(() => {
  if (activeNodeKey.value) {
    const node = findNodeByKey(treeData.value, activeNodeKey.value)
    if (!node) return null
    if (node.nodeType === 'FOLDER') return node
    if (node.notebookId) {
      return findNodeByKey(treeData.value, `folder-${node.notebookId}`)
    }
  }
  if (currentNote.value?.notebookId) {
    return findNodeByKey(treeData.value, `folder-${currentNote.value.notebookId}`)
  }
  return null
})

function onTreeBlankClick(event: MouseEvent) {
  if ((event.target as HTMLElement).closest('.el-tree-node')) return
  activeNodeKey.value = ''
  selectedFolderId.value = null
  treeRef.value?.setCurrentKey(undefined)
}

function getCreateFolderParentId(): number | null {
  if (folderForm.location === 'root') return null
  const context = createFolderContext.value
  if (!context) return null
  if (folderForm.location === 'sibling') return context.parentId ?? null
  return context.notebookId ?? null
}

function openCreateFolder() {
  folderDialogMode.value = 'create'
  editingFolderId.value = null
  folderForm.name = ''
  folderForm.location = createFolderContext.value ? 'inside' : 'root'
  folderDialogVisible.value = true
}

async function submitFolder() {
  const name = folderForm.name.trim()
  if (!name) {
    ElMessage.warning(t('notebook.folderName'))
    return
  }
  folderSubmitting.value = true
  try {
    if (folderDialogMode.value === 'create') {
      await createNotebook({
        name,
        parentId: getCreateFolderParentId(),
      })
    } else if (editingFolderId.value) {
      const node = findNodeByKey(treeData.value, `folder-${editingFolderId.value}`)
      await updateNotebook(editingFolderId.value, {
        name,
        parentId: node?.parentId ?? null,
      })
    }
    folderDialogVisible.value = false
    await loadTree()
  } finally {
    folderSubmitting.value = false
  }
}

async function togglePin() {
  if (!currentNote.value) return
  const pinned = currentNote.value.isPinned !== 1
  const updated = await updateNote(currentNote.value.id, { pinned })
  currentNote.value = updated
  await loadTree()
}

async function toggleFavorite() {
  if (!currentNote.value) return
  const favorite = currentNote.value.isFavorite !== 1
  const updated = await updateNote(currentNote.value.id, { favorite })
  currentNote.value = updated
}

async function onRestore(id: number) {
  await restoreNote(id)
  ElMessage.success(t('notebook.restore'))
  await loadTrash()
  await loadTree()
}

async function onPurge(id: number) {
  await ElMessageBox.confirm(t('notebook.purgeConfirm'), { type: 'warning' })
  await purgeNote(id)
  ElMessage.success(t('pomodoro.common.deleted'))
  await loadTrash()
}

async function onCreateTag() {
  const name = newTagName.value.trim()
  if (!name) return
  tagSubmitting.value = true
  try {
    await createNoteTag({ name })
    newTagName.value = ''
    await loadTags()
  } finally {
    tagSubmitting.value = false
  }
}

async function onDeleteTag(id: number) {
  await removeNoteTag(id)
  await loadTags()
  if (currentNote.value) {
    editForm.tagIds = editForm.tagIds.filter((tagId) => tagId !== id)
    scheduleSave()
  }
}

function openRenameFolder() {
  if (!selectedFolderNode.value?.notebookId) return
  folderDialogMode.value = 'rename'
  editingFolderId.value = selectedFolderNode.value.notebookId
  folderForm.name = selectedFolderNode.value.name
  folderDialogVisible.value = true
}

async function onDeleteFolder() {
  const folderId = selectedFolderNode.value?.notebookId
  if (!folderId) return
  await ElMessageBox.confirm(t('notebook.deleteFolderConfirm'), { type: 'warning' })
  await removeNotebook(folderId)
  selectedFolderId.value = null
  activeNodeKey.value = ''
  ElMessage.success(t('pomodoro.common.deleted'))
  await loadTree(false)
}

function onPageHide() {
  flushSaveOnUnload()
}

function onVisibilityChange() {
  if (document.visibilityState === 'hidden') {
    flushSaveInBackground()
  }
}

onBeforeRouteLeave(() => {
  flushSaveInBackground()
})

onMounted(async () => {
  window.addEventListener('pagehide', onPageHide)
  document.addEventListener('visibilitychange', onVisibilityChange)
  document.addEventListener('click', closeContextMenu)
  document.addEventListener('scroll', closeContextMenu, true)
  await Promise.all([loadTree(false), loadTags(), loadBaiduPanStatus()])
  if (route.query.baidu === 'connected') {
    ElMessage.success(t('notebook.baiduPanConnected'))
    await loadBaiduPanStatus()
    const { baidu: _baidu, ...rest } = route.query
    router.replace({ query: rest })
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('pagehide', onPageHide)
  document.removeEventListener('visibilitychange', onVisibilityChange)
  document.removeEventListener('click', closeContextMenu)
  document.removeEventListener('scroll', closeContextMenu, true)
  flushSaveOnUnload()
})
</script>

<style scoped lang="scss">
.notebook-page {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-sizing: border-box;
}

.notebook-page__title {
  margin: 0 0 16px;
  font-size: 18px;
  font-weight: 600;
  flex-shrink: 0;
}

.notebook-page__baidu-alert {
  margin-bottom: 12px;
  flex-shrink: 0;
}

:deep(.notebook-tabs) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  > .el-tabs__header {
    flex-shrink: 0;
    margin-bottom: 12px;
  }

  > .el-tabs__content {
    flex: 1;
    min-height: 0;
    overflow: hidden;
  }

  .el-tab-pane {
    display: flex;
    flex-direction: column;
    height: 100%;
    min-height: 0;
    overflow: hidden;
  }
}

.notebook-layout {
  display: flex;
  gap: 0;
  height: 100%;
  min-height: 0;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  overflow: hidden;
  background: var(--el-bg-color);
}

.notebook-sidebar {
  width: 300px;
  flex-shrink: 0;
  min-width: 0;
  overflow: hidden;
  border-right: 1px solid var(--el-border-color-lighter);
  display: flex;
  flex-direction: column;
  background: var(--el-fill-color-blank);
}

.notebook-sidebar__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 12px 12px 8px;
  min-height: 44px;
  box-sizing: border-box;
}

.notebook-sidebar__tabs {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
  flex: 1;
  height: 28px;
}

.notebook-sidebar__tab {
  display: inline-flex;
  align-items: center;
  height: 28px;
  border: none;
  background: transparent;
  padding: 0 8px;
  font-size: 12px;
  font-weight: 400;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  white-space: nowrap;

  &:hover:not(:disabled):not(.is-active) {
    color: var(--el-text-color-regular);
  }

  &.is-active {
    font-size: 15px;
    font-weight: 700;
    color: var(--el-text-color-primary);
    background: transparent;
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.45;
  }
}

.notebook-sidebar__title {
  font-weight: 600;
  font-size: 14px;
}

.notebook-sidebar__actions {
  display: flex;
  gap: 2px;
  flex-shrink: 0;
  min-width: 72px;
  justify-content: flex-end;

  &.is-hidden {
    visibility: hidden;
    pointer-events: none;
  }
}

.notebook-sidebar__body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.notebook-sidebar__all-pane,
.notebook-sidebar__toc-pane {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.notebook-sidebar__search {
  width: 100%;
  padding: 0 12px 8px;
  box-sizing: border-box;

  :deep(.el-input) {
    width: 100%;
  }
}

.notebook-sidebar__tree-wrap {
  flex: 1;
  overflow: auto;
  overscroll-behavior: contain;
  padding: 0 4px 12px;

  :deep(.el-tree--highlight-current .el-tree-node.is-current > .el-tree-node__content) {
    background: transparent;
  }
}

.notebook-tree-node {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;

  &.is-active {
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
  }
}

.notebook-tree-node__icon {
  flex-shrink: 0;
  font-size: 15px;
}

.notebook-tree-node__label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notebook-tree-node__pin {
  margin-left: auto;
  font-size: 12px;
  color: var(--el-color-primary);
}

.notebook-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  padding: 16px 20px;
  overflow: hidden;

  &:not(:has(.notebook-editor)) {
    overflow: auto;
  }
}

.notebook-editor {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.notebook-editor__header {
  flex-shrink: 0;
}

.notebook-main__empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.notebook-editor__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
}

.notebook-editor__meta {
  display: flex;
  align-items: center;
  gap: 4px;
}

.notebook-editor__save-hint {
  margin-left: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);

  &.is-ok {
    color: var(--el-color-success);
  }

  &.is-error {
    color: var(--el-color-danger);
  }
}

.notebook-editor__time {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.notebook-editor__title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.notebook-editor__title {
  flex: 1;
  min-width: 0;
  margin-bottom: 0;

  :deep(.el-input__wrapper) {
    box-shadow: none;
    border-bottom: 1px solid var(--el-border-color-lighter);
    border-radius: 0;
    padding-left: 0;
    padding-right: 0;
  }

  :deep(.el-input__inner) {
    font-size: 22px;
    font-weight: 600;
  }
}

.notebook-editor__optimize-wrap {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}

.notebook-editor__format-hint {
  padding: 4px;
  color: var(--el-text-color-secondary);

  &:hover {
    color: #f06292;
  }
}

.notebook-format-standard__title {
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--el-text-color-primary);
}

.notebook-format-standard__list {
  margin: 0;
  padding-left: 18px;
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 1.6;
}

.notebook-editor__optimize-btn {
  flex-shrink: 0;
  border: none !important;
  color: #f06292 !important;
  background: transparent !important;
  padding: 4px 8px;
  font-size: 14px;

  &:hover,
  &:focus {
    color: #ec407a !important;
    background: transparent !important;
    border: none !important;
  }
}

.notebook-editor__tags {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.notebook-editor__tags-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}

.notebook-editor__content {
  flex: 1 1 0;
  height: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
}

.notebook-editor__content-loading {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.notebook-editor__loading-icon {
  font-size: 28px;
  color: var(--el-color-primary);
}

.notebook-editor__content-editor {
  flex: 1 1 0;
  height: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.tag-manage__new {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.notebook-folder-location {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.notebook-move-hint {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.notebook-move-folder-node {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.notebook-move-tree {
  max-height: 360px;
  overflow: auto;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 4px 0;

  :deep(.el-tree--highlight-current .el-tree-node.is-current > .el-tree-node__content) {
    background: var(--el-color-primary-light-9);
  }

  :deep(.el-tree-node.is-current > .el-tree-node__content) {
    color: var(--el-color-primary);
  }
}

:deep(.el-tree-node__content) {
  height: 32px;
}
</style>
