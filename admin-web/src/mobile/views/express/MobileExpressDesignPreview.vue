<template>
  <div class="express-design-preview">
    <div class="preview-header">
      <h1 class="preview-title">快递管理界面设计</h1>
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
                <h2 class="page-title">🚚 快递管理</h2>
                <img :src="assets.starBlue" class="title-star" alt="" />
              </div>
            </div>

            <SchemeADoodleFrame shape="pill" color="#2563eb" class="search-box">
              <div class="search-inner">
                <img :src="assets.search" class="search-icon" alt="" />
                <input type="text" placeholder="搜索快递..." />
              </div>
            </SchemeADoodleFrame>

            <div class="section">
              <div class="section-head">
                <img :src="assets.starYellow" class="section-icon" alt="" />
                <h3 class="section-title">⭐ 今日数据概览</h3>
              </div>
              <div class="stats-grid">
                <SchemeADoodleFrame shape="rect" color="#2563eb" class="stat-card">
                  <div class="stat-card__value">{{ stats.total }}</div>
                  <div class="stat-card__label">站点总数</div>
                </SchemeADoodleFrame>
                <SchemeADoodleFrame shape="rect" color="#16a34a" class="stat-card">
                  <div class="stat-card__value">{{ stats.enabled }}</div>
                  <div class="stat-card__label">正常运营</div>
                </SchemeADoodleFrame>
                <SchemeADoodleFrame shape="rect" color="#3b82f6" class="stat-card">
                  <div class="stat-card__value">{{ defaultStation.name }}</div>
                  <div class="stat-card__label">默认快递</div>
                </SchemeADoodleFrame>
                <SchemeADoodleFrame shape="rect" color="#f97316" class="stat-card">
                  <div class="stat-card__value">¥{{ stats.avgPrice }}</div>
                  <div class="stat-card__label">均价面单费</div>
                </SchemeADoodleFrame>
              </div>
            </div>

            <div class="section">
              <div class="section-head">
                <img :src="assets.starBlue" class="section-icon" alt="" />
                <h3 class="section-title">📌 默认快递</h3>
              </div>
              <SchemeADoodleFrame shape="rect" color="#fbbf24" class="default-card">
                <div class="default-card__pin">📌</div>
                <div class="default-card__content">
                  <div class="default-card__header">
                    <span class="default-card__icon">📦</span>
                    <div class="default-card__info">
                      <div class="default-card__name">{{ defaultStation.name }}</div>
                      <div class="default-card__contact">📞 {{ defaultStation.contact }} · {{ defaultStation.address }}</div>
                    </div>
                  </div>
                  <div class="default-card__details">
                    <span class="default-card__tag">🏷️ 面单费 ¥{{ defaultStation.labelPrice }}</span>
                    <span class="default-card__tag">📍 覆盖{{ defaultStation.provinceCount }}省</span>
                    <span class="default-card__tag" v-if="defaultStation.noticeCount > 0">⚠️ {{ defaultStation.noticeCount }}条须知</span>
                  </div>
                  <div class="default-card__actions">
                    <button class="action-btn action-btn--edit">编辑</button>
                    <button class="action-btn action-btn--delete">删除</button>
                  </div>
                </div>
              </SchemeADoodleFrame>
            </div>

            <div class="section">
              <div class="section-head">
                <img :src="assets.starYellow" class="section-icon" alt="" />
                <h3 class="section-title">🏪 其他快递</h3>
              </div>
              <div class="shop-grid">
                <SchemeADoodleFrame
                  v-for="station in normalStations"
                  :key="station.id"
                  shape="rect"
                  color="#16a34a"
                  class="shop-card"
                >
                  <div class="shop-card__icon">📦</div>
                  <div class="shop-card__name">{{ station.name }}</div>
                  <div class="shop-card__contact">{{ station.contact }}</div>
                  <div class="shop-card__price">¥{{ station.labelPrice }}</div>
                  <div class="shop-card__province">📍{{ station.provinceCount }}省</div>
                </SchemeADoodleFrame>
              </div>
            </div>

            <div class="section" v-if="disabledStations.length">
              <div class="section-head">
                <img :src="assets.starBlue" class="section-icon" alt="" />
                <h3 class="section-title">⏸ 休息中的快递</h3>
              </div>
              <SchemeADoodleFrame
                v-for="station in disabledStations"
                :key="station.id"
                shape="rect"
                color="#94a3b8"
                class="disabled-card"
              >
                <div class="disabled-card__icon">⏸</div>
                <div class="disabled-card__content">
                  <div class="disabled-card__name">{{ station.name }}</div>
                  <div class="disabled-card__contact">📞 {{ station.contact }} · {{ station.address }}</div>
                </div>
              </SchemeADoodleFrame>
            </div>

            <button class="fab-btn">
              <span class="fab-btn__icon">➕</span>
              <span class="fab-btn__text">新增站点</span>
            </button>
          </div>
        </template>

        <template v-else-if="activeScheme === 'scheme-b'">
          <div class="scheme-page">
            <div class="page-header">
              <h2 class="page-title">🚚 快递管理</h2>
            </div>

            <SchemeADoodleFrame shape="pill" color="#2563eb" class="search-box">
              <div class="search-inner">
                <img :src="assets.search" class="search-icon" alt="" />
                <input type="text" placeholder="搜索快递..." />
              </div>
            </SchemeADoodleFrame>

            <div class="platform-tabs">
              <div
                v-for="tab in expressTabs"
                :key="tab.id"
                class="platform-tab"
                :class="{ active: activeExpress === tab.id }"
                @click="activeExpress = tab.id"
              >
                <span class="platform-tab__icon">{{ tab.icon }}</span>
                <span class="platform-tab__name">{{ tab.name }}</span>
              </div>
            </div>

            <div class="section">
              <div class="section-head">
                <img :src="assets.starYellow" class="section-icon" alt="" />
                <h3 class="section-title">💰 当前：{{ currentExpressName }}</h3>
              </div>
              <div class="express-detail-list">
                <SchemeADoodleFrame
                  v-for="station in filteredExpress"
                  :key="station.id"
                  shape="rect"
                  :color="station.isDefault ? '#fbbf24' : '#2563eb'"
                  class="express-detail-card"
                >
                  <div class="express-detail-card__header">
                    <span class="express-detail-card__icon">📦</span>
                    <div class="express-detail-card__info">
                      <div class="express-detail-card__name">{{ station.name }}</div>
                      <div class="express-detail-card__contact">📞 {{ station.contact }} · {{ station.address }}</div>
                    </div>
                  </div>
                  <div class="express-detail-card__divider"></div>
                  <div class="express-detail-card__details">
                    <span class="express-detail-card__tag">🏷️ 面单费 ¥{{ station.labelPrice }}</span>
                    <span class="express-detail-card__tag">📍 覆盖{{ station.provinceCount }}省份</span>
                    <span class="express-detail-card__tag" v-if="station.isDefault">⭐ 默认</span>
                    <span class="express-detail-card__tag" v-if="station.noticeCount > 0">⚠️ {{ station.noticeCount }}条须知</span>
                  </div>
                  <div class="express-detail-card__actions">
                    <button class="action-btn action-btn--edit">编辑</button>
                    <button class="action-btn action-btn--delete">删除</button>
                  </div>
                </SchemeADoodleFrame>
              </div>
            </div>

            <div class="wave-divider">
              <svg viewBox="0 0 200 20" class="wave-svg">
                <path d="M0,10 Q25,0 50,10 T100,10 T150,10 T200,10 V20 H0 Z" fill="#93c5fd" opacity="0.5"/>
              </svg>
            </div>

            <button class="fab-btn">
              <span class="fab-btn__icon">➕</span>
              <span class="fab-btn__text">新增站点</span>
            </button>
          </div>
        </template>

        <template v-else-if="activeScheme === 'scheme-c'">
          <div class="scheme-page">
            <div class="page-header">
              <h2 class="page-title">🚚 快递管理</h2>
            </div>

            <SchemeADoodleFrame shape="rect" color="#2563eb" class="data-card">
              <div class="data-card__header">
                <span class="data-card__icon">📊</span>
                <span class="data-card__title">快递数据卡</span>
              </div>
              <div class="data-card__stats">
                <div class="data-card__stat">
                  <div class="data-card__value">{{ stats.total }}</div>
                  <div class="data-card__label">站点总数</div>
                </div>
                <div class="data-card__stat">
                  <div class="data-card__value">{{ defaultStation.name }}</div>
                  <div class="data-card__label">默认快递</div>
                </div>
                <div class="data-card__stat">
                  <div class="data-card__value">¥{{ stats.avgPrice }}</div>
                  <div class="data-card__label">平均面单费</div>
                </div>
              </div>
              <div class="data-card__platforms">
                <div class="data-card__platform-row" v-for="item in expressStats" :key="item.name">
                  <span class="data-card__platform-name">{{ item.icon }} {{ item.name }}</span>
                  <div class="data-card__bar-wrap">
                    <div class="data-card__bar" :style="{ width: item.percent + '%', background: item.color }"></div>
                  </div>
                  <span class="data-card__platform-percent">{{ item.percent }}%</span>
                </div>
              </div>
            </SchemeADoodleFrame>

            <SchemeADoodleFrame shape="pill" color="#2563eb" class="search-box">
              <div class="search-inner">
                <img :src="assets.search" class="search-icon" alt="" />
                <input type="text" placeholder="搜索快递..." />
              </div>
            </SchemeADoodleFrame>

            <div class="section">
              <div class="section-head">
                <img :src="assets.starBlue" class="section-icon" alt="" />
                <h3 class="section-title">🛒 快递列表</h3>
              </div>
              <div class="shop-compact-grid">
                <SchemeADoodleFrame
                  v-for="station in stations"
                  :key="station.id"
                  shape="rect"
                  :color="station.status === 'ENABLED' ? '#16a34a' : '#94a3b8'"
                  class="compact-card"
                >
                  <div class="compact-card__inner">
                    <div class="compact-card__icon">📦</div>
                    <div class="compact-card__name">{{ station.name }}</div>
                    <div class="compact-card__price">¥{{ station.labelPrice }}</div>
                    <span
                      class="compact-card__status"
                      :class="{
                        'is-default': station.isDefault,
                        'is-operating': station.status === 'ENABLED' && !station.isDefault,
                        'is-resting': station.status === 'DISABLED'
                      }"
                    >
                      {{ station.isDefault ? '默认' : (station.status === 'ENABLED' ? '正常' : '休息') }}
                    </span>
                  </div>
                </SchemeADoodleFrame>
              </div>
            </div>

            <button class="fab-btn">
              <span class="fab-btn__icon">➕</span>
              <span class="fab-btn__text">新增站点</span>
            </button>
          </div>
        </template>

        <template v-else-if="activeScheme === 'scheme-d'">
          <div class="scheme-page">
            <div class="page-header">
              <h2 class="page-title">🚚 快递管理</h2>
            </div>

            <SchemeADoodleFrame shape="pill" color="#2563eb" class="search-box">
              <div class="search-inner">
                <img :src="assets.search" class="search-icon" alt="" />
                <input type="text" placeholder="搜索快递..." />
              </div>
            </SchemeADoodleFrame>

            <div class="section">
              <div class="section-head">
                <img :src="assets.starYellow" class="section-icon" alt="" />
                <h3 class="section-title">🔥 价格热力图</h3>
              </div>
              <SchemeADoodleFrame shape="rect" color="#2563eb" class="heatmap-card">
                <div class="heatmap-card__item">
                  <span class="heatmap-card__icon">📍</span>
                  <span class="heatmap-card__region">华北区</span>
                  <span class="heatmap-card__price">¥2.10 ~ ¥3.50</span>
                </div>
                <div class="heatmap-card__item">
                  <span class="heatmap-card__icon">📍</span>
                  <span class="heatmap-card__region">华东区</span>
                  <span class="heatmap-card__price">¥1.50 ~ ¥2.80</span>
                </div>
                <div class="heatmap-card__item">
                  <span class="heatmap-card__icon">📍</span>
                  <span class="heatmap-card__region">华南区</span>
                  <span class="heatmap-card__price">¥1.60 ~ ¥3.20</span>
                </div>
                <div class="heatmap-card__item">
                  <span class="heatmap-card__icon">📍</span>
                  <span class="heatmap-card__region">西南区</span>
                  <span class="heatmap-card__price">¥1.80 ~ ¥4.00</span>
                </div>
              </SchemeADoodleFrame>
            </div>

            <div class="section">
              <div class="section-head">
                <img :src="assets.starBlue" class="section-icon" alt="" />
                <h3 class="section-title">📋 站点列表</h3>
              </div>
              <div class="group-list">
                <div
                  v-for="station in stations"
                  :key="station.id"
                  class="group-item"
                >
                  <button
                    class="group-item__header"
                    @click="toggleGroup(station.id)"
                  >
                    <span class="group-item__arrow" :class="{ expanded: expandedGroups.has(station.id) }">
                      {{ expandedGroups.has(station.id) ? '▼' : '▶' }}
                    </span>
                    <span class="group-item__icon">{{ station.isDefault ? '⭐' : '📦' }}</span>
                    <span class="group-item__name">{{ station.name }}</span>
                    <span class="group-item__badge" v-if="station.isDefault">默认</span>
                  </button>
                  <div v-show="expandedGroups.has(station.id)" class="group-item__body">
                    <SchemeADoodleFrame
                      shape="rect"
                      :color="station.status === 'ENABLED' ? '#16a34a' : '#94a3b8'"
                      class="group-card"
                    >
                      <div class="group-card__contact">📞 {{ station.contact }}</div>
                      <div class="group-card__details">
                        <span>🏷️ ¥{{ station.labelPrice }}</span>
                        <span>📍 {{ station.provinceCount }}省</span>
                        <span v-if="station.noticeCount > 0">⚠️ {{ station.noticeCount }}条</span>
                      </div>
                      <div class="group-card__actions">
                        <button class="action-btn action-btn--edit">编辑</button>
                        <button class="action-btn action-btn--delete">删除</button>
                      </div>
                    </SchemeADoodleFrame>
                  </div>
                </div>
              </div>
            </div>

            <button class="fab-btn">
              <span class="fab-btn__icon">➕</span>
              <span class="fab-btn__text">新增站点</span>
            </button>
          </div>
        </template>
      </div>

      <div class="scheme-info">
        <h3 class="scheme-info__title">{{ currentScheme.name }}</h3>
        <p class="scheme-info__description">{{ currentScheme.description }}</p>
        <ul class="scheme-info__features">
          <li v-for="(feature, index) in currentScheme.features" :key="index">
            <span class="feature-icon">✓</span>
            {{ feature }}
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import SchemeADoodleFrame from '@/mobile/views/home/themes/scheme-a/SchemeADoodleFrame.vue'
import { schemeAAssets as assets } from '@/mobile/views/home/themes/scheme-a/assets.ts'

