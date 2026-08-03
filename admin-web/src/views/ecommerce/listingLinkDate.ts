import { todayDateString } from '@/utils/date'

/** 上架时间默认值：今天（新链接默认排期当天上架） */
export function defaultListingDate() {
  return todayDateString()
}

/** 归一化上架日期：空值回退今天，否则取日期部分（前 10 位，去掉时间后缀） */
export function toListingDate(value?: string | null) {
  if (!value) return defaultListingDate()
  return value.trim().slice(0, 10)
}

/** 归一化上架时间戳：空串原样返回，仅日期时补齐零时分秒，其余按原样提交 */
export function toListingDateTime(date: string) {
  if (!date) return date
  if (date.length <= 10) return `${date} 00:00:00`
  return date
}
