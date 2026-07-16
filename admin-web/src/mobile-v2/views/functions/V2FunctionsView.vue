<template>
  <V2Page>
    <V2Card v-for="item in functionItems" :key="item.key" class="v2-functions-item" @click="openFunction(item)">
      <div class="mobile-v2-list-item">
        <el-icon :size="20"><component :is="item.icon" /></el-icon>
        <div class="mobile-v2-list-item__body">
          <div class="mobile-v2-list-item__title">{{ t(`functions.items.${item.key}.name`) }}</div>
          <div class="mobile-v2-list-item__meta">{{ t(`functions.items.${item.key}.desc`) }}</div>
        </div>
        <el-icon><ArrowRight /></el-icon>
      </div>
    </V2Card>
  </V2Page>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { ArrowRight } from '@element-plus/icons-vue'
import V2Page from '@/mobile-v2/components/V2Page.vue'
import V2Card from '@/mobile-v2/components/V2Card.vue'
import { functionItems, type FunctionItem } from '@/data/function-items'

const router = useRouter()
const { t } = useI18n()

function openFunction(item: FunctionItem) {
  if (item.route) {
    router.push(item.route)
    return
  }
  ElMessage.info(t('functions.openSoon', { name: t(`functions.items.${item.key}.name`) }))
}
</script>

<style scoped lang="scss">
.v2-functions-item {
  cursor: pointer;
  padding: 4px 16px;
}

.v2-functions-item .mobile-v2-list-item {
  padding: 10px 0;
}
</style>
