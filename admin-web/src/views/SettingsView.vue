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

      </el-form>
    </div>

    <!-- 番茄钟音效设置 -->
    <div class="war-room-panel" style="margin-top: 20px">
      <h3 class="settings-section__title">{{ t('portal.settings.pomodoroSound') }}</h3>
      <el-form label-width="120px" style="max-width: 480px">
        <el-form-item :label="t('portal.settings.pomodoroSoundEnabled')">
          <el-switch
            :model-value="appStore.pomodoroSoundEnabled"
            @update:model-value="appStore.setPomodoroSoundEnabled"
          />
        </el-form-item>
        <el-form-item :label="t('portal.settings.pomodoroBeeps')">
          <div class="settings-slider-row">
            <el-slider
              :model-value="appStore.pomodoroBeeps"
              :min="1"
              :max="20"
              :step="1"
              style="width: 180px; margin-right: 12px"
              @update:model-value="appStore.setPomodoroBeeps"
            />
            <span class="settings-slider-row__value">{{ appStore.pomodoroBeeps }} 次</span>
          </div>
          <div class="settings-slider-row__desc">{{ t('portal.settings.pomodoroBeepsDesc') }}</div>
        </el-form-item>
        <el-form-item :label="t('portal.settings.pomodoroVolume')">
          <div class="settings-slider-row">
            <el-slider
              :model-value="appStore.pomodoroVolume * 100"
              :min="0"
              :max="100"
              :step="1"
              style="width: 180px; margin-right: 12px"
              @update:model-value="onPomodoroVolumeChange"
            />
            <span class="settings-slider-row__value">{{ Math.round(appStore.pomodoroVolume * 100) }}%</span>
          </div>
        </el-form-item>
      </el-form>
    </div>

    <!-- 待办提醒音效设置 -->
    <div class="war-room-panel" style="margin-top: 20px">
      <h3 class="settings-section__title">{{ t('portal.settings.todoRemindSound') }}</h3>
      <el-form label-width="120px" style="max-width: 480px">
        <el-form-item :label="t('portal.settings.todoRemindEnabled')">
          <el-switch
            :model-value="appStore.todoRemindEnabled"
            @update:model-value="appStore.setTodoRemindEnabled"
          />
        </el-form-item>
        <el-form-item :label="t('portal.settings.todoRemindBeeps')">
          <div class="settings-slider-row">
            <el-slider
              :model-value="appStore.todoRemindBeeps"
              :min="1"
              :max="20"
              :step="1"
              :disabled="!appStore.todoRemindEnabled"
              style="width: 180px; margin-right: 12px"
              @update:model-value="appStore.setTodoRemindBeeps"
            />
            <span class="settings-slider-row__value">{{ appStore.todoRemindBeeps }} 次</span>
          </div>
        </el-form-item>
        <el-form-item :label="t('portal.settings.todoRemindVolume')">
          <div class="settings-slider-row">
            <el-slider
              :model-value="appStore.todoRemindVolume * 100"
              :min="0"
              :max="100"
              :step="1"
              :disabled="!appStore.todoRemindEnabled"
              style="width: 180px; margin-right: 12px"
              @update:model-value="onTodoRemindVolumeChange"
            />
            <span class="settings-slider-row__value">{{ Math.round(appStore.todoRemindVolume * 100) }}%</span>
          </div>
        </el-form-item>
      </el-form>
    </div>

    <!-- 像素狗物品管理（仅查看，默认收起） -->
    <div class="war-room-panel settings-dog-items">
      <el-collapse v-model="dogItemsCollapsed">
        <el-collapse-item title="像素狗 · 物品管理" name="dog-items">
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
          </el-table>
        </el-collapse-item>
      </el-collapse>
    </div>
  </WarRoomPage>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import WarRoomPage from '@/components/war-room/WarRoomPage.vue'

import { useAppStore, type LocaleCode, type ThemeMode } from '@/stores/app'
import i18n from '@/i18n'
import { fetchDogItems, type PixelDogItemVO } from '@/api/pixelDog'

const { t } = useI18n()
const appStore = useAppStore()

function onLocaleChange(code: LocaleCode) {
  appStore.setLocale(code, i18n)
}

function onThemeChange(mode: ThemeMode) {
  appStore.applyTheme(mode)
}

function onPomodoroVolumeChange(val: number) {
  appStore.setPomodoroVolume(val / 100)
}

function onTodoRemindVolumeChange(val: number) {
  appStore.setTodoRemindVolume(val / 100)
}

// ========== 像素狗物品管理 ==========

const shapeNames = [
  '蝴蝶结', '礼帽', '眼镜', '项圈', '皇冠',
  '星星', '爱心', '披风', '光环', '小点',
]

const dogItemsCollapsed = ref<string[]>([])
const dogItems = ref<PixelDogItemVO[]>([])
const loadingItems = ref(false)

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

onMounted(() => {
  loadItems()
})
</script>

<style scoped lang="scss">
.settings-section__title {
  font-size: 16px;
  font-weight: 700;
  margin: 0 0 16px;
}

.settings-slider-row {
  display: flex;
  align-items: center;
  margin-bottom: 4px;

  &__value {
    font-size: 14px;
    min-width: 48px;
    color: var(--el-text-color-primary);
  }

  &__desc {
    font-size: 12px;
    color: var(--el-text-color-secondary);
    line-height: 1.4;
  }
}

.settings-dog-items {
  margin-top: 20px;

  :deep(.el-collapse-item__header) {
    font-size: 16px;
    font-weight: 700;
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
