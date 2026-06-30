import { getEcommerceImageUrl } from '@/api/ecommerce/image'

const PLATFORM_ICON_BY_CODE: Record<number, string> = {
  0: '/icons/platforms/offline.svg',
  1: '/icons/platforms/1688.svg',
  2: '/icons/platforms/taobao.svg',
  3: '/icons/platforms/tmall.svg',
  4: '/icons/platforms/pinduoduo.svg',
  5: '/icons/platforms/douyin.svg',
  6: '/icons/platforms/jd.svg',
}

function resolveIconByName(platformName?: string): string | undefined {
  const name = (platformName ?? '').trim()
  if (!name) return undefined
  if (/1688|阿里巴巴|alibaba/i.test(name)) return PLATFORM_ICON_BY_CODE[1]
  if (/天猫|tmall/i.test(name)) return PLATFORM_ICON_BY_CODE[3]
  if (/淘宝|taobao/i.test(name)) return PLATFORM_ICON_BY_CODE[2]
  if (/拼多多|pinduoduo/i.test(name)) return PLATFORM_ICON_BY_CODE[4]
  if (/抖音|douyin/i.test(name)) return PLATFORM_ICON_BY_CODE[5]
  if (/京东|jd/i.test(name)) return PLATFORM_ICON_BY_CODE[6]
  if (/线下|门店|offline/i.test(name)) return PLATFORM_ICON_BY_CODE[0]
  return undefined
}

export interface PlatformIconMeta {
  src: string
  isCustomAvatar?: boolean
}

export interface PlatformOptionTone {
  background: string
  color: string
}

const PLATFORM_OPTION_TONE_BY_CODE: Record<number, PlatformOptionTone> = {
  0: { background: '#f1f5f9', color: '#475569' },
  1: { background: '#fff7ed', color: '#c2410c' },
  2: { background: '#fff4e6', color: '#ea580c' },
  3: { background: '#fef2f2', color: '#dc2626' },
  4: { background: '#fff1f2', color: '#e11d48' },
  5: { background: '#f3f4f6', color: '#111827' },
  6: { background: '#fef2f2', color: '#b91c1c' },
}

function resolveCodeByName(platformName?: string): number | undefined {
  const name = (platformName ?? '').trim()
  if (!name) return undefined
  if (/1688|阿里巴巴|alibaba/i.test(name)) return 1
  if (/天猫|tmall/i.test(name)) return 3
  if (/淘宝|taobao/i.test(name)) return 2
  if (/拼多多|pinduoduo/i.test(name)) return 4
  if (/抖音|抖店|douyin/i.test(name)) return 5
  if (/京东|jd/i.test(name)) return 6
  if (/线下|门店|offline/i.test(name)) return 0
  return undefined
}

export function resolvePlatformOptionTone(
  platformCode?: number | null,
  platformName?: string,
): PlatformOptionTone {
  const code = platformCode ?? resolveCodeByName(platformName)
  if (code != null && PLATFORM_OPTION_TONE_BY_CODE[code]) {
    return PLATFORM_OPTION_TONE_BY_CODE[code]
  }
  return { background: '#eff6ff', color: '#2563eb' }
}

function resolveBuiltinPlatformIconMeta(platformName?: string, platformCode?: number | null): PlatformIconMeta {
  if (platformCode != null && PLATFORM_ICON_BY_CODE[platformCode]) {
    return { src: PLATFORM_ICON_BY_CODE[platformCode], isCustomAvatar: false }
  }
  return { src: resolveIconByName(platformName) ?? '/icons/platforms/other.svg', isCustomAvatar: false }
}

export function resolvePlatformIconMeta(
  platformName?: string,
  platformCode?: number | null,
  avatarUrl?: string | null,
): PlatformIconMeta {
  if (avatarUrl?.trim()) {
    return { src: getEcommerceImageUrl(avatarUrl), isCustomAvatar: true }
  }
  return resolveBuiltinPlatformIconMeta(platformName, platformCode)
}

export function resolvePlatformIcon(
  platformName?: string,
  platformCode?: number | null,
  avatarUrl?: string | null,
): string {
  return resolvePlatformIconMeta(platformName, platformCode, avatarUrl).src
}
