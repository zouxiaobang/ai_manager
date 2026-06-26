<template>
  <WarRoomPage :title="t('ecommerce.title')" fill>
    <div class="war-room-panel war-room-panel--tabs">
      <el-tabs v-model="activeTab">
        <el-tab-pane :label="t('ecommerce.tabs.product')" name="product">
          <ProductPanel ref="productRef" />
        </el-tab-pane>
        <el-tab-pane :label="t('ecommerce.tabs.platformShop')" name="platformShop">
          <PlatformShopPanel ref="platformShopRef" />
        </el-tab-pane>
        <el-tab-pane :label="t('ecommerce.tabs.inventory')" name="inventory">
          <InventoryPanel ref="inventoryRef" @view-product="onViewProduct" />
        </el-tab-pane>
        <el-tab-pane :label="t('ecommerce.tabs.order')" name="order">
          <SalesOrderPanel ref="salesOrderRef" />
        </el-tab-pane>
        <el-tab-pane :label="t('ecommerce.tabs.monthlySettlement')" name="monthlySettlement">
          <MonthlySettlementPanel ref="monthlySettlementRef" />
        </el-tab-pane>
        <el-tab-pane :label="t('ecommerce.tabs.factory')" name="factory">
          <FactoryPanel ref="factoryRef" />
        </el-tab-pane>
        <el-tab-pane :label="t('ecommerce.tabs.carton')" name="carton">
          <CartonPanel ref="cartonRef" />
        </el-tab-pane>
        <el-tab-pane :label="t('ecommerce.tabs.express')" name="express">
          <ExpressPanel ref="expressRef" />
        </el-tab-pane>
      </el-tabs>
    </div>
  </WarRoomPage>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import WarRoomPage from '@/components/war-room/WarRoomPage.vue'
import ProductPanel from './ecommerce/ProductPanel.vue'
import PlatformShopPanel from './ecommerce/PlatformShopPanel.vue'
import FactoryPanel from './ecommerce/FactoryPanel.vue'
import CartonPanel from './ecommerce/CartonPanel.vue'
import ExpressPanel from './ecommerce/ExpressPanel.vue'
import InventoryPanel from './ecommerce/InventoryPanel.vue'
import SalesOrderPanel from './ecommerce/SalesOrderPanel.vue'
import MonthlySettlementPanel from './ecommerce/MonthlySettlementPanel.vue'

const { t } = useI18n()
const activeTab = ref('product')
const productRef = ref<InstanceType<typeof ProductPanel> | null>(null)
const platformShopRef = ref<InstanceType<typeof PlatformShopPanel> | null>(null)
const factoryRef = ref<InstanceType<typeof FactoryPanel> | null>(null)
const cartonRef = ref<InstanceType<typeof CartonPanel> | null>(null)
const expressRef = ref<InstanceType<typeof ExpressPanel> | null>(null)
const inventoryRef = ref<InstanceType<typeof InventoryPanel> | null>(null)
const salesOrderRef = ref<InstanceType<typeof SalesOrderPanel> | null>(null)
const monthlySettlementRef = ref<InstanceType<typeof MonthlySettlementPanel> | null>(null)

watch(activeTab, (tab) => {
  if (tab === 'product') {
    productRef.value?.loadProducts()
  } else if (tab === 'platformShop') {
    platformShopRef.value?.loadAll()
  } else if (tab === 'factory') {
    factoryRef.value?.loadFactories()
  } else if (tab === 'carton') {
    cartonRef.value?.loadCartons()
  } else if (tab === 'express') {
    expressRef.value?.loadStations()
  } else if (tab === 'inventory') {
    inventoryRef.value?.loadInventories()
  } else if (tab === 'order') {
    salesOrderRef.value?.load()
  } else if (tab === 'monthlySettlement') {
    monthlySettlementRef.value?.load()
  }
})

async function onViewProduct(productId: number) {
  activeTab.value = 'product'
  await productRef.value?.openEdit(productId)
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
