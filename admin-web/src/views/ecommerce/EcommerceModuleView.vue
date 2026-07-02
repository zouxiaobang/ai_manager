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
import { computed, defineAsyncComponent, nextTick, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { EcommerceWorkbenchModule } from '@/data/ecommerce-nav'

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

const route = useRoute()
const router = useRouter()

const module = computed(() => route.meta.module as EcommerceWorkbenchModule)

const productRef = ref<EcommercePanelExpose | null>(null)
const platformShopRef = ref<EcommercePanelExpose | null>(null)
const factoryRef = ref<EcommercePanelExpose | null>(null)
const cartonRef = ref<EcommercePanelExpose | null>(null)
const expressRef = ref<EcommercePanelExpose | null>(null)
const inventoryRef = ref<EcommercePanelExpose | null>(null)
const salesOrderRef = ref<EcommercePanelExpose | null>(null)
const monthlySettlementRef = ref<EcommercePanelExpose | null>(null)

async function loadModule(tab: EcommerceWorkbenchModule) {
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
}

async function onViewProduct(productId: number) {
  await router.push({ path: '/ecommerce/products', query: { editId: String(productId) } })
  await nextTick()
  await productRef.value?.openEdit?.(productId)
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
    await productRef.value?.openEdit?.(id)
  },
  { immediate: true },
)
</script>

<style scoped lang="scss">
.ec-module-page {
  display: flex;
  flex-direction: column;
  padding: 0;
}

.ec-module-page__panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 0;

  :deep(.panel-toolbar) {
    margin-bottom: 12px;
  }

  :deep(.el-table) {
    flex: 1;
  }
}
</style>
