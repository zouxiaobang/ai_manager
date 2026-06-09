import { deleteData, getData, postData, putData } from '../request'
import type { PageQuery, PageResult } from '../pagination'

export interface EcSku {
  id?: number
  productId?: number
  skuCode: string
  specName?: string
  rebatePct?: number | null
  imageName?: string
  cartonId?: number | null
  cartonName?: string
  salePrice?: number | null
  productLengthCm?: number | null
  productWidthCm?: number | null
  productHeightCm?: number | null
  cartonLengthCm?: number | null
  cartonWidthCm?: number | null
  cartonHeightCm?: number | null
  cartonGrossWeightKg?: number | null
  cartonNetWeightKg?: number | null
  unitsPerCarton: number
  status: string
}

export interface EcProductListItem {
  id: number
  name: string
  imageName?: string
  factoryId?: number | null
  factoryName?: string
  rebatePct: number
  status: string
  skuCount: number
  updateTime?: string
}

export interface EcProductDetail {
  id: number
  factoryId?: number | null
  factoryName?: string
  name: string
  description?: string
  rebatePct: number
  imageName?: string
  status: string
  createTime?: string
  updateTime?: string
  skus: EcSku[]
}

export interface EcProductSaveRequest {
  name: string
  factoryId?: number | null
  description?: string
  rebatePct?: number
  imageName?: string
  status?: string
  skus: EcSku[]
}

export function fetchProducts(keyword?: string, pageQuery?: PageQuery) {
  return getData<PageResult<EcProductListItem>>('/api/ecommerce/products', {
    ...(keyword ? { keyword } : {}),
    ...(pageQuery ?? {}),
  })
}

export function fetchProduct(id: number) {
  return getData<EcProductDetail>(`/api/ecommerce/products/${id}`)
}

export function createProduct(data: EcProductSaveRequest) {
  return postData<EcProductDetail>('/api/ecommerce/products', data)
}

export function updateProduct(id: number, data: EcProductSaveRequest) {
  return putData<EcProductDetail>(`/api/ecommerce/products/${id}`, data)
}

export function deleteProduct(id: number) {
  return deleteData(`/api/ecommerce/products/${id}`)
}

export function emptySku(defaultRebatePct = 0): EcSku {
  return {
    skuCode: '',
    specName: '',
    rebatePct: defaultRebatePct,
    imageName: '',
    cartonId: null,
    cartonName: '',
    salePrice: null,
    productLengthCm: null,
    productWidthCm: null,
    productHeightCm: null,
    cartonLengthCm: null,
    cartonWidthCm: null,
    cartonHeightCm: null,
    cartonGrossWeightKg: null,
    cartonNetWeightKg: null,
    unitsPerCarton: 1,
    status: 'ON_SALE',
  }
}
