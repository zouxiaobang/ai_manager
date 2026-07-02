<template>
  <Teleport to="body">
    <Transition name="carton-calc-sheet">
      <div v-if="modelValue" class="carton-calc-sheet" @click.self="close">
        <div class="carton-calc-sheet__panel" role="dialog" aria-modal="true">
          <header class="carton-calc-sheet__header">
            <div>
              <h2 class="carton-calc-sheet__title">{{ t('ecommerce.carton.calculateTitle') }}</h2>
              <p class="carton-calc-sheet__subtitle">{{ t('ecommerce.carton.calculateSubtitle') }}</p>
            </div>
            <button type="button" class="carton-calc-sheet__close" aria-label="关闭" @click="close">×</button>
          </header>

          <div class="carton-calc-sheet__body">
            <form class="carton-calc-sheet__form" @submit.prevent="runCalculate">
              <section class="carton-calc-sheet__section">
                <h3 class="carton-calc-sheet__section-title">{{ t('ecommerce.carton.productSizeCm') }}</h3>
                <div class="carton-calc-sheet__field-grid">
                  <label class="carton-calc-sheet__field">
                    <span>{{ t('ecommerce.carton.productLength') }}</span>
                    <input
                      v-model.number="calcForm.lengthCm"
                      type="number"
                      inputmode="decimal"
                      min="0"
                      step="0.01"
                      placeholder="0"
                    />
                  </label>
                  <label class="carton-calc-sheet__field">
                    <span>{{ t('ecommerce.carton.productWidth') }}</span>
                    <input
                      v-model.number="calcForm.widthCm"
                      type="number"
                      inputmode="decimal"
                      min="0"
                      step="0.01"
                      placeholder="0"
                    />
                  </label>
                  <label class="carton-calc-sheet__field">
                    <span>{{ t('ecommerce.carton.productHeight') }}</span>
                    <input
                      v-model.number="calcForm.heightCm"
                      type="number"
                      inputmode="decimal"
                      min="0"
                      step="0.01"
                      placeholder="0"
                    />
                  </label>
                </div>
              </section>

              <section class="carton-calc-sheet__section">
                <h3 class="carton-calc-sheet__section-title">{{ t('ecommerce.carton.paddingSizeCm') }}</h3>
                <p class="carton-calc-sheet__hint">{{ t('ecommerce.carton.paddingHint') }}</p>
                <div class="carton-calc-sheet__field-grid">
                  <label class="carton-calc-sheet__field">
                    <span>{{ t('ecommerce.carton.paddingLength') }}</span>
                    <input
                      v-model.number="calcForm.padLengthCm"
                      type="number"
                      inputmode="decimal"
                      min="0"
                      step="0.01"
                    />
                  </label>
                  <label class="carton-calc-sheet__field">
                    <span>{{ t('ecommerce.carton.paddingWidth') }}</span>
                    <input
                      v-model.number="calcForm.padWidthCm"
                      type="number"
                      inputmode="decimal"
                      min="0"
                      step="0.01"
                    />
                  </label>
                  <label class="carton-calc-sheet__field">
                    <span>{{ t('ecommerce.carton.paddingHeight') }}</span>
                    <input
                      v-model.number="calcForm.padHeightCm"
                      type="number"
                      inputmode="decimal"
                      min="0"
                      step="0.01"
                    />
                  </label>
                </div>
              </section>

              <label class="carton-calc-sheet__field carton-calc-sheet__field--full">
                <span>{{ t('ecommerce.carton.factory') }}</span>
                <select v-model="calcForm.factoryId">
                  <option value="">{{ t('ecommerce.carton.factoryOptional') }}</option>
                  <option v-for="factory in factoryOptions" :key="factory.id" :value="factory.id">
                    {{ factory.name }}
                  </option>
                </select>
              </label>

              <button type="submit" class="carton-calc-sheet__submit" :disabled="calculating">
                {{ calculating ? '…' : t('ecommerce.carton.recalculate') }}
              </button>
            </form>

            <section class="carton-calc-sheet__result">
              <h3 class="carton-calc-sheet__section-title">{{ t('ecommerce.carton.calcResultTitle') }}</h3>
              <SchemeADoodleFrame color="#2563eb" :shadow="false" class="carton-calc-sheet__result-box">
                <div class="carton-calc-sheet__result-inner">
                  <template v-if="calcMatchedDisplay">
                    <template v-if="calcMatchedDisplay.empty">
                      <p class="carton-calc-sheet__result-empty">{{ t('ecommerce.carton.noMatch') }}</p>
                    </template>
                    <template v-else>
                      <p class="carton-calc-sheet__result-label">{{ t('ecommerce.carton.matchedCarton') }}</p>
                      <div class="carton-calc-sheet__result-head">
                        <span class="carton-calc-sheet__result-name">
                          {{ calcMatchedDisplay.carton!.name }}
                        </span>
                        <span class="carton-calc-sheet__result-price">
                          {{ formatUnitPrice(calcMatchedDisplay.carton!.unitPrice) }}
                        </span>
                      </div>
                      <p class="carton-calc-sheet__result-factory">
                        {{ calcMatchedDisplay.carton!.factoryName || '—' }}
                      </p>
                      <p v-if="calcMatchedDisplay.fitsWell" class="carton-calc-sheet__result-badge">
                        ✓ {{ t('ecommerce.carton.fitGood') }}
                      </p>
                      <p class="carton-calc-sheet__result-line">
                        {{ t('ecommerce.carton.innerSize') }}：{{ formatCartonSize(calcMatchedDisplay.carton!) }}
                      </p>
                      <p class="carton-calc-sheet__result-line">
                        {{ t('ecommerce.carton.availableSpace') }}：{{
                          t('ecommerce.carton.availableSpaceValue', {
                            length: formatDimNumber(calcMatchedDisplay.slackL),
                            width: formatDimNumber(calcMatchedDisplay.slackW),
                            height: formatDimNumber(calcMatchedDisplay.slackH),
                          })
                        }}
                      </p>
                    </template>
                  </template>
                  <p v-else class="carton-calc-sheet__result-placeholder">
                    {{ t('ecommerce.carton.calcResultPlaceholder') }}
                  </p>
                </div>
              </SchemeADoodleFrame>
            </section>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { watch } from 'vue'
