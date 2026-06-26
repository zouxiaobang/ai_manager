<template>
  <el-popover
    class="deploy-hover-popover"
    :class="{ 'is-inline': inline }"
    :placement="placement"
    trigger="hover"
    :show-after="200"
    :hide-after="80"
    :width="width"
    popper-class="deploy-hover-popper"
  >
    <template #reference>
      <div class="deploy-hover-popover__ref">
        <slot />
      </div>
    </template>
    <slot name="content" />
  </el-popover>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    width?: number
    /** 紧凑模式：宽高随内容，不撑满父容器 */
    inline?: boolean
    placement?:
      | 'top'
      | 'top-start'
      | 'top-end'
      | 'bottom'
      | 'bottom-start'
      | 'bottom-end'
      | 'left'
      | 'right'
  }>(),
  { width: 400, inline: false, placement: 'bottom-start' },
)
</script>

<style scoped lang="scss">
.deploy-hover-popover {
  display: flex;
  flex: 1;
  min-width: 0;
  width: 100%;
  height: 100%;

  &.is-inline {
    display: inline-flex;
    flex: none;
    align-items: center;
    width: auto;
    height: auto;
    line-height: 1;

    .deploy-hover-popover__ref {
      display: inline-flex;
      flex: none;
      align-items: center;
      width: auto;
      height: auto;
      line-height: 1;
    }
  }
}

.deploy-hover-popover__ref {
  display: flex;
  flex: 1;
  min-width: 0;
  width: 100%;
  height: 100%;

  :deep(.deploy-arch-card) {
    width: 100%;
    cursor: help;
  }

  :deep(.deploy-overview-card) {
    cursor: help;
  }

  :deep(.deploy-arch-diagram__feedback-label) {
    cursor: help;
  }
}
</style>

<style lang="scss">
.deploy-hover-popper {
  padding: 12px 14px !important;
}
</style>
