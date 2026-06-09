<template>
  <el-config-provider :locale="elementLocale" :dialog="globalDialogConfig">
    <router-view />
  </el-config-provider>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import en from 'element-plus/es/locale/lang/en'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { useAppStore } from '@/stores/app'
import { globalDialogConfig } from '@/plugins/draggableDialog'

const appStore = useAppStore()

const elementLocale = computed(() => (appStore.locale === 'zh-CN' ? zhCn : en))
</script>
