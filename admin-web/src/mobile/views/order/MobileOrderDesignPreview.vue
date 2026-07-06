<template>
  <div class="order-design-preview">
    <div class="preview-header">
      <h1 class="preview-title">订单中心界面设计</h1>
      <p class="preview-subtitle">手绘风格手机端订单管理方案</p>
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

        <!-- ═══════════ 方案一：卡片列表风 ═══════════ -->
        <template v-if="activeScheme === 'scheme-a'">
          <div class="scheme-page">
            <div class="page-header">
              <div class="page-title-wrap">
                <svg class="title-star" viewBox="0 0 24 24" fill="#f59e0b" width="24" height="24">
                  <path d="M12 2l3.09 6.26L22 9.27l-5 4.87L18.18 22 12 18.56 5.82 22 7 14.14 2 9.27l6.91-1.01L12 2z"/>
                </svg>
                <h2 class="page-title">订单中心</h2>
                <svg class="title-star" viewBox="0 0 24 24" fill="#3b82f6" width="24" height="24">
                  <path d="M12 2l3.09 6.26L22 9.27l-5 4.87L18.18 22 12 18.56 5.82 22 7 14.14 2 9.27l6.91-1.01L12 2z"/>
                </svg>
              </div>
            </div>

            <!-- 统计卡片 -->
            <div class="stats-row">
              <div class="doodle-frame" style="border-color: #3b82f6;">
                <div class="stat-card__body">
                  <div class="stat-card__value">128</div>
                  <div class="stat-card__label">总订单</div>
                </div>
              </div>
              <div class="doodle-frame" style="border-color: #f97316;">
                <div class="stat-card__body">
                  <div class="stat-card__value">18</div>
                  <div class="stat-card__label">待处理</div>
                </div>
              </div>
              <div class="doodle-frame" style="border-color: #22c55e;">
                <div class="stat-card__body">
                  <div class="stat-card__value">￥8.6w</div>
                  <div class="stat-card__label">本月收入</div>
                </div>
              </div>
              <div class="doodle-frame" style="border-color: #ef4444;">
                <div class="stat-card__body">
                  <div class="stat-card__value">3</div>
                  <div class="stat-card__label">退款中</div>
                </div>
              </div>
            </div>

            <!-- 搜索框 -->
            <div class="doodle-frame pill" style="border-color: #3b82f6; margin-bottom: 14px;">
              <div class="search-inner">
                <svg viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="2.5" width="18" height="18">
                  <circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/>
                </svg>
                <input type="text" placeholder="搜索订单号/买家..." />
              </div>
            </div>

            <!-- 状态标签 -->
            <div class="status-tabs">
              <div
                v-for="tab in statusTabs"
                :key="tab.key"
                class="status-tab"
                :class="{ active: activeTab === tab.key }"
                @click="activeTab = tab.key"
              >
                {{ tab.label }}
                <span class="status-tab__badge" v-if="tab.count">{{ tab.count }}</span>
              </div>
            </div>

            <!-- 订单列表 -->
            <div class="order-list">
              <div
                v-for="order in filteredOrders"
                :key="order.id"
                class="doodle-frame order-card"
                :style="{ borderColor: orderStatusColor(order.status) }"
              >
                <div class="order-card__inner">
                  <div class="order-card__head">
                    <span class="order-card__no">#{{ order.orderNo }}</span>
                    <span class="order-card__shop">{{ order.shopName }}</span>
                  </div>
                  <div class="order-card__body">
                    <div class="order-card__buyer">
                      <svg viewBox="0 0 24 24" fill="none" stroke="#64748b" stroke-width="2" width="14" height="14">
                        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
                      </svg>
                      {{ order.buyerName }}
                    </div>
                    <div class="order-card__amount">
                      <span class="order-card__price">￥{{ order.amount }}</span>
                      <span class="order-card__profit" v-if="order.profit > 0">+￥{{ order.profit }}</span>
                    </div>
                  </div>
                  <div class="order-card__footer">
                    <span class="order-card__time">{{ order.time }}</span>
                    <span class="order-card__status" :style="{ color: orderStatusColor(order.status) }">
                      {{ order.statusLabel }}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <div class="load-more">下拉加载更多...</div>
          </div>
        </template>

        <!-- ═══════════ 方案二：时间轴风 ═══════════ -->
        <template v-else-if="activeScheme === 'scheme-b'">
          <div class="scheme-page">
            <div class="page-header">
              <h2 class="page-title">订单中心</h2>
            </div>

            <div class="doodle-frame pill" style="border-color: #3b82f6; margin-bottom: 14px;">
              <div class="search-inner">
                <svg viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="2.5" width="18" height="18">
                  <circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/>
                </svg>
                <input type="text" placeholder="搜索订单..." />
              </div>
            </div>

            <!-- 快速筛选行 -->
            <div class="filter-chips">
              <div
                v-for="chip in filterChips"
                :key="chip.key"
                class="filter-chip"
                :class="{ active: activeFilter === chip.key }"
                :style="{ '--chip-color': chip.color }"
                @click="activeFilter = chip.key"
              >
                {{ chip.label }}
              </div>
            </div>

            <!-- 时间线分组 -->
            <div class="timeline">
              <div v-for="(group, gIdx) in timelineGroups" :key="gIdx" class="timeline-group">
                <div class="timeline-group__header">
                  <div class="timeline-dot"></div>
                  <span class="timeline-group__label">{{ group.label }}</span>
                  <span class="timeline-group__count">{{ group.orders.length }}单</span>
                </div>
                <div class="timeline-group__orders">
                  <div
                    v-for="(order, oIdx) in group.orders"
                    :key="order.id"
                    class="doodle-frame timeline-card"
                    :style="{ borderColor: orderStatusColor(order.status) }"
                  >
                    <div class="timeline-card__inner">
                      <div class="timeline-card__left">
                        <div class="timeline-card__no">#{{ order.orderNo }}</div>
                        <div class="timeline-card__buyer">{{ order.buyerName }}</div>
                        <div class="timeline-card__meta">
                          <span class="timeline-card__items">{{ order.items }}件商品</span>
                          <span class="timeline-card__time">{{ order.timeShort }}</span>
                        </div>
                      </div>
                      <div class="timeline-card__right">
                        <div class="timeline-card__amount">￥{{ order.amount }}</div>
                        <div class="timeline-card__status" :style="{ background: orderStatusColor(order.status) }">
                          {{ order.statusLabel }}
                        </div>
                      </div>
                    </div>
                    <!-- 手绘波浪连接线 -->
                    <svg class="timeline-card__squiggle" viewBox="0 0 60 12" preserveAspectRatio="none">
                      <path d="M0 6 Q15 0, 30 6 T60 6" fill="none" :stroke="orderStatusColor(order.status)" stroke-width="2" stroke-linecap="round"/>
                    </svg>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══════════ 方案三：数据看板风 ═══════════ -->
        <template v-else-if="activeScheme === 'scheme-c'">
          <div class="scheme-page">
            <div class="page-header">
              <div class="page-title-wrap">
                <h2 class="page-title">订单驾驶舱</h2>
              </div>
            </div>

            <!-- 月度概览大卡片 -->
            <div class="doodle-frame overview-card" style="border-color: #3b82f6;">
              <div class="overview-card__inner">
                <div class="overview-card__header">
                  <span class="overview-card__title">📊 6月数据概览</span>
                </div>
                <div class="overview-card__stats">
                  <div class="overview-card__stat">
                    <div class="overview-card__stat-value">128</div>
                    <div class="overview-card__stat-label">总订单</div>
                  </div>
                  <div class="overview-card__stat">
                    <div class="overview-card__stat-value">￥12.8w</div>
                    <div class="overview-card__stat-label">总营收</div>
                  </div>
                  <div class="overview-card__stat">
                    <div class="overview-card__stat-value">￥3.2w</div>
                    <div class="overview-card__stat-label">总利润</div>
                  </div>
                </div>
                <!-- 状态分布进度条 -->
                <div class="overview-card__progress">
                  <div class="progress-row">
                    <span class="progress-label">待发货</span>
                    <div class="progress-bar">
                      <div class="progress-fill" style="width: 28%; background: linear-gradient(90deg, #f97316, #fb923c);"></div>
                    </div>
                    <span class="progress-value">28%</span>
                  </div>
                  <div class="progress-row">
                    <span class="progress-label">已发货</span>
                    <div class="progress-bar">
                      <div class="progress-fill" style="width: 45%; background: linear-gradient(90deg, #3b82f6, #60a5fa);"></div>
                    </div>
                    <span class="progress-value">45%</span>
                  </div>
                  <div class="progress-row">
                    <span class="progress-label">已完成</span>
                    <div class="progress-bar">
                      <div class="progress-fill" style="width: 20%; background: linear-gradient(90deg, #22c55e, #4ade80);"></div>
                    </div>
                    <span class="progress-value">20%</span>
                  </div>
                  <div class="progress-row">
                    <span class="progress-label">退款/取消</span>
                    <div class="progress-bar">
                      <div class="progress-fill" style="width: 7%; background: linear-gradient(90deg, #ef4444, #f87171);"></div>
                    </div>
                    <span class="progress-value">7%</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 快捷操作 -->
            <div class="quick-actions">
              <div class="doodle-frame quick-action-btn" style="border-color: #22c55e;">
                <div class="quick-action__inner">
                  <span class="quick-action__icon">📦</span>
                  <span class="quick-action__text">新建订单</span>
                </div>
              </div>
              <div class="doodle-frame quick-action-btn" style="border-color: #3b82f6;">
                <div class="quick-action__inner">
                  <span class="quick-action__icon">📥</span>
                  <span class="quick-action__text">导入订单</span>
                </div>
              </div>
              <div class="doodle-frame quick-action-btn" style="border-color: #f97316;">
                <div class="quick-action__inner">
                  <span class="quick-action__icon">🚚</span>
                  <span class="quick-action__text">物流查询</span>
                </div>
              </div>
              <div class="doodle-frame quick-action-btn" style="border-color: #8b5cf6;">
                <div class="quick-action__inner">
                  <span class="quick-action__icon">📊</span>
                  <span class="quick-action__text">导出报表</span>
                </div>
              </div>
            </div>

            <!-- 待处理事项 -->
            <div class="section">
              <div class="section-head">
                <svg viewBox="0 0 24 24" fill="#3b82f6" width="18" height="18">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
                </svg>
                <h3 class="section-title">待处理</h3>
                <span class="section-count">6项</span>
              </div>

              <div class="todo-list">
                <div v-for="todo in pendingTodos" :key="todo.id" class="todo-item">
                  <div class="todo-item__dot" :style="{ background: todo.priorityColor }"></div>
                  <div class="todo-item__content">
                    <div class="todo-item__title">{{ todo.title }}</div>
                    <div class="todo-item__meta">{{ todo.meta }}</div>
                  </div>
                  <span class="todo-item__action">处理 →</span>
                </div>
              </div>
            </div>

            <!-- 近期订单 -->
            <div class="section">
              <div class="section-head">
                <svg viewBox="0 0 24 24" fill="none" stroke="#3b82f6" stroke-width="2" width="18" height="18">
                  <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>
                </svg>
                <h3 class="section-title">近期订单</h3>
                <span class="section-count">最新5单</span>
              </div>

              <div class="recent-orders">
                <div v-for="order in recentOrders" :key="order.id" class="recent-order-row">
                  <div class="recent-order__no">#{{ order.orderNo }}</div>
                  <div class="recent-order__amount">￥{{ order.amount }}</div>
                  <span class="order-card__status" style="font-size: 11px;" :style="{ color: orderStatusColor(order.status) }">
                    {{ order.statusLabel }}
                  </span>
                </div>
              </div>
            </div>

            <!-- 手绘波浪装饰 -->
            <svg class="wave-deco" viewBox="0 0 375 20" preserveAspectRatio="none">
              <path d="M0 20 Q 50 5, 100 20 T 200 20 T 300 20 T 375 20" fill="none" stroke="#3b82f6" stroke-width="2" opacity="0.3"/>
              <path d="M0 15 Q 60 0, 120 15 T 240 15 T 375 15" fill="none" stroke="#3b82f6" stroke-width="1.5" opacity="0.2"/>
            </svg>
          </div>
        </template>

        <!-- ═══════════ 方案四：看板风 ═══════════ -->
        <template v-else-if="activeScheme === 'scheme-d'">
          <div class="scheme-page">
            <div class="page-header">
              <div class="page-title-wrap">
                <h2 class="page-title">订单看板</h2>
                <svg viewBox="0 0 24 24" fill="#3b82f6" width="20" height="20" style="margin-left: 6px;">
                  <path d="M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z"/>
                </svg>
              </div>
            </div>

            <!-- 看板列切换 -->
            <div class="kanban-tabs">
              <div
                v-for="col in kanbanColumns"
                :key="col.key"
                class="kanban-tab"
                :class="{ active: activeKanban === col.key }"
                @click="activeKanban = col.key"
              >
                <span class="kanban-tab__dot" :style="{ background: col.color }"></span>
                <span class="kanban-tab__label">{{ col.label }}</span>
                <span class="kanban-tab__count" :style="{ background: col.color }">{{ col.count }}</span>
              </div>
            </div>

            <!-- 看板内容 -->
            <div class="kanban-column" :style="{ '--col-color': currentKanban.color }">
              <div class="kanban-column__header">
                <div class="kanban-column__title">
                  {{ currentKanban.icon }} {{ currentKanban.label }}
                </div>
                <div class="kanban-column__sort">
                  <svg viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="2" width="16" height="16">
                    <path d="M3 6h18M6 12h12M10 18h4"/>
                  </svg>
                </div>
              </div>

              <div class="kanban-column__cards">
                <div
                  v-for="(order, idx) in currentKanbanOrders"
                  :key="order.id"
                  class="doodle-frame kanban-card"
                  :style="{ borderColor: orderStatusColor(order.status), '--delay': idx * 0.05 + 's' }"
                >
                  <div class="kanban-card__inner">
                    <div class="kanban-card__head">
                      <span class="kanban-card__no">#{{ order.orderNo }}</span>
                      <span class="kanban-card__shop">{{ order.shopName }}</span>
                    </div>
                    <div class="kanban-card__buyer">{{ order.buyerName }}</div>
                    <div class="kanban-card__items" v-if="order.itemsStr">{{ order.itemsStr }}</div>
                    <div class="kanban-card__footer">
                      <span class="kanban-card__amount">￥{{ order.amount }}</span>
                      <span class="kanban-card__time">{{ order.timeShort }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>

        <div class="mockup-footer">
          <div class="mockup-home-indicator"></div>
        </div>
      </div>

      <!-- 方案说明 -->
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

// ────────── 方案定义 ──────────
const schemes = [
  {
    id: 'scheme-a',
    name: '方案一：卡片列表风',
    description: '状态标签页 + 统计卡片 + 订单列表',
    features: [
      '顶部四色统计卡片（总订单/待处理/本月收入/退款中）',
      '手绘风格搜索框，支持订单号/买家搜索',
      '水平滚动状态标签（全部/待发货/已发货/已完成/退款）',
      '每个订单使用手绘边框卡片，按状态显示不同颜色',
      '订单卡片展示：编号、店铺、买家、金额、利润、时间、状态标签'
    ]
  },
  {
    id: 'scheme-b',
    name: '方案二：时间轴风',
    description: '时间分组排列 + 时间线样式',
    features: [
      '按时间分组排列（今天/昨天/本周/更早）',
      '彩色筛选标签（全部/待发货/已发货/已完成/退款）',
      '左侧时间线圆点装饰，视觉引导时间流向',
      '订单卡片左侧展示摘要信息，右侧展示金额和状态徽章',
      '每个订单卡片底部附带手绘波浪线装饰'
    ]
  },
  {
    id: 'scheme-c',
    name: '方案三：数据看板风',
    description: '数据驾驶舱 + 快捷操作 + 待处理事项',
    features: [
      '顶部大数据卡片展示月度概览（总订单/总营收/总利润）',
      '手绘风格进度条显示订单状态分布占比',
      '四个快捷操作按钮（新建/导入/物流/导出）',
      '待处理事项区域，带优先级颜色标记',
      '近期订单快速预览列表 + 手绘波浪装饰线'
    ]
  },
  {
    id: 'scheme-d',
    name: '方案四：看板风',
    description: '看板列切换 + 卡片瀑布流',
    features: [
      '顶部看板列标签切换（待发货/已发货/已完成/退款）',
      '每个标签显示对应数量徽章，颜色区分状态',
      '看板卡片列表，每张卡片包含订单编号、店铺、买家、商品、金额',
      '卡片带手绘不规则边框，延迟渐入动画效果',
      '简洁高效的信息密度，一屏展示更多订单'
    ]
  }
]

const activeScheme = ref('scheme-a')
const currentScheme = computed(() => schemes.find(s => s.id === activeScheme.value)!)

// ────────── 订单状态 ──────────
type OrderStatus = 'draft' | 'paid' | 'partial_shipped' | 'shipped' | 'partial_refund' | 'completed' | 'cancelled' | 'refunded'

const orderStatusConfig: Record<OrderStatus, { label: string; color: string }> = {
  draft:          { label: '草稿',   color: '#94a3b8' },
  paid:           { label: '待发货', color: '#f97316' },
  partial_shipped:{ label: '部分发货', color: '#3b82f6' },
  shipped:        { label: '已发货', color: '#3b82f6' },
  partial_refund: { label: '部分退款', color: '#f59e0b' },
  completed:      { label: '已完成', color: '#22c55e' },
  cancelled:      { label: '已取消', color: '#94a3b8' },
  refunded:       { label: '已退款', color: '#ef4444' },
}

function orderStatusColor(status: OrderStatus): string {
  return orderStatusConfig[status]?.color ?? '#94a3b8'
}

// ────────── 模拟订单数据 ──────────
const allOrders = [
  { id: 1,  orderNo: 'SO20260705001', shopName: '官方旗舰店', buyerName: '张三',          amount: 299,  profit: 89,   time: '07-05 14:23', timeShort: '14:23', status: 'paid' as OrderStatus, items: 2, itemsStr: '纯棉T恤 ×2' },
  { id: 2,  orderNo: 'SO20260705002', shopName: '潮流服饰店', buyerName: '李四',          amount: 599,  profit: 170,  time: '07-05 11:05', timeShort: '11:05', status: 'shipped' as OrderStatus, items: 1, itemsStr: '运动跑鞋 ×1' },
  { id: 3,  orderNo: 'SO20260705003', shopName: '官方旗舰店', buyerName: '王五',          amount: 129,  profit: 35,   time: '07-05 09:42', timeShort: '09:42', status: 'paid' as OrderStatus, items: 3, itemsStr: '牛仔裤 ×1, T恤 ×2' },
  { id: 4,  orderNo: 'SO20260704004', shopName: '户外专营店', buyerName: '赵六',          amount: 899,  profit: 260,  time: '07-04 16:18', timeShort: '昨天', status: 'completed' as OrderStatus, items: 1, itemsStr: '登山背包 ×1' },
  { id: 5,  orderNo: 'SO20260704005', shopName: '官方旗舰店', buyerName: '孙七',          amount: 199,  profit: -20,  time: '07-04 13:50', timeShort: '昨天', status: 'refunded' as OrderStatus, items: 1, itemsStr: '针织毛衣 ×1' },
  { id: 6,  orderNo: 'SO20260704006', shopName: '童装天地',  buyerName: '周八',          amount: 349,  profit: 95,   time: '07-04 10:30', timeShort: '昨天', status: 'shipped' as OrderStatus, items: 2, itemsStr: '儿童卫衣 ×1, 裤子 ×1' },
  { id: 7,  orderNo: 'SO20260703007', shopName: '潮流服饰店', buyerName: '吴九',          amount: 459,  profit: 130,  time: '07-03 15:20', timeShort: '07-03', status: 'shipped' as OrderStatus, items: 2, itemsStr: '帆布鞋 ×1, 帽子 ×1' },
  { id: 8,  orderNo: 'SO20260703008', shopName: '官方旗舰店', buyerName: '郑十',          amount: 699,  profit: 200,  time: '07-03 11:10', timeShort: '07-03', status: 'paid' as OrderStatus, items: 3, itemsStr: '外套 ×1, 长裤 ×2' },
  { id: 9,  orderNo: 'SO20260702009', shopName: '户外专营店', buyerName: '陈一',          amount: 159,  profit: -50,  time: '07-02 14:30', timeShort: '07-02', status: 'cancelled' as OrderStatus, items: 1, itemsStr: '水壶 ×1' },
  { id: 10, orderNo: 'SO20260702010', shopName: '官方旗舰店', buyerName: '林二',          amount: 899,  profit: 250,  time: '07-02 09:15', timeShort: '07-02', status: 'partial_refund' as OrderStatus, items: 2, itemsStr: '羽绒服 ×1' },
]

// ────────── 方案一：状态标签 ──────────
const statusTabs = [
  { key: 'all',   label: '全部', count: 128 },
  { key: 'paid',  label: '待发货', count: 18 },
  { key: 'shipped', label: '已发货', count: 42 },
  { key: 'completed', label: '已完成', count: 58 },
  { key: 'refunded',  label: '退款/售后', count: 10 },
]
const activeTab = ref('all')

const filteredOrders = computed(() => {
  if (activeTab.value === 'all') return allOrders
  if (activeTab.value === 'refunded') {
    return allOrders.filter(o => o.status === 'refunded' || o.status === 'partial_refund' || o.status === 'cancelled')
  }
  return allOrders.filter(o => o.status === activeTab.value)
})

// ────────── 方案二：时间线 ──────────
const activeFilter = ref('all')
const filterChips = [
  { key: 'all',     label: '全部',        color: '#3b82f6' },
  { key: 'paid',    label: '待发货',      color: '#f97316' },
  { key: 'shipped', label: '已发货',      color: '#3b82f6' },
  { key: 'completed', label: '已完成',    color: '#22c55e' },
  { key: 'refund',  label: '退款/取消',   color: '#ef4444' },
]

const timelineGroups = computed(() => {
  let orders = allOrders
  if (activeFilter.value === 'refund') {
    orders = allOrders.filter(o => ['refunded', 'partial_refund', 'cancelled'].includes(o.status))
  } else if (activeFilter.value !== 'all') {
    orders = allOrders.filter(o => o.status === activeFilter.value)
  }
  return [
    { label: '今天', orders: orders.filter(o => o.timeShort.includes(':') && !o.timeShort.includes('昨天')) },
    { label: '昨天', orders: orders.filter(o => o.timeShort === '昨天') },
    { label: '本周', orders: orders.filter(o => o.timeShort.startsWith('07-') && !o.timeShort.includes('昨天') && !o.timeShort.includes(':')) },
  ].filter(g => g.orders.length > 0)
})

// ────────── 方案三：待处理事项 ──────────
const pendingTodos = [
  { id: 1, title: 'SO20260705001 待发货', meta: '张三 · 纯棉T恤 ×2 · ￥299', priorityColor: '#f97316' },
  { id: 2, title: 'SO20260705003 待发货', meta: '王五 · 牛仔裤等3件 · ￥129', priorityColor: '#f97316' },
  { id: 3, title: 'SO20260705010 部分退款待处理', meta: '林二 · 羽绒服 ×1 · 退款￥150', priorityColor: '#ef4444' },
  { id: 4, title: 'SO20260704004 待确认收货', meta: '赵六 · 登山背包 ×1 · 已签收3天', priorityColor: '#3b82f6' },
  { id: 5, title: 'SO20260703008 缺货提醒', meta: '外套XL码库存不足，已催工厂', priorityColor: '#f59e0b' },
  { id: 6, title: '本月物流对账单待确认', meta: '6月物流费用 ￥1,280.00', priorityColor: '#8b5cf6' },
]

const recentOrders = computed(() => allOrders.slice(0, 5))

// ────────── 方案四：看板 ──────────
const activeKanban = ref('paid')
const kanbanColumns = [
  { key: 'paid',     label: '待发货',   icon: '📦', color: '#f97316', count: 18, statuses: ['paid'] as OrderStatus[] },
  { key: 'shipped',  label: '已发货',   icon: '🚚', color: '#3b82f6', count: 42, statuses: ['shipped', 'partial_shipped'] as OrderStatus[] },
  { key: 'completed',label: '已完成',  icon: '✅', color: '#22c55e', count: 58, statuses: ['completed'] as OrderStatus[] },
  { key: 'refund',   label: '退款/售后', icon: '🔙', color: '#ef4444', count: 10, statuses: ['refunded', 'partial_refund', 'cancelled'] as OrderStatus[] },
]
const currentKanban = computed(() => kanbanColumns.find(c => c.key === activeKanban.value)!)
const currentKanbanOrders = computed(() => allOrders.filter(o => currentKanban.value.statuses.includes(o.status)))
</script>

<style scoped lang="scss">
// ═══════════════════════════════════════
//  全局样式
// ═══════════════════════════════════════
.order-design-preview {
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

// ═══════════════════════════════════════
//  方案切换标签
// ═══════════════════════════════════════
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
  border: 2px solid #3b82f6;
  background: white;
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 15px;
  color: #3b82f6;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    transform: scale(1.02);
  }

  &.active {
    background: #3b82f6;
    color: white;
  }
}

