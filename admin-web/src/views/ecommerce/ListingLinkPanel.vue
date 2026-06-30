<template>
  <div class="listing-link-panel" :class="{ 'listing-link-panel--dialog-only': !showList }">
    <template v-if="showList">
    <div class="link-list-surface" @click="onListSurfaceClick">
    <div class="link-panel-search">
      <el-input
        v-model="keyword"
        :placeholder="t('ecommerce.listingLink.searchPlaceholder')"
        clearable
        class="link-panel-search__input"
      />
      <el-select
        v-model="shopId"
        clearable
        filterable
        :placeholder="t('ecommerce.listingLink.shop')"
        class="link-panel-search__shop"
        @change="onShopFilterChange"
      >
        <el-option v-for="s in filteredShopOptions" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
      <el-button
        class="link-panel-search__recalc"
        :loading="listPricingRecalculating"
        @click.stop="onRecalcAllLinksPricing"
      >
        <el-icon><Refresh /></el-icon>
        {{ t('ecommerce.listingLink.recalcPricing') }}
      </el-button>
    </div>

    <el-tabs v-model="activePlatformTab" class="link-platform-tabs" @tab-change="onPlatformTabChange">
      <el-tab-pane :label="platformTabLabel('all')" name="all" />
      <el-tab-pane
        v-for="p in platformsWithShops"
        :key="p.id"
        :label="platformTabLabel(String(p.id), p.name)"
        :name="String(p.id)"
      />
    </el-tabs>

    <div v-loading="loading" class="link-card-grid">
      <article
        v-for="row in records"
        :key="row.id"
        class="link-card"
        :class="{ 'is-selected': isLinkCardSelected(row.id) }"
        @click.stop="openDetail(row.id)"
      >
        <div class="link-card__platform" :style="platformStripStyle(row)">
          <div class="link-card__platform-leading">
            <img
              :src="linkCardPlatformIcon(row).src"
              alt=""
              class="link-card__platform-icon"
              @error="onLinkCardPlatformIconError(row)"
            />
            <span class="link-card__platform-name">{{ row.platformName || t('ecommerce.listingLink.platform') }}</span>
          </div>
          <el-button
            link
            class="link-card__copy"
            :title="t('ecommerce.listingLink.copyLink')"
            @click.stop="onCopyLink(row)"
          >
            <el-icon :size="16"><DocumentCopy /></el-icon>
          </el-button>
        </div>

        <div class="link-card__body">
          <h3 class="link-card__title" :title="row.name">{{ row.name }}</h3>
          <p class="link-card__shop">
            <img
              :src="linkCardShopIcon(row).src"
              alt=""
              class="link-card__shop-icon"
              :class="{ 'is-avatar': linkCardShopIcon(row).isCustomAvatar }"
            />
            <span class="link-card__shop-name">{{ row.shopName || '—' }}</span>
          </p>
          <p class="link-card__date">{{ formatDate(row.listingTime) }}</p>

          <div class="link-card__sku-chips">
            <template v-if="cardSkuPreviews(row).length">
              <span
                v-for="sku in cardSkuPreviews(row)"
                :key="sku.id ?? sku.skuCodes"
                class="link-card__sku-chip"
                :class="profitChipClass(sku)"
              >
                <span class="link-card__sku-chip-name">{{ sku.skuName || sku.skuCodes }}</span>
                <span class="link-card__sku-chip-profit">{{ formatProfitChip(sku.profit) }}</span>
              </span>
              <span v-if="cardSkuOverflow(row) > 0" class="link-card__sku-more">
                <span class="link-card__sku-ellipsis">…</span>
                <span>+{{ cardSkuOverflow(row) }}</span>
              </span>
            </template>
          </div>
        </div>

        <div class="link-card__footer" @click.stop>
          <div class="link-card__footer-meta">
            <div
              v-if="cardImageStack(row).length"
              class="link-card__product-stack"
              :style="{ '--stack-count': cardImageStack(row).length }"
            >
              <div
                v-for="(item, index) in cardImageStack(row)"
                :key="item.key"
                class="link-card__product-thumb"
                :style="{ zIndex: index + 1 }"
              >
                <img
                  v-if="item.imageName && !brokenStackImageKeys.has(item.key)"
                  :src="getEcommerceImageUrl(item.imageName)"
                  alt=""
                  @error="markStackImageBroken(item.key)"
                />
                <el-icon v-else :size="14"><Picture /></el-icon>
              </div>
            </div>
            <span class="link-card__sku-count">
              {{ row.skuCount ?? row.skus?.length ?? 0 }} SKU
            </span>
          </div>
          <div class="link-card__footer-actions">
            <el-button size="small" @click.stop="openDetail(row.id)">
              {{ t('ecommerce.listingLink.viewDetail') }}
            </el-button>
            <el-button size="small" type="danger" plain @click.stop="onDelete(row)">
              {{ t('ecommerce.product.delete') }}
            </el-button>
          </div>
        </div>
      </article>

      <el-empty
        v-if="!loading && records.length === 0"
        class="link-card-grid__empty"
        :description="t('ecommerce.listingLink.noLinks')"
        :image-size="80"
      />
    </div>

    <TablePagination
      :page="page"
      :page-size="pageSize"
      :total="total"
      @update:page="onPageChange"
      @update:page-size="onSizeChange"
    />

    <el-button
      class="link-fab"
      type="primary"
      circle
      :title="t('ecommerce.listingLink.add')"
      @click.stop="openCreate"
    >
      <el-icon :size="22"><Plus /></el-icon>
    </el-button>
    </div>
    </template>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? t('ecommerce.listingLink.editTitle') : t('ecommerce.listingLink.createTitle')"
      width="1200px"
      destroy-on-close
      append-to-body
      top="4vh"
      class="link-edit-dialog"
      @closed="onEditDialogClosed"
    >
      <div class="link-edit-layout">
        <aside class="link-edit-panel link-edit-panel--info">
          <h3 class="link-edit-panel__title">{{ t('ecommerce.listingLink.linkInfoSection') }}</h3>
          <el-form :model="form" label-position="top" class="link-edit-info-form">
            <el-form-item :label="t('ecommerce.listingLink.name')" required>
              <el-input v-model="form.name" maxlength="64" show-word-limit />
            </el-form-item>
            <el-form-item :label="t('ecommerce.listingLink.shop')" required>
              <div class="link-edit-shop-select-wrap">
                <template v-if="selectedFormShop">
                  <img
                    :src="shopOptionShopIcon(selectedFormShop).src"
                    alt=""
                    class="link-edit-shop-select-prefix is-shop"
                    :class="{ 'is-avatar': shopOptionShopIcon(selectedFormShop).isCustomAvatar }"
                  />
                  <img
                    :src="shopOptionPlatformIcon(selectedFormShop)"
                    alt=""
                    class="link-edit-shop-select-prefix is-platform"
                    @error="onShopPlatformIconError(selectedFormShop)"
                  />
                </template>
                <el-select
                  v-model="form.shopId"
                  filterable
                  popper-class="link-edit-shop-select"
                  class="link-edit-shop-select-input"
                  style="width: 100%"
                  @change="onFormShopChange"
                >
                  <el-option v-for="s in shopOptions" :key="s.id" :label="shopOptionLabel(s)" :value="s.id">
                    <div class="shop-option-item">
                      <img
                        :src="shopOptionShopIcon(s).src"
                        alt=""
                        class="shop-option-item__shop"
                        :class="{ 'is-avatar': shopOptionShopIcon(s).isCustomAvatar }"
                      />
                      <span class="shop-option-item__name">{{ s.name }}</span>
                      <span class="shop-option-item__sep">·</span>
                      <img
                        :src="shopOptionPlatformIcon(s)"
                        alt=""
                        class="shop-option-item__platform"
                        @error="onShopPlatformIconError(s)"
                      />
                      <span class="shop-option-item__platform-name">{{ s.platformName }}</span>
                    </div>
                  </el-option>
                </el-select>
              </div>
            </el-form-item>
            <el-form-item :label="t('ecommerce.listingLink.platformUrl')">
              <el-input v-model="form.platformUrl" :placeholder="t('ecommerce.listingLink.platformUrl')">
                <template #suffix>
                  <el-icon
                    v-if="form.platformUrl?.trim()"
                    class="platform-url-open"
                    :title="t('ecommerce.listingLink.openPlatformLink')"
                    @click.stop="openPlatformUrl"
                  >
                    <Link />
                  </el-icon>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item :label="t('ecommerce.listingLink.listingTime')" required>
              <el-date-picker
                v-model="form.listingTime"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item :label="t('ecommerce.listingLink.remark')">
              <el-input v-model="form.remark" type="textarea" :rows="3" />
            </el-form-item>
          </el-form>

          <div class="link-edit-products">
            <div class="link-edit-products__label">{{ t('ecommerce.listingLink.product') }}</div>
            <div class="link-edit-products__list">
              <span
                v-for="productId in form.productIds"
                :key="productId"
                class="link-edit-product-tag"
              >
                <span class="link-edit-product-tag__text">{{ productLabelById(productId) }}</span>
                <button
                  type="button"
                  class="link-edit-product-tag__remove"
                  @click="removeProduct(productId)"
                >
                  <el-icon><Close /></el-icon>
                </button>
              </span>
              <el-popover
                v-model:visible="productPickerVisible"
                placement="top-start"
                :width="300"
                trigger="click"
              >
                <template #reference>
                  <button type="button" class="link-edit-product-add">
                    <el-icon><Plus /></el-icon>
                    {{ t('ecommerce.listingLink.addRelatedProduct') }}
                  </button>
                </template>
                <el-select
                  v-model="form.productIds"
                  multiple
                  filterable
                  remote
                  reserve-keyword
                  :remote-method="searchProducts"
                  :loading="productLoading"
                  :placeholder="t('ecommerce.listingLink.productsPlaceholder')"
                  style="width: 100%"
                  @visible-change="onProductSelectVisible"
                >
                  <el-option v-for="p in productOptions" :key="p.id" :label="productOptionLabel(p)" :value="p.id" />
                </el-select>
              </el-popover>
            </div>
          </div>
        </aside>

        <section class="link-edit-panel link-edit-panel--sku">
          <div class="link-edit-sku-header">
            <div class="link-edit-sku-header__leading">
              <h3 class="link-edit-panel__title">{{ t('ecommerce.listingLink.skuPricingSection') }}</h3>
              <el-button class="link-edit-sku-add" type="success" round @click="addSkuRow">
                <el-icon><Plus /></el-icon>
                {{ t('ecommerce.listingLink.addSku') }}
              </el-button>
              <el-button
                class="link-edit-sku-recalc"
                :loading="pricingRecalculating"
                @click="onRecalcAllPricing"
              >
                <el-icon><Refresh /></el-icon>
                {{ t('ecommerce.listingLink.recalcPricing') }}
              </el-button>
            </div>
            <div class="link-edit-sku-stats">
              <div class="link-edit-sku-stat">
                <span class="link-edit-sku-stat__label">{{ t('ecommerce.listingLink.skuCount') }}</span>
                <span class="link-edit-sku-stat__value">{{ skuPricingSummary.count }}</span>
              </div>
              <div class="link-edit-sku-stat">
                <span class="link-edit-sku-stat__label">{{ t('ecommerce.listingLink.avgProfitRate') }}</span>
                <span
                  class="link-edit-sku-stat__value"
                  :class="skuPricingSummary.avgProfitRate != null && skuPricingSummary.avgProfitRate >= 0 ? 'is-success' : 'is-muted'"
                >
                  {{ formatPercent(skuPricingSummary.avgProfitRate) }}
                </span>
              </div>
              <div class="link-edit-sku-stat">
                <span class="link-edit-sku-stat__label">{{ t('ecommerce.listingLink.avgProfit') }}</span>
                <span
                  class="link-edit-sku-stat__value"
                  :class="skuPricingSummary.avgProfit != null && skuPricingSummary.avgProfit >= 0 ? 'is-success' : 'is-danger'"
                >
                  <CnyAmount :value="skuPricingSummary.avgProfit" />
                </span>
              </div>
            </div>
          </div>

          <div class="link-edit-sku-grid-wrap">
            <div v-if="form.skus.length" class="link-edit-sku-grid">
              <article
                v-for="(row, index) in form.skus"
                :key="index"
                class="link-sku-card"
                :class="skuCardTone(row)"
              >
                <div class="link-sku-card__topbar">
                  <span
                    class="link-sku-card__stock"
                    :class="{ 'link-sku-card__stock--alert': skuStockAlert(row) }"
                  >
                    {{ t('ecommerce.listingLink.stockBadge', { count: skuStockTotal(row) }) }}
                  </span>
                  <div class="link-sku-card__actions" @click.stop>
                    <el-button
                      link
                      type="primary"
                      :title="t('ecommerce.listingLink.copySkuPricing')"
                      @click="copySkuPricing(row)"
                    >
                      <el-icon><DocumentCopy /></el-icon>
                    </el-button>
                    <el-button link type="danger" :disabled="form.skus.length <= 1" @click="form.skus.splice(index, 1)">
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </div>
                </div>

                <div class="link-sku-card__summary">
                  <div class="link-sku-card__thumb">
                    <img
                      v-if="formSkuImageUrl(row) && !editBrokenImageKeys.has(formSkuImageKey(row, index))"
                      :src="formSkuImageUrl(row)"
                      :alt="row.skuName || row.skuCodes"
                      class="link-sku-card__thumb-img"
                      @error="markEditImageBroken(row, index)"
                    />
                    <div v-else class="link-sku-card__thumb-placeholder">
                      <el-icon><Picture /></el-icon>
                    </div>
                  </div>

                  <div class="link-sku-card__meta link-sku-card__meta--edit">
                    <el-input
                      v-model="row.skuName"
                      size="small"
                      :placeholder="t('ecommerce.listingLink.skuName')"
                    />
                    <ListingLinkSkuSelect
                      v-model="row.skuCodes"
                      :product-ids="form.productIds"
                      :placeholder="t('ecommerce.listingLink.skuCodesHint')"
                      @change="(picked) => onSkuCodesChange(row, picked)"
                    />
                    <div v-if="primarySkuCode(row) !== '—'" class="link-sku-card__code">{{ primarySkuCode(row) }}</div>
                  </div>

                  <div class="link-sku-card__gauge">
                    <el-progress
                      type="circle"
                      :percentage="profitRingPercent(row)"
                      :width="84"
                      :stroke-width="5"
                      :color="profitRingColor(row)"
                      :show-text="false"
                      class="link-sku-card__gauge-ring"
                    />
                    <div class="profit-ring__inner">
                      <div class="profit-ring__rate">{{ formatPercent(calcProfitRate(row.profit ?? null, calcNetRevenue(row))) }}</div>
                      <div class="profit-ring__amount" :class="profitAmountClass(row)">
                        <CnyAmount :value="row.profit" />
                      </div>
                      <div class="profit-ring__label">{{ t('ecommerce.listingLink.profitRate') }}</div>
                    </div>
                  </div>
                </div>

                <div class="link-edit-sku-card__params">
                  <div class="link-edit-sku-card__param">
                    <span class="link-edit-sku-card__param-label">{{ t('ecommerce.listingLink.discountPct') }}</span>
                    <el-input-number
                      v-model="row.discountPct"
                      :min="1"
                      :max="100"
                      :precision="0"
                      size="small"
                      controls-position="right"
                      @change="() => recalcRow(row)"
                    />
                  </div>
                  <div class="link-edit-sku-card__param">
                    <span class="link-edit-sku-card__param-label">{{ t('ecommerce.listingLink.couponAmount') }}</span>
                    <el-input-number
                      v-model="row.couponAmount"
                      :min="0"
                      :precision="2"
                      size="small"
                      controls-position="right"
                      @change="() => recalcRow(row)"
                    />
                  </div>
                </div>

                <div class="link-sku-card__pricing">
                  <div class="link-sku-card__amounts">
                    <div class="amount-cell is-cost">
                      <span class="amount-cell__label">{{ t('ecommerce.listingLink.costPrice') }}</span>
                      <span class="amount-cell__value"><CnyAmount :value="row.costPrice" /></span>
                    </div>
                    <div class="amount-cell">
                      <span class="amount-cell__label">{{ t('ecommerce.listingLink.minSetAmountLine') }}</span>
                      <span class="amount-cell__value"><CnyAmount :value="row.minSetAmount" /></span>
                    </div>
                    <div class="amount-cell is-actual">
                      <span class="amount-cell__label">
                        {{ t('ecommerce.listingLink.actualSetAmountShort') }}<span class="required-mark">*</span>
                      </span>
                      <el-input-number
                        v-model="row.actualSetAmount"
                        :min="0"
                        :precision="2"
                        size="small"
                        controls-position="right"
                        class="link-edit-sku-card__actual-input"
                        @change="() => recalcRow(row)"
                      />
                    </div>
                  </div>
                  <div class="price-scale">
                    <div class="price-scale__track">
                      <div class="price-scale__half is-left">
                        <div class="price-scale__seg is-cost" :style="{ width: priceScale(row).costInLeft + '%' }" />
                        <div class="price-scale__seg is-gap" :style="{ width: priceScale(row).gapInLeft + '%' }" />
                      </div>
                      <div
                        class="price-scale__half is-right"
                        :class="{ 'is-risk-mode': priceScale(row).belowMin }"
                      >
                        <div
                          v-if="priceScale(row).rightSeg > 0"
                          class="price-scale__seg"
                          :class="priceScale(row).profitClass"
                          :style="{ width: priceScale(row).rightSeg + '%' }"
                        />
                      </div>
                      <div class="price-scale__marker" :title="t('ecommerce.listingLink.minSetAmountLine')" />
                    </div>
                  </div>
                </div>
              </article>
            </div>
            <el-empty v-else :description="t('ecommerce.listingLink.noSkus')" :image-size="72" />
          </div>

          <el-collapse v-if="form.costFormula" class="link-edit-formula-collapse">
            <el-collapse-item :title="t('ecommerce.listingLink.costFormula')" name="formula">
              <p class="link-edit-formula-collapse__text">{{ form.costFormula }}</p>
            </el-collapse-item>
          </el-collapse>
        </section>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('ecommerce.common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">{{ t('ecommerce.common.save') }}</el-button>
      </template>
    </el-dialog>

    <ListingLinkDetailDrawer v-model="detailVisible" :link-id="detailLinkId" @edit="onDetailEdit" />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Close, Delete, DocumentCopy, Link, Picture, Plus, Refresh } from '@element-plus/icons-vue'
