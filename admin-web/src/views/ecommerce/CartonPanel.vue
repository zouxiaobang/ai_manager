<template>
  <div class="carton-panel">
    <header class="carton-panel__header">
      <div>
        <h2 class="carton-panel__title">{{ t('ecommerce.carton.pageTitle') }}</h2>
        <p class="carton-panel__subtitle">{{ t('ecommerce.carton.pageSubtitle') }}</p>
      </div>
    </header>

    <div class="carton-split">
      <aside class="carton-split__list">
        <div class="carton-list-toolbar">
          <el-input
            v-model="keyword"
            :placeholder="t('ecommerce.carton.searchPlaceholder')"
            clearable
            @keyup.enter="loadCartons"
          />
          <div class="carton-list-toolbar__actions">
            <el-button
              size="default"
              class="carton-list-toolbar__reset"
              :loading="previewBackfillRunning"
              @click="onBackfillPreviewImages"
            >
              {{ t('ecommerce.carton.backfillPreviewImages') }}
            </el-button>
            <el-button
              size="default"
              class="carton-list-toolbar__reset"
              :loading="backfillRunning"
              @click="onResetProductCartons"
            >
              {{ t('ecommerce.carton.resetProductCartons') }}
            </el-button>
            <el-button type="primary" class="carton-list-toolbar__add" :icon="Plus" @click="openCreate">
              {{ t('ecommerce.carton.add') }}
            </el-button>
          </div>
        </div>

        <div v-loading="loading" class="carton-list-scroll">
          <button
            v-for="row in records"
            :key="row.id"
            type="button"
            class="carton-list-item"
            :class="{ 'carton-list-item--active': selectedId === row.id && (detailMode !== 'form' || editingId === row.id) }"
            @click="selectCarton(row)"
          >
            <span
              class="carton-list-item__icon"
              :style="factoryIconStyle(row)"
              aria-hidden="true"
            >
              <el-icon><Box /></el-icon>
            </span>
            <span class="carton-list-item__body">
              <span class="carton-list-item__title">{{ row.name }}</span>
              <span class="carton-list-item__meta">
                <span>{{ row.factoryName || '—' }}</span>
                <span class="carton-list-item__meta-sep">·</span>
                <span>{{ formatSize(row) }}</span>
              </span>
            </span>
            <span
              class="carton-list-item__price"
              :class="{ 'is-empty': row.unitPrice == null }"
              :style="priceTextStyle(row.unitPrice)"
            >
              <CnyAmount :value="row.unitPrice" />
            </span>
          </button>

          <el-empty
            v-if="!loading && records.length === 0"
            :description="t('ecommerce.carton.selectCartonHint')"
            :image-size="72"
          />
        </div>

      </aside>

      <main class="carton-split__detail">
        <div class="carton-detail-main">
          <div v-if="detailMode === 'empty'" class="carton-detail-empty">
            <el-empty :description="t('ecommerce.carton.selectCartonHint')" :image-size="100" />
          </div>

          <div v-else-if="detailMode === 'view' && selectedCarton" class="carton-detail">
            <header class="carton-detail__header">
              <h3 class="carton-detail__title">{{ selectedCarton.name }}</h3>
              <div class="carton-detail__actions">
                <el-button link type="primary" :title="t('ecommerce.carton.edit')" @click="openEdit(selectedCarton)">
                  <el-icon><Edit /></el-icon>
                </el-button>
                <el-button link type="danger" :title="t('ecommerce.carton.delete')" @click="onDelete(selectedCarton)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </header>

            <div class="carton-detail__body">
              <section class="carton-section">
                <h4 class="carton-section__title">{{ t('ecommerce.carton.detailSection') }}</h4>
                <div class="carton-detail-layout">
                  <CartonBoxIllustration
                    :length-cm="selectedCarton.lengthCm ?? undefined"
                    :width-cm="selectedCarton.widthCm ?? undefined"
                    :height-cm="selectedCarton.heightCm ?? undefined"
                    :variant="selectedCarton.illustrationVariant"
                    :preview-image="selectedCarton.previewImage"
                    :preview-version="selectedCarton.updateTime"
                    :seed="selectedCarton.id"
                  />
                  <dl class="carton-info-grid">
                    <div class="carton-info-grid__item">
                      <dt>{{ t('ecommerce.carton.factory') }}</dt>
                      <dd>{{ selectedCarton.factoryName || '—' }}</dd>
                    </div>
                    <div class="carton-info-grid__item">
                      <dt>{{ t('ecommerce.carton.specSize') }}</dt>
                      <dd>{{ formatSize(selectedCarton) }}</dd>
                    </div>
                    <div class="carton-info-grid__item">
                      <dt>{{ t('ecommerce.carton.unitPrice') }}</dt>
                      <dd
                        class="carton-info-grid__price"
                        :class="{ 'is-empty': selectedCarton.unitPrice == null }"
                        :style="priceDetailTextStyle(selectedCarton.unitPrice)"
                      >
                        <CnyAmount :value="selectedCarton.unitPrice" />
                      </dd>
                    </div>
                    <div class="carton-info-grid__item">
                      <dt>{{ t('ecommerce.carton.remark') }}</dt>
                      <dd>{{ selectedCarton.remark || '—' }}</dd>
                    </div>
                    <div class="carton-info-grid__item">
                      <dt>{{ t('ecommerce.carton.updatedAt') }}</dt>
                      <dd>{{ formatDate(selectedCarton.updateTime) }}</dd>
                    </div>
                  </dl>
                </div>
              </section>
            </div>
          </div>

          <div v-else class="carton-detail carton-detail--immersive">
            <div class="carton-immersive__stage">
              <div class="carton-immersive__stage-inner">
                <p class="carton-immersive__rotate-hint">
                  <el-icon><Rank /></el-icon>
                  {{ t('ecommerce.carton.rotateHint') }}
                </p>
                <CartonBox3DPreview
                  ref="preview3dRef"
                  :length-cm="form.lengthCm"
                  :width-cm="form.widthCm"
                  :height-cm="form.heightCm"
                  :illustration-variant="form.illustrationVariant"
                  :seed="editingId ?? 'create'"
                />
                <CartonStylePicker v-model="form.illustrationVariant" />
              </div>
            </div>

            <aside class="carton-immersive__card">
              <header class="carton-immersive__card-header">
                <h3 class="carton-immersive__card-title">
                  {{ editingId ? t('ecommerce.carton.editTitle') : t('ecommerce.carton.createTitle') }}
                </h3>
              </header>

              <div class="carton-immersive__card-body">
                <el-form :model="form" label-position="top" class="carton-form carton-form--immersive" @submit.prevent>
                  <el-form-item :label="t('ecommerce.carton.name')" required>
                    <el-input v-model="form.name" />
                  </el-form-item>
                  <el-form-item :label="t('ecommerce.carton.factory')" required>
                    <el-select
                      v-model="form.factoryId"
                      filterable
                      :placeholder="t('ecommerce.carton.factoryPlaceholder')"
                      style="width: 100%"
                    >
                      <el-option
                        v-for="f in factoryOptions"
                        :key="f.id"
                        :label="f.name"
                        :value="f.id"
                      />
                    </el-select>
                  </el-form-item>
                  <el-form-item :label="t('ecommerce.carton.specSize')">
                    <div class="dim-row">
                      <el-input-number
                        v-model="form.lengthCm"
                        :min="0"
                        :precision="2"
                        controls-position="right"
                        :placeholder="t('ecommerce.carton.length')"
                      />
                      <span>×</span>
                      <el-input-number
                        v-model="form.widthCm"
                        :min="0"
                        :precision="2"
                        controls-position="right"
                        :placeholder="t('ecommerce.carton.width')"
                      />
                      <span>×</span>
                      <el-input-number
                        v-model="form.heightCm"
                        :min="0"
                        :precision="2"
                        controls-position="right"
                        :placeholder="t('ecommerce.carton.height')"
                      />
                      <span class="dim-unit">cm</span>
                    </div>
                  </el-form-item>
                  <el-form-item :label="t('ecommerce.carton.unitPrice')">
                    <el-input-number
                      v-model="form.unitPrice"
                      :min="0"
                      :precision="2"
                      :step="0.1"
                      controls-position="right"
                      style="width: 100%"
                    />
                  </el-form-item>
                  <el-form-item :label="t('ecommerce.carton.remark')">
                    <el-input v-model="form.remark" type="textarea" :rows="2" />
                  </el-form-item>
                </el-form>
              </div>

              <footer class="carton-immersive__card-footer">
                <el-button @click="cancelForm">{{ t('ecommerce.common.cancel') }}</el-button>
                <el-button type="primary" :loading="saving" @click="onSave">{{ t('ecommerce.common.save') }}</el-button>
              </footer>
            </aside>
          </div>
        </div>

        <aside class="carton-detail-tools">
          <section class="carton-calc-panel">
            <header class="carton-calc-panel__header">
              <span class="carton-calc-panel__icon" aria-hidden="true">
                <el-icon><Grid /></el-icon>
              </span>
              <div class="carton-calc-panel__heading">
                <h4 class="carton-calc-panel__title">{{ t('ecommerce.carton.calculateTitle') }}</h4>
                <p class="carton-calc-panel__subtitle">{{ t('ecommerce.carton.calculateSubtitle') }}</p>
              </div>
            </header>

            <div class="carton-calc-panel__body">
              <div class="carton-calc-panel__workspace">
                <form class="carton-calc-panel__form" @submit.prevent="onCalculate">
                  <div class="carton-calc-panel__input-grid">
                    <div class="carton-calc-panel__input-col">
                      <div class="carton-calc-panel__col-title">{{ t('ecommerce.carton.productSizeCm') }}</div>
                      <label class="carton-calc-field__label">{{ t('ecommerce.carton.productLength') }}</label>
                      <el-input-number
                        v-model="calcForm.lengthCm"
                        class="carton-calc-field__input"
                        :min="0"
                        :precision="2"
                        controls-position="right"
                      />
                      <label class="carton-calc-field__label">{{ t('ecommerce.carton.productWidth') }}</label>
                      <el-input-number
                        v-model="calcForm.widthCm"
                        class="carton-calc-field__input"
                        :min="0"
                        :precision="2"
                        controls-position="right"
                      />
                      <label class="carton-calc-field__label">{{ t('ecommerce.carton.productHeight') }}</label>
                      <el-input-number
                        v-model="calcForm.heightCm"
                        class="carton-calc-field__input"
                        :min="0"
                        :precision="2"
                        controls-position="right"
                      />
                    </div>

                    <div class="carton-calc-panel__input-col">
                      <div class="carton-calc-panel__col-title">
                        {{ t('ecommerce.carton.paddingSizeCm') }}
                        <el-tooltip :content="t('ecommerce.carton.paddingHint')" placement="top">
                          <el-icon class="carton-calc-panel__help"><QuestionFilled /></el-icon>
                        </el-tooltip>
                      </div>
                      <label class="carton-calc-field__label">{{ t('ecommerce.carton.paddingLength') }}</label>
                      <el-input-number
                        v-model="calcForm.padLengthCm"
                        class="carton-calc-field__input"
                        :min="0"
                        :precision="2"
                        controls-position="right"
                      />
                      <label class="carton-calc-field__label">{{ t('ecommerce.carton.paddingWidth') }}</label>
                      <el-input-number
                        v-model="calcForm.padWidthCm"
                        class="carton-calc-field__input"
                        :min="0"
                        :precision="2"
                        controls-position="right"
                      />
                      <label class="carton-calc-field__label">{{ t('ecommerce.carton.paddingHeight') }}</label>
                      <el-input-number
                        v-model="calcForm.padHeightCm"
                        class="carton-calc-field__input"
                        :min="0"
                        :precision="2"
                        controls-position="right"
                      />
                    </div>
                  </div>

                  <div class="carton-calc-panel__factory-row">
                    <label class="carton-calc-field__label">{{ t('ecommerce.carton.factory') }}</label>
                    <el-select
                      v-model="calcForm.factoryId"
                      class="carton-calc-field__input"
                      clearable
                      filterable
                      :placeholder="t('ecommerce.carton.factoryOptional')"
                    >
                      <el-option v-for="f in factoryOptions" :key="f.id" :label="f.name" :value="f.id" />
                    </el-select>
                  </div>

                  <el-button type="primary" :loading="calculating" native-type="submit" class="carton-calc-panel__submit" :icon="Refresh">
                    {{ t('ecommerce.carton.recalculate') }}
                  </el-button>
                </form>

                <div class="carton-calc-panel__result">
                  <div class="carton-calc-panel__result-title">{{ t('ecommerce.carton.calcResultTitle') }}</div>
                  <div class="carton-calc-panel__result-box">
                    <template v-if="calcMatchedDisplay">
                      <template v-if="calcMatchedDisplay.empty">
                        <div class="carton-calc-panel__result-empty">{{ t('ecommerce.carton.noMatch') }}</div>
                      </template>
                      <template v-else>
                        <div class="carton-calc-panel__result-label">{{ t('ecommerce.carton.matchedCarton') }}</div>
                        <div class="carton-calc-panel__result-head">
                          <button
                            type="button"
                            class="carton-calc-panel__result-name"
                            @click="openCartonFromCalc(calcMatchedDisplay.carton!)"
                          >
                            {{ calcMatchedDisplay.carton!.name }}
                          </button>
                          <div class="carton-calc-panel__result-factory">
                            {{ calcMatchedDisplay.carton!.factoryName || '—' }}
                          </div>
                        </div>
                        <div
                          v-if="calcMatchedDisplay.fitsWell"
                          class="carton-calc-panel__result-badge"
                        >
                          <el-icon><CircleCheck /></el-icon>
                          {{ t('ecommerce.carton.fitGood') }}
                        </div>
                        <div class="carton-calc-panel__result-line">
                          {{ t('ecommerce.carton.innerSize') }}：{{ formatSize(calcMatchedDisplay.carton!) }}
                        </div>
                        <div class="carton-calc-panel__result-line">
                          <span class="carton-calc-panel__result-line-label">
                            {{ t('ecommerce.carton.availableSpace') }}
                            <el-tooltip :content="t('ecommerce.carton.availableSpaceHint')" placement="top">
                              <el-icon class="carton-calc-panel__help"><QuestionFilled /></el-icon>
                            </el-tooltip>
                          </span>
                          ：{{
                            t('ecommerce.carton.availableSpaceValue', {
                              length: formatDimNumber(calcMatchedDisplay.slackL),
                              width: formatDimNumber(calcMatchedDisplay.slackW),
                              height: formatDimNumber(calcMatchedDisplay.slackH),
                            })
                          }}
                        </div>
                      </template>
                    </template>
                    <div v-else class="carton-calc-panel__result-placeholder">
                      {{ t('ecommerce.carton.calcResultPlaceholder') }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </section>
        </aside>
      </main>
    </div>

    <el-dialog
      v-model="previewBackfillVisible"
      :title="t('ecommerce.carton.previewBackfillTitle')"
      width="480px"
      :close-on-click-modal="false"
      :show-close="!previewBackfillRunning"
    >
      <el-progress
        :percentage="previewBackfillProgress"
        :status="previewBackfillFailed ? 'exception' : previewBackfillCompleted ? 'success' : undefined"
      />
      <p class="backfill-status">{{ previewBackfillStatusText }}</p>
      <template #footer>
        <el-button :disabled="previewBackfillRunning" @click="previewBackfillVisible = false">
          {{ t('ecommerce.common.close') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="backfillProgressVisible"
      :title="t('ecommerce.carton.resetProgressTitle')"
      width="480px"
      :close-on-click-modal="false"
      :show-close="!backfillRunning"
    >
      <el-progress
        :percentage="backfillProgress"
        :status="backfillFailed ? 'exception' : backfillCompleted ? 'success' : undefined"
      />
      <p class="backfill-status">{{ backfillStatusText }}</p>
      <template #footer>
        <el-button :disabled="backfillRunning" @click="backfillProgressVisible = false">
          {{ t('ecommerce.common.close') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Box, CircleCheck, Delete, Edit, Grid, Plus, QuestionFilled, Rank, Refresh } from '@element-plus/icons-vue'
import CartonBoxIllustration from '@/components/ecommerce/CartonBoxIllustration.vue'
import CnyAmount from '@/components/CnyAmount.vue'
import CartonBox3DPreview from '@/components/ecommerce/CartonBox3DPreview.vue'
import CartonStylePicker from '@/components/ecommerce/CartonStylePicker.vue'
import { canGenerateCartonPreview, storeCartonPreviewImage } from '@/utils/cartonPreviewImage'
import {
  calculateCarton,
  createCarton,
  deleteCarton,
  fetchAllCartons,
  fetchBackfillTask,
  startBackfillSkuCartonsAsync,
  updateCarton,
  type EcCarton,
  type EcCartonCalculateResult,
  type EcCartonSaveRequest,
} from '@/api/ecommerce/carton'
import { fetchFactoryOptions, type EcFactory } from '@/api/ecommerce/factory'
import { formatDate } from '@/utils/date'
import { computeCartonFitSlack } from '@/utils/cartonMatch'
import { normalizeCartonMaterialVariant, resolveCartonMaterialVariant, DEFAULT_CARTON_MATERIAL_VARIANT } from '@/constants/cartonMaterials'

const { t } = useI18n()

type DetailMode = 'empty' | 'view' | 'form'

const saving = ref(false)
const preview3dRef = ref<InstanceType<typeof CartonBox3DPreview> | null>(null)
const keyword = ref('')
const factoryOptions = ref<EcFactory[]>([])
const selectedId = ref<number | null>(null)
const detailMode = ref<DetailMode>('empty')
const records = ref<EcCarton[]>([])
const loading = ref(false)
const editingId = ref<number | null>(null)
const calculating = ref(false)
const calcResult = ref<EcCartonCalculateResult | null>(null)
const backfillRunning = ref(false)
const backfillProgressVisible = ref(false)
const backfillProgress = ref(0)
const backfillStatusText = ref('')
const backfillCompleted = ref(false)
const backfillFailed = ref(false)

const previewBackfillRunning = ref(false)
const previewBackfillVisible = ref(false)
const previewBackfillProgress = ref(0)
const previewBackfillStatusText = ref('')
const previewBackfillCompleted = ref(false)
const previewBackfillFailed = ref(false)

let backfillPollTimer: ReturnType<typeof setInterval> | null = null

const DEFAULT_PADDING_CM = 2

const calcForm = reactive<{
  lengthCm: number | null
  widthCm: number | null
  heightCm: number | null
  padLengthCm: number
  padWidthCm: number
  padHeightCm: number
  factoryId?: number
}>({
  lengthCm: null,
  widthCm: null,
  heightCm: null,
  padLengthCm: DEFAULT_PADDING_CM,
  padWidthCm: DEFAULT_PADDING_CM,
  padHeightCm: DEFAULT_PADDING_CM,
  factoryId: undefined,
})

const form = reactive<{
  name: string
  factoryId?: number
  lengthCm: number | null
  widthCm: number | null
  heightCm: number | null
  unitPrice: number | null
  remark: string
  illustrationVariant: number
}>({
  name: '',
  factoryId: undefined,
  lengthCm: null,
  widthCm: null,
  heightCm: null,
  unitPrice: null,
  remark: '',
  illustrationVariant: DEFAULT_CARTON_MATERIAL_VARIANT,
})

const FACTORY_ICON_PALETTE = [
  { bg: '#eff6ff', color: '#2563eb' },
  { bg: '#f0fdf4', color: '#16a34a' },
  { bg: '#fff7ed', color: '#ea580c' },
  { bg: '#faf5ff', color: '#9333ea' },
  { bg: '#f0fdfa', color: '#0d9488' },
  { bg: '#fff1f2', color: '#e11d48' },
  { bg: '#ecfeff', color: '#0891b2' },
  { bg: '#fffbeb', color: '#d97706' },
] as const

const selectedCarton = computed(() => records.value.find((row) => row.id === selectedId.value) ?? null)

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

const listPriceRange = computed(() => {
  const prices = records.value
    .map((row) => row.unitPrice)
    .filter((price): price is number => price != null && price > 0)
  if (prices.length === 0) return { min: 0, max: 0 }
  return { min: Math.min(...prices), max: Math.max(...prices) }
})

function formatDimNumber(value: number) {
  return Number(value).toFixed(2)
}

function warmPriceColor(ratio: number) {
  const t = Math.min(1, Math.max(0, ratio))
  const r = Math.round(251 + (124 - 251) * t)
  const g = Math.round(191 + (45 - 191) * t)
  const b = Math.round(36 + (18 - 36) * t)
  return `rgb(${r}, ${g}, ${b})`
}

function priceTextStyle(unitPrice: number | null | undefined) {
  if (unitPrice == null) return undefined
  const { min, max } = listPriceRange.value
  const ratio = max > min ? (unitPrice - min) / (max - min) : 0.55
  return {
    color: warmPriceColor(ratio),
    fontWeight: ratio >= 0.66 ? 700 : 600,
  }
}

function priceDetailTextStyle(unitPrice: number | null | undefined) {
  if (unitPrice == null) return undefined
  const { min, max } = listPriceRange.value
  const ratio = max > min ? (unitPrice - min) / (max - min) : 0.55
  return {
    color: warmPriceColor(ratio),
    fontWeight: 700,
    fontSize: '18px',
  }
}

function factoryColorKey(row: Pick<EcCarton, 'factoryId' | 'factoryName'>) {
  if (row.factoryId != null) return `id:${row.factoryId}`
  if (row.factoryName) return `name:${row.factoryName}`
  return 'default'
}

function hashFactoryKey(key: string) {
  let hash = 0
  for (let i = 0; i < key.length; i++) {
    hash = (hash * 31 + key.charCodeAt(i)) | 0
  }
  return Math.abs(hash)
}

function factoryIconStyle(row: Pick<EcCarton, 'factoryId' | 'factoryName'>) {
  const palette = FACTORY_ICON_PALETTE[hashFactoryKey(factoryColorKey(row)) % FACTORY_ICON_PALETTE.length]
  return {
    background: palette.bg,
    color: palette.color,
  }
}

function formatSize(row: Pick<EcCarton, 'lengthCm' | 'widthCm' | 'heightCm'>) {
  const { lengthCm, widthCm, heightCm } = row
  if (lengthCm == null && widthCm == null && heightCm == null) return '—'
  const parts = [lengthCm, widthCm, heightCm].map((v) => (v == null ? '—' : Number(v).toFixed(2)))
  return `${parts[0]} × ${parts[1]} × ${parts[2]} cm`
}

function resetForm() {
  form.name = ''
  form.factoryId = factoryOptions.value[0]?.id
  form.lengthCm = null
  form.widthCm = null
  form.heightCm = null
  form.unitPrice = null
  form.remark = ''
  form.illustrationVariant = DEFAULT_CARTON_MATERIAL_VARIANT
}

function syncSelectionAfterLoad() {
  if (detailMode.value === 'form') return
  if (selectedId.value && records.value.some((row) => row.id === selectedId.value)) {
    detailMode.value = 'view'
    return
  }
  if (records.value.length > 0) {
    selectedId.value = records.value[0].id
    detailMode.value = 'view'
    return
  }
  selectedId.value = null
  detailMode.value = 'empty'
}

async function loadFactoryOptions() {
  factoryOptions.value = await fetchFactoryOptions('CARTON')
}

function cartonVolume(row: Pick<EcCarton, 'lengthCm' | 'widthCm' | 'heightCm'>) {
  const l = Number(row.lengthCm) || 0
  const w = Number(row.widthCm) || 0
  const h = Number(row.heightCm) || 0
  return l * w * h
}

function sortCartonsByVolume(rows: EcCarton[]) {
  return [...rows].sort((a, b) => {
    const diff = cartonVolume(b) - cartonVolume(a)
    return diff !== 0 ? diff : b.id - a.id
  })
}

function patchCartonRecord(saved: EcCarton) {
  const index = records.value.findIndex((row) => row.id === saved.id)
  if (index >= 0) {
    records.value[index] = { ...records.value[index], ...saved }
    records.value = sortCartonsByVolume([...records.value])
    return
  }
  records.value = sortCartonsByVolume([...records.value, saved])
}

async function loadCartons() {
  loading.value = true
  try {
    const result = await fetchAllCartons(keyword.value.trim() || undefined)
    records.value = sortCartonsByVolume(result.records)
    syncSelectionAfterLoad()
  } finally {
    loading.value = false
  }
}

let searchTimer: ReturnType<typeof setTimeout> | null = null
watch(keyword, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => loadCartons(), 300)
})

function selectCarton(row: EcCarton) {
  selectedId.value = row.id
  detailMode.value = 'view'
  editingId.value = null
}

async function openCartonFromCalc(carton: EcCarton) {
  let row = records.value.find((item) => item.id === carton.id)
  if (!row && keyword.value.trim()) {
    keyword.value = ''
    await loadCartons()
    row = records.value.find((item) => item.id === carton.id)
  }
  if (row) {
    selectCarton(row)
    document.querySelector('.carton-detail-main')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    return
  }
  ElMessage.warning(t('ecommerce.carton.cartonNotInList'))
}

async function onCalculate() {
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
      calcForm.factoryId,
    )
  } finally {
    calculating.value = false
  }
}

