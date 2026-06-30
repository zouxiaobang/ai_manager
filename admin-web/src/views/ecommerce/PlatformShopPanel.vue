<template>
  <div class="shop-dashboard">
    <header class="shop-dashboard__header">
      <div>
        <h2 class="shop-dashboard__title">{{ t('ecommerce.shop.pageTitle') }}</h2>
        <p class="shop-dashboard__subtitle">{{ t('ecommerce.shop.pageSubtitle') }}</p>
      </div>
    </header>

    <section v-loading="loading" class="shop-hero">
      <div class="shop-hero__stats">
        <div class="shop-hero__stat">
          <div class="shop-hero__stat-icon" aria-hidden="true">
            <el-icon><Shop /></el-icon>
          </div>
          <div class="shop-hero__stat-body">
            <div class="shop-hero__stat-label">{{ t('ecommerce.shop.statTotal') }}</div>
            <div class="shop-hero__stat-value is-total">{{ stats.total }}</div>
          </div>
        </div>
        <div class="shop-hero__stat">
          <div class="shop-hero__stat-icon" aria-hidden="true">
            <el-icon><CircleCheck /></el-icon>
          </div>
          <div class="shop-hero__stat-body">
            <div class="shop-hero__stat-label">{{ t('ecommerce.shop.statEnabled') }}</div>
            <div class="shop-hero__stat-value is-enabled">{{ stats.enabled }}</div>
          </div>
        </div>
        <div class="shop-hero__stat">
          <div class="shop-hero__stat-icon" aria-hidden="true">
            <el-icon><Grid /></el-icon>
          </div>
          <div class="shop-hero__stat-body">
            <div class="shop-hero__stat-label">{{ t('ecommerce.shop.statPlatformCount') }}</div>
            <div class="shop-hero__stat-value is-platform">{{ stats.platformCount }}</div>
          </div>
        </div>
        <div class="shop-hero__stat">
          <div class="shop-hero__stat-icon" aria-hidden="true">
            <el-icon><DataAnalysis /></el-icon>
          </div>
          <div class="shop-hero__stat-body">
            <div class="shop-hero__stat-label">{{ t('ecommerce.shop.statAvgFee') }}</div>
            <div class="shop-hero__stat-value is-fee">{{ stats.avgFeePct }}</div>
          </div>
        </div>
      </div>
      <div class="shop-hero__divider" aria-hidden="true" />
      <div class="shop-hero__chart-wrap">
        <div class="shop-hero__chart-body">
          <div class="shop-hero__chart">
            <div ref="chartRef" class="shop-hero__chart-canvas" />
            <div class="shop-hero__chart-center">
              <div class="shop-hero__chart-center-value">{{ stats.total }}</div>
              <div class="shop-hero__chart-center-label">{{ t('ecommerce.shop.statTotal') }}</div>
            </div>
          </div>
          <ul v-if="chartLegendItems.length" class="shop-hero__chart-legend">
            <li v-for="item in chartLegendItems" :key="item.platformId" class="shop-hero__legend-item">
              <span class="shop-hero__legend-dot" :style="{ background: item.color }" />
              <span class="shop-hero__legend-name">{{ item.name }}</span>
              <span class="shop-hero__legend-value">{{ item.count }} ({{ item.pct }}%)</span>
            </li>
          </ul>
        </div>
      </div>
    </section>

    <div class="shop-dashboard__toolbar">
      <el-input
        v-model="keyword"
        :placeholder="t('ecommerce.shop.searchPlaceholder')"
        clearable
        class="shop-dashboard__search"
      />
      <div class="shop-filter-chips">
        <button
          type="button"
          class="shop-filter-chip"
          :class="{ 'is-active': activePlatformId === null }"
          @click="activePlatformId = null"
        >
          {{ t('ecommerce.shop.filterAll') }}
          <span class="shop-filter-chip__count">{{ filteredShops.length }}</span>
        </button>
        <button
          v-for="group in platformGroups"
          :key="group.platformId"
          type="button"
          class="shop-filter-chip"
          :class="{ 'is-active': activePlatformId === group.platformId }"
          @click="activePlatformId = group.platformId"
        >
          <img
            :src="resolvePlatformIcon(group.platformName, group.platformCode, group.platformAvatarUrl)"
            alt=""
            class="shop-filter-chip__icon"
          />
          {{ group.platformName }}
          <span class="shop-filter-chip__count">{{ group.shops.length }}</span>
        </button>
      </div>
      <div class="shop-dashboard__toolbar-actions">
        <el-button :icon="Setting" @click="platformDrawerVisible = true">
          {{ t('ecommerce.shop.platformManage') }}
        </el-button>
      </div>
    </div>

    <div v-loading="loading" class="shop-groups">
      <template v-if="visibleGroups.length">
        <section v-for="group in visibleGroups" :key="group.platformId" class="shop-group">
          <button
            type="button"
            class="shop-group__header"
            @click="toggleGroup(group.platformId)"
          >
            <img
              :src="resolvePlatformIcon(group.platformName, group.platformCode, group.platformAvatarUrl)"
              alt=""
              class="shop-group__platform-icon"
              :class="{ 'is-avatar': Boolean(group.platformAvatarUrl?.trim()) }"
            />
            <span class="shop-group__name">
              {{ group.platformName }}
              <span class="shop-group__count">{{ t('ecommerce.shop.shopCount', { count: group.shops.length }) }}</span>
            </span>
            <el-icon class="shop-group__arrow" :class="{ 'is-collapsed': collapsedGroups.has(group.platformId) }">
              <ArrowDown />
            </el-icon>
          </button>
          <div v-show="!collapsedGroups.has(group.platformId)" class="shop-group__body">
            <div class="shop-tiles">
              <article
                v-for="shop in group.shops"
                :key="shop.id"
                class="shop-tile"
                :class="{ 'is-disabled': shop.status !== 'ENABLED' }"
              >
                <img
                  :src="resolveShopIcon(shop.name, shop.platformName, shop.platformCode, shop.avatarUrl)"
                  alt=""
                  class="shop-tile__icon"
                  :class="{ 'is-avatar': Boolean(shop.avatarUrl?.trim()) }"
                />
                <div class="shop-tile__main">
                  <div class="shop-tile__name" :title="shop.name">{{ shop.name }}</div>
                  <div class="shop-tile__fee">
                    {{ t('ecommerce.shop.totalFeePct') }} {{ formatPct(computeTotalFeePct(shop)) }}
                  </div>
                  <span
                    class="shop-tile__status"
                    :class="shop.status === 'ENABLED' ? 'is-operating' : 'is-resting'"
                  >
                    <span class="shop-tile__status-dot" />
                    {{ shop.status === 'ENABLED' ? t('ecommerce.shop.statusOperating') : t('ecommerce.shop.statusResting') }}
                  </span>
                </div>
                <el-dropdown trigger="click" @command="(cmd: string) => onShopCommand(cmd, shop)">
                  <button type="button" class="shop-tile__menu" @click.stop>
                    <svg class="shop-tile__menu-icon" viewBox="0 0 24 24" aria-hidden="true">
                      <circle cx="12" cy="5" r="1.6" fill="currentColor" />
                      <circle cx="12" cy="12" r="1.6" fill="currentColor" />
                      <circle cx="12" cy="19" r="1.6" fill="currentColor" />
                    </svg>
                  </button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="edit">
                        <el-icon><Edit /></el-icon>{{ t('ecommerce.shop.edit') }}
                      </el-dropdown-item>
                      <el-dropdown-item command="delete" divided>
                        <span class="shop-tile__delete">
                          <el-icon><Delete /></el-icon>{{ t('ecommerce.shop.delete') }}
                        </span>
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </article>
            </div>
          </div>
        </section>
      </template>
      <el-empty v-else :description="keyword.trim() ? t('ecommerce.shop.emptySearch') : t('ecommerce.shop.emptyShops')" />
    </div>

    <el-button type="primary" class="shop-fab" :icon="Plus" @click="openShopCreate">
      {{ t('ecommerce.shop.add') }}
    </el-button>

    <!-- 店铺表单 -->
    <ShopFormDialog
      v-model="shopDialogVisible"
      :shop="editingShop"
      :platform-options="platformOptions"
      @saved="loadAll"
    />

    <PlatformManageDrawer v-model="platformDrawerVisible" @changed="onPlatformChanged" />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, CircleCheck, DataAnalysis, Delete, Edit, Grid, Plus, Setting, Shop } from '@element-plus/icons-vue'
