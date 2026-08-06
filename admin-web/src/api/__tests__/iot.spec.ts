import { describe, expect, it, vi, beforeEach } from 'vitest'

// mock 请求封装：断言各 IoT API 是否正确转发 URL / 参数 / FormData
vi.mock('@/api/request', () => {
  const request = {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  }
  return {
    default: request,
    getData: vi.fn(),
    postData: vi.fn(),
    putData: vi.fn(),
    deleteData: vi.fn(),
  }
})

import request, { deleteData, getData, postData, putData } from '@/api/request'
import {
  deleteIotFirmware,
  fetchIotDevice,
  fetchIotDeviceOnlineStatus,
  fetchIotDevices,
  fetchIotFirmwares,
  fetchIotOnlineSessions,
  fetchIotOtaRecords,
  forceUpgradeIotFirmware,
  publishIotFirmware,
  rebootIotDevice,
  updateIotDevice,
  uploadIotFirmware,
} from '@/api/iot'

const requestMock = request as unknown as {
  get: ReturnType<typeof vi.fn>
  post: ReturnType<typeof vi.fn>
  put: ReturnType<typeof vi.fn>
  delete: ReturnType<typeof vi.fn>
}

describe('iot api 层', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('设备管理', () => {
    it('fetchIotDevices 透传分页与筛选参数', () => {
      vi.mocked(getData).mockResolvedValue({ records: [], total: 0, page: 1, pageSize: 20 })
      fetchIotDevices({ page: 2, pageSize: 10, keyword: 'aa', status: 'ONLINE' })
      expect(getData).toHaveBeenCalledWith('/api/iot/device', {
        page: 2,
        pageSize: 10,
        keyword: 'aa',
        status: 'ONLINE',
      })
    })

    it('fetchIotDevice 拼接详情路径', () => {
      fetchIotDevice(7)
      expect(getData).toHaveBeenCalledWith('/api/iot/device/7')
    })

    it('fetchIotDeviceOnlineStatus 拼接在线状态路径', () => {
      fetchIotDeviceOnlineStatus(7)
      expect(getData).toHaveBeenCalledWith('/api/iot/device/7/online')
    })

    it('rebootIotDevice 下发重启指令', () => {
      rebootIotDevice(7)
      expect(postData).toHaveBeenCalledWith('/api/iot/device/7/reboot')
    })

    it('updateIotDevice 更新设备备注', () => {
      updateIotDevice(7, { model: 'supermini-c3' })
      expect(putData).toHaveBeenCalledWith('/api/iot/device/7', { model: 'supermini-c3' })
    })
  })

  describe('固件管理', () => {
    it('fetchIotFirmwares 透传分页参数', () => {
      fetchIotFirmwares({ page: 1, pageSize: 20 })
      expect(getData).toHaveBeenCalledWith('/api/iot/firmware', { page: 1, pageSize: 20 })
    })

    it('uploadIotFirmware 以 multipart 上传并解包 data', async () => {
      requestMock.post.mockResolvedValue({ data: { code: 0, data: { id: 1, version: '2.2.1' } } })
      const file = new File(['bin'], 'fw.bin', { type: 'application/octet-stream' })
      const result = await uploadIotFirmware({ file, version: '2.2.1', force: true, releaseNote: 'fix boot' })

      expect(requestMock.post).toHaveBeenCalledTimes(1)
      const [url, body, options] = requestMock.post.mock.calls[0]
      expect(url).toBe('/api/iot/firmware/upload')
      expect(body).toBeInstanceOf(FormData)
      expect((body as FormData).get('version')).toBe('2.2.1')
      expect((body as FormData).get('force')).toBe('true')
      expect((body as FormData).get('releaseNote')).toBe('fix boot')
      expect(options).toMatchObject({
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 60000,
      })
      expect(result).toEqual({ id: 1, version: '2.2.1' })
    })

    it('uploadIotFirmware 非必填字段不追加 form 字段', async () => {
      requestMock.post.mockResolvedValue({ data: { code: 0, data: { id: 1, version: '1.0.0' } } })
      const file = new File(['bin'], 'fw.bin', { type: 'application/octet-stream' })
      await uploadIotFirmware({ file, version: '1.0.0' })
      const body = requestMock.post.mock.calls[0][1] as FormData
      expect(body.get('force')).toBeNull()
      expect(body.get('releaseNote')).toBeNull()
    })

    it('publishIotFirmware 默认非强制发布', () => {
      publishIotFirmware(3)
      expect(postData).toHaveBeenCalledWith('/api/iot/firmware/3/publish', { force: false })
    })

    it('publishIotFirmware 支持强制发布', () => {
      publishIotFirmware(3, true)
      expect(postData).toHaveBeenCalledWith('/api/iot/firmware/3/publish', { force: true })
    })

    it('forceUpgradeIotFirmware 走固件强制升级接口', () => {
      forceUpgradeIotFirmware(3)
      expect(postData).toHaveBeenCalledWith('/api/iot/firmware/3/force')
    })

    it('deleteIotFirmware 删除固件', () => {
      deleteIotFirmware(3)
      expect(deleteData).toHaveBeenCalledWith('/api/iot/firmware/3')
    })
  })

  describe('OTA 记录与在线会话', () => {
    it('fetchIotOtaRecords 拼接 OTA 记录路径', () => {
      fetchIotOtaRecords({ page: 1, pageSize: 20, state: 'SUCCESS' })
      expect(getData).toHaveBeenCalledWith('/api/iot/firmware/ota-records', {
        page: 1,
        pageSize: 20,
        state: 'SUCCESS',
      })
    })

    it('fetchIotOnlineSessions 透传在线会话查询', () => {
      fetchIotOnlineSessions({ page: 1, pageSize: 20, online: true })
      expect(getData).toHaveBeenCalledWith('/api/iot/session', { page: 1, pageSize: 20, online: true })
    })
  })
})