import { getEcommerceImageUrl } from '@/api/ecommerce/image'
import CnyAmount from '@/components/CnyAmount.vue'
import { formatSignedCnyPlain } from '@/utils/formatMoney'
import { resolvePlatformIconMeta } from '@/utils/platformVisual'
import { resolveShopIconMeta } from '@/utils/shopVisual'
import { resolveListingLinkSkuImageName } from '@/utils/listingLinkSkuImage'
import { fetchPlatformOptions, type EcPlatform } from '@/api/ecommerce/platform'
import { fetchShopOptions, type EcShop } from '@/api/ecommerce/shop'
import { fetchProduct, fetchProducts, type EcProductListItem } from '@/api/ecommerce/product'
import type { EcInventorySkuOption } from '@/api/ecommerce/inventory'
import {
  calculateListingPricing,
  copyListingLink,
  createListingLink,
  deleteListingLink,
  fetchListingLink,
  fetchListingLinks,
  recalculateAllListingPricing,
  updateListingLink,
  type EcListingLink,
  type EcListingLinkSku,
  type EcListingLinkSkuInventory,
} from '@/api/ecommerce/listingLink'
import TablePagination from '@/components/TablePagination.vue'
import { usePagination } from '@/composables/usePagination'
import { formatDate, todayDateString } from '@/utils/date'
import ListingLinkDetailDrawer from './ListingLinkDetailDrawer.vue'
import ListingLinkSkuSelect from './ListingLinkSkuSelect.vue'