function stopBackfillPoll() {
  if (backfillPollTimer) {
    clearInterval(backfillPollTimer)
    backfillPollTimer = null
  }
}

function updateBackfillProgress(task: {
  status: string
  total: number
  processed: number
  updated: number
  message?: string
}) {
  if (task.total > 0) {
    backfillProgress.value = Math.min(100, Math.round((task.processed / task.total) * 100))
  } else {
    backfillProgress.value = task.status === 'COMPLETED' ? 100 : 0
  }

  if (task.status === 'FAILED') {
    backfillFailed.value = true
    backfillStatusText.value = task.message || t('ecommerce.carton.resetFailed')
    return
  }

  if (task.status === 'COMPLETED') {
    backfillCompleted.value = true
    backfillStatusText.value = t('ecommerce.carton.resetDone', {
      total: task.total,
      updated: task.updated,
    })
    return
  }

  backfillStatusText.value = t('ecommerce.carton.resetRunning', {
    processed: task.processed,
    total: task.total,
    updated: task.updated,
  })
}

async function pollBackfillTask(taskId: string) {
  try {
    const task = await fetchBackfillTask(taskId)
    updateBackfillProgress(task)
    if (task.status === 'COMPLETED' || task.status === 'FAILED') {
      stopBackfillPoll()
      backfillRunning.value = false
      if (task.status === 'COMPLETED') {
        ElMessage.success(t('ecommerce.carton.resetDone', { total: task.total, updated: task.updated }))
      } else {
        ElMessage.error(task.message || t('ecommerce.carton.resetFailed'))
      }
    }
  } catch {
    stopBackfillPoll()
    backfillRunning.value = false
    backfillFailed.value = true
    backfillStatusText.value = t('ecommerce.carton.resetFailed')
  }
}

