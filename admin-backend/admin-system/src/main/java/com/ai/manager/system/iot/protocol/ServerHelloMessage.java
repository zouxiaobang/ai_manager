package com.ai.manager.system.iot.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.UUID;

/**
 * 后端应答 server hello（下发 session_id + audio_params）。
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServerHelloMessage {

    private String type = "server_hello";

    private String sessionId;

    private AudioParams audioParams;

    public static ServerHelloMessage of(int sampleRate, int channels, int bitsPerSample) {
        ServerHelloMessage msg = new ServerHelloMessage();
        msg.setSessionId(UUID.randomUUID().toString().replace("-", ""));
        msg.setAudioParams(new AudioParams(sampleRate, channels, bitsPerSample));
        return msg;
    }
}