const emit = defineEmits<{ saved: [id: number] }>()

const props = withDefaults(defineProps<{ showList?: boolean }>(), { showList: true })

const detailVisible = ref(false)
const detailLinkId = ref<number | null>(null)
const selectedLinkId = ref<number | null>(null)

const { t } = useI18n()

const keyword = ref('')
const platformId = ref<number | undefined>()
const shopId = ref<number | undefined>()
const activePlatformTab = ref('all')
const platformTabCounts = ref<Record<string, number>>({ all: 0 })
const skuPreviewMap = ref<Record<number, EcListingLinkSku[]>>({})
const productImageMap = ref<Record<number, string | undefined>>({})
const skuImageMap = ref<Record<string, string | undefined>>({})
const brokenStackImageKeys = ref<Set<string>>(new Set())
const editBrokenImageKeys = ref<Set<string>>(new Set())
const platformOptions = ref<EcPlatform[]>([])
const shopOptions = ref<EcShop[]>([])

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const pricingRecalculating = ref(false)
const listPricingRecalculating = ref(false)
const productLoading = ref(false)
const productOptions = ref<EcProductListItem[]>([])
const productPickerVisible = ref(false)
const lastRecalcCostFormula = ref<string | undefined>()

const filteredShopOptions = computed(() => {
  if (!platformId.value) return shopOptions.value
  return shopOptions.value.filter((s) => s.platformId === platformId.value)
})

const selectedFormShop = computed(() => shopOptions.value.find((s) => s.id === form.shopId))

const platformsWithShops = computed(() => {
  const platformIds = new Set(shopOptions.value.map((s) => s.platformId))
  return platformOptions.value.filter((p) => platformIds.has(p.id))
})

const { page, pageSize, total, records, loading, load, onPageChange, onSizeChange } = usePagination(
  (p, ps) =>
    fetchListingLinks(keyword.value.trim() || undefined, shopId.value, platformId.value, {
      page: p,
      pageSize: ps,
    }),
)

const CARD_SKU_PREVIEW_LIMIT = 2

const shopOptionMap = computed(() => new Map(shopOptions.value.map((s) => [s.id, s])))
const platformOptionMap = computed(() => new Map(platformOptions.value.map((p) => [p.id, p])))
const shopPlatformIconOverride = ref<Record<number, string>>({})
const linkCardPlatformIconOverride = ref<Record<number, string>>({})

const PLATFORM_STRIP_COLORS: Record<string, string> = {
  淘宝: 'linear-gradient(90deg, #ff6a00, #ff9500)',
  拼多多: 'linear-gradient(90deg, #e02e24, #f43530)',
  抖音: 'linear-gradient(90deg, #111827, #4b5563)',
  京东: 'linear-gradient(90deg, #c81623, #e1251b)',
}

type CardImageStackItem = { key: string; imageName?: string }

type SkuRow = EcListingLinkSku

const form = reactive<{
  name: string
  platformUrl: string
  productIds: number[]
  shopId: number | undefined
  listingTime: string
  remark: string
  status: string
  costFormula: string | undefined
  skus: SkuRow[]
}>({
  name: '',
  platformUrl: '',
  productIds: [],
  shopId: undefined,
  listingTime: '',
  remark: '',
  status: 'ENABLED',
  costFormula: undefined,
  skus: [],
})

function emptySkuRow(): SkuRow {
  return {
    skuName: '',
    skuCodes: '',
    discountPct: 100,
    couponAmount: 0,
    minSetAmount: undefined,
    costPrice: undefined,
    actualSetAmount: undefined,
    profit: undefined,
    skuAmount: undefined,
    cartonAmount: undefined,
    expressAmount: undefined,
    pricingRisk: undefined,
    inventories: undefined,
  }
}

function shopOptionLabel(s: EcShop) {
  return s.platformName ? `${s.name} · ${s.platformName}` : s.name
}

function shopOptionShopIcon(s: EcShop) {
  return resolveShopIconMeta(s.name, s.platformName, s.platformCode, s.avatarUrl)
}

function shopOptionPlatformIcon(s: EcShop) {
  if (shopPlatformIconOverride.value[s.id]) {
    return shopPlatformIconOverride.value[s.id]
  }
  const platform = platformOptionMap.value.get(s.platformId)
  return resolvePlatformIconMeta(
    s.platformName ?? platform?.name,
    s.platformCode ?? platform?.platformCode,
    platform?.avatarUrl,
  ).src
}

function onShopPlatformIconError(s: EcShop) {
  if (shopPlatformIconOverride.value[s.id]) return
  shopPlatformIconOverride.value = {
    ...shopPlatformIconOverride.value,
    [s.id]: resolvePlatformIconMeta(s.platformName, s.platformCode).src,
  }
}

function findShopForLink(row: EcListingLink): EcShop | undefined {
  return shopOptionMap.value.get(row.shopId)
}

function resolveLinkPlatform(row: EcListingLink) {
  const shop = findShopForLink(row)
  const platformId = row.platformId ?? shop?.platformId
  const platform = platformId != null ? platformOptionMap.value.get(platformId) : undefined
  return { shop, platform }
}

function linkCardShopIcon(row: EcListingLink) {
  const shop = findShopForLink(row)
  return resolveShopIconMeta(
    row.shopName ?? shop?.name,
    row.platformName ?? shop?.platformName,
    shop?.platformCode,
    shop?.avatarUrl,
  )
}

function linkCardPlatformIcon(row: EcListingLink) {
  if (linkCardPlatformIconOverride.value[row.id]) {
    return { src: linkCardPlatformIconOverride.value[row.id], isCustomAvatar: false }
  }
  const { shop, platform } = resolveLinkPlatform(row)
  return resolvePlatformIconMeta(
    row.platformName ?? shop?.platformName ?? platform?.name,
    shop?.platformCode ?? platform?.platformCode,
    platform?.avatarUrl,
  )
}

