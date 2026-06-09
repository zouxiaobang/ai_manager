import { getData } from './request'
import type { HealthData } from './types'

export function fetchHealth() {
  return getData<HealthData>('/api/health')
}
