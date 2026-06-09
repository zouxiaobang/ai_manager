<template>
  <div class="function-list">
    <div class="function-list__header">
      <h2 class="function-list__title">{{ t('functions.title') }}</h2>
      <p class="function-list__subtitle">{{ t('functions.subtitle') }}</p>
    </div>

    <el-row :gutter="16">
      <el-col
        v-for="item in functionItems"
        :key="item.key"
        :xs="24"
        :sm="12"
        :lg="8"
      >
        <el-card
          shadow="hover"
          class="function-card"
          @click="openFunction(item)"
        >
          <div
            class="function-card__icon"
            :style="{ background: `${item.accent}22`, color: item.accent }"
          >
            <el-icon :size="28">
              <component :is="item.icon" />
            </el-icon>
          </div>
          <h3 class="function-card__name">
            {{ t(`functions.items.${item.key}.name`) }}
          </h3>
          <p class="function-card__desc">
            {{ t(`functions.items.${item.key}.desc`) }}
          </p>
          <el-tag
            v-if="!item.route"
            size="small"
            type="info"
            class="function-card__tag"
          >
            {{ t('functions.soon') }}
          </el-tag>
          <el-tag v-else size="small" type="success" class="function-card__tag">
            {{ t('functions.enter') }}
          </el-tag>
        </el-card>
      </el-col>
    </el-row>
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
  ElMessage.info(
    t('functions.openSoon', { name: t(`functions.items.${item.key}.name`) }),
  )
}
</script>

<style scoped lang="scss">
.function-list__header {
  margin-bottom: 20px;
}

.function-list__title {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
}

.function-list__subtitle {
  margin: 0;
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.function-card {
  margin-bottom: 16px;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;

  &:hover {
    transform: translateY(-2px);
  }

  :deep(.el-card__body) {
    padding: 20px;
  }
}

.function-card__icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 14px;
}

.function-card__name {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
}

.function-card__desc {
  margin: 0 0 12px;
  font-size: 13px;
  line-height: 1.5;
  color: var(--el-text-color-secondary);
  min-height: 40px;
}

.function-card__tag {
  margin-top: 4px;
}
</style>