function onLinkCardPlatformIconError(row: EcListingLink) {
  if (linkCardPlatformIconOverride.value[row.id]) return
  const { shop } = resolveLinkPlatform(row)
  linkCardPlatformIconOverride.value = {
    ...linkCardPlatformIconOverride.value,
    [row.id]: resolvePlatformIconMeta(row.platformName ?? shop?.platformName, shop?.platformCode).src,
  }
}

function productOptionLabel(p: EcProductListItem) {
  return p.factoryName ? `${p.name} · ${p.factoryName}` : p.name
}

function productLabelById(productId: number) {
  const product = productOptions.value.find((p) => p.id === productId)
  return product ? productOptionLabel(product) : String(productId)
}

function removeProduct(productId: number) {
  form.productIds = form.productIds.filter((id) => id !== productId)
}

function onProductSelectVisible(visible: boolean) {
  if (visible) searchProducts('')
}

function formatPercent(rate?: number | null) {
  if (rate == null) return '—'
  return `${Number(rate).toFixed(2)}%`
}

function calcNetRevenue(row: SkuRow): number | null {
  if (row.actualSetAmount == null) return null
  const setAmount = Number(row.actualSetAmount)
  const coupon = Number(row.couponAmount ?? 0)
  const discount = Number(row.discountPct ?? 100)
  if (discount <= 0 || discount > 100 || setAmount < coupon) return null
  return Number(((setAmount - coupon) * (discount / 100)).toFixed(2))
}

function calcProfitRate(profit: number | null, netRevenue: number | null): number | null {
  if (profit == null || netRevenue == null || netRevenue <= 0) return null
  return Number(((profit / netRevenue) * 100).toFixed(2))
}

const skuPricingSummary = computed(() => {
  const skus = form.skus
  const profits = skus.map((s) => s.profit).filter((v): v is number => v != null)
  const rates: number[] = []
  for (const sku of skus) {
    const rate = calcProfitRate(sku.profit ?? null, calcNetRevenue(sku))
    if (rate != null) rates.push(rate)
  }
  return {
    count: skus.length,
    avgProfit: profits.length ? Number((profits.reduce((a, b) => a + b, 0) / profits.length).toFixed(2)) : null,
    avgProfitRate: rates.length ? Number((rates.reduce((a, b) => a + b, 0) / rates.length).toFixed(2)) : null,
  }
})

function parseFormSkuCodes(row: SkuRow) {
  return parseLinkSkuCodes(row)
}

function primarySkuCode(row: SkuRow) {
  return parseFormSkuCodes(row)[0] || row.skuCodes || '—'
}

function skuStockTotal(row: SkuRow) {
  return rowInventories(row).reduce((sum, inv) => sum + (inv.quantity ?? 0), 0)
}

function skuStockAlert(row: SkuRow) {
  return rowInventories(row).some((inv) => inv.alertActive)
}

function profitRingPercent(row: SkuRow) {
  const rate = calcProfitRate(row.profit ?? null, calcNetRevenue(row))
  if (rate == null) return 0
  return Math.min(100, Math.abs(rate))
}

function profitRingColor(row: SkuRow) {
  const profit = row.profit ?? 0
  if (profit < 0 || row.pricingRisk === 'NEGATIVE_PROFIT') return '#ef4444'
  if (row.pricingRisk === 'BELOW_MIN') return '#f59e0b'
  return '#22c55e'
}

function profitAmountClass(row: SkuRow) {
  const profit = row.profit ?? 0
  if (profit < 0 || row.pricingRisk === 'NEGATIVE_PROFIT') return 'is-danger'
  if (row.pricingRisk === 'BELOW_MIN') return 'is-warning'
  return 'is-success'
}

function priceScale(row: SkuRow) {
  const cost = Number(row.costPrice ?? 0)
  const min = Number(row.minSetAmount ?? 0)
  const actual = Number(row.actualSetAmount ?? 0)
  const profit = row.profit ?? 0
  const belowMin = actual < min || row.pricingRisk === 'BELOW_MIN'

  const costInLeft = min > 0 ? Math.min(100, Math.max(0, (cost / min) * 100)) : 35
  const gapInLeft = Math.max(0, 100 - costInLeft)

  let rightSeg = 0
  if (min > 0 && actual > min) {
    rightSeg = Math.min(100, ((actual - min) / min) * 100)
  } else if (min > 0 && actual < min) {
    rightSeg = Math.min(100, ((min - actual) / min) * 100)
  }

  return {
    costInLeft,
    gapInLeft,
    rightSeg,
    belowMin,
    profitClass: belowMin || profit < 0 ? 'is-risk' : 'is-profit',
  }
}

function skuCardTone(row: SkuRow) {
  if (row.pricingRisk === 'BELOW_MIN' || row.pricingRisk === 'NEGATIVE_PROFIT') return 'is-risk'
  if ((row.profit ?? 0) < 0) return 'is-risk'
  if ((row.profit ?? 0) > 0) return 'is-profit'
  return ''
}

function resolveFormSkuImageName(row: SkuRow): string | undefined {
  return resolveListingLinkSkuImageName(
    parseFormSkuCodes(row),
    form.productIds,
    skuImageMap.value,
    productImageMap.value,
  )
}

function formSkuImageUrl(row: SkuRow) {
  return getEcommerceImageUrl(resolveFormSkuImageName(row))
}

function formSkuImageKey(row: SkuRow, index: number) {
  return `edit-${row.skuCodes || row.skuName || index}`
}

function markEditImageBroken(row: SkuRow, index: number) {
  const key = formSkuImageKey(row, index)
  if (editBrokenImageKeys.value.has(key)) return
  editBrokenImageKeys.value = new Set([...editBrokenImageKeys.value, key])
}

async function enrichFormProductImages() {
  const ids = form.productIds
  if (!ids.length) return
  const entries = await Promise.all(
    ids.map(async (id) => {
      try {
        const product = await fetchProduct(id)
        return {
          id,
          imageName: product.imageName?.trim() || undefined,
          skus: product.skus ?? [],
        }
      } catch {
        return { id, imageName: productImageMap.value[id], skus: [] as { skuCode?: string; imageName?: string }[] }
      }
    }),
  )
  const nextProduct = { ...productImageMap.value }
  const nextSku = { ...skuImageMap.value }
  for (const entry of entries) {
    if (entry.imageName !== undefined || !(entry.id in nextProduct)) {
      nextProduct[entry.id] = entry.imageName
    }
    for (const sku of entry.skus) {
      const code = sku.skuCode?.trim()
      if (code) nextSku[code] = sku.imageName?.trim() || undefined
    }
  }
  productImageMap.value = nextProduct
  skuImageMap.value = nextSku
}

function openPlatformUrl() {
  const raw = form.platformUrl?.trim()
  if (!raw) return
  const url = /^https?:\/\//i.test(raw) ? raw : `https://${raw}`
  window.open(url, '_blank', 'noopener,noreferrer')
}

function formatProfitChip(profit?: number | null) {
  if (profit == null) return '—'
  return formatSignedCnyPlain(Number(profit))
}

function profitChipClass(sku: EcListingLinkSku) {
  if (sku.pricingRisk === 'BELOW_MIN' || sku.pricingRisk === 'NEGATIVE_PROFIT') return 'is-danger'
  const profit = sku.profit ?? 0
  if (profit < 0) return 'is-danger'
  if (profit > 0) return 'is-success'
  return 'is-muted'
}

function platformStripStyle(row: EcListingLink) {
  const name = row.platformName?.trim() ?? ''
  const gradient = PLATFORM_STRIP_COLORS[name] ?? 'linear-gradient(90deg, #2563eb, #3b82f6)'
  return { background: gradient }
}

function platformTabLabel(tab: string, platformName?: string) {
  const count = platformTabCounts.value[tab] ?? 0
  const label = tab === 'all' ? t('ecommerce.listingLink.allPlatforms') : (platformName ?? t('ecommerce.listingLink.platform'))
  return `${label} (${count})`
}

function cardSkuPreviews(row: EcListingLink) {
  const skus = skuPreviewMap.value[row.id] ?? row.skus ?? []
  return skus.slice(0, CARD_SKU_PREVIEW_LIMIT)
}

function cardSkuOverflow(row: EcListingLink) {
  const totalCount = row.skuCount ?? (skuPreviewMap.value[row.id] ?? row.skus ?? []).length
  return Math.max(0, totalCount - CARD_SKU_PREVIEW_LIMIT)
}

function linkProductIds(row: EcListingLink) {
  return (row.products ?? []).map((p) => p.productId).filter((id) => id != null)
}

function parseLinkSkuCodes(sku: EcListingLinkSku) {
  const codes = sku.skuCodes?.split(',').map((c) => c.trim()).filter(Boolean) ?? []
  if (codes.length) return codes
  return sku.skuName?.trim() ? [sku.skuName.trim()] : []
}