// ═══════════════════════════════════════
//  手机壳框架
// ═══════════════════════════════════════
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

// ═══════════════════════════════════════
//  页面内容
// ═══════════════════════════════════════
.scheme-page {
  padding: 16px;
  background: #faf8f5;
  height: calc(100% - 78px);
  overflow-y: auto;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
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
  flex-shrink: 0;
}

// ═══════════════════════════════════════
//  通用手绘边框（内联SVG等效）
// ═══════════════════════════════════════
.doodle-frame {
  position: relative;
  background: white;
  border: 2.5px solid;
  border-radius: 16px;

  &.pill {
    border-radius: 999px;
  }
}

// ═══════════════════════════════════════
//  搜索框
// ═══════════════════════════════════════
.search-inner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;

  input {
    flex: 1;
    border: none;
    outline: none;
    background: transparent;
    font-family: 'ZCOOL KuaiLe', sans-serif;
    font-size: 14px;
    color: #1e293b;

    &::placeholder {
      color: #94a3b8;
    }
  }
}

// ═══════════════════════════════════════
//  方案一：统计卡片行
// ═══════════════════════════════════════
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
  margin-bottom: 14px;

  .doodle-frame {
    border-radius: 12px;
  }
}

.stat-card__body {
  padding: 10px 4px;
  text-align: center;
}

