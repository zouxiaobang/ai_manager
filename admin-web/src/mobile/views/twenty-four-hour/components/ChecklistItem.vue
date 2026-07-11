<template>
  <div
    class="checklist-item"
    :class="{ 'checklist-item--done': checked }"
    :style="{ '--accent': accent }"
  >
    <button
      type="button"
      class="checklist-item__checkbox"
      :class="{ 'checklist-item__checkbox--checked': checked }"
      @click="$emit('toggle')"
    >
      <svg v-if="checked" viewBox="0 0 24 24" width="14" height="14">
        <path
          d="M5 13l4 4L19 7"
          stroke="currentColor"
          stroke-width="2.8"
          fill="none"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
      </svg>
    </button>

    <span class="checklist-item__label" @click="$emit('toggle')">
      {{ item.label }}
    </span>

    <span v-if="item.hasContent && content" class="checklist-item__preview" @click.stop="$emit('open-write')">
      「{{ contentPreview }}」
    </span>

    <button
      v-if="item.hasContent"
      type="button"
      class="checklist-item__write"
      :class="{ 'checklist-item__write--has': !!content }"
      @click.stop="$emit('open-write')"
    >
      <svg viewBox="0 0 24 24" width="16" height="16">
        <path
          d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"
          stroke="currentColor"
          stroke-width="1.8"
          fill="none"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
        <path
          d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"
          stroke="currentColor"
          stroke-width="1.8"
          fill="none"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
      </svg>
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ItemDef } from '@/data/24hour-phases'

const props = defineProps<{
  item: ItemDef
  checked: boolean
  content: string
  accent: string
}>()

defineEmits<{
  toggle: []
  'open-write': []
}>()

const contentPreview = computed(() => {
  if (!props.content) return ''
  const text = props.content.trim()
  if (text.length <= 8) return text
  return text.slice(0, 8) + '…'
})
</script>

<style scoped lang="scss">
.checklist-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 6px;
  border-radius: 10px;
  transition: background 0.15s ease;
  cursor: pointer;

  &:hover {
    background: #f8fafc;
  }

  &--done {
    opacity: 0.55;

    .checklist-item__label {
      text-decoration: line-through;
      color: #94a3b8;
    }
  }
}

.checklist-item__checkbox {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
  border-radius: 6px;
  border: 2px solid #cbd5e1;
  background: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  padding: 0;
  transition: all 0.15s ease;
  color: #fff;

  &:hover {
    border-color: var(--accent);
  }

  &--checked {
    background: var(--accent);
    border-color: var(--accent);
  }
}

.checklist-item__label {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  color: #1e293b;
  line-height: 1.4;
}

.checklist-item__preview {
  font-size: 12px;
  color: #64748b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 80px;
  cursor: pointer;
  flex-shrink: 0;
  transition: color 0.15s ease;

  &:hover {
    color: var(--accent);
  }
}

.checklist-item__write {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  padding: 0;
  transition: all 0.15s ease;

  &:hover {
    background: color-mix(in srgb, var(--accent) 10%, transparent);
    color: var(--accent);
  }

  &--has {
    color: var(--accent);
  }
}
</style>
