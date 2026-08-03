import { computed, ref, type Ref } from 'vue'
import type { EcListingLink } from '@/api/ecommerce/listingLink'
import type { EcPlatform } from '@/api/ecommerce/platform'
import type { EcProductListItem } from '@/api/ecommerce/product'
import type { EcShop } from '@/api/ecommerce/shop'
import { resolvePlatformIconMeta } from '@/utils/platformVisual'
import { resolveShopIconMeta } from '@/utils/shopVisual'

/** 选项解析依赖：三个候选列表 ref 由组件持有，composable 内派生查找表 */
export interface ListingLinkOptionsDeps {
  shopOptions: Ref<EcShop[]>
  platformOptions: Ref<EcPlatform[]>
  productOptions: Ref<EcProductListItem[]>
}

/**
 * 上架链接表单/卡片的选项解析状态机：
 * 店铺/平台查找表、图标覆盖态与全部标签/图标解析函数。
 * 覆盖态（图标加载失败的兜底）集中管理，避免散落组件。
 */
export function useListingLinkOptions(deps: ListingLinkOptionsDeps) {
  const shopOptionMap = computed(() => new Map(deps.shopOptions.value.map((s) => [s.id, s])))
  const platformOptionMap = computed(() => new Map(deps.platformOptions.value.map((p) => [p.id, p])))
  const shopPlatformIconOverride = ref<Record<number, string>>({})
  const linkCardPlatformIconOverride = ref<Record<number, string>>({})

  /** 店铺选项标签：带平台名时拼接展示 */
  function shopOptionLabel(s: EcShop) {
    return s.platformName ? `${s.name} · ${s.platformName}` : s.name
  }

  /** 店铺选项店铺图标 */
  function shopOptionShopIcon(s: EcShop) {
    return resolveShopIconMeta(s.name, s.platformName, s.platformCode, s.avatarUrl)
  }

  /** 店铺选项平台图标：加载失败兜底优先，否则查平台表解析 */
  function shopOptionPlatformIcon(s: EcShop) {
    if (shopPlatformIconOverride.value[s.id]) {
      return shopPlatformIconOverride.value[s.id]
    }
    const platform = platformOptionMap.value.get(s.platformId)
    return resolvePlatformIconMeta(
      s.platformName ?? platform?.name,
      s.platformCode ?? platform?.platformCode,
      platform?.avatarUrl,
    ).src
  }

  /** 记录店铺平台图标加载失败后的兜底图标 */
  function onShopPlatformIconError(s: EcShop) {
    if (shopPlatformIconOverride.value[s.id]) return
    shopPlatformIconOverride.value = {
      ...shopPlatformIconOverride.value,
      [s.id]: resolvePlatformIconMeta(s.platformName, s.platformCode).src,
    }
  }

  /** 按店铺 ID 从查找表解析店铺 */
  function findShopForLink(row: EcListingLink): EcShop | undefined {
    return shopOptionMap.value.get(row.shopId)
  }

  /** 解析链接对应店铺与平台（行内字段优先，其次查表） */
  function resolveLinkPlatform(row: EcListingLink) {
    const shop = findShopForLink(row)
    const platformId = row.platformId ?? shop?.platformId
    const platform = platformId != null ? platformOptionMap.value.get(platformId) : undefined
    return { shop, platform }
  }

  /** 链接卡片店铺图标：行内名称优先，缺省回退查表 */
  function linkCardShopIcon(row: EcListingLink) {
    const shop = findShopForLink(row)
    return resolveShopIconMeta(
      row.shopName ?? shop?.name,
      row.platformName ?? shop?.platformName,
      shop?.platformCode,
      shop?.avatarUrl,
    )
  }

  /** 链接卡片平台图标：兜底优先，否则解析行内/查表平台 */
  function linkCardPlatformIcon(row: EcListingLink) {
    if (linkCardPlatformIconOverride.value[row.id]) {
      return { src: linkCardPlatformIconOverride.value[row.id], isCustomAvatar: false }
    }
    const { shop, platform } = resolveLinkPlatform(row)
    return resolvePlatformIconMeta(
      row.platformName ?? shop?.platformName ?? platform?.name,
      shop?.platformCode ?? platform?.platformCode,
      platform?.avatarUrl,
    )
  }

  /** 记录链接卡片平台图标加载失败后的兜底图标 */
  function onLinkCardPlatformIconError(row: EcListingLink) {
    if (linkCardPlatformIconOverride.value[row.id]) return
    const { shop } = resolveLinkPlatform(row)
    linkCardPlatformIconOverride.value = {
      ...linkCardPlatformIconOverride.value,
      [row.id]: resolvePlatformIconMeta(row.platformName ?? shop?.platformName, shop?.platformCode).src,
    }
  }

  /** 产品选项标签：带工厂名时拼接展示 */
  function productOptionLabel(p: EcProductListItem) {
    return p.factoryName ? `${p.name} · ${p.factoryName}` : p.name
  }

  /** 产品 ID → 标签文本，查不到回退 ID 本身 */
  function productLabelById(productId: number) {
    const product = deps.productOptions.value.find((p) => p.id === productId)
    return product ? productOptionLabel(product) : String(productId)
  }

  return {
    shopOptionMap,
    platformOptionMap,
    shopPlatformIconOverride,
    linkCardPlatformIconOverride,
    shopOptionLabel,
    shopOptionShopIcon,
    shopOptionPlatformIcon,
    onShopPlatformIconError,
    findShopForLink,
    resolveLinkPlatform,
    linkCardShopIcon,
    linkCardPlatformIcon,
    onLinkCardPlatformIconError,
    productOptionLabel,
    productLabelById,
  }
}
