<template>
  <section v-if="home.showFunctionsSection.value" class="mh-scheme-a__section">
    <div class="mh-scheme-a__section-head">
      <img class="mh-scheme-a__section-icon" :src="schemeAAssets.starBlueOutline" alt="" />
      <h2 class="mh-scheme-a__section-title">{{ home.t('mobile.home.allFunctions') }}</h2>
    </div>

    <div v-if="home.filteredModules.value.length" class="mh-scheme-a__module-grid">
      <SchemeADoodleFrame
        v-for="item in home.filteredModules.value"
        :key="item.key"
        tag="button"
        type="button"
        class="mh-scheme-a__module"
        :color="schemeAModuleBorderColors[item.key] ?? '#93c5fd'"
        :seed="doodleSeedFromKey(item.key)"
        sketch
        :stroke-width="3"
        :shadow="false"
        @click="home.openModule(item)"
      >
        <div class="mh-scheme-a__module-inner">
          <img
            class="mh-scheme-a__module-icon"
            :src="schemeAModuleIcons[item.key] ?? schemeAAssets.notebook"
            alt=""
          />
          <span class="mh-scheme-a__module-text">
            <span class="mh-scheme-a__module-name">{{ home.moduleName(item) }}</span>
            <span
              class="mh-scheme-a__module-squiggle"
              :style="{ '--sa-module-squiggle': schemeAModuleSquiggleColors[item.key] }"
            />
          </span>
        </div>
      </SchemeADoodleFrame>
    </div>
    <div v-else class="mh-scheme-a__empty">{{ home.t('mobile.home.searchEmpty') }}</div>
  </section>
</template>

<script setup lang="ts">
import { inject } from 'vue'
import { MOBILE_HOME_KEY } from '../../mobileHomeContext'
import {
  schemeAAssets,
  schemeAModuleBorderColors,
  schemeAModuleIcons,
  schemeAModuleSquiggleColors,
} from './assets'
import { doodleSeedFromKey } from '@/mobile/utils/doodleSeed'
import SchemeADoodleFrame from './SchemeADoodleFrame.vue'

const home = inject(MOBILE_HOME_KEY)!
</script>
