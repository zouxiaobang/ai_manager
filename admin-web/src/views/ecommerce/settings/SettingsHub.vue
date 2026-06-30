<template>
  <div class="ec-settings-hub">
    <header class="ec-settings-hub__header">
      <div>
        <h2 class="ec-settings-hub__title">{{ t('ecommerce.settings.pageTitle') }}</h2>
        <p class="ec-settings-hub__desc">{{ t('ecommerce.settings.hubDesc') }}</p>
      </div>
      <div class="ec-settings-hub__search">
        <el-input
          v-model="searchQuery"
          clearable
          :prefix-icon="Search"
          :placeholder="t('ecommerce.settings.searchPlaceholder')"
          @clear="searchQuery = ''"
        />
      </div>
    </header>

    <section v-if="searchQuery.trim()" class="ec-settings-hub__search-results">
      <div class="ec-settings-hub__section-head">
        <h3 class="ec-settings-hub__section-title">
          {{ t('ecommerce.settings.searchResults', { count: searchResults.length }) }}
        </h3>
      </div>

      <el-empty
        v-if="!searchResults.length"
        :description="t('ecommerce.settings.searchEmpty')"
        class="ec-settings-hub__search-empty"
      />

      <div v-else class="ec-settings-hub__result-list">
        <button
          v-for="item in searchResults"
          :key="item.key"
          type="button"
          class="ec-settings-hub__result-row"
          @click="openItem(item.key)"
        >
          <div class="ec-settings-hub__result-main">
            <span class="ec-settings-hub__result-name">{{ t(item.labelKey) }}</span>
            <span class="ec-settings-hub__result-category">{{ categoryLabel(item.categoryKey) }}</span>
          </div>
          <p class="ec-settings-hub__result-desc">{{ t(item.descKey) }}</p>
          <div class="ec-settings-hub__result-meta">
            <el-tag size="small" :type="statusTagType(item)" effect="plain" round>
              {{ statusLabel(item) }}
            </el-tag>
            <el-icon class="ec-settings-hub__result-arrow"><ArrowRight /></el-icon>
          </div>
        </button>
      </div>
    </section>

    <template v-else>
      <section class="ec-settings-hub__grid">
        <button
          v-for="category in EC_SETTINGS_CATEGORIES"
          :key="category.key"
          type="button"
          class="ec-settings-hub__card"
          @click="openCategory(category.key)"
        >
          <div class="ec-settings-hub__card-icon" :class="`is-${category.tone}`">
            <el-icon><component :is="category.icon" /></el-icon>
          </div>
          <div class="ec-settings-hub__card-body">
            <div class="ec-settings-hub__card-head">
              <h3 class="ec-settings-hub__card-title">{{ t(category.labelKey) }}</h3>
              <span class="ec-settings-hub__card-count">
                {{ t('ecommerce.settings.itemCount', { count: category.itemKeys.length }) }}
              </span>
            </div>
            <p class="ec-settings-hub__card-desc">{{ t(category.descKey) }}</p>
            <div class="ec-settings-hub__card-tags">
              <span
                v-for="item in previewItems(category.key)"
                :key="item.key"
                class="ec-settings-hub__card-tag"
              >
                {{ t(item.labelKey) }}
              </span>
            </div>
          </div>
          <el-icon class="ec-settings-hub__card-arrow"><ArrowRight /></el-icon>
        </button>
      </section>

      <section v-if="recentChanges.length" class="ec-settings-hub__recent">
        <div class="ec-settings-hub__section-head">
          <h3 class="ec-settings-hub__section-title">{{ t('ecommerce.settings.recentChanges') }}</h3>
        </div>
        <div class="ec-settings-hub__recent-list">
          <button
            v-for="entry in recentChanges"
            :key="`${entry.itemKey}-${entry.updateTime}`"
            type="button"
            class="ec-settings-hub__recent-row"
            @click="openItem(entry.itemKey)"
          >
            <div class="ec-settings-hub__recent-main">
              <span class="ec-settings-hub__recent-name">{{ t(entry.labelKey) }}</span>
              <span class="ec-settings-hub__recent-summary">{{ entry.summary }}</span>
            </div>
            <span class="ec-settings-hub__recent-time">{{ formatRecentTime(entry.updateTime) }}</span>
          </button>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowRight, Search } from '@element-plus/icons-vue'
import { fetchPurchaseOrderConfig } from '@/api/ecommerce/purchaseOrderConfig'
import { fetchSettingsSummary } from '@/api/ecommerce/ecSettings'
import {
  EC_SETTINGS_CATEGORIES,
  EC_SETTINGS_ITEMS,
  getItemsForCategory,
  getSettingsCategory,
  getSettingsItem,
  type EcSettingsCategoryKey,
  type EcSettingsItemDef,
  type EcSettingsItemKey,
} from '@/data/ec-settings-catalog'

const emit = defineEmits<{
  openItem: [itemKey: EcSettingsItemKey]
  openCategory: [categoryKey: EcSettingsCategoryKey]
}>()

const { t, locale } = useI18n()

const searchQuery = ref('')

interface RecentChangeEntry {
  itemKey: EcSettingsItemKey
  labelKey: string
  summary: string
  updateTime: string
}

const recentChanges = ref<RecentChangeEntry[]>([])
const configuredItems = ref<Set<EcSettingsItemKey>>(new Set())

const searchResults = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  if (!query) return []

  return EC_SETTINGS_ITEMS.filter((item) => matchesSearch(item, query))
})

