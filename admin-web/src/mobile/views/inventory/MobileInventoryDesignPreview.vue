<template>
  <div class="inventory-design-preview">
    <div class="preview-header">
      <h1 class="preview-title">库存中心界面设计</h1>
      <p class="preview-subtitle">手绘风格手机端界面方案</p>
    </div>

    <div class="scheme-tabs">
      <button
        v-for="scheme in schemes"
        :key="scheme.id"
        class="scheme-tab"
        :class="{ active: activeScheme === scheme.id }"
        @click="activeScheme = scheme.id"
      >
        {{ scheme.name }}
      </button>
    </div>

    <div class="preview-container">
      <div class="mobile-mockup">
        <div class="mockup-header">
          <div class="mockup-notch"></div>
        </div>

        <template v-if="activeScheme === 'scheme-a'">
          <div class="scheme-page">
            <div class="page-header">
              <div class="page-title-wrap">
                <img :src="assets.starYellow" class="title-star" alt="" />
                <h2 class="page-title">📦 库存中心</h2>
                <img :src="assets.starBlue" class="title-star" alt="" />
              </div>
            </div>

            <SchemeADoodleFrame shape="pill" color="#2563eb" class="search-box">
              <div class="search-inner">
                <img :src="assets.search" class="search-icon" alt="" />
                <input type="text" placeholder="搜索商品..." />
              </div>
            </SchemeADoodleFrame>

            <div class="stats-row">
              <SchemeADoodleFrame color="#22c55e" class="stat-card">
                <div class="stat-card__body">
                  <div class="stat-card__value">{{ stats.totalStock }}</div>
                  <div class="stat-card__label">总库存</div>
                </div>
              </SchemeADoodleFrame>
              <SchemeADoodleFrame color="#2563eb" class="stat-card">
                <div class="stat-card__body">
                  <div class="stat-card__value">{{ stats.lowStock }}</div>
                  <div class="stat-card__label">库存不足</div>
                </div>
              </SchemeADoodleFrame>
              <SchemeADoodleFrame color="#ef4444" class="stat-card">
                <div class="stat-card__body">
                  <div class="stat-card__value">{{ stats.outOfStock }}</div>
                  <div class="stat-card__label">已售罄</div>
                </div>
              </SchemeADoodleFrame>
            </div>

            <div class="section">
              <div class="section-head">
                <img :src="assets.starBlueOutline" class="section-icon" alt="" />
                <h3 class="section-title">商品库存</h3>
                <span class="section-count">{{ products.length }}件</span>
              </div>

              <div class="product-list">
                <SchemeADoodleFrame
                  v-for="product in products"
                  :key="product.id"
                  class="product-card"
                  :color="getCardColor(product)"
                  @click="handleCardClick"
                >
                  <div class="product-card__inner">
                    <div class="product-card__image-wrap">
                      <img :src="product.image" class="product-card__image" alt="" />
                    </div>
                    <div class="product-card__content">
                      <div class="product-card__name">{{ product.name }}</div>
                      <div class="product-card__spec">{{ product.spec }}</div>
                      <div class="product-card__footer">
                        <span class="product-card__stock" :class="getStockClass(product)">
                          库存: {{ product.stock }}
                        </span>
                        <span class="product-card__price">¥{{ product.price }}</span>
                      </div>
                    </div>
                  </div>
                </SchemeADoodleFrame>
              </div>
            </div>

            <SchemeADoodleFrame tag="button" shape="pill" color="#2563eb" class="create-btn" @click="handleCreateClick">
              <span class="create-btn__text">➕ 新增库存</span>
            </SchemeADoodleFrame>
          </div>
        </template>

        <template v-else-if="activeScheme === 'scheme-b'">
          <div class="scheme-page">
            <div class="page-header">
              <h2 class="page-title">📦 库存中心</h2>
            </div>

            <div class="category-tabs">
              <div
                v-for="cat in categories"
                :key="cat.id"
                class="category-tab"
                :class="{ active: activeCategory === cat.id }"
                @click="activeCategory = cat.id"
              >
                <span class="category-tab__icon">{{ cat.icon }}</span>
                <span class="category-tab__name">{{ cat.name }}</span>
                <span class="category-tab__badge" v-if="cat.count > 0">{{ cat.count }}</span>
              </div>
            </div>

            <SchemeADoodleFrame shape="pill" color="#2563eb" class="search-box">
              <div class="search-inner">
                <img :src="assets.search" class="search-icon" alt="" />
                <input type="text" placeholder="搜索商品..." />
              </div>
            </SchemeADoodleFrame>

            <div class="section">
              <div class="section-head">
                <img :src="assets.starYellow" class="section-icon" alt="" />
                <h3 class="section-title">{{ currentCategoryName }}</h3>
              </div>

              <div class="product-grid">
                <SchemeADoodleFrame
                  v-for="product in filteredProducts"
                  :key="product.id"
                  class="product-grid-card"
                  :color="getCardColor(product)"
                  @click="handleCardClick"
                >
                  <div class="product-grid-card__inner">
                    <div class="product-grid-card__image-wrap">
                      <img :src="product.image" class="product-grid-card__image" alt="" />
                    </div>
                    <div class="product-grid-card__info">
                      <div class="product-grid-card__name">{{ product.name }}</div>
                      <div class="product-grid-card__price">¥{{ product.price }}</div>
                      <div class="product-grid-card__stock-bar">
                        <div
                          class="product-grid-card__stock-fill"
                          :style="{ width: getStockPercent(product) + '%', background: getStockColor(product) }"
                        ></div>
                      </div>
                      <div class="product-grid-card__stock-text">库存 {{ product.stock }}</div>
                    </div>
                  </div>
                </SchemeADoodleFrame>
              </div>
            </div>

            <div class="fab-container">
              <SchemeADoodleFrame tag="button" color="#2563eb" class="fab-btn" @click="handleCreateClick">
                <span class="fab-icon">➕</span>
              </SchemeADoodleFrame>
            </div>
          </div>
        </template>

        <template v-else-if="activeScheme === 'scheme-c'">
          <div class="scheme-page">
            <div class="page-header">
              <div class="page-title-wrap">
                <h2 class="page-title">📦 库存中心</h2>
              </div>
            </div>

            <SchemeADoodleFrame shape="pill" color="#2563eb" class="search-box">
              <div class="search-inner">
                <img :src="assets.search" class="search-icon" alt="" />
                <input type="text" placeholder="搜索商品..." />
              </div>
            </SchemeADoodleFrame>

            <div class="data-card">
              <SchemeADoodleFrame color="#2563eb" class="data-card__frame">
                <div class="data-card__inner">
                  <div class="data-card__title">📊 库存概览</div>
                  <div class="data-card__stats">
                    <div class="data-card__stat">
                      <div class="data-card__value">{{ stats.totalStock }}</div>
                      <div class="data-card__label">总数量</div>
                    </div>
                    <div class="data-card__stat">
                      <div class="data-card__value">{{ stats.productCount }}</div>
                      <div class="data-card__label">商品种类</div>
                    </div>
                    <div class="data-card__stat">
                      <div class="data-card__value">¥{{ stats.totalValue }}</div>
                      <div class="data-card__label">总价值</div>
                    </div>
                  </div>
                  <div class="data-card__progress">
                    <div class="data-card__progress-row">
                      <span class="data-card__progress-label">充足</span>
                      <div class="data-card__progress-bar">
                        <div class="data-card__progress-fill data-card__progress-fill--green" :style="{ width: stats.healthyPercent + '%' }"></div>
                      </div>
                      <span class="data-card__progress-value">{{ stats.healthyPercent }}%</span>
                    </div>
                    <div class="data-card__progress-row">
                      <span class="data-card__progress-label">不足</span>
                      <div class="data-card__progress-bar">
                        <div class="data-card__progress-fill data-card__progress-fill--orange" :style="{ width: stats.lowPercent + '%' }"></div>
                      </div>
                      <span class="data-card__progress-value">{{ stats.lowPercent }}%</span>
                    </div>
                    <div class="data-card__progress-row">
                      <span class="data-card__progress-label">售罄</span>
                      <div class="data-card__progress-bar">
                        <div class="data-card__progress-fill data-card__progress-fill--red" :style="{ width: stats.outPercent + '%' }"></div>
                      </div>
                      <span class="data-card__progress-value">{{ stats.outPercent }}%</span>
                    </div>
                  </div>
                </div>
              </SchemeADoodleFrame>
            </div>

            <div class="quick-actions">
              <SchemeADoodleFrame tag="button" color="#22c55e" class="quick-action-btn" @click="handleActionClick">
                <span class="quick-action-icon">📥</span>
                <span class="quick-action-text">入库</span>
              </SchemeADoodleFrame>
              <SchemeADoodleFrame tag="button" color="#ef4444" class="quick-action-btn" @click="handleActionClick">
                <span class="quick-action-icon">📤</span>
                <span class="quick-action-text">出库</span>
              </SchemeADoodleFrame>
              <SchemeADoodleFrame tag="button" color="#f59e0b" class="quick-action-btn" @click="handleActionClick">
                <span class="quick-action-icon">📋</span>
                <span class="quick-action-text">盘点</span>
              </SchemeADoodleFrame>
              <SchemeADoodleFrame tag="button" color="#8b5cf6" class="quick-action-btn" @click="handleActionClick">
                <span class="quick-action-icon">📊</span>
                <span class="quick-action-text">统计</span>
              </SchemeADoodleFrame>
            </div>

            <div class="section">
              <div class="section-head">
                <img :src="assets.starBlue" class="section-icon" alt="" />
                <h3 class="section-title">⚠️ 库存预警</h3>
              </div>

              <div class="alert-list">
                <SchemeADoodleFrame
                  v-for="product in alertProducts"
                  :key="product.id"
                  class="alert-card"
                  :color="product.stock === 0 ? '#ef4444' : '#f59e0b'"
                  @click="handleCardClick"
                >
                  <div class="alert-card__inner">
                    <div class="alert-card__icon">{{ product.stock === 0 ? '🚨' : '⚠️' }}</div>
                    <div class="alert-card__content">
                      <div class="alert-card__name">{{ product.name }}</div>
                      <div class="alert-card__desc">{{ product.stock === 0 ? '已售罄，请及时补货' : `库存不足(${product.stock}件)` }}</div>
                    </div>
                    <div class="alert-card__arrow">→</div>
                  </div>
                </SchemeADoodleFrame>
              </div>
            </div>

            <img :src="assets.squiggleBlue" class="wave-divider" alt="" />

            <SchemeADoodleFrame tag="button" color="#2563eb" class="view-all-btn" @click="handleViewAllClick">
              <span>查看全部商品</span>
              <span class="view-all-arrow">→</span>
            </SchemeADoodleFrame>
          </div>
        </template>

        <template v-else-if="activeScheme === 'scheme-d'">
          <div class="scheme-page">
            <div class="page-header">
              <div class="page-title-wrap">
                <img :src="assets.starYellow" class="title-star" alt="" />
                <h2 class="page-title">📦 库存中心</h2>
                <img :src="assets.starBlue" class="title-star" alt="" />
              </div>
            </div>

            <SchemeADoodleFrame shape="pill" color="#2563eb" class="search-box">
              <div class="search-inner">
                <img :src="assets.search" class="search-icon" alt="" />
                <input type="text" placeholder="搜索商品..." />
              </div>
            </SchemeADoodleFrame>

            <div class="status-groups">
              <div
                v-for="group in statusGroups"
                :key="group.status"
                class="status-group"
              >
                <div class="status-group__header" @click="toggleGroup(group.status)">
                  <span class="status-group__arrow" :class="{ expanded: expandedGroups.includes(group.status) }">▼</span>
                  <span class="status-group__icon">{{ group.icon }}</span>
                  <span class="status-group__name">{{ group.name }}</span>
                  <span class="status-group__count">({{ group.products.length }}件)</span>
                </div>
                <div class="status-group__content" v-show="expandedGroups.includes(group.status)">
                  <div class="status-group__list">
                    <SchemeADoodleFrame
                      v-for="product in group.products"
                      :key="product.id"
                      class="group-product-card"
                      :color="getCardColor(product)"
                      @click="handleCardClick"
                    >
                      <div class="group-product-card__inner">
                        <div class="group-product-card__image-wrap">
                          <img :src="product.image" class="group-product-card__image" alt="" />
                        </div>
                        <div class="group-product-card__info">
                          <div class="group-product-card__name">{{ product.name }}</div>
                          <div class="group-product-card__spec">{{ product.spec }}</div>
                          <div class="group-product-card__footer">
                            <span class="group-product-card__stock">{{ product.stock }}件</span>
                            <span class="group-product-card__price">¥{{ product.price }}</span>
                          </div>
                        </div>
                      </div>
                    </SchemeADoodleFrame>
                  </div>
                </div>
              </div>
            </div>

            <SchemeADoodleFrame tag="button" shape="pill" color="#2563eb" class="create-btn" @click="handleCreateClick">
              <span class="create-btn__text">➕ 新增库存</span>
            </SchemeADoodleFrame>
          </div>
        </template>

        <div class="mockup-footer">
          <div class="mockup-home-indicator"></div>
        </div>
      </div>

      <div class="scheme-info">
        <h3>{{ currentScheme.description }}</h3>
        <ul>
          <li v-for="(feature, index) in currentScheme.features" :key="index">
            {{ feature }}
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import SchemeADoodleFrame from '@/mobile/views/home/themes/scheme-a/SchemeADoodleFrame.vue'
import { schemeAAssets } from '@/mobile/views/home/themes/scheme-a/assets.ts'

