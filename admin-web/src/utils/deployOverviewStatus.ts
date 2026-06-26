import type { HealthData } from '@/api/types'
import type { DeployNodeCard } from '@/data/deploy-center'
import { deployAdminUrl, deployOverviewNodes } from '@/data/deploy-center'

type TagFn = (key: string, params?: Record<string, unknown>) => string

function isUp(value?: string): boolean {
  return value === 'UP'
}

function nodeTag(
  up: boolean,
  checking: boolean,
  t: TagFn,
  upKey: string,
  downKey: string,
  checkingKey: string,
): string[] {
  if (checking) return [t(checkingKey)]
  return [t(up ? upKey : downKey)]
}

export function buildDeployOverviewNodes(
  health: HealthData | null,
  checking: boolean,
  t: TagFn,
): DeployNodeCard[] {
  const appUp = health ? isUp(health.appNodeStatus) || isUp(health.status) : false
  const dataUp = health
    ? isUp(health.dataNodeStatus) || (isUp(health.mysql) && isUp(health.redis))
    : false
  const serviceUp = health ? isUp(health.status) : false

  return deployOverviewNodes.map((node) => {
    if (node.key === 'app') {
      return {
        ...node,
        tags: nodeTag(
          appUp,
          checking,
          t,
          'deployCenter.overviewTags.running',
          'deployCenter.overviewTags.offline',
          'deployCenter.overviewTags.checking',
        ),
        tagTone: appUp && !checking ? 'success' : 'theme',
      }
    }
    if (node.key === 'data') {
      return {
        ...node,
        tags: nodeTag(
          dataUp,
          checking,
          t,
          'deployCenter.overviewTags.running',
          'deployCenter.overviewTags.abnormal',
          'deployCenter.overviewTags.checking',
        ),
        tagTone: dataUp && !checking ? 'success' : 'theme',
      }
    }
    if (node.key === 'dev') {
      return {
        ...node,
        tags: [t('deployCenter.overviewTags.devLocal')],
        tagTone: 'theme' as const,
      }
    }
    if (node.key === 'url') {
      return {
        ...node,
        value: deployAdminUrl,
        tags: nodeTag(
          serviceUp,
          checking,
          t,
          'deployCenter.overviewTags.accessible',
          'deployCenter.overviewTags.unreachable',
          'deployCenter.overviewTags.checking',
        ),
        tagTone: serviceUp && !checking ? 'success' : 'theme',
      }
    }
    return node
  })
}

export function formatNodePairStatus(
  health: HealthData | null,
  checking: boolean,
): { footKey: string; tone: 'green' | 'orange' | 'gray' } {
  if (checking) return { footKey: 'checking', tone: 'gray' }
  if (!health) return { footKey: 'offline', tone: 'gray' }
  const appUp = isUp(health.appNodeStatus) || isUp(health.status)
  const dataUp = isUp(health.dataNodeStatus) || (isUp(health.mysql) && isUp(health.redis))
  if (appUp && dataUp) return { footKey: 'online', tone: 'green' }
  if (!appUp && !dataUp) return { footKey: 'offline', tone: 'gray' }
  return { footKey: 'partialOnline', tone: 'orange' }
}
