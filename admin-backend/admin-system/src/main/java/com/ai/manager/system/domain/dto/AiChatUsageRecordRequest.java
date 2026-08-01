package com.ai.manager.system.domain.dto;

import lombok.Data;

/**
 * 记录 AI 对话用量请求
 */
@Data
public class AiChatUsageRecordRequest {
    /** 本次消耗的 Token 数（input + output） */
    private Long tokens;
    /** 本次消费金额（元） */
    private java.math.BigDecimal cost;
}
