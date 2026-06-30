<template>
  <div class="product-panel">
    <ListingLinkPanel
      ref="listingDialogRef"
      class="product-listing-dialog-host"
      :show-list="false"
      @saved="onProductListingLinkSaved"
    />

    <el-tabs v-model="innerTab" class="product-panel__tabs">
      <el-tab-pane :label="t('ecommerce.product.tabProducts')" name="products">
        <div class="product-split">
          <!-- 左侧商品列表 -->
          <aside class="product-split__list">
            <div class="product-list-toolbar">
              <el-input
                v-model="keyword"
                :placeholder="t('ecommerce.product.searchPlaceholder')"
                clearable
              />
              <el-button type="primary" @click="openCreate">{{ t('ecommerce.product.add') }}</el-button>
            </div>

            <div v-loading="loading" class="product-list-scroll">
              <div
                v-for="row in records"
                :key="row.id"
                class="product-list-item"
                :class="{ 'product-list-item--active': editingId === row.id && detailOpen }"
                @click="openEdit(row.id)"
              >
                <div
                  class="product-list-item__thumb"
                  @click.stop="openListImagePreview(row)"
                >
                  <img
                    v-if="isListImageVisible(row)"
                    :src="getEcommerceImageUrl(row.imageName)"
                    alt=""
                    @error="onListImageError(row.id)"
                  />
                  <el-icon v-else :size="20"><Picture /></el-icon>
                </div>
                <div class="product-list-item__body">
                  <div class="product-list-item__title">{{ row.name }}</div>
                  <div class="product-list-item__meta">
                    <span v-if="row.factoryName">{{ row.factoryName }}</span>
                    <span v-else class="product-list-item__meta-muted">—</span>
                    <span class="product-list-item__meta-sep">·</span>
                    <span>{{ row.skuCount }} SKU</span>
                    <span class="product-list-item__meta-sep">·</span>
                    <span>{{ t('ecommerce.product.rebatePct') }} {{ formatPct(row.rebatePct) }}</span>
                  </div>
                </div>
                <el-tag
                  :type="row.status === 'ENABLED' ? 'success' : 'info'"
                  size="small"
                  class="product-list-item__status"
                >
                  {{ row.status === 'ENABLED' ? t('ecommerce.product.enabled') : t('ecommerce.product.disabled') }}
                </el-tag>
              </div>

              <el-empty
                v-if="!loading && records.length === 0"
                :description="t('ecommerce.product.selectProductHint')"
                :image-size="72"
              />
            </div>

            <TablePagination
              class="product-list-pagination"
              :page="page"
              :page-size="pageSize"
              :total="total"
              layout="total, prev, pager, next, jumper"
              @update:page="onPageChange"
            />
          </aside>

          <!-- 右侧详情 -->
          <main class="product-split__detail">
            <div v-if="!detailOpen" class="product-detail-empty">
              <el-empty :description="t('ecommerce.product.selectProductHint')" :image-size="100" />
            </div>

            <div v-else v-loading="detailLoading" class="product-detail">
              <header class="product-detail__header">
                <h2 class="product-detail__title">
                  {{ editingId ? (form.name || t('ecommerce.product.editTitle')) : t('ecommerce.product.createTitle') }}
                </h2>
                <div class="product-detail__actions">
                  <el-button v-if="editingId" link type="danger" :title="t('ecommerce.product.delete')" @click="onDeleteCurrent">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
              </header>

              <div class="product-detail__body">
                <el-form :model="form" label-width="96px" class="product-form">
                  <!-- SPU 信息 -->
                  <section class="product-section">
                    <h3 class="product-section__title product-section__title--blue">{{ t('ecommerce.product.spuSection') }}</h3>
                    <div class="spu-grid">
                      <div class="spu-grid__image">
                        <EcImageField
                          v-model="form.imageName"
                          size="large"
                          :dialog-title="form.name || t('ecommerce.product.imagePreview')"
                          @persist="persistProductAfterImageChange"
                        />
                      </div>
                      <div class="spu-grid__fields">
                        <el-form-item :label="t('ecommerce.product.name')" required>
                          <el-input v-model="form.name" />
                        </el-form-item>
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
                          <el-col :span="12">
                            <el-form-item :label="t('ecommerce.product.status')">
                              <el-radio-group v-model="form.status">
                                <el-radio value="ENABLED">{{ t('ecommerce.product.enabled') }}</el-radio>
                                <el-radio value="DISABLED">{{ t('ecommerce.product.disabled') }}</el-radio>
                              </el-radio-group>
                            </el-form-item>
                          </el-col>
                        </el-row>
                        <el-form-item :label="t('ecommerce.product.description')">
                          <el-input v-model="form.description" type="textarea" :rows="2" />
                        </el-form-item>
                      </div>
                    </div>
                  </section>

                  <!-- SKU 规格 -->
                  <section class="product-section">
                    <div class="product-section__head">
                      <h3 class="product-section__title product-section__title--blue">{{ t('ecommerce.product.skuSection') }}</h3>
                      <div class="sku-tabs-actions">
                        <el-dropdown trigger="click" @command="onAddSkuCommand">
                          <el-button type="primary" link>
                            <el-icon><Plus /></el-icon>
                            {{ t('ecommerce.product.addSku') }}
                            <el-icon class="sku-add-dropdown-icon"><ArrowDown /></el-icon>
                          </el-button>
                          <template #dropdown>
                            <el-dropdown-menu>
                              <el-dropdown-item command="copy">
                                {{ t('ecommerce.product.copyCurrentSku') }}
                              </el-dropdown-item>
                              <el-dropdown-item command="new">
                                {{ t('ecommerce.product.addNewSku') }}
                              </el-dropdown-item>
                            </el-dropdown-menu>
                          </template>
                        </el-dropdown>
                        <el-button
                          v-if="form.skus.length > 1"
                          link
                          type="danger"
                          @click="removeActiveSku"
                        >
                          {{ t('ecommerce.product.removeCurrentSku') }}
                        </el-button>
                      </div>
                    </div>

                    <div class="sku-picker">
                      <button
                        v-for="(sku, index) in form.skus"
                        :key="index"
                        type="button"
                        class="sku-picker__item"
                        :class="{ 'sku-picker__item--active': activeSkuTab === String(index) }"
                        @click="activeSkuTab = String(index)"
                      >
                        {{ skuPickerLabel(sku, index) }}
                      </button>
                    </div>

                    <template v-if="activeSku">
                      <div class="sku-spec-rows">
                        <!-- 销售信息 -->
                        <section class="sku-spec-block">
                          <div class="sku-spec-block__head sku-spec-block__head--with-action">
                            <h4 class="sku-spec-col__title">{{ t('ecommerce.product.skuSectionSales') }}</h4>
                            <el-tooltip :content="t('ecommerce.product.generateSkuCard')" placement="top">
                              <el-button
                                link
                                type="primary"
                                class="sku-card-generate-btn"
                                @click="skuCardVisible = true"
                              >
                                <el-icon :size="18"><Picture /></el-icon>
                              </el-button>
                            </el-tooltip>
                          </div>
                          <div class="sku-spec-block__body">
                            <div class="sku-sales-grid">
                              <div class="sku-sales-grid__image">
                                <EcImageField
                                  v-model="activeSku.imageName"
                                  size="medium"
                                  :dialog-title="activeSku.skuCode || t('ecommerce.product.imagePreview')"
                                  @persist="persistProductAfterImageChange"
                                />
                              </div>
                              <div class="sku-sales-grid__fields">
                                <el-row :gutter="16">
                                  <el-col :span="12">
                                    <el-form-item :label="t('ecommerce.product.skuCode')" required label-width="108px" class="sku-form-label-right">
                                      <el-input v-model="activeSku.skuCode">
                                        <template #suffix>
                                          <el-icon
                                            class="sku-code-copy"
                                            :class="{ 'sku-code-copy--disabled': !activeSku.skuCode?.trim() }"
                                            :title="t('ecommerce.product.copySkuCode')"
                                            @click.stop="copyActiveSkuCode"
                                          >
                                            <DocumentCopy />
                                          </el-icon>
                                        </template>
                                      </el-input>
                                    </el-form-item>
                                  </el-col>
                                  <el-col :span="12">
                                    <el-form-item :label="t('ecommerce.product.specName')" label-width="108px" class="sku-form-label-right">
                                      <el-input v-model="activeSku.specName" />
                                    </el-form-item>
                                  </el-col>
                                  <el-col :span="12">
                                    <el-form-item :label="t('ecommerce.product.salePrice')" label-width="108px" class="sku-form-label-right">
                                      <el-input-number
                                        v-model="activeSku.salePrice"
                                        :min="0"
                                        :precision="2"
                                        controls-position="right"
                                        style="width: 100%"
                                      />
                                    </el-form-item>
                                  </el-col>
                                  <el-col :span="12">
                                    <el-form-item
                                      :label="t('ecommerce.product.skuRebatePct')"
                                      label-width="108px"
                                      class="sku-field-nowrap sku-form-label-right"
                                    >
                                      <el-input-number
                                        v-model="activeSku.rebatePct"
                                        :min="0"
                                        :max="100"
                                        :precision="2"
                                        :step="0.1"
                                        controls-position="right"
                                        style="width: 100%"
                                      />
                                    </el-form-item>
                                  </el-col>
                                  <el-col :span="12">
                                    <el-form-item
                                      :label="t('ecommerce.product.skuStatus')"
                                      label-width="108px"
                                      class="sku-field-nowrap sku-form-label-right"
                                    >
                                      <el-radio-group v-model="activeSku.status" size="small" class="sku-status-group">
                                        <el-radio-button value="ON_SALE">{{ t('ecommerce.product.onSale') }}</el-radio-button>
                                        <el-radio-button value="OFF_SALE">{{ t('ecommerce.product.offSale') }}</el-radio-button>
                                        <el-radio-button value="DRAFT">{{ t('ecommerce.product.draft') }}</el-radio-button>
                                      </el-radio-group>
                                    </el-form-item>
                                  </el-col>
                                </el-row>
                              </div>
                            </div>
                          </div>
                        </section>

                        <!-- 单品尺寸 -->
                        <section class="sku-spec-block">
                          <div class="sku-spec-block__head">
                            <div class="sku-spec-col__title-wrap">
                              <h4 class="sku-spec-col__title">{{ t('ecommerce.product.skuSectionDimensions') }}</h4>
                              <span class="sku-spec-col__hint">{{ t('ecommerce.product.skuDimensionsHint') }}</span>
                            </div>
                          </div>
                          <div class="sku-spec-block__body">
                            <div class="sku-dimensions-stack">
                              <div class="sku-inline-field sku-dimensions-stack__row">
                                <span class="sku-inline-field__label">{{ t('ecommerce.product.productSize') }}</span>
                                <div class="dim-row dim-row--compact">
                                  <el-input-number
                                    v-model="activeSku.productLengthCm"
                                    :min="0"
                                    :precision="2"
                                    controls-position="right"
                                    placeholder="L"
                                    class="sku-dim-input"
                                  />
                                  <span>×</span>
                                  <el-input-number
                                    v-model="activeSku.productWidthCm"
                                    :min="0"
                                    :precision="2"
                                    controls-position="right"
                                    placeholder="W"
                                    class="sku-dim-input"
                                  />
                                  <span>×</span>
                                  <el-input-number
                                    v-model="activeSku.productHeightCm"
                                    :min="0"
                                    :precision="2"
                                    controls-position="right"
                                    placeholder="H"
                                    class="sku-dim-input"
                                  />
                                  <span class="dim-unit">cm</span>
                                </div>
                              </div>
                              <div class="sku-inline-field sku-dimensions-stack__row">
                                <span class="sku-inline-field__label">{{ t('ecommerce.product.unitWeight') }}</span>
                                <el-tag
                                  v-if="activeUnitWeightKg != null"
                                  class="sku-weight-tag"
                                  :style="unitWeightTagStyle(activeUnitWeightKg)"
                                  round
                                >
                                  {{ formatUnitWeightLabel(activeUnitWeightKg) }}
                                </el-tag>
                                <span v-else class="sku-readonly-value">—</span>
                              </div>
                              <div
                                class="sku-carton-match-card sku-dimensions-stack__row"
                                :class="{ 'sku-carton-match-card--matched': Boolean(activeSku.cartonName?.trim()) }"
                              >
                                <el-icon class="sku-carton-match-card__icon">
                                  <CircleCheckFilled v-if="activeSku.cartonName?.trim()" />
                                  <InfoFilled v-else />
                                </el-icon>
                                <div class="sku-carton-match-card__body">
                                  <p class="sku-carton-match-card__hint">
                                    {{
                                      activeSku.cartonName?.trim()
                                        ? t('ecommerce.product.cartonMatchAutoHint')
                                        : t('ecommerce.product.cartonMatchHint')
                                    }}
                                  </p>
                                  <p v-if="activeSku.cartonName?.trim()" class="sku-carton-match-card__hint sku-carton-match-card__hint--follow">
                                    {{ t('ecommerce.product.matchedCartonLabel', { name: activeSku.cartonName }) }}
                                  </p>
                                </div>
                              </div>
                            </div>
                          </div>
                        </section>

                        <!-- 包装信息 -->
                        <section class="sku-spec-block">
                          <div class="sku-spec-block__head">
                            <h4 class="sku-spec-col__title">{{ t('ecommerce.product.skuSectionPackaging') }}</h4>
                          </div>
                          <div class="sku-spec-block__body">
                            <div class="sku-packaging-stack">
                              <div class="sku-inline-field sku-packaging-stack__row">
                                <span class="sku-inline-field__label">{{ t('ecommerce.product.cartonSize') }}</span>
                                <div class="dim-row dim-row--compact">
                                  <el-input-number
                                    v-model="activeSku.cartonLengthCm"
                                    :min="0"
                                    :precision="2"
                                    controls-position="right"
                                    placeholder="L"
                                    class="sku-dim-input"
                                  />
                                  <span>×</span>
                                  <el-input-number
                                    v-model="activeSku.cartonWidthCm"
                                    :min="0"
                                    :precision="2"
                                    controls-position="right"
                                    placeholder="W"
                                    class="sku-dim-input"
                                  />
                                  <span>×</span>
                                  <el-input-number
                                    v-model="activeSku.cartonHeightCm"
                                    :min="0"
                                    :precision="2"
                                    controls-position="right"
                                    placeholder="H"
                                    class="sku-dim-input"
                                  />
                                  <span class="dim-unit">cm</span>
                                </div>
                              </div>
                              <div class="sku-inline-field sku-packaging-stack__row">
                                <span class="sku-inline-field__label">{{ t('ecommerce.product.gross') }}</span>
                                <el-input-number
                                  v-model="activeSku.cartonGrossWeightKg"
                                  :min="0"
                                  :precision="3"
                                  controls-position="right"
                                  class="sku-dim-input"
                                />
                              </div>
                              <div class="sku-inline-field sku-packaging-stack__row">
                                <span class="sku-inline-field__label">{{ t('ecommerce.product.net') }}</span>
                                <el-input-number
                                  v-model="activeSku.cartonNetWeightKg"
                                  :min="0"
                                  :precision="3"
                                  controls-position="right"
                                  class="sku-dim-input"
                                />
                              </div>
                              <div class="sku-inline-field sku-packaging-stack__row">
                                <span class="sku-inline-field__label">{{ t('ecommerce.product.unitsPerCarton') }}</span>
                                <el-input-number
                                  v-model="activeSku.unitsPerCarton"
                                  :min="1"
                                  controls-position="right"
                                  class="sku-dim-input"
                                />
                              </div>
                            </div>
                          </div>
                        </section>
                      </div>
                    </template>
                  </section>

                  <!-- 上架链接 -->
                  <section v-if="editingId" class="product-section">
                    <div class="product-section__head">
                      <h3 class="product-section__title product-section__title--blue">
                        {{ t('ecommerce.product.listingLinksSection') }}
                        <el-tag size="small" type="info" round class="product-section__count">
                          {{ productListingLinks.length }}
                        </el-tag>
                      </h3>
                      <el-button type="primary" link @click="createListingFromProduct">
                        {{ t('ecommerce.product.createListingLink') }}
                      </el-button>
                    </div>
                    <div class="product-listing-links">
                      <el-table
                        v-if="productListingLinks.length"
                        :data="productListingLinks"
                        size="small"
                        border
                        stripe
                        class="product-listing-table"
                      >
                        <el-table-column prop="name" :label="t('ecommerce.listingLink.name')" min-width="120">
                          <template #default="{ row }">
                            <el-button link type="primary" @click.stop="openListingLinkDetail(row.id)">
                              {{ row.name }}
                            </el-button>
                          </template>
                        </el-table-column>
                        <el-table-column prop="shopName" :label="t('ecommerce.listingLink.shop')" width="120" />
                        <el-table-column prop="platformName" :label="t('ecommerce.listingLink.platform')" width="100" />
                        <el-table-column :label="t('ecommerce.listingLink.listingTime')" width="110">
                          <template #default="{ row }">{{ formatDate(row.listingTime) }}</template>
                        </el-table-column>
                      </el-table>
                      <el-empty v-else :description="t('ecommerce.product.noListingLinks')" :image-size="64" />
                    </div>
                  </section>
                </el-form>
              </div>

              <footer class="product-detail__footer">
                <el-button @click="onCancel">{{ t('ecommerce.common.cancel') }}</el-button>
                <el-button type="primary" :loading="saving" @click="onSave">{{ t('ecommerce.common.save') }}</el-button>
              </footer>
            </div>
          </main>
        </div>
      </el-tab-pane>

      <el-tab-pane :label="t('ecommerce.listingLink.tabTitle')" name="listing">
        <ListingLinkPanel ref="listingRef" @saved="onProductListingLinkSaved" />
      </el-tab-pane>
    </el-tabs>

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

    <ListingLinkDetailDrawer
      v-model="listingDetailVisible"
      :link-id="listingDetailLinkId"
      @edit="onListingLinkEdit"
    />

    <SkuDetailCardDialog
      v-model="skuCardVisible"
      :sku="activeSku"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, CircleCheckFilled, Delete, DocumentCopy, InfoFilled, Picture, Plus } from '@element-plus/icons-vue'
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
import SkuDetailCardDialog from '@/components/ecommerce/SkuDetailCardDialog.vue'
import TablePagination from '@/components/TablePagination.vue'
import { usePagination } from '@/composables/usePagination'
import { formatDate } from '@/utils/date'
import ListingLinkPanel from './ListingLinkPanel.vue'
import ListingLinkDetailDrawer from './ListingLinkDetailDrawer.vue'
import { fetchListingLinksByProduct, type EcListingLink } from '@/api/ecommerce/listingLink'

