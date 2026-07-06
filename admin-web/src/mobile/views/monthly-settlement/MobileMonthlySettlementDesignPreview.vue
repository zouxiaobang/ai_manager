<template>
  <div class="settlement-design-preview">
    <div class="preview-header">
      <h1 class="preview-title">月结统计界面设计</h1>
      <p class="preview-subtitle">手绘风格手机端月结统计方案</p>
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
          <div class="mockup-notch" />
        </div>

        <template v-if="activeScheme === 'scheme-a'">
          <div class="scheme-page scheme-a">
            <!-- 顶部标题 -->
            <div class="page-header">
              <div class="page-title-wrap">
                <img :src="assets.starYellow" class="title-star" alt="" />
                <h2 class="page-title">📊 月结统计</h2>
                <img :src="assets.starBlue" class="title-star" alt="" />
              </div>
            </div>

            <!-- 月份选择器 -->
            <SchemeADoodleFrame shape="pill" color="#2563eb" class="month-picker">
              <div class="month-picker__inner">
                <span class="month-picker__icon">📅</span>
                <span class="month-picker__text">2026 年 6 月</span>
                <span class="month-picker__arrow">▼</span>
              </div>
            </SchemeADoodleFrame>

            <!-- 汇总数据卡片 -->
            <div class="summary-grid">
              <SchemeADoodleFrame color="#2563eb" class="summary-card">
                <div class="summary-card__body">
                  <div class="summary-card__label">营业额</div>
                  <div class="summary-card__value">¥1,203,000</div>
                </div>
              </SchemeADoodleFrame>
              <SchemeADoodleFrame color="#22c55e" class="summary-card">
                <div class="summary-card__body">
                  <div class="summary-card__label">预估利润</div>
                  <div class="summary-card__value">¥358,000</div>
                </div>
              </SchemeADoodleFrame>
              <SchemeADoodleFrame color="#f59e0b" class="summary-card">
                <div class="summary-card__body">
                  <div class="summary-card__label">实际利润</div>
                  <div class="summary-card__value">¥321,000</div>
                </div>
              </SchemeADoodleFrame>
              <SchemeADoodleFrame color="#ef4444" class="summary-card">
                <div class="summary-card__body">
                  <div class="summary-card__label">订单总数</div>
                  <div class="summary-card__value">873 单</div>
                </div>
              </SchemeADoodleFrame>
            </div>

            <!-- 利润趋势 -->
            <div class="profit-comparison">
              <div class="profit-bar">
                <div class="profit-bar__label">预估利润</div>
                <div class="profit-bar__track">
                  <div class="profit-bar__fill profit-bar__fill--est" style="width: 72%" />
                </div>
                <div class="profit-bar__value">¥35.8万</div>
              </div>
              <div class="profit-bar">
                <div class="profit-bar__label">实际利润</div>
                <div class="profit-bar__track">
                  <div class="profit-bar__fill profit-bar__fill--actual" style="width: 64%" />
                </div>
                <div class="profit-bar__value">¥32.1万</div>
              </div>
            </div>

            <!-- 店铺列表 -->
            <div class="section">
              <div class="section-head">
                <img :src="assets.starBlueOutline" class="section-icon" alt="" />
                <h3 class="section-title">各店汇总</h3>
                <span class="section-count">5 家店铺</span>
              </div>

              <div class="shop-list">
                <SchemeADoodleFrame
                  v-for="shop in shops"
                  :key="shop.id"
                  class="shop-card"
                  shape="rect"
                  :color="shop.color"
                  @click="handleCardClick"
                >
                  <div class="shop-card__inner">
                    <div class="shop-card__head">
                      <div class="shop-card__platform" :style="{ background: shop.platformBg }">
                        {{ shop.platform }}
                      </div>
                      <div class="shop-card__name">{{ shop.name }}</div>
                      <span class="shop-card__profit" :class="shop.profit >= 0 ? 'is-up' : 'is-down'">
                        {{ shop.profit >= 0 ? '↑' : '↓' }}{{ Math.abs(shop.profitRate) }}%
                      </span>
                    </div>
                    <div class="shop-card__metrics">
                      <div class="shop-card__metric">
                        <span class="shop-card__metric-label">营收</span>
                        <span class="shop-card__metric-value">{{ formatMoney(shop.revenue) }}</span>
                      </div>
                      <div class="shop-card__metric">
                        <span class="shop-card__metric-label">利润</span>
                        <span class="shop-card__metric-value is-profit">{{ formatMoney(shop.profit) }}</span>
                      </div>
                      <div class="shop-card__metric">
                        <span class="shop-card__metric-label">订单</span>
                        <span class="shop-card__metric-value">
                          {{ shop.included }}/{{ shop.pending }}/{{ shop.excluded }}
                        </span>
                      </div>
                    </div>
                    <div class="shop-card__express">
                      <span class="shop-card__express-dot" :class="shop.expressImported ? 'is-done' : 'is-pending'" />
                      <span class="shop-card__express-text">
                        {{ shop.expressImported ? '快递账单已导入' : '快递账单待导入' }}
                      </span>
                    </div>
                  </div>
                </SchemeADoodleFrame>
              </div>
            </div>

            <!-- 计算按钮 -->
            <SchemeADoodleFrame tag="button" shape="pill" color="#22c55e" class="calc-btn" @click="handleActionClick">
              <span class="calc-btn__text">🧮 开始月结统计</span>
            </SchemeADoodleFrame>
          </div>
        </template>

        <template v-else-if="activeScheme === 'scheme-b'">
          <div class="scheme-page scheme-b">
            <!-- 标题 -->
            <div class="page-header">
              <h2 class="page-title">📊 月结统计</h2>
              <span class="page-period">2026 年 6 月</span>
            </div>

            <!-- 店铺标签 -->
            <div class="shop-tabs">
              <div
                v-for="shop in shops"
                :key="shop.id"
                class="shop-tab"
                :class="{ active: activeShop === shop.id }"
                :style="{ '--tab-color': shop.color }"
                @click="activeShop = shop.id"
              >
                <span class="shop-tab__name">{{ shop.name }}</span>
                <span class="shop-tab__badge" v-if="shop.pending > 0">{{ shop.pending }}</span>
              </div>
            </div>

            <!-- 当前店铺详情 -->
            <template v-for="shop in shops" :key="shop.id">
              <div v-if="activeShop === shop.id" class="shop-detail-section">
                <!-- 店铺概况卡 -->
                <SchemeADoodleFrame :color="shop.color" class="shop-overview-card">
                  <div class="shop-overview-card__inner">
                    <div class="shop-overview-card__platform" :style="{ background: shop.platformBg }">
                      {{ shop.platform }}
                    </div>
                    <div class="shop-overview-card__name">{{ shop.name }}</div>
                    <div class="shop-overview-card__stats">
                      <div class="shop-overview-card__stat">
                        <div class="shop-overview-card__stat-label">营业额</div>
                        <div class="shop-overview-card__stat-value">{{ formatMoney(shop.revenue) }}</div>
                      </div>
                      <div class="shop-overview-card__stat">
                        <div class="shop-overview-card__stat-label">利润</div>
                        <div class="shop-overview-card__stat-value is-profit">{{ formatMoney(shop.profit) }}</div>
                      </div>
                      <div class="shop-overview-card__stat">
                        <div class="shop-overview-card__stat-label">利润率</div>
                        <div class="shop-overview-card__stat-value">{{ shop.profitRate }}%</div>
                      </div>
                    </div>
                  </div>
                </SchemeADoodleFrame>

                <!-- 成本对比 -->
                <div class="cost-comparison">
                  <SchemeADoodleFrame color="#94a3b8" class="cost-card">
                    <div class="cost-card__inner">
                      <div class="cost-card__label">预估成本</div>
                      <div class="cost-card__value">{{ formatMoney(shop.estCost) }}</div>
                    </div>
                  </SchemeADoodleFrame>
                  <SchemeADoodleFrame color="#ef4444" class="cost-card">
                    <div class="cost-card__inner">
                      <div class="cost-card__label">实际成本</div>
                      <div class="cost-card__value">{{ formatMoney(shop.actualCost) }}</div>
                    </div>
                  </SchemeADoodleFrame>
                </div>

                <!-- 订单状态 -->
                <div class="order-status">
                  <div class="order-status__head">
                    <img :src="assets.starBlueOutline" class="section-icon" alt="" />
                    <h3 class="section-title">订单状态</h3>
                  </div>
                  <div class="order-status__bars">
                    <div class="order-status__bar">
                      <span class="order-status__bar-label">已纳入</span>
                      <div class="order-status__bar-track">
                        <div class="order-status__bar-fill order-status__bar-fill--included" :style="{ width: orderBarPercent(shop, 'included') + '%' }" />
                      </div>
                      <span class="order-status__bar-count">{{ shop.included }}</span>
                    </div>
                    <div class="order-status__bar">
                      <span class="order-status__bar-label">待确认</span>
                      <div class="order-status__bar-track">
                        <div class="order-status__bar-fill order-status__bar-fill--pending" :style="{ width: orderBarPercent(shop, 'pending') + '%' }" />
                      </div>
                      <span class="order-status__bar-count">{{ shop.pending }}</span>
                    </div>
                    <div class="order-status__bar">
                      <span class="order-status__bar-label">已排除</span>
                      <div class="order-status__bar-track">
                        <div class="order-status__bar-fill order-status__bar-fill--excluded" :style="{ width: orderBarPercent(shop, 'excluded') + '%' }" />
                      </div>
                      <span class="order-status__bar-count">{{ shop.excluded }}</span>
                    </div>
                  </div>
                </div>

                <!-- 最高利润订单 -->
                <SchemeADoodleFrame color="#f59e0b" class="max-profit-card" @click="handleCardClick">
                  <div class="max-profit-card__inner">
                    <div class="max-profit-card__crown">👑</div>
                    <div class="max-profit-card__content">
                      <div class="max-profit-card__label">最高利润单</div>
                      <div class="max-profit-card__order">#DD20260628001</div>
                      <div class="max-profit-card__detail">纯棉T恤 · 白色/L · 利润 ¥1,280</div>
                    </div>
                  </div>
                </SchemeADoodleFrame>

                <!-- 操作按钮 -->
                <div class="action-row">
                  <SchemeADoodleFrame tag="button" shape="pill" color="#2563eb" class="action-btn" @click="handleActionClick">
                    <span>📋 待确认订单</span>
                  </SchemeADoodleFrame>
                  <SchemeADoodleFrame tag="button" shape="pill" color="#22c55e" class="action-btn" @click="handleActionClick">
                    <span>🔄 重新统计</span>
                  </SchemeADoodleFrame>
                </div>
              </div>
            </template>

            <img :src="assets.squiggleBlue" class="wave-divider" alt="" />
          </div>
        </template>

        <template v-else-if="activeScheme === 'scheme-c'">
          <div class="scheme-page scheme-c">
            <!-- 标题 -->
            <div class="page-header">
              <div class="page-title-wrap">
                <img :src="assets.starYellow" class="title-star" alt="" />
                <h2 class="page-title">📊 月结统计</h2>
                <img :src="assets.starBlue" class="title-star" alt="" />
              </div>
            </div>

            <!-- 进度概览 -->
            <div class="progress-overview">
              <SchemeADoodleFrame color="#2563eb" class="progress-card">
                <div class="progress-card__inner">
                  <div class="progress-card__header">月结准备进度</div>
                  <div class="progress-card__percent">
                    <span class="progress-card__percent-value">75%</span>
                    <span class="progress-card__percent-label">已完成</span>
                  </div>
                  <div class="progress-card__bar">
                    <div class="progress-card__bar-fill" style="width: 75%" />
                  </div>
                  <div class="progress-card__steps">
                    <span class="progress-card__step is-done">①</span>
                    <span class="progress-card__step is-done">②</span>
                    <span class="progress-card__step is-active">③</span>
                    <span class="progress-card__step">④</span>
                  </div>
                </div>
              </SchemeADoodleFrame>
            </div>

            <!-- 准备清单 -->
            <div class="checklist">
              <div class="checklist-head">
                <img :src="assets.starBlueOutline" class="section-icon" alt="" />
                <h3 class="section-title">月结工作台</h3>
              </div>

              <div class="checklist-items">
                <!-- 销售订单 -->
                <div class="checklist-item is-done" @click="handleCardClick">
                  <div class="checklist-item__dot" />
                  <div class="checklist-item__body">
                    <div class="checklist-item__title-row">
                      <div class="checklist-item__title">销售订单导入</div>
                      <span class="checklist-item__tag is-success">已完成</span>
                    </div>
                    <div class="checklist-item__desc">已导入 5 家店铺 · 873 笔订单</div>
                  </div>
                  <span class="checklist-item__arrow">→</span>
                </div>

                <!-- 快递账单 -->
                <div class="checklist-item is-warning" @click="handleCardClick">
                  <div class="checklist-item__dot" />
                  <div class="checklist-item__body">
                    <div class="checklist-item__title-row">
                      <div class="checklist-item__title">快递账单导入</div>
                      <span class="checklist-item__tag is-warning">部分导入</span>
                    </div>
                    <div class="checklist-item__desc">圆通 156/168 匹配 · 中通 89/92 匹配</div>
                  </div>
                  <span class="checklist-item__arrow">→</span>
                </div>

                <!-- 买家排除 -->
                <div class="checklist-item is-done" @click="handleCardClick">
                  <div class="checklist-item__dot" />
                  <div class="checklist-item__body">
                    <div class="checklist-item__title-row">
                      <div class="checklist-item__title">买家排除配置</div>
                      <span class="checklist-item__tag is-success">已配置</span>
                    </div>
                    <div class="checklist-item__desc">已排除 12 个买家</div>
                  </div>
                  <span class="checklist-item__arrow">→</span>
                </div>

                <!-- 待确认订单 -->
                <div class="checklist-item is-pending" @click="handleCardClick">
                  <div class="checklist-item__dot" />
                  <div class="checklist-item__body">
                    <div class="checklist-item__title-row">
                      <div class="checklist-item__title">待确认订单</div>
                      <span class="checklist-item__tag is-pending">待处理</span>
                    </div>
                    <div class="checklist-item__desc">16 笔订单需人工决策</div>
                  </div>
                  <span class="checklist-item__arrow">→</span>
                </div>
              </div>
            </div>

            <!-- 快递账单匹配详情 -->
            <div class="express-section">
              <div class="express-section__head">
                <img :src="assets.starBlueOutline" class="section-icon" alt="" />
                <h3 class="section-title">快递匹配</h3>
              </div>
              <div class="express-cards">
                <SchemeADoodleFrame color="#22c55e" class="express-card">
                  <div class="express-card__inner">
                    <div class="express-card__icon">📦</div>
                    <div class="express-card__body">
                      <div class="express-card__name">圆通速递</div>
                      <div class="express-card__match">
                        <span class="express-card__match-num">156</span>/168 匹配
                      </div>
                    </div>
                    <div class="express-card__status is-done">✓</div>
                  </div>
                </SchemeADoodleFrame>
                <SchemeADoodleFrame color="#22c55e" class="express-card">
                  <div class="express-card__inner">
                    <div class="express-card__icon">📦</div>
                    <div class="express-card__body">
                      <div class="express-card__name">中通快递</div>
                      <div class="express-card__match">
                        <span class="express-card__match-num">89</span>/92 匹配
                      </div>
                    </div>
                    <div class="express-card__status is-done">✓</div>
                  </div>
                </SchemeADoodleFrame>
                <SchemeADoodleFrame color="#f59e0b" class="express-card">
                  <div class="express-card__inner">
                    <div class="express-card__icon">📦</div>
                    <div class="express-card__body">
                      <div class="express-card__name">韵达快递</div>
                      <div class="express-card__match">
                        <span class="express-card__match-num">12</span>/15 匹配
                      </div>
                    </div>
                    <div class="express-card__status is-warning">!</div>
                  </div>
                </SchemeADoodleFrame>
              </div>
            </div>

            <!-- 计算按钮 -->
            <SchemeADoodleFrame tag="button" shape="pill" color="#22c55e" class="calc-btn" @click="handleActionClick">
              <span class="calc-btn__text">🧮 开始月结统计</span>
            </SchemeADoodleFrame>
          </div>
        </template>

        <template v-else-if="activeScheme === 'scheme-d'">
          <div class="scheme-page scheme-d">
            <!-- 标题 -->
            <div class="page-header">
              <h2 class="page-title">📊 月结统计</h2>
              <div class="page-header__period">
                <span class="page-period">2026 年 6 月</span>
                <span class="page-period-badge">已统计</span>
              </div>
            </div>

            <!-- 顶部汇总 -->
            <div class="top-summary">
              <div class="top-summary__row">
                <div class="top-summary__item top-summary__item--revenue">
                  <div class="top-summary__label">总营业额</div>
                  <div class="top-summary__value">¥1,203,000</div>
                </div>
                <div class="top-summary__item top-summary__item--profit">
                  <div class="top-summary__label">实际利润</div>
                  <div class="top-summary__value">¥321,000</div>
                </div>
              </div>
              <div class="top-summary__row">
                <div class="top-summary__item">
                  <div class="top-summary__label">总成本</div>
                  <div class="top-summary__value">¥882,000</div>
                </div>
                <div class="top-summary__item">
                  <div class="top-summary__label">订单总数</div>
                  <div class="top-summary__value">
                    <span class="top-summary__num is-included">873</span>
                    <span class="top-summary__sep">/</span>
                    <span class="top-summary__num is-pending">16</span>
                    <span class="top-summary__sep">/</span>
                    <span class="top-summary__num is-excluded">47</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 利润排名 -->
            <div class="section">
              <div class="section-head">
                <img :src="assets.starBlueOutline" class="section-icon" alt="" />
                <h3 class="section-title">利润排行</h3>
              </div>

              <div class="rank-list">
                <div
                  v-for="(shop, index) in rankedShops"
                  :key="shop.id"
                  class="rank-item"
                  :class="{ 'is-top': index < 3 }"
                  @click="handleCardClick"
                >
                  <div class="rank-item__medal" v-if="index < 3">
                    <span class="medal-icon">{{ ['🥇', '🥈', '🥉'][index] }}</span>
                  </div>
                  <div class="rank-item__num" v-else>{{ index + 1 }}</div>
                  <div class="rank-item__info">
                    <div class="rank-item__name-row">
                      <span class="rank-item__name">{{ shop.name }}</span>
                      <span class="rank-item__platform" :style="{ background: shop.platformBg }">{{ shop.platform }}</span>
                    </div>
                    <div class="rank-item__metrics">
                      <span class="rank-item__revenue">{{ formatMoney(shop.revenue) }}</span>
                      <span class="rank-item__profit is-profit">{{ formatMoney(shop.profit) }}</span>
                    </div>
                  </div>
                  <div class="rank-item__profit-bar">
                    <div
                      class="rank-item__profit-fill"
                      :style="{ width: rankProfitPercent(shop) + '%', background: shop.color }"
                    />
                  </div>
                </div>
              </div>
            </div>

            <!-- 平台分布 -->
            <div class="section">
              <div class="section-head">
                <img :src="assets.starBlueOutline" class="section-icon" alt="" />
                <h3 class="section-title">平台分布</h3>
              </div>

              <SchemeADoodleFrame color="#2563eb" class="platform-dist-card">
                <div class="platform-dist-card__inner">
                  <div class="platform-dist__rows">
                    <div class="platform-dist__row">
                      <span class="platform-dist__label">
                        <span class="platform-dot" style="background: #ff6a00" />
                        淘宝
                      </span>
                      <div class="platform-dist__bar">
                        <div class="platform-dist__fill" style="width: 38%; background: #ff6a00" />
                      </div>
                      <span class="platform-dist__value">¥37.2万</span>
                    </div>
                    <div class="platform-dist__row">
                      <span class="platform-dist__label">
                        <span class="platform-dot" style="background: #e1251b" />
                        京东
                      </span>
                      <div class="platform-dist__bar">
                        <div class="platform-dist__fill" style="width: 31%; background: #e1251b" />
                      </div>
                      <span class="platform-dist__value">¥37.3万</span>
                    </div>
                    <div class="platform-dist__row">
                      <span class="platform-dist__label">
                        <span class="platform-dot" style="background: #00b96b" />
                        抖音
                      </span>
                      <div class="platform-dist__bar">
                        <div class="platform-dist__fill" style="width: 38%; background: #00b96b" />
                      </div>
                      <span class="platform-dist__value">¥45.8万</span>
                    </div>
                    <div class="platform-dist__row">
                      <span class="platform-dist__label">
                        <span class="platform-dot" style="background: #ff2d1b" />
                        拼多多
                      </span>
                      <div class="platform-dist__bar">
                        <div class="platform-dist__fill" style="width: 7%; background: #ff2d1b" />
                      </div>
                      <span class="platform-dist__value">¥8.7万</span>
                    </div>
                  </div>
                </div>
              </SchemeADoodleFrame>
            </div>

            <!-- 快捷操作 -->
            <div class="quick-actions">
              <SchemeADoodleFrame tag="button" color="#2563eb" class="quick-action-btn" @click="handleActionClick">
                <span class="quick-action-icon">📥</span>
                <span class="quick-action-text">导入账单</span>
              </SchemeADoodleFrame>
              <SchemeADoodleFrame tag="button" color="#f59e0b" class="quick-action-btn" @click="handleActionClick">
                <span class="quick-action-icon">📋</span>
                <span class="quick-action-text">待确认</span>
              </SchemeADoodleFrame>
              <SchemeADoodleFrame tag="button" color="#22c55e" class="quick-action-btn" @click="handleActionClick">
                <span class="quick-action-icon">📊</span>
                <span class="quick-action-text">重新统计</span>
              </SchemeADoodleFrame>
              <SchemeADoodleFrame tag="button" color="#8b5cf6" class="quick-action-btn" @click="handleActionClick">
                <span class="quick-action-icon">⚙️</span>
                <span class="quick-action-text">排除配置</span>
              </SchemeADoodleFrame>
            </div>

            <img :src="assets.squiggleBlue" class="wave-divider" alt="" />
          </div>
        </template>

        <div class="mockup-footer">
          <div class="mockup-home-indicator" />
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
    name: '方案一：数据仪表风',
    description: '汇总卡片 + 店铺列表 + 利润对比',
    features: [
      '顶部四格数据卡片（营业额/预估利润/实际利润/订单数）',
      '预估利润与实际利润对比进度条',
      '每家店铺使用单独卡片展示核心指标',
      '快递账单导入状态醒目提示',
      '底部使用绿色胶囊按钮开始月结统计'
    ]
  },
  {
    id: 'scheme-b',
    name: '方案二：店铺标签风',
    description: '店铺标签切换 + 深度详情',
    features: [
      '顶部店铺标签导航，支持横向滚动',
      '当前选中店铺高亮显示，带待处理数量徽章',
      '店铺概况卡包含营业额、利润、利润率',
      '预估成本与实际成本对比卡片',
      '订单状态分布条形图（纳入/待确认/排除）',
      '最高利润订单突出展示',
      '底部快捷操作按钮'
    ]
  },
  {
    id: 'scheme-c',
    name: '方案三：流程清单风',
    description: '工作台清单 + 步骤引导 + 快递匹配',
    features: [
      '顶部进度卡片展示月结准备总体进度',
      '工作台清单展示4个流程节点（订单导入/快递账单/买家排除/待确认）',
      '每个节点显示状态标签和简要统计',
      '快递账单匹配详情展示各站点匹配率',
      '引导式操作，适合逐步完成月结'
    ]
  },
  {
    id: 'scheme-d',
    name: '方案四：排行分布风',
    description: '利润排行 + 平台分布 + 全局概览',
    features: [
      '顶部全局汇总（营业额/利润/成本/订单分类统计）',
      '利润排行榜，前三名金银铜牌展示',
      '各店铺利润条对比，直观显示贡献度',
      '平台分布水平条形图（淘宝/京东/抖音/拼多多）',
      '底部四个快捷操作按钮'
    ]
  }
]

