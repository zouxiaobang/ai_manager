<template>

  <el-select

    :model-value="selectedCodes"

    multiple

    filterable

    remote

    reserve-keyword

    collapse-tags

    collapse-tags-tooltip

    :remote-method="onSearch"

    :loading="loading"

    :placeholder="placeholder"

    style="width: 100%"

    @update:model-value="onChange"

  >

    <el-option
      v-for="opt in options"
      :key="opt.skuCode"
      :label="optionText(opt)"
      :value="opt.skuCode"
    >
      <span class="sku-option">
        <span class="sku-option__text">{{ optionText(opt) }}</span>
        <el-icon v-if="isSkuAlert(opt)" class="sku-option__alert" :title="alertTitle">
          <WarningFilled />
        </el-icon>
      </span>
    </el-option>

  </el-select>

</template>



<script setup lang="ts">

import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { WarningFilled } from '@element-plus/icons-vue'
import { fetchInventorySkuOptions, type EcInventorySkuOption } from '@/api/ecommerce/inventory'

const { t } = useI18n()



const props = defineProps<{

  modelValue?: string

  productIds?: number[]

  placeholder?: string

}>()



const emit = defineEmits<{ 'update:modelValue': [string]; change: [EcInventorySkuOption[]] }>()



const loading = ref(false)

const options = ref<EcInventorySkuOption[]>([])

const selectedCodes = ref<string[]>([])



watch(

  () => props.modelValue,

  (v) => {

    selectedCodes.value = v

      ? v.split(',').map((s) => s.trim()).filter(Boolean)

      : []

  },

  { immediate: true },

)



const alertTitle = t('ecommerce.inventory.alerting')

function isSkuAlert(opt: EcInventorySkuOption) {
  const stock = opt.quantity ?? 0
  return !opt.ignoreAlert && opt.alertThreshold != null && stock <= opt.alertThreshold
}

function optionText(opt: EcInventorySkuOption) {
  const stock = opt.quantity ?? 0
  const name = opt.specName ? `${opt.skuCode} · ${opt.specName}` : opt.skuCode
  const product = opt.productName ? ` · ${opt.productName}` : ''
  return `${name}${product} · 库存${stock}`
}

async function onSearch(keyword: string) {

  loading.value = true

  try {

    const ids = props.productIds?.length ? props.productIds : undefined

    options.value = await fetchInventorySkuOptions(undefined, keyword || undefined, ids)

  } finally {

    loading.value = false

  }

}



function onChange(codes: string[]) {

  selectedCodes.value = codes

  const value = codes.join(',')

  emit('update:modelValue', value)

  const picked = options.value.filter((o) => codes.includes(o.skuCode))

  emit('change', picked)

}



watch(

  () => props.productIds,

  () => {

    onSearch('')

  },

  { deep: true },

)



onSearch('')

</script>

<style scoped lang="scss">
.sku-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
}

.sku-option__text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sku-option__alert {
  flex-shrink: 0;
  font-size: 16px;
  color: #eab308;
}
</style>

