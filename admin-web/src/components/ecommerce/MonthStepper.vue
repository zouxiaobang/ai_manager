<template>
  <div class="month-stepper">
    <el-button
      class="month-stepper__nav month-stepper__nav--prev"
      :disabled="disabled"
      :aria-label="t('ecommerce.monthStepper.prevMonth')"
      @click="shift(-1)"
    >
      <el-icon><ArrowLeft /></el-icon>
    </el-button>
    <el-date-picker
      v-model="model"
      type="month"
      value-format="YYYY-MM"
      format="YYYY年MM月"
      :placeholder="placeholder"
      :disabled="disabled"
      :clearable="clearable"
      class="month-stepper__picker"
    />
    <el-button
      class="month-stepper__nav month-stepper__nav--next"
      :disabled="disabled"
      :aria-label="t('ecommerce.monthStepper.nextMonth')"
      @click="shift(1)"
    >
      <el-icon><ArrowRight /></el-icon>
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { shiftMonth } from '@/utils/date'

withDefaults(
  defineProps<{
    placeholder?: string
    disabled?: boolean
    clearable?: boolean
  }>(),
  {
    placeholder: '',
    disabled: false,
    clearable: false,
  },
)

const model = defineModel<string | undefined>()

const { t } = useI18n()

function shift(delta: number) {
  if (!model.value) {
    model.value = shiftMonth(
      `${new Date().getFullYear()}-${String(new Date().getMonth() + 1).padStart(2, '0')}`,
      delta,
    )
    return
  }
  model.value = shiftMonth(model.value, delta)
}
</script>

<style scoped lang="scss">
.month-stepper {
  display: flex;
  align-items: stretch;
  gap: 8px;
  width: 100%;
}

.month-stepper__nav {
  flex-shrink: 0;
  width: 36px;
  height: var(--el-component-size);
  padding: 0;
  margin: 0;
  --el-button-text-color: #fff;
  --el-button-hover-text-color: #fff;
  --el-button-active-text-color: #fff;

  &.month-stepper__nav--prev {
    --el-button-bg-color: #b91c1c;
    --el-button-border-color: #b91c1c;
    --el-button-hover-bg-color: #991b1b;
    --el-button-hover-border-color: #991b1b;
    --el-button-active-bg-color: #7f1d1d;
    --el-button-active-border-color: #7f1d1d;
    --el-button-disabled-bg-color: #e5b4b4;
    --el-button-disabled-border-color: #e5b4b4;
    --el-button-disabled-text-color: #fff;
  }

  &.month-stepper__nav--next {
    --el-button-bg-color: #15803d;
    --el-button-border-color: #15803d;
    --el-button-hover-bg-color: #166534;
    --el-button-hover-border-color: #166534;
    --el-button-active-bg-color: #14532d;
    --el-button-active-border-color: #14532d;
    --el-button-disabled-bg-color: #86a88f;
    --el-button-disabled-border-color: #86a88f;
    --el-button-disabled-text-color: #fff;
  }
}

.month-stepper__picker {
  flex: 1;
  min-width: 0;
}
</style>
