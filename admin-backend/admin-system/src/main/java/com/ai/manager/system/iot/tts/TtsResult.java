package com.ai.manager.system.iot.tts;

/**
 * TTS 合成结果。
 *
 * @param audioBytes 合成音频字节（wav PCM 或 opus 容器，由 {@code format} 标识）
 * @param format     音频格式：wav / opus（与请求 response_format 一致）
 * @param sampleRate 音频采样率（Hz），供后续重编码参考
 * @param durationMs 本次合成请求耗时（毫秒）
 */
public record TtsResult(byte[] audioBytes, String format, int sampleRate, long durationMs) {
}
