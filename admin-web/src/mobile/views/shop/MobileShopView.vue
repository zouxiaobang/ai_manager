<template>
  <!-- 移动端店铺管理页主容器 -->
  <div v-loading="shop.loading.value" class="mobile-shop-view">
    <!-- 顶部导航栏：返回按钮 + 页面标题 -->
    <div class="mobile-shop-view__header">
      <div class="mobile-shop-view__header-left">
        <!-- 返回按钮：返回上一页 -->
        <MobileDoodleChip
          tag="button"
          type="button"
          shape="pill"
          color="#2563eb"
          class="mobile-shop-view__back"
          @click="$router.back()"
        >
          <span>←</span>
        </MobileDoodleChip>
        <h1 class="mobile-shop-view__title">🏪 店铺管理</h1>
      </div>
    </div>

    <div class="mobile-shop-view__content">
      <!-- 搜索框：搜索店铺名称 -->
      <MobileDoodleSearch
        v-model="shop.searchQuery.value"
        placeholder="搜索店铺..."
      />
      <!-- 平台分类标签：按平台筛选店铺 -->
      <MobileCategoryTabs
        :categories="shop.platformList.value"
        v-model:active-value="shop.activePlatformId.value"
      />

      <!-- 店铺列表区域 -->
      <div class="mobile-shop-view__section">
        <!-- 列表头部：平台名称 + 店铺数量 -->
        <MobileSectionHeader
          :icon="schemeAAssets.starYellow"
          :title="currentPlatformName"
          :count="shop.filteredShops.value.length"
        />

        <!-- 店铺卡片网格 -->
        <MobileCardGrid
          :items="shop.filteredShops.value"
          empty-text="暂无店铺"
          @select="(item) => handleSelectShop(item as any)"
        >
          <!-- 空状态模板：无店铺时展示 -->
          <template #empty>
            <span class="shop-empty__icon">📭</span>
            <span class="shop-empty__text">暂无店铺</span>
          </template>
          <!-- 店铺卡片：展示单个店铺信息 -->
          <template #card="{ item }">
            <SchemeADoodleFrame
              tag="button"
              type="button"
              class="shop-grid-card"
              :class="{ 'shop-grid-card--disabled': (item as any).status !== 'ENABLED' }"
              :seed="item.id"
              :color="(item as any).status === 'ENABLED' ? '#16a34a' : '#94a3b8'"
              sketch
              :stroke-width="3"
              @click="handleSelectShop(item as any)"
            >
              <div class="shop-grid-card__inner">
                <img
                  :src="resolveShopIcon((item as any).name, (item as any).platformName, (item as any).platformCode, (item as any).avatarUrl)"
                  alt=""
                  class="shop-grid-card__icon"
                  :class="{ 'is-avatar': Boolean((item as any).avatarUrl?.trim()) }"
                />
                <div class="shop-grid-card__name" :title="(item as any).name">{{ (item as any).name }}</div>
                <span class="shop-grid-card__platform">
                  {{ (item as any).platformName }}
                </span>
                <span
                  class="shop-grid-card__status"
                  :class="(item as any).status === 'ENABLED' ? 'is-operating' : 'is-resting'"
                >
                  {{ (item as any).status === 'ENABLED' ? '营业中' : '休息中' }}
                </span>
              </div>
            </SchemeADoodleFrame>
          </template>
        </MobileCardGrid>
      </div>
    </div>

    <!-- 店铺详情底部弹窗：点击店铺卡片时弹出 -->
    <MobileShopInfoSheet v-model="shopInfoSheetOpen" :shop-id="selectedShopId" />
  </div>
</template>

<script setup lang="ts">
/**
 * 移动端店铺管理视图组件
 * 功能说明：
 * - 店铺管理的移动端入口页面
 * - 支持按平台分类筛选店铺
 * - 提供店铺搜索功能
 * - 店铺以卡片网格形式展示，包含店铺图标、名称、平台、营业状态
 * - 点击店铺可查看详情弹窗
 * - 使用手绘风格UI设计
 */
