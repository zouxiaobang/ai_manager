<template>
  <div class="storage-quota-input">
    <el-input-number
      v-model="displayValue"
      :min="0"
      :step="stepForUnit"
      :precision="unit === 'GB' ? 1 : 0"
      :controls="false"
      class="storage-quota-input__number"
    />
    <el-select v-model="unit" class="storage-quota-input__unit">
      <el-option label="MB" value="MB" />
      <el-option label="GB" value="GB" />
    </el-select>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

type QuotaUnit = 'MB' | 'GB'

const props = withDefaults(
  defineProps<{
    modelValue: number
    mbStep?: number
    gbStep?: number
  }>(),
  {
    mbStep: 256,
    gbStep: 0.5,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: number]
}>()

function pickInitialUnit(mb: number): QuotaUnit {
  if (mb >= 1024 && mb % 1024 === 0) return 'GB'
  return 'MB'
}

const unit = ref<QuotaUnit>(pickInitialUnit(props.modelValue))

const stepForUnit = computed(() => (unit.value === 'GB' ? props.gbStep : props.mbStep))

const displayValue = computed({
  get() {
    if (unit.value === 'GB') {
      const gb = props.modelValue / 1024
      return Number.isInteger(gb) ? gb : parseFloat(gb.toFixed(1))
    }
    return props.modelValue
  },
  set(value: number | undefined) {
    const next = value ?? 0
    const mb =
      unit.value === 'GB'
        ? Math.max(0, Math.round(next * 1024))
        : Math.max(0, Math.round(next))
    emit('update:modelValue', mb)
  },
})
</script>

<style scoped lang="scss">
.storage-quota-input {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  max-width: 100%;

  &__number {
    width: 108px;
    flex-shrink: 0;

    :deep(.el-input__wrapper) {
      padding-left: 10px;
      padding-right: 10px;
    }
  }

  &__unit {
    width: 72px;
    flex-shrink: 0;
  }
}
</style>