const assets = schemeAAssets

const schemes = [
  {
    id: 'scheme-a',
    name: '方案一：卡片列表风',
    description: '统计卡片 + 卡片式列表',
    features: [
      '顶部三色统计卡片（总库存/不足/售罄）',
      '每个商品使用手绘边框卡片展示',
      '卡片左侧显示商品图片，右侧显示详细信息',
      '库存状态用不同颜色边框区分',
      '底部使用胶囊形手绘边框新建按钮'
    ]
  },
  {
    id: 'scheme-b',
    name: '方案二：分类网格风',
    description: '分类标签 + 网格布局',
    features: [
      '顶部水平滚动分类标签，支持按品类筛选',
      '当前选中分类高亮显示并带有数量徽章',
      '商品使用网格卡片展示，每行两列',
      '卡片包含图片、名称、价格和库存进度条',
      '新建按钮使用浮动按钮设计'
    ]
  },
  {
    id: 'scheme-c',
    name: '方案三：数据卡片风',
    description: '大数据卡片 + 快捷操作',
    features: [
      '顶部大数据卡片展示库存概览（总数量/种类/价值）',
      '手绘风格进度条显示库存状态分布',
      '快捷操作区包含入库、出库、盘点、统计四个按钮',
      '库存预警区域单独展示低库存商品',
      '手绘风格波浪线分隔各区域'
    ]
  },
  {
    id: 'scheme-d',
    name: '方案四：状态分组风',
    description: '状态分组 + 可折叠列表',
    features: [
      '按库存状态分组（充足/不足/售罄）',
      '每组可折叠/展开，折叠状态显示商品数量',
      '展开状态显示该状态下的所有商品卡片',
      '商品卡片使用缩进方式显示层级关系',
      '手绘风格的折叠箭头（▼/▶）'
    ]
  }
]

