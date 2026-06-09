const PROVINCE_NAMES = [
  '内蒙古自治区', '广西壮族自治区', '西藏自治区', '宁夏回族自治区', '新疆维吾尔自治区',
  '黑龙江省', '吉林省', '辽宁省', '河北省', '山西省', '江苏省', '浙江省', '安徽省', '福建省',
  '江西省', '山东省', '河南省', '湖北省', '湖南省', '广东省', '海南省', '四川省', '贵州省',
  '云南省', '陕西省', '甘肃省', '青海省', '台湾省',
  '北京市', '天津市', '上海市', '重庆市',
  '香港特别行政区', '澳门特别行政区',
].sort((a, b) => b.length - a.length)

export function parseProvinceFromAddress(address?: string | null): string | null {
  const text = address?.trim()
  if (!text) return null
  for (const name of PROVINCE_NAMES) {
    if (text.startsWith(name)) return name
  }
  const provinceIdx = text.indexOf('省')
  if (provinceIdx > 0 && provinceIdx <= 10) {
    return text.slice(0, provinceIdx + 1)
  }
  const autonomousIdx = text.indexOf('自治区')
  if (autonomousIdx > 0 && autonomousIdx <= 12) {
    return text.slice(0, autonomousIdx + 3)
  }
  return null
}
