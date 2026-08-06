package com.ai.manager.system.iot.tts;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.system.iot.TestHttpSupport;
import com.ai.manager.system.iot.config.IotProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenAiCompatibleTtsProviderTest {

    @Mock
    private HttpClient httpClient;

    private IotProperties properties;
    private OpenAiCompatibleTtsProvider provider;

    @BeforeEach
    void setUp() {
        properties = new IotProperties();
        properties.getTts().setBaseUrl("https://example.com");
        properties.getTts().setApiKey("sk-test");
        properties.getTts().setModel("tts-1");
        properties.getTts().setVoice("nova");
        provider = new OpenAiCompatibleTtsProvider(properties, new ObjectMapper());
        ReflectionTestUtils.setField(provider, "httpClient", httpClient);
    }

    @Test
    void synthesize_shouldPostJsonWithModelInputVoiceAndPassThroughAudioBytes() throws Exception {
        byte[] audioBytes = new byte[]{10, 20, 30, 40};
        AtomicReference<HttpRequest> captured = new AtomicReference<>();
        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any()))
                .thenAnswer(inv -> {
                    HttpRequest req = inv.getArgument(0);
                    captured.set(req);
                    return TestHttpSupport.response(req, 200, audioBytes);
                });

        TtsResult result = provider.synthesize("你好", new TtsContext("s1", "", 16000));

        assertThat(result.audioBytes()).isEqualTo(audioBytes);
        assertThat(result.format()).isEqualTo("wav");
        assertThat(result.sampleRate()).isEqualTo(16000);

        HttpRequest request = captured.get();
        assertThat(request.uri().toString()).isEqualTo("https://example.com/v1/audio/speech");
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.headers().firstValue("Content-Type")).hasValue("application/json");
        assertThat(request.headers().firstValue("Authorization")).hasValue("Bearer sk-test");

        byte[] body = TestHttpSupport.readBody(request.bodyPublisher().orElseThrow());
        JsonNode json = new ObjectMapper().readTree(body);
        assertThat(json.path("model").asText()).isEqualTo("tts-1");
        assertThat(json.path("input").asText()).isEqualTo("你好");
        assertThat(json.path("voice").asText()).isEqualTo("nova"); // ctx.voice 为空回落配置
        assertThat(json.path("response_format").asText()).isEqualTo("wav");
    }

    @Test
    void synthesize_shouldPreferCtxVoiceWhenProvided() throws Exception {
        AtomicReference<HttpRequest> captured = new AtomicReference<>();
        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any()))
                .thenAnswer(inv -> {
                    HttpRequest req = inv.getArgument(0);
                    captured.set(req);
                    return TestHttpSupport.response(req, 200, new byte[]{1});
                });

        provider.synthesize("hi", new TtsContext("s1", "echo", 24000));

        byte[] body = TestHttpSupport.readBody(captured.get().bodyPublisher().orElseThrow());
        JsonNode json = new ObjectMapper().readTree(body);
        assertThat(json.path("voice").asText()).isEqualTo("echo");
    }

    @Test
    void synthesize_whenNon2xx_shouldThrowBusinessException() throws Exception {
        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any()))
                .thenAnswer(inv -> TestHttpSupport.response(inv.getArgument(0), 429,
                        "rate limited".getBytes(StandardCharsets.UTF_8)));

        assertThatThrownBy(() -> provider.synthesize("hi", new TtsContext("s1", "", 16000)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("HTTP 429");
    }

    @Test
    void synthesize_whenBlankText_shouldThrowBadRequest() {
        assertThatThrownBy(() -> provider.synthesize(" ", new TtsContext("s1", "", 16000)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文本为空");
    }

    @Test
    void synthesize_whenIoError_shouldThrowBusinessException() throws Exception {
        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any()))
                .thenThrow(new java.io.IOException("timeout"));

        assertThatThrownBy(() -> provider.synthesize("hi", new TtsContext("s1", "", 16000)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("TTS 请求异常");
    }
}
