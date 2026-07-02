<template>
  <div class="carton-design-preview">
    <div class="preview-header">
      <h1 class="preview-title">纸箱管理界面设计</h1>
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
          <div class="scheme-a-page">
            <div class="page-header">
              <div class="page-title-wrap">
                <img :src="assets.starYellow" class="title-star" alt="" />
                <h2 class="page-title">📦 纸箱管理</h2>
                <img :src="assets.starBlue" class="title-star" alt="" />
              </div>
              <div class="page-date">{{ currentDate }}</div>
            </div>

            <SchemeADoodleFrame shape="pill" color="#2563eb" class="search-box">
              <div class="search-inner">
                <img :src="assets.search" class="search-icon" alt="" />
                <input type="text" placeholder="搜索纸箱..." />
              </div>
            </SchemeADoodleFrame>

            <div class="stats-row">
              <SchemeADoodleFrame color="#22c55e" class="stat-card">
                <div class="stat-card__body">
                  <div class="stat-card__value">{{ stats.total }}</div>
                  <div class="stat-card__label">总库存</div>
                </div>
              </SchemeADoodleFrame>
              <SchemeADoodleFrame color="#2563eb" class="stat-card">
                <div class="stat-card__body">
                  <div class="stat-card__value">{{ stats.available }}</div>
                  <div class="stat-card__label">可用</div>
                </div>
              </SchemeADoodleFrame>
              <SchemeADoodleFrame color="#f59e0b" class="stat-card">
                <div class="stat-card__body">
                  <div class="stat-card__value">{{ stats.inUse }}</div>
                  <div class="stat-card__label">使用中</div>
                </div>
              </SchemeADoodleFrame>
            </div>

            <div class="section">
              <div class="section-head">
                <img :src="assets.starBlueOutline" class="section-icon" alt="" />
                <h3 class="section-title">纸箱列表</h3>
                <span class="section-count">{{ cartons.length }}个</span>
              </div>

              <div class="carton-list">
                <SchemeADoodleFrame
                  v-for="carton in cartons"
                  :key="carton.id"
                  class="carton-card"
                  :color="carton.status === 'available' ? '#22c55e' : '#f59e0b'"
                  @click="handleCardClick"
                >
                  <div class="carton-card__inner">
                    <div class="carton-card__image-wrap">
                      <img :src="carton.image" class="carton-card__image" alt="" />
                    </div>
                    <div class="carton-card__content">
                      <div class="carton-card__name">{{ carton.name }}</div>
                      <div class="carton-card__spec">{{ carton.spec }}</div>
                      <div class="carton-card__footer">
                        <span class="carton-card__stock">库存: {{ carton.stock }}</span>
                        <span
                          class="carton-card__status"
                          :class="'is-' + carton.status"
                        >
                          {{ carton.status === 'available' ? '可用' : '使用中' }}
                        </span>
                      </div>
                    </div>
                  </div>
                </SchemeADoodleFrame>
              </div>
            </div>

            <SchemeADoodleFrame tag="button" shape="pill" color="#2563eb" class="create-btn" @click="handleCreateClick">
              <span class="create-btn__text">➕ 新建纸箱</span>
            </SchemeADoodleFrame>
          </div>
        </template>

        <template v-else-if="activeScheme === 'scheme-b'">
          <div class="scheme-b-page">
            <div class="page-header">
              <h2 class="page-title">📦 纸箱管理</h2>
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
                <input type="text" placeholder="搜索纸箱..." />
              </div>
            </SchemeADoodleFrame>

            <div class="section">
              <div class="section-head">
                <img :src="assets.starYellow" class="section-icon" alt="" />
                <h3 class="section-title">{{ currentCategoryName }}</h3>
              </div>

              <div class="carton-grid">
                <SchemeADoodleFrame
                  v-for="carton in filteredCartons"
                  :key="carton.id"
                  class="carton-grid-card"
                  :color="carton.color"
                  @click="handleCardClick"
                >
                  <div class="carton-grid-card__inner">
                    <div class="carton-grid-card__image-wrap">
                      <img :src="carton.image" class="carton-grid-card__image" alt="" />
                    </div>
                    <div class="carton-grid-card__info">
                      <div class="carton-grid-card__name">{{ carton.name }}</div>
                      <div class="carton-grid-card__spec">{{ carton.spec }}</div>
                      <div class="carton-grid-card__stock">
                        <span class="stock-label">库存</span>
                        <span class="stock-value">{{ carton.stock }}</span>
                      </div>
                    </div>
                    <span
                      class="carton-grid-card__status"
                      :class="'is-' + carton.status"
                    >
                      {{ carton.status === 'available' ? '可' : '用' }}
                    </span>
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
          <div class="scheme-c-page">
            <div class="page-header">
              <div class="page-title-wrap">
                <h2 class="page-title">📦 纸箱管理</h2>
              </div>
            </div>

            <SchemeADoodleFrame shape="pill" color="#2563eb" class="search-box">
              <div class="search-inner">
                <img :src="assets.search" class="search-icon" alt="" />
                <input type="text" placeholder="搜索纸箱..." />
              </div>
            </SchemeADoodleFrame>

            <div class="ring-stats">
              <div class="ring-stat">
                <div class="ring-stat__ring" :style="{ '--progress': availableProgress }">
                  <span class="ring-stat__text">{{ stats.available }}</span>
                </div>
                <span class="ring-stat__label">可用</span>
              </div>
              <div class="ring-stat">
                <div class="ring-stat__ring ring-stat__ring--orange" :style="{ '--progress': inUseProgress }">
                  <span class="ring-stat__text">{{ stats.inUse }}</span>
                </div>
                <span class="ring-stat__label">使用中</span>
              </div>
              <div class="ring-stat">
                <div class="ring-stat__ring ring-stat__ring--blue" :style="{ '--progress': 1 }">
                  <span class="ring-stat__text">{{ stats.total }}</span>
                </div>
                <span class="ring-stat__label">总计</span>
              </div>
            </div>

            <div class="quick-actions">
              <SchemeADoodleFrame tag="button" color="#22c55e" class="quick-action-btn" @click="handleActionClick">
                <span class="quick-action-icon">📥</span>
                <span class="quick-action-text">入库</span>
              </SchemeADoodleFrame>
              <SchemeADoodleFrame tag="button" color="#f59e0b" class="quick-action-btn" @click="handleActionClick">
                <span class="quick-action-icon">📤</span>
                <span class="quick-action-text">出库</span>
              </SchemeADoodleFrame>
              <SchemeADoodleFrame tag="button" color="#2563eb" class="quick-action-btn" @click="handleActionClick">
                <span class="quick-action-icon">📋</span>
                <span class="quick-action-text">盘点</span>
              </SchemeADoodleFrame>
              <SchemeADoodleFrame tag="button" color="#8b5cf6" class="quick-action-btn" @click="handleActionClick">
                <span class="quick-action-icon">➕</span>
                <span class="quick-action-text">新建</span>
              </SchemeADoodleFrame>
            </div>

            <div class="section">
              <div class="section-head">
                <img :src="assets.starBlue" class="section-icon" alt="" />
                <h3 class="section-title">最近使用</h3>
              </div>

              <div class="carton-table">
                <div class="carton-table-header">
                  <span>纸箱名称</span>
                  <span>规格</span>
                  <span>库存</span>
                </div>
                <div
                  v-for="carton in recentCartons"
                  :key="carton.id"
                  class="carton-table-row"
                  @click="handleCardClick"
                >
                  <span class="carton-table-row__name">{{ carton.name }}</span>
                  <span class="carton-table-row__spec">{{ carton.spec }}</span>
                  <span class="carton-table-row__stock" :class="'is-' + carton.status">
                    {{ carton.stock }}
                  </span>
                </div>
              </div>
            </div>

            <img :src="assets.squiggleBlue" class="wave-divider" alt="" />

            <SchemeADoodleFrame tag="button" color="#2563eb" class="view-all-btn" @click="handleViewAllClick">
              <span>查看全部纸箱</span>
              <span class="view-all-arrow">→</span>
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
import SchemeADoodleFrame from '@/mobile/home/themes/scheme-a/SchemeADoodleFrame.vue'
import { schemeAAssets } from '@/mobile/home/themes/scheme-a/assets'