function cardImageStack(row: EcListingLink): CardImageStackItem[] {
  const productIds = linkProductIds(row)
  const linkSkus = skuPreviewMap.value[row.id] ?? row.skus ?? []

  if (productIds.length === 1 && linkSkus.length > 1) {
    const items: CardImageStackItem[] = []
    const seen = new Set<string>()
    for (const sku of linkSkus) {
      for (const code of parseLinkSkuCodes(sku)) {
        if (seen.has(code)) continue
        seen.add(code)
        items.push({ key: `sku-${code}`, imageName: skuImageMap.value[code] })
      }
    }
    if (items.length) return items
  }

  if (productIds.length > 0) {
    return productIds.map((id) => ({
      key: `product-${id}`,
      imageName: productImageMap.value[id],
    }))
  }

  return []
}

function markStackImageBroken(key: string) {
  if (brokenStackImageKeys.value.has(key)) return
  brokenStackImageKeys.value = new Set([...brokenStackImageKeys.value, key])
}

async function enrichProductImages(links: EcListingLink[]) {
  const ids = new Set<number>()
  for (const link of links) {
    for (const id of linkProductIds(link)) ids.add(id)
  }

  const needsSkuImages = (link: EcListingLink, productId: number) => {
    if (!linkProductIds(link).includes(productId)) return false
    const linkSkus = skuPreviewMap.value[link.id] ?? []
    if (linkProductIds(link).length !== 1 || linkSkus.length <= 1) return false
    return linkSkus.some((sku) => parseLinkSkuCodes(sku).some((code) => !(code in skuImageMap.value)))
  }

  const pending = [...ids].filter(
    (id) => !(id in productImageMap.value) || links.some((link) => needsSkuImages(link, id)),
  )
  if (!pending.length) return

  const entries = await Promise.all(
    pending.map(async (id) => {
      try {
        const detail = await fetchProduct(id)
        return { id, imageName: detail.imageName?.trim() || undefined, skus: detail.skus ?? [] }
      } catch {
        return { id, imageName: undefined, skus: [] }
      }
    }),
  )
  const next = { ...productImageMap.value }
  const nextSku = { ...skuImageMap.value }
  for (const entry of entries) {
    next[entry.id] = entry.imageName
    for (const sku of entry.skus) {
      const code = sku.skuCode?.trim()
      if (code) nextSku[code] = sku.imageName?.trim() || undefined
    }
  }
  productImageMap.value = next
  skuImageMap.value = nextSku
}

async function enrichSkuPreviews(links: EcListingLink[]) {
  const pending = links.filter((link) => !skuPreviewMap.value[link.id])
  if (pending.length) {
    const details = await Promise.all(pending.map((link) => fetchListingLink(link.id)))
    const next = { ...skuPreviewMap.value }
    for (const detail of details) {
      next[detail.id] = detail.skus ?? []
    }
    skuPreviewMap.value = next
  }
  await enrichProductImages(links)
}

async function refreshPlatformTabCounts() {
  const counts: Record<string, number> = {}
  const allResult = await fetchListingLinks(keyword.value.trim() || undefined, shopId.value, undefined, {
    page: 1,
    pageSize: 1,
  })
  counts.all = allResult.total
  await Promise.all(
    platformsWithShops.value.map(async (p) => {
      const result = await fetchListingLinks(keyword.value.trim() || undefined, shopId.value, p.id, {
        page: 1,
        pageSize: 1,
      })
      counts[String(p.id)] = result.total
    }),
  )
  platformTabCounts.value = counts
}

watch(platformsWithShops, (list) => {
  if (activePlatformTab.value === 'all') return
  if (!list.some((p) => String(p.id) === activePlatformTab.value)) {
    activePlatformTab.value = 'all'
    platformId.value = undefined
    load(true)
  }
})

function onPlatformTabChange(tab: string | number) {
  const name = String(tab)
  platformId.value = name === 'all' ? undefined : Number(name)
  load(true)
}

function onShopFilterChange() {
  load(true)
  refreshPlatformTabCounts()
}

function defaultListingDate() {
  return todayDateString()
}

function toListingDate(value?: string | null) {
  if (!value) return defaultListingDate()
  return value.trim().slice(0, 10)
}

function toListingDateTime(date: string) {
  if (!date) return date
  if (date.length <= 10) return `${date} 00:00:00`
  return date
}

function resetForm() {
  form.name = ''
  form.platformUrl = ''
  form.productIds = []
  form.shopId = shopOptions.value[0]?.id
  form.listingTime = defaultListingDate()
  form.remark = ''
  form.status = 'ENABLED'
  form.costFormula = undefined
  lastRecalcCostFormula.value = undefined
  form.skus = [emptySkuRow()]
  editBrokenImageKeys.value = new Set()
}

function addSkuRow() {
  form.skus.push(emptySkuRow())
}

function rowInventories(row: SkuRow): EcListingLinkSkuInventory[] {
  return row.inventories ?? []
}

function skuOptionAlertActive(p: EcInventorySkuOption) {
  if (p.ignoreAlert || p.alertThreshold == null) return false
  return (p.quantity ?? 0) <= p.alertThreshold
}

function onSkuCodesChange(row: SkuRow, picked: EcInventorySkuOption[]) {
  row.inventories = picked.map((p) => ({
    skuCode: p.skuCode,
    specName: p.specName,
    skuStatus: p.skuStatus,
    quantity: p.quantity,
    alertThreshold: p.alertThreshold,
    alertActive: skuOptionAlertActive(p),
    inboundAllowed: p.inboundAllowed,
  }))
  const nextSku = { ...skuImageMap.value }
  for (const p of picked) {
    const code = p.skuCode?.trim()
    const imageName = p.imageName?.trim()
    if (code && imageName) nextSku[code] = imageName
  }
  skuImageMap.value = nextSku
  recalcRow(row)
}

function clearRowPricing(row: SkuRow) {
  row.skuAmount = undefined
  row.cartonAmount = undefined
  row.expressAmount = undefined
  row.costPrice = undefined
  row.minSetAmount = undefined
  row.profit = undefined
  row.pricingRisk = undefined
}

async function recalcRow(row: SkuRow, options?: { silent?: boolean }): Promise<string | null> {
  if (!form.shopId) {
    if (!options?.silent) {
      ElMessage.warning(t('ecommerce.listingLink.shopRequired'))
    }
    return t('ecommerce.listingLink.shopRequired')
  }
  if (!row.skuCodes?.trim()) {
    clearRowPricing(row)
    return null
  }
  try {
    const result = await calculateListingPricing(
      {
        shopId: form.shopId,
        skuCodes: row.skuCodes.trim(),
        discountPct: row.discountPct ?? 100,
        couponAmount: row.couponAmount ?? 0,
        actualSetAmount: row.actualSetAmount ?? undefined,
      },
      { silent: true },
    )
    row.skuAmount = result.skuAmount
    row.cartonAmount = result.cartonAmount
    row.expressAmount = result.expressAmount
    row.costPrice = result.costPrice
    row.minSetAmount = result.minSetAmount
    row.profit = result.profit ?? undefined
    row.pricingRisk = result.pricingRisk
    row.skuCodes = result.skuCodes
    if (result.costFormula) {
      form.costFormula = result.costFormula
      lastRecalcCostFormula.value = result.costFormula
    }
    return null
  } catch (e) {
    clearRowPricing(row)
    const message = e instanceof Error ? e.message : t('ecommerce.listingLink.pricingRecalcFailed')
    if (!options?.silent) {
      ElMessage.warning(message)
    }
    return message
  }
}

function notifyPricingRecalcErrors(messages: Set<string>) {
  if (!messages.size) return
  if (messages.size === 1) {
    ElMessage.warning([...messages][0]!)
    return
  }
  ElMessage.warning(t('ecommerce.listingLink.pricingRecalcBatchFailed', { count: messages.size }))
}

async function recalcAllSkuPricing(): Promise<Set<string>> {
  const errors = new Set<string>()
  for (const row of form.skus) {
    if (!row.skuCodes?.trim()) continue
    const err = await recalcRow(row, { silent: true })
    if (err) errors.add(err)
  }
  return errors
}

async function onRecalcAllLinksPricing() {
  try {
    await ElMessageBox.confirm(t('ecommerce.listingLink.recalcAllLinksPricingConfirm'), {
      type: 'warning',
      title: t('ecommerce.listingLink.recalcPricing'),
      confirmButtonText: t('ecommerce.common.confirm'),
      cancelButtonText: t('ecommerce.common.cancel'),
    })
  } catch {
    return
  }
  listPricingRecalculating.value = true
  try {
    const result = await recalculateAllListingPricing()
    ElMessage.success(t('ecommerce.listingLink.recalcAllLinksPricingSuccess', { count: result.updated }))
    await load()
    await refreshPlatformTabCounts()
  } finally {
    listPricingRecalculating.value = false
  }
}

