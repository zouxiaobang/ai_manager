<template>
  <Teleport to="body">
    <Transition name="fullscreen-fade">
      <div
        v-if="visible"
        class="rag-doc-preview"
        @keydown.escape="close"
        tabindex="-1"
        ref="viewerRef"
      >
        <!-- 半透明背景遮罩 -->
        <div class="rag-doc-preview__backdrop" @click.self="close" />

        <!-- 全屏预览面板：视觉参考笔记本全屏阅读器（NoteFullscreenViewer） -->
        <div class="rag-doc-preview__panel">
          <!-- 顶部栏：文件名 + 关闭 -->
          <div class="rag-doc-preview__header">
            <h2 class="rag-doc-preview__title">{{ title }}</h2>
            <div class="rag-doc-preview__actions">
              <span class="rag-doc-preview__hint">{{ t('aiKnowledge.rag.previewHint') }}</span>
              <button
                type="button"
                class="rag-doc-preview__close-btn"
                :title="t('aiKnowledge.rag.previewClose')"
                @click="close"
              >
                <el-icon><Close /></el-icon>
              </button>
            </div>
          </div>

          <!-- 正文区域：md 复用 note-content.scss 排版；其余类型走同源基础字号的纯文本 -->
          <div class="rag-doc-preview__body" ref="bodyRef" v-loading="loading">
            <template v-if="!loading && content">
              <div
                v-if="isMd"
                class="rag-doc-preview__md note-content-body"
                v-html="mdHtml"
              />
              <pre v-else class="rag-doc-preview__plain">{{ content }}</pre>
            </template>
            <div v-else-if="!loading && !content" class="rag-doc-preview__state">
              <el-empty :description="t('aiKnowledge.rag.previewEmpty')" :image-size="64" />
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Close } from '@element-plus/icons-vue'
import { marked } from 'marked'

const props = defineProps<{
  visible: boolean
  title: string
  fileType: string
  content: string
  loading: boolean
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const { t } = useI18n()
const viewerRef = ref<HTMLElement | null>(null)
const bodyRef = ref<HTMLElement | null>(null)

/** 仅 Markdown 走 marked 渲染；其余类型（txt/pdf/docx/html）为后端抽好的纯文本 */
const isMd = computed(() => props.fileType.toLowerCase() === 'md')

const mdHtml = computed(() =>
  isMd.value && props.content ? (marked.parse(props.content) as string) : '',
)

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
      // 每次打开滚动到顶部
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
.rag-doc-preview {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  outline: none;
}

/* 淡入淡出（与笔记本全屏预览一致的遮罩层次） */
.fullscreen-fade-enter-active,
.fullscreen-fade-leave-active {
  transition: opacity 0.2s ease;
}
.fullscreen-fade-enter-from,
.fullscreen-fade-leave-to {
  opacity: 0;
}

.rag-doc-preview__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
}

.rag-doc-preview__panel {
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
  animation: rag-doc-preview-in 0.2s ease-out;
}

/* PC 宽屏（>1200）：放宽高度上限，正文展示更多内容；窄屏保持原尺寸 */
@media (min-width: 1201px) {
  .rag-doc-preview__panel {
    height: 94vh;
    max-height: 94vh;
  }
}

@keyframes rag-doc-preview-in {
  from {
    opacity: 0;
    transform: scale(0.96) translateY(12px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.rag-doc-preview__header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 24px;
  border-bottom: 1px solid var(--wr-border, #e8ecef);
  background: var(--wr-card, #fff);
}

.rag-doc-preview__title {
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

.rag-doc-preview__actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.rag-doc-preview__hint {
  font-size: 12px;
  color: var(--wr-muted, #999);
  white-space: nowrap;
}

.rag-doc-preview__close-btn {
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

.rag-doc-preview__body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 32px 40px;
  -webkit-overflow-scrolling: touch;
}

/* md：排版完全复用 note-content.scss（标题/表格/引用/字体颜色与笔记本全屏预览一致） */
.rag-doc-preview__md {
  word-break: break-word;
}

/* 纯文本：与笔记正文同源的基础字号/行高/文字色，保留原始换行 */
.rag-doc-preview__plain {
  margin: 0;
  font-size: 16px;
  line-height: 1.8;
  color: var(--wr-text, #333);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI',
    system-ui, 'Ubuntu', 'Droid Sans', sans-serif;
  white-space: pre-wrap;
  word-break: break-word;
  overflow-wrap: break-word;
}

.rag-doc-preview__state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}
</style>

<!-- 全局覆盖：消除 el-empty 在暗遮罩下的默认白色背景干扰（与笔记本全屏预览同款处理） -->
<style lang="scss">
.rag-doc-preview__state .el-empty {
  background: transparent;
}
</style>
