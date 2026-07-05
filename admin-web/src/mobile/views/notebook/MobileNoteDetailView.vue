<template>
  <div class="mobile-note-detail">
    <div class="mobile-note-detail__toolbar">
      <el-input
        v-model="editForm.title"
        class="mobile-note-detail__title"
        :placeholder="t('notebook.untitled')"
        :disabled="contentLoading || contentLoadBlocked"
        @input="scheduleSave"
      />
      <span class="mobile-note-detail__save">{{ saveStateLabel }}</span>
    </div>

    <div class="mobile-note-detail__editor">
      <div v-if="contentLoading" class="mobile-empty-hint">
        {{ t('notebook.contentLoading') }}
      </div>
      <NoteRichEditor
        v-show="!contentLoading"
        :key="editorRevision"
        v-model="editForm.content"
        class="mobile-note-detail__rich"
        :placeholder="t('notebook.contentPlaceholder')"
        @change="scheduleSave"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import NoteRichEditor from '@/views/notebook/NoteRichEditor.vue'
import { isContentLoadFailure } from '@/views/notebook/notePreview'
import {
  fetchNote,
  updateNote,
  updateNoteKeepalive,
  type NbNoteDetail,
  type NbNoteSaveRequest,
} from '@/api/notebook'

const route = useRoute()
const { t } = useI18n()

const noteId = computed(() => Number(route.params.id))
const currentNote = ref<NbNoteDetail | null>(null)
const contentLoading = ref(false)
const contentLoadBlocked = ref(false)
const editorRevision = ref(0)
const saveState = ref<'idle' | 'saving' | 'saved'>('idle')

const editForm = reactive({
  title: '',
  content: '',
  tagIds: [] as number[],
})

type NoteSnapshot = { title: string; content: string; tagIds: string }
const savedSnapshot = ref<NoteSnapshot | null>(null)

let saveTimer: ReturnType<typeof setTimeout> | null = null
let noteLoadSeq = 0

const saveStateLabel = computed(() => {
  if (saveState.value === 'saving') return t('notebook.saving')
  if (saveState.value === 'saved') return t('notebook.saved')
  return ''
})

function snapshotFromForm(): NoteSnapshot {
  return {
    title: editForm.title,
    content: editForm.content,
    tagIds: JSON.stringify([...editForm.tagIds].sort((a, b) => a - b)),
  }
}

function syncSavedSnapshot() {
  savedSnapshot.value = currentNote.value ? snapshotFromForm() : null
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

async function loadNoteDetail(id: number) {
  const seq = ++noteLoadSeq
  contentLoading.value = true
  contentLoadBlocked.value = false
  clearSaveTimer()
  try {
    const detail = await fetchNote(id)
    if (seq !== noteLoadSeq || noteId.value !== id) return
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
    if (seq !== noteLoadSeq || noteId.value !== id) return
    contentLoadBlocked.value = true
    ElMessage.warning(t('notebook.contentLoadFailed'))
    editForm.content = ''
    editorRevision.value += 1
    saveState.value = 'saved'
  } finally {
    if (seq === noteLoadSeq && noteId.value === id) {
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

async function saveCurrentNote() {
  const id = currentNote.value?.id
  if (!id || contentLoadBlocked.value || contentLoading.value) return
  if (!isDirty()) {
    saveState.value = 'saved'
    return
  }
  saveState.value = 'saving'
  try {
    const updated = await updateNote(id, buildSavePayload())
    if (currentNote.value?.id === id) {
      currentNote.value = updated
      syncSavedSnapshot()
    }
    saveState.value = 'saved'
  } catch {
    if (currentNote.value?.id === id) {
      saveState.value = 'idle'
    }
    ElMessage.error(t('notebook.saveFailed'))
  }
}

function flushSaveOnUnload() {
  clearSaveTimer()
  const id = currentNote.value?.id
  if (!id || !isDirty() || contentLoadBlocked.value) return
  updateNoteKeepalive(id, buildSavePayload())
}

watch(
  noteId,
  (id) => {
    if (!Number.isFinite(id) || id <= 0) return
    currentNote.value = null
    editForm.title = ''
    editForm.content = ''
    editForm.tagIds = []
    savedSnapshot.value = null
    editorRevision.value += 1
    void loadNoteDetail(id)
  },
  { immediate: true },
)

onMounted(() => {
  window.addEventListener('beforeunload', flushSaveOnUnload)
})

onBeforeUnmount(() => {
  clearSaveTimer()
  void saveCurrentNote()
  window.removeEventListener('beforeunload', flushSaveOnUnload)
})
</script>

<style scoped lang="scss">
.mobile-note-detail {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: calc(100vh - 120px);
  min-height: calc(100dvh - 120px);
}

.mobile-note-detail__toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mobile-note-detail__title {
  flex: 1;
}

.mobile-note-detail__save {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.mobile-note-detail__editor {
  flex: 1;
  min-height: 360px;
}

.mobile-note-detail__rich {
  min-height: 360px;
}
</style>
