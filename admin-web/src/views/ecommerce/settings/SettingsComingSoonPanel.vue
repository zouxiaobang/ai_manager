<template>
  <div class="ec-settings-soon">
    <el-empty :description="t('ecommerce.settings.comingSoonItemDesc')">
      <el-button @click="emit('back')">{{ t('ecommerce.settings.backToHub') }}</el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const emit = defineEmits<{
  back: []
}>()

const { t } = useI18n()
</script>

<style scoped>
.ec-settings-soon {
  padding: 48px 24px;
}
</style>