async function onRecalcAllPricing() {
  if (!form.shopId) {
    ElMessage.warning(t('ecommerce.listingLink.shopRequired'))
    return
  }
  if (!form.skus.some((s) => s.skuCodes?.trim())) {
    ElMessage.warning(t('ecommerce.listingLink.recalcPricingNoSku'))
    return
  }
  try {
    await ElMessageBox.confirm(t('ecommerce.listingLink.recalcPricingConfirm'), {
      type: 'warning',
      title: t('ecommerce.listingLink.recalcPricing'),
      confirmButtonText: t('ecommerce.common.confirm'),
      cancelButtonText: t('ecommerce.common.cancel'),
    })
  } catch {
    return
  }
  pricingRecalculating.value = true
  try {
    lastRecalcCostFormula.value = undefined
    const errors = await recalcAllSkuPricing()
    if (errors.size) {
      notifyPricingRecalcErrors(errors)
    } else {
      ElMessage.success(t('ecommerce.listingLink.recalcPricingSuccess'))
    }
  } finally {
    pricingRecalculating.value = false
  }
}

async function onFormShopChange() {
  lastRecalcCostFormula.value = undefined
  const errors = await recalcAllSkuPricing()
  notifyPricingRecalcErrors(errors)
}

function copySkuPricing(row: SkuRow) {
  const skuCodes = row.skuCodes?.trim()
  if (!skuCodes) {
    ElMessage.warning(t('ecommerce.listingLink.skuCodesHint'))
    return
  }
  if (row.actualSetAmount == null) {
    ElMessage.warning(t('ecommerce.listingLink.actualSetAmountRequired'))
    return
  }
  const newRow: SkuRow = {
    ...emptySkuRow(),
    skuCodes,
    discountPct: row.discountPct ?? 100,
    couponAmount: row.couponAmount ?? 0,
    actualSetAmount: row.actualSetAmount,
  }
  form.skus.push(newRow)
  recalcRow(newRow)
  ElMessage.success(t('ecommerce.listingLink.copySkuPricingSuccess'))
}

async function searchProducts(query: string) {
  productLoading.value = true
  try {
    const result = await fetchProducts(query.trim() || undefined, { page: 1, pageSize: 20 })
    productOptions.value = result.records ?? []
  } finally {
    productLoading.value = false
  }
}

async function ensureProductOption(productId: number, productName?: string) {
  if (productOptions.value.some((p) => p.id === productId)) return
  if (productName) {
    productOptions.value = [
      { id: productId, name: productName, rebatePct: 0, status: 'ENABLED', skuCount: 0 },
      ...productOptions.value,
    ]
    return
  }
  try {
    const detail = await fetchProduct(productId)
    productOptions.value = [
      {
        id: detail.id,
        name: detail.name,
        factoryName: detail.factoryName ?? undefined,
        rebatePct: detail.rebatePct,
        status: detail.status,
        skuCount: detail.skus?.length ?? 0,
      },
      ...productOptions.value,
    ]
  } catch {
    /* ignore */
  }
}

async function ensureProductOptions(
  productIds: number[],
  products?: { productId: number; productName?: string }[],
) {
  for (const p of products ?? []) {
    await ensureProductOption(p.productId, p.productName)
  }
  for (const id of productIds) {
    if (!productOptions.value.some((p) => p.id === id)) {
      await ensureProductOption(id)
    }
  }
}

function openDetail(id: number) {
  selectedLinkId.value = id
  detailLinkId.value = id
  detailVisible.value = true
}

function clearLinkSelection() {
  selectedLinkId.value = null
  detailVisible.value = false
}

function onListSurfaceClick(event: MouseEvent) {
  if (selectedLinkId.value == null) return
  const target = event.target as HTMLElement | null
  if (!target) return
  if (target.closest('.link-card')) return
  if (target.closest('.link-panel-search')) return
  if (target.closest('.link-platform-tabs')) return
  if (target.closest('.link-fab')) return
  if (target.closest('.table-pagination')) return
  if (target.closest('.el-overlay') || target.closest('.el-dialog') || target.closest('.el-drawer')) return
  clearLinkSelection()
}

function onEditDialogClosed() {
  nextTick(() => {
    document.body.style.removeProperty('overflow')
    document.body.style.removeProperty('padding-right')
    document.body.classList.remove('el-popup-parent--hidden')
  })
}

async function onDetailEdit(id: number) {
  detailVisible.value = false
  await nextTick()
  await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()))
  await openEdit(id)
}

function isLinkCardSelected(id: number) {
  return selectedLinkId.value === id
}

function openCreate() {
  editingId.value = null
  resetForm()
  searchProducts('')
  dialogVisible.value = true
}

function openCreateFromProduct(productId: number, productName: string, skuCodes?: string[]) {
  editingId.value = null
  resetForm()
  form.productIds = [productId]
  form.name = `${productName} 链接`
  if (skuCodes?.length) {
    form.skus = [{ ...emptySkuRow(), skuCodes: skuCodes.join(',') }]
  }
  ensureProductOption(productId, productName)
  dialogVisible.value = true
}

async function openEdit(id: number) {
  selectedLinkId.value = id
  editingId.value = id
  const detail = await fetchListingLink(id)
  form.name = detail.name
  form.platformUrl = detail.platformUrl || ''
  form.productIds = (detail.products ?? []).map((p) => p.productId)
  form.shopId = detail.shopId
  form.listingTime = toListingDate(detail.listingTime)
  form.remark = detail.remark || ''
  form.status = detail.status
  form.costFormula = detail.costFormula
  lastRecalcCostFormula.value = detail.costFormula
  form.skus = (detail.skus?.length ? detail.skus : [emptySkuRow()]).map((s) => ({ ...s }))
  if (form.productIds.length) {
    await ensureProductOptions(form.productIds, detail.products)
    await enrichFormProductImages()
  } else {
    searchProducts('')
  }
  dialogVisible.value = true
}

async function onSave() {
  if (!form.name.trim()) {
    ElMessage.warning(t('ecommerce.listingLink.nameRequired'))
    return
  }
  if (!form.shopId) {
    ElMessage.warning(t('ecommerce.listingLink.shopRequired'))
    return
  }
  if (!form.listingTime) {
    ElMessage.warning(t('ecommerce.listingLink.listingTimeRequired'))
    return
  }
  const skus = form.skus.filter((s) => s.skuName?.trim() && s.skuCodes?.trim())
  if (!skus.length) {
    ElMessage.warning(t('ecommerce.listingLink.skusRequired'))
    return
  }
  const missingActual = skus.find((s) => s.actualSetAmount == null)
  if (missingActual) {
    ElMessage.warning(t('ecommerce.listingLink.actualSetAmountRequired'))
    return
  }

  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      shopId: form.shopId,
      platformUrl: form.platformUrl?.trim() || undefined,
      productIds: form.productIds.length ? form.productIds : undefined,
      listingTime: toListingDateTime(form.listingTime),
      remark: form.remark?.trim() || undefined,
      status: form.status,
      skus: skus.map((s, i) => ({
        skuName: s.skuName.trim(),
        skuCodes: s.skuCodes.trim(),
        discountPct: s.discountPct ?? 100,
        couponAmount: s.couponAmount ?? 0,
        actualSetAmount: s.actualSetAmount ?? undefined,
        sortOrder: i,
      })),
    }
    let savedId: number
    if (editingId.value) {
      const saved = await updateListingLink(editingId.value, payload)
      savedId = saved.id
    } else {
      const saved = await createListingLink(payload)
      savedId = saved.id
    }
    selectedLinkId.value = savedId
    detailLinkId.value = savedId
    ElMessage.success(t('ecommerce.common.saved'))
    dialogVisible.value = false
    emit('saved', savedId)
    await load()
    await refreshPlatformTabCounts()
  } finally {
    saving.value = false
  }
}

async function onCopyLink(row: EcListingLink) {
  await copyListingLink(row.id)
  ElMessage.success(t('ecommerce.listingLink.copyLinkSuccess'))
  await load()
  await refreshPlatformTabCounts()
}

async function onDelete(row: EcListingLink) {
  await ElMessageBox.confirm(t('ecommerce.listingLink.deleteConfirm', { name: row.name }), { type: 'warning' })
  await deleteListingLink(row.id)
  ElMessage.success(t('ecommerce.common.deleted'))
  if (selectedLinkId.value === row.id) selectedLinkId.value = null
  if (detailLinkId.value === row.id) detailLinkId.value = null
  const next = { ...skuPreviewMap.value }
  delete next[row.id]
  skuPreviewMap.value = next
  await load()
  await refreshPlatformTabCounts()
}

