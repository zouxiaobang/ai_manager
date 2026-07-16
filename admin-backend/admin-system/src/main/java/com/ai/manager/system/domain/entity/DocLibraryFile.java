package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("doc_library_file")
public class DocLibraryFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long folderId;

    private String name;

    private String originalName;

    private String extension;

    private String mimeType;

    private Long fileSize;

    private String storageType;

    private String storagePath;

    private String storageKey;

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

    private LocalDateTime deletedAt;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
