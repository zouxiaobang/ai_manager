<template>
  <Teleport to="body">
    <Transition name="fullscreen-fade">
      <div
        v-if="visible"
        class="note-fullscreen-viewer"
        @keydown.escape="close"
        tabindex="-1"
        ref="viewerRef"
      >
        <!-- 半透明背景遮罩 -->
        <div class="note-fullscreen-viewer__backdrop" @click.self="close" />

        <!-- 全屏阅读面板 -->
        <div class="note-fullscreen-viewer__panel">
          <!-- 顶部栏：标题 + 操作按钮 -->
          <div class="note-fullscreen-viewer__header">
            <h2 class="note-fullscreen-viewer__title">{{ title }}</h2>
            <div class="note-fullscreen-viewer__actions">
              <span class="note-fullscreen-viewer__hint">{{ t('notebook.fullscreenHint') }}</span>
              <button
                type="button"
                class="note-fullscreen-viewer__close-btn"
                :title="t('notebook.fullscreenClose')"
                @click="close"
              >
                <el-icon><Close /></el-icon>
              </button>
            </div>
          </div>

          <!-- 正文阅读区域 -->
          <div class="note-fullscreen-viewer__body" ref="bodyRef">
            <div
              v-if="isMd && mdHtml"
              class="note-fullscreen-viewer__md"
              v-html="mdHtml"
            />
            <div
              v-else-if="html"
              class="note-fullscreen-viewer__html"
              v-html="html"
            />
            <div v-else class="note-fullscreen-viewer__empty">
              <el-empty :description="t('notebook.emptyContent')" :image-size="64" />
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Close } from '@element-plus/icons-vue'

const props = defineProps<{
  visible: boolean
  title: string
  html?: string
  mdHtml?: string
  isMd: boolean
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const { t } = useI18n()
const viewerRef = ref<HTMLElement | null>(null)
const bodyRef = ref<HTMLElement | null>(null)

function close() {
  emit('update:visible', false)
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && props.visible) {
    close()
  }
}

watch(
  () => props.visible,
  async (open) => {
    if (open) {
      await nextTick()
      viewerRef.value?.focus()
      // 滚动到顶部
      if (bodyRef.value) {
        bodyRef.value.scrollTop = 0
      }
    }
  },
)

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKeydown)
})

// 挂载时注册键盘事件
if (typeof document !== 'undefined') {
  document.addEventListener('keydown', onKeydown)
}
</script>

<style scoped lang="scss">
.note-fullscreen-viewer {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  outline: none;
}

.note-fullscreen-viewer__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
}

.note-fullscreen-viewer__panel {
  position: relative;
  display: flex;
  flex-direction: column;
  width: 92vw;
  max-width: 960px;
  height: 88vh;
  max-height: 800px;
  border-radius: 16px;
  background: var(--wr-card, #fff);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  overflow: hidden;
  animation: fullscreen-panel-in 0.2s ease-out;
}

@keyframes fullscreen-panel-in {
  from {
    opacity: 0;
    transform: scale(0.96) translateY(12px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.note-fullscreen-viewer__header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 24px;
  border-bottom: 1px solid var(--wr-border, #e8ecef);
  background: var(--wr-card, #fff);
}

.note-fullscreen-viewer__title {
  flex: 1;
  min-width: 0;
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  line-height: 1.4;
  color: var(--wr-text, #333);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.note-fullscreen-viewer__actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.note-fullscreen-viewer__hint {
  font-size: 12px;
  color: var(--wr-muted, #999);
  white-space: nowrap;
}

.note-fullscreen-viewer__close-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  padding: 0;
  border: 1px solid var(--wr-border, #e8ecef);
  border-radius: 10px;
  background: var(--wr-card, #fff);
  color: var(--wr-text-secondary, #666);
  font-size: 18px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;

  &:hover {
    background: var(--wr-stat-blue-bg, #eff6ff);
    color: var(--wr-rail-active-color, #0b21c7);
  }
}

.note-fullscreen-viewer__body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 32px 40px;
  -webkit-overflow-scrolling: touch;
}

.note-fullscreen-viewer__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

/* —— 富文本（HTML）阅读样式 —— */
.note-fullscreen-viewer__html,
.note-fullscreen-viewer__md {
  max-width: 100%;
  font-size: 16px;
  line-height: 1.8;
  color: var(--wr-text, #333);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI',
    system-ui, 'Ubuntu', 'Droid Sans', sans-serif;
}

:deep(h1), :deep(h2), :deep(h3), :deep(h4), :deep(h5), :deep(h6) {
  margin-top: 1.6em;
  margin-bottom: 0.6em;
  font-weight: 600;
  line-height: 1.35;
  color: var(--wr-text, #333);
}

:deep(h1) {
  font-size: 1.8em;
  border-bottom: 1px solid var(--wr-border, #e8ecef);
  padding-bottom: 0.3em;
}

:deep(h2) {
  font-size: 1.5em;
  border-bottom: 1px solid var(--wr-border, #e8ecef);
  padding-bottom: 0.3em;
}

:deep(h3) { font-size: 1.25em; }
:deep(h4) { font-size: 1.1em; }

:deep(p) {
  margin: 0 0 1em;
  line-height: 1.8;
}

:deep(ul), :deep(ol) {
  padding-left: 1.8em;
  margin-bottom: 1em;
}

:deep(li) { margin-bottom: 0.3em; }
:deep(li > ul), :deep(li > ol) { margin-bottom: 0; }

:deep(pre) {
  background: #f6f8fa;
  border: 1px solid var(--wr-border, #e8ecef);
  border-radius: 8px;
  padding: 16px 20px;
  overflow-x: auto;
  margin-bottom: 1em;
  font-size: 0.92em;
  line-height: 1.6;
}

:deep(code) {
  font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Consolas',
    'Liberation Mono', Menlo, monospace;
  font-size: 0.9em;
  background: #f0f2f5;
  padding: 2px 6px;
  border-radius: 4px;
  color: #d63384;
}

:deep(pre code) {
  background: none;
  padding: 0;
  border-radius: 0;
  color: inherit;
}

:deep(blockquote) {
  margin: 0 0 1em;
  padding: 8px 0 8px 16px;
  border-left: 4px solid var(--el-color-primary-light-5, #a0c4ff);
  color: var(--wr-text-secondary, #666);
  background: #f8fafc;
  border-radius: 0 6px 6px 0;
}

:deep(blockquote p:last-child) { margin-bottom: 0; }

:deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin-bottom: 1em;
  font-size: 0.95em;
}

:deep(th), :deep(td) {
  border: 1px solid var(--wr-border, #dde1e6);
  padding: 10px 14px;
  text-align: left;
}

:deep(th) {
  background: #f8f9fa;
  font-weight: 600;
}

:deep(tr:nth-child(even) td) {
  background: #fafbfc;
}

:deep(a) {
  color: var(--el-color-primary, #409eff);
  text-decoration: none;
  &:hover { text-decoration: underline; }
}

:deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 6px;
  margin: 0.5em 0;
}

:deep(hr) {
  border: none;
  border-top: 1px solid var(--wr-border, #e8ecef);
  margin: 1.5em 0;
}

:deep(input[type='checkbox']) {
  margin-right: 6px;
  transform: translateY(1px);
}
</style>

<!-- 全局覆盖：消除 el-empty 在暗遮罩下的默认白色背景干扰 -->
<style lang="scss">
.note-fullscreen-viewer__empty .el-empty {
  background: transparent;
}
</style>
