<template>
  <div class="mobile-page-header" :class="classList">
    <div class="mobile-page-header__left">
      <MobileDoodleChip
        v-if="showBack"
        tag="button"
        type="button"
        shape="pill"
        color="#2563eb"
        class="mobile-page-header__back"
        @click="handleBack"
      >
        <span>{{ backIcon }}</span>
      </MobileDoodleChip>
      <slot name="left">
        <h1 class="mobile-page-header__title">{{ title }}</h1>
      </slot>
    </div>
    <div class="mobile-page-header__right">
      <slot name="right" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import MobileDoodleChip from './MobileDoodleChip.vue'

const props = withDefaults(
  defineProps<{
    title?: string
    showBack?: boolean
    backIcon?: string
    class?: string
  }>(),
  {
    showBack: true,
    backIcon: '←',
  },
)

const emit = defineEmits<{
  back: []
}>()

const classList = computed(() => ({
  [props.class as string]: !!props.class,
}))

function handleBack() {
  emit('back')
}
</script>

<style scoped lang="scss">
.mobile-page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: max(16px, env(safe-area-inset-top)) 16px 16px;
  background: #fff;
}

.mobile-page-header__left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mobile-page-header__back {
  width: 36px;
  height: 36px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #2563eb;
  font-weight: 700;
  cursor: pointer;
  background: #fff;
  transition: transform 0.2s ease;

  :deep(.sa-doodle-frame__body) {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    padding: 0;
  }

  &:active {
    transform: scale(0.9);
  }
}

.mobile-page-header__title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 24px;
  color: #1e293b;
  margin: 0;
}

.mobile-page-header__right {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
