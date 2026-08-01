package com.ai.manager.system.service.support.llm;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * LLM 同步聊天的返回结果，包含回答文本和 Token 用量
 */
@Data
@AllArgsConstructor
public class LlmChatResult {

    /** AI 回答文本 */
    private String text;

    /** 本次消耗的总 Token 数（input + output），可能为 null */
    private Long totalTokens;
}
