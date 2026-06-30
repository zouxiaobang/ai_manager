<template>
  <img
    :src="iconSrc"
    :alt="alt || station?.name || ''"
    class="express-station-avatar"
    :class="`express-station-avatar--${size}`"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { resolveExpressIconFromStation, type ExpressStationVisual } from '@/utils/expressVisual'

const props = withDefaults(defineProps<{
  station?: ExpressStationVisual | null
  size?: 'xs' | 'sm' | 'md' | 'lg'
  alt?: string
}>(), {
  size: 'sm',
})

const iconSrc = computed(() => resolveExpressIconFromStation(props.station))
</script>

<style scoped lang="scss">
.express-station-avatar {
  object-fit: cover;
  border-radius: 6px;
  box-shadow: 0 0 0 1px rgb(0 0 0 / 6%);
  flex-shrink: 0;
  display: block;
}

.express-station-avatar--xs {
  width: 18px;
  height: 18px;
  border-radius: 4px;
}

.express-station-avatar--sm {
  width: 22px;
  height: 22px;
}

.express-station-avatar--md {
  width: 28px;
  height: 28px;
}

.express-station-avatar--lg {
  width: 40px;
  height: 40px;
  border-radius: 8px;
}
</style>
