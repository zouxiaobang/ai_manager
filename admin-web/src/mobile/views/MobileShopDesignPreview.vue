<template>
  <div class="shop-design-preview">
    <div class="preview-header">
      <h1 class="preview-title">店铺管理界面设计</h1>
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
                <h2 class="page-title">🛒 店铺管理</h2>
                <img :src="assets.starBlue" class="title-star" alt="" />
              </div>
            </div>

            <SchemeADoodleFrame shape="pill" color="#2563eb" class="search-box">
              <div class="search-inner">
                <img :src="assets.search" class="search-icon" alt="" />
                <input type="text" placeholder="搜索店铺..." />
              </div>
            </SchemeADoodleFrame>

            <div class="section">
              <div class="section-head">
                <img :src="assets.starYellow" class="section-icon" alt="" />
                <h3 class="section-title">今日数据概览</h3>
              </div>
              <div class="stats-grid">
                <SchemeADoodleFrame color="#2563eb" class="stat-card">
                  <div class="stat-card__body">
                    <div class="stat-card__value">{{ stats.total }}</div>
                    <div class="stat-card__label">店铺总数</div>
                  </div>
                </SchemeADoodleFrame>
                <SchemeADoodleFrame color="#16a34a" class="stat-card">
                  <div class="stat-card__body">
                    <div class="stat-card__value">{{ stats.online }}</div>
                    <div class="stat-card__label">在线店铺</div>
                  </div>
                </SchemeADoodleFrame>
                <SchemeADoodleFrame color="#3b82f6" class="stat-card">
                  <div class="stat-card__body">
                    <div class="stat-card__value">{{ stats.platforms }}</div>
                    <div class="stat-card__label">平台数量</div>
                  </div>
                </SchemeADoodleFrame>
                <SchemeADoodleFrame color="#f59e0b" class="stat-card">
                  <div class="stat-card__body">
                    <div class="stat-card__value">{{ stats.fee }}</div>
                    <div class="stat-card__label">平均扣点</div>
                  </div>
                </SchemeADoodleFrame>
              </div>
            </div>

            <div class="section">
              <div class="section-head">
                <img :src="assets.starBlue" class="section-icon" alt="" />
                <h3 class="section-title">🏪 热门店铺</h3>
              </div>
              <SchemeADoodleFrame color="#fbbf24" class="hot-shop-card">
                <div class="hot-shop-card__inner">
                  <img :src="assets.paperclip" class="hot-shop-card__paperclip" alt="" />
                  <div class="hot-shop-card__header">
                    <span class="hot-shop-card__platform">{{ getPlatformIcon(hotShop.platform) }} {{ hotShop.platform }}</span>
                    <span class="hot-shop-card__status is-enabled">正常营业</span>
                  </div>
                  <h4 class="hot-shop-card__name">{{ hotShop.name }}</h4>
                  <div class="hot-shop-card__metrics">
                    <span class="hot-shop-card__metric">{{ hotShop.revenue }}</span>
                    <span class="hot-shop-card__divider">·</span>
                    <span class="hot-shop-card__metric">{{ hotShop.orders }} 单</span>
                  </div>
                  <div class="hot-shop-card__actions">
                    <button class="hot-shop-card__btn hot-shop-card__btn--edit">编辑</button>
                    <button class="hot-shop-card__btn hot-shop-card__btn--delete">删除</button>
                  </div>
                </div>
              </SchemeADoodleFrame>
            </div>

            <div class="section">
              <div class="section-head">
                <img :src="assets.starBlueOutline" class="section-icon" alt="" />
                <h3 class="section-title">店铺列表</h3>
                <span class="section-count">{{ normalShops.length }}家</span>
              </div>
              <div class="shop-grid">
                <SchemeADoodleFrame
                  v-for="shop in normalShops"
                  :key="shop.id"
                  class="shop-grid-card"
                  :color="shop.status === 'ENABLED' ? '#16a34a' : '#cbd5e1'"
                  @click="handleCardClick"
                >
                  <div class="shop-grid-card__inner">
                    <div class="shop-grid-card__platform">{{ getPlatformIcon(shop.platform) }}</div>
                    <div class="shop-grid-card__info">
                      <div class="shop-grid-card__name">{{ shop.name }}</div>
                      <div class="shop-grid-card__status" :class="shop.status === 'ENABLED' ? 'is-enabled' : 'is-disabled'">
                        {{ shop.status === 'ENABLED' ? '正常营业' : '休息中' }}
                      </div>
                      <div class="shop-grid-card__revenue">{{ shop.revenue }}</div>
                    </div>
                  </div>
                </SchemeADoodleFrame>
              </div>
            </div>

            <div class="section" v-if="disabledShops.length">
              <div class="section-head">
                <img :src="assets.starYellow" class="section-icon" alt="" />
                <h3 class="section-title">休息中</h3>
              </div>
              <div class="disabled-list">
                <SchemeADoodleFrame
                  v-for="shop in disabledShops"
                  :key="shop.id"
                  class="disabled-card"
                  color="#cbd5e1"
                  @click="handleCardClick"
                >
                  <div class="disabled-card__inner">
                    <span class="disabled-card__pause">⏸</span>
                    <div class="disabled-card__info">
                      <div class="disabled-card__name">{{ shop.name }}</div>
                      <div class="disabled-card__meta">{{ getPlatformIcon(shop.platform) }} {{ shop.platform }} · 扣点 {{ shop.fee }}</div>
                    </div>
                  </div>
                </SchemeADoodleFrame>
              </div>
            </div>

            <div class="fab-container">
              <SchemeADoodleFrame tag="button" color="#2563eb" class="fab-btn" @click="handleCreateClick">
                <span class="fab-icon">➕</span>
                <span class="fab-text">新建</span>
              </SchemeADoodleFrame>
            </div>
          </div>
        </template>

        <template v-else-if="activeScheme === 'scheme-b'">
          <div class="scheme-page">
            <div class="page-header">
              <h2 class="page-title">🛒 店铺管理</h2>
            </div>

            <SchemeADoodleFrame shape="pill" color="#2563eb" class="search-box">
              <div class="search-inner">
                <img :src="assets.search" class="search-icon" alt="" />
                <input type="text" placeholder="搜索店铺..." />
              </div>
            </SchemeADoodleFrame>

            <div class="platform-tabs">
              <div
                v-for="tab in platformTabs"
                :key="tab.id"
                class="platform-tab"
                :class="{ active: activePlatform === tab.id }"
                @click="activePlatform = tab.id"
              >
                <span class="platform-tab__icon">{{ tab.icon }}</span>
                <span class="platform-tab__name">{{ tab.name }}</span>
                <span class="platform-tab__count" v-if="tab.count > 0">{{ tab.count }}</span>
              </div>
            </div>

            <div class="section">
              <div class="section-head">
                <img :src="assets.starYellow" class="section-icon" alt="" />
                <h3 class="section-title">{{ currentPlatformName }}</h3>
              </div>
              <div class="shop-list">
                <SchemeADoodleFrame
                  v-for="shop in filteredShops"
                  :key="shop.id"
                  class="shop-list-card"
                  :color="shop.status === 'ENABLED' ? '#16a34a' : '#cbd5e1'"
                  @click="handleCardClick"
                >
                  <div class="shop-list-card__inner">
                    <div class="shop-list-card__left">
                      <span class="shop-list-card__platform">{{ getPlatformIcon(shop.platform) }}</span>
                      <div class="shop-list-card__info">
                        <h4 class="shop-list-card__name">{{ shop.name }}</h4>
                        <div class="shop-list-card__meta">
                          <span class="shop-list-card__status" :class="shop.status === 'ENABLED' ? 'is-enabled' : 'is-disabled'">
                            {{ shop.status === 'ENABLED' ? '正常营业' : '休息中' }}
                          </span>
                          <span class="shop-list-card__fee">扣点 {{ shop.fee }}</span>
                        </div>
                        <div class="shop-list-card__detail">
                          {{ shop.region }} · {{ shop.orders }} 单 · {{ shop.revenue }}
                        </div>
                      </div>
                    </div>
                    <div class="shop-list-card__actions">
                      <button class="shop-list-card__btn shop-list-card__btn--edit">编辑</button>
                      <button class="shop-list-card__btn shop-list-card__btn--delete">删除</button>
                    </div>
                  </div>
                </SchemeADoodleFrame>
              </div>
            </div>

            <img :src="assets.squiggleBlue" class="wave-divider" alt="" />

            <div class="fab-container">
              <SchemeADoodleFrame tag="button" color="#2563eb" class="fab-btn" @click="handleCreateClick">
                <span class="fab-icon">➕</span>
                <span class="fab-text">新建</span>
              </SchemeADoodleFrame>
            </div>
          </div>
        </template>

        <template v-else-if="activeScheme === 'scheme-c'">
          <div class="scheme-page">
            <div class="page-header">
              <h2 class="page-title">🛒 店铺管理</h2>
            </div>

            <SchemeADoodleFrame color="#2563eb" class="data-card">
              <div class="data-card__inner">
                <div class="data-card__title">📊 经营数据</div>
                <div class="data-card__metrics">
                  <div class="data-card__metric">
                    <div class="data-card__value">{{ stats.revenue }}</div>
                    <div class="data-card__label">总销售额</div>
                  </div>
                  <div class="data-card__metric">
                    <div class="data-card__value">{{ stats.fee }}</div>
                    <div class="data-card__label">平均扣点</div>
                  </div>
                  <div class="data-card__metric">
                    <div class="data-card__value">{{ stats.orders }}</div>
                    <div class="data-card__label">订单总数</div>
                  </div>
                </div>
                <div class="data-card__platforms">
                  <div class="data-card__platform-row" v-for="item in platformStats" :key="item.name">
                    <span class="data-card__platform-name">{{ item.icon }} {{ item.name }}</span>
                    <div class="data-card__bar-wrap">
                      <div class="data-card__bar" :style="{ width: item.percent + '%', background: item.color }"></div>
                    </div>
                    <span class="data-card__platform-percent">{{ item.percent }}%</span>
                  </div>
                </div>
              </div>
            </SchemeADoodleFrame>

            <SchemeADoodleFrame shape="pill" color="#2563eb" class="search-box">
              <div class="search-inner">
                <img :src="assets.search" class="search-icon" alt="" />
                <input type="text" placeholder="搜索店铺..." />
              </div>
            </SchemeADoodleFrame>

            <div class="section">
              <div class="section-head">
                <img :src="assets.starBlue" class="section-icon" alt="" />
                <h3 class="section-title">🛍️ 店铺列表</h3>
              </div>
              <div class="shop-compact-grid">
                <SchemeADoodleFrame
                  v-for="shop in shops"
                  :key="shop.id"
                  class="shop-compact-card"
                  :color="shop.status === 'ENABLED' ? '#16a34a' : '#cbd5e1'"
                  @click="handleCardClick"
                >
                  <div class="shop-compact-card__inner">
                    <span class="shop-compact-card__platform">{{ getPlatformIcon(shop.platform) }}</span>
                    <div class="shop-compact-card__name">{{ shop.name }}</div>
                    <div class="shop-compact-card__status" :class="shop.status === 'ENABLED' ? 'is-enabled' : 'is-disabled'">
                      {{ shop.status === 'ENABLED' ? '营' : '休' }}
                    </div>
                  </div>
                </SchemeADoodleFrame>
              </div>
            </div>

            <div class="fab-container">
              <SchemeADoodleFrame tag="button" color="#2563eb" class="fab-btn" @click="handleCreateClick">
                <span class="fab-icon">➕</span>
                <span class="fab-text">新建</span>
              </SchemeADoodleFrame>
            </div>
          </div>
        </template>

        <template v-else-if="activeScheme === 'scheme-d'">
          <div class="scheme-page">
            <div class="page-header">
              <h2 class="page-title">🛒 店铺管理</h2>
            </div>

            <SchemeADoodleFrame shape="pill" color="#2563eb" class="search-box">
              <div class="search-inner">
                <img :src="assets.search" class="search-icon" alt="" />
                <input type="text" placeholder="搜索店铺..." />
              </div>
            </SchemeADoodleFrame>

            <div class="section">
              <div class="platform-groups">
                <div
                  v-for="group in platformGroups"
                  :key="group.platform"
                  class="platform-group"
                >
                  <div class="platform-group__header" @click="toggleGroup(group.platform)">
                    <span class="platform-group__arrow" :class="{ expanded: expandedGroups.includes(group.platform) }">▼</span>
                    <span class="platform-group__icon">{{ getPlatformIcon(group.platform) }}</span>
                    <span class="platform-group__name">{{ group.platform }}</span>
                    <span class="platform-group__count">({{ group.shops.length }}家)</span>
                  </div>
                  <div class="platform-group__content" v-show="expandedGroups.includes(group.platform)">
                    <div class="platform-group__shop-list">
                      <SchemeADoodleFrame
                        v-for="shop in group.shops"
                        :key="shop.id"
                        class="group-shop-card"
                        :color="shop.status === 'ENABLED' ? '#16a34a' : '#cbd5e1'"
                        @click="handleCardClick"
                      >
                        <div class="group-shop-card__inner">
                          <div class="group-shop-card__left">
                            <span class="group-shop-card__status-icon" v-if="shop.status === 'DISABLED'">⏸</span>
                            <div class="group-shop-card__info">
                              <div class="group-shop-card__name">{{ shop.name }}</div>
                              <div class="group-shop-card__meta">
                                <span class="group-shop-card__status" :class="shop.status === 'ENABLED' ? 'is-enabled' : 'is-disabled'">
                                  {{ shop.status === 'ENABLED' ? '正常营业' : '休息中' }}
                                </span>
                                <span class="group-shop-card__fee">扣点 {{ shop.fee }}</span>
                              </div>
                            </div>
                          </div>
                          <div class="group-shop-card__actions">
                            <button class="group-shop-card__btn group-shop-card__btn--edit">编辑</button>
                            <button class="group-shop-card__btn group-shop-card__btn--delete">删除</button>
                          </div>
                        </div>
                      </SchemeADoodleFrame>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="fab-container">
              <SchemeADoodleFrame tag="button" color="#2563eb" class="fab-btn" @click="handleCreateClick">
                <span class="fab-icon">➕</span>
                <span class="fab-text">新建</span>
              </SchemeADoodleFrame>
            </div>
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
    name: '方案一：卡片瀑布风',
    description: '卡片瀑布布局 - 推荐方案',
    features: [
      '顶部四格数据概览卡片，使用不同颜色边框区分',
      '热门店铺使用大卡片突出展示，带有回形针装饰',
      '普通店铺使用双列卡片布局，大小一致',
      '休息中的店铺使用灰色边框区分，卡片较窄',
      '每个卡片显示平台图标、状态、销售额'
    ]
  },
  {
    id: 'scheme-b',
    name: '方案二：平台标签风',
    description: '平台标签 + 单列列表',
    features: [
      '顶部平台标签导航，支持横向滚动',
      '当前选中平台高亮显示并带有数量徽章',
      '内容区域只显示当前平台的店铺',
      '店铺卡片使用单列布局，信息展示完整',
      '底部手绘风格波浪线装饰'
    ]
  },
  {
    id: 'scheme-c',
    name: '方案三：数据卡片风',
    description: '大数据卡片 + 紧凑网格',
    features: [
      '顶部大数据卡片展示经营概览（销售额/扣点/订单）',
      '平台分布条形图，使用手绘风格',
      '搜索框位于数据卡片下方',
      '店铺列表使用三列网格布局，卡片小巧',
      '每个卡片只显示店铺名称、平台图标、状态'
    ]
  },
  {
    id: 'scheme-d',
    name: '方案四：折叠分组风',
    description: '平台分组 + 可折叠列表',
    features: [
      '按平台分组，每组可折叠/展开',
      '折叠状态显示平台图标和店铺数量',
      '展开状态显示该平台下的所有店铺卡片',
      '店铺卡片使用缩进方式显示层级关系',
      '手绘风格的折叠箭头（▼/▶）'
    ]
  }
]

