package com.ai.manager.system.iot.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 设备上行 abort 消息（打断，如唤醒词打断 TTS）。
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AbortMessage {

    private String type;

    /** 打断原因，如 wake_word_detected / user_interrupt */
    private String reason;

    private String sessionId;
}