import type { ECharts } from 'echarts/core'
import { fetchPlatformOptions, type EcPlatform } from '@/api/ecommerce/platform'
import {
  deleteShop,
  fetchShopOptions,
  type EcShop,
} from '@/api/ecommerce/shop'
import { echarts } from '@/utils/echarts'
import { resolvePlatformIcon } from '@/utils/platformVisual'
import { resolveShopIcon } from '@/utils/shopVisual'
import PlatformManageDrawer from './PlatformManageDrawer.vue'
import ShopFormDialog from './ShopFormDialog.vue'

const { t } = useI18n()

const loading = ref(false)
const keyword = ref('')
const allShops = ref<EcShop[]>([])
const platformOptions = ref<EcPlatform[]>([])
const activePlatformId = ref<number | null>(null)
const collapsedGroups = ref(new Set<number>())
const platformDrawerVisible = ref(false)

const chartRef = ref<HTMLElement | null>(null)
let chart: ECharts | null = null

const shopDialogVisible = ref(false)
const editingShop = ref<EcShop | null>(null)

const PLATFORM_COLORS: Record<number, string> = {
  0: '#6b7280',
  1: '#f97316',
  2: '#ff6a00',
  3: '#e1251b',
  4: '#e02e24',
  5: '#111827',
  6: '#c91623',
  7: '#ff2442',
  8: '#ff4906',
  99: '#64748b',
}

