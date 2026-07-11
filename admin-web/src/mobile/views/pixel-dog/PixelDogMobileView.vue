<template>
  <div class="mobile-pixel-dog">
    <div class="mobile-pixel-dog__header">
      <button type="button" class="mobile-pixel-dog__back" @click="goBack">
        <span>←</span>
      </button>
      <span class="mobile-pixel-dog__header-title">像素伙伴</span>
    </div>

    <div class="mobile-pixel-dog__content">
      <div v-show="activeTab === 'home'" class="mobile-pixel-dog__tab-panel">
        <div class="mobile-pixel-dog__xp-bar">
          <span class="mobile-pixel-dog__xp-label">等级</span>
          <div class="mobile-pixel-dog__xp-track">
            <div class="mobile-pixel-dog__xp-fill" :style="{ width: xpPercent + '%' }" />
          </div>
          <span class="mobile-pixel-dog__xp-text">Lv.{{ dogState.level }} {{ dogState.xp }}/{{ dogState.xpNext }}</span>
        </div>

        <div class="mobile-pixel-dog__sprite-area">
          <div
            class="mobile-pixel-dog__speech"
            :class="`mobile-pixel-dog__speech--${currentSpeech.emotion}`"
          >
            <span class="mobile-pixel-dog__speech-text">{{ currentSpeech.text }}</span>
          </div>

          <PixelDogMobileSprite
            :status="dogState.status"
            :emotion="dogState.emotion"
            :bond="dogState.bond"
            :level="dogState.level"
            :equipped-items="dogState.equippedItems"
            :items="dogItems"
            @pet="onPet"
          />
        </div>
      </div>

      <div v-show="activeTab === 'items'" class="mobile-pixel-dog__subpanel">
        <DogMobileItemsPanel
          :items="dogItems"
          :level="dogState.level"
          :equipped-items="dogState.equippedItems"
          @toggle-equip="onToggleEquip"
        />
      </div>

      <div v-show="activeTab === 'history'" class="mobile-pixel-dog__subpanel">
        <div class="mobile-dog-history">
          <div class="mobile-dog-history__title">📜 成长记录</div>
          <div class="mobile-dog-history__list">
            <div class="mobile-dog-history__item">
              <span class="mobile-dog-history__icon">🎂</span>
              <span>等级 {{ dogState.level }} 达成！</span>
            </div>
            <div class="mobile-dog-history__item">
              <span class="mobile-dog-history__icon">❤️</span>
              <span>陪伴值达到 {{ dogState.bond }}</span>
            </div>
            <div class="mobile-dog-history__item">
              <span class="mobile-dog-history__icon">⭐</span>
              <span>累计获得 {{ totalXp }} 经验值</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="mobile-pixel-dog__bottom-bar">
      <div v-show="activeTab === 'home'" class="mobile-pixel-dog__interact-bar">
        <button
          type="button"
          class="mobile-pixel-dog__interact-btn mobile-pixel-dog__interact-btn--pet"
          @click="onPet"
        >
          <span class="mobile-pixel-dog__interact-icon">✋</span>
          <span class="mobile-pixel-dog__interact-label">抚摸</span>
        </button>
        <button
          type="button"
          class="mobile-pixel-dog__interact-btn mobile-pixel-dog__interact-btn--greet"
          @click="onGreet"
        >
          <span class="mobile-pixel-dog__interact-icon">👋</span>
          <span class="mobile-pixel-dog__interact-label">打招呼</span>
        </button>
        <button
          v-if="intimacy >= 60"
          type="button"
          class="mobile-pixel-dog__interact-btn mobile-pixel-dog__interact-btn--nuzzle"
          @click="onNuzzle"
        >
          <span class="mobile-pixel-dog__interact-icon">🐶</span>
          <span class="mobile-pixel-dog__interact-label">蹭蹭</span>
        </button>
        <button
          v-if="intimacy >= 85"
          type="button"
          class="mobile-pixel-dog__interact-btn mobile-pixel-dog__interact-btn--hug"
          @click="onHug"
        >
          <span class="mobile-pixel-dog__interact-icon">🤗</span>
          <span class="mobile-pixel-dog__interact-label">抱抱</span>
        </button>
      </div>

      <div v-show="activeTab === 'home'" class="mobile-pixel-dog__status-bar">
        <div class="mobile-pixel-dog__status-item">
          <span class="mobile-pixel-dog__status-icon">❤️</span>
          <span class="mobile-pixel-dog__status-value">{{ dogState.bond }}</span>
        </div>
        <div class="mobile-pixel-dog__status-item">
          <span class="mobile-pixel-dog__status-icon">{{ emotionIcon }}</span>
          <span class="mobile-pixel-dog__status-value" :class="emotionClass">{{ emotionText }}</span>
        </div>
        <div class="mobile-pixel-dog__status-item">
          <span class="mobile-pixel-dog__status-icon">{{ statusIcon }}</span>
          <span class="mobile-pixel-dog__status-value">{{ statusText }}</span>
        </div>
      </div>

      <div class="mobile-pixel-dog__tab-bar">
        <button
          type="button"
          class="mobile-pixel-dog__tab"
          :class="{ 'is-active': activeTab === 'home' }"
          @click="activeTab = 'home'"
        >
          <span class="mobile-pixel-dog__tab-icon">🐕</span>
          <span class="mobile-pixel-dog__tab-label">主页</span>
        </button>
        <button
          type="button"
          class="mobile-pixel-dog__tab"
          :class="{ 'is-active': activeTab === 'items' }"
          @click="activeTab = 'items'"
        >
          <span class="mobile-pixel-dog__tab-icon">🎁</span>
          <span class="mobile-pixel-dog__tab-label">物品</span>
        </button>
        <button
          type="button"
          class="mobile-pixel-dog__tab"
          :class="{ 'is-active': activeTab === 'history' }"
          @click="activeTab = 'history'"
        >
          <span class="mobile-pixel-dog__tab-icon">📜</span>
          <span class="mobile-pixel-dog__tab-label">记录</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PixelDogMobileSprite from './PixelDogMobileSprite.vue'
