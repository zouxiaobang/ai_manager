<template>
  <el-drawer
    v-model="visible"
    :title="t('ecommerce.shop.platformManageTitle')"
    size="960px"
    destroy-on-close
    class="platform-manage-drawer"
    @open="onOpen"
    @closed="onClosed"
  >
    <div class="platform-dashboard">
      <p class="platform-dashboard__subtitle">{{ t('ecommerce.platform.drawerSubtitle') }}</p>

      <section v-loading="loading" class="platform-hero">
        <div class="platform-hero__stats">
          <div class="platform-hero__stat">
            <div class="platform-hero__stat-icon" aria-hidden="true">
              <el-icon><Grid /></el-icon>
            </div>
            <div class="platform-hero__stat-body">
              <div class="platform-hero__stat-label">{{ t('ecommerce.platform.statTotal') }}</div>
              <div class="platform-hero__stat-value is-total">{{ stats.total }}</div>
            </div>
          </div>
          <div class="platform-hero__stat">
            <div class="platform-hero__stat-icon" aria-hidden="true">
              <el-icon><CircleCheck /></el-icon>
            </div>
            <div class="platform-hero__stat-body">
              <div class="platform-hero__stat-label">{{ t('ecommerce.platform.statEnabled') }}</div>
              <div class="platform-hero__stat-value is-enabled">{{ stats.enabled }}</div>
            </div>
          </div>
          <div class="platform-hero__stat">
            <div class="platform-hero__stat-icon" aria-hidden="true">
              <el-icon><Monitor /></el-icon>
            </div>
            <div class="platform-hero__stat-body">
              <div class="platform-hero__stat-label">{{ t('ecommerce.platform.statOnline') }}</div>
              <div class="platform-hero__stat-value is-online">{{ stats.online }}</div>
            </div>
          </div>
          <div class="platform-hero__stat">
            <div class="platform-hero__stat-icon" aria-hidden="true">
              <el-icon><OfficeBuilding /></el-icon>
            </div>
            <div class="platform-hero__stat-body">
              <div class="platform-hero__stat-label">{{ t('ecommerce.platform.statOffline') }}</div>
              <div class="platform-hero__stat-value is-offline">{{ stats.offline }}</div>
            </div>
          </div>
        </div>
        <div class="platform-hero__divider" aria-hidden="true" />
        <div class="platform-hero__chart-wrap">
          <div class="platform-hero__chart-body">
            <div class="platform-hero__chart">
              <div ref="chartRef" class="platform-hero__chart-canvas" />
              <div class="platform-hero__chart-center">
                <div class="platform-hero__chart-center-value">{{ stats.total }}</div>
                <div class="platform-hero__chart-center-label">{{ t('ecommerce.platform.statTotal') }}</div>
              </div>
            </div>
            <ul v-if="chartLegendItems.length" class="platform-hero__chart-legend">
              <li v-for="item in chartLegendItems" :key="item.key" class="platform-hero__legend-item">
                <span class="platform-hero__legend-dot" :style="{ background: item.color }" />
                <span class="platform-hero__legend-name">{{ item.name }}</span>
                <span class="platform-hero__legend-value">{{ item.count }} ({{ item.pct }}%)</span>
              </li>
            </ul>
          </div>
        </div>
      </section>

      <div class="platform-dashboard__toolbar">
        <el-input
          v-model="keyword"
          :placeholder="t('ecommerce.platform.searchPlaceholder')"
          clearable
          class="platform-dashboard__search"
        />
        <div class="platform-filter-chips">
          <button
            type="button"
            class="platform-filter-chip"
            :class="{ 'is-active': activeChannel === null }"
            @click="activeChannel = null"
          >
            {{ t('ecommerce.platform.filterAll') }}
            <span class="platform-filter-chip__count">{{ allPlatforms.length }}</span>
          </button>
          <button
            type="button"
            class="platform-filter-chip"
            :class="{ 'is-active': activeChannel === 'ONLINE' }"
            @click="activeChannel = 'ONLINE'"
          >
            {{ t('ecommerce.platform.online') }}
            <span class="platform-filter-chip__count">{{ stats.online }}</span>
          </button>
          <button
            type="button"
            class="platform-filter-chip"
            :class="{ 'is-active': activeChannel === 'OFFLINE' }"
            @click="activeChannel = 'OFFLINE'"
          >
            {{ t('ecommerce.platform.offline') }}
            <span class="platform-filter-chip__count">{{ stats.offline }}</span>
          </button>
        </div>
      </div>

      <div v-loading="loading" class="platform-groups">
        <template v-if="visibleChannelGroups.length">
          <section
            v-for="group in visibleChannelGroups"
            :key="group.channelType"
            class="platform-group"
          >
            <button
              type="button"
              class="platform-group__header"
              @click="toggleGroup(group.channelType)"
            >
              <span class="platform-group__badge" :class="group.channelType === 'ONLINE' ? 'is-online' : 'is-offline'">
                {{ group.channelType === 'ONLINE' ? t('ecommerce.platform.groupOnline') : t('ecommerce.platform.groupOffline') }}
              </span>
              <span class="platform-group__count">
                {{ t('ecommerce.platform.platformCount', { count: group.platforms.length }) }}
              </span>
              <el-icon class="platform-group__arrow" :class="{ 'is-collapsed': collapsedGroups.has(group.channelType) }">
                <ArrowDown />
              </el-icon>
            </button>
            <div v-show="!collapsedGroups.has(group.channelType)" class="platform-group__body">
              <div class="platform-tiles">
                <article
                  v-for="platform in group.platforms"
                  :key="platform.id"
                  class="platform-tile"
                  :class="{ 'is-disabled': platform.status !== 'ENABLED' }"
                >
                  <img
                    :src="resolvePlatformIcon(platform.name, platform.platformCode, platform.avatarUrl)"
                    alt=""
                    class="platform-tile__icon"
                    :class="{ 'is-avatar': Boolean(platform.avatarUrl?.trim()) }"
                  />
                  <div class="platform-tile__main">
                    <div class="platform-tile__name" :title="platform.name">{{ platform.name }}</div>
                    <div class="platform-tile__shops">
                      <span
                        v-if="(shopCountMap.get(platform.id) ?? 0) > 0"
                        class="platform-tile__shops-tag"
                      >
                        {{ t('ecommerce.platform.shopCount', { count: shopCountMap.get(platform.id) }) }}
                      </span>
                      <span v-else class="platform-tile__shops-empty">{{ t('ecommerce.platform.noShops') }}</span>
                    </div>
                    <span
                      class="platform-tile__status"
                      :class="platform.status === 'ENABLED' ? 'is-enabled' : 'is-disabled-status'"
                    >
                      <span class="platform-tile__status-dot" />
                      {{
                        platform.status === 'ENABLED'
                          ? t('ecommerce.product.enabled')
                          : t('ecommerce.product.disabled')
                      }}
                    </span>
                  </div>
                  <el-dropdown trigger="click" @command="(cmd: string) => onPlatformCommand(cmd, platform)">
                    <button type="button" class="platform-tile__menu" @click.stop>
                      <svg class="platform-tile__menu-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <circle cx="12" cy="5" r="1.6" fill="currentColor" />
                        <circle cx="12" cy="12" r="1.6" fill="currentColor" />
                        <circle cx="12" cy="19" r="1.6" fill="currentColor" />
                      </svg>
                    </button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="edit">
                          <el-icon><Edit /></el-icon>{{ t('ecommerce.platform.edit') }}
                        </el-dropdown-item>
                        <el-dropdown-item command="delete" divided>
                          <span class="platform-tile__delete">
                            <el-icon><Delete /></el-icon>{{ t('ecommerce.platform.delete') }}
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
        <el-empty v-else :description="t('ecommerce.platform.emptyPlatforms')" />
      </div>

      <el-button type="primary" class="platform-fab" :icon="Plus" @click="openCreate">
        {{ t('ecommerce.platform.add') }}
      </el-button>
    </div>

    <PlatformFormDialog v-model="dialogVisible" :platform="editingPlatform" @saved="onPlatformSaved" />
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, CircleCheck, Delete, Edit, Grid, Monitor, OfficeBuilding, Plus } from '@element-plus/icons-vue'
import type { ECharts } from 'echarts/core'
import {
  PLATFORM_CODE_OPTIONS,
  deletePlatform,
  fetchPlatforms,
  type EcPlatform,
} from '@/api/ecommerce/platform'
import { fetchShopOptions } from '@/api/ecommerce/shop'
import { echarts } from '@/utils/echarts'
import { resolvePlatformIcon } from '@/utils/platformVisual'
import PlatformFormDialog from './PlatformFormDialog.vue'

