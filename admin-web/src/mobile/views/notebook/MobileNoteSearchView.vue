<template>
  <div class="mobile-note-search">
    <!-- Header -->
    <div class="mobile-note-search__header">
      <button class="mobile-note-search__back" @click="goBack">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6" />
        </svg>
      </button>
      <div class="mobile-note-search__input-wrap">
        <svg class="mobile-note-search__input-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
        </svg>
        <input
          ref="searchInput"
          v-model="keyword"
          class="mobile-note-search__input"
          type="text"
          :placeholder="t('mobile.notebook.searchPlaceholder')"
          @input="onInput"
        />
        <button v-if="keyword" class="mobile-note-search__clear" @click="clearSearch">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>
    </div>

    <!-- Search Results -->
    <div class="mobile-note-search__body">
      <div v-if="loading" class="mobile-note-search__status">{{ t('common.loading') }}</div>
      <div v-else-if="keyword && !results.length" class="mobile-note-search__empty">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#ccc" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
        </svg>
        <p>{{ t('mobile.notebook.noResults') }}</p>
      </div>
      <div v-else-if="!keyword" class="mobile-note-search__hint">
        <p>{{ t('mobile.notebook.searchHint') }}</p>
      </div>

      <div v-else class="mobile-note-search__results">
        <SchemeADoodleFrame
          v-for="item in results"
          :key="item.id"
          :seed="item.id"
          color="#2563eb"
          sketch
          :shadow="false"
          class="mobile-note-search__card"
          @click="openNote(item.id)"
        >
          <div class="search-card__body">
            <div class="search-card__title">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <polyline points="14 2 14 8 20 8"/>
              </svg>
              <span>{{ item.title || t('notebook.untitled') }}</span>
            </div>
            <div v-if="item.contentExcerpt" class="search-card__excerpt">{{ item.contentExcerpt }}</div>
          </div>
        </SchemeADoodleFrame>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { searchNotes, type NbNoteDetail } from '@/api/notebook'
import SchemeADoodleFrame from '@/mobile/views/home/themes/scheme-a/SchemeADoodleFrame.vue'

const router = useRouter()
const { t } = useI18n()

const keyword = ref('')
const results = ref<NbNoteDetail[]>([])
const loading = ref(false)
const searchInput = ref<HTMLInputElement>()

let searchTimer: ReturnType<typeof setTimeout> | null = null

function goBack() {
  router.back()
}

function clearSearch() {
  keyword.value = ''
  results.value = []
  searchInput.value?.focus()
}

function openNote(id: number) {
  router.push(`/notebook/${id}`)
}

function onInput() {
  if (searchTimer) clearTimeout(searchTimer)
  if (!keyword.value.trim()) {
    results.value = []
    return
  }
  searchTimer = setTimeout(() => {
    void doSearch()
  }, 300)
}

async function doSearch() {
  const kw = keyword.value.trim()
  if (!kw) return
  loading.value = true
  try {
    results.value = await searchNotes(kw)
  } catch {
    results.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  nextTick(() => searchInput.value?.focus())
})
</script>

<style scoped>
.mobile-note-search {
  min-height: 100vh;
  background: #fff;
  display: flex;
  flex-direction: column;
}

.mobile-note-search__header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 12px 8px;
  background: #fff;
  position: sticky;
  top: 0;
  z-index: 10;
}

.mobile-note-search__back {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  border: 1.5px solid #333;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
}

.mobile-note-search__back:active {
  background: #e5e5e5;
}

.mobile-note-search__input-wrap {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
}

.mobile-note-search__input-icon {
  position: absolute;
  left: 10px;
  pointer-events: none;
}

.mobile-note-search__input {
  width: 100%;
  height: 40px;
  padding: 0 36px 0 32px;
  border-radius: 20px;
  border: 1.5px solid #ddd;
  background: #f5f5f5;
  font-size: 15px;
  outline: none;
  font-family: 'ZCOOL KuaiLe', cursive;
  box-sizing: border-box;
}

.mobile-note-search__input:focus {
  border-color: #2563eb;
  background: #fff;
}

.mobile-note-search__clear {
  position: absolute;
  right: 6px;
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 50%;
}

.mobile-note-search__clear:active {
  background: #e5e5e5;
}

.mobile-note-search__body {
  flex: 1;
  padding: 8px 12px 80px;
}

.mobile-note-search__status {
  text-align: center;
  color: #888;
  padding: 32px;
  font-size: 14px;
  font-family: 'ZCOOL KuaiLe', cursive;
}

.mobile-note-search__empty,
.mobile-note-search__hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 16px;
  color: #999;
  font-size: 14px;
  font-family: 'ZCOOL KuaiLe', cursive;
  text-align: center;
}

.mobile-note-search__empty svg,
.mobile-note-search__hint svg {
  margin-bottom: 12px;
}

.mobile-note-search__results {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.mobile-note-search__card {
  background: #fff;
  border-radius: 12px;
  cursor: pointer;
}

.search-card__body {
  padding: 24px;
}

.search-card__title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 600;
  color: #333;
  font-family: 'ZCOOL KuaiLe', cursive;
}

.search-card__excerpt {
  margin-top: 6px;
  font-size: 13px;
  color: #666;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.search-card__excerpt {
  margin-top: 6px;
  font-size: 13px;
  color: #666;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