const activeScheme = ref('scheme-a')

const currentScheme = computed(() => schemes.find(s => s.id === activeScheme.value)!)

const stats = {
  totalStock: 1256,
  lowStock: 128,
  outOfStock: 24,
  productCount: 86,
  totalValue: '128.5万',
  healthyPercent: 78,
  lowPercent: 15,
  outPercent: 7
}

const products = [
  {
    id: 1,
    name: '纯棉T恤',
    spec: '白色 / L码',
    stock: 156,
    price: 59,
    status: 'healthy' as const,
    category: 'clothing',
    image: ''
  },
  {
    id: 2,
    name: '牛仔裤',
    spec: '蓝色 / 32码',
    stock: 23,
    price: 129,
    status: 'low' as const,
    category: 'clothing',
    image: ''
  },
  {
    id: 3,
    name: '运动跑鞋',
    spec: '黑色 / 42码',
    stock: 0,
    price: 299,
    status: 'out' as const,
    category: 'shoes',
    image: ''
  },
  {
    id: 4,
    name: '休闲外套',
    spec: '灰色 / XL码',
    stock: 89,
    price: 199,
    status: 'healthy' as const,
    category: 'clothing',
    image: ''
  },
  {
    id: 5,
    name: '帆布鞋',
    spec: '白色 / 38码',
    stock: 15,
    price: 79,
    status: 'low' as const,
    category: 'shoes',
    image: ''
  },
  {
    id: 6,
    name: '针织毛衣',
    spec: '米色 / M码',
    stock: 0,
    price: 169,
    status: 'out' as const,
    category: 'clothing',
    image: ''
  },
  {
    id: 7,
    name: '运动背包',
    spec: '黑色 / 大号',
    stock: 67,
    price: 89,
    status: 'healthy' as const,
    category: 'bags',
    image: ''
  },
  {
    id: 8,
    name: '棒球帽',
    spec: '藏青 / 均码',
    stock: 8,
    price: 39,
    status: 'low' as const,
    category: 'accessories',
    image: ''
  }
]

