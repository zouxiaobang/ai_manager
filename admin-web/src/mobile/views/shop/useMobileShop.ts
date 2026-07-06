import { computed, ref } from 'vue'
import { fetchShopOptions, type EcShop } from '@/api/ecommerce/shop.ts'
import { fetchPlatformOptions, type EcPlatform } from '@/api/ecommerce/platform.ts'

const PLATFORM_ICONS: Record<string, string> = {
  '淘宝': '🛒',
  '京东': '🏪',
  '拼多多': '📱',
  '抖音': '🎵',
}

export function useMobileShop() {
  const shops = ref<EcShop[]>([])
  const platforms = ref<EcPlatform[]>([])
  const loading = ref(false)
  const searchQuery = ref('')
  const activePlatformId = ref<number | null>(null)

  const filteredShops = computed(() => {
    let result = shops.value

    if (activePlatformId.value !== null) {
      result = result.filter((s) => s.platformId === activePlatformId.value)
    }

    if (searchQuery.value.trim()) {
      const query = searchQuery.value.toLowerCase()
      result = result.filter(
        (s) =>
          s.name.toLowerCase().includes(query) ||
          (s.nameEn && s.nameEn.toLowerCase().includes(query)),
      )
    }

    return result
  })

  const platformCounts = computed(() => {
    const counts: Record<number, number> = {}
    shops.value.forEach((s) => {
      counts[s.platformId] = (counts[s.platformId] || 0) + 1
    })
    return counts
  })

  const platformList = computed(() => {
    const allCount = shops.value.length
    const list: Array<{ id: number | null; name: string; icon: string; count: number }> = [
      { id: null, name: '全部', icon: '📦', count: allCount },
    ]

    platforms.value.forEach((p) => {
      list.push({
        id: p.id,
        name: p.name,
        icon: PLATFORM_ICONS[p.name] || '📦',
        count: platformCounts.value[p.id] || 0,
      })
    })

    return list
  })

  async function loadShops() {
    loading.value = true
    try {
      const [shopList, platformList] = await Promise.all([fetchShopOptions(), fetchPlatformOptions()])
      shops.value = shopList
      platforms.value = platformList
    } finally {
      loading.value = false
    }
  }

  return {
    shops,
    platforms,
    loading,
    searchQuery,
    activePlatformId,
    filteredShops,
    platformList,
    loadShops,
  }
}