import DogMobileItemsPanel from './DogMobileItemsPanel.vue'
import { fetchDogState, updateDogState, fetchDogItems, type PixelDogStateVO, type PixelDogItemVO } from '@/api/pixelDog'
import { fetchActiveSession } from '@/api/pomodoro'

type DogStatus = 'IDLE' | 'HAPPY' | 'PETTING' | 'GREETING' | 'SLEEPING' | 'WALKING' | 'FOCUS' | 'NUZZLE'

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

const router = useRouter()

function goBack() {
  router.back()
}

const activeTab = ref<'home' | 'items' | 'history'>('home')

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

const xpPercent = computed(() => {
  if (!dogState.value.xpNext) return 0
  return Math.round((dogState.value.xp / dogState.value.xpNext) * 100)
})

const emotionIcon = computed(() => {
  const e = dogState.value.emotion
  if (e >= 50) return '😄'
  if (e >= 20) return '😊'
  if (e >= 0) return '😐'
  if (e >= -30) return '😕'
  return '😢'
})

const emotionText = computed(() => {
  const e = dogState.value.emotion
  if (e >= 50) return '开心'
  if (e >= 20) return '愉快'
  if (e >= 0) return '平静'
  if (e >= -30) return '无聊'
  return '低落'
})

const emotionClass = computed(() => {
  const e = dogState.value.emotion
  if (e >= 50) return 'mobile-pixel-dog__status-value--happy'
  if (e >= 20) return 'mobile-pixel-dog__status-value--good'
  if (e >= 0) return 'mobile-pixel-dog__status-value--neutral'
  if (e >= -30) return 'mobile-pixel-dog__status-value--sad'
  return 'mobile-pixel-dog__status-value--depressed'
})

const statusIcon = computed(() => {
  switch (dogState.value.status) {
    case 'HAPPY': return '✨'
    case 'PETTING': return '💆'
    case 'GREETING': return '👋'
    case 'FOCUS': return '🎯'
    case 'SLEEPING': return '💤'
    case 'WALKING': return '🚶'
    default: return '🐕'
  }
})

