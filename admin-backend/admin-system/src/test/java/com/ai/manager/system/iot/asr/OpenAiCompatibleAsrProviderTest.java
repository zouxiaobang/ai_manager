package com.ai.manager.system.iot.asr;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.system.iot.TestHttpSupport;
import com.ai.manager.system.iot.config.IotProperties;
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
class OpenAiCompatibleAsrProviderTest {

    @Mock
    private HttpClient httpClient;

    private IotProperties properties;
    private OpenAiCompatibleAsrProvider provider;

    @BeforeEach
    void setUp() {
        properties = new IotProperties();
        properties.getAsr().setBaseUrl("https://example.com");
        properties.getAsr().setApiKey("sk-test");
        properties.getAsr().setModel("whisper-1");
        provider = new OpenAiCompatibleAsrProvider(properties, new ObjectMapper());
        ReflectionTestUtils.setField(provider, "httpClient", httpClient);
    }

    @Test
    void transcribe_shouldPostMultipartWithFileModelLanguageAndParseText() throws Exception {
        AtomicReference<HttpRequest> captured = new AtomicReference<>();
        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenAnswer(inv -> {
                    HttpRequest req = inv.getArgument(0);
                    captured.set(req);
                    return TestHttpSupport.response(req, 200, "{\"text\":\"你好，世界\"}");
                });

        byte[] wav = new byte[]{1, 2, 3};
        AsrResult result = provider.transcribe(wav, new AsrContext("s1", "zh", 16000));

        assertThat(result.text()).isEqualTo("你好，世界");
        assertThat(result.raw()).contains("你好");
        assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);

        HttpRequest request = captured.get();
        assertThat(request.uri().toString()).isEqualTo("https://example.com/v1/audio/transcriptions");
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.headers().firstValue("Authorization")).hasValue("Bearer sk-test");
        String contentType = request.headers().firstValue("Content-Type").orElse("");
        assertThat(contentType).startsWith("multipart/form-data; boundary=");

        byte[] body = TestHttpSupport.readBody(request.bodyPublisher().orElseThrow());
        String bodyStr = new String(body, StandardCharsets.UTF_8);
        assertThat(bodyStr)
                .contains("name=\"file\"")
                .contains("audio.wav")
                .contains("audio/wav")
                .contains("name=\"model\"")
                .contains("whisper-1")
                .contains("name=\"language\"")
                .contains("\r\nzh\r\n");
        // 音频字节原样透传
        assertThat(body).containsSequence(wav);
    }

    @Test
    void transcribe_whenLanguageBlank_shouldOmitLanguageField() throws Exception {
        AtomicReference<HttpRequest> captured = new AtomicReference<>();
        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenAnswer(inv -> {
                    HttpRequest req = inv.getArgument(0);
                    captured.set(req);
                    return TestHttpSupport.response(req, 200, "{\"text\":\"ok\"}");
                });

        provider.transcribe(new byte[]{9}, new AsrContext("s2", "", 16000));

        String body = new String(TestHttpSupport.readBody(captured.get().bodyPublisher().orElseThrow()),
                StandardCharsets.UTF_8);
        assertThat(body).doesNotContain("name=\"language\"");
    }

    @Test
    void transcribe_whenNon2xx_shouldThrowBusinessException() throws Exception {
        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenAnswer(inv -> TestHttpSupport.response(inv.getArgument(0), 401, "{\"error\":\"unauthorized\"}"));

        assertThatThrownBy(() -> provider.transcribe(new byte[]{1}, new AsrContext("s3", "zh", 16000)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("HTTP 401");
    }

    @Test
    void transcribe_whenEmptyAudio_shouldThrowBadRequest() {
        assertThatThrownBy(() -> provider.transcribe(new byte[0], new AsrContext("s4", "zh", 16000)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("音频为空");
    }

    @Test
    void transcribe_whenIoError_shouldThrowBusinessException() throws Exception {
        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenThrow(new java.io.IOException("connection reset"));

        assertThatThrownBy(() -> provider.transcribe(new byte[]{1}, new AsrContext("s5", "zh", 16000)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ASR 请求异常");
    }

    @Test
    void transcribe_whenBaseUrlTrailingSlash_shouldTrimBeforeAppend() throws Exception {
        properties.getAsr().setBaseUrl("https://example.com/");
        AtomicReference<HttpRequest> captured = new AtomicReference<>();
        when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenAnswer(inv -> {
                    HttpRequest req = inv.getArgument(0);
                    captured.set(req);
                    return TestHttpSupport.response(req, 200, "{\"text\":\"x\"}");
                });

        provider.transcribe(new byte[]{1}, new AsrContext("s6", "", 16000));

        assertThat(captured.get().uri().toString()).isEqualTo("https://example.com/v1/audio/transcriptions");
    }
}