async function onBackfillPreviewImages() {
  const targets = records.value.filter((row) => canGenerateCartonPreview(row))
  if (targets.length === 0) {
    ElMessage.warning(t('ecommerce.carton.previewBackfillEmpty'))
    return
  }

  await ElMessageBox.confirm(
    t('ecommerce.carton.previewBackfillConfirm', { count: targets.length }),
    { type: 'warning' },
  )

  previewBackfillRunning.value = true
  previewBackfillVisible.value = true
  previewBackfillProgress.value = 0
  previewBackfillCompleted.value = false
  previewBackfillFailed.value = false
  previewBackfillStatusText.value = t('ecommerce.carton.previewBackfillStarting')

  let updated = 0
  try {
    for (let i = 0; i < targets.length; i++) {
      const row = targets[i]
      previewBackfillStatusText.value = t('ecommerce.carton.previewBackfillRunning', {
        current: i + 1,
        total: targets.length,
        name: row.name,
      })
      previewBackfillProgress.value = Math.round((i / targets.length) * 100)
      const stored = await storeCartonPreviewImage({
        id: row.id,
        name: row.name,
        lengthCm: row.lengthCm,
        widthCm: row.widthCm,
        heightCm: row.heightCm,
        illustrationVariant: row.illustrationVariant,
      })
      if (stored) {
        updated++
        patchCartonRecord({ ...row, previewImage: stored })
      }
    }
    previewBackfillProgress.value = 100
    previewBackfillCompleted.value = true
    previewBackfillStatusText.value = t('ecommerce.carton.previewBackfillDone', {
      total: targets.length,
      updated,
    })
    ElMessage.success(t('ecommerce.carton.previewBackfillDone', { total: targets.length, updated }))
    await loadCartons()
  } catch (error) {
    previewBackfillFailed.value = true
    const reason = previewErrorMessage(error)
    previewBackfillStatusText.value = reason
    ElMessage.error(t('ecommerce.carton.previewGenerateFailedDetail', { reason }))
  } finally {
    previewBackfillRunning.value = false
  }
}

