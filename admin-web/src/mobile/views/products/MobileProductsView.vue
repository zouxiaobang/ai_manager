<template>
  <!-- 移动端商品中心页主容器 -->
  <div class="mobile-products-view">

    <!-- 顶部导航栏：返回按钮 + 页面标题 -->
    <div class="mobile-products-view__header">
      <div class="mobile-products-view__header-left">
        <!-- 返回按钮：返回上一页 -->
        <MobileDoodleChip
          tag="button" type="button"
          shape="pill" color="#2563eb"
          class="mobile-products-view__back"
          @click="$router.back()"
        >
          <span>←</span>
        </MobileDoodleChip>
        <h1 class="mobile-products-view__title">🎁 商品中心</h1>
      </div>
    </div>

    <!-- 装饰元素：星星装饰（纯视觉装饰） -->
    <img class="mobile-products-view__deco mobile-products-view__deco--star-1" :src="schemeAAssets.starYellow" alt="" />
    <img class="mobile-products-view__deco mobile-products-view__deco--star-2" :src="schemeAAssets.starBlue" alt="" />

    <!-- 主内容区域：加载状态 + 概览卡片 + 搜索 + 工厂切换 + 商品列表 -->
    <div v-loading="products.loading.value" class="mobile-products-view__content">
      <!-- 概览统计卡片：SPU/SKU/工厂数量 + 启用率进度条 -->
      <SchemeADoodleFrame color="#2563eb" class="products-overview" sketch :stroke-width="2.5">
        <img class="products-overview__clip" :src="schemeAAssets.paperclip" alt="" />
        <div class="products-overview__inner">
          <!-- 概览卡片头部：图标 + 标题 -->
          <div class="products-overview__header">
            <img class="products-overview__icon" :src="schemeAAssets.starYellow" alt="" />
            <span class="products-overview__title">按工厂维度管理商品</span>
          </div>

          <!-- 统计数据区域：SPU/SKU/工厂三个统计项 -->
          <div class="products-overview__stats">
            <div class="products-overview__stat">
              <div class="products-overview__stat-val">{{ products.stats.totalProducts }}</div>
              <div class="products-overview__stat-lbl">SPU</div>
            </div>
            <div class="products-overview__stat">
              <div class="products-overview__stat-val">{{ products.stats.totalSkus }}</div>
              <div class="products-overview__stat-lbl">SKU</div>
            </div>
            <div class="products-overview__stat">
              <div class="products-overview__stat-val">{{ products.stats.totalFactories }}</div>
              <div class="products-overview__stat-lbl">工厂</div>
            </div>
          </div>

          <!-- 分隔线：虚线分隔 -->
          <div class="products-overview__divider" />

          <!-- 启用率进度条区域：商品启用率展示 -->
          <div class="products-overview__health">
            <span class="products-overview__health-label">启用率</span>
            <div class="products-overview__health-bar">
              <div
                class="products-overview__health-fill"
                :style="{ width: healthEnabledPct + '%' }"
              />
            </div>
            <span class="products-overview__health-pct">{{ healthEnabledPct }}%</span>
          </div>
        </div>
      </SchemeADoodleFrame>

      <!-- 搜索框：搜索商品名称/工厂 -->
      <MobileDoodleSearch
        v-model="products.searchQuery.value"
        placeholder="搜索商品名称 / 工厂..."
      />

      <!-- 工厂切换标签：按工厂筛选商品 -->
      <ProductsFactorySelect />

      <!-- 按工厂分组的商品卡片网格 -->
      <ProductsFactoryGrid />
    </div>

    <!-- 商品详情弹窗：点击商品卡片时弹出详情 -->
    <ProductsDetailSheet />
  </div>
</template>

<script setup lang="ts">
/**
 * 移动端商品中心视图组件
 * 功能说明：
 * - 商品管理的移动端入口页面，按工厂维度组织商品
 * - 顶部展示统计概览（SPU数量、SKU数量、工厂数量、启用率）
 * - 提供商品搜索功能（按商品名称、工厂搜索）
 * - 支持按工厂切换筛选商品
 * - 商品以工厂分组的卡片网格形式展示
 * - 点击商品可查看详情弹窗
 * - 使用手绘风格UI设计
 */
