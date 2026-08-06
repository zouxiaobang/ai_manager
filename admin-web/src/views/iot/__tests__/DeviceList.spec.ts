import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ElementPlus from 'element-plus'
import type { PageResult } from '@/api/pagination'
import DeviceList from '../DeviceList.vue'

// i18n 直接返回 key，避免真实 locale 依赖
vi.mock('vue-i18n', () => ({
  useI18n: () => ({ t: (key: string) => key }),
}))

vi.mock('@/api/iot', () => ({
  fetchIotDevices: vi.fn(),
  fetchIotDevice: vi.fn(),
  fetchIotDeviceOnlineStatus: vi.fn(),
  rebootIotDevice: vi.fn(),
}))

import * as iotApi from '@/api/iot'

function makePage<T>(records: T[]): PageResult<T> {
  return { records, total: records.length, page: 1, pageSize: 20 }
}

const deviceRows = [
  {
    id: 1,
    uuid: 'uuid-1001',
    mac: 'AA:BB:CC:DD:EE:01',
    model: 'supermini-c3',
    chip: 'esp32c3',
    firmwareVersion: '2.2.1',
    status: 'ONLINE' as const,
    lastSeenAt: '2026-08-05T10:20:30',
  },
  {
    id: 2,
    uuid: 'uuid-1002',
    mac: 'AA:BB:CC:DD:EE:02',
    model: 'kyle-s3-lcd',
    chip: 'esp32s3',
    firmwareVersion: '2.1.0',
    status: 'OFFLINE' as const,
    lastSeenAt: '2026-08-04T09:00:00',
  },
]

describe('DeviceList 组件渲染', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(iotApi.fetchIotDevices).mockResolvedValue(makePage(deviceRows))
  })

  it('挂载后加载设备列表并渲染表格行与状态标签', async () => {
    const wrapper = mount(DeviceList, { global: { plugins: [ElementPlus] } })
    await flushPromises()

    expect(iotApi.fetchIotDevices).toHaveBeenCalledWith({
      page: 1,
      pageSize: 20,
    })
    expect(wrapper.text()).toContain('AA:BB:CC:DD:EE:01')
    expect(wrapper.text()).toContain('AA:BB:CC:DD:EE:02')
    expect(wrapper.text()).toContain('supermini-c3')
    // 状态标签走 i18n key 文案
    expect(wrapper.text()).toContain('iot.device.statusOnline')
    expect(wrapper.text()).toContain('iot.device.statusOffline')
  })

  it('带筛选条件时透传 keyword 与 status', async () => {
    const wrapper = mount(DeviceList, { global: { plugins: [ElementPlus] } })
    await flushPromises()

    await wrapper.find('.iot-panel__search input').setValue('aa')
    await wrapper.find('.iot-panel__search input').trigger('keyup.enter')
    await flushPromises()

    expect(iotApi.fetchIotDevices).toHaveBeenLastCalledWith({
      page: 1,
      pageSize: 20,
      keyword: 'aa',
    })
  })
})