const schemes = [
  {
    id: 'scheme-a',
    name: '方案一：卡片列表风',
    description: '卡片瀑布布局 - 推荐方案',
    features: [
      '顶部四格数据概览卡片，使用不同颜色边框区分',
      '默认快递使用大卡片突出展示，带有回形针装饰',
      '普通快递使用双列卡片布局，大小一致',
      '休息中的快递使用灰色边框区分，卡片较窄',
      '每个卡片显示联系人、面单费、覆盖省份数'
    ]
  },
  {
    id: 'scheme-b',
    name: '方案二：价格标签风',
    description: '快递标签 + 单列列表',
    features: [
      '顶部快递标签导航，支持横向滚动',
      '当前选中快递高亮显示，带有手绘边框',
      '内容区域只显示当前快递的详情',
      '快递卡片使用单列布局，信息展示完整',
      '底部手绘风格波浪线装饰'
    ]
  },
  {
    id: 'scheme-c',
    name: '方案三：数据卡片风',
    description: '大数据卡片 + 紧凑网格',
    features: [
      '顶部大数据卡片展示快递概览（站点数/默认/均价）',
      '快递分布条形图，使用手绘风格',
      '搜索框位于数据卡片下方',
      '快递列表使用三列网格布局，卡片小巧',
      '每个卡片只显示快递名称、面单费、状态'
    ]
  },
  {
    id: 'scheme-d',
    name: '方案四：价格热力图风',
    description: '热力图 + 折叠分组',
    features: [
      '顶部价格热力图展示各区域价格范围',
      '按站点分组，每组可折叠/展开',
      '默认快递自动展开，其他快递默认折叠',
      '折叠状态显示快递图标和名称',
      '展开状态显示快递详情（联系人/面单费/省份/须知）'
    ]
  }
]

