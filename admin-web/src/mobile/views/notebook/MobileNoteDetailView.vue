<template>
  <MobilePage>
    <div v-if="loading" class="v2-note-detail__status">加载中…</div>
    <template v-else-if="note">
      <div class="v2-note-detail__container">
        <button
          v-if="tocItems.length > 0"
          class="v2-note-detail__toc-btn"
          :aria-label="t('notebook.showToc')"
          @click="tocVisible = !tocVisible"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/>
          </svg>
        </button>

        <MobileCard>
          <div class="v2-note-detail__header">
            <div class="v2-note-detail__meta">
              <span v-if="note.tags && note.tags.length">
                <span v-for="tag in note.tags" :key="tag.id" class="v2-note-detail__tag">{{ tag.name }}</span>
              </span>
              <span v-if="isMdNote" class="v2-note-detail__type">Markdown</span>
              <span class="v2-note-detail__time">{{ formatTime(note.createTime) }}</span>
            </div>
          </div>
          <!-- 正文排版复用公共 note-content.scss（与 PC 端全屏预览同源，保证两端格式一致） -->
          <div
            ref="contentRef"
            class="note-content-body"
            v-html="renderedContent"
          />
        </MobileCard>

        <Transition name="toc">
          <div v-if="tocVisible" key="toc-group">
            <div class="v2-note-detail__toc-overlay" @click="tocVisible = false" />
            <div class="v2-note-detail__toc-card">
              <div class="v2-note-detail__toc-header">{{ t('notebook.tabs.toc') }}</div>
              <div class="v2-note-detail__toc-list">
                <button
                  v-for="item in tocItems"
                  :key="item.id"
                  class="v2-note-detail__toc-item"
                  :class="`is-level-${item.level}`"
                  @click="scrollToHeading(item.id)"
                >
                  {{ item.text }}
                </button>
              </div>
            </div>
          </div>
        </Transition>
      </div>
    </template>
    <div v-else class="v2-note-detail__empty">{{ t('notebook.emptyTree') }}</div>
  </MobilePage>
</template>

<script setup lang="ts">
import { computed, ref, nextTick, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { fetchNote, type NbNoteDetail } from '@/api/notebook'
import { useMobileAppHeaderTitle } from '@/composables/useMobileAppHeaderTitle'
import MobilePage from '@/mobile/components/MobilePage.vue'
import MobileCard from '@/mobile/components/MobileCard.vue'
import { parseTocItems, renderNoteBody } from './mobileNoteRender'

const route = useRoute()
const { t } = useI18n()
const { setHeaderTitle } = useMobileAppHeaderTitle()

const loading = ref(true)
const note = ref<NbNoteDetail | null>(null)
const tocVisible = ref(false)
const contentRef = ref<HTMLElement>()

function formatTime(time?: string): string {
  if (!time) return ''
  const date = new Date(time)
  return `${date.getFullYear()}/${(date.getMonth() + 1).toString().padStart(2, '0')}/${date.getDate().toString().padStart(2, '0')} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}

/** 与 PC 端一致：标题以 .md 结尾判定为 Markdown 笔记（后端 noteType 对 md 笔记为 "NOTE"，不作为判定依据） */
const isMdNote = computed(() => (note.value?.title || '').toLowerCase().endsWith('.md'))

const renderedContent = computed(() => {
  const raw = note.value?.content
  if (!raw) return t('notebook.emptyContent')
  return renderNoteBody(isMdNote.value, raw)
})

const tocItems = computed(() => {
  return parseTocItems(renderedContent.value)
})

function scrollToHeading(id: string) {
  tocVisible.value = false
  nextTick(() => {
    const el = document.getElementById(id)
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  })
}

let disposed = false

;(async () => {
  const id = Number(route.params.id)
  if (!id) {
    loading.value = false
    return
  }
  try {
    note.value = await fetchNote(id)
    if (disposed) return
    // 把笔记标题提升为 header 标题，正文区不再重复占用一行大标题
    setHeaderTitle(note.value?.title || t('notebook.untitled'))
  } finally {
    loading.value = false
  }
})()

onBeforeUnmount(() => {
  disposed = true
  setHeaderTitle(null)
})
</script>

<style scoped lang="scss">
.v2-note-detail__status {
  text-align: center;
  color: var(--wr-text-secondary, #666666);
  padding: 32px;
  font-size: 14px;
}

.v2-note-detail__empty {
  text-align: center;
  padding: 60px 20px;
  color: var(--wr-muted, #999999);
  font-size: 14px;
}

.v2-note-detail__container {
  position: relative;
}

.v2-note-detail__toc-btn {
  position: absolute;
  top: -8px;
  left: -4px;
  z-index: 20;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: 1.5px solid var(--wr-border, #e8ecef);
  background: rgba(255, 255, 255, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--wr-text-secondary, #666666);
  transition: all 0.15s;
  box-shadow: var(--wr-shadow, 0 2px 8px rgb(0 0 0 / 6%));

  &:hover {
    border-color: var(--wr-stat-blue, #2563eb);
    color: var(--wr-stat-blue, #2563eb);
  }

  &:active {
    opacity: 0.75;
  }
}

.v2-note-detail__toc-overlay {
  position: fixed;
  inset: 0;
  z-index: 19;
  background: transparent;
}

.v2-note-detail__toc-card {
  position: absolute;
  top: 36px;
  left: -4px;
  z-index: 20;
  width: 220px;
  max-height: 320px;
  background: var(--wr-card, #ffffff);
  border: 1px solid var(--wr-border, #e8ecef);
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.v2-note-detail__toc-header {
  padding: 12px 14px 8px;
  font-size: 13px;
  font-weight: 700;
  color: var(--wr-text, #333333);
  border-bottom: 1px solid var(--wr-border, #e8ecef);
}

.v2-note-detail__toc-list {
  flex: 1;
  overflow-y: auto;
  padding: 6px 0;
}

.v2-note-detail__toc-item {
  display: block;
  width: 100%;
  padding: 7px 14px;
  border: none;
  background: transparent;
  text-align: left;
  font-size: 13px;
  line-height: 1.4;
  color: var(--wr-text, #333333);
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: background 0.15s;

  &:hover {
    background: var(--wr-index-bg, #eff6ff);
    color: var(--wr-stat-blue, #2563eb);
  }

  &.is-level-1 { padding-left: 14px; font-weight: 600; }
  &.is-level-2 { padding-left: 28px; }
  &.is-level-3 { padding-left: 42px; font-size: 12px; }
  &.is-level-4 { padding-left: 56px; font-size: 12px; }
  &.is-level-5 { padding-left: 70px; font-size: 12px; }
  &.is-level-6 { padding-left: 84px; font-size: 12px; }
}

.toc-enter-active,
.toc-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.toc-enter-from,
.toc-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.v2-note-detail__header {
  margin-bottom: 10px;
}

.v2-note-detail__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.v2-note-detail__tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  background: var(--wr-index-bg, #eff6ff);
  color: var(--wr-stat-blue, #2563eb);
  font-size: 12px;
  font-weight: 500;
}

.v2-note-detail__type {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f0fdf4;
  color: #059669;
  font-size: 12px;
  font-weight: 500;
}

.v2-note-detail__time {
  font-size: 12px;
  color: var(--wr-muted, #999999);
}

/* 正文排版已统一由公共 note-content.scss（.note-content-body）负责，与 PC 端全屏预览同源 */
</style>
