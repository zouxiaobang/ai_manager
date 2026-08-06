package com.ai.manager.system.iot.service;

import com.ai.manager.system.iot.domain.dto.DeviceBindRequest;
import com.ai.manager.system.iot.domain.entity.IotDevice;
import com.ai.manager.system.iot.domain.vo.DeviceVO;

import java.util.List;

public interface DeviceService {

    List<DeviceVO> listDevices();

    DeviceVO getDevice(Long id);

    /** 后台绑定/注册设备，签发 uuid 与 ws_token。 */
    DeviceVO bind(DeviceBindRequest request);

    /** 更新设备在线状态。 */
    DeviceVO updateStatus(Long id, String status);

    /** 远程 reboot（在线设备下发 system 命令）。 */
    DeviceVO reboot(Long id);

    /** 按 MAC 查询设备（归一化），供 OTA/WS 复用。 */
    IotDevice findByMac(String mac);
}
