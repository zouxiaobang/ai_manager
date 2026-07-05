<template>
  <SchemeADoodleFrame
    shape="pill"
    :color="color"
    sketch
    class="mobile-doodle-search"
    :class="classList"
  >
    <div class="mobile-doodle-search__inner">
      <img class="mobile-doodle-search__icon" :src="schemeAAssets.search" alt="" />
      <input
        :value="modelValue"
        @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
        type="search"
        enterkeyhint="search"
        :placeholder="placeholder"
        class="mobile-doodle-search__input"
      />
      <button
        v-if="modelValue.trim()"
        type="button"
        class="mobile-doodle-search__clear"
        @click="$emit('update:modelValue', '')"
      >
        <span>×</span>
      </button>
    </div>
  </SchemeADoodleFrame>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import SchemeADoodleFrame from '@/mobile/views/home/themes/scheme-a/SchemeADoodleFrame.vue'
import { schemeAAssets } from '@/mobile/views/home/themes/scheme-a/assets'

const props = withDefaults(
  defineProps<{
    modelValue: string
    placeholder?: string
    color?: string
    class?: string
  }>(),
  {
    placeholder: '搜索...',
    color: '#2563eb',
  },
)

defineEmits<{
  'update:modelValue': [value: string]
}>()

const classList = computed(() => ({
  [props.class as string]: !!props.class,
}))
</script>

<style scoped lang="scss">
.mobile-doodle-search {
  margin-bottom: 8px;

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.mobile-doodle-search__inner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
}

.mobile-doodle-search__icon {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
}

.mobile-doodle-search__input {
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

  &::-webkit-search-cancel-button {
    display: none;
  }

  &::-moz-search-cancel-button {
    display: none;
  }
}

.mobile-doodle-search__clear {
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
