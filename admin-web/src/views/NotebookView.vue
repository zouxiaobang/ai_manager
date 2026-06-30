<template>
  <WarRoomPage :title="t('notebook.title')" fill>
    <div class="notebook-page war-room-panel war-room-panel--notebook">
    <el-alert
      v-if="baiduPanStatus && !baiduPanStatus.authorized && !isBaiduAuthPending"
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
        <div class="notebook-tab-all">
          <section class="notebook-stats">
            <div class="notebook-stat-card notebook-stat-card--blue">
              <span class="notebook-stat-card__icon notebook-stat-card__icon--blue">
                <el-icon><Document /></el-icon>
              </span>
              <div class="notebook-stat-card__body">
                <span class="notebook-stat-card__label">{{ t('notebook.stats.notes') }}</span>
                <span class="notebook-stat-card__value">{{ notebookStats.noteCount }}</span>
              </div>
            </div>
            <div class="notebook-stat-card notebook-stat-card--green">
              <span class="notebook-stat-card__icon notebook-stat-card__icon--green">
                <el-icon><Folder /></el-icon>
              </span>
              <div class="notebook-stat-card__body">
                <span class="notebook-stat-card__label">{{ t('notebook.stats.folders') }}</span>
                <span class="notebook-stat-card__value">{{ notebookStats.folderCount }}</span>
              </div>
            </div>
            <div class="notebook-stat-card notebook-stat-card--purple">
              <span class="notebook-stat-card__icon notebook-stat-card__icon--purple">
                <el-icon><CollectionTag /></el-icon>
              </span>
              <div class="notebook-stat-card__body">
                <span class="notebook-stat-card__label">{{ t('notebook.stats.tags') }}</span>
                <span class="notebook-stat-card__value">{{ notebookStats.tagCount }}</span>
              </div>
            </div>
            <div class="notebook-stat-card notebook-stat-card--orange">
              <span class="notebook-stat-card__icon notebook-stat-card__icon--orange">
                <el-icon><Delete /></el-icon>
              </span>
              <div class="notebook-stat-card__body">
                <span class="notebook-stat-card__label">{{ t('notebook.stats.trash') }}</span>
                <span class="notebook-stat-card__value">{{ notebookStats.trashCount }}</span>
              </div>
            </div>
          </section>

          <div class="notebook-layout">
          <aside class="notebook-sidebar">
            <div class="notebook-sidebar__toolbar">
              <div class="notebook-sidebar__search-wrap">
                <el-input
                  ref="searchInputRef"
                  v-model="filterText"
                  clearable
                  :placeholder="t('notebook.searchPlaceholder')"
                  :prefix-icon="Search"
                  class="notebook-sidebar__search"
                >
                  <template #suffix>
                    <kbd v-if="!filterText" class="notebook-sidebar__search-kbd">{{ searchShortcutLabel }}</kbd>
                  </template>
                </el-input>
              </div>

              <div class="notebook-sidebar__create">
                <button
                  type="button"
                  class="notebook-sidebar__create-main"
                  @click="openCreateNote"
                >
                  <el-icon><Plus /></el-icon>
                  <span>{{ t('notebook.newNote') }}</span>
                </button>
                <span class="notebook-sidebar__create-divider" aria-hidden="true" />
                <el-dropdown
                  v-model:visible="createDropdownVisible"
                  trigger="click"
                  popper-class="notebook-sidebar__create-dropdown-popper"
                  @command="onSidebarCreateCommand"
                >
                  <button
                    type="button"
                    class="notebook-sidebar__create-more"
                    :title="t('notebook.newFolder')"
                  >
                    <el-icon><ArrowDown /></el-icon>
                  </button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="folder">
                        {{ t('notebook.newFolder') }}
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>

            <div class="notebook-sidebar__body">
              <div
                v-loading="treeLoading"
                class="notebook-sidebar__tree-wrap"
                @click.self="onTreeBlankClick"
              >
                <el-tree
                  v-if="treeData.length"
                  ref="treeRef"
                  :data="treeData"
                  node-key="nodeKey"
                  :props="treeProps"
                  :current-node-key="activeNodeKey"
                  :expand-on-click-node="false"
                  :filter-node-method="filterNode"
                  highlight-current
                  @current-change="onTreeCurrentChange"
                  @node-click="onTreeNodeClick"
                  @node-expand="onTreeNodeExpand"
                  @node-collapse="onTreeNodeCollapse"
                >
                  <template #default="{ data }">
                    <span
                      class="notebook-tree-node"
                      :class="{
                        'is-note': data.nodeType === 'NOTE',
                        'is-folder': data.nodeType === 'FOLDER',
                        'is-active': data.nodeKey === activeNodeKey,
                      }"
                      @click="onTreeNodeClick(data)"
                      @contextmenu="onTreeNodeContextMenu($event, data)"
                    >
                      <el-icon
                        v-if="data.nodeType === 'FOLDER'"
                        class="notebook-tree-node__icon is-folder"
                      >
                        <svg viewBox="0 0 16 16" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
                          <path
                            fill="currentColor"
                            d="M1.75 2.5A1.25 1.25 0 0 1 3 1.25h3.086a1 1 0 0 1 .708.293L7.543 2.75H13a1.25 1.25 0 0 1 1.25 1.25v9A1.25 1.25 0 0 1 13 14.25H3A1.25 1.25 0 0 1 1.75 13V2.5Z"
                          />
                        </svg>
                      </el-icon>
                      <el-icon v-else class="notebook-tree-node__icon is-note">
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
          </aside>

          <main class="notebook-main" :class="{ 'is-editing': !!currentNote }">
            <div v-if="currentNote" class="notebook-workspace">
              <div class="notebook-editor">
              <div class="notebook-editor__header">
              <div class="notebook-editor__title-row">
                <div class="notebook-editor__title-col">
                  <el-input
                    v-model="editForm.title"
                    class="notebook-editor__title"
                    :placeholder="t('notebook.untitled')"
                    :disabled="contentLoading"
                    @input="scheduleSave"
                  />
                  <div class="notebook-editor__meta-line">
                    <span v-if="contentLoading" class="notebook-editor__meta-item">
                      {{ t('notebook.contentLoading') }}
                    </span>
                    <span v-else-if="saveState === 'saving'" class="notebook-editor__meta-item">
                      {{ t('notebook.saving') }}
                    </span>
                    <span v-else-if="contentLoadBlocked" class="notebook-editor__meta-item is-error">
                      {{ t('notebook.contentLoadSaveBlocked') }}
                    </span>
                    <span v-else-if="saveState === 'saved'" class="notebook-editor__meta-item is-ok">
                      <el-icon class="notebook-editor__meta-check"><CircleCheck /></el-icon>
                      {{ t('notebook.saved') }}
                    </span>
                    <span
                      v-else-if="!contentLoadBlocked && currentNote.syncStatus === 'CLOUD_PENDING'"
                      class="notebook-editor__meta-item is-warn"
                    >
                      {{ t('notebook.cloudPending') }}
                    </span>
                    <span
                      v-else-if="!contentLoadBlocked && currentNote.syncStatus === 'SYNCING'"
                      class="notebook-editor__meta-item"
                    >
                      {{ t('notebook.syncing') }}
                    </span>
                    <span
                      v-else-if="!contentLoadBlocked && currentNote.syncStatus === 'FAILED'"
                      class="notebook-editor__meta-item is-error"
                    >
                      {{ t('notebook.syncFailed') }}
                    </span>
                    <template v-if="formattedUpdateTime">
                      <span v-if="showMetaStatus" class="notebook-editor__meta-dot">·</span>
                      <span class="notebook-editor__meta-item">{{ formattedUpdateTime }}</span>
                    </template>
                    <span
                      v-if="showMetaStatus || formattedUpdateTime"
                      class="notebook-editor__meta-dot"
                    >·</span>
                    <span class="notebook-editor__meta-item">
                      {{ t('notebook.wordCount', { count: noteWordCount }) }}
                    </span>
                  </div>
                </div>
                <div class="notebook-editor__actions-col">
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
                      text
                      class="notebook-editor__optimize-btn"
                      :disabled="contentLoading"
                      @click="onOptimizeContent"
                    >
                      {{ t('notebook.optimize') }}
                    </el-button>
                  </div>
                  <div class="notebook-editor__pin-actions">
                    <button
                      type="button"
                      class="notebook-editor__meta-action"
                      :class="{ 'is-active': currentNote.isPinned === 1 }"
                      @click="togglePin"
                    >
                      <el-icon><Top /></el-icon>
                      {{ currentNote.isPinned === 1 ? t('notebook.unpin') : t('notebook.pin') }}
                    </button>
                    <button
                      type="button"
                      class="notebook-editor__meta-action"
                      :class="{ 'is-active': currentNote.isFavorite === 1 }"
                      @click="toggleFavorite"
                    >
                      <el-icon><Star /></el-icon>
                      {{ currentNote.isFavorite === 1 ? t('notebook.unfavorite') : t('notebook.favorite') }}
                    </button>
                  </div>
                </div>
              </div>

              <div class="notebook-editor__tags">
                <span
                  v-for="(tag, tagIndex) in selectedNoteTags"
                  :key="tag.id"
                  class="notebook-tag-pill"
                  :style="getTagPillStyle(tag, tagIndex)"
                  :title="t('notebook.removeTag')"
                  @click="detachNoteTag(tag.id)"
                >
                  # {{ tag.name }}
                </span>
                <el-popover
                  v-model:visible="tagPickerVisible"
                  placement="bottom-start"
                  :width="240"
                  trigger="click"
                  popper-class="notebook-tag-popover"
                >
                  <template #reference>
                    <button type="button" class="notebook-tag-pill is-add">
                      <el-icon><Plus /></el-icon>
                      {{ t('notebook.addTag') }}
                    </button>
                  </template>
                  <div class="notebook-tag-picker">
                    <button
                      v-for="(tag, tagIndex) in allTags"
                      :key="tag.id"
                      type="button"
                      class="notebook-tag-picker__item"
                      :class="{ 'is-selected': editForm.tagIds.includes(tag.id) }"
                      @click="toggleNoteTag(tag.id)"
                    >
                      <span
                        class="notebook-tag-picker__pill"
                        :style="getTagPillStyle(tag, tagIndex)"
                      >
                        # {{ tag.name }}
                      </span>
                      <el-icon v-if="editForm.tagIds.includes(tag.id)"><Check /></el-icon>
                    </button>
                    <el-empty
                      v-if="!allTags.length"
                      :description="t('notebook.noTags')"
                      :image-size="48"
                    />
                    <button
                      type="button"
                      class="notebook-tag-picker__manage"
                      @click="openTagManage"
                    >
                      {{ t('notebook.tagManage') }}
                    </button>
                  </div>
                </el-popover>
              </div>
              </div>

              <div class="notebook-editor__content">
                <NoteRichEditor
                  v-if="currentNote"
                  ref="editorRef"
                  :key="currentNote.id"
                  v-model="editForm.content"
                  class="notebook-editor__content-editor"
                  :class="{ 'is-content-loading': contentLoading }"
                  :placeholder="t('notebook.contentPlaceholder')"
                  @change="scheduleSave"
                  @heading-active="onHeadingActive"
                />
                <div
                  v-if="contentLoading"
                  class="notebook-editor__content-loading notebook-editor__content-loading--overlay"
                >
                  <el-icon class="notebook-editor__loading-icon is-loading"><Loading /></el-icon>
                  <span>{{ t('notebook.contentLoading') }}</span>
                </div>
              </div>
              </div>

              <aside class="notebook-toc" :class="{ 'is-collapsed': !tocVisible }">
                <div class="notebook-toc__header">
                  <h3 class="notebook-toc__title">{{ t('notebook.tabs.toc') }}</h3>
                  <button
                    type="button"
                    class="notebook-toc__toggle"
                    :title="t('notebook.hideToc')"
                    @click="tocVisible = false"
                  >
                    <el-icon><Close /></el-icon>
                  </button>
                </div>
                <NoteTocPanel
                  :items="tocItems"
                  :active-index="activeTocIndex"
                  :empty-text="t('notebook.tocEmpty')"
                  @select="onTocItemClick"
                />
              </aside>

              <button
                v-if="!tocVisible"
                type="button"
                class="notebook-toc-expand"
                :title="t('notebook.showToc')"
                @click="tocVisible = true"
              >
                {{ t('notebook.tabs.toc') }}
              </button>
            </div>

            <NotebookFolderView
              v-if="!currentNote && selectedFolderNode"
              :folder-node="selectedFolderNode"
              :note-meta="folderNoteMeta"
              @open-folder="onFolderViewOpenFolder"
              @open-note="onFolderViewOpenNote"
            />

            <div v-show="!currentNote && !selectedFolderNode" class="notebook-main__empty">
              <el-empty :description="t('notebook.selectNoteHint')" />
            </div>
          </main>
        </div>
        </div>
      </el-tab-pane>

      <el-tab-pane :label="t('notebook.tabs.trash')" name="trash">
        <NotebookTrashView
          ref="trashViewRef"
          @count-change="trashCount = $event"
          @restored="loadTree()"
        />
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
  </WarRoomPage>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import WarRoomPage from '@/components/war-room/WarRoomPage.vue'
