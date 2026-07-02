<template>
  <SchemeADoodleFrame
    shape="pill"
    color="#2563eb"
    class="mh-scheme-a__search"
    :seed="doodleSeedFromKey('home-search')"
    sketch
    :stroke-width="3"
    :shadow="false"
  >
    <div class="mh-scheme-a__search-inner">
      <img class="mh-scheme-a__search-icon" :src="schemeAAssets.search" alt="" />
      <input
        v-model="searchQuery"
        type="search"
        enterkeyhint="search"
        :placeholder="home.t('mobile.home.searchPlaceholderSchemeA')"
      />
    </div>
  </SchemeADoodleFrame>
</template>

<script setup lang="ts">
import { inject } from 'vue'
import { MOBILE_HOME_KEY } from '../../mobileHomeContext'
import { schemeAAssets } from './assets'
import { doodleSeedFromKey } from '@/mobile/utils/doodleSeed'
import SchemeADoodleFrame from './SchemeADoodleFrame.vue'

const home = inject(MOBILE_HOME_KEY)!
const { searchQuery } = home
</script>
