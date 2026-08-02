<template>
  <div class="v2-pixel-dog">
    <div class="v2-pixel-dog__stars" aria-hidden="true" />

    <div class="v2-pixel-dog__content">
      <!-- 主页 -->
      <div v-show="activeTab === 'home'" class="v2-pixel-dog__home">
        <!-- 状态面板 -->
        <div class="v2-pixel-dog__status-panel">
          <div class="pixel-panel-jagged">
            <div class="pixel-panel-jagged__inner">
              <div class="dog-status-row">
                <div class="dog-status-row__item">
                  <span class="dog-status-row__icon">❤️</span>
                  <div class="dog-status-row__info">
                    <span class="dog-status-row__label">陪伴</span>
                    <span class="dog-status-row__value">{{ dogState.bond }}</span>
                  </div>
                  <div class="dog-status-row__bar">
                    <div class="dog-status-row__fill dog-status-row__fill--bond" :style="{ width: dogState.bond + '%' }" />
                  </div>
                </div>
                <div class="dog-status-row__divider" />
                <div class="dog-status-row__item">
                  <span class="dog-status-row__icon">{{ emotionIcon }}</span>
                  <div class="dog-status-row__info">
                    <span class="dog-status-row__label">心情</span>
                    <span class="dog-status-row__value" :class="emotionClass">{{ emotionText }}</span>
                  </div>
                  <div class="dog-status-row__bar">
                    <div class="dog-status-row__fill" :class="emotionBarClass" :style="{ width: emotionPercent + '%' }" />
                  </div>
                </div>
                <div class="dog-status-row__divider" />
                <div class="dog-status-row__item">
                  <span class="dog-status-row__icon">{{ statusIcon }}</span>
                  <div class="dog-status-row__info">
                    <span class="dog-status-row__label">状态</span>
                    <span class="dog-status-row__value">{{ statusText }}</span>
                  </div>
                </div>
              </div>

              <div class="dog-xp-row">
                <div class="dog-xp-row__level">
                  <span class="dog-xp-row__level-icon">⭐</span>
                  <span class="dog-xp-row__level-text">Lv.{{ dogState.level }}</span>
                </div>
                <div class="dog-xp-row__track">
                  <div class="dog-xp-row__fill" :style="{ width: xpPercent + '%' }" />
                </div>
                <span class="dog-xp-row__text">{{ dogState.xp }}/{{ dogState.xpNext }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 精灵区域 -->
        <div class="v2-pixel-dog__sprite-wrap">
          <div
            class="v2-pixel-dog__speech"
            :class="`v2-pixel-dog__speech--${currentSpeech.emotion}`"
          >
            <span class="v2-pixel-dog__speech-text">{{ currentSpeech.text }}</span>
          </div>

          <MobilePixelDogMobileSprite
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

      <!-- 物品 -->
      <div v-show="activeTab === 'items'" class="v2-pixel-dog__subpanel">
        <div class="pixel-panel-jagged">
          <div class="pixel-panel-jagged__inner">
            <MobileDogMobileItemsPanel
              :items="dogItems"
              :level="dogState.level"
              :equipped-items="dogState.equippedItems"
              @toggle-equip="onToggleEquip"
            />
          </div>
        </div>
      </div>

      <!-- 记录 -->
      <div v-show="activeTab === 'history'" class="v2-pixel-dog__subpanel">
        <div class="pixel-panel-jagged">
          <div class="pixel-panel-jagged__inner">
            <div class="pixel-dog-history">
              <h3 class="pixel-dog-history__title">&#128220; 成长记录</h3>
              <div class="pixel-dog-history__list">
                <div class="pixel-dog-history__item">
                  <span class="pixel-dog-history__icon">&#127874;</span>
                  <span>等级 {{ dogState.level }} 达成！</span>
                </div>
                <div class="pixel-dog-history__item">
                  <span class="pixel-dog-history__icon">&#10084;&#65039;</span>
                  <span>陪伴值达到 {{ dogState.bond }}</span>
                </div>
                <div class="pixel-dog-history__item">
                  <span class="pixel-dog-history__icon">&#11088;</span>
                  <span>累计获得 {{ totalXp }} 经验值</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 互动按钮 -->
    <div v-show="activeTab === 'home'" class="v2-pixel-dog__interact">
      <div class="pixel-panel-jagged pixel-panel-jagged--plan">
        <div class="pixel-panel-jagged__inner">
          <div class="v2-pixel-dog__interact-grid">
            <button
              type="button"
              class="pixel-btn-start pixel-btn-start--pet"
              @click="onPet"
            >
              <span class="pixel-btn-start__icon">&#9995;</span>
              <span class="pixel-btn-start__label">抚摸</span>
            </button>
            <button
              type="button"
              class="pixel-btn-start pixel-btn-start--greet"
              @click="onGreet"
            >
              <span class="pixel-btn-start__icon">&#128075;</span>
              <span class="pixel-btn-start__label">打招呼</span>
            </button>
            <button
              v-if="intimacy >= 60"
              type="button"
              class="pixel-btn-start pixel-btn-start--nuzzle"
              @click="onNuzzle"
            >
              <span class="pixel-btn-start__icon">&#128054;</span>
              <span class="pixel-btn-start__label">蹭蹭</span>
            </button>
            <button
              v-if="intimacy >= 85"
              type="button"
              class="pixel-btn-start pixel-btn-start--hug"
              @click="onHug"
            >
              <span class="pixel-btn-start__icon">&#129303;</span>
              <span class="pixel-btn-start__label">抱抱</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部导航 -->
    <div class="v2-pixel-dog__nav">
      <button
        v-for="item in navItems"
        :key="item.id"
        type="button"
        class="v2-pixel-dog__nav-item"
        :class="{ 'is-active': activeTab === item.id }"
        @click="activeTab = item.id"
      >
        <span class="v2-pixel-dog__nav-icon">{{ item.icon }}</span>
        <span class="v2-pixel-dog__nav-label">{{ item.label }}</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import MobilePixelDogMobileSprite from '@/mobile/views/pixel-dog/MobilePixelDogMobileSprite.vue'
import MobileDogMobileItemsPanel from '@/mobile/views/pixel-dog/MobileDogMobileItemsPanel.vue'
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

const xpPercent = computed(() => {
  if (!dogState.value.xpNext) return 0
  return Math.round((dogState.value.xp / dogState.value.xpNext) * 100)
})

const emotionPercent = computed(() => {
  return ((dogState.value.emotion + 100) / 200) * 100
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
  if (e >= 50) return 'dog-status-row__value--happy'
  if (e >= 20) return 'dog-status-row__value--good'
  if (e >= 0) return 'dog-status-row__value--neutral'
  if (e >= -30) return 'dog-status-row__value--sad'
  return 'dog-status-row__value--depressed'
})

const emotionBarClass = computed(() => {
  const e = dogState.value.emotion
  if (e >= 50) return 'dog-status-row__fill--happy'
  if (e >= 20) return 'dog-status-row__fill--good'
  if (e >= 0) return 'dog-status-row__fill--neutral'
  if (e >= -30) return 'dog-status-row__fill--sad'
  return 'dog-status-row__fill--depressed'
})

const statusIcon = computed(() => {
  switch (dogState.value.status) {
    case 'HAPPY': return '✨'
    case 'PETTING': return '💆'
    case 'GREETING': return '👋'
    case 'FOCUS': return '🎯'
    case 'SLEEPING': return '💤'
    case 'WALKING': return '🚶'
    case 'NUZZLE': return '🤗'
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
    case 'NUZZLE': return '蹭蹭中'
    default: return '空闲'
  }
})

const totalXp = computed(() => {
  let total = dogState.value.xp
  for (let i = 1; i < dogState.value.level; i++) {
    total += i * 100 + (i - 1) * 50
  }
  return total
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

const navItems = computed(() => [
  { id: 'home' as const, icon: '🐕', label: '主页' },
  { id: 'items' as const, icon: '🎁', label: '物品' },
  { id: 'history' as const, icon: '📜', label: '记录' },
])

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
@use '@/views/pomodoro/pomo-pixel-mixins.scss' as jag;

$pomo-bg: #08081a;
$pomo-text: #e0e8f0;
$pomo-dim: #607080;
$pomo-green: #8bc34a;
$pomo-red: #ef5350;
$pomo-blue: #5c9fd4;
$pomo-purple: #7e57c2;
$pomo-pixel-step: 6px;
$dog-brown: #8d6e63;
$dog-light: #a1887f;
$dog-dark: #4e342e;

@mixin pixel-chamfer($step: $pomo-pixel-step) {
  border-radius: 0;
  clip-path: polygon(
    0 $step,
    $step $step,
    $step 0,
    calc(100% - #{$step}) 0,
    calc(100% - #{$step}) $step,
    100% $step,
    100% calc(100% - #{$step}),
    calc(100% - #{$step}) calc(100% - #{$step}),
    calc(100% - #{$step}) 100%,
    $step 100%,
    $step calc(100% - #{$step}),
    0 calc(100% - #{$step})
  );
}

.v2-pixel-dog {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  background-color: $pomo-bg;
  image-rendering: pixelated;

  :deep(img),
  :deep(svg),
  :deep(canvas) {
    image-rendering: pixelated;
    shape-rendering: crispEdges;
  }
}

.v2-pixel-dog__stars {
  pointer-events: none;
  position: absolute;
  inset: 0;
  z-index: 0;
  background-color: $pomo-bg;
  background-image: url('/patterns/pomo-pixel-stars.svg');
  background-repeat: repeat;
  background-size: 280px 280px;
  image-rendering: pixelated;
  image-rendering: crisp-edges;
}

.v2-pixel-dog__content {
  position: relative;
  z-index: 1;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 10px 12px;
  padding-bottom: 170px;
}

.v2-pixel-dog__home {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.v2-pixel-dog__status-panel {
  flex-shrink: 0;
}

.v2-pixel-dog__sprite-wrap {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  min-height: 0;
}

.v2-pixel-dog__subpanel {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-bottom: 80px;
  scroll-behavior: smooth;
}

.v2-pixel-dog__speech {
  position: absolute;
  top: 8px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 2;
  padding: 8px 14px;
  background: #ffffff;
  border: 2px solid $dog-brown;
  @include pixel-chamfer(3px);
  box-shadow: 3px 3px 0 rgb(0 0 0 / 25%);
  transition: background 0.3s, border-color 0.3s;
  white-space: nowrap;

  &::after {
    content: '';
    position: absolute;
    bottom: -7px;
    left: 50%;
    transform: translateX(-50%);
    width: 0;
    height: 0;
    border-left: 7px solid transparent;
    border-right: 7px solid transparent;
    border-top: 7px solid $dog-brown;
    transition: border-top-color 0.3s;
  }

  &--happy {
    background: #fff8e1;
    border-color: #ffc107;
    &::after { border-top-color: #ffc107; }
    .v2-pixel-dog__speech-text { color: #e65100; }
  }

  &--normal {
    background: #ffffff;
    border-color: $dog-brown;
    &::after { border-top-color: $dog-brown; }
    .v2-pixel-dog__speech-text { color: #333; }
  }

  &--sad {
    background: #e3f2fd;
    border-color: #64b5f6;
    &::after { border-top-color: #64b5f6; }
    .v2-pixel-dog__speech-text { color: #1565c0; }
  }

  &--angry {
    background: #ffebee;
    border-color: #ef5350;
    &::after { border-top-color: #ef5350; }
    .v2-pixel-dog__speech-text { color: #c62828; }
  }
}

.v2-pixel-dog__speech-text {
  font-size: 13px;
  font-weight: 600;
  color: #333;
  transition: color 0.3s;
}

.v2-pixel-dog__interact {
  position: fixed;
  bottom: 56px;
  left: 0;
  right: 0;
  z-index: 99;
  padding: 0 12px 10px;
  background: rgb(8 8 26 / 72%);
  backdrop-filter: blur(4px);
}

.v2-pixel-dog__interact-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

/* 像素面板 */
.pixel-panel-jagged {
  @include jag.pixel-panel-jagged-frame(rgb(61 90 128 / 72%));

  &--plan {
    @include jag.pixel-panel-jagged-frame(rgb(92 159 212 / 68%));
  }
}

.pixel-panel-jagged__inner {
  @include jag.pixel-panel-jagged-inner;
  padding: 12px 14px;
}

/* 状态行 */
.dog-status-row {
  display: flex;
  align-items: center;
  gap: 10px;

  &__item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    min-width: 0;
  }

  &__divider {
    width: 2px;
    height: 36px;
    background: rgb(60 80 100 / 60%);
  }

  &__icon {
    font-size: 18px;
    line-height: 1;
  }

  &__info {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 1px;
  }

  &__label {
    font-size: 11px;
    color: $pomo-dim;
  }

  &__value {
    font-size: 14px;
    font-weight: 700;
    color: $pomo-green;

    &--happy { color: #ffb74d; }
    &--good { color: $pomo-green; }
    &--neutral { color: $pomo-blue; }
    &--sad { color: $pomo-purple; }
    &--depressed { color: $pomo-red; }
  }

  &__bar {
    width: 100%;
    height: 5px;
    background: rgb(30 30 50);
    border: 1px solid #3a3a5a;
    border-radius: 0;
    overflow: hidden;
  }

  &__fill {
    height: 100%;
    transition: width 0.3s ease;

    &--bond {
      background: linear-gradient(90deg, #f48fb1, #f06292);
    }

    &--happy { background: linear-gradient(90deg, #ffb74d, #ff9800); }
    &--good { background: linear-gradient(90deg, #8bc34a, #689f38); }
    &--neutral { background: linear-gradient(90deg, #5c9fd4, #42a5f5); }
    &--sad { background: linear-gradient(90deg, #7e57c2, #5e35b1); }
    &--depressed { background: linear-gradient(90deg, #ef5350, #e53935); }
  }
}

/* XP 行 */
.dog-xp-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed rgb(60 80 100 / 50%);

  &__level {
    display: flex;
    align-items: center;
    gap: 3px;
    padding: 3px 8px;
    border: 2px solid #ffb74d;
    @include pixel-chamfer(3px);
    background: rgb(255 152 0 / 12%);
    flex-shrink: 0;
  }

  &__level-icon {
    font-size: 12px;
    line-height: 1;
  }

  &__level-text {
    font-size: 12px;
    font-weight: 700;
    color: #ffb74d;
    line-height: 1;
  }

  &__track {
    flex: 1;
    height: 6px;
    background: rgb(30 30 50);
    border: 1px solid #3a3a5a;
    border-radius: 0;
    overflow: hidden;
  }

  &__fill {
    height: 100%;
    background: linear-gradient(90deg, #ffb74d, #ff9800);
    transition: width 0.3s ease;
  }

  &__text {
    font-size: 10px;
    color: $pomo-dim;
    white-space: nowrap;
    font-variant-numeric: tabular-nums;
  }
}

/* 互动按钮 */
.pixel-btn-start {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  min-height: 44px;
  padding: 10px 12px;
  border: 3px solid #6a9e3a;
  @include pixel-chamfer(4px);
  font-size: 15px;
  font-weight: 700;
  color: #1a2a10;
  background: $pomo-green;
  box-shadow: 0 4px 0 #4a7a28, 4px 4px 0 rgb(0 0 0 / 25%);
  cursor: pointer;
  transition: transform 0.1s, box-shadow 0.1s, filter 0.1s;
  image-rendering: pixelated;
  -webkit-tap-highlight-color: transparent;

  &:active {
    transform: translate(2px, 2px);
    box-shadow: 0 1px 0 #4a7a28, 1px 1px 0 rgb(0 0 0 / 20%);
  }

  &__icon {
    font-size: 18px;
    line-height: 1;
  }

  &__label {
    font-size: 14px;
    font-weight: 700;
    line-height: 1;
  }

  &--hug {
    border-color: #e6a700;
    background: #ffc107;
    color: #4a3200;
    box-shadow: 0 4px 0 #c88700, 4px 4px 0 rgb(0 0 0 / 25%);

    &:active {
      box-shadow: 0 1px 0 #c88700, 1px 1px 0 rgb(0 0 0 / 20%);
    }
  }

  &--nuzzle {
    border-color: #c2185b;
    background: #ec407a;
    color: #4a1030;
    box-shadow: 0 4px 0 #a0003d, 4px 4px 0 rgb(0 0 0 / 25%);

    &:active {
      box-shadow: 0 1px 0 #a0003d, 1px 1px 0 rgb(0 0 0 / 20%);
    }
  }

  &--pet {
    border-color: #8d6e63;
    background: $dog-light;
    color: #2d1f1f;
    box-shadow: 0 4px 0 $dog-brown, 4px 4px 0 rgb(0 0 0 / 25%);

    &:active {
      box-shadow: 0 1px 0 $dog-brown, 1px 1px 0 rgb(0 0 0 / 20%);
    }
  }

  &--greet {
    border-color: #5c9fd4;
    background: $pomo-blue;
    color: #1a2a4a;
    box-shadow: 0 4px 0 #3d7bc9, 4px 4px 0 rgb(0 0 0 / 25%);

    &:active {
      box-shadow: 0 1px 0 #3d7bc9, 1px 1px 0 rgb(0 0 0 / 20%);
    }
  }
}

/* 成长记录 */
.pixel-dog-history {
  &__title {
    font-size: 14px;
    font-weight: 700;
    color: $pomo-text;
    margin-bottom: 10px;
  }

  &__list {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  &__item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 12px;
    background: rgb(16 16 40 / 30%);
    border: 2px solid rgb(60 80 100);
    @include pixel-chamfer(3px);
    font-size: 13px;
    color: $pomo-text;
  }

  &__icon {
    font-size: 16px;
  }
}

/* 底部导航 */
.v2-pixel-dog__nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 8px 12px;
  padding-bottom: max(8px, env(safe-area-inset-bottom));
  border-top: 2px solid #2a2a50;
  background: rgb(8 8 26 / 78%);
  backdrop-filter: blur(4px);
}

.v2-pixel-dog__nav-item {
  --s: 3px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  flex: 1;
  max-width: 100px;
  padding: 6px 10px;
  border: 2px solid transparent;
  border-radius: 0;
  @include jag.pixel-jag-clip(3);
  background: transparent;
  color: #8090a8;
  cursor: pointer;
  transition: color 0.15s, border-color 0.15s, background 0.15s;
  -webkit-tap-highlight-color: transparent;

  &.is-active {
    color: $pomo-green;
    border-color: rgb(139 195 74 / 60%);
    background: rgb(139 195 74 / 10%);
  }

  &:active:not(.is-active) {
    color: #c0d0e8;
  }
}

.v2-pixel-dog__nav-icon {
  font-size: 18px;
  line-height: 1;
}

.v2-pixel-dog__nav-label {
  font-size: 11px;
  font-weight: 700;
  line-height: 1;
}
</style>