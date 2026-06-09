<template>
  <div class="factory-panel">
    <div class="panel-toolbar">
      <el-input
        v-model="keyword"
        :placeholder="t('ecommerce.factory.searchPlaceholder')"
        clearable
        style="width: 240px"
        @keyup.enter="loadFactories"
      />
      <el-button @click="loadFactories">{{ t('ecommerce.factory.refresh') }}</el-button>
      <el-button type="primary" @click="openCreate">{{ t('ecommerce.factory.add') }}</el-button>
    </div>

    <el-table v-loading="loading" :data="records" stripe border>
      <el-table-column prop="name" :label="t('ecommerce.factory.name')" min-width="160" />
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
    >
      <el-form :model="form" label-width="88px">
        <el-form-item :label="t('ecommerce.factory.name')" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item :label="t('ecommerce.factory.contactName')">
          <el-input v-model="form.contactName" />
        </el-form-item>
        <el-form-item :label="t('ecommerce.factory.contactPhone')">
          <el-input v-model="form.contactPhone" />
        </el-form-item>
        <el-form-item :label="t('ecommerce.factory.address')">
          <el-input v-model="form.address" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item :label="t('ecommerce.factory.remark')">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item :label="t('ecommerce.factory.status')">
          <el-radio-group v-model="form.status">
            <el-radio value="ENABLED">{{ t('ecommerce.product.enabled') }}</el-radio>
            <el-radio value="DISABLED">{{ t('ecommerce.product.disabled') }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('ecommerce.common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">{{ t('ecommerce.common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit } from '@element-plus/icons-vue'
import {
  createFactory,
  deleteFactory,
  fetchFactories,
  updateFactory,
  type EcFactory,
  type EcFactorySaveRequest,
} from '@/api/ecommerce/factory'
import TablePagination from '@/components/TablePagination.vue'
import { usePagination } from '@/composables/usePagination'
import { TABLE_ACTIONS_CELL_CLASS } from '@/constants/table'

const { t } = useI18n()

const saving = ref(false)
const keyword = ref('')
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

const { page, pageSize, total, records, loading, load, onPageChange, onSizeChange } = usePagination(
  (p, ps) => fetchFactories(keyword.value.trim() || undefined, { page: p, pageSize: ps }),
)

const form = reactive<EcFactorySaveRequest>({
  name: '',
  contactName: '',
  contactPhone: '',
  address: '',
  remark: '',
  status: 'ENABLED',
})

function resetForm() {
  form.name = ''
  form.contactName = ''
  form.contactPhone = ''
  form.address = ''
  form.remark = ''
  form.status = 'ENABLED'
}

async function loadFactories() {
  await load()
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
.panel-toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
</style>
