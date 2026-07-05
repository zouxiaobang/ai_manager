import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import {
  calculateCarton,
  type EcCarton,
  type EcCartonCalculateResult,
} from '@/api/ecommerce/carton'
import { fetchFactoryOptions, type EcFactory } from '@/api/ecommerce/factory'
import { computeCartonFitSlack } from '@/utils/cartonMatch'

export const DEFAULT_PADDING_CM = 2

export function formatCartonSize(row: Pick<EcCarton, 'lengthCm' | 'widthCm' | 'heightCm'>) {
  const { lengthCm, widthCm, heightCm } = row
  if (lengthCm == null && widthCm == null && heightCm == null) return '—'
  const parts = [lengthCm, widthCm, heightCm].map((v) => (v == null ? '—' : Number(v).toFixed(2)))
  return `${parts[0]} × ${parts[1]} × ${parts[2]} cm`
}

export function formatDimNumber(value: number) {
  return Number(value).toFixed(2)
}

export function formatUnitPrice(unitPrice: number | null | undefined) {
  return unitPrice != null ? `¥${Number(unitPrice).toFixed(2)}` : '-'
}

export function useMobileCartonCalculate() {
  const { t } = useI18n()

  const factoryOptions = ref<EcFactory[]>([])
  const calculating = ref(false)
  const calcResult = ref<EcCartonCalculateResult | null>(null)

  const calcForm = reactive<{
    lengthCm: number | null
    widthCm: number | null
    heightCm: number | null
    padLengthCm: number
    padWidthCm: number
    padHeightCm: number
    factoryId?: number | ''
  }>({
    lengthCm: null,
    widthCm: null,
    heightCm: null,
    padLengthCm: DEFAULT_PADDING_CM,
    padWidthCm: DEFAULT_PADDING_CM,
    padHeightCm: DEFAULT_PADDING_CM,
    factoryId: '' as number | '',
  })

  const calcMatchedDisplay = computed(() => {
    if (!calcResult.value) return null
    const carton = calcResult.value.matchedCarton
    if (!carton) return { empty: true as const }

    const padL = calcForm.padLengthCm ?? DEFAULT_PADDING_CM
    const padW = calcForm.padWidthCm ?? DEFAULT_PADDING_CM
    const padH = calcForm.padHeightCm ?? DEFAULT_PADDING_CM
    const reqL = (calcForm.lengthCm ?? 0) + padL
    const reqW = (calcForm.widthCm ?? 0) + padW
    const reqH = (calcForm.heightCm ?? 0) + padH
    const cartonL = Number(carton.lengthCm) || 0
    const cartonW = Number(carton.widthCm) || 0
    const cartonH = Number(carton.heightCm) || 0

    const fit = computeCartonFitSlack(reqL, reqW, reqH, cartonL, cartonW, cartonH)
    if (!fit) {
      return { empty: false as const, carton, slackL: 0, slackW: 0, slackH: 0, fitsWell: false }
    }

    return {
      empty: false as const,
      carton,
      slackL: fit.slackL,
      slackW: fit.slackW,
      slackH: fit.slackH,
      fitsWell: fit.fitsWell,
    }
  })

  async function loadFactoryOptions() {
    if (factoryOptions.value.length > 0) return
    factoryOptions.value = await fetchFactoryOptions('CARTON')
  }

  function resetCalc() {
    calcForm.lengthCm = null
    calcForm.widthCm = null
    calcForm.heightCm = null
    calcForm.padLengthCm = DEFAULT_PADDING_CM
    calcForm.padWidthCm = DEFAULT_PADDING_CM
    calcForm.padHeightCm = DEFAULT_PADDING_CM
    calcForm.factoryId = ''
    calcResult.value = null
  }

  async function runCalculate() {
    if (
      calcForm.lengthCm == null ||
      calcForm.widthCm == null ||
      calcForm.heightCm == null ||
      calcForm.lengthCm <= 0 ||
      calcForm.widthCm <= 0 ||
      calcForm.heightCm <= 0
    ) {
      ElMessage.warning(t('ecommerce.carton.productSizeRequired'))
      return
    }

    calculating.value = true
    try {
      const padL = calcForm.padLengthCm ?? DEFAULT_PADDING_CM
      const padW = calcForm.padWidthCm ?? DEFAULT_PADDING_CM
      const padH = calcForm.padHeightCm ?? DEFAULT_PADDING_CM
      calcResult.value = await calculateCarton(
        calcForm.lengthCm + padL,
        calcForm.widthCm + padW,
        calcForm.heightCm + padH,
        calcForm.factoryId === '' ? undefined : calcForm.factoryId,
      )
    } finally {
      calculating.value = false
    }
  }

  return {
    factoryOptions,
    calculating,
    calcResult,
    calcForm,
    calcMatchedDisplay,
    loadFactoryOptions,
    resetCalc,
    runCalculate,
    t,
  }
}
