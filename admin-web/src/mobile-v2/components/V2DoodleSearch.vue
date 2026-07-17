<template>
  <div class="v2-doodle-search" :class="rootClass">
    <div class="v2-doodle-search__inner">
      <svg class="v2-doodle-search__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="11" cy="11" r="8" />
        <line x1="21" y1="21" x2="16.65" y2="16.65" />
      </svg>
      <input
        :value="modelValue"
        @input="onInput"
        type="search"
        enterkeyhint="search"
        :placeholder="placeholder"
        class="v2-doodle-search__input"
      />
      <button
        v-if="modelValue.trim()"
        type="button"
        class="v2-doodle-search__clear"
        @click="onClear"
      >
        <span>×</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    modelValue: string
    placeholder?: string
    color?: string
  }>(),
  {
    placeholder: '搜索...',
    color: '#2563eb',
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

function onInput(e: Event) {
  emit('update:modelValue', (e.target as HTMLInputElement).value)
}

function onClear() {
  emit('update:modelValue', '')
}

const rootClass = {
  [`v2-doodle-search--${props.color.replace('#', '')}`]: true,
}
</script>

<style scoped lang="scss">
.v2-doodle-search {
  margin-bottom: 8px;
  border-radius: 100px;
  background: #f1f5f9;
  overflow: hidden;
}

.v2-doodle-search__inner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
}

.v2-doodle-search__icon {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
  color: #94a3b8;
}

.v2-doodle-search__input {
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

.v2-doodle-search__clear {
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