import SchemeADoodleFrame from '@/mobile/home/themes/scheme-a/SchemeADoodleFrame.vue'
import {
  formatCartonSize,
  formatDimNumber,
  formatUnitPrice,
  useMobileCartonCalculate,
} from '../useMobileCartonCalculate'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const {
  factoryOptions,
  calculating,
  calcForm,
  calcMatchedDisplay,
  loadFactoryOptions,
  runCalculate,
  t,
} = useMobileCartonCalculate()

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    void loadFactoryOptions()
  },
)

function close() {
  emit('update:modelValue', false)
}
</script>

<style scoped lang="scss">
.carton-calc-sheet {
  position: fixed;
  inset: 0;
  z-index: 200;
  display: flex;
  align-items: flex-end;
  background: rgb(15 23 42 / 45%);
}

.carton-calc-sheet__panel {
  width: 100%;
  max-height: 92dvh;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 20px 20px 0 0;
  overflow: hidden;
  font-family: 'ZCOOL KuaiLe', sans-serif;
}

.carton-calc-sheet__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 20px 16px 12px;
  border-bottom: 1px solid #f1f5f9;
  flex-shrink: 0;
}

.carton-calc-sheet__title {
  font-size: 20px;
  color: #1e293b;
  margin: 0 0 4px;
}

.carton-calc-sheet__subtitle {
  font-size: 12px;
  color: #64748b;
  margin: 0;
  line-height: 1.4;
}

.carton-calc-sheet__close {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 50%;
  background: #f1f5f9;
  color: #64748b;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
}

.carton-calc-sheet__body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px max(20px, env(safe-area-inset-bottom));
}

.carton-calc-sheet__section {
  margin-bottom: 16px;
}

.carton-calc-sheet__section-title {
  font-size: 15px;
  color: #1e293b;
  margin: 0 0 8px;
}

.carton-calc-sheet__hint {
  font-size: 11px;
  color: #94a3b8;
  margin: 0 0 8px;
  line-height: 1.4;
}

.carton-calc-sheet__field-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.carton-calc-sheet__field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 11px;
  color: #64748b;

  input,
  select {
    width: 100%;
    border: 2px solid #e2e8f0;
    border-radius: 10px;
    padding: 10px 8px;
    font-family: inherit;
    font-size: 16px;
    color: #1e293b;
    background: #fff;
    outline: none;

    &:focus {
      border-color: #2563eb;
    }
  }

  &--full {
    margin-bottom: 16px;
  }
}

.carton-calc-sheet__submit {
  width: 100%;
  border: none;
  border-radius: 12px;
  padding: 14px;
  font-family: inherit;
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  background: #2563eb;
  cursor: pointer;

  &:disabled {
    opacity: 0.7;
    cursor: wait;
  }

  &:active:not(:disabled) {
    transform: scale(0.98);
  }
}

.carton-calc-sheet__result {
  margin-top: 8px;
}

.carton-calc-sheet__result-box {
  :deep(.sa-doodle-frame__body) {
    padding: 20px 26px 20px 30px;
  }
}

.carton-calc-sheet__result-inner {
  padding: 0;
}

.carton-calc-sheet__result-placeholder,
.carton-calc-sheet__result-empty {
  font-size: 13px;
  color: #94a3b8;
  margin: 0;
  text-align: center;
}

.carton-calc-sheet__result-label {
  font-size: 11px;
  color: #64748b;
  margin: 0 0 4px;
}

.carton-calc-sheet__result-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 2px;
}

.carton-calc-sheet__result-name {
  flex: 1;
  min-width: 0;
  font-size: 18px;
  color: #1e293b;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.carton-calc-sheet__result-price {
  flex-shrink: 0;
  font-size: 25px;
  font-weight: 900;
  color: #c41e3a;
}

.carton-calc-sheet__result-factory {
  font-size: 12px;
  color: #94a3b8;
  margin: 2px 0 8px;
}

.carton-calc-sheet__result-badge {
  display: inline-block;
  font-size: 11px;
  color: #16a34a;
  background: #dcfce7;
  border-radius: 999px;
  padding: 4px 10px;
  margin: 0 0 8px;
}

.carton-calc-sheet__result-line {
  font-size: 12px;
  color: #475569;
  margin: 0 0 6px;
  line-height: 1.5;
}

.carton-calc-sheet-enter-active,
.carton-calc-sheet-leave-active {
  transition: opacity 0.2s ease;

  .carton-calc-sheet__panel {
    transition: transform 0.25s ease;
  }
}

.carton-calc-sheet-enter-from,
.carton-calc-sheet-leave-to {
  opacity: 0;

  .carton-calc-sheet__panel {
    transform: translateY(100%);
  }
}
</style>
