<template>
  <SchemeADoodleFrame
    :tag="tag"
    v-bind="extraAttrs"
    class="ec-doodle-card"
    :class="[attrs.class, { 'ec-doodle-card--clickable': clickable }]"
    :color="color"
    :seed="seed"
    :shape="shape"
    sketch
    :stroke-width="strokeWidth"
    :shadow="shadow"
  >
    <slot />
  </SchemeADoodleFrame>
</template>

<script setup lang="ts">
import { computed, useAttrs } from 'vue'
import SchemeADoodleFrame from '@/mobile/home/themes/scheme-a/SchemeADoodleFrame.vue'
import type { DoodleFrameShape } from '@/mobile/home/themes/scheme-a/doodlePaths'

withDefaults(
  defineProps<{
    tag?: string
    color?: string
    seed?: number
    shape?: DoodleFrameShape
    strokeWidth?: number
    shadow?: boolean
    clickable?: boolean
  }>(),
  {
    tag: 'div',
    color: '#cbd5e1',
    shape: 'rect',
    strokeWidth: 3,
    shadow: false,
    clickable: false,
  },
)

defineOptions({ inheritAttrs: false })
const attrs = useAttrs()

const extraAttrs = computed(() => {
  const raw = attrs as Record<string, unknown>
  const { class: _c, style: _s, ...rest } = raw
  return rest
})
</script>

<style scoped lang="scss">
.ec-doodle-card {
  width: 100%;
  min-width: 0;
  border: none !important;
  background: #fff;

  :deep(.sa-doodle-frame__body) {
    box-sizing: border-box;
    padding: 4px 5px 8px;
    height: 100%;
  }

  &--clickable {
    cursor: pointer;
  }
}
</style>
