<template>
  <MobileBottomSheet
    v-model="localVisible"
    :loading="!stationData && localVisible"
  >
    <template #header>
      <div class="express-detail-modal__header-left" v-if="stationData">
        <img
          v-if="stationData.avatarUrl?.trim()"
          :src="getEcommerceImageUrl(stationData.avatarUrl)"
          :alt="stationData.name"
          class="express-detail-modal__avatar"
        />
        <span v-else class="express-detail-modal__icon-emoji">{{ getExpressIcon(stationData.name) }}</span>
        <div class="express-detail-modal__header-info">
          <div class="express-detail-modal__title-row">
            <img class="express-detail-modal__title-icon" :src="schemeAAssets.starBlue" alt="" />
            <h2 class="express-detail-modal__title">{{ stationData.name }}</h2>
          </div>
          <div class="express-detail-modal__header-tags">
            <span
              v-if="stationData.isDefault"
              class="express-detail-modal__tag express-detail-modal__tag--default"
            >
              ⭐ 默认快递
            </span>
            <span v-if="stationData.nameAliases?.length" class="express-detail-modal__alias">
              又名：{{ stationData.nameAliases.join('、') }}
            </span>
          </div>
        </div>
      </div>
    </template>

    <template v-if="stationData">
      <!-- 基本信息 -->
      <div class="express-detail-modal__section">
        <div class="express-detail-modal__section-head">
          <img :src="schemeAAssets.starYellow" class="express-detail-modal__section-star" alt="" />
          <span class="express-detail-modal__section-title">基本信息</span>
        </div>
        <SchemeADoodleFrame color="#2563eb" :shadow="false" class="express-detail-modal__box">
          <div class="express-detail-modal__info-grid">
            <div class="express-detail-modal__info-item">
              <span class="express-detail-modal__info-label">📞 联系人</span>
              <span class="express-detail-modal__info-value">{{ stationData.contact || '-' }}</span>
            </div>
            <div class="express-detail-modal__info-item">
              <span class="express-detail-modal__info-label">📍 地址</span>
              <span class="express-detail-modal__info-value">{{ stationData.address || '-' }}</span>
            </div>
            <div class="express-detail-modal__info-item">
              <span class="express-detail-modal__info-label">🏷️ 面单费</span>
              <span class="express-detail-modal__info-value express-detail-modal__info-value--price">
                ¥{{ formatPrice(stationData.labelPrice) }}
              </span>
            </div>
            <div class="express-detail-modal__info-item">
              <span class="express-detail-modal__info-label">🌍 覆盖省份</span>
              <span class="express-detail-modal__info-value">{{ stationData.priceCount || 0 }} 个</span>
            </div>
          </div>
        </SchemeADoodleFrame>
      </div>

      <!-- 快递须知 -->
      <div v-if="stationData.notices?.length" class="express-detail-modal__section">
        <div class="express-detail-modal__section-head express-detail-modal__section-head--clickable" @click="noticeCollapsed = !noticeCollapsed">
          <img :src="schemeAAssets.squiggleRed" class="express-detail-modal__section-star" alt="" />
          <span class="express-detail-modal__section-title">📋 快递须知</span>
          <span class="express-detail-modal__toggle">{{ noticeCollapsed ? '▼' : '▲' }}</span>
        </div>
        <template v-if="!noticeCollapsed">
          <SchemeADoodleFrame
            v-for="notice in sortedNotices"
            :key="notice.id"
            :color="notice.highlightRed ? '#ef4444' : '#94a3b8'"
            :shadow="false"
            class="express-detail-modal__notice-box"
          >
            <div
              class="express-detail-modal__notice"
              :class="{ 'express-detail-modal__notice--highlight': notice.highlightRed }"
            >
              <span class="express-detail-modal__notice-content">{{ notice.content }}</span>
            </div>
          </SchemeADoodleFrame>
        </template>
      </div>

      <!-- 价格明细 -->
      <div v-if="stationData.prices?.length" class="express-detail-modal__section">
        <div class="express-detail-modal__section-head express-detail-modal__section-head--clickable" @click="priceCollapsed = !priceCollapsed">
          <img :src="schemeAAssets.starBlueOutline" class="express-detail-modal__section-star" alt="" />
          <span class="express-detail-modal__section-title">💰 价格明细</span>
          <span class="express-detail-modal__toggle">{{ priceCollapsed ? '▼' : '▲' }}</span>
        </div>
        <template v-if="!priceCollapsed">
          <!-- 搜索框 -->
          <MobileDoodleSearch
            v-if="stationData.prices?.length"
            v-model="priceSearchKeyword"
            placeholder="搜索地区..."
            color="#16a34a"
            class="express-detail-modal__price-search"
          />

          <SchemeADoodleFrame
            v-for="price in filteredPrices"
            :key="price.id"
            color="#16a34a"
            :shadow="false"
            class="express-detail-modal__price-box"
          >
            <div class="express-detail-modal__price-province">{{ price.provinceName }}</div>
            <div class="express-detail-modal__price-details">
              <div v-if="price.priceW03Kg != null" class="express-detail-modal__price-row">
                <span>≤0.3kg</span>
                <span class="express-detail-modal__price-value">¥{{ formatPrice(price.priceW03Kg) }}</span>
              </div>
              <div v-if="price.priceW05Kg != null" class="express-detail-modal__price-row">
                <span>≤0.5kg</span>
                <span class="express-detail-modal__price-value">¥{{ formatPrice(price.priceW05Kg) }}</span>
              </div>
              <div v-if="price.priceW1Kg != null" class="express-detail-modal__price-row">
                <span>≤1kg</span>
                <span class="express-detail-modal__price-value">¥{{ formatPrice(price.priceW1Kg) }}</span>
              </div>
              <div v-if="price.priceW15Kg != null" class="express-detail-modal__price-row">
                <span>≤1.5kg</span>
                <span class="express-detail-modal__price-value">¥{{ formatPrice(price.priceW15Kg) }}</span>
              </div>
              <div v-if="price.priceW2Kg != null" class="express-detail-modal__price-row">
                <span>≤2kg</span>
                <span class="express-detail-modal__price-value">¥{{ formatPrice(price.priceW2Kg) }}</span>
              </div>
              <div v-if="price.priceW25Kg != null" class="express-detail-modal__price-row">
                <span>≤2.5kg</span>
                <span class="express-detail-modal__price-value">¥{{ formatPrice(price.priceW25Kg) }}</span>
              </div>
              <div v-if="price.priceW3Kg != null" class="express-detail-modal__price-row">
                <span>≤3kg</span>
                <span class="express-detail-modal__price-value">¥{{ formatPrice(price.priceW3Kg) }}</span>
              </div>
              <div v-if="price.over3FirstPrice != null" class="express-detail-modal__price-row">
                <span>续重首重</span>
                <span class="express-detail-modal__price-value">¥{{ formatPrice(price.over3FirstPrice) }}</span>
              </div>
              <div v-if="price.over3AdditionalPrice != null" class="express-detail-modal__price-row">
                <span>续重每kg</span>
                <span class="express-detail-modal__price-value">¥{{ formatPrice(price.over3AdditionalPrice) }}</span>
              </div>
            </div>
          </SchemeADoodleFrame>

          <div
            v-if="stationData.prices?.length && filteredPrices.length === 0"
            class="express-detail-modal__empty"
          >
            未找到匹配的地区
          </div>
        </template>
      </div>
    </template>
  </MobileBottomSheet>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import MobileBottomSheet from '@/mobile/components/MobileBottomSheet.vue'
