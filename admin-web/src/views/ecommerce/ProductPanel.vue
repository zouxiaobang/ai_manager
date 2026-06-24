<template>
  <div class="product-panel">
    <el-tabs v-model="innerTab">
      <el-tab-pane :label="t('ecommerce.product.tabProducts')" name="products">
    <div class="panel-toolbar">
      <el-input
        v-model="keyword"
        :placeholder="t('ecommerce.product.searchPlaceholder')"
        clearable
        style="width: 320px"
      />
      <el-button type="primary" @click="openCreate">{{ t('ecommerce.product.add') }}</el-button>
    </div>

    <el-table v-loading="loading" :data="records" stripe border>
      <el-table-column
        :label="t('ecommerce.product.image')"
        width="64"
        align="center"
        class-name="product-list-image-cell"
      >
        <template #default="{ row }">
          <div
            class="product-list-thumb"
            :title="t('ecommerce.product.imageClickHint')"
            @click="openListImagePreview(row)"
          >
            <img
              v-if="isListImageVisible(row)"
              :src="getEcommerceImageUrl(row.imageName)"
              alt=""
              @error="onListImageError(row.id)"
            />
            <el-icon v-else :size="18"><Picture /></el-icon>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="name" :label="t('ecommerce.product.name')" min-width="160" />
      <el-table-column prop="factoryName" :label="t('ecommerce.product.factory')" min-width="140" show-overflow-tooltip />
      <el-table-column :label="t('ecommerce.product.rebatePct')" width="100" align="right">
        <template #default="{ row }">{{ formatPct(row.rebatePct) }}</template>
      </el-table-column>
      <el-table-column :label="t('ecommerce.product.skuCount')" width="90" align="center">
        <template #default="{ row }">{{ row.skuCount }}</template>
      </el-table-column>
      <el-table-column :label="t('ecommerce.product.status')" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" size="small">
            {{ row.status === 'ENABLED' ? t('ecommerce.product.enabled') : t('ecommerce.product.disabled') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('ecommerce.product.updatedAt')" width="170">
        <template #default="{ row }">{{ formatDate(row.updateTime) }}</template>
      </el-table-column>
      <el-table-column
        :label="t('ecommerce.product.actions')"
        width="88"
        fixed="right"
        align="center"
        :class-name="TABLE_ACTIONS_CELL_CLASS"
      >
        <template #default="{ row }">
          <div class="table-actions-cell-inner" @click.stop>
            <el-button link type="primary" :title="t('ecommerce.product.edit')" @click.stop="openEdit(row.id)">
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button link type="danger" :title="t('ecommerce.product.delete')" @click.stop="onDelete(row)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <TablePagination
      :page="page"
      :page-size="pageSize"
      :total="total"
      @update:page="onPageChange"
      @update:page-size="onSizeChange"
    />
      </el-tab-pane>
      <el-tab-pane :label="t('ecommerce.listingLink.tabTitle')" name="listing">
        <ListingLinkPanel ref="listingRef" />
      </el-tab-pane>
    </el-tabs>

    <el-drawer
      v-model="drawerVisible"
      :title="editingId ? t('ecommerce.product.editTitle') : t('ecommerce.product.createTitle')"
      size="1040px"
      destroy-on-close
    >
      <el-form :model="form" label-width="108px" class="product-form">
        <div class="product-form__image-top">
          <EcImageField
            v-model="form.imageName"
            size="large"
            show-name
            :dialog-title="t('ecommerce.product.imageName')"
          />
        </div>

        <el-divider content-position="left">{{ t('ecommerce.product.spuSection') }}</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="t('ecommerce.product.name')" required>
              <el-input v-model="form.name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="t('ecommerce.product.factory')">
              <el-select
                v-model="form.factoryId"
                clearable
                filterable
                :placeholder="t('ecommerce.product.factoryPlaceholder')"
                style="width: 100%"
              >
                <el-option
                  v-for="f in factoryOptions"
                  :key="f.id"
                  :label="f.name"
                  :value="f.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="t('ecommerce.product.rebatePct')">
              <el-input-number
                v-model="form.rebatePct"
                :min="0"
                :max="100"
                :precision="2"
                :step="0.1"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="t('ecommerce.product.description')">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item :label="t('ecommerce.product.status')">
          <el-radio-group v-model="form.status">
            <el-radio value="ENABLED">{{ t('ecommerce.product.enabled') }}</el-radio>
            <el-radio value="DISABLED">{{ t('ecommerce.product.disabled') }}</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-divider content-position="left">
          {{ t('ecommerce.product.skuSection') }}
          <el-button type="primary" link style="margin-left: 8px" @click="addSkuRow">
            {{ t('ecommerce.product.addSku') }}
          </el-button>
        </el-divider>

        <p class="sku-row-hint">{{ t('ecommerce.product.skuRowClickHint') }} · {{ t('ecommerce.product.cartonMatchHint') }}</p>

        <div class="sku-table-wrap">
          <el-table
            :data="form.skus"
            border
            size="small"
            max-height="420"
            class="sku-click-table"
            :row-class-name="skuRowClassName"
            @cell-click="onSkuCellClick"
            @cell-mouse-enter="onSkuCellMouseEnter"
            @cell-mouse-leave="onSkuCellMouseLeave"
          >
            <el-table-column :label="t('ecommerce.product.skuCode')" width="120" fixed>
              <template #default="{ row }">
                <div class="sku-cell-interactive" @click.stop>
                  <el-input v-model="row.skuCode" size="small" />
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="t('ecommerce.product.specName')" width="100">
              <template #default="{ row }">
                <div class="sku-cell-interactive" @click.stop>
                  <el-input v-model="row.specName" size="small" />
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="t('ecommerce.product.skuRebatePct')" width="110">
              <template #default="{ row }">
                <div class="sku-cell-interactive" @click.stop>
                  <el-input-number
                    v-model="row.rebatePct"
                    :min="0"
                    :max="100"
                    :precision="2"
                    :step="0.1"
                    size="small"
                    controls-position="right"
                    style="width: 100%"
                  />
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="t('ecommerce.product.imageName')" width="72" align="center">
              <template #default="{ row }">
                <div class="sku-cell-interactive" @click.stop>
                  <EcImageField
                    v-model="row.imageName"
                    size="compact"
                    :dialog-title="row.skuCode || t('ecommerce.product.imageName')"
                  />
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="t('ecommerce.product.productSize')" min-width="200">
              <template #default="{ row }">
                <div class="dim-row sku-cell-interactive" @click.stop>
                  <el-input-number v-model="row.productLengthCm" :min="0" :precision="2" size="small" controls-position="right" />
                  <span>×</span>
                  <el-input-number v-model="row.productWidthCm" :min="0" :precision="2" size="small" controls-position="right" />
                  <span>×</span>
                  <el-input-number v-model="row.productHeightCm" :min="0" :precision="2" size="small" controls-position="right" />
                  <span class="dim-unit">cm</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="t('ecommerce.product.matchedCarton')" min-width="140" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="sku-carton-name">{{ row.cartonName || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column :label="t('ecommerce.product.cartonSize')" min-width="200">
              <template #default="{ row }">
                <div class="dim-row sku-cell-interactive" @click.stop>
                  <el-input-number v-model="row.cartonLengthCm" :min="0" :precision="2" size="small" controls-position="right" />
                  <span>×</span>
                  <el-input-number v-model="row.cartonWidthCm" :min="0" :precision="2" size="small" controls-position="right" />
                  <span>×</span>
                  <el-input-number v-model="row.cartonHeightCm" :min="0" :precision="2" size="small" controls-position="right" />
                  <span class="dim-unit">cm</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="t('ecommerce.product.cartonWeight')" width="150">
              <template #default="{ row }">
                <div class="weight-row sku-cell-interactive" @click.stop>
                  <el-input-number v-model="row.cartonGrossWeightKg" :min="0" :precision="3" size="small" controls-position="right" :placeholder="t('ecommerce.product.gross')" />
                  <el-input-number v-model="row.cartonNetWeightKg" :min="0" :precision="3" size="small" controls-position="right" :placeholder="t('ecommerce.product.net')" />
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="t('ecommerce.product.unitsPerCarton')" width="100">
              <template #default="{ row }">
                <div class="sku-cell-interactive" @click.stop>
                  <el-input-number v-model="row.unitsPerCarton" :min="1" size="small" controls-position="right" />
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="t('ecommerce.product.salePrice')" width="100">
              <template #default="{ row }">
                <div class="sku-cell-interactive" @click.stop>
                  <el-input-number v-model="row.salePrice" :min="0" :precision="2" size="small" controls-position="right" />
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="t('ecommerce.product.skuStatus')" width="110">
              <template #default="{ row }">
                <div class="sku-cell-interactive" @click.stop>
                  <el-select v-model="row.status" size="small">
                  <el-option :label="t('ecommerce.product.onSale')" value="ON_SALE" />
                  <el-option :label="t('ecommerce.product.offSale')" value="OFF_SALE" />
                  <el-option :label="t('ecommerce.product.draft')" value="DRAFT" />
                </el-select>
                </div>
              </template>
            </el-table-column>
            <el-table-column
              width="48"
              fixed="right"
              align="center"
              :class-name="TABLE_ACTIONS_CELL_CLASS"
            >
              <template #default="{ $index }">
                <div class="table-actions-cell-inner" @click.stop>
                  <el-button
                    link
                    type="danger"
                    :disabled="form.skus.length <= 1"
                    :title="t('ecommerce.product.removeSku')"
                    @click.stop="removeSkuRow($index)"
                  >
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <template v-if="editingId">
          <el-divider content-position="left">{{ t('ecommerce.product.listingLinksSection') }}</el-divider>
          <div class="product-listing-links">
            <el-button type="primary" link @click="createListingFromProduct">
              {{ t('ecommerce.product.createListingLink') }}
            </el-button>
            <el-table v-if="productListingLinks.length" :data="productListingLinks" size="small" border stripe class="product-listing-table">
              <el-table-column prop="name" :label="t('ecommerce.listingLink.name')" min-width="120" />
              <el-table-column prop="shopName" :label="t('ecommerce.listingLink.shop')" width="120" />
              <el-table-column prop="platformName" :label="t('ecommerce.listingLink.platform')" width="100" />
              <el-table-column :label="t('ecommerce.listingLink.listingTime')" width="110">
                <template #default="{ row }">{{ formatDate(row.listingTime) }}</template>
              </el-table-column>
            </el-table>
            <el-empty v-else :description="t('ecommerce.product.noListingLinks')" :image-size="64" />
          </div>
        </template>
      </el-form>

      <template #footer>
        <el-button @click="drawerVisible = false">{{ t('ecommerce.common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">{{ t('ecommerce.common.save') }}</el-button>
      </template>
    </el-drawer>

    <el-dialog
      v-model="skuCardVisible"
      :title="t('ecommerce.product.skuCardTitle')"
      width="480px"
      destroy-on-close
    >
      <div v-if="skuCardData" class="sku-card">
        <div class="sku-card__hero">
          <EcImageField
            v-model="skuCardData.imageName"
            size="medium"
            :dialog-title="skuCardData.skuCode || t('ecommerce.product.imageName')"
          />
          <div class="sku-card__head">
            <h3 class="sku-card__code">{{ skuCardData.skuCode || '—' }}</h3>
            <p class="sku-card__spec">{{ skuCardData.specName || '—' }}</p>
            <el-tag size="small" :type="skuStatusTagType">{{ skuStatusLabel }}</el-tag>
          </div>
        </div>
        <div class="sku-card__body">
          <div class="sku-card__item">
            <span class="sku-card__label">{{ t('ecommerce.product.skuRebatePct') }}</span>
            <span class="sku-card__value sku-card__value--accent">{{ formatPct(skuCardData.rebatePct) }}</span>
          </div>
          <div class="sku-card__item">
            <span class="sku-card__label">{{ t('ecommerce.product.salePrice') }}</span>
            <span class="sku-card__value sku-card__value--price">{{ formatPrice(skuCardData.salePrice) }}</span>
          </div>
          <div class="sku-card__item">
            <span class="sku-card__label">{{ t('ecommerce.product.productSize') }}</span>
            <span class="sku-card__value">{{ formatSize(skuCardData.productLengthCm, skuCardData.productWidthCm, skuCardData.productHeightCm) }}</span>
          </div>
          <div class="sku-card__item">
            <span class="sku-card__label">{{ t('ecommerce.product.unitWeight') }}</span>
            <span class="sku-card__value">{{ formatUnitWeight(skuCardData) }}</span>
          </div>
          <div class="sku-card__item">
            <span class="sku-card__label">{{ t('ecommerce.product.matchedCarton') }}</span>
            <span class="sku-card__value">{{ skuCardData.cartonName || '—' }}</span>
          </div>
          <div class="sku-card__item">
            <span class="sku-card__label">{{ t('ecommerce.product.cartonSize') }}</span>
            <span class="sku-card__value">{{ formatSize(skuCardData.cartonLengthCm, skuCardData.cartonWidthCm, skuCardData.cartonHeightCm) }}</span>
          </div>
          <div class="sku-card__item">
            <span class="sku-card__label">{{ t('ecommerce.product.cartonWeight') }}</span>
            <span class="sku-card__value">{{ formatCartonWeight(skuCardData) }}</span>
          </div>
          <div class="sku-card__item">
            <span class="sku-card__label">{{ t('ecommerce.product.unitsPerCarton') }}</span>
            <span class="sku-card__value">{{ skuCardData.unitsPerCarton ?? '—' }}</span>
          </div>
        </div>
      </div>
    </el-dialog>

    <el-dialog
      v-model="listImagePreviewVisible"
      :title="listImagePreviewTitle || t('ecommerce.product.imagePreview')"
      width="560px"
      append-to-body
      destroy-on-close
    >
      <div class="list-image-preview">
        <img
          v-if="listImagePreviewUrl && !listImagePreviewBroken"
          :src="listImagePreviewUrl"
          alt=""
          class="list-image-preview__img"
          @error="listImagePreviewBroken = true"
        />
        <div v-else class="list-image-preview__empty">
          <el-icon :size="48"><Picture /></el-icon>
          <p>{{ t('ecommerce.product.noImage') }}</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { TableColumnCtx } from 'element-plus'
import { Delete, Edit, Picture } from '@element-plus/icons-vue'
import { getEcommerceImageUrl } from '@/api/ecommerce/image'
import {
  createProduct,
  deleteProduct,
  emptySku,
  fetchProduct,
  fetchProducts,
  updateProduct,
  type EcProductListItem,
  type EcProductSaveRequest,
  type EcSku,
} from '@/api/ecommerce/product'
import { fetchFactoryOptions, type EcFactory } from '@/api/ecommerce/factory'
import { matchCarton } from '@/api/ecommerce/carton'
import EcImageField from '@/components/ecommerce/EcImageField.vue'
import TablePagination from '@/components/TablePagination.vue'
import { usePagination } from '@/composables/usePagination'
import { TABLE_ACTIONS_CELL_CLASS } from '@/constants/table'
import { formatDate } from '@/utils/date'
import ListingLinkPanel from './ListingLinkPanel.vue'
import { fetchListingLinksByProduct, type EcListingLink } from '@/api/ecommerce/listingLink'

const innerTab = ref('products')
const listingRef = ref<InstanceType<typeof ListingLinkPanel> | null>(null)

const { t } = useI18n()

const saving = ref(false)
const keyword = ref('')
const factoryOptions = ref<EcFactory[]>([])
const { page, pageSize, total, records, loading, load, onPageChange, onSizeChange } = usePagination(
  (p, ps) => fetchProducts(keyword.value.trim() || undefined, { page: p, pageSize: ps }),
)

const drawerVisible = ref(false)
const editingId = ref<number | null>(null)
const skuCardVisible = ref(false)
const skuCardData = ref<EcSku | null>(null)
const hoveredSkuRowIndex = ref<number | null>(null)
const listImageBrokenIds = ref<Set<number>>(new Set())
const listImagePreviewVisible = ref(false)
const listImagePreviewTitle = ref('')
const listImagePreviewUrl = ref('')
const listImagePreviewBroken = ref(false)
const productListingLinks = ref<EcListingLink[]>([])

const SKU_DELETE_CELL_CLASS = TABLE_ACTIONS_CELL_CLASS

const form = reactive<EcProductSaveRequest>({
  name: '',
  factoryId: null,
  description: '',
  rebatePct: 0,
  imageName: '',
  status: 'ENABLED',
  skus: [emptySku()],
})

const skuStatusLabel = computed(() => {
  const status = skuCardData.value?.status
  if (status === 'ON_SALE') return t('ecommerce.product.onSale')
  if (status === 'OFF_SALE') return t('ecommerce.product.offSale')
  if (status === 'DRAFT') return t('ecommerce.product.draft')
  return status || '—'
})

const skuStatusTagType = computed(() => {
  const status = skuCardData.value?.status
  if (status === 'ON_SALE') return 'success'
  if (status === 'OFF_SALE') return 'info'
  return 'warning'
})

function formatPct(value: number | null | undefined) {
  if (value == null) return '0%'
  return `${Number(value).toFixed(2)}%`
}

function isListImageVisible(row: EcProductListItem) {
  return Boolean(row.imageName?.trim()) && !listImageBrokenIds.value.has(row.id)
}

function onListImageError(id: number) {
  if (listImageBrokenIds.value.has(id)) return
  listImageBrokenIds.value = new Set([...listImageBrokenIds.value, id])
}

function openListImagePreview(row: EcProductListItem) {
  listImagePreviewTitle.value = row.name
  listImagePreviewUrl.value = getEcommerceImageUrl(row.imageName)
  listImagePreviewBroken.value = false
  listImagePreviewVisible.value = true
}

function formatPrice(value: number | null | undefined) {
  if (value == null) return '—'
  return `¥${Number(value).toFixed(2)}`
}

function formatSize(l?: number | null, w?: number | null, h?: number | null) {
  if (l == null && w == null && h == null) return '—'
  const parts = [l, w, h].map((v) => (v == null ? '—' : Number(v).toFixed(2)))
  return `${parts[0]} × ${parts[1]} × ${parts[2]} cm`
}

function formatCartonWeight(sku: EcSku) {
  const gross = sku.cartonGrossWeightKg
  const net = sku.cartonNetWeightKg
  if (gross == null && net == null) return '—'
  const grossText = gross == null ? '—' : `${Number(gross).toFixed(3)} kg`
  const netText = net == null ? '—' : `${Number(net).toFixed(3)} kg`
  return `${t('ecommerce.product.gross')} ${grossText} / ${t('ecommerce.product.net')} ${netText}`
}

function formatUnitWeight(sku: EcSku) {
  const gross = sku.cartonGrossWeightKg
  const units = sku.unitsPerCarton
  if (gross == null || units == null || units < 1) return '—'
  return `${(Number(gross) / Number(units)).toFixed(3)} kg`
}

function resetForm() {
  form.name = ''
  form.factoryId = null
  form.description = ''
  form.rebatePct = 0
  form.imageName = ''
  form.status = 'ENABLED'
  form.skus = [emptySku()]
}

async function loadFactoryOptions() {
  factoryOptions.value = await fetchFactoryOptions()
}

async function loadProducts() {
  await load()
}

let searchTimer: ReturnType<typeof setTimeout> | null = null
watch(keyword, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => load(true), 300)
})

let cartonMatchTimer: ReturnType<typeof setTimeout> | null = null
watch(
  () => form.skus.map((sku) => ({
    id: sku.id,
    l: sku.productLengthCm,
    w: sku.productWidthCm,
    h: sku.productHeightCm,
  })),
  () => {
    if (cartonMatchTimer) clearTimeout(cartonMatchTimer)
    cartonMatchTimer = setTimeout(autoMatchNewSkuCartons, 300)
  },
  { deep: true },
)

watch(() => form.factoryId, () => {
  if (cartonMatchTimer) clearTimeout(cartonMatchTimer)
  cartonMatchTimer = setTimeout(autoMatchNewSkuCartons, 300)
})

async function autoMatchNewSkuCartons() {
  for (const sku of form.skus) {
    if (sku.id) continue
    const l = sku.productLengthCm
    const w = sku.productWidthCm
    const h = sku.productHeightCm
    if (l == null || w == null || h == null || l <= 0 || w <= 0 || h <= 0) {
      sku.cartonId = null
      sku.cartonName = ''
      continue
    }
    const matched = await matchCarton(l, w, h, form.factoryId ?? undefined)
    sku.cartonId = matched?.id ?? null
    sku.cartonName = matched?.name ?? ''
  }
}

function openCreate() {
  editingId.value = null
  productListingLinks.value = []
  resetForm()
  drawerVisible.value = true
}

async function openEdit(id: number) {
  editingId.value = id
  const detail = await fetchProduct(id)
  form.name = detail.name
  form.factoryId = detail.factoryId ?? null
  form.description = detail.description || ''
  form.rebatePct = Number(detail.rebatePct ?? 0)
  form.imageName = detail.imageName || ''
  form.status = detail.status
  form.skus = detail.skus.length > 0
    ? detail.skus.map((sku: EcSku) => ({
        ...sku,
        rebatePct: sku.rebatePct ?? detail.rebatePct ?? 0,
      }))
    : [emptySku(Number(detail.rebatePct ?? 0))]
  drawerVisible.value = true
  productListingLinks.value = await fetchListingLinksByProduct(id)
}

function createListingFromProduct() {
  if (!editingId.value) return
  const skuCodes = form.skus.map((s) => s.skuCode?.trim()).filter((code): code is string => Boolean(code))
  innerTab.value = 'listing'
  drawerVisible.value = false
  listingRef.value?.openCreateFromProduct(editingId.value, form.name, skuCodes.length ? skuCodes : undefined)
}

function addSkuRow() {
  form.skus.push(emptySku(form.rebatePct ?? 0))
}

function onSkuRowClick(row: EcSku) {
  skuCardData.value = row
  skuCardVisible.value = true
}

function isSkuInteractiveTarget(target: EventTarget | null) {
  if (!(target instanceof HTMLElement)) return false
  return Boolean(target.closest(
    '.sku-cell-interactive, input, textarea, button, [role="button"], .el-input, .el-input-number, .el-input-number__increase, .el-input-number__decrease, .el-select, .el-select__wrapper, .ec-image-field',
  ))
}

function onSkuCellClick(
  row: EcSku,
  column: TableColumnCtx<EcSku>,
  _cell: HTMLTableCellElement,
  event: MouseEvent,
) {
  if (column.className === SKU_DELETE_CELL_CLASS) return
  if (isSkuInteractiveTarget(event.target)) return
  onSkuRowClick(row)
}

function skuRowClassName({ rowIndex }: { rowIndex: number }) {
  return rowIndex === hoveredSkuRowIndex.value ? 'sku-row-hover' : ''
}

function onSkuCellMouseEnter(
  row: EcSku,
  column: TableColumnCtx<EcSku>,
  _cell: HTMLTableCellElement,
) {
  if (column.className === SKU_DELETE_CELL_CLASS) {
    hoveredSkuRowIndex.value = null
    return
  }
  hoveredSkuRowIndex.value = form.skus.indexOf(row)
}

function onSkuCellMouseLeave(
  _row: EcSku,
  column: TableColumnCtx<EcSku>,
  cell: HTMLTableCellElement,
  event: MouseEvent,
) {
  if (column.className === SKU_DELETE_CELL_CLASS) {
    hoveredSkuRowIndex.value = null
    return
  }
  const related = event.relatedTarget as Node | null
  const rowEl = cell.closest('tr')
  if (!related || !rowEl?.contains(related)) {
    hoveredSkuRowIndex.value = null
  }
}

function removeSkuRow(index: number) {
  if (form.skus.length <= 1) return
  form.skus.splice(index, 1)
}

async function onSave() {
  if (!form.name.trim()) {
    ElMessage.warning(t('ecommerce.product.nameRequired'))
    return
  }
  if (form.skus.some((s) => !s.skuCode?.trim())) {
    ElMessage.warning(t('ecommerce.product.skuCodeRequired'))
    return
  }

  saving.value = true
  try {
    const payload: EcProductSaveRequest = {
      name: form.name.trim(),
      factoryId: form.factoryId ?? null,
      description: form.description?.trim() || undefined,
      rebatePct: form.rebatePct ?? 0,
      imageName: form.imageName?.trim() || undefined,
      status: form.status,
      skus: form.skus.map((sku) => sanitizeSku(sku)),
    }
    if (editingId.value) {
      await updateProduct(editingId.value, payload)
    } else {
      await createProduct(payload)
    }
    ElMessage.success(t('ecommerce.common.saved'))
    drawerVisible.value = false
    await loadProducts()
  } finally {
    saving.value = false
  }
}

function sanitizeSku(sku: EcSku): EcSku {
  return {
    ...sku,
    skuCode: sku.skuCode.trim(),
    specName: sku.specName?.trim() || undefined,
    rebatePct: sku.rebatePct ?? form.rebatePct ?? 0,
    imageName: sku.imageName?.trim() || undefined,
    cartonId: sku.cartonId ?? undefined,
    unitsPerCarton: sku.unitsPerCarton && sku.unitsPerCarton >= 1 ? sku.unitsPerCarton : 1,
  }
}

async function onDelete(row: EcProductListItem) {
  await ElMessageBox.confirm(
    t('ecommerce.product.deleteConfirm', { name: row.name }),
    { type: 'warning' },
  )
  await deleteProduct(row.id)
  ElMessage.success(t('ecommerce.common.deleted'))
  await loadProducts()
}

watch(innerTab, (tab) => {
  if (tab === 'listing') {
    listingRef.value?.loadListingLinks()
  }
})

onMounted(async () => {
  await Promise.all([loadProducts(), loadFactoryOptions()])
})

defineExpose({ loadProducts, openEdit })
</script>

<style scoped lang="scss">
.panel-toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.product-listing-links {
  margin-bottom: 16px;
}

.product-listing-table {
  margin-top: 8px;
}

.product-panel :deep(td.product-list-image-cell .cell) {
  overflow: hidden;
  padding: 6px 0 !important;
  line-height: 0;
  font-size: 0;
  text-overflow: clip;
  white-space: nowrap;
}

.product-list-thumb {
  width: 40px;
  height: 40px;
  margin: 0 auto;
  border-radius: 4px;
  background: var(--el-fill-color-light);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: zoom-in;
  overflow: hidden;
  color: var(--el-text-color-secondary);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
}

.list-image-preview {
  min-height: 280px;
  max-height: 60vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  overflow: hidden;
}

.list-image-preview__img {
  max-width: 100%;
  max-height: 60vh;
  object-fit: contain;
  display: block;
}

.list-image-preview__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--el-text-color-secondary);
  padding: 40px;
}

.product-form__image-top {
  margin-bottom: 8px;
}

.sku-table-wrap {
  overflow-x: auto;
}

.dim-row,
.weight-row {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.dim-unit {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.weight-row {
  flex-direction: column;
  align-items: stretch;
}

.sku-row-hint {
  margin: 0 0 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.sku-click-table :deep(.el-table__body tr:hover > td.el-table__cell) {
  background-color: var(--el-table-tr-bg-color, var(--el-fill-color-blank));
}

.sku-click-table :deep(.el-table__body tr.sku-row-hover > td.el-table__cell) {
  background-color: var(--el-table-row-hover-bg-color);
}

.sku-click-table :deep(.el-table__body tr.sku-row-hover) {
  cursor: pointer;
}

.sku-click-table :deep(.table-actions-cell) {
  cursor: default;
}

.sku-card {
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  overflow: hidden;
  background: linear-gradient(180deg, var(--el-fill-color-blank) 0%, var(--el-fill-color-light) 100%);
}

.sku-card__hero {
  display: flex;
  gap: 16px;
  align-items: center;
  padding: 20px;
  background: var(--el-color-primary-light-9);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.sku-card__head {
  min-width: 0;
}

.sku-card__code {
  margin: 0 0 4px;
  font-size: 20px;
  font-weight: 600;
  line-height: 1.3;
}

.sku-card__spec {
  margin: 0 0 8px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.sku-card__body {
  padding: 16px 20px 20px;
  display: grid;
  gap: 12px;
}

.sku-card__item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 12px;
  border-bottom: 1px dashed var(--el-border-color-lighter);
}

.sku-card__item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.sku-card__label {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  flex-shrink: 0;
}

.sku-card__value {
  text-align: right;
  font-size: 14px;
  word-break: break-all;
}

.sku-card__value--accent {
  color: var(--el-color-primary);
  font-weight: 600;
}

.sku-card__value--price {
  color: var(--el-color-warning);
  font-weight: 700;
  font-size: 16px;
}
</style>
