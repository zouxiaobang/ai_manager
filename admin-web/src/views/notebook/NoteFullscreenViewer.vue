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

/* —— 正文阅读样式：参考 AI 智能问答回答的多彩 Markdown 渲染 —— */
.note-fullscreen-viewer__html,
.note-fullscreen-viewer__md {
  max-width: 100%;
  font-size: 16px;
  line-height: 1.8;
  color: var(--wr-text, #333);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI',
    system-ui, 'Ubuntu', 'Droid Sans', sans-serif;
}

/* 作用域限定在正文容器内，避免影响顶部标题等非正文元素 */
.note-fullscreen-viewer__body :deep(p) {
  margin: 0 0 10px;
  line-height: 1.75;

  &:last-child { margin-bottom: 0; }
}

.note-fullscreen-viewer__body :deep(strong) {
  font-weight: 700;
  color: #b91c1c;
}

.note-fullscreen-viewer__body :deep(em) {
  color: #d97706;
  font-style: italic;
}

.note-fullscreen-viewer__body :deep(code) {
  background: #fef2f2;
  color: #b91c1c;
  padding: 2px 7px;
  border-radius: 4px;
  font-size: 0.85em;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  border: 1px solid #fecaca;
}

.note-fullscreen-viewer__body :deep(pre) {
  background: #18181b;
  color: #e4e4e7;
  padding: 14px 18px;
  border-radius: 10px;
  overflow-x: auto;
  margin: 12px 0;
  font-size: 13px;
  line-height: 1.6;
  border: 1px solid #27272a;

  code {
    background: none;
    padding: 0;
    border: none;
    border-radius: 0;
    color: inherit;
    font-size: inherit;
  }
}

.note-fullscreen-viewer__body :deep(ul),
.note-fullscreen-viewer__body :deep(ol) {
  margin: 6px 0;
  padding-left: 22px;

  li {
    margin: 4px 0;
    line-height: 1.6;
  }
}

.note-fullscreen-viewer__body :deep(ul) {
  list-style-type: disc;

  ul { list-style-type: circle; }
}

.note-fullscreen-viewer__body :deep(ol) {
  list-style-type: decimal;

  ol { list-style-type: lower-alpha; }
}

.note-fullscreen-viewer__body :deep(h1) {
  margin: 24px 0 12px;
  font-size: 1.6em;
  font-weight: 700;
  color: #991b1b;
  padding-bottom: 8px;
  border-bottom: 3px solid #fca5a5;
}

.note-fullscreen-viewer__body :deep(h2) {
  margin: 20px 0 10px;
  font-size: 1.35em;
  font-weight: 700;
  color: #c2410c;
  padding-bottom: 6px;
  border-bottom: 2px solid #fdba74;
}

.note-fullscreen-viewer__body :deep(h3) {
  margin: 16px 0 8px;
  font-size: 1.15em;
  font-weight: 600;
  color: #a16207;
}

.note-fullscreen-viewer__body :deep(h4) {
  margin: 12px 0 6px;
  font-size: 1.05em;
  font-weight: 600;
  color: #7c3aed;
}

.note-fullscreen-viewer__body :deep(h5) {
  margin: 10px 0 6px;
  font-size: 1em;
  font-weight: 600;
  color: #374151;
}

.note-fullscreen-viewer__body :deep(h6) {
  margin: 10px 0 6px;
  font-size: 0.95em;
  font-weight: 600;
  color: #6b7280;
}

/* 任务列表 checkbox */
.note-fullscreen-viewer__body :deep(.task-list-item) {
  list-style: none;
  margin-left: -22px;

  input[type='checkbox'] {
    appearance: none;
    -webkit-appearance: none;
    width: 15px;
    height: 15px;
    border: 2px solid #d1d5db;
    border-radius: 3px;
    margin-right: 7px;
    vertical-align: middle;
    cursor: default;
    position: relative;
    top: -1px;
    transition: all 0.15s;
  }

  input[type='checkbox']:checked {
    background: #16a34a;
    border-color: #16a34a;

    &::after {
      content: '';
      position: absolute;
      left: 3px;
      top: 0;
      width: 5px;
      height: 9px;
      border: solid #fff;
      border-width: 0 2px 2px 0;
      transform: rotate(45deg);
    }
  }
}

/* 键盘按键 */
.note-fullscreen-viewer__body :deep(kbd) {
  background: linear-gradient(180deg, #f9fafb 0%, #f3f4f6 100%);
  border: 1px solid #d1d5db;
  border-radius: 5px;
  padding: 1px 6px;
  font-size: 12px;
  font-family: inherit;
  color: #374151;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
}

/* 高亮标记 */
.note-fullscreen-viewer__body :deep(mark) {
  background: #fef3c7;
  color: #92400e;
  padding: 1px 4px;
  border-radius: 3px;
}

.note-fullscreen-viewer__body :deep(blockquote) {
  margin: 12px 0;
  padding: 10px 16px;
  border-left: 4px solid #f59e0b;
  background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
  color: #92400e;
  border-radius: 0 8px 8px 0;
  line-height: 1.6;
  box-shadow: 0 1px 3px rgba(245, 158, 11, 0.1);

  p { margin: 0; }

  blockquote {
    margin: 8px 0;
    border-left-color: #f97316;
    background: #fff7ed;
    color: #9a3412;
  }
}

.note-fullscreen-viewer__body :deep(a) {
  color: #2563eb;
  text-decoration: none;
  font-weight: 500;
  border-bottom: 1px solid #bfdbfe;
  transition: color 0.15s, border-bottom-color 0.15s;

  &:hover {
    color: #1d4ed8;
    border-bottom-color: #2563eb;
  }

  /* 给外部链接添加小图标指示 */
  &[href^="http"]::after {
    content: '↗';
    display: inline-block;
    font-size: 0.75em;
    margin-left: 2px;
    color: #93c5fd;
    transition: transform 0.15s;
  }

  &:hover[href^="http"]::after {
    transform: translate(1px, -1px);
  }
}

.note-fullscreen-viewer__body :deep(table) {
  border-collapse: separate;
  border-spacing: 0;
  width: 100%;
  margin: 14px 0;
  font-size: 13px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;

  thead {
    background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
    color: #fff;
  }

  th {
    font-weight: 600;
    padding: 10px 14px;
    text-align: left;
    border-bottom: 1px solid #d97706;
    letter-spacing: 0.02em;
  }

  td {
    padding: 9px 14px;
    border-bottom: 1px solid #f3f4f6;
  }

  tbody tr {
    transition: background 0.15s;
  }

  tbody tr:last-child td { border-bottom: none; }

  tbody tr:nth-child(even) td {
    background: #fafaf9;
  }

  tbody tr:hover td {
    background: #fffbeb;
  }
}

.note-fullscreen-viewer__body :deep(hr) {
  border: none;
  height: 2px;
  background: linear-gradient(to right, transparent, #f59e0b, #d97706, #f59e0b, transparent);
  margin: 18px 0;
  border-radius: 2px;
  opacity: 0.6;
}

.note-fullscreen-viewer__body :deep(del) {
  color: #9ca3af;
  text-decoration: line-through;
  text-decoration-color: #d1d5db;
}

.note-fullscreen-viewer__body :deep(img) {
  max-width: 100%;
  border-radius: 10px;
  margin: 10px 0;
  border: 1px solid #f3f4f6;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  }
}

.note-fullscreen-viewer__body :deep(input[type='checkbox']) {
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