const alertProducts = computed(() => products.filter(p => p.stock <= 20))

const categories = [
  { id: 'all', name: '全部', icon: '📦', count: 1256 },
  { id: 'clothing', name: '服装', icon: '👕', count: 520 },
  { id: 'shoes', name: '鞋履', icon: '👟', count: 380 },
  { id: 'bags', name: '箱包', icon: '👜', count: 210 },
  { id: 'accessories', name: '配饰', icon: '🎀', count: 146 }
]

const activeCategory = ref('all')

const currentCategoryName = computed(() => {
  const cat = categories.find(c => c.id === activeCategory.value)
  return cat ? cat.icon + ' ' + cat.name : ''
})

const filteredProducts = computed(() => {
  if (activeCategory.value === 'all') return products
  return products.filter(p => p.category === activeCategory.value)
})

const statusGroups = computed(() => [
  {
    status: 'healthy',
    name: '库存充足',
    icon: '✅',
    products: products.filter(p => p.stock > 20)
  },
  {
    status: 'low',
    name: '库存不足',
    icon: '⚠️',
    products: products.filter(p => p.stock > 0 && p.stock <= 20)
  },
  {
    status: 'out',
    name: '已售罄',
    icon: '🚨',
    products: products.filter(p => p.stock === 0)
  }
])

