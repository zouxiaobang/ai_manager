import { getEcommerceImageUrl } from '@/api/ecommerce/image'
import { resolvePlatformIcon } from '@/utils/platformVisual'

export interface ShopIconMeta {
  src: string
  isCustomAvatar: boolean
}

export function resolveShopIconMeta(
  shopName?: string,
  platformName?: string,
  platformCode?: number | null,
  avatarUrl?: string | null,
): ShopIconMeta {
  if (avatarUrl?.trim()) {
    return { src: getEcommerceImageUrl(avatarUrl), isCustomAvatar: true }
  }
  return { src: resolvePlatformIcon(platformName ?? shopName, platformCode), isCustomAvatar: false }
}

export function resolveShopIcon(
  shopName?: string,
  platformName?: string,
  platformCode?: number | null,
  avatarUrl?: string | null,
): string {
  return resolveShopIconMeta(shopName, platformName, platformCode, avatarUrl).src
}
