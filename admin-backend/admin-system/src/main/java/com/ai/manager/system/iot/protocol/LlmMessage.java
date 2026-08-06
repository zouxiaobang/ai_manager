package com.ai.manager.system.iot.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 后端下发 llm 消息（LLM 回答 + 情绪/表情映射）。
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class LlmMessage {

    private String type = "llm";

    private String sessionId;

    /** 情绪/表情标识，如 happy / neutral / thinking */
    private String emotion;

    private String text;
}
