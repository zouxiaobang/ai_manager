<template>
  <div class="pixel-panel-jagged">
    <div class="pixel-panel-jagged__inner">
      <h3 style="color: white">📊 状态</h3>

      <div class="dog-status-item">
        <div class="dog-status-item__header">
          <span class="dog-status-item__icon">⭐</span>
          <span class="dog-status-item__label">
            等级
            <span class="xp-tip-wrapper">
              <span class="xp-tip-trigger" @mouseenter="showXpTip = true" @mouseleave="showXpTip = false">?</span>
              <div v-show="showXpTip" class="xp-tip">
                <div class="xp-tip__title">升级条件</div>
                <div class="xp-tip__item">🍅 完成番茄钟轮次 +20 XP</div>
                <div class="xp-tip__item">✅ 完成待办 +10 XP</div>
                <div class="xp-tip__item">📝 创建笔记 +15 XP</div>
              </div>
            </span>
          </span>
          <span class="dog-status-item__value">{{ dog.level }}</span>
        </div>
        <div class="dog-status-item__bar">
          <div
            class="dog-status-item__fill dog-status-item__fill--xp"
            :style="{ width: xpPercent + '%' }"
          />
        </div>
        <span class="dog-status-item__hint">{{ dog.xp }} / {{ dog.xpNext }} XP</span>
      </div>

      <div class="dog-status-item">
        <div class="dog-status-item__header">
          <span class="dog-status-item__icon">❤️</span>
          <span class="dog-status-item__label">陪伴值</span>
          <span class="dog-status-item__value">{{ dog.bond }}</span>
        </div>
        <div class="dog-status-item__bar">
          <div
            class="dog-status-item__fill dog-status-item__fill--bond"
            :style="{ width: dog.bond + '%' }"
          />
        </div>
        <span class="dog-status-item__hint">亲密度</span>
      </div>

      <div class="dog-status-item">
        <div class="dog-status-item__header">
          <span class="dog-status-item__icon">{{ emotionIcon }}</span>
          <span class="dog-status-item__label">心情</span>
          <span class="dog-status-item__value" :class="emotionClass">{{ emotionText }}</span>
        </div>
        <div class="dog-status-item__bar">
          <div
            class="dog-status-item__fill"
            :class="emotionBarClass"
            :style="{ width: emotionPercent + '%' }"
          />
        </div>
        <span class="dog-status-item__hint">{{ dog.emotion }}</span>
      </div>

      <div class="dog-status-item">
        <div class="dog-status-item__header">
          <span class="dog-status-item__icon">{{ statusIcon }}</span>
          <span class="dog-status-item__label">状态</span>
          <span class="dog-status-item__value">{{ statusText }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

type DogStatus = 'IDLE' | 'HAPPY' | 'PETTING' | 'GREETING' | 'SLEEPING' | 'WALKING' | 'FOCUS'

const showXpTip = ref(false)

interface Props {
  dog: {
    level: number
    xp: number
    xpNext: number
    bond: number
    emotion: number
    status: DogStatus
  }
}

const props = defineProps<Props>()

const xpPercent = computed(() => {
  if (!props.dog.xpNext) return 0
  return Math.round((props.dog.xp / props.dog.xpNext) * 100)
})

const emotionPercent = computed(() => {
  return ((props.dog.emotion + 100) / 200) * 100
})

const emotionIcon = computed(() => {
  const e = props.dog.emotion
  if (e >= 50) return '😄'
  if (e >= 20) return '😊'
  if (e >= 0) return '😐'
  if (e >= -30) return '😕'
  return '😢'
})

const emotionText = computed(() => {
  const e = props.dog.emotion
  if (e >= 50) return '开心'
  if (e >= 20) return '愉快'
  if (e >= 0) return '平静'
  if (e >= -30) return '无聊'
  return '低落'
})

const emotionClass = computed(() => {
  const e = props.dog.emotion
  if (e >= 50) return 'dog-status-item__value--happy'
  if (e >= 20) return 'dog-status-item__value--good'
  if (e >= 0) return 'dog-status-item__value--neutral'
  if (e >= -30) return 'dog-status-item__value--sad'
  return 'dog-status-item__value--depressed'
})

const emotionBarClass = computed(() => {
  const e = props.dog.emotion
  if (e >= 50) return 'dog-status-item__fill--happy'
  if (e >= 20) return 'dog-status-item__fill--good'
  if (e >= 0) return 'dog-status-item__fill--neutral'
  if (e >= -30) return 'dog-status-item__fill--sad'
  return 'dog-status-item__fill--depressed'
})

const statusIcon = computed(() => {
  switch (props.dog.status) {
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
  switch (props.dog.status) {
    case 'HAPPY': return '开心'
    case 'PETTING': return '被抚摸'
    case 'GREETING': return '打招呼'
    case 'FOCUS': return '专注中'
    case 'SLEEPING': return '睡觉中'
    case 'WALKING': return '散步'
    default: return '空闲'
  }
})
</script>

<style scoped lang="scss">
@use './pixel-dog.scss';
</style>