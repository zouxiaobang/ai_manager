package com.ai.manager.system.iot.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

/**
 * 文本消息 JSON 组装 / 解析（纯逻辑，可单测）。
 * <p>
 * 使用独立 ObjectMapper，不依赖 Spring 容器注入；协议字段统一 snake_case。
 * </p>
 */
public final class WireMessages {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private WireMessages() {
    }

    /** 序列化消息对象为 JSON 字符串。 */
    public static String toJson(Object message) {
        try {
            return MAPPER.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("serialize wire message failed", e);
        }
    }

    /** 读取 JSON 顶层 type 字段（用于分发，未知字段容错）。 */
    public static String parseType(String json) {
        try {
            com.fasterxml.jackson.databind.JsonNode node = MAPPER.readTree(json);
            return node.path("type").asText("");
        } catch (JsonProcessingException e) {
            return "";
        }
    }

    /** 解析设备上行 JSON 文本为 hello。 */
    public static HelloMessage parseHello(String json) {
        return parse(json, HelloMessage.class);
    }

    /** 解析设备上行 JSON 文本为 listen。 */
    public static ListenMessage parseListen(String json) {
        return parse(json, ListenMessage.class);
    }

    /** 解析设备上行 JSON 文本为 abort。 */
    public static AbortMessage parseAbort(String json) {
        return parse(json, AbortMessage.class);
    }

    /** 组装 server hello。 */
    public static String serverHello(int sampleRate, int channels, int bitsPerSample) {
        return toJson(ServerHelloMessage.of(sampleRate, channels, bitsPerSample));
    }

    /** 组装 stt 消息。 */
    public static String stt(String sessionId, String text) {
        SttMessage msg = new SttMessage();
        msg.setSessionId(sessionId);
        msg.setText(text);
        return toJson(msg);
    }

    /** 组装 tts 阶段消息。 */
    public static String tts(String sessionId, String state, String text) {
        TtsMessage msg = new TtsMessage();
        msg.setSessionId(sessionId);
        msg.setState(state);
        msg.setText(text);
        return toJson(msg);
    }

    /** 组装 llm 消息。 */
    public static String llm(String sessionId, String emotion, String text) {
        LlmMessage msg = new LlmMessage();
        msg.setSessionId(sessionId);
        msg.setEmotion(emotion);
        msg.setText(text);
        return toJson(msg);
    }

    private static <T> T parse(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("parse wire message failed", e);
        }
    }
}