async function onResetProductCartons() {
  await ElMessageBox.confirm(t('ecommerce.carton.resetConfirm'), { type: 'warning' })

  backfillRunning.value = true
  backfillProgressVisible.value = true
  backfillProgress.value = 0
  backfillCompleted.value = false
  backfillFailed.value = false
  backfillStatusText.value = t('ecommerce.carton.resetStarting')

  try {
    const taskId = await startBackfillSkuCartonsAsync()
    stopBackfillPoll()
    backfillPollTimer = setInterval(() => pollBackfillTask(taskId), 1000)
    await pollBackfillTask(taskId)
  } catch {
    backfillRunning.value = false
    backfillFailed.value = true
    backfillStatusText.value = t('ecommerce.carton.resetFailed')
  }
}

function openCreate() {
  editingId.value = null
  resetForm()
  detailMode.value = 'form'
}

function openEdit(row: EcCarton) {
  editingId.value = row.id
  selectedId.value = row.id
  form.name = row.name
  form.factoryId = row.factoryId ?? undefined
  form.lengthCm = row.lengthCm ?? null
  form.widthCm = row.widthCm ?? null
  form.heightCm = row.heightCm ?? null
  form.unitPrice = row.unitPrice ?? null
  form.remark = row.remark || ''
  form.illustrationVariant =
    normalizeCartonMaterialVariant(row.illustrationVariant) ??
    resolveCartonMaterialVariant(row.illustrationVariant, row.id)
  detailMode.value = 'form'
}

