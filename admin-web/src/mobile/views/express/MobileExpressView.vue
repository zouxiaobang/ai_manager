<template>
  <div class="mobile-express-view">
    <div class="express-page">

      <MobilePageHeader title="🚚 快递管理" @back="$router.back()" />

      <div class="mobile-express-view__content">
        <MobileDoodleSearch
            v-model="searchKeyword"
            placeholder="搜索快递..."
        />
      </div>

      <div class="section" v-if="defaultStation">
        <MobileSectionHeader
          :icon="assets.starYellow"
          title="默认快递"
        >
          <template #actions>
            <button
              type="button"
              class="calc-toggle"
              :class="{ 'is-active': calcOpen }"
              @click="handleCalcToggle"
            >
              🧮 {{ calcOpen ? '收起试算' : '试算' }}
            </button>
          </template>
        </MobileSectionHeader>
        <SchemeADoodleFrame
          shape="rect"
          color="#fbbf24"
          class="default-card"
          @click="handleCardClick(defaultStation)"
        >
          <div class="default-card__pin">📌</div>
          <div class="default-card__content">
            <div class="default-card__header">
              <img
                :src="resolveExpressIcon(defaultStation)"
                :alt="defaultStation.name"
                class="default-card__icon"
                :class="{ 'is-avatar': Boolean(defaultStation.avatarUrl?.trim()) }"
              />
              <div class="default-card__info">
                <div class="default-card__name">{{ defaultStation.name }}</div>
                <div class="default-card__contact">📞 {{ defaultStation.contact || '-' }} · {{ defaultStation.address || '-' }}</div>
              </div>
            </div>
            <div class="default-card__details">
              <span class="default-card__tag price-tag">🏷️ 面单费 ¥{{ formatPrice(defaultStation.labelPrice) }}</span>
              <span class="default-card__tag province-tag">📍 覆盖{{ defaultStation.priceCount || 0 }}省</span>
              <span v-if="defaultStation.noticeCount && defaultStation.noticeCount > 0" class="default-card__tag notice-tag">⚠️ {{ defaultStation.noticeCount }}条须知</span>
            </div>
          </div>
        </SchemeADoodleFrame>
      </div>

      <div class="section" v-if="filteredNormalStations.length">
        <MobileSectionHeader
          :icon="assets.starBlueOutline"
          title="其他快递"
        />
        <MobileCardGrid
          :items="filteredNormalStations"
          empty-text="暂无快递"
        >
          <template #card="{ item }">
            <SchemeADoodleFrame
              shape="rect"
              color="#16a34a"
              class="shop-card"
              @click="handleCardClick(item)"
            >
              <img
                :src="resolveExpressIcon(item as any)"
                :alt="item.name"
                class="shop-card__icon"
                :class="{ 'is-avatar': Boolean(item.avatarUrl?.trim()) }"
              />
              <div class="shop-card__name">{{ item.name }}</div>
              <div class="shop-card__contact">{{ item.contact || '-' }}</div>
              <div class="shop-card__price">¥{{ formatPrice(item.labelPrice) }}</div>
              <div class="shop-card__province">📍{{ item.priceCount || 0 }}省</div>
            </SchemeADoodleFrame>
          </template>
        </MobileCardGrid>
      </div>


    </div>

    <ExpressDetailModal
      v-model="detailOpen"
      :station-id="selectedStationId"
    />

    <ExpressCalcModal
      v-model="calcOpen"
      :default-station-id="defaultStation?.id"
    />
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {schemeAAssets as assets} from '@/mobile/views/home/themes/scheme-a/assets.ts'
import MobileDoodleSearch from '@/mobile/components/MobileDoodleSearch.vue'
import MobileSectionHeader from '@/mobile/components/MobileSectionHeader.vue'
import SchemeADoodleFrame from '@/mobile/views/home/themes/scheme-a/SchemeADoodleFrame.vue'
import ExpressDetailModal from '@/mobile/views/express/components/ExpressDetailModal.vue'
import ExpressCalcModal from '@/mobile/views/express/components/ExpressCalcModal.vue'
import {type EcExpressStation, fetchExpressStations,} from '@/api/ecommerce/express.ts'
import {getEcommerceImageUrl} from '@/api/ecommerce/image.ts'
import MobilePageHeader from "@/mobile/components/MobilePageHeader.vue";
import MobileCardGrid from "@/mobile/components/MobileCardGrid.vue";

const searchKeyword = ref('')
const stations = ref<EcExpressStation[]>([])
const loading = ref(false)
const detailOpen = ref(false)
const selectedStationId = ref<number | null>(null)
const calcOpen = ref(false)

const filteredStations = computed(() => {
  let result = stations.value

  if (searchKeyword.value.trim()) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(
      (s) =>
        s.name.toLowerCase().includes(keyword) ||
        s.contact?.toLowerCase().includes(keyword) ||
        s.address?.toLowerCase().includes(keyword),
    )
  }

  return result
})

const defaultStation = computed(() => filteredStations.value.find((s) => s.isDefault) || null)

const filteredNormalStations = computed(() =>
  filteredStations.value.filter((s) => !s.isDefault),
)

