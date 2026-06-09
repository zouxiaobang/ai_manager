<template>
  <div class="platform-shop-panel">
    <el-tabs v-model="innerTab">
      <el-tab-pane :label="t('ecommerce.platform.tabTitle')" name="platform">
        <div class="panel-toolbar">
          <el-input v-model="platformKeyword" :placeholder="t('ecommerce.platform.searchPlaceholder')" clearable style="width: 240px" />
          <el-select v-model="platformChannel" clearable :placeholder="t('ecommerce.platform.channelType')" style="width: 140px" @change="() => loadPlatforms(true)">
            <el-option :label="t('ecommerce.platform.online')" value="ONLINE" />
            <el-option :label="t('ecommerce.platform.offline')" value="OFFLINE" />
          </el-select>
          <el-button type="primary" @click="openPlatformCreate">{{ t('ecommerce.platform.add') }}</el-button>
        </div>
        <el-table v-loading="platformLoading" :data="platformRecords" stripe border size="small">
          <el-table-column prop="name" :label="t('ecommerce.platform.name')" min-width="120" />
          <el-table-column prop="nameEn" :label="t('ecommerce.platform.nameEn')" min-width="120" show-overflow-tooltip />
          <el-table-column :label="t('ecommerce.platform.platformCode')" width="110">
            <template #default="{ row }">{{ platformCodeLabel(row.platformCode) }}</template>
          </el-table-column>
          <el-table-column :label="t('ecommerce.platform.channelType')" width="90">
            <template #default="{ row }">{{ channelLabel(row.channelType) }}</template>
          </el-table-column>
          <el-table-column prop="remark" :label="t('ecommerce.platform.remark')" min-width="140" show-overflow-tooltip />
          <el-table-column :label="t('ecommerce.platform.status')" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" size="small">
                {{ row.status === 'ENABLED' ? t('ecommerce.product.enabled') : t('ecommerce.product.disabled') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('ecommerce.platform.actions')" width="88" fixed="right" align="center" :class-name="TABLE_ACTIONS_CELL_CLASS">
            <template #default="{ row }">
              <div class="table-actions-cell-inner" @click.stop>
                <el-button link type="primary" @click.stop="openPlatformEdit(row)"><el-icon><Edit /></el-icon></el-button>
                <el-button link type="danger" @click.stop="onPlatformDelete(row)"><el-icon><Delete /></el-icon></el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <TablePagination :page="platformPage" :page-size="platformPageSize" :total="platformTotal" @update:page="onPlatformPageChange" @update:page-size="onPlatformSizeChange" />
      </el-tab-pane>

      <el-tab-pane :label="t('ecommerce.shop.tabTitle')" name="shop">
        <div class="panel-toolbar">
          <el-input v-model="shopKeyword" :placeholder="t('ecommerce.shop.searchPlaceholder')" clearable style="width: 240px" />
          <el-select v-model="shopPlatformId" clearable filterable :placeholder="t('ecommerce.shop.platform')" style="width: 180px" @change="() => loadShops(true)">
            <el-option v-for="p in platformOptions" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
          <el-button type="primary" @click="openShopCreate">{{ t('ecommerce.shop.add') }}</el-button>
        </div>
        <el-table v-loading="shopLoading" :data="shopRecords" stripe border size="small">
          <el-table-column prop="name" :label="t('ecommerce.shop.name')" min-width="140" />
          <el-table-column prop="platformName" :label="t('ecommerce.shop.platform')" width="120" show-overflow-tooltip />
          <el-table-column :label="t('ecommerce.shop.categoryCommission')" width="100" align="right">
            <template #default="{ row }">{{ formatPct(row.categoryCommissionPct) }}</template>
          </el-table-column>
          <el-table-column :label="t('ecommerce.shop.techServiceFee')" width="100" align="right">
            <template #default="{ row }">{{ formatPct(row.techServiceFeePct) }}</template>
          </el-table-column>
          <el-table-column prop="remark" :label="t('ecommerce.shop.remark')" min-width="120" show-overflow-tooltip />
          <el-table-column :label="t('ecommerce.shop.actions')" width="88" fixed="right" align="center" :class-name="TABLE_ACTIONS_CELL_CLASS">
            <template #default="{ row }">
              <div class="table-actions-cell-inner" @click.stop>
                <el-button link type="primary" @click.stop="openShopEdit(row)"><el-icon><Edit /></el-icon></el-button>
                <el-button link type="danger" @click.stop="onShopDelete(row)"><el-icon><Delete /></el-icon></el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <TablePagination :page="shopPage" :page-size="shopPageSize" :total="shopTotal" @update:page="onShopPageChange" @update:page-size="onShopSizeChange" />
      </el-tab-pane>
    </el-tabs>

    <!-- 平台表单 -->
    <el-dialog v-model="platformDialogVisible" :title="platformEditingId ? t('ecommerce.platform.editTitle') : t('ecommerce.platform.createTitle')" width="520px" destroy-on-close>
      <el-form :model="platformForm" label-width="108px">
        <el-form-item :label="t('ecommerce.platform.name')" required><el-input v-model="platformForm.name" /></el-form-item>
        <el-form-item :label="t('ecommerce.platform.nameEn')"><el-input v-model="platformForm.nameEn" /></el-form-item>
        <el-form-item :label="t('ecommerce.platform.platformCode')" required>
          <el-select v-model="platformForm.platformCode" style="width: 100%">
            <el-option v-for="opt in PLATFORM_CODE_OPTIONS" :key="opt.value" :label="platformCodeLabel(opt.value)" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('ecommerce.platform.channelType')" required>
          <el-radio-group v-model="platformForm.channelType">
            <el-radio value="ONLINE">{{ t('ecommerce.platform.online') }}</el-radio>
            <el-radio value="OFFLINE">{{ t('ecommerce.platform.offline') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('ecommerce.platform.remark')"><el-input v-model="platformForm.remark" type="textarea" :rows="2" /></el-form-item>
        <el-form-item :label="t('ecommerce.platform.status')">
          <el-radio-group v-model="platformForm.status">
            <el-radio value="ENABLED">{{ t('ecommerce.product.enabled') }}</el-radio>
            <el-radio value="DISABLED">{{ t('ecommerce.product.disabled') }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="platformDialogVisible = false">{{ t('ecommerce.common.cancel') }}</el-button>
        <el-button type="primary" :loading="platformSaving" @click="onPlatformSave">{{ t('ecommerce.common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 店铺表单 -->
    <el-dialog v-model="shopDialogVisible" :title="shopEditingId ? t('ecommerce.shop.editTitle') : t('ecommerce.shop.createTitle')" width="680px" destroy-on-close top="5vh">
      <el-form :model="shopForm" label-width="128px">
        <el-form-item :label="t('ecommerce.shop.name')" required><el-input v-model="shopForm.name" /></el-form-item>
        <el-form-item :label="t('ecommerce.shop.nameEn')"><el-input v-model="shopForm.nameEn" /></el-form-item>
        <el-form-item :label="t('ecommerce.shop.platform')" required>
          <el-select v-model="shopForm.platformId" filterable style="width: 100%">
            <el-option v-for="p in platformOptions" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('ecommerce.shop.remark')"><el-input v-model="shopForm.remark" type="textarea" :rows="2" /></el-form-item>
        <el-collapse>
          <el-collapse-item :title="t('ecommerce.shop.feeSection')" name="fees">
            <el-form-item :label="t('ecommerce.shop.categoryCommission')"><el-input-number v-model="shopForm.categoryCommissionPct" :min="0" :max="100" :precision="2" controls-position="right" style="width: 100%" /></el-form-item>
            <el-form-item :label="t('ecommerce.shop.techServiceFee')"><el-input-number v-model="shopForm.techServiceFeePct" :min="0" :max="100" :precision="2" controls-position="right" style="width: 100%" /></el-form-item>
            <el-form-item :label="t('ecommerce.shop.paymentFee')"><el-input-number v-model="shopForm.paymentFeePct" :min="0" :max="100" :precision="2" controls-position="right" style="width: 100%" /></el-form-item>
            <el-form-item :label="t('ecommerce.shop.promotionFee')"><el-input-number v-model="shopForm.promotionFeePct" :min="0" :max="100" :precision="2" controls-position="right" style="width: 100%" /></el-form-item>
            <el-form-item :label="t('ecommerce.shop.fulfillmentFee')"><el-input-number v-model="shopForm.fulfillmentFeePct" :min="0" :max="100" :precision="2" controls-position="right" style="width: 100%" /></el-form-item>
            <el-form-item :label="t('ecommerce.shop.returnServiceFee')"><el-input-number v-model="shopForm.returnServiceFeePct" :min="0" :max="100" :precision="2" controls-position="right" style="width: 100%" /></el-form-item>
            <el-form-item :label="t('ecommerce.shop.installmentFee')"><el-input-number v-model="shopForm.installmentFeePct" :min="0" :max="100" :precision="2" controls-position="right" style="width: 100%" /></el-form-item>
            <el-form-item :label="t('ecommerce.shop.activityServiceFee')"><el-input-number v-model="shopForm.activityServiceFeePct" :min="0" :max="100" :precision="2" controls-position="right" style="width: 100%" /></el-form-item>
            <el-form-item :label="t('ecommerce.shop.annualPlatformFee')"><el-input-number v-model="shopForm.annualPlatformFee" :min="0" :precision="2" controls-position="right" style="width: 100%" /></el-form-item>
            <el-form-item :label="t('ecommerce.shop.depositAmount')"><el-input-number v-model="shopForm.depositAmount" :min="0" :precision="2" controls-position="right" style="width: 100%" /></el-form-item>
            <el-form-item :label="t('ecommerce.shop.shippingInsuranceFee')"><el-input-number v-model="shopForm.shippingInsuranceFee" :min="0" :precision="2" controls-position="right" style="width: 100%" /></el-form-item>
            <el-form-item :label="t('ecommerce.shop.defaultReceiveProvince')">
              <el-input v-model="shopForm.defaultReceiveProvince" :placeholder="t('ecommerce.shop.defaultReceiveProvinceHint')" />
            </el-form-item>
            <el-form-item :label="t('ecommerce.shop.otherFee')"><el-input-number v-model="shopForm.otherFeePct" :min="0" :max="100" :precision="2" controls-position="right" style="width: 100%" /></el-form-item>
            <el-form-item :label="t('ecommerce.shop.otherFeeRemark')"><el-input v-model="shopForm.otherFeeRemark" /></el-form-item>
          </el-collapse-item>
        </el-collapse>
        <el-form-item :label="t('ecommerce.shop.status')">
          <el-radio-group v-model="shopForm.status">
            <el-radio value="ENABLED">{{ t('ecommerce.product.enabled') }}</el-radio>
            <el-radio value="DISABLED">{{ t('ecommerce.product.disabled') }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shopDialogVisible = false">{{ t('ecommerce.common.cancel') }}</el-button>
        <el-button type="primary" :loading="shopSaving" @click="onShopSave">{{ t('ecommerce.common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit } from '@element-plus/icons-vue'
import {
  PLATFORM_CODE_OPTIONS,
  createPlatform,
  deletePlatform,
  fetchPlatformOptions,
  fetchPlatforms,
  updatePlatform,
  type EcPlatform,
  type EcPlatformSaveRequest,
} from '@/api/ecommerce/platform'
import {
  createShop,
  deleteShop,
  fetchShops,
  updateShop,
  type EcShop,
  type EcShopSaveRequest,
} from '@/api/ecommerce/shop'
import TablePagination from '@/components/TablePagination.vue'
import { usePagination } from '@/composables/usePagination'
import { TABLE_ACTIONS_CELL_CLASS } from '@/constants/table'

const { t } = useI18n()
const innerTab = ref('platform')

const platformOptions = ref<EcPlatform[]>([])
const platformKeyword = ref('')
const platformChannel = ref<string | undefined>()
const platformDialogVisible = ref(false)
const platformEditingId = ref<number | null>(null)
const platformSaving = ref(false)
const platformForm = reactive<EcPlatformSaveRequest>({
  name: '',
  nameEn: '',
  platformCode: 2,
  channelType: 'ONLINE',
  remark: '',
  status: 'ENABLED',
})

const shopKeyword = ref('')
const shopPlatformId = ref<number | undefined>()
const shopDialogVisible = ref(false)
const shopEditingId = ref<number | null>(null)
const shopSaving = ref(false)
const shopForm = reactive<EcShopSaveRequest>({
  name: '',
  nameEn: '',
  platformId: 0,
  remark: '',
  status: 'ENABLED',
})

const {
  page: platformPage,
  pageSize: platformPageSize,
  total: platformTotal,
  records: platformRecords,
  loading: platformLoading,
  load: loadPlatforms,
  onPageChange: onPlatformPageChange,
  onSizeChange: onPlatformSizeChange,
} = usePagination((p, ps) =>
  fetchPlatforms(platformKeyword.value.trim() || undefined, platformChannel.value, { page: p, pageSize: ps }),
)

const {
  page: shopPage,
  pageSize: shopPageSize,
  total: shopTotal,
  records: shopRecords,
  loading: shopLoading,
  load: loadShops,
  onPageChange: onShopPageChange,
  onSizeChange: onShopSizeChange,
} = usePagination((p, ps) =>
  fetchShops(shopKeyword.value.trim() || undefined, shopPlatformId.value, { page: p, pageSize: ps }),
)

function platformCodeLabel(code: number) {
  const opt = PLATFORM_CODE_OPTIONS.find((o) => o.value === code)
  if (!opt) return String(code)
  return t(`ecommerce.platform.codes.${opt.labelKey}`)
}

function channelLabel(type: string) {
  return type === 'OFFLINE' ? t('ecommerce.platform.offline') : t('ecommerce.platform.online')
}

function formatPct(v?: number | null) {
  if (v == null) return '—'
  return `${Number(v).toFixed(2)}%`
}

function resetPlatformForm() {
  platformForm.name = ''
  platformForm.nameEn = ''
  platformForm.platformCode = 2
  platformForm.channelType = 'ONLINE'
  platformForm.remark = ''
  platformForm.status = 'ENABLED'
}

function openPlatformCreate() {
  platformEditingId.value = null
  resetPlatformForm()
  platformDialogVisible.value = true
}

function openPlatformEdit(row: EcPlatform) {
  platformEditingId.value = row.id
  platformForm.name = row.name
  platformForm.nameEn = row.nameEn || ''
  platformForm.platformCode = row.platformCode
  platformForm.channelType = row.channelType
  platformForm.remark = row.remark || ''
  platformForm.status = row.status
  platformDialogVisible.value = true
}

async function onPlatformSave() {
  if (!platformForm.name.trim()) {
    ElMessage.warning(t('ecommerce.platform.nameRequired'))
    return
  }
  platformSaving.value = true
  try {
    const payload = { ...platformForm, name: platformForm.name.trim() }
    if (platformEditingId.value) {
      await updatePlatform(platformEditingId.value, payload)
    } else {
      await createPlatform(payload)
    }
    ElMessage.success(t('ecommerce.common.saved'))
    platformDialogVisible.value = false
    await Promise.all([loadPlatforms(), refreshPlatformOptions()])
  } finally {
    platformSaving.value = false
  }
}

async function onPlatformDelete(row: EcPlatform) {
  await ElMessageBox.confirm(t('ecommerce.platform.deleteConfirm', { name: row.name }), { type: 'warning' })
  await deletePlatform(row.id)
  ElMessage.success(t('ecommerce.common.deleted'))
  await Promise.all([loadPlatforms(), refreshPlatformOptions()])
}

function resetShopForm() {
  Object.assign(shopForm, {
    name: '',
    nameEn: '',
    platformId: platformOptions.value[0]?.id ?? 0,
    remark: '',
    categoryCommissionPct: undefined,
    techServiceFeePct: undefined,
    paymentFeePct: undefined,
    promotionFeePct: undefined,
    fulfillmentFeePct: undefined,
    returnServiceFeePct: undefined,
    installmentFeePct: undefined,
    activityServiceFeePct: undefined,
    annualPlatformFee: undefined,
    depositAmount: undefined,
    shippingInsuranceFee: undefined,
    otherFeePct: undefined,
    otherFeeRemark: '',
    status: 'ENABLED',
  })
}

function openShopCreate() {
  shopEditingId.value = null
  resetShopForm()
  shopDialogVisible.value = true
}

function openShopEdit(row: EcShop) {
  shopEditingId.value = row.id
  Object.assign(shopForm, { ...row })
  shopDialogVisible.value = true
}

async function onShopSave() {
  if (!shopForm.name.trim()) {
    ElMessage.warning(t('ecommerce.shop.nameRequired'))
    return
  }
  if (!shopForm.platformId) {
    ElMessage.warning(t('ecommerce.shop.platformRequired'))
    return
  }
  shopSaving.value = true
  try {
    const payload = { ...shopForm, name: shopForm.name.trim() }
    if (shopEditingId.value) {
      await updateShop(shopEditingId.value, payload)
    } else {
      await createShop(payload)
    }
    ElMessage.success(t('ecommerce.common.saved'))
    shopDialogVisible.value = false
    await loadShops()
  } finally {
    shopSaving.value = false
  }
}

async function onShopDelete(row: EcShop) {
  await ElMessageBox.confirm(t('ecommerce.shop.deleteConfirm', { name: row.name }), { type: 'warning' })
  await deleteShop(row.id)
  ElMessage.success(t('ecommerce.common.deleted'))
  await loadShops()
}

async function refreshPlatformOptions() {
  platformOptions.value = await fetchPlatformOptions()
}

let platformSearchTimer: ReturnType<typeof setTimeout> | null = null
watch(platformKeyword, () => {
  if (platformSearchTimer) clearTimeout(platformSearchTimer)
  platformSearchTimer = setTimeout(() => loadPlatforms(true), 300)
})

let shopSearchTimer: ReturnType<typeof setTimeout> | null = null
watch(shopKeyword, () => {
  if (shopSearchTimer) clearTimeout(shopSearchTimer)
  shopSearchTimer = setTimeout(() => loadShops(true), 300)
})

watch(innerTab, (tab) => {
  if (tab === 'platform') loadPlatforms()
  else loadShops()
})

async function loadAll() {
  await refreshPlatformOptions()
  if (innerTab.value === 'platform') await loadPlatforms()
  else await loadShops()
}

onMounted(loadAll)
defineExpose({ loadAll })
</script>

<style scoped lang="scss">
.panel-toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}
</style>
