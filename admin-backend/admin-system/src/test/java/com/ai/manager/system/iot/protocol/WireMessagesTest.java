package com.ai.manager.system.iot.protocol;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * WireMessages 文本消息 JSON 组装/解析测试。
 */
class WireMessagesTest {

    @Test
    void parseHello_shouldExtractSnakeCaseFields() {
        String json = """
                {"type":"hello","uuid":"u1","mac":"aabbccdd","version":"2.2.1","client_id":"c1","sample_rate":16000}
                """;
        HelloMessage hello = WireMessages.parseHello(json);
        assertThat(hello.getType()).isEqualTo("hello");
        assertThat(hello.getUuid()).isEqualTo("u1");
        assertThat(hello.getMac()).isEqualTo("aabbccdd");
        assertThat(hello.getVersion()).isEqualTo("2.2.1");
        assertThat(hello.getClientId()).isEqualTo("c1");
        assertThat(hello.getSampleRate()).isEqualTo(16000);
    }

    @Test
    void parseHello_shouldIgnoreUnknownFields() {
        String json = "{\"type\":\"hello\",\"extra\":123}";
        HelloMessage hello = WireMessages.parseHello(json);
        assertThat(hello.getType()).isEqualTo("hello");
        assertThat(hello.getUuid()).isNull();
    }

    @Test
    void parseListen_shouldExtractState() {
        ListenMessage listen = WireMessages.parseListen("{\"type\":\"listen\",\"state\":\"start\",\"session_id\":\"s1\"}");
        assertThat(listen.getState()).isEqualTo("start");
        assertThat(listen.getSessionId()).isEqualTo("s1");
    }

    @Test
    void parseAbort_shouldExtractReason() {
        AbortMessage abort = WireMessages.parseAbort("{\"type\":\"abort\",\"reason\":\"wake_word_detected\"}");
        assertThat(abort.getReason()).isEqualTo("wake_word_detected");
    }

    @Test
    void parseType_shouldReturnTypeField() {
        assertThat(WireMessages.parseType("{\"type\":\"listen\"}")).isEqualTo("listen");
        assertThat(WireMessages.parseType("{\"type\":\"mcp\"}")).isEqualTo("mcp");
        assertThat(WireMessages.parseType("not-json")).isEmpty();
    }

    @Test
    void serverHello_shouldContainSessionIdAndAudioParams() {
        String json = WireMessages.serverHello(16000, 1, 16);
        assertThat(json).contains("\"type\":\"server_hello\"");
        assertThat(json).contains("\"session_id\"");
        assertThat(json).contains("\"sample_rate\":16000");
        assertThat(json).contains("\"channels\":1");
    }

    @Test
    void serverHello_sessionIdShouldBeUnique() {
        String json1 = WireMessages.serverHello(16000, 1, 16);
        String json2 = WireMessages.serverHello(16000, 1, 16);
        assertThat(json1).isNotEqualTo(json2);
    }

    @Test
    void stt_shouldUseSnakeCase() {
        String json = WireMessages.stt("s1", "你好");
        assertThat(json).contains("\"type\":\"stt\"");
        assertThat(json).contains("\"session_id\":\"s1\"");
        assertThat(json).contains("\"text\":\"你好\"");
    }

    @Test
    void tts_shouldUseSnakeCase() {
        String json = WireMessages.tts("s1", "start", null);
        assertThat(json).contains("\"type\":\"tts\"");
        assertThat(json).contains("\"state\":\"start\"");
        assertThat(json).doesNotContain("\"text\"");
    }

    @Test
    void llm_shouldUseSnakeCase() {
        String json = WireMessages.llm("s1", "happy", "好的");
        assertThat(json).contains("\"type\":\"llm\"");
        assertThat(json).contains("\"emotion\":\"happy\"");
        assertThat(json).contains("\"text\":\"好的\"");
    }

    @Test
    void parseHello_withInvalidJson_shouldThrow() {
        assertThatThrownBy(() -> WireMessages.parseHello("not-json"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
