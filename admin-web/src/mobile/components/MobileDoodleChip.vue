<template>
  <SchemeADoodleFrame
    :tag="tag"
    v-bind="extraAttrs"
    :shape="shape"
    :color="color"
    :seed="seed"
    :stroke-width="strokeWidth"
    :shadow="false"
    sketch
    class="mobile-doodle-chip"
    :class="[
      {
        'mobile-doodle-chip--inline': inline,
        'mobile-doodle-chip--filled': filled,
      },
      attrs.class,
    ]"
    :style="chipStyle"
  >
    <slot />
  </SchemeADoodleFrame>
</template>

<script setup lang="ts">
import { computed, useAttrs } from 'vue'
import SchemeADoodleFrame from '@/mobile/home/themes/scheme-a/SchemeADoodleFrame.vue'
import type { DoodleFrameShape } from '@/mobile/home/themes/scheme-a/doodlePaths'

const props = withDefaults(
  defineProps<{
    tag?: string
    shape?: DoodleFrameShape
    color?: string
    seed?: number
    strokeWidth?: number
    /** 不占满父宽，用于标签/按钮 */
    inline?: boolean
    /** 实心填充（筛选项选中等） */
    filled?: boolean
    fillColor?: string
  }>(),
  {
    tag: 'span',
    shape: 'pill',
    color: '#cbd5e1',
    strokeWidth: 2.5,
    inline: true,
    filled: false,
  },
)

defineOptions({ inheritAttrs: false })
const attrs = useAttrs()

const extraAttrs = computed(() => {
  const raw = attrs as Record<string, unknown>
  const { class: _c, style: _s, ...rest } = raw
  return rest
})

const chipStyle = computed(() => {
  if (!props.filled) return undefined
  const bg = props.fillColor ?? props.color
  return { '--mdc-fill': bg }
})
</script>

<style scoped lang="scss">
.mobile-doodle-chip {
  width: auto;
  max-width: 100%;
  border: none !important;
  background: transparent;

  :deep(.sa-doodle-frame) {
    background: transparent;
  }

  :deep(.sa-doodle-frame__body) {
    padding: 5px 10px;
  }

  &--inline {
    display: inline-block;
    width: auto;
    vertical-align: middle;
  }

  &:not(.mobile-doodle-chip--inline) {
    display: block;
    width: 100%;
  }

  &--filled {
    :deep(.sa-doodle-frame) {
      background: var(--mdc-fill, #8b5cf6);
    }

    :deep(.sa-doodle-frame__body) {
      color: #991b1b;
    }
  }
}
</style>
