<template>
  <V2Page>
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

        <V2Card>
          <div class="v2-note-detail__header">
            <div class="v2-note-detail__meta">
              <span v-if="note.tags && note.tags.length">
                <span v-for="tag in note.tags" :key="tag.id" class="v2-note-detail__tag">{{ tag.name }}</span>
              </span>
              <span v-if="note.noteType === 'MARKDOWN'" class="v2-note-detail__type">Markdown</span>
              <span class="v2-note-detail__time">{{ formatTime(note.createTime) }}</span>
            </div>
          </div>
          <div
            ref="contentRef"
            class="v2-note-detail__content"
            v-html="renderedContent"
          />
        </V2Card>

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
  </V2Page>
</template>

<script setup lang="ts">
import { computed, ref, nextTick, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { fetchNote, type NbNoteDetail } from '@/api/notebook'
import { useMobileV2AppHeaderTitle } from '@/composables/useMobileV2AppHeaderTitle'
import V2Page from '@/mobile-v2/components/V2Page.vue'
import V2Card from '@/mobile-v2/components/V2Card.vue'

interface TocItem {
  id: string
  level: number
  text: string
}

const route = useRoute()
const { t } = useI18n()
const { setHeaderTitle } = useMobileV2AppHeaderTitle()

const loading = ref(true)
const note = ref<NbNoteDetail | null>(null)
const tocVisible = ref(false)
const contentRef = ref<HTMLElement>()

function formatTime(time?: string): string {
  if (!time) return ''
  const date = new Date(time)
  return `${date.getFullYear()}/${(date.getMonth() + 1).toString().padStart(2, '0')}/${date.getDate().toString().padStart(2, '0')} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}

function addHeadingIds(html: string): string {
  let counter = 0
  return html.replace(/<h([1-6])(\b[^>]*)?>/gi, (full, level, attrs) => {
    if (attrs && /\bid\s*=/i.test(attrs)) return full
    return `<h${level}${attrs || ''} id="nb-heading-${counter++}">`
  })
}

function parseTocItems(html: string): TocItem[] {
  if (!html?.trim()) return []
  const items: TocItem[] = []
  const regex = /<h([1-6])(?:\s[^>]*)?>([\s\S]*?)<\/h\1>/gi
  let match: RegExpExecArray | null
  let index = 0
  while ((match = regex.exec(html)) !== null) {
    const level = Number.parseInt(match[1], 10)
    const text = match[2].replace(/<[^>]+>/g, '').trim()
    if (text) {
      items.push({ id: `nb-heading-${index}`, level, text })
      index++
    }
  }
  return items
}

const renderedContent = computed(() => {
  const raw = note.value?.content
  if (!raw) return t('notebook.emptyContent')

  let html: string
  if (note.value?.noteType === 'MARKDOWN') {
    html = simpleMarkdownToHtml(raw)
  } else {
    html = raw
  }

  return addHeadingIds(html)
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

function simpleMarkdownToHtml(md: string): string {
  let html = md
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')

  html = html.replace(/^### (.+)$/gm, '<h3>$1</h3>')
  html = html.replace(/^## (.+)$/gm, '<h2>$1</h2>')
  html = html.replace(/^# (.+)$/gm, '<h1>$1</h1>')

  html = html.replace(/```([\s\S]*?)```/g, '<pre><code>$1</code></pre>')
  html = html.replace(/`([^`]+)`/g, '<code>$1</code>')

  html = html.replace(/^> (.+)$/gm, '<blockquote><p>$1</p></blockquote>')

  html = html.replace(/^[-*] (.+)$/gm, '<li>$1</li>')
  html = html.replace(/(<li>.*<\/li>\n?)+/g, '<ul>$&</ul>')

  html = html.replace(/^(\d+)\. (.+)$/gm, '<li>$2</li>')
  html = html.replace(/(<li>.*<\/li>\n?)+/g, '<ol>$&</ol>')

  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  html = html.replace(/\*(.+?)\*/g, '<em>$1</em>')

  html = html.replace(/~~(.+?)~~/g, '<del>$1</del>')

  html = html.replace(/!\[([^\]]*)\]\(([^)]+)\)/g, '<img src="$2" alt="$1" />')
  html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener">$1</a>')

  const lines = html.split('\n')
  const result: string[] = []
  let inList = false
  let listType = ''

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]
    const trimmed = line.trim()

    if (!trimmed) {
      if (inList) {
        result.push(listType === 'ul' ? '</ul>' : '</ol>')
        inList = false
        listType = ''
      }
      continue
    }

    if (trimmed.startsWith('<h') || trimmed.startsWith('<pre') || trimmed.startsWith('<blockquote') ||
        trimmed.startsWith('<ul') || trimmed.startsWith('<ol') || trimmed.startsWith('<li') ||
        trimmed.startsWith('<img')) {
      if (inList) {
        result.push(listType === 'ul' ? '</ul>' : '</ol>')
        inList = false
        listType = ''
      }
      result.push(trimmed)
      if (trimmed.startsWith('<pre')) {
        result.push('')
      }
      continue
    }

    if (trimmed.startsWith('<')) {
      if (inList) {
        result.push(listType === 'ul' ? '</ul>' : '</ol>')
        inList = false
        listType = ''
      }
      result.push(trimmed)
      continue
    }

    if (inList) {
      result.push(listType === 'ul' ? '</ol>' : '</ul>')
      inList = false
      listType = ''
    }

    result.push(`<p>${trimmed}</p>`)
  }

  if (inList) {
    result.push(listType === 'ul' ? '</ul>' : '</ol>')
  }

  return result.join('\n')
}