const { t } = useI18n()

const visible = defineModel<boolean>({ default: false })
const emit = defineEmits<{ changed: [] }>()

const loading = ref(false)
const keyword = ref('')
const activeChannel = ref<'ONLINE' | 'OFFLINE' | null>(null)
const allPlatforms = ref<EcPlatform[]>([])
const shopCountMap = ref(new Map<number, number>())
const collapsedGroups = ref(new Set<string>())

const chartRef = ref<HTMLElement | null>(null)
let chart: ECharts | null = null

const dialogVisible = ref(false)
const editingPlatform = ref<EcPlatform | null>(null)

const CHANNEL_COLORS = {
  ONLINE: '#dc2626',
  OFFLINE: '#16a34a',
} as const

interface ChannelGroup {
  channelType: 'ONLINE' | 'OFFLINE'
  platforms: EcPlatform[]
}

const filteredPlatforms = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return allPlatforms.value.filter((p) => {
    if (activeChannel.value && p.channelType !== activeChannel.value) return false
    if (!kw) return true
    const haystack = [p.name, p.nameEn, p.remark, platformCodeLabel(p.platformCode)]
      .filter(Boolean)
      .join(' ')
      .toLowerCase()
    return haystack.includes(kw)
  })
})

const visibleChannelGroups = computed<ChannelGroup[]>(() => {
  const online: EcPlatform[] = []
  const offline: EcPlatform[] = []
  for (const p of filteredPlatforms.value) {
    if (p.channelType === 'OFFLINE') offline.push(p)
    else online.push(p)
  }
  const sortByName = (a: EcPlatform, b: EcPlatform) => a.name.localeCompare(b.name, 'zh-CN')
  const groups: ChannelGroup[] = []
  if (online.length) groups.push({ channelType: 'ONLINE', platforms: online.sort(sortByName) })
  if (offline.length) groups.push({ channelType: 'OFFLINE', platforms: offline.sort(sortByName) })
  return groups
})

