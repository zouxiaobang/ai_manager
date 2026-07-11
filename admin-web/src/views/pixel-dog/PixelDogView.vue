<template>
  <div class="pixel-dog-page war-room-page--fill">
    <div class="pixel-dog-page__stars" aria-hidden="true" />

    <div
      class="pixel-dog-page__content"
      :class="{ 'pixel-dog-page__content--fill': activeTab !== 'home' }"
    >
      <div v-show="activeTab === 'home'" class="pixel-dog-main">
        <aside class="pixel-dog-side pixel-dog-side--left">
          <DogStatusPanel :dog="dogState" />
        </aside>

        <section class="pixel-dog-center">
          <PixelDogSprite
            :status="dogState.status"
            :emotion="dogState.emotion"
            :bond="dogState.bond"
            :level="dogState.level"
            :equipped-items="dogState.equippedItems"
            :items="dogItems"
            @pet="onPet"
            @greet="onGreet"
          />
          <div class="pixel-dog-speech" :class="`pixel-dog-speech--${currentSpeech.emotion}`">
            <span class="pixel-dog-speech__text">{{ currentSpeech.text }}</span>
          </div>
        </section>

        <aside class="pixel-dog-side pixel-dog-side--right">
          <DogInteractionPanel
            :dog="dogState"
            @pet="onPet"
            @greet="onGreet"
            @nuzzle="onNuzzle"
            @hug="onHug"
          />
        </aside>
      </div>

      <div v-show="activeTab === 'items'" class="pixel-dog-subpanel">
        <DogItemsPanel
          :items="dogItems"
          :level="dogState.level"
          :equipped-items="dogState.equippedItems"
          @toggle-equip="onToggleEquip"
        />
      </div>

      <div v-show="activeTab === 'history'" class="pixel-dog-subpanel">
        <div class="pixel-dog-history">
          <h3 style="color: white">📜 成长记录</h3>
          <div class="pixel-dog-history__list">
            <div class="pixel-dog-history__item">
              <span class="pixel-dog-history__icon">🎂</span>
              <span>等级 {{ dogState.level }} 达成！</span>
            </div>
            <div class="pixel-dog-history__item">
              <span class="pixel-dog-history__icon">❤️</span>
              <span>陪伴值达到 {{ dogState.bond }}</span>
            </div>
            <div class="pixel-dog-history__item">
              <span class="pixel-dog-history__icon">⭐</span>
              <span>累计获得 {{ totalXp }} 经验值</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <PomoPixelNav v-model="activeTab" :items="navItems" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import PomoPixelNav from '../pomodoro/PomoPixelNav.vue'
import DogStatusPanel from './DogStatusPanel.vue'
import DogInteractionPanel from './DogInteractionPanel.vue'
import PixelDogSprite from './PixelDogSprite.vue'
import DogItemsPanel from './DogItemsPanel.vue'
import { fetchDogState, updateDogState, fetchDogItems, type PixelDogStateVO, type PixelDogItemVO } from '@/api/pixelDog'
import { fetchActiveSession } from '@/api/pomodoro'

type DogStatus = 'IDLE' | 'HAPPY' | 'PETTING' | 'GREETING' | 'SLEEPING' | 'WALKING' | 'FOCUS'

interface DogState {
  level: number
  xp: number
  xpNext: number
  bond: number
  emotion: number
  status: DogStatus
  unlockedItems: number
  equippedItems: number
}

const { t } = useI18n()

const activeTab = ref<'home' | 'items' | 'history'>('home')
const pomodoroPhase = ref<'IDLE' | 'WORK' | 'SHORT_BREAK' | 'LONG_BREAK'>('IDLE')

const dogState = ref<DogState>({
  level: 1,
  xp: 0,
  xpNext: 100,
  bond: 0,
  emotion: 0,
  status: 'IDLE',
  unlockedItems: 1,
  equippedItems: 0,
})

const dogItems = ref<PixelDogItemVO[]>([])

function statusNumToStr(num: number): DogStatus {
  switch (num) {
    case 7: return 'FOCUS'
    default: return 'IDLE'
  }
}

function statusStrToNum(str: DogStatus): number {
  switch (str) {
    case 'FOCUS': return 7
    default: return 0
  }
}

async function loadState() {
  try {
    const result = await fetchDogState()
    if (result) {
      const vo = result as PixelDogStateVO
      dogState.value = {
        level: vo.level,
        xp: vo.xp,
        xpNext: vo.xpNext,
        bond: vo.bond,
        emotion: vo.emotion,
        status: statusNumToStr(vo.status),
        unlockedItems: vo.unlockedItems,
        equippedItems: vo.equippedItems || 0,
      }
    }
  } catch (e) {
    console.error('Failed to load dog state:', e)
  }
}

