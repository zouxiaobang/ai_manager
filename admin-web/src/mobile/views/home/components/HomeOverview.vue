<template>
  <div class="mh-doodle__overview">
    <button
      type="button"
      class="mh-doodle__metric mh-doodle__metric--green mh-doodle__tap"
      @click="home.router.push('/todos')"
    >
      <div class="mh-doodle__ring" :style="{ '--progress': home.todoProgressRatio.value }">
        <span class="mh-doodle__ring-text">{{ home.todoProgressText.value }}</span>
      </div>
      <span class="mh-doodle__metric-label">{{ home.t('mobile.home.todoProgress') }}</span>
    </button>
    <button
      type="button"
      class="mh-doodle__metric mh-doodle__metric--blue mh-doodle__tap"
      @click="home.router.push('/notebook')"
    >
      <img class="mh-doodle__metric-icon" :src="home.iconUrl('notebook')" alt="" />
      <span class="mh-doodle__metric-value">
        {{ home.noteCount.value }}<small>{{ home.t('mobile.home.notesUnit') }}</small>
      </span>
      <span class="mh-doodle__metric-label">{{ home.t('mobile.home.notesCount') }}</span>
    </button>
    <button
      type="button"
      class="mh-doodle__metric mh-doodle__metric--orange mh-doodle__tap"
      @click="home.router.push('/ecommerce')"
    >
      <img class="mh-doodle__metric-icon" :src="home.iconUrl('ecommerce')" alt="" />
      <span class="mh-doodle__metric-value mh-doodle__metric-value--sm">
        {{ home.t('mobile.home.ecommerceEnter') }}
      </span>
      <span class="mh-doodle__metric-label">{{ home.t('mobile.home.ecommercePending') }}</span>
    </button>
    <div class="mh-doodle__metric mh-doodle__metric--system">
      <span class="mh-doodle__system-badge" :class="home.systemBadgeClass.value">
        <el-icon v-if="home.healthStatus.value === 'up'"><CircleCheckFilled /></el-icon>
        <el-icon v-else-if="home.healthStatus.value === 'down'"><CircleCloseFilled /></el-icon>
        <el-icon v-else><QuestionFilled /></el-icon>
      </span>
      <span class="mh-doodle__metric-value mh-doodle__metric-value--sm">{{ home.systemOverviewText.value }}</span>
      <span class="mh-doodle__metric-label">{{ home.t('mobile.home.systemStatus') }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { inject } from 'vue'
import { CircleCheckFilled, CircleCloseFilled, QuestionFilled } from '@element-plus/icons-vue'
import { MOBILE_HOME_KEY } from '../mobileHomeContext'

const home = inject(MOBILE_HOME_KEY)!
</script>
