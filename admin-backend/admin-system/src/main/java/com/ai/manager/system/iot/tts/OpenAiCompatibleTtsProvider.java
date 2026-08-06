package com.ai.manager.system.iot.tts;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.iot.config.IotProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OpenAI 兼容 TTS provider：POST {baseUrl}/v1/audio/speech（JSON），返回音频字节。
 * <p>
 * 沿用 {@code client/BaiduPanClient} 的 JDK {@link HttpClient} + ObjectMapper 模式，不引重型 SDK；
 * 兼容 OpenAI / Azure / 自建 tts 服务，换厂商只改配置。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiCompatibleTtsProvider implements TtsProvider {

    /** 配置里的 type 标识 */
    public static final String TYPE = "openai-compatible";

    private static final String SPEECH_PATH = "/v1/audio/speech";
    private static final String DEFAULT_RESPONSE_FORMAT = "wav";
    private static final int REQUEST_TIMEOUT_SECONDS = 60;

    private final IotProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Override
    public TtsResult synthesize(String text, TtsContext ctx) {
        if (!StringUtils.hasText(text)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "TTS 输入文本为空");
        }
        IotProperties.Tts cfg = properties.getTts();
        String url = trimTrailingSlash(cfg.getBaseUrl()) + SPEECH_PATH;
        String responseFormat = StringUtils.hasText(cfg.getResponseFormat())
                ? cfg.getResponseFormat() : DEFAULT_RESPONSE_FORMAT;
        String voice = StringUtils.hasText(ctx.voice()) ? ctx.voice() : cfg.getVoice();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", cfg.getModel());
        payload.put("input", text);
        payload.put("voice", voice);
        payload.put("response_format", responseFormat);
        byte[] json;
        try {
            json = objectMapper.writeValueAsBytes(payload);
        } catch (JsonProcessingException e) {
            log.error("TTS 请求体序列化失败 sessionId={}", ctx.sessionId(), e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "TTS 请求体序列化失败");
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(json));
        if (StringUtils.hasText(cfg.getApiKey())) {
            builder.header("Authorization", "Bearer " + cfg.getApiKey());
        }

        long start = System.currentTimeMillis();
        try {
            HttpResponse<byte[]> resp = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
            long cost = System.currentTimeMillis() - start;
            if (resp.statusCode() >= 300) {
                String errBody = resp.body() == null ? "" : new String(resp.body(), StandardCharsets.UTF_8);
                throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(),
                        "TTS 请求失败: HTTP " + resp.statusCode() + ", body=" + errBody);
            }
            return new TtsResult(resp.body(), responseFormat, ctx.sampleRate(), cost);
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("TTS 请求异常 baseUrl={}, sessionId={}", cfg.getBaseUrl(), ctx.sessionId(), e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "TTS 请求异常: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("TTS 请求被中断 sessionId={}", ctx.sessionId(), e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "TTS 请求被中断");
        }
    }

    private String trimTrailingSlash(String url) {
        if (!StringUtils.hasText(url)) {
            return "";
        }
        String s = url;
        while (s.endsWith("/") && s.length() > 1) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