import { computed, ref, onMounted } from 'vue'
import { useMobileShop } from '@/mobile/views/shop/useMobileShop.ts'
import MobileCardGrid from '@/mobile/components/MobileCardGrid.vue'
import MobileDoodleSearch from '@/mobile/components/MobileDoodleSearch.vue'
import MobileCategoryTabs from '@/mobile/components/MobileCategoryTabs.vue'
import MobileSectionHeader from '@/mobile/components/MobileSectionHeader.vue'
import MobileDoodleChip from '@/mobile/components/MobileDoodleChip.vue'
import MobileShopInfoSheet from '@/mobile/views/shop/components/MobileShopInfoSheet.vue'
import SchemeADoodleFrame from '@/mobile/views/home/themes/scheme-a/SchemeADoodleFrame.vue'
import { schemeAAssets } from '@/mobile/views/home/themes/scheme-a/assets.ts'
import { resolveShopIcon } from '@/utils/shopVisual.ts'
import type { EcShop } from '@/api/ecommerce/shop.ts'

const shop = useMobileShop() // 店铺业务逻辑组合函数
const shopInfoSheetOpen = ref(false) // 店铺详情弹窗开关
const selectedShopId = ref<number | null>(null) // 当前选中的店铺ID

// 当前选中平台的名称
const currentPlatformName = computed(() => {
  const platform = shop.platformList.value.find((p) => p.id === shop.activePlatformId.value)
  return platform ? platform.name : ''
})

onMounted(() => {
  void shop.loadShops() // 组件挂载时加载店铺列表
})

// 处理店铺卡片点击：打开店铺详情弹窗
function handleSelectShop(shopItem: EcShop) {
  selectedShopId.value = shopItem.id
  shopInfoSheetOpen.value = true
}
</script>

<style scoped lang="scss">
.mobile-shop-view {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  font-family: 'ZCOOL KuaiLe', 'Alibaba PuHuiTi', 'PingFang SC', sans-serif;
}

.mobile-shop-view__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: max(16px, env(safe-area-inset-top)) 16px 16px;
  background: #fff;
}

.mobile-shop-view__header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mobile-shop-view__back {
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

.mobile-shop-view__title {
  font-size: 24px;
  color: #1e293b;
  margin: 0;
}

.mobile-shop-view__content {
  flex: 1;
  padding: 0 16px;
  padding-bottom: 20px;
  overflow-y: auto;
  background: #fff;
}

.mobile-shop-view__section {
  margin-top: 8px;
}

.shop-grid-card {
  background: #fff;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;

  &:active {
    transform: scale(0.98);
  }

  &--disabled {
    opacity: 0.65;
    background: #f8fafc;
  }

  :deep(.sa-doodle-frame__body) {
    padding: 10px 6px 8px;
    display: flex;
    flex-direction: column;
    align-items: center;
    min-height: 90px;
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
    margin-bottom: 6px;

    &.is-avatar {
      object-fit: cover;
      border-radius: 50%;
    }
  }

  &__name {
    font-size: 12px;
    font-weight: 800;
    color: #1e293b;
    text-align: center;
    white-space: normal;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    overflow: hidden;
    width: 100%;
    line-height: 1.3;
  }

  &__platform {
    margin-top: 4px;
    font-size: 10px;
    font-weight: 600;
    color: #64748b;
  }

  &__status {
    margin-top: 4px;
    font-size: 9px;
    font-weight: 800;
    padding: 2px 6px;
    border-radius: 999px;

    &.is-operating {
      color: #16a34a;
      background: #dcfce7;
    }

    &.is-resting {
      color: #94a3b8;
      background: #f1f5f9;
    }
  }
}

.shop-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  gap: 12px;

  &__icon {
    font-size: 48px;
    opacity: 0.6;
  }

  &__text {
    font-size: 14px;
    font-weight: 600;
    color: #94a3b8;
  }
}
</style>
