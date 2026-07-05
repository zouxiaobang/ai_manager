<template>
  <div class="mobile-section-header" :class="classList">
    <img v-if="icon" :src="icon" class="mobile-section-header__icon" alt="" />
    <span v-else-if="iconText" class="mobile-section-header__icon-text">{{ iconText }}</span>
    <img v-else :src="schemeAAssets.starYellow" class="mobile-section-header__icon" alt="" />
    <slot name="icon" />
    <h2 class="mobile-section-header__title">
      <slot name="title">{{ title }}</slot>
    </h2>
    <span v-if="count != null" class="mobile-section-header__count">
      {{ count }}{{ countUnit }}
    </span>
    <div class="mobile-section-header__actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { schemeAAssets } from '@/mobile/views/home/themes/scheme-a/assets'

const props = withDefaults(
  defineProps<{
    icon?: string
    iconText?: string
    title?: string
    count?: number
    countUnit?: string
    class?: string
  }>(),
  {
    countUnit: '个',
  },
)

const classList = computed(() => ({
  [props.class as string]: !!props.class,
}))
</script>

<style scoped lang="scss">
.mobile-section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.mobile-section-header__icon {
  width: 22px;
  height: 22px;
}

.mobile-section-header__icon-text {
  font-size: 18px;
}

.mobile-section-header__title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 18px;
  color: #1e293b;
  margin: 0;
}

.mobile-section-header__count {
  font-size: 12px;
  color: #94a3b8;
  margin-left: auto;
}

.mobile-section-header__actions {
  margin-left: auto;
  display: flex;
  align-items: center;
}
</style>
