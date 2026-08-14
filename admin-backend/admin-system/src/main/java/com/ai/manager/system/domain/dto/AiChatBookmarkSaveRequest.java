package com.ai.manager.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 保存 / 重命名对话标记请求
 */
@Data
public class AiChatBookmarkSaveRequest {

    @NotBlank(message = "标记名称不能为空")
    private String name;

    /** 定位锚点消息 id（记录时视口附近消息，可空） */
    private String msgId;

    @NotNull(message = "锚点偏移不能为空")
    private Integer msgOffsetTop;

    @NotNull(message = "滚动位置不能为空")
    private Integer scrollTop;
}
