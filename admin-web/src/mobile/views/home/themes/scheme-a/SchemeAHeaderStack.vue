<template>
  <div class="mh-scheme-a__header-stack">
    <div class="mh-scheme-a__header-art" aria-hidden="true">
      <img
        class="mh-scheme-a__hero-title"
        :src="schemeAAssets.heroTitleOverlay"
        alt=""
      />
      <img
        class="mh-scheme-a__hero-bear"
        :src="schemeAAssets.heroBearOverlay"
        alt=""
      />
    </div>
    <SchemeASearch class="mh-scheme-a__search--stacked" />
  </div>
</template>

<script setup lang="ts">
import SchemeASearch from './SchemeASearch.vue'
import { schemeAAssets } from './assets'
</script>
