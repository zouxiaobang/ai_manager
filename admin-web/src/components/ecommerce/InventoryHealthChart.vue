<template>
  <div class="inventory-health-chart">
    <div class="inventory-health-chart__head">
      <h3 class="inventory-health-chart__title">{{ t('ecommerce.inventory.healthTitle') }}</h3>
      <el-tooltip :content="t('ecommerce.inventory.healthHint')" placement="top">
        <el-icon class="inventory-health-chart__info"><InfoFilled /></el-icon>
      </el-tooltip>
    </div>

    <div class="inventory-health-chart__body">
      <div ref="chartRef" class="inventory-health-chart__donut" />

      <div class="inventory-health-chart__legend">
        <div v-for="item in legendItems" :key="item.key" class="inventory-health-chart__legend-row">
          <span class="inventory-health-chart__dot" :style="{ background: item.color }" />
          <span class="inventory-health-chart__legend-label">{{ item.label }}</span>
          <span class="inventory-health-chart__legend-pct">{{ item.pct }}%</span>
          <span class="inventory-health-chart__legend-count">({{ item.count }})</span>
        </div>
      </div>
    </div>

    <div class="inventory-health-chart__footer">
      <el-icon class="inventory-health-chart__footer-icon"><CircleCheckFilled /></el-icon>
      <span>{{ t('ecommerce.inventory.healthFooter') }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import * as echarts from 'echarts'
import { CircleCheckFilled, InfoFilled } from '@element-plus/icons-vue'
import {
  computeInventoryHealthScore,
  computeInventoryStats,
  inventoryStatusPercent,
  type InventoryStatusKey,
} from '@/utils/inventoryStats'
import type { EcInventory } from '@/api/ecommerce/inventory'
import { useEcSettingsStore } from '@/stores/ecSettings'

const props = defineProps<{ items: EcInventory[] }>()

const { t } = useI18n()
const ecSettings = useEcSettingsStore()
const chartRef = ref<HTMLElement | null>(null)
let chart: echarts.ECharts | null = null

const classificationOptions = computed(() => ({
  defaultAlertThreshold: ecSettings.inventory.defaultAlertThreshold,
  slowMovingDays: ecSettings.inventory.slowMovingDays,
  slowMovingFallbackDays: ecSettings.inventory.slowMovingFallbackDays,
}))

const stats = computed(() => computeInventoryStats(props.items, classificationOptions.value))
const healthScore = computed(() => computeInventoryHealthScore(stats.value))

const legendItems = computed(() => {
  const s = stats.value
  const rows: Array<{ key: InventoryStatusKey; label: string; color: string; count: number; pct: number }> = [
    { key: 'normal', label: t('ecommerce.home.inventoryNormal'), color: '#2563eb', count: s.normal, pct: 0 },
    { key: 'low', label: t('ecommerce.home.inventoryLow'), color: '#ea580c', count: s.low, pct: 0 },
    { key: 'zero', label: t('ecommerce.home.inventoryZeroShort'), color: '#dc2626', count: s.zero, pct: 0 },
    { key: 'slow', label: t('ecommerce.home.inventorySlow'), color: '#9ca3af', count: s.slow, pct: 0 },
  ]
  for (const row of rows) {
    row.pct = inventoryStatusPercent(s, row.key)
  }
  return rows
})

function renderChart() {
  const el = chartRef.value
  if (!el) return
  if (!chart) chart = echarts.init(el)

  const score = healthScore.value
  const centerFormatter = () => `{score|${score}}\n{label|${t('ecommerce.inventory.healthScore')}}`
  const centerRich = {
    score: { fontSize: 30, fontWeight: 700, color: '#111827', lineHeight: 36 },
    label: { fontSize: 12, color: '#6b7280', lineHeight: 18 },
  }

  const segments = [
    { value: stats.value.normal, name: t('ecommerce.home.inventoryNormal'), color: '#2563eb' },
    { value: stats.value.low, name: t('ecommerce.home.inventoryLow'), color: '#ea580c' },
    { value: stats.value.zero, name: t('ecommerce.home.inventoryZeroShort'), color: '#dc2626' },
    { value: stats.value.slow, name: t('ecommerce.home.inventorySlow'), color: '#9ca3af' },
  ].filter((item) => item.value > 0)

  chart.setOption(
    {
      animation: true,
      tooltip: { trigger: 'item' },
      series: [
        {
          type: 'pie',
          radius: ['62%', '82%'],
          center: ['50%', '50%'],
          avoidLabelOverlap: true,
          label: {
            show: true,
            position: 'center',
            formatter: centerFormatter,
            rich: centerRich,
          },
          emphasis: {
            scale: true,
            label: { show: true, formatter: centerFormatter, rich: centerRich },
          },
          labelLine: { show: false },
          data: segments.length
            ? segments.map((item) => ({
                value: item.value,
                name: item.name,
                itemStyle: { color: item.color },
              }))
            : [{ value: 1, name: t('ecommerce.inventory.noData'), itemStyle: { color: '#e5e7eb' } }],
        },
      ],
    },
    true,
  )
}

function onResize() {
  chart?.resize()
}

watch([() => props.items, healthScore], () => renderChart(), { deep: true })

onMounted(() => {
  renderChart()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  chart?.dispose()
  chart = null
})
</script>

<style scoped lang="scss">
.inventory-health-chart {
  display: flex;
  flex-direction: column;
  min-height: 100%;
  padding: 16px 18px;
  border: 1px solid #e8ecf2;
  border-radius: 12px;
  background: #fff;
}

.inventory-health-chart__head {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
}

.inventory-health-chart__title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}

.inventory-health-chart__info {
  font-size: 15px;
  color: #9ca3af;
  cursor: help;
}

.inventory-health-chart__body {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.inventory-health-chart__donut {
  width: 148px;
  height: 148px;
  flex-shrink: 0;
}

.inventory-health-chart__legend {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.inventory-health-chart__legend-row {
  display: grid;
  grid-template-columns: 10px 1fr auto auto;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.inventory-health-chart__dot {
  width: 10px;
  height: 10px;
  border-radius: 2px;
}

.inventory-health-chart__legend-label {
  color: #374151;
}

.inventory-health-chart__legend-pct {
  font-weight: 600;
  color: #111827;
  text-align: right;
}

.inventory-health-chart__legend-count {
  color: #9ca3af;
  font-size: 12px;
}

.inventory-health-chart__footer {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f2f5;
  font-size: 12px;
  color: #6b7280;
}

.inventory-health-chart__footer-icon {
  color: #16a34a;
  font-size: 14px;
}
</style>