const activeScheme = ref('scheme-a')
const activeExpress = ref('all')
const expandedGroups = ref(new Set<string>())

const currentScheme = computed(() => schemes.find(s => s.id === activeScheme.value)!)

interface ExpressStation {
  id: string
  name: string
  contact: string
  address: string
  labelPrice: string
  isDefault: boolean
  provinceCount: number
  noticeCount: number
  status: 'ENABLED' | 'DISABLED'
}

const stations: ExpressStation[] = [
  { id: '1', name: '顺丰速运', contact: '张经理', address: '广州市白云区', labelPrice: '2.50', isDefault: true, provinceCount: 31, noticeCount: 2, status: 'ENABLED' },
  { id: '2', name: '中通快递', contact: '李主管', address: '深圳市南山区', labelPrice: '1.80', isDefault: false, provinceCount: 28, noticeCount: 1, status: 'ENABLED' },
  { id: '3', name: '圆通速递', contact: '王站长', address: '杭州市西湖区', labelPrice: '1.60', isDefault: false, provinceCount: 25, noticeCount: 0, status: 'ENABLED' },
  { id: '4', name: '申通快递', contact: '陈老板', address: '上海市浦东新区', labelPrice: '1.70', isDefault: false, provinceCount: 30, noticeCount: 3, status: 'DISABLED' },
  { id: '5', name: '韵达快递', contact: '赵主任', address: '北京市朝阳区', labelPrice: '1.90', isDefault: false, provinceCount: 26, noticeCount: 1, status: 'ENABLED' },
  { id: '6', name: '京东物流', contact: '刘经理', address: '成都市武侯区', labelPrice: '3.20', isDefault: false, provinceCount: 22, noticeCount: 0, status: 'ENABLED' },
]

