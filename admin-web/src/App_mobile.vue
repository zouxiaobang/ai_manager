<template>
  <el-config-provider :locale="elementLocale">
    <router-view />
  </el-config-provider>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import en from 'element-plus/es/locale/lang/en'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { initBaiduPanAutoAuth } from '@/composables/useBaiduPanAutoAuth'

const appStore = useAppStore()
const router = useRouter()

initBaiduPanAutoAuth(router)

const elementLocale = computed(() => (appStore.locale === 'zh-CN' ? zhCn : en))
</script>