const innerTab = ref('products')
const listingRef = ref<InstanceType<typeof ListingLinkPanel> | null>(null)
const listingDialogRef = ref<InstanceType<typeof ListingLinkPanel> | null>(null)

import { useEcSettingsStore } from '@/stores/ecSettings'

const { t } = useI18n()
const route = useRoute()
const ecSettings = useEcSettingsStore()

const saving = ref(false)
const keyword = ref('')
const factoryOptions = ref<EcFactory[]>([])
const { page, pageSize, total, records, loading, load, onPageChange } = usePagination(
  (p, ps) => fetchProducts(keyword.value.trim() || undefined, { page: p, pageSize: ps }),
)

const detailOpen = ref(false)
const detailLoading = ref(false)
const editingId = ref<number | null>(null)
const activeSkuTab = ref('0')
const listImageBrokenIds = ref<Set<number>>(new Set())
const listImagePreviewVisible = ref(false)
const listImagePreviewTitle = ref('')
const listImagePreviewUrl = ref('')
const listImagePreviewBroken = ref(false)
const productListingLinks = ref<EcListingLink[]>([])
const listingDetailVisible = ref(false)
const listingDetailLinkId = ref<number | null>(null)
const skuCardVisible = ref(false)
const pendingInitialSelect = ref(true)