const stats = computed(() => {
  const platforms = allPlatforms.value
  const enabled = platforms.filter((p) => p.status === 'ENABLED').length
  const online = platforms.filter((p) => p.channelType === 'ONLINE').length
  const offline = platforms.filter((p) => p.channelType === 'OFFLINE').length
  return {
    total: platforms.length,
    enabled,
    online,
    offline,
  }
})

const chartLegendItems = computed(() => {
  const total = allPlatforms.value.length
  const online = stats.value.online
  const offline = stats.value.offline
  const items = [
    { key: 'ONLINE', name: t('ecommerce.platform.online'), count: online, color: CHANNEL_COLORS.ONLINE },
    { key: 'OFFLINE', name: t('ecommerce.platform.offline'), count: offline, color: CHANNEL_COLORS.OFFLINE },
  ]
  return items
    .filter((item) => item.count > 0)
    .map((item) => ({
      ...item,
      pct: total > 0 ? ((item.count / total) * 100).toFixed(1) : '0.0',
    }))
})

function platformCodeLabel(code: number) {
  const opt = PLATFORM_CODE_OPTIONS.find((o) => o.value === code)
  if (!opt) return String(code)
  return t(`ecommerce.platform.codes.${opt.labelKey}`)
}

function toggleGroup(channelType: string) {
  const next = new Set(collapsedGroups.value)
  if (next.has(channelType)) next.delete(channelType)
  else next.add(channelType)
  collapsedGroups.value = next
}

function syncDefaultCollapsedGroups() {
  const groups = visibleChannelGroups.value
  if (!groups.length) {
    collapsedGroups.value = new Set()
    return
  }
  const firstType = groups[0].channelType
  collapsedGroups.value = new Set(
    groups.filter((g) => g.channelType !== firstType).map((g) => g.channelType),
  )
}