function cancelForm() {
  if (selectedId.value && records.value.some((row) => row.id === selectedId.value)) {
    detailMode.value = 'view'
  } else if (records.value.length > 0) {
    selectedId.value = records.value[0].id
    detailMode.value = 'view'
  } else {
    selectedId.value = null
    detailMode.value = 'empty'
  }
  editingId.value = null
}

function previewErrorMessage(error: unknown) {
  if (error instanceof Error && error.message.trim()) return error.message.trim()
  if (typeof error === 'string' && error.trim()) return error.trim()
  return t('ecommerce.carton.previewGenerateFailed')
}

async function onSave() {
  if (!form.name.trim()) {
    ElMessage.warning(t('ecommerce.carton.nameRequired'))
    return
  }
  if (!form.factoryId) {
    ElMessage.warning(t('ecommerce.carton.factoryRequired'))
    return
  }

  saving.value = true
  try {
    let previewImage: string | undefined
    const hasPreviewSize =
      form.lengthCm != null &&
      form.widthCm != null &&
      form.heightCm != null &&
      form.lengthCm > 0 &&
      form.widthCm > 0 &&
      form.heightCm > 0

    if (hasPreviewSize) {
      try {
        previewImage = await storeCartonPreviewImage({
          id: editingId.value,
          name: form.name.trim(),
          lengthCm: form.lengthCm,
          widthCm: form.widthCm,
          heightCm: form.heightCm,
          illustrationVariant: form.illustrationVariant,
        }) ?? undefined
        if (!previewImage) {
          ElMessage.warning(t('ecommerce.carton.previewGenerateFailed'))
        }
      } catch (error) {
        ElMessage.warning(t('ecommerce.carton.previewGenerateFailedDetail', {
          reason: previewErrorMessage(error),
        }))
      }
    }

    const payload: EcCartonSaveRequest = {
      name: form.name.trim(),
      factoryId: form.factoryId,
      lengthCm: form.lengthCm,
      widthCm: form.widthCm,
      heightCm: form.heightCm,
      unitPrice: form.unitPrice,
      remark: form.remark?.trim() || undefined,
      illustrationVariant: form.illustrationVariant,
      ...(previewImage ? { previewImage } : {}),
    }
    let saved: EcCarton
    if (editingId.value) {
      saved = await updateCarton(editingId.value, payload)
      selectedId.value = editingId.value
    } else {
      saved = await createCarton(payload)
      selectedId.value = saved.id
    }
    patchCartonRecord(saved)
    ElMessage.success(t('ecommerce.common.saved'))
    editingId.value = null
    detailMode.value = 'view'
    await loadCartons()
  } finally {
    saving.value = false
  }
}

