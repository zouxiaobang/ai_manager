<template>
  <div class="v2-month-picker-bar">
    <button type="button" class="v2-month-picker-bar__arrow" :disabled="disabled" @click="shiftMonth(-1)">
      <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="15 18 9 12 15 6" />
      </svg>
    </button>

    <button type="button" class="v2-month-picker-bar__trigger" :disabled="disabled" @click="visible = true">
      <span class="v2-month-picker-bar__trigger-text">{{ displayValue }}</span>
      <svg class="v2-month-picker-bar__trigger-arrow" viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="6 9 12 15 18 9" />
      </svg>
    </button>

    <button type="button" class="v2-month-picker-bar__arrow" :disabled="disabled" @click="shiftMonth(1)">
      <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="9 18 15 12 9 6" />
      </svg>
    </button>

    <Teleport to="body">
      <Transition name="v2-month-picker">
        <div v-if="visible" class="v2-month-picker__overlay" @click.self="close">
          <div class="v2-month-picker__panel">
            <div class="v2-month-picker__header">
              <button type="button" class="v2-month-picker__nav-btn" @click="prevYear">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="15 18 9 12 15 6" />
                </svg>
              </button>
              <button type="button" class="v2-month-picker__year-label" @click="toggleYearPanel">
                {{ viewYear }}
                <svg
                  class="v2-month-picker__year-arrow"
                  :class="{ rotated: showYearPanel }"
                  viewBox="0 0 24 24" width="12" height="12"
                  fill="none" stroke="currentColor" stroke-width="2.5"
                  stroke-linecap="round" stroke-linejoin="round"
                >
                  <polyline points="6 9 12 15 18 9" />
                </svg>
              </button>
              <button type="button" class="v2-month-picker__nav-btn" @click="nextYear">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="9 18 15 12 9 6" />
                </svg>
              </button>
              <button type="button" class="v2-month-picker__close" @click="close" aria-label="关闭">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
                </svg>
              </button>
            </div>

            <div class="v2-month-picker__body">
              <div v-if="!showYearPanel" class="v2-month-picker__months">
                <button
                  v-for="m in months"
                  :key="m.value"
                  type="button"
                  class="v2-month-picker__month"
                  :class="{
                    'is-selected': isSelectedMonth(m.value),
                    'is-current': isCurrentMonth(m.value),
                  }"
                  @click="selectMonth(m.value)"
                >
                  {{ m.label }}
                </button>
              </div>

              <div v-else class="v2-month-picker__years">
                <button
                  v-for="y in yearRange"
                  :key="y"
                  type="button"
                  class="v2-month-picker__year"
                  :class="{
                    'is-selected': y === viewYear,
                    'is-current': y === currentYear,
                  }"
                  @click="selectYear(y)"
                >
                  {{ y }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

const props = withDefaults(defineProps<{
  modelValue?: string
  placeholder?: string
  disabled?: boolean
}>(), {
  modelValue: '',
  placeholder: '选择月份',
  disabled: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const visible = ref(false)
const showYearPanel = ref(false)

const currentYear = new Date().getFullYear()
const currentMonth = new Date().getMonth() + 1

const [initYear] = props.modelValue
  ? props.modelValue.split('-').map(Number)
  : [currentYear, currentMonth]

const viewYear = ref(initYear || currentYear)

const displayValue = computed(() => {
  if (!props.modelValue) return props.placeholder
  const [y, m] = props.modelValue.split('-')
  return `${y}年${parseInt(m)}月`
})

const months = [
  { label: '1月', value: 1 }, { label: '2月', value: 2 }, { label: '3月', value: 3 },
  { label: '4月', value: 4 }, { label: '5月', value: 5 }, { label: '6月', value: 6 },
  { label: '7月', value: 7 }, { label: '8月', value: 8 }, { label: '9月', value: 9 },
  { label: '10月', value: 10 }, { label: '11月', value: 11 }, { label: '12月', value: 12 },
]

const yearRange = computed(() => {
  const years: number[] = []
  for (let y = currentYear - 50; y <= currentYear + 10; y++) {
    years.push(y)
  }
  return years
})

function shiftMonth(delta: number) {
  if (props.disabled) return
  if (!props.modelValue) return
  const [y, m] = props.modelValue.split('-').map(Number)
  const d = new Date(y, m - 1 + delta, 1)
  const yy = d.getFullYear()
  const mm = `${d.getMonth() + 1}`.padStart(2, '0')
  viewYear.value = yy
  emit('update:modelValue', `${yy}-${mm}`)
}

function prevYear() { viewYear.value-- }
function nextYear() { viewYear.value++ }
function toggleYearPanel() { showYearPanel.value = !showYearPanel.value }

function selectYear(y: number) {
  viewYear.value = y
  showYearPanel.value = false
}

function selectMonth(m: number) {
  const value = `${viewYear.value}-${String(m).padStart(2, '0')}`
  emit('update:modelValue', value)
  visible.value = false
}

function isSelectedMonth(m: number): boolean {
  if (!props.modelValue) return false
  const [y, mon] = props.modelValue.split('-').map(Number)
  return y === viewYear.value && mon === m
}

function isCurrentMonth(m: number): boolean {
  return viewYear.value === currentYear && m === currentMonth
}

function close() {
  visible.value = false
  showYearPanel.value = false
}
</script>

<style scoped lang="scss">
.v2-month-picker-bar {
  display: flex;
  align-items: center;
  gap: 12px;

  &__arrow {
    width: 36px;
    height: 36px;
    border: 1px solid var(--wr-border, #e8ecef);
    border-radius: 10px;
    background: var(--wr-card, #fff);
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: background 0.2s, box-shadow 0.2s, color 0.2s, border-color 0.2s;
    color: var(--wr-text-secondary, #666);
    flex-shrink: 0;

    &:hover {
      border-color: var(--ec-stat-blue, #2563eb);
      color: var(--ec-stat-blue, #2563eb);
      box-shadow: 0 2px 6px rgba(37, 99, 235, 0.12);
    }

    &:active {
      background: #eff6ff;
      transform: scale(0.92);
    }

    &:disabled {
      opacity: 0.3;
      cursor: not-allowed;

      &:hover {
        border-color: var(--wr-border, #e8ecef);
        color: var(--wr-text-secondary, #666);
        box-shadow: none;
      }
    }
  }

  &__trigger {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 6px 14px;
    border: 1px solid var(--wr-border, #e8ecef);
    border-radius: 8px;
    background: var(--wr-card, #fff);
    cursor: pointer;
    transition: border-color 0.2s, background 0.2s;
    font-family: inherit;
    font-size: 14px;
    color: var(--wr-text, #333);

    &:hover {
      border-color: var(--ec-stat-blue, #2563eb);
    }

    &:active {
      background: #f3f4f6;
    }

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }

  &__trigger-text {
    min-width: 64px;
    text-align: center;
    font-weight: 600;
  }

  &__trigger-arrow {
    color: var(--wr-muted, #999);
    transition: transform 0.2s;
  }
}

.v2-month-picker__overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  background: rgba(15, 23, 42, 0.45);
}

.v2-month-picker__panel {
  width: 100%;
  max-width: 500px;
  margin: 0 auto;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  background: var(--wr-card, #fff);
  border-radius: 20px 20px 0 0;
  overflow: hidden;
  color: var(--wr-text, #333);
}

.v2-month-picker__header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 16px 12px;
  border-bottom: 1px solid var(--wr-border, #e8ecef);
  flex-shrink: 0;
}

.v2-month-picker__nav-btn {
  width: 34px;
  height: 34px;
  border: 1px solid var(--wr-border, #e8ecef);
  background: var(--wr-card, #fff);
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--wr-text, #333);
  transition: background 0.2s;
  flex-shrink: 0;

  &:active {
    background: #f3f4f6;
  }
}

.v2-month-picker__year-label {
  flex: 1;
  text-align: center;
  border: none;
  background: transparent;
  font-family: inherit;
  font-size: 17px;
  font-weight: 700;
  color: var(--wr-text, #333);
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;

  &:hover {
    background: #f3f4f6;
  }
}

.v2-month-picker__year-arrow {
  color: var(--wr-muted, #999);
  transition: transform 0.2s;

  &.rotated {
    transform: rotate(180deg);
  }
}

.v2-month-picker__close {
  width: 32px;
  height: 32px;
  border: 1px solid var(--wr-border, #e8ecef);
  background: var(--wr-card, #fff);
  border-radius: 8px;
  color: var(--wr-muted, #999);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background 0.2s;

  &:active {
    background: #f3f4f6;
  }
}

.v2-month-picker__body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 16px max(24px, env(safe-area-inset-bottom));
}

.v2-month-picker__months {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.v2-month-picker__month {
  padding: 14px 8px;
  border: 1px solid var(--wr-border, #e8ecef);
  border-radius: 10px;
  background: var(--wr-card, #fff);
  font-family: inherit;
  font-size: 15px;
  font-weight: 500;
  color: var(--wr-text-secondary, #666);
  cursor: pointer;
  transition: all 0.15s;
  text-align: center;

  &:active {
    transform: scale(0.95);
  }

  &.is-selected {
    border-color: var(--ec-stat-blue, #2563eb);
    background: #eff6ff;
    color: var(--ec-stat-blue, #2563eb);
    font-weight: 700;
  }

  &.is-current {
    position: relative;

    &::after {
      content: '';
      position: absolute;
      top: 4px;
      right: 4px;
      width: 5px;
      height: 5px;
      border-radius: 50%;
      background: var(--ec-stat-blue, #2563eb);
    }
  }
}

.v2-month-picker__years {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.v2-month-picker__year {
  padding: 12px 8px;
  border: 1px solid var(--wr-border, #e8ecef);
  border-radius: 8px;
  background: var(--wr-card, #fff);
  font-family: inherit;
  font-size: 14px;
  font-weight: 500;
  color: var(--wr-text-secondary, #666);
  cursor: pointer;
  transition: all 0.15s;
  text-align: center;

  &:active {
    transform: scale(0.95);
  }

  &.is-selected {
    border-color: var(--ec-stat-blue, #2563eb);
    background: #eff6ff;
    color: var(--ec-stat-blue, #2563eb);
    font-weight: 700;
  }

  &.is-current {
    color: var(--ec-stat-blue, #2563eb);
    font-weight: 600;
  }
}

.v2-month-picker-enter-active,
.v2-month-picker-leave-active {
  transition: opacity 0.2s ease;

  .v2-month-picker__panel {
    transition: transform 0.25s ease;
  }
}

.v2-month-picker-enter-from,
.v2-month-picker-leave-to {
  opacity: 0;

  .v2-month-picker__panel {
    transform: translateY(100%);
  }
}
</style>