import { ElMessage, ElMessageBox, type ElInput, type ElTree } from 'element-plus'
import {
  ArrowDown,
  Check,
  CircleCheck,
  Close,
  CollectionTag,
  Delete,
  Document,
  Folder,
  Loading,
  Plus,
  QuestionFilled,
  Search,
  Star,
  Top,
} from '@element-plus/icons-vue'
import NotebookFolderView from './notebook/NotebookFolderView.vue'
import NotebookTrashView from './notebook/NotebookTrashView.vue'
import NoteRichEditor from './notebook/NoteRichEditor.vue'
import NoteTocPanel from './notebook/NoteTocPanel.vue'
import NoteTreeContextMenu, { type TreeContextMenuAction } from './notebook/NoteTreeContextMenu.vue'
import { exportNoteAsWord } from './notebook/exportNoteWord'
import { parseNoteToc } from './notebook/noteToc'
import { hasNoteVisibleText, isSameNoteHtml, optimizeNoteHtml } from './notebook/noteContentOptimize'
import { NOTE_FORMAT_RULE_IDS } from './notebook/noteFormatStandard'
import type { NoteFolderListMeta } from './notebook/notePreview'
import { isContentLoadFailure } from './notebook/notePreview'
import { useBaiduPanAutoAuth } from '@/composables/useBaiduPanAutoAuth'
import {
  createNoteRequest,
  createNotebook,
  createNoteTag,
  fetchNote,
  fetchNotesMeta,
  fetchNoteTags,
  fetchNotebookTree,
  removeNote,
  removeNoteTag,
  removeNotebook,
  updateNote,
  updateNoteKeepalive,
  updateNotebook,
  type NbNoteSaveRequest,
  type NbNoteDetail,
  type NbNoteTag,
  type NbTreeNode,
} from '@/api/notebook'

