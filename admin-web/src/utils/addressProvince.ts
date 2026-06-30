const PROVINCE_NAMES = [
  '内蒙古自治区', '广西壮族自治区', '西藏自治区', '宁夏回族自治区', '新疆维吾尔自治区',
  '黑龙江省', '吉林省', '辽宁省', '河北省', '山西省', '江苏省', '浙江省', '安徽省', '福建省',
  '江西省', '山东省', '河南省', '湖北省', '湖南省', '广东省', '海南省', '四川省', '贵州省',
  '云南省', '陕西省', '甘肃省', '青海省', '台湾省',
  '北京市', '天津市', '上海市', '重庆市',
  '香港特别行政区', '澳门特别行政区',
]

const PROVINCE_NAMES_BY_LENGTH = [...PROVINCE_NAMES].sort((a, b) => b.length - a.length)

const PROVINCE_NAME_SET = new Set(PROVINCE_NAMES)

const PROVINCE_ALIASES: Record<string, string> = {
  内蒙: '内蒙古自治区',
  内蒙古: '内蒙古自治区',
  广西: '广西壮族自治区',
  广西壮族: '广西壮族自治区',
  宁夏: '宁夏回族自治区',
  宁夏回族: '宁夏回族自治区',
  新疆: '新疆维吾尔自治区',
  新疆维吾尔: '新疆维吾尔自治区',
  西藏: '西藏自治区',
  香港: '香港特别行政区',
  澳门: '澳门特别行政区',
  台: '台湾省',
  台湾: '台湾省',
}

function sanitizeProvinceInput(raw: string): string {
  return raw.trim().replace(/　/g, ' ').replace(/\s+/g, '')
}

function stripProvinceSuffix(provinceName: string): string {
  if (provinceName.endsWith('特别行政区')) {
    return provinceName.slice(0, -5)
  }
  if (provinceName.endsWith('自治区')) {
    const core = provinceName.slice(0, provinceName.indexOf('自治区'))
    const ethnicMarkers = ['壮族', '回族', '维吾尔']
    for (const marker of ethnicMarkers) {
      const idx = core.indexOf(marker)
      if (idx > 0) return core.slice(0, idx)
    }
    return core
  }
  if (provinceName.endsWith('省') || provinceName.endsWith('市')) {
    return provinceName.slice(0, -1)
  }
  return provinceName
}

function matchByAppendingSuffix(text: string): string | null {
  if (text.endsWith('省') || text.endsWith('市') || text.includes('自治区') || text.includes('特别行政区')) {
    return null
  }
  for (const suffix of ['省', '市', '自治区', '特别行政区']) {
    const candidate = `${text}${suffix}`
    if (PROVINCE_NAME_SET.has(candidate)) return candidate
  }
  return null
}

function matchByCoreName(text: string): string | null {
  const matches = PROVINCE_NAMES.filter((name) => stripProvinceSuffix(name) === text)
  return matches.length === 1 ? matches[0] : null
}

function matchUniquePrefix(text: string): string | null {
  if (text.length < 2) return null
  const matches = PROVINCE_NAMES.filter((name) => name.startsWith(text))
  return matches.length === 1 ? matches[0] : null
}

/** 规范化为快递价格表使用的省份全称，例如：广东→广东省、北京→北京市。 */
export function normalizeProvinceName(raw?: string | null): string | null {
  if (!raw?.trim()) return null
  const text = sanitizeProvinceInput(raw)
  if (PROVINCE_NAME_SET.has(text)) return text
  if (PROVINCE_ALIASES[text]) return PROVINCE_ALIASES[text]
  const withSuffix = matchByAppendingSuffix(text)
  if (withSuffix) return withSuffix
  const byCoreName = matchByCoreName(text)
  if (byCoreName) return byCoreName
  return matchUniquePrefix(text)
}

export function parseProvinceFromAddress(address?: string | null): string | null {
  const text = address?.trim()
  if (!text) return null
  const sanitized = sanitizeProvinceInput(text)
  for (const name of PROVINCE_NAMES_BY_LENGTH) {
    if (sanitized.startsWith(name)) return name
  }
  const provinceIdx = sanitized.indexOf('省')
  if (provinceIdx > 0 && provinceIdx <= 10) {
    const candidate = sanitized.slice(0, provinceIdx + 1)
    if (PROVINCE_NAME_SET.has(candidate)) return candidate
  }
  const autonomousIdx = sanitized.indexOf('自治区')
  if (autonomousIdx > 0 && autonomousIdx <= 12) {
    const candidate = sanitized.slice(0, autonomousIdx + 3)
    if (PROVINCE_NAME_SET.has(candidate)) return candidate
  }
  const sarIdx = sanitized.indexOf('特别行政区')
  if (sarIdx > 0 && sarIdx <= 8) {
    const candidate = sanitized.slice(0, sarIdx + 5)
    if (PROVINCE_NAME_SET.has(candidate)) return candidate
  }
  return normalizeProvinceName(sanitized)
}