const activeScheme = ref('scheme-a')

const currentScheme = computed(() => schemes.find(s => s.id === activeScheme.value)!)

interface Shop {
  id: string
  name: string
  platform: '淘宝' | '京东' | '拼多多' | '抖音'
  status: 'ENABLED' | 'DISABLED'
  fee: string
  region: string
  orders: number
  revenue: string
}

const shops: Shop[] = [
  { id: '1', name: '旗舰店', platform: '淘宝', status: 'ENABLED', fee: '12.3%', region: '广东省', orders: 156, revenue: '¥28.5万' },
  { id: '2', name: '品牌专营店', platform: '京东', status: 'ENABLED', fee: '9.8%', region: '北京市', orders: 89, revenue: '¥15.2万' },
  { id: '3', name: '分销小店', platform: '拼多多', status: 'DISABLED', fee: '6.2%', region: '浙江省', orders: 234, revenue: '¥8.7万' },
  { id: '4', name: '直播店', platform: '抖音', status: 'ENABLED', fee: '10.1%', region: '上海市', orders: 312, revenue: '¥45.8万' },
  { id: '5', name: '优选小店', platform: '淘宝', status: 'ENABLED', fee: '11.0%', region: '江苏省', orders: 78, revenue: '¥12.3万' },
  { id: '6', name: '自营旗舰', platform: '京东', status: 'ENABLED', fee: '9.2%', region: '广东省', orders: 145, revenue: '¥22.1万' },
]