function matchesSearch(item: EcSettingsItemDef, query: string) {
  const category = getSettingsCategory(item.categoryKey)
  const fields = [
    t(item.labelKey),
    t(item.descKey),
    category ? t(category.labelKey) : '',
    category ? t(category.descKey) : '',
    ...item.tagKeys.map((key) => t(key)),
    ...item.searchKeywords,
  ]

  return fields.some((field) => field.toLowerCase().includes(query))
}

function categoryLabel(categoryKey: EcSettingsCategoryKey) {
  const category = getSettingsCategory(categoryKey)
  return category ? t(category.labelKey) : ''
}

function previewItems(categoryKey: EcSettingsCategoryKey) {
  return getItemsForCategory(categoryKey).slice(0, 3)
}

function statusLabel(item: EcSettingsItemDef) {
  if (!item.implemented) return t('ecommerce.settings.statusComingSoon')
  if (configuredItems.value.has(item.key)) return t('ecommerce.settings.statusConfigured')
  return t('ecommerce.settings.statusNotConfigured')
}

function statusTagType(item: EcSettingsItemDef) {
  if (!item.implemented) return 'info'
  if (configuredItems.value.has(item.key)) return 'success'
  return 'warning'
}

function openItem(itemKey: EcSettingsItemKey) {
  emit('openItem', itemKey)
}

function openCategory(categoryKey: EcSettingsCategoryKey) {
  emit('openCategory', categoryKey)
}

function formatRecentTime(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString(locale.value, {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

async function loadRecentChanges() {
  const entries: RecentChangeEntry[] = []
  const configured = new Set<EcSettingsItemKey>()

  try {
    const summary = await fetchSettingsSummary()
    for (const row of summary) {
      const item = EC_SETTINGS_ITEMS.find((entry) => entry.panelKey === row.key || entry.key === row.key)
      if (!item) continue
      if (row.configured) configured.add(item.key)
      if (!row.updateTime) continue
      entries.push({
        itemKey: item.key,
        labelKey: item.labelKey,
        summary: t('ecommerce.settings.recentSummaryUpdated'),
        updateTime: row.updateTime,
      })
    }
  } catch {
    try {
      const purchaseOrder = await fetchPurchaseOrderConfig()
      const item = getSettingsItem('purchase-order')
      if (item && purchaseOrder.title?.trim()) {
        configured.add('purchase-order')
      }
      if (purchaseOrder.updateTime && item) {
        entries.push({
          itemKey: 'purchase-order',
          labelKey: item.labelKey,
          summary: purchaseOrder.title?.trim()
            ? t('ecommerce.settings.recentSummaryPo', { title: purchaseOrder.title.trim() })
            : t('ecommerce.settings.recentSummaryEmpty'),
          updateTime: purchaseOrder.updateTime,
        })
      }
    } catch {
      /* ignore when API unavailable */
    }
  }

  configuredItems.value = configured
  recentChanges.value = entries.sort(
    (a, b) => new Date(b.updateTime).getTime() - new Date(a.updateTime).getTime(),
  ).slice(0, 8)
}

function refreshRecentChanges() {
  void loadRecentChanges()
}

onMounted(() => {
  void loadRecentChanges()
})

defineExpose({ refreshRecentChanges })
</script>

<style scoped lang="scss">
.ec-settings-hub {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.ec-settings-hub__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
}

.ec-settings-hub__title {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 600;
}

.ec-settings-hub__desc {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  max-width: 560px;
}

.ec-settings-hub__search {
  width: min(360px, 100%);
}

.ec-settings-hub__section-head {
  margin-bottom: 12px;
}

.ec-settings-hub__section-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
}

.ec-settings-hub__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.ec-settings-hub__card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  width: 100%;
  padding: 18px;
  border: 1px solid #e8ecf2;
  border-radius: 14px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;

  &:hover {
    border-color: #c7d2fe;
    box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
    transform: translateY(-1px);
  }
}

.ec-settings-hub__card-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;

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

.ec-settings-hub__card-body {
  flex: 1;
  min-width: 0;
}

.ec-settings-hub__card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

.ec-settings-hub__card-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
}

.ec-settings-hub__card-count {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.ec-settings-hub__card-desc {
  margin: 0 0 10px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--el-text-color-secondary);
}

.ec-settings-hub__card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.ec-settings-hub__card-tag {
  padding: 2px 8px;
  border-radius: 999px;
  background: #f3f4f6;
  color: #4b5563;
  font-size: 11px;
}

.ec-settings-hub__card-arrow {
  flex-shrink: 0;
  margin-top: 4px;
  color: #9ca3af;
  font-size: 16px;
}

.ec-settings-hub__result-list,
.ec-settings-hub__recent-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ec-settings-hub__result-row,
.ec-settings-hub__recent-row {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  padding: 14px 16px;
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

.ec-settings-hub__result-main,
.ec-settings-hub__recent-main {
  flex: 1;
  min-width: 0;
}

.ec-settings-hub__result-name,
.ec-settings-hub__recent-name {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.ec-settings-hub__result-category {
  display: block;
  margin-top: 2px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.ec-settings-hub__result-desc {
  flex: 1.2;
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.ec-settings-hub__result-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.ec-settings-hub__result-arrow {
  color: #9ca3af;
}

.ec-settings-hub__recent-summary {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.ec-settings-hub__recent-time {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.ec-settings-hub__search-empty {
  padding: 24px 0;
}

@media (max-width: 1200px) {
  .ec-settings-hub__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .ec-settings-hub__grid {
    grid-template-columns: 1fr;
  }

  .ec-settings-hub__result-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .ec-settings-hub__recent-row {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
