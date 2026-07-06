<template>
  <MobileBottomSheet
    v-model="localVisible"
    :loading="!shopData && localVisible"
    transition-name="shop-info-sheet"
  >
    <template #header>
      <div class="shop-info-sheet__header-left" v-if="shopData">
        <img
          :src="resolveShopIcon(shopData.name, shopData.platformName, shopData.platformCode, shopData.avatarUrl)"
          alt=""
          class="shop-info-sheet__avatar"
          :class="{ 'is-avatar': Boolean(shopData.avatarUrl?.trim()) }"
        />
        <div class="shop-info-sheet__header-info">
          <div class="shop-info-sheet__title-row">
            <img class="shop-info-sheet__title-icon" :src="schemeAAssets.starBlue" alt="" />
            <h2 class="shop-info-sheet__title">{{ shopData.name }}</h2>
          </div>
          <div class="shop-info-sheet__header-tags">
            <span class="shop-info-sheet__platform-tag">{{ shopData.platformName }}</span>
            <span
              class="shop-info-sheet__status-tag"
              :class="shopData.status === 'ENABLED' ? 'is-operating' : 'is-resting'"
            >
              {{ shopData.status === 'ENABLED' ? '正常营业' : '休息中' }}
            </span>
            <span v-if="shopData.nameEn" class="shop-info-sheet__name-en">{{ shopData.nameEn }}</span>
          </div>
        </div>
      </div>
    </template>

    <template v-if="shopData">
      <!-- Fee Section -->
      <div class="shop-info-sheet__section">
        <div class="shop-info-sheet__section-head">
          <img :src="schemeAAssets.starYellow" class="shop-info-sheet__section-star" alt="" />
          <span class="shop-info-sheet__section-title">手续费率</span>
        </div>
        <div class="shop-info-sheet__fee-grid">
          <div class="shop-info-sheet__fee-item">
            <span class="shop-info-sheet__fee-label">类目佣金</span>
            <span class="shop-info-sheet__fee-value">{{ formatPct(shopData.categoryCommissionPct) }}</span>
          </div>
          <div class="shop-info-sheet__fee-item">
            <span class="shop-info-sheet__fee-label">技术服务费</span>
            <span class="shop-info-sheet__fee-value">{{ formatPct(shopData.techServiceFeePct) }}</span>
          </div>
          <div class="shop-info-sheet__fee-item">
            <span class="shop-info-sheet__fee-label">支付手续费</span>
            <span class="shop-info-sheet__fee-value">{{ formatPct(shopData.paymentFeePct) }}</span>
          </div>
          <div class="shop-info-sheet__fee-item">
            <span class="shop-info-sheet__fee-label">推广扣点</span>
            <span class="shop-info-sheet__fee-value">{{ formatPct(shopData.promotionFeePct) }}</span>
          </div>
          <div class="shop-info-sheet__fee-item">
            <span class="shop-info-sheet__fee-label">综合扣点</span>
            <span class="shop-info-sheet__fee-value">{{ formatPct(shopData.otherFeePct) }}</span>
          </div>
          <div class="shop-info-sheet__fee-item">
            <span class="shop-info-sheet__fee-label">总扣点</span>
            <span class="shop-info-sheet__fee-value shop-info-sheet__fee-value--total">{{ formatPct(totalFeePct) }}</span>
          </div>
        </div>
      </div>

      <!-- Other Info -->
      <div class="shop-info-sheet__section">
        <div class="shop-info-sheet__section-head">
          <img :src="schemeAAssets.starBlue" class="shop-info-sheet__section-star" alt="" />
          <span class="shop-info-sheet__section-title">其他信息</span>
        </div>
        <div class="shop-info-sheet__info-list">
          <div class="shop-info-sheet__info-row" v-if="shopData.defaultReceiveProvince">
            <span class="shop-info-sheet__info-label">默认收货省</span>
            <span class="shop-info-sheet__info-value">{{ shopData.defaultReceiveProvince }}</span>
          </div>
          <div class="shop-info-sheet__info-row" v-if="shopData.annualPlatformFee">
            <span class="shop-info-sheet__info-label">平台年费</span>
            <span class="shop-info-sheet__info-value">¥{{ shopData.annualPlatformFee }}/年</span>
          </div>
          <div class="shop-info-sheet__info-row" v-if="shopData.depositAmount">
            <span class="shop-info-sheet__info-label">保证金</span>
            <span class="shop-info-sheet__info-value">¥{{ shopData.depositAmount }}</span>
          </div>
          <div class="shop-info-sheet__info-row" v-if="shopData.shippingInsuranceFee">
            <span class="shop-info-sheet__info-label">运费险</span>
            <span class="shop-info-sheet__info-value">¥{{ shopData.shippingInsuranceFee }}/单</span>
          </div>
          <div class="shop-info-sheet__info-row" v-if="shopData.remark">
            <span class="shop-info-sheet__info-label">备注</span>
            <span class="shop-info-sheet__info-value shop-info-sheet__info-value--remark">{{ shopData.remark }}</span>
          </div>
        </div>
      </div>
    </template>
  </MobileBottomSheet>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import MobileBottomSheet from '@/mobile/components/MobileBottomSheet.vue'
