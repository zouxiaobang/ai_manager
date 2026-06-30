<template>
  <el-drawer
    v-model="visible"
    :title="t('ecommerce.inventory.globalLogsTitle')"
    size="960px"
    destroy-on-close
    @open="() => load(true)"
  >
    <div class="global-log-drawer">
      <div class="drawer-toolbar">
        <el-input
          v-model="keyword"
          :placeholder="t('ecommerce.inventory.searchPlaceholder')"
          clearable
          style="width: 220px"
        />
        <el-input
          v-model="skuCode"
          :placeholder="t('ecommerce.inventory.skuCode')"
          clearable
          style="width: 140px"
        />
        <el-select
          v-model="factoryId"
          clearable
          filterable
          :placeholder="t('ecommerce.inventory.factoryPlaceholder')"
          style="width: 160px"
        >
          <el-option v-for="f in factoryOptions" :key="f.id" :label="f.name" :value="f.id" />
        </el-select>
        <el-select
          v-model="changeType"
          clearable
          :placeholder="t('ecommerce.inventory.changeType')"
          style="width: 120px"
        >
          <el-option :label="t('ecommerce.inbound.inbound')" value="INBOUND" />
          <el-option :label="t('ecommerce.inventory.deduct')" value="DEDUCT" />
          <el-option :label="t('ecommerce.inventory.reclaim')" value="RECLAIM" />
          <el-option :label="t('ecommerce.stocktake.stocktake')" value="STOCKTAKE" />
        </el-select>
      </div>

      <el-table v-loading="loading" :data="records" stripe border size="small">
        <el-table-column prop="skuCode" :label="t('ecommerce.inventory.skuCode')" width="120" fixed />
        <el-table-column prop="specName" :label="t('ecommerce.inventory.specName')" width="100" show-overflow-tooltip />
        <el-table-column prop="productName" :label="t('ecommerce.inventory.productName')" min-width="120" show-overflow-tooltip />
        <el-table-column prop="factoryName" :label="t('ecommerce.inventory.factory')" width="100" show-overflow-tooltip />
        <el-table-column :label="t('ecommerce.inventory.changeType')" width="90">
          <template #default="{ row }">{{ changeTypeLabel(row.changeType) }}</template>
        </el-table-column>
        <el-table-column prop="changeQty" :label="t('ecommerce.inventory.changeQty')" width="80" align="right" />
        <el-table-column prop="remark" :label="t('ecommerce.inbound.remark')" min-width="140" show-overflow-tooltip />
        <el-table-column :label="t('ecommerce.inventory.logTime')" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>

      <TablePagination
        :page="page"
        :page-size="pageSize"
        :total="total"
        @update:page="onPageChange"
        @update:page-size="onSizeChange"
      />
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { fetchGlobalInventoryLogs } from '@/api/ecommerce/inventory'
import { fetchFactoryOptions, type EcFactory } from '@/api/ecommerce/factory'
import TablePagination from '@/components/TablePagination.vue'
import { usePagination } from '@/composables/usePagination'
import { formatDateTime } from '@/utils/date'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [boolean] }>()

const { t } = useI18n()
const keyword = ref('')
const skuCode = ref('')
const factoryId = ref<number | undefined>()
const changeType = ref<string | undefined>()
const factoryOptions = ref<EcFactory[]>([])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const { page, pageSize, total, records, loading, load, onPageChange, onSizeChange } = usePagination(
  (p, ps) =>
    fetchGlobalInventoryLogs({
      keyword: keyword.value.trim() || undefined,
      skuCode: skuCode.value.trim() || undefined,
      factoryId: factoryId.value,
      changeType: changeType.value,
      page: p,
      pageSize: ps,
    }),
)

function changeTypeLabel(type: string) {
  if (type === 'DEDUCT') return t('ecommerce.inventory.deduct')
  if (type === 'RECLAIM') return t('ecommerce.inventory.reclaim')
  if (type === 'INBOUND') return t('ecommerce.inbound.inbound')
  if (type === 'STOCKTAKE') return t('ecommerce.stocktake.stocktake')
  return type
}

let inputTimer: ReturnType<typeof setTimeout> | null = null
watch([keyword, skuCode], () => {
  if (inputTimer) clearTimeout(inputTimer)
  inputTimer = setTimeout(() => load(true), 300)
})

watch([factoryId, changeType], () => {
  if (visible.value) load(true)
})

onMounted(async () => {
  factoryOptions.value = await fetchFactoryOptions('PRODUCTION')
})
</script>

<style scoped lang="scss">
.drawer-toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}
</style>
