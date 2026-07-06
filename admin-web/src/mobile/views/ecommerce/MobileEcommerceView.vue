<template>
  <div v-loading="ec.loading.value" class="mec-shell mh-scheme-a">
    <EcHeroHeader />
    <div class="mec-content">
      <EcModuleGrid />
    </div>
  </div>
</template>

<script setup lang="ts">
import {onMounted, provide} from 'vue'
import {MOBILE_ECOMMERCE_KEY} from '@/mobile/views/ecommerce/mobileEcommerceContext'
import {useMobileEcommerce} from '@/mobile/views/ecommerce/useMobileEcommerce'
import EcHeroHeader from '@/mobile/views/ecommerce/components/EcHeroHeader.vue'
import EcModuleGrid from '@/mobile/views/ecommerce/components/EcModuleGrid.vue'

defineOptions({ name: 'MobileEcommerceView' })

const ec = useMobileEcommerce()
provide(MOBILE_ECOMMERCE_KEY, ec)

onMounted(() => {
  void ec.init()
})
</script>

<style lang="scss">
@use '@/mobile/views/home/themes/scheme-a/scheme-a.scss';
@use '@/mobile/views/ecommerce/styles/mobile-ecommerce.scss';
</style>