async function onDelete(row: EcCarton) {
  await ElMessageBox.confirm(
    t('ecommerce.carton.deleteConfirm', { name: row.name }),
    { type: 'warning' },
  )
  await deleteCarton(row.id)
  ElMessage.success(t('ecommerce.common.deleted'))
  if (selectedId.value === row.id) {
    selectedId.value = null
    detailMode.value = 'empty'
  }
  await loadCartons()
}

onMounted(async () => {
  await Promise.all([loadCartons(), loadFactoryOptions()])
})

onBeforeUnmount(() => {
  stopBackfillPoll()
})

defineExpose({ loadCartons })
</script>

<style scoped lang="scss">
.carton-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.carton-panel__header {
  flex-shrink: 0;
  margin-bottom: 12px;
}

.carton-panel__title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--wr-text, #333);
}

.carton-panel__subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--wr-text-secondary, #666);
}

.carton-split {
  display: flex;
  flex: 1;
  min-height: 0;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  overflow: hidden;
  background: var(--el-bg-color);
}

.carton-split__list {
  width: 38%;
  min-width: 280px;
  max-width: 420px;
  border-right: 1px solid var(--el-border-color-light);
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: var(--el-fill-color-blank);
}

.carton-list-toolbar {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  flex-shrink: 0;
}

