package com.ai.manager.system.domain.dto;

import lombok.Data;

@Data
public class EcFactorySaveRequest {

    private String name;

    private String factoryType;

    private String contactName;

    private String contactPhone;

    private String address;

    private String remark;

    private String status;
}