async function saveState() {
  try {
    await updateDogState({
      level: dogState.value.level,
      xp: dogState.value.xp,
      xpNext: dogState.value.xpNext,
      bond: dogState.value.bond,
      emotion: dogState.value.emotion,
      status: statusStrToNum(dogState.value.status),
      unlockedItems: dogState.value.unlockedItems,
      equippedItems: dogState.value.equippedItems,
    })
  } catch (e) {
    console.error('Failed to save dog state:', e)
  }
}

async function loadItems() {
  try {
    const data = await fetchDogItems()
    dogItems.value = data || []
  } catch (e) {
    console.error('Failed to load dog items:', e)
    dogItems.value = []
  }
}

async function onToggleEquip(itemId: number) {
  // 使用 BigInt 避免 32 位溢出（JS 位运算 << ^ & 均截断为 32 位）
  const currentBits = BigInt(dogState.value.equippedItems)
  const mask = 1n << BigInt(itemId - 1)
  dogState.value.equippedItems = Number(currentBits ^ mask)
  await saveState()
}

const totalXp = computed(() => {
  let total = dogState.value.xp
  for (let i = 1; i < dogState.value.level; i++) {
    total += i * 100 + (i - 1) * 50
  }
  return total
})

interface SpeechInfo {
  text: string
  emotion: 'happy' | 'normal' | 'sad' | 'angry'
}

const speechTexts: Record<string, string[]> = {
  cold_sad: [
    '一个人好寂寞...',
    '你好像不太喜欢我...',
    '我是不是做错什么了?',
    '好孤单，没人理我...',
    '也许我不该在这里...',
  ],
  cold_normal: [
    '...',
    '你好',
    '嗯',
    '随便吧...',
    '你在忙吗?',
  ],
  cold_happy: [
    '今天天气不错',
    '阳光真好',
    '发呆中...',
    '看着你工作',
    '安静地待着',
  ],
  cold_angry: [
    '你根本不在乎我!',
    '既然这样，算了...',
    '讨厌你!',
    '再也不理你了!',
    '哼! 走了!',
  ],
  normal_sad: [
    '有点无聊呢...',
    '你多久没理我了...',
    '希望你能陪陪我',
    '今天好安静',
    '想出去玩...',
  ],
  normal_happy: [
    '今天过得不错!',
    '一起加油吧!',
    '你在忙什么呢?',
    '记得休息哦~',
    '继续努力!',
  ],
  close_sad: [
    '不要不理我嘛...',
    '你去哪里了?',
    '我好想你...',
    '没有你我好难过',
    '回来好不好?',
  ],
  close_happy: [
    '蹭蹭你的手~',
    '撒娇卖萌中~',
    '最喜欢你了!',
    '想一直陪着你',
    '黏人模式开启~',
  ],
  intimate_sad: [
    '你不要离开我...',
    '紧紧抓住你的衣角',
    '眼眶红红的看着你',
    '在你怀里委屈地哭',
    '不要不理我，我会害怕...',
  ],
  intimate_happy: [
    '紧紧抱住你!',
    '在你怀里蹭来蹭去~',
    '幸福地闭上眼睛',
    '你是全世界最好的!',
    '永远爱你!',
  ],
}

function getRandomSpeech(): SpeechInfo {
  const emotion = dogState.value.emotion
  const bond = dogState.value.bond

  const intimacy = (emotion + 100) / 2 * 0.4 + bond * 0.6

  let category: string
  let emotionType: 'happy' | 'normal' | 'sad' | 'angry'

  if (bond < 20) {
    if (emotion >= 20) {
      category = 'cold_happy'
      emotionType = 'normal'
    } else if (emotion >= 0) {
      category = 'cold_normal'
      emotionType = 'normal'
    } else if (emotion >= -30) {
      category = 'cold_sad'
      emotionType = 'sad'
    } else {
      category = 'cold_angry'
      emotionType = 'angry'
    }
  } else if (intimacy < 60) {
    if (emotion >= 20) {
      category = 'normal_happy'
      emotionType = 'happy'
    } else {
      category = 'normal_sad'
      emotionType = 'sad'
    }
  } else if (intimacy < 85) {
    if (emotion >= 10) {
      category = 'close_happy'
      emotionType = 'happy'
    } else {
      category = 'close_sad'
      emotionType = 'sad'
    }
  } else {
    if (emotion >= 0) {
      category = 'intimate_happy'
      emotionType = 'happy'
    } else {
      category = 'intimate_sad'
      emotionType = 'sad'
    }
  }

  const list = speechTexts[category]
  return {
    text: list[Math.floor(Math.random() * list.length)],
    emotion: emotionType,
  }
}