.carton-list-toolbar__actions {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.carton-list-toolbar__reset {
  flex-shrink: 0;
  width: auto;
}

.carton-list-toolbar__add {
  flex: 1;
  min-width: 0;
  height: 44px;
  font-size: 15px;
  font-weight: 600;

  :deep(.el-icon) {
    font-size: 18px;
  }
}

.carton-list-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 8px;
}

.carton-list-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 12px;
  margin: 0;
  border: none;
  border-radius: 8px;
  border-left: 3px solid transparent;
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.15s;

  &:hover {
    background: var(--el-fill-color-light);
  }

  &--active {
    background: var(--el-color-primary-light-9);
    border-left-color: var(--el-color-primary);
  }
}

.carton-list-item__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: 8px;
  font-size: 18px;
}

.carton-list-item__body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.carton-list-item__title {
  font-weight: 600;
  font-size: 14px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.carton-list-item__meta {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.carton-list-item__meta-sep {
  margin: 0 4px;
}

.carton-list-item__price {
  flex-shrink: 0;
  align-self: center;
  padding-left: 8px;
  font-size: 15px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
  white-space: nowrap;

  &.is-empty {
    font-size: 13px;
    font-weight: 400;
    color: var(--el-text-color-placeholder);
  }
}

.carton-split__detail {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.carton-detail-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;

  &:has(> .carton-detail:not(.carton-detail--immersive)) {
    flex: 0 0 auto;
  }
}

.carton-detail-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.carton-detail {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;

  &:not(.carton-detail--immersive) {
    flex: 0 0 auto;
    min-height: auto;
  }
}

.carton-detail__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 20px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  flex-shrink: 0;
}

.carton-detail__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.carton-detail__actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.carton-detail__body {
  flex: 0 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: 12px 20px 14px;
}

.carton-detail__footer {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 20px;
  border-top: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-blank);
}

.carton-detail--immersive {
  flex-direction: row;
  min-height: 0;
  overflow: hidden;
}

.carton-immersive__stage {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px 24px;
  background: linear-gradient(165deg, #dbe4ee 0%, #eef2f7 42%, #f8fafc 100%);
}

.carton-immersive__stage-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  max-width: 520px;
  gap: 12px;
}

.carton-immersive__rotate-hint {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin: 0;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgb(255 255 255 / 72%);
  border: 1px solid rgb(148 163 184 / 28%);
  font-size: 12px;
  color: var(--el-text-color-secondary);

  .el-icon {
    font-size: 14px;
    transform: rotate(45deg);
  }
}

.carton-immersive__card {
  width: min(380px, 42%);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;
  border-left: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-blank);
  box-shadow: -8px 0 28px rgb(15 23 42 / 6%);
}

.carton-immersive__card-header {
  flex-shrink: 0;
  padding: 18px 20px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.carton-immersive__card-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
}

.carton-immersive__card-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 16px 20px 8px;
}

.carton-immersive__card-footer {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 20px 16px;
  border-top: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-blank);
}

.carton-form--immersive {
  :deep(.el-form-item) {
    margin-bottom: 14px;
  }

  :deep(.el-form-item__label) {
    padding-bottom: 4px;
    font-size: 13px;
    font-weight: 600;
    color: var(--el-text-color-regular);
  }
}

.carton-section__title {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-color-primary);
}

.carton-detail-layout {
  display: flex;
  gap: 20px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.carton-info-grid {
  flex: 1;
  min-width: 200px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin: 0;
}

.carton-info-grid__item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  min-width: 0;
  padding: 2px 0;

  dt {
    flex-shrink: 0;
    width: 116px;
    margin: 0;
    font-size: 14px;
    line-height: 1.7;
    color: var(--el-text-color-secondary);
  }

  dd {
    flex: 1;
    min-width: 0;
    margin: 0;
    font-size: 15px;
    line-height: 1.7;
    word-break: break-word;
  }
}

