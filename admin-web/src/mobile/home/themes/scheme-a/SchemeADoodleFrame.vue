<template>
  <component
    :is="tag"
    v-bind="passthroughAttrs"
    class="sa-doodle-frame"
    :class="[`sa-doodle-frame--${shape}`, attrs.class]"
    :style="frameStyle"
  >
    <svg
      class="sa-doodle-frame__stroke"
      :viewBox="viewBox"
      preserveAspectRatio="none"
      aria-hidden="true"
    >
      <path
        v-if="sketch"
        :d="pathD"
        fill="none"
        :stroke="color"
        :stroke-width="strokeWidth"
        stroke-linecap="round"
        stroke-linejoin="round"
        vector-effect="non-scaling-stroke"
        opacity="0.35"
        transform="translate(0.8, 0.6)"
      />
      <path
        :d="pathD"
        fill="none"
        :stroke="color"
        :stroke-width="strokeWidth"
        stroke-linecap="round"
        stroke-linejoin="round"
        vector-effect="non-scaling-stroke"
      />
    </svg>
    <div class="sa-doodle-frame__body">
      <slot />
    </div>
  </component>
</template>

<script setup lang="ts">
import { computed, useAttrs } from 'vue'
import {
  DOODLE_FRAME_PATHS,
  DOODLE_FRAME_VIEWBOX,
  resolveDoodleRectPath,
  type DoodleFrameShape,
} from './doodlePaths'

const props = withDefaults(
  defineProps<{
    color?: string
    shape?: DoodleFrameShape
    tag?: string
    strokeWidth?: number
    shadow?: boolean
    /** 矩形边框手绘变体种子，每张卡片可略有不同 */
    seed?: number
    /** 双层描边，增强素描手绘感 */
    sketch?: boolean
  }>(),
  {
    color: '#2563eb',
    shape: 'rect',
    tag: 'div',
    strokeWidth: 3,
    shadow: true,
    sketch: false,
  },
)

defineOptions({ inheritAttrs: false })
const attrs = useAttrs()

const passthroughAttrs = computed(() => {
  const raw = attrs as Record<string, unknown>
  const { class: _c, style: _s, ...rest } = raw
  return rest
})

const pathD = computed(() => {
  if (props.shape === 'rect' && props.seed != null) {
    return resolveDoodleRectPath(props.seed)
  }
  return DOODLE_FRAME_PATHS[props.shape]
})
const viewBox = computed(() => DOODLE_FRAME_VIEWBOX[props.shape])
const frameStyle = computed(() => ({
  '--sdf-color': props.color,
  '--sdf-shadow': props.shadow ? '1' : '0',
}))
</script>

<style scoped lang="scss">
.sa-doodle-frame {
  position: relative;
  display: block;
  width: 100%;
  border: none;
  background: #fff;
  text-align: inherit;
  color: inherit;
  box-shadow: calc(var(--sdf-shadow) * 2px) calc(var(--sdf-shadow) * 4px)
    calc(var(--sdf-shadow) * 12px) rgb(15 23 42 / calc(var(--sdf-shadow) * 0.08));
}

.sa-doodle-frame--rect {
  border-radius: 16px;
}

.sa-doodle-frame--pill {
  border-radius: 999px;
}

.sa-doodle-frame__stroke {
  position: absolute;
  inset: -1px;
  z-index: 2;
  width: calc(100% + 2px);
  height: calc(100% + 2px);
  overflow: visible;
  pointer-events: none;
}

.sa-doodle-frame__body {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 100%;
}
</style>
