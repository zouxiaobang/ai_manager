package com.ai.manager.system.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/**
 * 对话搜索结果
 */
@Data
public class AiChatSearchResultVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long categoryId;
    private String categoryName;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long conversationId;
    private String conversationTitle;
    /** 匹配字段类型：categoryName / conversationTitle / messageContent */
    private String matchField;
    /** 匹配内容摘要 */
    private String matchSummary;
}