.stat-card__value {
  font-size: 18px;
  font-weight: 800;
  color: #1e293b;
  line-height: 1.2;
}

.stat-card__label {
  font-size: 10px;
  color: #64748b;
  margin-top: 2px;
}

// ═══════════════════════════════════════
//  方案一：状态标签
// ═══════════════════════════════════════
.status-tabs {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 12px;
  margin-bottom: 12px;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.status-tab {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 14px;
  background: white;
  border: 2px solid #e2e8f0;
  border-radius: 999px;
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 13px;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;

  &:hover {
    border-color: #3b82f6;
    color: #3b82f6;
  }

  &.active {
    border-color: #3b82f6;
    background: #eff6ff;
    color: #3b82f6;
  }
}

.status-tab__badge {
  font-size: 10px;
  font-weight: 700;
  color: white;
  background: #3b82f6;
  padding: 1px 6px;
  border-radius: 999px;
  min-width: 18px;
  text-align: center;
}

// ═══════════════════════════════════════
//  方案一：订单卡片
// ═══════════════════════════════════════
.order-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.order-card {
  cursor: pointer;
  transition: transform 0.2s;

  &:hover {
    transform: scale(0.98);
  }
}

.order-card__inner {
  padding: 12px 14px;
}

.order-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.order-card__no {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 14px;
  color: #1e293b;
  font-weight: 700;
}

.order-card__shop {
  font-size: 11px;
  color: #94a3b8;
  background: #f1f5f9;
  padding: 1px 8px;
  border-radius: 4px;
}

.order-card__body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.order-card__buyer {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #475569;
}

.order-card__amount {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #9b0000;
}

.order-card__price {
  font-size: 16px;
  font-weight: 800;
  color: #1e293b;
}

.order-card__profit {
  font-size: 11px;
  font-weight: 700;
  color: #22c55e;
}

.order-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.order-card__time {
  font-size: 11px;
  color: #94a3b8;
}

.order-card__status {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 12px;
  font-weight: 700;
}

.load-more {
  padding: 20px 0 40px;
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
}

// ═══════════════════════════════════════
//  方案二：筛选标签
// ═══════════════════════════════════════
.filter-chips {
  display: flex;
  gap: 6px;
  margin-bottom: 16px;
  overflow-x: auto;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.filter-chip {
  flex-shrink: 0;
  padding: 6px 14px;
  border-radius: 999px;
  border: 2px solid var(--chip-color);
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 13px;
  color: var(--chip-color);
  background: white;
  cursor: pointer;
  transition: all 0.2s;

  &.active {
    background: var(--chip-color);
    color: white;
  }
}

// ═══════════════════════════════════════
//  方案二：时间线
// ═══════════════════════════════════════
.timeline {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.timeline-group__header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  position: relative;
}

.timeline-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #3b82f6;
  border: 3px solid #bfdbfe;
  flex-shrink: 0;
}

.timeline-group__label {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 16px;
  color: #1e293b;
}

.timeline-group__count {
  font-size: 12px;
  color: #94a3b8;
  margin-left: auto;
}

.timeline-group__orders {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-left: 22px;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    left: -6px;
    top: 0;
    bottom: 0;
    width: 2px;
    background: repeating-linear-gradient(
      to bottom,
      #bfdbfe 0px,
      #bfdbfe 4px,
      transparent 4px,
      transparent 8px
    );
  }
}