import MobileDoodleSearch from '@/mobile/components/MobileDoodleSearch.vue'
import SchemeADoodleFrame from '@/mobile/views/home/themes/scheme-a/SchemeADoodleFrame.vue'
import { schemeAAssets } from '@/mobile/views/home/themes/scheme-a/assets.ts'
import { fetchExpressStation, type EcExpressStation } from '@/api/ecommerce/express.ts'
import { getEcommerceImageUrl } from '@/api/ecommerce/image.ts'

const props = defineProps<{
  modelValue: boolean
  stationId: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const stationData = ref<EcExpressStation | null>(null)
const noticeCollapsed = ref(true)
const priceCollapsed = ref(true)

const localVisible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

// ===== 红色须知置顶 =====
const sortedNotices = computed(() => {
  const notices = stationData.value?.notices
  if (!notices?.length) return []
  return [...notices].sort((a, b) => {
    if (a.highlightRed && !b.highlightRed) return -1
    if (!a.highlightRed && b.highlightRed) return 1
    return 0
  })
})

// ===== 地区搜索 =====
const priceSearchKeyword = ref('')
const filteredPrices = computed(() => {
  if (!stationData.value?.prices?.length) return []
  const kw = priceSearchKeyword.value.trim().toLowerCase()
  if (!kw) return stationData.value.prices
  return stationData.value.prices.filter((p) => p.provinceName.toLowerCase().includes(kw))
})

// Fetch station detail when sheet opens with a valid stationId
watch(
  () => props.modelValue && props.stationId != null,
  async (shouldFetch) => {
    if (!shouldFetch || props.stationId == null) return
    stationData.value = null
    try {
      const detail = await fetchExpressStation(props.stationId)
      stationData.value = detail
    } catch {
      stationData.value = null
    }
  },
  { immediate: false },
)

function getExpressIcon(name: string): string {
  const nameLower = name.toLowerCase()
  if (nameLower.includes('顺丰')) return '💎'
  if (nameLower.includes('中通')) return '🟢'
  if (nameLower.includes('圆通')) return '🔴'
  if (nameLower.includes('申通')) return '🟡'
  if (nameLower.includes('韵达')) return '🔵'
  if (nameLower.includes('京东')) return '🔷'
  if (nameLower.includes('极兔')) return '🐰'
  if (nameLower.includes('德邦')) return '📦'
  if (nameLower.includes('ems')) return '✉️'
  if (nameLower.includes('百世')) return '🌐'
  return '📦'
}

function formatPrice(price?: number | null): string {
  if (price == null) return '0.00'
  return Number(price).toFixed(2)
}
</script>

<style scoped lang="scss">
.express-detail-modal__header-left {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.express-detail-modal__avatar {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  object-fit: cover;
  flex-shrink: 0;
  border: 2px solid #e2e8f0;
  background: #f8fafc;
}

.express-detail-modal__icon-emoji {
  font-size: 32px;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: 10px;
  border: 2px solid #e2e8f0;
  background: #f8fafc;
}

.express-detail-modal__header-info {
  flex: 1;
  min-width: 0;
}

.express-detail-modal__title-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.express-detail-modal__title-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.express-detail-modal__title {
  font-size: 18px;
  font-weight: 800;
  color: #1e293b;
  margin: 0;
  line-height: 1.3;
}

.express-detail-modal__header-tags {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.express-detail-modal__tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;

  &--default {
    background: #fffbeb;
    color: #fbbf24;
  }
}

.express-detail-modal__alias {
  font-size: 10px;
  color: #94a3b8;
}

.express-detail-modal__section {
  margin-bottom: 16px;
}

.express-detail-modal__section-head {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;

  &--clickable {
    cursor: pointer;
    user-select: none;
    -webkit-tap-highlight-color: transparent;
  }
}

.express-detail-modal__toggle {
  font-size: 10px;
  color: #94a3b8;
  transition: transform 0.2s;
}

.express-detail-modal__section-star {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.express-detail-modal__section-title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 14px;
  font-weight: 700;
  color: #475569;
  flex: 1;
}

.express-detail-modal__box {
  :deep(.sa-doodle-frame__body) {
    padding: 12px;
    background: #f8fafc;
  }
}

.express-detail-modal__info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.express-detail-modal__info-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #fff;
  border: 1.5px dashed #e2e8f0;
}

.express-detail-modal__info-label {
  font-size: 11px;
  font-weight: 600;
  color: #94a3b8;
}

.express-detail-modal__info-value {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  word-break: break-all;

  &--price {
    color: #f97316;
  }
}

.express-detail-modal__notice-box {
  margin-bottom: 8px;

  &:last-child {
    margin-bottom: 0;
  }

  :deep(.sa-doodle-frame__body) {
    padding: 10px 12px;
    background: #fef2f2;
  }
}

.express-detail-modal__notice {
  display: flex;
  gap: 8px;
  font-size: 13px;
  color: #475569;
  padding: 12px;

  &--highlight {
    font-weight: 700;
    color: #ef4444;
  }

  .express-detail-modal__notice-bullet {
    flex-shrink: 0;
  }

  .express-detail-modal__notice-content {
    flex: 1;
  }
}

.express-detail-modal__price-search {
  margin-bottom: 10px;
}

.express-detail-modal__price-box {
  margin-bottom: 8px;

  &:last-child {
    margin-bottom: 0;
  }

  :deep(.sa-doodle-frame__body) {
    padding: 24px;
    background: #f0fdf4;
  }
}

.express-detail-modal__price-province {
  font-size: 14px;
  font-weight: 800;
  color: #1e293b;
  margin-bottom: 8px;
  padding-bottom: 6px;
  border-bottom: 1.5px dashed #cbd5e1;
}

.express-detail-modal__price-details {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px 12px;
}

.express-detail-modal__price-row {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #64748b;

  .express-detail-modal__price-value {
    font-weight: 700;
    color: #f97316;
  }
}

.express-detail-modal__empty {
  text-align: center;
  padding: 20px;
  color: #94a3b8;
  font-size: 13px;
}
</style>
