<template>
  <div v-loading="ec.loading.value" class="mec-shell mh-scheme-a">
    <EcHeroHeader />
    <div class="mec-content">
      <EcOverviewPanel />
      <EcPendingOrders />
      <EcModuleGrid />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, provide } from 'vue'
import { MOBILE_ECOMMERCE_KEY } from '@/mobile/ecommerce/mobileEcommerceContext'
import { useMobileEcommerce } from '@/mobile/ecommerce/useMobileEcommerce'
import EcHeroHeader from '@/mobile/ecommerce/components/EcHeroHeader.vue'
import EcOverviewPanel from '@/mobile/ecommerce/components/EcOverviewPanel.vue'
import EcPendingOrders from '@/mobile/ecommerce/components/EcPendingOrders.vue'
import EcModuleGrid from '@/mobile/ecommerce/components/EcModuleGrid.vue'

const ec = useMobileEcommerce()
provide(MOBILE_ECOMMERCE_KEY, ec)

onMounted(() => {
  void ec.init()
})
</script>

<style lang="scss">
@use '@/mobile/home/themes/scheme-a/scheme-a.scss';
@use '@/mobile/ecommerce/styles/mobile-ecommerce.scss';
</style>
