package com.ai.manager.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EcExpressNoticeSaveRequest {

    /** 公告所属快递站点 */
    @NotNull(message = "站点不能为空")
    private Long stationId;

    /** 公告内容 */
    @NotBlank(message = "通知内容不能为空")
    private String content;

    private Boolean highlightRed;

    private Integer sortOrder;
}
