package com.ai.manager.system.domain.dto;

import lombok.Data;

@Data
public class EcPlatformSaveRequest {

    private String name;

    private String nameEn;

    private Integer platformCode;

    private String channelType;

    private String remark;

    private String status;
}
