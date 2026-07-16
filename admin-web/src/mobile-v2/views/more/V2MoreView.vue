<template>
  <V2Page>
    <V2Card class="v2-mine-profile" @click="goProfileEdit">
      <div class="v2-mine-profile__inner">
        <div class="v2-mine-profile__avatar">
          <el-icon :size="36"><UserFilled /></el-icon>
        </div>
        <div class="v2-mine-profile__info">
          <div class="v2-mine-profile__nickname">{{ nickname }}</div>
          <div class="v2-mine-profile__id">ID: {{ userId }}</div>
        </div>
        <el-icon class="v2-mine-profile__arrow"><ArrowRight /></el-icon>
      </div>
    </V2Card>

    <V2Card class="v2-mine-settings" @click="goSettings">
      <div class="mobile-v2-list-item">
        <el-icon :size="20"><Setting /></el-icon>
        <div class="mobile-v2-list-item__body">
          <div class="mobile-v2-list-item__title">{{ t('mobile.more.settings') }}</div>
        </div>
        <el-icon><ArrowRight /></el-icon>
      </div>
    </V2Card>

    <div class="v2-mine-functions-title">
      {{ t('functions.allTitle', { count: functionModules.length }) }}
    </div>

    <div class="v2-mine-functions-grid">
      <button
        v-for="entry in functionModules"
        :key="entry.key"
        type="button"
        class="v2-mine-fn-card"
        @click="openFunction(entry)"
      >
        <span
          class="v2-mine-fn-card__icon"
          :style="{ background: `${entry.barColor}18`, color: entry.barColor }"
        >
          <img :src="entry.iconUrl" :alt="entry.name" />
        </span>
        <span class="v2-mine-fn-card__name">{{ entry.name }}</span>
        <span class="v2-mine-fn-card__desc">{{ entry.desc }}</span>
        <span v-if="!entry.route" class="v2-mine-fn-card__badge">{{ t('functions.soon') }}</span>
      </button>
    </div>
  </V2Page>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { ArrowRight, Setting, UserFilled } from '@element-plus/icons-vue'
import V2Page from '@/mobile-v2/components/V2Page.vue'
import V2Card from '@/mobile-v2/components/V2Card.vue'
import { functionItems } from '@/data/function-items'
import { moduleVisuals, railModuleVisuals } from '@/data/module-visuals'
import { warRoomIconUrl } from '@/data/war-room-icons'

const router = useRouter()
const { t } = useI18n()

const nickname = ref('User')
const userId = ref('—')

interface FunctionModule {
  key: string
  name: string
  desc: string
  iconUrl: string
  barColor: string
  route?: string
}

const extraModules: { key: string; i18nKey: string; icon: string; barColor: string; route?: string }[] = [
  { key: 'pomodoro', i18nKey: 'pomodoro', icon: 'pomodoro', barColor: '#e85d4c', route: '/pomodoro' },
  { key: 'ecommerce', i18nKey: 'ecommerce', icon: 'ecommerce', barColor: '#f59e0b', route: '/ecommerce' },
  { key: 'pixel-dog', i18nKey: 'pixelDog', icon: 'pixel-dog', barColor: '#8b5cf6', route: '/pixel-dog' },
  { key: '24hour', i18nKey: 'twentyFourHour', icon: '24hour', barColor: '#0ea5e9', route: '/24hour' },
]

const functionModules = computed<FunctionModule[]>(() => {
  const items: FunctionModule[] = []

  for (const fi of functionItems) {
    const visual = moduleVisuals[fi.key]
    items.push({
      key: fi.key,
      name: t(`functions.items.${fi.key}.name`),
      desc: t(`functions.items.${fi.key}.desc`),
      iconUrl: warRoomIconUrl('modules', visual.icon),
      barColor: visual.barColor,
      route: fi.route,
    })
  }

  for (const em of extraModules) {
    const rail = railModuleVisuals[em.key as keyof typeof railModuleVisuals]
    items.push({
      key: em.key,
      name: t(`functions.items.${em.i18nKey}.name`),
      desc: t(`functions.items.${em.i18nKey}.desc`),
      iconUrl: warRoomIconUrl(rail ? 'modules' : 'nav', em.icon),
      barColor: em.barColor,
      route: em.route,
    })
  }

  return items
})

function openFunction(fn: FunctionModule) {
  if (fn.route) {
    router.push(fn.route)
  } else {
    ElMessage.info(t('functions.openSoon', { name: fn.name }))
  }
}

function goSettings() {
  router.push('/settings')
}

function goProfileEdit() {
  router.push('/profile')
}

onMounted(() => {
  const savedName = localStorage.getItem('user-nickname')
  const savedId = localStorage.getItem('user-id')
  if (savedName) nickname.value = savedName
  if (savedId) userId.value = savedId
})
</script>

<style scoped lang="scss">
.v2-mine-profile {
  cursor: pointer;
  padding: 0;
  transition: border-color 0.15s;

  &:hover {
    border-color: var(--wr-stat-blue, #2563eb);
  }

  &__inner {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 16px;
  }

  &__avatar {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 52px;
    height: 52px;
    border-radius: 50%;
    background: linear-gradient(135deg, var(--wr-stat-blue, #2563eb), color-mix(in srgb, var(--wr-stat-blue, #2563eb) 70%, white));
    color: #fff;
    flex-shrink: 0;
  }

  &__info {
    flex: 1;
    min-width: 0;
  }

  &__nickname {
    font-size: 17px;
    font-weight: 700;
    line-height: 1.3;
    color: var(--wr-text, #333333);
  }

  &__id {
    margin-top: 3px;
    font-size: 12px;
    color: var(--wr-muted, #999999);
    line-height: 1.3;
  }

  &__arrow {
    flex-shrink: 0;
    color: var(--wr-muted, #999999);
  }
}

.v2-mine-settings {
  cursor: pointer;
  padding: 4px 16px;
  transition: border-color 0.15s;

  &:hover {
    border-color: var(--wr-stat-blue, #2563eb);
  }

  .mobile-v2-list-item {
    padding: 10px 0;
  }
}

.v2-mine-functions-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--wr-text, #333333);
  padding: 0 4px;
}

.v2-mine-functions-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.v2-mine-fn-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  padding: 14px;
  border: 1px solid var(--wr-border, #e8ecef);
  border-radius: 14px;
  background: var(--wr-card, #ffffff);
  box-shadow: var(--wr-shadow, 0 4px 12px rgb(0 0 0 / 5%));
  cursor: pointer;
  text-align: left;
  transition: border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
  font-family: inherit;
  overflow: hidden;

  &:hover {
    border-color: #bfdbfe;
    box-shadow: 0 8px 20px rgb(37 99 235 / 10%);
    transform: translateY(-2px);
  }

  &__icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    border-radius: 12px;
    flex-shrink: 0;

    img {
      width: 24px;
      height: 24px;
      object-fit: contain;
    }
  }

  &__name {
    font-size: 13px;
    font-weight: 700;
    line-height: 1.35;
    color: var(--wr-text, #333333);
  }

  &__desc {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    font-size: 11px;
    line-height: 1.45;
    color: var(--wr-text-secondary, #666666);
  }

  &__badge {
    position: absolute;
    top: 8px;
    right: 8px;
    padding: 1px 6px;
    border-radius: 999px;
    font-size: 10px;
    color: var(--wr-muted, #999999);
    background: var(--wr-stat-gray-bg, #f3f4f6);
  }
}
</style>
