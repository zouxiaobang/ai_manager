import { describe, expect, it } from 'vitest'
import { functionItems } from '@/data/function-items'
import zhCN from '@/i18n/locales/zh-CN'
import enUS from '@/i18n/locales/en-US'

const LOCALES = [
  { label: 'zh-CN', messages: zhCN },
  { label: 'en-US', messages: enUS },
] as const

describe('functionItems 与 i18n 完整性', () => {
  // 功能列表 / 首页 / 移动端功能页都依赖 t(`functions.items.<key>.name|desc`)，
  // 缺任一 locale 键会显示 key 原文或空白（曾出现 claudeTerminal 未配置）
  it('两个 locale 为每个功能模块提供 name 与 desc', () => {
    for (const { label, messages } of LOCALES) {
      for (const item of functionItems) {
        const entry = messages.functions.items[item.key]
        expect(
          entry?.name?.trim(),
          `functions.items.${item.key}.name 缺失或为空（${label}）`,
        ).toBeTruthy()
        expect(
          entry?.desc?.trim(),
          `functions.items.${item.key}.desc 缺失或为空（${label}）`,
        ).toBeTruthy()
      }
    }
  })
})
