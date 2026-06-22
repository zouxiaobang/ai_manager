<template>
  <div class="mobile-page">
    <p class="mobile-functions__subtitle">{{ t('functions.subtitle') }}</p>
    <div class="mobile-functions__grid">
      <button
        v-for="item in functionItems"
        :key="item.key"
        type="button"
        class="mobile-functions__card"
        @click="openFunction(item)"
      >
        <span
          class="mobile-functions__icon"
          :style="{ background: `${item.accent}22`, color: item.accent }"
        >
          <el-icon :size="24"><component :is="item.icon" /></el-icon>
        </span>
        <span class="mobile-functions__name">{{ t(`functions.items.${item.key}.name`) }}</span>
        <span class="mobile-functions__desc">{{ t(`functions.items.${item.key}.desc`) }}</span>
        <el-tag v-if="!item.route" size="small" type="info">{{ t('functions.soon') }}</el-tag>
        <el-tag v-else size="small" type="success">{{ t('functions.enter') }}</el-tag>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
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
.mobile-functions__subtitle {
  margin: 0;
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.mobile-functions__grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

.mobile-functions__card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  padding: 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  background: var(--el-bg-color);
  text-align: left;
  cursor: pointer;
}

.mobile-functions__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 10px;
}

.mobile-functions__name {
  font-size: 16px;
  font-weight: 600;
}

.mobile-functions__desc {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
}
</style>