const expandedGroups = ref(['healthy', 'low', 'out'])

function toggleGroup(status: string) {
  const index = expandedGroups.value.indexOf(status)
  if (index > -1) {
    expandedGroups.value.splice(index, 1)
  } else {
    expandedGroups.value.push(status)
  }
}

function getCardColor(product: typeof products[0]): string {
  if (product.stock === 0) return '#ef4444'
  if (product.stock <= 20) return '#f59e0b'
  return '#22c55e'
}

function getStockClass(product: typeof products[0]): string {
  if (product.stock === 0) return 'is-out'
  if (product.stock <= 20) return 'is-low'
  return 'is-healthy'
}

function getStockPercent(product: typeof products[0]): number {
  const maxStock = 200
  return Math.min((product.stock / maxStock) * 100, 100)
}

function getStockColor(product: typeof products[0]): string {
  if (product.stock === 0) return '#ef4444'
  if (product.stock <= 20) return '#f59e0b'
  return '#22c55e'
}

function handleCardClick() {
  console.log('Card clicked')
}

function handleCreateClick() {
  console.log('Create clicked')
}

function handleActionClick() {
  console.log('Action clicked')
}

function handleViewAllClick() {
  console.log('View all clicked')
}
</script>

<style scoped lang="scss">
.inventory-design-preview {
  min-height: 100vh;
  background: linear-gradient(135deg, #faf8f5 0%, #f0f9ff 100%);
  padding: 24px;
}

.preview-header {
  text-align: center;
  margin-bottom: 24px;
}

.preview-title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 32px;
  color: #1e293b;
  margin: 0 0 8px;
}

