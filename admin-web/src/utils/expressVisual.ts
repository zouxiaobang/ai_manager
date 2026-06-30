import { getEcommerceImageUrl } from '@/api/ecommerce/image'

const EXPRESS_ICON = {
  zto: '/icons/express/zto.svg',
  sf: '/icons/express/sf.svg',
  yto: '/icons/express/yto.svg',
  yunda: '/icons/express/yunda.svg',
  sto: '/icons/express/sto.svg',
  jt: '/icons/express/jtexpress.svg',
  deppon: '/icons/express/deppon.svg',
  ems: '/icons/express/ems.svg',
  jd: '/icons/express/jd-logistics.svg',
  best: '/icons/express/best.svg',
  other: '/icons/express/other.svg',
} as const

const EXPRESS_MATCHERS: Array<{ pattern: RegExp; icon: string }> = [
  { pattern: /顺丰|sf[\s-]?express|sf标快|顺丰标快|顺丰速运/i, icon: EXPRESS_ICON.sf },
  { pattern: /中通|zto/i, icon: EXPRESS_ICON.zto },
  { pattern: /圆通|yto/i, icon: EXPRESS_ICON.yto },
  { pattern: /韵达|yunda/i, icon: EXPRESS_ICON.yunda },
  { pattern: /申通|sto/i, icon: EXPRESS_ICON.sto },
  { pattern: /极兔|j\s*&\s*t|jtexpress/i, icon: EXPRESS_ICON.jt },
  { pattern: /德邦|deppon/i, icon: EXPRESS_ICON.deppon },
  { pattern: /ems|邮政|中国邮政/i, icon: EXPRESS_ICON.ems },
  { pattern: /京东物流|京东快递|jd[\s-]?logistics/i, icon: EXPRESS_ICON.jd },
  { pattern: /百世|best[\s-]?express/i, icon: EXPRESS_ICON.best },
]

export interface ExpressStationVisual {
  name?: string
  nameAliases?: string[]
  avatarUrl?: string | null
}

function resolveIconByName(name?: string): string | undefined {
  const text = (name ?? '').trim()
  if (!text) return undefined
  return EXPRESS_MATCHERS.find(({ pattern }) => pattern.test(text))?.icon
}

export interface ExpressIconMeta {
  src: string
  isCustomAvatar: boolean
}

export function resolveExpressIconMeta(
  stationName?: string,
  aliases?: string[],
  avatarUrl?: string | null,
): ExpressIconMeta {
  if (avatarUrl?.trim()) {
    return { src: getEcommerceImageUrl(avatarUrl), isCustomAvatar: true }
  }
  const candidates = [stationName, ...(aliases ?? [])]
  for (const name of candidates) {
    const icon = resolveIconByName(name)
    if (icon) return { src: icon, isCustomAvatar: false }
  }
  return { src: EXPRESS_ICON.other, isCustomAvatar: false }
}

export function resolveExpressIconMetaFromStation(station?: ExpressStationVisual | null): ExpressIconMeta {
  if (!station) {
    return { src: EXPRESS_ICON.other, isCustomAvatar: false }
  }
  return resolveExpressIconMeta(station.name, station.nameAliases, station.avatarUrl)
}

export function resolveExpressIcon(
  stationName?: string,
  aliases?: string[],
  avatarUrl?: string | null,
): string {
  return resolveExpressIconMeta(stationName, aliases, avatarUrl).src
}

export function resolveExpressIconFromStation(station?: ExpressStationVisual | null): string {
  return resolveExpressIconMetaFromStation(station).src
}

const ALIAS_TAG_PALETTES = [
  { bg: '#ecfdf5', border: '#bbf7d0', text: '#047857' },
  { bg: '#eff6ff', border: '#bfdbfe', text: '#1d4ed8' },
  { bg: '#fef3c7', border: '#fde68a', text: '#b45309' },
  { bg: '#fce7f3', border: '#fbcfe8', text: '#be185d' },
  { bg: '#f3e8ff', border: '#e9d5ff', text: '#7e22ce' },
  { bg: '#fff7ed', border: '#fed7aa', text: '#c2410c' },
  { bg: '#ecfeff', border: '#a5f3fc', text: '#0e7490' },
  { bg: '#f0fdf4', border: '#86efac', text: '#15803d' },
  { bg: '#fef2f2', border: '#fecaca', text: '#b91c1c' },
  { bg: '#f5f3ff', border: '#ddd6fe', text: '#6d28d9' },
] as const

function hashAliasPaletteIndex(text: string): number {
  let hash = 0
  for (let i = 0; i < text.length; i++) {
    hash = (hash * 31 + text.charCodeAt(i)) >>> 0
  }
  return hash % ALIAS_TAG_PALETTES.length
}

export function aliasTagStyle(alias: string): Record<string, string> {
  const palette = ALIAS_TAG_PALETTES[hashAliasPaletteIndex(alias.trim() || alias)]
  return {
    background: palette.bg,
    borderColor: palette.border,
    color: palette.text,
  }
}