const assets = schemeAAssets

const schemes = [
  {
    id: 'scheme-a',
    name: '方案一：卡片式',
    description: '卡片式列表布局 - 推荐方案',
    features: [
      '顶部三色统计卡片（总库存/可用/使用中）',
      '每个纸箱使用手绘边框卡片展示',
      '卡片左侧显示纸箱插图，右侧显示详细信息',
      '使用状态用不同颜色边框区分（绿色可用/橙色使用中）',
      '底部使用胶囊形手绘边框新建按钮'
    ]
  },
  {
    id: 'scheme-b',
    name: '方案二：分类网格',
    description: '分类标签 + 网格布局',
    features: [
      '顶部水平滚动分类标签，支持按尺寸筛选',
      '当前选中分类高亮显示并带有数量徽章',
      '纸箱使用网格卡片展示，每行两列',
      '卡片包含插图、名称、规格、库存和状态',
      '新建按钮使用浮动按钮设计'
    ]
  },
  {
    id: 'scheme-c',
    name: '方案三：统计环',
    description: '统计圆环 + 紧凑列表',
    features: [
      '顶部圆形进度环展示库存状态（可用/使用中/总计）',
      '快捷操作区包含入库、出库、盘点、新建四个按钮',
      '最近使用纸箱使用紧凑表格形式展示',
      '手绘风格波浪线分隔各区域',
      '底部查看全部链接带动画箭头'
    ]
  }
]