const form = reactive<EcProductSaveRequest>({
  name: '',
  factoryId: null,
  description: '',
  rebatePct: 0,
  imageName: '',
  status: 'ENABLED',
  skus: [emptySku()],
})

const activeSkuIndex = computed(() => {
  const index = Number(activeSkuTab.value)
  return Number.isFinite(index) ? index : 0
})

const activeSku = computed(() => form.skus[activeSkuIndex.value] ?? null)

const activeUnitWeightKg = computed(() => {
  const sku = activeSku.value
  return sku ? getUnitWeightKg(sku) : null
})

function skuPickerLabel(sku: EcSku, index: number) {
  const spec = sku.specName?.trim()
  if (spec) return spec
  const code = sku.skuCode?.trim()
  if (code) return code
  return t('ecommerce.product.skuTabFallback', { n: index + 1 })
}

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

/** 单品重量标签色阶上限（kg），越接近上限颜色越深 */
const UNIT_WEIGHT_COLOR_MAX_KG = 2.5

function getUnitWeightKg(sku: EcSku): number | null {
  const gross = sku.cartonGrossWeightKg
  const units = sku.unitsPerCarton
  if (gross == null || units == null || units < 1) return null
  const kg = Number(gross) / Number(units)
  if (!Number.isFinite(kg) || kg < 0) return null
  return kg
}

