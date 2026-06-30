package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StorageImageItemVO {

    private String zone;

    private String fileName;

    private String relativePath;

    private long sizeBytes;

    private LocalDateTime modifiedAt;
}