function resolveExpressIcon(station: EcExpressStation): string {
  if (station.avatarUrl?.trim()) {
    return getEcommerceImageUrl(station.avatarUrl)
  }
  const nameLower = station.name.toLowerCase()
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
  return price.toFixed(2)
}

async function loadStations() {
  loading.value = true
  try {
    const res = await fetchExpressStations()
    stations.value = res.records || []
  } catch (e) {
    console.error('加载快递站点失败', e)
  } finally {
    loading.value = false
  }
}

function handleCardClick(station: EcExpressStation) {
  selectedStationId.value = station.id
  detailOpen.value = true
}

function handleCalcToggle() {
  calcOpen.value = !calcOpen.value
}

onMounted(() => {
  setTimeout(() => {
    const main = document.querySelector('.mobile-app__main')
    if (main instanceof HTMLElement) {
      main.scrollTop = 0
    }
  }, 50)
  loadStations()
})
</script>

<style scoped lang="scss">
.mobile-express-view {
  min-height: 100vh;
  background: #faf8f5;
  width: 100%;
  overflow-x: hidden;
}

.express-page {
  background: #fff;
  min-height: 100vh;
  width: 100%;
  overflow-x: hidden;
  box-sizing: border-box;
}

.mobile-express-view__content {
  padding: 0 16px;
  background: #fff;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: max(16px, env(safe-area-inset-top)) 16px 16px;
}

.back-btn {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #2563eb;
  font-weight: 700;
  cursor: pointer;
  background: #fff;
  transition: transform 0.2s ease;

  :deep(.sa-doodle-frame__body) {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    padding: 0;
  }

  &:active {
    transform: scale(0.9);
  }
}

.back-icon {
  font-weight: bold;
}

.page-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-title {
  font-size: 24px;
  font-weight: 800;
  color: #1e293b;
  margin: 0;
}

.section {
  padding: 0 16px;
  margin-bottom: 20px;
}

.default-card {
  position: relative;
  cursor: pointer;
  transition: transform 0.15s;

  &:active {
    transform: scale(0.98);
  }

  :deep(.sa-doodle-frame__body) {
    padding: 16px;
    background: #fffbeb;
  }

  &__pin {
    position: absolute;
    top: -8px;
    right: 16px;
    font-size: 24px;
    z-index: 1;
  }

  &__content {
    position: relative;
    padding: 12px;
  }

  &__header {
    display: flex;
    gap: 12px;
    margin-bottom: 12px;
  }

  &__icon {
    font-size: 32px;
    width: 36px;
    height: 36px;
    border-radius: 50%;
    object-fit: cover;
    background: #f3f4f6;
    flex-shrink: 0;

    &.is-avatar {
      font-size: inherit;
    }
  }

  &__info {
    flex: 1;
  }

  &__name {
    font-size: 18px;
    font-weight: 800;
    color: #1e293b;
  }

  &__contact {
    font-size: 13px;
    color: #64748b;
    margin-top: 4px;
  }

  &__details {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 12px;
  }

  &__tag {
    font-size: 12px;
    font-weight: 600;
    color: #64748b;
    background: #fff;
    padding: 4px 8px;
    border-radius: 6px;

    &.price-tag {
      color: #f97316;
      background: #fff7ed;
    }

    &.province-tag {
      color: #16a34a;
      background: #dcfce7;
    }

    &.notice-tag {
      color: #ef4444;
      background: #fef2f2;
    }
  }

  &__actions {
    display: flex;
    gap: 10px;
    padding-top: 12px;
    border-top: 2px dashed #fcd34d;
  }
}

.shop-card {
  cursor: pointer;
  transition: transform 0.15s;

  &:active {
    transform: scale(0.97);
  }

  :deep(.sa-doodle-frame__body) {
    padding: 12px 8px;
    text-align: center;
    background: #f0fdf4;
  }

  &__icon {
    font-size: 28px;
    width: 36px;
    height: 36px;
    border-radius: 50%;
    object-fit: cover;
    background: #f3f4f6;
    display: inline-block;

    &.is-avatar {
      font-size: inherit;
    }
  }

  &__name {
    font-size: 14px;
    font-weight: 800;
    color: #1e293b;
    margin-top: 6px;
  }

  &__contact {
    font-size: 11px;
    color: #64748b;
    margin-top: 2px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__price {
    font-size: 13px;
    font-weight: 700;
    color: #f97316;
    margin-top: 4px;
  }

  &__province {
    font-size: 10px;
    color: #94a3b8;
    margin-top: 2px;
  }
}

.action-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.15s;

  &:active {
    transform: scale(0.95);
  }

  &--delete {
    background: #fee2e2;
    color: #ef4444;
  }
}

.calc-toggle {
  border: 1.5px dashed #f97316;
  background: #fff7ed;
  color: #f97316;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;

  &:active {
    transform: scale(0.95);
  }

  &.is-active {
    background: #f97316;
    color: #fff;
  }
}
</style>

<style lang="scss">
.mobile-express-view :deep(.sa-doodle-frame) {
  overflow: hidden !important;
}

.mobile-express-view :deep(.sa-doodle-frame__stroke) {
  inset: 0 !important;
  width: 100% !important;
  height: 100% !important;
  overflow: hidden !important;
}
</style>
