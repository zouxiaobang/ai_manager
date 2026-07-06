<template>
  <div class="mobile-month-picker">
    <!-- Trigger -->
    <button
      class="mobile-month-picker__trigger"
      :class="{ 'mobile-month-picker__trigger--disabled': disabled }"
      :disabled="disabled"
      @click="visible = true"
      type="button"
    >
      <span class="mobile-month-picker__trigger-text">{{ modelValue || placeholder }}</span>
      <svg
        class="mobile-month-picker__arrow"
        :class="{ 'mobile-month-picker__arrow--up': visible }"
        viewBox="0 0 24 24" width="16" height="16"
        fill="none" stroke="#94a3b8" stroke-width="2.5"
        stroke-linecap="round" stroke-linejoin="round"
      >
        <polyline points="6 9 12 15 18 9" />
      </svg>
    </button>

    <!-- Bottom Sheet Popup -->
    <Teleport to="body">
      <Transition name="mobile-month-picker">
        <div v-if="visible" class="mobile-month-picker__overlay" @click.self="close">
          <div class="mobile-month-picker__panel">
            <!-- Header: Year Navigator -->
            <header class="mobile-month-picker__header">
              <button
                type="button"
                class="mobile-month-picker__year-btn"
                @click="prevYear"
                :disabled="loading"
              >
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#3b82f6" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="15 18 9 12 15 6" />
                </svg>
              </button>
              <button
                type="button"
                class="mobile-month-picker__year-label"
                @click="toggleYearPanel"
              >
                {{ viewYear }}
                <svg
                  class="mobile-month-picker__year-arrow"
                  :class="{ rotated: showYearPanel }"
                  viewBox="0 0 24 24" width="14" height="14"
                  fill="none" stroke="#475569" stroke-width="2.5"
                  stroke-linecap="round" stroke-linejoin="round"
                >
                  <polyline points="6 9 12 15 18 9" />
                </svg>
              </button>
              <button
                type="button"
                class="mobile-month-picker__year-btn"
                @click="nextYear"
                :disabled="loading"
              >
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#3b82f6" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="9 18 15 12 9 6" />
                </svg>
              </button>
              <button
                type="button"
                class="mobile-month-picker__close"
                @click="close"
                aria-label="关闭"
              >
                ✕
              </button>
            </header>

            <!-- Body: Month Grid / Year List -->
            <div class="mobile-month-picker__body">
              <!-- Month Grid -->
              <div v-if="!showYearPanel" class="mobile-month-picker__months">
                <button
                  v-for="m in months"
                  :key="m.value"
                  type="button"
                  class="mobile-month-picker__month"
                  :class="{
                    'mobile-month-picker__month--selected': isSelectedMonth(m.value),
                    'mobile-month-picker__month--current': isCurrentMonth(m.value),
                  }"
                  @click="selectMonth(m.value)"
                >
                  {{ m.label }}
                </button>
              </div>

              <!-- Year List -->
              <div v-else class="mobile-month-picker__years">
                <button
                  v-for="y in yearRange"
                  :key="y"
                  type="button"
                  class="mobile-month-picker__year"
                  :class="{
                    'mobile-month-picker__year--selected': y === viewYear,
                    'mobile-month-picker__year--current': y === currentYear,
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
  'change': [value: string]
}>()

// ─── State ─────────────────────────────
const visible = ref(false)
const showYearPanel = ref(false)
const loading = ref(false)

const currentYear = new Date().getFullYear()
const currentMonth = new Date().getMonth() + 1

// Parse current value or default to current year
const [initYear, initMonth] = props.modelValue
  ? props.modelValue.split('-').map(Number)
  : [currentYear, currentMonth]

const viewYear = ref(initYear || currentYear)

const months = [
  { label: '1月', value: 1 },
  { label: '2月', value: 2 },
  { label: '3月', value: 3 },
  { label: '4月', value: 4 },
  { label: '5月', value: 5 },
  { label: '6月', value: 6 },
  { label: '7月', value: 7 },
  { label: '8月', value: 8 },
  { label: '9月', value: 9 },
  { label: '10月', value: 10 },
  { label: '11月', value: 11 },
  { label: '12月', value: 12 },
]

// Year list: 50 years back, 10 years forward from current year
const yearRange = computed(() => {
  const years: number[] = []
  for (let y = currentYear - 50; y <= currentYear + 10; y++) {
    years.push(y)
  }
  return years
})

// ─── Methods ───────────────────────────
function prevYear() {
  viewYear.value--
}

function nextYear() {
  viewYear.value++
}