const stats = {
  total: 12,
  online: 10,
  platforms: 5,
  fee: '8.5%',
  revenue: '¥132.6万',
  orders: '1,014'
}

const hotShop = computed(() => shops.find(s => s.revenue === '¥45.8万')!)

const normalShops = computed(() => shops.filter(s => s.status === 'ENABLED' && s.name !== '直播店'))

const disabledShops = computed(() => shops.filter(s => s.status === 'DISABLED'))

const platformTabs = [
  { id: 'all', name: '全部', icon: '📦', count: 6 },
  { id: '淘宝', name: '淘宝', icon: '🛒', count: 2 },
  { id: '京东', name: '京东', icon: '🏪', count: 2 },
  { id: '拼多多', name: '拼多多', icon: '📱', count: 1 },
  { id: '抖音', name: '抖音', icon: '🎵', count: 1 },
]

const activePlatform = ref('all')

const currentPlatformName = computed(() => {
  const tab = platformTabs.find(t => t.id === activePlatform.value)
  return tab ? tab.icon + ' ' + tab.name + '（' + tab.count + '家）' : ''
})

const filteredShops = computed(() => {
  if (activePlatform.value === 'all') return shops
  return shops.filter(s => s.platform === activePlatform.value)
})

const platformStats = [
  { name: '淘宝', icon: '🛒', percent: 35, color: '#f97316' },
  { name: '京东', icon: '🏪', percent: 28, color: '#e1251b' },
  { name: '抖音', icon: '🎵', percent: 37, color: '#111827' },
]

