<template>
  <WarRoomPage fill>
    <div class="functions-page">
      <header class="functions-page__header">
        <h1 class="functions-page__title">{{ t('functions.title') }}</h1>
        <el-input
          v-model="keyword"
          class="functions-page__search"
          clearable
          :prefix-icon="Search"
          :placeholder="t('functions.searchPlaceholder')"
        />
      </header>

      <section v-if="filteredFeatured.length" class="functions-section">
        <h2 class="functions-section__title">{{ t('functions.featuredTitle') }}</h2>
        <div class="functions-featured">
          <article
            v-for="entry in filteredFeatured"
            :key="entry.item.key"
            class="functions-featured-card"
            :class="`is-${entry.item.key}`"
          >
            <img
              class="functions-featured-card__icon"
              :src="entry.iconUrl"
              :alt="t(`functions.items.${entry.item.key}.name`)"
            />
            <div class="functions-featured-card__body">
              <h3 class="functions-featured-card__name">
                {{ t(`functions.items.${entry.item.key}.name`) }}
              </h3>
              <p class="functions-featured-card__desc">
                {{ t(`functions.featured.${entry.item.key}`) }}
              </p>
              <button
                type="button"
                class="functions-featured-card__btn"
                @click="openFunction(entry)"
              >
                {{ t('functions.useNow') }}
              </button>
            </div>
          </article>
        </div>
      </section>

      <section class="functions-section">
        <div class="functions-section__head">
          <h2 class="functions-section__title">{{ t('functions.allTitle') }}</h2>
          <span class="functions-section__count">
            {{ t('functions.allCount', { count: filteredAll.length }) }}
          </span>
        </div>

        <div v-if="filteredAll.length" class="functions-all">
          <button
            v-for="entry in filteredAll"
            :key="entry.item.key"
            type="button"
            class="functions-all-card"
            @click="openFunction(entry)"
          >
            <span
              class="functions-all-card__icon"
              :style="{ background: `${entry.barColor}18`, color: entry.barColor }"
            >
              <img :src="entry.iconUrl" :alt="t(`functions.items.${entry.item.key}.name`)" />
            </span>
            <span class="functions-all-card__name">{{ t(`functions.items.${entry.item.key}.name`) }}</span>
            <span class="functions-all-card__desc">{{ t(`functions.items.${entry.item.key}.desc`) }}</span>
            <span v-if="!entry.item.route" class="functions-all-card__badge">{{ t('functions.soon') }}</span>
          </button>
        </div>

        <el-empty
          v-else
          class="functions-page__empty"
          :description="t('functions.searchEmpty')"
          :image-size="72"
        />
      </section>
    </div>
  </WarRoomPage>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import WarRoomPage from '@/components/war-room/WarRoomPage.vue'
import { functionItems, type FunctionItemKey } from '@/data/function-items'
import { moduleVisuals } from '@/data/module-visuals'
import { warRoomIconUrl } from '@/data/war-room-icons'

const FEATURED_KEYS: FunctionItemKey[] = ['pomodoro', 'notebook', 'todos']

const router = useRouter()
const { t } = useI18n()
const keyword = ref('')

const modules = computed(() =>
  functionItems.map((item) => ({
    item,
    moduleIcon: moduleVisuals[item.key].icon,
    iconUrl: warRoomIconUrl('modules', moduleVisuals[item.key].icon),
    barColor: moduleVisuals[item.key].barColor,
  })),
)

function matchesKeyword(entry: (typeof modules.value)[number], query: string) {
  if (!query) return true
  const name = t(`functions.items.${entry.item.key}.name`).toLowerCase()
  const desc = t(`functions.items.${entry.item.key}.desc`).toLowerCase()
  const featured = t(`functions.featured.${entry.item.key}`).toLowerCase()
  return name.includes(query) || desc.includes(query) || featured.includes(query)
}

const filteredFeatured = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  return modules.value.filter(
    (entry) => FEATURED_KEYS.includes(entry.item.key) && matchesKeyword(entry, query),
  )
})

const filteredAll = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  return modules.value.filter((entry) => matchesKeyword(entry, query))
})