function toggleYearPanel() {
  showYearPanel.value = !showYearPanel.value
}

function selectYear(y: number) {
  viewYear.value = y
  showYearPanel.value = false
}

function selectMonth(m: number) {
  const monthStr = `${m}`.padStart(2, '0')
  const value = `${viewYear.value}-${monthStr}`
  emit('update:modelValue', value)
  emit('change', value)
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
/* ===== Trigger ===== */
.mobile-month-picker__trigger {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border: 2px solid #e2e8f0;
  border-radius: 999px;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.15s;
  font-family: 'ZCOOL KuaiLe', 'Alibaba PuHuiTi', 'PingFang SC', sans-serif;
  font-size: 14px;
  color: #1e293b;

  &:hover {
    border-color: #3b82f6;
  }

  &:active {
    transform: scale(0.97);
  }

  &--disabled {
    opacity: 0.5;
    cursor: not-allowed;

    &:hover {
      border-color: #e2e8f0;
    }

    &:active {
      transform: none;
    }
  }
}

.mobile-month-picker__trigger-text {
  min-width: 60px;
  text-align: center;
}

.mobile-month-picker__arrow {
  transition: transform 0.2s;

  &--up {
    transform: rotate(180deg);
  }
}

/* ===== Overlay ===== */
.mobile-month-picker__overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  background: rgba(15, 23, 42, 0.45);
}

/* ===== Panel ===== */
.mobile-month-picker__panel {
  width: 100%;
  max-height: 92dvh;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 20px 20px 0 0;
  overflow: hidden;
  font-family: 'ZCOOL KuaiLe', 'Alibaba PuHuiTi', 'PingFang SC', sans-serif;
  color: #1e293b;
}

/* ===== Header ===== */
.mobile-month-picker__header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 16px 12px;
  border-bottom: 2px dashed #e2e8f0;
  flex-shrink: 0;
}

.mobile-month-picker__year-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: #f1f5f9;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s;
  flex-shrink: 0;

  &:hover {
    background: #e2e8f0;
  }

  &:active {
    transform: scale(0.92);
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }
}

.mobile-month-picker__year-label {
  flex: 1;
  text-align: center;
  border: none;
  background: transparent;
  font-family: inherit;
  font-size: 18px;
  font-weight: 800;
  color: #1e293b;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;

  &:hover {
    background: #f1f5f9;
  }

  &:active {
    transform: scale(0.97);
  }
}

.mobile-month-picker__year-arrow {
  transition: transform 0.2s;

  &.rotated {
    transform: rotate(180deg);
  }
}

.mobile-month-picker__close {
  width: 32px;
  height: 32px;
  border: none;
  background: #f1f5f9;
  border-radius: 50%;
  font-size: 16px;
  color: #64748b;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &:active {
    transform: scale(0.92);
  }
}

/* ===== Body ===== */
.mobile-month-picker__body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 16px max(20px, env(safe-area-inset-bottom));
}

/* ===== Month Grid ===== */
.mobile-month-picker__months {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.mobile-month-picker__month {
  padding: 14px 8px;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  background: #fff;
  font-family: inherit;
  font-size: 15px;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
  text-align: center;

  &:hover {
    border-color: #3b82f6;
    color: #3b82f6;
  }

  &:active {
    transform: scale(0.95);
  }

  &--selected {
    border-color: #3b82f6;
    background: #eff6ff;
    color: #3b82f6;
    font-weight: 700;
  }

  &--current {
    position: relative;

    &::after {
      content: '';
      position: absolute;
      top: 6px;
      right: 6px;
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: #3b82f6;
    }
  }
}

/* ===== Year List ===== */
.mobile-month-picker__years {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.mobile-month-picker__year {
  padding: 12px 8px;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  background: #fff;
  font-family: inherit;
  font-size: 14px;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
  text-align: center;

  &:hover {
    border-color: #3b82f6;
    color: #3b82f6;
  }

  &:active {
    transform: scale(0.95);
  }

  &--selected {
    border-color: #3b82f6;
    background: #eff6ff;
    color: #3b82f6;
    font-weight: 700;
  }

  &--current {
    color: #3b82f6;
  }
}

/* ===== Transition ===== */
.mobile-month-picker-enter-active,
.mobile-month-picker-leave-active {
  transition: opacity 0.2s ease;

  .mobile-month-picker__panel {
    transition: transform 0.25s ease;
  }
}

.mobile-month-picker-enter-from,
.mobile-month-picker-leave-to {
  opacity: 0;

  .mobile-month-picker__panel {
    transform: translateY(100%);
  }
}
</style>