.timeline-card {
  position: relative;
  cursor: pointer;
  transition: transform 0.2s;

  &:hover {
    transform: scale(0.98);
  }
}

.timeline-card__inner {
  display: flex;
  padding: 12px 14px;
  gap: 12px;
}

.timeline-card__left {
  flex: 1;
  min-width: 0;
}

.timeline-card__no {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 14px;
  color: #1e293b;
  margin-bottom: 2px;
}

.timeline-card__buyer {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 4px;
}

.timeline-card__meta {
  display: flex;
  gap: 12px;
  font-size: 11px;
  color: #94a3b8;
}

.timeline-card__right {
  text-align: right;
  flex-shrink: 0;
}

.timeline-card__amount {
  font-size: 16px;
  font-weight: 800;
  color: #1e293b;
  margin-bottom: 4px;
}

.timeline-card__status {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 999px;
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 11px;
  color: white;
  font-weight: 700;
}

.timeline-card__squiggle {
  width: calc(100% - 20px);
  height: 10px;
  margin: 0 auto 4px;
  display: block;
}

// ═══════════════════════════════════════
//  方案三：概览卡片
// ═══════════════════════════════════════
.overview-card {
  margin-bottom: 16px;
}

.overview-card__inner {
  padding: 16px;
}

.overview-card__header {
  margin-bottom: 16px;
}

