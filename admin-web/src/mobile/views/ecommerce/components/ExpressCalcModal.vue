<template>
  <MobileBottomSheet v-model="localVisible">
    <template #header>
      <div class="express-calc-modal__header">
        <img :src="schemeAAssets.starYellow" class="express-calc-modal__header-icon" alt="" />
        <span class="express-calc-modal__header-title">🧮 运费试算</span>
      </div>
    </template>

    <template v-if="allStations.length">
      <SchemeADoodleFrame
        color="#f97316"
        :shadow="false"
        class="express-calc-modal__calc-box"
      >
        <div class="express-calc-modal__calc-form">
          <div class="express-calc-modal__calc-row">
            <label class="express-calc-modal__calc-label">📦 快递公司</label>
            <select v-model.number="calcStationId" class="express-calc-modal__calc-select">
              <option v-for="s in allStations" :key="s.id" :value="s.id">
                {{ s.name }}{{ s.isDefault ? '（默认）' : '' }}
              </option>
            </select>
          </div>
          <div class="express-calc-modal__calc-row">
            <label class="express-calc-modal__calc-label">📍 省份</label>
            <select v-model="calcProvince" class="express-calc-modal__calc-select">
              <option value="">请选择省份</option>
              <option v-for="p in availableProvinces" :key="p" :value="p">{{ p }}</option>
            </select>
          </div>
          <div class="express-calc-modal__calc-row express-calc-modal__calc-row--dims">
            <label class="express-calc-modal__calc-label">📏 尺寸(cm)</label>
            <div class="express-calc-modal__calc-dims">
              <div class="express-calc-modal__calc-dim">
                <input
                  v-model.number="calcLength"
                  type="number"
                  min="0"
                  step="0.1"
                  placeholder="长"
                  class="express-calc-modal__calc-input"
                />
                <span class="express-calc-modal__calc-dim-x">×</span>
                <input
                  v-model.number="calcWidth"
                  type="number"
                  min="0"
                  step="0.1"
                  placeholder="宽"
                  class="express-calc-modal__calc-input"
                />
                <span class="express-calc-modal__calc-dim-x">×</span>
                <input
                  v-model.number="calcHeight"
                  type="number"
                  min="0"
                  step="0.1"
                  placeholder="高"
                  class="express-calc-modal__calc-input"
                />
              </div>
            </div>
          </div>
          <div class="express-calc-modal__calc-row">
            <label class="express-calc-modal__calc-label">⚖️ 实际重量(kg)</label>
            <input
              v-model.number="calcWeight"
              type="number"
              min="0"
              step="0.01"
              placeholder="选填，默认按体积重计算"
              class="express-calc-modal__calc-textinput"
            />
          </div>
          <button
            type="button"
            class="express-calc-modal__calc-submit"
            :disabled="calcLoading || !calcProvince || !calcStationId"
            @click="handleCalculate"
          >
            {{ calcLoading ? '计算中...' : '🧮 开始试算' }}
          </button>
        </div>

        <!-- 试算结果 -->
        <div v-if="calcResult" class="express-calc-modal__calc-result">
          <div class="express-calc-modal__calc-result-title">
            <img :src="schemeAAssets.starYellow" class="express-calc-modal__calc-result-star" alt="" />
            试算结果
          </div>
          <div class="express-calc-modal__calc-result-rows">
            <div class="express-calc-modal__calc-result-row">
              <span>体积重</span>
              <span>{{ formatWeight(calcResult.volumetricWeight) }} kg</span>
            </div>
            <div v-if="calcResult.actualWeight != null" class="express-calc-modal__calc-result-row">
              <span>实际重量</span>
              <span>{{ formatWeight(calcResult.actualWeight) }} kg</span>
            </div>
            <div class="express-calc-modal__calc-result-row">
              <span>计费重量</span>
              <span class="express-calc-modal__calc-result-strong">
                {{ formatWeight(calcResult.billingWeight) }} kg
              </span>
            </div>
            <div class="express-calc-modal__calc-result-row">
              <span>计费档位</span>
              <span>{{ calcResult.tier }}</span>
            </div>
            <div class="express-calc-modal__calc-result-row">
              <span>运费</span>
              <span>¥{{ formatPrice(calcResult.freight) }}</span>
            </div>
            <div v-if="calcResult.labelPrice > 0" class="express-calc-modal__calc-result-row">
              <span>面单费</span>
              <span>¥{{ formatPrice(calcResult.labelPrice) }}</span>
            </div>
            <div class="express-calc-modal__calc-result-row express-calc-modal__calc-result-row--total">
              <span>预计总计</span>
              <span class="express-calc-modal__calc-result-total">¥{{ formatPrice(calcResult.total) }}</span>
            </div>
          </div>
          <div v-if="calcResult.warning" class="express-calc-modal__calc-result-warning">
            ⚠️ {{ calcResult.warning }}
          </div>
        </div>
      </SchemeADoodleFrame>
    </template>

    <div v-else class="express-calc-modal__loading">
      加载中...
    </div>
  </MobileBottomSheet>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import MobileBottomSheet from '@/mobile/components/MobileBottomSheet.vue'
