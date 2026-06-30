/** 与后端 EcCartonMatcher 一致：尺寸升序后可旋转放入 */
const PRODUCT_AXIS_PERMUTATIONS: ReadonlyArray<readonly [number, number, number]> = [
  [0, 1, 2],
  [0, 2, 1],
  [1, 0, 2],
  [1, 2, 0],
  [2, 0, 1],
  [2, 1, 0],
]

function sortedDims(length: number, width: number, height: number): [number, number, number] | null {
  if (length <= 0 || width <= 0 || height <= 0) return null
  return [length, width, height].sort((a, b) => a - b) as [number, number, number]
}

function fitsSorted(product: [number, number, number], carton: [number, number, number]) {
  return product[0] <= carton[0] && product[1] <= carton[1] && product[2] <= carton[2]
}

export interface CartonFitSlack {
  slackL: number
  slackW: number
  slackH: number
  fits: boolean
  fitsWell: boolean
}

/**
 * 按匹配算法计算各边可用空间：
 * - 需求尺寸 = 产品长宽高 + 对应预留（与 calculate 接口一致）
 * - 允许旋转摆放，与 EcCartonMatcher 相同
 */
export function computeCartonFitSlack(
  reqLength: number,
  reqWidth: number,
  reqHeight: number,
  cartonLength: number,
  cartonWidth: number,
  cartonHeight: number,
): CartonFitSlack | null {
  const required = [reqLength, reqWidth, reqHeight]
  const carton = [cartonLength, cartonWidth, cartonHeight]
  const reqSorted = sortedDims(reqLength, reqWidth, reqHeight)
  const cartSorted = sortedDims(cartonLength, cartonWidth, cartonHeight)
  if (!reqSorted || !cartSorted) return null

  const fits = fitsSorted(reqSorted, cartSorted)

  for (const [pi, pj, pk] of PRODUCT_AXIS_PERMUTATIONS) {
    const slackByProduct = [0, 0, 0]
    const pairs: ReadonlyArray<readonly [number, number]> = [
      [pi, 0],
      [pj, 1],
      [pk, 2],
    ]
    let rotationFits = true
    for (const [prodIdx, cartIdx] of pairs) {
      const slack = carton[cartIdx] - required[prodIdx]
      if (slack < -1e-9) {
        rotationFits = false
        break
      }
      slackByProduct[prodIdx] = slack
    }
    if (rotationFits) {
      return {
        slackL: slackByProduct[0],
        slackW: slackByProduct[1],
        slackH: slackByProduct[2],
        fits,
        fitsWell: fits && slackByProduct.every((s) => s >= -1e-9),
      }
    }
  }

  const sortedSlack: [number, number, number] = [
    cartSorted[0] - reqSorted[0],
    cartSorted[1] - reqSorted[1],
    cartSorted[2] - reqSorted[2],
  ]
  const reqOrder = required
    .map((value, index) => ({ value, index }))
    .sort((a, b) => a.value - b.value)
  const slackByProduct = [0, 0, 0]
  reqOrder.forEach((item, sortedIndex) => {
    slackByProduct[item.index] = sortedSlack[sortedIndex]
  })

  return {
    slackL: slackByProduct[0],
    slackW: slackByProduct[1],
    slackH: slackByProduct[2],
    fits,
    fitsWell: false,
  }
}
