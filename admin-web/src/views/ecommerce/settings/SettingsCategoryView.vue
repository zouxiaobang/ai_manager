<template>
  <section class="ec-settings-category">
    <header class="ec-settings-category__header">
      <button type="button" class="ec-settings-category__back" @click="emit('back')">
        <el-icon><ArrowLeft /></el-icon>
        {{ t('ecommerce.settings.backToHub') }}
      </button>
      <div class="ec-settings-category__head-main">
        <div class="ec-settings-category__icon" :class="category ? `is-${category.tone}` : ''">
          <el-icon v-if="category"><component :is="category.icon" /></el-icon>
        </div>
        <div>
          <h2 class="ec-settings-category__title">{{ category ? t(category.labelKey) : '' }}</h2>
          <p class="ec-settings-category__desc">{{ category ? t(category.descKey) : '' }}</p>
        </div>
      </div>
    </header>

    <div class="ec-settings-category__list">
      <button
        v-for="item in items"
        :key="item.key"
        type="button"
        class="ec-settings-category__row"
        @click="emit('openItem', item.key)"
      >
        <div class="ec-settings-category__row-main">
          <span class="ec-settings-category__row-name">{{ t(item.labelKey) }}</span>
          <p class="ec-settings-category__row-desc">{{ t(item.descKey) }}</p>
        </div>
        <div class="ec-settings-category__row-meta">
          <el-tag size="small" :type="item.implemented ? 'success' : 'info'" effect="plain" round>
            {{ item.implemented ? t('ecommerce.settings.statusAvailable') : t('ecommerce.settings.statusComingSoon') }}
          </el-tag>
          <el-icon><ArrowRight /></el-icon>
        </div>
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import {
  getItemsForCategory,
  getSettingsCategory,
  type EcSettingsCategoryKey,
  type EcSettingsItemKey,
} from '@/data/ec-settings-catalog'

const props = defineProps<{
  categoryKey: EcSettingsCategoryKey
}>()

const emit = defineEmits<{
  back: []
  openItem: [itemKey: EcSettingsItemKey]
}>()

const { t } = useI18n()

const category = computed(() => getSettingsCategory(props.categoryKey))
const items = computed(() => getItemsForCategory(props.categoryKey))
</script>

<style scoped lang="scss">
.ec-settings-category__header {
  margin-bottom: 20px;
}

.ec-settings-category__back {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 14px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--el-color-primary);
  font-size: 13px;
  cursor: pointer;
}

.ec-settings-category__head-main {
  display: flex;
  align-items: center;
  gap: 14px;
}

.ec-settings-category__icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;

  &.is-blue {
    color: #2563eb;
    background: #eff6ff;
  }

  &.is-green {
    color: #16a34a;
    background: #f0fdf4;
  }

  &.is-purple {
    color: #7c3aed;
    background: #f5f3ff;
  }

  &.is-orange {
    color: #ea580c;
    background: #fff7ed;
  }

  &.is-cyan {
    color: #0891b2;
    background: #ecfeff;
  }

  &.is-gray {
    color: #4b5563;
    background: #f3f4f6;
  }
}

.ec-settings-category__title {
  margin: 0 0 6px;
  font-size: 20px;
  font-weight: 600;
}

.ec-settings-category__desc {
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.ec-settings-category__list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ec-settings-category__row {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  padding: 16px 18px;
  border: 1px solid #e8ecf2;
  border-radius: 12px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease;

  &:hover {
    border-color: #c7d2fe;
    background: #fafbff;
  }
}

.ec-settings-category__row-main {
  flex: 1;
  min-width: 0;
}

.ec-settings-category__row-name {
  display: block;
  font-size: 14px;
  font-weight: 600;
}

.ec-settings-category__row-desc {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.ec-settings-category__row-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  color: #9ca3af;
}
</style>