const stats = computed(() => {
  const enabled = stations.filter(s => s.status === 'ENABLED').length
  const prices = stations.filter(s => s.status === 'ENABLED').map(s => parseFloat(s.labelPrice))
  const avgPrice = prices.length ? (prices.reduce((a, b) => a + b, 0) / prices.length).toFixed(2) : '0.00'
  return {
    total: stations.length,
    enabled,
    avgPrice
  }
})

const defaultStation = computed(() => stations.find(s => s.isDefault)!)

const normalStations = computed(() => stations.filter(s => s.status === 'ENABLED' && !s.isDefault))

const disabledStations = computed(() => stations.filter(s => s.status === 'DISABLED'))

const expressTabs = [
  { id: 'all', name: '全部', icon: '📦' },
  { id: 'sf', name: '顺丰', icon: '💎' },
  { id: 'zt', name: '中通', icon: '🟢' },
  { id: 'yt', name: '圆通', icon: '🔴' },
  { id: 'st', name: '申通', icon: '🟡' },
  { id: 'yd', name: '韵达', icon: '🔵' },
  { id: 'jd', name: '京东', icon: '🔷' },
]

const currentExpressName = computed(() => {
  const tab = expressTabs.find(t => t.id === activeExpress.value)
  return tab?.name || '全部'
})