const { t } = useI18n()
const route = useRoute()

const { baiduPanStatus, isBaiduAuthPending, redirectToBaiduAuthorize } = useBaiduPanAutoAuth()

const activeTab = ref('all')
const tocVisible = ref(true)
const treeLoading = ref(false)
const treeData = ref<NbTreeNode[]>([])
const trashCount = ref(0)
const allTags = ref<NbNoteTag[]>([])
const filterText = ref('')
const searchInputRef = ref<InstanceType<typeof ElInput> | null>(null)
const isMacPlatform = typeof navigator !== 'undefined' && /Mac|iPhone|iPod|iPad/i.test(navigator.platform)
const searchShortcutLabel = computed(() => (isMacPlatform ? '⌘K' : 'Ctrl+K'))
const treeRef = ref<InstanceType<typeof ElTree> | null>(null)
const moveTreeRef = ref<InstanceType<typeof ElTree> | null>(null)
const editorRef = ref<InstanceType<typeof NoteRichEditor> | null>(null)
const trashViewRef = ref<InstanceType<typeof NotebookTrashView> | null>(null)
const createDropdownVisible = ref(false)

const editorRevision = ref(0)
const contentLoadBlocked = ref(false)
const contentLoading = ref(false)
let noteLoadSeq = 0
let treeClickDedupeAt = 0
let treeClickDedupeKey = ''
let treeSelectionHandling = false
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
const tagPickerVisible = ref(false)
const newTagName = ref('')
const tagSubmitting = ref(false)
const activeTocIndex = ref(-1)