import SchemeADoodleFrame from '@/mobile/views/home/themes/scheme-a/SchemeADoodleFrame.vue'
import { schemeAAssets } from '@/mobile/views/home/themes/scheme-a/assets'
import {
  fetchExpressStation,
  fetchExpressStations,
  type EcExpressStation,
  type EcExpressPrice,
} from '@/api/ecommerce/express'

const VOLUMETRIC_DIVISOR = 6000

const props = defineProps<{
  modelValue: boolean
  defaultStationId?: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const localVisible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

// ===== 试算状态 =====
const allStations = ref<EcExpressStation[]>([])
const stationDetailCache = ref<Map<number, EcExpressStation>>(new Map())
const calcStationId = ref<number | null>(null)
const calcProvince = ref('')
const calcLength = ref<number | null>(null)
const calcWidth = ref<number | null>(null)
const calcHeight = ref<number | null>(null)
const calcWeight = ref<number | null>(null)
const calcLoading = ref(false)

interface CalcResult {
  volumetricWeight: number
  actualWeight: number | null
  billingWeight: number
  tier: string
  freight: number
  labelPrice: number
  total: number
  warning?: string
}
const calcResult = ref<CalcResult | null>(null)

const calcStationDetail = computed<EcExpressStation | null>(() => {
  if (calcStationId.value == null) return null
  return stationDetailCache.value.get(calcStationId.value) ?? null
})

const availableProvinces = computed(() => {
  if (!calcStationDetail.value?.prices?.length) return []
  return calcStationDetail.value.prices.map((p) => p.provinceName).sort()
})

// 弹窗打开时加载数据
watch(localVisible, async (visible) => {
  if (!visible) return

  // 重置表单
  calcStationId.value = null
  calcProvince.value = ''
  calcLength.value = null
  calcWidth.value = null
  calcHeight.value = null
  calcWeight.value = null
  calcResult.value = null

  // 加载所有快递公司
  try {
    const page = await fetchExpressStations(undefined, { page: 1, size: 100 })
    allStations.value = page.records || []

    // 默认选中传进来的 stationId，否则选第一个
    if (props.defaultStationId && allStations.value.some((s) => s.id === props.defaultStationId)) {
      calcStationId.value = props.defaultStationId
    } else if (allStations.value.length > 0) {
      calcStationId.value = allStations.value[0].id
    }

    // 加载选中快递公司的详情
    if (calcStationId.value != null) {
      await loadStationDetail(calcStationId.value)
    }
  } catch {
    allStations.value = []
  }
})

// 切换快递公司时加载详情
watch(calcStationId, async (newId, oldId) => {
  if (newId == null || newId === oldId) return
  await loadStationDetail(newId)
  // 清空已选省份
  const provinces = stationDetailCache.value.get(newId)?.prices?.map((p) => p.provinceName) ?? []
  if (calcProvince.value && !provinces.includes(calcProvince.value)) {
    calcProvince.value = ''
  }
  calcResult.value = null
})

async function loadStationDetail(stationId: number) {
  if (stationDetailCache.value.has(stationId)) return
  try {
    const detail = await fetchExpressStation(stationId)
    stationDetailCache.value.set(stationId, detail)
  } catch {
    // ignore
  }
}

async function handleCalculate() {
  if (calcStationId.value == null || !calcProvince.value) return
  if (calcLength.value == null || calcWidth.value == null || calcHeight.value == null) return
  if (calcLength.value <= 0 || calcWidth.value <= 0 || calcHeight.value <= 0) return

  calcLoading.value = true
  try {
    if (!stationDetailCache.value.has(calcStationId.value)) {
      const detail = await fetchExpressStation(calcStationId.value)
      stationDetailCache.value.set(calcStationId.value, detail)
    }
    const station = stationDetailCache.value.get(calcStationId.value)
    if (!station) {
      calcResult.value = null
      return
    }
    const price = station.prices?.find((p) => p.provinceName === calcProvince.value)
    if (!price) {
      calcResult.value = null
      return
    }
    calcResult.value = computeFreight(
      calcLength.value,
      calcWidth.value,
      calcHeight.value,
      calcWeight.value,
      price,
      station.labelPrice ?? 0,
    )
  } finally {
    calcLoading.value = false
  }
}

function computeFreight(
  length: number,
  width: number,
  height: number,
  actualWeight: number | null,
  price: EcExpressPrice,
  labelPrice: number,
): CalcResult {
  const volumetricWeight = (length * width * height) / VOLUMETRIC_DIVISOR
  const billingWeight = actualWeight != null && actualWeight > 0
    ? Math.max(actualWeight, volumetricWeight)
    : volumetricWeight

  let freight = 0
  let tier = ''
  const warnings: string[] = []

  if (billingWeight <= 0.3) {
    freight = price.priceW03Kg ?? 0
    tier = '≤0.3kg'
    if (price.priceW03Kg == null) warnings.push('该地区未配置 ≤0.3kg 价格')
  } else if (billingWeight <= 0.5) {
    freight = price.priceW05Kg ?? 0
    tier = '≤0.5kg'
    if (price.priceW05Kg == null) warnings.push('该地区未配置 ≤0.5kg 价格')
  } else if (billingWeight <= 1) {
    freight = price.priceW1Kg ?? 0
    tier = '≤1kg'
    if (price.priceW1Kg == null) warnings.push('该地区未配置 ≤1kg 价格')
  } else if (billingWeight <= 1.5) {
    freight = price.priceW15Kg ?? 0
    tier = '≤1.5kg'
    if (price.priceW15Kg == null) warnings.push('该地区未配置 ≤1.5kg 价格')
  } else if (billingWeight <= 2) {
    freight = price.priceW2Kg ?? 0
    tier = '≤2kg'
    if (price.priceW2Kg == null) warnings.push('该地区未配置 ≤2kg 价格')
  } else if (billingWeight <= 2.5) {
    freight = price.priceW25Kg ?? 0
    tier = '≤2.5kg'
    if (price.priceW25Kg == null) warnings.push('该地区未配置 ≤2.5kg 价格')
  } else if (billingWeight <= 3) {
    freight = price.priceW3Kg ?? 0
    tier = '≤3kg'
    if (price.priceW3Kg == null) warnings.push('该地区未配置 ≤3kg 价格')
  } else {
    if (price.over3FirstPrice == null) {
      warnings.push('该地区未配置续重价格，无法计算 >3kg 运费')
      tier = '>3kg'
    } else {
      const over = billingWeight - 3
      const additionalKg = Math.ceil(over)
      const additionalPrice = price.over3AdditionalPrice ?? 0
      freight = price.over3FirstPrice + additionalKg * additionalPrice
      tier = `>3kg（首重¥${price.over3FirstPrice.toFixed(2)} + 续重${additionalKg}kg×¥${additionalPrice.toFixed(2)}）`
    }
  }

  const total = freight + labelPrice
  return {
    volumetricWeight,
    actualWeight: actualWeight ?? null,
    billingWeight,
    tier,
    freight,
    labelPrice,
    total,
    warning: warnings.length ? warnings.join('；') : undefined,
  }
}

function formatPrice(price?: number | null): string {
  if (price == null) return '0.00'
  return Number(price).toFixed(2)
}

function formatWeight(weight: number): string {
  return Number(weight).toFixed(3)
}
</script>

<style scoped lang="scss">
.express-calc-modal__header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.express-calc-modal__header-icon {
  width: 22px;
  height: 22px;
}

.express-calc-modal__header-title {
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 18px;
  font-weight: 800;
  color: #1e293b;
}

.express-calc-modal__loading {
  text-align: center;
  padding: 40px 20px;
  color: #94a3b8;
  font-size: 14px;
}

.express-calc-modal__calc-box {
  :deep(.sa-doodle-frame__body) {
    padding: 14px;
    background: #fff7ed;
  }
}

.express-calc-modal__calc-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.express-calc-modal__calc-row {
  display: flex;
  flex-direction: column;
  gap: 4px;

  &--dims {
    gap: 6px;
  }
}

.express-calc-modal__calc-label {
  font-size: 12px;
  font-weight: 700;
  color: #9a3412;
}

.express-calc-modal__calc-select,
.express-calc-modal__calc-textinput {
  border: 1.5px dashed #fed7aa;
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 14px;
  background: #fff;
  color: #1e293b;
  font-family: inherit;
  outline: none;

  &:focus {
    border-color: #f97316;
    border-style: solid;
  }
}

.express-calc-modal__calc-select {
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 12 12'%3E%3Cpath fill='%23f97316' d='M2 4l4 4 4-4z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 10px center;
  background-size: 12px;
  padding-right: 28px;
}

.express-calc-modal__calc-dims {
  display: flex;
  align-items: center;
  gap: 6px;
}

.express-calc-modal__calc-dim {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
}

.express-calc-modal__calc-input {
  flex: 1;
  min-width: 0;
  border: 1.5px dashed #fed7aa;
  border-radius: 8px;
  padding: 8px 6px;
  font-size: 14px;
  background: #fff;
  color: #1e293b;
  font-family: inherit;
  outline: none;
  text-align: center;

  &:focus {
    border-color: #f97316;
    border-style: solid;
  }
}

.express-calc-modal__calc-dim-x {
  color: #f97316;
  font-weight: 700;
  font-size: 14px;
}

.express-calc-modal__calc-submit {
  margin-top: 4px;
  border: none;
  border-radius: 999px;
  padding: 10px 16px;
  font-size: 14px;
  font-weight: 800;
  background: #f97316;
  color: #fff;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s;

  &:active {
    transform: scale(0.97);
  }

  &:disabled {
    background: #fed7aa;
    cursor: not-allowed;
  }
}

.express-calc-modal__calc-result {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1.5px dashed #fed7aa;
}

.express-calc-modal__calc-result-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-family: 'ZCOOL KuaiLe', sans-serif;
  font-size: 14px;
  font-weight: 700;
  color: #9a3412;
  margin-bottom: 8px;
}

.express-calc-modal__calc-result-star {
  width: 18px;
  height: 18px;
}

.express-calc-modal__calc-result-rows {
  display: flex;
  flex-direction: column;
  gap: 6px;
  background: #fff;
  border-radius: 10px;
  padding: 10px 12px;
  border: 1.5px dashed #fed7aa;
}

.express-calc-modal__calc-result-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #64748b;

  &--total {
    margin-top: 4px;
    padding-top: 8px;
    border-top: 1.5px dashed #fed7aa;
    font-size: 14px;
    color: #1e293b;
  }
}

.express-calc-modal__calc-result-strong {
  font-weight: 800;
  color: #f97316;
}

.express-calc-modal__calc-result-total {
  font-size: 18px;
  font-weight: 800;
  color: #f97316;
}

.express-calc-modal__calc-result-warning {
  margin-top: 8px;
  padding: 8px 10px;
  background: #fef2f2;
  border-radius: 8px;
  border: 1.5px dashed #fecaca;
  font-size: 11px;
  color: #ef4444;
  font-weight: 600;
}
</style>