const activeScheme = ref('scheme-a')
const activeShop = ref(1)

const currentScheme = computed(() => schemes.find(s => s.id === activeScheme.value)!)

interface Shop {
  id: number
  name: string
  platform: string
  color: string
  platformBg: string
  revenue: number
  estCost: number
  actualCost: number
  profit: number
  profitRate: number
  included: number
  excluded: number
  pending: number
  expressImported: boolean
}

const shops: Shop[] = [
  { id: 1, name: '旗舰店', platform: '淘宝', color: '#2563eb', platformBg: '#ff6a00', revenue: 285000, estCost: 198000, actualCost: 205000, profit: 80000, profitRate: 28.1, included: 145, excluded: 8, pending: 3, expressImported: true },
  { id: 2, name: '品牌专营店', platform: '京东', color: '#e1251b', platformBg: '#e1251b', revenue: 152000, estCost: 110000, actualCost: 115000, profit: 37000, profitRate: 24.3, included: 82, excluded: 5, pending: 2, expressImported: true },
  { id: 3, name: '直播店', platform: '抖音', color: '#00b96b', platformBg: '#00b96b', revenue: 458000, estCost: 320000, actualCost: 335000, profit: 123000, profitRate: 26.9, included: 298, excluded: 12, pending: 2, expressImported: false },
  { id: 4, name: '分销小店', platform: '拼多多', color: '#ff2d1b', platformBg: '#ff2d1b', revenue: 87000, estCost: 62000, actualCost: 65000, profit: 22000, profitRate: 25.3, included: 210, excluded: 18, pending: 6, expressImported: true },
  { id: 5, name: '自营旗舰', platform: '京东', color: '#e1251b', platformBg: '#e1251b', revenue: 221000, estCost: 155000, actualCost: 162000, profit: 59000, profitRate: 26.7, included: 138, excluded: 4, pending: 3, expressImported: false },
]

