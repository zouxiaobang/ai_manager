<template>
  <div class="pixel-dog-items">
    <h3 style="color: white">🎁 物品收集</h3>

    <div class="pixel-dog-items__grid">
      <div
        v-for="item in items"
        :key="item.id"
        class="pixel-dog-item"
        :class="{
          'pixel-dog-item--locked': !isUnlocked(item),
          'pixel-dog-item--equipped': isEquipped(item),
        }"
        @click="onItemClick(item)"
      >
        <div class="pixel-dog-item__icon">{{ item.icon }}</div>
        <div class="pixel-dog-item__name">{{ item.name }}</div>
        <div class="pixel-dog-item__status">
          <span v-if="!isUnlocked(item)" class="pixel-dog-item__locked">Lv.{{ item.requireLevel }}</span>
          <span v-else-if="isEquipped(item)" class="pixel-dog-item__equipped">已装饰 · 点击卸下</span>
          <span v-else class="pixel-dog-item__unequipped">已解锁 · 点击装饰</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PixelDogItemVO } from '@/api/pixelDog'

const props = defineProps<{
  items: PixelDogItemVO[]
  level: number
  equippedItems: number
}>()

const emit = defineEmits<{
  'toggle-equip': [itemId: number]
}>()

function isUnlocked(item: PixelDogItemVO): boolean {
  return props.level >= item.requireLevel
}

function isEquipped(item: PixelDogItemVO): boolean {
  // 使用 BigInt 避免 32 位溢出（1 << 32 在 JS 中会回绕为 1）
  return (BigInt(props.equippedItems) & (1n << BigInt(item.id - 1))) !== 0n
}

function onItemClick(item: PixelDogItemVO) {
  if (!isUnlocked(item)) return
  emit('toggle-equip', item.id)
}
</script>

<style scoped lang="scss">
@use './pixel-dog.scss';

.pixel-dog-item {
  cursor: pointer;
  transition: all 0.2s;

  &--locked {
    cursor: not-allowed;
    opacity: 0.4;
    filter: grayscale(0.8);
  }

  &--equipped {
    border-color: #fbbf24 !important;
    box-shadow: 0 0 8px rgba(251, 191, 36, 0.4);
  }

  &:not(&--locked):hover {
    transform: translateY(-2px);
  }
}

.pixel-dog-item__status {
  font-size: 10px;
  text-align: center;
  margin-top: 2px;
}

.pixel-dog-item__locked {
  color: #6b7280;
}

.pixel-dog-item__equipped {
  color: #fbbf24;
  font-weight: 600;
}

.pixel-dog-item__unequipped {
  color: #60a5fa;
}
</style>
