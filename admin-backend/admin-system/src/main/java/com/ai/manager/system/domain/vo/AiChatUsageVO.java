package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * AI 知识库用量统计
 */
@Data
public class AiChatUsageVO {

    /** 大模型剩余金额（null 表示不可用） */
    private BigDecimal remainingBalance;

    /** 累计消费金额 */
    private BigDecimal totalCost = BigDecimal.ZERO;

    /** 总 API 请求次数 */
    private Integer totalRequests = 0;

    /** 总消耗 Tokens */
    private Long totalTokens = 0L;

    /** 今日 API 请求次数 */
    private Integer todayRequests = 0;

    /** 今日消耗 Tokens */
    private Long todayTokens = 0L;

    /** 最后更新日期（YYYY-MM-DD） */
    private String lastDate;
}
