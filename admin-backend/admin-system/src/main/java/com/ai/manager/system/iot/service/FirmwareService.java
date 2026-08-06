package com.ai.manager.system.iot.service;

import com.ai.manager.system.iot.domain.dto.FirmwareDownloadInfo;
import com.ai.manager.system.iot.domain.entity.IotFirmware;
import com.ai.manager.system.iot.domain.vo.FirmwareVO;
import com.ai.manager.system.iot.domain.vo.OtaRecordVO;

import java.util.List;

public interface FirmwareService {

    /** 上传固件：保存文件、计算哈希、创建 DRAFT 记录。 */
    FirmwareVO upload(byte[] content, String version, String releaseNote, Integer force);

    /** 发布固件（DRAFT → PUBLISHED）。 */
    FirmwareVO publish(Long id);

    List<FirmwareVO> listFirmwares();

    FirmwareVO getFirmware(Long id);

    /** 最新已发布固件，无则 null。 */
    IotFirmware latestPublished();

    /** 固件下载信息（读文件字节 + 哈希/大小）。 */
    FirmwareDownloadInfo downloadInfo(Long firmwareId);

    /** 固件 OTA 升级记录列表。 */
    List<OtaRecordVO> listOtaRecords();
}
