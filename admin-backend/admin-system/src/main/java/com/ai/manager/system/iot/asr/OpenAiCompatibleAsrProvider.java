package com.ai.manager.system.iot.asr;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.iot.config.IotProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

/**
 * OpenAI 兼容 ASR provider：POST {baseUrl}/v1/audio/transcriptions（multipart）。
 * <p>
 * 沿用 {@code client/BaiduPanClient} 的 JDK {@link HttpClient} + ObjectMapper 模式，不引重型 SDK；
 * Whisper 兼容服务（OpenAI / Azure / 自建）均可对接，换厂商只改配置。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiCompatibleAsrProvider implements AsrProvider {

    /** 配置里的 type 标识 */
    public static final String TYPE = "openai-compatible";

    private static final String TRANSCRIPTIONS_PATH = "/v1/audio/transcriptions";
    private static final String UPLOAD_FILENAME = "audio.wav";
    private static final String UPLOAD_CONTENT_TYPE = "audio/wav";
    private static final int REQUEST_TIMEOUT_SECONDS = 60;

    private final IotProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Override
    public AsrResult transcribe(byte[] wavAudio, AsrContext ctx) {
        if (wavAudio == null || wavAudio.length == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "ASR 输入音频为空");
        }
        IotProperties.Asr cfg = properties.getAsr();
        String url = trimTrailingSlash(cfg.getBaseUrl()) + TRANSCRIPTIONS_PATH;
        String boundary = "----AiManagerAsr" + UUID.randomUUID().toString().replace("-", "");

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(
                        buildMultipart(wavAudio, cfg.getModel(), ctx.language(), boundary)));
        if (StringUtils.hasText(cfg.getApiKey())) {
            builder.header("Authorization", "Bearer " + cfg.getApiKey());
        }

        long start = System.currentTimeMillis();
        try {
            HttpResponse<String> resp = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            long cost = System.currentTimeMillis() - start;
            if (resp.statusCode() >= 300) {
                throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(),
                        "ASR 请求失败: HTTP " + resp.statusCode() + ", body=" + resp.body());
            }
            JsonNode root = objectMapper.readTree(resp.body());
            return new AsrResult(root.path("text").asText(""), resp.body(), cost);
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("ASR 请求异常 baseUrl={}, sessionId={}", cfg.getBaseUrl(), ctx.sessionId(), e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "ASR 请求异常: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("ASR 请求被中断 sessionId={}", ctx.sessionId(), e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "ASR 请求被中断");
        }
    }

    /**
     * 组装 multipart/form-data 请求体：file + model +（可选）language。
     * 与 BaiduPanClient 的 multipart 拼装保持一致（手写 boundary，避免引额外依赖）。
     */
    private byte[] buildMultipart(byte[] wav, String model, String language, String boundary) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeFilePart(out, boundary, "file", UPLOAD_FILENAME, UPLOAD_CONTENT_TYPE, wav);
        writeTextPart(out, boundary, "model", model);
        if (StringUtils.hasText(language)) {
            writeTextPart(out, boundary, "language", language);
        }
        writeTail(out, boundary);
        return out.toByteArray();
    }

    private void writeFilePart(ByteArrayOutputStream out, String boundary, String name,
                               String filename, String contentType, byte[] content) {
        String head = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: " + contentType + "\r\n\r\n";
        writeUtf8(out, head);
        out.write(content, 0, content.length);
        writeUtf8(out, "\r\n");
    }

    private void writeTextPart(ByteArrayOutputStream out, String boundary, String name, String value) {
        String part = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
                + value + "\r\n";
        writeUtf8(out, part);
    }

    private void writeTail(ByteArrayOutputStream out, String boundary) {
        writeUtf8(out, "--" + boundary + "--\r\n");
    }

    private void writeUtf8(ByteArrayOutputStream out, String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        out.write(bytes, 0, bytes.length);
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
