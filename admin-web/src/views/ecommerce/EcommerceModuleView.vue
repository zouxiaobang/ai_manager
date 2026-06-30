<template>
  <div class="ec-module-page war-room-page war-room-page--fill">
    <div class="war-room-panel ec-module-page__panel">
      <ProductPanel
        v-if="module === 'product'"
        ref="productRef"
      />
      <PlatformShopPanel
        v-else-if="module === 'platformShop'"
        ref="platformShopRef"
      />
      <InventoryPanel
        v-else-if="module === 'inventory'"
        ref="inventoryRef"
        @view-product="onViewProduct"
      />
      <SalesOrderPanel
        v-else-if="module === 'order'"
        ref="salesOrderRef"
      />
      <MonthlySettlementPanel
        v-else-if="module === 'monthlySettlement'"
        ref="monthlySettlementRef"
      />
      <FactoryPanel
        v-else-if="module === 'factory'"
        ref="factoryRef"
      />
      <CartonPanel
        v-else-if="module === 'carton'"
        ref="cartonRef"
      />
      <ExpressPanel
        v-else-if="module === 'express'"
        ref="expressRef"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { EcommerceWorkbenchModule } from '@/data/ecommerce-nav'
import ProductPanel from './ProductPanel.vue'
import PlatformShopPanel from './PlatformShopPanel.vue'
import FactoryPanel from './FactoryPanel.vue'
import CartonPanel from './CartonPanel.vue'
import ExpressPanel from './ExpressPanel.vue'
import InventoryPanel from './InventoryPanel.vue'
import SalesOrderPanel from './SalesOrderPanel.vue'
import MonthlySettlementPanel from './MonthlySettlementPanel.vue'

const route = useRoute()
const router = useRouter()

const module = computed(() => route.meta.module as EcommerceWorkbenchModule)

const productRef = ref<InstanceType<typeof ProductPanel> | null>(null)
const platformShopRef = ref<InstanceType<typeof PlatformShopPanel> | null>(null)
const factoryRef = ref<InstanceType<typeof FactoryPanel> | null>(null)
const cartonRef = ref<InstanceType<typeof CartonPanel> | null>(null)
const expressRef = ref<InstanceType<typeof ExpressPanel> | null>(null)
const inventoryRef = ref<InstanceType<typeof InventoryPanel> | null>(null)
const salesOrderRef = ref<InstanceType<typeof SalesOrderPanel> | null>(null)
const monthlySettlementRef = ref<InstanceType<typeof MonthlySettlementPanel> | null>(null)

async function loadModule(tab: EcommerceWorkbenchModule) {
  await nextTick()
  if (tab === 'product') {
    await productRef.value?.loadProducts()
  } else if (tab === 'platformShop') {
    await platformShopRef.value?.loadAll()
  } else if (tab === 'factory') {
    await factoryRef.value?.loadFactories()
  } else if (tab === 'carton') {
    await cartonRef.value?.loadCartons()
  } else if (tab === 'express') {
    await expressRef.value?.loadStations()
  } else if (tab === 'inventory') {
    await inventoryRef.value?.loadInventories()
  } else if (tab === 'order') {
    await salesOrderRef.value?.load()
  } else if (tab === 'monthlySettlement') {
    await monthlySettlementRef.value?.enter()
  }
}

async function onViewProduct(productId: number) {
  await router.push({ path: '/ecommerce/products', query: { editId: String(productId) } })
  await nextTick()
  await productRef.value?.openEdit(productId)
}

watch(
  module,
  (tab) => {
    if (tab) void loadModule(tab)
  },
  { immediate: true },
)

watch(
  () => route.query.editId,
  async (editId) => {
    if (module.value !== 'product' || !editId) return
    const id = Number(editId)
    if (!Number.isFinite(id)) return
    await nextTick()
    await productRef.value?.openEdit(id)
  },
  { immediate: true },
)
</script>

<style scoped lang="scss">
.ec-module-page {
  display: flex;
  flex-direction: column;
  padding: 20px 24px 24px;
}

.ec-module-page__panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 16px 20px;

  :deep(.panel-toolbar) {
    margin-bottom: 12px;
  }

  :deep(.el-table) {
    flex: 1;
  }
}
</style>
