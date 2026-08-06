import { describe, expect, it } from 'vitest'
import {
  formatBytes,
  formatIotTime,
  resolveDeviceName,
  resolveDeviceStatusMeta,
  resolveOtaStateMeta,
  resolveSessionOnline,
} from '../iotFormat'

describe('iotFormat 展示纯函数', () => {
  describe('resolveDeviceStatusMeta', () => {
    it('在线映射 success 标签', () => {
      expect(resolveDeviceStatusMeta('ONLINE')).toEqual({ label: 'iot.device.statusOnline', tagType: 'success' })
    })

    it('未激活映射 warning 标签', () => {
      expect(resolveDeviceStatusMeta('INACTIVE')).toEqual({ label: 'iot.device.statusInactive', tagType: 'warning' })
    })

    it('离线与未知状态兜底离线', () => {
      expect(resolveDeviceStatusMeta('OFFLINE')).toEqual({ label: 'iot.device.statusOffline', tagType: 'info' })
      expect(resolveDeviceStatusMeta(undefined)).toEqual({ label: 'iot.device.statusOffline', tagType: 'info' })
    })
  })

  describe('resolveOtaStateMeta', () => {
    it('成功映射 success', () => {
      expect(resolveOtaStateMeta('SUCCESS')).toEqual({ label: 'iot.ota.stateSuccess', tagType: 'success' })
    })

    it('失败映射 danger', () => {
      expect(resolveOtaStateMeta('FAILED')).toEqual({ label: 'iot.ota.stateFailed', tagType: 'danger' })
    })

    it('升级中映射 warning', () => {
      expect(resolveOtaStateMeta('UPGRADING')).toEqual({ label: 'iot.ota.stateUpgrading', tagType: 'warning' })
    })

    it('待升级与未知兜底 info', () => {
      expect(resolveOtaStateMeta('PENDING')).toEqual({ label: 'iot.ota.statePending', tagType: 'info' })
      expect(resolveOtaStateMeta(undefined)).toEqual({ label: 'iot.ota.statePending', tagType: 'info' })
    })
  })

  describe('formatIotTime', () => {
    it('空值返回占位符', () => {
      expect(formatIotTime('')).toBe('—')
      expect(formatIotTime(undefined)).toBe('—')
      expect(formatIotTime(null)).toBe('—')
    })

    it('ISO 时间 T 转空格并截断到秒', () => {
      expect(formatIotTime('2026-08-05T10:20:30.123Z')).toBe('2026-08-05 10:20:30')
    })

    it('非法字符串返回占位符', () => {
      expect(formatIotTime('garbage')).toBe('—')
      expect(formatIotTime('2026-08-05')).toBe('—')
    })
  })

  describe('formatBytes', () => {
    it('字节原样输出', () => {
      expect(formatBytes(512)).toBe('512 B')
    })

    it('KB 保留一位小数', () => {
      expect(formatBytes(2048)).toBe('2.0 KB')
    })

    it('MB / GB 换算', () => {
      expect(formatBytes(5 * 1024 * 1024)).toBe('5.0 MB')
      expect(formatBytes(3 * 1024 * 1024 * 1024)).toBe('3.0 GB')
    })

    it('空值与非法值返回占位符', () => {
      expect(formatBytes(undefined)).toBe('—')
      expect(formatBytes(null)).toBe('—')
      expect(formatBytes(-1)).toBe('—')
      expect(formatBytes(NaN)).toBe('—')
    })
  })

  describe('resolveSessionOnline', () => {
    it('显式 online 优先', () => {
      expect(resolveSessionOnline({ online: true, endedAt: 'x' })).toBe(true)
      expect(resolveSessionOnline({ online: false, sessionId: 's' })).toBe(false)
    })

    it('无显式值时按 sessionId 存在且未结束推断', () => {
      expect(resolveSessionOnline({ sessionId: 's' })).toBe(true)
      expect(resolveSessionOnline({ sessionId: 's', endedAt: '2026-08-05 10:00:00' })).toBe(false)
      expect(resolveSessionOnline({})).toBe(false)
    })
  })

  describe('resolveDeviceName', () => {
    const t = (key: string) => key

    it('deviceName 优先', () => {
      expect(resolveDeviceName({ deviceName: '客厅副屏', mac: 'AA', uuid: 'UU', id: 1 }, t)).toBe('客厅副屏')
    })

    it('无名称回退 MAC', () => {
      expect(resolveDeviceName({ mac: 'AA:BB', uuid: 'UU', id: 1 }, t)).toBe('AA:BB')
    })

    it('无名称与 MAC 回退 UUID', () => {
      expect(resolveDeviceName({ uuid: 'UU', id: 1 }, t)).toBe('UU')
    })

    it('全部缺失回退占位符 + id', () => {
      expect(resolveDeviceName({ id: 3 }, t)).toBe('iot.device.unnamed#3')
    })
  })
})
