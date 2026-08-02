<template>
  <Teleport to="body">
    <Transition :name="transitionName">
      <div v-if="modelValue" class="v2-bottom-sheet" @click.self="close">
        <div class="v2-bottom-sheet__panel" role="dialog" aria-modal="true">
          <header v-if="$slots.header" class="v2-bottom-sheet__header">
            <slot name="header" />
          </header>
          <div v-if="loading" class="v2-bottom-sheet__loading">
            <div class="v2-bottom-sheet__loading-spinner"></div>
          </div>
          <div v-else class="v2-bottom-sheet__body">
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
  loading?: boolean
  transitionName?: string
}>(), {
  loading: false,
  transitionName: 'v2-bottom-sheet',
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

function close() {
  emit('update:modelValue', false)
}
</script>

<style scoped lang="scss">
.v2-bottom-sheet {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 200;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  background: rgba(15, 23, 42, 0.45);
}

.v2-bottom-sheet__panel {
  width: 100%;
  max-height: 92dvh;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 20px 20px 0 0;
  overflow: hidden;
  color: #1e293b;
}

.v2-bottom-sheet__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 16px 12px;
  border-bottom: 2px dashed #e2e8f0;
  flex-shrink: 0;
}

.v2-bottom-sheet__loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
}

.v2-bottom-sheet__loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #e2e8f0;
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: v2-spin 0.7s linear infinite;
}

@keyframes v2-spin {
  to {
    transform: rotate(360deg);
  }
}

.v2-bottom-sheet__body {
  flex: 1;
  overflow-y: auto;
  padding: 14px 16px max(20px, env(safe-area-inset-bottom));
}

.v2-bottom-sheet-enter-active,
.v2-bottom-sheet-leave-active {
  transition: opacity 0.2s ease;

  .v2-bottom-sheet__panel {
    transition: transform 0.25s ease;
  }
}

.v2-bottom-sheet-enter-from,
.v2-bottom-sheet-leave-to {
  opacity: 0;

  .v2-bottom-sheet__panel {
    transform: translateY(100%);
  }
}
</style>