interface PlatformGroup {
  platformId: number
  platformName: string
  platformCode?: number
  platformAvatarUrl?: string | null
  shops: EcShop[]
}

const platformAvatarById = computed(() => {
  const map = new Map<number, string | null | undefined>()
  for (const p of platformOptions.value) {
    map.set(p.id, p.avatarUrl)
  }
  return map
})

function buildPlatformGroups(shops: EcShop[]): PlatformGroup[] {
  const map = new Map<number, PlatformGroup>()
  for (const shop of shops) {
    let group = map.get(shop.platformId)
    if (!group) {
      group = {
        platformId: shop.platformId,
        platformName: shop.platformName || t('ecommerce.platform.codes.other'),
        platformCode: shop.platformCode,
        platformAvatarUrl: platformAvatarById.value.get(shop.platformId),
        shops: [],
      }
      map.set(shop.platformId, group)
    }
    group.shops.push(shop)
  }
  return Array.from(map.values()).sort((a, b) => a.platformName.localeCompare(b.platformName, 'zh-CN'))
}

const filteredShops = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return allShops.value
  return allShops.value.filter((shop) => {
    const haystack = [shop.name, shop.nameEn].filter(Boolean).join(' ').toLowerCase()
    return haystack.includes(kw)
  })
})

const chartPlatformGroups = computed(() => buildPlatformGroups(allShops.value))

const platformGroups = computed(() => buildPlatformGroups(filteredShops.value))

const visibleGroups = computed(() => {
  if (activePlatformId.value === null) return platformGroups.value
  return platformGroups.value.filter((g) => g.platformId === activePlatformId.value)
})

const stats = computed(() => {
  const shops = allShops.value
  const enabled = shops.filter((s) => s.status === 'ENABLED').length
  const platformIds = new Set(shops.map((s) => s.platformId))
  const fees = shops.map(computeTotalFeePct).filter((v) => v > 0)
  const avg = fees.length ? fees.reduce((a, b) => a + b, 0) / fees.length : 0
  return {
    total: shops.length,
    enabled,
    platformCount: platformIds.size,
    avgFeePct: avg > 0 ? `${avg.toFixed(2)}%` : '—',
  }
})

const chartLegendItems = computed(() => {
  const total = allShops.value.length
  return chartPlatformGroups.value.map((g) => ({
    platformId: g.platformId,
    name: g.platformName,
    count: g.shops.length,
    pct: total > 0 ? ((g.shops.length / total) * 100).toFixed(1) : '0.0',
    color: platformColor(g.platformCode),
  }))
})

