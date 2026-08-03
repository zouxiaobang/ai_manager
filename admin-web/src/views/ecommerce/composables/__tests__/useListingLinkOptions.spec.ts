import { ref } from 'vue'
import { describe, expect, it, vi } from 'vitest'
import type { EcListingLink } from '@/api/ecommerce/listingLink'
import type { EcPlatform } from '@/api/ecommerce/platform'
import type { EcProductListItem } from '@/api/ecommerce/product'
import type { EcShop } from '@/api/ecommerce/shop'
import { useListingLinkOptions } from '../useListingLinkOptions'

vi.mock('@/utils/shopVisual', () => ({
  resolveShopIconMeta: (name?: string, platformName?: string, platformCode?: string, avatarUrl?: string | null) => ({
    src: avatarUrl ? `avatar:${avatarUrl}` : `shop:${name ?? platformName ?? platformCode ?? '?'}`,
    isCustomAvatar: !!avatarUrl,
  }),
}))
vi.mock('@/utils/platformVisual', () => ({
  resolvePlatformIconMeta: (name?: string, code?: string, avatarUrl?: string | null) => ({
    src: avatarUrl ? `avatar:${avatarUrl}` : `platform:${name ?? code ?? '?'}`,
    isCustomAvatar: !!avatarUrl,
  }),
}))

function makeShop(partial?: Partial<EcShop>): EcShop {
  return { id: 1, name: '店铺甲', platformId: 9, status: 'ENABLED', ...partial }
}

function makePlatform(partial?: Partial<EcPlatform>): EcPlatform {
  return { id: 9, name: '平台甲', platformCode: 1, channelType: 'ONLINE', status: 'ENABLED', ...partial }
}

function makeLink(partial?: Partial<EcListingLink>): EcListingLink {
  return { id: 1, shopId: 1, name: '链接甲', status: 'ENABLED', ...partial }
}

function makeProduct(partial?: Partial<EcProductListItem>): EcProductListItem {
  return { id: 5, name: '产品甲', rebatePct: 0, status: 'ENABLED', skuCount: 0, ...partial }
}

function setup(
  shopList: EcShop[] = [],
  platformList: EcPlatform[] = [],
  productList: EcProductListItem[] = [],
) {
  return useListingLinkOptions({
    shopOptions: ref(shopList),
    platformOptions: ref(platformList),
    productOptions: ref(productList),
  })
}