const rankedShops = computed(() =>
  [...shops].sort((a, b) => b.profit - a.profit)
)

function formatMoney(value: number): string {
  if (value >= 10000) {
    return '¥' + (value / 10000).toFixed(1) + '万'
  }
  return '¥' + value.toLocaleString()
}

function rankProfitPercent(shop: Shop): number {
  const maxProfit = Math.max(...shops.map(s => s.profit))
  return (shop.profit / maxProfit) * 80 + 10
}

function orderBarPercent(shop: Shop, type: 'included' | 'pending' | 'excluded'): number {
  const total = shop.included + shop.pending + shop.excluded
  if (total === 0) return 0
  const val = type === 'included' ? shop.included : type === 'pending' ? shop.pending : shop.excluded
  return (val / total) * 100
}

function handleCardClick() {
  console.log('Card clicked')
}

function handleActionClick() {
  console.log('Action clicked')
}
</script>

<style scoped lang="scss">
.settlement-design-preview {
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

.page-period {
  font-size: 13px;
  color: #64748b;
  margin-top: 4px;
  display: block;
}

.page-header__period {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}

.page-period-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 999px;
  background: #22c55e;
  color: white;
  font-weight: 600;
}

/* ── Scheme A ── */
.scheme-a {
  .month-picker {
    margin-bottom: 16px;

    :deep(.sa-doodle-frame__body) {
      padding: 0;
    }
  }

  .month-picker__inner {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 16px;
  }

  .month-picker__icon {
    font-size: 16px;
  }

  .month-picker__text {
    flex: 1;
    font-family: 'ZCOOL KuaiLe', sans-serif;
    font-size: 15px;
    color: #1e293b;
  }

  .month-picker__arrow {
    font-size: 10px;
    color: #94a3b8;
  }
}