function formatUnitWeightLabel(kg: number | null): string {
  if (kg == null) return '—'
  return `${kg.toFixed(3)} kg`
}

function unitWeightTagStyle(kg: number | null): Record<string, string> {
  if (kg == null) return {}
  const ratio = Math.min(1, Math.max(0, kg / UNIT_WEIGHT_COLOR_MAX_KG))
  const lightness = 88 - ratio * 52
  const saturation = 28 + ratio * 42
  const hue = 215
  const borderLightness = Math.max(lightness - 10, 24)
  const textColor = ratio >= 0.45 ? '#ffffff' : `hsl(${hue}, ${Math.min(saturation + 12, 75)}%, 28%)`
  return {
    backgroundColor: `hsl(${hue}, ${saturation}%, ${lightness}%)`,
    borderColor: `hsl(${hue}, ${saturation}%, ${borderLightness}%)`,
    color: textColor,
  }
}

async function copyActiveSkuCode() {
  const code = activeSku.value?.skuCode?.trim()
  if (!code) return
  try {
    await navigator.clipboard.writeText(code)
    ElMessage.success(t('ecommerce.product.copySkuCodeSuccess'))
  } catch {
    ElMessage.warning(t('ecommerce.product.copySkuCodeFailed'))
  }
}

function resetForm() {
  form.name = ''
  form.factoryId = null
  form.description = ''
  form.rebatePct = Number(ecSettings.rebate.defaultRebatePct ?? 0)
  form.imageName = ''
  form.status = 'ENABLED'
  form.skus = [emptySku(form.rebatePct ?? 0)]
  activeSkuTab.value = '0'
}