describe('useListingLinkOptions 选项解析域', () => {
  describe('shopOptionLabel', () => {
    it('有平台名时拼接展示', () => {
      const api = setup()
      expect(api.shopOptionLabel(makeShop({ platformName: '淘宝' }))).toBe('店铺甲 · 淘宝')
      expect(api.shopOptionLabel(makeShop({ platformName: undefined }))).toBe('店铺甲')
    })
  })

  describe('shopOptionShopIcon', () => {
    it('委托图标解析器返回元信息', () => {
      const api = setup()
      expect(api.shopOptionShopIcon(makeShop())).toEqual({ src: 'shop:店铺甲', isCustomAvatar: false })
      expect(api.shopOptionShopIcon(makeShop({ avatarUrl: 'a.png' }))).toEqual({
        src: 'avatar:a.png',
        isCustomAvatar: true,
      })
    })
  })

  describe('shopOptionPlatformIcon', () => {
    it('图标加载失败后的兜底优先', () => {
      const api = setup()
      api.shopPlatformIconOverride.value = { 1: 'override.svg' }
      expect(api.shopOptionPlatformIcon(makeShop())).toBe('override.svg')
    })

    it('查平台表解析平台图标', () => {
      const api = setup([makeShop()], [makePlatform()])
      expect(api.shopOptionPlatformIcon(makeShop())).toBe('platform:平台甲')
    })

    it('店铺自带平台信息优先于查表', () => {
      const api = setup([], [makePlatform({ id: 1, name: '拼多多', platformCode: 2 })])
      expect(api.shopOptionPlatformIcon(makeShop({ platformId: 1, platformName: '淘宝', platformCode: 1 }))).toBe(
        'platform:淘宝',
      )
    })
  })

  describe('onShopPlatformIconError', () => {
    it('记录兜底图标且不重复覆盖', () => {
      const api = setup()
      api.onShopPlatformIconError(makeShop({ platformName: '淘宝', platformCode: 1 }))
      expect(api.shopPlatformIconOverride.value[1]).toBe('platform:淘宝')
      api.onShopPlatformIconError(makeShop())
      expect(api.shopPlatformIconOverride.value[1]).toBe('platform:淘宝')
    })
  })

  describe('findShopForLink / resolveLinkPlatform', () => {
    it('按店铺 ID 查表', () => {
      const shop = makeShop()
      const api = setup([shop])
      expect(api.findShopForLink(makeLink())).toEqual(shop)
      expect(api.findShopForLink(makeLink({ shopId: 99 }))).toBeUndefined()
    })

    it('解析店铺与平台，行内 platformId 优先', () => {
      const api = setup([makeShop()], [makePlatform({ id: 9 })])
      expect(api.resolveLinkPlatform(makeLink({ platformId: 9 }))).toEqual({
        shop: expect.objectContaining({ id: 1 }),
        platform: expect.objectContaining({ id: 9 }),
      })
      // 无行内 platformId 时回退店铺的 platformId
      const api2 = setup([makeShop({ platformId: 9 })], [makePlatform({ id: 9 })])
      expect(api2.resolveLinkPlatform(makeLink())).toEqual({
        shop: expect.objectContaining({ id: 1 }),
        platform: expect.objectContaining({ id: 9 }),
      })
    })
  })

  describe('linkCardShopIcon', () => {
    it('行内店铺名优先', () => {
      const api = setup([makeShop({ name: '店铺表' })])
      expect(api.linkCardShopIcon(makeLink({ shopName: '行内店' }))).toEqual({ src: 'shop:行内店', isCustomAvatar: false })
      expect(api.linkCardShopIcon(makeLink())).toEqual({ src: 'shop:店铺表', isCustomAvatar: false })
    })
  })

  describe('linkCardPlatformIcon', () => {
    it('兜底优先', () => {
      const api = setup()
      api.linkCardPlatformIconOverride.value = { 1: 'override.svg' }
      expect(api.linkCardPlatformIcon(makeLink())).toEqual({ src: 'override.svg', isCustomAvatar: false })
    })

    it('行内/店铺/查表逐级回退', () => {
      const api = setup([makeShop({ platformId: 9, platformCode: 1 })], [makePlatform({ id: 9 })])
      expect(api.linkCardPlatformIcon(makeLink({ platformName: '淘宝' }))).toEqual({
        src: 'platform:淘宝',
        isCustomAvatar: false,
      })
      expect(api.linkCardPlatformIcon(makeLink())).toEqual({ src: 'platform:平台甲', isCustomAvatar: false })
    })
  })

  describe('onLinkCardPlatformIconError', () => {
    it('记录链接卡兜底图标', () => {
      const api = setup()
      api.onLinkCardPlatformIconError(makeLink({ platformName: '淘宝', shopName: '店' }))
      expect(api.linkCardPlatformIconOverride.value[1]).toBe('platform:淘宝')
    })
  })

  describe('productOptionLabel / productLabelById', () => {
    it('带工厂名拼接，查表回退 ID', () => {
      const api = setup([], [], [makeProduct({ factoryName: '工厂甲' })])
      expect(api.productOptionLabel(makeProduct({ factoryName: '工厂甲' }))).toBe('产品甲 · 工厂甲')
      expect(api.productLabelById(5)).toBe('产品甲 · 工厂甲')
      expect(api.productLabelById(99)).toBe('99')
    })
  })
})