.summary-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 16px;

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.summary-card__body {
  padding: 12px 10px;
  text-align: center;
}

.summary-card__label {
  font-size: 11px;
  color: #64748b;
  margin-bottom: 4px;
}

.summary-card__value {
  font-size: 17px;
  font-weight: 800;
  color: #1e293b;
  line-height: 1.2;
}

.profit-comparison {
  margin-bottom: 16px;
  padding: 14px;
  background: white;
  border-radius: 16px;
  border: 2px solid #e2e8f0;

  .profit-bar {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 10px;

    &:last-child {
      margin-bottom: 0;
    }
  }

  .profit-bar__label {
    font-size: 12px;
    color: #64748b;
    width: 56px;
    flex-shrink: 0;
  }

  .profit-bar__track {
    flex: 1;
    height: 10px;
    background: #f1f5f9;
    border-radius: 999px;
    overflow: hidden;
  }

  .profit-bar__fill {
    height: 100%;
    border-radius: 999px;
    transition: width 0.3s ease;

    &--est {
      background: linear-gradient(90deg, #2563eb, #60a5fa);
    }

    &--actual {
      background: linear-gradient(90deg, #22c55e, #4ade80);
    }
  }

  .profit-bar__value {
    font-size: 12px;
    font-weight: 700;
    color: #1e293b;
    width: 48px;
    text-align: right;
  }
}

/* ── Common Section ── */
.section {
  margin-bottom: 16px;
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
  font-size: 16px;
  color: #1e293b;
  margin: 0;
}

.section-count {
  font-size: 12px;
  color: #94a3b8;
  margin-left: auto;
}

/* Shop Card */
.shop-list {
  display: flex;
  flex-direction: column;
  gap: 10px;

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.shop-card {
  cursor: pointer;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(0.98);
  }
}

.shop-card__inner {
  padding: 14px;
}

.shop-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.shop-card__platform {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 10px;
  color: white;
  font-weight: 600;
  flex-shrink: 0;
}

.shop-card__name {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 15px;
  color: #1e293b;
  flex: 1;
  min-width: 0;
}

.shop-card__profit {
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;

  &.is-up {
    color: #22c55e;
  }

  &.is-down {
    color: #ef4444;
  }
}

.shop-card__metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 10px;
}

.shop-card__metric {
  text-align: center;
}

.shop-card__metric-label {
  display: block;
  font-size: 10px;
  color: #94a3b8;
  margin-bottom: 2px;
}

.shop-card__metric-value {
  display: block;
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;

  &.is-profit {
    color: #22c55e;
  }
}

.shop-card__express {
  display: flex;
  align-items: center;
  gap: 6px;
  padding-top: 8px;
  border-top: 1px dashed #e2e8f0;
}

.shop-card__express-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;

  &.is-done {
    background: #22c55e;
  }

  &.is-pending {
    background: #f59e0b;
  }
}

