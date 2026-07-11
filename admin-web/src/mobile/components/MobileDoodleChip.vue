<template>
  <!-- 移动端手绘风格胶囊标签组件：基于SchemeADoodleFrame封装 -->
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
import SchemeADoodleFrame from '@/mobile/views/home/themes/scheme-a/SchemeADoodleFrame.vue'
import type { DoodleFrameShape } from '@/mobile/views/home/themes/scheme-a/doodlePaths'

/**
 * 组件属性定义
 * @property tag - 渲染的HTML标签，默认span
 * @property shape - 手绘边框形状 pill(胶囊)/rect(矩形)/round(圆角)
 * @property color - 边框颜色，默认灰色
 * @property seed - 随机种子（用于生成手绘不规则形状）
 * @property strokeWidth - 边框线宽
 * @property inline - 是否行内模式（不占满父宽），默认true
 * @property filled - 是否实心填充（选中状态）
 * @property fillColor - 填充颜色（filled为true时生效，默认使用color）
 */
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

// 关闭属性继承，手动控制属性透传
defineOptions({ inheritAttrs: false })
const attrs = useAttrs()

/** 透传给子组件的额外属性（排除class和style） */
const extraAttrs = computed(() => {
  const raw = attrs as Record<string, unknown>
  const { class: _c, style: _s, ...rest } = raw
  return rest
})

/** 胶囊样式：实心填充时设置CSS变量 */
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