;(async () => {
  const id = Number(route.params.id)
  if (!id) {
    loading.value = false
    return
  }
  try {
    note.value = await fetchNote(id)
    // 把笔记标题提升为 header 标题，正文区不再重复占用一行大标题
    setHeaderTitle(note.value?.title || t('notebook.untitled'))
  } finally {
    loading.value = false
  }
})()

onBeforeUnmount(() => {
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

.v2-note-detail__content {
  font-size: 14px;
  line-height: 1.75;
  color: var(--wr-text, #333333);
  word-break: break-word;

  :deep(img) {
    max-width: 100%;
    height: auto;
    border-radius: 6px;
  }

  :deep(p),
  :deep(li) {
    margin: 0 0 12px;
    font-size: 14px;
    line-height: 1.75;
  }

  :deep(h1),
  :deep(h2),
  :deep(h3),
  :deep(h4),
  :deep(h5) {
    margin: 1.2em 0 0.6em;
    font-weight: 600;
    line-height: 1.4;
  }

  :deep(h1) { font-size: 22px; }
  :deep(h2) { font-size: 20px; }
  :deep(h3) { font-size: 18px; }
  :deep(h4) { font-size: 16px; }

  :deep(blockquote) {
    margin: 8px 0;
    padding: 4px 16px;
    border-left: 3px solid var(--wr-border, #d0d5dd);
    color: var(--wr-text-secondary, #666666);
    background: var(--wr-index-bg, #f8f9fa);
    border-radius: 0 4px 4px 0;
  }

  :deep(pre) {
    margin: 8px 0;
    padding: 12px 16px;
    background: #f5f6f7;
    border-radius: 8px;
    overflow-x: auto;
    font-size: 13px;
    line-height: 1.6;
    font-family: 'Menlo', 'Monaco', 'Courier New', monospace;
  }

  :deep(code) {
    padding: 2px 6px;
    background: #f0f0f0;
    border-radius: 4px;
    font-size: 13px;
    font-family: 'Menlo', 'Monaco', 'Courier New', monospace;
  }

  :deep(pre code) {
    padding: 0;
    background: none;
    border-radius: 0;
  }

  :deep(ul),
  :deep(ol) {
    padding-left: 24px;
    margin: 8px 0;
  }

  :deep(li) {
    margin-bottom: 4px;
  }

  :deep(a) {
    color: var(--wr-stat-blue, #2563eb);
    text-decoration: underline;
  }

  :deep(table) {
    width: 100%;
    border-collapse: collapse;
    margin: 12px 0;
    font-size: 14px;
  }

  :deep(th),
  :deep(td) {
    border: 1px solid var(--wr-border, #e0e0e0);
    padding: 8px 12px;
    text-align: left;
  }

  :deep(th) {
    background: var(--wr-index-bg, #f8f9fa);
    font-weight: 600;
  }

  :deep(hr) {
    margin: 16px 0;
    border: none;
    border-top: 1px solid var(--wr-border, #e0e0e0);
  }
}

/* 横屏：收紧正文行高与段距，放大内容区可读行数 */
@media (orientation: landscape) {
  .v2-note-detail__content {
    line-height: 1.6;

    :deep(p),
    :deep(li) {
      margin-bottom: 8px;
      line-height: 1.6;
    }
  }
}
</style>
