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

          <!-- 正文阅读区域：排版样式统一来自公共 note-content.scss（与文件夹导出 PDF 同源） -->
          <div class="note-fullscreen-viewer__body" ref="bodyRef">
            <div
              v-if="isMd && mdHtml"
              class="note-fullscreen-viewer__md note-content-body"
              v-html="mdHtml"
            />
            <div
              v-else-if="html"
              class="note-fullscreen-viewer__html note-content-body"
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

/* PC 宽屏（>1200，笔记本桌面档）：放宽高度上限，让正文展示更多内容；窄屏保持原尺寸 */
@media (min-width: 1201px) {
  .note-fullscreen-viewer__panel {
    height: 94vh;
    max-height: 94vh;
  }
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
</style>

<!-- 全局覆盖：消除 el-empty 在暗遮罩下的默认白色背景干扰 -->
<style lang="scss">
.note-fullscreen-viewer__empty .el-empty {
  background: transparent;
}
</style>
