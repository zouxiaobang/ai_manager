import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ElementPlus from 'element-plus'
import type { PageResult } from '@/api/pagination'
import FirmwareList from '../FirmwareList.vue'

// i18n mock：t 返回 key（避免真实 locale 依赖）；tm 返回假步骤数组（组件用 tm() 取复合消息）
vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
    tm: (key: string) => (key === 'iot.firmware.upgradeTipSteps' ? ['步骤一', '步骤二', '步骤三'] : undefined),
  }),
}))

vi.mock('@/api/iot', () => ({
  fetchIotFirmwares: vi.fn(),
  fetchIotOtaRecords: vi.fn(),
  publishIotFirmware: vi.fn(),
  forceUpgradeIotFirmware: vi.fn(),
  deleteIotFirmware: vi.fn(),
  uploadIotFirmware: vi.fn(),
}))

import * as iotApi from '@/api/iot'

function makePage<T>(records: T[]): PageResult<T> {
  return { records, total: records.length, page: 1, pageSize: 20 }
}

const firmwareRows = [
  { id: 1, version: '2.2.6', size: 1048576, status: 'PUBLISHED', force: false, releaseNote: 'K5 网络', createTime: '2026-08-11T10:00:00' },
  { id: 2, version: '2.2.5', size: 1024000, status: 'DRAFT', force: true, releaseNote: '旧版', createTime: '2026-08-10T09:00:00' },
]

describe('FirmwareList 组件渲染', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(iotApi.fetchIotFirmwares).mockResolvedValue(makePage(firmwareRows))
    vi.mocked(iotApi.fetchIotOtaRecords).mockResolvedValue(makePage([]))
  })

  it('挂载后加载固件列表，并渲染升级步骤 tip 图标', async () => {
    const wrapper = mount(FirmwareList, { global: { plugins: [ElementPlus] } })
    await flushPromises()

    expect(iotApi.fetchIotFirmwares).toHaveBeenCalledWith({ page: 1, pageSize: 20 })
    expect(wrapper.text()).toContain('2.2.6')
    // 升级步骤 tip 图标渲染在搜索框右侧
    const tipIcon = wrapper.find('.iot-firmware__upgrade-tip-icon')
    expect(tipIcon.exists()).toBe(true)
    // aria-label 携带 tip 标题 key（i18n mock 下即 key 本身）
    expect(tipIcon.attributes('aria-label')).toBe('iot.firmware.upgradeTipTitle')
  })

  it('悬停 tip 图标后展开固件升级步骤列表', async () => {
    const wrapper = mount(FirmwareList, { global: { plugins: [ElementPlus] } })
    await flushPromises()

    await wrapper.find('.iot-firmware__upgrade-tip-icon').trigger('mouseenter')
    // el-tooltip 有 show-after=150ms 延迟，等待打开后 tooltip 内容挂载到 body
    await new Promise((resolve) => setTimeout(resolve, 200))
    await flushPromises()
    // 标题用 t()（返回 key），步骤用 tm()（返回 mock 数组，逐条渲染）
    const popperText = document.body.textContent ?? ''
    expect(popperText).toContain('iot.firmware.upgradeTipTitle')
    expect(popperText).toContain('步骤一')
    expect(popperText).toContain('步骤三')
  })

  it('搜索后透传 keyword 刷新固件列表', async () => {
    const wrapper = mount(FirmwareList, { global: { plugins: [ElementPlus] } })
    await flushPromises()

    await wrapper.find('.iot-panel__search input').setValue('2.2')
    await wrapper.find('.iot-panel__search input').trigger('keyup.enter')
    await flushPromises()

    expect(iotApi.fetchIotFirmwares).toHaveBeenLastCalledWith({
      page: 1,
      pageSize: 20,
      keyword: '2.2',
    })
  })
})
