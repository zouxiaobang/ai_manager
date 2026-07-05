import { computed, ref } from 'vue'
import { fetchAllCartons, type EcCarton } from '@/api/ecommerce/carton'
import { cartonIllustrationUrl } from '@/constants/cartonIllustrations'
import { getEcommerceImageUrl } from '@/api/ecommerce/image'

export interface MobileCartonItem {
  id: number
  name: string
  spec: string
  unitPrice: string
  factoryName: string
  image: string
  volume: number
}

export type CartonCategory = 'all' | 'small' | 'medium' | 'large'

const categories: { id: CartonCategory; name: string; icon: string }[] = [
  { id: 'all', name: '全部', icon: '📦' },
  { id: 'small', name: '小号', icon: '📭' },
  { id: 'medium', name: '中号', icon: '📮' },
  { id: 'large', name: '大号', icon: '📯' },
]

function getCartonVolume(carton: EcCarton): number {
  const l = Number(carton.lengthCm) || 0
  const w = Number(carton.widthCm) || 0
  const h = Number(carton.heightCm) || 0
  return l * w * h
}

function classifyCarton(volume: number): CartonCategory {
  if (volume === 0) return 'medium'
  if (volume < 5000) return 'small'
  if (volume < 30000) return 'medium'
  return 'large'
}

export function useMobileCarton() {
  const cartons = ref<MobileCartonItem[]>([])
  const loading = ref(false)
  const searchQuery = ref('')
  const activeCategory = ref<CartonCategory>('all')

  const filteredCartons = computed(() => {
    let result = cartons.value

    if (activeCategory.value !== 'all') {
      result = result.filter((c) => {
        const category = classifyCarton(c.volume)
        return category === activeCategory.value
      })
    }

    if (searchQuery.value.trim()) {
      const query = searchQuery.value.toLowerCase()
      result = result.filter(
        (c) =>
          c.name.toLowerCase().includes(query) ||
          c.spec.toLowerCase().includes(query) ||
          c.factoryName.toLowerCase().includes(query),
      )
    }

    return result
  })

  const categoryCounts = computed(() => {
    const counts: Record<CartonCategory, number> = {
      all: cartons.value.length,
      small: 0,
      medium: 0,
      large: 0,
    }
    cartons.value.forEach((c) => {
      const category = classifyCarton(c.volume)
      counts[category]++
    })
    return counts
  })

  const categoryList = computed(() =>
    categories.map((cat) => ({
      ...cat,
      count: categoryCounts.value[cat.id],
    })),
  )

  async function loadCartons() {
    loading.value = true
    try {
      const result = await fetchAllCartons()
      cartons.value = (result.records ?? []).map((c: EcCarton) => ({
        id: c.id,
        name: c.name,
        spec: [c.lengthCm, c.widthCm, c.heightCm]
          .filter((v) => v != null)
          .join('×'),
        unitPrice: c.unitPrice != null ? `¥${Number(c.unitPrice).toFixed(2)}` : '-',
        factoryName: c.factoryName || '-',
        image: getEcommerceImageUrl(c.previewImage) || cartonIllustrationUrl(c.illustrationVariant, c.id),
        volume: getCartonVolume(c),
      }))
    } finally {
      loading.value = false
    }
  }

  return {
    cartons,
    loading,
    searchQuery,
    activeCategory,
    filteredCartons,
    categoryList,
    loadCartons,
    categories,
  }
}