.preview-subtitle {
  font-size: 16px;
  color: #64748b;
  margin: 0;
}

.scheme-tabs {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.scheme-tab {
  padding: 10px 20px;
  border-radius: 999px;
  border: 2px solid #2563eb;
  background: white;
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 15px;
  color: #2563eb;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    transform: scale(1.02);
  }

  &.active {
    background: #2563eb;
    color: white;
  }
}

.preview-container {
  display: flex;
  justify-content: center;
  gap: 32px;
  flex-wrap: wrap;
}

.mobile-mockup {
  width: 375px;
  height: 812px;
  border-radius: 40px;
  background: #1e293b;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  overflow: hidden;
  position: relative;
}

.mockup-header {
  height: 44px;
  background: #faf8f5;
  display: flex;
  justify-content: center;
  align-items: center;
}

.mockup-notch {
  width: 150px;
  height: 30px;
  border-radius: 0 0 20px 20px;
  background: #1e293b;
}

.mockup-footer {
  height: 34px;
  background: #faf8f5;
  display: flex;
  justify-content: center;
  align-items: center;
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
}

.mockup-home-indicator {
  width: 130px;
  height: 5px;
  border-radius: 999px;
  background: #94a3b8;
}

.scheme-page {
  padding: 16px;
  background: #faf8f5;
  height: calc(100% - 78px);
  overflow-y: auto;
}

.page-header {
  margin-bottom: 16px;
}

.page-title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 24px;
  color: #1e293b;
  margin: 0;
}

.page-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-star {
  width: 24px;
  height: 24px;
}

.search-box {
  margin-bottom: 16px;
}

.search-inner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
}

.search-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.search-inner input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 14px;
  color: #1e293b;
}

.search-inner input::placeholder {
  color: #94a3b8;
}

.section {
  margin-bottom: 20px;
}

.section-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.section-icon {
  width: 20px;
  height: 20px;
}

.section-title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 18px;
  color: #1e293b;
  margin: 0;
}

.section-count {
  font-size: 12px;
  color: #94a3b8;
  margin-left: auto;
}

