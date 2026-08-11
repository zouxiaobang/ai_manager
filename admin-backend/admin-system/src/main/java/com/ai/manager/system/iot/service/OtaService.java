package com.ai.manager.system.iot.service;

import com.ai.manager.system.iot.domain.dto.DeviceActivateRequest;
import com.ai.manager.system.iot.domain.dto.DeviceActivateResult;
import com.ai.manager.system.iot.domain.dto.FirmwareDownloadInfo;
import com.ai.manager.system.iot.domain.dto.OtaCheckRequest;
import com.ai.manager.system.iot.domain.dto.OtaCheckResponse;
import com.ai.manager.system.iot.domain.dto.OtaStatusRequest;

public interface OtaService {

    /** 设备版本检查 + 配置下发（POST /api/iot/ota/check）。 */
    OtaCheckResponse check(OtaCheckRequest request);

    /** 设备激活挑战应答（POST /api/iot/ota/activate）。 */
    DeviceActivateResult activate(DeviceActivateRequest request);

    /** 固件下载信息（GET /api/iot/ota/download/{id}）。 */
    FirmwareDownloadInfo getDownloadInfo(Long firmwareId);

    /** 设备上报 OTA 升级状态/进度（POST /api/iot/ota/status）。 */
    void reportStatus(OtaStatusRequest request);
}
