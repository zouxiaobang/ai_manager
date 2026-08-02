package com.ai.manager.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EcPlatformSaveRequest {

    /** 平台名称 */
    @NotBlank(message = "平台名称不能为空")
    private String name;

    private String nameEn;

    private String avatarUrl;

    /** 平台编码，全局唯一 */
    @NotNull(message = "平台编码不能为空")
    private Integer platformCode;

    private String channelType;

    private String remark;

    private String status;
}