const filteredExpress = computed(() => {
  if (activeExpress.value === 'all') return stations
  const map: Record<string, string> = {
    'sf': '顺丰速运',
    'zt': '中通快递',
    'yt': '圆通速递',
    'st': '申通快递',
    'yd': '韵达快递',
    'jd': '京东物流'
  }
  return stations.filter(s => s.name.includes(map[activeExpress.value]))
})

const expressStats = computed(() => {
  const total = stations.length
  const platformData = [
    { name: '顺丰', icon: '💎', count: 1, color: '#fbbf24' },
    { name: '中通', icon: '🟢', count: 1, color: '#16a34a' },
    { name: '圆通', icon: '🔴', count: 1, color: '#ef4444' },
    { name: '其他', icon: '📦', count: 3, color: '#94a3b8' },
  ]
  return platformData.map(item => ({
    ...item,
    percent: total > 0 ? Math.round((item.count / total) * 100) : 0
  }))
})

function toggleGroup(id: string) {
  const next = new Set(expandedGroups.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  expandedGroups.value = next
}
</script>

<style scoped lang="scss">
.express-design-preview {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 20px;
}

.preview-header {
  text-align: center;
  margin-bottom: 20px;
}

.preview-title {
  font-size: 28px;
  font-weight: 800;
  color: #1e293b;
  margin: 0;
}

.preview-subtitle {
  font-size: 14px;
  color: #64748b;
  margin: 8px 0 0;
}

.scheme-tabs {
  display: flex;
  gap: 10px;
  justify-content: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.scheme-tab {
  padding: 8px 16px;
  border: 2px solid #cbd5e1;
  border-radius: 999px;
  background: #fff;
  font-size: 14px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;

  &.active {
    border-color: #2563eb;
    background: #eff6ff;
    color: #2563eb;
  }
}

.preview-container {
  display: flex;
  gap: 30px;
  max-width: 1200px;
  margin: 0 auto;
  justify-content: center;
  flex-wrap: wrap;
}

.mobile-mockup {
  width: 375px;
  background: #fff;
  border-radius: 40px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.15);
  padding: 20px;
  overflow: hidden;
  border: 8px solid #1e293b;
}

.mockup-header {
  height: 30px;
  display: flex;
  justify-content: center;
  margin-bottom: 10px;
}

.mockup-notch {
  width: 120px;
  height: 20px;
  background: #1e293b;
  border-radius: 0 0 20px 20px;
}

.scheme-page {
  background: #faf8f5;
  border-radius: 30px;
  padding: 16px;
  min-height: 600px;
  position: relative;
}

.page-header {
  margin-bottom: 16px;
}

.page-title-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.page-title {
  font-size: 20px;
  font-weight: 800;
  color: #1e293b;
  margin: 0;
}

.title-star {
  width: 24px;
  height: 24px;
}

.search-box {
  margin-bottom: 16px;

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.search-inner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;

  input {
    flex: 1;
    border: none;
    outline: none;
    background: transparent;
    font-size: 16px;
    color: #1e293b;

    &::placeholder {
      color: #94a3b8;
    }
  }
}

.search-icon {
  width: 22px;
  height: 22px;
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
  font-size: 16px;
  font-weight: 800;
  color: #1e293b;
  margin: 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.stat-card {
  :deep(.sa-doodle-frame__body) {
    padding: 12px 8px;
    text-align: center;
  }

  &__value {
    font-size: 24px;
    font-weight: 800;
    color: #1e293b;
  }

  &__label {
    font-size: 11px;
    font-weight: 600;
    color: #64748b;
    margin-top: 4px;
  }
}

.default-card {
  position: relative;

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
  }

  &__header {
    display: flex;
    gap: 12px;
    margin-bottom: 12px;
  }

  &__icon {
    font-size: 32px;
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
  }

  &__actions {
    display: flex;
    gap: 10px;
    padding-top: 12px;
    border-top: 2px dashed #fcd34d;
  }
}

.shop-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.shop-card {
  :deep(.sa-doodle-frame__body) {
    padding: 12px 8px;
    text-align: center;
    background: #f0fdf4;
  }

  &__icon {
    font-size: 28px;
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

.disabled-card {
  :deep(.sa-doodle-frame__body) {
    padding: 12px;
    background: #f8fafc;
    opacity: 0.7;
  }

  display: flex;
  align-items: center;
  gap: 12px;

  &__icon {
    font-size: 24px;
  }

  &__content {
    flex: 1;
  }

  &__name {
    font-size: 14px;
    font-weight: 700;
    color: #64748b;
  }

  &__contact {
    font-size: 11px;
    color: #94a3b8;
    margin-top: 2px;
  }
}

.platform-tabs {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 8px;
  margin-bottom: 16px;

  &::-webkit-scrollbar {
    display: none;
  }
}

.platform-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border: 2px solid #cbd5e1;
  border-radius: 999px;
  background: #fff;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.2s;

  &.active {
    border-color: #2563eb;
    background: #eff6ff;

    .platform-tab__name {
      color: #2563eb;
      font-weight: 800;
    }
  }

  &__icon {
    font-size: 16px;
  }

  &__name {
    font-size: 13px;
    font-weight: 600;
    color: #64748b;
  }
}

.express-detail-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.express-detail-card {
  :deep(.sa-doodle-frame__body) {
    padding: 14px;
    background: #fff;
  }

  &__header {
    display: flex;
    gap: 12px;
    margin-bottom: 12px;
  }

  &__icon {
    font-size: 28px;
  }

  &__info {
    flex: 1;
  }

  &__name {
    font-size: 16px;
    font-weight: 800;
    color: #1e293b;
  }

  &__contact {
    font-size: 12px;
    color: #64748b;
    margin-top: 3px;
  }

  &__divider {
    height: 2px;
    background: #e2e8f0;
    border-radius: 1px;
    margin-bottom: 12px;
  }

  &__details {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 12px;
  }

  &__tag {
    font-size: 11px;
    font-weight: 600;
    color: #64748b;
    background: #f1f5f9;
    padding: 3px 8px;
    border-radius: 6px;

    &:nth-child(3) {
      color: #fbbf24;
      background: #fffbeb;
    }

    &:nth-child(4) {
      color: #ef4444;
      background: #fef2f2;
    }
  }

  &__actions {
    display: flex;
    gap: 10px;
    justify-content: flex-end;
  }
}

.wave-divider {
  margin: 20px 0;
}

.wave-svg {
  width: 100%;
  height: 20px;
}

.data-card {
  margin-bottom: 16px;

  :deep(.sa-doodle-frame__body) {
    padding: 16px;
    background: #eff6ff;
  }

  &__header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
  }

  &__icon {
    font-size: 20px;
  }

  &__title {
    font-size: 16px;
    font-weight: 800;
    color: #2563eb;
  }

  &__stats {
    display: flex;
    justify-content: space-around;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 2px dashed #93c5fd;
  }

  &__stat {
    text-align: center;

    &__value {
      font-size: 22px;
      font-weight: 800;
      color: #1e293b;
    }

    &__label {
      font-size: 11px;
      font-weight: 600;
      color: #64748b;
      margin-top: 2px;
    }
  }

  &__platforms {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  &__platform-row {
    display: flex;
    align-items: center;
    gap: 8px;

    &__name {
      font-size: 12px;
      font-weight: 700;
      color: #374151;
      width: 50px;
      flex-shrink: 0;
    }

    &__bar-wrap {
      flex: 1;
      height: 10px;
      background: #dbeafe;
      border-radius: 5px;
      overflow: hidden;
    }

    &__bar {
      height: 100%;
      border-radius: 5px;
    }

    &__percent {
      font-size: 12px;
      font-weight: 700;
      color: #64748b;
      width: 36px;
      text-align: right;
    }
  }
}

.shop-compact-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.compact-card {
  :deep(.sa-doodle-frame__body) {
    padding: 10px 6px 8px;
    display: flex;
    flex-direction: column;
    align-items: center;
    min-height: 90px;
    background: #fff;
  }

  &__inner {
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  &__icon {
    font-size: 24px;
  }

  &__name {
    font-size: 12px;
    font-weight: 800;
    color: #1e293b;
    margin-top: 6px;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    overflow: hidden;
    width: 100%;
    line-height: 1.3;
  }

  &__price {
    font-size: 12px;
    font-weight: 700;
    color: #f97316;
    margin-top: 4px;
  }

  &__status {
    font-size: 9px;
    font-weight: 800;
    padding: 2px 6px;
    border-radius: 999px;
    margin-top: 4px;

    &.is-default {
      color: #f59e0b;
      background: #fffbeb;
    }

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

.heatmap-card {
  :deep(.sa-doodle-frame__body) {
    padding: 14px;
    background: #fff7ed;
  }

  &__item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px 0;

    &:not(:last-child) {
      border-bottom: 1px dashed #fdba74;
    }
  }

  &__icon {
    font-size: 16px;
  }

  &__region {
    flex: 1;
    font-size: 14px;
    font-weight: 700;
    color: #1e293b;
  }

  &__price {
    font-size: 13px;
    font-weight: 800;
    color: #f97316;
  }
}

.group-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.group-item {
  border: 2px solid #e2e8f0;
  border-radius: 14px;
  overflow: hidden;
}

.group-item__header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  background: #fff;
  border: none;
  cursor: pointer;
  width: 100%;
  text-align: left;

  &:hover {
    background: #f8fafc;
  }
}

.group-item__arrow {
  font-size: 10px;
  color: #64748b;
  transition: transform 0.2s;

  &.expanded {
    transform: rotate(0deg);
  }
}

.group-item__icon {
  font-size: 18px;
}

.group-item__name {
  flex: 1;
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
}

.group-item__badge {
  font-size: 10px;
  font-weight: 800;
  color: #f59e0b;
  background: #fffbeb;
  padding: 2px 8px;
  border-radius: 999px;
}

.group-item__body {
  padding: 0 14px 14px;
}

.group-card {
  :deep(.sa-doodle-frame__body) {
    padding: 12px;
    background: #f0fdf4;
  }

  &__contact {
    font-size: 13px;
    color: #64748b;
    margin-bottom: 8px;
  }

  &__details {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 10px;

    span {
      font-size: 11px;
      font-weight: 600;
      color: #64748b;
      background: #fff;
      padding: 3px 6px;
      border-radius: 4px;
    }
  }

  &__actions {
    display: flex;
    gap: 8px;
    justify-content: flex-end;
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

  &--edit {
    background: #e9d5ff;
    color: #8b5cf6;
  }

  &--delete {
    background: #fee2e2;
    color: #ef4444;
  }
}

.fab-btn {
  position: fixed;
  right: 24px;
  bottom: 24px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: #2563eb;
  color: #fff;
  border: none;
  border-radius: 24px;
  font-size: 15px;
  font-weight: 700;
  box-shadow: 0 8px 24px rgba(37,99,235,0.35);
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
  z-index: 100;

  &:active {
    transform: scale(0.95);
    box-shadow: 0 4px 12px rgba(37,99,235,0.35);
  }

  &__icon {
    font-size: 20px;
  }

  &__text {
    font-weight: 800;
  }
}

.scheme-info {
  max-width: 320px;
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
}

.scheme-info__title {
  font-size: 20px;
  font-weight: 800;
  color: #1e293b;
  margin: 0 0 8px;
}

.scheme-info__description {
  font-size: 14px;
  color: #64748b;
  margin: 0 0 16px;
}

.scheme-info__features {
  list-style: none;
  padding: 0;
  margin: 0;
}

.scheme-info__features li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 8px 0;
  font-size: 14px;
  color: #374151;

  &:not(:last-child) {
    border-bottom: 1px solid #f1f5f9;
  }
}

.feature-icon {
  color: #16a34a;
  font-weight: 800;
  flex-shrink: 0;
}
</style>
