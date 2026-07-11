<template>
  <div class="mobile-dog-items">
    <div class="mobile-dog-items__title">🎁 物品收集</div>

    <div class="mobile-dog-items__grid">
      <div
        v-for="item in items"
        :key="item.id"
        class="mobile-dog-item"
        :class="{
          'mobile-dog-item--locked': !isUnlocked(item),
          'mobile-dog-item--equipped': isEquipped(item),
        }"
        :style="{ '--item-color': item.color }"
        @click="onItemClick(item)"
      >
        <div class="mobile-dog-item__icon">{{ item.icon }}</div>
        <div class="mobile-dog-item__name" :style="{ color: isUnlocked(item) ? item.color : undefined }">{{ item.name }}</div>
        <div class="mobile-dog-item__status">
          <span v-if="!isUnlocked(item)" class="mobile-dog-item__locked">Lv.{{ item.requireLevel }}</span>
          <span v-else-if="isEquipped(item)" class="mobile-dog-item__equipped">已装饰 · 点击卸下</span>
          <span v-else class="mobile-dog-item__unequipped">已解锁 · 点击装饰</span>
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
  return (BigInt(props.equippedItems) & (1n << BigInt(item.id - 1))) !== 0n
}

function onItemClick(item: PixelDogItemVO) {
  if (!isUnlocked(item)) return
  emit('toggle-equip', item.id)
}
</script>

<style scoped lang="scss">
$dog-brown: #8d6e63;

@mixin pixel-chamfer($size: 4px) {
  clip-path: polygon(
    $size 0,
    calc(100% - #{$size}) 0,
    100% $size,
    100% calc(100% - #{$size}),
    calc(100% - #{$size}) 100%,
    $size 100%,
    0 calc(100% - #{$size}),
    0 $size
  );
}

.mobile-dog-items {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  padding-bottom: 8px;

  &__grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 10px;
    margin-top: 8px;
  }

  &__title {
    font-size: 15px;
    font-weight: 700;
    color: #e0e8f0;
    padding: 4px 0;
  }
}

.mobile-dog-item {
  padding: 12px 6px;
  background: rgb(22 22 56 / 60%);
  border: 2px solid var(--item-color, #{$dog-brown});
  @include pixel-chamfer(4px);
  text-align: center;
  transition: all 0.15s;
  -webkit-tap-highlight-color: transparent;
  box-shadow: 0 2px 0 rgb(0 0 0 / 25%);

  &:active:not(&--locked) {
    transform: translate(1px, 2px);
    box-shadow: 0 0 0 rgb(0 0 0 / 25%);
  }

  &--locked {
    opacity: 0.45;
    filter: grayscale(0.85);
    cursor: not-allowed;
    border-color: #3a3a5a;
  }

  &--equipped {
    border-width: 3px;
    box-shadow: 0 0 10px color-mix(in srgb, var(--item-color, #fbbf24) 35%, transparent);
  }

  &__icon {
    font-size: 30px;
    margin-bottom: 6px;
    line-height: 1;
  }

  &__name {
    font-size: 13px;
    font-weight: 700;
    margin-bottom: 4px;
    text-shadow: 0 1px 2px rgb(0 0 0 / 60%);
    color: #fdfdfd;
  }

  &__status {
    font-size: 10px;
    text-align: center;
    line-height: 1.2;
  }

  &__locked {
    color: #9ca3af;
    font-weight: 500;
  }

  &__equipped {
    color: #fbbf24;
    font-weight: 600;
  }

  &__unequipped {
    color: #60a5fa;
    font-weight: 500;
  }
}
</style>