async function loadFactoryOptions() {
  factoryOptions.value = await fetchFactoryOptions('PRODUCTION')
}

async function loadProducts() {
  await load()
  await ensureDefaultProductSelected()
}

async function ensureDefaultProductSelected() {
  if (!pendingInitialSelect.value) return
  if (route.query.editId) {
    pendingInitialSelect.value = false
    return
  }
  if (detailOpen.value || detailLoading.value) return
  const first = records.value[0]
  if (!first) return
  pendingInitialSelect.value = false
  await openEdit(first.id)
}

async function selectFirstProductIfAny() {
  const first = records.value[0]
  if (!first) {
    detailOpen.value = false
    editingId.value = null
    return
  }
  await openEdit(first.id)
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
  pendingInitialSelect.value = false
  editingId.value = null
  productListingLinks.value = []
  resetForm()
  detailOpen.value = true
}

async function openEdit(id: number) {
  if (detailLoading.value) return
  pendingInitialSelect.value = false
  editingId.value = id
  detailOpen.value = true
  detailLoading.value = true
  try {
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
    activeSkuTab.value = '0'
    productListingLinks.value = await fetchListingLinksByProduct(id)
  } finally {
    detailLoading.value = false
  }
}

function onCancel() {
  detailOpen.value = false
  editingId.value = null
  productListingLinks.value = []
}

