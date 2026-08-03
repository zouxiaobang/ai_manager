import { computed, nextTick, ref, watch } from 'vue'
import type { EcExpressStation } from '@/api/ecommerce/express'
import { computeCalcFreight, computeVolumeWeight, type CalcResult } from '../expressCalc'

/**
 * 运费试算弹窗依赖：站点列表/详情拉取注入，保持弹窗编排状态机可脱离组件单测。
 */
export interface ExpressCalcDialogDeps {
  /** 拉取站点列表（组件侧包装默认参数，如 page=1&size=100） */
  fetchExpressStations: () => Promise<{ records?: EcExpressStation[] }>
  fetchExpressStation: (id: number) => Promise<EcExpressStation>
}

/**
 * 运费试算弹窗状态机：站点/省份/尺寸/重量/结果状态 + 打开预置 + 站点切换缓存 + 计费计算。
 * 站点详情按需拉取并缓存，避免切换与计算时重复请求。
 */
export function useExpressCalcDialog(deps: ExpressCalcDialogDeps) {
  const calcDialogVisible = ref(false)
  const calcStations = ref<EcExpressStation[]>([])
  const calcStationCache = ref(new Map<number, EcExpressStation>())
  const calcStationId = ref<number | null>(null)
  const calcProvince = ref('')
  const calcLength = ref<number | null>(null)
  const calcWidth = ref<number | null>(null)
  const calcHeight = ref<number | null>(null)
  const calcWeight = ref<number | null>(null)
  const calcLoading = ref(false)
  const calcResult = ref<CalcResult | null>(null)

  /** 当前选中站点的缓存详情 */
  const calcStationDetail = computed<EcExpressStation | null>(() => {
    if (calcStationId.value == null) return null
    return calcStationCache.value.get(calcStationId.value) ?? null
  })

  /** 可试算省份 = 当前站点价格矩阵的省份（排序展示） */
  const availableProvinces = computed(() => {
    if (!calcStationDetail.value?.prices?.length) return []
    return calcStationDetail.value.prices.map((p) => p.provinceName).sort()
  })

  /** 打开弹窗：重置状态、拉取站点、默认选中默认快递并预置广东 */
  async function openCalcDialog() {
    calcStationId.value = null
    calcProvince.value = ''
    calcLength.value = null
    calcWidth.value = null
    calcHeight.value = null
    calcWeight.value = null
    calcResult.value = null
    calcLoading.value = false

    try {
      const page = await deps.fetchExpressStations()
      calcStations.value = page.records || []

      // 默认选中默认快递公司
      const defaultStation = calcStations.value.find((s) => s.isDefault)
      if (defaultStation) {
        // 预加载详情到缓存，避免 watch 重复请求
        if (!calcStationCache.value.has(defaultStation.id)) {
          const detail = await deps.fetchExpressStation(defaultStation.id)
          calcStationCache.value.set(defaultStation.id, detail)
        }
        calcStationId.value = defaultStation.id
        // 等 watch 执行完后设置默认省份
        await nextTick()
        const detail = calcStationCache.value.get(defaultStation.id)
        if (detail?.prices?.some((p) => p.provinceName === '广东')) {
          calcProvince.value = '广东'
        }
      } else if (calcStations.value.length > 0) {
        calcStationId.value = calcStations.value[0].id
        await nextTick()
        const detail = calcStationCache.value.get(calcStations.value[0].id)
        if (detail?.prices?.some((p) => p.provinceName === '广东')) {
          calcProvince.value = '广东'
        }
      }
    } catch {
      // 站点拉取失败不打断弹窗，保留空列表由用户手动刷新
      calcStations.value = []
    }
    calcDialogVisible.value = true
  }

  // 切换站点时清空结果与省份，并按需拉取详情入缓存
  watch(calcStationId, async (newId, oldId) => {
    if (newId == null || newId === oldId) return
    calcResult.value = null
    calcProvince.value = ''
    if (calcStationCache.value.has(newId)) return
    try {
      const detail = await deps.fetchExpressStation(newId)
      calcStationCache.value.set(newId, detail)
    } catch {
      // 详情拉取失败静默：计算时会再兜底拉取一次
    }
  })

  /** 执行试算：尺寸/重量校验、体积重换算、命中档位计费 */
  async function handleCalculate() {
    if (calcStationId.value == null || !calcProvince.value) return

    const hasVolume = calcLength.value != null && calcWidth.value != null && calcHeight.value != null
      && calcLength.value > 0 && calcWidth.value > 0 && calcHeight.value > 0
    const hasWeight = calcWeight.value != null && calcWeight.value > 0
    if (!hasVolume && !hasWeight) return

    // 只填体积时按体积重换算为计费重量
    if (hasVolume && !hasWeight) {
      calcWeight.value = computeVolumeWeight(calcLength.value!, calcWidth.value!, calcHeight.value!)
    }

    calcLoading.value = true
    try {
      const stationId = calcStationId.value
      if (!calcStationCache.value.has(stationId)) {
        const detail = await deps.fetchExpressStation(stationId)
        calcStationCache.value.set(stationId, detail)
      }
      const station = calcStationCache.value.get(stationId)
      if (!station) {
        calcResult.value = null
        return
      }
      const price = station.prices?.find((p) => p.provinceName === calcProvince.value)
      if (!price) {
        calcResult.value = null
        return
      }
      calcResult.value = computeCalcFreight(
        calcLength.value ?? 0,
        calcWidth.value ?? 0,
        calcHeight.value ?? 0,
        calcWeight.value!,
        price,
        station.labelPrice ?? 0,
      )
    } finally {
      calcLoading.value = false
    }
  }

  return {
    calcDialogVisible,
    calcStations,
    calcStationId,
    calcProvince,
    calcLength,
    calcWidth,
    calcHeight,
    calcWeight,
    calcLoading,
    calcResult,
    calcStationDetail,
    availableProvinces,
    openCalcDialog,
    handleCalculate,
  }
}
