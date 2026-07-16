package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocLibraryTrashItemVO {
    private Long id;
    private String name;
    private String extension;
    private Long fileSize;
    private LocalDateTime deletedAt;
    private Long folderId;
    private String folderName;
}
