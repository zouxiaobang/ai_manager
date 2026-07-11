<template>
  <nav class="pomo-pixel-nav">
    <div class="pomo-pixel-nav__items">
      <button
        v-for="item in items"
        :key="item.id"
        type="button"
        class="pomo-pixel-nav__item"
        :class="{ 'is-active': modelValue === item.id }"
        @click="emit('update:modelValue', item.id)"
      >
        <span class="pomo-pixel-nav__icon" aria-hidden="true">{{ item.icon }}</span>
        <span class="pomo-pixel-nav__label">{{ item.label }}</span>
      </button>
    </div>
  </nav>
</template>

<script setup lang="ts">
export type PomoNavTabId = string

export interface PomoNavItem {
  id: string
  icon: string
  label: string
}

defineProps<{
  modelValue: string
  items: PomoNavItem[]
}>()

const emit = defineEmits<{
  'update:modelValue': [id: string]
}>()
</script>
