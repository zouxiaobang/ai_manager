<template>
  <div class="platform-tabs">
    <SchemeADoodleFrame
      v-for="tab in tabs"
      :key="tab.id"
      tag="button"
      type="button"
      shape="pill"
      :color="activeTab === tab.id ? '#2563eb' : '#cbd5e1'"
      :sketch="activeTab === tab.id"
      class="platform-tab"
      :class="{ active: activeTab === tab.id }"
      @click="$emit('change', tab.id)"
    >
      <span class="platform-tab__icon">{{ tab.icon }}</span>
      <span class="platform-tab__name">{{ tab.name }}</span>
    </SchemeADoodleFrame>
  </div>
</template>

<script setup lang="ts">
import SchemeADoodleFrame from '@/mobile/views/home/themes/scheme-a/SchemeADoodleFrame.vue'

interface ExpressTab {
  id: string
  name: string
  icon: string
}

defineProps<{
  tabs: ExpressTab[]
  activeTab: string
}>()

defineEmits<{
  change: [tabId: string]
}>()
</script>

<style scoped lang="scss">
.platform-tabs {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  overflow-y: hidden;
  padding: 0 16px 8px;
  margin-bottom: 12px;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  -webkit-overflow-scrolling: touch;

  &::-webkit-scrollbar {
    display: none;
  }
}

.platform-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0;
  background: transparent;
  border: none;
  white-space: nowrap;
  cursor: pointer;
  transition: transform 0.15s;

  :deep(.sa-doodle-frame) {
    overflow: hidden;
  }

  :deep(.sa-doodle-frame__body) {
    padding: 8px 14px;
    background: #fff;
  }

  :deep(.sa-doodle-frame__stroke) {
    inset: 0 !important;
    width: 100% !important;
    height: 100% !important;
    overflow: hidden;
  }

  &.active {
    :deep(.sa-doodle-frame__body) {
      background: #fff;
    }

    .platform-tab__name {
      color: #dc2626;
      font-weight: 800;
    }

    .platform-tab__icon {
      filter: none;
    }
  }

  &:active {
    transform: scale(0.96);
  }

  &__icon {
    font-size: 16px;
    flex-shrink: 0;
  }

  &__name {
    font-size: 13px;
    font-weight: 600;
    color: #64748b;
    font-family: 'ZCOOL KuaiLe', 'Alibaba PuHuiTi', 'PingFang SC', sans-serif;
  }
}
</style>