const activeScheme = ref('scheme-a')

const currentScheme = computed(() => schemes.find(s => s.id === activeScheme.value)!)

const stats = {
  total: 128,
  available: 85,
  inUse: 43
}

const availableProgress = computed(() => stats.available / stats.total)
const inUseProgress = computed(() => stats.inUse / stats.total)

const cartons = [
  {
    id: 1,
    name: '标准快递箱',
    spec: '40×30×20 cm',
    stock: 50,
    status: 'available' as const,
    color: '#22c55e',
    image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=simple%20cardboard%20box%20illustration%20doodle%20style%20white%20background&image_size=square'
  },
  {
    id: 2,
    name: '大号搬家箱',
    spec: '60×40×50 cm',
    stock: 20,
    status: 'available' as const,
    color: '#22c55e',
    image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=large%20cardboard%20moving%20box%20illustration%20doodle%20style%20white%20background&image_size=square'
  },
  {
    id: 3,
    name: '小号礼盒箱',
    spec: '20×15×10 cm',
    stock: 15,
    status: 'in_use' as const,
    color: '#f59e0b',
    image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=small%20gift%20box%20cardboard%20illustration%20doodle%20style%20white%20background&image_size=square'
  },
  {
    id: 4,
    name: '特大物流箱',
    spec: '80×60×50 cm',
    stock: 8,
    status: 'available' as const,
    color: '#22c55e',
    image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=extra%20large%20logistics%20cardboard%20box%20illustration%20doodle%20style%20white%20background&image_size=square'
  },
  {
    id: 5,
    name: '中号收纳箱',
    spec: '35×25×25 cm',
    stock: 35,
    status: 'in_use' as const,
    color: '#f59e0b',
    image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=medium%20storage%20cardboard%20box%20illustration%20doodle%20style%20white%20background&image_size=square'
  }
]

const recentCartons = cartons.slice(0, 4)

const categories = [
  { id: 'all', name: '全部', icon: '📦', count: 128 },
  { id: 'small', name: '小号', icon: '📭', count: 45 },
  { id: 'medium', name: '中号', icon: '📮', count: 52 },
  { id: 'large', name: '大号', icon: '📯', count: 31 }
]

const activeCategory = ref('all')

const currentCategoryName = computed(() => {
  const cat = categories.find(c => c.id === activeCategory.value)
  return cat ? cat.icon + ' ' + cat.name : ''
})

const filteredCartons = computed(() => cartons)

const currentDate = computed(() => {
  const d = new Date()
  const weekDays = ['日', '一', '二', '三', '四', '五', '六']
  return `${d.getMonth() + 1}月${d.getDate()}日 周${weekDays[d.getDay()]}`
})

function handleCardClick() {
  console.log('Card clicked')
}

function handleCreateClick() {
  console.log('Create carton clicked')
}

function handleActionClick() {
  console.log('Action clicked')
}

function handleViewAllClick() {
  console.log('View all clicked')
}
</script>

<style scoped lang="scss">
.carton-design-preview {
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
}