const currentSpeech = ref<SpeechInfo>(getRandomSpeech())

let speechInterval: ReturnType<typeof setInterval> | null = null

function startSpeechTimer() {
  if (speechInterval) {
    clearInterval(speechInterval)
  }
  speechInterval = setInterval(() => {
    currentSpeech.value = getRandomSpeech()
  }, 8000)
}

function stopSpeechTimer() {
  if (speechInterval) {
    clearInterval(speechInterval)
    speechInterval = null
  }
}

async function onPet() {
  dogState.value.emotion = Math.min(100, dogState.value.emotion + 10)
  dogState.value.bond = Math.min(100, dogState.value.bond + 1)
  dogState.value.status = 'PETTING'
  await saveState()
}

async function onGreet() {
  dogState.value.bond = Math.min(100, dogState.value.bond + 5)
  dogState.value.emotion = Math.min(100, dogState.value.emotion + 5)
  dogState.value.status = 'GREETING'
  await saveState()
}

async function onNuzzle() {
  dogState.value.emotion = Math.min(100, dogState.value.emotion + 15)
  dogState.value.bond = Math.min(100, dogState.value.bond + 3)
  dogState.value.status = 'PETTING'
  await saveState()
}

async function onHug() {
  dogState.value.emotion = Math.min(100, dogState.value.emotion + 30)
  dogState.value.bond = Math.min(100, dogState.value.bond + 5)
  dogState.value.status = 'HAPPY'
  await saveState()
}

const navItems = computed(() => [
  { id: 'home' as const, icon: '🐕', label: t('functions.items.pixelDog.navHome') },
  { id: 'items' as const, icon: '🎁', label: t('functions.items.pixelDog.navItems') },
  { id: 'history' as const, icon: '📜', label: t('functions.items.pixelDog.navHistory') },
])

let pomodoroSyncTimer: ReturnType<typeof setInterval> | null = null

async function checkPomodoroState() {
  try {
    const session = await fetchActiveSession()
    if (session) {
      pomodoroPhase.value = session.phase
      if (session.phase === 'WORK' && session.runState === 'RUNNING') {
        dogState.value.status = 'FOCUS'
      } else {
        dogState.value.status = 'IDLE'
      }
    }
  } catch (e) {
    console.error('Failed to check pomodoro state:', e)
  }
}

function startPomodoroSync() {
  if (pomodoroSyncTimer) {
    clearInterval(pomodoroSyncTimer)
  }
  pomodoroSyncTimer = setInterval(checkPomodoroState, 2000)
}

function stopPomodoroSync() {
  if (pomodoroSyncTimer) {
    clearInterval(pomodoroSyncTimer)
    pomodoroSyncTimer = null
  }
}

onMounted(() => {
  loadState()
  loadItems()
  startSpeechTimer()
  checkPomodoroState()
  startPomodoroSync()
})

onUnmounted(() => {
  stopSpeechTimer()
  stopPomodoroSync()
})
</script>

<style scoped lang="scss">
@use './pixel-dog.scss';
</style>

<style lang="scss">
@use '../pomodoro/pomo-pixel-mixins.scss' as jag;

$pomo-nav-border: #2a2a50;
$pomo-green: #8bc34a;

.pomo-pixel-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px 12px;
  border-top: 2px solid $pomo-nav-border;
  background: rgb(8 8 26 / 72%);
}

.pomo-pixel-nav__items {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.pomo-pixel-nav__item {
  --s: 3px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  min-width: 72px;
  padding: 8px 12px;
  border: 2px solid transparent;
  border-radius: 0;
  @include jag.pixel-jag-clip(3);
  background: transparent;
  color: #8090a8;
  cursor: pointer;
  transition: color 0.15s, border-color 0.15s, background 0.15s;

  &.is-active {
    color: $pomo-green;
    border-color: rgb(139 195 74 / 72%);
    background: rgb(139 195 74 / 10%);
  }

  &:hover:not(.is-active) {
    color: #c0d0e8;
  }
}

.pomo-pixel-nav__icon {
  font-size: 20px;
  line-height: 1;
}

.pomo-pixel-nav__label {
  font-size: 14px;
  font-weight: 700;
}
</style>