async function refreshOptions() {
  platformOptions.value = await fetchPlatformOptions()
  shopOptions.value = await fetchShopOptions()
}

let searchTimer: ReturnType<typeof setTimeout> | null = null
watch(keyword, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    load(true)
    refreshPlatformTabCounts()
  }, 300)
})

watch(records, (list) => {
  enrichSkuPreviews(list)
})

watch(dialogVisible, async (visible) => {
  if (visible && form.productIds.length) {
    await enrichFormProductImages()
  }
})

watch(
  () => [...form.productIds],
  async () => {
    if (dialogVisible.value) await enrichFormProductImages()
  },
)

async function loadListingLinks() {
  await refreshOptions()
  if (platformId.value) {
    activePlatformTab.value = String(platformId.value)
  }
  await load()
  await refreshPlatformTabCounts()
  await enrichSkuPreviews(records.value)
}

onMounted(() => {
  if (props.showList) {
    loadListingLinks()
  } else {
    refreshOptions()
  }
})

defineExpose({ loadListingLinks, openCreateFromProduct, openEdit })
</script>

<style scoped lang="scss">
.listing-link-panel {
  position: relative;
  padding-bottom: 72px;

  &:not(.listing-link-panel--dialog-only) {
    display: flex;
    flex-direction: column;
    height: 100%;
    min-height: 0;
  }

  &--dialog-only {
    position: absolute;
    width: 0;
    height: 0;
    padding: 0;
    overflow: visible;
    pointer-events: none;
  }
}

.link-list-surface {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.link-panel-search {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.link-panel-search__input {
  flex: 1;
  min-width: 220px;
}

.link-panel-search__shop {
  width: 200px;
}

.link-panel-search__recalc {
  flex-shrink: 0;
}

.link-platform-tabs {
  margin-bottom: 16px;

  :deep(.el-tabs__header) {
    margin-bottom: 0;
  }

  :deep(.el-tabs__nav-wrap::after) {
    height: 1px;
  }
}

.link-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  flex: 1;
  min-height: 200px;
  margin-bottom: 16px;
  align-content: start;
  align-items: stretch;
}

.link-card-grid__empty {
  grid-column: 1 / -1;
}

.link-card {
  display: flex;
  flex-direction: column;
  min-height: 248px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 14px;
  background: var(--el-fill-color-blank);
  overflow: hidden;
  cursor: pointer;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease,
    transform 0.15s ease;

  &:hover {
    border-color: var(--el-color-primary-light-5);
    box-shadow: 0 8px 24px rgb(37 99 235 / 10%);
  }

  &.is-selected {
    border-color: var(--el-color-primary);
    box-shadow:
      0 0 0 2px rgb(59 130 246 / 18%),
      0 10px 28px rgb(37 99 235 / 16%);
    transform: translateY(-1px);

    .link-card__body {
      background: linear-gradient(180deg, rgb(59 130 246 / 4%) 0%, transparent 72px);
    }
  }
}

.link-card__platform {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 10px 8px 14px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 0.02em;
}

.link-card__platform-leading {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  flex: 1;
}

.link-card__platform-icon {
  flex-shrink: 0;
  width: 18px;
  height: 18px;
  border-radius: 4px;
  object-fit: cover;
  background: rgb(255 255 255 / 20%);
}

.link-card__platform-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.link-card__copy {
  flex-shrink: 0;
  height: auto;
  padding: 2px;
  margin: 0;
  color: #fff !important;

  &:hover {
    opacity: 0.85;
  }
}

.link-card__body {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 14px 14px 10px;
  min-width: 0;
  min-height: 0;
}

.link-card__title {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.4;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.link-card__shop {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 4px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  min-width: 0;
}

.link-card__shop-icon {
  flex-shrink: 0;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  object-fit: cover;
  background: var(--el-fill-color-light);

  &.is-avatar {
    border-radius: 4px;
  }
}

.link-card__shop-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}

.link-card__date {
  margin: 0 0 8px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.link-card__sku-chips {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  gap: 6px;
  min-height: 28px;
  margin-top: auto;
  max-width: 100%;
  overflow: hidden;
}

.link-card__sku-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex: 0 1 auto;
  min-width: 0;
  max-width: calc(50% - 3px);
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 1.3;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);

  &.is-success {
    background: var(--el-color-success-light-9);
    border-color: var(--el-color-success-light-7);

    .link-card__sku-chip-profit {
      color: var(--el-color-success);
      font-weight: 600;
    }
  }

  &.is-danger {
    background: var(--el-color-danger-light-9);
    border-color: var(--el-color-danger-light-7);

    .link-card__sku-chip-profit {
      color: var(--el-color-danger);
      font-weight: 600;
    }
  }

  &.is-muted .link-card__sku-chip-profit {
    color: var(--el-text-color-secondary);
  }
}

.link-card__sku-chip-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
  color: var(--el-text-color-regular);
}

.link-card__sku-chip-profit {
  flex-shrink: 0;
  font-variant-numeric: tabular-nums;
}

.link-card__sku-more {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
}

.link-card__sku-ellipsis {
  letter-spacing: 0.05em;
}

.link-card__footer {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 10px;
  flex-shrink: 0;
  padding: 10px 14px 12px;
  border-top: 1px solid var(--el-border-color-extra-light);
  background: var(--el-fill-color-blank);
}

.link-card__footer-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  overflow: hidden;
}

.link-card__product-stack {
  --product-thumb-size: 28px;
  --product-thumb-step: calc(var(--product-thumb-size) / 3);
  --stack-count: 1;
  display: flex;
  align-items: center;
  flex: 0 0 auto;
  min-width: 0;
  overflow: hidden;
  width: calc(
    var(--product-thumb-size) + max(0, var(--stack-count) - 1) * var(--product-thumb-step)
  );
}

.link-card__product-thumb {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 var(--product-thumb-size);
  width: var(--product-thumb-size);
  height: var(--product-thumb-size);
  margin-left: calc(var(--product-thumb-size) * -2 / 3);
  border-radius: 8px;
  border: 2px solid var(--el-fill-color-blank);
  background: var(--el-fill-color-light);
  color: var(--el-text-color-placeholder);
  overflow: hidden;
  box-shadow: 0 1px 3px rgb(15 23 42 / 8%);
  box-sizing: border-box;

  &:first-child {
    margin-left: 0;
  }

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
}

.link-card__sku-count {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.link-card__footer-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-shrink: 0;
}

.link-fab {
  position: fixed;
  right: 32px;
  bottom: 32px;
  z-index: 20;
  width: 52px;
  height: 52px;
  box-shadow: 0 8px 24px rgb(37 99 235 / 35%);
}

.link-edit-dialog {
  :deep(.el-dialog__body) {
    padding-top: 8px;
  }
}

.link-edit-layout {
  display: flex;
  gap: 16px;
  align-items: stretch;
  min-height: 520px;
}

.link-edit-panel {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  background: var(--el-fill-color-blank);
}

.link-edit-panel--info {
  flex: 0 0 34%;
  max-width: 380px;
  padding: 16px 16px 12px;
  overflow: auto;
  display: flex;
  flex-direction: column;
}

.link-edit-panel--sku {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  padding: 16px 16px 12px;
}

.link-edit-sku-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.link-edit-sku-header__leading {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.link-edit-sku-add {
  flex-shrink: 0;
  height: 34px;
  padding: 0 14px;
  font-weight: 600;
}

.link-edit-sku-recalc {
  flex-shrink: 0;
  height: 34px;
  padding: 0 14px;
}

.link-edit-sku-stats {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-shrink: 0;
}

.link-edit-sku-stat {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.link-edit-sku-stat__label {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  line-height: 1.2;
}

.link-edit-sku-stat__value {
  font-size: 16px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--el-text-color-primary);

  &.is-success {
    color: #16a34a;
  }

  &.is-danger {
    color: #dc2626;
  }

  &.is-muted {
    color: var(--el-text-color-secondary);
  }
}

.link-edit-sku-grid-wrap {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-bottom: 4px;
}

.link-edit-sku-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  align-items: stretch;
}

.link-sku-card {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 280px;
  padding: 14px 14px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 14px;
  background: var(--el-fill-color-blank);
  transition: border-color 0.15s ease, box-shadow 0.15s ease;

  &:hover {
    box-shadow: 0 4px 16px rgba(15, 23, 42, 0.06);
  }

  &.is-profit {
    border-color: #d1fae5;
  }

  &.is-risk {
    border-color: #fecaca;
  }
}

.link-sku-card__topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}