const TAG_PILL_THEMES = [
  { bg: '#eff6ff', color: '#2563eb' },
  { bg: '#f5f3ff', color: '#7c3aed' },
  { bg: '#f0fdf4', color: '#16a34a' },
  { bg: '#fff7ed', color: '#ea580c' },
] as const

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

const selectedNoteTags = computed(() =>
  allTags.value.filter((tag) => editForm.tagIds.includes(tag.id)),
)

const noteWordCount = computed(() => {
  const text = editForm.content
    .replace(/<style[\s\S]*?<\/style>/gi, '')
    .replace(/<script[\s\S]*?<\/script>/gi, '')
    .replace(/<[^>]+>/g, '')
    .replace(/&nbsp;/g, ' ')
    .replace(/\u200B/g, '')
    .trim()
  return text.length
})

const formattedUpdateTime = computed(() => formatNoteDisplayTime(currentNote.value?.updateTime))

const showMetaStatus = computed(() => {
  if (!currentNote.value) return false
  if (contentLoading.value) return true
  if (saveState.value === 'saving' || saveState.value === 'saved') return true
  if (contentLoadBlocked.value) return true
  if (!contentLoadBlocked.value && currentNote.value.syncStatus === 'CLOUD_PENDING') return true
  if (!contentLoadBlocked.value && currentNote.value.syncStatus === 'SYNCING') return true
  if (!contentLoadBlocked.value && currentNote.value.syncStatus === 'FAILED') return true
  return false
})

function startOfDay(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate())
}

