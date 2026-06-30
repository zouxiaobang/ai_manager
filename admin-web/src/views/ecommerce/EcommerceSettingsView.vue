<template>
  <div class="ec-settings-page war-room-page war-room-page--fill">
    <div class="war-room-panel ec-settings-page__panel">
      <SettingsHub
        v-if="viewMode === 'hub'"
        ref="hubRef"
        @open-item="openItem"
        @open-category="openCategory"
      />

      <SettingsCategoryView
        v-else-if="viewMode === 'category' && activeCategory"
        :category-key="activeCategory"
        @back="goHub"
        @open-item="openItem"
      />

      <section v-else-if="viewMode === 'item' && activeItem" class="ec-settings-detail">
        <header class="ec-settings-detail__header">
          <button type="button" class="ec-settings-detail__back" @click="goBackFromItem">
            <el-icon><ArrowLeft /></el-icon>
            {{ backLabel }}
          </button>
          <div>
            <h2 class="ec-settings-detail__title">{{ t(activeItem.labelKey) }}</h2>
            <p class="ec-settings-detail__desc">{{ t(activeItem.descKey) }}</p>
          </div>
        </header>

        <PurchaseOrderConfigPanel
          v-if="activePanel === 'purchase-order'"
          @saved="onItemSaved"
        />
        <OutboundOrderConfigPanel
          v-else-if="activePanel === 'outbound-order'"
          @saved="onItemSaved"
        />
        <OrderImportStatusPanel
          v-else-if="activePanel === 'order-import-status'"
          @saved="onItemSaved"
        />
        <SettlementSettingsPanel
          v-else-if="activePanel === 'profit-rules'"
          @saved="onItemSaved"
        />
        <RebateSettingsPanel
          v-else-if="activePanel === 'rebate-default'"
          @saved="onItemSaved"
        />
        <NotificationSettingsPanel
          v-else-if="activePanel === 'notification'"
          @saved="onItemSaved"
        />
        <DataRetentionSettingsPanel
          v-else-if="activePanel === 'data-retention'"
          @saved="onItemSaved"
        />
        <InventorySettingsPanel
          v-else-if="activePanel === 'inventory-defaults'"
          @saved="onItemSaved"
        />
        <OrderImportSettingsPanel
          v-else-if="activePanel === 'import-template'"
          @saved="onItemSaved"
        />
        <ExpressSettingsPanel
          v-else-if="activePanel === 'express-bill-mapping'"
          @saved="onItemSaved"
        />
        <DeliveryNoteConfigPanel
          v-else-if="activePanel === 'delivery-note'"
          @saved="onItemSaved"
        />
        <CompanyInfoPanel
          v-else-if="activePanel === 'company-info'"
          @saved="onItemSaved"
        />
        <SettingsComingSoonPanel v-else @back="goBackFromItem" />
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ArrowLeft } from '@element-plus/icons-vue'
import {
  getSettingsCategory,
  getSettingsItem,
  isSettingsItemKey,
  resolveSettingsPanelKey,
  type EcSettingsCategoryKey,
  type EcSettingsItemKey,
  type EcSettingsPanelKey,
} from '@/data/ec-settings-catalog'
import SettingsHub from './settings/SettingsHub.vue'
import SettingsCategoryView from './settings/SettingsCategoryView.vue'
import OutboundOrderConfigPanel from './settings/OutboundOrderConfigPanel.vue'
import OrderImportStatusPanel from './settings/OrderImportStatusPanel.vue'
import SettlementSettingsPanel from './settings/SettlementSettingsPanel.vue'
import RebateSettingsPanel from './settings/RebateSettingsPanel.vue'
import NotificationSettingsPanel from './settings/NotificationSettingsPanel.vue'
import DataRetentionSettingsPanel from './settings/DataRetentionSettingsPanel.vue'
import PurchaseOrderConfigPanel from './settings/PurchaseOrderConfigPanel.vue'
import InventorySettingsPanel from './settings/InventorySettingsPanel.vue'
import OrderImportSettingsPanel from './settings/OrderImportSettingsPanel.vue'
import ExpressSettingsPanel from './settings/ExpressSettingsPanel.vue'
import DeliveryNoteConfigPanel from './settings/DeliveryNoteConfigPanel.vue'
import CompanyInfoPanel from './settings/CompanyInfoPanel.vue'
import SettingsComingSoonPanel from './settings/SettingsComingSoonPanel.vue'
import { useEcSettingsStore } from '@/stores/ecSettings'