function createListingFromProduct() {
  if (!editingId.value) return
  const skuCodes = form.skus.map((s) => s.skuCode?.trim()).filter((code): code is string => Boolean(code))
  listingDialogRef.value?.openCreateFromProduct(editingId.value, form.name, skuCodes.length ? skuCodes : undefined)
}

function openListingLinkDetail(id: number) {
  listingDetailLinkId.value = id
  listingDetailVisible.value = true
}

async function onListingLinkEdit(id: number) {
  await listingDialogRef.value?.openEdit(id)
}

async function onProductListingLinkSaved() {
  if (editingId.value) {
    productListingLinks.value = await fetchListingLinksByProduct(editingId.value)
  }
  listingRef.value?.loadListingLinks()
}

function onAddSkuCommand(command: string) {
  if (command === 'copy') {
    copyCurrentSku()
  } else {
    addNewSku()
  }
}

function addNewSku() {
  form.skus.push(emptySku(form.rebatePct ?? 0))
  activeSkuTab.value = String(form.skus.length - 1)
}

function copyCurrentSku() {
  const source = activeSku.value ?? form.skus[0]
  if (!source) {
    addNewSku()
    return
  }
  form.skus.push({
    ...source,
    id: undefined,
    productId: undefined,
    skuCode: '',
  })
  activeSkuTab.value = String(form.skus.length - 1)
}

function removeActiveSku() {
  if (form.skus.length <= 1) return
  const index = activeSkuIndex.value
  form.skus.splice(index, 1)
  const nextIndex = Math.min(index, form.skus.length - 1)
  activeSkuTab.value = String(nextIndex)
}

async function onSave(options?: { silent?: boolean }) {
  if (!form.name.trim()) {
    ElMessage.warning(t('ecommerce.product.nameRequired'))
    return false
  }
  if (form.skus.some((s) => !s.skuCode?.trim())) {
    ElMessage.warning(t('ecommerce.product.skuCodeRequired'))
    return false
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
      const created = await createProduct(payload)
      editingId.value = created.id
      productListingLinks.value = await fetchListingLinksByProduct(created.id)
    }
    if (!options?.silent) {
      ElMessage.success(t('ecommerce.common.saved'))
    }
    await load()
    return true
  } finally {
    saving.value = false
  }
}

