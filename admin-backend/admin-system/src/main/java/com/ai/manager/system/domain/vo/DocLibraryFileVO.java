package com.ai.manager.system.domain.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class DocLibraryFileVO {

    private Long id;

    private Long folderId;

    private String name;

    private String originalName;

    private String extension;

    private String mimeType;

    private Long fileSize;

    private String thumbnailPath;

    private Integer isPinned;

    private String description;

    private String kbStatus;

    private Integer viewCount;

    private Integer downloadCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<DocLibraryTagVO> tags;
}
