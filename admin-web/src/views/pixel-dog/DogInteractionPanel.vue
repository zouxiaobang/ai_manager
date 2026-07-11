<template>
  <div class="pixel-panel-jagged pixel-panel-jagged--plan">
    <div class="pixel-panel-jagged__inner">
      <h3 style="color: white">🎮 互动</h3>

      <button
        v-if="intimacy >= 85"
        type="button"
        class="pixel-btn-start pixel-btn-start--hug"
        @click="$emit('hug')"
      >
        <span class="pixel-btn-start__icon" aria-hidden="true">🤗</span>
        抱抱
      </button>

      <button
        v-if="intimacy >= 60"
        type="button"
        class="pixel-btn-start pixel-btn-start--nuzzle"
        @click="$emit('nuzzle')"
      >
        <span class="pixel-btn-start__icon" aria-hidden="true">🐶</span>
        蹭蹭
      </button>

      <button
        type="button"
        class="pixel-btn-start pixel-btn-start--pet"
        @click="$emit('pet')"
      >
        <span class="pixel-btn-start__icon" aria-hidden="true">✋</span>
        抚摸
      </button>

      <button
        type="button"
        class="pixel-btn-start pixel-btn-start--greet"
        @click="$emit('greet')"
      >
        <span class="pixel-btn-start__icon" aria-hidden="true">👋</span>
        打招呼
      </button>

      <div class="interaction-tips">
        <div class="interaction-tips__item">
          <span class="interaction-tips__icon">💡</span>
          <span>完成番茄钟可获得经验和陪伴值</span>
        </div>
        <div class="interaction-tips__item">
          <span class="interaction-tips__icon">⏰</span>
          <span>长时间不互动会导致心情下降</span>
        </div>
        <div class="interaction-tips__item">
          <span class="interaction-tips__icon">🌙</span>
          <span>凌晨 00:00-7:00 像素狗会睡觉</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  dog: {
    emotion: number
    bond: number
  }
}>()

defineEmits<{
  pet: []
  greet: []
  nuzzle: []
  hug: []
}>()

const intimacy = computed(() => {
  return (props.dog.emotion + 100) / 2 * 0.4 + props.dog.bond * 0.6
})
</script>

<style scoped lang="scss">
@use './pixel-dog.scss';
</style>