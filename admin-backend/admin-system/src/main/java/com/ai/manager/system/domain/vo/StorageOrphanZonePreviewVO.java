package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class StorageOrphanZonePreviewVO {

    private String zoneKey;

    private String zoneLabel;

    /** 分区用途说明 */
    private String zonePurpose;

    private boolean supported;

    private String localPath;

    private int scannedCount;

    private int orphanCount;

    private long freedBytes;

    /** 分区配额（字节），0 表示未限制 */
    private long zoneQuotaBytes;

    private List<StorageOrphanFileItemVO> files;
}