function renderChart() {
  const el = chartRef.value
  if (!el) return
  if (!chart) chart = echarts.init(el)

  const data = [
    {
      name: t('ecommerce.platform.online'),
      value: stats.value.online,
      itemStyle: { color: CHANNEL_COLORS.ONLINE },
    },
    {
      name: t('ecommerce.platform.offline'),
      value: stats.value.offline,
      itemStyle: { color: CHANNEL_COLORS.OFFLINE },
    },
  ].filter((d) => d.value > 0)

  chart.setOption({
    animation: true,
    tooltip: {
      trigger: 'item',
      borderWidth: 0,
      padding: 0,
      backgroundColor: 'transparent',
      extraCssText: 'box-shadow:none;',
      formatter: (params: unknown) => formatChannelPieTooltip(params),
    },
    series: [
      {
        type: 'pie',
        radius: ['50%', '80%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: true,
        label: { show: false },
        data: data.length
          ? data
          : [{ name: t('ecommerce.platform.emptyPlatforms'), value: 1, itemStyle: { color: '#e5e7eb' } }],
      },
    ],
  })
}

function formatChannelPieTooltip(params: unknown) {
  const p = params as {
    name: string
    value: number
    percent?: number
    color?: string
  }
  const color = p.color ?? '#64748b'
  const pct = typeof p.percent === 'number' ? p.percent.toFixed(1) : '0.0'
  return [
    '<div style="',
    `background:${color};`,
    'padding:8px 12px;',
    'border-radius:8px;',
    'color:#fff;',
    'font-size:13px;',
    'font-weight:600;',
    'box-shadow:0 4px 12px rgba(0,0,0,0.18);',
    '">',
    `${p.name}: ${p.value} (${pct}%)`,
    '</div>',
  ].join('')
}

function openCreate() {
  editingPlatform.value = null
  dialogVisible.value = true
}

function openEdit(row: EcPlatform) {
  editingPlatform.value = { ...row }
  dialogVisible.value = true
}

async function onPlatformSaved() {
  await loadAll()
  emit('changed')
}

async function onDelete(row: EcPlatform) {
  await ElMessageBox.confirm(t('ecommerce.platform.deleteConfirm', { name: row.name }), { type: 'warning' })
  await deletePlatform(row.id)
  ElMessage.success(t('ecommerce.common.deleted'))
  await loadAll()
  emit('changed')
}

function onPlatformCommand(cmd: string, platform: EcPlatform) {
  if (cmd === 'edit') openEdit(platform)
  else if (cmd === 'delete') onDelete(platform)
}

async function loadAll() {
  loading.value = true
  try {
    const [platformResult, shops] = await Promise.all([
      fetchPlatforms(undefined, undefined, { page: 1, pageSize: 500 }),
      fetchShopOptions(),
    ])
    allPlatforms.value = platformResult.records
    const counts = new Map<number, number>()
    for (const shop of shops) {
      counts.set(shop.platformId, (counts.get(shop.platformId) ?? 0) + 1)
    }
    shopCountMap.value = counts
    await nextTick()
    renderChart()
  } finally {
    loading.value = false
  }
}

function onOpen() {
  loadAll()
}

function onClosed() {
  keyword.value = ''
  activeChannel.value = null
}

function onResize() {
  chart?.resize()
}

watch(allPlatforms, () => {
  nextTick(renderChart)
})

watch([visibleChannelGroups, activeChannel, keyword], () => {
  syncDefaultCollapsedGroups()
})

watch(visible, (open) => {
  if (open) {
    window.addEventListener('resize', onResize)
  } else {
    window.removeEventListener('resize', onResize)
    chart?.dispose()
    chart = null
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  chart?.dispose()
  chart = null
})
</script>

<style scoped lang="scss">
.platform-manage-drawer {
  :deep(.el-drawer__body) {
    padding: 0 20px 20px;
    overflow-y: auto;
  }
}

.platform-dashboard {
  position: relative;
  padding-bottom: 72px;
}

.platform-dashboard__subtitle {
  margin: 0 0 16px;
  font-size: 13px;
  color: var(--wr-muted, #999);
}

.platform-hero {
  display: flex;
  gap: 20px;
  padding: 20px 24px;
  margin-bottom: 16px;
  border-radius: 16px;
  background: linear-gradient(135deg, #1d4ed8 0%, #2563eb 45%, #3b82f6 100%);
  color: #fff;
  min-height: 140px;
}

.platform-hero__stats {
  flex: 1;
  display: flex;
  align-items: stretch;
}

.platform-hero__stat {
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

.platform-hero__stat-icon {
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

.platform-hero__stat-body {
  min-width: 0;
}

.platform-hero__stat-label {
  font-size: 13px;
  opacity: 0.85;
}

.platform-hero__stat-value {
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

  &.is-online {
    color: #fca5a5;
  }

  &.is-offline {
    color: #86efac;
  }
}

.platform-hero__divider {
  width: 1px;
  align-self: stretch;
  flex-shrink: 0;
  margin: 4px 16px;
  background: rgb(255 255 255 / 35%);
}

.platform-hero__chart-wrap {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 260px;
}

.platform-hero__chart-body {
  display: flex;
  align-items: center;
  gap: 16px;
}

.platform-hero__chart {
  position: relative;
  width: 130px;
  height: 130px;
  flex-shrink: 0;
}

.platform-hero__chart-canvas {
  width: 100%;
  height: 100%;
}

.platform-hero__chart-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.platform-hero__chart-center-value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.1;
  color: #fff;
}

.platform-hero__chart-center-label {
  margin-top: 2px;
  font-size: 11px;
  color: rgb(255 255 255 / 80%);
}

.platform-hero__chart-legend {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.platform-hero__legend-item {
  display: grid;
  grid-template-columns: 8px auto auto;
  align-items: center;
  column-gap: 6px;
  font-size: 12px;
  line-height: 1.4;
  color: rgb(255 255 255 / 95%);
  text-align: left;
}

.platform-hero__legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.platform-hero__legend-name {
  white-space: nowrap;
}

.platform-hero__legend-value {
  color: rgb(255 255 255 / 85%);
  white-space: nowrap;
}

.platform-dashboard__toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.platform-dashboard__search {
  width: 220px;
  flex-shrink: 0;
}

.platform-filter-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.platform-filter-chip {
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

.platform-filter-chip__count {
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

.platform-groups {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.platform-group {
  border: 1px solid var(--wr-border, #e8ecef);
  border-radius: 14px;
  background: var(--wr-card, #fff);
  overflow: hidden;
}

.platform-group__header {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 14px 16px;
  border: none;
  background: transparent;
  cursor: pointer;
  text-align: left;
}

.platform-group__badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;

  &.is-online {
    background: #ecfdf5;
    color: #059669;
  }

  &.is-offline {
    background: #f5f3ff;
    color: #7c3aed;
  }
}

.platform-group__count {
  font-size: 13px;
  color: var(--wr-muted, #999);
}

.platform-group__arrow {
  margin-left: auto;
  transition: transform 0.2s;

  &.is-collapsed {
    transform: rotate(-90deg);
  }
}

.platform-group__body {
  padding: 12px 16px 16px;
}

.platform-tiles {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.platform-tile {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 260px;
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

.platform-tile__icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  flex-shrink: 0;
  object-fit: contain;
  background: #f3f4f6;
  padding: 4px;

  &.is-avatar {
    object-fit: cover;
    padding: 0;
    border-radius: 50%;
  }
}

.platform-tile__main {
  flex: 1;
  min-width: 0;
}

.platform-tile__name {
  font-size: 14px;
  font-weight: 600;
  color: var(--wr-text, #333);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.platform-tile__shops {
  margin-top: 4px;
}

.platform-tile__shops-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.5;
  background: var(--wr-stat-blue-bg, #eff6ff);
  color: var(--wr-stat-blue, #2563eb);
}

.platform-tile__shops-empty {
  font-size: 12px;
  color: var(--wr-muted, #999);
}

.platform-tile__status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  font-size: 12px;
  font-weight: 500;
  line-height: 1;

  &.is-enabled {
    color: #16a34a;
  }

  &.is-disabled-status {
    color: #ea580c;
  }
}

.platform-tile__status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
  background: currentColor;
}

.platform-tile__menu {
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

.platform-tile__menu-icon {
  width: 18px;
  height: 18px;
  display: block;
}

.platform-tile__delete {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #ef4444;
}

.platform-fab {
  position: fixed;
  right: 40px;
  bottom: 32px;
  z-index: 2010;
  height: 44px;
  padding: 0 20px;
  border-radius: 22px;
  font-weight: 600;
  box-shadow: 0 8px 24px rgb(37 99 235 / 35%);
}

@media (max-width: 900px) {
  .platform-hero {
    flex-direction: column;
  }

  .platform-hero__stats {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 0;
  }

  .platform-hero__stat {
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

  .platform-hero__divider {
    width: 100%;
    height: 1px;
    margin: 8px 0;
  }

  .platform-hero__chart-wrap {
    width: 100%;
    min-width: 0;
  }

  .platform-tile {
    width: 100%;
  }
}
</style>
