<template>
  <MobilePage>
    <div v-loading="ec.loading.value" class="v2-ec">
      <div class="v2-ec-header">
        <div class="v2-ec-search">
          <svg class="v2-ec-search__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
          <input
              v-model="ec.searchQuery.value"
              class="v2-ec-search__input"
              :placeholder="ec.t('mobile.home.searchPlaceholder')"
              type="search"
          />
        </div>

        <div class="v2-ec-header__stats v2-ec-stats">
          <div
            v-for="card in ec.overviewCards.value"
            :key="card.key"
            class="v2-ec-stat-card"
            :style="{ background: getStatBg(card.color) }"
            @click="navigateByKey(card.module)"
          >
            <div class="v2-ec-stat-card__icon" :style="{ background: card.color, color: '#fff' }">
              <img :src="card.icon" :alt="card.label" />
            </div>
            <div class="v2-ec-stat-card__info">
              <div class="v2-ec-stat-card__value" :style="{ color: card.color }">{{ card.value }}</div>
              <div class="v2-ec-stat-card__label">{{ card.label }}</div>
            </div>
          </div>
        </div>
      </div>



      <div v-if="ec.pendingShipCount.value > 0" class="v2-ec-pending-section">
        <div class="v2-ec-section-title">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#ea580c" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 01-8 0"/>
          </svg>
          <span>{{ ec.t('mobile.ecommerce.pendingOrders') }}</span>
        </div>
        <div
          v-for="order in ec.displayPendingOrders.value"
          :key="order.id"
          class="v2-ec-pending-item"
          @click="ec.openModule('order')"
        >
          <span class="v2-ec-pending-item__dot" :class="order._isWarning ? 'status--warning' : 'status--paid'" />
          <div class="v2-ec-pending-item__info">
            <div class="v2-ec-pending-item__title">{{ ec.orderTitle(order) }}</div>
            <div class="v2-ec-pending-item__meta">{{ ec.orderMeta(order) }}</div>
          </div>
          <span class="v2-ec-pending-item__tag" :class="order._isWarning ? 'tag--warning' : 'tag--primary'">
            {{ ec.orderTag(order) }}
          </span>
        </div>
      </div>

      <div class="v2-ec-section-title">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--wr-text, #333)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/>
        </svg>
        <span>{{ ec.t('mobile.home.allFunctions') }}</span>
      </div>

      <div v-if="ec.filteredModules.value.length" class="v2-ec-module-grid">
        <div
          v-for="item in ec.filteredModules.value"
          :key="item.key"
          class="v2-ec-module"
          :style="{ position: 'relative' }"
          @click="ec.openModule(item)"
        >
          <div class="v2-ec-module__icon-wrap" :style="{ background: getModuleBg(item.key) }">
            <img :src="item.icon" :alt="ec.t(item.labelKey)" />
          </div>
          <div class="v2-ec-module__name">{{ ec.t(item.labelKey) }}</div>
          <div
            v-if="item.key === 'order' && ec.pendingShipCount.value > 0"
            class="v2-ec-module__badge"
          >
            {{ ec.pendingShipCount.value > 99 ? '99+' : ec.pendingShipCount.value }}
          </div>
        </div>
      </div>
      <div v-else class="v2-ec-empty">{{ ec.t('mobile.home.searchEmpty') }}</div>
    </div>
  </MobilePage>
</template>

<script setup lang="ts">
import {onMounted, provide} from 'vue'
import {useRouter} from 'vue-router'
import MobilePage from '@/mobile/components/MobilePage.vue'
import {MOBILE_ECOMMERCE_KEY} from '@/mobile/views/ecommerce/mobileEcommerceContext'
import {useMobileEcommerce} from '@/mobile/views/ecommerce/useMobileEcommerce'
import {mobileEcommercePathForModule} from '@/mobile/views/ecommerce/data/mobile-ecommerce-modules'
import type {EcommerceWorkbenchModule} from '@/data/ecommerce-nav'

import './styles/v2-ecommerce.scss'

const ec = useMobileEcommerce()
provide(MOBILE_ECOMMERCE_KEY, ec)
const router = useRouter()

const moduleBgMap: Record<string, string> = {
  monthlySettlement: '#fff7ed',
  order: '#eff6ff',
  inventory: '#f0fdf4',
  product: '#eef2ff',
  express: '#fff7ed',
  factory: '#f5f3ff',
  platformShop: '#f0fdf4',
  carton: '#f8fafc',
}

function getModuleBg(key: string): string {
  return moduleBgMap[key] || '#f8fafc'
}

function getStatBg(color: string): string {
  const bgMap: Record<string, string> = {
    '#22c55e': '#f0fdf4',
    '#3b82f6': '#eff6ff',
    '#f59e0b': '#fffbeb',
    '#94a3b8': '#f8fafc',
  }
  return bgMap[color] || '#f8fafc'
}

function navigateByKey(module: EcommerceWorkbenchModule) {
  router.push(mobileEcommercePathForModule(module))
}

onMounted(() => {
  void ec.init()
})
</script>

<style scoped lang="scss">
.v2-ec {
  .v2-ec-header {
    margin-bottom: 16px;

    &__top {
      margin-bottom: 14px;
    }

    &__title-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    &__title {
      margin: 0;
      font-size: 20px;
      font-weight: 700;
      color: var(--wr-text, #333);
    }

    &__date {
      font-size: 12px;
      color: var(--wr-text-secondary, #666);
      font-weight: 500;
    }

    &__desc {
      margin: 4px 0 0;
      font-size: 13px;
      color: var(--wr-text-secondary, #666);
    }
  }

  .v2-ec-empty {
    padding: 24px;
    text-align: center;
    color: var(--wr-muted, #999);
    font-size: 14px;
  }
}
</style>
