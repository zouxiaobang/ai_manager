import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ElementPlus from 'element-plus'
import type { PageResult } from '@/api/pagination'
import OnlineSessions from '../OnlineSessions.vue'

vi.mock('vue-i18n', () => ({
  useI18n: () => ({ t: (key: string) => key }),
}))

vi.mock('@/api/iot', () => ({
  fetchIotOnlineSessions: vi.fn(),
}))

import * as iotApi from '@/api/iot'

function makePage<T>(records: T[]): PageResult<T> {
  return { records, total: records.length, page: 1, pageSize: 20 }
}

const sessions = [
  {
    id: 10,
    deviceName: '客厅副屏',
    sessionId: 'sess-abc',
    startedAt: '2026-08-05T10:00:00',
    endedAt: undefined,
    turnCount: 4,
    online: true,
  },
  {
    id: 11,
    deviceName: '书房副屏',
    sessionId: 'sess-def',
    startedAt: '2026-08-05T09:00:00',
    endedAt: '2026-08-05T09:30:00',
    turnCount: 12,
    online: false,
  },
]

describe('OnlineSessions 组件渲染', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(iotApi.fetchIotOnlineSessions).mockResolvedValue(makePage(sessions))
  })

  it('挂载后加载在线会话并渲染行与会话轮次', async () => {
    const wrapper = mount(OnlineSessions, { global: { plugins: [ElementPlus] } })
    await flushPromises()

    expect(iotApi.fetchIotOnlineSessions).toHaveBeenCalledWith({ page: 1, pageSize: 20 })
    expect(wrapper.text()).toContain('客厅副屏')
    expect(wrapper.text()).toContain('书房副屏')
    expect(wrapper.text()).toContain('sess-abc')
    expect(wrapper.text()).toContain('iot.session.stateOnline')
    expect(wrapper.text()).toContain('iot.session.stateEnded')
  })
})
