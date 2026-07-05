<template>
  <SchemeADoodleFrame shape="pill" color="#8b5cf6" sketch :stroke-width="2.5" class="mfc-search">
    <div class="mfc-search__inner">
      <img class="mfc-search__icon" :src="schemeAAssets.search" alt="" />
      <input
        v-model="factory.searchQuery.value"
        type="search"
        enterkeyhint="search"
        :placeholder="factory.t('ecommerce.factory.searchPlaceholder')"
      />
    </div>
  </SchemeADoodleFrame>
</template>

<script setup lang="ts">
import { inject } from 'vue'
import { MOBILE_FACTORY_KEY } from '../factoryContext'
import { schemeAAssets } from '@/mobile/views/home/themes/scheme-a/assets'
import SchemeADoodleFrame from '@/mobile/views/home/themes/scheme-a/SchemeADoodleFrame.vue'

const factory = inject(MOBILE_FACTORY_KEY)!
</script>

<style scoped lang="scss">
.mfc-search {
  :deep(.sa-doodle-frame__body) {
    padding: 0;
  }
}

.mfc-search__inner {
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

.mfc-search__icon {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
}
</style>
