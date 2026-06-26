<template>
  <span
    class="wr-svg-icon"
    :class="className"
    :style="iconStyle"
    role="img"
    aria-hidden="true"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { warRoomIconUrl, type WarRoomIconGroup } from '@/data/war-room-icons'

const props = withDefaults(
  defineProps<{
    group: WarRoomIconGroup
    name: string
    size?: number
    className?: string
  }>(),
  {
    size: 26,
    className: '',
  },
)

const iconStyle = computed(() => {
  const url = warRoomIconUrl(props.group, props.name)
  return {
    width: `${props.size}px`,
    height: `${props.size}px`,
    WebkitMaskImage: `url(${url})`,
    maskImage: `url(${url})`,
  }
})
</script>

<style scoped>
.wr-svg-icon {
  display: inline-block;
  flex-shrink: 0;
  background: currentColor;
  mask-repeat: no-repeat;
  mask-position: center;
  mask-size: contain;
  -webkit-mask-repeat: no-repeat;
  -webkit-mask-position: center;
  -webkit-mask-size: contain;
}
</style>
