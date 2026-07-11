<template>
  <div
    class="phase-card"
    :class="{
      'phase-card--current': isCurrent,
      'phase-card--all-done': allDone,
      'phase-card--collapsed': collapsed,
    }"
    :style="{ '--accent': phase.accent }"
  >
    <button class="phase-card__header" type="button" @click="$emit('toggle-collapse')">
      <div class="phase-card__header-left">
        <span class="phase-card__badge">{{ phase.badge }}</span>
        <div class="phase-card__titles">
          <h3 class="phase-card__title">{{ phase.title }}</h3>
          <p v-if="phase.desc" class="phase-card__desc">{{ phase.desc }}</p>
        </div>
      </div>
      <div class="phase-card__header-right">
        <span class="phase-card__count">{{ completedCount }}/{{ phase.items.length }}</span>
        <span class="phase-card__arrow" :class="{ expanded: !collapsed }">›</span>
      </div>
    </button>

    <Transition name="phase-collapse">
      <div v-if="!collapsed" class="phase-card__body">
        <slot name="item" v-for="item in phase.items" :item="item" :key="item.key" />
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import type { PhaseDef } from '@/data/24hour-phases'

defineProps<{
  phase: PhaseDef
  isCurrent: boolean
  allDone: boolean
  completedCount: number
  collapsed: boolean
}>()

defineEmits<{
  'toggle-collapse': []
}>()
</script>

<style scoped lang="scss">
.phase-card {
  background: #fff;
  border-radius: 16px;
  border: 2px solid #e2e8f0;
  overflow: hidden;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;

  &--current {
    border-color: var(--accent);
    box-shadow: 0 4px 14px color-mix(in srgb, var(--accent) 20%, transparent);

    .phase-card__badge {
      background: color-mix(in srgb, var(--accent) 15%, transparent);
      color: var(--accent);
    }
  }

  &--all-done {
    opacity: 0.7;

    .phase-card__title {
      text-decoration: line-through;
      color: #94a3b8;
    }
  }

  &--collapsed .phase-card__body {
    display: none;
  }
}

.phase-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 14px;
  width: 100%;
  background: transparent;
  border: none;
  cursor: pointer;
  text-align: left;
  font-family: inherit;
  transition: background 0.15s ease;

  &:active {
    background: #f8fafc;
  }
}

.phase-card__header-left {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.phase-card__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 3px 10px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 700;
  font-family: 'ZCOOL KuaiLe', sans-serif;
  background: #f1f5f9;
  color: #64748b;
  flex-shrink: 0;
}

.phase-card__titles {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1;
}

.phase-card__title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
  line-height: 1.3;
}

.phase-card__desc {
  font-size: 12px;
  color: #94a3b8;
  margin: 0;
  line-height: 1.4;
}

.phase-card__header-right {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.phase-card__count {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 999px;
}

.phase-card__arrow {
  font-size: 20px;
  color: #94a3b8;
  transition: transform 0.2s ease;
  line-height: 1;

  &.expanded {
    transform: rotate(90deg);
  }
}

.phase-card__body {
  padding: 4px 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.phase-collapse-enter-active,
.phase-collapse-leave-active {
  transition: all 0.25s ease;
  overflow: hidden;
}

.phase-collapse-enter-from,
.phase-collapse-leave-to {
  opacity: 0;
  max-height: 0;
  padding-top: 0;
  padding-bottom: 0;
}

.phase-collapse-enter-to,
.phase-collapse-leave-from {
  max-height: 500px;
}
</style>
