package com.ai.manager.system.iot.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 固件下载信息：文件字节 + 校验元数据
 */
@Data
@AllArgsConstructor
public class FirmwareDownloadInfo {

    private String version;

    private String fileHash;

    private long size;

    private byte[] content;
}