function computeTotalFeePct(shop: EcShop): number {
  const fields = [
    shop.categoryCommissionPct,
    shop.techServiceFeePct,
    shop.paymentFeePct,
    shop.promotionFeePct,
    shop.fulfillmentFeePct,
    shop.returnServiceFeePct,
    shop.installmentFeePct,
    shop.activityServiceFeePct,
    shop.otherFeePct,
  ]
  return fields.reduce<number>((sum, v) => sum + (v ?? 0), 0)
}

function formatPct(v: number) {
  if (!v) return '—'
  return `${v.toFixed(2)}%`
}

function platformColor(code?: number) {
  return PLATFORM_COLORS[code ?? 99] ?? PLATFORM_COLORS[99]
}

function toggleGroup(platformId: number) {
  const next = new Set(collapsedGroups.value)
  if (next.has(platformId)) next.delete(platformId)
  else next.add(platformId)
  collapsedGroups.value = next
}

function syncDefaultCollapsedGroups() {
  const groups = visibleGroups.value
  if (!groups.length) {
    collapsedGroups.value = new Set()
    return
  }
  const firstId = groups[0].platformId
  collapsedGroups.value = new Set(
    groups.filter((g) => g.platformId !== firstId).map((g) => g.platformId),
  )
}

function renderChart() {
  const el = chartRef.value
  if (!el) return
  if (!chart) chart = echarts.init(el)

  const data = chartPlatformGroups.value.map((g) => ({
    name: g.platformName,
    value: g.shops.length,
    platformCode: g.platformCode,
    itemStyle: { color: platformColor(g.platformCode) },
  }))

  chart.setOption({
    animation: true,
    tooltip: {
      trigger: 'item',
      borderWidth: 0,
      padding: 0,
      backgroundColor: 'transparent',
      extraCssText: 'box-shadow:none;',
      formatter: (params: unknown) => formatPlatformPieTooltip(params),
    },
    series: [
      {
        type: 'pie',
        radius: ['50%', '80%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: true,
        label: { show: false },
        data: data.length ? data : [{ name: t('ecommerce.shop.emptyShops'), value: 1, itemStyle: { color: '#e5e7eb' } }],
      },
    ],
  })
}

function formatPlatformPieTooltip(params: unknown) {
  const p = params as {
    name: string
    value: number
    percent?: number
    color?: string
    data?: { platformCode?: number }
  }
  const color = p.color ?? '#64748b'
  const pct = typeof p.percent === 'number' ? p.percent.toFixed(1) : '0.0'
  const icon = resolvePlatformIcon(p.name, p.data?.platformCode)
  return [
    '<div style="',
    `background:${color};`,
    'padding:8px 12px;',
    'border-radius:8px;',
    'color:#fff;',
    'font-size:13px;',
    'font-weight:600;',
    'display:flex;',
    'align-items:center;',
    'gap:8px;',
    'box-shadow:0 4px 12px rgba(0,0,0,0.18);',
    '">',
    `<img src="${icon}" width="20" height="20" alt="" style="object-fit:contain;background:#fff;border-radius:4px;padding:2px;flex-shrink:0;" />`,
    `<span>${p.name}: ${p.value} (${pct}%)</span>`,
    '</div>',
  ].join('')
}

function openShopCreate() {
  editingShop.value = null
  shopDialogVisible.value = true
}

function openShopEdit(row: EcShop) {
  editingShop.value = { ...row }
  shopDialogVisible.value = true
}

async function onShopDelete(row: EcShop) {
  await ElMessageBox.confirm(t('ecommerce.shop.deleteConfirm', { name: row.name }), { type: 'warning' })
  await deleteShop(row.id)
  ElMessage.success(t('ecommerce.common.deleted'))
  await loadAll()
}

function onShopCommand(cmd: string, shop: EcShop) {
  if (cmd === 'edit') openShopEdit(shop)
  else if (cmd === 'delete') onShopDelete(shop)
}

async function refreshPlatformOptions() {
  platformOptions.value = await fetchPlatformOptions()
}

async function loadShops() {
  allShops.value = await fetchShopOptions()
}

async function loadAll() {
  loading.value = true
  try {
    await Promise.all([refreshPlatformOptions(), loadShops()])
    await nextTick()
    renderChart()
  } finally {
    loading.value = false
  }
}

async function onPlatformChanged() {
  await loadAll()
}

function onResize() {
  chart?.resize()
}

watch(chartPlatformGroups, () => {
  nextTick(renderChart)
})

watch([platformGroups, activePlatformId], () => {
  syncDefaultCollapsedGroups()
})

onMounted(() => {
  loadAll()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  chart?.dispose()
  chart = null
})

defineExpose({ loadAll })
</script>

<style scoped lang="scss">
.shop-dashboard {
  position: relative;
  padding-bottom: 72px;
}

.shop-dashboard__header {
  margin-bottom: 16px;
}

.shop-dashboard__title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--wr-text, #333);
}

