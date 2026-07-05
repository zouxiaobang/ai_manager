const FACTORY_BASE = `${import.meta.env.BASE_URL}mobile-factory`
const POSTER_VER = '16'

export const factoryAssets = {
  /** 干净原图（仅纸纹变白，数据区由 CSS 遮罩覆盖） */
  posterHeader: `${FACTORY_BASE}/factory-poster-header.png?v=${POSTER_VER}`,
} as const
