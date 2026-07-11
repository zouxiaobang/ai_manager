<template>
  <!-- 移动端页面头部组件：包含返回按钮、标题、右侧操作区 -->
  <div class="mobile-page-header" :class="classList">
    <!-- 左侧区域：返回按钮 + 标题 -->
    <div class="mobile-page-header__left">
      <!-- 返回按钮（手绘风格胶囊形状） -->
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
      <!-- 左侧插槽（默认显示标题） -->
      <slot name="left">
        <h1 class="mobile-page-header__title">{{ title }}</h1>
      </slot>
    </div>
    <!-- 右侧操作区插槽 -->
    <div class="mobile-page-header__right">
      <slot name="right" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import MobileDoodleChip from './MobileDoodleChip.vue'

/**
 * 组件属性定义
 * @property title - 页面标题文字
 * @property showBack - 是否显示返回按钮，默认显示
 * @property backIcon - 返回按钮图标，默认左箭头
 * @property class - 自定义类名
 */
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

/**
 * 组件事件定义
 * @event back - 点击返回按钮时触发
 */
const emit = defineEmits<{
  back: []
}>()

/** 合并的类名列表 */
const classList = computed(() => ({
  [props.class as string]: !!props.class,
}))

/**
 * 处理返回按钮点击
 * 触发 back 事件，由父组件处理具体的返回逻辑
 */
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