import { computed, onMounted, provide } from 'vue'
import { MOBILE_PRODUCTS_KEY } from './productsContext'
import { useMobileProducts } from './useMobileProducts'
import ProductsFactorySelect from './components/ProductsFactorySelect.vue'
import ProductsFactoryGrid from './components/ProductsFactoryGrid.vue'
import ProductsDetailSheet from './components/ProductsDetailSheet.vue'
import SchemeADoodleFrame from '@/mobile/views/home/themes/scheme-a/SchemeADoodleFrame.vue'
import MobileDoodleSearch from '@/mobile/components/MobileDoodleSearch.vue'
import MobileDoodleChip from '@/mobile/components/MobileDoodleChip.vue'
import { schemeAAssets } from '@/mobile/views/home/themes/scheme-a/assets'

const products = useMobileProducts() // 商品业务逻辑组合函数
provide(MOBILE_PRODUCTS_KEY, products) // 向下提供商品上下文

const total = computed(() => products.stats.enabledCount + products.stats.disabledCount) // 商品总数
const healthEnabledPct = computed(() => // 商品启用率百分比
  total.value === 0 ? 0 : Math.round((products.stats.enabledCount / total.value) * 100),
)

onMounted(() => {
  void products.init() // 组件挂载时初始化商品数据
})
</script>

<style scoped lang="scss">
.mobile-products-view {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  font-family: 'ZCOOL KuaiLe', 'Alibaba PuHiTi', 'PingFang SC', sans-serif;
  padding-bottom: 100px;
  position: relative;
  overflow-x: hidden;
}

/* ===== SVG 手绘背景 ===== */
.mobile-products-view__bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

/* ===== 装饰元素 ===== */
.mobile-products-view__deco {
  position: absolute;
  z-index: 1;
  pointer-events: none;

  img {
    display: block;
    width: 100%;
    height: auto;
  }

  &--star-1 {
    top: 20px;
    right: 16px;
    width: 32px;
    animation: twinkle 2.5s ease-in-out infinite;
  }

  &--star-2 {
    top: 48px;
    left: 12px;
    width: 24px;
    animation: twinkle 2.5s ease-in-out infinite 0.8s;
  }

  &--squiggle {
    top: 90px;
    right: 4px;
    width: 48px;
    opacity: 0.5;
  }
}

@keyframes twinkle {
  0%, 100% { opacity: 0.4; transform: scale(1) rotate(0deg); }
  50% { opacity: 0.9; transform: scale(1.1) rotate(5deg); }
}

/* ===== Header ===== */
.mobile-products-view__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: max(16px, env(safe-area-inset-top)) 16px 12px;
  position: relative;
  z-index: 3;
}

.mobile-products-view__header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mobile-products-view__back {
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
  background: #faf8f5;
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

.mobile-products-view__title {
  font-size: 24px;
  margin: 0;
  color: #1e293b;
}

.mobile-products-view__content {
  flex: 1;
  padding: 0 16px 20px;
  overflow-y: auto;
  position: relative;
  z-index: 2;
}

/* ===== 概览卡片 ===== */
.products-overview {
  position: relative;
  margin-bottom: 14px;

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.products-overview__clip {
  position: absolute;
  top: -6px;
  right: 14px;
  width: 22px;
  height: 32px;
  pointer-events: none;
  z-index: 2;
}

.products-overview__inner {
  padding: 20px;
  position: relative;
  z-index: 1;
}

.products-overview__header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding-left: 8px;
}

.products-overview__icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.products-overview__title {
  font-size: 18px;
  color: #1e293b;
}

.products-overview__stats {
  display: flex;
  justify-content: space-around;
  gap: 8px;
  margin-bottom: 16px;
}

.products-overview__stat {
  flex: 1;
  text-align: center;
}

.products-overview__stat-val {
  font-size: 28px;
  font-weight: 800;
  color: #8b0000;
}

.products-overview__stat-lbl {
  font-size: 11px;
  color: #64748b;
  margin-top: 2px;
}

.products-overview__divider {
  height: 2px;
  background: repeating-linear-gradient(
    90deg,
    #93c5fd,
    #93c5fd 6px,
    transparent 6px,
    transparent 10px
  );
  margin-bottom: 14px;
}

.products-overview__health {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
}

.products-overview__health-label {
  font-size: 12px;
  color: #64748b;
  flex-shrink: 0;
}

.products-overview__health-bar {
  flex: 1;
  height: 10px;
  background: #e2e8f0;
  border-radius: 999px;
  overflow: hidden;
}

.products-overview__health-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #22c55e, #4ade80);
  transition: width 0.6s ease;
}

.products-overview__health-pct {
  font-size: 12px;
  font-weight: 700;
  color: #1e293b;
  width: 40px;
  text-align: right;
}
</style>