function formatNoteDisplayTime(updateTime?: string): string {
  if (!updateTime) return ''
  const normalized = updateTime.includes('T') ? updateTime : updateTime.replace(' ', 'T')
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) {
    return updateTime.replace(/:\d{2}$/, '')
  }

  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  const timePart = `${hh}:${mm}`

  const diffDays = Math.round(
    (startOfDay(new Date()).getTime() - startOfDay(date).getTime()) / 86_400_000,
  )

  if (diffDays === 0) return t('notebook.todayAt', { time: timePart })
  if (diffDays === 1) return t('notebook.yesterdayAt', { time: timePart })
  if (diffDays === 2) return t('notebook.dayBeforeYesterdayAt', { time: timePart })

  const yyyy = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${yyyy}-${month}-${day} ${timePart}`
}

function getTagPillStyle(tag: NbNoteTag, index = 0): Record<string, string> {
  if (tag.color) {
    return {
      backgroundColor: `${tag.color}1a`,
      color: tag.color,
    }
  }
  const theme = TAG_PILL_THEMES[index % TAG_PILL_THEMES.length]
  return {
    backgroundColor: theme.bg,
    color: theme.color,
  }
}

function toggleNoteTag(tagId: number) {
  const index = editForm.tagIds.indexOf(tagId)
  if (index >= 0) {
    editForm.tagIds.splice(index, 1)
  } else {
    editForm.tagIds.push(tagId)
  }
  scheduleSave()
}

function detachNoteTag(tagId: number) {
  editForm.tagIds = editForm.tagIds.filter((id) => id !== tagId)
  scheduleSave()
}

function openTagManage() {
  tagPickerVisible.value = false
  tagDialogVisible.value = true
}

function onHeadingActive(index: number) {
  activeTocIndex.value = index
}

function countTreeNodes(nodes: NbTreeNode[], type: NbTreeNode['nodeType']): number {
  let count = 0
  for (const node of nodes) {
    if (node.nodeType === type) count += 1
    if (node.children?.length) count += countTreeNodes(node.children, type)
  }
  return count
}

const notebookStats = computed(() => ({
  noteCount: countTreeNodes(treeData.value, 'NOTE'),
  folderCount: countTreeNodes(treeData.value, 'FOLDER'),
  tagCount: allTags.value.length,
  trashCount: trashCount.value,
}))

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
  activeTocIndex.value = index
  editorRef.value?.scrollToHeading(index)
}

async function restoreTreeSelectionView() {
  if (!activeNodeKey.value) return
  await nextTick()
  const ancestorKeys = collectAncestorFolderKeys(treeData.value, activeNodeKey.value) ?? []
  ancestorKeys.forEach((key) => {
    treeRef.value?.getNode(key)?.expand(null, true)
  })
  treeRef.value?.setCurrentKey(activeNodeKey.value)
  syncOpenNoteFromActiveKey()
  await nextTick()
  const treeEl = treeRef.value?.$el as HTMLElement | undefined
  treeEl?.querySelector('.el-tree-node.is-current')?.scrollIntoView({ block: 'nearest' })
}

function syncOpenNoteFromActiveKey() {
  if (!activeNodeKey.value) return
  const node = findNodeByKey(treeData.value, activeNodeKey.value)
  if (!node) {
    activeNodeKey.value = ''
    currentNote.value = null
    return
  }
  if (node.nodeType === 'FOLDER') {
    selectedFolderId.value = node.notebookId ?? null
    currentNote.value = null
    contentLoading.value = false
    return
  }
  if (node.nodeType === 'NOTE' && node.noteId) {
    if (currentNote.value?.id === node.noteId) return
    beginOpenNote(node.noteId, buildNoteStubFromTree(node), node.nodeKey)
  }
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
  const selectionAtStart = activeNodeKey.value
  try {
    treeData.value = await fetchNotebookTree()
    if (keepSelection && selectionAtStart) {
      if (activeNodeKey.value === selectionAtStart) {
        const stillExists = findNodeByKey(treeData.value, selectionAtStart)
        if (stillExists) {
          activeNodeKey.value = selectionAtStart
        } else {
          activeNodeKey.value = ''
          currentNote.value = null
        }
      } else if (activeNodeKey.value && !findNodeByKey(treeData.value, activeNodeKey.value)) {
        activeNodeKey.value = ''
        currentNote.value = null
      }
    }
    await nextTick()
    if (!filterText.value.trim()) {
      restoreUserTreeExpansion()
    }
    if (keepSelection && activeNodeKey.value) {
      await restoreTreeSelectionView()
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

async function onTabChange(tab: string | number) {
  createDropdownVisible.value = false
  if (tab === 'trash') {
    void flushSaveInBackground()
    await trashViewRef.value?.reload()
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

function onTreeCurrentChange(data: NbTreeNode | null) {
  if (!data || treeSelectionHandling) return
  void onTreeNodeClick(data)
}

async function onTreeNodeClick(data: NbTreeNode) {
  if (treeSelectionHandling) return
  const dedupeKey = `${data.nodeKey}:${data.nodeType}`
  const now = Date.now()
  if (treeClickDedupeKey === dedupeKey && now - treeClickDedupeAt < 80) {
    return
  }
  treeClickDedupeKey = dedupeKey
  treeClickDedupeAt = now

  closeContextMenu()
  if (data.nodeKey === activeNodeKey.value && data.nodeType === 'NOTE' && currentNote.value?.id === data.noteId) {
    return
  }
  if (data.nodeKey === activeNodeKey.value && data.nodeType === 'FOLDER' && !currentNote.value) {
    return
  }

  const pending = capturePendingSave()

  treeSelectionHandling = true
  try {
    if (data.nodeType === 'FOLDER') {
      selectedFolderId.value = data.notebookId ?? null
      activeNodeKey.value = data.nodeKey
      currentNote.value = null
      savedSnapshot.value = null
      contentLoadBlocked.value = false
      contentLoading.value = false
    } else if (data.nodeType === 'NOTE' && data.noteId) {
      beginOpenNote(data.noteId, buildNoteStubFromTree(data), data.nodeKey)
      ensureAncestorsExpanded(activeNodeKey.value)
    }

    await nextTick()
    treeRef.value?.setCurrentKey(activeNodeKey.value || undefined)
  } finally {
    treeSelectionHandling = false
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
    activeNodeKey.value = ''
    editForm.title = ''
    editForm.content = ''
    editForm.tagIds = []
  }
  ElMessage.success(t('pomodoro.common.deleted'))
  await loadTree(false)
  void trashViewRef.value?.reload()
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
    noteType: 'NOTE',
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
  tocVisible.value = true
  activeTocIndex.value = -1
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
    } else if (expectedSeq === noteLoadSeq) {
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

async function onOptimizeContent() {
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
  await editorRef.value?.setHtml(optimized)
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

function flushSaveOnUnload() {
  clearSaveTimer()
  if (contentLoadBlocked.value || contentLoading.value) return
  const note = currentNote.value
  if (!note?.id || !isDirty()) return
  updateNoteKeepalive(note.id, buildSavePayload())
  syncSavedSnapshot()
}

function focusSearchInput() {
  const input = searchInputRef.value?.$el?.querySelector('input') as HTMLInputElement | null
  input?.focus()
  input?.select()
}

function onSearchShortcut(event: KeyboardEvent) {
  if (event.key.toLowerCase() !== 'k') return
  const withMod = isMacPlatform ? event.metaKey : event.ctrlKey
  if (!withMod) return
  event.preventDefault()
  focusSearchInput()
}

function onSidebarCreateCommand(command: string) {
  createDropdownVisible.value = false
  if (command === 'folder') openCreateFolder()
}

function shouldKeepCreateDropdownOpen(target: HTMLElement) {
  return (
    target.closest('.notebook-sidebar__create-more') != null ||
    target.closest('.notebook-sidebar__create-dropdown-popper') != null
  )
}

function closeCreateDropdownIfOutside(event: MouseEvent) {
  if (!createDropdownVisible.value) return
  const target = event.target as HTMLElement | null
  if (!target || shouldKeepCreateDropdownOpen(target)) return
  createDropdownVisible.value = false
}

function onDocumentClickForCreateDropdown(event: MouseEvent) {
  closeCreateDropdownIfOutside(event)
}

async function openCreateNote() {
  createDropdownVisible.value = false
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

function openBaiduPanAuthorize() {
  redirectToBaiduAuthorize()
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

function onTreeBlankClick() {
  activeNodeKey.value = ''
  currentNote.value = null
  selectedFolderId.value = null
  contentLoading.value = false
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

watch(createDropdownVisible, (visible) => {
  if (visible) {
    window.setTimeout(() => {
      document.addEventListener('click', onDocumentClickForCreateDropdown, true)
    }, 0)
    return
  }
  document.removeEventListener('click', onDocumentClickForCreateDropdown, true)
})

watch(
  () => route.query.tab,
  (tab) => {
    if (tab === 'trash') {
      activeTab.value = 'trash'
    }
  },
)

onMounted(async () => {
  window.addEventListener('pagehide', onPageHide)
  window.addEventListener('keydown', onSearchShortcut)
  document.addEventListener('visibilitychange', onVisibilityChange)
  document.addEventListener('click', closeContextMenu)
  document.addEventListener('scroll', closeContextMenu, true)
  await Promise.all([loadTree(false), loadTags()])
  if (route.query.tab === 'trash') {
    activeTab.value = 'trash'
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('pagehide', onPageHide)
  window.removeEventListener('keydown', onSearchShortcut)
  document.removeEventListener('visibilitychange', onVisibilityChange)
  document.removeEventListener('click', closeContextMenu)
  document.removeEventListener('click', onDocumentClickForCreateDropdown, true)
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
  padding: 0;
  background: transparent;
  border: none;
  box-shadow: none;
}

.war-room-panel--notebook {
  margin: 0;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 16px 20px;
  background: var(--wr-card);
  border: 1px solid var(--wr-border);
  border-radius: 12px;
  box-shadow: var(--wr-shadow);
}

.notebook-tab-all {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  gap: 16px;
}

.notebook-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  width: 100%;
  flex-shrink: 0;
}

.notebook-stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  height: 88px;
  min-width: 0;
  padding: 16px 18px;
  box-sizing: border-box;
  border: 1px solid transparent;
  border-radius: 14px;
  box-shadow: 0 1px 2px rgb(15 23 42 / 4%);

  &--blue {
    background: var(--wr-stat-blue-bg);
    border-color: #bfdbfe;

    .notebook-stat-card__value {
      color: var(--wr-stat-blue);
    }
  }

  &--green {
    background: var(--wr-stat-green-bg);
    border-color: #bbf7d0;

    .notebook-stat-card__value {
      color: var(--wr-stat-green);
    }
  }

  &--purple {
    background: var(--wr-stat-purple-bg);
    border-color: #ddd6fe;

    .notebook-stat-card__value {
      color: var(--wr-stat-purple);
    }
  }

  &--orange {
    background: var(--wr-stat-orange-bg);
    border-color: #fed7aa;

    .notebook-stat-card__value {
      color: var(--wr-stat-orange);
    }
  }
}

.notebook-stat-card__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  flex-shrink: 0;
  font-size: 24px;
  color: #fff;

  &--blue {
    background: #3b82f6;
  }

  &--green {
    background: #22c55e;
  }

  &--purple {
    background: #8b5cf6;
  }

  &--orange {
    background: #f59e0b;
  }
}

.notebook-stat-card__body {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.notebook-stat-card__label {
  font-size: 14px;
  font-weight: 700;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.notebook-stat-card__value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.notebook-page__title {
  display: none;
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
    margin-bottom: 16px;
  }

  > .el-tabs__nav-wrap::after {
    height: 1px;
    background: var(--wr-border);
  }

  .el-tabs__item {
    font-size: 14px;
    color: var(--wr-text-secondary);

    &.is-active {
      color: var(--wr-rail-active-color);
      font-weight: 600;
    }

    &:hover {
      color: var(--wr-text);
    }
  }

  .el-tabs__active-bar {
    height: 2px;
    background: var(--wr-rail-active-color);
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
  position: relative;
  display: flex;
  gap: 0;
  flex: 1;
  min-height: 0;
  border: 1px solid var(--wr-border);
  border-radius: 12px;
  overflow: hidden;
  background: var(--wr-card);
}

.notebook-sidebar {
  width: 280px;
  flex-shrink: 0;
  min-width: 0;
  overflow: hidden;
  border-right: 1px solid var(--wr-border);
  display: flex;
  flex-direction: column;
  background: var(--wr-card);
}

.notebook-workspace {
  flex: 1;
  display: flex;
  min-width: 0;
  min-height: 0;
  position: relative;
  overflow: hidden;
  align-items: stretch;
}

.notebook-editor {
  flex: 1;
  min-width: 0;
  min-height: 0;
  align-self: stretch;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 16px 20px;
  box-sizing: border-box;
}

.notebook-toc {
  width: 240px;
  flex-shrink: 0;
  min-width: 0;
  align-self: flex-start;
  height: auto;
  max-height: 100%;
  display: flex;
  flex-direction: column;
  margin: 12px 12px 12px 0;
  border: 1px solid var(--wr-border);
  border-radius: 12px;
  background: var(--wr-card);
  box-shadow: var(--wr-shadow);
  overflow: hidden;
  transition: width 0.2s ease, opacity 0.2s ease, margin 0.2s ease;

  &.is-collapsed {
    width: 0;
    margin-right: 0;
    opacity: 0;
    pointer-events: none;
    border: none;
    overflow: hidden;
  }

  :deep(.note-toc-panel) {
    padding: 0;
  }
}

.notebook-toc__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 14px 14px 10px;
  min-height: 44px;
  box-sizing: border-box;
  flex-shrink: 0;
  border-bottom: 1px solid var(--wr-border);
}

.notebook-toc__title {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: var(--wr-text);
}

.notebook-toc__toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--wr-text-secondary);
  cursor: pointer;

  &:hover {
    color: var(--wr-rail-active-color);
    background: var(--wr-stat-blue-bg);
  }
}

.notebook-toc-expand {
  position: absolute;
  top: 12px;
  right: 0;
  z-index: 2;
  transform: none;
  padding: 8px 6px;
  border: 1px solid var(--wr-border);
  border-right: none;
  border-radius: 8px 0 0 8px;
  background: var(--wr-card);
  box-shadow: var(--wr-shadow);
  color: var(--wr-rail-active-color);
  font-size: 12px;
  font-weight: 600;
  writing-mode: vertical-rl;
  letter-spacing: 2px;
  cursor: pointer;

  &:hover {
    background: var(--wr-stat-blue-bg);
  }
}

.notebook-sidebar__toolbar {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px 12px 10px;
  box-sizing: border-box;
}

.notebook-sidebar__search-wrap {
  width: 100%;
}

.notebook-sidebar__search {
  width: 100%;

  :deep(.el-input__wrapper) {
    min-height: 40px;
    padding: 8px 12px;
    border-radius: 12px;
    box-shadow: 0 0 0 1px var(--wr-border) inset;
    background: var(--wr-card);
  }

  :deep(.el-input__inner) {
    font-size: 14px;
    line-height: 1.4;
  }

  :deep(.el-input__prefix) {
    font-size: 16px;
  }

  :deep(.el-input) {
    width: 100%;
  }
}

.notebook-sidebar__search-kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 22px;
  padding: 0 7px;
  border: 1px solid var(--wr-border);
  border-radius: 6px;
  background: var(--wr-bg);
  color: var(--wr-text-secondary);
  font-family: inherit;
  font-size: 12px;
  line-height: 1;
  pointer-events: none;
}

.notebook-sidebar__create {
  display: flex;
  align-items: stretch;
  width: 100%;
  border: 1px solid var(--wr-rail-active-color);
  border-radius: 10px;
  overflow: hidden;
  background: var(--wr-card);
}

.notebook-sidebar__create-main {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 36px;
  padding: 0 12px;
  border: none;
  background: transparent;
  color: var(--wr-rail-active-color);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;

  &:hover {
    background: var(--wr-stat-blue-bg);
  }
}

.notebook-sidebar__create-divider {
  width: 1px;
  flex-shrink: 0;
  background: var(--wr-rail-active-color);
  opacity: 0.35;
}

.notebook-sidebar__create-more {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  flex-shrink: 0;
  border: none;
  background: transparent;
  color: var(--wr-rail-active-color);
  cursor: pointer;

  &:hover {
    background: var(--wr-stat-blue-bg);
  }
}

.notebook-sidebar__body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.notebook-sidebar__tree-wrap {
  flex: 1;
  overflow: auto;
  overscroll-behavior: contain;
  padding: 0 8px 12px;

  :deep(.el-tree) {
    background: transparent;
  }

  :deep(.el-tree-node__content) {
    display: flex;
    align-items: center;
    height: auto;
    min-height: 40px;
    padding: 2px 0;
    border-radius: 10px;
    cursor: pointer;

    .notebook-tree-node {
      flex: 1;
      min-width: 0;
    }
  }

  :deep(.el-tree--highlight-current .el-tree-node.is-current > .el-tree-node__content) {
    background: transparent;
  }
}

.notebook-tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 6px 8px;
  border-radius: 10px;
  font-size: 13px;
  box-sizing: border-box;
  cursor: pointer;
  user-select: none;

  &.is-active {
    color: var(--wr-rail-active-color);
    background: var(--wr-rail-active-bg);
  }
}

:deep(.el-tree-node.is-current > .el-tree-node__content .notebook-tree-node) {
  color: var(--wr-rail-active-color);
  background: var(--wr-rail-active-bg);
}

.notebook-tree-node__icon {
  flex-shrink: 0;
  font-size: 16px;
  line-height: 1;

  &.is-folder {
    color: var(--wr-index-text);
    font-size: 17px;
  }

  &.is-note {
    color: var(--wr-stat-gray);
  }
}

.notebook-tree-node.is-active .notebook-tree-node__icon.is-note,
:deep(.el-tree-node.is-current > .el-tree-node__content .notebook-tree-node__icon.is-note) {
  color: var(--wr-rail-active-color);
}

.notebook-tree-node__label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notebook-tree-node__pin {
  margin-left: auto;
  font-size: 12px;
  color: var(--wr-rail-active-color);
}

.notebook-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  padding: 16px 20px;
  overflow: hidden;
  background: var(--wr-card);

  &.is-editing {
    padding: 0;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    min-height: 0;
  }

  &:not(:has(.notebook-editor)) {
    overflow: auto;
  }
}

.notebook-editor__header {
  flex-shrink: 0;
  margin-bottom: 16px;
}

.notebook-editor__title-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 14px;
}

.notebook-editor__title-col {
  flex: 1;
  min-width: 0;
}

.notebook-editor__actions-col {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
  flex-shrink: 0;
  padding-top: 4px;
}

.notebook-editor__pin-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.notebook-editor__title {
  width: 100%;
  min-width: 0;
  margin-bottom: 8px;

  :deep(.el-input__wrapper) {
    box-shadow: none !important;
    border: none;
    border-radius: 0;
    padding: 0;
    background: transparent;
  }

  :deep(.el-input__inner) {
    font-size: 28px;
    font-weight: 700;
    line-height: 1.3;
    color: var(--wr-text);
    height: auto;
  }
}

.notebook-editor__meta-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  font-size: 12px;
  line-height: 1.5;
}

.notebook-editor__meta-action {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--wr-text-secondary);
  font-size: 12px;
  cursor: pointer;
  white-space: nowrap;

  &.is-active {
    color: var(--wr-rail-active-color);
  }

  &:hover {
    color: var(--wr-rail-active-color);
  }
}

.notebook-editor__meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--wr-text-secondary);

  &.is-ok {
    color: var(--wr-text-secondary);
  }

  &.is-error {
    color: #dc2626;
  }

  &.is-warn {
    color: #d97706;
  }
}

.notebook-editor__meta-check {
  font-size: 14px;
  color: var(--wr-up-badge-bg);
}

.notebook-editor__meta-dot {
  margin: 0 8px;
  color: var(--wr-muted);
  user-select: none;
}

.notebook-editor__optimize-wrap {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}

.notebook-editor__format-hint {
  padding: 4px;
  color: var(--wr-muted);

  &:hover {
    color: #f06292;
  }
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
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 4px;
}

.notebook-tag-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  border: none;
  border-radius: 999px;
  font-size: 13px;
  line-height: 1.2;
  cursor: pointer;
  transition: opacity 0.15s ease, transform 0.15s ease;

  &:hover {
    opacity: 0.85;
  }

  &.is-add {
    border: 1px solid var(--wr-border);
    background: var(--wr-card);
    color: var(--wr-stat-purple, #7c3aed);
    box-shadow: 0 1px 2px rgb(0 0 0 / 4%);
  }
}

.notebook-tag-picker {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.notebook-tag-picker__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  padding: 6px 8px;
  border: none;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;

  &:hover,
  &.is-selected {
    background: var(--wr-stat-blue-bg);
  }
}

.notebook-tag-picker__pill {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
}

.notebook-tag-picker__manage {
  margin-top: 6px;
  padding: 8px 10px;
  border: none;
  border-top: 1px solid var(--wr-border);
  border-radius: 0;
  background: transparent;
  color: var(--wr-rail-active-color);
  font-size: 13px;
  text-align: left;
  cursor: pointer;

  &:hover {
    background: var(--wr-stat-blue-bg);
  }
}

.notebook-main__empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
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

.notebook-editor__content {
  flex: 1 1 auto;
  min-height: 300px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
  z-index: 2;
}

.notebook-editor__content-editor {
  flex: 1 1 auto;
  min-height: 300px;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  &.is-content-loading {
    pointer-events: none;
  }
}

.notebook-editor__content-loading {
  flex: 1;
  min-height: 300px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--el-text-color-secondary);
  font-size: 14px;

  &.notebook-editor__content-loading--overlay {
    position: absolute;
    inset: 0;
    z-index: 3;
    min-height: 0;
    background: rgb(255 255 255 / 78%);
  }
}

.notebook-editor__loading-icon {
  font-size: 28px;
  color: var(--el-color-primary);
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

@media (max-width: 1200px) {
  .notebook-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .notebook-stats {
    grid-template-columns: 1fr;
  }
}
</style>