async function persistProductAfterImageChange() {
  if (!detailOpen.value || saving.value) return
  if (!form.name.trim()) {
    ElMessage.warning(t('ecommerce.product.imagePersistNameRequired'))
    return
  }
  if (form.skus.some((s) => !s.skuCode?.trim())) {
    ElMessage.warning(t('ecommerce.product.imagePersistSkuRequired'))
    return
  }
  const saved = await onSave({ silent: true })
  if (saved) {
    ElMessage.success(t('ecommerce.product.imagePersisted'))
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

async function onDeleteCurrent() {
  if (!editingId.value) return
  const name = form.name
  await ElMessageBox.confirm(
    t('ecommerce.product.deleteConfirm', { name }),
    { type: 'warning' },
  )
  await deleteProduct(editingId.value)
  ElMessage.success(t('ecommerce.common.deleted'))
  detailOpen.value = false
  editingId.value = null
  productListingLinks.value = []
  await load()
  await selectFirstProductIfAny()
}

watch(innerTab, (tab) => {
  if (tab === 'listing') {
    listingRef.value?.loadListingLinks()
  }
})

watch(
  () => form.skus.length,
  (len) => {
    if (len === 0) {
      form.skus.push(emptySku(form.rebatePct ?? 0))
      activeSkuTab.value = '0'
      return
    }
    const index = Number(activeSkuTab.value)
    if (!Number.isFinite(index) || index >= len) {
      activeSkuTab.value = String(Math.max(0, len - 1))
    }
  },
)

onMounted(async () => {
  await ecSettings.ensureLoaded()
  await Promise.all([loadProducts(), loadFactoryOptions()])
})

defineExpose({ loadProducts, openEdit })
</script>

<style scoped lang="scss">
.product-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.product-panel__tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;

  :deep(.el-tabs__header) {
    margin-bottom: 12px;
    flex-shrink: 0;
  }

  :deep(.el-tabs__content) {
    flex: 1;
    min-height: 0;
  }

  :deep(.el-tab-pane) {
    height: 100%;
  }
}

.product-split {
  display: flex;
  height: 100%;
  min-height: 0;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  overflow: hidden;
  background: var(--el-bg-color);
}

.product-split__list {
  width: 35%;
  min-width: 260px;
  max-width: 380px;
  border-right: 1px solid var(--el-border-color-light);
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: var(--el-fill-color-blank);
}

.product-list-toolbar {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  flex-shrink: 0;
}

.product-list-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 8px;
}

.product-list-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  border-left: 3px solid transparent;
  transition: background-color 0.15s;

  &:hover {
    background: var(--el-fill-color-light);
  }

  &--active {
    background: var(--el-color-primary-light-9);
    border-left-color: var(--el-color-primary);
  }
}

.product-list-item__thumb {
  width: 48px;
  height: 48px;
  flex-shrink: 0;
  border-radius: 6px;
  background: var(--el-fill-color-light);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  color: var(--el-text-color-secondary);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
}

.product-list-item__body {
  flex: 1;
  min-width: 0;
}

.product-list-item__title {
  font-weight: 600;
  font-size: 14px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-list-item__meta {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-list-item__meta-muted {
  color: var(--el-text-color-placeholder);
}

.product-list-item__meta-sep {
  margin: 0 4px;
}

.product-list-item__status {
  flex-shrink: 0;
}

.product-list-pagination {
  flex-shrink: 0;
  padding: 8px 12px;
  border-top: 1px solid var(--el-border-color-lighter);

  :deep(.el-pagination) {
    flex-wrap: wrap;
    justify-content: center;
    row-gap: 6px;
  }
}

.product-split__detail {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.product-detail-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-detail {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.product-detail__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 20px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  flex-shrink: 0;
}

.product-detail__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-detail__body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 16px 20px;
}

.product-detail__footer {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 20px;
  border-top: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-blank);
}

.product-section {
  margin-bottom: 24px;

  &:last-child {
    margin-bottom: 0;
  }
}

.product-section__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.product-section__title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);

  &--blue::before,
  &--orange::before {
    content: '';
    width: 3px;
    height: 14px;
    border-radius: 2px;
    flex-shrink: 0;
  }

  &--blue::before {
    background: var(--el-color-primary);
  }

  &--orange::before {
    background: var(--el-color-warning);
  }
}

.product-section__head .product-section__title {
  margin-bottom: 0;
}

.product-section__count {
  margin-left: 4px;
  font-weight: normal;
}

.spu-grid {
  display: grid;
  grid-template-columns: 160px 1fr;
  gap: 20px;
  align-items: start;
}

.spu-grid__image {
  display: flex;
  justify-content: center;
}

.sku-tabs-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.sku-add-dropdown-icon {
  margin-left: 2px;
  font-size: 12px;
}

.sku-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.sku-picker__item {
  padding: 8px 16px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  background: var(--el-fill-color-blank);
  font-size: 14px;
  line-height: 1.4;
  color: var(--el-text-color-regular);
  cursor: pointer;
  transition:
    border-color 0.15s,
    color 0.15s,
    background 0.15s;

  &:hover {
    border-color: var(--el-color-primary-light-5);
    color: var(--el-color-primary);
  }

  &--active {
    border-color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
    color: var(--el-color-primary);
    font-weight: 500;
  }
}

