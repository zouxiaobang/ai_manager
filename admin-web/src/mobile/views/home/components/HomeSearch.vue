<template>
  <div class="mh-doodle__search">
    <el-input
      v-model="searchQuery"
      :placeholder="home.t('mobile.home.searchPlaceholder')"
      clearable
      :prefix-icon="Search"
    />
  </div>
</template>

<script setup lang="ts">
import { inject } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { MOBILE_HOME_KEY } from '../mobileHomeContext'

const home = inject(MOBILE_HOME_KEY)!
const { searchQuery } = home
</script>