.stats-row {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.stat-card {
  flex: 1;

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.stat-card__body {
  padding: 12px 8px;
  text-align: center;
}

.stat-card__value {
  font-size: 20px;
  font-weight: 800;
  color: #1e293b;
}

.stat-card__label {
  font-size: 11px;
  color: #64748b;
  margin-top: 2px;
}

.product-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.product-card {
  cursor: pointer;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(0.98);
  }

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.product-card__inner {
  display: flex;
  gap: 12px;
  padding: 14px;
}

.product-card__image-wrap {
  width: 80px;
  height: 80px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
  background: #f1f5f9;
}

.product-card__image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  background: linear-gradient(135deg, #e2e8f0 0%, #cbd5e1 100%);
}

.product-card__content {
  flex: 1;
  min-width: 0;
}

.product-card__name {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 16px;
  color: #1e293b;
  margin-bottom: 4px;
}

.product-card__spec {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 8px;
}

.product-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.product-card__stock {
  font-size: 13px;
  font-weight: 600;

  &.is-healthy {
    color: #22c55e;
  }

  &.is-low {
    color: #f59e0b;
  }

  &.is-out {
    color: #ef4444;
  }
}

.product-card__price {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}

.create-btn {
  margin-top: 8px;
  margin-bottom: 40px;
  padding: 14px;
  cursor: pointer;
  background: #2563eb;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(0.98);
  }
}

.create-btn__text {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 18px;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.category-tabs {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 12px;
  margin-bottom: 16px;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.category-tab {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  background: white;
  border-radius: 999px;
  border: 2px solid #2563eb;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;

  &:hover {
    transform: scale(0.98);
  }

  &.active {
    background: #2563eb;

    .category-tab__name {
      color: white;
    }
  }
}

.category-tab__icon {
  font-size: 16px;
}

.category-tab__name {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 14px;
  color: #2563eb;
}

.category-tab__badge {
  font-size: 11px;
  font-weight: 700;
  color: white;
  background: #f59e0b;
  padding: 2px 6px;
  border-radius: 999px;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.product-grid-card {
  cursor: pointer;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(0.98);
  }

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.product-grid-card__inner {
  padding: 12px;
}

.product-grid-card__image-wrap {
  width: 100%;
  height: 110px;
  border-radius: 10px;
  overflow: hidden;
  background: #f1f5f9;
  margin-bottom: 8px;
}

.product-grid-card__image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  background: linear-gradient(135deg, #e2e8f0 0%, #cbd5e1 100%);
}

.product-grid-card__info {
  text-align: center;
}

.product-grid-card__name {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 14px;
  color: #1e293b;
  margin-bottom: 4px;
}

.product-grid-card__price {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 6px;
}

.product-grid-card__stock-bar {
  height: 6px;
  background: #e2e8f0;
  border-radius: 999px;
  overflow: hidden;
  margin-bottom: 4px;
}

.product-grid-card__stock-fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.3s ease;
}

.product-grid-card__stock-text {
  font-size: 11px;
  color: #64748b;
}

.fab-container {
  position: fixed;
  bottom: 60px;
  right: 24px;
}

.fab-btn {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #2563eb;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.4);
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(1.05);
  }
}

.fab-icon {
  font-size: 28px;
  color: white;
}

.data-card {
  margin-bottom: 16px;
}

.data-card__frame {
  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.data-card__inner {
  padding: 16px;
}

.data-card__title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 18px;
  color: #1e293b;
  margin-bottom: 16px;
}

.data-card__stats {
  display: flex;
  justify-content: space-around;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 2px dashed #e2e8f0;
}

.data-card__stat {
  text-align: center;
}

.data-card__value {
  font-size: 22px;
  font-weight: 800;
  color: #1e293b;
}

.data-card__label {
  font-size: 11px;
  color: #64748b;
  margin-top: 2px;
}

.data-card__progress {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.data-card__progress-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.data-card__progress-label {
  font-size: 12px;
  color: #64748b;
  width: 50px;
  flex-shrink: 0;
}

.data-card__progress-bar {
  flex: 1;
  height: 12px;
  background: #e2e8f0;
  border-radius: 999px;
  overflow: hidden;
}

.data-card__progress-fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.3s ease;

  &--green {
    background: linear-gradient(90deg, #22c55e, #4ade80);
  }

  &--orange {
    background: linear-gradient(90deg, #f59e0b, #fbbf24);
  }

  &--red {
    background: linear-gradient(90deg, #ef4444, #f87171);
  }
}

.data-card__progress-value {
  font-size: 12px;
  font-weight: 700;
  color: #1e293b;
  width: 40px;
  text-align: right;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 20px;
}

.quick-action-btn {
  cursor: pointer;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(0.95);
  }

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.quick-action-icon {
  font-size: 24px;
  display: block;
  text-align: center;
  margin-bottom: 4px;
}

.quick-action-text {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 12px;
  color: #1e293b;
  display: block;
  text-align: center;
  padding-bottom: 8px;
}

.alert-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.alert-card {
  cursor: pointer;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(0.98);
  }

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.alert-card__inner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
}

.alert-card__icon {
  font-size: 24px;
  flex-shrink: 0;
}

.alert-card__content {
  flex: 1;
  min-width: 0;
}

.alert-card__name {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 15px;
  color: #1e293b;
  margin-bottom: 2px;
}

.alert-card__desc {
  font-size: 12px;
  color: #64748b;
}

.alert-card__arrow {
  font-size: 18px;
  color: #94a3b8;
}

.wave-divider {
  width: 100%;
  height: 12px;
  margin: 16px 0;
}

.view-all-btn {
  margin-bottom: 40px;
  padding: 12px 14px;
  cursor: pointer;
  transition: transform 0.2s ease;
  text-align: center;

  &:hover {
    transform: scale(0.98);
  }

  span {
    font-family: 'ZCOOL KuaiLe', sans-serif;
    font-size: 16px;
    color: #2563eb;
  }
}

.view-all-arrow {
  display: inline-block;
  margin-left: 4px;
  animation: bounceArrow 1s ease-in-out infinite;
}

@keyframes bounceArrow {
  0%, 100% { transform: translateX(0); }
  50% { transform: translateX(4px); }
}

.status-groups {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.status-group {
  background: white;
  border-radius: 16px;
  border: 2px solid #e2e8f0;
  overflow: hidden;
}

.status-group__header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 16px;
  cursor: pointer;
  transition: background 0.2s ease;

  &:hover {
    background: #f8fafc;
  }
}

.status-group__arrow {
  font-size: 10px;
  color: #94a3b8;
  transition: transform 0.2s ease;

  &.expanded {
    transform: rotate(180deg);
  }
}

.status-group__icon {
  font-size: 18px;
}

.status-group__name {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 16px;
  color: #1e293b;
}

.status-group__count {
  margin-left: auto;
  font-size: 12px;
  color: #94a3b8;
}

.status-group__content {
  padding: 0 12px 12px;
}

.status-group__list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.group-product-card {
  cursor: pointer;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(0.98);
  }

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.group-product-card__inner {
  display: flex;
  gap: 10px;
  padding: 12px;
}

.group-product-card__image-wrap {
  width: 60px;
  height: 60px;
  border-radius: 10px;
  overflow: hidden;
  flex-shrink: 0;
  background: #f1f5f9;
}

.group-product-card__image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  background: linear-gradient(135deg, #e2e8f0 0%, #cbd5e1 100%);
}

.group-product-card__info {
  flex: 1;
  min-width: 0;
}

.group-product-card__name {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 14px;
  color: #1e293b;
  margin-bottom: 2px;
}

.group-product-card__spec {
  font-size: 11px;
  color: #64748b;
  margin-bottom: 6px;
}

.group-product-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.group-product-card__stock {
  font-size: 12px;
  font-weight: 600;
  color: #2563eb;
}

.group-product-card__price {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
}

.scheme-info {
  max-width: 300px;
}

.scheme-info h3 {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 20px;
  color: #1e293b;
  margin: 0 0 12px;
}

.scheme-info ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.scheme-info li {
  padding: 8px 12px;
  margin-bottom: 8px;
  background: white;
  border-radius: 8px;
  border-left: 4px solid #2563eb;
  font-size: 14px;
  color: #475569;
}
</style>