function openFunction(entry: (typeof modules.value)[number]) {
  const { item } = entry
  if (item.route) {
    router.push(item.route)
    return
  }
  ElMessage.info(t('functions.openSoon', { name: t(`functions.items.${item.key}.name`) }))
}
</script>

<style scoped lang="scss">
.functions-page {
  display: flex;
  flex-direction: column;
  gap: 28px;
  min-height: 0;
}

.functions-page__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
}

.functions-page__title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
  color: var(--wr-text, #333);
  letter-spacing: -0.02em;
}

.functions-page__search {
  width: min(360px, 100%);
  margin-left: auto;

  :deep(.el-input__wrapper) {
    min-height: 42px;
    border-radius: 999px;
    box-shadow: 0 0 0 1px var(--wr-border, #e8ecef) inset;
  }
}

.functions-section__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.functions-section__title {
  margin: 0 0 16px;
  font-size: 18px;
  font-weight: 700;
  color: var(--wr-text, #333);
}

.functions-section__head .functions-section__title {
  margin-bottom: 0;
}

.functions-section__count {
  font-size: 13px;
  color: var(--wr-muted, #999);
}

.functions-featured {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.functions-featured-card {
  display: flex;
  align-items: center;
  gap: 16px;
  min-height: 148px;
  padding: 20px 22px;
  border: 1px solid transparent;
  border-radius: 16px;
  overflow: hidden;
}

.functions-featured-card.is-pomodoro {
  background: linear-gradient(135deg, #fff7ed 0%, #ffedd5 100%);
  border-color: #fed7aa;
}

.functions-featured-card.is-notebook {
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border-color: #bfdbfe;
}

.functions-featured-card.is-todos {
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
  border-color: #a7f3d0;
}

.functions-featured-card__icon {
  width: 72px;
  height: 72px;
  object-fit: contain;
  flex-shrink: 0;
}

.functions-featured-card__body {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  min-width: 0;
}

.functions-featured-card__name {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--wr-text, #333);
}

.functions-featured-card__desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.55;
  color: var(--wr-text-secondary, #666);
}

.functions-featured-card__btn {
  margin-top: 4px;
  min-height: 34px;
  padding: 0 18px;
  border: none;
  border-radius: 999px;
  background: #fff;
  color: var(--wr-text, #333);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 2px 8px rgb(15 23 42 / 8%);
  transition: transform 0.15s ease, box-shadow 0.15s ease;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgb(15 23 42 / 12%);
  }
}

.functions-all {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.functions-all-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
  min-height: 148px;
  padding: 18px;
  border: 1px solid var(--wr-border, #e8ecef);
  border-radius: 14px;
  background: var(--wr-card, #fff);
  box-shadow: var(--wr-shadow, 0 4px 12px rgb(0 0 0 / 5%));
  cursor: pointer;
  text-align: left;
  transition: border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;

  &:hover {
    border-color: #bfdbfe;
    box-shadow: 0 8px 20px rgb(37 99 235 / 10%);
    transform: translateY(-2px);
  }
}

.functions-all-card__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 12px;

  img {
    width: 28px;
    height: 28px;
    object-fit: contain;
  }
}

.functions-all-card__name {
  font-size: 15px;
  font-weight: 700;
  line-height: 1.35;
  color: var(--wr-text, #333);
}

.functions-all-card__desc {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-size: 12px;
  line-height: 1.55;
  color: var(--wr-text-secondary, #666);
}

.functions-all-card__badge {
  position: absolute;
  top: 14px;
  right: 14px;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  color: var(--wr-muted, #999);
  background: var(--wr-stat-gray-bg, #f3f4f6);
}

.functions-page__empty {
  padding: 32px 0;
}

@media (max-width: 1100px) {
  .functions-featured {
    grid-template-columns: 1fr;
  }

  .functions-all {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .functions-page__header {
    flex-direction: column;
    align-items: stretch;
  }

  .functions-page__search {
    margin-left: 0;
    width: 100%;
  }

  .functions-all {
    grid-template-columns: 1fr;
  }
}
</style>
