<template>
  <el-drawer
    v-model="visible"
    :title="t('ecommerce.listingLink.detailTitle')"
    size="920px"
    destroy-on-close
    @open="loadDetail"
  >
    <div v-loading="loading" class="link-detail">
      <template v-if="detail">
        <el-descriptions :column="2" border size="small" class="detail-block">
          <el-descriptions-item :label="t('ecommerce.listingLink.name')">{{ detail.name }}</el-descriptions-item>
          <el-descriptions-item :label="t('ecommerce.listingLink.shop')">{{ detail.shopName || '—' }}</el-descriptions-item>
          <el-descriptions-item :label="t('ecommerce.listingLink.platform')">{{ detail.platformName || '—' }}</el-descriptions-item>
          <el-descriptions-item :label="t('ecommerce.listingLink.product')">
            <template v-if="detail.products?.length">
              <span v-for="(p, idx) in detail.products" :key="p.productId">
                {{ p.productName || p.productId }}<span v-if="idx < detail.products.length - 1">、</span>
              </span>
            </template>
            <span v-else-if="detail.productNames">{{ detail.productNames }}</span>
            <span v-else>—</span>
          </el-descriptions-item>
          <el-descriptions-item :label="t('ecommerce.listingLink.platformUrl')" :span="2">
            <a v-if="detail.platformUrl" :href="detail.platformUrl" target="_blank" rel="noopener">{{ detail.platformUrl }}</a>
            <span v-else>—</span>
          </el-descriptions-item>
          <el-descriptions-item :label="t('ecommerce.listingLink.listingTime')">{{ formatDateTime(detail.listingTime) }}</el-descriptions-item>
          <el-descriptions-item :label="t('ecommerce.listingLink.remark')">{{ detail.remark || '—' }}</el-descriptions-item>
        </el-descriptions>

        <el-alert v-if="detail.costFormula" type="info" :closable="false" class="formula-alert" :title="t('ecommerce.listingLink.costFormula')">
          <p>{{ detail.costFormula }}</p>
        </el-alert>
        <p v-else class="formula-hint">{{ t('ecommerce.listingLink.calcHint') }}</p>

        <el-table :data="detail.skus ?? []" stripe border size="small">
          <el-table-column prop="skuName" :label="t('ecommerce.listingLink.skuName')" min-width="90" />
          <el-table-column prop="skuCodes" :label="t('ecommerce.listingLink.skuCodes')" min-width="110" />
          <el-table-column :label="t('ecommerce.listingLink.inventory')" min-width="120">
            <template #default="{ row }">
              <div v-for="inv in row.inventories ?? []" :key="inv.skuCode" class="inv-line">
                {{ inv.skuCode }}: {{ inv.quantity ?? 0 }}
                <el-tag v-if="inv.alertActive" type="danger" size="small">{{ t('ecommerce.listingLink.pricingRiskBelowMin') }}</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column :label="t('ecommerce.listingLink.costPrice')" width="88" align="right">
            <template #default="{ row }">{{ formatMoney(row.costPrice) }}</template>
          </el-table-column>
          <el-table-column :label="t('ecommerce.listingLink.minSetAmount')" width="96" align="right">
            <template #default="{ row }">{{ formatMoney(row.minSetAmount) }}</template>
          </el-table-column>
          <el-table-column :label="t('ecommerce.listingLink.actualSetAmount')" width="96" align="right">
            <template #default="{ row }">{{ formatMoney(row.actualSetAmount) }}</template>
          </el-table-column>
          <el-table-column :label="t('ecommerce.listingLink.profit')" width="100" align="right">
            <template #default="{ row }">
              {{ formatMoney(row.profit) }}
              <el-tag v-if="row.pricingRisk === 'BELOW_MIN'" type="danger" size="small">{{ t('ecommerce.listingLink.pricingRiskBelowMin') }}</el-tag>
              <el-tag v-else-if="row.pricingRisk === 'NEGATIVE_PROFIT'" type="warning" size="small">{{ t('ecommerce.listingLink.pricingRiskNegativeProfit') }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!(detail.skus?.length)" :description="t('ecommerce.listingLink.noSkus')" />
      </template>
    </div>
    <template v-if="detail" #footer>
      <el-button @click="visible = false">{{ t('ecommerce.common.close') }}</el-button>
      <el-button type="primary" @click="emitEdit">{{ t('ecommerce.listingLink.edit') }}</el-button>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { fetchListingLink, type EcListingLink } from '@/api/ecommerce/listingLink'
import { formatDateTime } from '@/utils/date'

const props = defineProps<{ modelValue: boolean; linkId?: number | null }>()
const emit = defineEmits<{ 'update:modelValue': [boolean]; edit: [id: number] }>()

const { t } = useI18n()
const loading = ref(false)
const detail = ref<EcListingLink | null>(null)

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

watch(
  () => props.linkId,
  () => {
    if (visible.value && props.linkId) loadDetail()
  },
)

async function loadDetail() {
  if (!props.linkId) return
  loading.value = true
  try {
    detail.value = await fetchListingLink(props.linkId)
  } finally {
    loading.value = false
  }
}

function formatMoney(v?: number | null) {
  if (v == null) return '—'
  return `¥${Number(v).toFixed(2)}`
}

function emitEdit() {
  if (detail.value?.id) {
    emit('edit', detail.value.id)
    visible.value = false
  }
}
</script>

<style scoped lang="scss">
.link-detail {
  min-height: 160px;
}

.detail-block {
  margin-bottom: 12px;
}

.formula-hint,
.formula-alert {
  margin: 0 0 12px;
  font-size: 12px;
}

.inv-line {
  font-size: 12px;
  line-height: 1.6;
}
</style>