.sku-spec-rows {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.sku-spec-block {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  overflow: hidden;
  background: var(--el-fill-color-blank);
}

.sku-spec-block__head {
  padding: 12px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-light);

  &--with-action {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }
}

.sku-card-generate-btn {
  flex-shrink: 0;
  padding: 4px;

  .el-icon {
    font-size: 18px;
  }
}

.sku-spec-block__body {
  padding: 16px;
}

.sku-spec-col__title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  display: flex;
  align-items: center;
  gap: 8px;
  line-height: 1.4;

  &::before {
    content: '';
    width: 3px;
    height: 14px;
    border-radius: 2px;
    flex-shrink: 0;
    background: var(--el-color-warning);
  }
}

.sku-spec-col__title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  min-width: 0;
}

.sku-spec-col__hint {
  font-size: 12px;
  font-weight: normal;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
}

.sku-sales-grid {
  display: grid;
  grid-template-columns: 120px 1fr;
  gap: 20px;
  align-items: start;
}

.sku-sales-grid__image {
  display: flex;
  justify-content: center;
}

.sku-form-label-right :deep(.el-form-item__label) {
  text-align: right;
  justify-content: flex-end;
}

.sku-dimensions-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: flex-start;
  width: 100%;
}

.sku-dimensions-stack__row {
  width: 100%;
}

.sku-packaging-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: flex-start;
  width: 100%;
}

.sku-packaging-stack__row {
  width: 100%;
}

.sku-inline-field {
  display: grid;
  grid-template-columns: 108px minmax(0, 1fr);
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.sku-inline-field__label {
  text-align: right;
  white-space: nowrap;
  font-size: 14px;
  color: var(--el-text-color-regular);
}

.sku-dim-input {
  width: 120px;

  :deep(.el-input__wrapper) {
    padding-left: 8px;
    padding-right: 8px;
  }

  :deep(.el-input__inner) {
    text-align: left;
  }
}

.dim-row--compact {
  flex-wrap: nowrap;
}

.sku-carton-match-card {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 8px;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  box-sizing: border-box;

  &--matched {
    background: var(--el-color-primary-light-9);
    border-color: var(--el-color-primary-light-7);

    .sku-carton-match-card__icon {
      color: var(--el-color-primary);
    }

    .sku-carton-match-card__hint {
      color: var(--el-color-primary);
    }
  }
}

.sku-carton-match-card__icon {
  flex-shrink: 0;
  font-size: 20px;
  margin-top: 1px;
  color: var(--el-text-color-secondary);
}

.sku-carton-match-card__body {
  min-width: 0;
}

.sku-carton-match-card__hint {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--el-text-color-secondary);

  &--follow {
    margin-top: 4px;
  }
}

.sku-code-copy {
  cursor: pointer;
  color: var(--el-text-color-secondary);
  transition: color 0.15s;

  &:hover {
    color: var(--el-color-primary);
  }

  &--disabled {
    cursor: not-allowed;
    opacity: 0.35;

    &:hover {
      color: var(--el-text-color-secondary);
    }
  }
}

.sku-field-nowrap :deep(.el-form-item__label) {
  white-space: nowrap;
}

.sku-input-compact {
  width: 112px;
  max-width: 100%;
}

.sku-status-group {
  flex-wrap: wrap;
}

.sku-readonly-value {
  font-size: 14px;
  color: var(--el-text-color-regular);
}

.sku-weight-tag {
  width: fit-content;
  max-width: 100%;
  justify-self: start;
  border: 1px solid transparent;
  font-weight: 600;
  font-variant-numeric: tabular-nums;

  :deep(.el-tag__content) {
    line-height: 1.2;
  }
}

.dim-row,
.weight-row {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.dim-unit {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.weight-row {
  flex-direction: column;
  align-items: stretch;
  gap: 8px;
}

.product-listing-links {
  padding-top: 0;
}

.product-listing-table {
  margin-top: 8px;
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

@media (max-width: 900px) {
  .product-split {
    flex-direction: column;
  }

  .product-split__list {
    width: 100%;
    max-width: none;
    max-height: 40vh;
    border-right: none;
    border-bottom: 1px solid var(--el-border-color-light);
  }

  .spu-grid {
    grid-template-columns: 1fr;
  }

  .sku-sales-grid {
    grid-template-columns: 1fr;
  }
}
</style>