.overview-card__title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 18px;
  color: #1e293b;
}

.overview-card__stats {
  display: flex;
  justify-content: space-around;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 2px dashed #e2e8f0;
}

.overview-card__stat {
  text-align: center;
}

.overview-card__stat-value {
  font-size: 22px;
  font-weight: 800;
  color: #1e293b;
}

.overview-card__stat-label {
  font-size: 11px;
  color: #64748b;
  margin-top: 2px;
}

.overview-card__progress {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.progress-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.progress-label {
  font-size: 12px;
  color: #64748b;
  width: 56px;
  flex-shrink: 0;
}

.progress-bar {
  flex: 1;
  height: 12px;
  background: #e2e8f0;
  border-radius: 999px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.3s;
}

.progress-value {
  font-size: 12px;
  font-weight: 700;
  color: #1e293b;
  width: 36px;
  text-align: right;
}

// ═══════════════════════════════════════
//  方案三：快捷操作
// ═══════════════════════════════════════
.quick-actions {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-bottom: 20px;
}

.quick-action-btn {
  cursor: pointer;
  transition: transform 0.2s;

  &:hover {
    transform: scale(0.95);
  }
}

.quick-action__inner {
  padding: 12px 4px;
  text-align: center;
}

.quick-action__icon {
  font-size: 24px;
  display: block;
  margin-bottom: 4px;
}

.quick-action__text {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 12px;
  color: #1e293b;
  display: block;
}

// ═══════════════════════════════════════
//  方案三：待处理事项
// ═══════════════════════════════════════
.section {
  margin-bottom: 20px;
}

.section-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.section-title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 18px;
  color: #1e293b;
  margin: 0;
  flex: 1;
}

