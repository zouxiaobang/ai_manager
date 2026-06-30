package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StorageOrphanFileItemVO {

    private String fileName;

    private String relativePath;

    private long sizeBytes;

    /** 文件最后修改时间，作为孤立时间参考 */
    private LocalDateTime orphanedAt;
}