const expandedGroups = ref(['淘宝', '拼多多', '抖音'])

interface PlatformGroup {
  platform: string
  shops: Shop[]
}

const platformGroups = computed<PlatformGroup[]>(() => {
  const platforms = ['淘宝', '京东', '拼多多', '抖音'] as const
  return platforms.map(p => ({
    platform: p,
    shops: shops.filter(s => s.platform === p)
  })).filter(g => g.shops.length > 0)
})

function toggleGroup(platform: string) {
  const index = expandedGroups.value.indexOf(platform)
  if (index > -1) {
    expandedGroups.value.splice(index, 1)
  } else {
    expandedGroups.value.push(platform)
  }
}

function getPlatformIcon(platform: string): string {
  const icons: Record<string, string> = {
    '淘宝': '🛒',
    '京东': '🏪',
    '拼多多': '📱',
    '抖音': '🎵'
  }
  return icons[platform] || '📦'
}

function handleCardClick() {
  console.log('Card clicked')
}

function handleCreateClick() {
  console.log('Create shop clicked')
}
</script>

<style scoped lang="scss">
.shop-design-preview {
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

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.stat-card {
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

.hot-shop-card {
  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.hot-shop-card__inner {
  padding: 16px;
  position: relative;
}

.hot-shop-card__paperclip {
  position: absolute;
  top: -8px;
  right: 16px;
  width: 32px;
  height: 40px;
}

.hot-shop-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.hot-shop-card__platform {
  font-size: 13px;
  color: #f97316;
  font-weight: 600;
}

.hot-shop-card__status {
  font-size: 11px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 999px;

  &.is-enabled {
    background: #dcfce7;
    color: #16a34a;
  }
}

.hot-shop-card__name {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 20px;
  color: #1e293b;
  margin: 0 0 8px;
}

.hot-shop-card__metrics {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.hot-shop-card__metric {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
}

.hot-shop-card__divider {
  color: #94a3b8;
}

.hot-shop-card__actions {
  display: flex;
  gap: 8px;
  padding-top: 12px;
  border-top: 2px dashed #fcd34d;
}

.hot-shop-card__btn {
  flex: 1;
  padding: 8px 12px;
  border-radius: 8px;
  border: 2px solid;
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 13px;
  cursor: pointer;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(0.98);
  }

  &--edit {
    border-color: #16a34a;
    color: #16a34a;
    background: #f0fdf4;
  }

  &--delete {
    border-color: #ef4444;
    color: #ef4444;
    background: #fef2f2;
  }
}

.shop-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.shop-grid-card {
  cursor: pointer;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(0.98);
  }

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.shop-grid-card__inner {
  padding: 12px;
}

.shop-grid-card__platform {
  font-size: 24px;
  display: block;
  margin-bottom: 4px;
}

.shop-grid-card__info {
  text-align: center;
}

.shop-grid-card__name {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 14px;
  color: #1e293b;
  margin-bottom: 4px;
}

.shop-grid-card__status {
  font-size: 10px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 999px;
  display: inline-block;
  margin-bottom: 6px;

  &.is-enabled {
    background: #dcfce7;
    color: #16a34a;
  }

  &.is-disabled {
    background: #f1f5f9;
    color: #94a3b8;
  }
}

.shop-grid-card__revenue {
  font-size: 13px;
  font-weight: 700;
  color: #2563eb;
}

.disabled-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.disabled-card {
  cursor: pointer;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(0.98);
  }

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.disabled-card__inner {
  padding: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.disabled-card__pause {
  font-size: 18px;
}

.disabled-card__info {
  flex: 1;
}

.disabled-card__name {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 14px;
  color: #64748b;
  margin-bottom: 2px;
}

.disabled-card__meta {
  font-size: 11px;
  color: #94a3b8;
}

.platform-tabs {
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

.platform-tab {
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

  &:hover {
    transform: scale(0.98);
  }

  &.active {
    background: #2563eb;

    .platform-tab__name {
      color: white;
    }
  }
}

.platform-tab__icon {
  font-size: 16px;
}

.platform-tab__name {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 14px;
  color: #2563eb;
}

.platform-tab__count {
  font-size: 11px;
  font-weight: 700;
  color: white;
  background: #f59e0b;
  padding: 2px 6px;
  border-radius: 999px;
}

.shop-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.shop-list-card {
  cursor: pointer;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(0.98);
  }

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.shop-list-card__inner {
  padding: 14px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.shop-list-card__left {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.shop-list-card__platform {
  font-size: 24px;
}

.shop-list-card__info {
  flex: 1;
  min-width: 0;
}

.shop-list-card__name {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 16px;
  color: #1e293b;
  margin: 0 0 6px;
}

.shop-list-card__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.shop-list-card__status {
  font-size: 10px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 999px;

  &.is-enabled {
    background: #dcfce7;
    color: #16a34a;
  }

  &.is-disabled {
    background: #f1f5f9;
    color: #94a3b8;
  }
}

.shop-list-card__fee {
  font-size: 11px;
  color: #64748b;
}

.shop-list-card__detail {
  font-size: 12px;
  color: #94a3b8;
}

.shop-list-card__actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-left: 12px;
}

.shop-list-card__btn {
  padding: 6px 12px;
  border-radius: 6px;
  border: 2px solid;
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 12px;
  cursor: pointer;
  white-space: nowrap;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(0.98);
  }

  &--edit {
    border-color: #16a34a;
    color: #16a34a;
  }

  &--delete {
    border-color: #ef4444;
    color: #ef4444;
  }
}

.data-card {
  margin-bottom: 16px;

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
  color: #2563eb;
  margin-bottom: 14px;
}

.data-card__metrics {
  display: flex;
  justify-content: space-around;
  margin-bottom: 16px;
  padding-bottom: 14px;
  border-bottom: 2px dashed #93c5fd;
}

.data-card__metric {
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

.data-card__platforms {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.data-card__platform-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.data-card__platform-name {
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  width: 60px;
}

.data-card__bar-wrap {
  flex: 1;
  height: 12px;
  background: #e2e8f0;
  border-radius: 6px;
  overflow: hidden;
}

.data-card__bar {
  height: 100%;
  border-radius: 6px;
  transition: width 0.3s ease;
}

.data-card__platform-percent {
  font-size: 12px;
  font-weight: 700;
  color: #475569;
  width: 40px;
  text-align: right;
}

.shop-compact-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.shop-compact-card {
  cursor: pointer;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(0.98);
  }

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.shop-compact-card__inner {
  padding: 10px;
  text-align: center;
}

.shop-compact-card__platform {
  font-size: 20px;
  display: block;
  margin-bottom: 4px;
}

.shop-compact-card__name {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 12px;
  color: #1e293b;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.shop-compact-card__status {
  font-size: 10px;
  font-weight: 800;
  padding: 2px 6px;
  border-radius: 999px;
  display: inline-block;

  &.is-enabled {
    background: #dcfce7;
    color: #16a34a;
  }

  &.is-disabled {
    background: #f1f5f9;
    color: #94a3b8;
  }
}

.platform-groups {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.platform-group {
  background: white;
  border-radius: 12px;
  border: 2px solid #2563eb;
  overflow: hidden;
}

.platform-group__header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  cursor: pointer;
  transition: background 0.2s ease;

  &:hover {
    background: #f8fafc;
  }
}

.platform-group__arrow {
  font-size: 10px;
  color: #2563eb;
  transition: transform 0.2s ease;

  &.expanded {
    transform: rotate(0deg);
  }
}

.platform-group__icon {
  font-size: 18px;
}

.platform-group__name {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 16px;
  color: #1e293b;
}

.platform-group__count {
  font-size: 13px;
  color: #64748b;
}

.platform-group__content {
  padding: 0 14px 10px;
}

.platform-group__shop-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.group-shop-card {
  cursor: pointer;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(0.98);
  }

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.group-shop-card__inner {
  padding: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.group-shop-card__left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.group-shop-card__status-icon {
  font-size: 16px;
}

.group-shop-card__info {
  flex: 1;
  min-width: 0;
}

.group-shop-card__name {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 14px;
  color: #1e293b;
  margin-bottom: 4px;
}

.group-shop-card__meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.group-shop-card__status {
  font-size: 10px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 999px;

  &.is-enabled {
    background: #dcfce7;
    color: #16a34a;
  }

  &.is-disabled {
    background: #f1f5f9;
    color: #94a3b8;
  }
}

.group-shop-card__fee {
  font-size: 11px;
  color: #64748b;
}

.group-shop-card__actions {
  display: flex;
  gap: 6px;
}

.group-shop-card__btn {
  padding: 5px 10px;
  border-radius: 6px;
  border: 2px solid;
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 11px;
  cursor: pointer;
  white-space: nowrap;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(0.98);
  }

  &--edit {
    border-color: #16a34a;
    color: #16a34a;
  }

  &--delete {
    border-color: #ef4444;
    color: #ef4444;
  }
}

.wave-divider {
  width: 100%;
  height: 12px;
  margin: 16px 0;
}

.fab-container {
  position: fixed;
  bottom: 60px;
  right: 24px;
}

.fab-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 16px;
  border-radius: 999px;
  background: #2563eb;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.4);
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(1.05);
  }
}

.fab-icon {
  font-size: 20px;
  color: white;
}

.fab-text {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 15px;
  color: white;
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