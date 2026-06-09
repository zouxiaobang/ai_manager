package com.ai.manager.system.domain.vo;

import lombok.Data;

@Data
public class SysImportFieldVO {

    private String key;

    private String labelZh;

    private String labelEn;

    private boolean required;
}
