package com.ai.manager.system.iot.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 设备侧 OTA 升级状态上报请求（POST /api/iot/ota/status）
 * <p>
 * 固件下载中每 10% 报一次 DOWNLOADING + progress，成功报 SUCCESS/100，失败报 FAILED。
 * 字段与设备端 core/ota_report.cc 的 BuildOtaStatusBody 对齐（snake_case）。
 * </p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OtaStatusRequest {

    /** MAC 地址（小写去冒号） */
    private String mac;

    /** 升级状态：DOWNLOADING/SUCCESS/FAILED */
    private String state;

    /** 下载进度 0~100 */
    private Integer progress;
}
