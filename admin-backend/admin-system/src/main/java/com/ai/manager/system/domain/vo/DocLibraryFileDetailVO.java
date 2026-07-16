package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DocLibraryFileDetailVO {

    private Long id;

    private Long folderId;

    private String name;

    private String originalName;

    private String extension;

    private String mimeType;

    private Long fileSize;

    private String storageType;

    private String storagePath;

    private String contentHash;

    private String thumbnailPath;

    private Integer isPinned;

    private String description;

    private Integer sortOrder;

    private String kbStatus;

    private String kbError;

    private LocalDateTime kbProcessedAt;

    private Integer viewCount;

    private Integer downloadCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<DocLibraryTagVO> tags;
}
