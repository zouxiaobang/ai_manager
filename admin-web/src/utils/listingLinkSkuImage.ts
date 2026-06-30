export function resolveListingLinkSkuImageName(
  skuCodes: string[],
  productIds: number[],
  skuImageMap: Record<string, string | undefined>,
  productImageMap: Record<number, string | undefined>,
): string | undefined {
  for (const code of skuCodes) {
    const imageName = skuImageMap[code]?.trim()
    if (imageName) return imageName
  }
  for (const productId of productIds) {
    const imageName = productImageMap[productId]?.trim()
    if (imageName) return imageName
  }
  return undefined
}