.link-sku-card__stock {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  line-height: 1.2;
  background: #ecfdf5;
  color: #059669;

  &--alert {
    background: #fef2f2;
    color: #dc2626;
  }
}

.link-sku-card__actions {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  margin-left: auto;
}

.link-sku-card__summary {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  min-height: 96px;
  margin-bottom: 10px;
}

.link-sku-card__thumb {
  flex-shrink: 0;
  width: 68px;
  height: 68px;
  border-radius: 10px;
  overflow: hidden;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
}

.link-sku-card__thumb-img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.link-sku-card__thumb-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 22px;
  color: var(--el-text-color-placeholder);
}

.link-sku-card__meta {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;

  &--edit {
    padding-top: 2px;
  }
}

.link-sku-card__code {
  font-size: 12px;
  font-weight: 700;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.link-sku-card__gauge {
  position: relative;
  flex-shrink: 0;
  width: 84px;
  height: 84px;
}

.link-sku-card__gauge-ring {
  :deep(.el-progress-circle__track) {
    stroke: #e5e7eb;
  }
}

.profit-ring__inner {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  pointer-events: none;
  z-index: 1;
}

.profit-ring__rate {
  font-size: 13px;
  font-weight: 700;
  line-height: 1.15;
  color: var(--el-text-color-primary);
  font-variant-numeric: tabular-nums;
}

.profit-ring__amount {
  margin-top: 2px;
  font-size: 12px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;

  &.is-success {
    color: #16a34a;
  }

  &.is-danger {
    color: #dc2626;
  }

  &.is-warning {
    color: #d97706;
  }
}

.profit-ring__label {
  margin-top: 2px;
  font-size: 10px;
  color: var(--el-text-color-secondary);
}

.link-edit-sku-card__params {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 10px;
}

.link-edit-sku-card__param {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;

  :deep(.el-input-number) {
    width: 100%;
  }
}

.link-edit-sku-card__param-label {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

.link-sku-card__pricing {
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid var(--el-border-color-extra-light);
}

.link-sku-card__amounts {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 12px;
}

.amount-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  min-width: 0;
  text-align: center;
}

.amount-cell__label {
  min-height: 28px;
  font-size: 11px;
  color: var(--el-text-color-secondary);
  line-height: 1.35;
}

.amount-cell__value {
  font-size: 14px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--el-text-color-primary);
}

.amount-cell.is-cost {
  .amount-cell__label,
  .amount-cell__value {
    color: #3b82f6;
  }
}

.amount-cell.is-actual {
  .amount-cell__label {
    color: #16a34a;
  }
}

.is-risk .amount-cell.is-actual .amount-cell__label {
  color: #dc2626;
}

.link-edit-sku-card__actual-input {
  width: 100%;

  :deep(.el-input__wrapper) {
    padding-left: 8px;
    padding-right: 28px;
  }
}

.price-scale {
  padding: 0 1px;
}

.price-scale__track {
  position: relative;
  display: flex;
  height: 8px;
  border-radius: 999px;
  overflow: visible;
  background: #e2e8f0;
}

.price-scale__half {
  display: flex;
  width: 50%;
  height: 100%;
  min-width: 0;

  &.is-right.is-risk-mode {
    justify-content: flex-end;
  }
}

.price-scale__seg {
  height: 100%;
  min-width: 0;
  transition: width 0.2s ease;

  &.is-cost {
    background: #3b82f6;
    border-radius: 999px 0 0 999px;
  }

  &.is-gap {
    background: #cbd5e1;
  }

  &.is-profit {
    background: #22c55e;
    border-radius: 0 999px 999px 0;
  }

  &.is-risk {
    background: #f87171;
    border-radius: 0 999px 999px 0;
  }
}

.price-scale__marker {
  position: absolute;
  top: -4px;
  bottom: -4px;
  left: 50%;
  width: 0;
  border-left: 2px solid #111827;
  transform: translateX(-1px);
  pointer-events: none;
  z-index: 2;
}

.link-edit-panel__title {
  margin: 0;
  padding-left: 10px;
  border-left: 3px solid var(--el-color-primary);
  font-size: 15px;
  font-weight: 600;
  line-height: 1.3;
  color: var(--el-text-color-primary);
}

.link-edit-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.link-edit-info-form {
  flex: 1;
  min-height: 0;
  margin-top: 14px;

  :deep(.el-form-item) {
    margin-bottom: 14px;
  }

  :deep(.el-form-item__label) {
    padding-bottom: 4px;
    font-size: 13px;
    font-weight: 500;
    color: var(--el-text-color-regular);
    line-height: 1.4;
  }
}

.link-edit-products {
  flex-shrink: 0;
  margin-top: auto;
  padding-top: 14px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.link-edit-products__label {
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 500;
  color: var(--el-text-color-regular);
}

.link-edit-products__list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.link-edit-product-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 100%;
  padding: 4px 8px 4px 10px;
  border: 1px solid #bbf7d0;
  border-radius: 6px;
  background: #ecfdf5;
  color: #047857;
  font-size: 12px;
  line-height: 1.4;
}

.link-edit-product-tag__text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.link-edit-product-tag__remove {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: none;
  background: transparent;
  color: #6b7280;
  cursor: pointer;
  font-size: 14px;
  line-height: 1;

  &:hover {
    color: #dc2626;
  }
}

.link-edit-product-add {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border: 1px dashed #86efac;
  border-radius: 6px;
  background: #fff;
  color: #059669;
  font-size: 12px;
  line-height: 1.4;
  cursor: pointer;
  transition: border-color 0.15s ease, background 0.15s ease;

  .el-icon {
    font-size: 14px;
  }

  &:hover {
    border-color: #22c55e;
    background: #f0fdf4;
  }
}

.platform-url-open {
  cursor: pointer;
  color: var(--el-color-primary);
  font-size: 16px;

  &:hover {
    color: var(--el-color-primary-light-3);
  }
}

.link-edit-shop-select-wrap {
  position: relative;
  width: 100%;
}

.link-edit-shop-select-prefix {
  position: absolute;
  top: 50%;
  z-index: 2;
  width: 18px;
  height: 18px;
  object-fit: contain;
  transform: translateY(-50%);
  pointer-events: none;

  &.is-shop {
    left: 10px;

    &.is-avatar {
      border-radius: 50%;
      object-fit: cover;
    }
  }

  &.is-platform {
    left: 32px;
  }
}

.link-edit-shop-select-input {
  :deep(.el-select__wrapper) {
    padding-left: 56px;
  }
}

.link-edit-formula-collapse {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  overflow: hidden;

  :deep(.el-collapse) {
    border: none;
  }

  :deep(.el-collapse-item__header) {
    height: 40px;
    padding: 0 12px;
    font-size: 13px;
    font-weight: 600;
    background: var(--el-fill-color-blank);
    border-bottom: none;
  }

  :deep(.el-collapse-item__wrap) {
    border-top: 1px solid var(--el-border-color-extra-light);
  }

  :deep(.el-collapse-item__content) {
    padding: 8px 12px 10px;
  }
}

.link-edit-formula-collapse__text {
  margin: 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--el-text-color-secondary);
  word-break: break-word;
}

.required-mark {
  margin-left: 2px;
  color: var(--el-color-danger);
}

.sku-row-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
}

.inventory-line {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  line-height: 1.5;
}

.inventory-min {
  margin-top: 2px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.inv-alert-tag {
  flex-shrink: 0;
}

.profit-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

@media (max-width: 960px) {
  .link-edit-layout {
    flex-direction: column;
    min-height: auto;
  }

  .link-edit-panel--info {
    flex: none;
    max-width: none;
  }

  .link-edit-panel__table-wrap {
    min-height: 320px;
  }
}
</style>

<style lang="scss">
.link-edit-shop-select {
  .el-select-dropdown__item {
    height: auto;
    padding: 4px 8px;
    line-height: normal;
  }

  .shop-option-item {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
    padding: 4px 2px;
    font-size: 13px;
    line-height: 1.4;
    color: var(--el-text-color-primary);
  }

  .shop-option-item__shop {
    width: 22px;
    height: 22px;
    flex-shrink: 0;
    object-fit: contain;

    &.is-avatar {
      border-radius: 50%;
      object-fit: cover;
    }
  }

  .shop-option-item__name {
    font-weight: 500;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .shop-option-item__sep {
    flex-shrink: 0;
    color: var(--el-text-color-placeholder);
  }

  .shop-option-item__platform {
    width: 18px;
    height: 18px;
    flex-shrink: 0;
    object-fit: contain;
  }

  .shop-option-item__platform-name {
    flex-shrink: 0;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  .el-select-dropdown__item.is-selected .shop-option-item__name {
    color: var(--el-color-primary);
    font-weight: 600;
  }
}
</style>