const statusText = computed(() => {
  switch (dogState.value.status) {
    case 'HAPPY': return '开心'
    case 'PETTING': return '被抚摸'
    case 'GREETING': return '打招呼'
    case 'FOCUS': return '专注中'
    case 'SLEEPING': return '睡觉中'
    case 'WALKING': return '散步'
    default: return '空闲'
  }
})

const intimacy = computed(() => {
  return (dogState.value.emotion + 100) / 2 * 0.4 + dogState.value.bond * 0.6
})

interface SpeechInfo {
  text: string
  emotion: 'happy' | 'normal' | 'sad' | 'angry'
}

const speechTexts: Record<string, string[]> = {
  cold_sad: ['一个人好寂寞...', '你好像不太喜欢我...', '我是不是做错什么了?', '好孤单，没人理我...', '也许我不该在这里...'],
  cold_normal: ['...', '你好', '嗯', '随便吧...', '你在忙吗?'],
  cold_happy: ['今天天气不错', '阳光真好', '发呆中...', '看着你工作', '安静地待着'],
  cold_angry: ['你根本不在乎我!', '既然这样，算了...', '讨厌你!', '再也不理你了!', '哼! 走了!'],
  normal_sad: ['有点无聊呢...', '你多久没理我了...', '希望你能陪陪我', '今天好安静', '想出去玩...'],
  normal_happy: ['今天过得不错!', '一起加油吧!', '你在忙什么呢?', '记得休息哦~', '继续努力!'],
  close_sad: ['不要不理我嘛...', '你去哪里了?', '我好想你...', '没有你我好难过', '回来好不好?'],
  close_happy: ['蹭蹭你的手~', '撒娇卖萌中~', '最喜欢你了!', '想一直陪着你', '黏人模式开启~'],
  intimate_sad: ['你不要离开我...', '紧紧抓住你的衣角', '眼眶红红的看着你', '在你怀里委屈地哭', '不要不理我，我会害怕...'],
  intimate_happy: ['紧紧抱住你!', '在你怀里蹭来蹭去~', '幸福地闭上眼睛', '你是全世界最好的!', '永远爱你!'],
}

function getRandomSpeech(): SpeechInfo {
  const emotion = dogState.value.emotion
  const bond = dogState.value.bond

  const intimacyVal = (emotion + 100) / 2 * 0.4 + bond * 0.6

  let category: string
  let emotionType: 'happy' | 'normal' | 'sad' | 'angry'

  if (bond < 20) {
    if (emotion >= 20) { category = 'cold_happy'; emotionType = 'normal' }
    else if (emotion >= 0) { category = 'cold_normal'; emotionType = 'normal' }
    else if (emotion >= -30) { category = 'cold_sad'; emotionType = 'sad' }
    else { category = 'cold_angry'; emotionType = 'angry' }
  } else if (intimacyVal < 60) {
    if (emotion >= 20) { category = 'normal_happy'; emotionType = 'happy' }
    else { category = 'normal_sad'; emotionType = 'sad' }
  } else if (intimacyVal < 85) {
    if (emotion >= 10) { category = 'close_happy'; emotionType = 'happy' }
    else { category = 'close_sad'; emotionType = 'sad' }
  } else {
    if (emotion >= 0) { category = 'intimate_happy'; emotionType = 'happy' }
    else { category = 'intimate_sad'; emotionType = 'sad' }
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
  if (speechInterval) clearInterval(speechInterval)
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
  dogState.value.status = 'NUZZLE'
  await saveState()
}

async function onHug() {
  dogState.value.emotion = Math.min(100, dogState.value.emotion + 30)
  dogState.value.bond = Math.min(100, dogState.value.bond + 5)
  dogState.value.status = 'HAPPY'
  await saveState()
}

let pomodoroSyncTimer: ReturnType<typeof setInterval> | null = null

async function checkPomodoroState() {
  try {
    const session = await fetchActiveSession()
    if (session) {
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
  if (pomodoroSyncTimer) clearInterval(pomodoroSyncTimer)
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
@use './pixel-dog-mobile.scss';
</style>
