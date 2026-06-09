<template>
  <el-card shadow="never" class="dashboard-card">
    <el-empty :description="t('portal.comingSoon')" />
  </el-card>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>
