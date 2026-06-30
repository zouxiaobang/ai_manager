<template>
  <div class="factory-panel">
    <header class="factory-panel__header">
      <div>
        <h2 class="factory-panel__title">{{ t('ecommerce.factory.pageTitle') }}</h2>
        <p class="factory-panel__subtitle">{{ t('ecommerce.factory.pageSubtitle') }}</p>
      </div>
    </header>

    <section v-loading="statsLoading" class="factory-stat-cards">
      <div
        v-for="card in statCards"
        :key="card.key"
        class="factory-stat-card"
        :class="[`is-${card.tone}`, { 'is-clickable': card.filterType !== undefined }]"
        @click="card.filterType !== undefined ? onStatCardClick(card.filterType) : undefined"
      >
        <div class="factory-stat-card__icon" aria-hidden="true">
          <el-icon><component :is="card.icon" /></el-icon>
        </div>
        <div class="factory-stat-card__body">
          <div class="factory-stat-card__label">{{ card.label }}</div>
          <div class="factory-stat-card__value">{{ card.value }}</div>
        </div>
      </div>
    </section>

    <div class="factory-panel__toolbar panel-toolbar">
      <el-input
        v-model="keyword"
        :placeholder="t('ecommerce.factory.searchPlaceholder')"
        clearable
        class="factory-panel__search"
        @keyup.enter="loadFactories"
      />
      <el-radio-group v-model="typeFilter" class="factory-panel__type-filter" @change="onTypeFilterChange">
        <el-radio-button value="">{{ t('ecommerce.factory.filterAll') }}</el-radio-button>
        <el-radio-button value="PRODUCTION">{{ t('ecommerce.factory.factoryTypeProduction') }}</el-radio-button>
        <el-radio-button value="CUSTOMER">{{ t('ecommerce.factory.factoryTypeCustomer') }}</el-radio-button>
        <el-radio-button value="CARTON">{{ t('ecommerce.factory.factoryTypeCarton') }}</el-radio-button>
      </el-radio-group>
      <div class="factory-panel__toolbar-spacer" />
      <el-button type="primary" @click="openCreate">{{ t('ecommerce.factory.add') }}</el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="records"
      border
      row-key="id"
      class="factory-table"
      :row-class-name="rowClassName"
    >
      <el-table-column prop="name" :label="t('ecommerce.factory.name')" min-width="160" />
      <el-table-column :label="t('ecommerce.factory.factoryType')" width="90">
        <template #default="{ row }">
          <el-tag :type="factoryTypeTagType(row.factoryType)" size="small" :class="factoryTypeTagClass(row.factoryType)">
            {{ factoryTypeLabel(row.factoryType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="contactName" :label="t('ecommerce.factory.contactName')" width="100" />
      <el-table-column prop="contactPhone" :label="t('ecommerce.factory.contactPhone')" width="130" />
      <el-table-column prop="address" :label="t('ecommerce.factory.address')" min-width="200" show-overflow-tooltip />
      <el-table-column prop="remark" :label="t('ecommerce.factory.remark')" min-width="140" show-overflow-tooltip />
      <el-table-column :label="t('ecommerce.factory.status')" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" size="small">
            {{ row.status === 'ENABLED' ? t('ecommerce.product.enabled') : t('ecommerce.product.disabled') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        :label="t('ecommerce.factory.actions')"
        width="88"
        fixed="right"
        align="center"
        :class-name="TABLE_ACTIONS_CELL_CLASS"
      >
        <template #default="{ row }">
          <div class="table-actions-cell-inner" @click.stop>
            <el-button link type="primary" :title="t('ecommerce.factory.edit')" @click.stop="openEdit(row)">
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button link type="danger" :title="t('ecommerce.factory.delete')" @click.stop="onDelete(row)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <TablePagination
      :page="page"
      :page-size="pageSize"
      :total="total"
      @update:page="onPageChange"
      @update:page-size="onSizeChange"
    />

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? t('ecommerce.factory.editTitle') : t('ecommerce.factory.createTitle')"
      width="560px"
      destroy-on-close
      class="factory-form-dialog"
    >
      <el-form :model="form" label-width="88px" class="factory-form-dialog__form" @submit.prevent>
        <div class="factory-form-dialog__type-cards">
          <button
            type="button"
            class="factory-type-card is-production"
            :class="{ 'is-active': form.factoryType === 'PRODUCTION' }"
            @click="form.factoryType = 'PRODUCTION'"
          >
            <span class="factory-type-card__icon" aria-hidden="true">
              <el-icon><OfficeBuilding /></el-icon>
            </span>
            <span class="factory-type-card__label">{{ t('ecommerce.factory.statProduction') }}</span>
          </button>
          <button
            type="button"
            class="factory-type-card is-customer"
            :class="{ 'is-active': form.factoryType === 'CUSTOMER' }"
            @click="form.factoryType = 'CUSTOMER'"
          >
            <span class="factory-type-card__icon" aria-hidden="true">
              <el-icon><User /></el-icon>
            </span>
            <span class="factory-type-card__label">{{ t('ecommerce.factory.statCustomer') }}</span>
          </button>
          <button
            type="button"
            class="factory-type-card is-carton"
            :class="{ 'is-active': form.factoryType === 'CARTON' }"
            @click="form.factoryType = 'CARTON'"
          >
            <span class="factory-type-card__icon" aria-hidden="true">
              <el-icon><Box /></el-icon>
            </span>
            <span class="factory-type-card__label">{{ t('ecommerce.factory.statCarton') }}</span>
          </button>
        </div>

        <el-form-item :label="t('ecommerce.factory.name')" required>
          <el-input v-model="form.name" />
        </el-form-item>

        <div class="factory-form-dialog__contact-row">
          <el-form-item :label="t('ecommerce.factory.contactName')" class="factory-form-dialog__contact-col">
            <el-input v-model="form.contactName" />
          </el-form-item>
          <el-form-item :label="t('ecommerce.factory.contactPhone')" class="factory-form-dialog__contact-col">
            <el-input v-model="form.contactPhone" />
          </el-form-item>
        </div>

        <el-form-item :label="t('ecommerce.factory.address')">
          <el-input v-model="form.address" type="textarea" :rows="2" />
        </el-form-item>

        <el-form-item :label="t('ecommerce.factory.remark')">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>

        <el-form-item :label="t('ecommerce.factory.status')">
          <div class="factory-status-toggle" role="radiogroup" :aria-label="t('ecommerce.factory.status')">
            <button
              type="button"
              role="radio"
              class="factory-status-toggle__option"
              :class="{ 'is-active': form.status === 'ENABLED' }"
              :aria-checked="form.status === 'ENABLED'"
              @click="form.status = 'ENABLED'"
            >
              {{ t('ecommerce.product.enabled') }}
            </button>
            <button
              type="button"
              role="radio"
              class="factory-status-toggle__option"
              :class="{ 'is-active': form.status === 'DISABLED' }"
              :aria-checked="form.status === 'DISABLED'"
              @click="form.status = 'DISABLED'"
            >
              {{ t('ecommerce.product.disabled') }}
            </button>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button
          type="primary"
          size="large"
          class="factory-form-dialog__save"
          :loading="saving"
          @click="onSave"
        >
          {{ t('ecommerce.common.save') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, OfficeBuilding, User, Box, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import {
  createFactory,
  deleteFactory,
  fetchFactories,
  fetchFactoryStats,
  updateFactory,
  type EcFactory,
  type EcFactorySaveRequest,
  type EcFactoryType,
} from '@/api/ecommerce/factory'
import TablePagination from '@/components/TablePagination.vue'
import { usePagination } from '@/composables/usePagination'
import { TABLE_ACTIONS_CELL_CLASS } from '@/constants/table'

const { t } = useI18n()

type FactoryTypeFilter = '' | EcFactoryType

const saving = ref(false)
const statsLoading = ref(false)
const keyword = ref('')
const typeFilter = ref<FactoryTypeFilter>('')
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

const stats = reactive({
  production: 0,
  customer: 0,
  carton: 0,
  enabled: 0,
  disabled: 0,
})

const { page, pageSize, total, records, loading, load, onPageChange, onSizeChange } = usePagination(
  (p, ps) =>
    fetchFactories(keyword.value.trim() || undefined, {
      page: p,
      pageSize: ps,
      ...(typeFilter.value ? { factoryType: typeFilter.value } : {}),
    }),
)

const form = reactive<EcFactorySaveRequest>({
  name: '',
  factoryType: 'PRODUCTION',
  contactName: '',
  contactPhone: '',
  address: '',
  remark: '',
  status: 'ENABLED',
})

const statCards = computed(() => [
  {
    key: 'production',
    label: t('ecommerce.factory.statProduction'),
    value: stats.production,
    tone: 'orange',
    icon: OfficeBuilding,
    filterType: 'PRODUCTION' as const,
  },
  {
    key: 'customer',
    label: t('ecommerce.factory.statCustomer'),
    value: stats.customer,
    tone: 'blue',
    icon: User,
    filterType: 'CUSTOMER' as const,
  },
  {
    key: 'carton',
    label: t('ecommerce.factory.statCarton'),
    value: stats.carton,
    tone: 'purple',
    icon: Box,
    filterType: 'CARTON' as const,
  },
  {
    key: 'enabled',
    label: t('ecommerce.factory.statEnabled'),
    value: stats.enabled,
    tone: 'green',
    icon: CircleCheck,
  },
  {
    key: 'disabled',
    label: t('ecommerce.factory.statDisabled'),
    value: stats.disabled,
    tone: 'gray',
    icon: CircleClose,
  },
])

function normalizeFactoryType(type?: string): EcFactoryType {
  if (type === 'CUSTOMER') return 'CUSTOMER'
  if (type === 'CARTON') return 'CARTON'
  return 'PRODUCTION'
}

function factoryTypeLabel(type?: string) {
  if (type === 'CUSTOMER') return t('ecommerce.factory.factoryTypeCustomer')
  if (type === 'CARTON') return t('ecommerce.factory.factoryTypeCarton')
  return t('ecommerce.factory.factoryTypeProduction')
}

function factoryTypeTagType(type?: string): 'primary' | 'warning' | 'info' {
  if (type === 'CUSTOMER') return 'primary'
  if (type === 'CARTON') return 'info'
  return 'warning'
}

function factoryTypeTagClass(type?: string) {
  if (type === 'CARTON') return 'factory-type-tag--carton'
  return ''
}

function rowClassName({ row }: { row: EcFactory }) {
  if (row.factoryType === 'CUSTOMER') return 'factory-row--customer'
  if (row.factoryType === 'CARTON') return 'factory-row--carton'
  return 'factory-row--production'
}

function resetForm() {
  form.name = ''
  form.factoryType = 'PRODUCTION'
  form.contactName = ''
  form.contactPhone = ''
  form.address = ''
  form.remark = ''
  form.status = 'ENABLED'
}

async function loadStats() {
  statsLoading.value = true
  try {
    const result = await fetchFactoryStats()
    stats.production = result.productionCount
    stats.customer = result.customerCount
    stats.carton = result.cartonCount
    stats.enabled = result.enabledCount
    stats.disabled = result.disabledCount
  } finally {
    statsLoading.value = false
  }
}

async function loadFactories() {
  await Promise.all([load(), loadStats()])
}

function onTypeFilterChange() {
  load(true)
}

function onStatCardClick(filterType: EcFactoryType) {
  typeFilter.value = typeFilter.value === filterType ? '' : filterType
  load(true)
}

let searchTimer: ReturnType<typeof setTimeout> | null = null
watch(keyword, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => load(true), 300)
})

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: EcFactory) {
  editingId.value = row.id
  form.name = row.name
  form.factoryType = normalizeFactoryType(row.factoryType)
  form.contactName = row.contactName || ''
  form.contactPhone = row.contactPhone || ''
  form.address = row.address || ''
  form.remark = row.remark || ''
  form.status = row.status
  dialogVisible.value = true
}

async function onSave() {
  if (!form.name.trim()) {
    ElMessage.warning(t('ecommerce.factory.nameRequired'))
    return
  }
  saving.value = true
  try {
    const payload: EcFactorySaveRequest = {
      name: form.name.trim(),
      factoryType: form.factoryType,
      contactName: form.contactName?.trim() || undefined,
      contactPhone: form.contactPhone?.trim() || undefined,
      address: form.address?.trim() || undefined,
      remark: form.remark?.trim() || undefined,
      status: form.status,
    }
    if (editingId.value) {
      await updateFactory(editingId.value, payload)
    } else {
      await createFactory(payload)
    }
    ElMessage.success(t('ecommerce.common.saved'))
    dialogVisible.value = false
    await loadFactories()
  } finally {
    saving.value = false
  }
}

async function onDelete(row: EcFactory) {
  await ElMessageBox.confirm(
    t('ecommerce.factory.deleteConfirm', { name: row.name }),
    { type: 'warning' },
  )
  await deleteFactory(row.id)
  ElMessage.success(t('ecommerce.common.deleted'))
  await loadFactories()
}

onMounted(loadFactories)

defineExpose({ loadFactories })
</script>

<style scoped lang="scss">
.factory-panel__header {
  margin-bottom: 16px;
}

.factory-panel__title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--wr-text, #333);
}

.factory-panel__subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--wr-muted, #999);
}

.factory-stat-cards {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.factory-stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border: 1px solid transparent;
  border-radius: 14px;
  background: var(--wr-card, #fff);
  box-shadow: 0 1px 2px rgb(15 23 42 / 4%);
  transition: box-shadow 0.15s ease, transform 0.15s ease;

  &.is-clickable {
    cursor: pointer;

    &:hover {
      transform: translateY(-1px);
      box-shadow: var(--wr-shadow, 0 4px 12px rgb(0 0 0 / 5%));
    }
  }

  &.is-orange {
    background: var(--wr-stat-orange-bg, #fff7ed);
    border-color: #fed7aa;

    .factory-stat-card__icon {
      background: var(--wr-stat-orange, #ea580c);
      color: #fff;
    }

    .factory-stat-card__value {
      color: var(--wr-stat-orange, #ea580c);
    }
  }

  &.is-blue {
    background: var(--wr-stat-blue-bg, #eff6ff);
    border-color: #bfdbfe;

    .factory-stat-card__icon {
      background: var(--wr-stat-blue, #2563eb);
      color: #fff;
    }

    .factory-stat-card__value {
      color: var(--wr-stat-blue, #2563eb);
    }
  }

  &.is-purple {
    background: var(--wr-stat-purple-bg, #f5f3ff);
    border-color: #ddd6fe;

    .factory-stat-card__icon {
      background: var(--wr-stat-purple, #7c3aed);
      color: #fff;
    }

    .factory-stat-card__value {
      color: var(--wr-stat-purple, #7c3aed);
    }
  }

  &.is-green {
    background: var(--wr-stat-green-bg, #f0fdf4);
    border-color: #bbf7d0;

    .factory-stat-card__icon {
      background: var(--wr-stat-green, #16a34a);
      color: #fff;
    }

    .factory-stat-card__value {
      color: var(--wr-stat-green, #16a34a);
    }
  }

  &.is-gray {
    background: var(--wr-stat-gray-bg, #f3f4f6);
    border-color: #e5e7eb;

    .factory-stat-card__icon {
      background: var(--wr-stat-gray, #6b7280);
      color: #fff;
    }

    .factory-stat-card__value {
      color: var(--wr-stat-gray, #6b7280);
    }
  }
}

.factory-stat-card__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  flex-shrink: 0;
  font-size: 18px;
}

.factory-stat-card__body {
  min-width: 0;
}

.factory-stat-card__label {
  font-size: 13px;
  font-weight: 600;
  color: var(--wr-text-secondary, #666);
}

.factory-stat-card__value {
  margin-top: 4px;
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}

.panel-toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}

.factory-panel__search {
  width: 260px;
}

.factory-panel__type-filter {
  flex-shrink: 0;
}

.factory-panel__toolbar-spacer {
  flex: 1;
  min-width: 12px;
}

.factory-type-tag--carton {
  --el-tag-bg-color: var(--wr-stat-purple-bg, #f5f3ff);
  --el-tag-border-color: #ddd6fe;
  --el-tag-text-color: var(--wr-stat-purple, #7c3aed);
}

.factory-table {
  :deep(.el-table__body .el-table__cell) {
    padding-top: 16px;
    padding-bottom: 16px;
  }

  :deep(.el-table__header .el-table__cell) {
    padding-top: 12px;
    padding-bottom: 12px;
  }

  :deep(.factory-row--customer > td.el-table__cell) {
    background-color: var(--wr-stat-blue-bg, #eff6ff) !important;
  }

  :deep(.factory-row--production > td.el-table__cell) {
    background-color: var(--wr-stat-orange-bg, #fff7ed) !important;
  }

  :deep(.factory-row--carton > td.el-table__cell) {
    background-color: var(--wr-stat-purple-bg, #f5f3ff) !important;
  }

  :deep(.factory-row--customer:hover > td.el-table__cell) {
    background-color: #dbeafe !important;
  }

  :deep(.factory-row--production:hover > td.el-table__cell) {
    background-color: #ffedd5 !important;
  }

  :deep(.factory-row--carton:hover > td.el-table__cell) {
    background-color: #ede9fe !important;
  }

  :deep(.factory-row--customer.current-row > td.el-table__cell) {
    background-color: #dbeafe !important;
  }

  :deep(.factory-row--production.current-row > td.el-table__cell) {
    background-color: #ffedd5 !important;
  }

  :deep(.factory-row--carton.current-row > td.el-table__cell) {
    background-color: #ede9fe !important;
  }
}

@media (max-width: 960px) {
  .factory-stat-cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.factory-form-dialog {
  :deep(.el-dialog__body) {
    padding-top: 8px;
  }

  :deep(.el-dialog__footer) {
    padding: 8px 20px 20px;
  }
}

.factory-form-dialog__save {
  width: 100%;
  height: 44px;
  font-size: 16px;
  font-weight: 600;
}

.factory-form-dialog__form {
  :deep(.el-form-item) {
    margin-bottom: 16px;
  }

  :deep(.el-form-item:last-child) {
    margin-bottom: 0;
  }
}

.factory-form-dialog__type-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  width: 100%;
  margin-bottom: 16px;
}

.factory-type-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-height: 88px;
  padding: 14px 8px;
  border: 2px solid var(--wr-border, #e8ecef);
  border-radius: 12px;
  background: var(--wr-card, #fff);
  cursor: pointer;
  transition: border-color 0.15s ease, background 0.15s ease, box-shadow 0.15s ease;

  &:hover {
    box-shadow: var(--wr-shadow, 0 4px 12px rgb(0 0 0 / 5%));
  }

  &.is-production {
    .factory-type-card__icon {
      background: var(--wr-stat-orange-bg, #fff7ed);
      color: var(--wr-stat-orange, #ea580c);
    }

    &.is-active {
      border-color: #fdba74;
      background: var(--wr-stat-orange-bg, #fff7ed);
      box-shadow: 0 0 0 1px rgb(234 88 12 / 8%);
    }
  }

  &.is-customer {
    .factory-type-card__icon {
      background: var(--wr-stat-blue-bg, #eff6ff);
      color: var(--wr-stat-blue, #2563eb);
    }

    &.is-active {
      border-color: #93c5fd;
      background: var(--wr-stat-blue-bg, #eff6ff);
      box-shadow: 0 0 0 1px rgb(37 99 235 / 8%);
    }
  }

  &.is-carton {
    .factory-type-card__icon {
      background: var(--wr-stat-purple-bg, #f5f3ff);
      color: var(--wr-stat-purple, #7c3aed);
    }

    &.is-active {
      border-color: #c4b5fd;
      background: var(--wr-stat-purple-bg, #f5f3ff);
      box-shadow: 0 0 0 1px rgb(124 58 237 / 8%);
    }
  }
}

.factory-type-card__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  font-size: 22px;
}

.factory-type-card__label {
  font-size: 13px;
  font-weight: 600;
  color: var(--wr-text, #333);
  text-align: center;
}

.factory-form-dialog__contact-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.factory-form-dialog__contact-col {
  min-width: 0;
  margin-bottom: 0 !important;

  :deep(.el-form-item__content) {
    flex: 1;
    min-width: 0;
  }

  :deep(.el-input) {
    width: 100%;
  }
}

.factory-status-toggle {
  display: inline-flex;
  align-items: center;
  padding: 3px;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  background: #fff;
}

.factory-status-toggle__option {
  min-width: 76px;
  padding: 6px 22px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: var(--wr-text, #333);
  font-size: 14px;
  line-height: 1.4;
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease;

  &.is-active {
    background: var(--wr-stat-green, #16a34a);
    color: #fff;
  }

  &:not(.is-active):hover {
    color: var(--wr-text-secondary, #666);
  }
}

@media (max-width: 560px) {
  .factory-form-dialog__type-cards {
    grid-template-columns: 1fr;
  }

  .factory-form-dialog__contact-row {
    grid-template-columns: 1fr;
  }
}
</style>
