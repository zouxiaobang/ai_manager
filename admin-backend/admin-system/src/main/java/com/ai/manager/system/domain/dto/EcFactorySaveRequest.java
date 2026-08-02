package com.ai.manager.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EcFactorySaveRequest {

    /** 工厂名称 */
    @NotBlank(message = "工厂名称不能为空")
    private String name;

    private String factoryType;

    private String contactName;

    private String contactPhone;

    private String address;

    private String remark;

    private String status;
}