.shop-card__express-text {
  font-size: 11px;
  color: #64748b;
}

/* Calc Button */
.calc-btn {
  margin-top: 8px;
  margin-bottom: 40px;
  padding: 14px;
  cursor: pointer;
  background: #22c55e;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(0.98);
  }

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.calc-btn__text {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 18px;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

/* ── Scheme B ── */
.scheme-b {
  .page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .page-title {
    font-size: 22px;
  }
}

.shop-tabs {
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

.shop-tab {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: white;
  border-radius: 999px;
  border: 2px solid var(--tab-color, #2563eb);
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;

  &:hover {
    transform: scale(0.98);
  }

  &.active {
    background: var(--tab-color, #2563eb);

    .shop-tab__name {
      color: white;
    }
  }
}

.shop-tab__name {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 13px;
  color: var(--tab-color, #2563eb);
  white-space: nowrap;
}

.shop-tab__badge {
  font-size: 10px;
  font-weight: 700;
  color: white;
  background: #ef4444;
  padding: 1px 5px;
  border-radius: 999px;
  min-width: 18px;
  text-align: center;
}

.shop-detail-section {
  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.shop-overview-card {
  margin-bottom: 12px;

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.shop-overview-card__inner {
  padding: 16px;
}

.shop-overview-card__platform {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 11px;
  color: white;
  font-weight: 600;
  margin-bottom: 8px;
}

.shop-overview-card__name {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 20px;
  color: #1e293b;
  margin-bottom: 14px;
}

.shop-overview-card__stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.shop-overview-card__stat {
  text-align: center;
}

.shop-overview-card__stat-label {
  font-size: 11px;
  color: #94a3b8;
  margin-bottom: 4px;
}

.shop-overview-card__stat-value {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;

  &.is-profit {
    color: #22c55e;
  }
}

.cost-comparison {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 16px;

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.cost-card__inner {
  padding: 12px;
  text-align: center;
}

.cost-card__label {
  font-size: 11px;
  color: #64748b;
  margin-bottom: 4px;
}

.cost-card__value {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}

.order-status {
  margin-bottom: 16px;
  padding: 14px;
  background: white;
  border-radius: 16px;
  border: 2px solid #e2e8f0;
}

.order-status__head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;

  .section-icon {
    width: 18px;
    height: 18px;
  }

  .section-title {
    font-size: 15px;
  }
}

.order-status__bars {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.order-status__bar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.order-status__bar-label {
  font-size: 11px;
  color: #64748b;
  width: 42px;
  flex-shrink: 0;
}

.order-status__bar-track {
  flex: 1;
  height: 8px;
  background: #f1f5f9;
  border-radius: 999px;
  overflow: hidden;
}

.order-status__bar-fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.3s ease;

  &--included {
    background: linear-gradient(90deg, #22c55e, #4ade80);
  }

  &--pending {
    background: linear-gradient(90deg, #f59e0b, #fbbf24);
  }

  &--excluded {
    background: linear-gradient(90deg, #94a3b8, #cbd5e1);
  }
}

.order-status__bar-count {
  font-size: 12px;
  font-weight: 700;
  color: #1e293b;
  width: 32px;
  text-align: right;
}

.max-profit-card {
  margin-bottom: 16px;
  cursor: pointer;

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.max-profit-card__inner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
}

.max-profit-card__crown {
  font-size: 28px;
  flex-shrink: 0;
}

.max-profit-card__content {
  flex: 1;
  min-width: 0;
}

.max-profit-card__label {
  font-size: 11px;
  color: #f59e0b;
  font-weight: 600;
  margin-bottom: 2px;
}

.max-profit-card__order {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 16px;
  color: #1e293b;
  margin-bottom: 2px;
}

.max-profit-card__detail {
  font-size: 12px;
  color: #64748b;
}

.action-row {
  display: flex;
  gap: 8px;
  margin-bottom: 40px;

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.action-btn {
  flex: 1;
  cursor: pointer;
  text-align: center;
  padding: 12px;

  span {
    font-family: 'ZCOOL KuaiLe', sans-serif;
    font-size: 13px;
    color: white;
  }
}

.wave-divider {
  width: 100%;
  height: 12px;
  margin: 16px 0;
}

/* ── Scheme C ── */
.scheme-c {
  .page-header {
    margin-bottom: 12px;
  }

  .page-title {
    font-size: 22px;
  }
}

.progress-overview {
  margin-bottom: 16px;

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.progress-card__inner {
  padding: 16px;
}

.progress-card__header {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 16px;
  color: #1e293b;
  margin-bottom: 12px;
}

.progress-card__percent {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 10px;
}

.progress-card__percent-value {
  font-size: 36px;
  font-weight: 800;
  color: #2563eb;
  line-height: 1;
}

.progress-card__percent-label {
  font-size: 13px;
  color: #64748b;
}

.progress-card__bar {
  height: 8px;
  background: #e2e8f0;
  border-radius: 999px;
  overflow: hidden;
  margin-bottom: 12px;
}

.progress-card__bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #2563eb, #60a5fa);
  border-radius: 999px;
  transition: width 0.3s ease;
}

.progress-card__steps {
  display: flex;
  justify-content: space-between;
}

.progress-card__step {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  background: #e2e8f0;
  color: #94a3b8;

  &.is-done {
    background: #22c55e;
    color: white;
  }

  &.is-active {
    background: #2563eb;
    color: white;
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.2);
  }
}

.checklist {
  margin-bottom: 16px;
}

.checklist-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.checklist-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.checklist-item {
  display: grid;
  grid-template-columns: 14px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 14px;
  border-radius: 16px;
  border: 2px solid #e2e8f0;
  background: white;
  cursor: pointer;
  transition: all 0.15s ease;

  &:hover {
    border-color: #2563eb;
  }

  &.is-done {
    border-color: #bbf7d0;
    background: #f0fdf4;
  }

  &.is-warning {
    border-color: #fed7aa;
    background: #fff7ed;
  }

  &.is-pending {
    border-color: #e2e8f0;
    background: #f8fafc;
  }
}

.checklist-item__dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid #94a3b8;
  flex-shrink: 0;

  .is-done & {
    border-color: #22c55e;
    background: #22c55e;
  }

  .is-warning & {
    border-color: #f59e0b;
    background: #f59e0b;
  }

  .is-pending & {
    border-color: #94a3b8;
  }
}

.checklist-item__body {
  min-width: 0;
}

.checklist-item__title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.checklist-item__title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 14px;
  color: #1e293b;
}

.checklist-item__tag {
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 999px;
  font-weight: 600;

  &.is-success {
    background: #dcfce7;
    color: #15803d;
  }

  &.is-warning {
    background: #fef3c7;
    color: #d97706;
  }

  &.is-pending {
    background: #f1f5f9;
    color: #64748b;
  }
}

.checklist-item__desc {
  font-size: 12px;
  color: #64748b;
}

.checklist-item__arrow {
  font-size: 16px;
  color: #94a3b8;
  flex-shrink: 0;
}

.express-section {
  margin-bottom: 16px;
}

.express-section__head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.express-cards {
  display: flex;
  flex-direction: column;
  gap: 8px;

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.express-card {
  cursor: pointer;

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.express-card__inner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
}

.express-card__icon {
  font-size: 24px;
  flex-shrink: 0;
}

.express-card__body {
  flex: 1;
  min-width: 0;
}

.express-card__name {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 14px;
  color: #1e293b;
}

.express-card__match {
  font-size: 12px;
  color: #64748b;
  margin-top: 2px;
}

.express-card__match-num {
  color: #22c55e;
  font-weight: 700;
}

.express-card__status {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;

  &.is-done {
    background: #dcfce7;
    color: #15803d;
  }

  &.is-warning {
    background: #fef3c7;
    color: #d97706;
  }
}

/* ── Scheme D ── */
.scheme-d {
  .page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
  }

  .page-title {
    font-size: 22px;
  }
}

.top-summary {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.top-summary__row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.top-summary__item {
  padding: 12px;
  border-radius: 12px;
  background: white;
  border: 2px solid #e2e8f0;

  &--revenue {
    border-color: #bfdbfe;
    background: #eff6ff;
  }

  &--profit {
    border-color: #bbf7d0;
    background: #f0fdf4;
  }
}

.top-summary__label {
  font-size: 11px;
  color: #64748b;
  margin-bottom: 4px;
}

.top-summary__value {
  font-size: 18px;
  font-weight: 800;
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 4px;
}

.top-summary__num {
  &.is-included {
    color: #22c55e;
  }

  &.is-pending {
    color: #f59e0b;
  }

  &.is-excluded {
    color: #94a3b8;
  }
}

.top-summary__sep {
  color: #cbd5e1;
  font-weight: 400;
}

.rank-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rank-item {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  padding: 14px;
  border-radius: 16px;
  background: white;
  border: 2px solid #e2e8f0;
  cursor: pointer;
  transition: all 0.15s ease;
  position: relative;

  &:hover {
    border-color: #2563eb;
  }

  &.is-top {
    border-color: #fbbf24;
    background: #fffbeb;
  }
}

.rank-item__medal {
  text-align: center;
}

.medal-icon {
  font-size: 22px;
}

.rank-item__num {
  font-size: 16px;
  font-weight: 800;
  color: #94a3b8;
  text-align: center;
}

.rank-item__info {
  min-width: 0;
}

.rank-item__name-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.rank-item__name {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 14px;
  color: #1e293b;
}

.rank-item__platform {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 3px;
  color: white;
  font-weight: 600;
}

.rank-item__metrics {
  display: flex;
  gap: 12px;
  font-size: 12px;
}

.rank-item__revenue {
  color: #64748b;
}

.rank-item__profit {
  font-weight: 700;

  &.is-profit {
    color: #22c55e;
  }
}

.rank-item__profit-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: #f1f5f9;
  border-radius: 0 0 16px 16px;
  overflow: hidden;
}

.rank-item__profit-fill {
  height: 100%;
  border-radius: 0 0 16px 16px;
  transition: width 0.3s ease;
}

/* Platform Distribution */
.platform-dist-card {
  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.platform-dist-card__inner {
  padding: 16px;
}

.platform-dist__rows {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.platform-dist__row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.platform-dist__label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #1e293b;
  font-weight: 600;
  width: 80px;
  flex-shrink: 0;
}

.platform-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.platform-dist__bar {
  flex: 1;
  height: 10px;
  background: #f1f5f9;
  border-radius: 999px;
  overflow: hidden;
}

.platform-dist__fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.3s ease;
}

.platform-dist__value {
  font-size: 12px;
  font-weight: 700;
  color: #1e293b;
  width: 52px;
  text-align: right;
}

/* Quick Actions */
.quick-actions {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 16px;

  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.quick-action-btn {
  cursor: pointer;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(0.95);
  }
}

.quick-action-icon {
  font-size: 24px;
  display: block;
  text-align: center;
  margin-bottom: 4px;
  padding-top: 8px;
}

.quick-action-text {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 11px;
  color: #1e293b;
  display: block;
  text-align: center;
  padding-bottom: 8px;
}

/* ── Scheme Info ── */
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
