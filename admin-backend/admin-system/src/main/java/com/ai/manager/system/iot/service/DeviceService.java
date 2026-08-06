package com.ai.manager.system.iot.service;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.iot.domain.dto.DeviceBindRequest;
import com.ai.manager.system.iot.domain.dto.DeviceUpdateRequest;
import com.ai.manager.system.iot.domain.entity.IotDevice;
import com.ai.manager.system.iot.domain.vo.DeviceOnlineStatusVO;
import com.ai.manager.system.iot.domain.vo.DeviceVO;

public interface DeviceService {

    /** 分页查询设备列表，keyword 匹配 mac/uuid/model，status 精确过滤。 */
    PageResult<DeviceVO> listDevices(Long page, Long pageSize, String keyword, String status);

    DeviceVO getDevice(Long id);

    /** 后台绑定/注册设备，签发 uuid 与 ws_token。 */
    DeviceVO bind(DeviceBindRequest request);

    /** 更新设备在线状态。 */
    DeviceVO updateStatus(Long id, String status);

    /** 更新设备信息（model/chip/firmwareVersion，非空才更新）。 */
    DeviceVO update(Long id, DeviceUpdateRequest request);

    /** 探测设备在线状态（WS 会话注册表实时判断）。 */
    DeviceOnlineStatusVO probeOnline(Long id);

    /** 远程 reboot（在线设备下发 system 命令）。 */
    DeviceVO reboot(Long id);

    /** 按 MAC 查询设备（归一化），供 OTA/WS 复用。 */
    IotDevice findByMac(String mac);
}