.section-count {
  font-size: 12px;
  color: #94a3b8;
}

.todo-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.todo-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  background: white;
  border-radius: 12px;
  border: 2px solid #e2e8f0;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: #3b82f6;
    transform: scale(0.98);
  }
}

.todo-item__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.todo-item__content {
  flex: 1;
  min-width: 0;
}

.todo-item__title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 14px;
  color: #1e293b;
}

.todo-item__meta {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
}

.todo-item__action {
  font-size: 12px;
  color: #3b82f6;
  font-weight: 700;
  flex-shrink: 0;
}

.recent-orders {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.recent-order-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: white;
  border-radius: 10px;
  border: 2px solid #e2e8f0;
}

.recent-order__no {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 13px;
  color: #1e293b;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-order__amount {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  flex-shrink: 0;
}

.wave-deco {
  width: 100%;
  height: 16px;
  margin: 8px 0 40px;
  display: block;
}

// ═══════════════════════════════════════
//  方案四：看板
// ═══════════════════════════════════════
.kanban-tabs {
  display: flex;
  gap: 6px;
  margin-bottom: 16px;
  overflow-x: auto;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.kanban-tab {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: white;
  border: 2px solid #e2e8f0;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: #3b82f6;
  }

  &.active {
    border-color: #3b82f6;
    background: #eff6ff;
  }
}

