<template>
  <V2Page>
    <div v-loading="shop.loading.value" class="v2-ec">
      <div class="v2-ec-search">
        <svg class="v2-ec-search__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
        </svg>
        <input
          v-model="shop.searchQuery.value"
          class="v2-ec-search__input"
          placeholder="搜索店铺..."
          type="search"
        />
      </div>

      <div class="v2-ec-tabs">
        <button
          v-for="platform in shop.platformList.value"
          :key="platform.id ?? 'all'"
          class="v2-ec-tab"
          :class="{ 'is-active': shop.activePlatformId.value === platform.id }"
          @click="shop.activePlatformId.value = platform.id"
        >
          {{ platform.icon }} {{ platform.name }} ({{ platform.count }})
        </button>
      </div>

      <div v-if="shop.filteredShops.value.length" class="v2-ec-shop-grid">
        <div
          v-for="item in shop.filteredShops.value"
          :key="item.id"
          class="v2-ec-shop-card"
          :class="{ 'v2-ec-shop-card--disabled': item.status !== 'ENABLED' }"
          @click="handleSelectShop(item)"
        >
          <div class="v2-ec-shop-card__inner">
            <img
              :src="resolveShopIcon(item.name, item.platformName, item.platformCode, item.avatarUrl)"
              alt=""
              class="v2-ec-shop-card__icon"
              :class="{ 'is-avatar': Boolean(item.avatarUrl?.trim()) }"
            />
            <div class="v2-ec-shop-card__name" :title="item.name">{{ item.name }}</div>
            <span class="v2-ec-shop-card__platform">{{ item.platformName }}</span>
            <span
              class="v2-ec-shop-card__status"
              :class="item.status === 'ENABLED' ? 'is-operating' : 'is-resting'"
            >
              {{ item.status === 'ENABLED' ? '营业中' : '休息中' }}
            </span>
          </div>
        </div>
      </div>
      <div v-else class="v2-ec-empty">暂无店铺</div>

      <MobileShopInfoSheet v-model="shopInfoSheetOpen" :shop-id="selectedShopId" />
    </div>
  </V2Page>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useMobileShop } from '@/mobile/views/shop/useMobileShop.ts'
import V2Page from '@/mobile-v2/components/V2Page.vue'
import MobileShopInfoSheet from '@/mobile/views/shop/components/MobileShopInfoSheet.vue'
import { resolveShopIcon } from '@/utils/shopVisual.ts'
import type { EcShop } from '@/api/ecommerce/shop.ts'

import './styles/v2-ecommerce.scss'

const shop = useMobileShop()
const shopInfoSheetOpen = ref(false)
const selectedShopId = ref<number | null>(null)

onMounted(() => {
  void shop.loadShops()
})

function handleSelectShop(shopItem: EcShop) {
  selectedShopId.value = shopItem.id
  shopInfoSheetOpen.value = true
}
</script>

<style scoped lang="scss">
.v2-ec {
  .v2-ec-header {
    margin-bottom: 16px;

    &__top {
      margin-bottom: 0;
    }

    &__title-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    &__back-row {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    &__title {
      margin: 0;
      font-size: 20px;
      font-weight: 700;
      color: var(--wr-text, #333);
    }
  }

  .v2-ec-shop-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }

  .v2-ec-shop-card {
    display: flex;
    flex-direction: column;
    padding: 14px 10px 12px;
    background: var(--wr-card, #ffffff);
    border-radius: 12px;
    border: 1px solid var(--wr-border, #e8ecef);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    cursor: pointer;
    transition: box-shadow 0.2s, transform 0.15s;
    min-height: 120px;

    &:active {
      transform: scale(0.97);
    }

    &--disabled {
      opacity: 0.65;
    }

    &__inner {
      display: flex;
      flex-direction: column;
      align-items: center;
      width: 100%;
    }

    &__icon {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      flex-shrink: 0;
      object-fit: cover;
      background: #f3f4f6;
      margin-bottom: 8px;

      &.is-avatar {
        object-fit: cover;
        border-radius: 50%;
      }
    }

    &__name {
      font-size: 12px;
      font-weight: 700;
      color: var(--wr-text, #333);
      text-align: center;
      white-space: normal;
      display: -webkit-box;
      -webkit-box-orient: vertical;
      -webkit-line-clamp: 2;
      overflow: hidden;
      width: 100%;
      line-height: 1.3;
      margin-bottom: 4px;
    }

    &__platform {
      font-size: 10px;
      font-weight: 500;
      color: var(--wr-text-secondary, #666);
      margin-bottom: 6px;
    }

    &__status {
      font-size: 9px;
      font-weight: 700;
      padding: 2px 8px;
      border-radius: 999px;

      &.is-operating {
        color: #16a34a;
        background: #dcfce7;
        border: 1px solid #16a34a;
      }

      &.is-resting {
        color: #94a3b8;
        background: #f1f5f9;
        border: 1px solid #e2e8f0;
      }
    }
  }

  .v2-ec-empty {
    padding: 40px 20px;
    text-align: center;
    color: var(--wr-muted, #999);
    font-size: 14px;
    font-weight: 500;
  }
}
</style>
