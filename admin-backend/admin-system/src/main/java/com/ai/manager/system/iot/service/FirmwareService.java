package com.ai.manager.system.iot.service;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.iot.domain.dto.FirmwareDownloadInfo;
import com.ai.manager.system.iot.domain.entity.IotFirmware;
import com.ai.manager.system.iot.domain.vo.FirmwareVO;
import com.ai.manager.system.iot.domain.vo.OtaRecordVO;

public interface FirmwareService {

    /** 上传固件：保存文件、计算哈希、创建 DRAFT 记录。 */
    FirmwareVO upload(byte[] content, String version, String releaseNote, Integer force);

    /** 发布固件（DRAFT → PUBLISHED）。 */
    FirmwareVO publish(Long id);

    /** 强制升级：置 force=1 并发布，下次 OTA check 强制下发。 */
    FirmwareVO forceUpgrade(Long id);

    /** 删除固件（已发布禁止删除；软删记录并删除文件）。 */
    void delete(Long id);

    /** 分页查询固件，keyword 匹配 version/releaseNote。 */
    PageResult<FirmwareVO> listFirmwares(Long page, Long pageSize, String keyword);

    FirmwareVO getFirmware(Long id);

    /** 最新已发布固件，无则 null。 */
    IotFirmware latestPublished();

    /** 固件下载信息（读文件字节 + 哈希/大小）。 */
    FirmwareDownloadInfo downloadInfo(Long firmwareId);

    /** 分页查询 OTA 升级记录，附设备名与固件版本。 */
    PageResult<OtaRecordVO> listOtaRecords(Long page, Long pageSize);
}
