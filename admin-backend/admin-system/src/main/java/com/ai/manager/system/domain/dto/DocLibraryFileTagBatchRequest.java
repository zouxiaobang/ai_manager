package com.ai.manager.system.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class DocLibraryFileTagBatchRequest {

    private List<Long> fileIds;

    private List<Long> tagIds;
}
