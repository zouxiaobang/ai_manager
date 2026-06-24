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

      :label="optionLabel(opt)"

      :value="opt.skuCode"

    />

  </el-select>

</template>



<script setup lang="ts">

import { ref, watch } from 'vue'

import { fetchInventorySkuOptions, type EcInventorySkuOption } from '@/api/ecommerce/inventory'



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



function optionLabel(opt: EcInventorySkuOption) {

  const stock = opt.quantity ?? 0

  const alert =
    !opt.ignoreAlert && opt.alertThreshold != null && stock <= opt.alertThreshold ? ' ⚠' : ''

  const name = opt.specName ? `${opt.skuCode} · ${opt.specName}` : opt.skuCode

  const product = opt.productName ? ` · ${opt.productName}` : ''

  return `${name}${product} · 库存${stock}${alert}`

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

