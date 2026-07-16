package com.ai.manager.system.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class DocLibraryFileBatchMoveRequest {

    private List<Long> ids;

    private Long folderId;
}
