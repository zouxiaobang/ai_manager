<template>
  <section v-if="home.showOverview.value" class="mh-scheme-a__section">
    <div class="mh-scheme-a__section-head">
      <img class="mh-scheme-a__section-icon" :src="schemeAAssets.starYellow" alt="" />
      <h2 class="mh-scheme-a__section-title">{{ home.t('mobile.home.overviewTitle') }}</h2>
      <img class="mh-scheme-a__section-deco" :src="schemeAAssets.squiggleBlue" alt="" />
    </div>

    <div class="mh-scheme-a__overview-grid">
      <SchemeADoodleFrame
        tag="button"
        type="button"
        class="mh-scheme-a__metric mh-scheme-a__metric--green"
        color="#22c55e"
        :seed="doodleSeedFromKey('metric-todo')"
        sketch
        :stroke-width="3"
        :shadow="false"
        @click="home.router.push('/todos')"
      >
        <div class="mh-scheme-a__metric-inner">
          <div class="mh-scheme-a__ring" :style="{ '--progress': home.todoProgressRatio.value }">
            <span class="mh-scheme-a__ring-text">{{ home.todoProgressText.value }}</span>
          </div>
          <div class="mh-scheme-a__metric-body">
            <div class="mh-scheme-a__metric-value mh-scheme-a__metric-value--green">
              {{ home.todoProgressText.value }}
            </div>
            <div class="mh-scheme-a__metric-label">{{ home.t('mobile.home.todoProgress') }}</div>
          </div>
        </div>
      </SchemeADoodleFrame>

      <SchemeADoodleFrame
        tag="button"
        type="button"
        class="mh-scheme-a__metric mh-scheme-a__metric--blue"
        color="#3b82f6"
        :seed="doodleSeedFromKey('metric-notebook')"
        sketch
        :stroke-width="3"
        :shadow="false"
        @click="home.router.push('/notebook')"
      >
        <div class="mh-scheme-a__metric-inner">
          <img class="mh-scheme-a__metric-icon" :src="schemeAAssets.notebook" alt="" />
          <div class="mh-scheme-a__metric-body">
            <div class="mh-scheme-a__metric-value mh-scheme-a__metric-value--blue">
              {{ home.noteCount.value }}<span class="mh-scheme-a__metric-unit">{{ home.t('mobile.home.notesUnit') }}</span>
            </div>
            <div class="mh-scheme-a__metric-label">{{ home.t('mobile.home.notesCount') }}</div>
          </div>
        </div>
      </SchemeADoodleFrame>

      <SchemeADoodleFrame
        tag="button"
        type="button"
        class="mh-scheme-a__metric mh-scheme-a__metric--yellow"
        color="#f59e0b"
        :seed="doodleSeedFromKey('metric-ecommerce')"
        sketch
        :stroke-width="3"
        :shadow="false"
        @click="home.router.push('/ecommerce')"
      >
        <div class="mh-scheme-a__metric-inner">
          <img class="mh-scheme-a__metric-icon" :src="schemeAAssets.cart" alt="" />
          <div class="mh-scheme-a__metric-body">
            <div class="mh-scheme-a__metric-value mh-scheme-a__metric-value--orange">
              {{ home.t('mobile.home.ecommerceLabel') }}
            </div>
            <div class="mh-scheme-a__metric-label mh-scheme-a__metric-label--link">
              {{ home.t('mobile.home.ecommerceEnter') }} &gt;
            </div>
          </div>
        </div>
      </SchemeADoodleFrame>

      <SchemeADoodleFrame
        class="mh-scheme-a__metric mh-scheme-a__metric--grey"
        color="#94a3b8"
        :seed="doodleSeedFromKey('metric-system')"
        sketch
        :stroke-width="3"
        :shadow="false"
      >
        <div class="mh-scheme-a__metric-inner">
          <img class="mh-scheme-a__metric-icon" :src="schemeAAssets.shield" alt="" />
          <div class="mh-scheme-a__metric-body">
            <div class="mh-scheme-a__metric-value">{{ home.t('mobile.home.systemStatus') }}</div>
            <div class="mh-scheme-a__metric-label">{{ home.systemOverviewText.value }}</div>
          </div>
        </div>
      </SchemeADoodleFrame>
    </div>
  </section>
</template>

<script setup lang="ts">
import { inject } from 'vue'
import { MOBILE_HOME_KEY } from '../../mobileHomeContext'
import { schemeAAssets } from './assets'
import { doodleSeedFromKey } from '@/mobile/utils/doodleSeed'
import SchemeADoodleFrame from './SchemeADoodleFrame.vue'

const home = inject(MOBILE_HOME_KEY)!
</script>
