import { computed } from 'vue'
import { useRoute } from 'vue-router'

/** 移动端电商子模块是否启用手绘边框（工厂页等独立移动端视图除外） */
export function useMobileEcDoodle() {
  const route = useRoute()

  const enabled = computed(() => {
    if (typeof document === 'undefined') return false
    if (!document.documentElement.classList.contains('is-mobile-shell')) return false
    const name = String(route.name ?? '')
    return name.startsWith('mobile-ecommerce') && name !== 'mobile-ecommerce'
  })

  return { enabled }
}