.carton-info-grid__price {
  font-variant-numeric: tabular-nums;

  &.is-empty {
    font-size: 15px !important;
    font-weight: 400 !important;
    color: var(--el-text-color-placeholder) !important;
  }
}

.carton-detail-tools {
  flex: 1;
  flex-shrink: 0;
  min-height: 0;
  width: 100%;
  border-top: 1px solid var(--el-border-color-lighter);
  background: var(--wr-bg, #f5f6f8);
  padding: 10px 20px 16px;
}

.carton-calc-panel {
  border: 1px solid var(--wr-border, #e8ecef);
  border-radius: 12px;
  background: var(--wr-card, #fff);
  box-shadow: var(--wr-shadow, 0 4px 12px rgb(0 0 0 / 5%));
  overflow: hidden;
}

.carton-calc-panel__header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px 18px 14px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.carton-calc-panel__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: 10px;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-size: 20px;
}

.carton-calc-panel__heading {
  min-width: 0;
}

.carton-calc-panel__title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--wr-text, #333);
}

.carton-calc-panel__subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--el-text-color-secondary);
}

.carton-calc-panel__body {
  padding: 16px 18px 18px;
}

.carton-calc-panel__workspace {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(260px, 0.9fr);
  gap: 16px;
  align-items: start;
}

.carton-calc-panel__form {
  padding: 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: #fafbfc;
}

.carton-calc-field__label {
  font-size: 13px;
  line-height: 1.4;
  color: var(--el-text-color-regular);
  white-space: nowrap;
}

.carton-calc-field__input {
  width: 100%;
  min-width: 0;
}

.carton-calc-panel__input-col,
.carton-calc-panel__factory-row {
  display: grid;
  grid-template-columns: max-content minmax(0, 1fr);
  column-gap: 8px;
  row-gap: 10px;
  align-items: center;

  .carton-calc-field__label {
    justify-self: end;
    text-align: right;
  }
}

.carton-calc-panel__input-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 16px;
  margin-bottom: 0;
}

.carton-calc-panel__factory-row {
  margin-top: 18px;
  margin-bottom: 12px;
}

.carton-calc-panel__input-col :deep(.el-input-number),
.carton-calc-panel__input-col :deep(.el-select),
.carton-calc-panel__factory-row :deep(.el-input-number),
.carton-calc-panel__factory-row :deep(.el-select) {
  width: 100%;
}

.carton-calc-panel__input-col :deep(.el-input-number .el-input) {
  width: 100%;
}

.carton-calc-panel__input-col {
  min-width: 0;

  .carton-calc-panel__col-title {
    grid-column: 1 / -1;
    margin-bottom: 2px;
  }
}

.carton-calc-panel__col-title {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--wr-text, #333);
}

.carton-calc-panel__help {
  font-size: 14px;
  color: var(--el-color-primary);
  cursor: help;
}

.carton-calc-panel__submit {
  width: 100%;
  height: 42px;
  margin-top: 4px;
  font-size: 15px;
  font-weight: 600;
}

.carton-calc-panel__result {
  min-width: 0;
}

.carton-calc-panel__result-title {
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 600;
  color: var(--wr-text, #333);
}

.carton-calc-panel__result-box {
  min-height: 220px;
  padding: 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: #fff;
}

.carton-calc-panel__result-placeholder,
.carton-calc-panel__result-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 188px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  text-align: center;
}

.carton-calc-panel__result-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.carton-calc-panel__result-head {
  margin-top: 6px;
}

.carton-calc-panel__result-name {
  display: block;
  width: 100%;
  padding: 0;
  border: none;
  background: none;
  font-size: 20px;
  font-weight: 700;
  line-height: 1.4;
  color: var(--el-color-primary);
  text-align: left;
  word-break: break-word;
  cursor: pointer;
  transition: opacity 0.15s ease;

  &:hover {
    text-decoration: underline;
    opacity: 0.88;
  }
}

.carton-calc-panel__result-factory {
  margin-top: 4px;
  font-size: 13px;
  line-height: 1.5;
  color: var(--el-text-color-secondary);
  word-break: break-word;
}

.carton-calc-panel__result-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 10px;
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--el-color-success-light-9);
  color: var(--el-color-success);
  font-size: 12px;
  font-weight: 500;
}

.carton-calc-panel__result-line {
  margin-top: 12px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--el-text-color-regular);
}

.carton-calc-panel__result-line-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  vertical-align: middle;
}

.dim-row {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.dim-unit {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.backfill-status {
  margin: 12px 0 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

@media (max-width: 900px) {
  .carton-calc-panel__workspace {
    grid-template-columns: 1fr;
  }

  .carton-calc-panel__input-grid {
    grid-template-columns: 1fr;
  }

  .carton-list-toolbar__actions {
    flex-direction: column;
    align-items: stretch;
  }

  .carton-detail--immersive {
    flex-direction: column;
  }

  .carton-immersive__card {
    width: 100%;
    border-left: none;
    border-top: 1px solid var(--el-border-color-lighter);
    box-shadow: none;
  }
}

@media (max-width: 768px) {
  .carton-split {
    flex-direction: column;
  }

  .carton-split__list {
    width: 100%;
    max-width: none;
    max-height: 42vh;
    border-right: none;
    border-bottom: 1px solid var(--el-border-color-light);
  }
}
</style>
