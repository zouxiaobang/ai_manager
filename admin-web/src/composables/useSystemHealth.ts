import { ref } from 'vue'
import { fetchHealth } from '@/api/health'
import type { HealthData } from '@/api/types'

const healthData = ref<HealthData | null>(null)
const healthLoading = ref(false)
let inflight: Promise<void> | null = null

export function useSystemHealth() {
  async function refreshHealth(force = false) {
    if (inflight && !force) {
      await inflight
      return
    }

    inflight = (async () => {
      healthLoading.value = true
      try {
        healthData.value = await fetchHealth()
      } catch {
        healthData.value = null
      } finally {
        healthLoading.value = false
        inflight = null
      }
    })()

    await inflight
  }

  return {
    healthData,
    healthLoading,
    refreshHealth,
  }
}
