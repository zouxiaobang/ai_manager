<template>
  <SchemeADoodleFrame shape="pill" color="#2563eb" sketch class="carton-search-bar">
    <div class="carton-search-bar__inner">
      <img class="carton-search-bar__icon" :src="schemeAAssets.search" alt="" />
      <input
        :value="modelValue"
        @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
        type="search"
        enterkeyhint="search"
        placeholder="搜索纸箱..."
        class="carton-search-bar__input"
      />
      <button
        v-if="modelValue.trim()"
        type="button"
        class="carton-search-bar__clear"
        @click="$emit('update:modelValue', '')"
      >
        <span>×</span>
      </button>
    </div>
  </SchemeADoodleFrame>
</template>

<script setup lang="ts">
import SchemeADoodleFrame from '@/mobile/home/themes/scheme-a/SchemeADoodleFrame.vue'
import { schemeAAssets } from '@/mobile/home/themes/scheme-a/assets'

defineProps<{
  modelValue: string
}>()

defineEmits<{
  'update:modelValue': [value: string]
}>()
</script>

<style scoped lang="scss">
.carton-search-bar {
  margin-bottom: 8px;

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.carton-search-bar__inner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
}

.carton-search-bar__icon {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
}

.carton-search-bar__input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-family: inherit;
  font-size: 15px;
  color: #1e293b;

  &::placeholder {
    color: #94a3b8;
  }
}

.carton-search-bar__clear {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  border: none;
  background: #e2e8f0;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;

  &:active {
    transform: scale(0.9);
  }

  span {
    font-size: 16px;
    color: #64748b;
    font-weight: 700;
    line-height: 1;
  }
}
</style>