.shop-dashboard__subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--wr-muted, #999);
}

.shop-hero {
  display: flex;
  gap: 20px;
  padding: 20px 24px;
  margin-bottom: 16px;
  border-radius: 16px;
  background: linear-gradient(135deg, #1d4ed8 0%, #2563eb 45%, #3b82f6 100%);
  color: #fff;
  min-height: 140px;
}

.shop-hero__stats {
  flex: 1;
  display: flex;
  align-items: stretch;
}

.shop-hero__stat {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  min-width: 0;
  padding: 0 18px;

  &:not(:last-child) {
    border-right: 1px solid rgb(255 255 255 / 35%);
  }
}

.shop-hero__stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: rgb(255 255 255 / 18%);
  flex-shrink: 0;
  font-size: 28px;
  color: #fff;
}

.shop-hero__stat-body {
  min-width: 0;
}

.shop-hero__stat-label {
  font-size: 13px;
  opacity: 0.85;
}

.shop-hero__stat-value {
  margin-top: 12px;
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;

  &.is-total {
    color: #fef08a;
  }

  &.is-enabled {
    color: #86efac;
  }

  &.is-platform {
    color: #c4b5fd;
  }

  &.is-fee {
    color: #fda4af;
  }
}

.shop-hero__divider {
  width: 1px;
  align-self: stretch;
  flex-shrink: 0;
  margin: 4px 16px;
  background: rgb(255 255 255 / 35%);
}

.shop-hero__chart-wrap {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 280px;
}

.shop-hero__chart-body {
  display: flex;
  align-items: center;
  gap: 16px;
}

.shop-hero__chart {
  position: relative;
  width: 140px;
  height: 140px;
  flex-shrink: 0;
}

.shop-hero__chart-canvas {
  width: 100%;
  height: 100%;
}

.shop-hero__chart-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.shop-hero__chart-center-value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.1;
  color: #fff;
}

.shop-hero__chart-center-label {
  margin-top: 2px;
  font-size: 11px;
  color: rgb(255 255 255 / 80%);
}

