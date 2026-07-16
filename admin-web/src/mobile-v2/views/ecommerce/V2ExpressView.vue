<template>
  <V2Page>
    <div class="v2-ec v2-ec-express">
      <div class="v2-ec-search">
        <svg class="v2-ec-search__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
        </svg>
        <input
          v-model="searchKeyword"
          class="v2-ec-search__input"
          placeholder="搜索快递..."
          type="search"
        />
      </div>

      <div v-if="defaultStation" class="v2-ec-express__section">
        <div class="v2-ec-section-title">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#f59e0b" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
          </svg>
          <span>默认快递</span>
          <button
            type="button"
            class="v2-ec-express__calc-toggle"
            :class="{ 'is-active': calcOpen }"
            @click="handleCalcToggle"
          >
            🧮 {{ calcOpen ? '收起试算' : '试算' }}
          </button>
        </div>
        <div class="v2-ec-express__default-card" @click="handleCardClick(defaultStation)">
          <div class="v2-ec-express__default-pin">📌</div>
          <div class="v2-ec-express__default-body">
            <div class="v2-ec-express__default-header">
              <img
                :src="resolveExpressIcon(defaultStation)"
                :alt="defaultStation.name"
                class="v2-ec-express__default-icon"
                :class="{ 'is-avatar': Boolean(defaultStation.avatarUrl?.trim()) }"
              />
              <div class="v2-ec-express__default-info">
                <div class="v2-ec-express__default-name">{{ defaultStation.name }}</div>
                <div class="v2-ec-express__default-contact">📞 {{ defaultStation.contact || '-' }} · {{ defaultStation.address || '-' }}</div>
              </div>
            </div>
            <div class="v2-ec-express__default-details">
              <span class="v2-ec-express__tag v2-ec-express__tag--price">🏷️ 面单费 ¥{{ formatPrice(defaultStation.labelPrice) }}</span>
              <span class="v2-ec-express__tag v2-ec-express__tag--province">📍 覆盖{{ defaultStation.priceCount || 0 }}省</span>
              <span v-if="defaultStation.noticeCount && defaultStation.noticeCount > 0" class="v2-ec-express__tag v2-ec-express__tag--notice">⚠️ {{ defaultStation.noticeCount }}条须知</span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="filteredNormalStations.length" class="v2-ec-express__section">
        <div class="v2-ec-section-title">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#3b82f6" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/>
          </svg>
          <span>其他快递</span>
        </div>
        <div class="v2-ec-express__grid">
          <div
            v-for="item in filteredNormalStations"
            :key="item.id"
            class="v2-ec-express__grid-card"
            @click="handleCardClick(item)"
          >
            <img
              :src="resolveExpressIcon(item)"
              :alt="item.name"
              class="v2-ec-express__grid-icon"
              :class="{ 'is-avatar': Boolean(item.avatarUrl?.trim()) }"
            />
            <div class="v2-ec-express__grid-name">{{ item.name }}</div>
            <div class="v2-ec-express__grid-contact">{{ item.contact || '-' }}</div>
            <div class="v2-ec-express__grid-price">¥{{ formatPrice(item.labelPrice) }}</div>
            <div class="v2-ec-express__grid-province">📍{{ item.priceCount || 0 }}省</div>
          </div>
        </div>
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
  </V2Page>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import V2Page from '@/mobile-v2/components/V2Page.vue'
import ExpressDetailModal from '@/mobile/views/express/components/ExpressDetailModal.vue'
import ExpressCalcModal from '@/mobile/views/express/components/ExpressCalcModal.vue'
import { type EcExpressStation, fetchExpressStations } from '@/api/ecommerce/express.ts'
import { getEcommerceImageUrl } from '@/api/ecommerce/image.ts'

import './styles/v2-ecommerce.scss'

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
.v2-ec-express {
  &__header {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: max(16px, env(safe-area-inset-top)) 0 16px;
  }

  &__back {
    flex-shrink: 0;
    width: 36px;
    height: 36px;
    padding: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    color: var(--ec-stat-blue, #2563eb);
    font-weight: 700;
    cursor: pointer;
    background: var(--wr-card, #fff);
    border: 1px solid var(--wr-border, #e8ecef);
    border-radius: 10px;
    transition: transform 0.2s ease;

    &:active {
      transform: scale(0.9);
    }
  }

  &__title {
    margin: 0;
    font-size: 22px;
    font-weight: 800;
    color: var(--wr-text, #1e293b);
  }

  &__section {
    margin-bottom: 20px;
  }

  &__default-card {
    position: relative;
    background: #fffbeb;
    border-radius: 12px;
    border: 1px solid #fde68a;
    cursor: pointer;
    transition: transform 0.15s;

    &:active {
      transform: scale(0.98);
    }
  }

  &__default-pin {
    position: absolute;
    top: -8px;
    right: 16px;
    font-size: 24px;
    z-index: 1;
  }

  &__default-body {
    padding: 16px;
  }

  &__default-header {
    display: flex;
    gap: 12px;
    margin-bottom: 12px;
  }

  &__default-icon {
    font-size: 32px;
    width: 40px;
    height: 40px;
    border-radius: 50%;
    object-fit: cover;
    background: #f3f4f6;
    flex-shrink: 0;

    &.is-avatar {
      font-size: inherit;
    }
  }

  &__default-info {
    flex: 1;
  }

  &__default-name {
    font-size: 18px;
    font-weight: 800;
    color: #1e293b;
  }

  &__default-contact {
    font-size: 13px;
    color: #64748b;
    margin-top: 4px;
  }

  &__default-details {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  &__tag {
    font-size: 12px;
    font-weight: 600;
    padding: 4px 10px;
    border-radius: 6px;

    &--price {
      color: #f97316;
      background: #fff7ed;
    }

    &--province {
      color: #16a34a;
      background: #dcfce7;
    }

    &--notice {
      color: #ef4444;
      background: #fef2f2;
    }
  }

  &__grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }

  &__grid-card {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    padding: 14px 8px 12px;
    background: var(--wr-card, #ffffff);
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    cursor: pointer;
    transition: box-shadow 0.2s, transform 0.15s;
    border: 1px solid var(--wr-border, #e8ecef);
    min-height: 120px;

    &:active {
      transform: scale(0.96);
    }
  }

  &__grid-icon {
    font-size: 28px;
    width: 40px;
    height: 40px;
    border-radius: 50%;
    object-fit: cover;
    background: #f3f4f6;

    &.is-avatar {
      font-size: inherit;
    }
  }

  &__grid-name {
    font-size: 14px;
    font-weight: 800;
    color: #1e293b;
    margin-top: 4px;
    text-align: center;
  }

  &__grid-contact {
    font-size: 11px;
    color: #64748b;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 100%;
    text-align: center;
  }

  &__grid-price {
    font-size: 13px;
    font-weight: 700;
    color: #f97316;
    margin-top: 2px;
  }

  &__grid-province {
    font-size: 10px;
    color: #94a3b8;
    text-align: center;
  }

  &__calc-toggle {
    margin-left: auto;
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
}
</style>
