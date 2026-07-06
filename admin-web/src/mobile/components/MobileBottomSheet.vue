<template>
  <Teleport to="body">
    <Transition :name="transitionName">
      <div v-if="modelValue" class="mobile-bottom-sheet" @click.self="close">
        <div class="mobile-bottom-sheet__panel" role="dialog" aria-modal="true">
          <!-- Header -->
          <header class="mobile-bottom-sheet__header" v-if="$slots.header || title">
            <slot name="header">
              <div class="mobile-bottom-sheet__header-left">
                <slot name="header-left">
                  <h2 class="mobile-bottom-sheet__title">{{ title }}</h2>
                </slot>
              </div>
              <button
                v-if="showClose"
                type="button"
                class="mobile-bottom-sheet__close"
                aria-label="关闭"
                @click="close"
              >
                <slot name="close-icon">×</slot>
              </button>
            </slot>
          </header>

          <!-- Loading state -->
          <div v-if="loading" class="mobile-bottom-sheet__loading">
            <div class="mobile-bottom-sheet__loading-spinner"></div>
            <span>{{ loadingText }}</span>
          </div>

          <!-- Body (scrollable content) -->
          <div v-else class="mobile-bottom-sheet__body">
            <slot />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  modelValue: boolean
  title?: string
  loading?: boolean
  loadingText?: string
  transitionName?: string
  showClose?: boolean
}>(), {
  title: '',
  loading: false,
  loadingText: '加载中...',
  transitionName: 'mobile-bottom-sheet',
  showClose: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

function close() {
  emit('update:modelValue', false)
}
</script>

<style scoped lang="scss">
.mobile-bottom-sheet {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 200;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  background: rgb(15 23 42 / 45%);
}

.mobile-bottom-sheet__panel {
  width: 100%;
  max-height: 92dvh;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 20px 20px 0 0;
  overflow: hidden;
  font-family: 'ZCOOL KuaiLe', 'Alibaba PuHuiTi', 'PingFang SC', sans-serif;
  color: #1e293b;
}

.mobile-bottom-sheet__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 16px 12px;
  border-bottom: 2px dashed #e2e8f0;
  flex-shrink: 0;
}

.mobile-bottom-sheet__header-left {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.mobile-bottom-sheet__title {
  font-size: 18px;
  font-weight: 800;
  color: #1e293b;
  margin: 0;
  line-height: 1.3;
}

.mobile-bottom-sheet__close {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 50%;
  background: #f1f5f9;
  color: #64748b;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
  transition: transform 0.15s ease;

  &:active {
    transform: scale(0.92);
  }
}

.mobile-bottom-sheet__loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 60px 20px;
  font-size: 14px;
  font-weight: 600;
  color: #94a3b8;
}

.mobile-bottom-sheet__loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #e2e8f0;
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.mobile-bottom-sheet__body {
  flex: 1;
  overflow-y: auto;
  padding: 14px 16px max(20px, env(safe-area-inset-bottom));
}

/* Transition - slide up from bottom */
.mobile-bottom-sheet-enter-active,
.mobile-bottom-sheet-leave-active {
  transition: opacity 0.2s ease;

  .mobile-bottom-sheet__panel {
    transition: transform 0.25s ease;
  }
}

.mobile-bottom-sheet-enter-from,
.mobile-bottom-sheet-leave-to {
  opacity: 0;

  .mobile-bottom-sheet__panel {
    transform: translateY(100%);
  }
}
</style>