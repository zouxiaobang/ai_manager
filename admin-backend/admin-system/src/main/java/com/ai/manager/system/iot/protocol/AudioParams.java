package com.ai.manager.system.iot.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * server hello 下发的音频参数（纯逻辑 POJO）。
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class AudioParams {

    /** 采样率 Hz */
    private int sampleRate = 16000;

    /** 声道数 */
    private int channels = 1;

    /** 位深 */
    private int bitsPerSample = 16;
}
