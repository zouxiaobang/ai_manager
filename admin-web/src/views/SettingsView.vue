<!--
 * 系统设置页面组件
 * 管理应用全局配置，包括语言切换、主题切换、移动端首页主题设置、像素狗物品管理等
 -->
<template>
  <WarRoomPage :title="t('portal.menu.settings')">
    <!-- 设置表单区域：语言、主题、移动端首页主题设置 -->
    <div class="war-room-panel">
      <el-form label-width="120px" style="max-width: 480px">
        <el-form-item :label="t('app.language')">
          <el-select
            :model-value="appStore.locale"
            style="width: 200px"
            @change="onLocaleChange"
          >
            <el-option :label="t('app.languageChinese')" value="zh-CN" />
            <el-option :label="t('app.languageEnglish')" value="en-US" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('portal.settings.theme')">
          <el-radio-group
            :model-value="appStore.theme"
            @update:model-value="onThemeChange"
          >
            <el-radio-button value="light">{{ t('app.themeLight') }}</el-radio-button>
            <el-radio-button value="dark">{{ t('app.themeDark') }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('mobile.settings.homeTheme')">
          <el-radio-group
            class="settings-home-theme"
            :model-value="appStore.mobileHomeTheme"
            @update:model-value="onHomeThemeChange"
          >
            <el-radio
              v-for="item in mobileHomeThemeOptions"
              :key="item.id"
              :value="item.id"
              class="settings-home-theme__option"
            >
              <span class="settings-home-theme__label">{{ t(item.labelKey) }}</span>
              <span class="settings-home-theme__desc">{{ t(item.descKey) }}</span>
            </el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
    </div>

    <!-- 像素狗物品管理 -->
    <div class="war-room-panel settings-dog-items">
      <div class="settings-dog-items__header">
        <h3 class="settings-dog-items__title">像素狗 · 物品管理</h3>
        <el-button type="primary" :icon="Plus" @click="openAddDialog">添加物品</el-button>
      </div>

      <el-table :data="dogItems" v-loading="loadingItems" border stripe style="width: 100%">
        <el-table-column label="ID" prop="id" width="60" />
        <el-table-column label="图标" width="60">
          <template #default="{ row }">
            <span style="font-size: 20px">{{ row.icon }}</span>
          </template>
        </el-table-column>
        <el-table-column label="名称" prop="name" width="100" />
        <el-table-column label="颜色" width="100">
          <template #default="{ row }">
            <div class="settings-dog-items__color">
              <span class="settings-dog-items__color-swatch" :style="{ background: row.color }" />
              {{ row.color }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="形状" width="80">
          <template #default="{ row }">
            {{ shapeNames[row.shape] || row.shape }}
          </template>
        </el-table-column>
        <el-table-column label="解锁等级" prop="requireLevel" width="90" />
        <el-table-column label="排序" prop="sortOrder" width="70" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="onDeleteItem(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 添加/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingItem.id ? '编辑物品' : '添加物品'"
      width="420px"
    >
      <el-form :model="editingItem" label-width="80px">
        <el-form-item label="图标">
          <el-input v-model="editingItem.icon" placeholder="🎀" style="width: 80px" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="editingItem.name" placeholder="蝴蝶结" />
        </el-form-item>
        <el-form-item label="颜色">
          <el-color-picker v-model="editingItem.color" />
          <span style="margin-left: 8px">{{ editingItem.color }}</span>
        </el-form-item>
        <el-form-item label="形状">
          <el-select v-model="editingItem.shape" style="width: 200px">
            <el-option
              v-for="(name, idx) in shapeNames"
              :key="idx"
              :label="`${idx}: ${name}`"
              :value="idx"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="解锁等级">
          <el-input-number v-model="editingItem.requireLevel" :min="1" :max="100" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="editingItem.sortOrder" :min="0" :max="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSaveItem">保存</el-button>
      </template>
    </el-dialog>
  </WarRoomPage>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import WarRoomPage from '@/components/war-room/WarRoomPage.vue'
import { mobileHomeThemeOptions } from '@/data/mobile-home-themes'
import type { MobileHomeThemeId } from '@/data/mobile-home-themes'
import { useAppStore, type LocaleCode, type ThemeMode } from '@/stores/app'
import i18n from '@/i18n'
import {
  fetchDogItems,
  createDogItem,
  updateDogItem,
  deleteDogItem,
  type PixelDogItemVO,
  type PixelDogItemRequest,
} from '@/api/pixelDog'

const { t } = useI18n()
const appStore = useAppStore()

function onLocaleChange(code: LocaleCode) {
  appStore.setLocale(code, i18n)
}

function onThemeChange(mode: ThemeMode) {
  appStore.applyTheme(mode)
}

function onHomeThemeChange(id: MobileHomeThemeId) {
  appStore.setMobileHomeTheme(id)
}

// ========== 像素狗物品管理 ==========

const shapeNames = [
  '蝴蝶结', '礼帽', '眼镜', '项圈', '皇冠',
  '星星', '爱心', '披风', '光环', '小点',
]

const dogItems = ref<PixelDogItemVO[]>([])
const loadingItems = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)

const editingItem = ref<PixelDogItemRequest & { id?: number }>({
  icon: '',
  name: '',
  color: '#ff69b4',
  requireLevel: 1,
  sortOrder: 0,
  shape: 0,
})

async function loadItems() {
  loadingItems.value = true
  try {
    const data = await fetchDogItems()
    dogItems.value = data || []
  } catch (e) {
    console.error('Failed to load dog items:', e)
  } finally {
    loadingItems.value = false
  }
}

function openAddDialog() {
  editingItem.value = {
    icon: '',
    name: '',
    color: '#ff69b4',
    requireLevel: 1,
    sortOrder: dogItems.value.length + 1,
    shape: 0,
  }
  dialogVisible.value = true
}

function openEditDialog(row: PixelDogItemVO) {
  editingItem.value = {
    id: row.id,
    icon: row.icon,
    name: row.name,
    color: row.color,
    requireLevel: row.requireLevel,
    sortOrder: row.sortOrder,
    shape: row.shape,
  }
  dialogVisible.value = true
}

async function onSaveItem() {
  const item = editingItem.value
  if (!item.name || !item.icon) {
    ElMessage.warning('请填写名称和图标')
    return
  }
  saving.value = true
  try {
    if (item.id) {
      await updateDogItem(item.id, item)
      ElMessage.success('修改成功')
    } else {
      await createDogItem(item)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    await loadItems()
  } catch (e) {
    console.error('Failed to save item:', e)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function onDeleteItem(row: PixelDogItemVO) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.name}」吗？`, '提示', {
      type: 'warning',
    })
    await deleteDogItem(row.id)
    ElMessage.success('删除成功')
    await loadItems()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('Failed to delete item:', e)
    }
  }
}

onMounted(() => {
  loadItems()
})
</script>

<style scoped lang="scss">
.settings-home-theme {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
  width: 100%;
}

.settings-home-theme__option {
  display: flex;
  align-items: flex-start;
  height: auto;
  margin-right: 0;
  white-space: normal;
}

.settings-home-theme__label {
  display: block;
  font-weight: 600;
  line-height: 1.35;
}

.settings-home-theme__desc {
  display: block;
  margin-top: 2px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.35;
}

.settings-dog-items {
  margin-top: 20px;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
  }

  &__title {
    font-size: 16px;
    font-weight: 700;
    margin: 0;
  }

  &__color {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  &__color-swatch {
    display: inline-block;
    width: 16px;
    height: 16px;
    border-radius: 3px;
    border: 1px solid #ddd;
  }
}
</style>
