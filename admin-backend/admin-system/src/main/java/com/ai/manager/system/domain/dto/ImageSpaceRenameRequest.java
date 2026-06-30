package com.ai.manager.system.domain.dto;

import lombok.Data;

@Data
public class ImageSpaceRenameRequest {

    private String zone;

    private String relativePath;

    private String newFileName;
}
