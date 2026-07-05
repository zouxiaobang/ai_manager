<template>
  <SchemeADoodleFrame shape="pill" color="#2563eb" class="mec-search">
    <div class="mec-search__inner">
      <img class="mec-search__icon" :src="schemeAAssets.search" alt="" />
      <input
        v-model="ec.searchQuery.value"
        type="search"
        enterkeyhint="search"
        :placeholder="ec.t('mobile.ecommerce.searchPlaceholder')"
      />
    </div>
  </SchemeADoodleFrame>
</template>

<script setup lang="ts">
import { inject } from 'vue'
import { MOBILE_ECOMMERCE_KEY } from '../mobileEcommerceContext'
import { schemeAAssets } from '@/mobile/views/home/themes/scheme-a/assets'
import SchemeADoodleFrame from '@/mobile/views/home/themes/scheme-a/SchemeADoodleFrame.vue'

const ec = inject(MOBILE_ECOMMERCE_KEY)!
</script>

<style scoped lang="scss">
.mec-search {
  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.mec-search__inner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;

  input {
    flex: 1;
    min-width: 0;
    border: none;
    outline: none;
    background: transparent;
    font-family: inherit;
    font-size: 16px;
    color: #1e293b;

    &::placeholder {
      color: #94a3b8;
    }
  }
}

.mec-search__icon {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
}
</style>