.shop-hero__chart-legend {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.shop-hero__legend-item {
  display: grid;
  grid-template-columns: 8px 4.5em auto;
  align-items: center;
  column-gap: 6px;
  font-size: 12px;
  line-height: 1.4;
  color: rgb(255 255 255 / 95%);
  text-align: left;
}

.shop-hero__legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.shop-hero__legend-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shop-hero__legend-value {
  color: rgb(255 255 255 / 85%);
  text-align: left;
  white-space: nowrap;
}

.shop-dashboard__toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.shop-dashboard__search {
  width: 220px;
  flex-shrink: 0;
}

.shop-filter-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.shop-filter-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border: 1px solid var(--wr-border, #e8ecef);
  border-radius: 999px;
  background: var(--wr-card, #fff);
  color: var(--wr-text, #333);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s, color 0.15s;

  &:hover {
    border-color: #93c5fd;
    color: var(--wr-stat-blue, #2563eb);
  }

  &.is-active {
    border-color: var(--wr-stat-blue, #2563eb);
    background: var(--wr-stat-blue-bg, #eff6ff);
    color: var(--wr-stat-blue, #2563eb);
    font-weight: 700;
  }
}

.shop-filter-chip__icon {
  width: 18px;
  height: 18px;
  border-radius: 4px;
  flex-shrink: 0;
  object-fit: contain;
}

.shop-filter-chip__count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 999px;
  background: rgb(37 99 235 / 10%);
  font-size: 12px;
  font-weight: 600;
}

.shop-dashboard__toolbar-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.shop-groups {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.shop-group {
  border: 1px solid var(--wr-border, #e8ecef);
  border-radius: 14px;
  background: var(--wr-card, #fff);
  overflow: hidden;
}

.shop-group__header {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 14px 16px;
  border: none;
  background: transparent;
  cursor: pointer;
  text-align: left;
  transition: background 0.15s;

  &:hover {
    background: transparent;
  }
}

.shop-group__platform-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  flex-shrink: 0;
  object-fit: contain;

  &.is-avatar {
    object-fit: cover;
    border-radius: 50%;
  }
}

.shop-group__name {
  font-size: 15px;
  font-weight: 600;
  color: var(--wr-text, #333);
}

.shop-group__count {
  margin-left: 8px;
  font-size: 13px;
  font-weight: 400;
  color: var(--wr-muted, #999);
}

.shop-group__arrow {
  margin-left: auto;
  transition: transform 0.2s;

  &.is-collapsed {
    transform: rotate(-90deg);
  }
}

.shop-group__body {
  padding: 12px 16px 16px;
}

.shop-tiles {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.shop-tile {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 240px;
  padding: 14px;
  border: 1px solid var(--wr-border, #e8ecef);
  border-radius: 12px;
  background: #fff;
  transition: box-shadow 0.15s, border-color 0.15s;

  :deep(.el-dropdown) {
    align-self: center;
    flex-shrink: 0;
  }

  &:hover {
    border-color: #bfdbfe;
    box-shadow: 0 4px 12px rgb(37 99 235 / 8%);
  }

  &.is-disabled {
    opacity: 0.72;
  }
}

.shop-tile__icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  flex-shrink: 0;
  object-fit: cover;
  background: #f3f4f6;

  &:not(.is-avatar) {
    object-fit: contain;
    padding: 4px;
  }
}

.shop-tile__main {
  flex: 1;
  min-width: 0;
}

.shop-tile__name {
  font-size: 14px;
  font-weight: 600;
  color: var(--wr-text, #333);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.shop-tile__fee {
  margin-top: 4px;
  font-size: 12px;
  color: var(--wr-text-secondary, #666);
}

.shop-tile__status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  font-size: 12px;
  font-weight: 500;
  line-height: 1;

  &.is-operating {
    color: #16a34a;
  }

  &.is-resting {
    color: #ea580c;
  }
}

.shop-tile__status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
  background: currentColor;
}

.shop-tile__menu {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #4b5563;
  cursor: pointer;
  flex-shrink: 0;

  &:hover {
    background: #f3f4f6;
    color: #1f2937;
  }
}

.shop-tile__menu-icon {
  width: 18px;
  height: 18px;
  display: block;
}

.shop-tile__delete {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #ef4444;
}

.shop-fab {
  position: fixed;
  right: 32px;
  bottom: 32px;
  z-index: 10;
  height: 44px;
  padding: 0 20px;
  border-radius: 22px;
  font-weight: 600;
  box-shadow: 0 8px 24px rgb(37 99 235 / 35%);
}

@media (max-width: 768px) {
  .shop-hero {
    flex-direction: column;
  }

  .shop-hero__stats {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 0;
  }

  .shop-hero__stat {
    justify-content: center;
    padding: 12px 16px;
    border-right: 1px solid rgb(255 255 255 / 35%);
    border-bottom: 1px solid rgb(255 255 255 / 35%);

    &:nth-child(2n) {
      border-right: none;
    }

    &:nth-child(n + 3) {
      border-bottom: none;
    }
  }

  .shop-hero__divider {
    width: 100%;
    height: 1px;
    margin: 8px 0;
  }

  .shop-hero__chart-wrap {
    width: 100%;
    min-width: 0;
  }

  .shop-tile {
    width: 100%;
  }
}
</style>
