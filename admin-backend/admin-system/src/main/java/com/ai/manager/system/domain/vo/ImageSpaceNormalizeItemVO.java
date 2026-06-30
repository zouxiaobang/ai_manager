package com.ai.manager.system.domain.vo;

import lombok.Data;

@Data
public class ImageSpaceNormalizeItemVO {

    private String oldName;

    private String newName;

    /** SKU / SPU_MAIN */
    private String source;

    /** PLANNED / RENAMED / SKIPPED / FAILED */
    private String status;

    private String message;
}