type ViewMode = 'hub' | 'category' | 'item'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const hubRef = ref<InstanceType<typeof SettingsHub> | null>(null)
const activeCategory = ref<EcSettingsCategoryKey | null>(null)
const activeItemKey = ref<EcSettingsItemKey | null>(null)

const activeItem = computed(() => {
  if (!activeItemKey.value) return null
  return getSettingsItem(activeItemKey.value) ?? null
})

const activePanel = computed<EcSettingsPanelKey | undefined>(() => {
  if (!activeItemKey.value) return undefined
  return resolveSettingsPanelKey(activeItemKey.value)
})

const viewMode = computed<ViewMode>(() => {
  if (activeItemKey.value) return 'item'
  if (activeCategory.value) return 'category'
  return 'hub'
})

const backLabel = computed(() => {
  if (activeCategory.value) {
    const category = getSettingsCategory(activeCategory.value)
    return category ? t(category.labelKey) : t('ecommerce.settings.backToHub')
  }
  return t('ecommerce.settings.backToHub')
})

function syncFromRoute() {
  const item = route.query.item
  const category = route.query.category

  if (typeof item === 'string' && isSettingsItemKey(item)) {
    activeItemKey.value = item
    const itemDef = getSettingsItem(item)
    activeCategory.value = itemDef?.categoryKey ?? null
    return
  }

  activeItemKey.value = null

  if (typeof category === 'string' && getSettingsCategory(category as EcSettingsCategoryKey)) {
    activeCategory.value = category as EcSettingsCategoryKey
    return
  }

  activeCategory.value = null
}

function updateRouteQuery(patch: { category?: string; item?: string }) {
  const nextQuery: Record<string, string | string[]> = { ...route.query } as Record<string, string | string[]>

  if ('category' in patch) {
    if (patch.category) nextQuery.category = patch.category
    else delete nextQuery.category
  }
  if ('item' in patch) {
    if (patch.item) nextQuery.item = patch.item
    else delete nextQuery.item
  }

  void router.replace({
    path: route.path,
    query: nextQuery,
  })
}

function openItem(itemKey: EcSettingsItemKey) {
  const item = getSettingsItem(itemKey)
  activeItemKey.value = itemKey
  activeCategory.value = item?.categoryKey ?? null
  updateRouteQuery({ item: itemKey, category: undefined })
}

function openCategory(categoryKey: EcSettingsCategoryKey) {
  activeCategory.value = categoryKey
  activeItemKey.value = null
  updateRouteQuery({ category: categoryKey, item: undefined })
}

function goHub() {
  activeCategory.value = null
  activeItemKey.value = null
  updateRouteQuery({ category: undefined, item: undefined })
}

function goBackFromItem() {
  if (activeCategory.value) {
    activeItemKey.value = null
    updateRouteQuery({ item: undefined, category: activeCategory.value })
    return
  }
  goHub()
}

function onItemSaved() {
  hubRef.value?.refreshRecentChanges()
  useEcSettingsStore().invalidate()
}

watch(
  () => [route.query.item, route.query.category],
  () => {
    syncFromRoute()
  },
  { immediate: true },
)

defineExpose({
  goHub,
})
</script>

<style scoped lang="scss">
.ec-settings-page {
  display: flex;
  flex-direction: column;
  padding: 20px 24px 24px;
}

.ec-settings-page__panel {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 20px 24px 32px;
}

.ec-settings-detail__header {
  margin-bottom: 20px;
}

.ec-settings-detail__back {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 14px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--el-color-primary);
  font-size: 13px;
  cursor: pointer;
}

.ec-settings-detail__title {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 600;
}

.ec-settings-detail__desc {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
</style>
