<template>
  <div
    class="v2-phase-card"
    :class="{
      'v2-phase-card--current': isCurrent,
      'v2-phase-card--all-done': allDone,
      'v2-phase-card--collapsed': collapsed,
    }"
    :style="{ '--accent': phase.accent }"
  >
    <button class="v2-phase-card__header" type="button" @click="$emit('toggle-collapse')">
      <div class="v2-phase-card__header-left">
        <span class="v2-phase-card__badge">{{ phase.badge }}</span>
        <div class="v2-phase-card__titles">
          <h3 class="v2-phase-card__title">{{ phase.title }}</h3>
          <p v-if="phase.desc" class="v2-phase-card__desc">{{ phase.desc }}</p>
        </div>
      </div>
      <div class="v2-phase-card__header-right">
        <div class="v2-phase-card__ring">
          <svg viewBox="0 0 40 40" width="36" height="36">
            <circle cx="20" cy="20" r="17" fill="none" stroke="#e5e7eb" stroke-width="3" />
            <circle
              cx="20" cy="20" r="17"
              fill="none"
              :stroke="phase.accent"
              stroke-width="3"
              stroke-linecap="round"
              :stroke-dasharray="circumference"
              :stroke-dashoffset="ringOffset"
              transform="rotate(-90 20 20)"
            />
          </svg>
          <span class="v2-phase-card__ring-text">{{ completedCount }}</span>
        </div>
        <span class="v2-phase-card__arrow" :class="{ 'v2-phase-card__arrow--open': !collapsed }">
          <svg viewBox="0 0 24 24" width="20" height="20">
            <path d="M9 18l6-6-6-6" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </span>
      </div>
    </button>

    <Transition name="v2-phase-collapse">
      <div v-if="!collapsed" class="v2-phase-card__body">
        <div class="v2-phase-card__items">
          <slot name="item" v-for="item in phase.items" :item="item" :key="item.key" />
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { PhaseDef } from '@/data/24hour-phases'

const props = defineProps<{
  phase: PhaseDef
  isCurrent: boolean
  allDone: boolean
  completedCount: number
  collapsed: boolean
}>()

defineEmits<{
  'toggle-collapse': []
}>()

const circumference = 2 * Math.PI * 17

const ringOffset = computed(() => {
  const total = props.phase.items.length
  if (total === 0) return circumference
  const done = props.completedCount
  return circumference - (done / total) * circumference
})
</script>

<style scoped lang="scss">
.v2-phase-card {
  background: #fff;
  border-radius: 14px;
  border: 1.5px solid #e8ecef;
  overflow: hidden;
  transition: border-color 0.25s ease, box-shadow 0.25s ease;

  &--current {
    border-color: var(--accent);
    box-shadow: 0 0 0 1px var(--accent), 0 6px 20px color-mix(in srgb, var(--accent) 18%, transparent);

    .v2-phase-card__badge {
      background: color-mix(in srgb, var(--accent) 12%, transparent);
      color: var(--accent);
      font-weight: 600;
    }

    .v2-phase-card__title {
      color: var(--accent);
    }
  }

  &--all-done {
    opacity: 0.55;

    .v2-phase-card__title {
      text-decoration: line-through;
      color: #94a3b8;
    }
  }

  + .v2-phase-card {
    margin-top: 10px;
  }
}

.v2-phase-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 14px 16px;
  width: 100%;
  background: transparent;
  border: none;
  cursor: pointer;
  text-align: left;
  font-family: inherit;

  &:active {
    background: #f8fafc;
  }
}

.v2-phase-card__header-left {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.v2-phase-card__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 2px 9px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  background: #f1f5f9;
  color: #64748b;
  flex-shrink: 0;
  line-height: 1.6;
}

.v2-phase-card__titles {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1;
}

.v2-phase-card__title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
  line-height: 1.3;
}

.v2-phase-card__desc {
  font-size: 12px;
  color: #94a3b8;
  margin: 0;
  line-height: 1.4;
}

.v2-phase-card__header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.v2-phase-card__ring {
  position: relative;
  width: 36px;
  height: 36px;
  flex-shrink: 0;

  svg {
    display: block;
  }
}

.v2-phase-card__ring-text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: #1e293b;
}

.v2-phase-card__arrow {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  transition: transform 0.2s ease;

  &--open {
    transform: rotate(90deg);
    color: var(--accent);
  }
}

.v2-phase-card__body {
  border-top: 1px solid #f1f5f9;
}

.v2-phase-card__items {
  padding: 4px 8px 12px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.v2-phase-collapse-enter-active,
.v2-phase-collapse-leave-active {
  transition: all 0.25s ease;
  overflow: hidden;
}

.v2-phase-collapse-enter-from,
.v2-phase-collapse-leave-to {
  opacity: 0;
  max-height: 0;
  padding-top: 0;
  padding-bottom: 0;
  border-top-width: 0;
}

.v2-phase-collapse-enter-to,
.v2-phase-collapse-leave-from {
  max-height: 600px;
}
</style>
