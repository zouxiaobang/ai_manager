<template>
  <WarRoomPage :title="t('ecommerce.title')" fill>
    <div class="war-room-panel war-room-panel--tabs">
      <el-tabs v-model="activeTab">
        <el-tab-pane :label="t('ecommerce.tabs.monthlySettlement')" name="monthlySettlement">
          <MonthlySettlementPanel v-if="activeTab === 'monthlySettlement'" ref="monthlySettlementRef" />
        </el-tab-pane>
        <el-tab-pane :label="t('ecommerce.tabs.order')" name="order">
          <SalesOrderPanel v-if="activeTab === 'order'" ref="salesOrderRef" />
        </el-tab-pane>
        <el-tab-pane :label="t('ecommerce.tabs.inventory')" name="inventory">
          <InventoryPanel
            v-if="activeTab === 'inventory'"
            ref="inventoryRef"
            @view-product="onViewProduct"
          />
        </el-tab-pane>
        <el-tab-pane :label="t('ecommerce.tabs.product')" name="product">
          <ProductPanel v-if="activeTab === 'product'" ref="productRef" />
        </el-tab-pane>
        <el-tab-pane :label="t('ecommerce.tabs.express')" name="express">
          <ExpressPanel v-if="activeTab === 'express'" ref="expressRef" />
        </el-tab-pane>
        <el-tab-pane :label="t('ecommerce.tabs.factory')" name="factory">
          <FactoryPanel v-if="activeTab === 'factory'" ref="factoryRef" />
        </el-tab-pane>
        <el-tab-pane :label="t('ecommerce.tabs.platformShop')" name="platformShop">
          <PlatformShopPanel v-if="activeTab === 'platformShop'" ref="platformShopRef" />
        </el-tab-pane>
        <el-tab-pane :label="t('ecommerce.tabs.carton')" name="carton">
          <CartonPanel v-if="activeTab === 'carton'" ref="cartonRef" />
        </el-tab-pane>
      </el-tabs>
    </div>
  </WarRoomPage>
</template>

<script setup lang="ts">
import { defineAsyncComponent, nextTick, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import WarRoomPage from '@/components/war-room/WarRoomPage.vue'

const ProductPanel = defineAsyncComponent(() => import('./ProductPanel.vue'))
const PlatformShopPanel = defineAsyncComponent(() => import('./PlatformShopPanel.vue'))
const FactoryPanel = defineAsyncComponent(() => import('./FactoryPanel.vue'))
const CartonPanel = defineAsyncComponent(() => import('./CartonPanel.vue'))
const ExpressPanel = defineAsyncComponent(() => import('./ExpressPanel.vue'))
const InventoryPanel = defineAsyncComponent(() => import('./InventoryPanel.vue'))
const SalesOrderPanel = defineAsyncComponent(() => import('./SalesOrderPanel.vue'))
const MonthlySettlementPanel = defineAsyncComponent(() => import('./MonthlySettlementPanel.vue'))

interface EcommercePanelExpose {
  loadProducts?: () => Promise<void>
  loadAll?: () => Promise<void>
  loadFactories?: () => Promise<void>
  loadCartons?: () => Promise<void>
  loadStations?: () => Promise<void>
  loadInventories?: () => Promise<void>
  load?: () => Promise<void>
  enter?: () => Promise<void>
  openEdit?: (id: number) => Promise<void>
}

const { t } = useI18n()
const route = useRoute()

const TAB_NAMES = [
  'monthlySettlement',
  'order',
  'inventory',
  'product',
  'express',
  'factory',
  'platformShop',
  'carton',
] as const

type TabName = (typeof TAB_NAMES)[number]

function resolveTab(tab: unknown): TabName {
  if (typeof tab === 'string' && TAB_NAMES.includes(tab as TabName)) {
    return tab as TabName
  }
  return 'monthlySettlement'
}

const activeTab = ref<TabName>(resolveTab(route.query.tab))
const productRef = ref<EcommercePanelExpose | null>(null)
const platformShopRef = ref<EcommercePanelExpose | null>(null)
const factoryRef = ref<EcommercePanelExpose | null>(null)
const cartonRef = ref<EcommercePanelExpose | null>(null)
const expressRef = ref<EcommercePanelExpose | null>(null)
const inventoryRef = ref<EcommercePanelExpose | null>(null)
const salesOrderRef = ref<EcommercePanelExpose | null>(null)
const monthlySettlementRef = ref<EcommercePanelExpose | null>(null)

watch(
  () => route.query.tab,
  (tab) => {
    activeTab.value = resolveTab(tab)
  },
)

watch(activeTab, async (tab) => {
  await nextTick()
  if (tab === 'product') {
    await productRef.value?.loadProducts?.()
  } else if (tab === 'platformShop') {
    await platformShopRef.value?.loadAll?.()
  } else if (tab === 'factory') {
    await factoryRef.value?.loadFactories?.()
  } else if (tab === 'carton') {
    await cartonRef.value?.loadCartons?.()
  } else if (tab === 'express') {
    await expressRef.value?.loadStations?.()
  } else if (tab === 'inventory') {
    await inventoryRef.value?.loadInventories?.()
  } else if (tab === 'order') {
    await salesOrderRef.value?.load?.()
  } else if (tab === 'monthlySettlement') {
    await monthlySettlementRef.value?.enter?.()
  }
}, { immediate: true })

async function onViewProduct(productId: number) {
  activeTab.value = 'product'
  await nextTick()
  await productRef.value?.openEdit?.(productId)
}
</script>

<style scoped lang="scss">
.war-room-panel--tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  :deep(.el-tabs) {
    display: flex;
    flex-direction: column;
    height: 100%;
    min-height: 0;
  }

  :deep(.el-tabs__content) {
    flex: 1;
    min-height: 0;
    overflow: auto;
  }
}
</style>
