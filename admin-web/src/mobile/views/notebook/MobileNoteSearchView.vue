<template>
  <div class="v2-note-search">
    <div class="v2-note-search__header">
      <button class="v2-note-search__back" @click="goBack">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6" />
        </svg>
      </button>
      <div class="v2-note-search__input-wrap">
        <svg class="v2-note-search__input-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
        </svg>
        <input
          ref="searchInput"
          v-model="keyword"
          class="v2-note-search__input"
          type="text"
          :placeholder="t('notebook.searchPlaceholder')"
          @input="onInput"
        />
        <button v-if="keyword" class="v2-note-search__clear" @click="clearSearch">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>
    </div>

    <div class="v2-note-search__body">
      <div v-if="loading" class="v2-note-search__status">加载中…</div>
      <div v-else-if="keyword && !results.length" class="v2-note-search__empty">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
        </svg>
        <p>{{ t('notebook.noResults') }}</p>
      </div>
      <div v-else-if="!keyword" class="v2-note-search__hint">
        <p>{{ t('notebook.searchHint') }}</p>
      </div>

      <MobileCard v-else class="v2-note-search__results">
        <div
          v-for="item in results"
          :key="item.id"
          class="v2-note-search__item"
          @click="openNote(item.id)"
        >
          <div class="v2-note-search__item-icon" :style="{ background: getNoteColor(item.id) }">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="8" y="2" width="8" height="4" rx="1"/><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/></svg>
          </div>
          <div class="v2-note-search__item-body">
            <div class="v2-note-search__item-title">{{ item.title || t('notebook.untitled') }}</div>
            <div v-if="item.contentExcerpt" class="v2-note-search__item-excerpt">{{ item.contentExcerpt }}</div>
          </div>
        </div>
      </MobileCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { searchNotes, type NbNoteDetail } from '@/api/notebook'
import MobileCard from '@/mobile/components/MobileCard.vue'

const router = useRouter()
const { t } = useI18n()

const keyword = ref('')
const results = ref<NbNoteDetail[]>([])
const loading = ref(false)
const searchInput = ref<HTMLInputElement>()

const NOTE_COLORS = ['#2563eb', '#7c3aed', '#059669', '#ea580c', '#0891b2', '#f97316', '#d97706', '#6b7280']

function getNoteColor(id: number): string {
  return NOTE_COLORS[id % NOTE_COLORS.length]
}

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

<style scoped lang="scss">
.v2-note-search {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.v2-note-search__header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  position: sticky;
  top: 0;
  z-index: 10;
  background: var(--wr-bg, #f9f9fa);
}

.v2-note-search__back {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: 1.5px solid var(--wr-border, #e8ecef);
  background: var(--wr-card, #ffffff);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  color: var(--wr-text, #333333);
  transition: all 0.15s;

  &:hover {
    border-color: var(--wr-stat-blue, #2563eb);
    color: var(--wr-stat-blue, #2563eb);
  }

  &:active {
    opacity: 0.75;
  }
}

.v2-note-search__input-wrap {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
}

.v2-note-search__input-icon {
  position: absolute;
  left: 12px;
  pointer-events: none;
  color: var(--wr-muted, #999999);
}

.v2-note-search__input {
  width: 100%;
  height: 40px;
  padding: 0 36px 0 36px;
  border-radius: 10px;
  border: 1.5px solid var(--wr-border, #e8ecef);
  background: var(--wr-card, #ffffff);
  font-size: 14px;
  outline: none;
  box-sizing: border-box;
  color: var(--wr-text, #333333);
  transition: all 0.15s;

  &:focus {
    border-color: var(--wr-stat-blue, #2563eb);
  }

  &::placeholder {
    color: var(--wr-muted, #999999);
  }
}

.v2-note-search__clear {
  position: absolute;
  right: 8px;
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 50%;
  color: var(--wr-muted, #999999);
  transition: all 0.15s;

  &:hover {
    color: var(--wr-text, #333333);
  }

  &:active {
    opacity: 0.75;
  }
}

.v2-note-search__body {
  flex: 1;
  padding: 4px 4px 80px;
}

.v2-note-search__status {
  text-align: center;
  color: var(--wr-text-secondary, #666666);
  padding: 32px;
  font-size: 14px;
}

.v2-note-search__empty,
.v2-note-search__hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 16px;
  color: var(--wr-muted, #999999);
  font-size: 14px;
  text-align: center;

  svg {
    margin-bottom: 12px;
  }

  p {
    margin: 0;
  }
}

.v2-note-search__results {
  padding: 0 10px;
}

.v2-note-search__item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--wr-border, #e8ecef);
  cursor: pointer;
  transition: background 0.15s;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: var(--wr-index-bg, #eff6ff);
  }
}

.v2-note-search__item-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 2px;
}

.v2-note-search__item-body {
  flex: 1;
  min-width: 0;
}

.v2-note-search__item-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--wr-text, #333333);
  line-height: 1.4;
  margin-bottom: 4px;
}

.v2-note-search__item-excerpt {
  font-size: 13px;
  color: var(--wr-text-secondary, #666666);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
