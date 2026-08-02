<template>
  <component
    :is="tag"
    v-bind="passthroughAttrs"
    class="sa-doodle-frame-v2"
    :class="[
      `sa-doodle-frame-v2--${shape}`,
      { 'sa-doodle-frame-v2__shadow': selected },
      attrs.class,
    ]"
    :style="frameStyle"
  >
    <div class="sa-doodle-frame-v2__body">
      <slot />
    </div>
  </component>
</template>

<script setup lang="ts">
import {computed, useAttrs} from 'vue'

const props = withDefaults(
  defineProps<{
    color?: string
    shape?: string
    tag?: string
    shadow?: boolean
    selected?: boolean
  }>(),
  {
    color: '#2563eb',
    shape: 'rect',
    tag: 'div',
    shadow: true,
  },
)

defineOptions({ inheritAttrs: false })
const attrs = useAttrs()

const passthroughAttrs = computed(() => {
  const raw = attrs as Record<string, unknown>
  const { class: _c, style: _s, ...rest } = raw
  return rest
})

const frameStyle = computed(() => ({
  '--sdf-color': props.color,
  '--sdf-shadow': props.shadow ? '1' : '0'
}))
</script>

<style scoped lang="scss">
.sa-doodle-frame-v2 {
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
.sa-doodle-frame-v2__shadow {
  box-shadow: 0 2px 6px rgb(0, 0, 0);
}

.sa-doodle-frame-v2--rect {
  border-radius: 16px;
}

.sa-doodle-frame-v2--pill {
  border-radius: 999px;
}

.sa-doodle-frame-v2__body {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 100%;
}
</style>