.scheme-tab {
  padding: 10px 24px;
  border-radius: 999px;
  border: 2px solid #2563eb;
  background: white;
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 16px;
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

.scheme-a-page,
.scheme-b-page,
.scheme-c-page {
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

.page-date {
  font-size: 12px;
  color: #64748b;
  margin-top: 4px;
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

.carton-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.carton-card {
  cursor: pointer;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(0.98);
  }

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.carton-card__inner {
  display: flex;
  gap: 12px;
  padding: 14px;
}

.carton-card__image-wrap {
  width: 80px;
  height: 80px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
  background: #f1f5f9;
}

.carton-card__image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.carton-card__content {
  flex: 1;
  min-width: 0;
}

.carton-card__name {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 16px;
  color: #1e293b;
  margin-bottom: 4px;
}

.carton-card__spec {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 8px;
}

.carton-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.carton-card__stock {
  font-size: 13px;
  font-weight: 600;
  color: #2563eb;
}

.carton-card__status {
  font-size: 11px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 999px;

  &.is-available {
    background: #dcfce7;
    color: #16a34a;
  }

  &.is-in_use {
    background: #fef3c7;
    color: #f59e0b;
  }
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

.carton-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.carton-grid-card {
  cursor: pointer;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(0.98);
  }

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.carton-grid-card__inner {
  padding: 12px;
  position: relative;
}

.carton-grid-card__image-wrap {
  width: 100%;
  height: 90px;
  border-radius: 10px;
  overflow: hidden;
  background: #f1f5f9;
  margin-bottom: 8px;
}

.carton-grid-card__image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.carton-grid-card__info {
  text-align: center;
}

.carton-grid-card__name {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 14px;
  color: #1e293b;
  margin-bottom: 2px;
}

.carton-grid-card__spec {
  font-size: 10px;
  color: #64748b;
  margin-bottom: 6px;
}

.carton-grid-card__stock {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.stock-label {
  font-size: 11px;
  color: #64748b;
}

.stock-value {
  font-size: 14px;
  font-weight: 700;
  color: #2563eb;
}

.carton-grid-card__status {
  position: absolute;
  top: 8px;
  right: 8px;
  font-size: 10px;
  font-weight: 800;
  padding: 3px 6px;
  border-radius: 8px;

  &.is-available {
    background: #dcfce7;
    color: #16a34a;
  }

  &.is-in_use {
    background: #fef3c7;
    color: #f59e0b;
  }
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

.ring-stats {
  display: flex;
  justify-content: space-around;
  margin-bottom: 20px;
}

.ring-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.ring-stat__ring {
  --progress: 0;
  position: relative;
  width: 70px;
  height: 70px;
  border-radius: 50%;
  background: conic-gradient(#22c55e calc(var(--progress) * 360deg), #dcfce7 0);
  box-shadow: inset 0 0 0 3px #86efac;

  &::after {
    content: '';
    position: absolute;
    inset: 8px;
    border-radius: 50%;
    background: #f0fdf4;
  }

  &--orange {
    background: conic-gradient(#f59e0b calc(var(--progress) * 360deg), #fef3c7 0);
    box-shadow: inset 0 0 0 3px #fcd34d;

    &::after {
      background: #fffbeb;
    }
  }

  &--blue {
    background: conic-gradient(#2563eb calc(var(--progress) * 360deg), #dbeafe 0);
    box-shadow: inset 0 0 0 3px #93c5fd;

    &::after {
      background: #eff6ff;
    }
  }
}

.ring-stat__text {
  position: absolute;
  inset: 0;
  z-index: 1;
  display: grid;
  place-items: center;
  font-size: 16px;
  font-weight: 800;
  color: #1e293b;
}

.ring-stat__label {
  font-size: 12px;
  color: #64748b;
  font-weight: 600;
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

.carton-table {
  background: white;
  border-radius: 12px;
  border: 2px solid #2563eb;
  overflow: hidden;
}

.carton-table-header {
  display: flex;
  padding: 10px 12px;
  background: #eff6ff;
  font-size: 11px;
  font-weight: 700;
  color: #2563eb;
  border-bottom: 2px solid #2563eb;
}

.carton-table-header span {
  flex: 1;
  text-align: center;
}

.carton-table-row {
  display: flex;
  padding: 12px;
  border-bottom: 1px dashed #e2e8f0;
  cursor: pointer;
  transition: background 0.2s ease;

  &:hover {
    background: #f8fafc;
  }

  &:last-child {
    border-bottom: none;
  }
}

.carton-table-row__name {
  flex: 1.5;
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 14px;
  color: #1e293b;
}

.carton-table-row__spec {
  flex: 1;
  font-size: 12px;
  color: #64748b;
  text-align: center;
}

.carton-table-row__stock {
  flex: 0.8;
  font-size: 13px;
  font-weight: 700;
  text-align: right;

  &.is-available {
    color: #22c55e;
  }

  &.is-in_use {
    color: #f59e0b;
  }
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