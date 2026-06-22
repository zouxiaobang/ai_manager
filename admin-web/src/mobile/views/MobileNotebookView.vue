<template>
  <div v-loading="loading" class="mobile-page">
    <el-input
      v-model="keyword"
      clearable
      :prefix-icon="Search"
      :placeholder="t('notebook.searchPlaceholder')"
    />

    <el-button type="primary" style="width: 100%" @click="onCreateNote">
      {{ t('notebook.newNote') }}
    </el-button>

    <section v-if="pinnedNotes.length" class="mobile-card">
      <h2 class="mobile-section-title">{{ t('mobile.notebook.pinned') }}</h2>
      <div
        v-for="item in pinnedNotes"
        :key="item.id"
        class="mobile-list-item"
        @click="openNote(item.id)"
      >
        <el-icon><Document /></el-icon>
        <div class="mobile-list-item__body">
          <div class="mobile-list-item__title">{{ item.title }}</div>
          <div v-if="item.folderPath" class="mobile-list-item__meta">{{ item.folderPath }}</div>
        </div>
      </div>
    </section>

    <section class="mobile-card">
      <h2 class="mobile-section-title">{{ t('mobile.notebook.allNotes') }}</h2>
      <div v-if="filteredNotes.length">
        <div
          v-for="item in filteredNotes"
          :key="item.id"
          class="mobile-list-item"
          @click="openNote(item.id)"
        >
          <el-icon><Document /></el-icon>
          <div class="mobile-list-item__body">
            <div class="mobile-list-item__title">{{ item.title }}</div>
            <div v-if="item.folderPath" class="mobile-list-item__meta">{{ item.folderPath }}</div>
          </div>
        </div>
      </div>
      <div v-else class="mobile-empty-hint">{{ t('notebook.emptyTree') }}</div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Document, Search } from '@element-plus/icons-vue'
import { createNoteRequest, fetchNotebookTree } from '@/api/notebook'
import { flattenNotes } from '@/mobile/utils/noteTree'

const router = useRouter()
const { t } = useI18n()

const loading = ref(false)
const keyword = ref('')
const notes = ref(flattenNotes([]))

const pinnedNotes = computed(() => notes.value.filter((n) => n.isPinned))

const filteredNotes = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  return notes.value.filter((item) => {
    if (item.isPinned) return false
    if (!q) return true
    return item.title.toLowerCase().includes(q) || item.folderPath.toLowerCase().includes(q)
  })
})

async function loadNotes() {
  loading.value = true
  try {
    const tree = await fetchNotebookTree()
    notes.value = flattenNotes(tree)
  } finally {
    loading.value = false
  }
}

function openNote(id: number) {
  router.push(`/notebook/${id}`)
}

async function onCreateNote() {
  const created = await createNoteRequest({
    title: t('notebook.untitled'),
    content: '',
    tagIds: [],
  })
  await loadNotes()
  router.push(`/notebook/${created.id}`)
}

onMounted(() => {
  void loadNotes()
})
</script>