import { schemeAAssets } from '@/mobile/views/home/themes/scheme-a/assets.ts'
import { fetchShop } from '@/api/ecommerce/shop.ts'
import type { EcShop } from '@/api/ecommerce/shop.ts'
import { resolveShopIcon } from '@/utils/shopVisual.ts'

const props = defineProps<{
  modelValue: boolean
  shopId: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const shopData = ref<EcShop | null>(null)

// Local visible state to sync with parent v-model
const localVisible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

// Fetch shop detail when sheet opens with a valid shopId
watch(
  () => props.modelValue && props.shopId != null,
  async (shouldFetch) => {
    if (!shouldFetch || props.shopId == null) return
    shopData.value = null // reset to show loading
    try {
      const detail = await fetchShop(props.shopId)
      shopData.value = detail
    } catch {
      shopData.value = null
    }
  },
  { immediate: false },
)

function formatPct(v: number | null | undefined) {
  if (v == null) return '—'
  return `${Number(v).toFixed(2)}%`
}

const totalFeePct = computed(() => {
  if (!shopData.value) return 0
  const fields = [
    shopData.value.categoryCommissionPct,
    shopData.value.techServiceFeePct,
    shopData.value.paymentFeePct,
    shopData.value.promotionFeePct,
    shopData.value.otherFeePct,
  ]
  let sum = 0
  for (const f of fields) {
    if (f) sum += f
  }
  return Math.round(sum * 100) / 100
})
</script>

<style scoped lang="scss">
.shop-info-sheet__header-left {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.shop-info-sheet__avatar {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  object-fit: contain;
  flex-shrink: 0;
  border: 2px solid #e2e8f0;
  background: #f8fafc;
  padding: 3px;

  &.is-avatar {
    border-color: #2563eb;
  }
}

.shop-info-sheet__header-info {
  flex: 1;
  min-width: 0;
}

.shop-info-sheet__title-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.shop-info-sheet__title-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.shop-info-sheet__title {
  font-size: 18px;
  font-weight: 800;
  color: #1e293b;
  margin: 0;
  line-height: 1.3;
}

.shop-info-sheet__header-tags {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.shop-info-sheet__platform-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 11px;
  font-weight: 700;
}

.shop-info-sheet__status-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;

  &.is-operating {
    background: #f0fdf4;
    color: #16a34a;
  }

  &.is-resting {
    background: #f1f5f9;
    color: #94a3b8;
  }
}

.shop-info-sheet__name-en {
  font-size: 10px;
  color: #94a3b8;
  font-weight: 600;
}

.shop-info-sheet__section {
  margin-bottom: 16px;
}

.shop-info-sheet__section-head {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
}

.shop-info-sheet__section-star {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.shop-info-sheet__section-title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 14px;
  font-weight: 700;
  color: #475569;
}

.shop-info-sheet__fee-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.shop-info-sheet__fee-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1.5px dashed #e2e8f0;
}

.shop-info-sheet__fee-label {
  font-size: 11px;
  font-weight: 600;
  color: #94a3b8;
}

.shop-info-sheet__fee-value {
  font-size: 15px;
  font-weight: 800;
  color: #1e293b;

  &--total {
    color: #2563eb;
  }
}

.shop-info-sheet__info-list {
  display: flex;
  flex-direction: column;
  gap: 0;
  border-radius: 10px;
  overflow: hidden;
  border: 1.5px dashed #e2e8f0;
}

.shop-info-sheet__info-row {
  display: flex;
  align-items: flex-start;
  padding: 10px 12px;
  background: #f8fafc;
  gap: 12px;

  & + & {
    border-top: 1px dashed #e2e8f0;
  }
}

.shop-info-sheet__info-label {
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 600;
  color: #94a3b8;
  min-width: 72px;
}

.shop-info-sheet__info-value {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;

  &--remark {
    color: #64748b;
    font-weight: 500;
    word-break: break-all;
  }
}
</style>
