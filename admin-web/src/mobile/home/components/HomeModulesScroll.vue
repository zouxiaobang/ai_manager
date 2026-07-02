<template>
  <section v-if="home.showFunctionsSection.value" class="mh-doodle__card" :class="cardClass">
    <h2 v-if="showTitle" class="mh-doodle__title">{{ home.t('mobile.home.allFunctions') }}</h2>
    <div v-if="home.filteredModules.value.length" class="mh-doodle__module-scroll">
      <button
        v-for="item in home.filteredModules.value"
        :key="item.key"
        type="button"
        class="mh-doodle__module mh-doodle__module--scroll mh-doodle__tap"
        :style="home.moduleCardStyle(item)"
        @click="home.openModule(item)"
      >
        <span class="mh-doodle__module-icon-wrap" :style="{ background: `${item.barColor}22` }">
          <img class="mh-doodle__module-icon" :src="home.iconUrl(item.icon)" :alt="home.moduleName(item)" />
        </span>
        <span class="mh-doodle__module-name">{{ home.moduleName(item) }}</span>
        <span class="mh-doodle__module-desc">{{ home.moduleDesc(item) }}</span>
      </button>
    </div>
    <div v-else class="mobile-empty-hint">{{ home.t('mobile.home.searchEmpty') }}</div>
  </section>
</template>

<script setup lang="ts">
import { inject } from 'vue'
import { MOBILE_HOME_KEY } from '../mobileHomeContext'

withDefaults(
  defineProps<{
    cardClass?: string
    showTitle?: boolean
  }>(),
  { showTitle: true },
)

const home = inject(MOBILE_HOME_KEY)!
</script>