.kanban-tab__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.kanban-tab__label {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 13px;
  color: #1e293b;
}

.kanban-tab__count {
  font-size: 10px;
  font-weight: 700;
  color: white;
  padding: 1px 6px;
  border-radius: 999px;
  min-width: 18px;
  text-align: center;
}

.kanban-column__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.kanban-column__title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 18px;
  color: #1e293b;
}

.kanban-column__sort {
  cursor: pointer;
  padding: 4px;
}

.kanban-column__cards {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.kanban-card {
  cursor: pointer;
  transition: all 0.3s ease;
  animation: cardSlideIn 0.4s ease backwards;
  animation-delay: var(--delay, 0s);

  &:hover {
    transform: translateX(-4px);
  }
}

@keyframes cardSlideIn {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.kanban-card__inner {
  padding: 12px 14px;
}

.kanban-card__head {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}

.kanban-card__no {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 14px;
  color: #1e293b;
  font-weight: 700;
}

.kanban-card__shop {
  font-size: 10px;
  color: #94a3b8;
  background: #f1f5f9;
  padding: 1px 6px;
  border-radius: 4px;
}

.kanban-card__buyer {
  font-size: 13px;
  color: #475569;
  margin-bottom: 4px;
}

.kanban-card__items {
  font-size: 11px;
  color: #94a3b8;
  margin-bottom: 6px;
}

.kanban-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.kanban-card__amount {
  font-size: 16px;
  font-weight: 800;
  color: #1e293b;
}

.kanban-card__time {
  font-size: 11px;
  color: #94a3b8;
}

// ═══════════════════════════════════════
//  方案信息面板
// ═══════════════════════════════════════
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
  border-left: 4px solid #3b82f6;
  font-size: 14px;
  color: #475569;
}
</style>
